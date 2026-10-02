package com.rishabh.gymbros.features.workout.domain

import com.rishabh.gymbros.features.workout.model.DailyTrainingPoint
import com.rishabh.gymbros.features.workout.model.ExercisePersonalRecord
import com.rishabh.gymbros.features.workout.model.PlannedExercise
import com.rishabh.gymbros.features.workout.model.PotentialPersonalRecord
import com.rishabh.gymbros.features.workout.model.ProgressionSuggestion
import com.rishabh.gymbros.features.workout.model.WorkoutSetDraft
import com.rishabh.gymbros.features.workout.model.TrainingPeriodSummary
import com.rishabh.gymbros.features.workout.model.WorkoutAnalyticsSnapshot
import com.rishabh.gymbros.features.workout.model.WorkoutCategory
import com.rishabh.gymbros.features.workout.model.WorkoutSession
import com.rishabh.gymbros.features.workout.model.WorkoutSetRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max

object WorkoutAnalytics {
    /** Session metric key written when a workout is saved (same value as WorkoutRepository.METRIC_TOTAL_CARDIO_SECONDS). */
    const val CARDIO_SECONDS_METRIC_KEY = "total_cardio_seconds"

    fun snapshot(
        sessions: List<WorkoutSession>,
        setLogs: Map<Long, List<WorkoutSetRecord>>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        sessionMetrics: Map<Long, Map<String, Double>> = emptyMap()
    ): WorkoutAnalyticsSnapshot {
        val currentStart = today.minusDays(6)
        val previousStart = today.minusDays(13)
        val previousEnd = today.minusDays(6)

        return WorkoutAnalyticsSnapshot(
            currentPeriod = periodSummary(sessions, setLogs, currentStart, today.plusDays(1), zoneId),
            previousPeriod = periodSummary(sessions, setLogs, previousStart, previousEnd, zoneId),
            dailyPoints = (6L downTo 0L).map { offset ->
                val day = today.minusDays(offset)
                val daySessions = WorkoutCalculations.sessionsForDay(sessions, day, zoneId)
                DailyTrainingPoint(
                    epochDay = day.toEpochDay(),
                    volumeKg = daySessions.sumOf { it.totalVolumeKg },
                    durationSeconds = daySessions.sumOf { it.durationSeconds },
                    sessionCount = daySessions.size
                )
            },
            personalRecords = personalRecords(sessions, setLogs),
            categoryVolumeKg = categoryVolume(sessions, currentStart, today.plusDays(1), zoneId),
            categoryDurationSeconds = categoryDuration(sessions, currentStart, today.plusDays(1), zoneId, sessionMetrics)
        )
    }

    fun personalRecords(
        sessions: List<WorkoutSession>,
        setLogs: Map<Long, List<WorkoutSetRecord>>
    ): List<ExercisePersonalRecord> {
        val endedAtBySession = sessions.associate { it.id to it.endedAt }
        val completed = setLogs.values.flatten().filter { it.completed }
        if (completed.isEmpty()) return emptyList()

        return completed
            .groupBy { it.exerciseName.trim().lowercase() }
            .map { (_, logs) ->
                val displayName = logs.maxByOrNull { endedAtBySession[it.sessionId] ?: 0L }?.exerciseName
                    ?: logs.first().exerciseName
                val bestLoad = logs.maxOfOrNull { it.loadKg } ?: 0.0
                val bestReps = logs.maxOfOrNull { it.reps } ?: 0
                val bestE1rm = logs.maxOfOrNull { estimatedOneRepMax(it.loadKg, it.reps) } ?: 0.0
                val bestSetVolume = logs.maxOfOrNull { it.loadKg.coerceAtLeast(0.0) * it.reps.coerceAtLeast(0) } ?: 0.0
                val totalVolume = logs.sumOf { it.loadKg.coerceAtLeast(0.0) * it.reps.coerceAtLeast(0) }
                val lastPerformedAt = logs.maxOfOrNull { endedAtBySession[it.sessionId] ?: 0L } ?: 0L
                ExercisePersonalRecord(
                    exerciseName = displayName,
                    bestLoadKg = bestLoad,
                    bestReps = bestReps,
                    bestEstimatedOneRepMaxKg = bestE1rm,
                    bestSetVolumeKg = bestSetVolume,
                    completedSets = logs.size,
                    totalVolumeKg = totalVolume,
                    lastPerformedAt = lastPerformedAt
                )
            }
            .sortedWith(
                compareByDescending<ExercisePersonalRecord> { it.bestEstimatedOneRepMaxKg }
                    .thenByDescending { it.bestLoadKg }
                    .thenBy { it.exerciseName }
            )
    }

