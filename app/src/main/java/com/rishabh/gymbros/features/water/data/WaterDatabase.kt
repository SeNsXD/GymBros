package com.rishabh.gymbros.features.water.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.rishabh.gymbros.features.water.model.WaterEntry

internal class WaterDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_WATER_ENTRIES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_AMOUNT_ML INTEGER NOT NULL CHECK($COL_AMOUNT_ML > 0)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX idx_water_entries_timestamp ON $TABLE_WATER_ENTRIES($COL_TIMESTAMP)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1. Future schema migrations should be added here without deleting user history.
    }

    fun insertEntry(timestamp: Long, amountMl: Int): Long {
        require(amountMl > 0)
        val values = ContentValues().apply {
            put(COL_TIMESTAMP, timestamp)
            put(COL_AMOUNT_ML, amountMl)
        }
        return writableDatabase.insertOrThrow(TABLE_WATER_ENTRIES, null, values)
    }

    fun deleteEntry(id: Long): Boolean {
        return writableDatabase.delete(
            TABLE_WATER_ENTRIES,
            "$COL_ID = ?",
            arrayOf(id.toString())
        ) > 0
    }

    fun updateEntryAmount(id: Long, amountMl: Int): Boolean {
        require(amountMl > 0)
        val values = ContentValues().apply {
            put(COL_AMOUNT_ML, amountMl)
        }
        return writableDatabase.update(
            TABLE_WATER_ENTRIES,
            values,
            "$COL_ID = ?",
            arrayOf(id.toString())
        ) > 0
    }

    fun latestEntryBetween(startInclusive: Long, endExclusive: Long): WaterEntry? {
        readableDatabase.query(
            TABLE_WATER_ENTRIES,
            PROJECTION,
            "$COL_TIMESTAMP >= ? AND $COL_TIMESTAMP < ?",
            arrayOf(startInclusive.toString(), endExclusive.toString()),
            null,
            null,
            "$COL_TIMESTAMP DESC, $COL_ID DESC",
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toWaterEntry() else null
        }
    }

    fun entriesBetween(startInclusive: Long, endExclusive: Long): List<WaterEntry> {
        readableDatabase.query(
            TABLE_WATER_ENTRIES,
            PROJECTION,
            "$COL_TIMESTAMP >= ? AND $COL_TIMESTAMP < ?",
            arrayOf(startInclusive.toString(), endExclusive.toString()),
            null,
            null,
            "$COL_TIMESTAMP DESC, $COL_ID DESC"
        ).use { cursor ->
            return buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toWaterEntry())
                }
            }
        }
    }

    fun clearAllWaterData() {
        writableDatabase.delete(TABLE_WATER_ENTRIES, null, null)
    }

    fun allEntries(): List<WaterEntry> {
        readableDatabase.query(
            TABLE_WATER_ENTRIES,
            PROJECTION,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC, $COL_ID DESC"
        ).use { cursor ->
            return buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toWaterEntry())
                }
            }
        }
    }

    private fun android.database.Cursor.toWaterEntry(): WaterEntry {
        return WaterEntry(
            id = getLong(getColumnIndexOrThrow(COL_ID)),
            timestamp = getLong(getColumnIndexOrThrow(COL_TIMESTAMP)),
            amountMl = getInt(getColumnIndexOrThrow(COL_AMOUNT_ML))
        )
    }

    private companion object {
        const val DATABASE_NAME = "gymbros_water.db"
        const val DATABASE_VERSION = 1

        const val TABLE_WATER_ENTRIES = "water_entries"
        const val COL_ID = "id"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_AMOUNT_ML = "amount_ml"

        val PROJECTION = arrayOf(COL_ID, COL_TIMESTAMP, COL_AMOUNT_ML)
    }
}
