package com.rishabh.gymbros.ui.checklist

import android.app.TimePickerDialog
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.app.AlarmManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.rishabh.gymbros.features.checklist.*
import com.rishabh.gymbros.ui.theme.IgniteColors
import com.rishabh.gymbros.ui.theme.Ui2Colors
import com.rishabh.gymbros.ui.theme.Ui2Theme
import com.rishabh.gymbros.features.settings.model.UiVersion
import java.util.Locale

@Composable
fun GymChecklistDialog(onDismiss: () -> Unit) {
    val maxHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.7f
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = IgniteColors.Background) {
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Done", color = IgniteColors.Gold) }
                }
                GymChecklistScreen(Modifier.weight(1f, fill = false))
            }
        }
    }
}

private data class ChecklistPalette(
    val isV2: Boolean,
    val background: Color,
    val surface: Color,
    val raised: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val dim: Color,
    val outline: Color
)

private fun classicChecklistPalette() = ChecklistPalette(
    false, IgniteColors.Background, IgniteColors.Surface2, IgniteColors.Surface3,
    IgniteColors.Text, IgniteColors.MutedText, IgniteColors.Gold, IgniteColors.Dimmed, IgniteColors.Hairline
)
private val LocalChecklistPalette = staticCompositionLocalOf { classicChecklistPalette() }

