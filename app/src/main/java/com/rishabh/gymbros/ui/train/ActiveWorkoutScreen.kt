package com.rishabh.gymbros.ui.train

import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import com.rishabh.gymbros.R
import com.rishabh.gymbros.ui.theme.Ui2Colors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.DialogProperties
import com.rishabh.gymbros.features.workout.data.WorkoutRepository
import com.rishabh.gymbros.features.workout.domain.ExerciseLibraryCatalog
import com.rishabh.gymbros.features.workout.model.ActiveWorkoutExercise
import com.rishabh.gymbros.features.workout.model.WorkoutExerciseType
import com.rishabh.gymbros.features.workout.model.WorkoutSetDraft
import com.rishabh.gymbros.features.workout.model.WorkoutSetRecord
import com.rishabh.gymbros.features.workout.model.WorkoutUiState
import com.rishabh.gymbros.features.workout.model.WorkoutCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

private val WorkoutCardShape = RoundedCornerShape(18.dp)
private val WorkoutFieldShape = RoundedCornerShape(12.dp)

@Composable
internal fun ActiveWorkoutScreen(
    state: WorkoutUiState,
    repository: WorkoutRepository,
    onLeaveRunning: () -> Unit = {},
    workoutMusicEnabled: Boolean? = null,
    spotifyConnected: Boolean = false,
    spotifyConnecting: Boolean = false,
    spotifyIsPlaying: Boolean = false,
    hasSelectedSpotifyPlaylist: Boolean = false,
    onStartWorkoutMusic: () -> Unit = {},
    onSkipWorkoutMusic: () -> Unit = {},
    onMinimize: () -> Unit = {},
    addExerciseRequestKey: Int = 0,
    onAddExerciseRequestConsumed: () -> Unit = {},
    onWorkoutDiscarded: () -> Unit = {},
    useUi2SetQualityStyle: Boolean = false,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showIncompleteWarning by rememberSaveable { mutableStateOf(false) }
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    var showCancelDialog by rememberSaveable { mutableStateOf(false) }
    var showLeaveDialog by rememberSaveable { mutableStateOf(false) }
    var showReplaceDialog by rememberSaveable { mutableStateOf(false) }
    var replaceExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    var editExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    var bellPlayedFor by rememberSaveable { mutableLongStateOf(0L) }
    var lastVibratedSecond by rememberSaveable { mutableLongStateOf(-1L) }
    var pendingNextAction by rememberSaveable { mutableStateOf("ADD") }
    var reorderMode by rememberSaveable { mutableStateOf(false) }
    var reorderInitialExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingScrollToAddedExercise by rememberSaveable { mutableStateOf(false) }
    var showExerciseLibrary by rememberSaveable { mutableStateOf(false) }
    var libraryReplaceExerciseId by rememberSaveable { mutableStateOf<String?>(null) }

    // Preserve the exact workout viewport when the sheet is minimized.
    // This is deliberately independent of the active exercise: reopening the workout
    // should look exactly as it did when the user left it, with no corrective scroll.
    val context = LocalContext.current
    val viewportPrefs = remember(context) {
        context.getSharedPreferences("gymbros_active_workout_viewport", android.content.Context.MODE_PRIVATE)
    }
    val savedListIndex = viewportPrefs.getInt("first_visible_item", 0).coerceAtLeast(0)
    val savedListOffset = viewportPrefs.getInt("first_visible_offset", 0).coerceAtLeast(0)
    val workoutListState = rememberLazyListState(
        initialFirstVisibleItemIndex = savedListIndex,
        initialFirstVisibleItemScrollOffset = savedListOffset
    )

    // Bottom-nav changes dispose this screen without using the minimize gesture.
    // Persist the viewport there too, so returning resumes at the exact same place.
    DisposableEffect(workoutListState) {
        onDispose {
            viewportPrefs.edit()
                .putInt("first_visible_item", workoutListState.firstVisibleItemIndex)
                .putInt("first_visible_offset", workoutListState.firstVisibleItemScrollOffset)
                .apply()
        }
    }

    LaunchedEffect(addExerciseRequestKey) {
        if (addExerciseRequestKey > 0) {
            libraryReplaceExerciseId = null
            showExerciseLibrary = true
            onAddExerciseRequestConsumed()
        }
    }
    val sheetOffsetFraction = remember { Animatable(0f) }
    val sheetScope = rememberCoroutineScope()
    var sheetDismissing by remember { mutableStateOf(false) }
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }

    fun dismissWorkoutSheet() {
        if (sheetDismissing) return
        viewportPrefs.edit()
            .putInt("first_visible_item", workoutListState.firstVisibleItemIndex)
            .putInt("first_visible_offset", workoutListState.firstVisibleItemScrollOffset)
            .apply()
        sheetDismissing = true
        sheetScope.launch {
            sheetOffsetFraction.animateTo(
                1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
            )
            onMinimize()
        }
    }

    // No entrance animation when returning to an active workout. The sheet is
    // rendered directly at its restored viewport instead of sliding/scrolling into place.

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val keyboardOpen = WindowInsets.ime.getBottom(density) > 0
    val bellPlayer = remember { MediaPlayer.create(context, R.raw.workout_rest_bell_v2) }
    val vibratorManager = remember { context.getSystemService(VibratorManager::class.java) }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { bellPlayer?.release() }
        }
    }

    BackHandler(enabled = state.isWorkoutActive) { showLeaveDialog = true }

    // Resolve synchronously on the first composition. Starting this as false and
    // correcting it later in LaunchedEffect allowed the Yes/No dialog to flash for
    // one frame when a paused Spotify session was resumed.
    var musicPromptResolvedLocally by rememberSaveable(state.activeStartedAt) {
        mutableStateOf(workoutMusicEnabled != null || spotifyIsPlaying)
    }

    LaunchedEffect(spotifyIsPlaying, workoutMusicEnabled) {
        if (spotifyIsPlaying || workoutMusicEnabled != null) {
            musicPromptResolvedLocally = true
        }
    }

    val showWorkoutMusicPrompt =
        state.isWorkoutActive &&
                spotifyConnected &&
                workoutMusicEnabled == null &&
                !musicPromptResolvedLocally &&
                !spotifyIsPlaying &&
                !spotifyConnecting

    LaunchedEffect(state.activeStartedAt) {
        while (state.isWorkoutActive) {
            nowMillis = System.currentTimeMillis()
            delay(500L)
        }
    }

    LaunchedEffect(state.restEndsAt, nowMillis) {
        val end = state.restEndsAt ?: return@LaunchedEffect
        val remaining = ((end - nowMillis + 999L) / 1000L).coerceAtLeast(0L)
        if (remaining in setOf(6L, 4L, 2L) && lastVibratedSecond != remaining) {
            lastVibratedSecond = remaining
            runCatching {
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(150L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }
        if (nowMillis >= end && bellPlayedFor != end) {
            bellPlayedFor = end
            runCatching {
                bellPlayer?.setVolume(1.0f, 1.0f)
                bellPlayer?.seekTo(0)
                bellPlayer?.start()
            }
            repository.skipRest()
        }
    }

    val exercises = state.activeExercises
    val currentIndex = state.activeExerciseIndex.coerceIn(0, exercises.size)
    val currentExercise = exercises.getOrNull(currentIndex)
    val currentSets = currentExercise?.let { ex ->
        state.activeSetDrafts.filter { it.exerciseId == ex.id }.sortedBy { it.setNumber }
    }.orEmpty()
    val elapsedSeconds = state.activeStartedAt?.let { ((nowMillis - it) / 1000L).coerceAtLeast(0L) } ?: 0L
    val restRemaining = state.restEndsAt?.let { ((it - nowMillis + 999L) / 1000L).coerceAtLeast(0L) } ?: 0L

    LaunchedEffect(exercises.size, pendingScrollToAddedExercise) {
        if (pendingScrollToAddedExercise && exercises.isNotEmpty()) {
            // Wait for the newly-added exercise section to enter the LazyColumn,
            // then bring that exercise into view automatically.
            delay(120L)
            workoutListState.animateScrollToItem(exercises.size)
            pendingScrollToAddedExercise = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { sheetHeightPx = it.height.toFloat() }
            .graphicsLayer { translationY = size.height * sheetOffsetFraction.value }
            .background(LocalWorkoutUiPalette.current.Canvas)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(sheetDismissing) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                if (!sheetDismissing && (dragAmount > 0f || sheetOffsetFraction.value > 0f)) {
                                    change.consume()
                                    val height = sheetHeightPx.coerceAtLeast(1f)
                                    sheetScope.launch {
                                        sheetOffsetFraction.snapTo(
                                            (sheetOffsetFraction.value + dragAmount / height)
                                                .coerceIn(0f, 1f)
                                        )
                                    }
                                }
                            },
                            onDragEnd = {
                                sheetScope.launch {
                                    if (sheetOffsetFraction.value >= 0.35f) {
                                        sheetDismissing = true
                                        sheetOffsetFraction.animateTo(
                                            1f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                                        )
                                        onMinimize()
                                    } else {
                                        sheetOffsetFraction.animateTo(
                                            0f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                sheetScope.launch {
                                    sheetOffsetFraction.animateTo(
                                        0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                    )
                                }
                            }
                        )
                    }
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
            ) {
                WorkoutSessionHeader(
                    elapsedSeconds = elapsedSeconds,
                    templateName = state.activeLayoutTitle,
                    keyboardRestSeconds = if (keyboardOpen && restRemaining > 0L) restRemaining else null,
                    showFinish = false,
                    onFinish = { showFinishDialog = true },
                    onMinimize = { dismissWorkoutSheet() },
                    onCancel = { showCancelDialog = true }
                )
            }
            AnimatedVisibility(
                visible = showWorkoutMusicPrompt,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
            ) {
                WorkoutMusicPrompt(
                    hasSelectedPlaylist = hasSelectedSpotifyPlaylist,
                    spotifyConnected = spotifyConnected,
                    onYes = {
                        musicPromptResolvedLocally = true
                        onStartWorkoutMusic()
                    },
                    onNo = {
                        musicPromptResolvedLocally = true
                        onSkipWorkoutMusic()
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            if (reorderMode && exercises.isNotEmpty()) {
                ReorderExerciseMode(
                    exercises = exercises,
                    initialExerciseId = reorderInitialExerciseId,
                    onMove = repository::moveActiveExercise,
                    onDone = {
                        reorderMode = false
                        reorderInitialExerciseId = null
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = workoutListState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = if (restRemaining > 0L) 108.dp else 38.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (exercises.isEmpty()) {
                        item { AddExerciseLandingCard(exerciseCount = 0, onAddExercise = { libraryReplaceExerciseId = null; showExerciseLibrary = true }) }
                    } else {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Exercises",
                                    color = LocalWorkoutUiPalette.current.TextStrong,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                SmallAction("Reorder", onClick = {
                                    reorderInitialExerciseId = null
                                    reorderMode = true
                                })
                            }
                        }
                        items(exercises, key = { "exercise-section-${it.id}" }) { exercise ->
                            val exerciseSets = state.activeSetDrafts.filter { it.exerciseId == exercise.id }.sortedBy { it.setNumber }
                            ExerciseSection(
                                exercise = exercise,
                                exerciseSets = exerciseSets,
                                state = state,
                                useUi2SetQualityStyle = useUi2SetQualityStyle,
                                onEdit = { editExerciseId = exercise.id },
                                onReplace = { libraryReplaceExerciseId = exercise.id; showExerciseLibrary = true },
                                onRemoveExercise = { deleteExerciseId = exercise.id },
                                onLongPressExercise = {
                                    reorderInitialExerciseId = exercise.id
                                    reorderMode = true
                                },
                                onUpdateStrength = { setNumber, reps, kg -> repository.updateSet(exercise.id, setNumber, reps, kg) },
                                onUpdateSetQuality = { setNumber, quality -> repository.updateSetQuality(exercise.id, setNumber, quality) },
                                onToggleSet = { setNumber ->
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    repository.toggleSetComplete(exercise.id, setNumber)
                                },
                                onAddSet = { repository.addSet(exercise.id) },
                                onRemoveLastSet = { repository.removeSet(exercise.id) },
                                onUpdateCardio = { seconds, km, level -> repository.updateCardioDistance(exercise.id, seconds, km, level) },
                                onUpdateTimeOnly = { seconds -> repository.updateTimeOnly(exercise.id, seconds) },
                                onStartCardioRest = { repository.startRest(300) }
                            )
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Add Exercise now lives in the context-aware center action while a workout is open.
                                FinishWorkoutButton(onClick = { showFinishDialog = true })
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = restRemaining > 0L && !reorderMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            CompactRestTimer(
                remainingSeconds = restRemaining,
                initialTotalSeconds = state.restDurationSeconds.toLong(),
                onMinusFifteen = { repository.adjustRest(-15) },
                onPlusThirty = { repository.adjustRest(30) },
                onSkip = repository::skipRest
            )
        }
    }

    editExerciseId?.let { id ->
        val exercise = exercises.firstOrNull { it.id == id }
        if (exercise != null) {
            EditExerciseDialog(
                exercise = exercise,
                onDismiss = { editExerciseId = null },
                onSave = { name, equipment, exerciseType ->
                    repository.editActiveExerciseMetadata(id, name, equipment, exerciseType)
                    editExerciseId = null
                }
            )
        } else editExerciseId = null
    }

    deleteExerciseId?.let { id ->
        val exercise = exercises.firstOrNull { it.id == id }
        if (exercise != null) {
            AlertDialog(
                onDismissRequest = { deleteExerciseId = null },
                title = { Text("Delete exercise?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete ${exercise.name}? This will remove all sets recorded for this exercise.") },
                confirmButton = { com.rishabh.gymbros.ui.components.DialogActionPill("Delete", { repository.removeActiveExercise(id); deleteExerciseId = null }, destructive = true) },
                dismissButton = { com.rishabh.gymbros.ui.components.DialogActionPill("Cancel", { deleteExerciseId = null }) }
            )
        } else deleteExerciseId = null
    }

    if (showExerciseLibrary) {
        Dialog(
            onDismissRequest = { showExerciseLibrary = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(.94f)
                    .fillMaxHeight(.88f),
                shape = RoundedCornerShape(24.dp),
                color = LocalWorkoutUiPalette.current.Canvas
            ) {
                ExerciseLibraryScreen(
                    plannedExercises = state.plannedExercises,
                    addedExerciseNames = state.activeExercises.map { it.name }.toSet(),
                    pageTitle = if (libraryReplaceExerciseId != null) "Replace Exercise" else "Add Exercise",
                    onBack = { showExerciseLibrary = false },
                    onAdd = { item ->
                        val targetId = libraryReplaceExerciseId
                        if (targetId != null) {
                            val type = item.exerciseType
                            repository.replaceActiveExercise(
                                exerciseId = targetId,
                                name = item.name,
                                exerciseType = type,
                                equipment = item.equipment,
                                sets = if (type == WorkoutExerciseType.CARDIO_DISTANCE) 1 else 3,
                                sourceExerciseId = item.id
                            )
                            replaceExerciseId = null
                        } else {
                            repository.addLibraryMovement(item)
                            pendingScrollToAddedExercise = true
                        }
                        libraryReplaceExerciseId = null
                        showExerciseLibrary = false
                    },
                    onCreateCustom = { name, type, equipment, tag ->
                        repository.addCustomMovement(
                            name = name,
                            exerciseType = type,
                            equipment = equipment,
                            tag = tag
                        )
                        val targetId = libraryReplaceExerciseId
                        if (targetId != null) {
                            repository.replaceActiveExercise(
                                exerciseId = targetId,
                                name = name,
                                exerciseType = type,
                                equipment = equipment,
                                sets = if (type == WorkoutExerciseType.CARDIO_DISTANCE) 1 else 3,
                                sourceExerciseId = null
                            )
                            replaceExerciseId = null
                        } else {
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
                            pendingScrollToAddedExercise = true
                        }
                        libraryReplaceExerciseId = null
                        showExerciseLibrary = false
                    },
                    onDeleteCustom = { exerciseId -> repository.deleteCustomMovement(exerciseId) }
                )
            }
        }
    }

    if (showReplaceDialog && currentExercise != null) {
        val pastNames = state.sessions
            .sortedByDescending { it.endedAt }
            .flatMap { session -> state.sessionSetLogs[session.id].orEmpty().map { it.exerciseName } }
            .filter { it.isNotBlank() && !it.equals(currentExercise.name, ignoreCase = true) }
            .distinctBy { it.lowercase(Locale.ENGLISH) }
            .take(24)
        ReplaceExerciseDialog(
            currentName = currentExercise.name,
            pastExerciseNames = pastNames,
            onDismiss = { showReplaceDialog = false },
            onPickPast = { name ->
                showReplaceDialog = false
                repository.replaceActiveExercise(
                    exerciseId = currentExercise.id,
                    name = name,
                    exerciseType = currentExercise.exerciseType,
                    equipment = currentExercise.equipment,
                    sets = currentExercise.sets
                )
            },
            onCreateNew = {
                showReplaceDialog = false
                libraryReplaceExerciseId = currentExercise.id
                showExerciseLibrary = true
            }
        )
    }

    if (showIncompleteWarning) {
        AlertDialog(
            onDismissRequest = { showIncompleteWarning = false },
            containerColor = LocalWorkoutUiPalette.current.CardHigh,
            title = { Text("Sets still incomplete", color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.ExtraBold) },
            text = { Text("You haven't completed every set for this exercise. Move on anyway?", color = LocalWorkoutUiPalette.current.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showIncompleteWarning = false
                    if (pendingNextAction == "NEXT") {
                        repository.selectActiveExercise((state.activeExerciseIndex + 1).coerceAtMost(state.activeExercises.lastIndex))
                    } else {
                        repository.goToAddExercise()
                        libraryReplaceExerciseId = null
                        showExerciseLibrary = true
                    }
                }) { Text("Move on", color = LocalWorkoutUiPalette.current.Orange) }
            },
            dismissButton = { TextButton(onClick = { showIncompleteWarning = false }) { Text("Stay", color = LocalWorkoutUiPalette.current.TextSecondary) } }
        )
    }

    if (showFinishDialog) {
        val completed = state.activeSetDrafts.count { it.completed }
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            containerColor = LocalWorkoutUiPalette.current.CardHigh,
            title = { Text("Finish workout?", color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    "${exercises.size} exercise${if (exercises.size == 1) "" else "s"} • $completed/${state.activeSetDrafts.size} sets completed. Your completed work will be saved.",
                    color = LocalWorkoutUiPalette.current.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    enabled = exercises.isNotEmpty(),
                    onClick = { showFinishDialog = false; repository.finishWorkout() }
                ) { Text("Finish", color = LocalWorkoutUiPalette.current.Orange) }
            },
            dismissButton = { TextButton(onClick = { showFinishDialog = false }) { Text("Keep working", color = LocalWorkoutUiPalette.current.TextSecondary) } }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor = LocalWorkoutUiPalette.current.CardHigh,
            title = { Text("Cancel workout?", color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.ExtraBold) },
            text = { Text("This workout will be discarded and will not be added to history.", color = LocalWorkoutUiPalette.current.TextSecondary) },
            confirmButton = {
                com.rishabh.gymbros.ui.components.DialogActionPill(
                    text = "Discard workout",
                    onClick = {
                        showCancelDialog = false
                        onWorkoutDiscarded()
                        repository.cancelWorkout()
                    },
                    destructive = true
                )
            },
            dismissButton = { com.rishabh.gymbros.ui.components.DialogActionPill("Keep workout", { showCancelDialog = false }) }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = LocalWorkoutUiPalette.current.CardHigh,
            title = { Text("Leave workout running?", color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.ExtraBold) },
            text = { Text("Your active workout stays saved locally. You can return to Workout later.", color = LocalWorkoutUiPalette.current.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showLeaveDialog = false; onLeaveRunning() }) { Text("Leave running", color = LocalWorkoutUiPalette.current.Orange) }
            },
            dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("Stay", color = LocalWorkoutUiPalette.current.TextSecondary) } }
        )
    }
}

@Composable
private fun WorkoutSessionHeader(
    elapsedSeconds: Long,
    templateName: String?,
    keyboardRestSeconds: Long? = null,
    showFinish: Boolean,
    onFinish: () -> Unit,
    onMinimize: () -> Unit,
    onCancel: () -> Unit
) {
    var minimizing by remember { mutableStateOf(false) }
    val arrowOffset by animateFloatAsState(
        targetValue = if (minimizing) 24f else 0f,
        animationSpec = tween(180),
        label = "minimizeArrow"
    )

    LaunchedEffect(minimizing) {
        if (minimizing) {
            delay(180L)
            onMinimize()
            minimizing = false
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "WORKOUT",
                color = LocalWorkoutUiPalette.current.Orange,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatClock(elapsedSeconds),
                    color = LocalWorkoutUiPalette.current.TextStrong,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                keyboardRestSeconds?.let { remaining ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(LocalWorkoutUiPalette.current.OrangeTint)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "REST ${formatClock(remaining)}",
                            color = LocalWorkoutUiPalette.current.Orange,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            if (!templateName.isNullOrBlank()) {
                Text(
                    "Template • $templateName",
                    color = LocalWorkoutUiPalette.current.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { translationY = arrowOffset }
                .size(38.dp)
                .clip(CircleShape)
                .background(LocalWorkoutUiPalette.current.Elevated)
                .clickable(enabled = !minimizing) { minimizing = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Minimize workout",
                tint = LocalWorkoutUiPalette.current.TextStrong,
                modifier = Modifier.size(23.dp)
            )
        }

        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallAction("Cancel", onCancel, danger = true)
            if (showFinish) SmallAction("Finish", onFinish)
        }
    }
}

@Composable
internal fun SmallAction(text: String, onClick: () -> Unit, danger: Boolean = false) {
    val palette = LocalWorkoutUiPalette.current
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (danger) palette.CancelTint else palette.OrangeTint)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            color = if (danger) palette.CancelAccent else palette.Orange,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
internal fun AddExerciseLandingCard(exerciseCount: Int, onAddExercise: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(WorkoutCardShape)
            .background(LocalWorkoutUiPalette.current.Card)
            .border(1.dp, LocalWorkoutUiPalette.current.Orange.copy(alpha = .25f), WorkoutCardShape)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add Exercise", color = LocalWorkoutUiPalette.current.TextStrong, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(
            if (exerciseCount == 0) "Start by adding your first strength or cardio exercise." else "Add the next exercise whenever you're ready. There is no fixed last exercise.",
            color = LocalWorkoutUiPalette.current.TextSecondary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(LocalWorkoutUiPalette.current.Heat)
                .clickable(onClick = onAddExercise)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("+ Add Exercise", color = LocalWorkoutUiPalette.current.OnAccent, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ReorderExerciseMode(
    exercises: List<ActiveWorkoutExercise>,
    initialExerciseId: String?,
    onMove: (Int, Int) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var orderedExercises by remember(exercises.map { it.id }) { mutableStateOf(exercises) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }
    val reorderListState = rememberLazyListState()

    LaunchedEffect(initialExerciseId) {
        val selectedIndex = orderedExercises.indexOfFirst { it.id == initialExerciseId }
        if (selectedIndex >= 0) reorderListState.animateScrollToItem(selectedIndex)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Reorder exercises", color = LocalWorkoutUiPalette.current.TextStrong, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text("Long-press and drag", color = LocalWorkoutUiPalette.current.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            SmallAction("Done", onClick = onDone)
        }
        LazyColumn(
            state = reorderListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(orderedExercises, key = { "reorder-${it.id}" }) { exercise ->
                val index = orderedExercises.indexOfFirst { it.id == exercise.id }
                val dragging = draggingId == exercise.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = if (dragging) dragOffset else 0f
                            alpha = if (dragging) 0.90f else 1f
                            shadowElevation = if (dragging) 12f else 0f
                        }
                        .clip(WorkoutFieldShape)
                        .background(if (dragging) LocalWorkoutUiPalette.current.CardHigh else LocalWorkoutUiPalette.current.Card)
                        .border(
                            1.dp,
                            if (dragging) LocalWorkoutUiPalette.current.Orange.copy(alpha = .55f) else LocalWorkoutUiPalette.current.Border,
                            WorkoutFieldShape
                        )
                        .pointerInput(exercise.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = exercise.id
                                    dragStartIndex = orderedExercises.indexOfFirst { it.id == exercise.id }
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragStartIndex = -1
                                    dragOffset = 0f
                                    orderedExercises = exercises
                                },
                                onDragEnd = {
                                    val finalIndex = orderedExercises.indexOfFirst { it.id == exercise.id }
                                    if (dragStartIndex >= 0 && finalIndex >= 0 && dragStartIndex != finalIndex) {
                                        onMove(dragStartIndex, finalIndex)
                                    }
                                    draggingId = null
                                    dragStartIndex = -1
                                    dragOffset = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val threshold = 44.dp.toPx()
                                    val currentIndex = orderedExercises.indexOfFirst { it.id == exercise.id }
                                    when {
                                        dragOffset < -threshold && currentIndex > 0 -> {
                                            val mutable = orderedExercises.toMutableList()
                                            val moved = mutable.removeAt(currentIndex)
                                            mutable.add(currentIndex - 1, moved)
                                            orderedExercises = mutable
                                            dragOffset += threshold
                                        }
                                        dragOffset > threshold && currentIndex in 0 until orderedExercises.lastIndex -> {
                                            val mutable = orderedExercises.toMutableList()
                                            val moved = mutable.removeAt(currentIndex)
                                            mutable.add(currentIndex + 1, moved)
                                            orderedExercises = mutable
                                            dragOffset -= threshold
                                        }
                                    }
                                }
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.DragHandle, contentDescription = "Drag ${exercise.name}", tint = LocalWorkoutUiPalette.current.Orange, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f)) {
                        Text(exercise.name, color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(exerciseSubtitle(exercise), color = LocalWorkoutUiPalette.current.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseSection(
    exercise: ActiveWorkoutExercise,
    exerciseSets: List<WorkoutSetDraft>,
    state: WorkoutUiState,
    useUi2SetQualityStyle: Boolean = false,
    onEdit: () -> Unit,
    onReplace: () -> Unit,
    onRemoveExercise: () -> Unit,
    onLongPressExercise: () -> Unit,
    onUpdateStrength: (Int, Int, Double) -> Unit,
    onUpdateSetQuality: (Int, Int?) -> Unit,
    onToggleSet: (Int) -> Unit,
    onAddSet: () -> Unit,
    onRemoveLastSet: () -> Unit,
    onUpdateCardio: (Int, Double, Double) -> Unit,
    onUpdateTimeOnly: (Int) -> Unit,
    onStartCardioRest: () -> Unit
) {
    // Rows only turn green together, once every set of this exercise is done (2 sets or 3 sets alike).
    val allSetsCompleted = exerciseSets.isNotEmpty() && exerciseSets.all { it.completed }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AddedExerciseCard(
            exercise = exercise,
            completed = allSetsCompleted,
            onEdit = onEdit,
            onReplace = onReplace,
            onRemove = onRemoveExercise,
            onLongPress = onLongPressExercise,
        )
        when (exercise.exerciseType) {
            WorkoutExerciseType.STRENGTH -> {
                Column(verticalArrangement = Arrangement.spacedBy(if (allSetsCompleted) 0.dp else 4.dp)) {
                    StrengthColumnHeader()
                    exerciseSets.forEachIndexed { index, set ->
                        StrengthSetRow(
                            draft = set,
                            previous = previousSetFor(exercise.name, exercise.equipment, set.setNumber, state.sessions, state.sessionSetLogs),
                            onUpdate = { reps, kg -> onUpdateStrength(set.setNumber, reps, kg) },
                            onQualityChange = { quality -> onUpdateSetQuality(set.setNumber, quality) },
                            onToggle = { onToggleSet(set.setNumber) },
                            useUi2SetQualityStyle = useUi2SetQualityStyle,
                            exerciseCompleted = allSetsCompleted,
                            isFirstSet = index == 0,
                            isLastSet = index == exerciseSets.lastIndex
                        )
                    }
                }
                SetControls(exercise.sets, onAddSet, onRemoveLastSet)
            }
            WorkoutExerciseType.CARDIO_DISTANCE -> CardioDistanceEditor(
                exercise = exercise,
                draft = exerciseSets.firstOrNull(),
                previous = previousCardioFor(exercise.name, state.sessions, state.sessionMetrics, state.sessionSetLogs),
                best = bestCardioFor(exercise.name, state.sessions, state.sessionMetrics, state.sessionSetLogs),
                onUpdate = onUpdateCardio,
                onConfirmComplete = {
                    val wasCompleted = exerciseSets.firstOrNull()?.completed == true
                    onToggleSet(1)
                    if (!wasCompleted) onStartCardioRest()
                }
            )
            WorkoutExerciseType.CARDIO_ROPE -> {
                exerciseSets.forEach { set ->
                    RopeSetRow(
                        draft = set,
                        previous = previousSetFor(exercise.name, "", set.setNumber, state.sessions, state.sessionSetLogs),
                        onUpdate = { seconds -> onUpdateStrength(set.setNumber, seconds, 0.0) },
                        onToggle = { onToggleSet(set.setNumber) },
                        exerciseCompleted = allSetsCompleted
                    )
                }
                SetControls(exercise.sets, onAddSet, onRemoveLastSet)
            }
            WorkoutExerciseType.TIME_ONLY -> {
                exerciseSets.forEach { set ->
                    RopeSetRow(
                        draft = set,
                        previous = previousSetFor(exercise.name, "", set.setNumber, state.sessions, state.sessionSetLogs),
                        onUpdate = { seconds -> onUpdateStrength(set.setNumber, seconds, 0.0) },
                        onToggle = { onToggleSet(set.setNumber) },
                        exerciseCompleted = allSetsCompleted
                    )
                }
                SetControls(exercise.sets, onAddSet, onRemoveLastSet)
            }
        }
    }
}

private val RestCyan = Color(0xFF1FB6D9)
private val RestGreen = Color(0xFF2EE6A0)
private val RestRed = Color(0xFFFA2A55)
private val RestPurple = Color(0xFFA78BFA)

@Composable
private fun CompactRestTimer(
    remainingSeconds: Long,
    initialTotalSeconds: Long,
    onMinusFifteen: () -> Unit,
    onPlusThirty: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutUiPalette.current
    // Longest the countdown has been this rest, so -15 shrinks the bar and +30 grows the scale.
    var totalSeconds by remember { mutableLongStateOf(maxOf(initialTotalSeconds, remainingSeconds, 1L)) }
    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds > totalSeconds) totalSeconds = remainingSeconds
    }

    // Over 1 min: cyan. Last minute: green. Last 15 seconds: red.
    val phaseColor by animateColorAsState(
        targetValue = when {
            remainingSeconds <= 15L -> RestRed
            remainingSeconds <= 60L -> RestGreen
            else -> RestCyan
        },
        label = "restPhaseColor"
    )
    val digitColor by animateColorAsState(
        targetValue = if (remainingSeconds <= 60L) phaseColor else palette.TextStrong,
        label = "restDigitColor"
    )
    val progress by animateFloatAsState(
        targetValue = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "restProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(WorkoutFieldShape)
            .background(palette.Card)
            .border(1.dp, palette.Border, WorkoutFieldShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 10.dp, top = 7.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Rest",
                    color = palette.TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    formatClock(remainingSeconds),
                    color = digitColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = 24.sp,
                        lineHeight = 26.sp,
                        letterSpacing = (-0.5).sp,
                        fontFeatureSettings = "tnum"
                    )
                )
            }
            RestChip("\u221215", RestRed, onMinusFifteen, Modifier.width(46.dp))
            RestChip("+30", RestGreen, onPlusThirty, Modifier.width(46.dp))
            RestChip("Skip", RestPurple, onSkip, Modifier.width(54.dp))
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = .06f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(progress)
                .height(2.dp)
                .background(phaseColor)
        )
    }
}

@Composable
private fun RestChip(label: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = .16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = color, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
internal fun AddedExerciseCard(
    exercise: ActiveWorkoutExercise,
    completed: Boolean,
    onEdit: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(WorkoutFieldShape)
            .background(LocalWorkoutUiPalette.current.Card)
            .border(1.dp, LocalWorkoutUiPalette.current.Border, WorkoutFieldShape)
            .pointerInput(exercise.id) {
                detectTapGestures(onLongPress = { onLongPress() })
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val context = LocalContext.current
        val libraryItem = remember(exercise.sourceExerciseId, exercise.name) {
            ExerciseLibraryCatalog.items(context).firstOrNull { it.id == exercise.sourceExerciseId }
                ?: ExerciseLibraryCatalog.items(context).firstOrNull { it.name.equals(exercise.name, ignoreCase = true) }
        }
        if (libraryItem != null) {
            ExerciseAssetImage(item = libraryItem, size = 48)
        } else {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (completed) LocalWorkoutUiPalette.current.Orange else LocalWorkoutUiPalette.current.ElevatedHigh),
                contentAlignment = Alignment.Center
            ) {
                if (completed) Icon(painter = painterResource(R.drawable.ic_gymbros_tick), contentDescription = "Completed", tint = LocalWorkoutUiPalette.current.OnAccent, modifier = Modifier.size(20.dp))
                else Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = LocalWorkoutUiPalette.current.SoftOrange, modifier = Modifier.size(22.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(exercise.name, color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(exerciseSubtitle(exercise), color = LocalWorkoutUiPalette.current.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Icon(
            Icons.Outlined.Edit,
            contentDescription = "Edit exercise",
            tint = com.rishabh.gymbros.ui.theme.Ui2Colors.Cyan,
            modifier = Modifier.size(22.dp).clickable(onClick = onEdit)
        )
        Icon(
            Icons.Outlined.SwapHoriz,
            contentDescription = "Replace exercise",
            tint = WorkoutAccents.Purple,
            modifier = Modifier.size(22.dp).clickable(onClick = onReplace)
        )
        Icon(
            Icons.Outlined.DeleteOutline,
            contentDescription = "Remove exercise",
            tint = com.rishabh.gymbros.ui.theme.Ui2Colors.Danger,
            modifier = Modifier.size(22.dp).clickable(onClick = onRemove)
        )
    }
}

@Composable
private fun ExerciseHeader(
    exercise: ActiveWorkoutExercise,
    index: Int,
    total: Int,
    showReplace: Boolean,
    onBackToList: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(LocalWorkoutUiPalette.current.Card).clickable(onClick = onBackToList), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ArrowBack, "Exercise list", tint = LocalWorkoutUiPalette.current.TextStrong)
        }
        Column(Modifier.weight(1f)) {
            Text("EXERCISE ${index + 1} / $total", color = LocalWorkoutUiPalette.current.Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(exercise.name, color = LocalWorkoutUiPalette.current.TextStrong, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(exerciseTypeLabel(exercise), color = LocalWorkoutUiPalette.current.Orange, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showReplace) {
                Text(
                    text = "Replace",
                    color = LocalWorkoutUiPalette.current.Gold,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(LocalWorkoutUiPalette.current.OrangeTint)
                        .clickable(onClick = onReplace)
                        .padding(horizontal = 11.dp, vertical = 8.dp)
                )
            }
            Icon(Icons.Outlined.DeleteOutline, "Remove exercise", tint = LocalWorkoutUiPalette.current.DeleteAccent, modifier = Modifier.size(24.dp).clickable(onClick = onRemove))
        }
    }
}

@Composable
private fun StrengthColumnHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
        LabelCell("SET", .10f)
        LabelCell("PREVIOUS", .30f)
        LabelCell("KG", .20f)
        LabelCell("REPS", .20f)
        Spacer(Modifier.weight(.20f))
    }
}

@Composable
private fun RowScope.LabelCell(text: String, weight: Float) {
    Text(text, color = LocalWorkoutUiPalette.current.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(weight), textAlign = TextAlign.Center)
}

@Composable
private fun StrengthSetRow(
    draft: WorkoutSetDraft,
    previous: WorkoutSetRecord?,
    onUpdate: (Int, Double) -> Unit,
    onQualityChange: (Int?) -> Unit,
    onToggle: () -> Unit,
    useUi2SetQualityStyle: Boolean = false,
    exerciseCompleted: Boolean = false,
    isFirstSet: Boolean = false,
    isLastSet: Boolean = false
) {
    var reps by remember(draft.reps) { mutableStateOf(if (draft.reps > 0) draft.reps.toString() else "") }
    var kg by remember(draft.loadKg) { mutableStateOf(if (draft.loadKg > 0.0) formatDecimal(draft.loadKg) else "") }
    val repsValue = reps.toIntOrNull() ?: 0
    val kgValue = kg.toDoubleOrNull() ?: 0.0
    val canComplete = repsValue in 1..WorkoutRepository.MAX_REPS && kgValue > 0.0 && kgValue <= WorkoutRepository.MAX_KG

    val rowShape = if (exerciseCompleted) {
        when {
            isFirstSet && isLastSet -> WorkoutFieldShape
            isFirstSet -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
            isLastSet -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
            else -> RoundedCornerShape(0.dp)
        }
    } else WorkoutFieldShape

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(if (draft.completed) Color(0xFF38E89F).copy(alpha = 0.14f) else LocalWorkoutUiPalette.current.Card)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SetFeelBadge(
                setNumber = draft.setNumber,
                quality = draft.quality,
                onQualityChange = onQualityChange,
                modifier = Modifier.weight(.10f)
            )
            Text(previous?.let { "${formatDecimal(it.loadKg)}kg × ${it.reps}" } ?: "—", color = LocalWorkoutUiPalette.current.TextSecondary, modifier = Modifier.weight(.30f), textAlign = TextAlign.Center, fontSize = 12.sp)
            MiniNumberField(
                kg,
                { raw ->
                    kg = boundedDecimal(raw, WorkoutRepository.MAX_KG)
                    onUpdate(reps.toIntOrNull() ?: 0, kg.toDoubleOrNull() ?: 0.0)
                },
                Modifier.weight(.20f),
                KeyboardType.Decimal
            )
            MiniNumberField(
                reps,
                { raw ->
                    reps = boundedInt(raw, WorkoutRepository.MAX_REPS)
                    onUpdate(reps.toIntOrNull() ?: 0, kg.toDoubleOrNull() ?: 0.0)
                },
                Modifier.weight(.20f),
                KeyboardType.Number
            )
            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            draft.completed -> LocalWorkoutUiPalette.current.Orange
                            canComplete -> LocalWorkoutUiPalette.current.ElevatedHigh
                            else -> LocalWorkoutUiPalette.current.CardHigh
                        }
                    )
                    .clickable(enabled = draft.completed || canComplete, onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_gymbros_tick),
                    contentDescription = "Complete set",
                    tint = when {
                        draft.completed -> LocalWorkoutUiPalette.current.OnAccent
                        canComplete -> LocalWorkoutUiPalette.current.TextSecondary
                        else -> LocalWorkoutUiPalette.current.Dimmed
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private data class SetFeel(val label: String, val value: Int, val color: Color)

private val SetFeelLevels = listOf(
    SetFeel("Bad", 0, Color(0xFFFF5C5C)),
    SetFeel("Hard", 25, Color(0xFFFF9A3D)),
    SetFeel("Clean", 50, Color(0xFF38E89F)),
    SetFeel("Smooth", 75, Color(0xFF4C9AFF)),
    SetFeel("Easy", 100, Color(0xFFA78BFA))
)

private fun setFeelIndex(quality: Int?): Int? =
    quality?.let { ((it.coerceIn(0, 100) + 12) / 25).coerceIn(0, SetFeelLevels.lastIndex) }

private class BelowAnchorPositionProvider(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = anchorBounds.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        var y = anchorBounds.bottom + gapPx
        if (y + popupContentSize.height > windowSize.height) y = anchorBounds.top - popupContentSize.height - gapPx
        return IntOffset(x, y.coerceAtLeast(0))
    }
}

@Composable
private fun SetFeelBadge(
    setNumber: Int,
    quality: Int?,
    onQualityChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    val selected = setFeelIndex(quality)
    val ringColor = selected?.let { SetFeelLevels[it].color } ?: LocalWorkoutUiPalette.current.Border
    val gapPx = with(LocalDensity.current) { 6.dp.roundToPx() }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .border(2.dp, ringColor, CircleShape)
                .clickable { open = true },
            contentAlignment = Alignment.Center
        ) {
            Text(
                setNumber.toString(),
                color = LocalWorkoutUiPalette.current.TextStrong,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
        if (open) {
            Popup(
                popupPositionProvider = BelowAnchorPositionProvider(gapPx),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(LocalWorkoutUiPalette.current.CardHigh)
                        .border(1.dp, LocalWorkoutUiPalette.current.Border, RoundedCornerShape(14.dp))
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SetFeelLevels.forEachIndexed { index, level ->
                        val isSelected = selected == index
                        Column(
                            modifier = Modifier
                                .width(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onQualityChange(if (isSelected) null else level.value)
                                    open = false
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .then(if (isSelected) Modifier.border(1.5.dp, level.color, CircleShape) else Modifier)
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(level.color)
                            )
                            Text(
                                level.label,
                                color = if (isSelected) LocalWorkoutUiPalette.current.TextStrong else LocalWorkoutUiPalette.current.TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MiniNumberField(value: String, onValueChange: (String) -> Unit, modifier: Modifier, keyboardType: KeyboardType) {
    var focused by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(9.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(34.dp)
            .onFocusChanged { focused = it.isFocused }
            .border(if (focused) 1.5.dp else 1.dp, if (focused) scheme.primary else scheme.outline, shape),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.Bold),
        cursorBrush = SolidColor(scheme.primary),
        decorationBox = { innerTextField ->
            Box(Modifier.fillMaxSize().padding(horizontal = 4.dp), contentAlignment = Alignment.Center) { innerTextField() }
        }
    )
}

@Composable
internal fun SetControls(setCount: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ControlButton("− Remove set", enabled = setCount > 1, onClick = onRemove, modifier = Modifier.weight(1f), customBackground = Color(0xFF2A1520), customContent = Color(0xFFFF6B81))
        ControlButton("+ Add set", enabled = true, onClick = onAdd, modifier = Modifier.weight(1f), accent = true, customBackground = Color(0xFF10243A), customContent = Color(0xFF5AB0FF))
    }
}

@Composable
private fun ControlButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    customBackground: Color? = null,
    customContent: Color? = null
) {
    Box(
        modifier = modifier.clip(CircleShape).background(customBackground ?: if (accent) LocalWorkoutUiPalette.current.OrangeTint else LocalWorkoutUiPalette.current.CardHigh).clickable(enabled = enabled, onClick = onClick).padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (!enabled) LocalWorkoutUiPalette.current.Dimmed else customContent ?: if (accent) LocalWorkoutUiPalette.current.Orange else LocalWorkoutUiPalette.current.TextSecondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CardioDistanceEditor(
    exercise: ActiveWorkoutExercise,
    draft: WorkoutSetDraft?,
    previous: PreviousCardioStats?,
    best: PreviousCardioStats?,
    onUpdate: (Int, Double, Double) -> Unit,
    onConfirmComplete: () -> Unit
) {
    val initial = splitDuration(exercise.targetReps)
    var hours by remember(exercise.id, exercise.targetReps) { mutableStateOf(initial.first) }
    var minutes by remember(exercise.id, exercise.targetReps) { mutableStateOf(initial.second) }
    var seconds by remember(exercise.id, exercise.targetReps) { mutableStateOf(initial.third) }
    var km by remember(exercise.id, exercise.loadKg) {
        mutableStateOf(if (exercise.loadKg > 0.0) formatDecimal(exercise.loadKg) else "")
    }
    var level by remember(exercise.id, exercise.cardioLevel) {
        mutableStateOf(if (exercise.cardioLevel > 0.0) formatDecimal(exercise.cardioLevel) else "")
    }
    val previousParts = previous?.let { splitDuration(it.seconds) } ?: Triple("", "", "")
    val secondsValue = durationFromParts(hours, minutes, seconds)
    val kmValue = km.toDoubleOrNull() ?: 0.0
    val canComplete = secondsValue > 0 || kmValue > 0.0
    val completed = draft?.completed == true
    val bestText = best?.let {
        "${formatCardioDuration(it.seconds)} · ${formatDecimal(it.distanceKm)} km · Lv ${formatDecimal(it.level)}"
    } ?: "No completed ${exercise.name} session yet"

    fun push() = onUpdate(durationFromParts(hours, minutes, seconds), km.toDoubleOrNull() ?: 0.0, level.toDoubleOrNull() ?: 0.0)

    Column(
        modifier = Modifier.fillMaxWidth().clip(WorkoutCardShape)
            .background(if (completed) LocalWorkoutUiPalette.current.OrangeTint else LocalWorkoutUiPalette.current.Card)
            .border(1.dp, if (completed) LocalWorkoutUiPalette.current.Orange.copy(alpha = .55f) else LocalWorkoutUiPalette.current.Border, WorkoutCardShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cardio_trophy),
                contentDescription = "Personal best",
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp)
            )
            Text(
                if (best != null) "Best: $bestText" else bestText,
                color = if (best != null) LocalWorkoutUiPalette.current.Gold else LocalWorkoutUiPalette.current.Dimmed,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Time",
                modifier = Modifier.width(36.dp),
                color = LocalWorkoutUiPalette.current.Muted,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
            CompactCardioField(
                value = hours,
                onValueChange = { hours = boundedInt(it, 99); push() },
                placeholder = previousParts.first.ifBlank { "HR" },
                suffix = "hr",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            CompactCardioField(
                value = minutes,
                onValueChange = { minutes = boundedInt(it, 59); push() },
                placeholder = previousParts.second.ifBlank { "MIN" },
                suffix = "min",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            CompactCardioField(
                value = seconds,
                onValueChange = { seconds = boundedInt(it, 59); push() },
                placeholder = previousParts.third.ifBlank { "SEC" },
                suffix = "sec",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactCardioField(
                value = km,
                onValueChange = { km = boundedDecimal(it, WorkoutRepository.MAX_CARDIO_DISTANCE_KM); push() },
                placeholder = previous?.distanceKm?.takeIf { it > 0.0 }?.let { formatDecimal(it) } ?: "0",
                suffix = "km",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
                label = "Distance"
            )
            CompactCardioField(
                value = level,
                onValueChange = { level = boundedDecimal(it, WorkoutRepository.MAX_CARDIO_LEVEL); push() },
                placeholder = previous?.level?.takeIf { it > 0.0 }?.let { formatDecimal(it) } ?: "0",
                suffix = "",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
                label = "Level"
            )
        }

        val buttonTextColor = if (completed) LocalWorkoutUiPalette.current.OnAccent else if (canComplete) LocalWorkoutUiPalette.current.TextStrong else LocalWorkoutUiPalette.current.Dimmed
        Row(
            Modifier.fillMaxWidth().height(38.dp).clip(CircleShape)
                .background(if (completed) LocalWorkoutUiPalette.current.Orange else if (canComplete) LocalWorkoutUiPalette.current.ElevatedHigh else LocalWorkoutUiPalette.current.CardHigh)
                .clickable(enabled = completed || canComplete) { onConfirmComplete() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_gymbros_tick),
                contentDescription = null,
                tint = buttonTextColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                if (completed) "Cardio completed" else "Mark cardio completed",
                modifier = Modifier.padding(start = 6.dp),
                color = buttonTextColor,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
internal fun CompactCardioField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    suffix: String,
    keyboardType: KeyboardType,
    modifier: Modifier,
    label: String? = null
) {
    var focused by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(36.dp)
            .onFocusChanged { focused = it.isFocused }
            .border(if (focused) 1.5.dp else 1.dp, if (focused) scheme.primary else scheme.outline, shape),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center, color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.Bold),
        cursorBrush = SolidColor(scheme.primary),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (label != null) {
                    Text(label, color = LocalWorkoutUiPalette.current.Muted, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.weight(1f))
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = LocalWorkoutUiPalette.current.Dimmed, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    innerTextField()
                }
                if (value.isNotEmpty() && suffix.isNotEmpty()) {
                    Text(suffix, modifier = Modifier.padding(start = 3.dp), color = LocalWorkoutUiPalette.current.Muted, fontSize = 11.sp, maxLines = 1)
                }
                if (label == null) Spacer(Modifier.weight(1f))
            }
        }
    )
}

@Composable
private fun TimeOnlyEditor(
    exercise: ActiveWorkoutExercise,
    draft: WorkoutSetDraft?,
    previousSeconds: Int?,
    onUpdate: (Int) -> Unit,
    onToggle: () -> Unit
) {
    val initial = splitDuration(exercise.targetReps)
    var hours by remember(exercise.targetReps) { mutableStateOf(initial.first) }
    var minutes by remember(exercise.targetReps) { mutableStateOf(initial.second) }
    var seconds by remember(exercise.targetReps) { mutableStateOf(initial.third) }
    val totalSeconds = durationFromParts(hours, minutes, seconds)

    Column(
        modifier = Modifier.fillMaxWidth().clip(WorkoutCardShape).background(LocalWorkoutUiPalette.current.Card).border(1.dp, LocalWorkoutUiPalette.current.Border, WorkoutCardShape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Timed exercise", color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        Text(
            previousSeconds?.let { "Previous • ${formatCardioDuration(it)}" } ?: "Previous • No completed ${exercise.name} yet",
            color = if (previousSeconds != null) LocalWorkoutUiPalette.current.Gold else LocalWorkoutUiPalette.current.Dimmed,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (previousSeconds != null) FontWeight.Bold else FontWeight.Normal
        )
        DurationFields(
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            onHoursChange = { value -> hours = boundedInt(value, 99); onUpdate(durationFromParts(hours, minutes, seconds)) },
            onMinutesChange = { value -> minutes = boundedInt(value, 59); onUpdate(durationFromParts(hours, minutes, seconds)) },
            onSecondsChange = { value -> seconds = boundedInt(value, 59); onUpdate(durationFromParts(hours, minutes, seconds)) }
        )
        Box(
            modifier = Modifier.fillMaxWidth().clip(CircleShape).background(if (draft?.completed == true) LocalWorkoutUiPalette.current.Orange else if (totalSeconds > 0) LocalWorkoutUiPalette.current.ElevatedHigh else LocalWorkoutUiPalette.current.CardHigh).clickable(enabled = draft?.completed == true || totalSeconds > 0, onClick = onToggle).padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                if (draft?.completed == true) Icon(painter = painterResource(R.drawable.ic_gymbros_tick), contentDescription = null, tint = LocalWorkoutUiPalette.current.OnAccent, modifier = Modifier.size(20.dp))
                Text(
                    if (draft?.completed == true) "Exercise completed" else "Mark completed",
                    color = if (draft?.completed == true) LocalWorkoutUiPalette.current.OnAccent else if (totalSeconds > 0) LocalWorkoutUiPalette.current.TextStrong else LocalWorkoutUiPalette.current.Dimmed,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun DurationFields(
    hours: String,
    minutes: String,
    seconds: String,
    hoursHint: String = "HR",
    minutesHint: String = "MIN",
    secondsHint: String = "SEC",
    onHoursChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    onSecondsChange: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DurationPartField(hoursHint, hours, onHoursChange, Modifier.weight(1f))
        DurationPartField(minutesHint, minutes, onMinutesChange, Modifier.weight(1f))
        DurationPartField(secondsHint, seconds, onSecondsChange, Modifier.weight(1f))
    }
}

@Composable
private fun DurationPartField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center, color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.Bold),
        shape = WorkoutFieldShape
    )
}

internal fun splitDuration(totalSeconds: Int): Triple<String, String, String> {
    if (totalSeconds <= 0) return Triple("", "", "")
    val safe = totalSeconds.coerceIn(0, WorkoutRepository.MAX_CARDIO_SECONDS)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return Triple(if (hours > 0) hours.toString() else "", if (minutes > 0) minutes.toString() else "", if (seconds > 0) seconds.toString() else "")
}

internal fun durationFromParts(hours: String, minutes: String, seconds: String): Int {
    val h = hours.toIntOrNull()?.coerceIn(0, 99) ?: 0
    val m = minutes.toIntOrNull()?.coerceIn(0, 59) ?: 0
    val s = seconds.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return (h * 3600 + m * 60 + s).coerceIn(0, WorkoutRepository.MAX_CARDIO_SECONDS)
}

internal fun formatCardioDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val h = safe / 3600
    val m = (safe % 3600) / 60
    val s = safe % 60
    return when {
        h > 0 -> "%dh %02dm %02ds".format(h, m, s)
        m > 0 -> "%dm %02ds".format(m, s)
        else -> "${s}s"
    }
}

@Composable
private fun FullNumberField(label: String, value: String, onValueChange: (String) -> Unit, keyboardType: KeyboardType, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = modifier, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), shape = WorkoutFieldShape)
}

@Composable
private fun RopeSetRow(
    draft: WorkoutSetDraft,
    previous: WorkoutSetRecord?,
    onUpdate: (Int) -> Unit,
    onToggle: () -> Unit,
    exerciseCompleted: Boolean = false
) {
    var seconds by remember(draft.reps) { mutableStateOf(if (draft.reps > 0) draft.reps.toString() else "") }
    val canComplete = (seconds.toIntOrNull() ?: 0) > 0
    Row(
        modifier = Modifier.fillMaxWidth().clip(WorkoutFieldShape).background(if (exerciseCompleted) LocalWorkoutUiPalette.current.OrangeTint else LocalWorkoutUiPalette.current.Card).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text("Set ${draft.setNumber}", color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.Bold)
            Text("Previous ${previous?.reps?.let { "${it}s" } ?: "—"}", color = LocalWorkoutUiPalette.current.Dimmed, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
            value = seconds,
            onValueChange = { seconds = boundedInt(it, 3600); onUpdate(seconds.toIntOrNull() ?: 0) },
            modifier = Modifier.width(110.dp),
            label = { Text("Seconds") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = WorkoutFieldShape
        )
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (draft.completed) LocalWorkoutUiPalette.current.Orange else if (canComplete) LocalWorkoutUiPalette.current.ElevatedHigh else LocalWorkoutUiPalette.current.CardHigh)
                .clickable(enabled = draft.completed || canComplete, onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(R.drawable.ic_gymbros_tick), contentDescription = null, tint = if (draft.completed) LocalWorkoutUiPalette.current.OnAccent else if (canComplete) LocalWorkoutUiPalette.current.TextSecondary else LocalWorkoutUiPalette.current.Dimmed)
        }
    }
}

@Composable
private fun RestTimerCard(remainingSeconds: Long, onStart: () -> Unit, onMinus: () -> Unit, onPlus: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(WorkoutCardShape).background(LocalWorkoutUiPalette.current.Card).border(1.dp, LocalWorkoutUiPalette.current.Orange.copy(alpha = .2f), WorkoutCardShape).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Timer, null, tint = LocalWorkoutUiPalette.current.Orange)
                Text("Rest timer", color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.ExtraBold)
            }
            Text(if (remainingSeconds > 0) formatClock(remainingSeconds) else "2:00", color = LocalWorkoutUiPalette.current.Gold, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (remainingSeconds > 0) {
                ControlButton("−15s", true, onMinus, Modifier.weight(1f))
                ControlButton("Skip", true, onSkip, Modifier.weight(1f))
                ControlButton("+15s", true, onPlus, Modifier.weight(1f), accent = true)
            } else {
                ControlButton("Start 2:00 rest", true, onStart, Modifier.weight(1f), accent = true)
            }
        }
    }
}

