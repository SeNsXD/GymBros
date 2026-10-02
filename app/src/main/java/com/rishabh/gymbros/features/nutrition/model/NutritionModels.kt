package com.rishabh.gymbros.features.nutrition.model

enum class MealType(val label: String) { BREAKFAST("Breakfast"), LUNCH("Lunch"), SNACKS("Snacks"), DINNER("Dinner") }

data class FoodReference(
    val code: String,
    val name: String,
    val category: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double
) {
    fun nutrientsFor(grams: Double) = NutrientTotals(
        calories = caloriesPer100g * grams / 100.0,
        protein = proteinPer100g * grams / 100.0,
        carbs = carbsPer100g * grams / 100.0,
        fat = fatPer100g * grams / 100.0,
        fiber = fiberPer100g * grams / 100.0
    )
}

data class FoodLogEntry(
    val id: Long,
    val foodCode: String,
    val foodName: String,
    val grams: Double,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val meal: MealType,
    val timestamp: Long
)

data class NutrientTotals(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0
) {
    operator fun plus(other: NutrientTotals) = NutrientTotals(
        calories + other.calories, protein + other.protein, carbs + other.carbs,
        fat + other.fat, fiber + other.fiber
    )
}

data class NutritionGoals(
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0
) {
    val configured: Boolean get() = calories > 0 && protein > 0 && carbs > 0 && fat > 0
}

data class NutritionUiState(
    val todayEntries: List<FoodLogEntry> = emptyList(),
    val totals: NutrientTotals = NutrientTotals(),
    val goals: NutritionGoals = NutritionGoals(),
    val isLoading: Boolean = true
)
