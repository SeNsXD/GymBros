package com.rishabh.gymbros.features.nutrition.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.rishabh.gymbros.features.nutrition.model.FoodLogEntry
import com.rishabh.gymbros.features.nutrition.model.MealType

internal class NutritionDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE food_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                food_code TEXT NOT NULL,
                food_name TEXT NOT NULL,
                grams REAL NOT NULL CHECK(grams > 0),
                calories REAL NOT NULL,
                protein REAL NOT NULL,
                carbs REAL NOT NULL,
                fat REAL NOT NULL,
                fiber REAL NOT NULL,
                meal TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_food_log_timestamp ON food_log(timestamp)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun insert(entry: FoodLogEntry): Long = writableDatabase.insertOrThrow("food_log", null, values(entry))
    fun update(entry: FoodLogEntry): Boolean = writableDatabase.update("food_log", values(entry), "id = ?", arrayOf(entry.id.toString())) > 0
    fun delete(id: Long): Boolean = writableDatabase.delete("food_log", "id = ?", arrayOf(id.toString())) > 0

    fun entriesBetween(start: Long, end: Long): List<FoodLogEntry> {
        readableDatabase.query("food_log", PROJECTION, "timestamp >= ? AND timestamp < ?", arrayOf(start.toString(), end.toString()), null, null, "timestamp DESC, id DESC").use { c ->
            return buildList { while (c.moveToNext()) add(c.toEntry()) }
        }
    }

    private fun values(e: FoodLogEntry) = ContentValues().apply {
        put("food_code", e.foodCode); put("food_name", e.foodName); put("grams", e.grams)
        put("calories", e.calories); put("protein", e.protein); put("carbs", e.carbs); put("fat", e.fat); put("fiber", e.fiber)
        put("meal", e.meal.name); put("timestamp", e.timestamp)
    }

    private fun android.database.Cursor.toEntry() = FoodLogEntry(
        id = getLong(getColumnIndexOrThrow("id")), foodCode = getString(getColumnIndexOrThrow("food_code")),
        foodName = getString(getColumnIndexOrThrow("food_name")), grams = getDouble(getColumnIndexOrThrow("grams")),
        calories = getDouble(getColumnIndexOrThrow("calories")), protein = getDouble(getColumnIndexOrThrow("protein")),
        carbs = getDouble(getColumnIndexOrThrow("carbs")), fat = getDouble(getColumnIndexOrThrow("fat")),
        fiber = getDouble(getColumnIndexOrThrow("fiber")),
        meal = runCatching { MealType.valueOf(getString(getColumnIndexOrThrow("meal"))) }.getOrDefault(MealType.SNACKS),
        timestamp = getLong(getColumnIndexOrThrow("timestamp"))
    )

    companion object {
        const val DATABASE_NAME = "gymbros_nutrition.db"
        private const val DATABASE_VERSION = 1
        private val PROJECTION = arrayOf("id","food_code","food_name","grams","calories","protein","carbs","fat","fiber","meal","timestamp")
    }
}
