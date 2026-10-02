package com.rishabh.gymbros.features.cloud

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.Scopes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypted Google Drive backup for GymBros.
 *
 * Security model:
 * - Local app data remains the source of truth.
 * - Backups are compressed and encrypted with a random AES-256-GCM master key before upload.
 * - The master key is wrapped locally by Android Keystore.
 * - A second copy of the master key is wrapped by a user recovery password and stored in Drive.
 * - The recovery password and raw master key are never uploaded or logged.
 * - Drive access is restricted to the appDataFolder scope.
 */
class CloudBackupManager private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<CloudBackupState> = _state.asStateFlow()

    // Keep the token returned by the interactive authorization flow in memory.
    // Re-authorizing immediately after the user grants Drive access can make
    // Google Play services start a second authorization transaction and fail
    // with internal messages such as "Caller-provided VM not permitted".
    @Volatile private var cachedAccessToken: String? = null

    init { refreshUser() }

    fun refreshUser() {
        val disconnected = prefs.getBoolean(KEY_MANUALLY_DISCONNECTED, false)
        val email = if (disconnected) "" else prefs.getString(KEY_ACCOUNT_EMAIL, "").orEmpty()
        val name = if (disconnected) "" else prefs.getString(KEY_ACCOUNT_NAME, "").orEmpty()
        val connected = !disconnected && prefs.getBoolean(KEY_DRIVE_CONNECTED, false)
        val localKeyReady = runCatching { loadLocalMasterKey() != null }.getOrDefault(false)
        _state.value = _state.value.copy(
            signedIn = connected,
            email = email,
            displayName = name,
            autoBackup = prefs.getBoolean(KEY_AUTO, true),
            lastBackupMillis = prefs.getLong(KEY_LAST_BACKUP, 0L),
            securitySetupRequired = connected && !localKeyReady &&
                !_state.value.securityUnlockRequired,
            secureBackupReady = localKeyReady
        )
    }

    fun dismissRestoreSuggestion() {
        prefs.edit().putBoolean(KEY_RESTORE_DISMISSED, true).putBoolean(KEY_RESTORE_DECISION_PENDING, false).apply()
        _state.value = _state.value.copy(restoreSuggestionDismissed = true)
    }

    fun setAutoBackup(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO, enabled).apply()
        _state.value = _state.value.copy(autoBackup = enabled)
    }

    suspend fun signIn(activityContext: Context): Result<Unit> {
        val activity = activityContext as? Activity
            ?: return Result.failure(IllegalArgumentException("Google authorization requires an Activity"))
        _state.value = _state.value.copy(busy = true, message = "Opening Google Drive permission…")
        return try {
            prefs.edit().putBoolean(KEY_MANUALLY_DISCONNECTED, false).apply()
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(requestedScopes())
                .setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)
                .build()
            val result = Identity.getAuthorizationClient(activity).authorize(request).await()
            if (result.hasResolution()) {
                val pending = result.pendingIntent ?: error("Google authorization could not be opened")
                activity.startIntentSenderForResult(
                    pending.intentSender,
                    DRIVE_AUTH_REQUEST_CODE,
                    null,
                    0,
                    0,
                    0
                )
                _state.value = _state.value.copy(busy = false, message = "Finish Google permission to connect Drive")
            } else {
                acceptAuthorization(result)
                refreshCloudMetadata(forceRestoreSuggestion = true).getOrThrow()
                _state.value = _state.value.copy(busy = false, message = "Google Drive connected")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            _state.value = _state.value.copy(busy = false)
            throw e
        } catch (e: Exception) {
            Log.e(TAG_AUTH, "Google authorization failed: ${e.javaClass.simpleName}")
            _state.value = _state.value.copy(busy = false, message = e.localizedMessage ?: "Google authorization failed")
            Result.failure(e)
        }
    }

    suspend fun handleAuthorizationResult(resultCode: Int, data: Intent?): Result<Unit> {
        if (resultCode != Activity.RESULT_OK || data == null) {
            _state.value = _state.value.copy(busy = false, message = "Google Drive connection cancelled")
            return Result.failure(IllegalStateException("Google Drive connection cancelled"))
        }
        return runCatching {
            _state.value = _state.value.copy(busy = true, message = "Connecting Google Drive…")
            val result = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
            acceptAuthorization(result)
            prefs.edit().putBoolean(KEY_RESTORE_DISMISSED, false).apply()
            refreshCloudMetadata(forceRestoreSuggestion = true).getOrThrow()
            _state.value = _state.value.copy(busy = false, message = "Google Drive connected")
        }.onFailure { e ->
            Log.e(TAG_AUTH, "Authorization result failed: ${e.javaClass.simpleName}")
            _state.value = _state.value.copy(busy = false, message = e.localizedMessage ?: "Could not connect Google Drive")
        }
    }

    suspend fun signOut(): Result<Unit> = runCatching {
        cachedAccessToken = null
        prefs.edit()
            .putBoolean(KEY_MANUALLY_DISCONNECTED, true)
            .putBoolean(KEY_DRIVE_CONNECTED, false)
            .remove(KEY_ACCOUNT_EMAIL)
            .remove(KEY_ACCOUNT_NAME)
            .apply()
        _state.value = readState().copy(message = "Signed out")
    }

    suspend fun setRecoveryPassword(password: String): Result<Unit> = runCatching {
        require(password.length >= MIN_PASSWORD_LENGTH) { "Use at least $MIN_PASSWORD_LENGTH characters for the recovery password" }
        require(_state.value.signedIn) { "Connect Google Drive first" }
        _state.value = _state.value.copy(busy = true, message = "Securing your backup key…")
        val token = getAccessToken()
        val existing = findEnvelope(token)
        if (existing != null) {
            // An encrypted master-key envelope already exists in Drive. Move the UI
            // into unlock mode instead of leaving it on Create recovery password.
            _state.value = _state.value.copy(
                busy = false,
                securitySetupRequired = false,
                securityUnlockRequired = true,
                secureBackupReady = false,
                message = "Secure backup found. Enter your recovery password to unlock it."
            )
            return@runCatching
        }
        val master = ByteArray(32).also(secureRandom::nextBytes)
        val envelope = createPasswordEnvelope(master, password)
        uploadDriveFile(
            token = token,
            name = KEY_ENVELOPE_FILE,
            bytes = envelope.toString().toByteArray(Charsets.UTF_8),
            appProperties = mapOf("gymbrosKind" to "key_envelope", "version" to "1")
        )
        saveLocalMasterKey(master)
        refreshCloudMetadata().getOrThrow()
        _state.value = _state.value.copy(busy = false, message = "Secure backup encryption is ready")
    }.onFailure { e ->
        _state.value = _state.value.copy(busy = false, message = e.localizedMessage ?: "Could not secure backup key")
    }

    suspend fun unlockWithRecoveryPassword(password: String): Result<Unit> = runCatching {
        require(password.isNotEmpty()) { "Enter your recovery password" }
        require(_state.value.signedIn) { "Connect Google Drive first" }
        _state.value = _state.value.copy(busy = true, message = "Unlocking encrypted backups…")
        val token = getAccessToken()
        val envelopeFile = findEnvelope(token) ?: error("No encrypted recovery key was found in Drive")
        val envelope = JSONObject(String(downloadDriveFile(token, envelopeFile.id), Charsets.UTF_8))
        val master = decryptPasswordEnvelope(envelope, password)
        saveLocalMasterKey(master)
        refreshCloudMetadata().getOrThrow()
        _state.value = _state.value.copy(busy = false, message = "Encrypted backups unlocked")
    }.onFailure { e ->
        _state.value = _state.value.copy(
            busy = false,
            message = if (e is javax.crypto.AEADBadTagException) "Incorrect recovery password" else e.localizedMessage ?: "Could not unlock backups"
        )
    }

    suspend fun refreshCloudMetadata(forceRestoreSuggestion: Boolean = false): Result<Unit> {
        if (!_state.value.signedIn) return Result.success(Unit)
        return runCatching {
            val token = getAccessToken()
            val files = listDriveFiles(token)
            val backupFiles = files.filter { it.name.startsWith(BACKUP_PREFIX) }
            val points = backupFiles.mapNotNull(::toRecoveryPoint).sortedByDescending { it.createdAtMillis }
            val latest = points.firstOrNull()
            val envelopeExists = files.any { it.name == KEY_ENVELOPE_FILE }
            val localKeyReady = loadLocalMasterKey() != null
            val backupExists = latest != null
            if (forceRestoreSuggestion && backupExists) prefs.edit().putBoolean(KEY_RESTORE_DISMISSED, false).apply()
            val dismissed = prefs.getBoolean(KEY_RESTORE_DISMISSED, false)
            prefs.edit()
                .putBoolean(KEY_RESTORE_CHECK_PENDING, false)
                .putBoolean(KEY_RESTORE_DECISION_PENDING, backupExists && !dismissed)
                .apply()
            _state.value = _state.value.copy(
                backupCheckPending = false,
                cloudBackupMillis = latest?.createdAtMillis ?: 0L,
                cloudBackupSchema = latest?.schema ?: 0,
                cloudBackupAvailable = backupExists,
                recoveryPoints = points,
                restoreSuggestionDismissed = dismissed,
                securitySetupRequired = !envelopeExists && !localKeyReady,
                securityUnlockRequired = envelopeExists && !localKeyReady,
                secureBackupReady = localKeyReady
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(message = e.localizedMessage ?: "Could not check Google Drive backups")
        }
    }

    suspend fun backupNow(automatic: Boolean = false): Result<Unit> {
        if (!_state.value.signedIn) return Result.failure(IllegalStateException("Connect Google Drive first"))
        if (automatic && !prefs.getBoolean(KEY_AUTO, true)) return Result.success(Unit)
        if (automatic && (prefs.getBoolean(KEY_RESTORE_CHECK_PENDING, false) || prefs.getBoolean(KEY_RESTORE_DECISION_PENDING, false) || _state.value.shouldSuggestRestore)) return Result.success(Unit)
        if (_state.value.busy) return Result.success(Unit)
        val master = loadLocalMasterKey()
        if (master == null) {
            if (!automatic) _state.value = _state.value.copy(message = if (_state.value.securityUnlockRequired) "Enter your recovery password first" else "Create a recovery password first")
            return if (automatic) Result.success(Unit) else Result.failure(IllegalStateException("Secure backup encryption is not unlocked"))
        }
        _state.value = _state.value.copy(busy = true, message = if (automatic) "Backing up securely…" else "Creating encrypted recovery point…")
        return runCatching {
            val token = getAccessToken()
            val payload = withContext(Dispatchers.IO) { createSnapshot() }
            val compressed = gzip(payload.toString().toByteArray(Charsets.UTF_8))
            val encrypted = encryptBackup(compressed, master)
            val now = System.currentTimeMillis()
            val props = backupProperties(now, automatic = automatic, label = "", pinned = false, safety = false)
            uploadDriveFile(token, "$BACKUP_PREFIX$now$BACKUP_SUFFIX", encrypted, props)
            trimRecoveryPoints(token)
            prefs.edit().putLong(KEY_LAST_BACKUP, now).putBoolean(KEY_RESTORE_DISMISSED, true).apply()
            refreshCloudMetadata().getOrThrow()
            _state.value = _state.value.copy(
                busy = false,
                lastBackupMillis = now,
                restoreSuggestionDismissed = true,
                message = if (automatic) "Encrypted backup saved automatically" else "Encrypted backup complete • recovery point saved"
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(busy = false, message = e.localizedMessage ?: "Backup failed")
        }
    }

    suspend fun restoreLatest(): Result<Unit> {
        val id = _state.value.recoveryPoints.maxByOrNull { it.createdAtMillis }?.id
            ?: return Result.failure(IllegalStateException("No cloud backup found"))
        return restoreBackup(id)
    }

    suspend fun restoreRecoveryPoint(id: String): Result<Unit> = restoreBackup(id)

    suspend fun renameRecoveryPoint(id: String, label: String): Result<Unit> = runCatching {
        val token = getAccessToken()
        val point = _state.value.recoveryPoints.firstOrNull { it.id == id } ?: error("Recovery point not found")
        patchDriveProperties(token, id, backupProperties(point.createdAtMillis, point.automatic, label.trim().take(40), point.pinned, point.safety))
        refreshCloudMetadata().getOrThrow()
        _state.value = _state.value.copy(message = "Recovery point renamed")
    }.onFailure { e -> _state.value = _state.value.copy(message = e.localizedMessage ?: "Rename failed") }

    suspend fun setRecoveryPointPinned(id: String, pinned: Boolean): Result<Unit> = runCatching {
        val token = getAccessToken()
        val point = _state.value.recoveryPoints.firstOrNull { it.id == id } ?: error("Recovery point not found")
        patchDriveProperties(token, id, backupProperties(point.createdAtMillis, point.automatic, point.label, pinned, point.safety))
        trimRecoveryPoints(token)
        refreshCloudMetadata().getOrThrow()
        _state.value = _state.value.copy(message = if (pinned) "Recovery point pinned" else "Recovery point unpinned")
    }.onFailure { e -> _state.value = _state.value.copy(message = e.localizedMessage ?: "Pin update failed") }

    suspend fun deleteRecoveryPoint(id: String): Result<Unit> = runCatching {
        val token = getAccessToken()
        deleteDriveFile(token, id)
        refreshCloudMetadata().getOrThrow()
        _state.value = _state.value.copy(message = "Recovery point deleted")
    }.onFailure { e -> _state.value = _state.value.copy(message = e.localizedMessage ?: "Delete failed") }

    suspend fun deleteAllCloudBackups(): Result<Unit> = runCatching {
        require(_state.value.signedIn) { "Connect Google Drive first" }
        _state.value = _state.value.copy(busy = true, message = "Deleting encrypted backups…")
        val token = getAccessToken()
        listDriveFiles(token).filter { it.name.startsWith(BACKUP_PREFIX) }.forEach { deleteDriveFile(token, it.id) }
        prefs.edit().remove(KEY_LAST_BACKUP).putBoolean(KEY_RESTORE_DISMISSED, true).apply()
        refreshCloudMetadata().getOrThrow()
        _state.value = _state.value.copy(
            busy = false,
            lastBackupMillis = 0L,
            cloudBackupMillis = 0L,
            cloudBackupSchema = 0,
            cloudBackupAvailable = false,
            recoveryPoints = emptyList(),
            restoreSuggestionDismissed = true,
            message = "All encrypted backups deleted"
        )
    }.onFailure { e -> _state.value = _state.value.copy(busy = false, message = e.localizedMessage ?: "Delete failed") }

    private suspend fun restoreBackup(fileId: String): Result<Unit> {
        if (!_state.value.signedIn) return Result.failure(IllegalStateException("Connect Google Drive first"))
        if (_state.value.busy) return Result.success(Unit)
        val master = loadLocalMasterKey() ?: run {
            _state.value = _state.value.copy(message = "Enter your recovery password first")
            return Result.failure(IllegalStateException("Encrypted backups are locked"))
        }
        _state.value = _state.value.copy(busy = true, message = "Verifying encrypted backup…")
        return runCatching {
            val token = getAccessToken()
            val selected = _state.value.recoveryPoints.firstOrNull { it.id == fileId } ?: error("Cloud backup no longer exists")
            val encrypted = downloadDriveFile(token, fileId)
            val compressed = decryptBackup(encrypted, master)
            val json = try { JSONObject(String(gunzip(compressed), Charsets.UTF_8)) } catch (_: Exception) {
                error("Backup is corrupted or incomplete. Your current data was not changed.")
            }
            validateSnapshot(json)
            createSafetyRecoveryPoint(token, master)
            withContext(Dispatchers.IO) {
                restoreSnapshot(json)
                com.rishabh.gymbros.features.settings.data.SettingsRepository.getInstance(context).reloadFromDisk()
                com.rishabh.gymbros.features.workout.data.WorkoutRepository.getInstance(context).reloadFromDisk()
                com.rishabh.gymbros.features.water.data.WaterRepository.getInstance(context).reloadFromDisk()
                com.rishabh.gymbros.features.nutrition.data.NutritionRepository.getInstance(context).refresh()
            }
            prefs.edit()
                .putLong(KEY_LAST_BACKUP, selected.createdAtMillis)
                .putBoolean(KEY_RESTORE_DISMISSED, true)
                .putBoolean(KEY_RESTORE_DECISION_PENDING, false)
                .apply()
            refreshCloudMetadata().getOrThrow()
            _state.value = _state.value.copy(
                busy = false,
                lastBackupMillis = selected.createdAtMillis,
                restoreSuggestionDismissed = true,
                message = "Your data has been restored. An encrypted safety backup was saved."
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(busy = false, message = e.localizedMessage ?: "Restore failed")
        }
    }

    private suspend fun createSafetyRecoveryPoint(token: String, master: ByteArray) {
        val payload = withContext(Dispatchers.IO) { createSnapshot() }
        val compressed = gzip(payload.toString().toByteArray(Charsets.UTF_8))
        val encrypted = encryptBackup(compressed, master)
        val now = System.currentTimeMillis()
        uploadDriveFile(
            token,
            "$BACKUP_PREFIX$now$BACKUP_SUFFIX",
            encrypted,
            backupProperties(now, automatic = false, label = "Before restore", pinned = false, safety = true)
        )
        trimRecoveryPoints(token)
    }

    private fun acceptAuthorization(result: AuthorizationResult) {
        cachedAccessToken = result.accessToken?.takeIf { it.isNotBlank() }
        // AuthorizationResult is for OAuth authorization. Google may legitimately
        // return no GoogleSignInAccount here, so Drive connectivity must not depend
        // on an email being present.
        val account = result.toGoogleSignInAccount()
        val email = account?.email ?: account?.account?.name.orEmpty()
        val displayName = account?.displayName.orEmpty()

        prefs.edit()
            .putBoolean(KEY_DRIVE_CONNECTED, true)
            .putString(KEY_ACCOUNT_EMAIL, email)
            .putString(KEY_ACCOUNT_NAME, displayName)
            .putBoolean(KEY_MANUALLY_DISCONNECTED, false)
            .putBoolean(KEY_RESTORE_CHECK_PENDING, true)
            .apply()

        refreshUser()
        _state.value = _state.value.copy(backupCheckPending = true)
    }

    private fun requestedScopes(): List<Scope> = listOf(
        Scope(Scopes.DRIVE_APPFOLDER)
    )

    private suspend fun getAccessToken(): String {
        require(prefs.getBoolean(KEY_DRIVE_CONNECTED, false)) { "Connect Google Drive first" }

        // The authorization result already contains the access token we need.
        // Reuse it instead of asking Google Play services to authorize the same
        // request a second time immediately after consent.
        cachedAccessToken?.takeIf { it.isNotBlank() }?.let { return it }

        val request = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes())
            .build()
        val result = Identity.getAuthorizationClient(context).authorize(request).await()
        if (result.hasResolution()) {
            error("Google Drive permission needs to be granted again. Tap Sign out, then Continue with Google.")
        }
        val token = result.accessToken?.takeIf { it.isNotBlank() }
            ?: error("Google Drive did not return an access token")
        cachedAccessToken = token
        return token
    }

    private fun accountStorageKey(): String {
        // The local wrapped master key only needs a stable app-local slot. The
        // portable copy remains protected by the recovery password in Drive.
        return "drive_appdata_v1"
    }

    private fun localMasterPrefKey(): String = "local_master_${accountStorageKey()}"

    private fun saveLocalMasterKey(master: ByteArray) {
        val wrappingKey = getOrCreateLocalWrappingKey()
        val cipher = Cipher.getInstance(AES_GCM)
        // Android Keystore-backed AES/GCM keys require the provider to generate
        // the IV for encryption. The generated IV is stored with the ciphertext
        // and supplied again only when decrypting.
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)
        val wrapped = cipher.doFinal(master)
        val iv = cipher.iv ?: error("Android Keystore did not generate an IV")
        val json = JSONObject()
            .put("v", 1)
            .put("iv", b64(iv))
            .put("ct", b64(wrapped))
            .put("kid", sha256Hex(master).take(24))
        prefs.edit().putString(localMasterPrefKey(), json.toString()).apply()
    }

    private fun loadLocalMasterKey(): ByteArray? {
        val raw = prefs.getString(localMasterPrefKey(), null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val key = keyStore.getKey(LOCAL_KEY_ALIAS, null) as? SecretKey ?: return null
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, unb64(json.getString("iv"))))
            cipher.doFinal(unb64(json.getString("ct")))
        }.getOrNull()
    }

    private fun getOrCreateLocalWrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(LOCAL_KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                LOCAL_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun createPasswordEnvelope(master: ByteArray, password: String): JSONObject {
        val salt = ByteArray(PBKDF2_SALT_BYTES).also(secureRandom::nextBytes)
        val iv = ByteArray(GCM_IV_BYTES).also(secureRandom::nextBytes)
        val key = derivePasswordKey(password, salt, PBKDF2_ITERATIONS)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(KEY_ENVELOPE_AAD.toByteArray(Charsets.UTF_8))
        val wrapped = cipher.doFinal(master)
        return JSONObject()
            .put("version", 1)
            .put("kdf", "PBKDF2-HMAC-SHA256")
            .put("iterations", PBKDF2_ITERATIONS)
            .put("salt", b64(salt))
            .put("iv", b64(iv))
            .put("wrappedKey", b64(wrapped))
            .put("keyId", sha256Hex(master).take(24))
    }

    private fun decryptPasswordEnvelope(envelope: JSONObject, password: String): ByteArray {
        require(envelope.optInt("version") == 1) { "Unsupported backup key format" }
        val salt = unb64(envelope.getString("salt"))
        val iv = unb64(envelope.getString("iv"))
        val iterations = envelope.optInt("iterations", PBKDF2_ITERATIONS)
        val key = derivePasswordKey(password, salt, iterations)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(KEY_ENVELOPE_AAD.toByteArray(Charsets.UTF_8))
        val master = cipher.doFinal(unb64(envelope.getString("wrappedKey")))
        require(sha256Hex(master).take(24) == envelope.optString("keyId")) { "Recovery password validation failed" }
        return master
    }

    private fun derivePasswordKey(password: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            SecretKeySpec(bytes, "AES")
        } finally { spec.clearPassword() }
    }

    private fun encryptBackup(compressed: ByteArray, master: ByteArray): ByteArray {
        val iv = ByteArray(GCM_IV_BYTES).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(master, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(BACKUP_AAD.toByteArray(Charsets.UTF_8))
        val ciphertext = cipher.doFinal(compressed)
        return JSONObject()
            .put("version", 1)
            .put("cipher", "AES-256-GCM")
            .put("iv", b64(iv))
            .put("ciphertext", b64(ciphertext))
            .toString().toByteArray(Charsets.UTF_8)
    }

    private fun decryptBackup(encrypted: ByteArray, master: ByteArray): ByteArray {
        val json = JSONObject(String(encrypted, Charsets.UTF_8))
        require(json.optInt("version") == 1) { "Unsupported encrypted backup format" }
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(master, "AES"), GCMParameterSpec(GCM_TAG_BITS, unb64(json.getString("iv"))))
        cipher.updateAAD(BACKUP_AAD.toByteArray(Charsets.UTF_8))
        return try { cipher.doFinal(unb64(json.getString("ciphertext"))) } catch (_: javax.crypto.AEADBadTagException) {
            error("Backup authentication failed. Your current data was not changed.")
        }
    }

    private suspend fun findEnvelope(token: String): DriveFileMeta? = listDriveFiles(token).firstOrNull { it.name == KEY_ENVELOPE_FILE }

    private fun backupProperties(createdAt: Long, automatic: Boolean, label: String, pinned: Boolean, safety: Boolean) = mapOf(
        "gymbrosKind" to "backup",
        "schema" to SNAPSHOT_SCHEMA.toString(),
        "createdAt" to createdAt.toString(),
        "automatic" to automatic.toString(),
        "label" to label,
        "pinned" to pinned.toString(),
        "safety" to safety.toString(),
        "encrypted" to "true"
    )

    private fun toRecoveryPoint(file: DriveFileMeta): CloudRecoveryPoint? {
        val p = file.appProperties
        val stamp = p["createdAt"]?.toLongOrNull() ?: file.createdAtMillis
        if (stamp <= 0L) return null
        return CloudRecoveryPoint(
            id = file.id,
            createdAtMillis = stamp,
            schema = p["schema"]?.toIntOrNull() ?: SNAPSHOT_SCHEMA,
            sizeBytes = file.sizeBytes,
            automatic = p["automatic"].toBoolean(),
            label = p["label"].orEmpty(),
            pinned = p["pinned"].toBoolean(),
            safety = p["safety"].toBoolean()
        )
    }

    private suspend fun trimRecoveryPoints(token: String) {
        val points = listDriveFiles(token).filter { it.name.startsWith(BACKUP_PREFIX) }
            .mapNotNull { file -> toRecoveryPoint(file)?.let { it to file } }
            .sortedByDescending { it.first.createdAtMillis }
        val unpinned = points.filterNot { it.first.pinned }
        unpinned.drop(MAX_RECOVERY_POINTS).forEach { deleteDriveFile(token, it.second.id) }
    }

    private suspend fun listDriveFiles(token: String): List<DriveFileMeta> = withContext(Dispatchers.IO) {
        val fields = URLEncoder.encode("nextPageToken,files(id,name,size,createdTime,modifiedTime,appProperties)", "UTF-8")
        val url = "$DRIVE_API/files?spaces=appDataFolder&pageSize=1000&fields=$fields"
        val json = requestJson("GET", url, token)
        val files = json.optJSONArray("files") ?: JSONArray()
        (0 until files.length()).map { i ->
            val f = files.getJSONObject(i)
            val propsJson = f.optJSONObject("appProperties") ?: JSONObject()
            val props = propsJson.keys().asSequence().associateWith { propsJson.optString(it) }
            DriveFileMeta(
                id = f.getString("id"),
                name = f.optString("name"),
                sizeBytes = f.optString("size").toLongOrNull() ?: 0L,
                createdAtMillis = parseDriveTime(f.optString("createdTime")),
                appProperties = props
            )
        }
    }

    private suspend fun uploadDriveFile(token: String, name: String, bytes: ByteArray, appProperties: Map<String, String>): DriveFileMeta = withContext(Dispatchers.IO) {
        val boundary = "gymbros_${UUID.randomUUID()}"
        val metadata = JSONObject()
            .put("name", name)
            .put("parents", JSONArray().put("appDataFolder"))
            .put("appProperties", JSONObject(appProperties))
        val conn = openConnection("POST", "$DRIVE_UPLOAD/files?uploadType=multipart&fields=id,name,size,createdTime,appProperties", token)
        conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        conn.doOutput = true
        conn.outputStream.use { out ->
            fun text(value: String) = out.write(value.toByteArray(StandardCharsets.UTF_8))
            text("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n")
            text(metadata.toString())
            text("\r\n--$boundary\r\nContent-Type: application/octet-stream\r\n\r\n")
            out.write(bytes)
            text("\r\n--$boundary--\r\n")
        }
        val body = readResponse(conn)
        parseDriveFile(JSONObject(body))
    }

    private suspend fun patchDriveProperties(token: String, fileId: String, appProperties: Map<String, String>) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("appProperties", JSONObject(appProperties)).toString().toByteArray(Charsets.UTF_8)
        val conn = openConnection("PATCH", "$DRIVE_API/files/$fileId?fields=id", token)
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.doOutput = true
        conn.outputStream.use { it.write(body) }
        readResponse(conn)
        Unit
    }

    private suspend fun downloadDriveFile(token: String, fileId: String): ByteArray = withContext(Dispatchers.IO) {
        val conn = openConnection("GET", "$DRIVE_API/files/$fileId?alt=media", token)
        val code = conn.responseCode
        if (code !in 200..299) throw DriveHttpException(code, conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty())
        conn.inputStream.use { it.readBytes() }
    }

    private suspend fun deleteDriveFile(token: String, fileId: String) = withContext(Dispatchers.IO) {
        val conn = openConnection("DELETE", "$DRIVE_API/files/$fileId", token)
        val code = conn.responseCode
        if (code !in 200..299 && code != 404) throw DriveHttpException(code, conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty())
        Unit
    }

    private fun requestJson(method: String, url: String, token: String): JSONObject {
        val conn = openConnection(method, url, token)
        return JSONObject(readResponse(conn))
    }

    private fun openConnection(method: String, url: String, token: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 20_000
        readTimeout = 60_000
        setRequestProperty("Authorization", "Bearer $token")
        setRequestProperty("Accept", "application/json")
        useCaches = false
    }

    private fun readResponse(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw DriveHttpException(code, body)
        return body.ifBlank { "{}" }
    }

    private fun parseDriveFile(f: JSONObject): DriveFileMeta {
        val propsJson = f.optJSONObject("appProperties") ?: JSONObject()
        return DriveFileMeta(
            id = f.getString("id"),
            name = f.optString("name"),
            sizeBytes = f.optString("size").toLongOrNull() ?: 0L,
            createdAtMillis = parseDriveTime(f.optString("createdTime")),
            appProperties = propsJson.keys().asSequence().associateWith { propsJson.optString(it) }
        )
    }

    private fun parseDriveTime(value: String): Long = runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(0L)
    private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun unb64(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    private fun createSnapshot(): JSONObject {
        val root = JSONObject().put("schema", SNAPSHOT_SCHEMA).put("createdAt", System.currentTimeMillis())
        val databases = JSONObject()
        DATABASES.forEach { name -> databases.put(name, exportDatabase(context.getDatabasePath(name))) }
        root.put("databases", databases)
        val allPrefs = JSONObject()
        PREF_FILES.forEach { name -> allPrefs.put(name, exportPreferences(name)) }
        root.put("preferences", allPrefs)
        root.put("files", exportPrivateFiles())
        return root
    }

    private fun exportDatabase(file: java.io.File): JSONObject {
        val out = JSONObject()
        if (!file.exists()) return out
        val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        try {
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name", null).use { tables ->
                while (tables.moveToNext()) {
                    val table = tables.getString(0)
                    val rows = JSONArray()
                    db.rawQuery("SELECT * FROM `${table.replace("`", "``")}`", null).use { c ->
                        while (c.moveToNext()) rows.put(cursorRow(c))
                    }
                    out.put(table, rows)
                }
            }
        } finally { db.close() }
        return out
    }

    private fun cursorRow(c: Cursor): JSONObject = JSONObject().also { row ->
        for (i in 0 until c.columnCount) {
            when (c.getType(i)) {
                Cursor.FIELD_TYPE_NULL -> row.put(c.getColumnName(i), JSONObject.NULL)
                Cursor.FIELD_TYPE_INTEGER -> row.put(c.getColumnName(i), c.getLong(i))
                Cursor.FIELD_TYPE_FLOAT -> row.put(c.getColumnName(i), c.getDouble(i))
                Cursor.FIELD_TYPE_BLOB -> row.put(c.getColumnName(i), "__blob__" + Base64.encodeToString(c.getBlob(i), Base64.NO_WRAP))
                else -> row.put(c.getColumnName(i), c.getString(i))
            }
        }
    }

    private fun exportPreferences(name: String): JSONObject = JSONObject().also { json ->
        context.getSharedPreferences(name, Context.MODE_PRIVATE).all.forEach { (key, value) ->
            val entry = JSONObject()
            when (value) {
                is Boolean -> entry.put("type", "boolean").put("value", value)
                is Int -> entry.put("type", "int").put("value", value)
                is Long -> entry.put("type", "long").put("value", value)
                is Float -> entry.put("type", "float").put("value", value.toDouble())
                is String -> entry.put("type", "string").put("value", value)
                is Set<*> -> entry.put("type", "string_set").put(
                    "value",
                    JSONArray(value.filterIsInstance<String>())
                )
                null -> entry.put("type", "null").put("value", JSONObject.NULL)
                else -> entry.put("type", "string").put("value", value.toString())
            }
            json.put(key, entry)
        }
    }

    private fun exportPrivateFiles(): JSONArray = JSONArray().also { array ->
        val root = context.filesDir
        root.walkTopDown().filter { it.isFile }.forEach { file ->
            val relative = file.relativeTo(root).invariantSeparatorsPath
            array.put(JSONObject().put("path", relative).put("data", Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)))
        }
    }

    private fun restoreSnapshot(root: JSONObject) {
        // Onboarding can restore before the home, water or nutrition screens ever opened.
        com.rishabh.gymbros.features.water.data.WaterDatabase(context).use { it.writableDatabase }
        com.rishabh.gymbros.features.workout.data.WorkoutDatabase(context).use { it.writableDatabase }
        com.rishabh.gymbros.features.nutrition.data.NutritionDatabase(context).use { it.writableDatabase }
        val databases = root.getJSONObject("databases")
        DATABASES.forEach { name -> if (databases.has(name)) restoreDatabase(context.getDatabasePath(name), databases.getJSONObject(name)) }
        val preferences = root.getJSONObject("preferences")
        PREF_FILES.forEach { name -> if (preferences.has(name)) restorePreferences(name, preferences.getJSONObject(name)) }
        restorePrivateFiles(root.optJSONArray("files") ?: JSONArray())
    }

    private fun restoreDatabase(file: java.io.File, tables: JSONObject) {
        check(file.exists()) { "Could not prepare the local database for restore." }
        val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE)
        db.beginTransaction()
        try {
            val names = tables.keys().asSequence().toList()
            db.execSQL("PRAGMA foreign_keys=OFF")
            names.forEach { table -> db.delete(table, null, null) }
            names.forEach { table ->
                val rows = tables.getJSONArray(table)
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val values = ContentValues()
                    row.keys().forEach { key ->
                        val value = row.get(key)
                        when (value) {
                            JSONObject.NULL -> values.putNull(key)
                            is Int -> values.put(key, value)
                            is Long -> values.put(key, value)
                            is Double -> values.put(key, value)
                            is String -> if (value.startsWith("__blob__")) values.put(key, Base64.decode(value.removePrefix("__blob__"), Base64.NO_WRAP)) else values.put(key, value)
                            is Boolean -> values.put(key, if (value) 1 else 0)
                            else -> values.put(key, value.toString())
                        }
                    }
                    db.insertOrThrow(table, null, values)
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction(); db.close() }
    }

    private fun restorePreferences(name: String, json: JSONObject) {
        val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
        json.keys().forEach { key ->
            val value = json.get(key)

            // Schema 2+ stores the original SharedPreferences type explicitly.
            if (value is JSONObject && value.has("type")) {
                when (value.optString("type")) {
                    "boolean" -> editor.putBoolean(key, value.optBoolean("value"))
                    "int" -> editor.putInt(key, value.optInt("value"))
                    "long" -> editor.putLong(key, value.optLong("value"))
                    "float" -> editor.putFloat(key, value.optDouble("value").toFloat())
                    "string" -> editor.putString(key, value.optString("value", ""))
                    "string_set" -> {
                        val array = value.optJSONArray("value") ?: JSONArray()
                        editor.putStringSet(
                            key,
                            (0 until array.length()).map { array.getString(it) }.toSet()
                        )
                    }
                    "null" -> editor.remove(key)
                }
                return@forEach
            }

            // Compatibility with schema 1 backups. JSON cannot distinguish an integral Float
            // (for example 67.0f) from an Int, so known Float preference keys are repaired here.
            if (isLegacyFloatPreference(name, key) && value is Number) {
                editor.putFloat(key, value.toFloat())
                return@forEach
            }

            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Double -> editor.putFloat(key, value.toFloat())
                is String -> editor.putString(key, value)
                is JSONArray -> editor.putStringSet(
                    key,
                    (0 until value.length()).map { value.getString(it) }.toSet()
                )
            }
        }
        editor.commit()
    }

    private fun isLegacyFloatPreference(prefName: String, key: String): Boolean = when (prefName) {
        "gymbros_settings" -> key == "weight_kg"
        "gymbros_train_preferences" -> key == "daily_cardio_goal_km"
        else -> false
    }

    private fun restorePrivateFiles(files: JSONArray) {
        val root = context.filesDir.canonicalFile
        for (i in 0 until files.length()) {
            val item = files.getJSONObject(i)
            val target = java.io.File(root, item.getString("path")).canonicalFile
            require(target.path.startsWith(root.path + java.io.File.separator)) { "Invalid backup file path" }
            target.parentFile?.mkdirs()
            target.writeBytes(Base64.decode(item.getString("data"), Base64.NO_WRAP))
        }
    }

    private fun validateSnapshot(root: JSONObject) {
        val schema = root.optInt("schema", 0)
        require(schema in 1..SNAPSHOT_SCHEMA) { "Unsupported backup format (schema $schema). Your current data was not changed." }
        require(root.optJSONObject("databases") != null) { "Backup validation failed: databases are missing. Your current data was not changed." }
        require(root.optJSONObject("preferences") != null) { "Backup validation failed: preferences are missing. Your current data was not changed." }
        val databases = root.getJSONObject("databases")
        require(DATABASES.all { databases.has(it) }) { "Backup validation failed: required database data is missing. Your current data was not changed." }
        val preferences = root.getJSONObject("preferences")
        require(PREF_FILES.all { preferences.has(it) }) { "Backup validation failed: required settings are missing. Your current data was not changed." }
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun gzip(bytes: ByteArray): ByteArray = ByteArrayOutputStream().use { out -> GZIPOutputStream(out).use { it.write(bytes) }; out.toByteArray() }
    private fun gunzip(bytes: ByteArray): ByteArray = GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }


    private fun readState(): CloudBackupState {
        val disconnected = prefs.getBoolean(KEY_MANUALLY_DISCONNECTED, false)
        val connected = !disconnected && prefs.getBoolean(KEY_DRIVE_CONNECTED, false)
        val localKeyReady = runCatching { loadLocalMasterKey() != null }.getOrDefault(false)

        return CloudBackupState(
            backupCheckPending = prefs.getBoolean(KEY_RESTORE_CHECK_PENDING, false),
            signedIn = connected,
            email = if (disconnected) "" else prefs.getString(KEY_ACCOUNT_EMAIL, "").orEmpty(),
            displayName = if (disconnected) "" else prefs.getString(KEY_ACCOUNT_NAME, "").orEmpty(),
            autoBackup = prefs.getBoolean(KEY_AUTO, true),
            lastBackupMillis = prefs.getLong(KEY_LAST_BACKUP, 0L),
            restoreSuggestionDismissed = prefs.getBoolean(KEY_RESTORE_DISMISSED, false),
            // Immediately expose setup after a successful Drive connection. A cloud
            // metadata refresh will refine this to "unlock" if an envelope already exists.
            securitySetupRequired = connected && !localKeyReady,
            securityUnlockRequired = false,
            secureBackupReady = localKeyReady
        )
    }

    companion object {
        const val DRIVE_AUTH_REQUEST_CODE = 9052
        private const val PREFS = "gymbros_cloud_backup"
        private const val KEY_AUTO = "auto_backup"
        private const val KEY_LAST_BACKUP = "last_backup"
        private const val KEY_RESTORE_DISMISSED = "restore_suggestion_dismissed"
        private const val KEY_RESTORE_CHECK_PENDING = "restore_check_pending"
        private const val KEY_RESTORE_DECISION_PENDING = "restore_decision_pending"
        private const val KEY_MANUALLY_DISCONNECTED = "google_manually_disconnected"
        private const val KEY_DRIVE_CONNECTED = "drive_connected"
        private const val KEY_ACCOUNT_EMAIL = "drive_account_email"
        private const val KEY_ACCOUNT_NAME = "drive_account_name"
        private const val KEY_ENVELOPE_FILE = "gymbros_key_envelope_v1.json"
        private const val BACKUP_PREFIX = "gymbros_backup_"
        private const val BACKUP_SUFFIX = ".gbak"
        private const val MAX_RECOVERY_POINTS = 5
        private const val SNAPSHOT_SCHEMA = 2
        private const val DRIVE_API = "https://www.googleapis.com/drive/v3"
        private const val DRIVE_UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private const val LOCAL_KEY_ALIAS = "gymbros_backup_local_wrap_v1"
        private const val AES_GCM = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val PBKDF2_SALT_BYTES = 16
        private const val PBKDF2_ITERATIONS = 600_000
        private const val MIN_PASSWORD_LENGTH = 12
        private const val BACKUP_AAD = "GymBros|EncryptedBackup|v1"
        private const val KEY_ENVELOPE_AAD = "GymBros|RecoveryKeyEnvelope|v1"
        private const val TAG_AUTH = "GymBrosDriveAuth"
        private val DATABASES = listOf("gymbros_water.db", "gymbros_workouts.db", "gymbros_nutrition.db")
        private val PREF_FILES = listOf("gymbros_settings", "gymbros_workout_state", "gymbros_train_preferences", "gymbros_progress", "gymbros_nutrition")
        @Volatile private var instance: CloudBackupManager? = null
        fun getInstance(context: Context): CloudBackupManager = instance ?: synchronized(this) {
            instance ?: CloudBackupManager(context.applicationContext).also { instance = it }
        }
    }
}

