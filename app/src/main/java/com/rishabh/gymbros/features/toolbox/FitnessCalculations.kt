package com.rishabh.gymbros.features.toolbox

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

enum class FitnessTool { CALORIES, BMI, PROTEIN, WEIGHT_LOSS }
enum class FitnessGoal { LOSE, MAINTAIN, GAIN }
enum class EquationSex { MALE, FEMALE }
enum class ActivityLevel(val multiplier: Double, val label: String) {
    SEDENTARY(1.2, "Mostly sitting · little exercise"),
    LIGHT(1.375, "Light activity · exercise 1–3 days/week"),
    MODERATE(1.55, "Moderate activity · exercise 3–5 days/week"),
    HIGH(1.725, "High activity · hard exercise most days"),
    VERY_HIGH(1.9, "Very high · physical work + hard training")
}

data class FitnessInput(
    val tool: FitnessTool,
    val weight: String,
    val height: String,
    val age: String,
    val goal: FitnessGoal = FitnessGoal.MAINTAIN,
    val sex: EquationSex? = null,
    val activity: ActivityLevel? = null,
    val goalWeight: String = "",
    val weeklyLoss: String = "0.5",
    val needsIndividualAdvice: Boolean = false
)

data class FitnessResult(val text: String, val error: Boolean = false)

/** A single dot OR comma is a decimal separator; never strip signs, units or separators. */
fun parseFitnessDecimal(raw: String): Double? {
    val text = raw.trim()
    if (text.length > 12 || !text.matches(Regex("[0-9]+([.,][0-9]+)?"))) return null
    return text.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
}

fun fitnessWeightPrefill(weight: Float?): String =
    weight?.takeIf { it.isFinite() }?.let { String.format(Locale.US, "%.1f", it) }.orEmpty()

private fun number(value: Double, places: Int = 1) = String.format(Locale.getDefault(), "%.${places}f", value)
private fun invalid(message: String) = FitnessResult(message, error = true)

fun calculateFitness(input: FitnessInput): FitnessResult {
    val age = input.age.trim().takeIf { it.matches(Regex("[0-9]{1,3}")) }?.toIntOrNull()
        ?: return invalid("Enter your age in whole years.")
    // All four tools intentionally share the adult BMI scope; no pediatric interpretation.
    if (age !in 20..120) return invalid("These calculators support adults aged 20–120. Younger users need age-specific guidance.")
    if (input.needsIndividualAdvice) return invalid("These tools do not cover pregnancy, breastfeeding or medically prescribed nutrition. Use an individual assessment instead.")
    val weight = parseFitnessDecimal(input.weight)
        ?: return invalid("Enter weight in kg using digits and one decimal separator, e.g. 70.5 or 70,5.")
    val height = parseFitnessDecimal(input.height)
        ?: return invalid("Enter height in centimetres, e.g. 175. Do not enter feet or inches in this field.")
    // Supported product input ranges, not claims about healthy body measurements.
    if (weight !in 25.0..350.0) return invalid("Supported weight range: 25–350 kg. Check the units or seek an individual assessment.")
    if (height !in 100.0..250.0) return invalid("Supported height range: 100–250 cm. Check the units or seek an individual assessment.")
    val bmi = weight / ((height / 100) * (height / 100))
    return when (input.tool) {
        FitnessTool.BMI -> {
            val category = when {
                bmi < 18.5 -> "Underweight (below 18.5)"
                bmi < 25 -> "Healthy range (18.5 to below 25)"
                bmi < 30 -> "Overweight (25 to below 30)"
                else -> "Obesity range (30 or above)"
            }
            FitnessResult("BMI ≈ ${number(bmi, 2)}\n$category\nCategory uses the unrounded value. BMI is a screening measure, not body-fat percentage; muscular builds can be misclassified.")
        }
        FitnessTool.PROTEIN -> {
            if (bmi < 18.5 || bmi >= 30) return invalid("A simple protein target based on current weight may be unsuitable at this BMI. Get an individual target rather than using this range.")
            val range = when (input.goal) {
                FitnessGoal.LOSE -> 1.6 to 2.2
                FitnessGoal.GAIN -> 1.6 to 2.0
                FitnessGoal.MAINTAIN -> 1.4 to 2.0
            }
            FitnessResult("${(weight * range.first).roundToInt()}–${(weight * range.second).roundToInt()} g/day\nFor healthy adults who exercise regularly. Uses ${number(range.first)}–${number(range.second)} g/kg/day; this is a range, not an exact requirement.")
        }
        FitnessTool.CALORIES -> {
            val sex = input.sex ?: return invalid("Select the sex coefficient used by the resting-energy equation.")
            val activity = input.activity ?: return invalid("Choose the activity description that best matches your usual week.")
            val resting = 10 * weight + 6.25 * height - 5 * age + if (sex == EquationSex.MALE) 5 else -161
            if (resting <= 0) return invalid("These measurements do not produce a usable resting-energy estimate.")
            val maintenance = resting * activity.multiplier
            val target = when (input.goal) {
                FitnessGoal.LOSE -> maintenance - 400
                FitnessGoal.GAIN -> maintenance + 250
                FitnessGoal.MAINTAIN -> maintenance
            }
            // Conservative product guardrails, not universal minimum intake prescriptions.
            val floor = if (sex == EquationSex.MALE) 1500 else 1200
            if (target < floor || (input.goal == FitnessGoal.LOSE && bmi < 18.5)) {
                return invalid("No calorie target shown: this would suggest a low intake or weight loss from an underweight starting point. Get an individual assessment; do not treat a calculator minimum as a safe diet.")
            }
            val adjustment = when (input.goal) {
                FitnessGoal.LOSE -> "Loss starting estimate (maintenance − 400)"
                FitnessGoal.GAIN -> "Gain starting estimate (maintenance + 250)"
                FitnessGoal.MAINTAIN -> "Maintenance starting estimate"
            }
            FitnessResult("Resting energy ≈ ${resting.roundToInt()} kcal/day\nMaintenance ≈ ${maintenance.roundToInt()} kcal/day\n$adjustment: ${target.roundToInt()} kcal/day\nMifflin–St Jeor estimate; activity factor ${number(activity.multiplier, 3)}. Actual needs vary. Review your intake and weight trend over several weeks.${if (age > 78) " This age is beyond the original study's range; obtain an individual assessment for greater confidence." else ""}")
        }
        FitnessTool.WEIGHT_LOSS -> {
            val goal = parseFitnessDecimal(input.goalWeight)
                ?: return invalid("Enter a positive goal weight in kg.")
            val rate = parseFitnessDecimal(input.weeklyLoss)
                ?: return invalid("Enter a weekly loss rate in kg, e.g. 0.5.")
            if (goal !in 25.0..350.0 || goal >= weight) return invalid("Goal weight must be 25–350 kg and below your current weight.")
            val goalBmi = goal / ((height / 100) * (height / 100))
            if (goalBmi < 18.5) return invalid("This goal falls below the adult BMI screening range. No weight-loss timeline is shown; get an individual assessment.")
            if (rate !in 0.1..0.9 || rate > goal * 0.01) return invalid("For this planning tool, choose 0.1–0.9 kg/week and no more than 1% of goal weight per week. These limits do not guarantee an appropriate rate for you.")
            val weeks = (weight - goal) / rate
            FitnessResult("${if (weeks < 1) "Less than 1 week" else "About ${ceil(weeks).toInt()} weeks"} at your assumed rate\nAssumes ${number(rate, 2)} kg/week throughout. This is arithmetic, not a forecast: real loss slows or fluctuates. Reassess as your weight changes.")
        }
    }
}
