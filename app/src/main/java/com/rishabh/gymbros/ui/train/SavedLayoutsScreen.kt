package com.rishabh.gymbros.ui.train

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.ui.platform.LocalContext
import com.rishabh.gymbros.features.workout.domain.ExerciseLibraryCatalog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rishabh.gymbros.features.workout.data.WorkoutRepository
import com.rishabh.gymbros.features.workout.model.PlannedExercise
import com.rishabh.gymbros.features.workout.model.WorkoutCategory
import com.rishabh.gymbros.features.workout.model.WorkoutLayout
import com.rishabh.gymbros.features.workout.model.WorkoutExerciseType
import com.rishabh.gymbros.features.workout.model.WorkoutLayoutExercise
import com.rishabh.gymbros.features.workout.model.WorkoutSession
import com.rishabh.gymbros.features.workout.model.WorkoutSetRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LayoutCardShape = RoundedCornerShape(18.dp)
private val LayoutInnerShape = RoundedCornerShape(12.dp)

@Composable
internal fun SavedLayoutsScreen(
    layouts: List<WorkoutLayout>,
    plannedExercises: List<PlannedExercise>,
    selectedCategory: WorkoutCategory,
    onBack: () -> Unit,
    onApply: (Long) -> Unit,
    onDuplicate: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onCreate: (String, WorkoutCategory, List<WorkoutLayoutExercise>) -> Unit,
    onUpdate: (Long, String, WorkoutCategory, List<WorkoutLayoutExercise>) -> Unit,
    addExerciseRequestKey: Int = 0,
    onAddExerciseRequestConsumed: () -> Unit = {},
    onEditorVisibilityChanged: (Boolean) -> Unit = {},
    sessions: List<WorkoutSession> = emptyList(),
    setLogs: Map<Long, List<WorkoutSetRecord>> = emptyMap(),
    modifier: Modifier = Modifier
) {
    var editorRequest by remember { mutableStateOf<LayoutEditorRequest?>(null) }
    var deleteCandidate by remember { mutableStateOf<WorkoutLayout?>(null) }
    androidx.compose.runtime.LaunchedEffect(editorRequest != null) { onEditorVisibilityChanged(editorRequest != null) }

    editorRequest?.let { request ->
        BackHandler { editorRequest = null }
        LayoutEditorScreen(
            request = request,
            plannedExercises = plannedExercises,
            sessions = sessions,
            setLogs = setLogs,
            addExerciseRequestKey = addExerciseRequestKey,
            onAddExerciseRequestConsumed = onAddExerciseRequestConsumed,
            onBack = { editorRequest = null },
            onSave = { title, category, exercises ->
                val existing = request.layout
                if (existing == null) {
                    onCreate(title, category, exercises)
                } else {
                    onUpdate(existing.id, title, category, exercises)
                }
                editorRequest = null
            },
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LocalWorkoutUiPalette.current.Canvas),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LayoutsHeader(
                onBack = onBack,
                onCreate = {
                    val starter = plannedExercises
                        .filter { it.category == selectedCategory }
                        .mapIndexed { index, exercise ->
                            WorkoutLayoutExercise(
                                position = index,
                                name = exercise.name,
                                sets = exercise.sets,
                                targetReps = exercise.targetReps,
                                loadKg = exercise.loadKg
                            )
                        }
                    editorRequest = LayoutEditorRequest(
                        layout = null,
                        defaultCategory = selectedCategory,
                        starterExercises = starter
                    )
                }
            )
        }

        if (layouts.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LayoutCardShape)
                        .background(LocalWorkoutUiPalette.current.Card)
                        .workoutBorder(1.dp, LocalWorkoutUiPalette.current.Border, LayoutCardShape)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "No saved layouts",
                        color = LocalWorkoutUiPalette.current.TextStrong,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create a reusable routine from your current exercises or build one from scratch.",
                        color = LocalWorkoutUiPalette.current.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            layouts.forEach { layout ->
                item(key = "layout-${layout.id}") {
                    LayoutCard(
                        layout = layout,
                        onApply = { onApply(layout.id) },
                        onEdit = { editorRequest = LayoutEditorRequest(layout, layout.category, layout.exercises) },
                        onDuplicate = { onDuplicate(layout.id) },
                        onDelete = { deleteCandidate = layout }
                    )
                }
            }
        }
    }



    deleteCandidate?.let { layout ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete layout?", fontWeight = FontWeight.Bold) },
            text = { Text("${layout.title} will be removed. Your workout history will not be affected.") },
            confirmButton = {
                com.rishabh.gymbros.ui.components.DialogActionPill(
                    text = "Delete",
                    onClick = { onDelete(layout.id); deleteCandidate = null },
                    destructive = true
                )
            },
            dismissButton = {
                com.rishabh.gymbros.ui.components.DialogActionPill("Cancel", { deleteCandidate = null })
            },
            containerColor = LocalWorkoutUiPalette.current.CardHigh,
            titleContentColor = LocalWorkoutUiPalette.current.TextStrong,
            textContentColor = LocalWorkoutUiPalette.current.TextSecondary
        )
    }
}

