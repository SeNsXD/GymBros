package com.rishabh.gymbros.ui.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rishabh.gymbros.features.nutrition.data.NutritionRepository
import com.rishabh.gymbros.features.nutrition.model.*
import com.rishabh.gymbros.ui.components.HeatButton
import com.rishabh.gymbros.ui.components.IgniteCard
import com.rishabh.gymbros.ui.theme.IgniteColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun NutritionScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(context) { NutritionRepository.getInstance(context) }
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodReference>>(emptyList()) }
    var selected by remember { mutableStateOf<FoodReference?>(null) }
    var editing by remember { mutableStateOf<FoodLogEntry?>(null) }
    var showGoals by rememberSaveable { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) { repository.refresh() }
    LaunchedEffect(query) {
        searchJob?.cancel()
        if (query.isBlank()) results = emptyList() else searchJob = scope.launch {
            delay(120)
            results = repository.searchFoods(query)
        }
    }

    if (selected != null) {
        FoodLoggingDialog(food = selected!!, onDismiss = { selected = null }, onAdd = { food, grams, meal ->
            repository.addFood(food, grams, meal); selected = null; query = ""; results = emptyList()
        })
    }
    editing?.let { entry ->
        EditLogDialog(entry, onDismiss = { editing = null }, onSave = { grams, meal -> repository.updateEntry(entry, grams, meal); editing = null }, onDelete = { repository.deleteEntry(entry.id); editing = null })
    }
    if (showGoals) GoalDialog(state.goals, { showGoals = false }, { repository.setGoals(it); showGoals = false })

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Nutrition", color = IgniteColors.Text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("IFCT 2017 • offline food tracking", color = IgniteColors.MutedText, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { showGoals = true }) { Text(if (state.goals.configured) "Edit goals" else "Set goals", color = IgniteColors.Gold) }
            }
        }
        item { TodaySummary(state) }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = IgniteColors.Gold) },
                placeholder = { Text("Search Indian foods", color = IgniteColors.MutedText) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = IgniteColors.Text, unfocusedTextColor = IgniteColors.Text, focusedBorderColor = IgniteColors.Ember, unfocusedBorderColor = IgniteColors.Hairline)
            )
        }
        if (query.isNotBlank()) {
            if (results.isEmpty()) item { Text("No matching IFCT foods", color = IgniteColors.MutedText, modifier = Modifier.padding(vertical = 10.dp)) }
            items(results, key = { it.code }) { food -> FoodSearchRow(food) { selected = food } }
        } else {
            MealType.entries.forEach { meal ->
                val entries = state.todayEntries.filter { it.meal == meal }
                item(key = "meal_${meal.name}") { MealSection(meal, entries, onEntry = { editing = it }) }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable private fun TodaySummary(state: NutritionUiState) {
    IgniteCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TODAY", color = IgniteColors.Text, fontWeight = FontWeight.Bold)
            Text("${state.totals.calories.roundToInt()} kcal", color = IgniteColors.Gold, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(12.dp))
        if (!state.goals.configured) Text("Set your calorie and macro goals to see daily progress.", color = IgniteColors.MutedText, style = MaterialTheme.typography.bodySmall)
        else {
            MacroProgress("Calories", state.totals.calories, state.goals.calories.toDouble(), "kcal")
            MacroProgress("Protein", state.totals.protein, state.goals.protein.toDouble(), "g")
            MacroProgress("Carbs", state.totals.carbs, state.goals.carbs.toDouble(), "g")
            MacroProgress("Fat", state.totals.fat, state.goals.fat.toDouble(), "g")
        }
    }
}

@Composable private fun MacroProgress(label: String, value: Double, goal: Double, unit: String) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = IgniteColors.SoftText, style = MaterialTheme.typography.bodySmall)
            Text("${value.roundToInt()} / ${goal.roundToInt()} $unit", color = IgniteColors.Text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(progress = { (value / goal).toFloat().coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(99.dp)), color = IgniteColors.Ember, trackColor = IgniteColors.Surface3)
    }
}

