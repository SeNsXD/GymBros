package com.rishabh.gymbros.features.spotify

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Repeat
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

class SpotifyManager private constructor(context: Context) {

    data class UiState(
        val connected: Boolean = false,
        val connecting: Boolean = false,
        val trackName: String = "Nothing playing",
        val artistName: String = "Spotify",
        val trackUri: String? = null,
        val isPaused: Boolean = true,
        val artwork: Bitmap? = null,
        val playbackPositionMs: Long = 0L,
        val durationMs: Long = 0L,
        val isShuffling: Boolean = false,
        val repeatMode: Int = Repeat.OFF,
        val errorMessage: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var appRemote: SpotifyAppRemote? = null
    private var playerStateSubscription: Subscription<PlayerState>? = null
    private var connectionRequested = false
    private var pendingTrackUri: String? = null
    private var lastRequestedTrackUri: String? = null
    private var playbackRequestGeneration = 0
    private var cancelledPlaybackGeneration = -1
    private val continuation = SpotifyTrackContinuation()
    val isSearchTrackActive: Boolean get() = continuation.active
    private var authorizationInProgress = false
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private var authorizedThisSession = preferences.getBoolean(KEY_AUTHORIZED, false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var connectionAttempt = 0
    private val continuationPoll = object : Runnable {
        override fun run() {
            val remote = appRemote ?: return
            if (!continuation.active || !remote.isConnected) return
            val generation = playbackRequestGeneration
            remote.playerApi.playerState.setResultCallback { playerState ->
                if (generation == playbackRequestGeneration) checkTrackCompletion(playerState)
            }
            mainHandler.postDelayed(this, 1_000L)
        }
    }

    private fun monitorTrackCompletion() {
        mainHandler.removeCallbacks(continuationPoll)
        if (continuation.active) mainHandler.postDelayed(continuationPoll, 1_000L)
    }

    private fun checkTrackCompletion(playerState: PlayerState) {
        continuation.observe(playerState.track?.uri, playerState.playbackPosition,
            playerState.track?.duration ?: 0L, playerState.isPaused,
            playerState.playbackOptions.repeatMode == Repeat.ONE, android.os.SystemClock.elapsedRealtime())
            ?.let(::play)
    }

    fun setContinuationPlaylist(uri: String?) { continuation.setPlaylist(uri) }

    fun playSearchTrack(context: Context, uri: String, playlistUri: String?) {
        // playFromUser clears any previous selection; arm only the new chosen track.
        playFromUser(context, uri)
        continuation.arm(uri, playlistUri)
        monitorTrackCompletion()
    }

    /** Starts explicit Spotify authorization. After approval MainActivity sends the
     * result back through handleAuthorizationResult(), which then connects App Remote.
     */
    fun connect(context: Context) {
        if (appRemote?.isConnected == true || connectionRequested || authorizationInProgress) return

        val activity = context.findActivity()
        if (activity == null) {
            _state.value = _state.value.copy(
                connected = false,
                connecting = false,
                errorMessage = "Spotify authorization needs an active GymBros screen."
            )
            return
        }

        // Explicitly grant App Remote control through Spotify's Authorization Library.
        // The App Remote connection is started only after Spotify returns CODE from
        // this user-consent flow. This avoids treating a connection attempt as consent.
        authorizationInProgress = true
        connectionRequested = false
        _state.value = _state.value.copy(connecting = true, errorMessage = null)

        val request = AuthorizationRequest.Builder(
            CLIENT_ID,
            AuthorizationResponse.Type.CODE,
            REDIRECT_URI
        )
            .setScopes(arrayOf(APP_REMOTE_SCOPE))
            .setShowDialog(true)
            .build()

        Log.d(TAG, "Starting explicit Spotify app-remote-control authorization…")
        AuthorizationClient.openLoginActivity(activity, AUTH_REQUEST_CODE, request)
    }

    /**
     * Tries App Remote silently whenever GymBros enters the foreground.
     *
     * Only a previously successful authorization permits automatic reconnection.
     * The Connect button always allows explicit authorization again.
     */
    fun reconnectIfAuthorized(context: Context) {
        if (!authorizedThisSession || appRemote?.isConnected == true || connectionRequested) return
        Log.d(TAG, "Attempting silent Spotify reconnect…")
        connectRemote(context, showAuthView = false)
    }

    fun handleAuthorizationResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        context: Context
    ): Boolean {
        if (requestCode != AUTH_REQUEST_CODE) return false
        authorizationInProgress = false

        val response = AuthorizationClient.getResponse(resultCode, data)
        Log.d(TAG, "Spotify authorization result: ${response.type}")

        when (response.type) {
            AuthorizationResponse.Type.CODE -> {
                // The important part for App Remote is that Spotify has now granted
                // app-remote-control. App Remote itself does not need this auth code.
                authorizedThisSession = true
                preferences.edit().putBoolean(KEY_AUTHORIZED, true).apply()
                connectionRequested = false
                _state.value = _state.value.copy(connecting = true, errorMessage = null)
                connectRemote(context, showAuthView = false)
            }

            AuthorizationResponse.Type.ERROR -> {
                Log.e(TAG, "Spotify Auth SDK returned an error before App Remote: ${response.error}")
                authorizedThisSession = false
                preferences.edit().putBoolean(KEY_AUTHORIZED, false).apply()
                pendingTrackUri = null
                connectionRequested = false
                _state.value = _state.value.copy(
                    connected = false,
                    connecting = false,
                    errorMessage = response.error?.takeIf { it.isNotBlank() }
                        ?.let { "Spotify authorization failed: $it" }
                        ?: "Spotify authorization failed."
                )
            }

            else -> {
                pendingTrackUri = null
                connectionRequested = false
                _state.value = _state.value.copy(
                    connected = false,
                    connecting = false,
                    errorMessage = "Spotify authorization was cancelled."
                )
            }
        }
        return true
    }

    private fun connectRemote(context: Context, showAuthView: Boolean) {
        if (appRemote?.isConnected == true || connectionRequested) return

        connectionRequested = true
        val attempt = ++connectionAttempt
        _state.value = _state.value.copy(connecting = true, errorMessage = null)
        Log.d(TAG, "Connecting Spotify App Remote (showAuthView=$showAuthView)…")

        val params = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(showAuthView)
            .build()

        SpotifyAppRemote.connect(context, params, object : Connector.ConnectionListener {
            override fun onConnected(remote: SpotifyAppRemote) {
                if (attempt != connectionAttempt) {
                    SpotifyAppRemote.disconnect(remote)
                    return
                }

                Log.d(TAG, "Spotify App Remote connected")
                connectionRequested = false
                appRemote = remote
                // A successful silent App Remote connection proves the Spotify-side grant
                // exists, including for users who authorized before we added local persistence.
                authorizedThisSession = true
                preferences.edit().putBoolean(KEY_AUTHORIZED, true).apply()
                _state.value = _state.value.copy(
                    connected = true,
                    connecting = false,
                    errorMessage = null
                )
                subscribeToPlayerState()
                pendingTrackUri?.let { uri ->
                    pendingTrackUri = null
                    playInternal(uri)
                }
                monitorTrackCompletion()
            }

            override fun onFailure(throwable: Throwable) {
                if (attempt != connectionAttempt) return
                pendingTrackUri = null

                Log.e(TAG, "Spotify App Remote failed: ${throwable.javaClass.name}: ${throwable.message}", throwable)
                if (throwable is com.spotify.android.appremote.api.error.UserNotAuthorizedException) {
                    authorizedThisSession = false
                    preferences.edit().putBoolean(KEY_AUTHORIZED, false).apply()
                }
                connectionRequested = false
                appRemote = null
                _state.value = _state.value.copy(
                    connected = false,
                    connecting = false,
                    errorMessage = friendlyError(throwable)
                )
            }
        })
    }

    /**
     * Disconnect App Remote when GymBros moves to the background without revoking
     * the user's saved Spotify authorization. onStart/reconnectIfAuthorized can
     * silently restore the connection when the app returns.
     */
    fun disconnectForBackground() {
        if (authorizationInProgress) return
        mainHandler.removeCallbacks(continuationPoll)
        continuation.resetObservation()
        pendingTrackUri = null
        connectionAttempt++
        connectionRequested = false
        playerStateSubscription?.cancel()
        playerStateSubscription = null
        appRemote?.let { SpotifyAppRemote.disconnect(it) }
        appRemote = null
        _state.value = _state.value.copy(
            connected = false,
            connecting = false,
            artwork = null,
            errorMessage = null
        )
    }

    fun disconnect() {
        mainHandler.removeCallbacks(continuationPoll)
        continuation.clear()
        pendingTrackUri = null
        authorizationInProgress = false
        connectionAttempt++
        connectionRequested = false
        authorizedThisSession = false
        preferences.edit().putBoolean(KEY_AUTHORIZED, false).apply()
        playerStateSubscription?.cancel()
        playerStateSubscription = null
        appRemote?.let { SpotifyAppRemote.disconnect(it) }
        appRemote = null
        _state.value = UiState()
    }

    fun togglePlayPause() = withRemote { remote ->
        if (_state.value.isPaused) remote.playerApi.resume() else remote.playerApi.pause()
    }

    fun playFromUser(context: Context, uri: String) {
        continuation.clear()
        mainHandler.removeCallbacks(continuationPoll)
        if (appRemote?.isConnected == true) play(uri)
        else {
            pendingTrackUri = uri
            connect(context)
        }
    }

    fun stopPreview(uri: String) {
        if (pendingTrackUri == uri) pendingTrackUri = null
        if (lastRequestedTrackUri == uri) cancelledPlaybackGeneration = playbackRequestGeneration
        if (_state.value.trackUri == uri && !_state.value.isPaused) appRemote?.playerApi?.pause()
    }

    fun play(uri: String) {
        continuation.clear()
        mainHandler.removeCallbacks(continuationPoll)
        playInternal(uri)
    }

    private fun playInternal(uri: String) = withRemote { remote ->
        val generation = ++playbackRequestGeneration
        lastRequestedTrackUri = uri
        remote.playerApi.play(uri).setResultCallback {
            // Closing a preview may happen before Spotify acknowledges playback.
            if (generation == playbackRequestGeneration && generation == cancelledPlaybackGeneration) {
                remote.playerApi.pause()
            }
        }.setErrorCallback { throwable ->
            if (generation == playbackRequestGeneration) {
                continuation.clear()
                mainHandler.removeCallbacks(continuationPoll)
                Log.e(TAG, "Spotify track playback failed", throwable)
                _state.value = _state.value.copy(errorMessage = "Couldn't play this track. Please try again.")
            }
        }
    }

    fun addToQueue(uri: String) = withRemote { remote ->
        remote.playerApi.queue(uri)
            .setResultCallback {
                _state.value = _state.value.copy(errorMessage = null)
            }
            .setErrorCallback { throwable ->
                Log.e(TAG, "Spotify queue failed", throwable)
                _state.value = _state.value.copy(errorMessage = "Couldn't add this song to the queue.")
            }
    }

    fun resume() = withRemote { it.playerApi.resume() }
    fun previous() = withRemote { continuation.clear(); it.playerApi.skipPrevious() }
    fun next() = withRemote { continuation.clear(); it.playerApi.skipNext() }

    fun seek(positionMs: Long) = withRemote { remote ->
        continuation.resetObservation()
        remote.playerApi.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun toggleShuffle() = withRemote { remote ->
        remote.playerApi.setShuffle(!_state.value.isShuffling)
    }

    fun toggleRepeat() = withRemote { remote ->
        remote.playerApi.toggleRepeat()
    }

    private fun withRemote(action: (SpotifyAppRemote) -> Unit) {
        val remote = appRemote
        if (remote?.isConnected == true) {
            action(remote)
        } else {
            _state.value = _state.value.copy(
                connected = false,
                connecting = false,
                errorMessage = "Spotify is disconnected. Tap Connect to reconnect."
            )
        }
    }

    private fun subscribeToPlayerState() {
        val remote = appRemote ?: return
        playerStateSubscription?.cancel()

        val subscription = remote.playerApi.subscribeToPlayerState()
        playerStateSubscription = subscription

        subscription
            .setEventCallback { playerState ->
                val track = playerState.track
                _state.value = _state.value.copy(
                    connected = true,
                    trackName = track?.name ?: "Nothing playing",
                    artistName = track?.artist?.name ?: "Spotify",
                    trackUri = track?.uri,
                    artwork = if (_state.value.trackUri == track?.uri) _state.value.artwork else null,
                    isPaused = playerState.isPaused,
                    playbackPositionMs = playerState.playbackPosition,
                    durationMs = track?.duration ?: 0L,
                    isShuffling = playerState.playbackOptions.isShuffling,
                    repeatMode = playerState.playbackOptions.repeatMode,
                    errorMessage = null
                )
                checkTrackCompletion(playerState)

                val imageUri = track?.imageUri
                if (imageUri == null) {
                    _state.value = _state.value.copy(artwork = null)
                } else {
                    remote.imagesApi.getImage(imageUri)
                        .setResultCallback { bitmap ->
                            if (_state.value.trackUri == track.uri) {
                                _state.value = _state.value.copy(artwork = bitmap)
                            }
                        }
                        .setErrorCallback { throwable ->
                            Log.w(TAG, "Spotify artwork unavailable", throwable)
                        }
                }
            }
            .setErrorCallback { throwable ->
                Log.e(TAG, "Spotify player-state subscription failed", throwable)
                _state.value = _state.value.copy(
                    errorMessage = throwable.message ?: "Spotify player state unavailable"
                )
            }
    }

    private fun friendlyError(throwable: Throwable): String {
        val type = throwable.javaClass.simpleName
        val detail = throwable.message?.takeIf { it.isNotBlank() }
        return when {
            type.contains("CouldNotFindSpotifyApp", ignoreCase = true) ->
                "Spotify app not found. Install/update Spotify and try again."
            type.contains("NotLoggedIn", ignoreCase = true) ->
                "You're not logged into Spotify. Open Spotify, log in, then try again."
            type.contains("UserNotAuthorized", ignoreCase = true) ->
                "GymBros isn't authorized for Spotify yet. Tap Connect again and approve access."
            detail != null -> "$type: $detail"
            else -> "$type: Could not connect to Spotify"
        }
    }

    companion object {
        const val CLIENT_ID = "ce3bbab4198d4af38a18152de5e3ecd2"
        const val REDIRECT_URI = "https://com.rishabh.gymbros/callback"
        const val AUTH_REQUEST_CODE = 1337

        private const val APP_REMOTE_SCOPE = "app-remote-control"

        private const val TAG = "GymBrosSpotify"
        private const val CONNECTION_TIMEOUT_MS = 12_000L
        private const val PREFS_NAME = "gymbros_spotify_preferences"
        private const val KEY_AUTHORIZED = "spotify_authorized"

        @Volatile private var INSTANCE: SpotifyManager? = null

        fun getInstance(context: Context): SpotifyManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SpotifyManager(context.applicationContext).also { INSTANCE = it }
            }
    }
}
