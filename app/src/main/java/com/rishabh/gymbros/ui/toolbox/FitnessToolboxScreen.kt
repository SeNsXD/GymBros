package com.rishabh.gymbros.ui.toolbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rishabh.gymbros.features.settings.model.AppSettings
import com.rishabh.gymbros.features.settings.model.UiVersion
import com.rishabh.gymbros.ui.theme.IgniteBrushes
import com.rishabh.gymbros.ui.theme.IgniteColors
import com.rishabh.gymbros.ui.theme.Ui2Colors
import com.rishabh.gymbros.ui.theme.Ui2Theme
import com.rishabh.gymbros.features.toolbox.*

typealias ToolboxCalculator = FitnessTool

/** Shared visual tokens so the classic (Ignite) and V2 surfaces can reuse one set of composables. */
private data class ToolboxPalette(
    val isV2: Boolean,
    val card: Color,
    val raised: Color,
    val outline: Color,
    val text: Color,
    val muted: Color,
    val dim: Color
)

private fun classicToolboxPalette() = ToolboxPalette(
    isV2 = false,
    card = IgniteColors.Surface2,
    raised = IgniteColors.Surface3,
    outline = IgniteColors.Hairline,
    text = IgniteColors.Text,
    muted = IgniteColors.MutedText,
    dim = IgniteColors.MutedText
)

private fun ui2ToolboxPalette() = ToolboxPalette(
    isV2 = true,
    card = Ui2Colors.Card,
    raised = Ui2Colors.CardRaised,
    outline = Ui2Colors.CardOutline,
    text = Ui2Colors.Text,
    muted = Ui2Colors.Muted,
    dim = Ui2Colors.Dim
)

/** Accent rotation used across V2 surfaces (see ToolCardV2 / DailyQuestCardV2 on the home screen). */
private val ui2AccentCycle = listOf(Ui2Colors.Primary, Ui2Colors.Cyan, Ui2Colors.Lavender, Color(0xFFFF8A3D))

@Composable
fun FitnessToolboxScreen(
    settings: AppSettings,
    uiVersion: UiVersion = UiVersion.V1,
    modifier: Modifier = Modifier
) {
    val colors = if (uiVersion == UiVersion.V2) ui2ToolboxPalette() else classicToolboxPalette()
    if (colors.isV2) {
        Ui2Theme { FitnessToolboxContent(colors, settings, modifier) }
    } else {
        FitnessToolboxContent(colors, settings, modifier)
    }
}

