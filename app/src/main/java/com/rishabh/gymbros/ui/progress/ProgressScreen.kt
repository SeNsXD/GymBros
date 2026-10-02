package com.rishabh.gymbros.ui.progress

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog as ComposeDatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import com.rishabh.gymbros.features.settings.model.UiVersion
import com.rishabh.gymbros.ui.theme.Ui2Colors
import com.rishabh.gymbros.ui.theme.Ui2Theme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rishabh.gymbros.ui.theme.IgniteBrushes
import com.rishabh.gymbros.ui.theme.IgniteColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// This palette is scoped to Progress; classic colors and all tracking logic remain shared.
private data class ProgressPalette(
    val isV2: Boolean,
    val background: Brush,
    val surface: Color,
    val raised: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val goal: Color,
    val selection: Color,
    val photoEdge: Color,
    val danger: Color,
    val secondary: Color,
    val tertiary: Color
)

private fun classicProgressPalette() = ProgressPalette(
    false, IgniteBrushes.Screen, IgniteColors.Surface2, Color(0xFF1E1E22),
    IgniteColors.Text, IgniteColors.MutedText, Color(0xFFFF7A3C), Color(0xFFFFC857),
    Color(0xFFFF8A24), Color(0xFF0B0B0D), Color(0xFFE2493D),
    Color(0xFFFF8A24), Color(0xFFFFC857)
)
private val LocalProgressPalette = staticCompositionLocalOf { classicProgressPalette() }

@Composable
fun ProgressScreen(modifier: Modifier = Modifier, uiVersion: UiVersion = UiVersion.V1) {
    val colors = if (uiVersion == UiVersion.V2) ProgressPalette(
        true, Brush.verticalGradient(listOf(Ui2Colors.Background, Ui2Colors.Background)),
        Ui2Colors.Card, Ui2Colors.CardRaised, Ui2Colors.Text, Ui2Colors.Muted,
        Ui2Colors.Primary, Ui2Colors.Cyan, Ui2Colors.Primary, Ui2Colors.Background, Ui2Colors.Danger,
        Ui2Colors.Cyan, Ui2Colors.Lavender
    ) else classicProgressPalette()
    CompositionLocalProvider(LocalProgressPalette provides colors) {
        if (uiVersion == UiVersion.V2) Ui2Theme { ProgressContent(modifier) }
        else ProgressContent(modifier)
    }
}

private fun Modifier.progressSurface(colors: ProgressPalette, radius: androidx.compose.ui.unit.Dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return clip(shape).background(colors.surface)
}

private enum class ProgressAngle(val label: String) { FRONT("Front"), SIDE("Side"), BACK("Back") }
private data class ProgressPhoto(val uri: String, val epochDay: Long)
private data class ProgressCheckIn(
    val id: String,
    val epochDay: Long,
    val weightKg: Double?,
    val frontUri: String? = null,
    val sideUri: String? = null,
    val backUri: String? = null
) {
    fun uriFor(angle: ProgressAngle): String? = when (angle) {
        ProgressAngle.FRONT -> frontUri
        ProgressAngle.SIDE -> sideUri
        ProgressAngle.BACK -> backUri
    }
}

