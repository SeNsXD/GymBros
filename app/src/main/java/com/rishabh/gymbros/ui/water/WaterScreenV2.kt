package com.rishabh.gymbros.ui.water

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.rishabh.gymbros.features.water.model.WaterEntry
import com.rishabh.gymbros.features.water.model.DailyWaterIntake
import com.rishabh.gymbros.features.settings.model.AppSettings
import com.rishabh.gymbros.features.settings.model.WaterUnit
import com.rishabh.gymbros.features.settings.model.formatVolume
import com.rishabh.gymbros.features.settings.model.volumeNumber
import com.rishabh.gymbros.features.settings.model.volumeUnitLabel
import com.rishabh.gymbros.features.water.model.WaterUiState
import com.rishabh.gymbros.ui.components.AnimatedWaterBottle
import com.rishabh.gymbros.ui.components.IgniteCard
import com.rishabh.gymbros.ui.components.PercentageChip
import com.rishabh.gymbros.ui.components.SectionHeader
import com.rishabh.gymbros.ui.theme.IgniteBrushes
import com.rishabh.gymbros.ui.theme.IgniteColors
import com.rishabh.gymbros.ui.theme.Ui2Colors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WaterScreenV2(
    state: WaterUiState,
    settings: AppSettings,
    onAddWater: (Int) -> Unit,
    onSubtractWater: (Int) -> Unit,
    onUndoLast: () -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderStartHourChange: (Int) -> Unit,
    onReminderEndHourChange: (Int) -> Unit,
    onReminderIntervalChange: (Int) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onQuickAddChange: (Int, Int) -> Unit,
    onQuickAddLabelChange: (Int, String) -> Unit,
    onWidgetButtonChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showWaterSettings by rememberSaveable { mutableStateOf(false) }
    var waterHistoryRange by rememberSaveable { mutableStateOf("7D") }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onReminderEnabledChange(granted)
    }

    val dateText = LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
    )

    if (showWaterSettings) {
        WaterSettingsDialog(
            goalMl = state.goalMl,
            quickAddPresets = settings.quickAddPresets.take(3),
            quickAddLabels = settings.quickAddLabels,
            onDismiss = { showWaterSettings = false },
            onSave = { goal, presets, labels ->
                onDailyGoalChange(goal)
                presets.forEachIndexed { index, value -> onQuickAddChange(index, value) }
                labels.forEachIndexed { index, label -> onQuickAddLabelChange(index, label) }
                // Button 2 is the shared positive quick-add amount used by Home and the Water widget.
                onWidgetButtonChange(presets.getOrElse(1) { 600 })
                showWaterSettings = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Ui2Colors.Background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Water",
                        color = Ui2Colors.Text,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = dateText,
                        color = Ui2Colors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Ui2Colors.Card)
                        .border(
                            1.dp,
                            Ui2Colors.Cyan.copy(alpha = 0.28f),
                            RoundedCornerShape(50)
                        )
                        .clickable { showWaterSettings = true }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Goal ${formatVolume(state.goalMl, settings.unit)}",
                        color = Ui2Colors.Cyan,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit water goal and quick buttons",
                        tint = com.rishabh.gymbros.ui.theme.Ui2Colors.Cyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AnimatedWaterBottleV2(progress = state.progress)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = volumeNumber(state.intakeMl, settings.unit),
                            color = Ui2Colors.Text,
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = volumeUnitLabel(settings.unit),
                            color = Ui2Colors.Cyan,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 5.dp)
                        )
                    }
                    Text(
                        text = "of ${formatVolume(state.goalMl, settings.unit)} today",
                        color = Ui2Colors.Muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    WaterProgressChipV2(
                        text = if (state.goalReached) "Goal hit" else "${state.progressPercent}% done",
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                val labels = settings.quickAddLabels
                settings.quickAddPresets.take(3).forEachIndexed { index, amount ->
                    QuickAddButton(
                        amountMl = amount,
                        unit = settings.unit,
                        label = if (index == 0) "Minus" else labels.getOrElse(index) { "Quick add" },
                        styleIndex = index,
                        modifier = Modifier.weight(1f),
                        onClick = if (index == 0) onSubtractWater else onAddWater
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            WaterHistoryRangeSelector(
                selected = waterHistoryRange,
                onSelected = { waterHistoryRange = it }
            )
            Spacer(Modifier.height(10.dp))
            WaterHistoryChart(
                days = state.weeklyIntake,
                goalMl = state.goalMl,
                unit = settings.unit,
                range = waterHistoryRange
            )

            Spacer(Modifier.height(22.dp))
            SectionHeader(title = "Reminders")
            Spacer(Modifier.height(10.dp))
            IgniteCard(modifier = Modifier.fillMaxWidth(), borderColor = Ui2Colors.Cyan.copy(alpha = 0.18f), brush = Brush.verticalGradient(listOf(Ui2Colors.Cyan.copy(alpha = 0.035f), Ui2Colors.Card, Ui2Colors.Card))) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = Ui2Colors.Cyan
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sip reminders",
                            color = Ui2Colors.Text,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (settings.reminderEnabled) "On" else "Off",
                            color = Ui2Colors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Switch(
                        checked = settings.reminderEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                onReminderEnabledChange(false)
                            } else if (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onReminderEnabledChange(true)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Ui2Colors.Background,
                            checkedTrackColor = Ui2Colors.Cyan,
                            uncheckedThumbColor = Ui2Colors.Muted,
                            uncheckedTrackColor = Ui2Colors.CardRaised
                        )
                    )
                }

                if (settings.reminderEnabled) {
                    Spacer(Modifier.height(18.dp))
                    ReminderTitle(
                        title = "Reminder window",
                        subtitle = "No reminder is shown after your goal is hit"
                    )
                    Spacer(Modifier.height(12.dp))
                    ReminderHourRow(
                        label = "Start",
                        hour = settings.reminderStartHour,
                        onMinus = { onReminderStartHourChange(settings.reminderStartHour - 1) },
                        onPlus = { onReminderStartHourChange(settings.reminderStartHour + 1) }
                    )
                    Spacer(Modifier.height(9.dp))
                    ReminderHourRow(
                        label = "End",
                        hour = settings.reminderEndHour,
                        onMinus = { onReminderEndHourChange(settings.reminderEndHour - 1) },
                        onPlus = { onReminderEndHourChange(settings.reminderEndHour + 1) }
                    )
                    Spacer(Modifier.height(9.dp))
                    ReminderStepperRow(
                        label = "Repeat every",
                        value = "${settings.reminderIntervalHours} hr",
                        onMinus = { onReminderIntervalChange(settings.reminderIntervalHours - 1) },
                        onPlus = { onReminderIntervalChange(settings.reminderIntervalHours + 1) }
                    )
                }
            }
            Text(
                text = "Reminders are scheduled locally on this device.",
                color = Ui2Colors.Muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(22.dp))
            SectionHeader(
                title = "Today's log",
                action = {
                    if (state.todayEntries.isNotEmpty()) {
                        Text(
                            text = "Undo last",
                            color = Ui2Colors.Cyan,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .clickable(onClick = onUndoLast)
                                .padding(horizontal = 4.dp, vertical = 12.dp)
                        )
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
        }

        if (state.todayEntries.isEmpty()) {
            item {
                IgniteCard(modifier = Modifier.fillMaxWidth(), borderColor = Ui2Colors.Cyan.copy(alpha = 0.18f), brush = Brush.verticalGradient(listOf(Ui2Colors.Cyan.copy(alpha = 0.035f), Ui2Colors.Card, Ui2Colors.Card))) {
                    Text(
                        text = "No water logged yet",
                        color = Ui2Colors.Text,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Use a quick-add button to start today's hydration log.",
                        color = Ui2Colors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        } else {
            items(
                items = state.todayEntries,
                key = { it.id }
            ) { entry ->
                WaterLogRow(entry = entry, unit = settings.unit)
                Spacer(Modifier.height(7.dp))
            }
        }
    }
}



@Composable
private fun WaterProgressChipV2(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Ui2Colors.Cyan,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Ui2Colors.Cyan.copy(alpha = .10f))
            .border(1.dp, Ui2Colors.Cyan.copy(alpha = .22f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun AnimatedWaterBottleV2(progress: Float) {
    val context = LocalContext.current
    val animationsEnabled = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        }.getOrDefault(true)
    }
    val animatedProgress = if (animationsEnabled) {
        val value by animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 700),
            label = "water-v2-fill"
        )
        value
    } else progress.coerceIn(0f, 1f)

    val wavePhase = if (animationsEnabled && animatedProgress > 0f) {
        val transition = rememberInfiniteTransition(label = "water-v2-wave")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2.0).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "water-v2-wave-phase"
        )
        phase
    } else 0f

    Column {
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(13.dp)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                .background(Ui2Colors.CardRaised)
                .border(1.dp, Ui2Colors.Cyan.copy(alpha = .28f), RoundedCornerShape(5.dp))
        )
        val bottleShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
        Box(
            modifier = Modifier
                .width(92.dp)
                .height(171.dp)
                .clip(bottleShape)
                .background(Ui2Colors.Card)
                .border(2.dp, Ui2Colors.Cyan.copy(alpha = .55f), bottleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (animatedProgress > 0f) {
                    val waterTop = size.height * (1f - animatedProgress)
                    val amplitude = 5.dp.toPx()
                    val wavePath = Path().apply {
                        moveTo(0f, waterTop)
                        val steps = 36
                        for (step in 0..steps) {
                            val x = size.width * step / steps.toFloat()
                            val angle = (step / steps.toFloat()) * (Math.PI * 4.0) + wavePhase
                            val y = waterTop + kotlin.math.sin(angle).toFloat() * amplitude
                            lineTo(x, y)
                        }
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(
                        path = wavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Ui2Colors.Cyan, Ui2Colors.Water),
                            startY = waterTop,
                            endY = size.height
                        )
                    )
                }

                val tickColor = Color.White.copy(alpha = .26f)
                listOf(.20f, .40f, .60f, .80f).forEach { fraction ->
                    val y = size.height * fraction
                    drawLine(
                        color = tickColor,
                        start = Offset(size.width - 13.dp.toPx(), y),
                        end = Offset(size.width - 5.dp.toPx(), y),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
private fun WaterSettingsDialog(
    goalMl: Int,
    quickAddPresets: List<Int>,
    quickAddLabels: List<String>,
    onDismiss: () -> Unit,
    onSave: (Int, List<Int>, List<String>) -> Unit
) {
    com.rishabh.gymbros.ui.theme.Ui2Theme {

        var goalText by remember(goalMl) { mutableStateOf(goalMl.toString()) }
        val initialPresets = List(3) { index -> quickAddPresets.getOrElse(index) { if (index == 0) 600 else 250 } }
        val initialLabels = List(3) { index ->
            if (index == 0) "Minus" else quickAddLabels.getOrElse(index) { "Quick add" }
        }
        var amount0 by remember(initialPresets) { mutableStateOf(initialPresets[0].toString()) }
        var amount1 by remember(initialPresets) { mutableStateOf(initialPresets[1].toString()) }
        var amount2 by remember(initialPresets) { mutableStateOf(initialPresets[2].toString()) }
        var label1 by remember(initialLabels) { mutableStateOf(initialLabels[1]) }
        var label2 by remember(initialLabels) { mutableStateOf(initialLabels[2]) }

        val parsedGoal = goalText.toIntOrNull()
        val parsedAmounts = listOf(amount0, amount1, amount2).map { it.toIntOrNull() }
        val valid = parsedGoal != null && parsedGoal in 500..5000 &&
                parsedAmounts.all { it != null && it in 50..5000 } &&
                label1.isNotBlank() && label2.isNotBlank()

        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Ui2Colors.Card,
            shape = RoundedCornerShape(22.dp),
            title = {
                Column {
                    Text(
                        text = "Water Settings",
                        color = Ui2Colors.Text,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Change your daily goal and quick buttons.",
                        color = Ui2Colors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Daily water goal",
                        color = Ui2Colors.Primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = it.filter(Char::isDigit).take(5) },
                        label = { Text("Goal (ml)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Quick add buttons",
                        color = Ui2Colors.Primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Button 2 also controls the quick-add amount on Home and the Water widget.",
                        color = Ui2Colors.Muted,
                        style = MaterialTheme.typography.bodySmall
                    )

                    WaterQuickButtonEditor(
                        label = "Minus",
                        labelValue = "Minus",
                        amountValue = amount0,
                        onLabelChange = {},
                        onAmountChange = { amount0 = it },
                        labelEditable = false
                    )
                    WaterQuickButtonEditor(
                        label = "Button 2",
                        labelValue = label1,
                        amountValue = amount1,
                        onLabelChange = { label1 = it.take(14) },
                        onAmountChange = { amount1 = it }
                    )
                    WaterQuickButtonEditor(
                        label = "Button 3",
                        labelValue = label2,
                        amountValue = amount2,
                        onLabelChange = { label2 = it.take(14) },
                        onAmountChange = { amount2 = it }
                    )

                    Text(
                        text = "Goal: 500–5,000 ml • Buttons: 50–5,000 ml",
                        color = Ui2Colors.Muted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Ui2Colors.Muted)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = {
                        onSave(
                            parsedGoal!!,
                            parsedAmounts.map { it!! },
                            listOf("Minus", label1.trim(), label2.trim())
                        )
                    }
                ) {
                    Text(
                        text = "Save",
                        color = if (valid) Ui2Colors.Primary else Ui2Colors.Muted
                    )
                }
            }
        )
    }
}

@Composable
private fun WaterQuickButtonEditor(
    label: String,
    labelValue: String,
    amountValue: String,
    onLabelChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    labelEditable: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = Ui2Colors.Muted,
            style = MaterialTheme.typography.labelSmall
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = labelValue,
                onValueChange = onLabelChange,
                enabled = labelEditable,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = amountValue,
                onValueChange = { onAmountChange(it.filter(Char::isDigit).take(4)) },
                label = { Text("ml") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(105.dp)
            )
        }
    }
}


@Composable
private fun WaterHistoryRangeSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    val options = listOf("7D" to "7 days", "30D" to "30 days", "3M" to "3 months")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ui2Colors.Card),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (key, label) ->
            val active = selected == key
            Text(
                text = label,
                color = if (active) Ui2Colors.Background else Ui2Colors.Muted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) Ui2Colors.Cyan else Color.Transparent)
                    .clickable { onSelected(key) }
                    .padding(vertical = 9.dp)
            )
        }
    }
}