    fun previousRecordForExercise(
        exerciseName: String,
        sessions: List<WorkoutSession>,
        setLogs: Map<Long, List<WorkoutSetRecord>>
    ): ExercisePersonalRecord? {
        return personalRecords(sessions, setLogs)
            .firstOrNull { it.exerciseName.equals(exerciseName.trim(), ignoreCase = true) }
    }

    fun progressionSuggestion(
        exercise: PlannedExercise,
        sessions: List<WorkoutSession>,
        setLogs: Map<Long, List<WorkoutSetRecord>>
    ): ProgressionSuggestion? {
        if (exercise.category != WorkoutCategory.STRENGTH) return null
        val endedAtBySession = sessions.associate { it.id to it.endedAt }
        val matchingLogs = setLogs.values.flatten().filter {
            it.completed && it.exerciseName.equals(exercise.name.trim(), ignoreCase = true) && it.loadKg > 0.0 && it.reps > 0
        }
        if (matchingLogs.isEmpty()) return null
        val latestSessionId = matchingLogs
            .map { it.sessionId }
            .distinct()
            .maxByOrNull { endedAtBySession[it] ?: 0L }
            ?: return null
        val latestBestSet = matchingLogs
            .filter { it.sessionId == latestSessionId }
            .maxByOrNull { estimatedOneRepMax(it.loadKg, it.reps) }
            ?: return null

        val targetReps = exercise.targetReps.coerceAtLeast(1)
        val suggested = when {
            latestBestSet.reps >= targetReps + 2 -> latestBestSet.loadKg + 2.5
            latestBestSet.reps >= targetReps -> latestBestSet.loadKg + 1.25
            else -> latestBestSet.loadKg
        }
        val message = when {
            suggested > latestBestSet.loadKg -> "Last session: ${formatLoad(latestBestSet.loadKg)} kg × ${latestBestSet.reps}. Try ${formatLoad(suggested)} kg if form stays clean."
            else -> "Last session: ${formatLoad(latestBestSet.loadKg)} kg × ${latestBestSet.reps}. Match it cleanly before adding load."
        }
        return ProgressionSuggestion(
            exerciseName = exercise.name,
            previousBestLoadKg = latestBestSet.loadKg,
            previousBestReps = latestBestSet.reps,
            suggestedLoadKg = suggested,
            message = message
        )
    }

    fun potentialPersonalRecord(
        draft: WorkoutSetDraft,
        previousRecord: ExercisePersonalRecord?
    ): PotentialPersonalRecord? {
        if (!draft.completed || draft.loadKg <= 0.0 || draft.reps <= 0) return null
        val previousLoad = previousRecord?.bestLoadKg ?: 0.0
        val previousE1rm = previousRecord?.bestEstimatedOneRepMaxKg ?: 0.0
        val currentE1rm = estimatedOneRepMax(draft.loadKg, draft.reps)
        return PotentialPersonalRecord(
            isNewLoadPr = draft.loadKg > previousLoad + 0.0001,
            isNewEstimatedOneRepMaxPr = currentE1rm > previousE1rm + 0.0001,
            previousBestLoadKg = previousLoad,
            previousBestEstimatedOneRepMaxKg = previousE1rm
        )
    }