@Composable
private fun ProgressContent(modifier: Modifier = Modifier) {
    val colors = LocalProgressPalette.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gymbros_progress", Context.MODE_PRIVATE) }
    var checkIns by remember { mutableStateOf<List<ProgressCheckIn>>(loadCheckIns(prefs)) }
    var goalSetupDone by remember { mutableStateOf(prefs.getBoolean("goal_setup_done_v1", false)) }
    var goalType by remember { mutableStateOf(prefs.getString("goal_type_v1", "") ?: "") }
    var goalCurrentWeight by remember { mutableStateOf(prefs.getString("goal_current_weight_v1", "") ?: "") }
    var goalTargetWeight by remember { mutableStateOf(prefs.getString("goal_target_weight_v1", "") ?: "") }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDate by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var pendingWeight by rememberSaveable { mutableStateOf("") }
    var compareMode by rememberSaveable { mutableStateOf(false) }
    var compareAngleName by rememberSaveable { mutableStateOf(ProgressAngle.FRONT.name) }
    var olderId by rememberSaveable { mutableStateOf<String?>(null) }
    var newerId by rememberSaveable { mutableStateOf<String?>(null) }
    var overflowOpen by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var goalSetupDismissed by rememberSaveable { mutableStateOf(false) }

    fun persist(next: List<ProgressCheckIn>) {
        checkIns = next.sortedByDescending { it.epochDay }
        prefs.edit().putStringSet("checkins_v1", checkIns.map(::encodeCheckIn).toSet()).apply()
    }

    fun resetProgress() {
        File(context.filesDir, "progress_photos").deleteRecursively()
        prefs.edit().clear().apply()
        checkIns = emptyList()
        goalSetupDone = false
        goalType = ""
        goalCurrentWeight = ""
        goalTargetWeight = ""
        showAddDialog = false
        pendingDate = LocalDate.now().toEpochDay()
        pendingWeight = ""
        compareMode = false
        compareAngleName = ProgressAngle.FRONT.name
        olderId = null
        newerId = null
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(3)) { uris ->
        // A timeline entry is created only after the user actually selects at least one photo.
        if (uris.isNotEmpty()) {
            val id = "checkin-${System.currentTimeMillis()}"
            val detectedDate = uris.firstNotNullOfOrNull { detectPhotoDate(context, it) }
            val checkIn = ProgressCheckIn(
                id = id,
                epochDay = detectedDate?.toEpochDay() ?: pendingDate,
                weightKg = pendingWeight.trim().toDoubleOrNull(),
                frontUri = uris.getOrNull(0)?.let { copyPhotoToPrivateStorage(context, it, "${id}-front") },
                sideUri = uris.getOrNull(1)?.let { copyPhotoToPrivateStorage(context, it, "${id}-side") },
                backUri = uris.getOrNull(2)?.let { copyPhotoToPrivateStorage(context, it, "${id}-back") }
            )
            persist(checkIns + checkIn)
            pendingDate = LocalDate.now().toEpochDay()
            pendingWeight = ""
        }
    }

    val photoCheckIns = checkIns.filter { it.frontUri != null || it.sideUri != null || it.backUri != null }
    val sortedPhotos = photoCheckIns.sortedBy { it.epochDay }
    val firstPhoto = sortedPhotos.firstOrNull()
    val latestPhoto = sortedPhotos.lastOrNull()
    if (olderId == null && firstPhoto != null) olderId = firstPhoto.id
    if (newerId == null && latestPhoto != null) newerId = latestPhoto.id

    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.background).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Progress", color = colors.text, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.clip(CircleShape).background(colors.accent.copy(alpha = .14f)).then(if (colors.isV2) Modifier else Modifier.border(1.dp, colors.accent.copy(alpha = .30f), CircleShape)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(if (colors.isV2) "Active phase" else "ACTIVE PHASE", color = colors.accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Text("Your private physique timeline", color = colors.muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (goalSetupDone) {
                    Box {
                        Text(
                            "⋮",
                            color = colors.muted,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.clip(CircleShape).clickable { overflowOpen = true }.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Reset Progress", color = colors.danger) },
                                onClick = { overflowOpen = false; confirmReset = true }
                            )
                        }
                    }
                }
            }
        }

        if (goalSetupDone) {
            item {
                ProgressHeroCard(
                    checkIns = checkIns,
                    startWeight = goalCurrentWeight.toDoubleOrNull(),
                    goalWeight = goalTargetWeight.toDoubleOrNull(),
                    goalType = goalType
                )
            }
            milestoneMessage(checkIns, goalCurrentWeight.toDoubleOrNull(), goalTargetWeight.toDoubleOrNull())?.let { message ->
                item { MilestoneBanner(message) }
            }
            item {
                WeightTrendCard(
                    checkIns = checkIns,
                    startWeight = goalCurrentWeight.toDoubleOrNull(),
                    goalWeight = goalTargetWeight.toDoubleOrNull()
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f).then(if (colors.isV2) Modifier.height(48.dp) else Modifier),
                    shape = CircleShape
                ) { Text(if (colors.isV2) "+  Add check-in" else "+  Add Check-in", fontWeight = if (colors.isV2) FontWeight.Medium else null) }
                Button(
                    onClick = { compareMode = !compareMode },
                    enabled = photoCheckIns.size >= 2,
                    modifier = Modifier.weight(1f).then(if (colors.isV2) Modifier.height(48.dp) else Modifier),
                    shape = CircleShape,
                    colors = if (colors.isV2) androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = colors.surface,
                        contentColor = colors.secondary,
                        disabledContainerColor = colors.raised,
                        disabledContentColor = colors.muted
                    ) else androidx.compose.material3.ButtonDefaults.buttonColors(),
                    border = null
                ) {
                    Text(
                        if (colors.isV2) (if (compareMode) "Close compare" else "Compare photos")
                        else (if (compareMode) "Close Compare" else "Compare Photos"),
                        fontWeight = if (colors.isV2) FontWeight.Medium else null
                    )
                }
            }
        }

        if (compareMode && photoCheckIns.size >= 2) {
            item {
                CompareCard(
                    checkIns = photoCheckIns,
                    olderId = olderId,
                    newerId = newerId,
                    angle = ProgressAngle.valueOf(compareAngleName),
                    onOlderChange = { olderId = it },
                    onNewerChange = { newerId = it },
                    onAngleChange = { compareAngleName = it.name }
                )
            }
            item {
                MilestoneRow(photoCheckIns) { old, new ->
                    olderId = old.id
                    newerId = new.id
                    compareMode = true
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (colors.isV2) "Photo timeline" else "Photo Timeline", color = colors.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("${photoCheckIns.size} ${if (photoCheckIns.size == 1) "entry" else "entries"}", color = colors.muted, style = MaterialTheme.typography.labelSmall)
            }
        }

        if (photoCheckIns.isEmpty()) {
            item { Text("No progress photos yet. Add a check-in when you want to record progress.", color = colors.muted) }
        } else {
            items(photoCheckIns.size) { index ->
                val checkIn = photoCheckIns[index]
                val olderWeights = checkIns.filter { it.weightKg != null && it.epochDay < checkIn.epochDay }.sortedBy { it.epochDay }
                val previousWeight = olderWeights.lastOrNull()?.weightKg
                CheckInCard(
                    checkIn = checkIn,
                    previousWeight = previousWeight,
                    onDelete = {
                        listOf(checkIn.frontUri, checkIn.sideUri, checkIn.backUri).forEach(::deletePrivatePhoto)
                        persist(checkIns.filterNot { it.id == checkIn.id })
                    },
                    onUpdate = { updated -> persist(checkIns.map { if (it.id == updated.id) updated else it }) }
                )
            }
        }
    }

    if (!goalSetupDone && !goalSetupDismissed) {
        GoalSetupDialog(
            initialGoal = goalType,
            initialCurrentWeight = goalCurrentWeight,
            initialTargetWeight = goalTargetWeight,
            onDismiss = { goalSetupDismissed = true },
            onSave = { goal, current, target ->
                goalType = goal
                goalCurrentWeight = current
                goalTargetWeight = target
                goalSetupDone = true
                prefs.edit()
                    .putBoolean("goal_setup_done_v1", true)
                    .putString("goal_type_v1", goal)
                    .putString("goal_current_weight_v1", current)
                    .putString("goal_target_weight_v1", target)
                    .apply()
                // Keep starting weight for chart calculations only. It must not create a blank photo card.
            }
        )
    }

    if (showAddDialog) {
        AddCheckInDialog(
            epochDay = pendingDate,
            weight = pendingWeight,
            onWeightChange = { pendingWeight = it },
            onDateChange = { pendingDate = it.toEpochDay() },
            onDismiss = { showAddDialog = false },
            onContinue = {
                showAddDialog = false
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = colors.surface,
            title = { Text("Reset Progress?", color = colors.text, fontWeight = FontWeight.ExtraBold) },
            text = { Text("This will permanently remove your progress goal, check-ins and saved progress photos.", color = colors.muted) },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; resetProgress() }) { Text("Reset", color = colors.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCheckInDialog(
    epochDay: Long,
    weight: String,
    onWeightChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    val colors = LocalProgressPalette.current
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val date = LocalDate.ofEpochDay(epochDay)
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("New Progress Check-in", color = colors.text, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add your weight, then choose up to 3 photos in this order: Front, Side, Back.", color = colors.muted)
                OutlinedTextField(
                    value = weight,
                    onValueChange = { value -> if (value.length <= 6 && value.all { it.isDigit() || it == '.' }) onWeightChange(value) },
                    label = { Text("Weight (kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = {
                    if (colors.isV2) showDatePicker = true
                    else DatePickerDialog(context, { _, y, m, d -> onDateChange(LocalDate.of(y, m + 1, d)) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                }) { Text("Date · ${date.format(formatter)}") }
            }
        },
        confirmButton = { TextButton(onClick = onContinue) { Text("Choose Photos") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * 86_400_000L)
        ComposeDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(enabled = dateState.selectedDateMillis != null, onClick = {
                    dateState.selectedDateMillis?.let { onDateChange(LocalDate.ofEpochDay(it / 86_400_000L)) }
                    showDatePicker = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dateState) }
    }

}

@Composable
private fun ProgressHeroCard(
    checkIns: List<ProgressCheckIn>,
    startWeight: Double?,
    goalWeight: Double?,
    goalType: String
) {
    val colors = LocalProgressPalette.current
    if (startWeight == null || goalWeight == null) return
    val weighted = checkIns.filter { it.weightKg != null }.sortedBy { it.epochDay }
    val current = weighted.lastOrNull()?.weightKg ?: startWeight
    val totalDistance = kotlin.math.abs(goalWeight - startWeight)
    val movedSigned = if (goalWeight < startWeight) startWeight - current else current - startWeight
    val progress = if (totalDistance <= 0.0) 1f else (movedSigned / totalDistance).toFloat().coerceIn(0f, 1f)
    val totalChange = kotlin.math.abs(current - startWeight)
    val isGainGoal = goalWeight > startWeight
    val changeLabel = if (colors.isV2) (if (isGainGoal) "Net gain" else "Net loss") else (if (isGainGoal) "Net Gain" else "Net Loss")
    val weeks = weighted.takeIf { it.size >= 2 }?.let {
        val days = ChronoUnit.DAYS.between(LocalDate.ofEpochDay(it.first().epochDay), LocalDate.ofEpochDay(it.last().epochDay)).coerceAtLeast(1)
        days / 7.0
    } ?: 0.0
    val rate = if (weeks > 0.0) totalChange / weeks else 0.0

    Column(Modifier.fillMaxWidth().progressSurface(colors, 22.dp).padding(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text(if (colors.isV2) (if (isGainGoal) "Weight gain" else "Weight loss") else (if (isGainGoal) "WEIGHT GAIN" else "WEIGHT LOSS"), color = colors.accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatWeight(current), color = colors.text, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                    Text(" kg", color = colors.muted, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
                }
                Text("Started ${formatWeight(startWeight)} kg", color = colors.muted, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (colors.isV2) "Target" else "TARGET", color = colors.muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("${formatWeight(goalWeight)} kg", color = if (colors.isV2) colors.tertiary else colors.accent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("${(progress * 100).toInt()}% complete", color = if (colors.isV2) colors.accent else colors.muted, style = MaterialTheme.typography.labelSmall)
            }
        }
        Box(Modifier.fillMaxWidth().height(9.dp).clip(CircleShape).background(colors.muted.copy(alpha = .16f))) {
            Box(
                Modifier.fillMaxWidth(progress).height(9.dp).clip(CircleShape).background(
                    Brush.horizontalGradient(listOf(colors.accent, colors.accent))
                )
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroStatChip(formatWeight(totalChange), changeLabel, Modifier.weight(1f), if (colors.isV2) colors.accent else null)
            HeroStatChip("${weighted.size}", "Check-ins", Modifier.weight(1f), if (colors.isV2) colors.secondary else null)
            HeroStatChip(if (rate > 0.0) "${formatWeight(rate)} kg" else "—", if (colors.isV2) "Per week" else "Rate / week", Modifier.weight(1f), if (colors.isV2) Ui2Colors.Lavender else null)
        }
    }
}

@Composable
private fun HeroStatChip(value: String, label: String, modifier: Modifier = Modifier, tint: Color? = null) {
    val colors = LocalProgressPalette.current
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(colors.raised)
            .then(if (tint != null) Modifier.background(tint.copy(alpha = 0.10f)) else Modifier)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = tint ?: colors.text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(label, color = colors.muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun MilestoneBanner(message: String) {
    val colors = LocalProgressPalette.current
    if (colors.isV2) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(colors.accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Text("★", color = colors.accent, fontWeight = FontWeight.ExtraBold)
            }
            Text(message, color = colors.text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.accent.copy(alpha = 0.10f))
            .border(
                1.dp,
                if (colors.isV2) Brush.horizontalGradient(listOf(colors.accent.copy(alpha = 0.5f), colors.secondary.copy(alpha = 0.5f), colors.tertiary.copy(alpha = 0.5f)))
                else Brush.horizontalGradient(listOf(colors.accent.copy(alpha = 0.28f), colors.accent.copy(alpha = 0.28f))),
                RoundedCornerShape(14.dp)
            ).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("★", color = colors.accent, fontWeight = FontWeight.ExtraBold)
        Text(message, color = colors.text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WeightTrendCard(checkIns: List<ProgressCheckIn>, startWeight: Double?, goalWeight: Double?) {
    val colors = LocalProgressPalette.current
    if (startWeight == null || goalWeight == null) return
    val real = checkIns.filter { it.weightKg != null }.sortedBy { it.epochDay }
    val points = buildList {
        if (real.isEmpty() || kotlin.math.abs((real.first().weightKg ?: startWeight) - startWeight) > 0.001) {
            add(ProgressCheckIn("chart-start", real.firstOrNull()?.epochDay ?: LocalDate.now().toEpochDay(), startWeight))
        }
        addAll(real)
    }
    Column(
        Modifier.fillMaxWidth().progressSurface(colors, 20.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Weight Graph", color = colors.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("●", color = colors.accent, style = MaterialTheme.typography.labelSmall)
                    Text("Actual", color = colors.muted, style = MaterialTheme.typography.labelSmall)
                    Text("●", color = colors.goal, style = MaterialTheme.typography.labelSmall)
                    Text("Goal", color = colors.muted, style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(
                Modifier.then(if (colors.isV2) Modifier.clip(CircleShape).background(Ui2Colors.Background).padding(3.dp) else Modifier),
                horizontalArrangement = Arrangement.spacedBy(if (colors.isV2) 2.dp else 4.dp)
            ) {
                listOf("1M", "3M", "ALL").forEach { range ->
                    val active = range == "ALL"
                    Box(Modifier.clip(CircleShape).background(if (active) colors.accent.copy(alpha=if (colors.isV2) .16f else .14f) else if (colors.isV2) Color.Transparent else colors.raised).padding(horizontal=8.dp, vertical=5.dp)) {
                        Text(range, color = if (active) colors.accent else colors.muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        WeightLineChart(points, goalWeight)
    }
}

@Composable
private fun WeightLineChart(points: List<ProgressCheckIn>, goalWeight: Double) {
    val colors = LocalProgressPalette.current
    if (points.isEmpty()) return
    val actualColor = colors.accent
    val goalColor = colors.goal
    val grid = colors.muted.copy(alpha = 0.14f)
    val values = points.mapNotNull { it.weightKg } + goalWeight
    var min = (values.minOrNull() ?: goalWeight) - 1.0
    var max = (values.maxOrNull() ?: goalWeight) + 1.0
    if (max - min < 2.0) { min -= 1.0; max += 1.0 }
    val labels = listOf(max, (max + min) / 2.0, min)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH) }

    Row(Modifier.fillMaxWidth().height(150.dp)) {
        Column(Modifier.width(42.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { Text("${formatWeight(it)}kg", color = colors.muted, style = MaterialTheme.typography.labelSmall) }
        }
        Canvas(Modifier.weight(1f).fillMaxSize()) {
            for (i in 0..2) {
                val y = size.height * i / 2f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            fun yFor(value: Double): Float {
                val normalized = ((value - min) / (max - min)).toFloat()
                return size.height - normalized * size.height
            }
            val goalY = yFor(goalWeight)
            drawLine(
                goalColor.copy(alpha = 0.85f), Offset(0f, goalY), Offset(size.width, goalY),
                strokeWidth = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
            if (points.size == 1) {
                drawCircle(actualColor, radius = 6f, center = Offset(size.width / 2f, yFor(points.first().weightKg!!)))
            } else {
                val path = Path()
                points.forEachIndexed { index, point ->
                    val x = size.width * index / (points.size - 1).toFloat()
                    val y = yFor(point.weightKg!!)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                val lineBrush = if (colors.isV2)
                    Brush.horizontalGradient(listOf(actualColor, actualColor), startX = 0f, endX = size.width)
                else Brush.horizontalGradient(listOf(actualColor, actualColor))
                if (colors.isV2) {
                    // Soft fill under the line.
                    val fill = Path()
                    fill.moveTo(0f, size.height)
                    points.forEachIndexed { index, point ->
                        val x = size.width * index / (points.size - 1).toFloat()
                        fill.lineTo(x, yFor(point.weightKg!!))
                    }
                    fill.lineTo(size.width, size.height)
                    fill.close()
                    drawPath(fill, Brush.verticalGradient(listOf(actualColor.copy(alpha = 0.22f), Color.Transparent)))
                }
                drawPath(path, lineBrush, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                points.forEachIndexed { index, point ->
                    val x = size.width * index / (points.size - 1).toFloat()
                    val center = Offset(x, yFor(point.weightKg!!))
                    if (colors.isV2) drawCircle(colors.surface, radius = if (index == points.lastIndex) 10f else 9f, center = center)
                    drawCircle(actualColor, radius = if (colors.isV2 && index == points.lastIndex) 7f else 5f, center = center)
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(start = 42.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        val shown = if (points.size <= 4) points else listOf(points.first(), points[points.size / 2], points.last())
        shown.forEach { Text(LocalDate.ofEpochDay(it.epochDay).format(dateFormatter), color = colors.muted, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun CheckInCard(
    checkIn: ProgressCheckIn,
    previousWeight: Double?,
    onDelete: () -> Unit,
    onUpdate: (ProgressCheckIn) -> Unit
) {
    val colors = LocalProgressPalette.current
    val context = LocalContext.current
    val date = LocalDate.ofEpochDay(checkIn.epochDay)
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH) }
    var expanded by rememberSaveable(checkIn.id) { mutableStateOf(false) }
    var editingWeight by rememberSaveable(checkIn.id) { mutableStateOf(false) }
    var weightText by rememberSaveable(checkIn.id) { mutableStateOf(checkIn.weightKg?.let(::formatWeight) ?: "") }
    var pendingAngleName by rememberSaveable(checkIn.id) { mutableStateOf(ProgressAngle.FRONT.name) }

    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val angle = ProgressAngle.valueOf(pendingAngleName)
            val saved = copyPhotoToPrivateStorage(context, uri, "${checkIn.id}-${angle.name.lowercase()}-${System.currentTimeMillis()}")
            if (saved != null) {
                deletePrivatePhoto(checkIn.uriFor(angle))
                val detectedDate = detectPhotoDate(context, uri)
                val updated = when (angle) {
                    ProgressAngle.FRONT -> checkIn.copy(frontUri = saved)
                    ProgressAngle.SIDE -> checkIn.copy(sideUri = saved)
                    ProgressAngle.BACK -> checkIn.copy(backUri = saved)
                }.let { if (detectedDate != null) it.copy(epochDay = detectedDate.toEpochDay()) else it }
                onUpdate(updated)
            }
        }
    }

    fun choosePhoto(angle: ProgressAngle) {
        pendingAngleName = angle.name
        singlePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    fun removePhoto(angle: ProgressAngle) {
        deletePrivatePhoto(checkIn.uriFor(angle))
        onUpdate(when (angle) {
            ProgressAngle.FRONT -> checkIn.copy(frontUri = null)
            ProgressAngle.SIDE -> checkIn.copy(sideUri = null)
            ProgressAngle.BACK -> checkIn.copy(backUri = null)
        })
    }

    Column(
        Modifier.fillMaxWidth().progressSurface(colors, 16.dp)
            .clickable { expanded = !expanded }.padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThumbnailStack(checkIn)
                Column {
                    Text(date.format(formatter), color = colors.text, fontWeight = FontWeight.Bold)
                    val delta = if (checkIn.weightKg != null && previousWeight != null) checkIn.weightKg - previousWeight else null
                    val meta = buildString {
                        append(checkIn.weightKg?.let { "${formatWeight(it)} kg" } ?: "Weight not added")
                        if (delta != null && kotlin.math.abs(delta) > 0.001) {
                            append("  ·  ")
                            append(if (delta < 0) "▼ ${formatWeight(kotlin.math.abs(delta))} kg" else "▲ ${formatWeight(delta)} kg")
                        }
                    }
                    if (colors.isV2) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(checkIn.weightKg?.let { "${formatWeight(it)} kg" } ?: "Weight not added", color = colors.muted, style = MaterialTheme.typography.bodySmall)
                            val chip: Pair<String, Color>? = when {
                                delta != null && kotlin.math.abs(delta) > 0.001 ->
                                    if (delta < 0) "-${formatWeight(kotlin.math.abs(delta))} kg" to colors.accent
                                    else "+${formatWeight(delta)} kg" to colors.secondary
                                checkIn.weightKg != null && previousWeight == null -> "Start" to colors.secondary
                                else -> null
                            }
                            if (chip != null) {
                                Box(Modifier.clip(CircleShape).background(chip.second.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                    Text(chip.first, color = chip.second, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    } else {
                        Text(meta, color = colors.muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            TimelineChevron(expanded = expanded)
        }

        if (expanded) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.muted.copy(alpha = 0.12f)))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProgressAngle.entries.forEach { angle ->
                    EditableAnglePhoto(checkIn.uriFor(angle), angle.label, Modifier.weight(1f), { choosePhoto(angle) }, { removePhoto(angle) })
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { weightText = checkIn.weightKg?.let(::formatWeight) ?: ""; editingWeight = true }) { Text("Edit weight") }
                TextButton(onClick = {
                    DatePickerDialog(context, { _, y, m, d -> onUpdate(checkIn.copy(epochDay = LocalDate.of(y, m + 1, d).toEpochDay())) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                }) { Text("Change date") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) { Text("Delete", color = colors.muted) }
            }
        }
    }

    if (editingWeight) {
        AlertDialog(
            onDismissRequest = { editingWeight = false }, containerColor = colors.surface,
            title = { Text("Edit weight", color = colors.text, fontWeight = FontWeight.Bold) },
            text = { OutlinedTextField(weightText, { if (validWeightText(it)) weightText = it }, label = { Text("Weight (kg)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(enabled = weightText.toDoubleOrNull() != null, onClick = { onUpdate(checkIn.copy(weightKg = weightText.toDoubleOrNull())); editingWeight = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editingWeight = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TimelineChevron(expanded: Boolean) {
    val colors = LocalProgressPalette.current
    val color = colors.muted
    Canvas(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colors.raised)
            .padding(9.dp)
    ) {
        val stroke = 2.2.dp.toPx()
        val left = Offset(size.width * 0.18f, if (expanded) size.height * 0.62f else size.height * 0.38f)
        val center = Offset(size.width * 0.50f, if (expanded) size.height * 0.34f else size.height * 0.66f)
        val right = Offset(size.width * 0.82f, if (expanded) size.height * 0.62f else size.height * 0.38f)
        drawLine(color, left, center, strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, center, right, strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable
private fun ThumbnailStack(checkIn: ProgressCheckIn) {
    val colors = LocalProgressPalette.current
    Row(Modifier.width(74.dp), verticalAlignment = Alignment.CenterVertically) {
        ProgressAngle.entries.forEachIndexed { index, angle ->
            Box(
                Modifier.size(34.dp).then(if (index == 0) Modifier else Modifier.offset(x = (-10 * index).dp))
                    .clip(RoundedCornerShape(9.dp)).background(colors.raised)
                    .border(2.dp, colors.photoEdge, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                val uri = checkIn.uriFor(angle)
                if (uri != null) PrivatePhotoImage(uri, angle.label) else Text("+", color = colors.muted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun EditableAnglePhoto(
    uri: String?, label: String, modifier: Modifier = Modifier, onClick: () -> Unit, onRemove: () -> Unit
) {
    val colors = LocalProgressPalette.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box {
            Box(
                Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(12.dp))
                    .background(colors.muted.copy(alpha = 0.08f))
                    .border(1.dp, colors.muted.copy(alpha = 0.12f), RoundedCornerShape(12.dp)).clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                if (uri != null) PrivatePhotoImage(uri, label)
                else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("+", color = colors.muted)
                    Text(label, color = colors.muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (uri != null) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(5.dp).size(24.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.65f)).clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) { Text("×", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
        Text(label.uppercase(), color = colors.muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}


private fun milestoneMessage(checkIns: List<ProgressCheckIn>, start: Double?, goal: Double?): String? {
    if (start == null || goal == null || kotlin.math.abs(goal - start) < 0.01) return null
    val current = checkIns.filter { it.weightKg != null }.maxByOrNull { it.epochDay }?.weightKg ?: start
    val raw = if (goal < start) (start - current) / (start - goal) else (current - start) / (goal - start)
    val pct = (raw * 100.0).coerceIn(0.0, 100.0)
    val milestone = (pct.toInt() / 5) * 5
    if (milestone < 5) return null
    return if (milestone >= 100) "Goal reached — incredible consistency." else "$milestone% of the way to your goal — keep going."
}


@Composable
private fun PrivatePhotoImage(uri: String, label: String) {
    val context = LocalContext.current
    // Decode off the main thread. Synchronous ImageDecoder work here caused visible
    // scroll stalls when the milestone/goal card moved on or off screen.
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            decodeProgressBitmap(context, uri, maxDimension = 720)
        }
    }
    val currentBitmap = bitmap

    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap.asImageBitmap(),
            contentDescription = label,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun AnglePhoto(uri: String?, label: String, modifier: Modifier = Modifier) {
    val colors = LocalProgressPalette.current
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, uri) {
        value = uri?.let { photoUri ->
            withContext(Dispatchers.IO) {
                decodeProgressBitmap(context, photoUri, maxDimension = 720)
            }
        }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(12.dp)).background(colors.muted.copy(alpha = 0.08f)).border(1.dp, colors.muted.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            val currentBitmap = bitmap
            if (currentBitmap != null) Image(currentBitmap.asImageBitmap(), label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Text("—", color = colors.muted)
        }
        Text(label.uppercase(), color = colors.muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CompareCard(
    checkIns: List<ProgressCheckIn>, olderId: String?, newerId: String?, angle: ProgressAngle,
    onOlderChange: (String) -> Unit, onNewerChange: (String) -> Unit, onAngleChange: (ProgressAngle) -> Unit
) {
    val colors = LocalProgressPalette.current
    val sorted = checkIns.sortedBy { it.epochDay }
    val older = sorted.firstOrNull { it.id == olderId } ?: sorted.first()
    val newer = sorted.firstOrNull { it.id == newerId } ?: sorted.last()
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH) }
    Column(Modifier.fillMaxWidth().progressSurface(colors, 20.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Before vs Now", color = colors.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProgressAngle.entries.forEach { a -> FilterChip(a.label, a == angle) { onAngleChange(a) } }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text("BEFORE · ${LocalDate.ofEpochDay(older.epochDay).format(formatter)}", color = colors.muted, style = MaterialTheme.typography.labelSmall)
                AnglePhoto(older.uriFor(angle), angle.label)
            }
            Column(Modifier.weight(1f)) {
                Text("NOW · ${LocalDate.ofEpochDay(newer.epochDay).format(formatter)}", color = colors.muted, style = MaterialTheme.typography.labelSmall)
                AnglePhoto(newer.uriFor(angle), angle.label)
            }
        }
        Text("Choose check-ins", color = colors.muted, style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sorted.forEach { c ->
                val label = LocalDate.ofEpochDay(c.epochDay).format(formatter)
                FilterChip(label, c.id == older.id) { onOlderChange(c.id) }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sorted.forEach { c ->
                val label = LocalDate.ofEpochDay(c.epochDay).format(formatter)
                FilterChip(label, c.id == newer.id) { onNewerChange(c.id) }
            }
        }
    }
}

@Composable
private fun MilestoneRow(checkIns: List<ProgressCheckIn>, onSelect: (ProgressCheckIn, ProgressCheckIn) -> Unit) {
    val colors = LocalProgressPalette.current
    val sorted = checkIns.sortedBy { it.epochDay }
    val latest = sorted.last()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Milestone Comparison", color = colors.text, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("First vs Latest", false) { onSelect(sorted.first(), latest) }
            listOf(28L to "4 Weeks", 56L to "8 Weeks", 84L to "12 Weeks", 182L to "6 Months").forEach { (days, label) ->
                val target = latest.epochDay - days
                val older = sorted.filter { it.epochDay <= target }.maxByOrNull { it.epochDay }
                    ?: sorted.minByOrNull { kotlin.math.abs(it.epochDay - target) }
                FilterChip(label, false, enabled = older != null && older.id != latest.id) { if (older != null) onSelect(older, latest) }
            }
        }
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = LocalProgressPalette.current
    val accent = colors.selection
    Box(
        Modifier.clip(CircleShape)
            .background(if (selected) accent.copy(alpha = 0.18f) else colors.surface)
            .border(1.dp, if (selected) accent.copy(alpha = 0.6f) else colors.muted.copy(alpha = 0.16f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, color = if (!enabled) colors.muted.copy(alpha = 0.35f) else if (selected) accent else colors.muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}


@Composable
private fun GoalSetupDialog(
    initialGoal: String,
    initialCurrentWeight: String,
    initialTargetWeight: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    val colors = LocalProgressPalette.current
    var goal by rememberSaveable { mutableStateOf(initialGoal) }
    var current by rememberSaveable { mutableStateOf(initialCurrentWeight) }
    var target by rememberSaveable { mutableStateOf(initialTargetWeight) }
    val goals = listOf("Bulking", "Weight Gain", "Weight Loss", "Cutting")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("What's your goal?", color = colors.text, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Set up your physique progress tracker.", color = colors.muted)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    goals.forEach { item -> FilterChip(item, goal == item) { goal = item } }
                }
                OutlinedTextField(
                    value = current,
                    onValueChange = { v -> if (validWeightText(v)) current = v },
                    label = { Text("Current weight (kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { v -> if (validWeightText(v)) target = v },
                    label = { Text("Goal weight (kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = goal.isNotBlank() && current.toDoubleOrNull() != null && target.toDoubleOrNull() != null,
                onClick = { onSave(goal, current, target) }
            ) { Text("Start Tracking") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now", color = colors.muted, fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun validWeightText(value: String): Boolean =
    value.length <= 6 && value.all { it.isDigit() || it == '.' } && value.count { it == '.' } <= 1

private fun copyPhotoToPrivateStorage(context: Context, uri: Uri, name: String): String? {
    return runCatching {
        val dir = File(context.filesDir, "progress_photos").apply { mkdirs() }
        val file = File(dir, "$name.jpg")

        // Progress photos are UI/reference images, not originals. Saving a bounded copy keeps
        // scrolling smooth and prevents Cloud Backup from ballooning with multi-megabyte photos.
        val bitmap = decodeProgressBitmap(context, uri.toString(), maxDimension = 1600) ?: return null
        FileOutputStream(file).use { output ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 86, output)
        }
        Uri.fromFile(file).toString()
    }.getOrNull()
}

private fun decodeProgressBitmap(context: Context, uriString: String, maxDimension: Int): android.graphics.Bitmap? {
    return runCatching {
        val parsed = Uri.parse(uriString)
        val source = if (parsed.scheme == "file") {
            ImageDecoder.createSource(File(parsed.path!!))
        } else {
            ImageDecoder.createSource(context.contentResolver, parsed)
        }
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val width = info.size.width
            val height = info.size.height
            val largest = maxOf(width, height)
            if (largest > maxDimension) {
                val scale = maxDimension.toFloat() / largest.toFloat()
                decoder.setTargetSize(
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1)
                )
            }
        }
    }.getOrNull()
}


private fun detectPhotoDate(context: Context, uri: Uri): LocalDate? {
    // First try MediaStore's DATE_TAKEN. This preserves the camera capture date for normal gallery photos.
    runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Images.ImageColumns.DATE_TAKEN),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN)
                if (index >= 0 && !cursor.isNull(index)) {
                    val millis = cursor.getLong(index)
                    if (millis > 0L) {
                        return java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }
                }
            }
        }
    }

    // Some providers expose the last-modified time rather than DATE_TAKEN.
    runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                if (index >= 0 && !cursor.isNull(index)) {
                    val millis = cursor.getLong(index)
                    if (millis > 0L) {
                        return java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }
                }
            }
        }
    }
    return null
}

private fun deletePrivatePhoto(uri: String?) {
    if (uri.isNullOrBlank()) return
    runCatching {
        val parsed = Uri.parse(uri)
        if (parsed.scheme == "file") parsed.path?.let { File(it).delete() }
    }
}

private fun encodeCheckIn(c: ProgressCheckIn): String = listOf(
    c.id, c.epochDay.toString(), c.weightKg?.toString().orEmpty(), c.frontUri.orEmpty(), c.sideUri.orEmpty(), c.backUri.orEmpty()
).joinToString("\t")

private fun loadCheckIns(prefs: android.content.SharedPreferences): List<ProgressCheckIn> {
    val saved = prefs.getStringSet("checkins_v1", emptySet()).orEmpty().mapNotNull { raw ->
        val p = raw.split('\t')
        if (p.size < 6) null else ProgressCheckIn(p[0], p[1].toLongOrNull() ?: return@mapNotNull null, p[2].toDoubleOrNull(), p[3].ifBlank { null }, p[4].ifBlank { null }, p[5].ifBlank { null })
    }
    if (saved.isNotEmpty()) return saved.sortedByDescending { it.epochDay }

    // Preserve the old progress-photo library: each legacy photo becomes a check-in.
    return loadLegacyPhotos(prefs).mapIndexed { index, photo ->
        ProgressCheckIn("legacy-${photo.epochDay}-$index", photo.epochDay, null, frontUri = photo.uri)
    }.sortedByDescending { it.epochDay }
}

private fun loadLegacyPhotos(prefs: android.content.SharedPreferences): List<ProgressPhoto> {
    val saved = prefs.getStringSet("photos_v2", emptySet()).orEmpty().mapNotNull { raw ->
        val parts = raw.split('\t')
        val uri = parts.firstOrNull().orEmpty()
        val day = parts.getOrNull(1)?.toLongOrNull()
        if (uri.isBlank()) null else ProgressPhoto(uri, day ?: LocalDate.now().toEpochDay())
    }
    if (saved.isNotEmpty()) return saved.sortedByDescending { it.epochDay }
    return prefs.getStringSet("photos", emptySet()).orEmpty().map { ProgressPhoto(it, LocalDate.now().toEpochDay()) }
}

private fun formatWeight(value: Double): String = if (value == value.toLong().toDouble()) value.toLong().toString() else String.format(Locale.ENGLISH, "%.1f", value)