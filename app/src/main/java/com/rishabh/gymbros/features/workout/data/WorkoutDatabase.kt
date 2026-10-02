package com.rishabh.gymbros.features.workout.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.rishabh.gymbros.features.workout.model.PlannedExercise
import com.rishabh.gymbros.features.workout.model.WorkoutCategory
import com.rishabh.gymbros.features.workout.model.WorkoutExerciseType
import com.rishabh.gymbros.features.workout.model.WorkoutLayout
import com.rishabh.gymbros.features.workout.model.WorkoutLayoutExercise
import com.rishabh.gymbros.features.workout.model.WorkoutSession
import com.rishabh.gymbros.features.workout.model.WorkoutSetDraft
import com.rishabh.gymbros.features.workout.model.WorkoutSetRecord

internal class WorkoutDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        createSessionTable(db)
        createExerciseTable(db)
        createSetLogTable(db)
        createLayoutTables(db)
        createSessionMetricTable(db)
        insertDefaultExercises(db)
        insertAdvancedDefaultsIfEmpty(db)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createSetLogTable(db)
        }
        if (oldVersion < 3) {
            createLayoutTables(db)
    
        }
        if (oldVersion < 4) {
            createSessionMetricTable(db)
            insertAdvancedDefaultsIfEmpty(db)
        }
        if (oldVersion < 5) {
            runCatching { db.execSQL("ALTER TABLE $TABLE_LAYOUT_EXERCISES ADD COLUMN $COL_LAYOUT_EXERCISE_TYPE TEXT NOT NULL DEFAULT 'STRENGTH'") }
            runCatching { db.execSQL("ALTER TABLE $TABLE_LAYOUT_EXERCISES ADD COLUMN $COL_LAYOUT_EXERCISE_CARDIO_LEVEL REAL NOT NULL DEFAULT 0") }
        }
        if (oldVersion < 6) {
            runCatching { db.execSQL("ALTER TABLE $TABLE_LAYOUT_EXERCISES ADD COLUMN $COL_LAYOUT_EXERCISE_EQUIPMENT TEXT NOT NULL DEFAULT ''") }
        }
        if (oldVersion < 7) {
            // Cardio targetReps used to mean minutes; Ignite v2 stores exact duration seconds.
            runCatching { db.execSQL("UPDATE $TABLE_LAYOUT_EXERCISES SET $COL_LAYOUT_EXERCISE_REPS = $COL_LAYOUT_EXERCISE_REPS * 60 WHERE $COL_LAYOUT_EXERCISE_TYPE = 'CARDIO_DISTANCE'") }
        }
        if (oldVersion < 8) {
            runCatching { db.execSQL("ALTER TABLE $TABLE_PLANNED_EXERCISES ADD COLUMN $COL_EXERCISE_TAG TEXT NOT NULL DEFAULT ''") }
            runCatching { db.execSQL("ALTER TABLE $TABLE_PLANNED_EXERCISES ADD COLUMN $COL_EXERCISE_TYPE TEXT NOT NULL DEFAULT 'STRENGTH'") }
            runCatching { db.execSQL("ALTER TABLE $TABLE_PLANNED_EXERCISES ADD COLUMN $COL_EXERCISE_EQUIPMENT TEXT NOT NULL DEFAULT ''") }
        }
        if (oldVersion < 9) removeUnchangedStarterLayout(db)
        if (oldVersion < 10) {
            db.execSQL("ALTER TABLE $TABLE_SESSIONS ADD COLUMN $COL_MUSIC_URI TEXT")
            db.execSQL("ALTER TABLE $TABLE_SESSIONS ADD COLUMN $COL_MUSIC_TITLE TEXT")
            db.execSQL("ALTER TABLE $TABLE_SESSIONS ADD COLUMN $COL_MUSIC_ARTIST TEXT")
        }
        if (oldVersion < 11) {
            runCatching { db.execSQL("ALTER TABLE $TABLE_SET_LOGS ADD COLUMN $COL_SET_EQUIPMENT TEXT NOT NULL DEFAULT ''") }
            runCatching { db.execSQL("ALTER TABLE $TABLE_SET_LOGS ADD COLUMN $COL_SET_QUALITY INTEGER") }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    private fun createSessionTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SESSIONS (
                $COL_SESSION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_STARTED_AT INTEGER NOT NULL,
                $COL_ENDED_AT INTEGER NOT NULL,
                $COL_DURATION_SECONDS INTEGER NOT NULL,
                $COL_VOLUME_KG REAL NOT NULL,
                $COL_COMPLETED_EXERCISES INTEGER NOT NULL,
                $COL_TOTAL_EXERCISES INTEGER NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_TITLE TEXT NOT NULL,
                $COL_MUSIC_URI TEXT,
                $COL_MUSIC_TITLE TEXT,
                $COL_MUSIC_ARTIST TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_workout_session_ended_at ON $TABLE_SESSIONS($COL_ENDED_AT)"
        )
    }

    private fun createExerciseTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PLANNED_EXERCISES (
                $COL_EXERCISE_ID TEXT PRIMARY KEY,
                $COL_EXERCISE_NAME TEXT NOT NULL,
                $COL_SETS INTEGER NOT NULL,
                $COL_REPS_LABEL TEXT NOT NULL,
                $COL_TARGET_REPS INTEGER NOT NULL,
                $COL_LOAD_KG REAL NOT NULL,
                $COL_EXERCISE_CATEGORY TEXT NOT NULL,
                $COL_IS_CUSTOM INTEGER NOT NULL DEFAULT 0,
                $COL_EXERCISE_TAG TEXT NOT NULL DEFAULT '',
                $COL_EXERCISE_TYPE TEXT NOT NULL DEFAULT 'STRENGTH',
                $COL_EXERCISE_EQUIPMENT TEXT NOT NULL DEFAULT '' 
            )
            """.trimIndent()
        )
    }

    private fun createSetLogTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SET_LOGS (
                $COL_SET_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SET_SESSION_ID INTEGER NOT NULL,
                $COL_SET_EXERCISE_ID TEXT NOT NULL,
                $COL_SET_EXERCISE_NAME TEXT NOT NULL,
                $COL_SET_NUMBER INTEGER NOT NULL,
                $COL_SET_REPS INTEGER NOT NULL,
                $COL_SET_LOAD_KG REAL NOT NULL,
                $COL_SET_COMPLETED INTEGER NOT NULL,
                $COL_SET_EQUIPMENT TEXT NOT NULL DEFAULT '',
                $COL_SET_QUALITY INTEGER,
                FOREIGN KEY($COL_SET_SESSION_ID) REFERENCES $TABLE_SESSIONS($COL_SESSION_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_workout_set_session ON $TABLE_SET_LOGS($COL_SET_SESSION_ID)"
        )
    }

    private fun createLayoutTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_LAYOUTS (
                $COL_LAYOUT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LAYOUT_TITLE TEXT NOT NULL,
                $COL_LAYOUT_CATEGORY TEXT NOT NULL,
                $COL_LAYOUT_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_LAYOUT_EXERCISES (
                $COL_LAYOUT_EXERCISE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LAYOUT_EXERCISE_LAYOUT_ID INTEGER NOT NULL,
                $COL_LAYOUT_EXERCISE_POSITION INTEGER NOT NULL,
                $COL_LAYOUT_EXERCISE_NAME TEXT NOT NULL,
                $COL_LAYOUT_EXERCISE_SETS INTEGER NOT NULL,
                $COL_LAYOUT_EXERCISE_REPS INTEGER NOT NULL,
                $COL_LAYOUT_EXERCISE_LOAD_KG REAL NOT NULL,
                $COL_LAYOUT_EXERCISE_TYPE TEXT NOT NULL DEFAULT 'STRENGTH',
                $COL_LAYOUT_EXERCISE_CARDIO_LEVEL REAL NOT NULL DEFAULT 0,
                $COL_LAYOUT_EXERCISE_EQUIPMENT TEXT NOT NULL DEFAULT '',
                FOREIGN KEY($COL_LAYOUT_EXERCISE_LAYOUT_ID) REFERENCES $TABLE_LAYOUTS($COL_LAYOUT_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_layout_exercise_layout ON $TABLE_LAYOUT_EXERCISES($COL_LAYOUT_EXERCISE_LAYOUT_ID, $COL_LAYOUT_EXERCISE_POSITION)"
        )
    }

    private fun createSessionMetricTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SESSION_METRICS (
                $COL_METRIC_SESSION_ID INTEGER NOT NULL,
                $COL_METRIC_KEY TEXT NOT NULL,
                $COL_METRIC_VALUE REAL NOT NULL,
                PRIMARY KEY($COL_METRIC_SESSION_ID, $COL_METRIC_KEY),
                FOREIGN KEY($COL_METRIC_SESSION_ID) REFERENCES $TABLE_SESSIONS($COL_SESSION_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_workout_metric_session ON $TABLE_SESSION_METRICS($COL_METRIC_SESSION_ID)"
        )
    }

    fun insertSession(
        startedAt: Long,
        endedAt: Long,
        durationSeconds: Long,
        totalVolumeKg: Double,
        completedExercises: Int,
        totalExercises: Int,
        category: WorkoutCategory,
        title: String,
        musicTrackUri: String? = null,
        musicTrackTitle: String? = null,
        musicTrackArtist: String? = null
    ): Long {
        val values = ContentValues().apply {
            put(COL_STARTED_AT, startedAt)
            put(COL_ENDED_AT, endedAt)
            put(COL_DURATION_SECONDS, durationSeconds)
            put(COL_VOLUME_KG, totalVolumeKg)
            put(COL_COMPLETED_EXERCISES, completedExercises)
            put(COL_TOTAL_EXERCISES, totalExercises)
            put(COL_CATEGORY, category.name)
            put(COL_TITLE, title)
            put(COL_MUSIC_URI, musicTrackUri)
            put(COL_MUSIC_TITLE, musicTrackTitle)
            put(COL_MUSIC_ARTIST, musicTrackArtist)
        }
        return writableDatabase.insertOrThrow(TABLE_SESSIONS, null, values)
    }

    fun insertSetLogs(
        sessionId: Long,
        exerciseNames: Map<String, String>,
        exerciseEquipment: Map<String, String>,
        drafts: List<WorkoutSetDraft>
    ) {
        if (sessionId <= 0L || drafts.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            drafts.forEach { draft ->
                val values = ContentValues().apply {
                    put(COL_SET_SESSION_ID, sessionId)
                    put(COL_SET_EXERCISE_ID, draft.exerciseId)
                    put(COL_SET_EXERCISE_NAME, exerciseNames[draft.exerciseId].orEmpty())
                    put(COL_SET_NUMBER, draft.setNumber)
                    put(COL_SET_REPS, draft.reps)
                    put(COL_SET_LOAD_KG, draft.loadKg)
                    put(COL_SET_COMPLETED, if (draft.completed) 1 else 0)
                    put(COL_SET_EQUIPMENT, exerciseEquipment[draft.exerciseId].orEmpty())
                    if (draft.quality != null) put(COL_SET_QUALITY, draft.quality) else putNull(COL_SET_QUALITY)
                }
                db.insertOrThrow(TABLE_SET_LOGS, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertSessionMetrics(sessionId: Long, metrics: Map<String, Double>) {
        if (sessionId <= 0L || metrics.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            metrics.forEach { (key, value) ->
                if (key.isBlank() || !value.isFinite()) return@forEach
                val values = ContentValues().apply {
                    put(COL_METRIC_SESSION_ID, sessionId)
                    put(COL_METRIC_KEY, key)
                    put(COL_METRIC_VALUE, value)
                }
                db.insertWithOnConflict(
                    TABLE_SESSION_METRICS,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun metricsForSession(sessionId: Long): Map<String, Double> {
        readableDatabase.query(
            TABLE_SESSION_METRICS,
            METRIC_PROJECTION,
            "$COL_METRIC_SESSION_ID = ?",
            arrayOf(sessionId.toString()),
            null,
            null,
            "$COL_METRIC_KEY ASC"
        ).use { cursor ->
            return buildMap {
                while (cursor.moveToNext()) {
                    put(
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_METRIC_KEY)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_METRIC_VALUE))
                    )
                }
            }
        }
    }

    fun setLogsForSession(sessionId: Long): List<WorkoutSetRecord> {
        readableDatabase.query(
            TABLE_SET_LOGS,
            SET_LOG_PROJECTION,
            "$COL_SET_SESSION_ID = ?",
            arrayOf(sessionId.toString()),
            null,
            null,
            "$COL_SET_EXERCISE_NAME ASC, $COL_SET_NUMBER ASC"
        ).use { cursor ->
            return buildList {
                while (cursor.moveToNext()) {
                    add(
                        WorkoutSetRecord(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SET_ID)),
                            sessionId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SET_SESSION_ID)),
                            exerciseId = cursor.getString(cursor.getColumnIndexOrThrow(COL_SET_EXERCISE_ID)),
                            exerciseName = cursor.getString(cursor.getColumnIndexOrThrow(COL_SET_EXERCISE_NAME)),
                            setNumber = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SET_NUMBER)),
                            reps = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SET_REPS)),
                            loadKg = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SET_LOAD_KG)),
                            completed = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SET_COMPLETED)) == 1,
                            equipment = cursor.getString(cursor.getColumnIndexOrThrow(COL_SET_EQUIPMENT)).orEmpty(),
                            quality = cursor.getColumnIndexOrThrow(COL_SET_QUALITY).let { index -> if (cursor.isNull(index)) null else cursor.getInt(index) }
                        )
                    )
                }
            }
        }
    }

    fun allSessions(): List<WorkoutSession> {
        readableDatabase.query(
            TABLE_SESSIONS,
            SESSION_PROJECTION,
            null,
            null,
            null,
            null,
            "$COL_ENDED_AT DESC, $COL_SESSION_ID DESC"
        ).use { cursor ->
            return buildList {
                while (cursor.moveToNext()) {
                    add(
                        WorkoutSession(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SESSION_ID)),
                            startedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_STARTED_AT)),
                            endedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ENDED_AT)),
                            durationSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DURATION_SECONDS)),
                            totalVolumeKg = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_VOLUME_KG)),
                            completedExercises = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COMPLETED_EXERCISES)),
                            totalExercises = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TOTAL_EXERCISES)),
                            category = categoryFromName(cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY))),
                            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
                            musicTrackUri = cursor.getString(cursor.getColumnIndexOrThrow(COL_MUSIC_URI)),
                            musicTrackTitle = cursor.getString(cursor.getColumnIndexOrThrow(COL_MUSIC_TITLE)),
                            musicTrackArtist = cursor.getString(cursor.getColumnIndexOrThrow(COL_MUSIC_ARTIST))
                        )
                    )
                }
            }
        }
    }

    fun plannedExercises(): List<PlannedExercise> {
        readableDatabase.query(
            TABLE_PLANNED_EXERCISES,
            EXERCISE_PROJECTION,
            null,
            null,
            null,
            null,
            "rowid ASC"
        ).use { cursor ->
            return buildList {
                while (cursor.moveToNext()) {
                    add(
                        PlannedExercise(
                            id = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXERCISE_ID)),
                            name = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXERCISE_NAME)),
                            sets = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SETS)),
                            repsLabel = cursor.getString(cursor.getColumnIndexOrThrow(COL_REPS_LABEL)),
                            targetReps = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TARGET_REPS)),
                            loadKg = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LOAD_KG)),
                            category = categoryFromName(cursor.getString(cursor.getColumnIndexOrThrow(COL_EXERCISE_CATEGORY))),
                            isCustom = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_CUSTOM)) == 1,
                            tag = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXERCISE_TAG)),
                            exerciseType = runCatching { WorkoutExerciseType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_EXERCISE_TYPE))) }.getOrDefault(WorkoutExerciseType.STRENGTH),
                            equipment = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXERCISE_EQUIPMENT))
                        )
                    )
                }
            }
        }
    }

    fun insertPlannedExercise(exercise: PlannedExercise) {
        val values = ContentValues().apply {
            put(COL_EXERCISE_ID, exercise.id)
            put(COL_EXERCISE_NAME, exercise.name)
            put(COL_SETS, exercise.sets)
            put(COL_REPS_LABEL, exercise.repsLabel)
            put(COL_TARGET_REPS, exercise.targetReps)
            put(COL_LOAD_KG, exercise.loadKg)
            put(COL_EXERCISE_CATEGORY, exercise.category.name)
            put(COL_IS_CUSTOM, if (exercise.isCustom) 1 else 0)
            put(COL_EXERCISE_TAG, exercise.tag)
            put(COL_EXERCISE_TYPE, exercise.exerciseType.name)
            put(COL_EXERCISE_EQUIPMENT, exercise.equipment)
        }
        writableDatabase.insertOrThrow(TABLE_PLANNED_EXERCISES, null, values)
    }


    fun deleteCustomPlannedExercise(exerciseId: String): Boolean {
        return writableDatabase.delete(
            TABLE_PLANNED_EXERCISES,
            "$COL_EXERCISE_ID = ? AND $COL_IS_CUSTOM = 1",
            arrayOf(exerciseId)
        ) > 0
    }

    fun replacePlannedExercisesForCategory(
        category: WorkoutCategory,
        exercises: List<PlannedExercise>
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(
                TABLE_PLANNED_EXERCISES,
                "$COL_EXERCISE_CATEGORY = ?",
                arrayOf(category.name)
            )
            exercises.forEach { exercise ->
                val values = ContentValues().apply {
                    put(COL_EXERCISE_ID, exercise.id)
                    put(COL_EXERCISE_NAME, exercise.name)
                    put(COL_SETS, exercise.sets)
                    put(COL_REPS_LABEL, exercise.repsLabel)
                    put(COL_TARGET_REPS, exercise.targetReps)
                    put(COL_LOAD_KG, exercise.loadKg)
                    put(COL_EXERCISE_CATEGORY, category.name)
                    put(COL_IS_CUSTOM, if (exercise.isCustom) 1 else 0)
                    put(COL_EXERCISE_TAG, exercise.tag)
                    put(COL_EXERCISE_TYPE, exercise.exerciseType.name)
                    put(COL_EXERCISE_EQUIPMENT, exercise.equipment)
                }
                db.insertOrThrow(TABLE_PLANNED_EXERCISES, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun allLayouts(): List<WorkoutLayout> {
        readableDatabase.query(
            TABLE_LAYOUTS,
            LAYOUT_PROJECTION,
            null,
            null,
            null,
            null,
            "$COL_LAYOUT_CREATED_AT DESC, $COL_LAYOUT_ID DESC"
        ).use { cursor ->
            return buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LAYOUT_ID))
                    add(
                        WorkoutLayout(
                            id = id,
                            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_LAYOUT_TITLE)),
                            category = categoryFromName(cursor.getString(cursor.getColumnIndexOrThrow(COL_LAYOUT_CATEGORY))),
                            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LAYOUT_CREATED_AT)),
                            exercises = layoutExercises(id)
                        )
                    )
                }
            }
        }
    }

    fun insertLayout(
        title: String,
        category: WorkoutCategory,
        exercises: List<WorkoutLayoutExercise>
    ): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val layoutId = db.insertOrThrow(
                TABLE_LAYOUTS,
                null,
                ContentValues().apply {
                    put(COL_LAYOUT_TITLE, title)
                    put(COL_LAYOUT_CATEGORY, category.name)
                    put(COL_LAYOUT_CREATED_AT, System.currentTimeMillis())
                }
            )
            insertLayoutExercises(db, layoutId, exercises)
            db.setTransactionSuccessful()
            return layoutId
        } finally {
            db.endTransaction()
        }
    }

    fun updateLayout(
        layoutId: Long,
        title: String,
        category: WorkoutCategory,
        exercises: List<WorkoutLayoutExercise>
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.update(
                TABLE_LAYOUTS,
                ContentValues().apply {
                    put(COL_LAYOUT_TITLE, title)
                    put(COL_LAYOUT_CATEGORY, category.name)
                },
                "$COL_LAYOUT_ID = ?",
                arrayOf(layoutId.toString())
            )
            db.delete(
                TABLE_LAYOUT_EXERCISES,
                "$COL_LAYOUT_EXERCISE_LAYOUT_ID = ?",
                arrayOf(layoutId.toString())
            )
            insertLayoutExercises(db, layoutId, exercises)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }


    fun clearAllWorkoutData() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_SESSION_METRICS, null, null)
            db.delete(TABLE_SET_LOGS, null, null)
            db.delete(TABLE_SESSIONS, null, null)
            db.delete(TABLE_LAYOUT_EXERCISES, null, null)
            db.delete(TABLE_LAYOUTS, null, null)
            db.delete(TABLE_PLANNED_EXERCISES, null, null)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun deleteSession(sessionId: Long) {
        if (sessionId <= 0L) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Explicit deletes keep this safe even on databases created before
            // foreign-key cascading was enabled.
            db.delete(TABLE_SESSION_METRICS, "$COL_METRIC_SESSION_ID = ?", arrayOf(sessionId.toString()))
            db.delete(TABLE_SET_LOGS, "$COL_SET_SESSION_ID = ?", arrayOf(sessionId.toString()))
            db.delete(TABLE_SESSIONS, "$COL_SESSION_ID = ?", arrayOf(sessionId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteLayout(layoutId: Long) {
        writableDatabase.delete(
            TABLE_LAYOUTS,
            "$COL_LAYOUT_ID = ?",
            arrayOf(layoutId.toString())
        )
    }

    private fun layoutExercises(layoutId: Long): List<WorkoutLayoutExercise> {
        readableDatabase.query(
            TABLE_LAYOUT_EXERCISES,
            LAYOUT_EXERCISE_PROJECTION,
            "$COL_LAYOUT_EXERCISE_LAYOUT_ID = ?",
            arrayOf(layoutId.toString()),
            null,
            null,
            "$COL_LAYOUT_EXERCISE_POSITION ASC, $COL_LAYOUT_EXERCISE_ID ASC"
        ).use { cursor ->
            return buildList {
                while (cursor.moveToNext()) {
                    add(
                        WorkoutLayoutExercise(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LAYOUT_EXERCISE_ID)),
                            layoutId = layoutId,
                            position = cursor.getInt(cursor.getColumnIndexOrThrow(COL_LAYOUT_EXERCISE_POSITION)),
                            name = cursor.getString(cursor.getColumnIndexOrThrow(COL_LAYOUT_EXERCISE_NAME)),
                            sets = cursor.getInt(cursor.getColumnIndexOrThrow(COL_LAYOUT_EXERCISE_SETS)),
                            targetReps = cursor.getInt(cursor.getColumnIndexOrThrow(COL_LAYOUT_EXERCISE_REPS)),
                            loadKg = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LAYOUT_EXERCISE_LOAD_KG)),
                            exerciseType = runCatching {
                                WorkoutExerciseType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_LAYOUT_EXERCISE_TYPE)))
                            }.getOrDefault(WorkoutExerciseType.STRENGTH),
                            cardioLevel = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LAYOUT_EXERCISE_CARDIO_LEVEL)),
                            equipment = cursor.getString(cursor.getColumnIndexOrThrow(COL_LAYOUT_EXERCISE_EQUIPMENT))
                        )
                    )
                }
            }
        }
    }

    private fun insertLayoutExercises(
        db: SQLiteDatabase,
        layoutId: Long,
        exercises: List<WorkoutLayoutExercise>
    ) {
        exercises.forEachIndexed { index, exercise ->
            db.insertOrThrow(
                TABLE_LAYOUT_EXERCISES,
                null,
                ContentValues().apply {
                    put(COL_LAYOUT_EXERCISE_LAYOUT_ID, layoutId)
                    put(COL_LAYOUT_EXERCISE_POSITION, index)
                    put(COL_LAYOUT_EXERCISE_NAME, exercise.name)
                    put(COL_LAYOUT_EXERCISE_SETS, exercise.sets)
                    put(COL_LAYOUT_EXERCISE_REPS, exercise.targetReps)
                    put(COL_LAYOUT_EXERCISE_LOAD_KG, exercise.loadKg)
                    put(COL_LAYOUT_EXERCISE_TYPE, exercise.exerciseType.name)
                    put(COL_LAYOUT_EXERCISE_CARDIO_LEVEL, exercise.cardioLevel)
                    put(COL_LAYOUT_EXERCISE_EQUIPMENT, exercise.equipment)
                }
            )
        }
    }

    private fun insertAdvancedDefaultsIfEmpty(db: SQLiteDatabase) {
        val defaults = listOf(
            PlannedExercise(
                id = "cardio-easy-run",
                name = "Easy Run",
                sets = 1,
                repsLabel = "30 min",
                targetReps = 30,
                loadKg = 0.0,
                category = WorkoutCategory.CARDIO
            ),
            PlannedExercise(
                id = "hiit-burpees",
                name = "Burpees",
                sets = 5,
                repsLabel = "10 reps",
                targetReps = 10,
                loadKg = 0.0,
                category = WorkoutCategory.HIIT
            ),
            PlannedExercise(
                id = "hiit-mountain-climbers",
                name = "Mountain Climbers",
                sets = 4,
                repsLabel = "30 reps",
                targetReps = 30,
                loadKg = 0.0,
                category = WorkoutCategory.HIIT
            ),
            PlannedExercise(
                id = "mobility-hip-flexor",
                name = "Hip Flexor Stretch",
                sets = 2,
                repsLabel = "45 sec",
                targetReps = 45,
                loadKg = 0.0,
                category = WorkoutCategory.MOBILITY
            ),
            PlannedExercise(
                id = "mobility-thoracic-rotation",
                name = "Thoracic Rotation",
                sets = 2,
                repsLabel = "30 sec",
                targetReps = 30,
                loadKg = 0.0,
                category = WorkoutCategory.MOBILITY
            )
        )
        defaults.groupBy { it.category }.forEach { (category, categoryDefaults) ->
            val existingCount = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_PLANNED_EXERCISES WHERE $COL_EXERCISE_CATEGORY = ?",
                arrayOf(category.name)
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
            if (existingCount == 0) {
                categoryDefaults.forEach { exercise ->
                    db.insert(
                        TABLE_PLANNED_EXERCISES,
                        null,
                        ContentValues().apply {
                            put(COL_EXERCISE_ID, exercise.id)
                            put(COL_EXERCISE_NAME, exercise.name)
                            put(COL_SETS, exercise.sets)
                            put(COL_REPS_LABEL, exercise.repsLabel)
                            put(COL_TARGET_REPS, exercise.targetReps)
                            put(COL_LOAD_KG, exercise.loadKg)
                            put(COL_EXERCISE_CATEGORY, exercise.category.name)
                            put(COL_IS_CUSTOM, 0)
                            put(COL_EXERCISE_TAG, exercise.tag)
                            put(COL_EXERCISE_TYPE, exercise.exerciseType.name)
                            put(COL_EXERCISE_EQUIPMENT, exercise.equipment)
                        }
                    )
                }
            }
        }
    }

    private fun insertDefaultExercises(db: SQLiteDatabase) {
        val defaults = defaultStrengthExercises()
        defaults.forEach { exercise ->
            db.insert(
                TABLE_PLANNED_EXERCISES,
                null,
                ContentValues().apply {
                    put(COL_EXERCISE_ID, exercise.id)
                    put(COL_EXERCISE_NAME, exercise.name)
                    put(COL_SETS, exercise.sets)
                    put(COL_REPS_LABEL, exercise.repsLabel)
                    put(COL_TARGET_REPS, exercise.targetReps)
                    put(COL_LOAD_KG, exercise.loadKg)
                    put(COL_EXERCISE_CATEGORY, exercise.category.name)
                    put(COL_IS_CUSTOM, 0)
                }
            )
        }
    }

    private fun removeUnchangedStarterLayout(db: SQLiteDatabase) {
        val isStarter = db.rawQuery(
            "SELECT $COL_LAYOUT_ID FROM $TABLE_LAYOUTS WHERE $COL_LAYOUT_ID = 1 AND $COL_LAYOUT_TITLE = ? AND $COL_LAYOUT_CATEGORY = ?",
            arrayOf("Upper Body Hypertrophy", WorkoutCategory.STRENGTH.name)
        ).use { it.moveToFirst() }
        if (!isStarter) return
        val expected = defaultStrengthExercises()
        val unchanged = db.query(
            TABLE_LAYOUT_EXERCISES, null, "$COL_LAYOUT_EXERCISE_LAYOUT_ID = ?", arrayOf("1"),
            null, null, "$COL_LAYOUT_EXERCISE_POSITION ASC"
        ).use { cursor ->
            if (cursor.count != expected.size) return@use false
            expected.all { exercise ->
                cursor.moveToNext() &&
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_LAYOUT_EXERCISE_NAME)) == exercise.name &&
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_LAYOUT_EXERCISE_SETS)) == exercise.sets &&
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_LAYOUT_EXERCISE_REPS)) == exercise.targetReps &&
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LAYOUT_EXERCISE_LOAD_KG)) == exercise.loadKg &&
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_LAYOUT_EXERCISE_TYPE)) == "STRENGTH" &&
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_LAYOUT_EXERCISE_EQUIPMENT)).isEmpty()
            }
        }
        if (unchanged) db.delete(TABLE_LAYOUTS, "$COL_LAYOUT_ID = ?", arrayOf("1"))
    }
    private fun defaultStrengthExercises(): List<PlannedExercise> = listOf(
        PlannedExercise(
            id = "strength-incline-db-press",
            name = "Incline DB Bench Press",
            sets = 3,
            repsLabel = "10–12 reps",
            targetReps = 11,
            loadKg = 28.0,
            category = WorkoutCategory.STRENGTH
        ),
        PlannedExercise(
            id = "strength-bent-over-row",
            name = "Barbell Bent-Over Row",
            sets = 4,
            repsLabel = "8 reps",
            targetReps = 8,
            loadKg = 65.0,
            category = WorkoutCategory.STRENGTH
        ),
        PlannedExercise(
            id = "strength-overhead-cable-extension",
            name = "Overhead Cable Extension",
            sets = 3,
            repsLabel = "12 reps",
            targetReps = 12,
            loadKg = 32.0,
            category = WorkoutCategory.STRENGTH
        )
    )

    private fun categoryFromName(value: String): WorkoutCategory {
        return runCatching { WorkoutCategory.valueOf(value) }
            .getOrDefault(WorkoutCategory.STRENGTH)
    }

    private companion object {
        const val DATABASE_NAME = "gymbros_workouts.db"
        const val DATABASE_VERSION = 11

        const val TABLE_SESSIONS = "workout_sessions"
        const val COL_SESSION_ID = "id"
        const val COL_STARTED_AT = "started_at"
        const val COL_ENDED_AT = "ended_at"
        const val COL_DURATION_SECONDS = "duration_seconds"
        const val COL_VOLUME_KG = "volume_kg"
        const val COL_COMPLETED_EXERCISES = "completed_exercises"
        const val COL_TOTAL_EXERCISES = "total_exercises"
        const val COL_CATEGORY = "category"
        const val COL_TITLE = "title"
        const val COL_MUSIC_URI = "music_track_uri"
        const val COL_MUSIC_TITLE = "music_track_title"
        const val COL_MUSIC_ARTIST = "music_track_artist"

        const val TABLE_SESSION_METRICS = "workout_session_metrics"
        const val COL_METRIC_SESSION_ID = "session_id"
        const val COL_METRIC_KEY = "metric_key"
        const val COL_METRIC_VALUE = "metric_value"

        const val TABLE_PLANNED_EXERCISES = "planned_exercises"
        const val COL_EXERCISE_ID = "id"
        const val COL_EXERCISE_NAME = "name"
        const val COL_SETS = "sets_count"
        const val COL_REPS_LABEL = "reps_label"
        const val COL_TARGET_REPS = "target_reps"
        const val COL_LOAD_KG = "load_kg"
        const val COL_EXERCISE_CATEGORY = "category"
        const val COL_IS_CUSTOM = "is_custom"
        const val COL_EXERCISE_TAG = "exercise_tag"
        const val COL_EXERCISE_TYPE = "exercise_type"
        const val COL_EXERCISE_EQUIPMENT = "equipment"

        const val TABLE_SET_LOGS = "workout_set_logs"
        const val COL_SET_ID = "id"
        const val COL_SET_SESSION_ID = "session_id"
        const val COL_SET_EXERCISE_ID = "exercise_id"
        const val COL_SET_EXERCISE_NAME = "exercise_name"
        const val COL_SET_NUMBER = "set_number"
        const val COL_SET_REPS = "reps"
        const val COL_SET_LOAD_KG = "load_kg"
        const val COL_SET_COMPLETED = "completed"
        const val COL_SET_EQUIPMENT = "equipment"
        const val COL_SET_QUALITY = "quality"

        const val TABLE_LAYOUTS = "workout_layouts"
        const val COL_LAYOUT_ID = "id"
        const val COL_LAYOUT_TITLE = "title"
        const val COL_LAYOUT_CATEGORY = "category"
        const val COL_LAYOUT_CREATED_AT = "created_at"

        const val TABLE_LAYOUT_EXERCISES = "workout_layout_exercises"
        const val COL_LAYOUT_EXERCISE_ID = "id"
        const val COL_LAYOUT_EXERCISE_LAYOUT_ID = "layout_id"
        const val COL_LAYOUT_EXERCISE_POSITION = "position_index"
        const val COL_LAYOUT_EXERCISE_NAME = "name"
        const val COL_LAYOUT_EXERCISE_SETS = "sets_count"
        const val COL_LAYOUT_EXERCISE_REPS = "target_reps"
        const val COL_LAYOUT_EXERCISE_LOAD_KG = "load_kg"
        const val COL_LAYOUT_EXERCISE_TYPE = "exercise_type"
        const val COL_LAYOUT_EXERCISE_CARDIO_LEVEL = "cardio_level"
        const val COL_LAYOUT_EXERCISE_EQUIPMENT = "equipment"

        val SESSION_PROJECTION = arrayOf(
            COL_SESSION_ID,
            COL_STARTED_AT,
            COL_ENDED_AT,
            COL_DURATION_SECONDS,
            COL_VOLUME_KG,
            COL_COMPLETED_EXERCISES,
            COL_TOTAL_EXERCISES,
            COL_CATEGORY,
            COL_TITLE,
            COL_MUSIC_URI,
            COL_MUSIC_TITLE,
            COL_MUSIC_ARTIST
        )


        val METRIC_PROJECTION = arrayOf(
            COL_METRIC_SESSION_ID,
            COL_METRIC_KEY,
            COL_METRIC_VALUE
        )

        val EXERCISE_PROJECTION = arrayOf(
            COL_EXERCISE_ID,
            COL_EXERCISE_NAME,
            COL_SETS,
            COL_REPS_LABEL,
            COL_TARGET_REPS,
            COL_LOAD_KG,
            COL_EXERCISE_CATEGORY,
            COL_IS_CUSTOM,
            COL_EXERCISE_TAG,
            COL_EXERCISE_TYPE,
            COL_EXERCISE_EQUIPMENT
        )

        val SET_LOG_PROJECTION = arrayOf(
            COL_SET_ID,
            COL_SET_SESSION_ID,
            COL_SET_EXERCISE_ID,
            COL_SET_EXERCISE_NAME,
            COL_SET_NUMBER,
            COL_SET_REPS,
            COL_SET_LOAD_KG,
            COL_SET_COMPLETED,
            COL_SET_EQUIPMENT,
            COL_SET_QUALITY
        )

        val LAYOUT_PROJECTION = arrayOf(
            COL_LAYOUT_ID,
            COL_LAYOUT_TITLE,
            COL_LAYOUT_CATEGORY,
            COL_LAYOUT_CREATED_AT
        )

        val LAYOUT_EXERCISE_PROJECTION = arrayOf(
            COL_LAYOUT_EXERCISE_ID,
            COL_LAYOUT_EXERCISE_LAYOUT_ID,
            COL_LAYOUT_EXERCISE_POSITION,
            COL_LAYOUT_EXERCISE_NAME,
            COL_LAYOUT_EXERCISE_SETS,
            COL_LAYOUT_EXERCISE_REPS,
            COL_LAYOUT_EXERCISE_LOAD_KG,
            COL_LAYOUT_EXERCISE_TYPE,
            COL_LAYOUT_EXERCISE_CARDIO_LEVEL,
            COL_LAYOUT_EXERCISE_EQUIPMENT
        )
    }
}