// ---- Water history chart colors (by daily intake) ----
private val WaterPink = Color(0xFFFF5C7A)
private val WaterPurple = Color(0xFFA78BFA)
private const val WATER_LOW_MAX_ML = 2000   // below this -> pink
private const val WATER_MID_MAX_ML = 3000   // 2000..2999 -> purple, 3000+ -> cyan

private fun waterBarColor(intakeMl: Int, isToday: Boolean): Color = when {
    isToday -> Ui2Colors.Lime               // current day always stays green
    intakeMl < WATER_LOW_MAX_ML -> WaterPink
    intakeMl < WATER_MID_MAX_ML -> WaterPurple
    else -> Ui2Colors.Cyan
}

private data class WaterBar(
    val axisLabel: String,
    val axisSub: String,
    val intakeMl: Int,
    val isToday: Boolean
)

@Composable
private fun WaterHistoryChart(
    days: List<DailyWaterIntake>,
    goalMl: Int,
    unit: WaterUnit,
    range: String
) {
    val today = LocalDate.now()
    // 91 = 13 full weeks, so every 3M bar covers exactly 7 days.
    val requestedDays = when (range) {
        "30D" -> 30
        "3M" -> 91
        else -> 7
    }

    // Fill missing dates with zero so the selected period remains continuous.
    val byEpoch = days.associateBy { it.epochDay }
    val daily = (requestedDays - 1 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        byEpoch[date.toEpochDay()] ?: DailyWaterIntake(date.toEpochDay(), 0)
    }

    val monthFmt = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

    val bars: List<WaterBar> = if (range == "3M") {
        // One bar per week: big number = day the week starts, small text = month.
        daily.chunked(7).map { week ->
            val start = LocalDate.ofEpochDay(week.first().epochDay)
            WaterBar(
                axisLabel = start.dayOfMonth.toString(),
                axisSub = start.format(monthFmt),
                intakeMl = week.sumOf { it.intakeMl } / week.size,
                isToday = false
            )
        }
    } else {
        daily.mapIndexed { i, item ->
            val date = LocalDate.ofEpochDay(item.epochDay)
            val isToday = i == daily.lastIndex
            WaterBar(
                axisLabel = if (range == "7D")
                    date.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)).take(2)
                else date.dayOfMonth.toString(),
                axisSub = when {
                    range == "7D" -> ""
                    isToday -> "Today"
                    i == 0 || date.dayOfMonth == 1 -> date.format(monthFmt)
                    else -> " "
                },
                intakeMl = item.intakeMl,
                isToday = isToday
            )
        }
    }

    val title = when (range) {
        "30D" -> "Last 30 days"
        "3M" -> "Last 3 months"
        else -> "Last 7 days"
    }

    // 30D / 3M use the same big bars + values as the 7-day view and scroll
    // sideways, opening on the most recent days.
    val scrollable = range != "7D"
    val scrollState = rememberScrollState()
    LaunchedEffect(range, scrollState.maxValue) {
        if (scrollable) scrollState.scrollTo(scrollState.maxValue)
    }

    IgniteCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = Ui2Colors.Cyan.copy(alpha = 0.16f),
        brush = Brush.verticalGradient(
            listOf(Ui2Colors.Cyan.copy(alpha = 0.03f), Ui2Colors.Card, Ui2Colors.Card)
        )
    ) {
        Text(
            text = title,
            color = Ui2Colors.Text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = when (range) {
                "3M" -> "Each bar = 1 week average • Goal ${formatVolume(goalMl, unit)} per day"
                else -> "Goal ${formatVolume(goalMl, unit)} per day"
            },
            color = Ui2Colors.Muted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (scrollable) Modifier.horizontalScroll(scrollState) else Modifier)
                .height(if (scrollable) 175.dp else 150.dp),
            horizontalArrangement = Arrangement.spacedBy(if (scrollable) 0.dp else 7.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEach { bar ->
                val ratio = if (goalMl <= 0) 0f
                else (bar.intakeMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)
                val noData = bar.intakeMl == 0 && !bar.isToday
                val barColor = waterBarColor(bar.intakeMl, bar.isToday)
                    .copy(alpha = if (noData) 0.35f else 1f)
                val columnModifier = if (scrollable) Modifier.width(32.dp) else Modifier.weight(1f)

                Column(
                    modifier = columnModifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Amount above each bar (hidden for empty days in 30D/3M to avoid noise).
                    Text(
                        text = if (bar.intakeMl > 0 || !scrollable) volumeNumber(bar.intakeMl, unit) else "",
                        color = waterBarColor(bar.intakeMl, bar.isToday),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.wrapContentWidth(unbounded = true)
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .width(if (scrollable) 16.dp else 18.dp)
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Ui2Colors.QuestEmpty),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(ratio.coerceAtLeast(0.025f))
                                .background(barColor)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = bar.axisLabel,
                        color = if (bar.isToday) Ui2Colors.Lime else Ui2Colors.Cyan.copy(alpha = .78f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                    if (scrollable) {
                        Text(
                            text = bar.axisSub,
                            color = if (bar.isToday) Ui2Colors.Lime else Ui2Colors.Muted,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.wrapContentWidth(unbounded = true)
                        )
                    }
                }
            }
        }

        // Summary numbers make 30D / 3M readable at a glance.
        if (scrollable) {
            val logged = daily.filter { it.intakeMl > 0 }
            val avg = if (logged.isEmpty()) 0 else logged.sumOf { it.intakeMl } / logged.size
            val goalDays = daily.count { goalMl > 0 && it.intakeMl >= goalMl }
            val best = daily.maxOfOrNull { it.intakeMl } ?: 0

            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                WaterStat("Daily avg", formatVolume(avg, unit), Modifier.weight(1f))
                WaterStat("Goal reached", "$goalDays / $requestedDays days", Modifier.weight(1f))
                WaterStat("Best day", formatVolume(best, unit), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WaterStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            color = Ui2Colors.Text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = label,
            color = Ui2Colors.Muted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun QuickAddButton(
    amountMl: Int,
    unit: WaterUnit,
    label: String,
    styleIndex: Int,
    modifier: Modifier = Modifier,
    onClick: (Int) -> Unit
) {
    val visual = when (styleIndex % 3) {
        // Minus: GymBros danger / red-pink
        0 -> QuickAddVisual(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFA2A55).copy(alpha = 0.20f),
                    Ui2Colors.Card
                )
            ),
            border = Color(0xFFFA2A55).copy(alpha = 0.46f),
            accent = Color(0xFFFA2A55)
        )

        // Primary quick add: blue/cyan
        1 -> QuickAddVisual(
            brush = Brush.linearGradient(
                colors = listOf(
                    Ui2Colors.Cyan.copy(alpha = 0.20f),
                    Ui2Colors.Card
                )
            ),
            border = Ui2Colors.Cyan.copy(alpha = 0.46f),
            accent = Ui2Colors.Cyan
        )

        // Secondary quick add: green
        else -> QuickAddVisual(
            brush = Brush.linearGradient(
                colors = listOf(
                    Ui2Colors.Primary.copy(alpha = 0.20f),
                    Ui2Colors.Card
                )
            ),
            border = Ui2Colors.Primary.copy(alpha = 0.46f),
            accent = Ui2Colors.Primary
        )
    }

    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(visual.brush)
            .border(BorderStroke(1.dp, visual.border), shape)
            .clickable { onClick(amountMl) }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (styleIndex == 0) "−${volumeNumber(amountMl, unit)}" else "+${volumeNumber(amountMl, unit)}",
            color = Ui2Colors.Text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = label,
            color = visual.accent.copy(alpha = 0.90f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 1.dp),
            maxLines = 1
        )
    }
}

