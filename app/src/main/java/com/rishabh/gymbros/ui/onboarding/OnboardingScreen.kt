package com.rishabh.gymbros.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.rishabh.gymbros.features.cloud.CloudBackupManager
import com.rishabh.gymbros.features.onboarding.OnboardingStep
import com.rishabh.gymbros.features.onboarding.OnboardingStore
import com.rishabh.gymbros.features.onboarding.profileInputError
import com.rishabh.gymbros.features.onboarding.acceptsNumericInput
import com.rishabh.gymbros.features.onboarding.heightFromFeet
import com.rishabh.gymbros.features.settings.model.HeightUnit
import com.rishabh.gymbros.ui.theme.Ui2Colors
import com.rishabh.gymbros.ui.theme.Ui2Theme
import com.rishabh.gymbros.features.settings.data.SettingsRepository
import com.rishabh.gymbros.features.spotify.SpotifyManager
import com.rishabh.gymbros.ui.profile.loadProfileBitmap
import com.rishabh.gymbros.ui.profile.persistProfileImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(store: OnboardingStore) {
    val step by store.step.collectAsState()
    BackHandler(enabled = step == OnboardingStep.CONNECTIONS) { store.goTo(OnboardingStep.PROFILE) }
    Ui2Theme {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("GYM BROS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text("Step ${step.ordinal + 1} of 3", style = MaterialTheme.typography.labelMedium)
            when (step) {
                OnboardingStep.PRIVACY -> {
                    Text("Welcome to your next chapter", style = MaterialTheme.typography.headlineLarge)
                    Text("A little setup. Then it’s time to move.")
                    PrivacyCard { store.goTo(OnboardingStep.PROFILE) }
                }
                OnboardingStep.PROFILE -> ProfileSetup { store.goTo(OnboardingStep.CONNECTIONS) }
                OnboardingStep.CONNECTIONS -> ConnectionSetup { store.goTo(OnboardingStep.COMPLETE) }
                OnboardingStep.COMPLETE -> Unit
            }
        }
    }
    }
}

@Composable
private fun PrivacyCard(onAccept: () -> Unit) {
    var accepted by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Ui2Colors.Card, contentColor = androidx.compose.ui.graphics.Color.White)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Your data, on your phone", style = MaterialTheme.typography.titleLarge)
            Text("Your profile and fitness data stay on your phone by default. You can use Gym Bros without an account.")
            Text("Google backup is optional. If you connect it, a copy of your app data is stored in the cloud under your Google account. Spotify is also optional and connects your workout music.")
            Row(
                Modifier.fillMaxWidth().toggleable(accepted, role = Role.Checkbox, onValueChange = { accepted = it }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = accepted, onCheckedChange = null,
                    colors = CheckboxDefaults.colors(uncheckedColor = androidx.compose.ui.graphics.Color.White))
                Spacer(Modifier.width(12.dp))
                Text("I understand and agree to continue.")
            }
            Button(onClick = onAccept, enabled = accepted, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
        }
    }
}
@Composable
private fun ProfileSetup(onContinue: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { SettingsRepository.getInstance(context) }
    val settings = repository.currentSettings
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(settings.userName) }
    var age by rememberSaveable { mutableStateOf(settings.ageYears?.toString().orEmpty()) }
    val initialInches = settings.heightCm?.let { (it / 2.54).roundToInt() }
    var heightFeet by rememberSaveable { mutableStateOf(initialInches?.let { (it / 12).toString() }.orEmpty()) }
    var heightInches by rememberSaveable { mutableStateOf(initialInches?.let { (it % 12).toString() }.orEmpty()) }
    var weight by rememberSaveable { mutableStateOf(settings.weightKg?.toString().orEmpty()) }
    var imageUri by rememberSaveable { mutableStateOf(settings.profileImageUri) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var photoBusy by remember { mutableStateOf(false) }
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, imageUri) {
        value = withContext(Dispatchers.IO) { loadProfileBitmap(context, imageUri) }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            photoBusy = true
            scope.launch {
                val stored = withContext(Dispatchers.IO) { persistProfileImage(context, uri) }
                if (stored != null) {
                    imageUri = stored
                    repository.setProfileImageUri(stored)
                    error = null
                } else error = "Couldn’t save that photo. Please try another image."
                photoBusy = false
            }
        }
    }
    Text("Make it yours", style = MaterialTheme.typography.headlineLarge)
    Text("Name and age are required. Add your height, weight and profile photo now or later in You.")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(Modifier.size(88.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            val photo = bitmap
            if (photo != null) Image(photo, "Profile photo", Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
            else Icon(Icons.Rounded.Person, "Profile photo placeholder", Modifier.padding(20.dp))
        }
        TextButton(enabled = !photoBusy, onClick = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) { Text(if (photoBusy) "Saving photo…" else if (imageUri.isBlank()) "Add photo · optional" else "Change photo") }
    }
    SetupField("Name *", name, { name = it.take(30); error = null }, KeyboardType.Text)
    SetupField("Age *", age, { age = it; error = null }, KeyboardType.Number)
    Text("Height · optional", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SetupField("Feet", heightFeet, { heightFeet = it; error = null }, KeyboardType.Number, Modifier.weight(1f))
        SetupField("Inches", heightInches, { heightInches = it; error = null }, KeyboardType.Number, Modifier.weight(1f))
    }
    SetupField("Weight in kg · optional", weight, { weight = it; error = null }, KeyboardType.Decimal)
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = name.isNotBlank() && age.isNotBlank() && !photoBusy,
        onClick = {
            val heightCm = heightFromFeet(heightFeet, heightInches)
            error = profileInputError(name.trim(), age.trim(), "", weight.trim())
                ?: if ((heightFeet.isNotBlank() || heightInches.isNotBlank()) && heightCm == null)
                    "Enter a height from 2 ft 8 in to 8 ft 2 in, using 0–11 inches, or leave both fields blank."
                else null
            if (error == null) {
                repository.setProfile(name, age.trim().toInt(), weight.trim().toFloatOrNull(), heightCm)
                repository.setHeightUnit(HeightUnit.FT)
                onContinue()
            }
        }
    ) { Text("Continue") }
}

