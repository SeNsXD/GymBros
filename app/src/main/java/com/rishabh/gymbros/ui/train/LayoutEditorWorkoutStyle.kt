package com.rishabh.gymbros.ui.train

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishabh.gymbros.features.workout.data.WorkoutRepository
import com.rishabh.gymbros.features.workout.model.ActiveWorkoutExercise
import com.rishabh.gymbros.features.workout.model.WorkoutExerciseType
import com.rishabh.gymbros.features.workout.model.WorkoutSession
import com.rishabh.gymbros.features.workout.model.WorkoutSetRecord

// The new / edit layout screen is built from the same pieces as the active workout screen
// (exercise card, SET / PREVIOUS / KG / REPS grid, number fields, add / remove set buttons,
// cardio fields), so both screens look and behave alike. The only differences: there are no
// "complete set" ticks, because a layout is a plan rather than a log.

private val LayoutSectionCardShape = RoundedCornerShape(18.dp)
private val LayoutSectionFieldShape = RoundedCornerShape(12.dp)

internal data class LayoutExerciseDraft(
    val name: String,
    val sets: String,
    val reps: String,
    val load: String,
    val exerciseType: WorkoutExerciseType = WorkoutExerciseType.STRENGTH,
    val cardioLevel: String = "",
    val equipment: String = "",
    val sourceExerciseId: String? = null
)

/** Empty text for zero, like the active workout's fields, so an unset target shows a blank box. */
internal fun layoutDecimalText(value: Double): String = if (value > 0.0) formatDecimal(value) else ""

internal fun layoutIntText(value: Int): String = if (value > 0) value.toString() else ""

/** Same header layout as the workout screen: small orange label, big title, subtitle, pills on the right. */
@Composable
internal fun LayoutEditorHeader(
    isNew: Boolean,
    name: String,
    exerciseCount: Int,
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val palette = LocalWorkoutUiPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (isNew) "NEW LAYOUT" else "EDIT LAYOUT",
                color = palette.Orange,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp
            )
            Text(
                name.ifBlank { "Untitled layout" },
                color = palette.TextStrong,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Template • $exerciseCount exercise${if (exerciseCount == 1) "" else "s"}",
                color = palette.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallAction("Cancel", onCancel, danger = true)
            if (canSave) SmallAction("Save", onSave)
        }
    }
}

/** One exercise: the workout screen's exercise card, followed by its set rows and +/- set buttons. */
@Composable
internal fun LayoutExerciseSection(
    draft: LayoutExerciseDraft,
    index: Int,
    sessions: List<WorkoutSession>,
    setLogs: Map<Long, List<WorkoutSetRecord>>,
    onChange: (LayoutExerciseDraft) -> Unit,
    onEdit: () -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
    onLongPress: () -> Unit
) {
    val setCount = (draft.sets.toIntOrNull() ?: 1).coerceIn(1, 99)
    val exercise = ActiveWorkoutExercise(
        id = "layout-$index",
        name = draft.name,
        exerciseType = draft.exerciseType,
        sets = setCount,
        targetReps = draft.reps.toIntOrNull() ?: 0,
        loadKg = draft.load.toDoubleOrNull() ?: 0.0,
        cardioLevel = draft.cardioLevel.toDoubleOrNull() ?: 0.0,
        equipment = draft.equipment,
        sourceExerciseId = draft.sourceExerciseId
    )
    val addSet = { onChange(draft.copy(sets = (setCount + 1).coerceAtMost(99).toString())) }
    val removeSet = { onChange(draft.copy(sets = (setCount - 1).coerceAtLeast(1).toString())) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AddedExerciseCard(
            exercise = exercise,
            completed = false,
            onEdit = onEdit,
            onReplace = onReplace,
            onRemove = onDelete,
            onLongPress = onLongPress
        )
        when (draft.exerciseType) {
            WorkoutExerciseType.STRENGTH -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LayoutStrengthColumnHeader()
                    repeat(setCount) { i ->
                        val setNumber = i + 1
                        LayoutStrengthSetRow(
                            setNumber = setNumber,
                            previous = previousSetFor(draft.name, draft.equipment, setNumber, sessions, setLogs),
                            kg = draft.load,
                            reps = draft.reps,
                            // A layout stores one KG and one REPS target for all of an exercise's
                            // sets, so editing any row updates every row.
                            onKgChange = { raw -> onChange(draft.copy(load = boundedDecimal(raw, WorkoutRepository.MAX_KG))) },
                            onRepsChange = { raw -> onChange(draft.copy(reps = boundedInt(raw, WorkoutRepository.MAX_REPS))) }
                        )
                    }
                }
                SetControls(setCount, addSet, removeSet)
            }
            WorkoutExerciseType.CARDIO_DISTANCE -> LayoutCardioCard(draft = draft, onChange = onChange)
            WorkoutExerciseType.CARDIO_ROPE, WorkoutExerciseType.TIME_ONLY -> {
                repeat(setCount) { i ->
                    val setNumber = i + 1
                    LayoutTimeSetRow(
                        setNumber = setNumber,
                        previous = previousSetFor(draft.name, "", setNumber, sessions, setLogs),
                        seconds = draft.reps,
                        onSecondsChange = { raw -> onChange(draft.copy(reps = boundedInt(raw, 3600))) }
                    )
                }
                SetControls(setCount, addSet, removeSet)
            }
        }
    }
}