@Composable private fun FoodSearchRow(food: FoodReference, onClick: () -> Unit) {
    IgniteCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(13.dp), onClick = onClick) {
        Text(food.name, color = IgniteColors.Text, fontWeight = FontWeight.SemiBold)
        Text(food.category, color = IgniteColors.MutedText, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(5.dp))
        Text("${food.caloriesPer100g.roundToInt()} kcal  •  P ${one(food.proteinPer100g)}g  •  C ${one(food.carbsPer100g)}g  •  F ${one(food.fatPer100g)}g  / 100 g", color = IgniteColors.SoftText, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun MealSection(meal: MealType, entries: List<FoodLogEntry>, onEntry: (FoodLogEntry) -> Unit) {
    val totals = entries.fold(NutrientTotals()) { a,e -> a + NutrientTotals(e.calories,e.protein,e.carbs,e.fat,e.fiber) }
    IgniteCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(meal.label, color = IgniteColors.Text, fontWeight = FontWeight.Bold)
            Text("${totals.calories.roundToInt()} kcal", color = IgniteColors.Gold, fontWeight = FontWeight.Bold)
        }
        if (entries.isNotEmpty()) {
            Text("P ${one(totals.protein)}g  •  C ${one(totals.carbs)}g  •  F ${one(totals.fat)}g", color = IgniteColors.MutedText, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 3.dp))
        }
        if (entries.isEmpty()) Text("No foods logged", color = IgniteColors.MutedText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        entries.forEach { e ->
            Row(Modifier.fillMaxWidth().clickable { onEntry(e) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(e.foodName, color = IgniteColors.Text, style = MaterialTheme.typography.bodyMedium); Text("${one(e.grams)} g • P ${one(e.protein)} • C ${one(e.carbs)} • F ${one(e.fat)}", color = IgniteColors.MutedText, style = MaterialTheme.typography.labelSmall) }
                Text("${e.calories.roundToInt()} kcal", color = IgniteColors.SoftText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable private fun FoodLoggingDialog(food: FoodReference, onDismiss: () -> Unit, onAdd: (FoodReference, Double, MealType) -> Unit) {
    var gramsText by rememberSaveable(food.code) { mutableStateOf("100") }
    var meal by rememberSaveable { mutableStateOf(MealType.BREAKFAST) }
    val grams = gramsText.toDoubleOrNull()?.coerceIn(1.0, 5000.0) ?: 0.0
    val n = food.nutrientsFor(grams)
    AlertDialog(onDismissRequest = onDismiss, containerColor = IgniteColors.Surface2,
        title = { Column { Text(food.name, color = IgniteColors.Text, fontWeight = FontWeight.Black); Text(food.category, color = IgniteColors.MutedText, style = MaterialTheme.typography.labelSmall) } },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${n.calories.roundToInt()} kcal  •  Protein ${one(n.protein)} g\nCarbs ${one(n.carbs)} g  •  Fat ${one(n.fat)} g  •  Fiber ${one(n.fiber)} g", color = IgniteColors.Text)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallAdjustButton(Icons.Outlined.Remove) { gramsText = (grams - 10).coerceAtLeast(1.0).roundToInt().toString() }
                OutlinedTextField(value = gramsText, onValueChange = { gramsText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(7) }, modifier = Modifier.weight(1f), label = { Text("Amount (g)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                SmallAdjustButton(Icons.Outlined.Add) { gramsText = (grams + 10).roundToInt().toString() }
            }
            MealPicker(meal) { meal = it }
        } },
        confirmButton = { TextButton(enabled = grams > 0, onClick = { onAdd(food, grams, meal) }) { Text("ADD FOOD", color = IgniteColors.Gold, fontWeight = FontWeight.Black) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = IgniteColors.MutedText) } })
}

@Composable private fun EditLogDialog(entry: FoodLogEntry, onDismiss: () -> Unit, onSave: (Double, MealType) -> Unit, onDelete: () -> Unit) {
    var gramsText by rememberSaveable(entry.id) { mutableStateOf(one(entry.grams)) }
    var meal by rememberSaveable(entry.id) { mutableStateOf(entry.meal) }
    val grams = gramsText.toDoubleOrNull() ?: 0.0
    AlertDialog(onDismissRequest = onDismiss, containerColor = IgniteColors.Surface2, title = { Text(entry.foodName, color = IgniteColors.Text, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(gramsText, { gramsText = it.filter { c -> c.isDigit() || c == '.' }.take(7) }, label = { Text("Amount (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true); MealPicker(meal) { meal = it }; TextButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, null, tint = com.rishabh.gymbros.ui.theme.Ui2Colors.Danger); Spacer(Modifier.width(6.dp)); Text("Delete entry") } } },
        confirmButton = { TextButton(enabled = grams > 0, onClick = { onSave(grams, meal) }) { Text("Save", color = IgniteColors.Gold) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun MealPicker(selected: MealType, onSelect: (MealType) -> Unit) {
    Column { Text("Meal", color = IgniteColors.MutedText, style = MaterialTheme.typography.labelSmall); Spacer(Modifier.height(6.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) { MealType.entries.forEach { meal -> Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (meal == selected) IgniteColors.Ember else IgniteColors.Surface3).clickable { onSelect(meal) }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) { Text(meal.label.take(5), color = if (meal == selected) IgniteColors.TextOnGradient else IgniteColors.SoftText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) } } } }
}

@Composable private fun GoalDialog(goals: NutritionGoals, onDismiss: () -> Unit, onSave: (NutritionGoals) -> Unit) {
    var cal by rememberSaveable { mutableStateOf(goals.calories.takeIf { it > 0 }?.toString().orEmpty()) }; var protein by rememberSaveable { mutableStateOf(goals.protein.takeIf { it > 0 }?.toString().orEmpty()) }; var carbs by rememberSaveable { mutableStateOf(goals.carbs.takeIf { it > 0 }?.toString().orEmpty()) }; var fat by rememberSaveable { mutableStateOf(goals.fat.takeIf { it > 0 }?.toString().orEmpty()) }
    fun field(v: String) = v.toIntOrNull()?.coerceAtLeast(0) ?: 0
    AlertDialog(onDismissRequest = onDismiss, containerColor = IgniteColors.Surface2, title = { Text("Daily nutrition goals", color = IgniteColors.Text, fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { GoalField("Calories (kcal)", cal) { cal=it }; GoalField("Protein (g)", protein) { protein=it }; GoalField("Carbs (g)", carbs) { carbs=it }; GoalField("Fat (g)", fat) { fat=it } } }, confirmButton = { TextButton(onClick = { onSave(NutritionGoals(field(cal),field(protein),field(carbs),field(fat))) }) { Text("Save", color = IgniteColors.Gold) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
@Composable private fun GoalField(label: String, value: String, onChange: (String)->Unit) { OutlinedTextField(value, { onChange(it.filter(Char::isDigit).take(5)) }, label={Text(label)}, singleLine=true, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number)) }
@Composable private fun SmallAdjustButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick:()->Unit) { IconButton(onClick=onClick, modifier=Modifier.clip(RoundedCornerShape(10.dp)).background(IgniteColors.Surface3)) { Icon(icon,null,tint=IgniteColors.Gold) } }
private fun one(v: Double): String = if (kotlin.math.abs(v - v.roundToInt()) < 0.05) v.roundToInt().toString() else "%.1f".format(v)
