package com.rishabh.gymbros.ui.train

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Color
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.rishabh.gymbros.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.rishabh.gymbros.features.workout.data.WorkoutRepository
import com.rishabh.gymbros.features.workout.domain.ExerciseLibraryCatalog
import com.rishabh.gymbros.features.workout.model.ExerciseLibraryItem
import com.rishabh.gymbros.features.workout.model.PlannedExercise
import com.rishabh.gymbros.features.workout.model.WorkoutCategory
import com.rishabh.gymbros.features.workout.model.WorkoutExerciseType

private val LibraryCardShape = RoundedCornerShape(18.dp)

@Composable
fun ExerciseLibraryScreen(
    plannedExercises: List<PlannedExercise>,
    addedExerciseNames: Set<String>? = null,
    pageTitle: String = "Add Exercise",
    onBack: () -> Unit,
    onAdd: (ExerciseLibraryItem) -> Unit,
    onCreateCustom: (String, WorkoutExerciseType, String, String) -> Unit,
    onDeleteCustom: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedEquipmentName by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateExercise by rememberSaveable { mutableStateOf(false) }
    var detailItem by remember { mutableStateOf<ExerciseLibraryItem?>(null) }
    val context = LocalContext.current
    val repDbItems = remember(context) { ExerciseLibraryCatalog.items(context) }
    val customItems = remember(plannedExercises) {
        plannedExercises.filter { it.isCustom }.map { custom ->
            ExerciseLibraryItem(
                id = custom.id,
                name = custom.name,
                category = custom.category,
                muscleGroup = custom.tag.ifBlank { custom.category.title },
                defaultSets = 1,
                defaultReps = 0,
                defaultLoadKg = 0.0,
                description = "Custom exercise",
                equipment = custom.equipment,
                primaryMuscles = listOfNotNull(custom.tag.takeIf { it.isNotBlank() }),
                exerciseType = custom.exerciseType
            )
        }
    }
    val libraryItems = remember(repDbItems, customItems) { repDbItems + customItems }
    val filtered = remember(query, selectedCategoryName, selectedEquipmentName, libraryItems) {
        libraryItems.filter { item ->
            matchesLibraryCategory(item, selectedCategoryName) &&
                    matchesEquipmentFilter(item, selectedEquipmentName) && (
                    query.isBlank() ||
                            item.name.contains(query.trim(), ignoreCase = true) ||
                            item.muscleGroup.contains(query.trim(), ignoreCase = true) ||
                            item.equipment.contains(query.trim(), ignoreCase = true)
                    )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LocalWorkoutUiPalette.current.Canvas),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LibraryHeader(title = pageTitle, onBack = onBack)
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(LibraryCardShape)
                    .background(LocalWorkoutUiPalette.current.pick(LocalWorkoutUiPalette.current.Card, LocalWorkoutUiPalette.current.OrangeTint))
                    .workoutBorder(1.dp, LocalWorkoutUiPalette.current.Orange.copy(alpha = 0.35f), LibraryCardShape)
                    .clickable { showCreateExercise = true }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(LocalWorkoutUiPalette.current.pick(WorkoutAccents.Cyan, LocalWorkoutUiPalette.current.Orange)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = LocalWorkoutUiPalette.current.pick(WorkoutAccents.OnCyan, LocalWorkoutUiPalette.current.CanvasDeep))
                    }
                    Column {
                        Text("Enter Custom Exercise", color = LocalWorkoutUiPalette.current.pick(WorkoutAccents.Cyan, LocalWorkoutUiPalette.current.TextStrong), fontWeight = FontWeight.ExtraBold)
                        Text("Add your own movement to the exercise list", color = LocalWorkoutUiPalette.current.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = if (LocalWorkoutUiPalette.current.MultiColor) {
                    OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = WorkoutAccents.Cyan,
                        unfocusedContainerColor = LocalWorkoutUiPalette.current.Card,
                        focusedContainerColor = LocalWorkoutUiPalette.current.Card
                    )
                } else {
                    OutlinedTextFieldDefaults.colors()
                },
                label = { Text("Search movements") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                }
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf("Arms", "Back", "Biceps", "Cardio", "Chest", "Core", "Legs", "Shoulders", "Triceps")
                CategoryChip(
                    text = "All",
                    selected = selectedCategoryName == null,
                    onClick = { selectedCategoryName = null }
                )
                categories.forEach { category ->
                    CategoryChip(
                        text = category,
                        selected = selectedCategoryName == category,
                        onClick = { selectedCategoryName = category }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val equipmentFilters = listOf("Barbell", "Bodyweight", "Cable", "Dumbbell", "Machine", "Other")
                CategoryChip(
                    text = "All Equipment",
                    selected = selectedEquipmentName == null,
                    onClick = { selectedEquipmentName = null },
                    multiTint = WorkoutAccents.PurpleTint,
                    multiText = WorkoutAccents.Purple
                )
                equipmentFilters.forEach { equipment ->
                    CategoryChip(
                        text = equipment,
                        selected = selectedEquipmentName == equipment,
                        onClick = { selectedEquipmentName = equipment },
                        multiTint = WorkoutAccents.PurpleTint,
                        multiText = WorkoutAccents.Purple
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exercise Library",
                    color = LocalWorkoutUiPalette.current.TextStrong,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${filtered.size} moves",
                    color = LocalWorkoutUiPalette.current.Muted,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        if (filtered.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LibraryCardShape)
                        .background(LocalWorkoutUiPalette.current.Card)
                        .workoutBorder(1.dp, LocalWorkoutUiPalette.current.Border, LibraryCardShape)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No movements found",
                        color = LocalWorkoutUiPalette.current.TextStrong,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Try another search or category.",
                        color = LocalWorkoutUiPalette.current.Dimmed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            items(filtered.size) { index ->
                val item = filtered[index]
                val alreadyAdded = addedExerciseNames?.any { it.equals(item.name, ignoreCase = true) }
                    ?: plannedExercises.any { planned ->
                        planned.category == item.category && planned.name.equals(item.name, ignoreCase = true)
                    }
                ExerciseLibraryCard(
                    item = item,
                    added = alreadyAdded,
                    onAdd = { if (!alreadyAdded) onAdd(item) },
                    onDetails = { detailItem = item }
                )
            }
        }
    }

    detailItem?.let { item ->
        val alreadyAdded = addedExerciseNames?.any { it.equals(item.name, ignoreCase = true) }
            ?: plannedExercises.any { planned ->
                planned.name.equals(item.name, ignoreCase = true)
            }
        val isCustom = plannedExercises.any { it.id == item.id && it.isCustom }
        ExerciseDetailsDialog(
            item = item,
            added = alreadyAdded,
            isCustom = isCustom,
            onChoose = {
                if (!alreadyAdded) onAdd(item)
                detailItem = null
            },
            onDelete = {
                if (isCustom) {
                    onDeleteCustom(item.id)
                    detailItem = null
                }
            },
            onDismiss = { detailItem = null }
        )
    }

    if (showCreateExercise) {
        CreateExerciseDialog(
            onDismiss = { showCreateExercise = false },
            onCreate = { name, type, equipment, tag ->
                onCreateCustom(name, type, equipment, tag)
                showCreateExercise = false
            }
        )
    }
}

@Composable
private fun CustomExerciseCard(exercise: PlannedExercise) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LibraryCardShape)
            .background(LocalWorkoutUiPalette.current.Card)
            .workoutBorder(1.dp, LocalWorkoutUiPalette.current.Orange.copy(alpha = 0.18f), LibraryCardShape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(LocalWorkoutUiPalette.current.pick(WorkoutAccents.CyanTint, LocalWorkoutUiPalette.current.OrangeTint)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = LocalWorkoutUiPalette.current.pick(WorkoutAccents.Cyan, LocalWorkoutUiPalette.current.Orange))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(exercise.name, color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.Bold)
            Text(
                "${exercise.category.title} • ${exercise.sets} × ${exercise.targetReps}${if (exercise.loadKg > 0.0) " • ${exercise.loadKg} kg" else ""}",
                color = LocalWorkoutUiPalette.current.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text("CUSTOM", color = LocalWorkoutUiPalette.current.pick(WorkoutAccents.Cyan, LocalWorkoutUiPalette.current.Gold), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CreateExerciseDialog(
    onDismiss: () -> Unit,
    onCreate: (String, WorkoutExerciseType, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var typeName by rememberSaveable { mutableStateOf(WorkoutExerciseType.STRENGTH.name) }
    var equipment by rememberSaveable { mutableStateOf<String?>(null) }
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    var equipmentExpanded by rememberSaveable { mutableStateOf(false) }
    var tagExpanded by rememberSaveable { mutableStateOf(false) }
    val type = WorkoutExerciseType.valueOf(typeName)
    val equipmentOptions = listOf("Barbell", "Bodyweight", "Cable", "Dumbbell", "Machine", "Other")
    val tagOptions = listOf("Chest", "Back", "Shoulders", "Triceps", "Biceps", "Legs", "Arms", "Core", "Cardio")
    val valid = name.trim().isNotBlank() && tag != null &&
            (type != WorkoutExerciseType.STRENGTH || equipment != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalWorkoutUiPalette.current.CardHigh,
        title = { Text("Enter Custom Exercise", color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    CategoryChip("Strength", type == WorkoutExerciseType.STRENGTH) {
                        typeName = WorkoutExerciseType.STRENGTH.name
                        equipment = null
                        tag = null
                    }
                    CategoryChip("Cardio", type == WorkoutExerciseType.CARDIO_DISTANCE) {
                        typeName = WorkoutExerciseType.CARDIO_DISTANCE.name
                        equipment = null
                        tag = null
                    }
                    CategoryChip("Time only", type == WorkoutExerciseType.TIME_ONLY) {
                        typeName = WorkoutExerciseType.TIME_ONLY.name
                        equipment = null
                        tag = null
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Exercise name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (type == WorkoutExerciseType.STRENGTH) {
                    SelectorDropdown(
                        label = "Equipment",
                        placeholder = "Select equipment",
                        selected = equipment,
                        expanded = equipmentExpanded,
                        options = equipmentOptions,
                        onExpandedChange = { equipmentExpanded = it },
                        onSelect = { equipment = it; equipmentExpanded = false },
                        onClear = { equipment = null }
                    )
                }

                SelectorDropdown(
                    label = "Tag",
                    placeholder = "Select tag",
                    selected = tag,
                    expanded = tagExpanded,
                    options = tagOptions,
                    onExpandedChange = { tagExpanded = it },
                    onSelect = { tag = it; tagExpanded = false },
                    onClear = { tag = null }
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onCreate(name.trim(), type, equipment.orEmpty(), tag.orEmpty())
            }) { Text("Create", color = LocalWorkoutUiPalette.current.Orange, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = LocalWorkoutUiPalette.current.TextSecondary) } }
    )
}

@Composable
private fun SelectorDropdown(
    label: String,
    placeholder: String,
    selected: String?,
    expanded: Boolean,
    options: List<String>,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = LocalWorkoutUiPalette.current.Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, LocalWorkoutUiPalette.current.Border, RoundedCornerShape(14.dp))
                    .clickable { onExpandedChange(!expanded) }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    selected ?: placeholder,
                    color = if (selected == null) LocalWorkoutUiPalette.current.Dimmed else LocalWorkoutUiPalette.current.TextStrong,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Open $label", tint = LocalWorkoutUiPalette.current.TextSecondary)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option) })
                }
            }
        }
        if (selected != null) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(LocalWorkoutUiPalette.current.OrangeTint)
                    .border(1.dp, LocalWorkoutUiPalette.current.Orange.copy(alpha = 0.45f), CircleShape)
                    .clickable(onClick = onClear)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Remove $selected", tint = LocalWorkoutUiPalette.current.SoftOrange, modifier = Modifier.size(16.dp))
                Text(selected, color = LocalWorkoutUiPalette.current.SoftOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun LibraryHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = LocalWorkoutUiPalette.current.TextStrong
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = LocalWorkoutUiPalette.current.TextStrong,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Add movements to your planned workout",
                color = LocalWorkoutUiPalette.current.Dimmed,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(LocalWorkoutUiPalette.current.pick(WorkoutAccents.CyanTint, LocalWorkoutUiPalette.current.OrangeTint)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null,
                tint = LocalWorkoutUiPalette.current.pick(WorkoutAccents.Cyan, LocalWorkoutUiPalette.current.Orange)
            )
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    selected: Boolean,
    multiTint: Color = WorkoutAccents.CyanTint,
    multiText: Color = WorkoutAccents.Cyan,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) LocalWorkoutUiPalette.current.pick(multiTint, LocalWorkoutUiPalette.current.OrangeTint)
                else LocalWorkoutUiPalette.current.Card
            )
            .workoutBorder(
                1.dp,
                if (selected) LocalWorkoutUiPalette.current.Orange.copy(alpha = 0.55f)
                else LocalWorkoutUiPalette.current.Border,
                CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            color = if (selected) LocalWorkoutUiPalette.current.pick(multiText, LocalWorkoutUiPalette.current.SoftOrange) else LocalWorkoutUiPalette.current.Text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ExerciseLibraryCard(
    item: ExerciseLibraryItem,
    added: Boolean,
    onAdd: () -> Unit,
    onDetails: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LibraryCardShape)
            .background(LocalWorkoutUiPalette.current.Card)
            .workoutBorder(1.dp, LocalWorkoutUiPalette.current.Border, LibraryCardShape)
            .clickable(onClick = onDetails)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExerciseAssetImage(item = item, size = 58)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = LocalWorkoutUiPalette.current.TextStrong,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.muscleGroup} • ${item.equipment}",
                color = LocalWorkoutUiPalette.current.pick(WorkoutAccents.Cyan, LocalWorkoutUiPalette.current.SoftOrange),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.description,
                color = LocalWorkoutUiPalette.current.Dimmed,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (added) LocalWorkoutUiPalette.current.ElevatedHigh else LocalWorkoutUiPalette.current.pick(WorkoutAccents.Cyan, LocalWorkoutUiPalette.current.Orange))
                .clickable(enabled = !added) { onAdd() },
            contentAlignment = Alignment.Center
        ) {
            if (added) {
                Icon(
                    painter = painterResource(R.drawable.ic_gymbros_tick),
                    contentDescription = "Already added",
                    tint = LocalWorkoutUiPalette.current.pick(WorkoutAccents.Cyan, LocalWorkoutUiPalette.current.SoftOrange),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(Icons.Outlined.Add, contentDescription = "Add to plan", tint = LocalWorkoutUiPalette.current.pick(WorkoutAccents.OnCyan, LocalWorkoutUiPalette.current.CanvasDeep))
            }
        }
    }
}

