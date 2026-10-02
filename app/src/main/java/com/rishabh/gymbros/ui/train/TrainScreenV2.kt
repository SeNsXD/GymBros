package com.rishabh.gymbros.ui.train

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.rishabh.gymbros.R
import com.rishabh.gymbros.features.progression.GymBrosProgression
import com.rishabh.gymbros.features.workout.data.WorkoutRepository
import com.rishabh.gymbros.features.water.data.WaterRepository
import com.rishabh.gymbros.features.spotify.SpotifyManager
import com.rishabh.gymbros.features.spotify.SpotifyWebApiManager
import com.rishabh.gymbros.features.workout.domain.ExerciseLibraryCatalog
import com.rishabh.gymbros.features.workout.domain.WorkoutAnalytics
import com.rishabh.gymbros.features.workout.domain.WorkoutCalculations
import com.rishabh.gymbros.features.workout.model.WorkoutCompletionSummary
import com.rishabh.gymbros.features.workout.model.WorkoutLayout
import com.rishabh.gymbros.features.workout.model.WorkoutExerciseType
import com.rishabh.gymbros.ui.theme.Ui2Colors
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign

private val TrainCardShape = RoundedCornerShape(16.dp)
private val TrainSmallShape = RoundedCornerShape(12.dp)

// Workout dashboard accents (Arctic). Mint = Ui2Colors.Lime, cyan = Ui2Colors.Cyan.
private val WorkoutPurple = Color(0xFFA78BFA)
private val WorkoutPurpleTile = Color(0xFF211B3F)
private val WorkoutPink = Color(0xFFFF5C8A)

private val TrainGlassBrush = Brush.verticalGradient(
    listOf(
        Ui2Colors.Primary.copy(alpha = 0.025f),
        Color(0xFF19181A).copy(alpha = 0.98f),
        Color(0xFF141416)
    )
)
private val TrainGlassBorder = Brush.linearGradient(
    listOf(
        Ui2Colors.Primary.copy(alpha = 0.11f),
        Color.White.copy(alpha = 0.055f),
        Color.White.copy(alpha = 0.035f)
    )
)

