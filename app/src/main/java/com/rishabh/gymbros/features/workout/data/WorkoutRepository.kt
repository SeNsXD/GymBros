package com.rishabh.gymbros.features.workout.data

import android.content.Context
import android.util.Base64
import com.rishabh.gymbros.features.settings.data.SettingsRepository
import com.rishabh.gymbros.features.workout.domain.WorkoutAnalytics
import com.rishabh.gymbros.features.workout.model.ActiveWorkoutExercise
import com.rishabh.gymbros.features.workout.model.ExerciseLibraryItem
import com.rishabh.gymbros.features.workout.model.PlannedExercise
import com.rishabh.gymbros.features.workout.model.WorkoutCategory
import com.rishabh.gymbros.features.workout.model.WorkoutCompletionSummary
import com.rishabh.gymbros.features.workout.model.WorkoutExerciseType
import com.rishabh.gymbros.features.workout.model.WorkoutLayoutExercise
import com.rishabh.gymbros.features.workout.model.WorkoutSetDraft
import com.rishabh.gymbros.features.workout.model.WorkoutUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import java.time.LocalDate
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

class WorkoutRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database = WorkoutDatabase(appContext)
    private val settingsRepository = SettingsRepository.getInstance(appContext)
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _state = MutableStateFlow(initialStateFromPreferences())
    val state: StateFlow<WorkoutUiState> = _state.asStateFlow()
    suspend fun reloadFromDisk() {
        mutex.withLock {
            _state.value = initialStateFromPreferences()
            refreshLocked()
        }
    }

    fun hardResetWorkoutData() {
        scope.launch {
            mutex.withLock {
                database.clearAllWorkoutData()
                preferences.edit().clear().apply()
                _state.value = WorkoutUiState(isLoading = false)
                refreshLocked()
            }
        }
    }

    fun hasActiveWorkout(): Boolean = preferences.getLong(KEY_ACTIVE_STARTED_AT, 0L) > 0L

    /** A chosen song belongs to this workout; previewing songs never calls this. */
    suspend fun attachMusicTrack(startedAt: Long, uri: String, title: String, artist: String): Boolean = mutex.withLock {
        if (preferences.getLong(KEY_ACTIVE_STARTED_AT, 0L) != startedAt || startedAt <= 0L ||
            !Regex("spotify:track:[A-Za-z0-9]{22}").matches(uri)) return@withLock false
        preferences.edit().putString(KEY_ACTIVE_MUSIC_URI, uri)
            .putString(KEY_ACTIVE_MUSIC_TITLE, title).putString(KEY_ACTIVE_MUSIC_ARTIST, artist).apply()
        true
    }

    fun toggleTodayRestDay() {
        scope.launch {
            mutex.withLock {
                val today = LocalDate.now().toEpochDay()
                val days = restDaysFromPreferences().toMutableSet()
                if (!days.add(today)) days.remove(today)
                preferences.edit().putString(KEY_REST_DAYS, days.sorted().joinToString(",")).apply()
                refreshLocked()
            }
        }
    }

    private fun restDaysFromPreferences(): Set<Long> = preferences
        .getString(KEY_REST_DAYS, null)
        .orEmpty()
        .split(',')
        .mapNotNull { it.trim().toLongOrNull() }
        .toSet()

    private fun initialStateFromPreferences(): WorkoutUiState {
        val startedAt = preferences.getLong(KEY_ACTIVE_STARTED_AT, 0L).takeIf { it > 0L }
        if (startedAt == null) return WorkoutUiState()
        val exercises = decodeActiveExercises(preferences.getString(KEY_ACTIVE_EXERCISES, null).orEmpty())
        val drafts = decodeDrafts(preferences.getString(KEY_ACTIVE_SET_DRAFTS, null).orEmpty())
        val now = System.currentTimeMillis()
        val restEndsAt = preferences.getLong(KEY_REST_ENDS_AT, 0L).takeIf { it > now }
        return WorkoutUiState(
            activeExercises = exercises,
            activeLayoutId = preferences.getLong(KEY_ACTIVE_LAYOUT_ID, 0L).takeIf { it > 0L },
            activeLayoutTitle = preferences.getString(KEY_ACTIVE_LAYOUT_TITLE, null),
            activeStartedAt = startedAt,
            activeSetDrafts = drafts,
            activeExerciseIndex = preferences.getInt(KEY_ACTIVE_EXERCISE_INDEX, 0).coerceIn(0, exercises.size),
            restEndsAt = restEndsAt,
            restDurationSeconds = preferences.getInt(KEY_REST_DURATION_SECONDS, DEFAULT_REST_SECONDS),
            isLoading = true
        )
    }

    init {
        scope.launch {
            mutex.withLock {
                applyPersonalCalorieCorrectionLocked()
                applyPersonalSongsPlayedCorrectionLocked()
                refreshLocked()
            }
        }
    }

    /**
     * One-time correction for workouts logged before cardio-only calorie tracking was enabled.
     * This only patches the two affected personal sessions, then permanently marks the migration
     * complete so future workout calories continue to come from the normal calculation.
     */
    private fun applyPersonalCalorieCorrectionLocked() {
        if (preferences.getBoolean(KEY_PERSONAL_CALORIE_FIX_2026_09_25, false)) return

        fun setDailyCardioCalories(date: LocalDate, targetCalories: Double) {
            val sessions = database.allSessions().filter { session ->
                java.time.Instant.ofEpochMilli(session.endedAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate() == date
            }
            if (sessions.isEmpty()) return

            // Make the requested number the total for the day even if more than one session exists.
            sessions.forEach { session ->
                database.insertSessionMetrics(session.id, mapOf(METRIC_CARDIO_CALORIES to 0.0))
            }
            val latestSession = sessions.maxByOrNull { it.endedAt } ?: return
            database.insertSessionMetrics(
                latestSession.id,
                mapOf(METRIC_CARDIO_CALORIES to targetCalories)
            )
        }

        // 25 Sep 2026: Cross Walk, 1 km in 8 min at level 15.
        // Net/active calorie estimate using the app's cardio MET method: ~84 kcal.
        setDailyCardioCalories(LocalDate.of(2026, 9, 25), 84.0)

        // 24 Sep 2026: preserve the previously recorded cardio burn.
        setDailyCardioCalories(LocalDate.of(2026, 9, 24), 375.0)

        preferences.edit().putBoolean(KEY_PERSONAL_CALORIE_FIX_2026_09_25, true).apply()
    }

    /**
     * One-time recovery for the 26 Sep 2026 Spotify workout count that was lost while
     * the song counter was being wired up. The completed Soundtrack Session quest
     * represented 8 qualifying workout songs, so restore those 8 to that day's latest
     * saved workout only when the day currently has no saved song count at all.
     */
    private fun applyPersonalSongsPlayedCorrectionLocked() {
        if (preferences.getBoolean(KEY_PERSONAL_SONG_FIX_2026_09_26, false)) return

        val targetDate = LocalDate.of(2026, 9, 26)
        val sessions = database.allSessions().filter { session ->
            java.time.Instant.ofEpochMilli(session.endedAt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate() == targetDate
        }
        if (sessions.isNotEmpty()) {
            var savedTotal = 0.0
            sessions.forEach { session ->
                val songCount = database.metricsForSession(session.id)[METRIC_SONGS_PLAYED]
                savedTotal += songCount?.toString()?.toDoubleOrNull() ?: 0.0
            }
            if (savedTotal <= 0.0) {
                val latest = sessions.maxByOrNull { it.endedAt }
                if (latest != null) {
                    database.insertSessionMetrics(latest.id, mapOf(METRIC_SONGS_PLAYED to 8.0))
                }
            }
        }
        preferences.edit().putBoolean(KEY_PERSONAL_SONG_FIX_2026_09_26, true).apply()
    }

    /** Kept for older screens/analytics. Workout v2 itself is intentionally category-free. */
    fun selectCategory(category: WorkoutCategory) {
        if (_state.value.isWorkoutActive) return
        _state.value = _state.value.copy(selectedCategory = category)
    }

    /** Starts a blank workout, or a workout populated from a saved template. */
    fun startWorkout(layoutId: Long? = null) {
        scope.launch {
            mutex.withLock {
                if (preferences.getLong(KEY_ACTIVE_STARTED_AT, 0L) > 0L) return@withLock

                val layout = layoutId?.let { id -> database.allLayouts().firstOrNull { it.id == id } }
                val exercises = layout?.exercises.orEmpty().mapIndexed { index, item ->
                    ActiveWorkoutExercise(
                        id = "active-${UUID.randomUUID()}",
                        name = item.name,
                        exerciseType = item.exerciseType,
                        sets = if (item.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE) 1 else item.sets.coerceIn(1, 99),
                        targetReps = item.targetReps.coerceIn(0, maxTargetForType(item.exerciseType)),
                        loadKg = when (item.exerciseType) {
                            WorkoutExerciseType.STRENGTH -> item.loadKg.coerceIn(0.0, MAX_KG)
                            WorkoutExerciseType.CARDIO_DISTANCE -> item.loadKg.coerceIn(0.0, MAX_CARDIO_DISTANCE_KM)
                            WorkoutExerciseType.CARDIO_ROPE, WorkoutExerciseType.TIME_ONLY -> 0.0
                        },
                        cardioLevel = if (item.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE) item.cardioLevel.coerceIn(0.0, MAX_CARDIO_LEVEL) else 0.0,
                        equipment = item.equipment
                    )
                }
                val drafts = createInitialDrafts(exercises)
                preferences.edit()
                    .putLong(KEY_ACTIVE_STARTED_AT, System.currentTimeMillis())
                    .putString(KEY_ACTIVE_EXERCISES, encodeActiveExercises(exercises))
                    .putString(KEY_ACTIVE_SET_DRAFTS, encodeDrafts(drafts))
                    .putInt(KEY_ACTIVE_EXERCISE_INDEX, if (exercises.isEmpty()) 0 else 0)
                    .apply {
                        if (layout != null) {
                            putLong(KEY_ACTIVE_LAYOUT_ID, layout.id)
                            putString(KEY_ACTIVE_LAYOUT_TITLE, layout.title)
                        } else {
                            remove(KEY_ACTIVE_LAYOUT_ID)
                            remove(KEY_ACTIVE_LAYOUT_TITLE)
                        }
                    }
                    .remove(KEY_REST_ENDS_AT)
                    .remove(KEY_REST_DURATION_SECONDS)
                    .apply()
                _state.value = _state.value.copy(lastCompletion = null)
                refreshLocked()
            }
        }
    }

    fun addActiveExercise(
        name: String,
        exerciseType: WorkoutExerciseType,
        equipment: String = "",
        sets: Int,
        targetReps: Int = 0,
        loadKg: Double = 0.0,
        cardioLevel: Double = 0.0,
        tag: String = ""
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val safeSets = when (exerciseType) {
            WorkoutExerciseType.CARDIO_DISTANCE -> 1
            WorkoutExerciseType.STRENGTH, WorkoutExerciseType.CARDIO_ROPE, WorkoutExerciseType.TIME_ONLY -> sets.coerceIn(1, 99)
        }
        val safeTarget = targetReps.coerceIn(0, maxTargetForType(exerciseType))
        val safeLoad = when (exerciseType) {
            WorkoutExerciseType.STRENGTH -> loadKg.coerceIn(0.0, MAX_KG)
            WorkoutExerciseType.CARDIO_DISTANCE -> loadKg.coerceIn(0.0, MAX_CARDIO_DISTANCE_KM)
            WorkoutExerciseType.CARDIO_ROPE, WorkoutExerciseType.TIME_ONLY -> 0.0
        }
        val safeLevel = if (exerciseType == WorkoutExerciseType.CARDIO_DISTANCE) cardioLevel.coerceIn(0.0, MAX_CARDIO_LEVEL) else 0.0
        val safeEquipment = if (exerciseType == WorkoutExerciseType.STRENGTH) equipment.trim().take(40) else ""

        scope.launch {
            mutex.withLock {
                if (preferences.getLong(KEY_ACTIVE_STARTED_AT, 0L) <= 0L) return@withLock
                val existingExercises = activeExercisesFromPreferences()
                val drafts = activeDraftsFromPreferences(existingExercises).toMutableList()
                val exercises = existingExercises.toMutableList()
                val exercise = ActiveWorkoutExercise(
                    id = "active-${UUID.randomUUID()}",
                    name = cleanName,
                    exerciseType = exerciseType,
                    sets = safeSets,
                    targetReps = safeTarget,
                    loadKg = safeLoad,
                    cardioLevel = safeLevel,
                    equipment = safeEquipment,
                    tag = tag.trim().take(24)
                )
                exercises += exercise
                drafts += createInitialDrafts(listOf(exercise))
                saveActiveExercises(exercises)
                saveDrafts(drafts)
                preferences.edit().putInt(KEY_ACTIVE_EXERCISE_INDEX, exercises.lastIndex).apply()
                refreshLocked()
            }
        }
    }

    fun removeActiveExercise(exerciseId: String) {
        scope.launch {
            mutex.withLock {
                if (!_state.value.isWorkoutActive) return@withLock
                val exercises = activeExercisesFromPreferences().filterNot { it.id == exerciseId }
                val drafts = activeDraftsFromPreferences(activeExercisesFromPreferences()).filterNot { it.exerciseId == exerciseId }
                saveActiveExercises(exercises)
                saveDrafts(drafts)
                val current = preferences.getInt(KEY_ACTIVE_EXERCISE_INDEX, 0)
                preferences.edit().putInt(KEY_ACTIVE_EXERCISE_INDEX, current.coerceAtMost(exercises.size)).apply()
                refreshLocked()
            }
        }
    }

    fun addSet(exerciseId: String) {
        scope.launch {
            mutex.withLock {
                val exercises = activeExercisesFromPreferences().toMutableList()
                val index = exercises.indexOfFirst { it.id == exerciseId }
                if (index < 0) return@withLock
                val exercise = exercises[index]
                if (exercise.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE) return@withLock
                val newSetCount = (exercise.sets + 1).coerceAtMost(99)
                exercises[index] = exercise.copy(sets = newSetCount)
                val drafts = activeDraftsFromPreferences(exercises).toMutableList()
                drafts += WorkoutSetDraft(
                    exerciseId = exerciseId,
                    setNumber = newSetCount,
                    reps = exercise.targetReps.coerceIn(0, MAX_REPS),
                    loadKg = if (exercise.exerciseType == WorkoutExerciseType.STRENGTH) exercise.loadKg.coerceIn(0.0, MAX_KG) else 0.0,
                    completed = false
                )
                saveActiveExercises(exercises)
                saveDrafts(drafts)
                refreshLocked()
            }
        }
    }

    fun removeSet(exerciseId: String) {
        scope.launch {
            mutex.withLock {
                val exercises = activeExercisesFromPreferences().toMutableList()
                val index = exercises.indexOfFirst { it.id == exerciseId }
                if (index < 0) return@withLock
                val exercise = exercises[index]
                if (exercise.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE || exercise.sets <= 1) return@withLock
                val removeNumber = exercise.sets
                exercises[index] = exercise.copy(sets = removeNumber - 1)
                val drafts = activeDraftsFromPreferences(exercises)
                    .filterNot { it.exerciseId == exerciseId && it.setNumber == removeNumber }
                    .map { draft ->
                        if (draft.exerciseId == exerciseId && draft.setNumber > removeNumber) draft.copy(setNumber = draft.setNumber - 1)
                        else draft
                    }
                saveActiveExercises(exercises)
                saveDrafts(drafts)
                refreshLocked()
            }
        }
    }

    /** Deletes one specific strength/rope set and compacts following set numbers. */
    fun deleteSet(exerciseId: String, setNumber: Int) {
        scope.launch {
            mutex.withLock {
                if (!_state.value.isWorkoutActive) return@withLock
                val exercises = activeExercisesFromPreferences().toMutableList()
                val exerciseIndex = exercises.indexOfFirst { it.id == exerciseId }
                if (exerciseIndex < 0) return@withLock
                val exercise = exercises[exerciseIndex]
                if (exercise.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE || exercise.sets <= 1
                ) return@withLock
                val drafts = activeDraftsFromPreferences(exercises)
                if (drafts.none { it.exerciseId == exerciseId && it.setNumber == setNumber }) return@withLock
                exercises[exerciseIndex] = exercise.copy(sets = exercise.sets - 1)
                val updated = drafts.mapNotNull { draft ->
                    when {
                        draft.exerciseId != exerciseId -> draft
                        draft.setNumber == setNumber -> null
                        draft.setNumber > setNumber -> draft.copy(setNumber = draft.setNumber - 1)
                        else -> draft
                    }
                }
                saveActiveExercises(exercises)
                saveDrafts(updated)
                refreshLocked()
            }
        }
    }

    /** Restores a deleted set at its exact former position, including values/completion. */
    fun restoreSet(deleted: WorkoutSetDraft) {
        scope.launch {
            mutex.withLock {
                if (!_state.value.isWorkoutActive) return@withLock
                val exercises = activeExercisesFromPreferences().toMutableList()
                val exerciseIndex = exercises.indexOfFirst { it.id == deleted.exerciseId }
                if (exerciseIndex < 0) return@withLock
                val exercise = exercises[exerciseIndex]
                if (exercise.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE) return@withLock
                val drafts = activeDraftsFromPreferences(exercises).toMutableList()
                for (i in drafts.indices) {
                    val draft = drafts[i]
                    if (draft.exerciseId == deleted.exerciseId && draft.setNumber >= deleted.setNumber) {
                        drafts[i] = draft.copy(setNumber = draft.setNumber + 1)
                    }
                }
                drafts += deleted
                exercises[exerciseIndex] = exercise.copy(sets = exercise.sets + 1)
                val ordered = exercises.flatMap { ex -> drafts.filter { it.exerciseId == ex.id }.sortedBy { it.setNumber } }
                saveActiveExercises(exercises)
                saveDrafts(ordered)
                refreshLocked()
            }
        }
    }

    /** Reorders the existing active exercise objects only; saved templates are untouched. */
    fun moveActiveExercise(fromIndex: Int, toIndex: Int) {
        scope.launch {
            mutex.withLock {
                if (!_state.value.isWorkoutActive) return@withLock
                val exercises = activeExercisesFromPreferences().toMutableList()
                if (fromIndex !in exercises.indices || toIndex !in exercises.indices || fromIndex == toIndex) return@withLock
                val selectedId = exercises.getOrNull(preferences.getInt(KEY_ACTIVE_EXERCISE_INDEX, 0))?.id
                val moved = exercises.removeAt(fromIndex)
                exercises.add(toIndex, moved)
                saveActiveExercises(exercises)
                val drafts = activeDraftsFromPreferences(exercises)
                saveDrafts(exercises.flatMap { ex -> drafts.filter { it.exerciseId == ex.id }.sortedBy { it.setNumber } })
                val selectedIndex = selectedId?.let { id -> exercises.indexOfFirst { it.id == id }.takeIf { it >= 0 } }
                    ?: preferences.getInt(KEY_ACTIVE_EXERCISE_INDEX, 0).coerceIn(0, exercises.size)
                preferences.edit().putInt(KEY_ACTIVE_EXERCISE_INDEX, selectedIndex).apply()
                refreshLocked()
            }
        }
    }

    fun updateSet(exerciseId: String, setNumber: Int, reps: Int, loadKg: Double) {
        scope.launch {
            mutex.withLock {
                if (!_state.value.isWorkoutActive) return@withLock
                val exercises = activeExercisesFromPreferences().toMutableList()
                val exerciseIndex = exercises.indexOfFirst { it.id == exerciseId }
                if (exerciseIndex < 0) return@withLock
                val exercise = exercises[exerciseIndex]
                val safeReps = reps.coerceIn(0, if (exercise.exerciseType == WorkoutExerciseType.TIME_ONLY || exercise.exerciseType == WorkoutExerciseType.CARDIO_ROPE) MAX_CARDIO_SECONDS else MAX_REPS)
                val safeLoad = loadKg.coerceIn(0.0, MAX_KG)
                val drafts = activeDraftsFromPreferences(exercises).toMutableList()
                val index = drafts.indexOfFirst { it.exerciseId == exerciseId && it.setNumber == setNumber }
                if (index < 0) return@withLock
                drafts[index] = drafts[index].copy(reps = safeReps, loadKg = safeLoad)
                // Keep data entry fast: editing a strength set pre-fills every later unfinished set.
                if (exercise.exerciseType == WorkoutExerciseType.STRENGTH || exercise.exerciseType == WorkoutExerciseType.TIME_ONLY) {
                    drafts.indices
                        .filter { i ->
                            drafts[i].exerciseId == exerciseId &&
                                    drafts[i].setNumber > setNumber &&
                                    !drafts[i].completed
                        }
                        .forEach { i -> drafts[i] = drafts[i].copy(reps = safeReps, loadKg = safeLoad) }
                }
                exercises[exerciseIndex] = when (exercise.exerciseType) {
                    WorkoutExerciseType.STRENGTH -> exercise.copy(targetReps = safeReps, loadKg = safeLoad)
                    WorkoutExerciseType.CARDIO_ROPE -> exercise.copy(targetReps = safeReps)
                    WorkoutExerciseType.CARDIO_DISTANCE -> exercise
                    WorkoutExerciseType.TIME_ONLY -> exercise.copy(targetReps = safeReps)
                }
                saveActiveExercises(exercises)
                saveDrafts(drafts)
                refreshLocked()
            }
        }
    }

    fun updateSetQuality(exerciseId: String, setNumber: Int, quality: Int?) {
        scope.launch {
            mutex.withLock {
                if (!_state.value.isWorkoutActive) return@withLock
                val exercises = activeExercisesFromPreferences()
                val drafts = activeDraftsFromPreferences(exercises).toMutableList()
                val index = drafts.indexOfFirst { it.exerciseId == exerciseId && it.setNumber == setNumber }
                if (index < 0) return@withLock
                drafts[index] = drafts[index].copy(quality = quality?.coerceIn(0, 100))
                saveDrafts(drafts)
                refreshLocked()
            }
        }
    }

    fun updateCardioDistance(exerciseId: String, durationSeconds: Int, distanceKm: Double, level: Double) {
        scope.launch {
            mutex.withLock {
                val exercises = activeExercisesFromPreferences().toMutableList()
                val index = exercises.indexOfFirst { it.id == exerciseId && it.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE }
                if (index < 0) return@withLock
                exercises[index] = exercises[index].copy(
                    targetReps = durationSeconds.coerceIn(0, MAX_CARDIO_SECONDS),
                    loadKg = distanceKm.coerceIn(0.0, MAX_CARDIO_DISTANCE_KM),
                    cardioLevel = level.coerceIn(0.0, MAX_CARDIO_LEVEL)
                )
                val drafts = activeDraftsFromPreferences(exercises).toMutableList()
                val draftIndex = drafts.indexOfFirst { it.exerciseId == exerciseId && it.setNumber == 1 }
                if (draftIndex >= 0) {
                    drafts[draftIndex] = drafts[draftIndex].copy(
                        reps = durationSeconds.coerceIn(0, MAX_CARDIO_SECONDS),
                        loadKg = distanceKm.coerceIn(0.0, MAX_CARDIO_DISTANCE_KM)
                    )
                }
                saveActiveExercises(exercises)
                saveDrafts(drafts)
                refreshLocked()
            }
        }
    }

    fun updateTimeOnly(exerciseId: String, durationSeconds: Int) {
        scope.launch {
            mutex.withLock {
                val safeSeconds = durationSeconds.coerceIn(0, MAX_CARDIO_SECONDS)
                val exercises = activeExercisesFromPreferences().toMutableList()
                val index = exercises.indexOfFirst { it.id == exerciseId && it.exerciseType == WorkoutExerciseType.TIME_ONLY }
                if (index < 0) return@withLock
                exercises[index] = exercises[index].copy(targetReps = safeSeconds, loadKg = 0.0, cardioLevel = 0.0, sets = 1)
                val drafts = activeDraftsFromPreferences(exercises).toMutableList()
                val draftIndex = drafts.indexOfFirst { it.exerciseId == exerciseId && it.setNumber == 1 }
                if (draftIndex >= 0) {
                    drafts[draftIndex] = drafts[draftIndex].copy(reps = safeSeconds, loadKg = 0.0)
                }
                saveActiveExercises(exercises)
                saveDrafts(drafts)
                refreshLocked()
            }
        }
    }

    fun toggleSetComplete(exerciseId: String, setNumber: Int) {
        scope.launch {
            mutex.withLock {
                if (!_state.value.isWorkoutActive) return@withLock
                val exercises = activeExercisesFromPreferences()
                val exercise = exercises.firstOrNull { it.id == exerciseId } ?: return@withLock
                val drafts = activeDraftsFromPreferences(exercises).toMutableList()
                val index = drafts.indexOfFirst { it.exerciseId == exerciseId && it.setNumber == setNumber }
                if (index < 0) return@withLock
                val becomesCompleted = !drafts[index].completed
                if (becomesCompleted) {
                    val draft = drafts[index]
                    val valid = when (exercise.exerciseType) {
                        WorkoutExerciseType.STRENGTH -> draft.reps in 1..MAX_REPS && draft.loadKg > 0.0 && draft.loadKg <= MAX_KG
                        WorkoutExerciseType.CARDIO_ROPE -> draft.reps > 0
                        WorkoutExerciseType.CARDIO_DISTANCE -> exercise.targetReps > 0 || exercise.loadKg > 0.0
                        WorkoutExerciseType.TIME_ONLY -> draft.reps > 0
                    }
                    if (!valid) return@withLock
                }
                drafts[index] = drafts[index].copy(completed = becomesCompleted)
                saveDrafts(drafts)
                if (becomesCompleted && (exercise.exerciseType == WorkoutExerciseType.STRENGTH || exercise.exerciseType == WorkoutExerciseType.TIME_ONLY)) {
                    startRestLocked(DEFAULT_REST_SECONDS)
                }
                refreshLocked()
            }
        }
    }

    fun toggleExercise(exerciseId: String) {
        scope.launch {
            mutex.withLock {
                val exercises = activeExercisesFromPreferences()
                val drafts = activeDraftsFromPreferences(exercises).toMutableList()
                val indices = drafts.indices.filter { drafts[it].exerciseId == exerciseId }
                if (indices.isEmpty()) return@withLock
                val complete = indices.any { !drafts[it].completed }
                indices.forEach { i -> drafts[i] = drafts[i].copy(completed = complete) }
                saveDrafts(drafts)
                refreshLocked()
            }
        }
    }

    /** Goes to the Add Exercise state rather than assuming a finite last exercise. */
    fun nextExercise() = goToAddExercise()

    fun goToAddExercise() {
        scope.launch {
            mutex.withLock {
                val exercises = activeExercisesFromPreferences()
                preferences.edit().putInt(KEY_ACTIVE_EXERCISE_INDEX, exercises.size).apply()
                refreshLocked()
            }
        }
    }

    fun previousExercise() {
        scope.launch {
            mutex.withLock {
                val exercises = activeExercisesFromPreferences()
                if (exercises.isEmpty()) return@withLock
                val current = preferences.getInt(KEY_ACTIVE_EXERCISE_INDEX, exercises.size)
                preferences.edit().putInt(KEY_ACTIVE_EXERCISE_INDEX, (current - 1).coerceAtLeast(0)).apply()
                refreshLocked()
            }
        }
    }

    fun selectActiveExercise(index: Int) {
        scope.launch {
            mutex.withLock {
                val exercises = activeExercisesFromPreferences()
                preferences.edit().putInt(KEY_ACTIVE_EXERCISE_INDEX, index.coerceIn(0, exercises.size)).apply()
                refreshLocked()
            }
        }
    }

    fun startRest(seconds: Int = DEFAULT_REST_SECONDS) {
        scope.launch {
            mutex.withLock {
                if (_state.value.isWorkoutActive) {
                    startRestLocked(seconds)
                    refreshLocked()
                }
            }
        }
    }

    fun adjustRest(secondsDelta: Int) {
        scope.launch {
            mutex.withLock {
                if (!_state.value.isWorkoutActive) return@withLock
                val now = System.currentTimeMillis()
                val currentEnd = preferences.getLong(KEY_REST_ENDS_AT, 0L).takeIf { it > now } ?: return@withLock
                val remaining = ((currentEnd - now + 999L) / 1000L).toInt().coerceAtLeast(0)
                val newRemaining = (remaining + secondsDelta).coerceIn(0, 30 * 60)
                if (newRemaining <= 0) {
                    preferences.edit().remove(KEY_REST_ENDS_AT).remove(KEY_REST_DURATION_SECONDS).apply()
                } else {
                    preferences.edit()
                        .putLong(KEY_REST_ENDS_AT, now + newRemaining * 1000L)
                        .putInt(KEY_REST_DURATION_SECONDS, newRemaining)
                        .apply()
                }
                refreshLocked()
            }
        }
    }

    fun skipRest() {
        scope.launch {
            mutex.withLock {
                preferences.edit().remove(KEY_REST_ENDS_AT).remove(KEY_REST_DURATION_SECONDS).apply()
                refreshLocked()
            }
        }
    }

    fun cancelWorkout() {
        scope.launch {
            mutex.withLock {
                clearActiveWorkoutPreferences()
                refreshLocked()
            }
        }
    }

    fun incrementActiveSongsPlayed() {
        if (!_state.value.isWorkoutActive) return
        val current = preferences.getInt(KEY_ACTIVE_SONGS_PLAYED, 0)
        preferences.edit().putInt(KEY_ACTIVE_SONGS_PLAYED, current + 1).apply()
    }

    fun activeSongsPlayed(): Int = preferences.getInt(KEY_ACTIVE_SONGS_PLAYED, 0)

    fun finishWorkout() {
        scope.launch {
            mutex.withLock {
                val startedAt = preferences.getLong(KEY_ACTIVE_STARTED_AT, 0L)
                if (startedAt <= 0L) return@withLock
                val exercises = activeExercisesFromPreferences()
                if (exercises.isEmpty()) return@withLock
                val drafts = activeDraftsFromPreferences(exercises)
                val now = System.currentTimeMillis()
                val durationSeconds = ((now - startedAt) / 1000L).coerceAtLeast(0L)

                val strengthIds = exercises.filter { it.exerciseType == WorkoutExerciseType.STRENGTH }.mapTo(hashSetOf()) { it.id }
                val strengthDrafts = drafts.filter { it.exerciseId in strengthIds }
                val volumeKg = strengthDrafts.sumOf { it.volumeKg }
                val completedIds = exercises.mapNotNull { exercise ->
                    val exerciseSets = drafts.filter { it.exerciseId == exercise.id }
                    if (exerciseSets.isNotEmpty() && exerciseSets.all { it.completed }) exercise.id else null
                }.toSet()
                val category = if (exercises.all { it.exerciseType != WorkoutExerciseType.STRENGTH }) WorkoutCategory.CARDIO else WorkoutCategory.STRENGTH

                val historicalSessions = database.allSessions()
                val historicalLogs = historicalSessions.associate { it.id to database.setLogsForSession(it.id) }
                val prCount = exercises
                    .filter { it.exerciseType == WorkoutExerciseType.STRENGTH }
                    .count { exercise ->
                        val previous = WorkoutAnalytics.previousRecordForExercise(exercise.name, historicalSessions, historicalLogs)
                        drafts.filter { it.exerciseId == exercise.id && it.completed }
                            .any { WorkoutAnalytics.potentialPersonalRecord(it, previous)?.isAnyPr == true }
                    }

                val totalCardioSeconds = exercises.sumOf { exercise ->
                    val exerciseDrafts = drafts.filter { it.exerciseId == exercise.id && it.completed }
                    when (exercise.exerciseType) {
                        WorkoutExerciseType.CARDIO_DISTANCE -> if (exerciseDrafts.isNotEmpty()) exercise.targetReps.toLong() else 0L
                        WorkoutExerciseType.CARDIO_ROPE -> exerciseDrafts.sumOf { it.reps.toLong().coerceAtLeast(0L) }
                        WorkoutExerciseType.STRENGTH, WorkoutExerciseType.TIME_ONLY -> 0L
                    }
                }
                val totalDistanceKm = exercises
                    .filter { it.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE }
                    .filter { exercise -> drafts.any { it.exerciseId == exercise.id && it.completed } }
                    .sumOf { it.loadKg }
                val estimatedCalories = estimateWorkoutCalories(
                    weightKg = settingsRepository.currentSettings.weightKg?.toDouble(),
                    durationSeconds = durationSeconds,
                    exercises = exercises,
                    drafts = drafts
                )
                val cardioCalories = estimateCardioCalories(
                    weightKg = settingsRepository.currentSettings.weightKg?.toDouble(),
                    exercises = exercises,
                    drafts = drafts
                )

                val sessionId = database.insertSession(
                    startedAt = startedAt,
                    endedAt = now,
                    durationSeconds = durationSeconds,
                    totalVolumeKg = volumeKg,
                    completedExercises = completedIds.size,
                    totalExercises = exercises.size,
                    category = category,
                    title = "Workout",
                    musicTrackUri = preferences.getString(KEY_ACTIVE_MUSIC_URI, null),
                    musicTrackTitle = preferences.getString(KEY_ACTIVE_MUSIC_TITLE, null),
                    musicTrackArtist = preferences.getString(KEY_ACTIVE_MUSIC_ARTIST, null)
                )
                database.insertSetLogs(
                    sessionId = sessionId,
                    exerciseNames = exercises.associate { it.id to it.name },
                    exerciseEquipment = exercises.associate { it.id to it.equipment },
                    drafts = drafts
                )
                val metrics = mutableMapOf(
                    METRIC_TOTAL_CARDIO_SECONDS to totalCardioSeconds.toDouble(),
                    METRIC_DISTANCE_KM to totalDistanceKm,
                    METRIC_ESTIMATED_CALORIES to estimatedCalories.toDouble(),
                    METRIC_CARDIO_CALORIES to cardioCalories.toDouble(),
                    METRIC_PR_COUNT to prCount.toDouble(),
                    METRIC_SONGS_PLAYED to preferences.getInt(KEY_ACTIVE_SONGS_PLAYED, 0).toDouble()
                )
                exercises
                    .filter { exercise -> drafts.any { it.exerciseId == exercise.id && it.completed } }
                    .forEach { exercise ->
                        when (exercise.exerciseType) {
                            WorkoutExerciseType.CARDIO_DISTANCE -> {
                                metrics[cardioMetricKey(exercise.name, CARDIO_FIELD_SECONDS)] = exercise.targetReps.toDouble()
                                metrics[cardioMetricKey(exercise.name, CARDIO_FIELD_MINUTES)] = exercise.targetReps.toDouble() / 60.0
                                metrics[cardioMetricKey(exercise.name, CARDIO_FIELD_DISTANCE_KM)] = exercise.loadKg
                                metrics[cardioMetricKey(exercise.name, CARDIO_FIELD_LEVEL)] = exercise.cardioLevel
                            }
                            WorkoutExerciseType.TIME_ONLY -> {
                                metrics[timeOnlyMetricKey(exercise.name)] = exercise.targetReps.toDouble()
                            }
                            else -> Unit
                        }
                    }
                database.insertSessionMetrics(sessionId, metrics)

                val sourceLayoutId = preferences.getLong(KEY_ACTIVE_LAYOUT_ID, 0L).takeIf { it > 0L }
                val sourceLayout = sourceLayoutId?.let { id -> database.allLayouts().firstOrNull { it.id == id } }
                val layoutExercises = exercises.mapIndexed { index, exercise ->
                    val exerciseDrafts = drafts.filter { it.exerciseId == exercise.id }.sortedBy { it.setNumber }
                    val savedExercise = sourceLayout?.exercises?.firstOrNull {
                        it.name.equals(exercise.name, ignoreCase = true) && it.exerciseType == exercise.exerciseType
                    }
                    val changedDraft = when (exercise.exerciseType) {
                        WorkoutExerciseType.STRENGTH -> exerciseDrafts.lastOrNull { draft ->
                            savedExercise != null &&
                                    (draft.reps != savedExercise.targetReps || abs(draft.loadKg - savedExercise.loadKg) >= 0.001)
                        }
                        WorkoutExerciseType.CARDIO_ROPE -> exerciseDrafts.lastOrNull { draft ->
                            savedExercise != null && draft.reps != savedExercise.targetReps
                        }
                        WorkoutExerciseType.CARDIO_DISTANCE, WorkoutExerciseType.TIME_ONLY -> null
                    }
                    val representative = changedDraft ?: exerciseDrafts.lastOrNull { it.completed } ?: exerciseDrafts.lastOrNull()
                    when (exercise.exerciseType) {
                        WorkoutExerciseType.STRENGTH -> exercise.toLayoutExercise(index).copy(
                            sets = exerciseDrafts.size.coerceAtLeast(1),
                            targetReps = representative?.reps?.coerceIn(0, MAX_REPS) ?: exercise.targetReps,
                            loadKg = representative?.loadKg?.coerceIn(0.0, MAX_KG) ?: exercise.loadKg
                        )
                        WorkoutExerciseType.CARDIO_ROPE -> exercise.toLayoutExercise(index).copy(
                            sets = exerciseDrafts.size.coerceAtLeast(1),
                            targetReps = representative?.reps?.coerceAtLeast(0) ?: exercise.targetReps
                        )
                        WorkoutExerciseType.CARDIO_DISTANCE, WorkoutExerciseType.TIME_ONLY -> exercise.toLayoutExercise(index)
                    }
                }
                val layoutChanged = sourceLayout?.let { templateStructureChanged(it.exercises, layoutExercises) } ?: false
                val valuesChanged = sourceLayout?.let { templateValuesChanged(it.exercises, layoutExercises) } ?: false
                val completion = WorkoutCompletionSummary(
                    sessionId = sessionId,
                    category = category,
                    durationSeconds = durationSeconds,
                    completedSets = drafts.count { it.completed },
                    totalSets = drafts.size,
                    completedExercises = completedIds.size,
                    totalExercises = exercises.size,
                    volumeKg = volumeKg,
                    metrics = metrics,
                    estimatedCalories = estimatedCalories,
                    totalCardioSeconds = totalCardioSeconds,
                    personalRecords = prCount,
                    exercises = layoutExercises,
                    sourceLayoutId = sourceLayoutId,
                    sourceLayoutTitle = sourceLayout?.title,
                    templateChanged = layoutChanged || valuesChanged,
                    templateLayoutChanged = layoutChanged,
                    templateValuesChanged = valuesChanged
                )

                clearActiveWorkoutPreferences()
                refreshLocked()
                _state.value = _state.value.copy(lastCompletion = completion)
            }
        }
    }

    fun saveLastCompletionAsLayout(title: String) {
        val cleanTitle = title.trim()
        val completion = _state.value.lastCompletion ?: return
        if (cleanTitle.isBlank() || completion.exercises.isEmpty()) return
        scope.launch {
            mutex.withLock {
                database.insertLayout(cleanTitle, completion.category, sanitizeLayoutExercises(completion.exercises))
                refreshLocked()
                _state.value = _state.value.copy(lastCompletion = null)
            }
        }
    }

    /** Full update: structure, exercise list, set counts and values. */
    fun updateCompletionSourceLayout() {
        val completion = _state.value.lastCompletion ?: return
        val layoutId = completion.sourceLayoutId ?: return
        scope.launch {
            mutex.withLock {
                val current = database.allLayouts().firstOrNull { it.id == layoutId } ?: return@withLock
                database.updateLayout(layoutId, current.title, completion.category, sanitizeLayoutExercises(completion.exercises))
                refreshLocked()
                _state.value = _state.value.copy(lastCompletion = null)
            }
        }
    }

    /** Structure-only update: apply the completed workout's exercise structure while preserving saved values for matching exercises. */
    fun updateCompletionSourceLayoutStructureOnly() {
        val completion = _state.value.lastCompletion ?: return
        val layoutId = completion.sourceLayoutId ?: return
        scope.launch {
            mutex.withLock {
                val current = database.allLayouts().firstOrNull { it.id == layoutId } ?: return@withLock
                val structured = completion.exercises.mapIndexed { index, fresh ->
                    val old = current.exercises.firstOrNull {
                        it.name.equals(fresh.name, ignoreCase = true) && it.exerciseType == fresh.exerciseType
                    }
                    if (old != null) {
                        fresh.copy(
                            id = 0L,
                            layoutId = 0L,
                            position = index,
                            targetReps = old.targetReps,
                            loadKg = old.loadKg,
                            cardioLevel = old.cardioLevel
                        )
                    } else {
                        // A newly-added exercise has no previous saved values to preserve.
                        fresh.copy(id = 0L, layoutId = 0L, position = index)
                    }
                }
                database.updateLayout(layoutId, current.title, current.category, sanitizeLayoutExercises(structured))
                refreshLocked()
                _state.value = _state.value.copy(lastCompletion = null)
            }
        }
    }

    /** Values-only update: preserve exercise order/type/set count, only update reps/kg/level. */
    fun updateCompletionSourceLayoutValuesOnly() {
        val completion = _state.value.lastCompletion ?: return
        val layoutId = completion.sourceLayoutId ?: return
        scope.launch {
            mutex.withLock {
                val current = database.allLayouts().firstOrNull { it.id == layoutId } ?: return@withLock
                val values = current.exercises.map { old ->
                    val fresh = completion.exercises.firstOrNull {
                        it.name.equals(old.name, ignoreCase = true) && it.exerciseType == old.exerciseType
                    }
                    if (fresh != null) {
                        old.copy(
                            sets = fresh.sets,
                            targetReps = fresh.targetReps,
                            loadKg = fresh.loadKg,
                            cardioLevel = fresh.cardioLevel
                        )
                    } else old
                }
                database.updateLayout(layoutId, current.title, current.category, values)
                refreshLocked()
                _state.value = _state.value.copy(lastCompletion = null)
            }
        }
    }

    fun dismissCompletionSummary() {
        _state.value = _state.value.copy(lastCompletion = null)
    }

    fun updateActiveMetric(key: String, value: Double) {
        // Retained for compatibility with older compiled screens; Workout v2 stores exercise-specific cardio data.
    }

    fun addCustomMovement(
        name: String,
        exerciseType: WorkoutExerciseType,
        equipment: String = "",
        tag: String
    ) {
        val clean = name.trim()
        val cleanTag = tag.trim().take(24)
        if (clean.isBlank() || cleanTag.isBlank()) return
        val category = if (cleanTag.equals("Cardio", ignoreCase = true)) WorkoutCategory.CARDIO else WorkoutCategory.STRENGTH
        val safeEquipment = if (exerciseType == WorkoutExerciseType.STRENGTH) equipment.trim().take(40) else ""
        scope.launch {
            mutex.withLock {
                database.insertPlannedExercise(
                    PlannedExercise(
                        id = "custom-${UUID.randomUUID()}",
                        name = clean,
                        sets = 1,
                        repsLabel = "",
                        targetReps = 0,
                        loadKg = 0.0,
                        category = category,
                        isCustom = true,
                        tag = cleanTag,
                        exerciseType = exerciseType,
                        equipment = safeEquipment
                    )
                )
                refreshLocked()
            }
        }
    }


    fun deleteCustomMovement(exerciseId: String) {
        if (!exerciseId.startsWith("custom-")) return
        scope.launch {
            mutex.withLock {
                database.deleteCustomPlannedExercise(exerciseId)
                refreshLocked()
            }
        }
    }

    fun addLibraryMovement(item: ExerciseLibraryItem) {
        scope.launch {
            mutex.withLock {
                val type = exerciseTypeOverride(item.id, item.name) ?: item.exerciseType
                val exercise = ActiveWorkoutExercise(
                    id = "active-${UUID.randomUUID()}",
                    name = item.name,
                    exerciseType = type,
                    sets = if (type == WorkoutExerciseType.CARDIO_DISTANCE) 1 else 3,
                    targetReps = item.defaultReps.coerceAtLeast(0),
                    loadKg = item.defaultLoadKg.coerceAtLeast(0.0),
                    equipment = if (type == WorkoutExerciseType.STRENGTH) item.equipment else "",
                    sourceExerciseId = item.id,
                    tag = if (type == WorkoutExerciseType.CARDIO_DISTANCE) "Cardio" else item.muscleGroup
                )

                if (_state.value.isWorkoutActive) {
                    val existingExercises = activeExercisesFromPreferences()
                    val drafts = activeDraftsFromPreferences(existingExercises).toMutableList()
                    val exercises = existingExercises.toMutableList().apply { add(exercise) }
                    drafts += createInitialDrafts(listOf(exercise))
                    saveActiveExercises(exercises)
                    saveDrafts(drafts)
                    preferences.edit().putInt(KEY_ACTIVE_EXERCISE_INDEX, exercises.lastIndex).apply()
                } else {
                    val exercises = listOf(exercise)
                    preferences.edit()
                        .putLong(KEY_ACTIVE_STARTED_AT, System.currentTimeMillis())
                        .putString(KEY_ACTIVE_EXERCISES, encodeActiveExercises(exercises))
                        .putString(KEY_ACTIVE_SET_DRAFTS, encodeDrafts(createInitialDrafts(exercises)))
                        .putInt(KEY_ACTIVE_EXERCISE_INDEX, 0)
                        .remove(KEY_ACTIVE_LAYOUT_ID)
                        .remove(KEY_ACTIVE_LAYOUT_TITLE)
                        .remove(KEY_REST_ENDS_AT)
                        .remove(KEY_REST_DURATION_SECONDS)
                        .apply()
                    _state.value = _state.value.copy(lastCompletion = null)
                }
                refreshLocked()
            }
        }
    }

    fun editActiveExerciseMetadata(
        exerciseId: String,
        name: String,
        equipment: String = "",
        exerciseType: WorkoutExerciseType? = null
    ) {
        val cleanName = name.trim().take(60)
        if (cleanName.isBlank()) return
        scope.launch {
            mutex.withLock {
                if (!_state.value.isWorkoutActive) return@withLock
                val exercises = activeExercisesFromPreferences().toMutableList()
                val index = exercises.indexOfFirst { it.id == exerciseId }
                if (index < 0) return@withLock
                val current = exercises[index]
                val newType = exerciseType ?: current.exerciseType
                val typeChanged = newType != current.exerciseType
                val updated = current.copy(
                    name = cleanName,
                    exerciseType = newType,
                    equipment = if (newType == WorkoutExerciseType.STRENGTH) equipment.trim().take(40) else "",
                    loadKg = if (newType == WorkoutExerciseType.STRENGTH) current.loadKg else 0.0,
                    targetReps = if (typeChanged) 0 else current.targetReps,
                    cardioLevel = if (newType == WorkoutExerciseType.CARDIO_DISTANCE) current.cardioLevel else 0.0
                )
                exercises[index] = updated

                if (typeChanged) {
                    val drafts = activeDraftsFromPreferences(activeExercisesFromPreferences())
                        .filterNot { it.exerciseId == exerciseId }
                        .toMutableList()
                    drafts += createInitialDrafts(listOf(updated))
                    val orderedDrafts = exercises.flatMap { exercise ->
                        drafts.filter { it.exerciseId == exercise.id }.sortedBy { it.setNumber }
                    }
                    saveDrafts(orderedDrafts)
                    saveExerciseTypeOverride(current, newType)
                }
                saveActiveExercises(exercises)
                refreshLocked()
            }
        }
    }

    private fun exerciseOverrideKey(sourceExerciseId: String?, name: String): String =
        sourceExerciseId?.takeIf { it.isNotBlank() } ?: "name:${name.trim().lowercase(Locale.ROOT)}"

    private fun exerciseTypeOverride(sourceExerciseId: String?, name: String): WorkoutExerciseType? {
        val key = exerciseOverrideKey(sourceExerciseId, name)
        val entry = preferences.getStringSet(KEY_EXERCISE_TYPE_OVERRIDES, emptySet())
            .orEmpty().firstOrNull { it.substringBefore('|') == key } ?: return null
        return runCatching { WorkoutExerciseType.valueOf(entry.substringAfter('|')) }.getOrNull()
    }

    private fun saveExerciseTypeOverride(exercise: ActiveWorkoutExercise, type: WorkoutExerciseType) {
        if (type != WorkoutExerciseType.STRENGTH && type != WorkoutExerciseType.TIME_ONLY) return
        val key = exerciseOverrideKey(exercise.sourceExerciseId, exercise.name)
        val entries = preferences.getStringSet(KEY_EXERCISE_TYPE_OVERRIDES, emptySet()).orEmpty().toMutableSet()
        entries.removeAll { it.substringBefore('|') == key }
        entries += "$key|${type.name}"
        preferences.edit().putStringSet(KEY_EXERCISE_TYPE_OVERRIDES, entries).apply()
    }

    fun replaceActiveExercise(
        exerciseId: String,
        name: String,
        exerciseType: WorkoutExerciseType,
        equipment: String = "",
        sets: Int = 3,
        sourceExerciseId: String? = null
    ) {
        val cleanName = name.trim().take(60)
        if (cleanName.isBlank()) return
        val resolvedExerciseType = exerciseTypeOverride(sourceExerciseId, cleanName) ?: exerciseType
        scope.launch {
            mutex.withLock {
                if (!_state.value.isWorkoutActive) return@withLock
                val exercises = activeExercisesFromPreferences().toMutableList()
                val index = exercises.indexOfFirst { it.id == exerciseId }
                if (index < 0) return@withLock
                val safeSets = when (resolvedExerciseType) {
                    WorkoutExerciseType.CARDIO_DISTANCE -> 1
                    WorkoutExerciseType.STRENGTH, WorkoutExerciseType.CARDIO_ROPE, WorkoutExerciseType.TIME_ONLY -> 3
                }
                val replacement = ActiveWorkoutExercise(
                    id = exerciseId,
                    name = cleanName,
                    exerciseType = resolvedExerciseType,
                    sets = safeSets,
                    targetReps = 0,
                    loadKg = 0.0,
                    cardioLevel = 0.0,
                    equipment = if (resolvedExerciseType == WorkoutExerciseType.STRENGTH) equipment.trim().take(40) else "",
                    sourceExerciseId = sourceExerciseId
                )
                exercises[index] = replacement
                val drafts = activeDraftsFromPreferences(activeExercisesFromPreferences())
                    .filterNot { it.exerciseId == exerciseId }
                    .toMutableList()
                drafts += createInitialDrafts(listOf(replacement))
                saveActiveExercises(exercises)
                val orderedDrafts = exercises.flatMap { exercise ->
                    drafts.filter { it.exerciseId == exercise.id }.sortedBy { it.setNumber }
                }
                saveDrafts(orderedDrafts)
                preferences.edit().putInt(KEY_ACTIVE_EXERCISE_INDEX, index).apply()
                refreshLocked()
            }
        }
    }

    fun saveCurrentAsLayout(title: String, category: WorkoutCategory) {
        val clean = title.trim()
        if (clean.isBlank()) return
        scope.launch {
            mutex.withLock {
                val exercises = if (_state.value.isWorkoutActive) {
                    activeExercisesFromPreferences().mapIndexed { i, e -> e.toLayoutExercise(i) }
                } else {
                    database.plannedExercises().filter { it.category == category }.mapIndexed { i, e -> e.toLayoutExercise(i) }
                }
                if (exercises.isNotEmpty()) database.insertLayout(clean, category, exercises)
                refreshLocked()
            }
        }
    }

    fun createLayout(title: String, category: WorkoutCategory, exercises: List<WorkoutLayoutExercise>) {
        val clean = title.trim()
        val safe = sanitizeLayoutExercises(exercises)
        if (clean.isBlank() || safe.isEmpty()) return
        scope.launch { mutex.withLock { database.insertLayout(clean, category, safe); refreshLocked() } }
    }

    fun updateLayout(layoutId: Long, title: String, category: WorkoutCategory, exercises: List<WorkoutLayoutExercise>) {
        val clean = title.trim()
        val safe = sanitizeLayoutExercises(exercises)
        if (layoutId <= 0L || clean.isBlank() || safe.isEmpty()) return
        scope.launch { mutex.withLock { database.updateLayout(layoutId, clean, category, safe); refreshLocked() } }
    }

    fun duplicateLayout(layoutId: Long) {
        scope.launch {
            mutex.withLock {
                val source = database.allLayouts().firstOrNull { it.id == layoutId } ?: return@withLock
                database.insertLayout("${source.title} Copy", source.category, source.exercises.mapIndexed { i, e -> e.copy(id = 0, layoutId = 0, position = i) })
                refreshLocked()
            }
        }
    }

    fun deleteWorkoutSession(sessionId: Long) {
        if (sessionId <= 0L) return
        scope.launch {
            mutex.withLock {
                database.deleteSession(sessionId)
                refreshLocked()
            }
        }
    }

    fun deleteLayout(layoutId: Long) {
        if (layoutId <= 0L) return
        scope.launch { mutex.withLock { database.deleteLayout(layoutId); refreshLocked() } }
    }

    /** From Saved Layouts, 'Use' now actually starts that template. */
    fun applyLayout(layoutId: Long) = startWorkout(layoutId)

    fun refresh() {
        scope.launch { mutex.withLock { refreshLocked() } }
    }

    private fun refreshLocked() {
        val allExercises = database.plannedExercises()
        val sessions = database.allSessions()
        val activeStartedAt = preferences.getLong(KEY_ACTIVE_STARTED_AT, 0L).takeIf { it > 0L }
        val activeExercises = if (activeStartedAt != null) activeExercisesFromPreferences() else emptyList()
        val drafts = if (activeStartedAt != null) activeDraftsFromPreferences(activeExercises) else emptyList()
        val currentIndex = if (activeStartedAt != null) {
            preferences.getInt(KEY_ACTIVE_EXERCISE_INDEX, if (activeExercises.isEmpty()) 0 else 0).coerceIn(0, activeExercises.size)
        } else 0
        val completedIds = activeExercises.mapNotNull { exercise ->
            val sets = drafts.filter { it.exerciseId == exercise.id }
            if (sets.isNotEmpty() && sets.all { it.completed }) exercise.id else null
        }.toSet()
        val now = System.currentTimeMillis()
        val restEndsAt = preferences.getLong(KEY_REST_ENDS_AT, 0L).takeIf { it > now }
        if (restEndsAt == null && preferences.contains(KEY_REST_ENDS_AT)) {
            preferences.edit().remove(KEY_REST_ENDS_AT).remove(KEY_REST_DURATION_SECONDS).apply()
        }
        val setLogs = sessions.associate { it.id to database.setLogsForSession(it.id) }
        val metrics = sessions.associate { it.id to database.metricsForSession(it.id) }
        val activeLayoutId = preferences.getLong(KEY_ACTIVE_LAYOUT_ID, 0L).takeIf { it > 0L }

        _state.value = _state.value.copy(
            plannedExercises = allExercises,
            sessions = sessions,
            sessionSetLogs = setLogs,
            layouts = database.allLayouts(),
            sessionMetrics = metrics,
            activeMetrics = emptyMap(),
            activeExercises = activeExercises,
            activeLayoutId = activeLayoutId,
            activeLayoutTitle = preferences.getString(KEY_ACTIVE_LAYOUT_TITLE, null),
            activeStartedAt = activeStartedAt,
            completedExerciseIds = completedIds,
            activeSetDrafts = drafts,
            activeExerciseIndex = currentIndex,
            restEndsAt = restEndsAt,
            restDurationSeconds = preferences.getInt(KEY_REST_DURATION_SECONDS, DEFAULT_REST_SECONDS),
            restDays = restDaysFromPreferences(),
            isLoading = false
        )
    }

    private fun sanitizeLayoutExercises(exercises: List<WorkoutLayoutExercise>): List<WorkoutLayoutExercise> {
        return exercises.mapIndexedNotNull { index, exercise ->
            val cleanName = exercise.name.trim()
            val safeSets = if (exercise.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE) 1 else exercise.sets
            val targetMax = maxTargetForType(exercise.exerciseType)
            val loadMax = if (exercise.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE) MAX_CARDIO_DISTANCE_KM else MAX_KG
            if (cleanName.isBlank() || safeSets !in 1..99 || exercise.targetReps !in 0..targetMax || exercise.loadKg !in 0.0..loadMax || exercise.cardioLevel < 0.0) null
            else exercise.copy(
                id = 0L,
                layoutId = 0L,
                position = index,
                name = cleanName,
                sets = safeSets,
                loadKg = if (exercise.exerciseType == WorkoutExerciseType.CARDIO_ROPE || exercise.exerciseType == WorkoutExerciseType.TIME_ONLY) 0.0 else exercise.loadKg,
                cardioLevel = if (exercise.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE) exercise.cardioLevel.coerceIn(0.0, MAX_CARDIO_LEVEL) else 0.0,
                equipment = if (exercise.exerciseType == WorkoutExerciseType.STRENGTH) exercise.equipment.trim().take(40) else ""
            )
        }
    }

    private fun PlannedExercise.toLayoutExercise(position: Int) = WorkoutLayoutExercise(
        position = position,
        name = name,
        sets = sets,
        targetReps = targetReps,
        loadKg = loadKg,
        exerciseType = if (category == WorkoutCategory.CARDIO) WorkoutExerciseType.CARDIO_DISTANCE else WorkoutExerciseType.STRENGTH
    )

    private fun ActiveWorkoutExercise.toLayoutExercise(position: Int) = WorkoutLayoutExercise(
        position = position,
        name = name,
        sets = sets,
        targetReps = targetReps,
        loadKg = loadKg,
        exerciseType = exerciseType,
        cardioLevel = cardioLevel,
        equipment = equipment
    )

    private fun createInitialDrafts(exercises: List<ActiveWorkoutExercise>): List<WorkoutSetDraft> {
        // Carry the last saved Clean Set quality forward per exercise + set number,
        // just like the UI already surfaces previous KG/reps. This reads completed
        // history only, so an abandoned/incomplete workout never becomes the default.
        val historicalSessions = database.allSessions().sortedByDescending { it.endedAt }
        val historicalLogs = historicalSessions.associate { it.id to database.setLogsForSession(it.id) }

        fun previousQuality(exercise: ActiveWorkoutExercise, setNumber: Int): Int? {
            historicalSessions.forEach { session ->
                val match = historicalLogs[session.id].orEmpty().firstOrNull { record ->
                    record.completed &&
                            record.exerciseName.equals(exercise.name, ignoreCase = true) &&
                            (exercise.equipment.isBlank() || record.equipment.equals(exercise.equipment, ignoreCase = true)) &&
                            record.setNumber == setNumber &&
                            record.quality != null
                }
                if (match?.quality != null) return match.quality.coerceIn(0, 100)
            }
            return null
        }

        return exercises.flatMap { exercise ->
            val count = if (exercise.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE) 1 else exercise.sets.coerceAtLeast(1)
            (1..count).map { setNumber ->
                WorkoutSetDraft(
                    exerciseId = exercise.id,
                    setNumber = setNumber,
                    reps = exercise.targetReps,
                    loadKg = if (exercise.exerciseType == WorkoutExerciseType.CARDIO_ROPE || exercise.exerciseType == WorkoutExerciseType.TIME_ONLY) 0.0 else exercise.loadKg,
                    completed = false,
                    quality = if (exercise.exerciseType == WorkoutExerciseType.STRENGTH) previousQuality(exercise, setNumber) else null
                )
            }
        }
    }

    private fun activeExercisesFromPreferences(): List<ActiveWorkoutExercise> =
        decodeActiveExercises(preferences.getString(KEY_ACTIVE_EXERCISES, null).orEmpty())

    private fun activeDraftsFromPreferences(exercises: List<ActiveWorkoutExercise>): List<WorkoutSetDraft> {
        val decoded = decodeDrafts(preferences.getString(KEY_ACTIVE_SET_DRAFTS, null).orEmpty())
        return if (decoded.isNotEmpty() || exercises.isEmpty()) decoded else createInitialDrafts(exercises).also(::saveDrafts)
    }

    private fun saveActiveExercises(exercises: List<ActiveWorkoutExercise>) {
        preferences.edit().putString(KEY_ACTIVE_EXERCISES, encodeActiveExercises(exercises)).apply()
    }

    private fun saveDrafts(drafts: List<WorkoutSetDraft>) {
        preferences.edit().putString(KEY_ACTIVE_SET_DRAFTS, encodeDrafts(drafts)).apply()
    }

    private fun encodeActiveExercises(exercises: List<ActiveWorkoutExercise>): String = exercises.joinToString("\n") { e ->
        listOf(
            e.id,
            encodeText(e.name),
            e.exerciseType.name,
            e.sets.toString(),
            e.targetReps.toString(),
            e.loadKg.toString(),
            e.cardioLevel.toString(),
            encodeText(e.equipment),
            encodeText(e.sourceExerciseId.orEmpty()),
            encodeText(e.tag)
        ).joinToString("|")
    }

    private fun decodeActiveExercises(encoded: String): List<ActiveWorkoutExercise> {
        if (encoded.isBlank()) return emptyList()
        return encoded.lineSequence().mapNotNull { line ->
            val p = line.split('|')
            if (p.size !in 7..10) return@mapNotNull null
            val type = runCatching { WorkoutExerciseType.valueOf(p[2]) }.getOrDefault(WorkoutExerciseType.STRENGTH)
            ActiveWorkoutExercise(
                id = p[0],
                name = decodeText(p[1]),
                exerciseType = type,
                sets = if (type == WorkoutExerciseType.CARDIO_DISTANCE) 1 else (p[3].toIntOrNull()?.coerceIn(1, 99) ?: 1),
                targetReps = p[4].toIntOrNull()?.coerceIn(0, maxTargetForType(type)) ?: 0,
                loadKg = when (type) {
                    WorkoutExerciseType.STRENGTH -> p[5].toDoubleOrNull()?.coerceIn(0.0, MAX_KG) ?: 0.0
                    WorkoutExerciseType.CARDIO_DISTANCE -> p[5].toDoubleOrNull()?.coerceIn(0.0, MAX_CARDIO_DISTANCE_KM) ?: 0.0
                    WorkoutExerciseType.CARDIO_ROPE, WorkoutExerciseType.TIME_ONLY -> 0.0
                },
                cardioLevel = if (type == WorkoutExerciseType.CARDIO_DISTANCE) p[6].toDoubleOrNull()?.coerceIn(0.0, MAX_CARDIO_LEVEL) ?: 0.0 else 0.0,
                equipment = if (type == WorkoutExerciseType.STRENGTH && p.size >= 8) decodeText(p[7]) else "",
                sourceExerciseId = if (p.size >= 9) decodeText(p[8]).ifBlank { null } else null,
                tag = if (p.size >= 10) decodeText(p[9]) else ""
            )
        }.toList()
    }

    private fun encodeDrafts(drafts: List<WorkoutSetDraft>): String = drafts.joinToString("\n") { d ->
        listOf(d.exerciseId, d.setNumber, d.reps, d.loadKg, if (d.completed) 1 else 0, d.quality ?: "").joinToString("|")
    }

    private fun decodeDrafts(encoded: String): List<WorkoutSetDraft> {
        if (encoded.isBlank()) return emptyList()
        return encoded.lineSequence().mapNotNull { line ->
            val p = line.split('|')
            if (p.size !in 5..6) return@mapNotNull null
            WorkoutSetDraft(
                exerciseId = p[0],
                setNumber = p[1].toIntOrNull() ?: return@mapNotNull null,
                reps = p[2].toIntOrNull() ?: return@mapNotNull null,
                loadKg = p[3].toDoubleOrNull() ?: return@mapNotNull null,
                completed = p[4] == "1",
                quality = p.getOrNull(5)?.toIntOrNull()?.coerceIn(0, 100)
            )
        }.toList()
    }

    private fun startRestLocked(seconds: Int) {
        val safe = seconds.coerceIn(0, 30 * 60)
        if (safe <= 0) {
            preferences.edit().remove(KEY_REST_ENDS_AT).remove(KEY_REST_DURATION_SECONDS).apply()
        } else {
            preferences.edit()
                .putLong(KEY_REST_ENDS_AT, System.currentTimeMillis() + safe * 1000L)
                .putInt(KEY_REST_DURATION_SECONDS, safe)
                .apply()
        }
    }

    private fun templateStructureChanged(
        saved: List<WorkoutLayoutExercise>,
        current: List<WorkoutLayoutExercise>
    ): Boolean {
        // Only adding an extra exercise should offer "Update Layout".
        // Replacements are session-only unless the user explicitly creates a larger layout.
        return current.size > saved.size
    }

    private fun templateValuesChanged(
        saved: List<WorkoutLayoutExercise>,
        current: List<WorkoutLayoutExercise>
    ): Boolean {
        return saved.any { old ->
            val fresh = current.firstOrNull {
                it.name.equals(old.name, ignoreCase = true) && it.exerciseType == old.exerciseType
            } ?: return@any false
            old.sets != fresh.sets ||
                    old.targetReps != fresh.targetReps ||
                    abs(old.loadKg - fresh.loadKg) >= 0.001 ||
                    abs(old.cardioLevel - fresh.cardioLevel) >= 0.001
        }
    }

    private fun encodeText(text: String): String = Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)
    private fun decodeText(text: String): String = runCatching { String(Base64.decode(text, Base64.NO_WRAP or Base64.URL_SAFE), Charsets.UTF_8) }.getOrDefault(text)

    private fun clearActiveWorkoutPreferences() {
        preferences.edit()
            .remove(KEY_ACTIVE_MUSIC_URI)
            .remove(KEY_ACTIVE_MUSIC_TITLE)
            .remove(KEY_ACTIVE_MUSIC_ARTIST)
            .remove(KEY_ACTIVE_STARTED_AT)
            .remove(KEY_ACTIVE_EXERCISES)
            .remove(KEY_ACTIVE_SET_DRAFTS)
            .remove(KEY_ACTIVE_EXERCISE_INDEX)
            .remove(KEY_ACTIVE_LAYOUT_ID)
            .remove(KEY_ACTIVE_LAYOUT_TITLE)
            .remove(KEY_REST_ENDS_AT)
            .remove(KEY_REST_DURATION_SECONDS)
            .remove(KEY_ACTIVE_CATEGORY)
            .remove(KEY_COMPLETED_IDS)
            .remove(KEY_ACTIVE_METRICS)
            .remove(KEY_ACTIVE_SONGS_PLAYED)
            .apply()
    }

    private fun maxTargetForType(type: WorkoutExerciseType): Int = when (type) {
        WorkoutExerciseType.STRENGTH -> MAX_REPS
        WorkoutExerciseType.CARDIO_DISTANCE, WorkoutExerciseType.CARDIO_ROPE, WorkoutExerciseType.TIME_ONLY -> MAX_CARDIO_SECONDS
    }

    private fun estimateCardioCalories(
        weightKg: Double?,
        exercises: List<ActiveWorkoutExercise>,
        drafts: List<WorkoutSetDraft>
    ): Int {
        val weight = (weightKg ?: 70.0).coerceIn(20.0, 500.0)
        val completedIds = drafts.filter { it.completed }.mapTo(hashSetOf()) { it.exerciseId }
        fun kcal(met: Double, seconds: Long): Double {
            if (seconds <= 0L) return 0.0
            return (met - 1.0).coerceAtLeast(0.0) * 3.5 * weight / 200.0 * (seconds / 60.0)
        }
        return exercises.filter { it.id in completedIds }.sumOf { exercise ->
            when (exercise.exerciseType) {
                WorkoutExerciseType.CARDIO_DISTANCE -> kcal(
                    cardioMetFor(exercise.name, exercise.cardioLevel, exercise.loadKg, exercise.targetReps.toLong()),
                    exercise.targetReps.toLong()
                )
                WorkoutExerciseType.CARDIO_ROPE -> kcal(8.0, drafts.filter { it.exerciseId == exercise.id && it.completed }.sumOf { it.reps.toLong() })
                else -> 0.0
            }
        }.roundToInt().coerceAtLeast(0)
    }

    private fun estimateWorkoutCalories(
        weightKg: Double?,
        durationSeconds: Long,
        exercises: List<ActiveWorkoutExercise>,
        drafts: List<WorkoutSetDraft>
    ): Int {
        val weight = (weightKg ?: 70.0).coerceIn(20.0, 500.0)
        val completedIds = drafts.filter { it.completed }.mapTo(hashSetOf()) { it.exerciseId }
        val cardioSeconds = exercises.filter { it.id in completedIds }.sumOf { exercise ->
            when (exercise.exerciseType) {
                WorkoutExerciseType.CARDIO_DISTANCE -> exercise.targetReps.toLong().coerceAtLeast(0L)
                WorkoutExerciseType.CARDIO_ROPE -> drafts.filter { it.exerciseId == exercise.id && it.completed }.sumOf { it.reps.toLong().coerceAtLeast(0L) }
                else -> 0L
            }
        }
        val timedStrengthSeconds = exercises.filter { it.id in completedIds && it.exerciseType == WorkoutExerciseType.TIME_ONLY }
            .sumOf { it.targetReps.toLong().coerceAtLeast(0L) }
        val remainingSeconds = (durationSeconds - cardioSeconds - timedStrengthSeconds).coerceAtLeast(0L)

        // Daily Completion reports active exercise calories (net), not resting calories.
        // Standard MET equation: ((MET - 1) * 3.5 * weightKg / 200) * minutes.
        fun kcal(met: Double, seconds: Long): Double {
            if (seconds <= 0L) return 0.0
            val minutes = seconds / 60.0
            val activeMet = (met - 1.0).coerceAtLeast(0.0)
            return activeMet * 3.5 * weight / 200.0 * minutes
        }

        val cardioCalories = exercises.filter { it.id in completedIds }.sumOf { exercise ->
            when (exercise.exerciseType) {
                WorkoutExerciseType.CARDIO_DISTANCE -> {
                    val seconds = exercise.targetReps.toLong().coerceAtLeast(0L)
                    val met = cardioMetFor(
                        name = exercise.name,
                        level = exercise.cardioLevel,
                        distanceKm = exercise.loadKg,
                        seconds = seconds
                    )
                    kcal(met, seconds)
                }
                WorkoutExerciseType.CARDIO_ROPE -> {
                    val seconds = drafts.filter { it.exerciseId == exercise.id && it.completed }.sumOf { it.reps.toLong().coerceAtLeast(0L) }
                    kcal(8.0, seconds)
                }
                else -> 0.0
            }
        }
        val timeOnlyCalories = kcal(4.0, timedStrengthSeconds)
        val hasStrength = exercises.any { it.exerciseType == WorkoutExerciseType.STRENGTH && it.id in completedIds }
        val baseMet = if (hasStrength) 4.5 else 2.0
        val baseCalories = kcal(baseMet, remainingSeconds)
        return (cardioCalories + timeOnlyCalories + baseCalories).roundToInt().coerceAtLeast(0)
    }

    /**
     * Estimates cardio intensity from the data Gym Bros already stores for a cardio exercise.
     * Distance + elapsed time are preferred when they are meaningful; machine level is used as
     * a secondary modifier because resistance scales are not standardized between machines.
     * Values are intentionally capped to avoid implausible calorie spikes from bad inputs.
     */
    private fun cardioMetFor(
        name: String,
        level: Double,
        distanceKm: Double,
        seconds: Long
    ): Double {
        val normalized = name.lowercase(Locale.ENGLISH)
        val hours = seconds.coerceAtLeast(0L) / 3600.0
        val speedKmh = if (hours > 0.0 && distanceKm > 0.0) distanceKm / hours else 0.0
        val safeSpeed = speedKmh.coerceIn(0.0, 40.0)
        val normalizedLevel = (level.coerceIn(0.0, 20.0) / 20.0)

        val isCrossTrainer = "cross" in normalized || "elliptical" in normalized

        val met = when {
            // Check cross-trainer BEFORE generic walking. Names such as "Cross Walk" contain
            // "walk" and previously fell into the walking branch (~6 MET) instead of the
            // cross-trainer intensity mapping.
            isCrossTrainer -> {
                // Cross-trainer/elliptical resistance scales are not standardized, so level is
                // used as an intensity band rather than treated as a precise physical workload.
                // Distance-derived pace only makes a small adjustment because elliptical
                // distance is also machine-specific.
                val levelValue = level.coerceIn(0.0, 20.0)
                val baseMet = when {
                    levelValue <= 0.0 -> 7.0
                    levelValue <= 5.0 -> 5.5
                    levelValue <= 10.0 -> 7.5
                    levelValue <= 14.0 -> 8.5
                    levelValue <= 17.0 -> 10.0
                    else -> 11.0
                }
                val paceAdjustment = when {
                    safeSpeed <= 0.0 -> 0.0
                    safeSpeed < 5.0 -> -0.5
                    safeSpeed < 7.0 -> -0.25
                    safeSpeed < 9.0 -> 0.0
                    safeSpeed < 11.0 -> 0.4
                    else -> 0.7
                }
                baseMet + paceAdjustment
            }

            "walk" in normalized -> when {
                safeSpeed <= 0.0 -> 3.5
                safeSpeed < 3.2 -> 2.8
                safeSpeed < 4.0 -> 3.0
                safeSpeed < 4.8 -> 3.5
                safeSpeed < 5.6 -> 4.3
                safeSpeed < 6.4 -> 5.0
                else -> 6.3
            }

            "treadmill" in normalized || "run" in normalized || "jog" in normalized -> when {
                safeSpeed <= 0.0 -> 8.0
                safeSpeed < 6.5 -> 5.0
                safeSpeed < 8.0 -> 7.0
                safeSpeed < 9.0 -> 8.3
                safeSpeed < 10.0 -> 9.0
                safeSpeed < 11.0 -> 9.8
                safeSpeed < 12.0 -> 10.5
                safeSpeed < 13.0 -> 11.0
                safeSpeed < 14.5 -> 11.8
                safeSpeed < 16.0 -> 12.8
                else -> 14.0
            }

            "cycle" in normalized || "bike" in normalized || "cycling" in normalized -> when {
                safeSpeed <= 0.0 -> 6.8
                safeSpeed < 16.0 -> 4.0
                safeSpeed < 19.0 -> 6.8
                safeSpeed < 22.5 -> 8.0
                safeSpeed < 25.5 -> 10.0
                safeSpeed < 30.5 -> 12.0
                else -> 14.0
            }


            "stair" in normalized || "step" in normalized -> 7.0 + (normalizedLevel * 2.0)
            "row" in normalized -> 6.0 + (normalizedLevel * 2.5)
            else -> 6.0 + (normalizedLevel * 1.5)
        }

        return met.coerceIn(2.0, 14.0)
    }

    companion object {
        private const val PREFS_NAME = "gymbros_workout_state"
        private const val KEY_ACTIVE_STARTED_AT = "active_started_at"
        private const val KEY_ACTIVE_MUSIC_URI = "active_music_uri"
        private const val KEY_ACTIVE_MUSIC_TITLE = "active_music_title"
        private const val KEY_ACTIVE_MUSIC_ARTIST = "active_music_artist"
        private const val KEY_ACTIVE_EXERCISES = "active_exercises_v2"
        private const val KEY_ACTIVE_SET_DRAFTS = "active_set_drafts"
        private const val KEY_ACTIVE_EXERCISE_INDEX = "active_exercise_index"
        private const val KEY_ACTIVE_LAYOUT_ID = "active_layout_id_v2"
        private const val KEY_ACTIVE_LAYOUT_TITLE = "active_layout_title_v2"
        private const val KEY_REST_ENDS_AT = "rest_ends_at"
        private const val KEY_REST_DURATION_SECONDS = "rest_duration_seconds"
        private const val KEY_REST_DAYS = "rest_days_epoch"
        private const val KEY_ACTIVE_CATEGORY = "active_category"
        private const val KEY_COMPLETED_IDS = "completed_exercise_ids"
        private const val KEY_ACTIVE_METRICS = "active_metrics"
        private const val KEY_EXERCISE_TYPE_OVERRIDES = "exercise_type_overrides_v1"

        const val DEFAULT_REST_SECONDS = 120
        const val MAX_REPS = 99
        const val MAX_KG = 999.0
        const val MAX_CARDIO_SECONDS = 359_999
        @Deprecated("Use MAX_CARDIO_SECONDS") const val MAX_CARDIO_MINUTES = 1440
        const val MAX_CARDIO_DISTANCE_KM = 999.9
        const val MAX_CARDIO_LEVEL = 100.0

        const val CARDIO_FIELD_SECONDS = "seconds"
        const val CARDIO_FIELD_MINUTES = "minutes"
        const val CARDIO_FIELD_DISTANCE_KM = "distance_km"
        const val CARDIO_FIELD_LEVEL = "level"

        fun timeOnlyMetricKey(exerciseName: String): String {
            val normalized = exerciseName.trim().lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "timed" }
            return "time_only_${normalized}_seconds"
        }

        fun cardioMetricKey(exerciseName: String, field: String): String {
            val normalized = exerciseName.trim().lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "cardio" }
            return "cardio_exercise_${normalized}_$field"
        }

        private const val KEY_ACTIVE_SONGS_PLAYED = "active_songs_played"
        private const val KEY_PERSONAL_CALORIE_FIX_2026_09_25 = "personal_calorie_fix_2026_09_25"
        private const val KEY_PERSONAL_SONG_FIX_2026_09_26 = "personal_song_fix_2026_09_26"

        const val METRIC_DISTANCE_KM = "distance_km"
        const val METRIC_TOTAL_CARDIO_SECONDS = "total_cardio_seconds"
        const val METRIC_ESTIMATED_CALORIES = "estimated_calories"
        const val METRIC_CARDIO_CALORIES = "cardio_calories"
        const val METRIC_PR_COUNT = "pr_count"
        const val METRIC_SONGS_PLAYED = "songs_played"

        // Retained keys keep old history screens/builds source-compatible.
        const val METRIC_PACE_SECONDS_PER_KM = "pace_seconds_per_km"
        const val METRIC_HIIT_WORK_SECONDS = "hiit_work_seconds"
        const val METRIC_HIIT_REST_SECONDS = "hiit_rest_seconds"
        const val METRIC_HIIT_TARGET_ROUNDS = "hiit_target_rounds"
        const val METRIC_HIIT_ROUNDS_COMPLETED = "hiit_rounds_completed"
        const val METRIC_MOBILITY_HOLD_SECONDS = "mobility_hold_seconds"

        @Volatile private var instance: WorkoutRepository? = null
        fun getInstance(context: Context): WorkoutRepository = instance ?: synchronized(this) {
            instance ?: WorkoutRepository(context.applicationContext).also { instance = it }
        }
    }
}
