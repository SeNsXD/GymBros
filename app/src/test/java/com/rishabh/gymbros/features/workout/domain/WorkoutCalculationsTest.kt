package com.rishabh.gymbros.features.workout.domain

import com.rishabh.gymbros.features.workout.model.WorkoutCategory
import com.rishabh.gymbros.features.workout.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WorkoutCalculationsTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun streakCountsConsecutiveWorkoutDaysEndingToday() {
        val today = LocalDate.of(2026, 9, 23)
        val sessions = listOf(
            sessionOn(today),
            sessionOn(today.minusDays(1)),
            sessionOn(today.minusDays(2))
        )

        assertEquals(3, WorkoutCalculations.streakDays(sessions, today, zone))
    }

    @Test
    fun streakCanEndYesterday() {
        val today = LocalDate.of(2026, 9, 23)
        val sessions = listOf(
            sessionOn(today.minusDays(1)),
            sessionOn(today.minusDays(2))
        )

        assertEquals(2, WorkoutCalculations.streakDays(sessions, today, zone))
    }

    @Test
    fun streakIsZeroWhenMostRecentWorkoutIsOlderThanYesterday() {
        val today = LocalDate.of(2026, 9, 23)
        val sessions = listOf(sessionOn(today.minusDays(2)))

        assertEquals(0, WorkoutCalculations.streakDays(sessions, today, zone))
    }

    private fun sessionOn(day: LocalDate): WorkoutSession {
        val endedAt = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        return WorkoutSession(
            id = endedAt,
            startedAt = endedAt - 3_600_000L,
            endedAt = endedAt,
            durationSeconds = 3600,
            totalVolumeKg = 1000.0,
            completedExercises = 3,
            totalExercises = 3,
            category = WorkoutCategory.STRENGTH,
            title = "Strength Session"
        )
    }
}
