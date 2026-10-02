package com.rishabh.gymbros.features.progression

import android.content.Context

object GymBrosProgression {
    private const val PREFS = "gymbros_progression"
    private const val KEY_ACHIEVEMENT_XP = "achievement_xp"
    private const val XP_PER_WORKOUT = 100
    const val XP_PER_LEVEL = 1000

    fun achievementXp(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_ACHIEVEMENT_XP, 0)

    fun totalXp(context: Context, totalWorkouts: Int): Int =
        (totalWorkouts.coerceAtLeast(0) * XP_PER_WORKOUT) + achievementXp(context)

    fun level(totalXp: Int): Int = 1 + totalXp.coerceAtLeast(0) / XP_PER_LEVEL

    fun levelProgress(totalXp: Int): Float =
        (totalXp.coerceAtLeast(0) % XP_PER_LEVEL) / XP_PER_LEVEL.toFloat()

    fun xpIntoLevel(totalXp: Int): Int = totalXp.coerceAtLeast(0) % XP_PER_LEVEL

    fun awardAchievementOnce(context: Context, id: String, xp: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "achievement_$id"
        if (prefs.getBoolean(key, false)) return false
        prefs.edit()
            .putBoolean(key, true)
            .putInt(KEY_ACHIEVEMENT_XP, prefs.getInt(KEY_ACHIEVEMENT_XP, 0) + xp.coerceAtLeast(0))
            .apply()
        return true
    }

    fun awardDailyTaskOnce(context: Context, dateKey: String, taskId: String, xp: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "daily_${dateKey}_$taskId"
        if (prefs.getBoolean(key, false)) return false
        prefs.edit()
            .putBoolean(key, true)
            .putInt(KEY_ACHIEVEMENT_XP, prefs.getInt(KEY_ACHIEVEMENT_XP, 0) + xp.coerceAtLeast(0))
            .apply()
        return true
    }

    fun totalDailyTasksCompleted(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all.count { (key, value) ->
            key.startsWith("daily_") && value == true
        }

    fun isDailyTaskAwarded(context: Context, dateKey: String, taskId: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("daily_${dateKey}_$taskId", false)

    fun isAchievementAwarded(context: Context, id: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("achievement_$id", false)
}
