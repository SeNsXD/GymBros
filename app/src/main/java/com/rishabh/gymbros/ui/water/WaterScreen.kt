package com.rishabh.gymbros.ui.water

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WaterScreen(
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
            .background(IgniteBrushes.Screen)
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
                        color = IgniteColors.Text,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = dateText,
                        color = IgniteColors.MutedText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(IgniteColors.Surface2)
                        .border(
                            1.dp,
                            IgniteColors.Gold.copy(alpha = 0.24f),
                            RoundedCornerShape(50)
                        )
                        .clickable { showWaterSettings = true }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Goal ${formatVolume(state.goalMl, settings.unit)}",
                        color = IgniteColors.Gold,
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
                AnimatedWaterBottle(progress = state.progress)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = volumeNumber(state.intakeMl, settings.unit),
                            color = IgniteColors.Text,
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = volumeUnitLabel(settings.unit),
                            color = IgniteColors.Gold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 5.dp)
                        )
                    }
                    Text(
                        text = "of ${formatVolume(state.goalMl, settings.unit)} today",
                        color = IgniteColors.MutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    PercentageChip(
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
            SectionHeader(title = "Weekly intake")
            Spacer(Modifier.height(10.dp))
            WeeklyWaterChart(
                days = state.weeklyIntake,
                goalMl = state.goalMl,
                unit = settings.unit
            )

            Spacer(Modifier.height(22.dp))
            SectionHeader(title = "Reminders")
            Spacer(Modifier.height(10.dp))
            IgniteCard(modifier = Modifier.fillMaxWidth(), borderColor = IgniteColors.Gold.copy(alpha = 0.10f), brush = Brush.verticalGradient(listOf(IgniteColors.Gold.copy(alpha = 0.025f), IgniteColors.Surface2, IgniteColors.Surface2))) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = IgniteColors.Gold
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sip reminders",
                            color = IgniteColors.Text,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (settings.reminderEnabled) "On" else "Off",
                            color = IgniteColors.MutedText,
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
                            checkedThumbColor = IgniteColors.TextOnGradient,
                            checkedTrackColor = IgniteColors.Gold,
                            uncheckedThumbColor = IgniteColors.MutedText,
                            uncheckedTrackColor = IgniteColors.Surface3
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
                color = IgniteColors.MutedText,
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
                            color = IgniteColors.Gold,
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
                IgniteCard(modifier = Modifier.fillMaxWidth(), borderColor = IgniteColors.Gold.copy(alpha = 0.10f), brush = Brush.verticalGradient(listOf(IgniteColors.Gold.copy(alpha = 0.025f), IgniteColors.Surface2, IgniteColors.Surface2))) {
                    Text(
                        text = "No water logged yet",
                        color = IgniteColors.Text,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Use a quick-add button to start today's hydration log.",
                        color = IgniteColors.MutedText,
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
private fun WaterSettingsDialog(
    goalMl: Int,
    quickAddPresets: List<Int>,
    quickAddLabels: List<String>,
    onDismiss: () -> Unit,
    onSave: (Int, List<Int>, List<String>) -> Unit
) {
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
    val valid = parsedGoal != null && parsedGoal in 250..10000 &&
            parsedAmounts.all { it != null && it in 50..5000 } &&
            label1.isNotBlank() && label2.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IgniteColors.Surface2,
        shape = RoundedCornerShape(22.dp),
        title = {
            Column {
                Text(
                    text = "Water Settings",
                    color = IgniteColors.Text,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Change your daily goal and quick buttons.",
                    color = IgniteColors.MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Daily water goal",
                    color = IgniteColors.Gold,
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
                    color = IgniteColors.Gold,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Button 2 also controls the quick-add amount on Home and the Water widget.",
                    color = IgniteColors.MutedText,
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
                    text = "Goal: 250–10,000 ml • Buttons: 50–5,000 ml",
                    color = IgniteColors.MutedText,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = IgniteColors.MutedText)
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
                    color = if (valid) IgniteColors.Gold else IgniteColors.MutedText
                )
            }
        }
    )
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
            color = IgniteColors.MutedText,
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
private fun WeeklyWaterChart(
    days: List<DailyWaterIntake>,
    goalMl: Int,
    unit: WaterUnit
) {
    val chartDays = if (days.isNotEmpty()) days.takeLast(7) else (6L downTo 0L).map {
        DailyWaterIntake(LocalDate.now().minusDays(it).toEpochDay(), 0)
    }
    IgniteCard(modifier = Modifier.fillMaxWidth(), borderColor = IgniteColors.Gold.copy(alpha = 0.10f), brush = Brush.verticalGradient(listOf(IgniteColors.Gold.copy(alpha = 0.025f), IgniteColors.Surface2, IgniteColors.Surface2))) {
        Text(
            text = "Last 7 days",
            color = IgniteColors.Text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Goal ${formatVolume(goalMl, unit)} per day",
            color = IgniteColors.MutedText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            chartDays.forEach { item ->
                val day = LocalDate.ofEpochDay(item.epochDay)
                val ratio = if (goalMl <= 0) 0f else (item.intakeMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = volumeNumber(item.intakeMl, unit),
                        color = IgniteColors.MutedText,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IgniteColors.Surface3),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(ratio.coerceAtLeast(0.025f))
                                .background(IgniteColors.Gold)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = day.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)).take(2),
                        color = if (day == LocalDate.now()) IgniteColors.Gold else IgniteColors.MutedText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (day == LocalDate.now()) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
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
        0 -> QuickAddVisualV1(
            brush = Brush.linearGradient(
                colors = listOf(
                    IgniteColors.Ember.copy(alpha = 0.20f),
                    IgniteColors.Surface2
                )
            ),
            border = IgniteColors.Ember.copy(alpha = 0.46f),
            accent = IgniteColors.Ember
        )

        1 -> QuickAddVisualV1(
            brush = Brush.linearGradient(
                colors = listOf(
                    IgniteColors.Gold.copy(alpha = 0.16f),
                    IgniteColors.Surface2
                )
            ),
            border = IgniteColors.Gold.copy(alpha = 0.40f),
            accent = IgniteColors.Gold
        )

        else -> QuickAddVisualV1(
            brush = Brush.linearGradient(
                colors = listOf(
                    IgniteColors.DeepEmber.copy(alpha = 0.24f),
                    IgniteColors.Surface2
                )
            ),
            border = IgniteColors.DeepEmber.copy(alpha = 0.52f),
            accent = IgniteColors.Gold
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
            color = IgniteColors.Text,
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

private data class QuickAddVisualV1(
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
            tint = IgniteColors.Gold
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = IgniteColors.Text,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                color = IgniteColors.MutedText,
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
            color = IgniteColors.Text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        ReminderStepButton("−", onMinus)
        Text(
            text = value,
            color = IgniteColors.Gold,
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
        color = IgniteColors.Text,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(IgniteColors.Surface3)
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
            listOf(IgniteColors.Surface2, IgniteColors.Surface2)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timeText,
                color = IgniteColors.MutedText,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "+${formatVolume(entry.amountMl, unit)}",
                color = IgniteColors.Gold,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