/** The classic dialog and V2 page share the same checklist storage and reminder actions. */
@Composable
fun GymChecklistScreen(
    modifier: Modifier = Modifier,
    uiVersion: UiVersion = UiVersion.V1,
    onBack: (() -> Unit)? = null
) {
    val colors = if (uiVersion == UiVersion.V2) ChecklistPalette(
        true, Ui2Colors.Background, Ui2Colors.Card, Ui2Colors.CardRaised,
        Ui2Colors.Text, Ui2Colors.Muted, Ui2Colors.Primary, Ui2Colors.Dim, Ui2Colors.CardOutline
    ) else classicChecklistPalette()
    CompositionLocalProvider(LocalChecklistPalette provides colors) {
        if (colors.isV2) Ui2Theme { GymChecklistContent(modifier, onBack) }
        else GymChecklistContent(modifier, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GymChecklistContent(
    modifier: Modifier,
    onBack: (() -> Unit)?
) {
    val colors = LocalChecklistPalette.current
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val store = remember {
        GymChecklistStore(context)
    }

    var list by remember {
        mutableStateOf(store.items())
    }

    var settings by remember {
        mutableStateOf(store.settings())
    }

    var newItem by remember {
        mutableStateOf("")
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) GymChecklistScheduler.reschedule(context)
    }

    val exactAlarmPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        GymChecklistScheduler.reschedule(context)
    }

    fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                runCatching {
                    exactAlarmPermissionLauncher.launch(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                }
            }
        }
    }

    LaunchedEffect(settings.enabled) {
        if (settings.enabled && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (settings.enabled) {
            GymChecklistScheduler.reschedule(context)
        }
    }

    val dayNames = listOf(
        "M",
        "T",
        "W",
        "T",
        "F",
        "S",
        "S"
    )

    fun persistItems(
        value: List<GymChecklistItem>
    ) {
        list = value
        store.saveItems(value)
    }

    fun persistSettings(
        value: GymChecklistSettings
    ) {
        settings = value
        store.saveSettings(value)
        GymChecklistScheduler.reschedule(context)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = if (colors.isV2) 18.dp else 16.dp),
        contentPadding = PaddingValues(
            vertical = if (colors.isV2) 20.dp else 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(
            12.dp
        )
    ) {

        if (colors.isV2) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to Home", tint = colors.text)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Text("Gym Checklist", color = colors.text, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        /*
         * GYM BAG CHECKLIST
         */
        item {

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface
                ),
                shape = RoundedCornerShape(if (colors.isV2) 22.dp else 16.dp),
                border = if (colors.isV2) androidx.compose.foundation.BorderStroke(0.5.dp, colors.outline) else null
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "GYM BAG",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${list.count { it.checked }} of ${list.size} packed",
                            color = colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )
                    LinearProgressIndicator(
                        progress = { if (list.isEmpty()) 0f else list.count { it.checked }.toFloat() / list.size },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = colors.accent,
                        trackColor = colors.raised,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                    Spacer(Modifier.height(10.dp))

                    /*
                     * CHECKLIST ITEMS
                     */
                    list.forEach { item ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            ChecklistCheckbox(
                                checked = item.checked,
                                onCheckedChange = { checked ->

                                    persistItems(
                                        list.map {

                                            if (it.id == item.id) {
                                                it.copy(
                                                    checked = checked
                                                )
                                            } else {
                                                it
                                            }
                                        }
                                    )
                                }
                            )

                            Text(
                                text = item.name,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {

                                    persistItems(
                                        list.filterNot {
                                            it.id == item.id
                                        }
                                    )
                                }
                            ) {

                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    tint = com.rishabh.gymbros.ui.theme.Ui2Colors.Danger,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    /*
                     * ADD ITEM
                     */
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.raised)
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        BasicTextField(
                            value = newItem,
                            onValueChange = {
                                newItem = it
                            },
                            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.text),
                            cursorBrush = SolidColor(colors.accent),
                            decorationBox = { input ->
                                Box {
                                    if (newItem.isEmpty()) Text("Add item", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                                    input()
                                }
                            },
                            singleLine = true
                        )

                        IconButton(
                            modifier = Modifier.size(48.dp),
                            onClick = {

                                val name = newItem.trim()

                                if (name.isNotEmpty()) {

                                    persistItems(
                                        list + GymChecklistItem(
                                            id = System.currentTimeMillis(),
                                            name = name,
                                            checked = false
                                        )
                                    )

                                    newItem = ""
                                }
                            }
                        ) {

                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Add item",
                                tint = colors.background,
                                modifier = Modifier.size(32.dp)
                                    .background(colors.accent, RoundedCornerShape(8.dp)).padding(6.dp)
                            )
                        }
                    }

                    /*
                     * RESET
                     */
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.accent),
                        onClick = {

                            persistItems(
                                list.map {
                                    it.copy(
                                        checked = false
                                    )
                                }
                            )
                        }
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Reset checklist"
                        )
                    }
                }
            }
        }

        /*
         * REMINDER CARD
         */
        item {

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface
                ),
                shape = RoundedCornerShape(if (colors.isV2) 22.dp else 16.dp),
                border = if (colors.isV2) androidx.compose.foundation.BorderStroke(0.5.dp, colors.outline) else null
            ) {

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(
                        12.dp
                    )
                ) {

                    /*
                     * REMINDER SWITCH
                     */
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Gym reminder",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Remind me about my checklist",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = settings.enabled,
                            onCheckedChange = { enabled ->
                                persistSettings(settings.copy(enabled = enabled))
                                if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else if (enabled) {
                                    requestExactAlarmPermissionIfNeeded()
                                }
                            }
                        )
                    }

                    /*
                     * DAYS
                     */
                    Text(
                        text = "GYM DAYS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {

                        dayNames.forEachIndexed { index, name ->

                            val day = index + 1

                            val selected =
                                settings.days.contains(day)

                            Surface(
                                modifier = Modifier.weight(1f).aspectRatio(1f).clip(CircleShape).clickable {
                                    val newDays = settings.days.toMutableSet().apply {
                                        if (contains(day)) remove(day) else add(day)
                                    }.toSet()
                                    persistSettings(settings.copy(days = newDays))
                                },
                                shape = CircleShape,
                                color = if (selected) colors.accent else colors.raised,
                                contentColor = if (selected) colors.background else colors.muted,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) colors.accent else colors.dim.copy(alpha = 0.5f)
                                )
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(text = name, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    /*
                     * TIME PICKER
                     */
                    HorizontalDivider(thickness = 0.5.dp, color = colors.outline)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {

                                if (colors.isV2) showTimePicker = true
                                else TimePickerDialog(
                                    context,
                                    { _, hour, minute ->

                                        persistSettings(
                                            settings.copy(
                                                hour = hour,
                                                minute = minute
                                            )
                                        )
                                    },
                                    settings.hour,
                                    settings.minute,
                                    false
                                ).show()
                            }
                            .padding(
                                vertical = 10.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "Reminder time"
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = String.format(
                                    Locale.getDefault(),
                                    "%d:%02d %s",
                                    if (
                                        settings.hour % 12 == 0
                                    ) {
                                        12
                                    } else {
                                        settings.hour % 12
                                    },
                                    settings.minute,
                                    if (
                                        settings.hour < 12
                                    ) {
                                        "AM"
                                    } else {
                                        "PM"
                                    }
                                ),
                                color = colors.accent,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null,
                                tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }

                    /*
                     * REMINDER INFO
                     */
                    Text(
                        text = "At reminder time GymBros will show what is still unpacked. Tapping the notification opens this checklist.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = settings.hour, initialMinute = settings.minute, is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = colors.surface,
            title = { Text("Reminder time", color = colors.text) },
            text = { TimeInput(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    persistSettings(settings.copy(hour = timeState.hour, minute = timeState.minute))
                    showTimePicker = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

}

@Composable
private fun ChecklistCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalChecklistPalette.current
    Box(
        modifier = Modifier.size(48.dp).toggleable(
            value = checked, role = Role.Checkbox, onValueChange = onCheckedChange
        ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(22.dp)
                .background(if (checked) colors.accent else Color.Transparent, RoundedCornerShape(6.dp))
                .border(1.5.dp, if (checked) colors.accent else colors.dim.copy(alpha = 0.65f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Icon(Icons.Outlined.Check, contentDescription = null,
                tint = colors.background, modifier = Modifier.size(16.dp))
        }
    }
}
