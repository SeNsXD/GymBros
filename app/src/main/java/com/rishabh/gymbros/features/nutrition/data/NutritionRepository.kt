package com.rishabh.gymbros.features.nutrition.data

import android.content.Context
import com.rishabh.gymbros.features.nutrition.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.ZoneId

class NutritionRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val foods = IfctFoodDatabase(appContext)
    private val log = NutritionDatabase(appContext)
    private val prefs = appContext.getSharedPreferences("gymbros_nutrition", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val _state = MutableStateFlow(NutritionUiState())
    val state: StateFlow<NutritionUiState> = _state.asStateFlow()

    init { refresh() }

    suspend fun searchFoods(query: String): List<FoodReference> = withContext(Dispatchers.IO) { foods.search(query) }

    fun addFood(food: FoodReference, grams: Double, meal: MealType) {
        if (grams <= 0) return
        scope.launch {
            val n = food.nutrientsFor(grams)
            log.insert(FoodLogEntry(0, food.code, food.name, grams, n.calories, n.protein, n.carbs, n.fat, n.fiber, meal, System.currentTimeMillis()))
            refreshNow()
        }
    }

    fun updateEntry(entry: FoodLogEntry, grams: Double, meal: MealType) {
        if (grams <= 0) return
        scope.launch {
            val food = foods.findByCode(entry.foodCode) ?: return@launch
            val n = food.nutrientsFor(grams)
            log.update(entry.copy(grams = grams, calories = n.calories, protein = n.protein, carbs = n.carbs, fat = n.fat, fiber = n.fiber, meal = meal))
            refreshNow()
        }
    }

    fun deleteEntry(id: Long) { scope.launch { log.delete(id); refreshNow() } }

    fun setGoals(goals: NutritionGoals) {
        prefs.edit().putInt("goal_calories", goals.calories).putInt("goal_protein", goals.protein)
            .putInt("goal_carbs", goals.carbs).putInt("goal_fat", goals.fat).apply()
        refresh()
    }

    fun refresh() { scope.launch { refreshNow() } }

    private fun refreshNow() {
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val entries = log.entriesBetween(start, end)
        val totals = entries.fold(NutrientTotals()) { a, e -> a + NutrientTotals(e.calories,e.protein,e.carbs,e.fat,e.fiber) }
        _state.value = NutritionUiState(entries, totals, readGoals(), false)
    }

    private fun readGoals() = NutritionGoals(
        prefs.getInt("goal_calories", 0), prefs.getInt("goal_protein", 0), prefs.getInt("goal_carbs", 0), prefs.getInt("goal_fat", 0)
    )

    companion object {
        @Volatile private var instance: NutritionRepository? = null
        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: NutritionRepository(context.applicationContext).also { instance = it }
        }
    }
}