private data class LayoutEditorRequest(
    val layout: WorkoutLayout?,
    val defaultCategory: WorkoutCategory,
    val starterExercises: List<WorkoutLayoutExercise>
)

@Composable
private fun LayoutsHeader(
    onBack: () -> Unit,
    onCreate: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LocalWorkoutUiPalette.current.Card)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = LocalWorkoutUiPalette.current.TextStrong
                )
            }
            Column {
                Text(
                    text = "Saved Layouts",
                    color = LocalWorkoutUiPalette.current.TextStrong,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Build once. Start faster.",
                    color = LocalWorkoutUiPalette.current.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(LocalWorkoutUiPalette.current.pick(WorkoutAccents.Purple, LocalWorkoutUiPalette.current.Orange))
                .clickable(onClick = onCreate)
                .padding(horizontal = 13.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = LocalWorkoutUiPalette.current.pick(WorkoutAccents.OnPurple, LocalWorkoutUiPalette.current.OnAccent),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "New",
                    color = LocalWorkoutUiPalette.current.pick(WorkoutAccents.OnPurple, LocalWorkoutUiPalette.current.OnAccent),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun LayoutCard(
    layout: WorkoutLayout,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val created = Instant.ofEpochMilli(layout.createdAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LayoutCardShape)
            .background(LocalWorkoutUiPalette.current.Card)
            .workoutBorder(1.dp, LocalWorkoutUiPalette.current.Border, LayoutCardShape)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(LayoutInnerShape)
                        .background(LocalWorkoutUiPalette.current.pick(WorkoutAccents.PurpleTint, LocalWorkoutUiPalette.current.OrangeTint)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = LocalWorkoutUiPalette.current.pick(WorkoutAccents.Purple, LocalWorkoutUiPalette.current.Orange)
                    )
                }
                Column {
                    Text(
                        text = layout.title,
                        color = LocalWorkoutUiPalette.current.TextStrong,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${layout.exercises.size} exercises • $created",
                        color = LocalWorkoutUiPalette.current.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(LayoutInnerShape)
                .background(LocalWorkoutUiPalette.current.CanvasDeep)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            layout.exercises.take(4).forEach { exercise ->
                Text(
                    text = "• ${layoutExerciseSummary(exercise)}",
                    color = LocalWorkoutUiPalette.current.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (layout.exercises.size > 4) {
                Text(
                    text = "+ ${layout.exercises.size - 4} more",
                    color = LocalWorkoutUiPalette.current.Dimmed,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LayoutUseAction(
                modifier = Modifier.weight(1f),
                onClick = onApply
            )
            LayoutIconAction(Icons.Outlined.Edit, "Edit", com.rishabh.gymbros.ui.theme.Ui2Colors.Cyan, onEdit)
            LayoutIconAction(Icons.Outlined.ContentCopy, "Duplicate", LocalWorkoutUiPalette.current.TextSecondary, onDuplicate)
            LayoutIconAction(Icons.Outlined.DeleteOutline, "Delete", com.rishabh.gymbros.ui.theme.Ui2Colors.Danger, onDelete, danger = true)
        }
    }
}

@Composable
private fun LayoutUseAction(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalWorkoutUiPalette.current
    val accent = palette.pick(WorkoutAccents.Purple, palette.Orange)
    Row(
        modifier = modifier
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.pick(WorkoutAccents.PurpleTint, Color.Transparent))
            .workoutBorder(1.dp, palette.Orange.copy(alpha = .55f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.PlayArrow, null, tint = accent, modifier = Modifier.size(17.dp))
        Text(
            text = "Use layout",
            color = accent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun LayoutIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    val palette = LocalWorkoutUiPalette.current
    val multiBackground = if (danger) WorkoutAccents.DeleteTint else WorkoutAccents.ChipNeutral
    val iconTint = if (palette.MultiColor && danger) WorkoutAccents.DeleteRed else tint
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.pick(multiBackground, palette.CanvasDeep))
            .workoutBorder(1.dp, palette.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = iconTint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun LayoutEditorScreen(
    request: LayoutEditorRequest,
    plannedExercises: List<PlannedExercise>,
    sessions: List<WorkoutSession> = emptyList(),
    setLogs: Map<Long, List<WorkoutSetRecord>> = emptyMap(),
    addExerciseRequestKey: Int = 0,
    onAddExerciseRequestConsumed: () -> Unit = {},
    onBack: () -> Unit,
    onSave: (String, WorkoutCategory, List<WorkoutLayoutExercise>) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by rememberSaveable(request.layout?.id) {
        mutableStateOf(request.layout?.title ?: "")
    }
    var category by remember(request.layout?.id) {
        mutableStateOf(request.layout?.category ?: request.defaultCategory)
    }
    val drafts = remember(request.layout?.id) {
        mutableStateListOf<LayoutExerciseDraft>().apply {
            request.starterExercises.forEach { exercise ->
                add(
                    LayoutExerciseDraft(
                        name = exercise.name,
                        sets = exercise.sets.toString(),
                        reps = layoutIntText(exercise.targetReps),
                        load = layoutDecimalText(exercise.loadKg),
                        exerciseType = exercise.exerciseType,
                        cardioLevel = layoutDecimalText(exercise.cardioLevel),
                        equipment = exercise.equipment,
                        sourceExerciseId = exercise.sourceExerciseId
                    )
                )
            }

        }
    }

    var showExerciseLibrary by rememberSaveable(request.layout?.id) { mutableStateOf(false) }
    var replaceIndex by rememberSaveable(request.layout?.id) { mutableStateOf<Int?>(null) }
    var reorderMode by rememberSaveable(request.layout?.id) { mutableStateOf(false) }
    var editTypeIndex by rememberSaveable(request.layout?.id) { mutableStateOf<Int?>(null) }
    androidx.compose.runtime.LaunchedEffect(addExerciseRequestKey) {
        if (addExerciseRequestKey > 0) {
            replaceIndex = null
            showExerciseLibrary = true
            onAddExerciseRequestConsumed()
        }
    }

    if (showExerciseLibrary) {
        BackHandler { showExerciseLibrary = false; replaceIndex = null }
        ExerciseLibraryScreen(
            plannedExercises = plannedExercises,
            addedExerciseNames = drafts.map { it.name }.filter { it.isNotBlank() }.toSet(),
            pageTitle = if (replaceIndex == null) "Add Exercise" else "Replace Exercise",
            onBack = { showExerciseLibrary = false; replaceIndex = null },
            onAdd = { item ->
                val newDraft = LayoutExerciseDraft(
                    name = item.name,
                    sets = item.defaultSets.coerceAtLeast(1).toString(),
                    reps = layoutIntText(item.defaultReps),
                    load = layoutDecimalText(item.defaultLoadKg),
                    exerciseType = item.exerciseType,
                    cardioLevel = "",
                    equipment = item.equipment,
                    sourceExerciseId = item.id
                )
                val target = replaceIndex
                if (target != null && target in drafts.indices) drafts[target] = newDraft else drafts.add(newDraft)
                showExerciseLibrary = false
                replaceIndex = null
            },
            onCreateCustom = { _, _, _, _ -> },
            onDeleteCustom = { },
            modifier = modifier
        )
        return
    }

    if (reorderMode) {
        BackHandler { reorderMode = false }
        LayoutReorderExerciseMode(
            drafts = drafts.toList(),
            onMove = { from, to ->
                if (from in drafts.indices && to in drafts.indices && from != to) {
                    val moved = drafts.removeAt(from)
                    drafts.add(to, moved)
                }
            },
            onDone = { reorderMode = false },
            modifier = modifier
        )
        return
    }

    val editingIndex = editTypeIndex
    if (editingIndex != null && editingIndex in drafts.indices) {
        val originalDraft = drafts[editingIndex]
        var editName by rememberSaveable(editingIndex, originalDraft.name) { mutableStateOf(originalDraft.name) }
        var editEquipment by rememberSaveable(editingIndex, originalDraft.name) { mutableStateOf(originalDraft.equipment.ifBlank { "Barbell" }) }
        var editExerciseType by rememberSaveable(editingIndex, originalDraft.name) {
            mutableStateOf(if (originalDraft.exerciseType == WorkoutExerciseType.CARDIO_ROPE) WorkoutExerciseType.TIME_ONLY else originalDraft.exerciseType)
        }
        val equipmentOptions = listOf("Barbell", "Dumbbell", "Machine", "Cable")

        AlertDialog(
            onDismissRequest = { editTypeIndex = null },
            title = { Text("Edit exercise", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it.take(60) },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Exercise type", color = LocalWorkoutUiPalette.current.Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { LayoutEditChoiceChip("Strength", editExerciseType == WorkoutExerciseType.STRENGTH, Modifier.fillMaxWidth()) { editExerciseType = WorkoutExerciseType.STRENGTH } }
                        Box(Modifier.weight(1f)) { LayoutEditChoiceChip("Cardio", editExerciseType == WorkoutExerciseType.CARDIO_DISTANCE, Modifier.fillMaxWidth()) { editExerciseType = WorkoutExerciseType.CARDIO_DISTANCE } }
                        Box(Modifier.weight(1f)) { LayoutEditChoiceChip("Time only", editExerciseType == WorkoutExerciseType.TIME_ONLY, Modifier.fillMaxWidth()) { editExerciseType = WorkoutExerciseType.TIME_ONLY } }
                    }

                    if (editExerciseType == WorkoutExerciseType.STRENGTH) {
                        Text("Equipment", color = LocalWorkoutUiPalette.current.Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        equipmentOptions.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { option ->
                                    LayoutEditChoiceChip(option, editEquipment == option, Modifier.weight(1f)) { editEquipment = option }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = editName.trim().isNotBlank(),
                    onClick = {
                        drafts[editingIndex] = originalDraft.copy(
                            name = editName.trim(),
                            equipment = editEquipment,
                            exerciseType = editExerciseType
                        )
                        editTypeIndex = null
                    }
                ) { Text("Save", color = LocalWorkoutUiPalette.current.Orange) }
            },
            dismissButton = { TextButton(onClick = { editTypeIndex = null }) { Text("Cancel", color = LocalWorkoutUiPalette.current.DismissText) } }
        )
    }

    val parsedExercises = drafts.mapIndexedNotNull { index, draft ->
        val sets = draft.sets.toIntOrNull()
        val reps = if (draft.reps.isBlank()) 0 else draft.reps.toIntOrNull()
        val load = draft.load.toDoubleOrNull() ?: 0.0
        val cardioLevel = draft.cardioLevel.toDoubleOrNull() ?: 0.0
        val maxTarget = if (draft.exerciseType == WorkoutExerciseType.STRENGTH) WorkoutRepository.MAX_REPS else WorkoutRepository.MAX_CARDIO_SECONDS
        if (draft.name.isBlank() || sets == null || sets !in 1..99 || reps == null || reps !in 0..maxTarget || load !in 0.0..WorkoutRepository.MAX_KG || cardioLevel < 0.0) {
            null
        } else {
            WorkoutLayoutExercise(
                position = index,
                name = draft.name.trim(),
                sets = if (draft.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE) 1 else sets,
                targetReps = reps,
                loadKg = if (draft.exerciseType == WorkoutExerciseType.CARDIO_ROPE) 0.0 else load,
                exerciseType = draft.exerciseType,
                cardioLevel = cardioLevel,
                equipment = draft.equipment,
                sourceExerciseId = draft.sourceExerciseId
            )
        }
    }
    val canSave = title.isNotBlank() && parsedExercises.size == drafts.size && parsedExercises.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LocalWorkoutUiPalette.current.Canvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
            ) {
                LayoutEditorHeader(
                    isNew = request.layout == null,
                    name = title,
                    exerciseCount = drafts.size,
                    canSave = canSave,
                    onCancel = onBack,
                    onSave = { onSave(title.trim(), category, parsedExercises) }
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 38.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Layout name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (drafts.isEmpty()) {
                    item {
                        AddExerciseLandingCard(
                            exerciseCount = 0,
                            onAddExercise = { replaceIndex = null; showExerciseLibrary = true }
                        )
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Exercises",
                                color = LocalWorkoutUiPalette.current.TextStrong,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (drafts.size > 1) SmallAction("Reorder", onClick = { reorderMode = true })
                        }
                    }

                    drafts.forEachIndexed { index, draft ->
                        item(key = "draft-$index-${draft.name}") {
                            LayoutExerciseSection(
                                draft = draft,
                                index = index,
                                sessions = sessions,
                                setLogs = setLogs,
                                onChange = { updated -> if (index in drafts.indices) drafts[index] = updated },
                                onEdit = { editTypeIndex = index },
                                onReplace = { replaceIndex = index; showExerciseLibrary = true },
                                onDelete = { if (index in drafts.indices) drafts.removeAt(index) },
                                onLongPress = { if (drafts.size > 1) reorderMode = true }
                            )
                        }
                    }
                }

                item {
                    LayoutSaveButton(
                        text = if (request.layout == null) "Save Layout" else "Save Changes",
                        enabled = canSave,
                        onClick = { onSave(title.trim(), category, parsedExercises) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LayoutSaveButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .then(
                if (enabled) Modifier.background(LocalWorkoutUiPalette.current.Heat)
                else Modifier.background(LocalWorkoutUiPalette.current.CardHigh)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) LocalWorkoutUiPalette.current.OnAccent else LocalWorkoutUiPalette.current.Dimmed,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun LayoutReorderExerciseMode(
    drafts: List<LayoutExerciseDraft>,
    onMove: (Int, Int) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ordered by remember(drafts) { mutableStateOf(drafts) }
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize().background(LocalWorkoutUiPalette.current.Canvas)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Reorder exercises", color = LocalWorkoutUiPalette.current.TextStrong, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text("Long-press and drag", color = LocalWorkoutUiPalette.current.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            SmallAction("Done", onClick = onDone)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ordered.indices.toList(), key = { idx -> "layout-reorder-${ordered[idx].sourceExerciseId ?: ordered[idx].name}-$idx" }) { displayedIndex ->
                val draft = ordered[displayedIndex]
                val dragging = draggingIndex == displayedIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = if (dragging) dragOffset else 0f
                            alpha = if (dragging) 0.90f else 1f
                            shadowElevation = if (dragging) 12f else 0f
                        }
                        .clip(LayoutInnerShape)
                        .background(if (dragging) LocalWorkoutUiPalette.current.CardHigh else LocalWorkoutUiPalette.current.Card)
                        .border(1.dp, if (dragging) LocalWorkoutUiPalette.current.Orange.copy(alpha = .55f) else LocalWorkoutUiPalette.current.Border, LayoutInnerShape)
                        .pointerInput(draft.name, displayedIndex) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggingIndex = displayedIndex; dragOffset = 0f },
                                onDragCancel = { draggingIndex = -1; dragOffset = 0f; ordered = drafts },
                                onDragEnd = {
                                    val current = ordered.indexOf(draft)
                                    if (displayedIndex >= 0 && current >= 0 && displayedIndex != current) onMove(displayedIndex, current)
                                    draggingIndex = -1
                                    dragOffset = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val threshold = 44.dp.toPx()
                                    val current = ordered.indexOf(draft)
                                    when {
                                        dragOffset < -threshold && current > 0 -> {
                                            val m = ordered.toMutableList(); val moved = m.removeAt(current); m.add(current - 1, moved); ordered = m; dragOffset += threshold; draggingIndex = current - 1
                                        }
                                        dragOffset > threshold && current in 0 until ordered.lastIndex -> {
                                            val m = ordered.toMutableList(); val moved = m.removeAt(current); m.add(current + 1, moved); ordered = m; dragOffset -= threshold; draggingIndex = current + 1
                                        }
                                    }
                                }
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.DragHandle, "Drag to reorder", tint = LocalWorkoutUiPalette.current.Orange, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f)) {
                        Text(draft.name, color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (draft.equipment.isBlank()) exerciseTypeName(draft.exerciseType) else draft.equipment, color = LocalWorkoutUiPalette.current.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun exerciseTypeName(type: WorkoutExerciseType) = when(type) {
    WorkoutExerciseType.STRENGTH -> "Strength"
    WorkoutExerciseType.CARDIO_DISTANCE -> "Cardio"
    WorkoutExerciseType.CARDIO_ROPE -> "Battle Ropes"
    WorkoutExerciseType.TIME_ONLY -> "Time Only"
}

@Composable
private fun LayoutExerciseEditorRow(
    index: Int,
    draft: LayoutExerciseDraft,
    canDelete: Boolean,
    onChange: (LayoutExerciseDraft) -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LayoutInnerShape)
            .background(LocalWorkoutUiPalette.current.CanvasDeep)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Movement ${index + 1}",
                color = LocalWorkoutUiPalette.current.SoftOrange,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Replace exercise",
                    color = LocalWorkoutUiPalette.current.Orange,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onReplace)
                )
                if (canDelete) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Remove movement",
                        tint = LocalWorkoutUiPalette.current.Dimmed,
                        modifier = Modifier.size(20.dp).clickable(onClick = onDelete)
                    )
                }
            }
        }
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onChange(draft.copy(name = it)) },
            label = { Text("Exercise") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            LayoutTypeChip("Strength", draft.exerciseType == WorkoutExerciseType.STRENGTH) {
                onChange(draft.copy(exerciseType = WorkoutExerciseType.STRENGTH, sets = "", reps = "", load = "", cardioLevel = ""))
            }
            LayoutTypeChip("Cardio", draft.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE) {
                onChange(draft.copy(exerciseType = WorkoutExerciseType.CARDIO_DISTANCE, sets = "1", reps = "", load = "", cardioLevel = ""))
            }
            LayoutTypeChip("Time Only", draft.exerciseType == WorkoutExerciseType.TIME_ONLY || draft.exerciseType == WorkoutExerciseType.CARDIO_ROPE) {
                onChange(draft.copy(exerciseType = WorkoutExerciseType.TIME_ONLY, sets = "1", reps = "", load = "", cardioLevel = ""))
            }
        }
        if (draft.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE || draft.exerciseType == WorkoutExerciseType.TIME_ONLY) {
            val hasDuration = draft.reps.isNotBlank()
            val totalSeconds = draft.reps.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val hours = if (hasDuration) (totalSeconds / 3600).toString() else ""
            val minutes = if (hasDuration) ((totalSeconds % 3600) / 60).toString() else ""
            val seconds = if (hasDuration) (totalSeconds % 60).toString() else ""
            Text(
                text = if (draft.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE) "Previous time" else "Duration",
                color = LocalWorkoutUiPalette.current.TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                LayoutDurationField("HR", hours, Modifier.weight(1f)) { h ->
                    val total = (h.toIntOrNull() ?: 0) * 3600 + (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)
                    onChange(draft.copy(reps = if (h.isBlank() && minutes.isBlank() && seconds.isBlank()) "" else total.coerceAtMost(WorkoutRepository.MAX_CARDIO_SECONDS).toString()))
                }
                LayoutDurationField("MIN", minutes, Modifier.weight(1f)) { m ->
                    val total = (hours.toIntOrNull() ?: 0) * 3600 + (m.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)
                    onChange(draft.copy(reps = if (hours.isBlank() && m.isBlank() && seconds.isBlank()) "" else total.coerceAtMost(WorkoutRepository.MAX_CARDIO_SECONDS).toString()))
                }
                LayoutDurationField("SEC", seconds, Modifier.weight(1f)) { sec ->
                    val total = (hours.toIntOrNull() ?: 0) * 3600 + (minutes.toIntOrNull() ?: 0) * 60 + (sec.toIntOrNull() ?: 0)
                    onChange(draft.copy(reps = if (hours.isBlank() && minutes.isBlank() && sec.isBlank()) "" else total.coerceAtMost(WorkoutRepository.MAX_CARDIO_SECONDS).toString()))
                }
            }
            if (draft.exerciseType == WorkoutExerciseType.CARDIO_DISTANCE) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(
                        value = draft.load,
                        onValueChange = { raw -> onChange(draft.copy(load = raw.filter { it.isDigit() || it == '.' }.take(7))) },
                        label = { Text("Previous distance (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draft.cardioLevel,
                        onValueChange = { value -> onChange(draft.copy(cardioLevel = value.filter { it.isDigit() || it == '.' }.take(6))) },
                        label = { Text("Previous level") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(
                    value = draft.sets,
                    onValueChange = { raw ->
                        val value = raw.filter(Char::isDigit).take(2)
                        onChange(draft.copy(sets = if (value.isBlank()) "" else (value.toIntOrNull() ?: 1).coerceIn(1, 99).toString()))
                    },
                    label = { Text("Sets") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = draft.reps,
                    onValueChange = { raw ->
                        val maxValue = if (draft.exerciseType == WorkoutExerciseType.CARDIO_ROPE) WorkoutRepository.MAX_CARDIO_SECONDS else WorkoutRepository.MAX_REPS
                        val value = raw.filter(Char::isDigit).take(6)
                        onChange(draft.copy(reps = if (value.isBlank()) "" else (value.toIntOrNull() ?: 0).coerceIn(0, maxValue).toString()))
                    },
                    label = { Text(if (draft.exerciseType == WorkoutExerciseType.CARDIO_ROPE) "Seconds" else "Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (draft.exerciseType == WorkoutExerciseType.STRENGTH) {
                    OutlinedTextField(
                        value = draft.load,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == '.' }.take(6)
                            val number = filtered.toDoubleOrNull()
                            onChange(draft.copy(load = if (number != null && number > WorkoutRepository.MAX_KG) formatLayoutLoad(WorkoutRepository.MAX_KG) else filtered))
                        },
                        label = { Text("kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }
}


@Composable
private fun LayoutDurationField(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val digits = raw.filter(Char::isDigit).take(2)
            if (digits.isBlank()) onValueChange("")
            else onValueChange((digits.toIntOrNull() ?: 0).coerceIn(0, if (label == "HR") 99 else 59).toString())
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun LayoutEditChoiceChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(LayoutInnerShape)
            .background(if (selected) LocalWorkoutUiPalette.current.OrangeTint else LocalWorkoutUiPalette.current.Card)
            .border(1.dp, if (selected) LocalWorkoutUiPalette.current.Orange.copy(alpha = .45f) else LocalWorkoutUiPalette.current.Border, LayoutInnerShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Text(
            text,
            color = if (selected) LocalWorkoutUiPalette.current.Orange else LocalWorkoutUiPalette.current.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LayoutTypeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) LocalWorkoutUiPalette.current.OrangeTint else LocalWorkoutUiPalette.current.Card)
            .border(1.dp, if (selected) LocalWorkoutUiPalette.current.Orange.copy(alpha = 0.45f) else LocalWorkoutUiPalette.current.Border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) LocalWorkoutUiPalette.current.Orange else LocalWorkoutUiPalette.current.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun layoutExerciseSummary(exercise: WorkoutLayoutExercise): String = when (exercise.exerciseType) {
    WorkoutExerciseType.STRENGTH -> "${exercise.name} — ${exercise.sets} × ${exercise.targetReps} @ ${formatLayoutLoad(exercise.loadKg)} kg"
    WorkoutExerciseType.CARDIO_DISTANCE -> "${exercise.name} — cardio • ${formatLayoutDuration(exercise.targetReps)} • ${formatLayoutLoad(exercise.loadKg)} km • level ${formatLayoutLoad(exercise.cardioLevel)}"
    WorkoutExerciseType.CARDIO_ROPE -> "${exercise.name} — ${exercise.sets} rope sets"
    WorkoutExerciseType.TIME_ONLY -> "${exercise.name} — timed • ${formatLayoutDuration(exercise.targetReps)}"
}

private fun defaultLayoutSets(category: WorkoutCategory): Int = when (category) {
    WorkoutCategory.CARDIO -> 1
    WorkoutCategory.MOBILITY -> 2
    WorkoutCategory.HIIT -> 4
    WorkoutCategory.STRENGTH -> 3
}

private fun defaultLayoutTarget(category: WorkoutCategory): Int = when (category) {
    WorkoutCategory.CARDIO -> 20
    WorkoutCategory.MOBILITY -> 30
    WorkoutCategory.HIIT -> 10
    WorkoutCategory.STRENGTH -> 10
}

private fun formatLayoutLoad(value: Double): String {
    val whole = value.toInt()
    return if (kotlin.math.abs(value - whole.toDouble()) < 0.001) whole.toString() else String.format(Locale.ENGLISH, "%.1f", value)
}

private fun formatLayoutDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val h = safe / 3600
    val m = (safe % 3600) / 60
    val sec = safe % 60
    return if (h > 0) "%d:%02d:%02d".format(Locale.ENGLISH, h, m, sec) else "%d:%02d".format(Locale.ENGLISH, m, sec)
}
