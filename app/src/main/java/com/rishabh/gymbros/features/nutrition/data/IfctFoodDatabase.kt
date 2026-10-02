package com.rishabh.gymbros.features.nutrition.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.rishabh.gymbros.features.nutrition.model.FoodReference
import org.json.JSONArray

/**
 * Bundled GymBros food reference database.
 *
 * The class name is intentionally kept for source compatibility, but v2 no longer uses the
 * old IFCT-only asset. It is seeded from the GymBros master database (INDB + selected USDA
 * essentials) and supports Indian/common-name aliases during search.
 */
internal class IfctFoodDatabase(private val context: Context) : SQLiteOpenHelper(
    context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        createSchema(db)
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This is reference data only. User meal history lives in NutritionDatabase, so replacing
        // this table is safe and guarantees existing installs do not keep the outdated food list.
        db.execSQL("DROP TABLE IF EXISTS foods")
        createSchema(db)
        seed(db)
    }

    private fun createSchema(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE foods (
                code TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                calories REAL NOT NULL,
                protein REAL NOT NULL,
                carbs REAL NOT NULL,
                fat REAL NOT NULL,
                fiber REAL NOT NULL,
                aliases TEXT NOT NULL DEFAULT '',
                source TEXT NOT NULL DEFAULT '',
                source_id TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_food_name ON foods(name COLLATE NOCASE)")
        db.execSQL("CREATE INDEX idx_food_aliases ON foods(aliases COLLATE NOCASE)")
    }

    private fun seed(db: SQLiteDatabase) {
        val json = context.assets.open("ifct_foods.json").bufferedReader().use { it.readText() }
        val foods = JSONArray(json)
        db.beginTransaction()
        try {
            for (i in 0 until foods.length()) {
                val item = foods.getJSONObject(i)
                val name = item.getString("name")
                val suppliedAliases = item.optString("aliases")
                db.insertOrThrow("foods", null, ContentValues().apply {
                    put("code", item.getString("code"))
                    put("name", name)
                    put("category", item.optString("category", "Food"))
                    put("calories", item.optDouble("energyKcal", 0.0))
                    put("protein", item.optDouble("protein", 0.0))
                    put("carbs", item.optDouble("carbs", 0.0))
                    put("fat", item.optDouble("fat", 0.0))
                    put("fiber", item.optDouble("fiber", 0.0))
                    put("aliases", mergeAliases(suppliedAliases, aliasesFor(name)))
                    put("source", item.optString("source"))
                    put("source_id", item.optString("sourceId"))
                })
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun search(query: String, limit: Int = 40): List<FoodReference> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val like = "%$q%"
        readableDatabase.query(
            "foods",
            PROJECTION,
            "name LIKE ? COLLATE NOCASE OR aliases LIKE ? COLLATE NOCASE",
            arrayOf(like, like),
            null,
            null,
            "name COLLATE NOCASE",
            limit.toString()
        ).use { c ->
            return buildList { while (c.moveToNext()) add(c.toFood()) }
                .sortedWith(compareBy<FoodReference> {
                    when {
                        it.name.equals(q, true) -> 0
                        it.name.startsWith(q, true) -> 1
                        else -> 2
                    }
                }.thenBy { it.name.lowercase() })
        }
    }

    fun findByCode(code: String): FoodReference? {
        readableDatabase.query(
            "foods", PROJECTION, "code = ?", arrayOf(code), null, null, null, "1"
        ).use { c -> return if (c.moveToFirst()) c.toFood() else null }
    }

    private fun android.database.Cursor.toFood() = FoodReference(
        code = getString(getColumnIndexOrThrow("code")),
        name = getString(getColumnIndexOrThrow("name")),
        category = getString(getColumnIndexOrThrow("category")),
        caloriesPer100g = getDouble(getColumnIndexOrThrow("calories")),
        proteinPer100g = getDouble(getColumnIndexOrThrow("protein")),
        carbsPer100g = getDouble(getColumnIndexOrThrow("carbs")),
        fatPer100g = getDouble(getColumnIndexOrThrow("fat")),
        fiberPer100g = getDouble(getColumnIndexOrThrow("fiber"))
    )

    private fun aliasesFor(name: String): String {
        val n = name.lowercase()
        val aliases = linkedSetOf<String>()
        fun add(vararg values: String) = aliases.addAll(values)

        if ("chapati" in n || "roti" in n) add("roti", "chapati", "chapatti", "phulka")
        if ("tea" in n || "chai" in n) add("chai", "tea", "garam chai")
        if ("coffee" in n) add("coffee", "kaafi")
        if ("curd" in n || "yogurt" in n || "yoghurt" in n) add("dahi", "curd", "yogurt", "yoghurt")
        if ("buttermilk" in n) add("chaas", "chhach", "chach", "buttermilk")
        if ("milk" in n) add("milk", "doodh", "dudh")
        if ("chickpea" in n || "chana" in n) add("chana", "chickpea", "chickpeas", "garbanzo")
        if ("kidney bean" in n || "rajma" in n) add("rajma", "kidney beans")
        if ("egg" in n) add("egg", "anda")
        if ("banana" in n) add("banana", "kela")
        if ("mango" in n) add("mango", "aam")
        if ("almond" in n) add("almond", "badam")
        if ("peanut" in n) add("peanut", "moongfali", "mungfali")
        if ("ghee" in n || "clarified butter" in n) add("ghee", "desi ghee", "ghi", "clarified butter")
        if ("cottage cheese" in n || "paneer" in n) add("paneer", "cottage cheese")
        if ("puffed rice" in n) add("murmura", "muri", "puffed rice")
        if ("flattened rice" in n || "poha" in n) add("poha", "pohe", "flattened rice")

        return aliases.joinToString(";")
    }

    private fun mergeAliases(first: String, second: String): String =
        (first.split(';') + second.split(';'))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .joinToString(";")

    companion object {
        private const val DATABASE_NAME = "gymbros_ifct.db"
        private const val DATABASE_VERSION = 2
        private val PROJECTION = arrayOf(
            "code", "name", "category", "calories", "protein", "carbs", "fat", "fiber"
        )
    }
}
