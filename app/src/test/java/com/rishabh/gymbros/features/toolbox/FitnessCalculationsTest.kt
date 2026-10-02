package com.rishabh.gymbros.features.toolbox

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class FitnessCalculationsTest {
    private fun input(tool: FitnessTool) = FitnessInput(tool, "70", "175", "25", sex = EquationSex.MALE, activity = ActivityLevel.MODERATE)
    private fun result(input: FitnessInput) = calculateFitness(input)

    @Test fun dotAndCommaAreEquivalent() {
        assertEquals(70.5, parseFitnessDecimal("70,5")!!, 0.00001)
        assertEquals(parseFitnessDecimal("70.5"), parseFitnessDecimal(" 70,5 "))
        for (bad in listOf("-70", "5'10", "1,234.5", "1.2.3", "NaN", "Infinity", "1e3", "70kg", "", ".", "999999999999999")) assertNull(bad, parseFitnessDecimal(bad))
    }

    @Test fun germanProfilePrefillRemainsParseable() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("70.5", fitnessWeightPrefill(70.5f))
            assertEquals(70.5, parseFitnessDecimal(fitnessWeightPrefill(70.5f))!!, 0.00001)
            assertFalse(result(input(FitnessTool.PROTEIN).copy(weight = "70,5")).error)
        } finally { Locale.setDefault(previous) }
    }

    @Test fun everyToolRejectsInvalidMeasurementsAndAges() {
        FitnessTool.entries.forEach { tool ->
            val base = input(tool).copy(goalWeight = "65")
            for (age in listOf("", "0", "19", "121", "999", "25.5")) assertTrue(result(base.copy(age = age)).error)
            for (w in listOf("0", "24.9", "351", "NaN", "Infinity", "abc")) assertTrue(result(base.copy(weight = w)).error)
            for (h in listOf("0", "99", "251", "510", "5'10")) assertTrue(result(base.copy(height = h)).error)
            assertTrue(result(base.copy(needsIndividualAdvice = true)).error)
        }
    }

    @Test fun bmiCategoriesUseUnroundedValues() {
        for ((weight, category) in listOf("73.96" to "Underweight", "74" to "Healthy range", "99.96" to "Healthy range", "100" to "Overweight", "119.96" to "Overweight", "120" to "Obesity range")) {
            val calculated = result(input(FitnessTool.BMI).copy(weight = weight, height = "200"))
            assertFalse(calculated.error)
            assertTrue(calculated.text.contains(category))
            assertTrue(calculated.text.contains("unrounded"))
        }
    }

    @Test fun calorieReferenceCasesAndGoalAdjustments() {
        val base = input(FitnessTool.CALORIES)
        assertTrue(result(base).text.contains("1674 kcal/day"))
        assertTrue(result(base).text.contains("2594 kcal/day"))
        assertTrue(result(base.copy(goal = FitnessGoal.LOSE)).text.contains("2194 kcal/day"))
        assertTrue(result(base.copy(goal = FitnessGoal.GAIN)).text.contains("2844 kcal/day"))
        assertTrue(result(base.copy(sex = EquationSex.FEMALE)).text.contains("2337 kcal/day"))
    }

    @Test fun calorieRequiresExplicitChoicesAndWithholdsLowTargets() {
        val base = input(FitnessTool.CALORIES)
        assertTrue(result(base.copy(sex = null)).error)
        assertTrue(result(base.copy(activity = null)).error)
        val low = base.copy(weight = "45", height = "150", age = "70", sex = EquationSex.FEMALE, activity = ActivityLevel.SEDENTARY, goal = FitnessGoal.LOSE)
        assertTrue(result(low).error)
        assertFalse(result(low).text.contains("652"))
        assertTrue(result(base.copy(weight = "50", height = "190", goal = FitnessGoal.LOSE)).error)
    }

    @Test fun proteinRangesAndUnsupportedBodySizes() {
        val base = input(FitnessTool.PROTEIN)
        assertTrue(result(base).text.startsWith("98–140 g/day"))
        assertTrue(result(base.copy(goal = FitnessGoal.LOSE)).text.startsWith("112–154 g/day"))
        assertTrue(result(base.copy(goal = FitnessGoal.GAIN)).text.startsWith("112–140 g/day"))
        assertTrue(result(base.copy(weight = "150")).error)
        assertTrue(result(base.copy(weight = "40")).error)
    }

    @Test fun lossTimelineRejectsInappropriateGoalsAndRates() {
        val base = input(FitnessTool.WEIGHT_LOSS).copy(weight = "80", goalWeight = "70")
        assertTrue(result(base).text.startsWith("About 20 weeks"))
        for (goal in listOf("0", "20", "40", "80", "90")) assertTrue(result(base.copy(goalWeight = goal)).error)
        for (rate in listOf("0", "0.01", "-0.5", "100", "0.95", "0.8")) assertTrue(result(base.copy(weeklyLoss = rate)).error)
        assertFalse(result(base.copy(weeklyLoss = "0,5")).error)
    }

    @Test fun veryShortTimelinesDoNotSayZeroWeeks() {
        val calculated = result(input(FitnessTool.WEIGHT_LOSS).copy(weight = "70.01", goalWeight = "70"))
        assertTrue(calculated.text.startsWith("Less than 1 week"))
    }
}
