package com.rishabh.gymbros.features.onboarding

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

enum class OnboardingStep { PRIVACY, PROFILE, CONNECTIONS, COMPLETE }

class OnboardingStore(context: Context) {
    // Kept separate from cloud snapshots: acknowledgement belongs to this installation.
    private val preferences = context.getSharedPreferences("gymbros_onboarding", Context.MODE_PRIVATE)
    private val initialStep = if (preferences.contains("step")) {
        runCatching { OnboardingStep.valueOf(preferences.getString("step", "PRIVACY")!!) }
            .getOrDefault(OnboardingStep.PRIVACY)
    } else {
        // Existing installations keep access to their data without repeating setup.
        val existingInstall = context.getSharedPreferences("gymbros_settings", Context.MODE_PRIVATE).all.isNotEmpty()
        (if (existingInstall) OnboardingStep.COMPLETE else OnboardingStep.PRIVACY).also {
            preferences.edit().putString("step", it.name).apply()
        }
    }
    private val mutableStep = MutableStateFlow(initialStep)
    val step = mutableStep.asStateFlow()

    fun goTo(step: OnboardingStep) {
        preferences.edit().putString("step", step.name).apply()
        mutableStep.value = step
    }
}

internal fun profileInputError(name: String, age: String, height: String, weight: String): String? = when {
    name.isBlank() -> "Enter your name."
    name.trim().length > 30 -> "Use 30 characters or fewer for your name."
    age.toIntOrNull() !in 10..120 -> "Enter an age between 10 and 120."
    height.isNotBlank() && height.toIntOrNull() !in 80..250 -> "Enter a height between 80 and 250 cm, or leave it blank."
    weight.isNotBlank() && (weight.toFloatOrNull()?.let { it.isFinite() && it in 20f..500f } != true) ->
        "Enter a weight between 20 and 500 kg, or leave it blank."
    else -> null
}

internal fun acceptsNumericInput(value: String, decimal: Boolean = false): Boolean =
    value.all { it in '0'..'9' || (decimal && it == '.') } && value.count { it == '.' } <= 1

internal fun heightFromFeet(feet: String, inches: String): Int? {
    val ft = feet.toIntOrNull() ?: return null
    val inch = if (inches.isBlank()) 0 else inches.toIntOrNull() ?: return null
    if (ft !in 0..8 || inch !in 0..11) return null
    return ((ft * 12 + inch) * 2.54).roundToInt().takeIf { it in 80..250 }
}