private data class DriveFileMeta(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val createdAtMillis: Long,
    val appProperties: Map<String, String>
)

private fun driveErrorDetail(body: String): String {
    if (body.isBlank()) return "No error details returned by Google"
    return runCatching {
        val root = JSONObject(body)
        val error = root.optJSONObject("error")
        val message = error?.optString("message").orEmpty()
        val errors = error?.optJSONArray("errors")
        val reason = if (errors != null && errors.length() > 0) {
            errors.optJSONObject(0)?.optString("reason").orEmpty()
        } else ""
        when {
            reason.isNotBlank() && message.isNotBlank() -> "$reason — $message"
            message.isNotBlank() -> message
            reason.isNotBlank() -> reason
            else -> body.take(500)
        }
    }.getOrElse { body.take(500) }
}

private class DriveHttpException(val statusCode: Int, body: String) : Exception(
    when (statusCode) {
        401 -> "Google Drive 401: ${driveErrorDetail(body)}"
        403 -> "Google Drive 403: ${driveErrorDetail(body)}"
        404 -> "Google Drive 404: ${driveErrorDetail(body)}"
        else -> "Google Drive request failed ($statusCode): ${driveErrorDetail(body)}"
    }
)

data class CloudRecoveryPoint(
    val id: String,
    val createdAtMillis: Long,
    val schema: Int,
    val sizeBytes: Long,
    val automatic: Boolean,
    val label: String = "",
    val pinned: Boolean = false,
    val safety: Boolean = false
)

data class CloudBackupState(
    val backupCheckPending: Boolean = false,
    val signedIn: Boolean = false,
    val email: String = "",
    val displayName: String = "",
    val autoBackup: Boolean = true,
    val busy: Boolean = false,
    val lastBackupMillis: Long = 0L,
    val cloudBackupMillis: Long = 0L,
    val cloudBackupSchema: Int = 0,
    val cloudBackupAvailable: Boolean = false,
    val recoveryPoints: List<CloudRecoveryPoint> = emptyList(),
    val restoreSuggestionDismissed: Boolean = false,
    val message: String? = null,
    val securitySetupRequired: Boolean = false,
    val securityUnlockRequired: Boolean = false,
    val secureBackupReady: Boolean = false
) {
    val shouldSuggestRestore: Boolean
        get() = signedIn && cloudBackupAvailable && !restoreSuggestionDismissed && !backupCheckPending
}
