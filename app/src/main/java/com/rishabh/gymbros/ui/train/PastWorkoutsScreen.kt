package com.rishabh.gymbros.ui.train

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.rishabh.gymbros.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rishabh.gymbros.features.workout.data.WorkoutRepository
import com.rishabh.gymbros.features.workout.model.WorkoutCategory
import com.rishabh.gymbros.features.workout.model.WorkoutSession
import com.rishabh.gymbros.features.workout.model.WorkoutSetRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val HistoryCardShape = RoundedCornerShape(18.dp)
private val HistoryInnerShape = RoundedCornerShape(12.dp)

@Composable
internal fun PastWorkoutsScreen(
    sessions: List<WorkoutSession>,
    setLogs: Map<Long, List<WorkoutSetRecord>>,
    sessionMetrics: Map<Long, Map<String, Double>>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedSessionId by remember { mutableStateOf<Long?>(sessions.firstOrNull()?.id) }
    var pendingDelete by remember { mutableStateOf<WorkoutSession?>(null) }
    val context = LocalContext.current
    val repository = remember(context) { WorkoutRepository.getInstance(context.applicationContext) }
    val totalVolume = sessions.sumOf { it.totalVolumeKg }.roundToInt()
    val totalMinutes = sessions.sumOf { it.durationSeconds } / 60L
    val latestSession = sessions.maxByOrNull { it.endedAt }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LocalWorkoutUiPalette.current.Canvas),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HistoryHeader(onBack = onBack)
        }

        latestSession?.let { session ->
            item(key = "last-completion-${session.id}") {
                LastWorkoutCompletionCard(
                    session = session,
                    metrics = sessionMetrics[session.id].orEmpty()
                )
            }
        }

        item {
            HistorySummary(
                workoutCount = sessions.size,
                totalVolumeKg = totalVolume,
                totalMinutes = totalMinutes
            )
        }

        if (sessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(HistoryCardShape)
                        .background(LocalWorkoutUiPalette.current.Card)
                        .workoutBorder(1.dp, LocalWorkoutUiPalette.current.Border, HistoryCardShape)
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "No workouts yet",
                            color = LocalWorkoutUiPalette.current.TextStrong,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Finish a Workout session and its set-by-set history will appear here.",
                            color = LocalWorkoutUiPalette.current.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            sessions.forEach { session ->
                item(key = "history-${session.id}") {
                    WorkoutHistoryCard(
                        session = session,
                        logs = setLogs[session.id].orEmpty(),
                        metrics = sessionMetrics[session.id].orEmpty(),
                        expanded = expandedSessionId == session.id,
                        onToggle = {
                            expandedSessionId = if (expandedSessionId == session.id) null else session.id
                        },
                        onDelete = { pendingDelete = session }
                    )
                }
            }
        }
    }

    pendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete workout?") },
            text = { Text("This will permanently delete this workout and its set history. This cannot be undone.") },
            confirmButton = {
                com.rishabh.gymbros.ui.components.DialogActionPill(
                    text = "Delete",
                    onClick = {
                        if (expandedSessionId == session.id) expandedSessionId = null
                        repository.deleteWorkoutSession(session.id)
                        pendingDelete = null
                    },
                    destructive = true
                )
            },
            dismissButton = {
                com.rishabh.gymbros.ui.components.DialogActionPill("Cancel", { pendingDelete = null })
            }
        )
    }
}

@Composable
private fun HistoryHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                text = "Past Workouts",
                color = LocalWorkoutUiPalette.current.TextStrong,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Your training timeline",
                color = LocalWorkoutUiPalette.current.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun LastWorkoutCompletionCard(
    session: WorkoutSession,
    metrics: Map<String, Double>
) {
    val finishedAt = Instant.ofEpochMilli(session.endedAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, d MMM • h:mm a", Locale.ENGLISH))
    val calories = metrics[WorkoutRepository.METRIC_ESTIMATED_CALORIES]?.roundToInt() ?: 0
    val cardioSeconds = metrics[WorkoutRepository.METRIC_TOTAL_CARDIO_SECONDS]?.toLong() ?: 0L
    val prs = metrics[WorkoutRepository.METRIC_PR_COUNT]?.roundToInt() ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HistoryCardShape)
            .background(LocalWorkoutUiPalette.current.pick(LocalWorkoutUiPalette.current.Card, LocalWorkoutUiPalette.current.CardHigh))
            .workoutBorder(1.dp, LocalWorkoutUiPalette.current.Orange.copy(alpha = 0.30f), HistoryCardShape)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Last workout",
                color = LocalWorkoutUiPalette.current.pick(WorkoutAccents.Pink, LocalWorkoutUiPalette.current.SoftOrange),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = finishedAt,
                color = LocalWorkoutUiPalette.current.Dimmed,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            SummaryMetric("Calories", "~$calories", Modifier.weight(1f), WorkoutAccents.Pink)
            SummaryMetric("Cardio", formatDuration(cardioSeconds), Modifier.weight(1f), WorkoutAccents.Cyan)
            SummaryMetric("PRs", prs.toString(), Modifier.weight(1f), WorkoutAccents.Purple)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            SummaryMetric("Exercises", "${session.completedExercises}/${session.totalExercises}", Modifier.weight(1f), WorkoutAccents.Mint)
            SummaryMetric("Time", formatDuration(session.durationSeconds), Modifier.weight(1f), WorkoutAccents.Cyan)
        }
    }
}