private data class QuickAddVisual(
    val brush: Brush,
    val border: Color,
    val accent: Color
)

@Composable
private fun ReminderTitle(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = Ui2Colors.Cyan
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Ui2Colors.Text,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                color = Ui2Colors.Muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ReminderHourRow(
    label: String,
    hour: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val time = java.time.LocalTime.of(hour.coerceIn(0, 23), 0)
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
    ReminderStepperRow(label, time, onMinus, onPlus)
}

@Composable
private fun ReminderStepperRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Ui2Colors.Text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        ReminderStepButton("−", onMinus)
        Text(
            text = value,
            color = Ui2Colors.Cyan,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(90.dp)
        )
        ReminderStepButton("+", onPlus)
    }
}

@Composable
private fun ReminderStepButton(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = Ui2Colors.Text,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Ui2Colors.CardRaised)
            .border(1.dp, Ui2Colors.CardOutline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(top = 7.dp)
    )
}

@Composable
private fun WaterLogRow(entry: WaterEntry, unit: WaterUnit) {
    val timeText = remember(entry.timestamp) {
        Instant.ofEpochMilli(entry.timestamp)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
    }

    IgniteCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 11.dp),
        brush = Brush.linearGradient(
            listOf(Ui2Colors.Card, Ui2Colors.Card)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timeText,
                color = Ui2Colors.Muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "+${formatVolume(entry.amountMl, unit)}",
                color = Ui2Colors.Cyan,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}