@Composable
fun TrainScreenV2Arctic(
    modifier: Modifier = Modifier,
    onOpenProfile: () -> Unit = {},
    onLeaveActiveWorkout: () -> Unit = {},
    navigationReselectKey: Int = 0,
    startWorkoutRequestKey: Int = 0,
    onStartWorkoutRequestConsumed: () -> Unit = {},
    addExerciseRequestKey: Int = 0,
    onAddExerciseRequestConsumed: () -> Unit = {},
    workoutMusicEnabled: Boolean? = null,
    onWorkoutMusicChoice: (Boolean) -> Unit = {},
    onActiveWorkoutVisibilityChanged: (Boolean) -> Unit = {},
    onLayoutEditorVisibilityChanged: (Boolean) -> Unit = {},
    onSubScreenVisibilityChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )
    fun requestWorkoutNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val repository = remember(context) { WorkoutRepository.getInstance(context.applicationContext) }
    val waterRepository = remember(context) { WaterRepository.getInstance(context.applicationContext) }
    val spotifyManager = remember(context) { SpotifyManager.getInstance(context.applicationContext) }
    val spotifyState by spotifyManager.state.collectAsState()
    val state by repository.state.collectAsState()
    var songPlaybackUri by rememberSaveable { mutableStateOf<String?>(null) }
    var songPlaybackCounted by rememberSaveable { mutableStateOf(false) }
    var songLastPositionMs by rememberSaveable { mutableStateOf(0L) }

    LaunchedEffect(state.isWorkoutActive, spotifyState.trackUri, spotifyState.playbackPositionMs, spotifyState.durationMs) {
        if (!state.isWorkoutActive) {
            songPlaybackUri = null
            songPlaybackCounted = false
            songLastPositionMs = 0L
            return@LaunchedEffect
        }
        val uri = spotifyState.trackUri ?: return@LaunchedEffect
        val position = spotifyState.playbackPositionMs.coerceAtLeast(0L)
        val duration = spotifyState.durationMs.coerceAtLeast(0L)
        val isNewTrack = songPlaybackUri != uri
        val sameSongRestarted = !isNewTrack && songLastPositionMs > 10_000L && position + 5_000L < songLastPositionMs
        if (isNewTrack || sameSongRestarted) {
            songPlaybackUri = uri
            songPlaybackCounted = false
        }
        if (!songPlaybackCounted && duration > 0L && position * 2L >= duration) {
            repository.incrementActiveSongsPlayed()
            songPlaybackCounted = true
        }
        songLastPositionMs = position
    }
    val spotifyWebApiManager = remember(context) { SpotifyWebApiManager.getInstance(context.applicationContext) }
    val spotifyWebState by spotifyWebApiManager.state.collectAsState()
    val workoutSessionPreferences = remember(context) {
        context.getSharedPreferences("gymbros_workout_session_preferences", android.content.Context.MODE_PRIVATE)
    }
    val musicChoiceSessionKey = "music_choice_session"
    val musicChoiceValueKey = "music_choice_value"
    val persistedWorkoutMusicChoice: Boolean? =
        if (
            state.isWorkoutActive &&
            (state.activeStartedAt ?: 0L) > 0L &&
            workoutSessionPreferences.getLong(musicChoiceSessionKey, -1L) == state.activeStartedAt
        ) {
            workoutSessionPreferences.getBoolean(musicChoiceValueKey, false)
        } else null
    val effectiveWorkoutMusicEnabled = workoutMusicEnabled ?: persistedWorkoutMusicChoice
    var pendingWorkoutMusicStart by rememberSaveable { mutableStateOf(false) }
    // Treat the music question as answered for the lifetime of this workout once the
    // user has made a choice OR Spotify has already been used during the session.
    // This state is intentionally independent from play/pause so pausing music and
    // reopening the active workout cannot briefly recreate the Yes/No prompt.
    var workoutMusicPromptHandled by rememberSaveable(state.activeStartedAt) {
        mutableStateOf(effectiveWorkoutMusicEnabled != null)
    }

    LaunchedEffect(state.isWorkoutActive) {
        if (!state.isWorkoutActive) {
            pendingWorkoutMusicStart = false
            workoutMusicPromptHandled = false
        }
    }

    LaunchedEffect(effectiveWorkoutMusicEnabled, spotifyState.connected, spotifyState.trackUri) {
        if (
            state.isWorkoutActive &&
            (effectiveWorkoutMusicEnabled != null || (spotifyState.connected && spotifyState.trackUri != null))
        ) {
            workoutMusicPromptHandled = true
        }
    }

    fun persistWorkoutMusicChoice(enabled: Boolean) {
        if ((state.activeStartedAt ?: 0L) > 0L) {
            workoutSessionPreferences.edit()
                .putLong(musicChoiceSessionKey, state.activeStartedAt ?: 0L)
                .putBoolean(musicChoiceValueKey, enabled)
                .apply()
        }
        workoutMusicPromptHandled = true
        onWorkoutMusicChoice(enabled)
    }

    LaunchedEffect(state.isWorkoutActive) {
        if (!state.isWorkoutActive) {
            workoutSessionPreferences.edit()
                .remove(musicChoiceSessionKey)
                .remove(musicChoiceValueKey)
                .apply()
        }
    }

    LaunchedEffect(pendingWorkoutMusicStart, spotifyState.connected, spotifyWebState.selectedPlaylist?.uri) {
        if (pendingWorkoutMusicStart && spotifyState.connected) {
            spotifyWebState.selectedPlaylist?.let { playlist ->
                spotifyManager.play(playlist.uri)
            }
            pendingWorkoutMusicStart = false
        }
    }
    val trainPreferences = remember(context) { context.getSharedPreferences("gymbros_train_preferences", android.content.Context.MODE_PRIVATE) }
    var calorieGoal by rememberSaveable { mutableStateOf(trainPreferences.getInt("daily_calorie_goal", 600).coerceAtLeast(1)) }
    var showCalorieGoalDialog by rememberSaveable { mutableStateOf(false) }
    var cardioGoalKm by rememberSaveable { mutableStateOf(trainPreferences.getFloat("daily_cardio_goal_km", 6f).toDouble().coerceAtLeast(0.1)) }
    var showCardioGoalDialog by rememberSaveable { mutableStateOf(false) }
    var showMyStats by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (trainPreferences.getBoolean("open_my_stats_from_home", false)) {
            trainPreferences.edit().remove("open_my_stats_from_home").apply()
            showMyStats = true
        }
    }
    val waterState by waterRepository.state.collectAsState()
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showLayouts by rememberSaveable { mutableStateOf(false) }
    var showPerformance by rememberSaveable { mutableStateOf(false) }
    var showExerciseLibrary by rememberSaveable { mutableStateOf(false) }
    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showActiveWorkout by rememberSaveable {
        mutableStateOf(startWorkoutRequestKey > 0 && state.isWorkoutActive)
    }
    var initialRouteResolved by rememberSaveable { mutableStateOf(!state.isLoading) }
    var pendingEnterActiveWorkout by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showActiveWorkout, state.isWorkoutActive) {
        onActiveWorkoutVisibilityChanged(state.isWorkoutActive && showActiveWorkout)
    }

    // Tells the app shell when this tab is showing one of its full-screen sub-pages, so
    // swiping between bottom-bar tabs is paused there and doesn't throw the user out of it.
    val subScreenOpen = showHistory || showLayouts || showPerformance || showExerciseLibrary ||
            (state.isWorkoutActive && showActiveWorkout)
    LaunchedEffect(subScreenOpen) {
        onSubScreenVisibilityChanged(subScreenOpen)
    }

    fun resumeCurrentWorkout() {
        // activeExerciseIndex == exercises.size means the workout was left on the
        // Add Exercise state. In that case reopen at the latest real exercise.
        val exercises = state.activeExercises
        if (exercises.isNotEmpty() && state.activeExerciseIndex !in exercises.indices) {
            repository.selectActiveExercise(exercises.lastIndex)
        }
        showActiveWorkout = true
    }

    LaunchedEffect(navigationReselectKey) {
        if (navigationReselectKey > 0) {
            // Workout tab is dashboard-only. Never auto-resume an active session here.
            showActiveWorkout = false
            showStartPicker = false
        }
    }

    LaunchedEffect(startWorkoutRequestKey, state.isLoading) {
        if (startWorkoutRequestKey > 0 && !state.isLoading) {
            // Consume this one-shot request immediately so revisiting the Workout tab
            // cannot replay an old Start Workout popup.
            onStartWorkoutRequestConsumed()
            if (state.isWorkoutActive) {
                resumeCurrentWorkout()
            } else {
                requestWorkoutNotificationPermissionIfNeeded()
                if (state.layouts.isEmpty()) {
                    pendingEnterActiveWorkout = true
                    repository.startWorkout()
                } else {
                    showStartPicker = true
                }
            }
        }
    }

    LaunchedEffect(state.isLoading, state.isWorkoutActive, pendingEnterActiveWorkout) {
        if (!state.isLoading && !initialRouteResolved) {
            // Entering Workout normally resolves to the dashboard.
            // Start/Resume requests are handled separately.
            if (startWorkoutRequestKey <= 0) {
                showActiveWorkout = false
            }
            initialRouteResolved = true
        }
        if (!state.isWorkoutActive) {
            showActiveWorkout = false
            pendingEnterActiveWorkout = false
        } else if (pendingEnterActiveWorkout) {
            showActiveWorkout = true
            pendingEnterActiveWorkout = false
        }
    }

    if (state.isLoading && !initialRouteResolved && !state.isWorkoutActive) {
        Box(modifier = modifier.fillMaxSize().background(Ui2Colors.Background))
        return
    }

    if (showCalorieGoalDialog) {
        var goalText by rememberSaveable(calorieGoal) { mutableStateOf(calorieGoal.toString()) }
        AlertDialog(
            onDismissRequest = { showCalorieGoalDialog = false },
            containerColor = Ui2Colors.CardRaised,
            title = { Text("Daily calorie goal", color = Ui2Colors.Text, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it.filter(Char::isDigit).take(4) },
                    label = { Text("Calories (kcal)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val goal = goalText.toIntOrNull()?.coerceIn(1, 9999) ?: calorieGoal
                    calorieGoal = goal
                    trainPreferences.edit().putInt("daily_calorie_goal", goal).apply()
                    showCalorieGoalDialog = false
                }) { Text("Save", color = Ui2Colors.Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showCalorieGoalDialog = false }) { Text("Cancel") } }
        )
    }

    if (showCardioGoalDialog) {
        var goalText by rememberSaveable(cardioGoalKm) { mutableStateOf(formatDailyDecimal(cardioGoalKm)) }
        AlertDialog(
            onDismissRequest = { showCardioGoalDialog = false },
            containerColor = Ui2Colors.CardRaised,
            title = { Text("Daily cardio goal", color = Ui2Colors.Text, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { input ->
                        goalText = input.filter { it.isDigit() || it == '.' }.let { filtered ->
                            val firstDot = filtered.indexOf('.')
                            if (firstDot >= 0) filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "") else filtered
                        }.take(5)
                    },
                    label = { Text("Distance (km)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val goal = goalText.toDoubleOrNull()?.coerceIn(0.1, 999.0) ?: cardioGoalKm
                    cardioGoalKm = goal
                    trainPreferences.edit().putFloat("daily_cardio_goal_km", goal.toFloat()).apply()
                    showCardioGoalDialog = false
                }) { Text("Save", color = Ui2Colors.Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showCardioGoalDialog = false }) { Text("Cancel") } }
        )
    }

    if (showHistory) {
        BackHandler { showHistory = false }
        CompositionLocalProvider(LocalWorkoutUiPalette provides Ui2WorkoutPalette) {
            PastWorkoutsScreen(
                sessions = state.sessions,
                setLogs = state.sessionSetLogs,
                sessionMetrics = state.sessionMetrics,
                onBack = { showHistory = false },
                modifier = modifier
            )
        }
        return
    }

    if (showLayouts) {
        BackHandler { showLayouts = false }
        CompositionLocalProvider(LocalWorkoutUiPalette provides Ui2WorkoutPalette) {
            SavedLayoutsScreen(
                layouts = state.layouts,
                plannedExercises = state.plannedExercises,
                selectedCategory = state.selectedCategory,
                onBack = { showLayouts = false },
                onApply = { layoutId ->
                    showLayouts = false
                    if (state.isWorkoutActive) {
                        resumeCurrentWorkout()
                    } else {
                        requestWorkoutNotificationPermissionIfNeeded()
                        pendingEnterActiveWorkout = true
                        repository.startWorkout(layoutId)
                    }
                },
                onDuplicate = repository::duplicateLayout,
                onDelete = repository::deleteLayout,
                onCreate = repository::createLayout,
                onUpdate = repository::updateLayout,
                addExerciseRequestKey = addExerciseRequestKey,
                onAddExerciseRequestConsumed = onAddExerciseRequestConsumed,
                onEditorVisibilityChanged = onLayoutEditorVisibilityChanged,
                sessions = state.sessions,
                setLogs = state.sessionSetLogs,
                modifier = modifier
            )
        }
        return
    }

    if (showPerformance) {
        BackHandler { showPerformance = false }
        CompositionLocalProvider(LocalWorkoutUiPalette provides Ui2WorkoutPalette) {
            PerformanceScreen(
                state = state,
                onBack = { showPerformance = false },
                modifier = modifier
            )
        }
        return
    }

    if (showExerciseLibrary) {
        BackHandler { showExerciseLibrary = false }
        CompositionLocalProvider(LocalWorkoutUiPalette provides Ui2WorkoutPalette) {
            ExerciseLibraryScreen(
                plannedExercises = state.plannedExercises,
                addedExerciseNames = if (state.isWorkoutActive) state.activeExercises.map { it.name }.toSet() else null,
                pageTitle = "Add Exercise",
                onBack = { showExerciseLibrary = false },
                onAdd = { item ->
                    showExerciseLibrary = false
                    pendingEnterActiveWorkout = true
                    repository.addLibraryMovement(item)
                },
                onCreateCustom = { name, type, equipment, tag ->
                    repository.addCustomMovement(
                        name = name,
                        exerciseType = type,
                        equipment = equipment,
                        tag = tag
                    )
                    if (state.isWorkoutActive) {
                        repository.addActiveExercise(
                            name = name,
                            exerciseType = type,
                            equipment = equipment,
                            sets = 1,
                            targetReps = 0,
                            loadKg = 0.0,
                            cardioLevel = 0.0,
                            tag = tag
                        )
                        showExerciseLibrary = false
                        pendingEnterActiveWorkout = true
                    }
                },
                onDeleteCustom = { exerciseId -> repository.deleteCustomMovement(exerciseId) },
                modifier = modifier
            )
        }
        return
    }

    val today = LocalDate.now()
    val todaySessions = WorkoutCalculations.sessionsForDay(state.sessions, today)
    val streakDays = WorkoutCalculations.streakDays(state.sessions, today, restDays = state.restDays)
    val isTodayRestDay = today.toEpochDay() in state.restDays
    val personalRecordCount = remember(state.sessions, state.sessionSetLogs) {
        WorkoutAnalytics.personalRecords(state.sessions, state.sessionSetLogs).size
    }
    val todayVolumeKg = todaySessions.sumOf { it.totalVolumeKg }
    val completedExerciseCount = todaySessions.sumOf { it.completedExercises }
    val todaySessionIds = todaySessions.map { it.id }.toSet()
    val todayEstimatedCalories = todaySessionIds
        .fold(0.0) { total, sessionId ->
            total + (state.sessionMetrics[sessionId]?.get(WorkoutRepository.METRIC_CARDIO_CALORIES) ?: 0.0)
        }
        .roundToInt()
    val todayCardioKm = todaySessionIds.fold(0.0) { total, sessionId ->
        total + (state.sessionMetrics[sessionId]?.get(WorkoutRepository.METRIC_DISTANCE_KM) ?: 0.0)
    }

    // Monday -> today. The card updates automatically as workout history changes.
    val weekStart = today.with(DayOfWeek.MONDAY)
    val weekSessions = remember(state.sessions, today) {
        state.sessions.filter { session ->
            val sessionDay = Instant.ofEpochMilli(session.endedAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            !sessionDay.isBefore(weekStart) && !sessionDay.isAfter(today)
        }
    }
    val weekWorkoutCount = weekSessions.size
    val weekExerciseCount = weekSessions.sumOf { it.completedExercises }
    val weekDurationSeconds = weekSessions.sumOf { it.durationSeconds }
    val weekPrCount = remember(state.sessions, state.sessionSetLogs, today) {
        WorkoutAnalytics.personalRecords(state.sessions, state.sessionSetLogs).count { record ->
            if (record.lastPerformedAt <= 0L) false
            else {
                val recordDay = Instant.ofEpochMilli(record.lastPerformedAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                !recordDay.isBefore(weekStart) && !recordDay.isAfter(today)
            }
        }
    }
    val weekDaysV2 = remember(state.sessions, state.restDays, today) {
        (0L..6L).map { offset ->
            val day = weekStart.plusDays(offset)
            val seconds = WorkoutCalculations.completedSessionsForDay(state.sessions, day).sumOf { it.durationSeconds }
            WorkoutStreakDay(day = day, durationSeconds = seconds, isRestDay = day.toEpochDay() in state.restDays)
        }
    }
    val trainedDaysThisWeek = weekDaysV2.count { it.durationSeconds > 0L }

    if (showMyStats) {
        val totalDistance = state.sessionMetrics.values.sumOf { it[WorkoutRepository.METRIC_DISTANCE_KM] ?: 0.0 }
        val totalCalories = state.sessionMetrics.values.sumOf { it[WorkoutRepository.METRIC_CARDIO_CALORIES] ?: 0.0 }.roundToInt()
        val totalSecondsAll = state.sessions.sumOf { it.durationSeconds }
        val totalPrs = personalRecordCount
        val totalSongsPlayed = state.sessionMetrics.values.sumOf { (it[WorkoutRepository.METRIC_SONGS_PLAYED] ?: 0.0).toInt() }
        MyStatsDialogV2(
            onDismiss = { showMyStats = false },
            streakDays = streakDays,
            totalPrs = totalPrs,
            totalWorkouts = state.sessions.size,
            totalDistanceKm = totalDistance,
            totalCalories = totalCalories,
            totalSeconds = totalSecondsAll,
            waterMl = waterState.lifetimeIntakeMl,
            totalQuestsCompleted = GymBrosProgression.totalDailyTasksCompleted(context),
            totalSongsPlayed = totalSongsPlayed,
            preferences = trainPreferences
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Ui2Colors.Background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 10.dp,
                bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                WorkoutHeaderV2(
                    today = today,
                    onStatsClick = { showMyStats = true }
                )
            }

            item {
                CurrentWorkoutStreakCardV2(streakDays = streakDays)
            }

            item {
                WorkoutHeroCardV2(
                    today = today,
                    weekDays = weekDaysV2,
                    trainedDaysThisWeek = trainedDaysThisWeek,
                    weekDurationSeconds = weekDurationSeconds
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().height(154.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PerformanceCardV2(
                        prCount = personalRecordCount,
                        onClick = { showPerformance = true },
                        modifier = Modifier.weight(1.12f).fillMaxSize()
                    )
                    Column(
                        modifier = Modifier.weight(.88f).fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WorkoutMetricCardV2(
                            title = "Past workouts",
                            value = state.sessions.size.toString(),
                            unit = "logged",
                            icon = Icons.Outlined.History,
                            accent = WorkoutPink,
                            onClick = { showHistory = true },
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                        WorkoutMetricCardV2(
                            title = "Exercises",
                            value = ExerciseLibraryCatalog.count.toString(),
                            unit = "moves",
                            icon = Icons.Outlined.LibraryBooks,
                            accent = Ui2Colors.Cyan,
                            onClick = { showExerciseLibrary = true },
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                }
            }

            item {
                SavedLayoutsCardV2(
                    count = state.layouts.size,
                    onClick = { showLayouts = true }
                )
            }

            item {
                RestDayCardV2(
                    isRestDay = isTodayRestDay,
                    enabled = !state.isWorkoutActive && todaySessions.isEmpty(),
                    onClick = repository::toggleTodayRestDay
                )
            }
        }

        if (showStartPicker) {
            StartWorkoutPickerDialog(
                layouts = state.layouts,
                onDismiss = { showStartPicker = false },
                onExerciseLibrary = {
                    showStartPicker = false
                    showExerciseLibrary = true
                },
                onBlank = {
                    showStartPicker = false
                    pendingEnterActiveWorkout = true
                    repository.startWorkout()
                },
                onLayout = { layoutId ->
                    showStartPicker = false
                    pendingEnterActiveWorkout = true
                    repository.startWorkout(layoutId)
                }
            )
        }

        state.lastCompletion?.let { completion ->
            WorkoutCompletionDialog(
                completion = completion,
                onSaveTemplate = repository::saveLastCompletionAsLayout,
                onUpdateTemplate = repository::updateCompletionSourceLayout,
                onLayoutOnly = repository::updateCompletionSourceLayoutStructureOnly,
                onValuesOnly = repository::updateCompletionSourceLayoutValuesOnly,
                musicWasPlaying = spotifyState.connected && !spotifyState.isPaused && spotifyState.trackUri != null,
                onStopMusic = {
                    if (spotifyState.connected && !spotifyState.isPaused) spotifyManager.togglePlayPause()
                },
                onDismiss = repository::dismissCompletionSummary
            )
        }

        // Keep the Workout dashboard composed underneath the active session.
        // This prevents the black/empty frame while the active session slides down.
        if (state.isWorkoutActive && showActiveWorkout) {
            CompositionLocalProvider(LocalWorkoutUiPalette provides Ui2WorkoutPalette) {
                ActiveWorkoutScreen(
                    state = state,
                    repository = repository,
                    onLeaveRunning = onLeaveActiveWorkout,
                    // Once answered, never feed a transient null back into ActiveWorkoutScreen.
                    // Preserve the actual Yes/No choice; playback pause state is unrelated.
                    workoutMusicEnabled = effectiveWorkoutMusicEnabled,
                    spotifyConnected = spotifyState.connected,
                    spotifyConnecting = spotifyState.connecting,
                    spotifyIsPlaying = spotifyState.connected && !spotifyState.isPaused && spotifyState.trackUri != null,
                    hasSelectedSpotifyPlaylist = spotifyWebState.selectedPlaylist != null,
                    onStartWorkoutMusic = {
                        persistWorkoutMusicChoice(true)
                        val playlist = spotifyWebState.selectedPlaylist
                        when {
                            playlist == null -> Unit
                            spotifyState.connected -> spotifyManager.play(playlist.uri)
                            else -> {
                                pendingWorkoutMusicStart = true
                                spotifyManager.connect(context)
                            }
                        }
                    },
                    onSkipWorkoutMusic = {
                        pendingWorkoutMusicStart = false
                        persistWorkoutMusicChoice(false)
                    },
                    onMinimize = { showActiveWorkout = false },
                    addExerciseRequestKey = addExerciseRequestKey,
                    onAddExerciseRequestConsumed = onAddExerciseRequestConsumed,
                    onWorkoutDiscarded = {
                        pendingWorkoutMusicStart = false
                        if (spotifyState.connected && !spotifyState.isPaused) spotifyManager.togglePlayPause()
                    },
                    useUi2SetQualityStyle = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun WorkoutHeaderV2(
    today: LocalDate,
    onStatsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = today.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.ENGLISH)),
                color = Ui2Colors.Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Workout",
                color = Ui2Colors.Text,
                fontSize = 28.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Ui2Colors.CardRaised)
                .clickable(onClick = onStatsClick)
                .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Analytics,
                contentDescription = null,
                tint = Ui2Colors.Lime,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "My Stats",
                color = Ui2Colors.Lime,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

// Ring quarters in order: green, blue, purple, red-pink (same red-pink as the Daily quests card).
private val WeekRingQuarterColors = listOf(
    Ui2Colors.Lime,
    Ui2Colors.Cyan,
    Ui2Colors.Lavender,
    Color(0xFFFA2A55)
)

// Sweep-gradient stops (fractions of a full 360 degree turn). The visible ring runs from
// 140 to 400 degrees, so each quarter is 65 degrees: boundaries sit at 205, 270 and 335.
// The 0.2 cut lies in the unused gap at the bottom of the ring, so it is never seen.
private val WeekRingBrushStops: List<Pair<Float, Color>> = run {
    val blend = 0.010f
    val q1 = 205f / 360f
    val q2 = 270f / 360f
    val q3 = 335f / 360f
    listOf(
        0f to WeekRingQuarterColors[3],
        0.2f to WeekRingQuarterColors[3],
        0.2f to WeekRingQuarterColors[0],
        (q1 - blend) to WeekRingQuarterColors[0],
        (q1 + blend) to WeekRingQuarterColors[1],
        (q2 - blend) to WeekRingQuarterColors[1],
        (q2 + blend) to WeekRingQuarterColors[2],
        (q3 - blend) to WeekRingQuarterColors[2],
        (q3 + blend) to WeekRingQuarterColors[3],
        1f to WeekRingQuarterColors[3]
    )
}

@Composable
private fun WorkoutHeroCardV2(
    today: LocalDate,
    weekDays: List<WorkoutStreakDay>,
    trainedDaysThisWeek: Int,
    weekDurationSeconds: Long
) {
    // Weekly completion drives the hero ring. The sweep runs once whenever this
    // dashboard card enters composition (for example, when returning from another
    // Workout card/screen) and then stays at its completed position.
    val targetProgress = (trainedDaysThisWeek / 7f).coerceIn(0f, 1f)
    val animatedProgress = remember { Animatable(0f) }
    // The ring is split into four quarters: 0-25% green, 25-50% blue, 50-75% purple,
    // 75-100% red-pink. The color of the quarter the ring has reached tints the label.
    val currentQuarter = (targetProgress / 0.25f).toInt().coerceIn(0, 3)
    val ringColor by androidx.compose.animation.animateColorAsState(
        targetValue = WeekRingQuarterColors[currentQuarter],
        animationSpec = tween(durationMillis = 600),
        label = "week-ring-color"
    )

    LaunchedEffect(targetProgress) {
        animatedProgress.snapTo(0f)
        if (targetProgress > 0f) {
            delay(90L)
            animatedProgress.animateTo(
                targetValue = targetProgress,
                animationSpec = tween(durationMillis = 700)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Ui2Colors.CardRaised.copy(alpha = .95f), Ui2Colors.Card)
                )
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(156.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 11.dp.toPx()
                val inset = stroke / 2f + 3.dp.toPx()
                drawArc(
                    color = Ui2Colors.QuestEmpty,
                    startAngle = -220f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - inset * 2f, size.height - inset * 2f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                val p = animatedProgress.value.coerceIn(0f, 1f)
                if (p > 0f) {
                    val arcTopLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                    val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2f, size.height - inset * 2f)
                    val ringCenter = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                    // One continuous arc painted with a sweep gradient: a single stroke means
                    // seamless joins, clean round caps at both ends, and a glow that follows the
                    // ring. The gradient is laid out on the same angles as the arc (it starts at
                    // 140 degrees and covers 260), with a short soft blend at each quarter.
                    fun ringBrush(alpha: Float): Brush = Brush.sweepGradient(
                        colorStops = WeekRingBrushStops.map { (stop, color) -> stop to color.copy(alpha = alpha) }.toTypedArray(),
                        center = ringCenter
                    )
                    drawArc(
                        brush = ringBrush(.16f),
                        startAngle = -220f,
                        sweepAngle = 260f * p,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = stroke + 7.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = ringBrush(1f),
                        startAngle = -220f,
                        sweepAngle = 260f * p,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Trained this week", color = Ui2Colors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = formatWorkoutDurationV2(weekDurationSeconds),
                    color = Ui2Colors.Text,
                    fontSize = 31.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$trainedDaysThisWeek of 7 days",
                    color = ringColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // The old summary strip is intentionally gone. Its space is given to the
        // Mon-Sun graph so the weekly pattern is easier to scan at a glance.
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(94.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            weekDays.forEach { item ->
                WorkoutDayV2(item = item, today = today)
            }
        }
    }
}

@Composable
private fun CurrentWorkoutStreakCardV2(streakDays: Int) {
    // Same neutral surface as the other Arctic cards; only the streak pill,
    // fire and streak value keep the warm orange treatment.
    val streakAccent = Color(0xFFFF8D00)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ui2Colors.Card)
            .border(0.5.dp, Ui2Colors.CardOutline, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Current Workout Streak",
            modifier = Modifier.weight(1f),
            color = Ui2Colors.Text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(streakAccent.copy(alpha = 0.12f))
                .border(1.dp, streakAccent.copy(alpha = 0.4f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CurrentStreakAnimatedFireV2()
            Text(
                text = "$streakDays ${if (streakDays == 1) "day" else "days"}",
                color = streakAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CurrentStreakAnimatedFireV2() {
    val context = LocalContext.current
    val motionEnabled = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) != 0f
        }.getOrDefault(true)
    }
    val transition = rememberInfiniteTransition(label = "current-streak-fire")
    val bigScaleY by transition.animateFloat(0.95f, 1.20f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "current-big-y")
    val bigScaleX by transition.animateFloat(0.95f, 1.08f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "current-big-x")
    val bigRotation by transition.animateFloat(-3f, 3f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "current-big-r")
    val smallScaleY by transition.animateFloat(0.95f, 1.18f, infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "current-small-y")
    val smallRotation by transition.animateFloat(3f, -3f, infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "current-small-r")
    val ember1 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "current-ember-1")
    val ember2 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1900, delayMillis = 350, easing = LinearEasing)), label = "current-ember-2")

    Box(Modifier.size(width = 16.dp, height = 18.dp)) {
        Canvas(
            Modifier.fillMaxSize().graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 1f)
                scaleX = if (motionEnabled) bigScaleX else 1f
                scaleY = if (motionEnabled) bigScaleY else 1f
                rotationZ = if (motionEnabled) bigRotation else 0f
            }
        ) {
            val p = Path().apply {
                moveTo(size.width * .50f, 0f)
                cubicTo(size.width * .58f, size.height * .28f, size.width, size.height * .34f, size.width * .88f, size.height * .72f)
                cubicTo(size.width * .78f, size.height, size.width * .22f, size.height, size.width * .12f, size.height * .72f)
                cubicTo(size.width * .04f, size.height * .48f, size.width * .28f, size.height * .36f, size.width * .36f, size.height * .24f)
                cubicTo(size.width * .34f, size.height * .46f, size.width * .48f, size.height * .54f, size.width * .54f, size.height * .55f)
                cubicTo(size.width * .47f, size.height * .36f, size.width * .43f, size.height * .17f, size.width * .50f, 0f)
                close()
            }
            drawPath(p, Color(0xFFFF5A1F))
        }
        Canvas(
            Modifier
                .size(width = 10.dp, height = 11.dp)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    scaleX = if (motionEnabled) 1.02f else 1f
                    scaleY = if (motionEnabled) smallScaleY else 1f
                    rotationZ = if (motionEnabled) smallRotation else 0f
                }
        ) {
            val p = Path().apply {
                moveTo(size.width * .5f, 0f)
                cubicTo(size.width, size.height * .45f, size.width * .88f, size.height, size.width * .5f, size.height)
                cubicTo(size.width * .12f, size.height, 0f, size.height * .55f, size.width * .35f, size.height * .28f)
                cubicTo(size.width * .34f, size.height * .52f, size.width * .48f, size.height * .57f, size.width * .5f, 0f)
                close()
            }
            drawPath(p, Color(0xFFFFC233))
        }
        if (motionEnabled) {
            CurrentStreakEmberV2(progress = ember1, startX = 5.dp, driftX = (-2).dp)
            CurrentStreakEmberV2(progress = ember2, startX = 10.dp, driftX = 2.dp)
        }
    }
}

@Composable
private fun BoxScope.CurrentStreakEmberV2(
    progress: Float,
    startX: androidx.compose.ui.unit.Dp,
    driftX: androidx.compose.ui.unit.Dp
) {
    val alpha = when {
        progress < .15f -> progress / .15f * .9f
        else -> ((1f - progress) / .85f * .9f).coerceAtLeast(0f)
    }
    Box(
        Modifier
            .size(3.dp)
            .offset(x = startX, y = 2.dp)
            .graphicsLayer {
                translationX = driftX.toPx() * progress
                translationY = -14.dp.toPx() * progress
                this.alpha = alpha
            }
            .background(Color(0xFFFFB020), CircleShape)
    )
}

@Composable
private fun WorkoutDayV2(item: WorkoutStreakDay, today: LocalDate) {
    val isFuture = item.day.isAfter(today)
    val isToday = item.day == today
    val trained = item.durationSeconds > 0L
    val indicator = when {
        isToday -> WorkoutPurple
        item.isRestDay -> Ui2Colors.Cyan
        trained -> Ui2Colors.Lime
        !isFuture -> Color(0xFFFA2A55)
        else -> Ui2Colors.QuestEmpty
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(41.dp)
    ) {
        Box(
            Modifier
                .width(9.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isFuture) Ui2Colors.QuestEmpty.copy(alpha = .65f) else indicator)
        )
        Text(
            text = item.day.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)).take(3),
            color = if (isToday) Ui2Colors.Text else Ui2Colors.Muted,
            fontSize = 10.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(top = 7.dp)
        )
        Text(
            text = when {
                isFuture -> ""
                item.isRestDay && !trained -> "Rest"
                isToday && !trained -> "Today"
                else -> formatHoursWorkedV2(item.durationSeconds)
            },
            color = when {
                isToday -> WorkoutPurple
                item.isRestDay -> Ui2Colors.Cyan
                trained -> Ui2Colors.Lime
                !isFuture -> Color(0xFFFA2A55)
                else -> Ui2Colors.Dim
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun PerformanceCardV2(
    prCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF191A2B), Color(0xFF11131D))
                )
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text("Performance", color = WorkoutPurple, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val points = listOf(.78f, .70f, .64f, .63f, .52f, .42f, .30f, .18f)
            val dotRadius = 3.5.dp.toPx()
            val usableWidth = size.width - dotRadius
            fun pointAt(i: Int) = androidx.compose.ui.geometry.Offset(
                usableWidth * i / points.lastIndex.toFloat(),
                size.height * points[i]
            )
            for (i in 0 until points.lastIndex) {
                drawLine(
                    color = WorkoutPurple,
                    start = pointAt(i),
                    end = pointAt(i + 1),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawCircle(color = WorkoutPurple, radius = dotRadius, center = pointAt(points.lastIndex))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(prCount.toString(), color = Ui2Colors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(" PRs", color = WorkoutPurple, fontSize = 12.sp, modifier = Modifier.padding(bottom = 5.dp))
        }
    }
}

@Composable
private fun WorkoutMetricCardV2(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Ui2Colors.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                Text(value, color = Ui2Colors.Text, fontSize = 24.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
                Text(unit, color = Ui2Colors.Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 3.dp, bottom = 3.dp))
            }
        }
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SavedLayoutsCardV2(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Ui2Colors.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(WorkoutPurpleTile),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.DateRange, null, tint = WorkoutPurple, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
            Text("Saved layouts", color = Ui2Colors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (count == 1) "1 routine & template" else "$count routines & templates",
                color = Ui2Colors.Muted,
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        Text("›", color = WorkoutPurple, fontSize = 22.sp)
    }
}

@Composable
private fun RestDayCardV2(
    isRestDay: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val clickable = enabled || isRestDay
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Ui2Colors.Card)
            .clickable(enabled = clickable, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Rest day", color = Ui2Colors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    isRestDay -> "Recovery day marked, your streak is protected"
                    !enabled -> "Available before today's workout starts"
                    else -> "Take a day off, keep your streak"
                },
                color = Ui2Colors.Muted,
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(20.dp)
                .clip(CircleShape)
                .background(if (isRestDay) Ui2Colors.Cyan.copy(alpha = .75f) else Ui2Colors.QuestEmpty)
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(if (isRestDay) Alignment.CenterEnd else Alignment.CenterStart)
                    .background(if (isRestDay) Ui2Colors.Text else Ui2Colors.Cyan, CircleShape)
            )
        }
    }
}