@Composable
private fun SetupField(label: String, value: String, onChange: (String) -> Unit, keyboard: KeyboardType, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = { input ->
            if (keyboard == KeyboardType.Text || acceptsNumericInput(input, decimal = keyboard == KeyboardType.Decimal)) {
                onChange(input)
            }
        }, label = { Text(label) },
        modifier = modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard)
    )
}

@Composable
private fun ConnectionSetup(onFinish: () -> Unit) {
    val context = LocalContext.current
    val cloud = remember(context) { CloudBackupManager.getInstance(context) }
    val spotify = remember(context) { SpotifyManager.getInstance(context) }
    val cloudState by cloud.state.collectAsState()
    val spotifyState by spotify.state.collectAsState()
    val scope = rememberCoroutineScope()
    BackHandler(enabled = cloudState.busy) { /* Wait for sign-in or its cancellation result. */ }
    LaunchedEffect(cloudState.signedIn) {
        if (cloudState.signedIn && !cloudState.busy) cloud.refreshCloudMetadata()
    }
    var restorePassword by rememberSaveable { mutableStateOf("") }
    var restorePasswordVisible by rememberSaveable { mutableStateOf(false) }
    if (cloudState.shouldSuggestRestore && !cloudState.backupCheckPending) {
        val needsUnlock = cloudState.securityUnlockRequired
        AlertDialog(
            onDismissRequest = { if (!cloudState.busy) cloud.dismissRestoreSuggestion() },
            containerColor = Ui2Colors.Card,
            titleContentColor = androidx.compose.ui.graphics.Color.White,
            textContentColor = androidx.compose.ui.graphics.Color.White,
            title = { Text(if (needsUnlock) "Unlock & restore your data" else "Restore your previous data?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("We found a backup for ${cloudState.email}. Restore it to bring back your profile, workouts and saved data. This replaces the data currently on this phone.")
                    if (needsUnlock) {
                        Text("Enter the recovery password you created for this encrypted backup.")
                        OutlinedTextField(
                            value = restorePassword,
                            onValueChange = { restorePassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Recovery password") },
                            visualTransformation = if (restorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { restorePasswordVisible = !restorePasswordVisible }) {
                                    Icon(
                                        imageVector = if (restorePasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (restorePasswordVisible) "Hide recovery password" else "Show recovery password"
                                    )
                                }
                            }
                        )
                    }
                    cloudState.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                Button(
                    enabled = !cloudState.busy && (!needsUnlock || restorePassword.isNotBlank()),
                    onClick = {
                        scope.launch {
                            if (needsUnlock) {
                                val unlocked = cloud.unlockWithRecoveryPassword(restorePassword)
                                if (unlocked.isSuccess) cloud.restoreLatest()
                            } else {
                                cloud.restoreLatest()
                            }
                        }
                    }
                ) { Text(if (cloudState.busy) "Restoring…" else if (needsUnlock) "Unlock & restore" else "Restore my data") }
            },
            dismissButton = { TextButton(enabled = !cloudState.busy, onClick = cloud::dismissRestoreSuggestion) { Text("Not now") } }
        )
    }
    Text("Connect your extras", style = MaterialTheme.typography.headlineLarge)
    Text("Both are optional. You can connect them later from the app.")
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Ui2Colors.Card)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Google backup", style = MaterialTheme.typography.titleLarge)
            Text("Connect Google Drive to enable client-side encrypted backups of your GymBros data. Manage backups and restore previous data in You.")
            if (cloudState.signedIn) Text("Google connected", color = Ui2Colors.Primary, style = MaterialTheme.typography.titleMedium)
            else Button(enabled = !cloudState.busy, onClick = {
                scope.launch { cloud.signIn(context) }
            }) { Text(if (cloudState.signedIn) "Google connected" else if (cloudState.busy) "Connecting…" else "Connect Google") }
            if (cloudState.signedIn) Text(cloudState.email)
            if (cloudState.signedIn && cloudState.backupCheckPending && !cloudState.busy) {
                Text("Your account is connected, but we couldn’t check for a backup. Automatic backup is paused until this check succeeds.")
                TextButton(onClick = { scope.launch { cloud.refreshCloudMetadata(forceRestoreSuggestion = true) } }) { Text("Check for previous data") }
            }
            cloudState.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Ui2Colors.Card)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Spotify music", style = MaterialTheme.typography.titleLarge)
            Text("Connect Spotify to control your workout music. Spotify needs to be installed on your phone.")
            if (spotifyState.connected) Text("Spotify connected", color = Ui2Colors.Primary, style = MaterialTheme.typography.titleMedium)
            else Button(enabled = !spotifyState.connecting, onClick = { spotify.connect(context) }) {
                Text(if (spotifyState.connected) "Spotify connected" else if (spotifyState.connecting) "Connecting…" else "Connect Spotify")
            }
            spotifyState.errorMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
    Button(modifier = Modifier.fillMaxWidth(), onClick = onFinish, enabled = !cloudState.busy) {
        Text(if (cloudState.signedIn || spotifyState.connected) "Get started" else "Skip for now")
    }
}