    fun periodSummary(
        sessions: List<WorkoutSession>,
        setLogs: Map<Long, List<WorkoutSetRecord>>,
        startInclusive: LocalDate,
        endExclusive: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): TrainingPeriodSummary {
        val startMillis = startInclusive.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = endExclusive.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val periodSessions = sessions.filter { it.endedAt in startMillis until endMillis }
        val sessionIds = periodSessions.mapTo(hashSetOf()) { it.id }
        val completedSets = setLogs
            .filterKeys { it in sessionIds }
            .values
            .flatten()
            .count { it.completed }
        return TrainingPeriodSummary(
            sessionCount = periodSessions.size,
            durationSeconds = periodSessions.sumOf { it.durationSeconds },
            volumeKg = periodSessions.sumOf { it.totalVolumeKg },
            completedSets = completedSets
        )
    }

    fun percentageChange(current: Double, previous: Double): Int? {
        if (previous == 0.0) return if (current == 0.0) 0 else null
        return (((current - previous) / previous) * 100.0).toInt()
    }

    /**
     * Splits each session's time between categories. A session is stored under a single
     * category, so a mixed workout (for example strength plus a 5 km run) would otherwise be
     * counted entirely as Strength. The cardio time saved with the session is credited to
     * Cardio & Run and the remainder stays with the session's own category.
     */
    internal fun categoryDuration(
        sessions: List<WorkoutSession>,
        startInclusive: LocalDate,
        endExclusive: LocalDate,
        zoneId: ZoneId,
        sessionMetrics: Map<Long, Map<String, Double>> = emptyMap()
    ): Map<WorkoutCategory, Long> {
        val startMillis = startInclusive.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = endExclusive.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val totals = WorkoutCategory.entries.associateWith { 0L }.toMutableMap()
        sessions.asSequence()
            .filter { it.endedAt in startMillis until endMillis }
            .forEach { session ->
                val duration = session.durationSeconds.coerceAtLeast(0L)
                val cardioSeconds = if (session.category == WorkoutCategory.CARDIO) {
                    duration
                } else {
                    (sessionMetrics[session.id]?.get(CARDIO_SECONDS_METRIC_KEY) ?: 0.0)
                        .toLong()
                        .coerceIn(0L, duration)
                }
                totals[WorkoutCategory.CARDIO] = (totals[WorkoutCategory.CARDIO] ?: 0L) + cardioSeconds
                val remaining = duration - cardioSeconds
                if (remaining > 0L && session.category != WorkoutCategory.CARDIO) {
                    totals[session.category] = (totals[session.category] ?: 0L) + remaining
                }
            }
        return totals
    }

    private fun categoryVolume(
        sessions: List<WorkoutSession>,
        startInclusive: LocalDate,
        endExclusive: LocalDate,
        zoneId: ZoneId
    ): Map<WorkoutCategory, Double> {
        val startMillis = startInclusive.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = endExclusive.atStartOfDay(zoneId).toInstant().toEpochMilli()
        return WorkoutCategory.entries.associateWith { category ->
            sessions.asSequence()
                .filter { it.category == category && it.endedAt in startMillis until endMillis }
                .sumOf { it.totalVolumeKg }
        }
    }

    fun estimatedOneRepMax(loadKg: Double, reps: Int): Double {
        if (loadKg <= 0.0 || reps <= 0) return max(0.0, loadKg)
        // Epley estimate. Cap the rep contribution so high-rep conditioning entries do not explode the metric.
        val safeReps = reps.coerceAtMost(30)
        return loadKg * (1.0 + safeReps / 30.0)
    }

    private fun formatLoad(value: Double): String {
        val rounded = kotlin.math.round(value)
        return if (kotlin.math.abs(value - rounded) < 0.001) rounded.toInt().toString()
        else String.format(java.util.Locale.ENGLISH, "%.1f", value)
    }
}