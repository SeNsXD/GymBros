package com.rishabh.gymbros.features.water.domain

import com.rishabh.gymbros.features.water.model.WaterEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WaterCalculationsTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun dailyTotal_onlyCountsRequestedDay() {
        val day = LocalDate.of(2026, 9, 23)
        val entries = listOf(
            entry(1, day, 250),
            entry(2, day, 500),
            entry(3, day.minusDays(1), 750)
        )

        assertEquals(750, WaterCalculations.dailyTotal(entries, day, zone))
    }

    @Test
    fun displayPercent_capsAtOneHundred() {
        assertEquals(100, WaterCalculations.displayPercent(3_500, 3_000))
    }

    @Test
    fun streak_canEndYesterdayWhileTodayIsInProgress() {
        val today = LocalDate.of(2026, 9, 23)
        val entries = buildList {
            add(entry(1, today, 250))
            add(entry(2, today.minusDays(1), 3_000))
            add(entry(3, today.minusDays(2), 3_250))
            add(entry(4, today.minusDays(3), 2_000))
        }

        assertEquals(
            2,
            WaterCalculations.streakDays(entries, 3_000, today, zone)
        )
    }

    @Test
    fun streak_includesTodayWhenGoalIsHit() {
        val today = LocalDate.of(2026, 9, 23)
        val entries = listOf(
            entry(1, today, 3_000),
            entry(2, today.minusDays(1), 3_000),
            entry(3, today.minusDays(2), 3_000)
        )

        assertEquals(
            3,
            WaterCalculations.streakDays(entries, 3_000, today, zone)
        )
    }

    private fun entry(
        id: Long,
        date: LocalDate,
        amount: Int
    ): WaterEntry {
        return WaterEntry(
            id = id,
            timestamp = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
            amountMl = amount
        )
    }
}
