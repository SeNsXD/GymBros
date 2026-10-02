package com.rishabh.gymbros.features.workout.domain

import com.rishabh.gymbros.features.workout.model.WorkoutSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object WorkoutCalculations {
    fun streakDays(
        sessions: List<WorkoutSession>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        restDays: Set<Long> = emptySet()
    ): Int {

        val completedDays = sessions
            .asSequence()
            .filter { it.endedAt > 0L && it.completedExercises > 0 }
            .map { Instant.ofEpochMilli(it.endedAt).atZone(zoneId).toLocalDate() }
            .toSet()

        fun protected(day: LocalDate) = day in completedDays || day.toEpochDay() in restDays
        var cursor = when {
            protected(today) -> today
            protected(today.minusDays(1)) -> today.minusDays(1)
            else -> return 0
        }

        var streak = 0
        while (protected(cursor)) {
            // Rest days preserve continuity without inflating the workout-day streak count.
            if (cursor in completedDays) streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun completedSessionsForDay(
        sessions: List<WorkoutSession>,
        day: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<WorkoutSession> = sessionsForDay(sessions, day, zoneId)
        .filter { it.endedAt > 0L && it.completedExercises > 0 }

    fun bestStreakDays(
        sessions: List<WorkoutSession>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        restDays: Set<Long> = emptySet()
    ): Int {
        val days = sessions
            .asSequence()
            .filter { it.endedAt > 0L && it.completedExercises > 0 }
            .map { Instant.ofEpochMilli(it.endedAt).atZone(zoneId).toLocalDate() }
            .distinct()
            .sorted()
            .toList()
        if (days.isEmpty()) return 0

        val workoutDays = days.toSet()
        val protectedDays = workoutDays + restDays.map(LocalDate::ofEpochDay)
        val first = protectedDays.minOrNull() ?: return 0
        val last = protectedDays.maxOrNull() ?: return 0
        var cursor = first
        var currentWorkouts = 0
        var best = 0
        while (!cursor.isAfter(last)) {
            when {
                cursor in workoutDays -> {
                    currentWorkouts += 1
                    if (currentWorkouts > best) best = currentWorkouts
                }
                cursor.toEpochDay() in restDays -> Unit
                else -> currentWorkouts = 0
            }
            cursor = cursor.plusDays(1)
        }
        return best
    }

    fun sessionsForDay(
        sessions: List<WorkoutSession>,
        day: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<WorkoutSession> {
        val start = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return sessions.filter { it.endedAt in start until end }
    }
}
