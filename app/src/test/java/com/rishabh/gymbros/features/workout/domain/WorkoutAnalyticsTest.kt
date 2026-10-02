package com.rishabh.gymbros.features.workout.domain

import com.rishabh.gymbros.features.workout.model.PlannedExercise
import com.rishabh.gymbros.features.workout.model.WorkoutCategory
import com.rishabh.gymbros.features.workout.model.WorkoutSetDraft
import com.rishabh.gymbros.features.workout.model.WorkoutSession
import com.rishabh.gymbros.features.workout.model.WorkoutSetRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WorkoutAnalyticsTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun personalRecordsUseCompletedSetsOnly() {
        val day = LocalDate.of(2026, 9, 23)
        val session = sessionOn(1L, day, 1200.0)
        val logs = mapOf(
            1L to listOf(
                set(1, 1L, "Bench Press", 1, 8, 60.0, true),
                set(2, 1L, "Bench Press", 2, 4, 100.0, false),
                set(3, 1L, "Bench Press", 3, 5, 70.0, true)
            )
        )

        val records = WorkoutAnalytics.personalRecords(listOf(session), logs)
        assertEquals(1, records.size)
        assertEquals(70.0, records.first().bestLoadKg, 0.001)
        assertEquals(2, records.first().completedSets)
        assertTrue(records.first().bestEstimatedOneRepMaxKg > 80.0)
    }

    @Test
    fun progressionSuggestionUsesPreviousCompletedPerformance() {
        val day = LocalDate.of(2026, 9, 23)
        val session = sessionOn(1L, day.minusDays(2), 1200.0)
        val logs = mapOf(
            1L to listOf(
                set(1, 1L, "Bench Press", 1, 10, 60.0, true),
                set(2, 1L, "Bench Press", 2, 12, 60.0, true)
            )
        )
        val suggestion = WorkoutAnalytics.progressionSuggestion(
            exercise = PlannedExercise(
                id = "bench",
                name = "Bench Press",
                sets = 3,
                repsLabel = "10 reps",
                targetReps = 10,
                loadKg = 60.0,
                category = WorkoutCategory.STRENGTH
            ),
            sessions = listOf(session),
            setLogs = logs
        )
        assertTrue(suggestion != null)
        assertTrue(suggestion!!.suggestedLoadKg > 60.0)
    }

    @Test
    fun potentialPrOnlyCountsCompletedSet() {
        val previous = com.rishabh.gymbros.features.workout.model.ExercisePersonalRecord(
            exerciseName = "Bench Press",
            bestLoadKg = 60.0,
            bestReps = 10,
            bestEstimatedOneRepMaxKg = WorkoutAnalytics.estimatedOneRepMax(60.0, 10),
            bestSetVolumeKg = 600.0,
            completedSets = 4,
            totalVolumeKg = 2400.0,
            lastPerformedAt = 0L
        )
        val pending = WorkoutSetDraft("bench", 1, 8, 65.0, false)
        val completed = pending.copy(completed = true)
        assertEquals(null, WorkoutAnalytics.potentialPersonalRecord(pending, previous))
        assertTrue(WorkoutAnalytics.potentialPersonalRecord(completed, previous)!!.isAnyPr)
    }

    @Test
    fun snapshotSeparatesCurrentAndPreviousSevenDays() {
        val today = LocalDate.of(2026, 9, 23)
        val sessions = listOf(
            sessionOn(1L, today, 1000.0),
            sessionOn(2L, today.minusDays(3), 2000.0),
            sessionOn(3L, today.minusDays(9), 500.0)
        )
        val logs = sessions.associate { it.id to emptyList<WorkoutSetRecord>() }

        val snapshot = WorkoutAnalytics.snapshot(sessions, logs, today, zone)
        assertEquals(2, snapshot.currentPeriod.sessionCount)
        assertEquals(3000.0, snapshot.currentPeriod.volumeKg, 0.001)
        assertEquals(1, snapshot.previousPeriod.sessionCount)
        assertEquals(500.0, snapshot.previousPeriod.volumeKg, 0.001)
        assertEquals(7, snapshot.dailyPoints.size)
    }

    @Test
    fun categoryDurationCreditsCardioTimeOfMixedSessions() {
        val day = LocalDate.of(2026, 9, 30)
        val mixed = sessionOn(1L, day, 1000.0) // 3600 s, stored as STRENGTH
        val metrics = mapOf(1L to mapOf(WorkoutAnalytics.CARDIO_SECONDS_METRIC_KEY to 900.0))

        val totals = WorkoutAnalytics.categoryDuration(
            sessions = listOf(mixed),
            startInclusive = day.minusDays(6),
            endExclusive = day.plusDays(1),
            zoneId = zone,
            sessionMetrics = metrics
        )

        assertEquals(900L, totals[WorkoutCategory.CARDIO])
        assertEquals(2700L, totals[WorkoutCategory.STRENGTH])
        assertEquals(3600L, totals.values.sum())
    }

    @Test
    fun categoryDurationWithoutCardioMetricStaysWithSessionCategory() {
        val day = LocalDate.of(2026, 9, 30)
        val totals = WorkoutAnalytics.categoryDuration(
            sessions = listOf(sessionOn(1L, day, 1000.0)),
            startInclusive = day.minusDays(6),
            endExclusive = day.plusDays(1),
            zoneId = zone
        )

        assertEquals(3600L, totals[WorkoutCategory.STRENGTH])
        assertEquals(0L, totals[WorkoutCategory.CARDIO])
    }

    private fun sessionOn(id: Long, day: LocalDate, volume: Double): WorkoutSession {
        val endedAt = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        return WorkoutSession(
            id = id,
            startedAt = endedAt - 3_600_000L,
            endedAt = endedAt,
            durationSeconds = 3600,
            totalVolumeKg = volume,
            completedExercises = 1,
            totalExercises = 1,
            category = WorkoutCategory.STRENGTH,
            title = "Strength Session"
        )
    }

    private fun set(
        id: Long,
        sessionId: Long,
        exercise: String,
        number: Int,
        reps: Int,
        load: Double,
        completed: Boolean
    ) = WorkoutSetRecord(
        id = id,
        sessionId = sessionId,
        exerciseId = exercise.lowercase().replace(' ', '-'),
        exerciseName = exercise,
        setNumber = number,
        reps = reps,
        loadKg = load,
        completed = completed
    )
}