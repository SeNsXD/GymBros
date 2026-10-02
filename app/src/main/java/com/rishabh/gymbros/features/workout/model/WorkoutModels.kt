package com.rishabh.gymbros.features.workout.model


enum class WorkoutExerciseType {
    STRENGTH,
    CARDIO_DISTANCE,
    CARDIO_ROPE,
    TIME_ONLY
}

enum class WorkoutCategory(
    val title: String,
    val subtitle: String
) {
    STRENGTH("Strength", "Push Day"),
    CARDIO("Cardio & Run", "Outdoor"),
    HIIT("HIIT & Core", "Intervals"),
    MOBILITY("Mobility", "Recovery")
}

data class PlannedExercise(
    val id: String,
    val name: String,
    val sets: Int,
    val repsLabel: String,
    val targetReps: Int,
    val loadKg: Double,
    val category: WorkoutCategory,
    val isCustom: Boolean = false,
    val tag: String = "",
    val exerciseType: WorkoutExerciseType = WorkoutExerciseType.STRENGTH,
    val equipment: String = ""
) {
    val estimatedVolumeKg: Double
        get() = sets.coerceAtLeast(0) * targetReps.coerceAtLeast(0) * loadKg.coerceAtLeast(0.0)
}

data class WorkoutSetDraft(
    val exerciseId: String,
    val setNumber: Int,
    val reps: Int,
    val loadKg: Double,
    val completed: Boolean,
    val quality: Int? = null
) {
    val volumeKg: Double
        get() = if (completed) reps.coerceAtLeast(0) * loadKg.coerceAtLeast(0.0) else 0.0
}

data class WorkoutSetRecord(
    val id: Long,
    val sessionId: Long,
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val reps: Int,
    val loadKg: Double,
    val completed: Boolean,
    val equipment: String = "",
    val quality: Int? = null
)

data class WorkoutSession(
    val id: Long,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long,
    val totalVolumeKg: Double,
    val completedExercises: Int,
    val totalExercises: Int,
    val category: WorkoutCategory,
    val title: String,
    val musicTrackUri: String? = null,
    val musicTrackTitle: String? = null,
    val musicTrackArtist: String? = null
)

/** Flexible numeric metrics attached to a completed workout (distance, pace, rounds, etc.). */
data class WorkoutSessionMetric(
    val sessionId: Long,
    val key: String,
    val value: Double
)

data class WorkoutLayoutExercise(
    val id: Long = 0L,
    val layoutId: Long = 0L,
    val position: Int,
    val name: String,
    val sets: Int,
    val targetReps: Int,
    val loadKg: Double,
    val exerciseType: WorkoutExerciseType = WorkoutExerciseType.STRENGTH,
    val cardioLevel: Double = 0.0,
    val equipment: String = "",
    val sourceExerciseId: String? = null
) {
    val repsLabel: String
        get() = "$targetReps reps"
}

data class WorkoutLayout(
    val id: Long,
    val title: String,
    val category: WorkoutCategory,
    val createdAt: Long,
    val exercises: List<WorkoutLayoutExercise>
)

data class ActiveWorkoutExercise(
    val id: String,
    val name: String,
    val exerciseType: WorkoutExerciseType,
    val sets: Int,
    val targetReps: Int,
    val loadKg: Double,
    val cardioLevel: Double = 0.0,
    val equipment: String = "",
    val sourceExerciseId: String? = null,
    val tag: String = ""
)

/** Ephemeral summary shown after a workout is safely written to local storage. */
data class WorkoutCompletionSummary(
    val sessionId: Long,
    val category: WorkoutCategory,
    val durationSeconds: Long,
    val completedSets: Int,
    val totalSets: Int,
    val completedExercises: Int,
    val totalExercises: Int,
    val volumeKg: Double,
    val metrics: Map<String, Double> = emptyMap(),
    val estimatedCalories: Int = 0,
    val totalCardioSeconds: Long = 0L,
    val personalRecords: Int = 0,
    val exercises: List<WorkoutLayoutExercise> = emptyList(),
    val sourceLayoutId: Long? = null,
    val sourceLayoutTitle: String? = null,
    val templateChanged: Boolean = false,
    val templateLayoutChanged: Boolean = false,
    val templateValuesChanged: Boolean = false
)