@Composable
private fun LayoutStrengthColumnHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
        LayoutLabelCell("SET", .12f)
        LayoutLabelCell("PREVIOUS", .30f)
        LayoutLabelCell("KG", .29f)
        LayoutLabelCell("REPS", .29f)
    }
}

@Composable
private fun RowScope.LayoutLabelCell(text: String, weight: Float) {
    Text(
        text,
        color = LocalWorkoutUiPalette.current.Muted,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(weight),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun LayoutStrengthSetRow(
    setNumber: Int,
    previous: WorkoutSetRecord?,
    kg: String,
    reps: String,
    onKgChange: (String) -> Unit,
    onRepsChange: (String) -> Unit
) {
    val palette = LocalWorkoutUiPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LayoutSectionFieldShape)
            .background(palette.Card)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(.12f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).border(2.dp, palette.Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    setNumber.toString(),
                    color = palette.TextStrong,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        Text(
            previous?.let { "${formatDecimal(it.loadKg)}kg × ${it.reps}" } ?: "—",
            color = palette.TextSecondary,
            modifier = Modifier.weight(.30f),
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
        MiniNumberField(kg, onKgChange, Modifier.weight(.29f), KeyboardType.Decimal)
        MiniNumberField(reps, onRepsChange, Modifier.weight(.29f), KeyboardType.Number)
    }
}

@Composable
private fun LayoutTimeSetRow(
    setNumber: Int,
    previous: WorkoutSetRecord?,
    seconds: String,
    onSecondsChange: (String) -> Unit
) {
    val palette = LocalWorkoutUiPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LayoutSectionFieldShape)
            .background(palette.Card)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Set $setNumber", color = palette.TextStrong, fontWeight = FontWeight.Bold)
            Text(
                "Previous ${previous?.reps?.let { "${it}s" } ?: "—"}",
                color = palette.Dimmed,
                style = MaterialTheme.typography.bodySmall
            )
        }
        OutlinedTextField(
            value = seconds,
            onValueChange = onSecondsChange,
            modifier = Modifier.width(110.dp),
            label = { Text("Seconds") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = LayoutSectionFieldShape
        )
    }
}

@Composable
private fun LayoutCardioCard(
    draft: LayoutExerciseDraft,
    onChange: (LayoutExerciseDraft) -> Unit
) {
    val palette = LocalWorkoutUiPalette.current
    val initial = splitDuration(draft.reps.toIntOrNull() ?: 0)
    var hours by remember(draft.name, draft.exerciseType) { mutableStateOf(initial.first) }
    var minutes by remember(draft.name, draft.exerciseType) { mutableStateOf(initial.second) }
    var seconds by remember(draft.name, draft.exerciseType) { mutableStateOf(initial.third) }
    var km by remember(draft.name, draft.exerciseType) { mutableStateOf(draft.load) }
    var level by remember(draft.name, draft.exerciseType) { mutableStateOf(draft.cardioLevel) }

    fun push() {
        val total = durationFromParts(hours, minutes, seconds)
        onChange(
            draft.copy(
                sets = "1",
                reps = if (total > 0) total.toString() else "",
                load = km,
                cardioLevel = level
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LayoutSectionCardShape)
            .background(palette.Card)
            .border(1.dp, palette.Border, LayoutSectionCardShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Time",
                modifier = Modifier.width(36.dp),
                color = palette.Muted,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
            CompactCardioField(
                value = hours,
                onValueChange = { hours = boundedInt(it, 99); push() },
                placeholder = "HR",
                suffix = "hr",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            CompactCardioField(
                value = minutes,
                onValueChange = { minutes = boundedInt(it, 59); push() },
                placeholder = "MIN",
                suffix = "min",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            CompactCardioField(
                value = seconds,
                onValueChange = { seconds = boundedInt(it, 59); push() },
                placeholder = "SEC",
                suffix = "sec",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactCardioField(
                value = km,
                onValueChange = { km = boundedDecimal(it, WorkoutRepository.MAX_CARDIO_DISTANCE_KM); push() },
                placeholder = "0",
                suffix = "km",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
                label = "Distance"
            )
            CompactCardioField(
                value = level,
                onValueChange = { level = boundedDecimal(it, WorkoutRepository.MAX_CARDIO_LEVEL); push() },
                placeholder = "0",
                suffix = "",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
                label = "Level"
            )
        }
    }
}
