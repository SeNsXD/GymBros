package com.rishabh.gymbros.features.water.data

import android.content.Context
import com.rishabh.gymbros.features.settings.data.SettingsRepository
import com.rishabh.gymbros.features.water.domain.WaterCalculations
import com.rishabh.gymbros.features.water.model.DailyWaterIntake
import com.rishabh.gymbros.features.water.model.WaterUiState
import com.rishabh.gymbros.widgets.GymBrosWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId

class WaterRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database = WaterDatabase(appContext)
    private val settingsRepository = SettingsRepository.getInstance(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val zoneId: ZoneId
        get() = ZoneId.systemDefault()

    private val _state = MutableStateFlow(WaterUiState())
    val state: StateFlow<WaterUiState> = _state.asStateFlow()
    suspend fun reloadFromDisk() { mutex.withLock { refreshLocked() } }

    init {
        scope.launch {
            while (isActive) {
                mutex.withLock { refreshLocked() }
                delay(60_000L)
            }
        }
        scope.launch {
            settingsRepository.settings.collect {
                mutex.withLock { refreshLocked() }
            }
        }
    }

    fun hardResetWaterData() {
        scope.launch {
            mutex.withLock {
                database.clearAllWaterData()
                refreshLocked()
            }
            GymBrosWidgetUpdater.updateAll(appContext)
        }
    }

    fun addWater(amountMl: Int) {
        if (amountMl <= 0) return
        scope.launch {
            addWaterAndWait(amountMl)
        }
    }

    internal suspend fun addWaterAndWait(amountMl: Int) {
        if (amountMl <= 0) return
        var changed = false
        mutex.withLock {
            val (start, end) = todayBounds()
            val currentIntake = database.entriesBetween(start, end).sumOf { it.amountMl }
            val remaining = (MAX_DAILY_WATER_ML - currentIntake).coerceAtLeast(0)
            val amountToAdd = amountMl.coerceAtMost(remaining)

            if (amountToAdd > 0) {
                database.insertEntry(
                    timestamp = System.currentTimeMillis(),
                    amountMl = amountToAdd
                )
                refreshLocked()
                changed = true
            }
        }
        if (changed) GymBrosWidgetUpdater.updateAll(appContext)
    }

    fun subtractWater(amountMl: Int) {
        if (amountMl <= 0) return
        scope.launch {
            subtractWaterAndWait(amountMl)
        }
    }

    internal suspend fun subtractWaterAndWait(amountMl: Int) {
        if (amountMl <= 0) return
        var changed = false
        mutex.withLock {
            val (start, end) = todayBounds()
            val entries = database.entriesBetween(start, end)
            var remaining = amountMl.coerceAtMost(entries.sumOf { it.amountMl })

            for (entry in entries) {
                if (remaining <= 0) break
                if (entry.amountMl <= remaining) {
                    changed = database.deleteEntry(entry.id) || changed
                    remaining -= entry.amountMl
                } else {
                    changed = database.updateEntryAmount(
                        id = entry.id,
                        amountMl = entry.amountMl - remaining
                    ) || changed
                    remaining = 0
                }
            }

            if (changed) refreshLocked()
        }
        if (changed) GymBrosWidgetUpdater.updateAll(appContext)
    }

    fun undoLast() {
        scope.launch {
            var changed = false
            mutex.withLock {
                val (start, end) = todayBounds()
                val lastEntry = database.latestEntryBetween(start, end) ?: return@withLock
                changed = database.deleteEntry(lastEntry.id)
                if (changed) refreshLocked()
            }
            if (changed) GymBrosWidgetUpdater.updateAll(appContext)
        }
    }

    fun refresh() {
        scope.launch {
            mutex.withLock { refreshLocked() }
        }
    }

    private fun refreshLocked() {
        val today = LocalDate.now(zoneId)
        val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val todayEntries = database.entriesBetween(start, end)
        val allEntries = database.allEntries()
        val intake = todayEntries.sumOf { it.amountMl.toLong() }
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        val lifetimeIntake = allEntries.sumOf { it.amountMl.toLong() }
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        val goal = settingsRepository.currentSettings.dailyGoalMl
        val progress = WaterCalculations.progress(intake, goal)
        val entriesByDay = allEntries.groupBy { entry ->
            java.time.Instant.ofEpochMilli(entry.timestamp).atZone(zoneId).toLocalDate().toEpochDay()
        }
        val weeklyIntake = (6L downTo 0L).map { daysAgo ->
            val day = today.minusDays(daysAgo)
            val amount = entriesByDay[day.toEpochDay()].orEmpty()
                .sumOf { it.amountMl.toLong() }
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            DailyWaterIntake(epochDay = day.toEpochDay(), intakeMl = amount)
        }

        // Keep 90 days available for the Water History view. This is rebuilt from the
        // same persisted WaterEntry records, so it survives app restarts.
        val historyIntake = (89L downTo 0L).map { daysAgo ->
            val day = today.minusDays(daysAgo)
            val amount = entriesByDay[day.toEpochDay()].orEmpty()
                .sumOf { it.amountMl.toLong() }
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            DailyWaterIntake(epochDay = day.toEpochDay(), intakeMl = amount)
        }

        _state.value = WaterUiState(
            intakeMl = intake,
            lifetimeIntakeMl = lifetimeIntake,
            goalMl = goal,
            progress = progress,
            progressPercent = WaterCalculations.displayPercent(intake, goal),
            goalReached = intake >= goal,
            streakDays = WaterCalculations.streakDays(
                entries = allEntries,
                goalMl = goal,
                today = today,
                zoneId = zoneId
            ),
            todayEntries = todayEntries,
            weeklyIntake = weeklyIntake,
            historyIntake = historyIntake,
            isLoading = false
        )
    }

    private fun todayBounds(): Pair<Long, Long> {
        val today = LocalDate.now(zoneId)
        return today.atStartOfDay(zoneId).toInstant().toEpochMilli() to
            today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    companion object {
        private const val MAX_DAILY_WATER_ML = 5_000

        @Volatile
        private var instance: WaterRepository? = null

        fun getInstance(context: Context): WaterRepository {
            return instance ?: synchronized(this) {
                instance ?: WaterRepository(context.applicationContext).also { instance = it }
            }
        }
    }

}
