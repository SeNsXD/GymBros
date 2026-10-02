package com.rishabh.gymbros.ui.train

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rishabh.gymbros.features.workout.domain.WorkoutAnalytics
import com.rishabh.gymbros.features.workout.model.ExercisePersonalRecord
import com.rishabh.gymbros.features.workout.model.TrainingPeriodSummary
import com.rishabh.gymbros.features.workout.model.WorkoutCategory
import com.rishabh.gymbros.features.workout.model.WorkoutUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val PerformanceCardShape = RoundedCornerShape(18.dp)

@Composable
fun PerformanceScreen(
    state: WorkoutUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val analytics = remember(state.sessions, state.sessionSetLogs, state.sessionMetrics, today) {
        WorkoutAnalytics.snapshot(
            sessions = state.sessions,
            setLogs = state.sessionSetLogs,
            today = today,
            sessionMetrics = state.sessionMetrics
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LocalWorkoutUiPalette.current.Canvas),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            PerformanceHeader(onBack = onBack)
        }

        item {
            MomentumCard(
                current = analytics.currentPeriod,
                previous = analytics.previousPeriod
            )
        }

        item {
            SectionTitle(
                title = "Last 7 days",
                subtitle = "Your actual completed training volume"
            )
            Spacer(modifier = Modifier.height(10.dp))
            WeeklyVolumeChart(
                points = analytics.dailyPoints.map { point ->
                    LocalDate.ofEpochDay(point.epochDay) to point.volumeKg
                }
            )
        }

        item {
            SectionTitle(
                title = "Training mix",
                subtitle = "Training time by workout category, last 7 days"
            )
            Spacer(modifier = Modifier.height(10.dp))
            CategoryDurationCard(analytics.categoryDurationSeconds)
        }

        item {
            SectionTitle(
                title = "Personal records",
                subtitle = "Calculated only from completed set logs"
            )
        }

        if (analytics.personalRecords.isEmpty()) {
            item {
                EmptyPerformanceCard()
            }
        } else {
            items(analytics.personalRecords.size.coerceAtMost(10)) { index ->
                PersonalRecordCard(record = analytics.personalRecords[index])
            }
        }
    }
}

