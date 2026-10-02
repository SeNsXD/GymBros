package com.rishabh.gymbros

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.KeyguardManager
import android.content.Context
import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.rishabh.gymbros.features.reminders.WaterReminderScheduler
import com.rishabh.gymbros.features.cloud.CloudBackupManager
import kotlinx.coroutines.launch
import com.rishabh.gymbros.features.settings.data.SettingsRepository
import com.rishabh.gymbros.features.settings.model.AccentTheme
import com.rishabh.gymbros.features.settings.model.UiVersion
import com.rishabh.gymbros.features.water.data.WaterRepository
import com.rishabh.gymbros.navigation.AppDestination
import com.rishabh.gymbros.ui.components.BottomTabBar
import com.rishabh.gymbros.ui.components.SwipeTabPager
import com.rishabh.gymbros.ui.home.HomeScreenV2
import com.rishabh.gymbros.ui.profile.YouScreenV2
import com.rishabh.gymbros.ui.progress.ProgressScreen
import com.rishabh.gymbros.ui.splash.MeetArcticUpdateScreen
import com.rishabh.gymbros.ui.nutrition.NutritionScreen
import com.rishabh.gymbros.ui.toolbox.FitnessToolboxScreen
import com.rishabh.gymbros.ui.checklist.GymChecklistScreen
import com.rishabh.gymbros.ui.theme.GymBrosAccent
import com.rishabh.gymbros.ui.theme.Ui2Colors
import com.rishabh.gymbros.ui.theme.UiVersionMaterialTheme
import com.rishabh.gymbros.ui.train.TrainScreenV2Arctic
import com.rishabh.gymbros.ui.water.WaterScreenV2
import com.rishabh.gymbros.widgets.GymBrosWidgetUpdater