@Composable
private fun HistorySummary(
    workoutCount: Int,
    totalVolumeKg: Int,
    totalMinutes: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HistoryCardShape)
            .background(LocalWorkoutUiPalette.current.pick(LocalWorkoutUiPalette.current.Card, LocalWorkoutUiPalette.current.CardHigh))
            .workoutBorder(1.dp, LocalWorkoutUiPalette.current.Border, HistoryCardShape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryMetric("Workouts", workoutCount.toString(), Modifier.weight(1f), WorkoutAccents.Pink)
        SummaryMetric("Volume", "$totalVolumeKg kg", Modifier.weight(1f), WorkoutAccents.Purple)
        SummaryMetric("Time", "${totalMinutes}m", Modifier.weight(1f), WorkoutAccents.Cyan)
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier, accent: Color? = null) {
    Column(
        modifier = modifier
            .clip(HistoryInnerShape)
            .background(LocalWorkoutUiPalette.current.pick(LocalWorkoutUiPalette.current.CanvasDeep, LocalWorkoutUiPalette.current.Card))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = if (LocalWorkoutUiPalette.current.MultiColor && accent != null) accent else LocalWorkoutUiPalette.current.SoftOrange,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = LocalWorkoutUiPalette.current.Dimmed,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun WorkoutHistoryCard(
    session: WorkoutSession,
    logs: List<WorkoutSetRecord>,
    metrics: Map<String, Double>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val date = Instant.ofEpochMilli(session.endedAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, d MMM • h:mm a", Locale.ENGLISH))
    val grouped = logs.groupBy { it.exerciseName.ifBlank { "Exercise" } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HistoryCardShape)
            .background(LocalWorkoutUiPalette.current.Card)
            .workoutBorder(1.dp, LocalWorkoutUiPalette.current.Border, HistoryCardShape)
            .clickable(onClick = onToggle)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LocalWorkoutUiPalette.current.pick(WorkoutAccents.PinkTint, LocalWorkoutUiPalette.current.OrangeTint)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = LocalWorkoutUiPalette.current.pick(WorkoutAccents.Pink, LocalWorkoutUiPalette.current.Orange)
                    )
                }
                Column {
                    Text(
                        text = session.title,
                        color = LocalWorkoutUiPalette.current.TextStrong,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    session.musicTrackTitle?.let { title ->
                        Text(
                            text = "Music: $title · ${session.musicTrackArtist.orEmpty()}",
                            color = LocalWorkoutUiPalette.current.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = date,
                        color = LocalWorkoutUiPalette.current.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = LocalWorkoutUiPalette.current.pick(WorkoutAccents.Pink, LocalWorkoutUiPalette.current.SoftOrange)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryChip(Icons.Outlined.Schedule, formatDuration(session.durationSeconds), WorkoutAccents.Cyan)
            HistoryTickChip("${session.completedExercises}/${session.totalExercises}", WorkoutAccents.Mint)
            if (session.category == WorkoutCategory.STRENGTH || session.totalVolumeKg > 0.0) {
                HistoryChip(Icons.Outlined.FitnessCenter, "${session.totalVolumeKg.roundToInt()} kg", WorkoutAccents.Purple)
            }
        }

        if (metrics.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val distance = metrics[WorkoutRepository.METRIC_DISTANCE_KM]
                val pace = metrics[WorkoutRepository.METRIC_PACE_SECONDS_PER_KM]
                val rounds = metrics[WorkoutRepository.METRIC_HIIT_ROUNDS_COMPLETED]
                val holdSeconds = metrics[WorkoutRepository.METRIC_MOBILITY_HOLD_SECONDS]
                if (distance != null && distance > 0.0) {
                    HistoryChip(Icons.Outlined.DirectionsRun, "${formatMetric(distance)} km", WorkoutAccents.Pink)
                }
                if (pace != null && pace > 0.0) {
                    HistoryChip(Icons.Outlined.Timer, formatPace(pace.toLong()), WorkoutAccents.Cyan)
                }
                if (rounds != null && rounds > 0.0) {
                    HistoryChip(Icons.Outlined.Timer, "${rounds.toInt()} rounds", WorkoutAccents.Cyan)
                }
                if (holdSeconds != null && holdSeconds > 0.0) {
                    HistoryChip(Icons.Outlined.Timer, "${holdSeconds.toInt()}s holds", WorkoutAccents.Cyan)
                }
            }
        }

        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (LocalWorkoutUiPalette.current.MultiColor) {
                    // UI 2.0: red translucent pill.
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(WorkoutAccents.DeleteRed.copy(alpha = 0.16f))
                            .clickable(onClick = onDelete)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete workout",
                            tint = WorkoutAccents.DeleteRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Delete workout",
                            color = WorkoutAccents.DeleteRed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                } else {
                    TextButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete workout",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Delete workout",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }

            if (grouped.isEmpty()) {
                Text(
                    text = "Detailed set data is unavailable for this older workout.",
                    color = LocalWorkoutUiPalette.current.Dimmed,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                grouped.forEach { (exerciseName, exerciseLogs) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(HistoryInnerShape)
                            .background(LocalWorkoutUiPalette.current.CanvasDeep)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = exerciseName,
                            color = LocalWorkoutUiPalette.current.Text,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        exerciseLogs.sortedBy { it.setNumber }.forEach { set ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Set ${set.setNumber}",
                                    color = LocalWorkoutUiPalette.current.TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = when (session.category) {
                                            WorkoutCategory.CARDIO -> "${set.reps} min"
                                            WorkoutCategory.MOBILITY -> "${set.reps} sec"
                                            WorkoutCategory.HIIT -> "${set.reps} reps${if (set.loadKg > 0.0) " × ${formatLoad(set.loadKg)} kg" else ""}"
                                            WorkoutCategory.STRENGTH -> "${set.reps} reps × ${formatLoad(set.loadKg)} kg"
                                        },
                                        color = if (set.completed) LocalWorkoutUiPalette.current.SoftOrange else LocalWorkoutUiPalette.current.Dimmed,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (set.completed) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (set.completed) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(LocalWorkoutUiPalette.current.Orange),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_gymbros_tick),
                                                contentDescription = "Completed",
                                                tint = LocalWorkoutUiPalette.current.OnAccent,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun HistoryTickChip(text: String, accent: Color? = null) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(LocalWorkoutUiPalette.current.pick(WorkoutAccents.ChipNeutral, LocalWorkoutUiPalette.current.Elevated))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_gymbros_tick),
            contentDescription = null,
            tint = if (LocalWorkoutUiPalette.current.MultiColor && accent != null) accent else LocalWorkoutUiPalette.current.SoftOrange,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            color = LocalWorkoutUiPalette.current.TextSecondary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun HistoryChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, accent: Color? = null) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(LocalWorkoutUiPalette.current.pick(WorkoutAccents.ChipNeutral, LocalWorkoutUiPalette.current.Elevated))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (LocalWorkoutUiPalette.current.MultiColor && accent != null) accent else LocalWorkoutUiPalette.current.SoftOrange,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            color = LocalWorkoutUiPalette.current.TextSecondary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60L
    return if (minutes < 60L) "${minutes}m" else "${minutes / 60L}h ${minutes % 60L}m"
}

private fun formatLoad(value: Double): String {
    val rounded = value.roundToInt()
    return if (kotlin.math.abs(value - rounded.toDouble()) < 0.001) rounded.toString() else String.format(Locale.ENGLISH, "%.1f", value)
}

private fun formatMetric(value: Double): String {
    val rounded = value.roundToInt()
    return if (kotlin.math.abs(value - rounded.toDouble()) < 0.001) rounded.toString() else String.format(Locale.ENGLISH, "%.2f", value).trimEnd('0').trimEnd('.')
}

private fun formatPace(secondsPerKm: Long): String {
    if (secondsPerKm <= 0L) return "— /km"
    return "${secondsPerKm / 60L}:${(secondsPerKm % 60L).toString().padStart(2, '0')} /km"
}