@Composable
private fun PerformanceHeader(onBack: () -> Unit) {
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
                text = "Performance",
                color = LocalWorkoutUiPalette.current.TextStrong,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Progress, consistency and personal records",
                color = LocalWorkoutUiPalette.current.Dimmed,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(LocalWorkoutUiPalette.current.pick(WorkoutAccents.PurpleTint, LocalWorkoutUiPalette.current.OrangeTint)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Analytics,
                contentDescription = null,
                tint = LocalWorkoutUiPalette.current.pick(WorkoutAccents.Purple, LocalWorkoutUiPalette.current.Orange),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun MomentumCard(
    current: TrainingPeriodSummary,
    previous: TrainingPeriodSummary
) {
    val volumeTrend = WorkoutAnalytics.percentageChange(current.volumeKg, previous.volumeKg)
    val durationTrend = WorkoutAnalytics.percentageChange(
        current.durationSeconds.toDouble(),
        previous.durationSeconds.toDouble()
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PerformanceCardShape)
            .background(LocalWorkoutUiPalette.current.Card)
            .workoutBorder(1.dp, LocalWorkoutUiPalette.current.Border, PerformanceCardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LocalWorkoutUiPalette.current.pick(WorkoutAccents.PurpleTint, LocalWorkoutUiPalette.current.ElevatedHigh)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShowChart,
                        contentDescription = null,
                        tint = LocalWorkoutUiPalette.current.pick(WorkoutAccents.Purple, LocalWorkoutUiPalette.current.Orange)
                    )
                }
                Column {
                    Text(
                        text = "7-Day Momentum",
                        color = LocalWorkoutUiPalette.current.TextStrong,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Compared with the previous 7 days",
                        color = LocalWorkoutUiPalette.current.Dimmed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(
                label = "VOLUME",
                value = formatVolume(current.volumeKg),
                trend = formatTrend(volumeTrend),
                icon = "volume",
                accent = WorkoutAccents.Purple,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "TIME",
                value = formatDuration(current.durationSeconds),
                trend = formatTrend(durationTrend),
                icon = "time",
                accent = WorkoutAccents.Cyan,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(
                label = "SESSIONS",
                value = current.sessionCount.toString(),
                trend = "7 days",
                icon = "session",
                accent = WorkoutAccents.Mint,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "SETS",
                value = current.completedSets.toString(),
                trend = "completed",
                icon = "sets",
                accent = WorkoutAccents.Pink,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    trend: String,
    icon: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(LocalWorkoutUiPalette.current.CanvasDeep)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = when (icon) {
                "time" -> Icons.Outlined.Schedule
                "session" -> Icons.Outlined.FitnessCenter
                "sets" -> Icons.Outlined.EmojiEvents
                else -> Icons.Outlined.ShowChart
            },
            contentDescription = null,
            tint = LocalWorkoutUiPalette.current.pick(accent, LocalWorkoutUiPalette.current.SoftOrange),
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = value,
            color = LocalWorkoutUiPalette.current.TextStrong,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1
        )
        Text(
            text = label,
            color = LocalWorkoutUiPalette.current.Muted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = trend,
            color = LocalWorkoutUiPalette.current.pick(accent, LocalWorkoutUiPalette.current.Orange),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun WeeklyVolumeChart(points: List<Pair<LocalDate, Double>>) {
    val maxVolume = points.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(158.dp)
            .clip(PerformanceCardShape)
            .background(LocalWorkoutUiPalette.current.Card)
            .workoutBorder(1.dp, LocalWorkoutUiPalette.current.Border, PerformanceCardShape)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEach { (day, volume) ->
            val ratio = (volume / maxVolume).toFloat().coerceIn(0f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = if (volume > 0) compactNumber(volume) else "",
                    color = LocalWorkoutUiPalette.current.Dimmed,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height((18f + 70f * ratio).dp)
                        .clip(CircleShape)
                        .background(
                            if (volume > 0) {
                                if (LocalWorkoutUiPalette.current.MultiColor) androidx.compose.ui.graphics.SolidColor(WorkoutAccents.Purple)
                                else LocalWorkoutUiPalette.current.Heat
                            }
                            else androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(LocalWorkoutUiPalette.current.Elevated, LocalWorkoutUiPalette.current.Elevated)
                            )
                        )
                )
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = day.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)).take(1),
                    color = LocalWorkoutUiPalette.current.Muted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CategoryDurationCard(values: Map<WorkoutCategory, Long>) {
    val total = values.values.sum().coerceAtLeast(1L)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PerformanceCardShape)
            .background(LocalWorkoutUiPalette.current.Card)
            .workoutBorder(1.dp, LocalWorkoutUiPalette.current.Border, PerformanceCardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        WorkoutCategory.entries.forEach { category ->
            val seconds = values[category] ?: 0L
            val ratio = (seconds.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (LocalWorkoutUiPalette.current.MultiColor) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(categoryAccent(category))
                            )
                        }
                        Text(
                            text = category.title,
                            color = LocalWorkoutUiPalette.current.Text,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = formatDuration(seconds),
                        color = LocalWorkoutUiPalette.current.Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(LocalWorkoutUiPalette.current.Elevated)
                ) {
                    if (ratio > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(if (LocalWorkoutUiPalette.current.MultiColor) androidx.compose.ui.graphics.SolidColor(categoryAccent(category)) else LocalWorkoutUiPalette.current.Heat)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalRecordCard(record: ExercisePersonalRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PerformanceCardShape)
            .background(LocalWorkoutUiPalette.current.Card)
            .workoutBorder(1.dp, LocalWorkoutUiPalette.current.Border, PerformanceCardShape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(LocalWorkoutUiPalette.current.pick(WorkoutAccents.PurpleTint, LocalWorkoutUiPalette.current.OrangeTint)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = LocalWorkoutUiPalette.current.pick(WorkoutAccents.Purple, LocalWorkoutUiPalette.current.Gold)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.exerciseName,
                color = LocalWorkoutUiPalette.current.TextStrong,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${record.completedSets} completed sets • ${formatVolume(record.totalVolumeKg)} lifetime",
                color = LocalWorkoutUiPalette.current.Dimmed,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${cleanNumber(record.bestLoadKg)} kg",
                color = LocalWorkoutUiPalette.current.pick(WorkoutAccents.Purple, LocalWorkoutUiPalette.current.SoftOrange),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "e1RM ${cleanNumber(record.bestEstimatedOneRepMaxKg)} kg",
                color = LocalWorkoutUiPalette.current.Muted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun EmptyPerformanceCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 130.dp)
            .clip(PerformanceCardShape)
            .background(LocalWorkoutUiPalette.current.Card)
            .workoutBorder(1.dp, LocalWorkoutUiPalette.current.Border, PerformanceCardShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.EmojiEvents,
            contentDescription = null,
            tint = LocalWorkoutUiPalette.current.pick(WorkoutAccents.Purple, LocalWorkoutUiPalette.current.Orange),
            modifier = Modifier.size(30.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No personal records yet",
            color = LocalWorkoutUiPalette.current.TextStrong,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Complete sets in a workout and your records will appear here.",
            color = LocalWorkoutUiPalette.current.Dimmed,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            color = LocalWorkoutUiPalette.current.TextStrong,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = subtitle,
            color = LocalWorkoutUiPalette.current.Dimmed,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun categoryAccent(category: WorkoutCategory): Color = when (category) {
    WorkoutCategory.STRENGTH -> WorkoutAccents.Purple
    WorkoutCategory.CARDIO -> WorkoutAccents.Cyan
    WorkoutCategory.HIIT -> WorkoutAccents.Pink
    WorkoutCategory.MOBILITY -> WorkoutAccents.Mint
}

private fun formatTrend(value: Int?): String = when {
    value == null -> "New activity"
    value > 0 -> "+$value% vs prior week"
    value < 0 -> "$value% vs prior week"
    else -> "No change"
}

private fun formatVolume(value: Double): String = when {
    value >= 1000.0 -> String.format(Locale.US, "%.1fk kg", value / 1000.0)
    else -> "${value.roundToInt()} kg"
}

private fun compactNumber(value: Double): String = when {
    value >= 1000.0 -> String.format(Locale.US, "%.1fk", value / 1000.0)
    else -> value.roundToInt().toString()
}

private fun cleanNumber(value: Double): String {
    val rounded = value.roundToInt()
    return if (kotlin.math.abs(value - rounded) < 0.05) rounded.toString()
    else String.format(Locale.US, "%.1f", value)
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60L
    return when {
        minutes >= 60L -> "${minutes / 60L}h ${minutes % 60L}m"
        else -> "${minutes}m"
    }
}