@Composable
private fun FitnessToolboxContent(colors: ToolboxPalette, settings: AppSettings, modifier: Modifier) {
    var selected by remember { mutableStateOf<ToolboxCalculator?>(null) }

    val tools = listOf(
        Triple("Calorie Calculator", "Resting energy and calorie estimates", Icons.Outlined.LocalFireDepartment) to ToolboxCalculator.CALORIES,
        Triple("BMI Calculator", "Calculate BMI from height and weight", Icons.Outlined.MonitorWeight) to ToolboxCalculator.BMI,
        Triple("Protein Calculator", "Daily protein target for your goal", Icons.Outlined.FitnessCenter) to ToolboxCalculator.PROTEIN,
        Triple("Weight-Loss Pace", "Estimate weeks to reach your goal", Icons.Outlined.TrendingDown) to ToolboxCalculator.WEIGHT_LOSS
    )

    Column(
        modifier
            .fillMaxSize()
            .then(if (colors.isV2) Modifier.background(Ui2Colors.Background) else Modifier.background(IgniteBrushes.Screen))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        if (colors.isV2) {
            Text("Fitness toolbox", color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "Fast calculators that work offline.",
                color = colors.muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )
        } else {
            Text("FITNESS TOOLBOX", color = colors.text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Fast fitness calculators that work offline.", color = colors.muted, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
        }

        tools.forEachIndexed { index, pair ->
            val (info, tool) = pair
            val (title, subtitle, icon) = info
            val accent = if (colors.isV2) ui2AccentCycle[index % ui2AccentCycle.size] else IgniteColors.Gold
            ToolCard(colors, title, subtitle, icon, accent) { selected = tool }
        }
    }
    selected?.let { CalculatorDialog(colors, it, settings) { selected = null } }
}

@Composable
private fun ToolCard(colors: ToolboxPalette, title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    if (!colors.isV2) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .background(colors.card, RoundedCornerShape(18.dp))
                .border(1.dp, colors.outline, RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, color = colors.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, color = colors.muted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = colors.muted)
        }
        return
    }

    // UI 2.0: 84dp row card, raised icon chip, trailing pill affordance — matches ToolCardV2 on the home screen.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.card)
            .border(1.dp, colors.outline, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(colors.raised),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(colors.raised),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ChevronRight, null, tint = accent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CalculatorDialog(colors: ToolboxPalette, type: ToolboxCalculator, settings: AppSettings, onDismiss: () -> Unit) {
    var weight by remember { mutableStateOf(fitnessWeightPrefill(settings.weightKg)) }
    var height by remember { mutableStateOf(settings.heightCm?.toString().orEmpty()) }
    var age by remember { mutableStateOf(settings.ageYears?.toString().orEmpty()) }
    var goalWeight by remember { mutableStateOf("") }
    var weeklyLoss by remember { mutableStateOf("0.5") }
    var activity by remember { mutableStateOf<ActivityLevel?>(null) }
    var goal by remember { mutableStateOf(FitnessGoal.MAINTAIN) }
    var sex by remember { mutableStateOf<EquationSex?>(null) }
    var showDetails by remember { mutableStateOf(false) }

    val title = when (type) {
        ToolboxCalculator.CALORIES -> "Calorie Calculator"
        ToolboxCalculator.BMI -> "BMI Calculator"
        ToolboxCalculator.PROTEIN -> "Protein Calculator"
        ToolboxCalculator.WEIGHT_LOSS -> "Weight-Loss Pace"
    }
    val accent = when (type) {
        ToolboxCalculator.CALORIES -> if (colors.isV2) ui2AccentCycle[0] else IgniteColors.Gold
        ToolboxCalculator.BMI -> if (colors.isV2) ui2AccentCycle[1] else IgniteColors.Gold
        ToolboxCalculator.PROTEIN -> if (colors.isV2) ui2AccentCycle[2] else IgniteColors.Gold
        ToolboxCalculator.WEIGHT_LOSS -> if (colors.isV2) ui2AccentCycle[3] else IgniteColors.Gold
    }
    val result = calculateFitness(FitnessInput(type, weight, height, age, goal, sex, activity, goalWeight, weeklyLoss))
    val summaryLines = when (type) { ToolboxCalculator.CALORIES -> 3; ToolboxCalculator.BMI -> 2; else -> 1 }
    val lines = result.text.lines()
    val summary = if (result.error) result.text else lines.take(summaryLines).joinToString("\n")
        .replace("Loss starting estimate (maintenance − 400)", "Loss estimate")
        .replace("Gain starting estimate (maintenance + 250)", "Gain estimate")
        .replace("Maintenance starting estimate", "Maintenance estimate")

    if (!colors.isV2) {
        // UI 1.0 — unchanged.
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title, color = colors.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
            containerColor = colors.card,
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Adults 20+ · kg / cm", color = colors.muted, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(colors, "Weight (kg)", weight, modifier = Modifier.weight(1f)) { weight = it }
                        NumberField(colors, "Height (cm)", height, modifier = Modifier.weight(1f)) { height = it }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        NumberField(colors, "Age", age, wholeNumber = true, modifier = Modifier.weight(1f)) { age = it }
                        if (type == ToolboxCalculator.CALORIES) {
                            CompactDropdown(colors, "Gender", sex?.let { if (it == EquationSex.MALE) "Male" else "Female" } ?: "Select",
                                listOf("Male", "Female"), Modifier.weight(1f)) { sex = if (it == 0) EquationSex.MALE else EquationSex.FEMALE }
                        }
                    }
                    if (type == ToolboxCalculator.CALORIES) {
                        CompactDropdown(colors, "Activity", activity?.label ?: "Select your usual activity", ActivityLevel.entries.map { it.label }) {
                            activity = ActivityLevel.entries[it]
                        }
                    }
                    if (type == ToolboxCalculator.CALORIES || type == ToolboxCalculator.PROTEIN) {
                        val goals = listOf("Lose", "Maintain", "Gain")
                        CompactDropdown(colors, "Goal", goals[goal.ordinal], goals) { goal = FitnessGoal.entries[it] }
                    }
                    if (type == ToolboxCalculator.WEIGHT_LOSS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumberField(colors, "Goal (kg)", goalWeight, modifier = Modifier.weight(1f)) { goalWeight = it }
                            NumberField(colors, "Loss (kg/week)", weeklyLoss, modifier = Modifier.weight(1f)) { weeklyLoss = it }
                        }
                    }
                    HorizontalDivider(color = colors.outline)
                    Surface(color = IgniteColors.Surface3, shape = RoundedCornerShape(12.dp)) {
                        Text(summary, modifier = Modifier.fillMaxWidth().padding(12.dp), color = if (result.error) MaterialTheme.colorScheme.error else IgniteColors.Gold,
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    if (!result.error && lines.size > summaryLines) {
                        TextButton(onClick = { showDetails = !showDetails }, contentPadding = PaddingValues(0.dp)) {
                            Text(if (showDetails) "Hide details ▴" else "How this estimate works ▾", color = colors.muted)
                        }
                        if (showDetails) Text(lines.drop(summaryLines).joinToString("\n"), color = colors.muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Estimates only · not a prescribed diet.", color = colors.muted, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = IgniteColors.Gold) } }
        )
        return
    }

    // UI 2.0 — full-bleed scrim dialog with a rounded, bordered card (matches the Spotify popups).
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ui2Colors.Background.copy(alpha = 0.72f))
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .heightIn(max = 720.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(colors.card)
                    .border(1.dp, colors.outline, RoundedCornerShape(26.dp))
                    .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.raised),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when (type) {
                                ToolboxCalculator.CALORIES -> Icons.Outlined.LocalFireDepartment
                                ToolboxCalculator.BMI -> Icons.Outlined.MonitorWeight
                                ToolboxCalculator.PROTEIN -> Icons.Outlined.FitnessCenter
                                ToolboxCalculator.WEIGHT_LOSS -> Icons.Outlined.TrendingDown
                            },
                            null, tint = accent, modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(title, color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("Adults 20+ · kg / cm", color = colors.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))

                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(colors, "Weight (kg)", weight, modifier = Modifier.weight(1f)) { weight = it }
                        NumberField(colors, "Height (cm)", height, modifier = Modifier.weight(1f)) { height = it }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        NumberField(colors, "Age", age, wholeNumber = true, modifier = Modifier.weight(1f)) { age = it }
                        if (type == ToolboxCalculator.CALORIES) {
                            CompactDropdown(colors, "Gender", sex?.let { if (it == EquationSex.MALE) "Male" else "Female" } ?: "Select",
                                listOf("Male", "Female"), Modifier.weight(1f)) { sex = if (it == 0) EquationSex.MALE else EquationSex.FEMALE }
                        }
                    }
                    if (type == ToolboxCalculator.CALORIES) {
                        CompactDropdown(colors, "Activity", activity?.label ?: "Select your usual activity", ActivityLevel.entries.map { it.label }) {
                            activity = ActivityLevel.entries[it]
                        }
                    }
                    if (type == ToolboxCalculator.CALORIES || type == ToolboxCalculator.PROTEIN) {
                        val goals = listOf("Lose", "Maintain", "Gain")
                        CompactDropdown(colors, "Goal", goals[goal.ordinal], goals) { goal = FitnessGoal.entries[it] }
                    }
                    if (type == ToolboxCalculator.WEIGHT_LOSS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumberField(colors, "Goal (kg)", goalWeight, modifier = Modifier.weight(1f)) { goalWeight = it }
                            NumberField(colors, "Loss (kg/week)", weeklyLoss, modifier = Modifier.weight(1f)) { weeklyLoss = it }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.raised)
                            .padding(14.dp)
                    ) {
                        Text(
                            summary,
                            color = if (result.error) Ui2Colors.Danger else accent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!result.error && lines.size > summaryLines) {
                        TextButton(onClick = { showDetails = !showDetails }, contentPadding = PaddingValues(0.dp)) {
                            Text(if (showDetails) "Hide details ▴" else "How this estimate works ▾", color = colors.muted, fontSize = 13.sp)
                        }
                        if (showDetails) Text(lines.drop(summaryLines).joinToString("\n"), color = colors.muted, fontSize = 12.sp)
                    }
                    Text("Estimates only · not a prescribed diet.", color = colors.dim, fontSize = 11.sp)
                }

                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Done", color = Ui2Colors.Background, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NumberField(colors: ToolboxPalette, label: String, value: String, wholeNumber: Boolean = false, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val validSyntax = value.isBlank() || if (wholeNumber) value.matches(Regex("[0-9]{1,3}")) else parseFitnessDecimal(value) != null
    // Preserve invalid input for correction: never turn 70,5 into 705 or 5'10 into 510.
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        isError = !validSyntax,
        supportingText = if (!validSyntax) ({ Text(if (wholeNumber) "Use whole years." else "Use digits and one decimal separator (dot or comma).") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = if (wholeNumber) KeyboardType.Number else KeyboardType.Decimal),
        colors = if (colors.isV2) OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.text, unfocusedTextColor = colors.text,
            focusedBorderColor = Ui2Colors.Primary, unfocusedBorderColor = colors.outline,
            focusedLabelColor = Ui2Colors.Primary, unfocusedLabelColor = colors.muted,
            cursorColor = Ui2Colors.Primary
        ) else OutlinedTextFieldDefaults.colors(),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun CompactDropdown(colors: ToolboxPalette, label: String, value: String, options: List<String>, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(onClick = { expanded = true }, shape = RoundedCornerShape(if (colors.isV2) 14.dp else 10.dp),
            color = colors.raised, border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = colors.muted)
                    Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.text, maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Choose $label", tint = if (colors.isV2) Ui2Colors.Primary else IgniteColors.Gold)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(index); expanded = false })
            }
        }
    }
}