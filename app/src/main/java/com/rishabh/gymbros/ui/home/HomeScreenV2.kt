package com.rishabh.gymbros.ui.home

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rishabh.gymbros.R
import com.rishabh.gymbros.features.FeatureDefinition
import com.rishabh.gymbros.features.spotify.SpotifyManager
import com.rishabh.gymbros.features.spotify.SpotifyWebApiManager
import com.rishabh.gymbros.features.workout.data.WorkoutRepository
import com.rishabh.gymbros.features.workout.domain.WorkoutCalculations
import com.rishabh.gymbros.features.workout.domain.WorkoutAnalytics
import com.rishabh.gymbros.features.workout.model.WorkoutCategory
import com.rishabh.gymbros.features.workout.model.WorkoutExerciseType
import com.rishabh.gymbros.features.FeatureRegistry
import com.rishabh.gymbros.features.FeatureStatus
import com.rishabh.gymbros.features.progression.GymBrosProgression
import com.rishabh.gymbros.features.settings.model.AppSettings
import com.rishabh.gymbros.features.settings.model.formatVolume
import com.rishabh.gymbros.features.settings.model.volumeNumber
import com.rishabh.gymbros.features.settings.model.volumeUnitLabel
import com.rishabh.gymbros.features.water.model.WaterUiState
import com.rishabh.gymbros.navigation.AppDestination
import com.rishabh.gymbros.ui.components.AnimatedWaterBottle
import com.rishabh.gymbros.ui.components.IgniteCard
import com.rishabh.gymbros.ui.components.SectionHeader
import com.rishabh.gymbros.ui.theme.IgniteBrushes
import com.rishabh.gymbros.ui.theme.IgniteColors
import com.rishabh.gymbros.ui.theme.ObsidianSunsetBrushes
import com.rishabh.gymbros.ui.theme.ObsidianSunsetColors
import com.rishabh.gymbros.ui.theme.Ui2Colors
import com.rishabh.gymbros.ui.train.MyStatsDialogV2
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.spotify.protocol.types.Repeat
import android.widget.Toast
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun HomeScreenV2(
    waterState: WaterUiState,
    settings: AppSettings,
    onAddWater: (Int) -> Unit,
    onSubtractWater: (Int) -> Unit,
    onNavigate: (AppDestination) -> Unit,
    workoutMusicEnabled: Boolean? = null,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val context = LocalContext.current
    val homeLayoutPrefs = remember(context) {
        context.getSharedPreferences("gymbros_home_layout_v2", android.content.Context.MODE_PRIVATE)
    }
    var organizingHome by rememberSaveable { mutableStateOf(false) }
    var homeCardOrder by remember { mutableStateOf(loadHomeCardOrder(homeLayoutPrefs)) }
    fun moveHomeCard(cardId: String, direction: Int) {
        val current = homeCardOrder.toMutableList()
        val from = current.indexOf(cardId)
        if (from < 0) return
        val to = (from + direction).coerceIn(0, current.lastIndex)
        if (from == to) return
        current.removeAt(from)
        current.add(to, cardId)
        homeCardOrder = current
        saveHomeCardOrder(homeLayoutPrefs, current)
    }
    fun resetHomeCardOrder() {
        homeCardOrder = DEFAULT_HOME_CARD_ORDER
        saveHomeCardOrder(homeLayoutPrefs, DEFAULT_HOME_CARD_ORDER)
    }
    val workoutRepository = remember(context) { WorkoutRepository.getInstance(context.applicationContext) }
    val musicScope = rememberCoroutineScope()
    val workoutState by workoutRepository.state.collectAsState()
    val spotifyManager = remember(context) { SpotifyManager.getInstance(context.applicationContext) }
    val spotifyState by spotifyManager.state.collectAsState()
    val spotifyWebApiManager = remember(context) { SpotifyWebApiManager.getInstance(context.applicationContext) }
    val spotifyWebState by spotifyWebApiManager.state.collectAsState()
    var showSpotifyPlaylists by rememberSaveable { mutableStateOf(false) }
    var showSpotifySearch by rememberSaveable { mutableStateOf(false) }
    var showSpotifyWorkoutPlayer by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(spotifyWebState.selectedPlaylist?.uri) {
        spotifyManager.setContinuationPlaylist(spotifyWebState.selectedPlaylist?.uri)
    }
    LaunchedEffect(spotifyState.trackUri, spotifyWebState.selectedPlaylist?.id, spotifyWebState.authorized, showSpotifyWorkoutPlayer) {
        spotifyWebApiManager.refreshTrackMembership(spotifyState.trackUri)
    }
    var showDailyTasksDialog by rememberSaveable { mutableStateOf(false) }
    var showMyStatsDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(workoutState.isWorkoutActive) {
        if (workoutState.isWorkoutActive) {
            spotifyManager.reconnectIfAuthorized(context)
        } else {
            showSpotifyWorkoutPlayer = false
        }
    }
    LaunchedEffect(spotifyWebState.actionMessage) {
        val message = spotifyWebState.actionMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        spotifyWebApiManager.clearActionMessage()
    }
    val trainPreferences = remember(context) { context.getSharedPreferences("gymbros_train_preferences", android.content.Context.MODE_PRIVATE) }
    val todayWorkoutSessions = WorkoutCalculations.sessionsForDay(workoutState.sessions, today)
    val todayWorkoutMinutes = (todayWorkoutSessions.sumOf { it.durationSeconds } / 60L).toInt()
    val todayExerciseCount = todayWorkoutSessions.sumOf { it.completedExercises }
    val todaySessionIds = todayWorkoutSessions.map { it.id }.toSet()
    val todayCardioCalories = todaySessionIds.sumOf { sessionId ->
        workoutState.sessionMetrics[sessionId]?.get(WorkoutRepository.METRIC_CARDIO_CALORIES) ?: 0.0
    }.roundToInt()

    // Full-workout mode includes cardio + time-only work + the non-cardio portion
    // of the session. Some older sessions can contain an estimated_calories value
    // that is effectively cardio-only, so do not blindly trust that value when it
    // is indistinguishable from the stored cardio calories.
    val completedFullWorkoutCalories = todayWorkoutSessions.sumOf { session ->
        val metrics = workoutState.sessionMetrics[session.id].orEmpty()
        val cardioCalories = (metrics[WorkoutRepository.METRIC_CARDIO_CALORIES] ?: 0.0).coerceAtLeast(0.0)
        val cardioSeconds = (metrics[WorkoutRepository.METRIC_TOTAL_CARDIO_SECONDS] ?: 0.0).toLong().coerceIn(0L, session.durationSeconds)
        val timeOnlySeconds = metrics.entries
            .filter { (key, _) -> key.startsWith("time_only_") && key.endsWith("_seconds") }
            .sumOf { (_, value) -> value.toLong().coerceAtLeast(0L) }
            .coerceIn(0L, (session.durationSeconds - cardioSeconds).coerceAtLeast(0L))
        val weightKg = (settings.weightKg?.toDouble() ?: 70.0).coerceIn(20.0, 500.0)
        fun activeCalories(met: Double, seconds: Long): Double =
            if (seconds <= 0L) 0.0 else (met - 1.0).coerceAtLeast(0.0) * 3.5 * weightKg / 200.0 * (seconds / 60.0)

        val nonCardioSeconds = (session.durationSeconds - cardioSeconds - timeOnlySeconds).coerceAtLeast(0L)
        val nonCardioMet = if (session.category == WorkoutCategory.STRENGTH) 4.5 else 2.0
        val reconstructed = cardioCalories + activeCalories(4.0, timeOnlySeconds) + activeCalories(nonCardioMet, nonCardioSeconds)
        val stored = (metrics[WorkoutRepository.METRIC_ESTIMATED_CALORIES] ?: 0.0).coerceAtLeast(0.0)
        // Never let a strength session collapse to the cardio-only number. The reconstructed
        // value is based on the actual session duration; stored estimate remains a safety floor.
        maxOf(reconstructed, stored)
    }.roundToInt()

    val todayCardioKm = todaySessionIds.sumOf { sessionId ->
        workoutState.sessionMetrics[sessionId]?.get(WorkoutRepository.METRIC_DISTANCE_KM) ?: 0.0
    }

    val durationSeconds = todayWorkoutSessions.sumOf { it.durationSeconds }
    var calorieGoal by rememberSaveable { mutableStateOf(trainPreferences.getInt("daily_calorie_goal", 600).coerceAtLeast(1)) }
    var cardioGoalKm by rememberSaveable { mutableStateOf(trainPreferences.getFloat("daily_cardio_goal_km", 6f).toDouble().coerceAtLeast(0.1)) }
    var timeGoalMinutes by rememberSaveable { mutableStateOf(trainPreferences.getInt("daily_time_goal_minutes", 120).coerceIn(1, 1440)) }
    var trackFullWorkoutCalories by rememberSaveable {
        mutableStateOf(trainPreferences.getBoolean("daily_calorie_track_full_workout", false))
    }

    // Keep Full workout calories moving while a workout is in progress instead of
    // waiting until Finish Workout writes the final session metrics.
    var activeCalorieClockMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(workoutState.activeStartedAt, trackFullWorkoutCalories) {
        if (workoutState.activeStartedAt != null && trackFullWorkoutCalories) {
            while (true) {
                activeCalorieClockMillis = System.currentTimeMillis()
                delay(15_000L)
            }
        }
    }
    val activeFullWorkoutCalories = workoutState.activeStartedAt?.let { startedAt ->
        val activeDate = Instant.ofEpochMilli(startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        if (activeDate != today || !trackFullWorkoutCalories) {
            0
        } else {
            val elapsedSeconds = ((activeCalorieClockMillis - startedAt) / 1000L).coerceAtLeast(0L)
            val weightKg = (settings.weightKg?.toDouble() ?: 70.0).coerceIn(20.0, 500.0)
            val activeTypes = workoutState.activeExercises.map { it.exerciseType }.toSet()
            val met = when {
                WorkoutExerciseType.STRENGTH in activeTypes -> 4.5
                WorkoutExerciseType.CARDIO_DISTANCE in activeTypes || WorkoutExerciseType.CARDIO_ROPE in activeTypes -> 6.0
                WorkoutExerciseType.TIME_ONLY in activeTypes -> 4.0
                else -> 3.5
            }
            (((met - 1.0).coerceAtLeast(0.0) * 3.5 * weightKg / 200.0) * (elapsedSeconds / 60.0))
                .roundToInt()
                .coerceAtLeast(0)
        }
    } ?: 0
    val todayFullWorkoutCalories = completedFullWorkoutCalories + activeFullWorkoutCalories
    val todayMoveCalories = if (trackFullWorkoutCalories) todayFullWorkoutCalories else todayCardioCalories
    val completionProgress = (durationSeconds / (timeGoalMinutes.toFloat() * 60f)).coerceIn(0f, 1f)
    val completionPercent = (completionProgress * 100f).roundToInt()
    var showMoveSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var achievementXp by remember { mutableStateOf(GymBrosProgression.achievementXp(context)) }
    val totalDistanceKm = workoutState.sessionMetrics.values.sumOf { it[WorkoutRepository.METRIC_DISTANCE_KM] ?: 0.0 }
    val todayKey = today.toString()
    val isRestDayToday = today.toEpochDay() in workoutState.restDays
    val todayPrCount = todaySessionIds.sumOf { sessionId ->
        (workoutState.sessionMetrics[sessionId]?.get(WorkoutRepository.METRIC_PR_COUNT) ?: 0.0).roundToInt()
    }
    val startedWorkoutToday = workoutState.isWorkoutActive || todayWorkoutSessions.isNotEmpty()
    val finishedWorkoutToday = todayWorkoutSessions.isNotEmpty()
    val todaySongsPlayed = todaySessionIds.sumOf { sessionId ->
        (workoutState.sessionMetrics[sessionId]?.get(WorkoutRepository.METRIC_SONGS_PLAYED) ?: 0.0).roundToInt()
    }
    val todayReps = todaySessionIds.sumOf { sessionId ->
        workoutState.sessionSetLogs[sessionId].orEmpty()
            .filter { it.completed }
            .sumOf { it.reps.coerceAtLeast(0) }
    }
    val completedAllExercisesToday = todayWorkoutSessions.any { it.totalExercises > 0 && it.completedExercises >= it.totalExercises }
    val dailyQuestPrefs = remember(context) { context.getSharedPreferences("gymbros_daily_quests", android.content.Context.MODE_PRIVATE) }
    val savedQuestIds = dailyQuestPrefs.getString("quest_ids_$todayKey", null)
        ?.split(',')?.filter { it.isNotBlank() }?.takeIf { it.size == 4 }
    val progressionPrefs = remember(context) { context.getSharedPreferences("gymbros_progression", android.content.Context.MODE_PRIVATE) }
    val legacyCompletedToday = remember(todayKey) {
        progressionPrefs.all.count { (key, value) -> key.startsWith("daily_${todayKey}_") && value == true } >= 4
    }
    val generatedDailyTasks = if (isRestDayToday) {
        listOf(HomeDailyTaskV2("rest_day", "Enjoy your rest day", "Recovery is part of the work.", 500, 1, 1))
    } else {
        buildDailyQuestSet(
            dateKey = todayKey,
            selectedIds = savedQuestIds,
            startedWorkout = startedWorkoutToday,
            finishedWorkout = finishedWorkoutToday,
            waterMl = waterState.intakeMl,
            waterGoalMl = waterState.goalMl,
            prCount = todayPrCount,
            cardioKm = todayCardioKm,
            calories = todayCardioCalories,
            workoutMinutes = todayWorkoutMinutes,
            completedExercises = todayExerciseCount,
            totalReps = todayReps,
            songsPlayed = todaySongsPlayed,
            completedAllExercises = completedAllExercisesToday
        )
    }
    val recoveryDateKey = "2026-09-26"
    val needsFourthQuestRecovery = todayKey == recoveryDateKey && !isRestDayToday && generatedDailyTasks.size == 4 &&
            generatedDailyTasks.take(3).all { it.progress >= it.target } && generatedDailyTasks[3].progress < generatedDailyTasks[3].target
    val dailyTasks = when {
        legacyCompletedToday && savedQuestIds == null && !isRestDayToday -> generatedDailyTasks.map { it.copy(progress = it.target) }
        needsFourthQuestRecovery -> generatedDailyTasks.mapIndexed { index, task -> if (index == 3) task.copy(progress = task.target) else task }
        else -> generatedDailyTasks
    }

    LaunchedEffect(todayKey, needsFourthQuestRecovery) {
        if (needsFourthQuestRecovery) {
            // Recovery only: preserve the completion that was lost during the quest migration.
            // Mark it claimed so this repair never awards XP a second time.
            progressionPrefs.edit().putBoolean("daily_${todayKey}_${generatedDailyTasks[3].id}", true).apply()
        }
    }

    LaunchedEffect(todayKey, isRestDayToday, generatedDailyTasks.map { it.id }) {
        if (!isRestDayToday && savedQuestIds == null && generatedDailyTasks.size == 4) {
            dailyQuestPrefs.edit().putString("quest_ids_$todayKey", generatedDailyTasks.joinToString(",") { it.id }).apply()
            if (legacyCompletedToday) {
                val editor = progressionPrefs.edit()
                generatedDailyTasks.forEach { editor.putBoolean("daily_${todayKey}_${it.id}", true) }
                editor.apply()
            }
        }
    }
    LaunchedEffect(todayKey, isRestDayToday, dailyTasks.map { it.progress }) {
        if (!(legacyCompletedToday && savedQuestIds == null && !isRestDayToday)) {
            dailyTasks.filter { it.progress >= it.target }.forEach { task ->
                GymBrosProgression.awardDailyTaskOnce(context, todayKey, task.id, task.xp)
            }
        }
    }
    val totalXp = GymBrosProgression.totalXp(context, workoutState.sessions.size)

    // Read the Progress page's existing saved weight data; no Progress storage is changed.
    val progressPrefs = remember(context) {
        context.getSharedPreferences("gymbros_progress", android.content.Context.MODE_PRIVATE)
    }
    val progressStartWeight = progressPrefs.getString("goal_current_weight_v1", "")?.toDoubleOrNull()
    val progressGoalWeight = progressPrefs.getString("goal_target_weight_v1", "")?.toDoubleOrNull()
    val progressCheckIns = progressPrefs.getStringSet("checkins_v1", emptySet()).orEmpty()
        .mapNotNull { raw ->
            val parts = raw.split('\t')
            if (parts.size < 3) null else {
                val day = parts[1].toLongOrNull()
                val weight = parts[2].toDoubleOrNull()
                if (day == null || weight == null) null else day to weight
            }
        }
    val progressCurrentWeight = progressCheckIns.maxByOrNull { it.first }?.second ?: progressStartWeight
    val weightGoalProgress = when {
        progressStartWeight == null || progressGoalWeight == null || progressCurrentWeight == null -> 0f
        kotlin.math.abs(progressGoalWeight - progressStartWeight) < 0.001 -> 1f
        else -> {
            val moved = if (progressGoalWeight < progressStartWeight) {
                progressStartWeight - progressCurrentWeight
            } else {
                progressCurrentWeight - progressStartWeight
            }
            (moved / kotlin.math.abs(progressGoalWeight - progressStartWeight)).toFloat().coerceIn(0f, 1f)
        }
    }

    val weekStart = today.with(DayOfWeek.MONDAY)
    val weekDays = remember(workoutState.sessions, workoutState.restDays, today) {
        (0L..6L).map { offset ->
            val day = weekStart.plusDays(offset)
            val seconds = WorkoutCalculations.completedSessionsForDay(workoutState.sessions, day).sumOf { it.durationSeconds }
            WorkoutStreakDayV2(day, seconds, day.toEpochDay() in workoutState.restDays)
        }
    }
    val bestStreak = remember(workoutState.sessions, workoutState.restDays) {
        WorkoutCalculations.bestStreakDays(workoutState.sessions, restDays = workoutState.restDays)
    }

    com.rishabh.gymbros.ui.theme.Ui2Theme {
        if (showMoveSettingsDialog) {
            var maxTimeText by rememberSaveable(timeGoalMinutes) { mutableStateOf(timeGoalMinutes.toString()) }
            var maxKmText by rememberSaveable(cardioGoalKm) { mutableStateOf(formatDailyDecimal(cardioGoalKm)) }
            var maxCalText by rememberSaveable(calorieGoal) { mutableStateOf(calorieGoal.toString()) }
            var useFullWorkoutCalories by rememberSaveable { mutableStateOf(trackFullWorkoutCalories) }

            AlertDialog(
                onDismissRequest = { showMoveSettingsDialog = false },
                containerColor = Ui2Colors.Card,
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Today's Move", color = Ui2Colors.Text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Box(Modifier.clip(CircleShape).background(Ui2Colors.Primary.copy(alpha = .14f)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Text("DAILY TARGETS", color = Ui2Colors.Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("Customize rings and metric thresholds.", color = Ui2Colors.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = maxTimeText,
                                onValueChange = { maxTimeText = it.filter(Char::isDigit).take(4) },
                                label = { Text("Time", color = Ui2Colors.Lavender) },
                                suffix = { Text("min", color = Ui2Colors.Muted, fontSize = 10.sp) },
                                singleLine = true, modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = maxKmText,
                                onValueChange = { input ->
                                    maxKmText = input.filter { it.isDigit() || it == '.' }.let { filtered ->
                                        val firstDot = filtered.indexOf('.')
                                        if (firstDot >= 0) filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "") else filtered
                                    }.take(5)
                                },
                                label = { Text("Distance", color = Ui2Colors.Cyan) },
                                suffix = { Text("km", color = Ui2Colors.Muted, fontSize = 10.sp) },
                                singleLine = true, modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = maxCalText,
                                onValueChange = { maxCalText = it.filter(Char::isDigit).take(4) },
                                label = { Text("Calories", color = Ui2Colors.Danger) },
                                suffix = { Text("cal", color = Ui2Colors.Muted, fontSize = 10.sp) },
                                singleLine = true, modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            "Calories Calculation",
                            color = Ui2Colors.Text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (!useFullWorkoutCalories) Ui2Colors.Primary.copy(alpha = .14f) else Ui2Colors.CardRaised)
                                    .border(
                                        1.dp,
                                        if (!useFullWorkoutCalories) Ui2Colors.Primary.copy(alpha = .55f) else Ui2Colors.CardOutline,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { useFullWorkoutCalories = false }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Cardio only",
                                    color = if (!useFullWorkoutCalories) Ui2Colors.Primary else Ui2Colors.Muted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (useFullWorkoutCalories) Ui2Colors.Primary.copy(alpha = .14f) else Ui2Colors.CardRaised)
                                    .border(
                                        1.dp,
                                        if (useFullWorkoutCalories) Ui2Colors.Primary.copy(alpha = .55f) else Ui2Colors.CardOutline,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { useFullWorkoutCalories = true }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Full workout",
                                    color = if (useFullWorkoutCalories) Ui2Colors.Primary else Ui2Colors.Muted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Text(
                            if (useFullWorkoutCalories) "Cal ring uses estimated calories from the entire completed workout."
                            else "Cal ring uses cardio calories only.",
                            color = Ui2Colors.Muted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val newTime = maxTimeText.toIntOrNull()?.coerceIn(1, 1440) ?: timeGoalMinutes
                        val newKm = maxKmText.toDoubleOrNull()?.coerceIn(0.1, 999.0) ?: cardioGoalKm
                        val newCal = maxCalText.toIntOrNull()?.coerceIn(1, 9999) ?: calorieGoal
                        timeGoalMinutes = newTime
                        cardioGoalKm = newKm
                        calorieGoal = newCal
                        trackFullWorkoutCalories = useFullWorkoutCalories
                        trainPreferences.edit()
                            .putInt("daily_time_goal_minutes", newTime)
                            .putFloat("daily_cardio_goal_km", newKm.toFloat())
                            .putInt("daily_calorie_goal", newCal)
                            .putBoolean("daily_calorie_track_full_workout", useFullWorkoutCalories)
                            .apply()
                        showMoveSettingsDialog = false
                    }) {
                        Text("Save Target", color = Ui2Colors.Primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMoveSettingsDialog = false }) { Text("Cancel") }
                }
            )
        }


    }

    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val displayName = settings.userName.trim()
    val greetingText = if (displayName.isBlank()) greeting else "$greeting, $displayName"
    val dateText = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH))

    if (showSpotifyPlaylists) {
        SpotifyPlaylistPicker(
            state = spotifyWebState,
            onDismiss = { showSpotifyPlaylists = false },
            onAuthorize = { spotifyWebApiManager.openPlaylistAuthorization(context) },
            onRefresh = spotifyWebApiManager::fetchPlaylists,
            onPlaylistSelected = { playlist ->
                spotifyWebApiManager.selectPlaylist(playlist)
                showSpotifyPlaylists = false
                // If music is already playing, changing the selected playlist must also
                // change Spotify's active playback context. Otherwise Spotify continues
                // the old playlist when the current track ends.
                if (!spotifyManager.isSearchTrackActive && spotifyState.connected && !spotifyState.isPaused) {
                    spotifyManager.play(playlist.uri)
                }
            }
        )
    }

    if (showSpotifyWorkoutPlayer) {
        SpotifyWorkoutFullPlayer(
            state = spotifyState,
            selectedPlaylist = spotifyWebState.selectedPlaylist,
            onDismiss = { showSpotifyWorkoutPlayer = false },
            onPlaySelectedPlaylist = {
                spotifyWebState.selectedPlaylist?.let { playlist -> spotifyManager.play(playlist.uri) }
            },
            onPrevious = spotifyManager::previous,
            onTogglePlayPause = spotifyManager::togglePlayPause,
            onNext = spotifyManager::next,
            onSearch = { showSpotifySearch = true },
            onSeek = spotifyManager::seek,
            onToggleShuffle = spotifyManager::toggleShuffle,
            onToggleRepeat = spotifyManager::toggleRepeat,
            songAdded = spotifyState.trackUri != null && spotifyWebState.checkedTrackUri == spotifyState.trackUri && spotifyWebState.checkedPlaylistId == spotifyWebState.selectedPlaylist?.id && spotifyWebState.trackSaved == true,
            addSongLoading = spotifyWebState.addSongLoading || spotifyWebState.membershipLoading,
            onAddSong = { spotifyState.trackUri?.let { spotifyWebApiManager.toggleTrackInSelectedPlaylist(context, it) } }
        )
    }

    if (showSpotifySearch) {
        SpotifyTrackSearchSheet(
            state = spotifyWebState,
            player = spotifyState,
            onClearSearch = spotifyWebApiManager::clearTrackSearch,
            onClearRecent = spotifyWebApiManager::clearRecentSearchTracks,
            onStopPreview = spotifyManager::stopPreview,
            onQueue = { track ->
                spotifyManager.addToQueue(track.uri)
                Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showSpotifySearch = false
                spotifyWebApiManager.clearTrackSearch()
            },
            onSearch = spotifyWebApiManager::searchTracks,
            onAuthorize = { spotifyWebApiManager.openPlaylistAuthorization(context) },
            onSelect = { track ->
                musicScope.launch {
                    val startedAt = workoutState.activeStartedAt
                    val attached = if (startedAt != null) {
                        workoutRepository.attachMusicTrack(startedAt, track.uri, track.name, track.artistName)
                    } else false
                    spotifyWebApiManager.rememberSearchedTrack(track)
                    spotifyManager.playSearchTrack(context, track.uri, spotifyWebState.selectedPlaylist?.uri)
                    showSpotifySearch = false
                    spotifyWebApiManager.clearTrackSearch()
                    if (!attached) Toast.makeText(context, "Playing track. Start a workout to save music in its history.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Box(
        modifier = modifier.background(Ui2Colors.Background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = if (workoutState.isWorkoutActive) 104.dp else 0.dp)
        ) {
            item {
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greetingText,
                            color = Ui2Colors.Text,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dateText.uppercase(Locale.ENGLISH),
                            color = Ui2Colors.Muted,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    HomeProfileAvatar(
                        profileImageUri = settings.profileImageUri,
                        questProgress = if (dailyTasks.isEmpty()) 0f else dailyTasks.count { it.progress >= it.target }.toFloat() / dailyTasks.size.toFloat(),
                        level = GymBrosProgression.level(totalXp),
                        onClick = { showMyStatsDialog = true }
                    )
                }

                Spacer(Modifier.height(16.dp))
                homeCardOrder.forEachIndexed { index, cardId ->
                    ReorderableHomeCard(
                        cardId = cardId,
                        organizing = false,
                        canMoveUp = index > 0,
                        canMoveDown = index < homeCardOrder.lastIndex,
                        onMoveUp = { moveHomeCard(cardId, -1) },
                        onMoveDown = { moveHomeCard(cardId, 1) }
                    ) {
                        when (cardId) {
                            HOME_CARD_DAILY_ACTIVITY -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                BestWorkoutStreakCardV2(bestStreak = bestStreak)
                                TodaysMoveCard(
                                    caloriesDone = todayMoveCalories,
                                    caloriesGoal = calorieGoal,
                                    cardioKm = todayCardioKm.toFloat(),
                                    cardioGoalKm = cardioGoalKm.toFloat(),
                                    minutesDone = todayWorkoutMinutes,
                                    minutesGoal = timeGoalMinutes,
                                    isRestDay = isRestDayToday,
                                    onCenterClick = { showMoveSettingsDialog = true }
                                )
                            }
                            HOME_CARD_STREAK_WATER -> ProgressWaterRowV2(
                                progress = weightGoalProgress,
                                currentWeight = progressCurrentWeight,
                                goalWeight = progressGoalWeight,
                                state = waterState,
                                quickAddMl = settings.widgetButtonMl,
                                onAddWater = { onAddWater(settings.widgetButtonMl) },
                                onSubtractWater = { onSubtractWater(settings.widgetButtonMl) },
                                onOpenProgress = { onNavigate(AppDestination.PROGRESS) }
                            )
                            HOME_CARD_DAILY_STREAK -> DailyQuestCardV2(
                                items = dailyTasks,
                                onClick = { showDailyTasksDialog = true }
                            )
                            HOME_CARD_SPOTIFY -> {
                                if (!workoutState.isWorkoutActive || workoutMusicEnabled == false) {
                                    SpotifyPreWorkoutCard(
                                        state = spotifyState,
                                        selectedPlaylist = spotifyWebState.selectedPlaylist,
                                        onSelectPlaylist = {
                                            showSpotifyPlaylists = true
                                            if (spotifyWebState.authorized && spotifyWebState.playlists.isEmpty() && !spotifyWebState.loading) {
                                                spotifyWebApiManager.fetchPlaylists()
                                            }
                                        },
                                        onPlaySelectedPlaylist = {
                                            spotifyWebState.selectedPlaylist?.let { playlist -> spotifyManager.play(playlist.uri) }
                                        },
                                        onSearch = { showSpotifySearch = true },
                                        onPlayPause = {
                                            if (spotifyState.connected) {
                                                if (spotifyState.trackUri != null) spotifyManager.togglePlayPause()
                                                else spotifyWebState.selectedPlaylist?.let { spotifyManager.play(it.uri) } ?: spotifyManager.resume()
                                            } else {
                                                spotifyManager.connect(context)
                                            }
                                        },
                                        onOpen = { if (spotifyState.connected && spotifyState.trackUri != null) showSpotifyWorkoutPlayer = true }
                                    )
                                }
                            }
                            HOME_CARD_TOOLS -> ToolsSectionV2(onNavigate = onNavigate)
                        }
                    }
                    if (index < homeCardOrder.lastIndex) Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Ui2Colors.CardRaised)
                            .border(1.dp, Ui2Colors.CardOutline, CircleShape)
                            .clickable { organizingHome = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = "Organize Home",
                            tint = Ui2Colors.Lime,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        if (workoutState.isWorkoutActive && workoutMusicEnabled != false) {
            SpotifyWorkoutMiniPlayer(
                state = spotifyState,
                selectedPlaylist = spotifyWebState.selectedPlaylist,
                onPrevious = spotifyManager::previous,
                onTogglePlayPause = spotifyManager::togglePlayPause,
                onNext = spotifyManager::next,
                onToggleShuffle = spotifyManager::toggleShuffle,
                onToggleRepeat = spotifyManager::toggleRepeat,
                songAdded = spotifyState.trackUri != null && spotifyWebState.checkedTrackUri == spotifyState.trackUri && spotifyWebState.checkedPlaylistId == spotifyWebState.selectedPlaylist?.id && spotifyWebState.trackSaved == true,
                addSongLoading = spotifyWebState.addSongLoading || spotifyWebState.membershipLoading,
                onAddSong = { spotifyState.trackUri?.let { spotifyWebApiManager.toggleTrackInSelectedPlaylist(context, it) } },
                onOpen = { showSpotifyWorkoutPlayer = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }

    if (showDailyTasksDialog) {
        DailyTasksDialog(
            items = dailyTasks,
            dateKey = todayKey,
            context = context,
            isRestDay = isRestDayToday,
            onDismiss = { showDailyTasksDialog = false }
        )
    }

    if (showMyStatsDialog) {
        val totalPrs = WorkoutAnalytics.personalRecords(workoutState.sessions, workoutState.sessionSetLogs).size
        val totalCaloriesAll = workoutState.sessionMetrics.values.sumOf { it[WorkoutRepository.METRIC_CARDIO_CALORIES] ?: 0.0 }.roundToInt()
        val totalSecondsAll = workoutState.sessions.sumOf { it.durationSeconds }
        val totalSongsPlayed = workoutState.sessionMetrics.values.sumOf { (it[WorkoutRepository.METRIC_SONGS_PLAYED] ?: 0.0).toInt() }
        MyStatsDialogV2(
            onDismiss = { showMyStatsDialog = false },
            streakDays = WorkoutCalculations.streakDays(workoutState.sessions, today, restDays = workoutState.restDays),
            totalPrs = totalPrs,
            totalWorkouts = workoutState.sessions.size,
            totalDistanceKm = totalDistanceKm,
            totalCalories = totalCaloriesAll,
            totalSeconds = totalSecondsAll,
            waterMl = waterState.lifetimeIntakeMl,
            totalQuestsCompleted = GymBrosProgression.totalDailyTasksCompleted(context),
            totalSongsPlayed = totalSongsPlayed,
            preferences = trainPreferences
        )
    }



    if (organizingHome) {
        HomeOrganizerDialog(
            order = homeCardOrder,
            onMove = { cardId, direction -> moveHomeCard(cardId, direction) },
            onReset = { resetHomeCardOrder() },
            onDone = { organizingHome = false }
        )
    }
}

@Composable
private fun HomeOrganizerDialog(
    order: List<String>,
    onMove: (String, Int) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit
) {
    Dialog(
        onDismissRequest = onDone,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .72f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDone
                )
                .padding(horizontal = 20.dp, vertical = 54.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Ui2Colors.Card)
                    .border(1.dp, Ui2Colors.CardOutline, RoundedCornerShape(24.dp))
                    .clickable { }
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Organize Home",
                            color = Ui2Colors.Text,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Long-press ☰ and drag. Other cards move out of the way.",
                            color = Ui2Colors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    TextButton(onClick = onDone) {
                        Text("Done", color = Ui2Colors.Lime, fontWeight = FontWeight.Bold)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(order, key = { it }) { cardId ->
                        // Live tracking during an active drag is a PLAIN state, updated
                        // synchronously in onDrag — one pointer-move in, one number out, no
                        // queueing. settleOffset is only ever touched once per gesture (on
                        // release/cancel) to spring any leftover offset back to 0, so it's fine
                        // for that one case to be a coroutine-driven Animatable.
                        var dragDistance by remember(cardId) { mutableStateOf(0f) }
                        val settleOffset = remember(cardId) { Animatable(0f) }
                        var dragging by remember(cardId) { mutableStateOf(false) }
                        val dragScope = rememberCoroutineScope()
                        val density = LocalDensity.current
                        // A little over half a row's height — deliberate enough that small
                        // finger jitter doesn't reorder things, but still responsive.
                        val swapThresholdPx = remember(density) { with(density) { 34.dp.toPx() } }
                        // Read the live order/index from inside the gesture without making the
                        // pointerInput depend on them — see the pointerInput block below for why.
                        val latestOrder = rememberUpdatedState(order)
                        val label = when (cardId) {
                            HOME_CARD_DAILY_ACTIVITY -> "Streak + Today's move"
                            HOME_CARD_STREAK_WATER -> "Progress + Water"
                            HOME_CARD_DAILY_STREAK -> "Daily Quests"
                            HOME_CARD_SPOTIFY -> "Spotify"
                            HOME_CARD_TOOLS -> "Your Tools"
                            else -> cardId
                        }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                // While this card is being dragged we position it ourselves via
                                // translationY below, so we skip the auto placement animation for
                                // it. Every OTHER card still animates into its new slot, which is
                                // what produces the "cards glide out of the way" motion.
                                .animateItem(
                                    placementSpec = if (dragging) null else spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                .zIndex(if (dragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = dragDistance + settleOffset.value
                                    scaleX = if (dragging) 1.02f else 1f
                                    scaleY = if (dragging) 1.02f else 1f
                                    shadowElevation = if (dragging) 18f else 0f
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (dragging) Ui2Colors.CardRaised.copy(alpha = .98f) else Ui2Colors.CardRaised)
                                .border(
                                    1.dp,
                                    if (dragging) Ui2Colors.Lime.copy(alpha = .55f) else Ui2Colors.CardOutline,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                color = Ui2Colors.Text,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                Modifier
                                    .size(width = 64.dp, height = 54.dp)
                                    // Key on cardId ONLY. Keying on index/order.size (as this used
                                    // to) restarts this pointerInput block the instant a swap
                                    // happens mid-drag, which cancels the in-flight gesture and
                                    // fires onDragCancel — that's what was deselecting the card
                                    // and ending the drag after a single move. cardId never
                                    // changes for a given item, so the gesture now survives the
                                    // whole drag session.
                                    .pointerInput(cardId) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                dragging = true
                                                dragDistance = 0f
                                                dragScope.launch { settleOffset.snapTo(0f) }
                                            },
                                            onDragEnd = {
                                                dragging = false
                                                // Spring the leftover offset back to 0 instead of
                                                // snapping, so the drop looks like a settle rather
                                                // than a jump-cut. This is the ONLY coroutine
                                                // launched in this whole gesture, and it only ever
                                                // runs once per release — so there's nothing left
                                                // to race with dragDistance updates above.
                                                val leftover = dragDistance
                                                dragDistance = 0f
                                                dragScope.launch {
                                                    settleOffset.snapTo(leftover)
                                                    settleOffset.animateTo(
                                                        0f,
                                                        spring(
                                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                                            stiffness = Spring.StiffnessMedium
                                                        )
                                                    )
                                                }
                                            },
                                            onDragCancel = {
                                                dragging = false
                                                val leftover = dragDistance
                                                dragDistance = 0f
                                                dragScope.launch {
                                                    settleOffset.snapTo(leftover)
                                                    settleOffset.animateTo(
                                                        0f,
                                                        spring(
                                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                                            stiffness = Spring.StiffnessMedium
                                                        )
                                                    )
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                // Plain, synchronous math — no launch here. Each
                                                // pointer-move is handled exactly once, in exactly
                                                // the order it arrived, which is what makes this
                                                // predictable instead of bursty.
                                                //
                                                // NOTE: we only ever act on ONE threshold crossing
                                                // per callback (if/else-if, not a loop). order is a
                                                // snapshot passed in as a parameter, so after
                                                // calling onMove it does NOT update until the next
                                                // recomposition — a loop here would keep computing
                                                // currentIndex against the now-stale pre-move order
                                                // and could swap out of bounds. One swap per real
                                                // pointer-move event is exactly the cadence the
                                                // reference animation has anyway.
                                                dragDistance += dragAmount.y
                                                val currentOrder = latestOrder.value
                                                val currentIndex = currentOrder.indexOf(cardId)
                                                if (dragDistance <= -swapThresholdPx && currentIndex > 0) {
                                                    onMove(cardId, -1)
                                                    dragDistance += swapThresholdPx
                                                } else if (dragDistance >= swapThresholdPx && currentIndex < currentOrder.lastIndex) {
                                                    onMove(cardId, 1)
                                                    dragDistance -= swapThresholdPx
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "☰",
                                    color = Ui2Colors.Lime,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onReset) {
                        Text("Reset Layout", color = Ui2Colors.Muted, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Saved automatically",
                        color = Ui2Colors.Muted.copy(alpha = .65f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}

private const val HOME_CARD_DAILY_ACTIVITY = "v2_daily_activity"
private const val HOME_CARD_STREAK_WATER = "v2_streak_water"
private const val HOME_CARD_DAILY_STREAK = "v2_daily_quests"
private const val HOME_CARD_SPOTIFY = "v2_spotify"
private const val HOME_CARD_TOOLS = "v2_tools"
private const val HOME_CARD_ORDER_KEY = "home_card_order_v2"
private val DEFAULT_HOME_CARD_ORDER = listOf(
    HOME_CARD_DAILY_ACTIVITY,
    HOME_CARD_STREAK_WATER,
    HOME_CARD_DAILY_STREAK,
    HOME_CARD_SPOTIFY,
    HOME_CARD_TOOLS
)

private fun loadHomeCardOrder(prefs: android.content.SharedPreferences): List<String> {
    val saved = prefs.getString(HOME_CARD_ORDER_KEY, null)
        ?.split(',')
        ?.filter { it in DEFAULT_HOME_CARD_ORDER }
        .orEmpty()
    return (saved + DEFAULT_HOME_CARD_ORDER.filterNot(saved::contains)).distinct()
}

private fun saveHomeCardOrder(prefs: android.content.SharedPreferences, order: List<String>) {
    prefs.edit().putString(HOME_CARD_ORDER_KEY, order.joinToString(",")).apply()
}

@Composable
private fun ReorderableHomeCard(
    cardId: String,
    organizing: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    content: @Composable () -> Unit
) {
    var dragDistance by remember(cardId) { mutableStateOf(0f) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (organizing) Modifier.pointerInput(cardId, canMoveUp, canMoveDown) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragDistance = 0f },
                        onDragEnd = { dragDistance = 0f },
                        onDragCancel = { dragDistance = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDistance += dragAmount.y
                            if (dragDistance <= -70f && canMoveUp) {
                                onMoveUp()
                                dragDistance = 0f
                            } else if (dragDistance >= 70f && canMoveDown) {
                                onMoveDown()
                                dragDistance = 0f
                            }
                        }
                    )
                } else Modifier
            )
    ) {
        content()
        if (organizing) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(IgniteColors.Surface3.copy(alpha = .97f))
                    .border(1.dp, IgniteColors.Hairline, RoundedCornerShape(12.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "↑",
                    color = if (canMoveUp) IgniteColors.Gold else IgniteColors.MutedText.copy(alpha = .35f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(enabled = canMoveUp, onClick = onMoveUp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Text("☰", color = IgniteColors.Gold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    "↓",
                    color = if (canMoveDown) IgniteColors.Gold else IgniteColors.MutedText.copy(alpha = .35f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(enabled = canMoveDown, onClick = onMoveDown)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// Pink used for the calories (kcal) arc, connector line and label in Today's move.
private val CalorieAccent = Color(0xFFFA2A55)

@Composable
private fun DailyActivityCardV2(
    durationMinutes: Int,
    timeProgress: Float,
    estimatedCalories: Int,
    calorieGoal: Int,
    cardioKm: Double,
    cardioGoalKm: Double,
    onEditCalorieGoal: () -> Unit,
    onEditCardioGoal: () -> Unit,
    onEditTimeGoal: () -> Unit
) {
    val calorieProgress = estimatedCalories.toFloat() / calorieGoal.coerceAtLeast(1).toFloat()
    val cardioProgress = (cardioKm / cardioGoalKm.coerceAtLeast(0.1)).toFloat()
    val overallPercent = (((calorieProgress.coerceIn(0f, 1f) + cardioProgress.coerceIn(0f, 1f) + timeProgress.coerceIn(0f, 1f)) / 3f) * 100f).roundToInt()
    val status = when {
        overallPercent >= 100 -> "Goals crushed"
        overallPercent >= 70 -> "Almost there!"
        overallPercent >= 35 -> "Keep it moving"
        else -> "Let's get moving"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Ui2Colors.Card)
            .border(1.dp, Ui2Colors.CardOutline, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Text("Today's move", color = Ui2Colors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(status, color = Ui2Colors.Text, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .padding(top = 0.dp)
        ) {
            V2ActivitySegmentRing(
                calorieProgress = calorieProgress,
                cardioProgress = cardioProgress,
                timeProgress = timeProgress,
                overallPercent = overallPercent,
                modifier = Modifier.size(188.dp).align(Alignment.TopCenter).padding(top = 0.dp)
            )

            V2ActivityCallout(
                value = estimatedCalories.toString(), unit = "kcal", color = CalorieAccent,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 4.dp),
                onClick = onEditCalorieGoal
            )
            V2ActivityCallout(
                value = durationMinutes.toString(), unit = "Min", color = Ui2Colors.Lavender,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp, top = 50.dp),
                onClick = onEditTimeGoal
            )
            V2ActivityCallout(
                value = formatDailyDecimal(cardioKm), unit = "km", color = Ui2Colors.Cyan,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 22.dp),
                onClick = onEditCardioGoal
            )

            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = 94.dp.toPx()
                val r = 79.dp.toPx()
                val dot = 3.dp.toPx()
                fun connector(angleDeg: Float, color: Color, endX: Float) {
                    val rad = Math.toRadians(angleDeg.toDouble())
                    val cosA = kotlin.math.cos(rad).toFloat()
                    val sinA = kotlin.math.sin(rad).toFloat()
                    val x = cx + cosA * r
                    val y = cy + sinA * r
                    // Anchor stays inside the ring; leader exits outward and finishes beside its value.
                    val elbowX = x + cosA * 17.dp.toPx()
                    val elbowY = y + sinA * 17.dp.toPx()
                    drawLine(
                        color,
                        androidx.compose.ui.geometry.Offset(x, y),
                        androidx.compose.ui.geometry.Offset(elbowX, elbowY),
                        2.dp.toPx(),
                        StrokeCap.Round
                    )
                    drawLine(
                        color,
                        androidx.compose.ui.geometry.Offset(elbowX, elbowY),
                        androidx.compose.ui.geometry.Offset(endX, elbowY),
                        2.dp.toPx(),
                        StrokeCap.Round
                    )
                    drawCircle(Color.White, dot, androidx.compose.ui.geometry.Offset(x, y))
                    drawCircle(color, dot + 1.5.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y), style = Stroke(1.5.dp.toPx()))
                }
                // Anchors follow the shifted-up ring; leader lines now stay attached and level.
                connector(-52f, CalorieAccent, size.width - 39.dp.toPx())
                connector(198f, Ui2Colors.Lavender, 31.dp.toPx())
                connector(48f, Ui2Colors.Cyan, size.width - 36.dp.toPx())
            }
        }
    }
}

@Composable
private fun V2ActivityCallout(value: String, unit: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Text(value, color = Ui2Colors.Text, fontSize = 15.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold)
        Text(unit, color = color, fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V2ActivitySegmentRing(
    calorieProgress: Float,
    cardioProgress: Float,
    timeProgress: Float,
    overallPercent: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 17.dp.toPx()
            val inset = stroke / 2f + 2.dp.toPx()
            val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            val starts = listOf(-82f, 38f, 158f)
            val sweep = 102f
            val progress = listOf(calorieProgress, cardioProgress, timeProgress)
            val colors = listOf(CalorieAccent, Ui2Colors.Cyan, Ui2Colors.Lavender)
            starts.forEachIndexed { i, start ->
                drawArc(Ui2Colors.QuestEmpty, start, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                val p = progress[i].coerceIn(0f, 1f)
                if (p > 0f) drawArc(colors[i], start, sweep * p, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            drawCircle(Ui2Colors.CardRaised.copy(alpha = .62f), radius = size.minDimension * .31f, center = center)
            drawCircle(Ui2Colors.CardOutline.copy(alpha = .75f), radius = size.minDimension * .31f, center = center, style = Stroke(1.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$overallPercent%", color = Ui2Colors.Text, fontSize = 38.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold)
            Text("Complete", color = Ui2Colors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BestWorkoutStreakCardV2(bestStreak: Int) {
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
            "Best Workout streak",
            color = Ui2Colors.Text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(streakAccent.copy(alpha = 0.12f))
                .border(1.dp, streakAccent.copy(alpha = 0.4f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedStreakFire()
            Text(
                "$bestStreak ${if (bestStreak == 1) "day" else "days"}",
                color = streakAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ProgressWaterRowV2(
    progress: Float,
    currentWeight: Double?,
    goalWeight: Double?,
    state: WaterUiState,
    quickAddMl: Int,
    onAddWater: () -> Unit,
    onSubtractWater: () -> Unit,
    onOpenProgress: () -> Unit
) {
    val hasGoal = currentWeight != null && goalWeight != null
    val goalProgress = progress.coerceIn(0f, 1f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .height(148.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Ui2Colors.Card)
                .border(1.dp, Ui2Colors.CardOutline, RoundedCornerShape(20.dp))
                .clickable(onClick = onOpenProgress)
                .padding(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Progress", color = Ui2Colors.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("›", color = Ui2Colors.Primary, fontSize = 18.sp)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (hasGoal) "${(goalProgress * 100).roundToInt()}%" else "Set goal",
                color = Ui2Colors.Primary,
                fontSize = if (hasGoal) 30.sp else 22.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (hasGoal) "${formatDailyDecimal(currentWeight!!)} → ${formatDailyDecimal(goalWeight!!)} kg" else "Track your weight goal",
                color = Ui2Colors.Muted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { goalProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
                color = Ui2Colors.Primary,
                trackColor = Ui2Colors.QuestEmpty,
                drawStopIndicator = {}
            )
        }
        V2WaterWaveCard(
            intakeMl = state.intakeMl,
            goalMl = state.goalMl,
            quickAddMl = quickAddMl,
            onAddWater = onAddWater,
            onSubtractWater = onSubtractWater,
            modifier = Modifier.weight(1f).height(148.dp)
        )
    }
}

private val WaterEmptyBackground = Ui2Colors.Card
private val WaterEmptyBorder = Ui2Colors.CardOutline

@Composable
private fun V2WaterWaveCard(
    intakeMl: Int,
    goalMl: Int,
    quickAddMl: Int,
    onAddWater: () -> Unit,
    onSubtractWater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val motionEnabled = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        }.getOrDefault(true)
    }
    val targetProgress = if (goalMl <= 0) 0f else (intakeMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)
    val animatedProgress = if (motionEnabled) {
        val value by animateFloatAsState(targetProgress, tween(700), label = "home-v2-water-fill")
        value
    } else targetProgress
    val wavePhase = if (motionEnabled && animatedProgress > 0f) {
        val transition = rememberInfiniteTransition(label = "home-v2-water-wave")
        val phase by transition.animateFloat(
            0f,
            (Math.PI * 2.0).toFloat(),
            infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
            label = "home-v2-water-phase"
        )
        phase
    } else 0f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(WaterEmptyBackground)
            .border(1.dp, WaterEmptyBorder, RoundedCornerShape(20.dp))
    ) {
        // Card starts empty (just the dark base above). Water rises from the
        // bottom as intake grows, with a light-to-deep cyan gradient confined
        // to the filled region only — nothing is drawn while progress is 0.
        Canvas(Modifier.fillMaxSize()) {
            if (animatedProgress > 0f) {
                val waterTop = size.height * (1f - animatedProgress)
                val amplitude = 5.dp.toPx()
                val path = Path().apply {
                    moveTo(0f, waterTop)
                    val steps = 40
                    for (step in 0..steps) {
                        val x = size.width * step / steps.toFloat()
                        val angle = (step / steps.toFloat()) * (Math.PI * 4.0) + wavePhase
                        val y = waterTop + kotlin.math.sin(angle).toFloat() * amplitude
                        lineTo(x, y)
                    }
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path,
                    Brush.verticalGradient(
                        colors = listOf(Ui2Colors.Cyan, Ui2Colors.Water),
                        startY = waterTop,
                        endY = size.height
                    )
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(13.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$intakeMl",
                    color = Color.White,
                    fontSize = 30.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = " / $goalMl ml",
                    // Black once the water has risen high enough to sit behind this text.
                    color = if (animatedProgress > 0.62f) Color.Black else Ui2Colors.Cyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 2.dp, bottom = 3.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = .10f))
                        .border(1.dp, Color.White.copy(alpha = .20f), CircleShape)
                        .clickable(onClick = onSubtractWater),
                    contentAlignment = Alignment.Center
                ) {
                    Text("−", color = Color.White.copy(alpha = .85f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "+$quickAddMl ml",
                    color = Color.White.copy(alpha = .94f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = .12f))
                        .border(1.dp, Color.White.copy(alpha = .24f), RoundedCornerShape(50))
                        .clickable(onClick = onAddWater)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun DailyQuestCardV2(
    items: List<HomeDailyTaskV2>,
    onClick: () -> Unit
) {
    val completed = items.count { it.progress >= it.target }
    val total = items.size.coerceAtLeast(1)
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Ui2Colors.Card)
            .border(1.dp, Ui2Colors.CardOutline, RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Daily quests",
                color = Ui2Colors.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            // All quests done: the counter and its pill turn white.
            val allDone = completed >= total
            Text(
                "$completed of $total",
                color = if (allDone) Color.White else Ui2Colors.Lime,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (allDone) Color.White.copy(alpha = 0.16f) else Ui2Colors.QuestEmpty)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(total) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (index < completed) {
                                listOf(Ui2Colors.Primary, Ui2Colors.Lavender, Ui2Colors.Cyan, Color(0xFFFA2A55))[index % 4]
                            } else Ui2Colors.QuestEmpty
                        )
                )
            }
        }
    }
}

@Composable
private fun ToolsSectionV2(onNavigate: (AppDestination) -> Unit) {
    Column {
        Text(
            text = "Your tools",
            color = Ui2Colors.Text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 2.dp, bottom = 10.dp)
        )
        val orderedFeatures = FeatureRegistry.features
            .filterNot { feature ->
                feature.title.trim().lowercase() in setOf("water", "workout", "nutrition") || feature.id == "nutrition" || feature.destination == AppDestination.PROGRESS
            }
            .sortedBy { feature ->
                when (feature.title.trim().lowercase()) {
                    "fitness toolbox" -> 1
                    "gym checklist" -> 2
                    else -> 3
                }
            }
        orderedFeatures.forEach { feature ->
            ToolCardV2(
                feature = feature,
                onClick = feature.destination?.let { destination -> { onNavigate(destination) } }
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ToolCardV2(feature: FeatureDefinition, onClick: (() -> Unit)?) {
    val active = feature.status == FeatureStatus.ACTIVE
    val accent = when (feature.id) {
        "progress" -> Ui2Colors.Lime
        "toolbox" -> Ui2Colors.Cyan
        "checklist" -> Ui2Colors.Lavender
        else -> Ui2Colors.Lime
    }
    val subtitle = when (feature.id) {
        "progress" -> "Track your weekly gains"
        "toolbox" -> "Calculators and trackers"
        "checklist" -> "Tick off today's sets"
        else -> "Open ${feature.title}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Ui2Colors.Card)
            .border(1.dp, Ui2Colors.CardOutline, RoundedCornerShape(22.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Ui2Colors.CardRaised),
            contentAlignment = Alignment.Center
        ) {
            Icon(feature.icon, null, tint = accent, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(feature.title, color = Ui2Colors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Ui2Colors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Ui2Colors.CardRaised)
                .padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(7.dp).background(if (active) Ui2Colors.Lime else Ui2Colors.Dim, CircleShape))
            Text(
                if (active) "Active" else "Soon",
                color = if (active) Color.White else Ui2Colors.Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DailyCompletionCard(
    durationMinutes: Int,
    completionProgress: Float,
    completionPercent: Int,
    completedExerciseCount: Int,
    plannedExerciseCount: Int,
    estimatedCalories: Int,
    calorieGoal: Int,
    cardioKm: Double,
    cardioGoalKm: Double,
    timeGoalMinutes: Int,
    onEditCalorieGoal: () -> Unit,
    onEditCardioGoal: () -> Unit,
    onEditTimeGoal: () -> Unit,
    isWorkoutActive: Boolean
) {
    val calorieProgress = estimatedCalories.toFloat() / calorieGoal.coerceAtLeast(1).toFloat()
    val cardioProgress = (cardioKm / cardioGoalKm.coerceAtLeast(0.1)).toFloat()
    val overallPercent = (((calorieProgress.coerceIn(0f, 1f) +
            cardioProgress.coerceIn(0f, 1f) + completionProgress.coerceIn(0f, 1f)) / 3f) * 100f).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 350.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFB9B00).copy(alpha = 0.025f), Color(0xFF19181A), Color(0xFF141416))
                )
            )
            .border(0.7.dp, Color(0xFFFB9B00).copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(ObsidianSunsetColors.Elevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = ObsidianSunsetColors.Orange, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = "Daily activity",
                    color = ObsidianSunsetColors.TextStrong,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "$overallPercent%",
                color = ObsidianSunsetColors.SoftOrange,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DailyActivityRings(
                calorieProgress = calorieProgress,
                cardioProgress = cardioProgress,
                timeProgress = completionProgress,
                modifier = Modifier.size(150.dp)
            )
            Column(
                modifier = Modifier.width(152.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                DailyActivityStatCard(
                    icon = Icons.Outlined.LocalFireDepartment,
                    accent = Color(0xFFFF4000),
                    value = "$estimatedCalories / $calorieGoal",
                    label = "Calories",
                    onClick = onEditCalorieGoal
                )
                DailyActivityStatCard(
                    icon = Icons.Outlined.DirectionsRun,
                    accent = Color(0xFFFF8000),
                    value = "${formatDailyDecimal(cardioKm)} / ${formatDailyDecimal(cardioGoalKm)} km",
                    label = "Cardio",
                    onClick = onEditCardioGoal
                )
                DailyActivityStatCard(
                    icon = Icons.Outlined.Schedule,
                    accent = Color(0xFFFFB300),
                    value = "$durationMinutes / $timeGoalMinutes min",
                    label = "Time",
                    onClick = onEditTimeGoal
                )
            }
        }
    }
}

@Composable
private fun DailyActivityRings(
    calorieProgress: Float,
    cardioProgress: Float,
    timeProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val track = ObsidianSunsetColors.Border
        val stroke = 10.dp.toPx()
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val scale = size.minDimension / 92.dp.toPx()
        val radii = listOf(40.dp.toPx() * scale, 29.dp.toPx() * scale, 18.dp.toPx() * scale)
        val progresses = listOf(calorieProgress, cardioProgress, timeProgress)
        val ringColors = listOf(Color(0xFFFF4000), Color(0xFFFF8000), Color(0xFFFFB300))
        radii.forEachIndexed { index, radius ->
            drawCircle(color = track, radius = radius, center = center, style = Stroke(width = stroke))
            val progress = progresses[index].coerceIn(0f, 1f)
            if (progress > 0f) {
                val diameter = radius * 2f
                drawArc(
                    color = ringColors[index], startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
private fun DailyActivityStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    value: String,
    label: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(ObsidianSunsetColors.CardHigh)
            .border(0.5.dp, ObsidianSunsetColors.Border, RoundedCornerShape(13.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(ObsidianSunsetColors.Elevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(value, color = ObsidianSunsetColors.TextStrong, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, color = accent.copy(alpha = 0.82f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

private fun formatDailyDecimal(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.US, "%.1f", value)
}

private data class WorkoutStreakDayV2(
    val day: LocalDate,
    val durationSeconds: Long,
    val isRestDay: Boolean = false
)

@Composable
private fun WorkoutStreakCard(
    days: List<WorkoutStreakDayV2>,
    bestStreak: Int,
    today: LocalDate
) {
    val transition = rememberInfiniteTransition(label = "best-streak-pill")
    val pulseProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), repeatMode = RepeatMode.Reverse),
        label = "streak-border"
    )
    val pulseBorder = androidx.compose.ui.graphics.lerp(
        Color(0xFF6B4212),
        Color(0xFFFF7A1A),
        pulseProgress
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1F1812))
            .border(0.5.dp, Color(0xFF3A2A1C), RoundedCornerShape(14.dp))
            .padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Best Workout streak",
            modifier = Modifier.weight(1f),
            color = Color(0xFFF4ECE4),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFF2B1F14))
                .border(1.dp, pulseBorder, CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            AnimatedStreakFire()
            Text(
                text = if (bestStreak == 1) "1 day" else "$bestStreak days",
                color = Color(0xFFFFB020),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AnimatedStreakFire() {
    val context = LocalContext.current
    val motionEnabled = remember(context) {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f }
            .getOrDefault(true)
    }
    val transition = rememberInfiniteTransition(label = "streak-fire")
    val bigScaleY by transition.animateFloat(0.95f, 1.20f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "big-y")
    val bigScaleX by transition.animateFloat(0.95f, 1.08f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "big-x")
    val bigRotation by transition.animateFloat(-3f, 3f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "big-r")
    val smallScaleY by transition.animateFloat(0.95f, 1.18f, infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "small-y")
    val smallRotation by transition.animateFloat(3f, -3f, infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "small-r")
    val ember1 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "ember-1")
    val ember2 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1900, delayMillis = 350, easing = LinearEasing)), label = "ember-2")

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
            Modifier.size(width = 10.dp, height = 11.dp).align(Alignment.BottomCenter).graphicsLayer {
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
            StreakEmber(progress = ember1, startX = 5.dp, driftX = (-2).dp)
            StreakEmber(progress = ember2, startX = 10.dp, driftX = 2.dp)
        }
    }
}

@Composable
private fun BoxScope.StreakEmber(progress: Float, startX: androidx.compose.ui.unit.Dp, driftX: androidx.compose.ui.unit.Dp) {
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
private fun DailyQuestCard(
    items: List<HomeDailyTaskV2>,
    isRestDay: Boolean,
    onClick: () -> Unit
) {
    val completed = items.count { it.progress >= it.target }
    val total = items.size.coerceAtLeast(1)
    val progress = completed.toFloat() / total.toFloat()

    val interactionSource = remember { MutableInteractionSource() }

    IgniteCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        borderColor = Color.White.copy(alpha = 0.07f),
        brush = Brush.verticalGradient(listOf(Color(0xFF1B1B1E), Color(0xFF17171A), Color(0xFF141416))),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(IgniteColors.EmberTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_daily_quest_custom),
                    contentDescription = "Daily Quest",
                    tint = IgniteColors.Gold,
                    modifier = Modifier.size(23.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Daily Quest",
                    color = IgniteColors.Text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isRestDay) "Enjoy your rest day • +500 XP" else "$completed of ${items.size} quests complete today",
                    color = IgniteColors.MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "${(progress * 100).roundToInt()}%",
                color = IgniteColors.Gold,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(IgniteColors.EmberTint)
                    .border(1.dp, IgniteColors.Gold.copy(alpha = 0.28f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun CompactHomeWaterCard(
    state: WaterUiState,
    settings: AppSettings,
    onAddWater: () -> Unit,
    onSubtractWater: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val quickAdd = settings.widgetButtonMl

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1B1B1E), Color(0xFF17171A), Color(0xFF141416))))
            .border(0.8.dp, Color.White.copy(alpha = 0.07f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reuse the app's existing animated water vessel, just scaled down for Home.
        AnimatedWaterBottle(
            progress = state.progress,
            width = 38.dp,
            bodyHeight = 56.dp,
            capWidth = 18.dp,
            capHeight = 7.dp
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Water",
                color = IgniteColors.HydrationDim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = volumeNumber(state.intakeMl, settings.unit),
                    color = IgniteColors.HydrationText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = volumeUnitLabel(settings.unit),
                    color = IgniteColors.HydrationAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                )
            }
            Text(
                text = "of ${formatVolume(state.goalMl, settings.unit)}",
                color = IgniteColors.HydrationFaint,
                fontSize = 10.5.sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(IgniteColors.HydrationSurface)
                    .border(1.dp, IgniteColors.HydrationStroke, CircleShape)
                    .clickable(onClick = onSubtractWater),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    color = IgniteColors.HydrationDim,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(IgniteBrushes.HydrationAction)
                    .clickable(onClick = onAddWater)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ ${formatVolume(quickAdd, settings.unit)}",
                    color = IgniteColors.HydrationActionText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HydrationSummaryCard(
    state: WaterUiState,
    settings: AppSettings,
    onAddWater: () -> Unit,
    onSubtractWater: () -> Unit
) {
    val cardShape = RoundedCornerShape(26.dp)
    val quickAdd = settings.widgetButtonMl
    val displayPercent = state.progressPercent.coerceIn(0, 100)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1B1B1E), Color(0xFF17171A), Color(0xFF141416))))
            .border(1.dp, Color.White.copy(alpha = 0.07f), cardShape)
    ) {
        // Soft amber bloom in the top-right, matching the supplied Ignite hydration concept.
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-120).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            IgniteColors.HydrationHi.copy(alpha = 0.16f),
                            IgniteColors.HydrationHi.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Same animated bottle used on the Water page, enlarged for the Home hero card.
                // Its cap starts on the same visual line as the hydration title.
                AnimatedWaterBottle(
                    progress = state.progress,
                    width = 82.dp,
                    bodyHeight = 132.dp,
                    capWidth = 30.dp,
                    capHeight = 11.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's Hydration",
                        color = IgniteColors.HydrationDim,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.End)
                    )

                    Spacer(Modifier.height(26.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = volumeNumber(state.intakeMl, settings.unit),
                            color = IgniteColors.HydrationText,
                            fontSize = 32.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = volumeUnitLabel(settings.unit),
                            color = IgniteColors.HydrationAmber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 3.dp, bottom = 2.dp)
                        )
                    }
                    Text(
                        text = "of ${formatVolume(state.goalMl, settings.unit)} today",
                        color = IgniteColors.HydrationFaint,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(IgniteColors.HydrationSurface)
                        .border(1.dp, IgniteColors.HydrationStroke, RoundedCornerShape(14.dp))
                        .clickable(onClick = onSubtractWater),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "−",
                        color = IgniteColors.HydrationDim,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(IgniteBrushes.HydrationAction)
                        .clickable(onClick = onAddWater),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add ${formatVolume(quickAdd, settings.unit)}",
                        color = IgniteColors.HydrationActionText,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(IgniteColors.HydrationHi.copy(alpha = 0.12f))
                        .border(
                            1.dp,
                            IgniteColors.HydrationHi.copy(alpha = 0.25f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$displayPercent%",
                        color = IgniteColors.HydrationAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: FeatureDefinition,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?
) {
    val isActive = feature.status == FeatureStatus.ACTIVE
    val statusText = if (isActive) "Active" else "Coming soon"
    val statusColor = if (isActive) IgniteColors.Gold else IgniteColors.MutedText

    IgniteCard(
        modifier = modifier.height(46.dp),
        borderColor = IgniteColors.Hairline,
        brush = Brush.linearGradient(listOf(IgniteColors.Surface2, IgniteColors.Surface2)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isActive) IgniteColors.EmberTint else IgniteColors.Surface3),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = if (isActive) IgniteColors.Gold else IgniteColors.Dimmed,
                    modifier = Modifier.size(17.dp)
                )
            }

            Text(
                text = feature.title,
                color = if (isActive) IgniteColors.Text else IgniteColors.SoftText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 10.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}




private data class DailyQuestCandidateV2(
    val family: String,
    val id: String,
    val title: String,
    val subtitle: String,
    val xp: Int,
    val progress: Int,
    val target: Int
)

private fun buildDailyQuestSet(
    dateKey: String,
    selectedIds: List<String>? = null,
    startedWorkout: Boolean,
    finishedWorkout: Boolean,
    waterMl: Int,
    waterGoalMl: Int,
    prCount: Int,
    cardioKm: Double,
    calories: Int,
    workoutMinutes: Int,
    completedExercises: Int,
    totalReps: Int,
    songsPlayed: Int,
    completedAllExercises: Boolean
): List<HomeDailyTaskV2> {
    val waterGoal = waterGoalMl.coerceAtLeast(1)
    val distanceMeters = (cardioKm.coerceAtLeast(0.0) * 1000.0).roundToInt()

    fun q(family: String, id: String, title: String, subtitle: String, xp: Int, progress: Int, target: Int) =
        DailyQuestCandidateV2(family, id, title, subtitle, xp.coerceAtMost(500), progress.coerceAtLeast(0), target.coerceAtLeast(1))

    val pool = listOf(
        q("workout", "show_up", "Show Up", "Start a workout", 75, if (startedWorkout) 1 else 0, 1),
        q("workout", "finish_strong", "Finish Strong", "Finish and save a workout", 150, if (finishedWorkout) 1 else 0, 1),
        q("hydration", "first_sip", "First Sip", "Log water at least once", 50, waterMl, 1),
        q("hydration", "water_50", "Stay Hydrated", "Reach 50% of your water goal", 100, waterMl, (waterGoal * 50 / 100).coerceAtLeast(1)),
        q("hydration", "water_75", "Almost There", "Reach 75% of your water goal", 125, waterMl, (waterGoal * 75 / 100).coerceAtLeast(1)),
        q("hydration", "water_100", "Hydration Complete", "Reach 100% of your water goal", 150, waterMl, waterGoal),
        q("pr", "pr_1", "Push Your Limit", "Hit 1 PR", 200, prCount, 1),
        q("pr", "pr_2", "Double PR", "Hit 2 PRs today", 350, prCount, 2),
        q("pr", "pr_3", "Triple PR", "Hit 3 PRs today", 500, prCount, 3),
        q("distance", "run_1k", "1K Run", "Complete 1 km of cardio", 100, distanceMeters, 1000),
        q("distance", "run_2k", "2K Run", "Complete 2 km of cardio", 200, distanceMeters, 2000),
        q("distance", "run_3k", "3K Run", "Complete 3 km of cardio", 300, distanceMeters, 3000),
        q("distance", "run_4k", "4K Run", "Complete 4 km of cardio", 400, distanceMeters, 4000),
        q("distance", "run_5k", "5K Run", "Complete 5 km of cardio", 500, distanceMeters, 5000),
        q("calories", "burn_100", "Burn 100", "Burn 100 kcal during cardio", 100, calories, 100),
        q("calories", "burn_200", "Burn 200", "Burn 200 kcal during cardio", 200, calories, 200),
        q("calories", "burn_300", "Burn 300", "Burn 300 kcal during cardio", 300, calories, 300),
        q("calories", "burn_400", "Burn 400", "Burn 400 kcal during cardio", 400, calories, 400),
        q("duration", "train_30", "Quick Session", "Train for at least 30 minutes", 100, workoutMinutes, 30),
        q("duration", "train_45", "Put In The Time", "Train for at least 45 minutes", 150, workoutMinutes, 45),
        q("duration", "train_60", "Full Session", "Train for at least 60 minutes", 200, workoutMinutes, 60),
        q("duration", "train_90", "Committed", "Train for at least 90 minutes", 300, workoutMinutes, 90),
        q("reps", "reps_100", "Rep Machine", "Complete 100 total reps", 100, totalReps, 100),
        q("reps", "reps_200", "200 Rep Club", "Complete 200 total reps", 200, totalReps, 200),
        q("music", "songs_3", "Workout Vibes", "Listen to 3 qualifying workout songs", 100, songsPlayed, 3),
        q("music", "songs_5", "Five Tracks Deep", "Listen to 5 qualifying workout songs", 125, songsPlayed, 5),
        q("music", "songs_8", "Soundtrack Session", "Listen to 8 qualifying workout songs", 175, songsPlayed, 8),
        q("exercise", "exercise_3", "Strength Day", "Complete 3 exercises", 125, completedExercises, 3),
        q("exercise", "exercise_5", "Exercise Five", "Complete 5 exercises", 175, completedExercises, 5),
        q("completion", "no_skipping", "No Skipping", "Complete every exercise in a workout", 175, if (completedAllExercises) 1 else 0, 1)
    )

    // Stable for the whole calendar day: choose four different quest families, then
    // one tier from each family. This prevents 1K + 2K (or similar tiers) appearing together.
    val chosen = selectedIds?.mapNotNull { id -> pool.firstOrNull { it.id == id } }?.takeIf { it.size == 4 } ?: run {
        val random = Random(dateKey.hashCode())
        pool.groupBy { it.family }.entries.shuffled(random).take(4).map { (_, quests) ->
            quests[random.nextInt(quests.size)]
        }
    }
    return chosen.map { selected ->
        HomeDailyTaskV2(selected.id, selected.title, selected.subtitle, selected.xp, selected.progress, selected.target)
    }
}

private data class HomeDailyTaskV2(val id: String, val title: String, val subtitle: String, val xp: Int, val progress: Int, val target: Int)

@Composable
private fun DailyTasksDialog(
    items: List<HomeDailyTaskV2>,
    dateKey: String,
    context: android.content.Context,
    isRestDay: Boolean,
    onDismiss: () -> Unit
) {
    val accent = Ui2Colors.Primary
    val surface = Ui2Colors.Card
    val muted = Ui2Colors.Muted
    val primary = Ui2Colors.Text
    val track = Ui2Colors.QuestEmpty
    val chip = Ui2Colors.CardRaised
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = .72f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
                .padding(horizontal = 18.dp, vertical = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.fillMaxWidth().widthIn(max = 420.dp).clip(RoundedCornerShape(30.dp))
                    .background(surface)
                    .border(1.dp, Ui2Colors.CardOutline, RoundedCornerShape(30.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(chip), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_daily_quest_custom),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text("Daily quest", color = primary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text(if (isRestDay) "Rest day" else "Refreshes every day", color = muted, fontSize = 13.sp)
                    }
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha=.06f)).clickable(onClick=onDismiss), contentAlignment=Alignment.Center) { Icon(Icons.Rounded.Close, "Close", tint=muted, modifier=Modifier.size(20.dp)) }
                }
                items.forEachIndexed { index, task ->
                    val questColor = listOf(Ui2Colors.Primary, Ui2Colors.Lavender, Ui2Colors.Cyan, Color(0xFFFA2A55))[index % 4]
                    val progress = (task.progress.toFloat() / task.target.coerceAtLeast(1)).coerceIn(0f, 1f)
                    val complete = progress >= 1f
                    val icon = when {
                        task.id.startsWith("water") || task.id == "first_sip" -> Icons.Outlined.WaterDrop
                        task.id.startsWith("pr_") -> Icons.Outlined.EmojiEvents
                        task.id.startsWith("run_") -> Icons.Outlined.DirectionsRun
                        task.id.startsWith("burn_") -> Icons.Outlined.LocalFireDepartment
                        task.id.startsWith("songs_") -> Icons.Outlined.MusicNote
                        task.id.startsWith("train_") -> Icons.Outlined.Schedule
                        else -> Icons.Outlined.FitnessCenter
                    }
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Color.White.copy(alpha=.075f), Color.White.copy(alpha=.02f))))
                            .border(1.dp, Color.White.copy(alpha=.08f), RoundedCornerShape(28.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(chip), contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = questColor, modifier = Modifier.size(20.dp))
                            }
                            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                                Text(task.title, color = primary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(task.subtitle, color = muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("+${task.xp} XP", color = questColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Box(Modifier.size(24.dp).clip(CircleShape).background(if (complete) questColor else track), contentAlignment = Alignment.Center) {
                                    if (complete) Icon(Icons.Rounded.CheckCircle, "Completed", tint = Color(0xFF17171A), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        LinearProgressIndicator(
                            progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = questColor, trackColor = track
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeProfileAvatar(
    profileImageUri: String,
    questProgress: Float,
    level: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(profileImageUri) {
        if (profileImageUri.isBlank()) null else runCatching {
            context.contentResolver.openInputStream(Uri.parse(profileImageUri))?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
    val ringDiameter = 58.dp
    val badgeHeight = 18.dp
    Box(
        modifier = Modifier
            .padding(start = 12.dp, bottom = badgeHeight / 2)
            .size(width = 64.dp, height = ringDiameter + badgeHeight / 2)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        // Ring and avatar are direct children of the TopCenter box, exactly like the
        // original — this is the part that was always rendering correctly. Do not wrap
        // these in another Box; a plain Box() defaults to TopStart alignment, which is
        // what shifted the photo into the corner last time.
        Canvas(Modifier.size(ringDiameter)) {
            drawArc(Ui2Colors.CardOutline, -90f, 360f, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            drawArc(Ui2Colors.Lime, -90f, questProgress.coerceIn(0f, 1f) * 360f, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        Box(
            Modifier.size(50.dp).offset(y = 4.dp).clip(CircleShape).background(Ui2Colors.Card),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) Image(bitmap, "Open My Stats", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Outlined.PersonOutline, "Open My Stats", tint = Ui2Colors.Lime, modifier = Modifier.size(24.dp))
        }
        // Badge's vertical CENTER sits at y = ringDiameter (the ring's bottom edge), so it
        // sits half-in/half-out of the ring like the mockup, instead of floating below it.
        // Fixed height + fixed corner radius (half the height) = always a pill, at any
        // digit count; only width grows for 2-3 digits.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = ringDiameter - badgeHeight / 2)
                .widthIn(min = 28.dp)
                .height(badgeHeight)
                .clip(RoundedCornerShape(9.dp))
                .background(Ui2Colors.Lime)
                .border(2.dp, Ui2Colors.Background, RoundedCornerShape(9.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = level.toString(),
                color = Ui2Colors.Card,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }
    }
}

@Composable
private fun SpotifyPreWorkoutCard(
    state: SpotifyManager.UiState,
    selectedPlaylist: SpotifyWebApiManager.Playlist?,
    onSelectPlaylist: () -> Unit,
    onPlaySelectedPlaylist: () -> Unit,
    onSearch: () -> Unit,
    onPlayPause: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showingArtwork = state.connected && !state.isPaused && state.artwork != null
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .clip(shape)
            .background(Ui2Colors.Card)
            .border(1.dp, Ui2Colors.CardOutline, shape)
            .clickable(
                enabled = state.connected && state.trackUri != null,
                onClick = onOpen
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF111113))
                .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (showingArtwork) {
                state.artwork?.let { artwork ->
                    Image(
                        bitmap = artwork.asImageBitmap(),
                        contentDescription = "Current song artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_gymbros_music),
                    contentDescription = "Spotify",
                    modifier = Modifier.size(34.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(start = 8.dp)
                .widthIn(max = 132.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = selectedPlaylist != null, onClick = onPlaySelectedPlaylist)
                .padding(vertical = 6.dp, horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_playlist_queue),
                contentDescription = "Playlist",
                modifier = Modifier.size(19.dp),
                contentScale = ContentScale.Fit,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
            )
            Text(
                text = when {
                    selectedPlaylist != null -> selectedPlaylist.name
                    state.connected -> "Select your playlist"
                    else -> "Connect Spotify"
                },
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.weight(1f))

        if (state.connected) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onSelectPlaylist)
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (selectedPlaylist == null) "Select" else "Change", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onSearch),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search Spotify songs",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = if (state.isPaused) "Play" else "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(Ui2Colors.CardRaised)
                    .border(1.dp, Ui2Colors.CardOutline, RoundedCornerShape(11.dp))
                    .clickable(enabled = !state.connecting) { onPlayPause() }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (state.connecting) "Connecting" else "Connect", color = Ui2Colors.Text, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}


@Composable
private fun SpotifyPlaylistPicker(
    state: SpotifyWebApiManager.UiState,
    onDismiss: () -> Unit,
    onAuthorize: () -> Unit,
    onRefresh: () -> Unit,
    onPlaylistSelected: (SpotifyWebApiManager.Playlist) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ui2Colors.Background.copy(alpha = 0.72f))
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .heightIn(max = 720.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Ui2Colors.Card)
                    .border(1.dp, Ui2Colors.CardOutline, RoundedCornerShape(26.dp))
                    .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Ui2Colors.CardRaised),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.MusicNote, null, tint = Ui2Colors.Lime, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Select playlist",
                            color = Ui2Colors.Text,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Choose what plays during your workout",
                            color = Ui2Colors.Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))

                when {
                    state.loading -> {
                        Box(
                            Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Loading your playlists…", color = Ui2Colors.Muted)
                        }
                    }

                    !state.authorized -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Connect playlist access to choose what GymBros plays.",
                                color = Ui2Colors.Muted,
                                fontSize = 15.sp
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Ui2Colors.Lime)
                                    .clickable(onClick = onAuthorize)
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Allow Playlist Access", color = Ui2Colors.Background, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    state.playlists.isEmpty() -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                state.errorMessage ?: "No Spotify playlists were returned.",
                                color = Ui2Colors.Muted,
                                fontSize = 15.sp
                            )
                            TextButton(onClick = onRefresh) {
                                Text("Try again", color = Ui2Colors.Lime, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(max = 500.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.playlists, key = { it.id }) { playlist ->
                                val selected = state.selectedPlaylist?.id == playlist.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Ui2Colors.CardRaised)
                                        .then(
                                            if (selected) Modifier.border(
                                                1.5.dp,
                                                Ui2Colors.Lime,
                                                RoundedCornerShape(16.dp)
                                            ) else Modifier
                                        )
                                        .clickable { onPlaylistSelected(playlist) }
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (selected) Ui2Colors.Lime else Ui2Colors.Background),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.MusicNote,
                                            contentDescription = null,
                                            tint = if (selected) Ui2Colors.Background else Ui2Colors.Dim,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            playlist.name,
                                            color = Ui2Colors.Text,
                                            fontSize = 15.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (playlist.ownerName.isNotBlank()) {
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                "by ${playlist.ownerName}",
                                                color = Ui2Colors.Muted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (selected) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = Ui2Colors.Lime,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(Ui2Colors.CardRaised)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Close",
                        color = Ui2Colors.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotifyWorkoutMiniPlayer(
    state: SpotifyManager.UiState,
    selectedPlaylist: SpotifyWebApiManager.Playlist?,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    songAdded: Boolean,
    addSongLoading: Boolean,
    onAddSong: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val controlsEnabled = state.connected && !state.connecting && state.trackUri != null
    val progress = if (state.durationMs > 0L)
        (state.playbackPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(138.dp)
            .clip(shape)
            .background(Color(0xFF151515))
            .border(0.8.dp, Color.White.copy(alpha = 0.10f), shape)
            .clickable(onClick = onOpen)
    ) {
        state.artwork?.let { artwork ->
            androidx.compose.foundation.Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.42f,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.22f), Color.Black.copy(alpha = 0.70f))
                )
            )
        )

        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_gymbros_music),
                        contentDescription = "Spotify",
                        modifier = Modifier.size(13.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = when {
                            state.connecting -> "Reconnecting"
                            state.connected -> selectedPlaylist?.name ?: "Workout"
                            else -> "Offline"
                        },
                        color = if (state.connected) Color(0xFF1ED760) else Color.White.copy(alpha = 0.62f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = if (songAdded) "Remove song from selected playlist" else "Save song to selected playlist",
                    tint = if (songAdded) Color(0xFF1ED760) else Color.White,
                    modifier = Modifier
                        .size(23.dp)
                        .clickable(enabled = controlsEnabled && !addSongLoading) { onAddSong() }
                )
            }

            Text(
                text = when {
                    state.connecting -> "Connecting to Spotify…"
                    state.trackUri != null -> state.trackName
                    else -> "Spotify disconnected"
                },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when {
                    state.connecting -> "Restoring your workout music"
                    state.trackUri != null -> state.artistName
                    else -> "Open Spotify to reconnect"
                },
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(5.dp))
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpotifyMiniControlButton(Icons.Rounded.Shuffle, "Shuffle", onToggleShuffle, controlsEnabled, active = state.isShuffling)
                SpotifyMiniControlButton(Icons.Rounded.SkipPrevious, "Previous", onPrevious, controlsEnabled, large = true)
                SpotifyMiniControlButton(
                    if (state.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    if (state.isPaused) "Play" else "Pause",
                    onTogglePlayPause, controlsEnabled, emphasized = true
                )
                SpotifyMiniControlButton(Icons.Rounded.SkipNext, "Next", onNext, controlsEnabled, large = true)
                SpotifyMiniControlButton(
                    if (state.repeatMode == Repeat.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    "Repeat",
                    onToggleRepeat,
                    controlsEnabled,
                    active = state.repeatMode != Repeat.OFF
                )
            }
        }
    }
}

@Composable
private fun SpotifyWorkoutFullPlayer(
    state: SpotifyManager.UiState,
    selectedPlaylist: SpotifyWebApiManager.Playlist?,
    onDismiss: () -> Unit,
    onPlaySelectedPlaylist: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onSearch: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    songAdded: Boolean,
    addSongLoading: Boolean,
    onAddSong: () -> Unit
) {
    val controlsEnabled = state.connected && !state.connecting && state.trackUri != null
    var seekPosition by remember(state.trackUri, state.playbackPositionMs) {
        mutableStateOf(state.playbackPositionMs.toFloat())
    }
    val duration = state.durationMs.coerceAtLeast(1L)
    LaunchedEffect(state.trackUri, state.isPaused, state.connected, state.playbackPositionMs) {
        if (state.connected && !state.isPaused && state.trackUri != null) {
            while (seekPosition < duration.toFloat()) {
                delay(1_000L)
                seekPosition = (seekPosition + 1_000f).coerceAtMost(duration.toFloat())
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Ui2Colors.Card)
                .border(1.dp, Ui2Colors.CardOutline, RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpotifyControlButton(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Collapse player",
                    onClick = onDismiss,
                    enabled = true
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NOW PLAYING",
                        color = Ui2Colors.Muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedPlaylist?.name ?: "Spotify",
                        color = Ui2Colors.Text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = selectedPlaylist != null, onClick = onPlaySelectedPlaylist)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                IconButton(onClick = onAddSong, enabled = controlsEnabled && !addSongLoading) {
                    if (addSongLoading) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = Ui2Colors.Lime, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = if (songAdded) "Remove song from selected playlist" else "Save song to selected playlist",
                            tint = if (songAdded) Ui2Colors.Lime else Ui2Colors.Text,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Crossfade(targetState = state.artwork, animationSpec = tween(220), label = "spotifyFullArtwork") { artwork ->
                if (artwork != null) {
                    androidx.compose.foundation.Image(
                        bitmap = artwork.asImageBitmap(),
                        contentDescription = "Album artwork",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(18.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Ui2Colors.CardRaised),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.MusicNote,
                            contentDescription = null,
                            tint = if (state.connected) Ui2Colors.Lime else Ui2Colors.Dim,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when {
                            state.connecting -> "Connecting to Spotify…"
                            state.trackUri != null -> state.trackName
                            else -> "Spotify disconnected"
                        },
                        color = Ui2Colors.Text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            state.connecting -> "Restoring your workout music"
                            state.trackUri != null -> state.artistName
                            else -> "Open Spotify to reconnect"
                        },
                        color = Ui2Colors.Muted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                IconButton(onClick = onSearch) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search Spotify songs", tint = Ui2Colors.Text,
                        modifier = Modifier.size(25.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Slider(
                value = seekPosition.coerceIn(0f, duration.toFloat()),
                onValueChange = { seekPosition = it },
                onValueChangeFinished = { if (controlsEnabled) onSeek(seekPosition.toLong()) },
                enabled = controlsEnabled,
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Ui2Colors.Lime,
                    activeTrackColor = Ui2Colors.Lime,
                    inactiveTrackColor = Ui2Colors.QuestEmpty,
                    disabledThumbColor = Ui2Colors.Dim,
                    disabledActiveTrackColor = Ui2Colors.Dim,
                    disabledInactiveTrackColor = Ui2Colors.QuestEmpty
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatSpotifyTime(seekPosition.toLong()), color = Ui2Colors.Muted, fontSize = 10.sp)
                Text(formatSpotifyTime(state.durationMs), color = Ui2Colors.Muted, fontSize = 10.sp)
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SpotifyFullControlButton(
                    icon = Icons.Rounded.Shuffle,
                    description = "Shuffle",
                    onClick = onToggleShuffle,
                    enabled = controlsEnabled,
                    active = state.isShuffling
                )
                SpotifyFullControlButton(Icons.Rounded.SkipPrevious, "Previous", onPrevious, controlsEnabled)
                SpotifyFullControlButton(
                    icon = if (state.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    description = if (state.isPaused) "Play" else "Pause",
                    onClick = onTogglePlayPause,
                    enabled = controlsEnabled,
                    emphasized = true
                )
                SpotifyFullControlButton(Icons.Rounded.SkipNext, "Next", onNext, controlsEnabled)
                SpotifyFullControlButton(
                    icon = if (state.repeatMode == Repeat.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    description = "Repeat",
                    onClick = onToggleRepeat,
                    enabled = controlsEnabled,
                    active = state.repeatMode != Repeat.OFF
                )
            }
        }
    }
}

@Composable
private fun SpotifyMiniControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
    emphasized: Boolean = false,
    active: Boolean = false,
    large: Boolean = false
) {
    val tint = when {
        !enabled -> Ui2Colors.Dim
        active -> Ui2Colors.Lime
        emphasized -> Ui2Colors.Text
        else -> Ui2Colors.Text
    }
    Box(
        modifier = Modifier
            .then(
                if (emphasized) {
                    Modifier.heightIn(max = 50.dp)
                        .aspectRatio(1f, matchHeightConstraintsFirst = true)
                } else {
                    Modifier.size(if (large) 44.dp else 38.dp)
                }
            )
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(
                when {
                    emphasized -> 38.dp
                    large -> 28.dp
                    else -> 22.dp
                }
            )
        )
    }
}

@Composable
private fun SpotifyFullControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
    emphasized: Boolean = false,
    active: Boolean = false
) {
    val tint = when {
        !enabled -> Ui2Colors.Dim
        active -> Ui2Colors.Lime
        emphasized -> Ui2Colors.Background
        else -> Ui2Colors.Text
    }
    Box(
        modifier = Modifier
            .size(if (emphasized) 58.dp else 44.dp)
            .clip(CircleShape)
            .then(
                if (emphasized) Modifier.background(if (enabled) Ui2Colors.Lime else Ui2Colors.CardRaised)
                else Modifier
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (emphasized && !enabled) Ui2Colors.Dim else tint,
            modifier = Modifier.size(if (emphasized) 30.dp else 24.dp)
        )
    }
}

private fun formatSpotifyTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(Locale.US, minutes, seconds)
}

@Composable
private fun SpotifyControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    emphasized: Boolean = false,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(if (emphasized) 42.dp else 36.dp)
            .clip(CircleShape)
            .background(
                when {
                    emphasized && enabled -> Color.Transparent
                    emphasized -> Color.Transparent
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                emphasized && enabled -> Ui2Colors.Text
                emphasized -> Ui2Colors.Dim
                enabled -> Ui2Colors.Text
                else -> Ui2Colors.Dim
            },
            modifier = Modifier.size(if (emphasized) 25.dp else 22.dp)
        )
    }
}