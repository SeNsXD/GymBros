package com.rishabh.gymbros.features.workout.domain

import android.content.Context
import com.rishabh.gymbros.features.workout.model.ExerciseLibraryItem
import com.rishabh.gymbros.features.workout.model.WorkoutCategory
import com.rishabh.gymbros.features.workout.model.WorkoutExerciseType
import org.json.JSONObject

/** Loads the bundled RepDB exercise snapshot from app assets. */
object ExerciseLibraryCatalog {
    const val count: Int = 601
    @Volatile private var cached: List<ExerciseLibraryItem>? = null

    fun items(context: Context): List<ExerciseLibraryItem> = cached ?: synchronized(this) {
        cached ?: load(context.applicationContext).also { cached = it }
    }

    private fun load(context: Context): List<ExerciseLibraryItem> {
        val raw = context.assets.open("repdb/exercises.json").bufferedReader().use { it.readText() }
        val array = JSONObject(raw).getJSONArray("exercises")
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                val sourceCategory = obj.optString("category")
                val category = when (sourceCategory) {
                    "cardio" -> WorkoutCategory.CARDIO
                    "stretching" -> WorkoutCategory.MOBILITY
                    "plyometrics" -> WorkoutCategory.HIIT
                    else -> WorkoutCategory.STRENGTH
                }
                val bodyPart = pretty(obj.optString("body_part", "full_body"))
                val equipment = pretty(if (obj.isNull("equipment")) "bodyweight" else obj.optString("equipment").ifBlank { "bodyweight" })
                val images = obj.optJSONObject("images")?.optJSONObject("flat")
                val imagePath = images?.optString("start")?.takeIf { it.isNotBlank() }
                    ?: images?.optString("main")?.takeIf { it.isNotBlank() }
                    ?: images?.optString("peak")?.takeIf { it.isNotBlank() }
                val instructionsArray = obj.optJSONArray("instructions_en")
                val instructions = buildList {
                    if (instructionsArray != null) for (i in 0 until instructionsArray.length()) add(instructionsArray.optString(i))
                }.filter { it.isNotBlank() }
                val primary = obj.optJSONArray("primary_muscles")?.let { a ->
                    buildList { for (i in 0 until a.length()) add(pretty(a.optString(i))) }
                }.orEmpty()
                val exerciseType = when {
                    category == WorkoutCategory.CARDIO -> WorkoutExerciseType.CARDIO_DISTANCE
                    obj.optString("force_type").equals("static", ignoreCase = true) -> WorkoutExerciseType.TIME_ONLY
                    else -> WorkoutExerciseType.STRENGTH
                }
                add(
                    ExerciseLibraryItem(
                        id = obj.getString("id"),
                        name = obj.optString("name_en", obj.getString("id")),
                        category = category,
                        muscleGroup = bodyPart,
                        defaultSets = if (category == WorkoutCategory.CARDIO) 1 else 3,
                        defaultReps = when (exerciseType) {
                            WorkoutExerciseType.CARDIO_DISTANCE -> 0
                            WorkoutExerciseType.TIME_ONLY -> 60
                            else -> 12
                        },
                        defaultLoadKg = 0.0,
                        description = obj.optString("description_en"),
                        equipment = equipment,
                        difficulty = pretty(obj.optString("difficulty")),
                        primaryMuscles = primary,
                        instructions = instructions,
                        met = obj.optDouble("met", 0.0),
                        imageAssetPath = imagePath?.let { "repdb/$it" },
                        exerciseType = exerciseType
                    )
                )
            }
        }.sortedBy { it.name.lowercase() }
    }

    private fun pretty(value: String): String = value
        .replace('_', ' ')
        .trim()
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { c -> c.uppercase() } }
}
