package com.rishabh.gymbros.features.settings.data

import android.content.Context
import com.rishabh.gymbros.features.settings.model.AppSettings
import com.rishabh.gymbros.features.settings.model.AccentTheme
import com.rishabh.gymbros.features.settings.model.DEFAULT_DAILY_GOAL_ML
import com.rishabh.gymbros.features.settings.model.DEFAULT_QUICK_ADD_PRESETS
import com.rishabh.gymbros.features.settings.model.DEFAULT_QUICK_ADD_LABELS
import com.rishabh.gymbros.features.settings.model.HeightUnit
import com.rishabh.gymbros.features.settings.model.WaterUnit
import com.rishabh.gymbros.features.settings.model.WeightUnit
import com.rishabh.gymbros.features.settings.model.UiVersion
import com.rishabh.gymbros.widgets.GymBrosWidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /**
     * Switches the launcher icon to match the selected interface version.
     *
     * Enabling/disabling the launcher activity-alias that started the running task makes Android
     * close the app, so this must never run while the app is on screen. MainActivity calls it from
     * onStop(), i.e. once the user has left the app.
     */
    fun applyLauncherIcon() {
        LauncherIconManager.applyForUiVersion(appContext, UiVersion.V2)
    }

    fun reloadFromDisk() {
        _settings.value = loadSettings()
    }

    val currentSettings: AppSettings
        get() = _settings.value

    fun setProfile(
        name: String,
        ageYears: Int?,
        weightKg: Float?,
        heightCm: Int?
    ) {
        update(updateWidgets = false) {
            copy(
                userName = name.trim().take(30),
                ageYears = ageYears?.coerceIn(10, 120),
                weightKg = weightKg?.coerceIn(20f, 500f),
                heightCm = heightCm?.coerceIn(80, 250)
            )
        }
    }


    fun setProfileImageUri(uri: String) {
        update(updateWidgets = false) { copy(profileImageUri = uri.trim()) }
    }

    fun setDailyGoalMl(value: Int) {
        update { copy(dailyGoalMl = value.coerceIn(500, 5_000)) }
    }

    fun setQuickAddPreset(index: Int, amountMl: Int) {
        if (index !in 0..2) return
        update {
            val values = quickAddPresets.toMutableList().apply {
                while (size < 3) add(DEFAULT_QUICK_ADD_PRESETS[size])
                this[index] = amountMl.coerceIn(50, 5_000)
            }
            copy(quickAddPresets = values)
        }
    }


    fun setQuickAddLabel(index: Int, label: String) {
        if (index !in 0..2) return
        update(updateWidgets = false) {
            val labels = quickAddLabels.toMutableList().apply {
                while (size < 3) add(DEFAULT_QUICK_ADD_LABELS[size])
                this[index] = label.take(24)
            }
            copy(quickAddLabels = labels)
        }
    }

    fun setWidgetButtonMl(amountMl: Int) {
        update { copy(widgetButtonMl = amountMl.coerceIn(50, 5_000)) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        update { copy(reminderEnabled = enabled) }
    }

    fun setReminderStartHour(hour: Int) {
        update {
            val safe = hour.coerceIn(0, 22).coerceAtMost(reminderEndHour - 1)
            copy(reminderStartHour = safe)
        }
    }

    fun setReminderEndHour(hour: Int) {
        update {
            val safe = hour.coerceIn(1, 23).coerceAtLeast(reminderStartHour + 1)
            copy(reminderEndHour = safe)
        }
    }

    fun setReminderIntervalHours(hours: Int) {
        update { copy(reminderIntervalHours = hours.coerceIn(1, 6)) }
    }

    fun setUnit(unit: WaterUnit) {
        update { copy(unit = unit) }
    }

    fun setWeightUnit(unit: WeightUnit) {
        update(updateWidgets = false) { copy(weightUnit = unit) }
    }

    fun setHeightUnit(unit: HeightUnit) {
        update(updateWidgets = false) { copy(heightUnit = unit) }
    }

    fun setAccentTheme(theme: AccentTheme) {
        update(updateWidgets = false) { copy(accentTheme = theme) }
    }

    fun setUiVersion(version: UiVersion) {
        // Arctic is the only live interface; keep this API for old callers/data compatibility.
        update(updateWidgets = false) { copy(uiVersion = UiVersion.V2) }
    }

    private fun update(
        updateWidgets: Boolean = true,
        transform: AppSettings.() -> AppSettings
    ) {
        val next = _settings.value.transform()
        _settings.value = next
        preferences.edit()
            .putString(KEY_USER_NAME, next.userName)
            .putString(KEY_PROFILE_IMAGE_URI, next.profileImageUri)
            .putInt(KEY_AGE_YEARS, next.ageYears ?: 0)
            .putFloat(KEY_WEIGHT_KG, next.weightKg ?: 0f)
            .putInt(KEY_HEIGHT_CM, next.heightCm ?: 0)
            .putInt(KEY_DAILY_GOAL, next.dailyGoalMl)
            .putInt(KEY_QUICK_1, next.quickAddPresets.getOrElse(0) { 250 })
            .putInt(KEY_QUICK_2, next.quickAddPresets.getOrElse(1) { 500 })
            .putInt(KEY_QUICK_3, next.quickAddPresets.getOrElse(2) { 750 })
            .putString(KEY_QUICK_LABEL_1, next.quickAddLabels.getOrElse(0) { DEFAULT_QUICK_ADD_LABELS[0] })
            .putString(KEY_QUICK_LABEL_2, next.quickAddLabels.getOrElse(1) { DEFAULT_QUICK_ADD_LABELS[1] })
            .putString(KEY_QUICK_LABEL_3, next.quickAddLabels.getOrElse(2) { DEFAULT_QUICK_ADD_LABELS[2] })
            .putInt(KEY_WIDGET_BUTTON_ML, next.widgetButtonMl)
            .putBoolean(KEY_REMINDER_ENABLED, next.reminderEnabled)
            .putInt(KEY_REMINDER_START, next.reminderStartHour)
            .putInt(KEY_REMINDER_END, next.reminderEndHour)
            .putInt(KEY_REMINDER_INTERVAL, next.reminderIntervalHours)
            .putString(KEY_UNIT, next.unit.name)
            .putString(KEY_WEIGHT_UNIT, next.weightUnit.name)
            .putString(KEY_HEIGHT_UNIT, next.heightUnit.name)
            .putString(KEY_ACCENT_THEME, next.accentTheme.name)
            .putString(KEY_UI_VERSION, next.uiVersion.name)
            .apply()
        if (updateWidgets) GymBrosWidgetUpdater.updateAll(appContext)
    }

    private fun loadSettings(): AppSettings {
        val unit = runCatching {
            WaterUnit.valueOf(preferences.getString(KEY_UNIT, WaterUnit.ML.name) ?: WaterUnit.ML.name)
        }.getOrDefault(WaterUnit.ML)
        val weightUnit = runCatching {
            WeightUnit.valueOf(
                preferences.getString(KEY_WEIGHT_UNIT, WeightUnit.KG.name) ?: WeightUnit.KG.name
            )
        }.getOrDefault(WeightUnit.KG)
        val heightUnit = runCatching {
            HeightUnit.valueOf(
                preferences.getString(KEY_HEIGHT_UNIT, HeightUnit.FT.name) ?: HeightUnit.FT.name
            )
        }.getOrDefault(HeightUnit.FT)

        val accentTheme = runCatching {
            AccentTheme.valueOf(preferences.getString(KEY_ACCENT_THEME, AccentTheme.ORANGE.name) ?: AccentTheme.ORANGE.name)
        }.getOrDefault(AccentTheme.ORANGE)
        val uiVersion = runCatching {
            UiVersion.valueOf(preferences.getString(KEY_UI_VERSION, UiVersion.V2.name) ?: UiVersion.V2.name)
        }.getOrDefault(UiVersion.V2).let { UiVersion.V2 }

        val storedGoal = preferences.getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL_ML)
        val goalAlreadyMigrated = preferences.getBoolean(KEY_IGNITE_GOAL_MIGRATED, false)
        val dailyGoal = if (!goalAlreadyMigrated && storedGoal == LEGACY_DEFAULT_DAILY_GOAL_ML) {
            DEFAULT_DAILY_GOAL_ML
        } else {
            storedGoal
        }.coerceIn(500, 5_000)

        if (!goalAlreadyMigrated) {
            preferences.edit()
                .putBoolean(KEY_IGNITE_GOAL_MIGRATED, true)
                .putInt(KEY_DAILY_GOAL, dailyGoal)
                .apply()
        }

        return AppSettings(
            userName = preferences.getString(KEY_USER_NAME, "").orEmpty().take(30),
            profileImageUri = preferences.getString(KEY_PROFILE_IMAGE_URI, "").orEmpty(),
            ageYears = preferences.getInt(KEY_AGE_YEARS, 0).takeIf { it > 0 }?.coerceIn(10, 120),
            weightKg = readFloatSafely(KEY_WEIGHT_KG, 0f).takeIf { it > 0f }?.coerceIn(20f, 500f),
            heightCm = preferences.getInt(KEY_HEIGHT_CM, 0).takeIf { it > 0 }?.coerceIn(80, 250),
            dailyGoalMl = dailyGoal,
            quickAddPresets = listOf(
                preferences.getInt(KEY_QUICK_1, 250).coerceIn(50, 5_000),
                preferences.getInt(KEY_QUICK_2, 500).coerceIn(50, 5_000),
                preferences.getInt(KEY_QUICK_3, 750).coerceIn(50, 5_000)
            ),
            quickAddLabels = listOf(
                preferences.getString(KEY_QUICK_LABEL_1, DEFAULT_QUICK_ADD_LABELS[0]).orEmpty().ifBlank { DEFAULT_QUICK_ADD_LABELS[0] }.take(24),
                preferences.getString(KEY_QUICK_LABEL_2, DEFAULT_QUICK_ADD_LABELS[1]).orEmpty().ifBlank { DEFAULT_QUICK_ADD_LABELS[1] }.take(24),
                preferences.getString(KEY_QUICK_LABEL_3, DEFAULT_QUICK_ADD_LABELS[2]).orEmpty().ifBlank { DEFAULT_QUICK_ADD_LABELS[2] }.take(24)
            ),
            widgetButtonMl = preferences.getInt(
                KEY_WIDGET_BUTTON_ML,
                preferences.getInt(KEY_QUICK_1, 250)
            ).coerceIn(50, 5_000),
            reminderEnabled = preferences.getBoolean(KEY_REMINDER_ENABLED, false),
            reminderStartHour = preferences.getInt(KEY_REMINDER_START, 8).coerceIn(0, 22),
            reminderEndHour = preferences.getInt(KEY_REMINDER_END, 20).coerceIn(1, 23),
            reminderIntervalHours = preferences.getInt(KEY_REMINDER_INTERVAL, 2).coerceIn(1, 6),
            unit = unit,
            weightUnit = weightUnit,
            heightUnit = heightUnit,
            accentTheme = accentTheme,
            uiVersion = uiVersion
        ).let { loaded ->
            if (loaded.reminderEndHour <= loaded.reminderStartHour) {
                loaded.copy(reminderStartHour = 8, reminderEndHour = 20)
            } else loaded
        }
    }

    private fun readFloatSafely(key: String, defaultValue: Float): Float {
        val raw = preferences.all[key] ?: return defaultValue
        return when (raw) {
            is Float -> raw
            is Number -> raw.toFloat().also { repaired ->
                // Repair legacy cloud restores that stored an integral Float as Int/Long.
                preferences.edit().putFloat(key, repaired).commit()
            }
            is String -> raw.toFloatOrNull()?.also { repaired ->
                preferences.edit().putFloat(key, repaired).commit()
            } ?: defaultValue
            else -> defaultValue
        }
    }

    companion object {
        private const val PREFS_NAME = "gymbros_settings"
        private const val LEGACY_DEFAULT_DAILY_GOAL_ML = 3_000
        private const val KEY_IGNITE_GOAL_MIGRATED = "ignite_goal_migrated"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_PROFILE_IMAGE_URI = "profile_image_uri"
        private const val KEY_AGE_YEARS = "age_years"
        private const val KEY_WEIGHT_KG = "weight_kg"
        private const val KEY_HEIGHT_CM = "height_cm"
        private const val KEY_DAILY_GOAL = "daily_goal_ml"
        private const val KEY_QUICK_1 = "quick_add_1"
        private const val KEY_QUICK_2 = "quick_add_2"
        private const val KEY_QUICK_3 = "quick_add_3"
        private const val KEY_QUICK_LABEL_1 = "quick_add_label_1"
        private const val KEY_QUICK_LABEL_2 = "quick_add_label_2"
        private const val KEY_QUICK_LABEL_3 = "quick_add_label_3"
        private const val KEY_WIDGET_BUTTON_ML = "widget_button_ml"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_START = "reminder_start_hour"
        private const val KEY_REMINDER_END = "reminder_end_hour"
        private const val KEY_REMINDER_INTERVAL = "reminder_interval_hours"
        private const val KEY_UNIT = "water_unit"
        private const val KEY_WEIGHT_UNIT = "weight_unit"
        private const val KEY_HEIGHT_UNIT = "height_unit"
        private const val KEY_ACCENT_THEME = "accent_theme"
        private const val KEY_UI_VERSION = "ui_version"

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