@Composable
fun ExerciseAssetImage(item: ExerciseLibraryItem, size: Int) {
    val context = LocalContext.current
    val bitmap = remember(item.imageAssetPath) {
        item.imageAssetPath?.let { path ->
            runCatching { context.assets.open(path).use { input -> BitmapFactory.decodeStream(input) }?.asImageBitmap() }.getOrNull()
        }
    }
    Box(
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape(14.dp)).background(LocalWorkoutUiPalette.current.ElevatedHigh),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize().padding(3.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = LocalWorkoutUiPalette.current.Orange)
        }
    }
}

@Composable
private fun ExerciseDetailsDialog(item: ExerciseLibraryItem, added: Boolean, isCustom: Boolean, onChoose: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalWorkoutUiPalette.current.CardHigh,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExerciseAssetImage(item, 72)
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, color = LocalWorkoutUiPalette.current.TextStrong, fontWeight = FontWeight.ExtraBold)
                    Text("${item.muscleGroup} • ${item.equipment}", color = LocalWorkoutUiPalette.current.SoftOrange, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(item.description, color = LocalWorkoutUiPalette.current.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("Difficulty: ${item.difficulty}${if (item.met > 0) "  •  MET ${item.met}" else ""}", color = LocalWorkoutUiPalette.current.Dimmed, style = MaterialTheme.typography.bodySmall)
                }
                if (item.primaryMuscles.isNotEmpty()) item {
                    Text("TARGET MUSCLES", color = LocalWorkoutUiPalette.current.SoftOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text(item.primaryMuscles.joinToString(" • "), color = LocalWorkoutUiPalette.current.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                if (item.instructions.isNotEmpty()) {
                    item { Text("HOW TO", color = LocalWorkoutUiPalette.current.SoftOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
                    items(item.instructions.size) { index ->
                        Text("${index + 1}. ${item.instructions[index]}", color = LocalWorkoutUiPalette.current.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !added, onClick = onChoose) {
                Text(if (added) "Already Added" else "Choose Exercise", color = LocalWorkoutUiPalette.current.Orange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isCustom) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = com.rishabh.gymbros.ui.theme.Ui2Colors.Danger)
                        Spacer(Modifier.width(4.dp))
                        Text("Delete Exercise", color = LocalWorkoutUiPalette.current.DangerText)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Close", color = LocalWorkoutUiPalette.current.TextSecondary) }
            }
        }
    )
}

private fun matchesLibraryCategory(item: ExerciseLibraryItem, category: String?): Boolean {
    if (category == null) return true
    if (category == "Cardio") return item.category == WorkoutCategory.CARDIO
    val text = (item.muscleGroup + " " + item.primaryMuscles.joinToString(" ")).lowercase()
    return when (category) {
        "Chest" -> "chest" in text || "pectoral" in text
        "Back" -> listOf("back", "lat", "trape", "rhomboid").any { it in text }
        "Shoulders" -> listOf("shoulder", "deltoid").any { it in text }
        "Triceps" -> "tricep" in text
        "Biceps" -> "bicep" in text
        "Legs" -> listOf("leg", "quad", "hamstring", "glute", "calf", "calves", "adductor").any { it in text }
        "Arms" -> listOf("arm", "bicep", "tricep", "forearm").any { it in text }
        "Core" -> listOf("core", "abdominal", "abs", "oblique").any { it in text }
        else -> true
    }
}


private fun matchesEquipmentFilter(item: ExerciseLibraryItem, equipment: String?): Boolean {
    if (equipment == null) return true
    val value = item.equipment.trim().lowercase()
    return when (equipment) {
        "Barbell" -> "barbell" in value || "ez bar" in value || "ez-bar" in value
        "Machine" -> "machine" in value || "lever" in value || "smith" in value
        "Cable" -> "cable" in value
        "Dumbbell" -> "dumbbell" in value
        "Bodyweight" -> "bodyweight" in value || "body weight" in value || value == "none" || value.isBlank()
        "Other" -> listOf("barbell", "ez bar", "ez-bar", "machine", "lever", "smith", "cable", "dumbbell", "bodyweight", "body weight").none { it in value }
        else -> true
    }
}