package com.rishabh.gymbros.features.water.domain

import com.rishabh.gymbros.features.water.model.WaterEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object WaterCalculations {

    fun dailyTotal(
        entries: List<WaterEntry>,
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int {
        val total = entries.asSequence()
            .filter { entry ->
                Instant.ofEpochMilli(entry.timestamp)
                    .atZone(zoneId)
                    .toLocalDate() == date
            }
            .sumOf { it.amountMl.toLong() }

        return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun progress(intakeMl: Int, goalMl: Int): Float {
        if (goalMl <= 0) return 0f
        return (intakeMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)
    }

    fun displayPercent(intakeMl: Int, goalMl: Int): Int {
        return (progress(intakeMl, goalMl) * 100f).toInt().coerceIn(0, 100)
    }

    /**
     * A streak is the run of consecutive goal-hit days ending today if today is complete,
     * otherwise ending yesterday. This keeps yesterday's streak visible while today is in progress.
     */
    fun streakDays(
        entries: List<WaterEntry>,
        goalMl: Int,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int {
        if (goalMl <= 0 || entries.isEmpty()) return 0

        val totalsByDate = entries.groupingBy { entry ->
            Instant.ofEpochMilli(entry.timestamp)
                .atZone(zoneId)
                .toLocalDate()
        }.fold(0L) { acc, entry -> acc + entry.amountMl.toLong() }

        val todayHit = (totalsByDate[today] ?: 0L) >= goalMl
        var cursor = if (todayHit) today else today.minusDays(1)

        if ((totalsByDate[cursor] ?: 0L) < goalMl) return 0

        var streak = 0
        while ((totalsByDate[cursor] ?: 0L) >= goalMl) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
