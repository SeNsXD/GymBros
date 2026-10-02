package com.rishabh.gymbros.features.checklist

import android.content.Context

data class GymChecklistItem(val id: Long, val name: String, val checked: Boolean)
data class GymChecklistSettings(val enabled: Boolean, val hour: Int, val minute: Int, val days: Set<Int>)

class GymChecklistStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("gym_checklist", Context.MODE_PRIVATE)

    fun items(): List<GymChecklistItem> {
        val raw = prefs.getString("items", null)
        if (raw == null) return defaultItems().also { saveItems(it) }
        return raw.split("\n").filter { it.isNotBlank() }.mapNotNull { line ->
            val p = line.split("|", limit = 3); if (p.size != 3) null else GymChecklistItem(p[0].toLongOrNull() ?: return@mapNotNull null, decode(p[2]), p[1] == "1")
        }
    }

    fun saveItems(items: List<GymChecklistItem>) {
        prefs.edit().putString("items", items.joinToString("\n") { "${it.id}|${if (it.checked) 1 else 0}|${encode(it.name)}" }).apply()
    }

    fun settings(): GymChecklistSettings = GymChecklistSettings(
        prefs.getBoolean("enabled", false), prefs.getInt("hour", 17), prefs.getInt("minute", 30),
        (prefs.getStringSet("days", setOf("1","2","3","4","5","6")) ?: emptySet())
            .toSet().mapNotNull { it.toIntOrNull() }.filter { it in 1..7 }.toSet()
    )

    fun saveSettings(value: GymChecklistSettings) {
        prefs.edit().putBoolean("enabled", value.enabled).putInt("hour", value.hour).putInt("minute", value.minute)
            .putStringSet("days", value.days.map { it.toString() }.toSet()).apply()
    }

    fun resetChecks() = saveItems(items().map { it.copy(checked = false) })

    private fun defaultItems() = listOf("Water Bottle", "Headphones", "Towel", "Lifting Straps", "Gym Shoes", "Shaker")
        .mapIndexed { i, n -> GymChecklistItem(System.currentTimeMillis() + i, n, false) }
    private fun encode(s: String) = s.replace("%", "%25").replace("|", "%7C").replace("\n", " ")
    private fun decode(s: String) = s.replace("%7C", "|").replace("%25", "%")
}