@Composable
private fun WorkoutMusicPrompt(
    hasSelectedPlaylist: Boolean,
    spotifyConnected: Boolean,
    onYes: () -> Unit,
    onNo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LocalWorkoutUiPalette.current.CardHigh)
            .border(1.dp, LocalWorkoutUiPalette.current.Orange.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Start The Music?",
                color = LocalWorkoutUiPalette.current.TextStrong,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
            Text(
                if (hasSelectedPlaylist) "Play your selected Spotify playlist for this workout."
                else "Choose No for a silent workout. Select a Spotify playlist on Home before using Yes.",
                color = LocalWorkoutUiPalette.current.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        TextButton(onClick = onNo) {
            Text("No", color = LocalWorkoutUiPalette.current.TextSecondary, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (hasSelectedPlaylist) LocalWorkoutUiPalette.current.Orange else LocalWorkoutUiPalette.current.Elevated)
                .clickable(enabled = hasSelectedPlaylist, onClick = onYes)
                .padding(horizontal = 15.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (spotifyConnected) "Yes" else "Yes",
                color = if (hasSelectedPlaylist) LocalWorkoutUiPalette.current.OnAccent else LocalWorkoutUiPalette.current.Dimmed,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun NextExerciseButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(CircleShape).background(LocalWorkoutUiPalette.current.Heat).clickable(onClick = onClick).padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = LocalWorkoutUiPalette.current.OnAccent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SecondaryAddExerciseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(LocalWorkoutUiPalette.current.CardHigh)
            .border(1.dp, LocalWorkoutUiPalette.current.Orange.copy(alpha = .35f), CircleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("+ Add Exercise", color = LocalWorkoutUiPalette.current.Orange, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun FinishWorkoutButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(LocalWorkoutUiPalette.current.CardHigh)
            .border(1.dp, LocalWorkoutUiPalette.current.Gold.copy(alpha = .45f), CircleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Finish Workout", color = LocalWorkoutUiPalette.current.Gold, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ReplaceExerciseDialog(
    currentName: String,
    pastExerciseNames: List<String>,
    onDismiss: () -> Unit,
    onPickPast: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalWorkoutUiPalette.current.CardHigh,
        title = { Text("Replace $currentName", color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Choose an exercise from your workout history or create a new one.", color = LocalWorkoutUiPalette.current.TextSecondary, style = MaterialTheme.typography.bodySmall)
                Box(
                    modifier = Modifier.fillMaxWidth().clip(WorkoutFieldShape).background(LocalWorkoutUiPalette.current.OrangeTint).clickable(onClick = onCreateNew).padding(13.dp)
                ) {
                    Text("+ Create new exercise", color = LocalWorkoutUiPalette.current.Orange, fontWeight = FontWeight.Bold)
                }
                if (pastExerciseNames.isEmpty()) {
                    Text("No previous exercises found yet.", color = LocalWorkoutUiPalette.current.Dimmed, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("From past workouts", color = LocalWorkoutUiPalette.current.Muted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    pastExerciseNames.forEach { name ->
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(WorkoutFieldShape).background(LocalWorkoutUiPalette.current.Card).clickable { onPickPast(name) }.padding(12.dp)
                        ) {
                            Text(name, color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = LocalWorkoutUiPalette.current.TextSecondary) } }
    )
}

@Composable
private fun EditExerciseDialog(
    exercise: ActiveWorkoutExercise,
    onDismiss: () -> Unit,
    onSave: (String, String, WorkoutExerciseType) -> Unit
) {
    var name by rememberSaveable(exercise.id) { mutableStateOf(exercise.name) }
    var equipment by rememberSaveable(exercise.id) { mutableStateOf(exercise.equipment.ifBlank { "Barbell" }) }
    var exerciseType by rememberSaveable(exercise.id) { mutableStateOf(exercise.exerciseType) }
    val equipmentOptions = listOf("Barbell", "Dumbbell", "Machine", "Cable")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit exercise", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(60) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                if (exercise.exerciseType == WorkoutExerciseType.STRENGTH ||
                    exercise.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE ||
                    exercise.exerciseType == WorkoutExerciseType.TIME_ONLY) {
                    Text("Exercise type", color = LocalWorkoutUiPalette.current.Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (exercise.exerciseType == WorkoutExerciseType.STRENGTH) {
                            ChoiceChip(
                                text = "Strength",
                                selected = exerciseType == WorkoutExerciseType.STRENGTH,
                                modifier = Modifier.weight(1f)
                            ) { exerciseType = WorkoutExerciseType.STRENGTH }
                        } else {
                            ChoiceChip(
                                text = "Cardio",
                                selected = exerciseType == WorkoutExerciseType.CARDIO_DISTANCE,
                                modifier = Modifier.weight(1f)
                            ) { exerciseType = WorkoutExerciseType.CARDIO_DISTANCE }
                        }
                        ChoiceChip(
                            text = "Time only",
                            selected = exerciseType == WorkoutExerciseType.TIME_ONLY,
                            modifier = Modifier.weight(1f)
                        ) { exerciseType = WorkoutExerciseType.TIME_ONLY }
                    }
                }

                if (exerciseType == WorkoutExerciseType.STRENGTH) {
                    Text("Equipment", color = LocalWorkoutUiPalette.current.Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    equipmentOptions.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { option ->
                                ChoiceChip(option, equipment == option, Modifier.weight(1f)) { equipment = option }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.trim().isNotBlank(),
                onClick = { onSave(name.trim(), equipment, exerciseType) }
            ) { Text("Save", color = LocalWorkoutUiPalette.current.Orange) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = LocalWorkoutUiPalette.current.DismissText) } }
    )
}

@Composable
private fun ModeChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.clip(CircleShape).background(if (selected) LocalWorkoutUiPalette.current.OrangeTint else LocalWorkoutUiPalette.current.Elevated).clickable(onClick = onClick).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (selected) LocalWorkoutUiPalette.current.Orange else LocalWorkoutUiPalette.current.TextSecondary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SetQualitySlider(
    quality: Int?,
    onQualityChange: (Int?) -> Unit,
    ui2Style: Boolean = false
) {
    var trackWidth by remember { mutableFloatStateOf(1f) }
    val value = (quality ?: 50).coerceIn(0, 100) / 100f
    val qualityColors = listOf(
        Color(0xFFE53935),
        Color(0xFFFF7A00),
        Ui2Colors.Cyan,
        Color(0xFF8B5CF6)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (ui2Style) 12.dp else 12.dp)
            .onSizeChanged { trackWidth = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(trackWidth) {
                detectTapGestures { offset ->
                    onQualityChange(((offset.x / trackWidth).coerceIn(0f, 1f) * 100f).toInt())
                }
            }
            .pointerInput(trackWidth) {
                detectHorizontalDragGestures { change, _ ->
                    onQualityChange(((change.position.x / trackWidth).coerceIn(0f, 1f) * 100f).toInt())
                }
            }
    ) {
        if (ui2Style) {
            Row(
                modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                qualityColors.forEach { color ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        } else {
            Canvas(Modifier.fillMaxWidth().height(4.dp).align(Alignment.Center)) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(qualityColors),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer { translationX = (trackWidth - (if (ui2Style) 9.dp else 10.dp).toPx()) * value }
                .size(if (ui2Style) 9.dp else 10.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color.Black.copy(alpha = .35f), CircleShape)
        )
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.clip(WorkoutFieldShape).background(if (selected) LocalWorkoutUiPalette.current.OrangeTint else LocalWorkoutUiPalette.current.Card).border(1.dp, if (selected) LocalWorkoutUiPalette.current.Orange.copy(alpha = .45f) else LocalWorkoutUiPalette.current.Border, WorkoutFieldShape).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 9.dp)) {
        Text(text, color = if (selected) LocalWorkoutUiPalette.current.Orange else LocalWorkoutUiPalette.current.TextSecondary, style = MaterialTheme.typography.bodySmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

internal fun previousSetFor(
    exerciseName: String,
    equipment: String,
    setNumber: Int,
    sessions: List<com.rishabh.gymbros.features.workout.model.WorkoutSession>,
    setLogs: Map<Long, List<WorkoutSetRecord>>
): WorkoutSetRecord? {
    val latestSessionIds = sessions.sortedByDescending { it.endedAt }.map { it.id }
    latestSessionIds.forEach { sessionId ->
        val exact = setLogs[sessionId].orEmpty().firstOrNull {
            it.completed && it.exerciseName.equals(exerciseName, ignoreCase = true) &&
                    (equipment.isBlank() || it.equipment.equals(equipment, ignoreCase = true)) && it.setNumber == setNumber
        }
        if (exact != null) return exact
    }
    latestSessionIds.forEach { sessionId ->
        val any = setLogs[sessionId].orEmpty().firstOrNull { it.completed && it.exerciseName.equals(exerciseName, ignoreCase = true) &&
                (equipment.isBlank() || it.equipment.equals(equipment, ignoreCase = true)) }
        if (any != null) return any
    }
    return null
}

private fun exerciseSubtitle(exercise: ActiveWorkoutExercise): String = when (exercise.exerciseType) {
    WorkoutExerciseType.STRENGTH -> "${exercise.sets} sets • ${exercise.equipment.ifBlank { "Strength" }}"
    WorkoutExerciseType.CARDIO_DISTANCE -> if (exercise.targetReps > 0 || exercise.loadKg > 0.0) {
        "${formatCardioDuration(exercise.targetReps)} • ${formatDecimal(exercise.loadKg)} km • level ${formatDecimal(exercise.cardioLevel)}"
    } else {
        "Cardio • log time, distance and level"
    }
    WorkoutExerciseType.CARDIO_ROPE -> "${exercise.sets} sets • log time per set"
    WorkoutExerciseType.TIME_ONLY -> if (exercise.targetReps > 0) "Timed • ${formatCardioDuration(exercise.targetReps)}" else "Timed exercise • log duration"
}

private fun exerciseTypeLabel(exercise: ActiveWorkoutExercise): String = when (exercise.exerciseType) {
    WorkoutExerciseType.STRENGTH -> listOf(exercise.equipment, "Strength").filter { it.isNotBlank() }.joinToString(" • ")
    WorkoutExerciseType.CARDIO_DISTANCE -> "Cardio • HR / MIN / SEC • km • level"
    WorkoutExerciseType.CARDIO_ROPE -> "Cardio • battle ropes"
    WorkoutExerciseType.TIME_ONLY -> "Time only • HR / MIN / SEC"
}

private data class PreviousCardioStats(val seconds: Int, val distanceKm: Double, val level: Double)

private fun previousCardioFor(
    exerciseName: String,
    sessions: List<com.rishabh.gymbros.features.workout.model.WorkoutSession>,
    sessionMetrics: Map<Long, Map<String, Double>>,
    setLogs: Map<Long, List<WorkoutSetRecord>>
): PreviousCardioStats? {
    sessions.sortedByDescending { it.endedAt }.forEach { session ->
        val metrics = sessionMetrics[session.id].orEmpty()
        val seconds = metrics[WorkoutRepository.cardioMetricKey(exerciseName, WorkoutRepository.CARDIO_FIELD_SECONDS)]
        val minutes = metrics[WorkoutRepository.cardioMetricKey(exerciseName, WorkoutRepository.CARDIO_FIELD_MINUTES)]
        val distance = metrics[WorkoutRepository.cardioMetricKey(exerciseName, WorkoutRepository.CARDIO_FIELD_DISTANCE_KM)]
        val level = metrics[WorkoutRepository.cardioMetricKey(exerciseName, WorkoutRepository.CARDIO_FIELD_LEVEL)]
        if (seconds != null || minutes != null || distance != null || level != null) {
            val duration = seconds?.toInt() ?: (((minutes ?: 0.0) * 60.0).toInt())
            return PreviousCardioStats(duration, distance ?: 0.0, level ?: 0.0)
        }
        val fallback = setLogs[session.id].orEmpty().firstOrNull {
            it.completed && it.exerciseName.equals(exerciseName, ignoreCase = true)
        }
        if (fallback != null) return PreviousCardioStats(fallback.reps, fallback.loadKg, 0.0)
    }
    return null
}

private fun bestCardioFor(
    exerciseName: String,
    sessions: List<com.rishabh.gymbros.features.workout.model.WorkoutSession>,
    sessionMetrics: Map<Long, Map<String, Double>>,
    setLogs: Map<Long, List<WorkoutSetRecord>>
): PreviousCardioStats? {
    val values = sessions.mapNotNull { session ->
        val metrics = sessionMetrics[session.id].orEmpty()
        val seconds = metrics[WorkoutRepository.cardioMetricKey(exerciseName, WorkoutRepository.CARDIO_FIELD_SECONDS)]
        val minutes = metrics[WorkoutRepository.cardioMetricKey(exerciseName, WorkoutRepository.CARDIO_FIELD_MINUTES)]
        val distance = metrics[WorkoutRepository.cardioMetricKey(exerciseName, WorkoutRepository.CARDIO_FIELD_DISTANCE_KM)]
        val level = metrics[WorkoutRepository.cardioMetricKey(exerciseName, WorkoutRepository.CARDIO_FIELD_LEVEL)]
        if (seconds != null || minutes != null || distance != null || level != null) {
            PreviousCardioStats(seconds?.toInt() ?: (((minutes ?: 0.0) * 60.0).toInt()), distance ?: 0.0, level ?: 0.0)
        } else {
            setLogs[session.id].orEmpty().firstOrNull { it.completed && it.exerciseName.equals(exerciseName, ignoreCase = true) }
                ?.let { PreviousCardioStats(it.reps, it.loadKg, 0.0) }
        }
    }
    return values.maxWithOrNull(compareBy<PreviousCardioStats> { it.distanceKm }.thenByDescending { it.seconds }.thenBy { it.level })
}

private fun previousTimeOnlyFor(
    exerciseName: String,
    sessions: List<com.rishabh.gymbros.features.workout.model.WorkoutSession>,
    sessionMetrics: Map<Long, Map<String, Double>>,
    setLogs: Map<Long, List<WorkoutSetRecord>>
): Int? {
    sessions.sortedByDescending { it.endedAt }.forEach { session ->
        sessionMetrics[session.id].orEmpty()[WorkoutRepository.timeOnlyMetricKey(exerciseName)]?.let { return it.toInt().coerceAtLeast(0) }
        val fallback = setLogs[session.id].orEmpty().firstOrNull {
            it.completed && it.exerciseName.equals(exerciseName, ignoreCase = true)
        }
        if (fallback != null && fallback.reps > 0) return fallback.reps
    }
    return null
}

internal fun boundedInt(raw: String, max: Int): String {
    val cleaned = raw.filter { it.isDigit() }.take(5)
    if (cleaned.isBlank()) return ""
    return (cleaned.toIntOrNull() ?: 0).coerceIn(0, max).toString()
}

internal fun boundedDecimal(raw: String, max: Double): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    if (filtered.isBlank()) return ""
    val dot = filtered.indexOf('.')
    val normalized = if (dot < 0) {
        filtered.take(7)
    } else {
        filtered.substring(0, dot + 1) + filtered.substring(dot + 1).replace(".", "").take(2)
    }
    val value = normalized.toDoubleOrNull() ?: return normalized
    return if (value > max) formatDecimal(max) else normalized
}
internal fun formatDecimal(value: Double): String {
    val rounded = kotlin.math.round(value)
    return if (abs(value - rounded) < .001) rounded.toInt().toString() else String.format(Locale.ENGLISH, "%.1f", value)
}
private fun formatClock(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0L)
    val hours = s / 3600
    val minutes = (s % 3600) / 60
    val seconds = s % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}