@Composable
private fun StartWorkoutButtonV2(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .shadow(
                elevation = 18.dp,
                shape = CircleShape,
                ambientColor = Ui2Colors.Lime.copy(alpha = .22f),
                spotColor = Ui2Colors.Cyan.copy(alpha = .18f)
            )
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFB9E98A), Color(0xFF79DDC7))
                )
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF0A1713),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatWorkoutDurationV2(seconds: Long): String {
    val totalMinutes = (seconds / 60L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
        hours > 0L -> "${hours}h"
        else -> "${minutes} min"
    }
}

private fun formatHoursWorkedV2(seconds: Long): String {
    if (seconds <= 0L) return "0h"
    val hours = seconds / 3600.0
    return if (hours >= 10.0) "${hours.roundToInt()}h" else String.format(Locale.US, "%.1fh", hours)
}

@Composable
private fun TrainHeader(today: LocalDate) {
    val dateText = today.format(
        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
    ).uppercase(Locale.ENGLISH)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateText,
            color = Ui2Colors.Muted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.0.sp
        )
    }
}

@Composable
private fun TodayHeading() {
    Text(
        text = "Workout & Performance",
        color = Ui2Colors.Text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun RestDayCard(
    isRestDay: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TrainCardShape)
            .background(
                if (isRestDay) {
                    Brush.verticalGradient(
                        listOf(
                            Ui2Colors.Primary.copy(alpha = 0.10f),
                            Ui2Colors.Primary.copy(alpha = 0.035f)
                        )
                    )
                } else {
                    TrainGlassBrush
                }
            )
            .border(
                width = 1.dp,
                brush = if (isRestDay) {
                    Brush.linearGradient(
                        listOf(
                            Ui2Colors.Primary.copy(alpha = 0.34f),
                            Ui2Colors.Primary.copy(alpha = 0.12f)
                        )
                    )
                } else {
                    TrainGlassBorder
                },
                shape = TrainCardShape
            )
            .clickable(enabled = enabled || isRestDay, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Hotel,
            contentDescription = null,
            tint = if (isRestDay) Ui2Colors.Primary else Ui2Colors.Muted
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = "Rest Day",
                color = Ui2Colors.Text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = when {
                    isRestDay -> "Recovery day marked • your streak is protected"
                    !enabled -> "Available before today's workout starts"
                    else -> "Mark today for recovery without breaking your streak"
                },
                color = Ui2Colors.Muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun StreakPill(streakDays: Int) {
    val text = if (streakDays <= 0) "Start your streak" else "$streakDays-Day Streak"
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Ui2Colors.CardRaised)
            .border(
                width = 1.dp,
                color = Ui2Colors.Primary.copy(alpha = 0.18f),
                shape = CircleShape
            )
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.LocalFireDepartment,
            contentDescription = null,
            tint = Ui2Colors.Primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            color = Ui2Colors.Primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ShortcutRow(
    pastWorkoutCount: Int,
    savedLayoutCount: Int,
    onPastWorkouts: () -> Unit,
    onSavedLayouts: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ShortcutCard(
            title = "Past Workouts",
            subtitle = "View activity logs",
            badge = "$pastWorkoutCount LOGGED",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = Ui2Colors.Primary
                )
            },
            onClick = onPastWorkouts,
            modifier = Modifier.weight(1f)
        )
        ShortcutCard(
            title = "Saved Layouts",
            subtitle = "Routines & templates",
            badge = "$savedLayoutCount SAVED",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = null,
                    tint = Ui2Colors.Primary
                )
            },
            onClick = onSavedLayouts,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InsightShortcutRow(
    personalRecordCount: Int,
    libraryCount: Int,
    onPerformance: () -> Unit,
    onExerciseLibrary: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        InsightShortcutCard(
            title = "Performance",
            subtitle = "Progress & PRs",
            badge = if (personalRecordCount > 0) "$personalRecordCount PRS" else "START",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Analytics,
                    contentDescription = null,
                    tint = Ui2Colors.Primary
                )
            },
            onClick = onPerformance,
            modifier = Modifier.weight(1f)
        )
        InsightShortcutCard(
            title = "Exercises",
            subtitle = "Movement library",
            badge = "$libraryCount MOVES",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.LibraryBooks,
                    contentDescription = null,
                    tint = Ui2Colors.Primary
                )
            },
            onClick = onExerciseLibrary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InsightShortcutCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 96.dp)
            .clip(TrainCardShape)
            .background(TrainGlassBrush)
            .border(1.dp, TrainGlassBorder, TrainCardShape)
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(TrainSmallShape)
                    .background(Ui2Colors.QuestEmpty),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Text(
                text = badge,
                color = Ui2Colors.Primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Ui2Colors.Primary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                maxLines = 1
            )
        }
        Column {
            Text(
                text = title,
                color = Ui2Colors.Text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Ui2Colors.Dim,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ShortcutCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .heightIn(min = 136.dp)
            .clip(TrainCardShape)
            .background(TrainGlassBrush)
            .border(1.dp, TrainGlassBorder, TrainCardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(TrainSmallShape)
                    .background(Ui2Colors.QuestEmpty),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Text(
                text = badge,
                color = Ui2Colors.Muted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Ui2Colors.QuestEmpty)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                maxLines = 1
            )
        }

        Column {
            Text(
                text = title,
                color = Ui2Colors.Text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Ui2Colors.Muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun ThisWeekCard(
    workouts: Int,
    exercises: Int,
    durationSeconds: Long,
    prs: Int
) {
    val totalMinutes = durationSeconds / 60L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    val timeText = when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
        hours > 0L -> "${hours}h"
        else -> "${minutes}m"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TrainCardShape)
            .background(TrainGlassBrush)
            .border(1.dp, TrainGlassBorder, TrainCardShape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "This Week",
                color = Ui2Colors.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Icon(
                imageVector = Icons.Outlined.DateRange,
                contentDescription = null,
                tint = Ui2Colors.Primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeekMetric(workouts.toString(), if (workouts == 1) "Workout" else "Workouts", Modifier.weight(1f))
            WeekMetric(exercises.toString(), "Exercises", Modifier.weight(1f))
            WeekMetric(timeText, "Training", Modifier.weight(1f))
            WeekMetric(prs.toString(), "PRs", Modifier.weight(1f))
        }
    }
}

@Composable
private fun WeekMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Ui2Colors.Primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Text(
            text = label,
            color = Ui2Colors.Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

private fun formatDailyDecimal(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.US, "%.1f", value)
}

private data class WorkoutStreakDay(
    val day: LocalDate,
    val durationSeconds: Long,
    val isRestDay: Boolean = false
)

@Composable
private fun WorkoutStreakCard(
    days: List<WorkoutStreakDay>,
    currentStreak: Int,
    bestStreak: Int,
    today: LocalDate,
    onStatsClick: () -> Unit
) {
    val workoutDaysThisWeek = days.count { it.durationSeconds > 0L }
    val maxVisualMinutes = 90f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(TrainGlassBrush)
            .border(1.dp, TrainGlassBorder, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Ui2Colors.QuestEmpty)
                        .border(1.dp, Ui2Colors.CardOutline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        tint = Ui2Colors.Primary,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Column {
                    Text(
                        text = "Workout Streak",
                        color = Ui2Colors.Text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Ui2Colors.Primary.copy(alpha = 0.22f),
                                Ui2Colors.Primary.copy(alpha = 0.09f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Ui2Colors.Primary.copy(alpha = 0.42f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(onClick = onStatsClick)
                    .padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(25.dp)
                        .clip(CircleShape)
                        .background(Ui2Colors.Primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Analytics,
                        contentDescription = null,
                        tint = Ui2Colors.Primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = "My Stats",
                    color = Ui2Colors.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(102.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { point ->
                val minutes = point.durationSeconds / 60f
                val hasWorkout = point.durationSeconds > 0L
                val isFuture = point.day.isAfter(today)
                val isToday = point.day == today
                val fillFraction = if (hasWorkout) (minutes / maxVisualMinutes).coerceIn(0.18f, 1f) else 0f
                val label = when { hasWorkout -> formatStreakDuration(point.durationSeconds); point.isRestDay -> "REST"; else -> "--" }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = label,
                        color = if (isToday) Ui2Colors.Text else Ui2Colors.Muted,
                        fontSize = 11.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(42.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Ui2Colors.QuestEmpty.copy(alpha = if (isFuture) 0.55f else 1f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (hasWorkout && !isFuture) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((42f * fillFraction).dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Brush.linearGradient(listOf(Ui2Colors.Primary, Ui2Colors.Cyan)))
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = point.day.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)),
                        color = when {
                            isToday -> Ui2Colors.Primary
                            isFuture -> Ui2Colors.Dim
                            else -> Ui2Colors.Muted
                        },
                        fontSize = 11.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(TrainGlassBrush)
                .border(1.dp, TrainGlassBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StreakSummary(
                icon = Icons.Outlined.DateRange,
                label = "This week",
                primary = "$workoutDaysThisWeek / 7",
                suffix = "days",
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .width(1.dp)
                    .height(48.dp)
                    .background(Ui2Colors.CardOutline)
            )
            StreakSummary(
                icon = Icons.Outlined.EmojiEvents,
                label = "Best streak",
                primary = bestStreak.toString(),
                suffix = if (bestStreak == 1) "day" else "days",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StreakSummary(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    primary: String,
    suffix: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Ui2Colors.QuestEmpty),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Ui2Colors.Primary,
                modifier = Modifier.size(19.dp)
            )
        }
        Column {
            Text(
                text = label,
                color = Ui2Colors.Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = primary,
                    color = Ui2Colors.Primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = suffix,
                    color = Ui2Colors.Text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatStreakDuration(seconds: Long): String {
    val totalMinutes = (seconds / 60L).coerceAtLeast(1L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) {
        if (minutes > 0L) "${hours}h ${minutes}m" else "${hours}h"
    } else {
        "${minutes}m"
    }
}

@Composable
private fun StartWorkoutButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = com.rishabh.gymbros.ui.theme.GymBrosAccent.onAccent
    Row(
        modifier = modifier
            .shadow(
                elevation = 22.dp,
                shape = CircleShape,
                ambientColor = Ui2Colors.Primary.copy(alpha = 0.5f),
                spotColor = Ui2Colors.Primary.copy(alpha = 0.5f)
            )
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Ui2Colors.Primary, Ui2Colors.Cyan)))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 34.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Workout v2 keeps the original floating CTA style, but the dumbbell
        // icon was intentionally removed per the requested redesign.
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun StartWorkoutPickerDialog(
    layouts: List<WorkoutLayout>,
    onDismiss: () -> Unit,
    onExerciseLibrary: () -> Unit,
    onBlank: () -> Unit,
    onLayout: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Ui2Colors.CardRaised,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Start Workout",
                    color = Ui2Colors.Text,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = "Pick a saved template or start from an empty workout.",
                    color = Ui2Colors.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
                PickerCard(
                    title = "Start new workout",
                    subtitle = "Add exercises yourself",
                    onClick = onBlank,
                    accent = true
                )
                if (layouts.isNotEmpty()) {
                    Text(
                        text = "SAVED TEMPLATES",
                        color = Ui2Colors.Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                    layouts.forEach { layout ->
                        PickerCard(
                            title = layout.title,
                            subtitle = "${layout.exercises.size} exercise${if (layout.exercises.size == 1) "" else "s"}",
                            onClick = { onLayout(layout.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Ui2Colors.Muted)
            }
        }
    )
}

@Composable
private fun PickerCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    accent: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TrainSmallShape)
            .background(if (accent) Ui2Colors.Primary.copy(alpha = 0.12f) else Ui2Colors.Card)
            .border(
                1.dp,
                if (accent) Ui2Colors.Primary.copy(alpha = 0.35f) else Ui2Colors.CardOutline,
                TrainSmallShape
            )
            .clickable(onClick = onClick)
            .padding(13.dp)
    ) {
        Text(
            text = title,
            color = if (accent) Ui2Colors.Primary else Ui2Colors.Text,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = subtitle,
            color = Ui2Colors.Muted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private data class CompletionConfetti(
    val xFraction: Float,
    val yDp: Float,
    val color: Color,
    val sizeDp: Float,
    val shape: Int, // 0 = dot, 1 = bar, 2 = square
    val rotation: Float
)

private val CompletionConfettiPieces = listOf(
    CompletionConfetti(0.17f, 30f, Color(0xFF4DE1FF), 10f, 1, -30f),
    CompletionConfetti(0.29f, 14f, Color(0xFFA78BFA), 8f, 2, 20f),
    CompletionConfetti(0.10f, 80f, Color(0xFFFF5C8A), 8f, 0, 0f),
    CompletionConfetti(0.23f, 112f, Color(0xFF38E89F), 10f, 1, 35f),
    CompletionConfetti(0.35f, 132f, Color(0xFF4DE1FF), 6f, 0, 0f),
    CompletionConfetti(0.79f, 26f, Color(0xFFFF5C8A), 10f, 1, 25f),
    CompletionConfetti(0.70f, 14f, Color(0xFF4DE1FF), 8f, 0, 0f),
    CompletionConfetti(0.86f, 70f, Color(0xFFA78BFA), 8f, 2, -15f),
    CompletionConfetti(0.75f, 112f, Color(0xFF38E89F), 10f, 1, -35f),
    CompletionConfetti(0.63f, 134f, Color(0xFFA78BFA), 6f, 0, 0f),
    CompletionConfetti(0.41f, 10f, Color(0xFFFF5C8A), 5f, 0, 0f),
    CompletionConfetti(0.93f, 118f, Color(0xFF4DE1FF), 6f, 0, 0f)
)

@Composable
private fun CompletionCelebration(
    badgeScale: Float,
    ringProgress: Float,
    confettiProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {
        val cx = size.width / 2f
        val cy = 78.dp.toPx()
        val center = Offset(cx, cy)
        val mint = Ui2Colors.Primary

        // Soft glow rings that pulse outward as the badge lands.
        drawCircle(
            color = mint.copy(alpha = 0.07f * ringProgress),
            radius = 66.dp.toPx() * (0.6f + 0.4f * ringProgress),
            center = center
        )
        drawCircle(
            color = mint.copy(alpha = 0.13f * ringProgress),
            radius = 50.dp.toPx() * (0.6f + 0.4f * ringProgress),
            center = center
        )

        // Confetti drifts down into place and fades in.
        CompletionConfettiPieces.forEach { piece ->
            val px = piece.xFraction * size.width
            val py = (piece.yDp - (1f - confettiProgress) * 24f).dp.toPx()
            val s = piece.sizeDp.dp.toPx()
            val color = piece.color.copy(alpha = confettiProgress.coerceIn(0f, 1f))
            when (piece.shape) {
                0 -> drawCircle(color, radius = s / 2f, center = Offset(px, py))
                1 -> rotate(piece.rotation, pivot = Offset(px, py)) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(px - s / 2f, py - s / 4f),
                        size = Size(s, s / 2f),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
                else -> rotate(piece.rotation, pivot = Offset(px, py)) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(px - s / 2f, py - s / 2f),
                        size = Size(s, s),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
            }
        }

        // Badge + check mark, bouncing in.
        val radius = 36.dp.toPx() * badgeScale
        drawCircle(color = mint, radius = radius, center = center)
        val unit = badgeScale * 1.dp.toPx()
        val check = Path().apply {
            moveTo(cx - 16f * unit, cy + 1f * unit)
            lineTo(cx - 4f * unit, cy + 13f * unit)
            lineTo(cx + 18f * unit, cy - 12f * unit)
        }
        drawPath(
            path = check,
            color = Color(0xFF0A1713),
            style = Stroke(width = 7.dp.toPx() * badgeScale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun CompletionStatTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    tintBackground: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Ui2Colors.Background)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tintBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(
                text = value,
                color = Ui2Colors.Text,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1
            )
            Text(
                text = label,
                color = Ui2Colors.Muted,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WorkoutCompletionDialog(
    completion: WorkoutCompletionSummary,
    onSaveTemplate: (String) -> Unit,
    onUpdateTemplate: () -> Unit,
    onLayoutOnly: () -> Unit,
    onValuesOnly: () -> Unit,
    musicWasPlaying: Boolean,
    onStopMusic: () -> Unit,
    onDismiss: () -> Unit
) {
    var templateName by rememberSaveable(completion.sessionId) { mutableStateOf("") }
    // Celebration animation: badge bounce, glow rings, confetti, calorie count-up, haptic.
    val haptic = LocalHapticFeedback.current
    val badgeScale = remember(completion.sessionId) { Animatable(0f) }
    val ringProgress = remember(completion.sessionId) { Animatable(0f) }
    val confettiProgress = remember(completion.sessionId) { Animatable(0f) }
    var kcalTarget by remember(completion.sessionId) { mutableStateOf(0) }
    val kcalShown by animateIntAsState(
        targetValue = kcalTarget,
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "completionKcal"
    )
    LaunchedEffect(completion.sessionId) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        kcalTarget = completion.estimatedCalories
        launch {
            badgeScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        launch { ringProgress.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        launch {
            delay(150)
            confettiProgress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Ui2Colors.Card)
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompletionCelebration(
                badgeScale = badgeScale.value,
                ringProgress = ringProgress.value,
                confettiProgress = confettiProgress.value
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Workout complete",
                    color = Ui2Colors.Text,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { contentDescription = "Workout complete" }
                )
                Text(
                    text = if (completion.completedExercises >= completion.totalExercises) {
                        "All ${completion.totalExercises} exercises done. Saved to your history."
                    } else {
                        "${completion.completedExercises} of ${completion.totalExercises} exercises done. Saved to your history."
                    },
                    color = Ui2Colors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            // Hero calories
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        tint = WorkoutAccents.Pink,
                        modifier = Modifier.size(30.dp).padding(bottom = 4.dp)
                    )
                    Text(
                        text = "~$kcalShown",
                        color = Ui2Colors.Text,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "kcal",
                        color = Ui2Colors.Muted,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Text(
                    text = "Estimated calories burned",
                    color = Ui2Colors.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (musicWasPlaying) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TrainSmallShape)
                        .background(Ui2Colors.CardRaised)
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Stop music?",
                        color = Ui2Colors.Text,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onStopMusic) {
                        Text("Yes", color = Ui2Colors.Primary, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompletionStatTile(
                    icon = Icons.Outlined.DirectionsRun,
                    tint = Ui2Colors.Cyan,
                    tintBackground = WorkoutAccents.CyanTint,
                    value = formatLongDuration(completion.totalCardioSeconds),
                    label = "Total cardio",
                    modifier = Modifier.weight(1f)
                )
                CompletionStatTile(
                    icon = Icons.Outlined.Schedule,
                    tint = WorkoutAccents.Purple,
                    tintBackground = WorkoutAccents.PurpleTint,
                    value = formatLongDuration(completion.durationSeconds),
                    label = "Total time",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompletionStatTile(
                    icon = Icons.Outlined.DoneAll,
                    tint = Ui2Colors.Primary,
                    tintBackground = WorkoutAccents.MintTint,
                    value = "${completion.completedExercises}/${completion.totalExercises}",
                    label = "Exercises done",
                    modifier = Modifier.weight(1f)
                )
                CompletionStatTile(
                    icon = Icons.Outlined.EmojiEvents,
                    tint = WorkoutAccents.Pink,
                    tintBackground = WorkoutAccents.PinkTint,
                    value = completion.personalRecords.toString(),
                    label = "PRs today",
                    modifier = Modifier.weight(1f)
                )
            }

            if (completion.sourceLayoutId != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Ui2Colors.CardRaised)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Update & Save",
                            color = Ui2Colors.Text,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = completion.sourceLayoutTitle ?: "Saved layout",
                            color = Ui2Colors.Muted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        CompletionUpdateOption(
                            title = "Update\nValue",
                            subtitle = "Edit the last\nworkout values",
                            icon = Icons.Outlined.Bolt,
                            accent = WorkoutAccents.Purple,
                            onClick = {
                                onValuesOnly()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CompletionUpdateOption(
                            title = "Update\nLayout",
                            subtitle = "Save current\nexercise layout",
                            icon = Icons.Outlined.FitnessCenter,
                            accent = Ui2Colors.Primary,
                            onClick = {
                                onLayoutOnly()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CompletionUpdateOption(
                            title = "Update Value\n+ Layout",
                            subtitle = "Edit values and\nsave layout",
                            icon = Icons.Outlined.DoneAll,
                            accent = WorkoutAccents.Purple,
                            onClick = {
                                onUpdateTemplate()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else if (completion.sourceLayoutId == null) {
                Text(
                    text = "Save as template",
                    color = Ui2Colors.Text,
                    fontWeight = FontWeight.ExtraBold
                )
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Template name") },
                    singleLine = true,
                    shape = TrainSmallShape
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(
                            if (templateName.trim().isBlank()) {
                                Ui2Colors.CardRaised
                            } else {
                                Ui2Colors.Primary.copy(alpha = 0.12f)
                            }
                        )
                        .clickable(enabled = templateName.trim().isNotBlank()) {
                            onSaveTemplate(templateName.trim())
                            onDismiss()
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save template",
                        color = if (templateName.trim().isBlank()) {
                            Ui2Colors.Dim
                        } else {
                            Ui2Colors.Primary
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Calories are an estimate based on session duration and logged cardio, not a wearable measurement.",
                color = Ui2Colors.Dim,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Ui2Colors.Primary)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Done",
                    color = Color(0xFF0A1713),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MyStatsDialogV2(
    onDismiss: () -> Unit,
    streakDays: Int,
    totalPrs: Int,
    totalWorkouts: Int,
    totalDistanceKm: Double,
    totalCalories: Int,
    totalSeconds: Long,
    waterMl: Int,
    totalQuestsCompleted: Int,
    totalSongsPlayed: Int,
    preferences: android.content.SharedPreferences
) {
    val context = LocalContext.current
    val totalXp = GymBrosProgression.totalXp(context, totalWorkouts)
    val level = GymBrosProgression.level(totalXp)
    val levelProgress = GymBrosProgression.levelProgress(totalXp).coerceIn(0f, 1f)
    val xpIntoLevel = GymBrosProgression.xpIntoLevel(totalXp)
    val nextMilestone = when {
        totalQuestsCompleted < 10 -> 10
        totalQuestsCompleted < 25 -> 25
        totalQuestsCompleted < 50 -> 50
        else -> ((totalQuestsCompleted / 25) + 1) * 25
    }
    val previousMilestone = when (nextMilestone) { 10 -> 0; 25 -> 10; 50 -> 25; else -> nextMilestone - 25 }
    val questProgress = ((totalQuestsCompleted - previousMilestone).toFloat() / (nextMilestone - previousMilestone).coerceAtLeast(1)).coerceIn(0f, 1f)
    val mint = Color(0xFF38E89F); val cyan = Color(0xFF4DE1FF); val violet = Color(0xFF9B7BFF)
    val bg = Color(0xFF0A0B10); val text = Color(0xFFF5F7FA); val muted = Color(0xFF8B91A1)
    val edge = Color.White.copy(alpha = .08f)
    val glass = Brush.linearGradient(listOf(Color.White.copy(alpha=.075f), Color.White.copy(alpha=.02f)))
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        val dialogView = LocalView.current
        Column(
            Modifier.fillMaxWidth().heightIn(max = 760.dp).clip(RoundedCornerShape(30.dp))
                .background(bg).verticalScroll(scroll).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.width(42.dp).height(5.dp).clip(CircleShape).background(Color.White.copy(.18f)).align(Alignment.CenterHorizontally))

            // Level card — responsive version of the supplied mockup.
            // The streak pill is overlaid so it never steals horizontal space from the rank text.
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(glass).border(1.dp, edge, RoundedCornerShape(24.dp)).padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            val sw = 6.dp.toPx(); val inset = sw / 2
                            val sz = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw)
                            val tl = androidx.compose.ui.geometry.Offset(inset, inset)
                            drawArc(Color.White.copy(.08f), -90f, 360f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                            drawArc(Brush.linearGradient(listOf(mint, cyan)), -90f, 360f * levelProgress, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                        }
                        Icon(Icons.Outlined.Bolt, null, tint = mint, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "RANK",
                            color = muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.7.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "Level $level",
                            color = text,
                            fontSize = 19.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "$xpIntoLevel / ${GymBrosProgression.XP_PER_LEVEL} XP",
                            color = muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.TopEnd).clip(CircleShape)
                        .background(violet.copy(.14f)).border(1.dp, violet.copy(.30f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.LocalFireDepartment, null, tint = Color(0xFFC4B2FF), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "$streakDays day streak",
                        color = Color(0xFFC4B2FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            // Quest milestone card.
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
                    .background(Brush.linearGradient(listOf(violet.copy(.20f), violet.copy(.04f), Color.White.copy(.02f))))
                    .border(1.dp,edge,RoundedCornerShape(32.dp)).padding(20.dp)
            ) {
                Text(text = "QUEST MILESTONES", color = Color(0xFFC4B2FF), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                    modifier=Modifier.clip(CircleShape).background(violet.copy(.14f)).border(1.dp,violet.copy(.30f),CircleShape).padding(horizontal=12.dp,vertical=6.dp))
                Row(Modifier.padding(vertical=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Brush.linearGradient(listOf(violet,cyan))),contentAlignment=Alignment.Center) {
                        Icon(Icons.Outlined.Star,null,tint=Color.White,modifier=Modifier.size(30.dp))
                    }
                    Column { Text(text = "Daily quests", color = muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(text = totalQuestsCompleted.toString(), color = text, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 52.sp) }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    repeat(10) { i -> Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(if (i < (questProgress*10).toInt()) Brush.horizontalGradient(listOf(violet,cyan)) else Brush.linearGradient(listOf(Color.White.copy(.09f),Color.White.copy(.09f))))) }
                }
                Spacer(Modifier.height(14.dp)); Row { Text(text = "${(nextMilestone-totalQuestsCompleted).coerceAtLeast(0)} more", color = text, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(text = " to reach the next quest milestone", color = muted, fontSize = 13.sp) }
            }

            Text(text = "LIFETIME STATS", color = muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier =Modifier.padding(start=6.dp,top=8.dp))
            MyStatsGridRow(MyStat("$totalWorkouts","Workouts",Icons.Outlined.FitnessCenter,mint,Color.Transparent), MyStat("${formatDailyDecimal(totalDistanceKm)} km","Distance",Icons.Outlined.DirectionsRun,cyan,Color.Transparent))

            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(cyan.copy(.12f),Color.White.copy(.02f)))).border(1.dp,edge,RoundedCornerShape(26.dp)).padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(.06f)),contentAlignment=Alignment.Center){Icon(Icons.Outlined.LocalFireDepartment,null,tint=violet,modifier=Modifier.size(18.dp))}
                Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)){Text(text = buildAnnotatedString {
                withStyle(SpanStyle(color = text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)) { append("$totalCalories") }
                withStyle(SpanStyle(color = muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)) { append(" kcal") }
            });Text(text = "Cardio burned", color = muted, fontSize = 12.sp)}
                Canvas(Modifier.width(110.dp).height(44.dp)){ val p=androidx.compose.ui.graphics.Path(); p.moveTo(0f,size.height*.78f);p.cubicTo(size.width*.18f,size.height*.72f,size.width*.2f,size.height*.45f,size.width*.36f,size.height*.52f);p.cubicTo(size.width*.55f,size.height*.6f,size.width*.58f,size.height*.22f,size.width*.75f,size.height*.30f);p.cubicTo(size.width*.9f,size.height*.35f,size.width*.95f,size.height*.08f,size.width,size.height*.06f);drawPath(p,cyan,style=Stroke(2.5.dp.toPx(),cap=StrokeCap.Round)) }
            }
            MyStatsGridRow(MyStat(formatLongDuration(totalSeconds),"Time trained",Icons.Outlined.Schedule,mint,Color.Transparent), MyStat(if(waterMl>=1000) "${formatDailyDecimal(waterMl/1000.0)} L" else "$waterMl ml","Water drank",Icons.Outlined.WaterDrop,cyan,Color.Transparent))
            MyStatsGridRow(MyStat("$totalPrs","Total PR",Icons.Outlined.EmojiEvents,violet,Color.Transparent), MyStat("$totalSongsPlayed","Songs played",Icons.Outlined.MusicNote,mint,Color.Transparent))

            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(cyan.copy(.10f)).border(1.dp,cyan.copy(.25f),RoundedCornerShape(22.dp)).padding(horizontal=18.dp,vertical=15.dp),verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Outlined.TrendingUp,null,tint=cyan,modifier=Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Text(text = if(streakDays>0) "Keep the $streakDays-day streak moving." else "Complete a workout to start your streak.", color = cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                TextButton(onClick={ scope.launch { saveMyStatsFullScreenshot(context,dialogView,scroll,with(context.resources.displayMetrics){(58f*density).roundToInt()}) } }) { Icon(Icons.Outlined.Download,null,tint=mint);Spacer(Modifier.width(6.dp));Text("Save",color=mint) }
                TextButton(onClick=onDismiss){Text("Close",color=muted)}
            }
        }
    }
}

private suspend fun saveMyStatsFullScreenshot(
    context: android.content.Context,
    view: android.view.View,
    scrollState: androidx.compose.foundation.ScrollState,
    bottomCropPx: Int
) {
    val originalScroll = scrollState.value
    try {
        val viewportWidth = view.width.coerceAtLeast(1)
        val viewportHeight = view.height.coerceAtLeast(1)
        val maxScroll = scrollState.maxValue.coerceAtLeast(0)
        val fullHeight = viewportHeight + maxScroll

        // The My Stats content is a Compose scroll container. A normal View.draw()
        // only captures the currently visible viewport, so stitch each viewport
        // from scroll position 0 through maxValue into one full-height bitmap.
        val fullBitmap = Bitmap.createBitmap(viewportWidth, fullHeight, Bitmap.Config.ARGB_8888)
        val outputCanvas = AndroidCanvas(fullBitmap)
        var scrollY = 0

        while (true) {
            scrollState.scrollTo(scrollY)
            delay(32) // allow Compose to redraw at the requested scroll position

            val frame = Bitmap.createBitmap(viewportWidth, viewportHeight, Bitmap.Config.ARGB_8888)
            view.draw(AndroidCanvas(frame))

            val remaining = fullHeight - scrollY
            val copyHeight = minOf(viewportHeight, remaining)
            val srcTop = if (scrollY == maxScroll && maxScroll > 0) viewportHeight - copyHeight else 0
            val src = android.graphics.Rect(0, srcTop, viewportWidth, srcTop + copyHeight)
            val dst = android.graphics.Rect(0, scrollY, viewportWidth, scrollY + copyHeight)
            outputCanvas.drawBitmap(frame, src, dst, null)
            frame.recycle()

            if (scrollY >= maxScroll) break
            scrollY = minOf(scrollY + viewportHeight, maxScroll)
        }

        // Save/Close are UI controls, not part of the shareable stats card.
        // They are the final row in the scroll content, so remove that row only
        // from the exported bitmap while keeping the complete Level -> banner card.
        val exportHeight = (fullHeight - bottomCropPx).coerceAtLeast(1)
        val exportBitmap = Bitmap.createBitmap(fullBitmap, 0, 0, viewportWidth, exportHeight)
        if (exportBitmap !== fullBitmap) fullBitmap.recycle()

        val name = "GymBros_MyStats_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GymBros")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create image")
        context.contentResolver.openOutputStream(uri)?.use {
            exportBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        } ?: error("Could not open image output")
        exportBitmap.recycle()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }
        Toast.makeText(context, "Full My Stats card saved to Pictures/GymBros", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "Couldn't save My Stats image", Toast.LENGTH_SHORT).show()
    } finally {
        scrollState.scrollTo(originalScroll.coerceIn(0, scrollState.maxValue))
    }
}

private data class MyStat(
    val value: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color,
    val chip: Color
)

@Composable
private fun MyStatsGridRow(left: MyStat, right: MyStat) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MyStatsTile(left, Modifier.weight(1f))
        MyStatsTile(right, Modifier.weight(1f))
    }
}

@Composable
private fun MyStatsTile(stat: MyStat, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f))))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(stat.chip), contentAlignment = Alignment.Center) {
            Icon(stat.icon, stat.label, tint = stat.accent, modifier = Modifier.size(14.dp))
        }
        Column {
            Text(stat.value, color = Color.White, fontSize = 16.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(stat.label, color = Color(0xFF8A8A86), fontSize = 11.sp, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun SummaryMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Ui2Colors.Muted,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = Ui2Colors.Text,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun CompletionUpdateOption(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 112.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Ui2Colors.Background)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
        }
        Text(
            text = title,
            color = Ui2Colors.Text,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Text(
            text = subtitle,
            color = Ui2Colors.Muted,
            fontSize = 8.sp,
            lineHeight = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun TemplateDecision(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TrainSmallShape)
            .background(Ui2Colors.Card)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(title, color = Ui2Colors.Primary, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = Ui2Colors.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatLongDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hours = safe / 3600L
    val minutes = (safe % 3600L) / 60L
    val remainingSeconds = safe % 60L
    return when {
        hours > 0L -> "${hours}h ${minutes}m"
        minutes > 0L -> "${minutes}m ${remainingSeconds}s"
        else -> "${remainingSeconds}s"
    }
}

private fun formatClock(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val hours = safe / 3600L
    val minutes = (safe % 3600L) / 60L
    val seconds = safe % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(Locale.ENGLISH, hours, minutes, seconds)
    } else {
        "%02d:%02d".format(Locale.ENGLISH, minutes, seconds)
    }
}

private fun formatDurationMinutes(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes min"
        else -> {
            val hours = minutes / 60
            val remaining = minutes % 60
            "${hours}h ${remaining}m"
        }
    }
}