data class WorkoutUiState(
    val plannedExercises: List<PlannedExercise> = emptyList(),
    val sessions: List<WorkoutSession> = emptyList(),
    val sessionSetLogs: Map<Long, List<WorkoutSetRecord>> = emptyMap(),
    val layouts: List<WorkoutLayout> = emptyList(),
    val sessionMetrics: Map<Long, Map<String, Double>> = emptyMap(),
    val activeMetrics: Map<String, Double> = emptyMap(),
    val activeExercises: List<ActiveWorkoutExercise> = emptyList(),
    val activeLayoutId: Long? = null,
    val activeLayoutTitle: String? = null,
    val selectedCategory: WorkoutCategory = WorkoutCategory.STRENGTH,
    val activeStartedAt: Long? = null,
    val completedExerciseIds: Set<String> = emptySet(),
    val activeSetDrafts: List<WorkoutSetDraft> = emptyList(),
    val activeExerciseIndex: Int = 0,
    val restEndsAt: Long? = null,
    val restDurationSeconds: Int = 0,
    val lastCompletion: WorkoutCompletionSummary? = null,
    val restDays: Set<Long> = emptySet(),
    val isLoading: Boolean = true
) {
    val isWorkoutActive: Boolean
        get() = activeStartedAt != null
}

/** A reusable movement exposed by the built-in exercise library. */
data class ExerciseLibraryItem(
    val id: String,
    val name: String,
    val category: WorkoutCategory,
    val muscleGroup: String,
    val defaultSets: Int,
    val defaultReps: Int,
    val defaultLoadKg: Double = 0.0,
    val description: String,
    val equipment: String = "",
    val difficulty: String = "",
    val primaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val met: Double = 0.0,
    val imageAssetPath: String? = null,
    val exerciseType: WorkoutExerciseType = if (category == WorkoutCategory.CARDIO) WorkoutExerciseType.CARDIO_DISTANCE else WorkoutExerciseType.STRENGTH
)


data class ProgressionSuggestion(
    val exerciseName: String,
    val previousBestLoadKg: Double,
    val previousBestReps: Int,
    val suggestedLoadKg: Double,
    val message: String
)

data class PotentialPersonalRecord(
    val isNewLoadPr: Boolean,
    val isNewEstimatedOneRepMaxPr: Boolean,
    val previousBestLoadKg: Double,
    val previousBestEstimatedOneRepMaxKg: Double
) {
    val isAnyPr: Boolean
        get() = isNewLoadPr || isNewEstimatedOneRepMaxPr
}

data class ExercisePersonalRecord(
    val exerciseName: String,
    val bestLoadKg: Double,
    val bestReps: Int,
    val bestEstimatedOneRepMaxKg: Double,
    val bestSetVolumeKg: Double,
    val completedSets: Int,
    val totalVolumeKg: Double,
    val lastPerformedAt: Long
)

data class TrainingPeriodSummary(
    val sessionCount: Int = 0,
    val durationSeconds: Long = 0L,
    val volumeKg: Double = 0.0,
    val completedSets: Int = 0
)

data class DailyTrainingPoint(
    val epochDay: Long,
    val volumeKg: Double,
    val durationSeconds: Long,
    val sessionCount: Int
)

data class WorkoutAnalyticsSnapshot(
    val currentPeriod: TrainingPeriodSummary = TrainingPeriodSummary(),
    val previousPeriod: TrainingPeriodSummary = TrainingPeriodSummary(),
    val dailyPoints: List<DailyTrainingPoint> = emptyList(),
    val personalRecords: List<ExercisePersonalRecord> = emptyList(),
    val categoryVolumeKg: Map<WorkoutCategory, Double> = emptyMap(),
    val categoryDurationSeconds: Map<WorkoutCategory, Long> = emptyMap()
)
