package com.rishabh.gymbros

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rishabh.gymbros.features.workout.data.WorkoutRepository
import com.rishabh.gymbros.features.cloud.CloudBackupManager
import com.rishabh.gymbros.features.spotify.SpotifyManager
import com.rishabh.gymbros.features.settings.data.SettingsRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.rishabh.gymbros.features.workout.notification.WorkoutActiveNotification
import com.rishabh.gymbros.ui.splash.GymBrosLaunchScreen
import com.rishabh.gymbros.ui.theme.GymBrosTheme

class MainActivity : ComponentActivity() {
    private val requestedDestination = mutableStateOf<String?>(null)
    private val workoutRepository by lazy { WorkoutRepository.getInstance(applicationContext) }
    private val cloudBackupManager by lazy { CloudBackupManager.getInstance(applicationContext) }
    private val spotifyManager by lazy { SpotifyManager.getInstance(applicationContext) }
    private val settingsRepository by lazy { SettingsRepository.getInstance(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedDestination.value = if (intent.getBooleanExtra(WorkoutActiveNotification.EXTRA_OPEN_WORKOUT, false)) "TRAIN" else null
        val launchUiVersion = com.rishabh.gymbros.features.settings.model.UiVersion.V2
        setContent {
            GymBrosTheme {
                var showLaunchAnimation by rememberSaveable { mutableStateOf(true) }

                Box(modifier = Modifier.fillMaxSize()) {
                    GymBrosApp(requestedDestination = requestedDestination.value, onDestinationConsumed = { requestedDestination.value = null })

                    if (showLaunchAnimation) {
                        GymBrosLaunchScreen(
                            onFinished = { showLaunchAnimation = false },
                            uiVersion = launchUiVersion
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(WorkoutActiveNotification.EXTRA_OPEN_WORKOUT, false)) {
            requestedDestination.value = "TRAIN"
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CloudBackupManager.DRIVE_AUTH_REQUEST_CODE) {
            lifecycleScope.launch { cloudBackupManager.handleAuthorizationResult(resultCode, data) }
            return
        }
        if (spotifyManager.handleAuthorizationResult(requestCode, resultCode, data, this)) {
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()
        WorkoutActiveNotification.cancel(this)
        spotifyManager.reconnectIfAuthorized(this)
    }

    override fun onStop() {
        spotifyManager.disconnectForBackground()
        if (cloudBackupManager.state.value.signedIn && cloudBackupManager.state.value.autoBackup) {
            lifecycleScope.launch { cloudBackupManager.backupNow(automatic = true) }
        }
        if (workoutRepository.hasActiveWorkout()) {
            WorkoutActiveNotification.show(this)
        } else {
            WorkoutActiveNotification.cancel(this)
        }
        // Apply a pending 1.0/2.0 launcher icon change only now that the app is no longer visible.
        // Doing it while the app is open makes Android close it.
        settingsRepository.applyLauncherIcon()
        super.onStop()
    }

}