@Composable
fun GymBrosApp(requestedDestination: String? = null, onDestinationConsumed: () -> Unit = {}) {
    val onboardingContext = LocalContext.current
    val onboarding = androidx.compose.runtime.remember(onboardingContext) {
        com.rishabh.gymbros.features.onboarding.OnboardingStore(onboardingContext)
    }
    val onboardingStep by onboarding.step.collectAsState()
    if (onboardingStep != com.rishabh.gymbros.features.onboarding.OnboardingStep.COMPLETE) {
        com.rishabh.gymbros.ui.onboarding.OnboardingScreen(onboarding)
        return
    }
    var currentDestinationName by rememberSaveable {
        mutableStateOf(AppDestination.HOME.name)
    }
    var workoutTabReselectKey by rememberSaveable { mutableStateOf(0) }
    var startWorkoutRequestKey by rememberSaveable { mutableStateOf(0) }
    var addExerciseRequestKey by rememberSaveable { mutableStateOf(0) }
    var workoutMusicEnabled by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var activeWorkoutScreenVisible by rememberSaveable { mutableStateOf(false) }
    var layoutEditorVisible by rememberSaveable { mutableStateOf(false) }
    // True while the Workout tab is showing one of its own full-screen pages (history, layouts,
    // performance, exercise library, running workout). Tab swiping is paused there.
    var trainSubScreenOpen by remember { mutableStateOf(false) }
    val currentDestination = AppDestination.valueOf(currentDestinationName)
    val context = LocalContext.current
    val waterRepository = WaterRepository.getInstance(context)
    val settingsRepository = SettingsRepository.getInstance(context)
    val workoutRepository = com.rishabh.gymbros.features.workout.data.WorkoutRepository.getInstance(context)
    val cloudBackupManager = CloudBackupManager.getInstance(context)
    val cloudBackupState by cloudBackupManager.state.collectAsState()
    val workoutState by workoutRepository.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(workoutState.isWorkoutActive) {
        if (!workoutState.isWorkoutActive) {
            workoutMusicEnabled = null
            activeWorkoutScreenVisible = false
        }
    }
    val waterState by waterRepository.state.collectAsState()
    val settings by settingsRepository.settings.collectAsState()
    // Arctic is the only live interface. The legacy Ignite button is intentionally kept
    // as an entry point to the Meet Arctic presentation; it never switches app UI/data.
    var showMeetArctic by rememberSaveable { mutableStateOf(false) }
    val requestUiVersionChange: (UiVersion) -> Unit = { requested ->
        if (requested == UiVersion.V1) showMeetArctic = true
    }
    LaunchedEffect(settings.uiVersion) {
        if (settings.uiVersion != UiVersion.V2) settingsRepository.setUiVersion(UiVersion.V2)
    }
    LaunchedEffect(requestedDestination) {
        requestedDestination?.let { requested ->
            runCatching { AppDestination.valueOf(requested) }.getOrNull()?.let { destination ->
                currentDestinationName = destination.name
                onDestinationConsumed()
            }
        }
    }
    GymBrosAccent.current = settings.accentTheme
    var progressUnlocked by rememberSaveable { mutableStateOf(false) }
    val credentialLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) { progressUnlocked = true; currentDestinationName = AppDestination.PROGRESS.name }
    }
    fun openProgressSecurely() {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (km.isDeviceSecure) {
            km.createConfirmDeviceCredentialIntent("Unlock Progress", "Confirm your screen lock to view progress photos")?.let { credentialLauncher.launch(it) }
                ?: run { progressUnlocked = true; currentDestinationName = AppDestination.PROGRESS.name }
        } else { progressUnlocked = true; currentDestinationName = AppDestination.PROGRESS.name }
    }

    // Pages reachable by swiping, in the same left-to-right order as the bottom bar.
    val swipeTabs = remember {
        listOf(AppDestination.HOME, AppDestination.WATER, AppDestination.TRAIN, AppDestination.YOU)
    }
    val swipeEnabled = currentDestination in swipeTabs &&
            !(currentDestination == AppDestination.TRAIN && trainSubScreenOpen) &&
            !showMeetArctic
    fun swipeToTab(target: AppDestination) {
        // Same bookkeeping as tapping the tab in the bottom bar.
        if (target == AppDestination.TRAIN) workoutTabReselectKey += 1
        activeWorkoutScreenVisible = false
        currentDestinationName = target.name
    }

    LaunchedEffect(Unit) {
        GymBrosWidgetUpdater.updateAll(context.applicationContext)
    }

    LaunchedEffect(
        settings.reminderEnabled,
        settings.reminderStartHour,
        settings.reminderEndHour,
        settings.reminderIntervalHours
    ) {
        WaterReminderScheduler.reschedule(context, settings)
    }

    BackHandler(enabled = currentDestination != AppDestination.HOME) {
        currentDestinationName = AppDestination.HOME.name
    }

    UiVersionMaterialTheme(isUi2 = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Ui2Colors.Background,
                bottomBar = {
                    BottomTabBar(
                        selected = currentDestination,
                        uiVersion = UiVersion.V2,
                        onSelect = { destination ->
                            if (destination == AppDestination.TRAIN) {
                                workoutTabReselectKey += 1
                            }
                            activeWorkoutScreenVisible = false
                            currentDestinationName = destination.name
                        },
                        onStartWorkout = {
                            if (currentDestination == AppDestination.TRAIN && (layoutEditorVisible || (workoutState.isWorkoutActive && activeWorkoutScreenVisible))) {
                                // Center action in a running workout or layout editor = Add Exercise.
                                addExerciseRequestKey += 1
                            } else {
                                // Active elsewhere = resume; inactive = open the normal Start Workout flow.
                                activeWorkoutScreenVisible = workoutState.isWorkoutActive
                                startWorkoutRequestKey += 1
                                currentDestinationName = AppDestination.TRAIN.name
                            }
                        },
                        isWorkoutActive = workoutState.isWorkoutActive,
                        activeWorkoutScreenVisible = activeWorkoutScreenVisible,
                        contextAddExerciseVisible = layoutEditorVisible
                    )
                }
            ) { innerPadding ->
                val contentModifier = Modifier
                    .fillMaxSize()
                    .background(Ui2Colors.Background)
                    .padding(innerPadding)

                val renderScreen: @Composable (AppDestination) -> Unit = { screen ->
                    when (screen) {
                        AppDestination.HOME -> {
                            val navigateFromHome: (AppDestination) -> Unit = { destination ->
                                when (destination) {
                                    AppDestination.CHECKLIST -> currentDestinationName = destination.name
                                    AppDestination.PROGRESS -> openProgressSecurely()
                                    else -> currentDestinationName = destination.name
                                }
                            }
                            HomeScreenV2(
                                waterState = waterState,
                                settings = settings,
                                onAddWater = waterRepository::addWater,
                                onSubtractWater = waterRepository::subtractWater,
                                workoutMusicEnabled = workoutMusicEnabled,
                                modifier = contentModifier,
                                onNavigate = navigateFromHome
                            )
                        }

                        AppDestination.WATER -> {
                            WaterScreenV2(
                                state = waterState,
                                settings = settings,
                                onAddWater = waterRepository::addWater,
                                onSubtractWater = waterRepository::subtractWater,
                                onUndoLast = waterRepository::undoLast,
                                onReminderEnabledChange = settingsRepository::setReminderEnabled,
                                onReminderStartHourChange = settingsRepository::setReminderStartHour,
                                onReminderEndHourChange = settingsRepository::setReminderEndHour,
                                onReminderIntervalChange = settingsRepository::setReminderIntervalHours,
                                onDailyGoalChange = settingsRepository::setDailyGoalMl,
                                onQuickAddChange = settingsRepository::setQuickAddPreset,
                                onQuickAddLabelChange = settingsRepository::setQuickAddLabel,
                                onWidgetButtonChange = settingsRepository::setWidgetButtonMl,
                                modifier = contentModifier
                            )
                        }

                        AppDestination.PROGRESS -> if (progressUnlocked) ProgressScreen(modifier = contentModifier, uiVersion = UiVersion.V2) else LaunchedEffect(Unit) { openProgressSecurely() }

                        AppDestination.NUTRITION -> NutritionScreen(modifier = contentModifier)

                        AppDestination.TOOLBOX -> FitnessToolboxScreen(settings = settings, uiVersion = UiVersion.V2, modifier = contentModifier)

                        AppDestination.CHECKLIST -> GymChecklistScreen(
                            modifier = contentModifier,
                            uiVersion = UiVersion.V2,
                            onBack = { currentDestinationName = AppDestination.HOME.name }
                        )

                        AppDestination.TRAIN -> {
                            TrainScreenV2Arctic(
                                modifier = contentModifier,
                                onOpenProfile = { currentDestinationName = AppDestination.YOU.name },
                                onLeaveActiveWorkout = { currentDestinationName = AppDestination.HOME.name },
                                navigationReselectKey = workoutTabReselectKey,
                                startWorkoutRequestKey = startWorkoutRequestKey,
                                onStartWorkoutRequestConsumed = { startWorkoutRequestKey = 0 },
                                addExerciseRequestKey = addExerciseRequestKey,
                                onAddExerciseRequestConsumed = { addExerciseRequestKey = 0 },
                                workoutMusicEnabled = workoutMusicEnabled,
                                onWorkoutMusicChoice = { workoutMusicEnabled = it },
                                onActiveWorkoutVisibilityChanged = { activeWorkoutScreenVisible = it },
                                onLayoutEditorVisibilityChanged = { layoutEditorVisible = it },
                                onSubScreenVisibilityChanged = { trainSubScreenOpen = it }
                            )
                        }
                        AppDestination.YOU -> YouScreenV2(
                            settings = settings,
                            onProfileSave = settingsRepository::setProfile,
                            onProfileImageChange = settingsRepository::setProfileImageUri,
                            onDailyGoalChange = settingsRepository::setDailyGoalMl,
                            onQuickAddChange = settingsRepository::setQuickAddPreset,
                            onQuickAddLabelChange = settingsRepository::setQuickAddLabel,
                            onWidgetButtonChange = settingsRepository::setWidgetButtonMl,
                            onUnitChange = settingsRepository::setUnit,
                            onWeightUnitChange = settingsRepository::setWeightUnit,
                            onHeightUnitChange = settingsRepository::setHeightUnit,
                            onAccentThemeChange = settingsRepository::setAccentTheme,
                            onUiVersionChange = requestUiVersionChange,
                            cloudBackupState = cloudBackupState,
                            onCloudSignIn = { scope.launch { cloudBackupManager.signIn(context) } },
                            onCloudSignOut = { scope.launch { cloudBackupManager.signOut() } },
                            onCloudAutoBackupChange = cloudBackupManager::setAutoBackup,
                            onCloudBackupNow = { scope.launch { cloudBackupManager.backupNow() } },
                            onCloudRestore = { scope.launch { cloudBackupManager.restoreLatest() } },
                            onCloudRestorePoint = { id -> scope.launch { cloudBackupManager.restoreRecoveryPoint(id) } },
                            onCloudRenamePoint = { id, label -> scope.launch { cloudBackupManager.renameRecoveryPoint(id, label) } },
                            onCloudPinPoint = { id, pinned -> scope.launch { cloudBackupManager.setRecoveryPointPinned(id, pinned) } },
                            onCloudDeletePoint = { id -> scope.launch { cloudBackupManager.deleteRecoveryPoint(id) } },
                            onCloudDeleteAll = { scope.launch { cloudBackupManager.deleteAllCloudBackups() } },
                            onCloudSetRecoveryPassword = { password -> scope.launch { cloudBackupManager.setRecoveryPassword(password) } },
                            onCloudUnlockRecoveryPassword = { password -> scope.launch { cloudBackupManager.unlockWithRecoveryPassword(password) } },
                            onCloudDismissRestoreSuggestion = cloudBackupManager::dismissRestoreSuggestion,
                            modifier = contentModifier
                        )
                    }
                }

                if (currentDestination in swipeTabs) {
                    // The four main tabs: finger-following swipe between pages.
                    SwipeTabPager(
                        tabs = swipeTabs,
                        selected = currentDestination,
                        enabled = swipeEnabled,
                        onSwipeSelect = { swipeToTab(it) },
                        modifier = Modifier.fillMaxSize()
                    ) { screen -> renderScreen(screen) }
                } else {
                    // Everything else behaves exactly as before: instant switch.
                    AnimatedContent(
                        targetState = currentDestination,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                        label = "tabContent"
                    ) { screen -> renderScreen(screen) }
                }
            }
            if (showMeetArctic) {
                MeetArcticUpdateScreen(onFinished = { showMeetArctic = false })
            }
        }
    }
}