package com.rishabh.gymbros.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rishabh.gymbros.features.cloud.CloudBackupState
import com.rishabh.gymbros.features.cloud.CloudRecoveryPoint
import com.rishabh.gymbros.ui.components.IgniteCard
import com.rishabh.gymbros.ui.theme.IgniteBrushes
import com.rishabh.gymbros.ui.theme.IgniteColors
import java.text.DateFormat
import java.util.Date

@Composable
fun CloudBackupCard(
    state: CloudBackupState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onAutoBackupChange: (Boolean) -> Unit,
    onBackupNow: () -> Unit,
    onRestore: () -> Unit,
    onRestorePoint: (String) -> Unit,
    onRenamePoint: (String, String) -> Unit,
    onPinPoint: (String, Boolean) -> Unit,
    onDeletePoint: (String) -> Unit,
    onDeleteAll: () -> Unit,
    onSetRecoveryPassword: (String) -> Unit,
    onUnlockRecoveryPassword: (String) -> Unit,
    onDismissRestoreSuggestion: () -> Unit
) {
    var pendingRestore by remember { mutableStateOf<CloudRecoveryPoint?>(null) }
    var pendingRename by remember { mutableStateOf<CloudRecoveryPoint?>(null) }
    var renameText by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<CloudRecoveryPoint?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var recoveryPointsExpanded by remember { mutableStateOf(false) }

    IgniteCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(IgniteColors.GoldTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CloudDone, null, tint = IgniteColors.Gold, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Cloud Backup", color = IgniteColors.Text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (state.signedIn) state.email.ifBlank { "Google account connected" }
                    else "Keep your GymBros data safe with Google",
                    color = IgniteColors.MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse Cloud Backup" else "Expand Cloud Backup",
                tint = IgniteColors.MutedText
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(16.dp))

                if (!state.signedIn) {
                    ActionButton("Continue with Google", !state.busy, onSignIn)
                    Text(
                        "Your local data stays on this phone until you sign in. After sign-in, you can create a cloud backup and enable automatic backups.",
                        color = IgniteColors.MutedText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                } else {
                    SecureBackupSecuritySection(
                        state = state,
                        onSetRecoveryPassword = onSetRecoveryPassword,
                        onUnlockRecoveryPassword = onUnlockRecoveryPassword
                    )

                    if (state.shouldSuggestRestore) {
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(IgniteColors.GoldTint).padding(16.dp)
                        ) {
                            Text("Cloud backup found", color = IgniteColors.Text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "We found your Gym Bros backup from ${formatBackupTime(state.cloudBackupMillis)}. Restore it to bring your data back.",
                                color = IgniteColors.MutedText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            ActionButton(if (state.busy) "Restoring…" else "Restore my data", !state.busy, onRestore)
                            Text(
                                "Not now",
                                color = IgniteColors.Gold,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp)
                                    .clickable(enabled = !state.busy, onClick = onDismissRestoreSuggestion)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Automatic backup", color = IgniteColors.Text, fontWeight = FontWeight.SemiBold)
                            Text("Back up when GymBros goes to the background", color = IgniteColors.MutedText, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = state.autoBackup, onCheckedChange = onAutoBackupChange, enabled = !state.busy)
                    }

                    val stamp = maxOf(state.cloudBackupMillis, state.lastBackupMillis)
                    if (stamp > 0L) {
                        Text("Last backup: ${formatBackupTime(stamp)}", color = IgniteColors.MutedText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallAction("Backup Now", Icons.Outlined.CloudUpload, !state.busy, Modifier.weight(1f), onBackupNow)
                        SmallAction("Restore", Icons.Outlined.Restore, !state.busy, Modifier.weight(1f), onRestore)
                    }

                    if (state.recoveryPoints.isNotEmpty()) {
                        Spacer(Modifier.height(18.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { recoveryPointsExpanded = !recoveryPointsExpanded }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.History, null, tint = IgniteColors.Gold, modifier = Modifier.size(20.dp))
                            Text("Recovery points", color = IgniteColors.Text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${state.recoveryPoints.size} saved", color = IgniteColors.MutedText, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.weight(1f))
                            Icon(
                                if (recoveryPointsExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                contentDescription = if (recoveryPointsExpanded) "Collapse recovery points" else "Expand recovery points",
                                tint = IgniteColors.MutedText
                            )
                        }

                        AnimatedVisibility(
                            visible = recoveryPointsExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Text(
                                    "Keeps the 5 newest regular backups. Pinned backups are protected from rotation.",
                                    color = IgniteColors.MutedText,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 3.dp, bottom = 8.dp)
                                )

                                state.recoveryPoints.forEachIndexed { index, point ->
                                    RecoveryPointRow(
                                        point = point,
                                        newest = index == 0,
                                        enabled = !state.busy,
                                        onRestore = { pendingRestore = point },
                                        onRename = if (point.id == "__latest__") null else { { pendingRename = point; renameText = point.label } },
                                        onPin = if (point.id == "__latest__") null else { { onPinPoint(point.id, !point.pinned) } },
                                        onDelete = if (point.id == "__latest__") null else { { pendingDelete = point } }
                                    )
                                    if (index != state.recoveryPoints.lastIndex) Spacer(Modifier.height(7.dp))
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(IgniteColors.Surface3)
                            .clickable(enabled = !state.busy) { confirmDeleteAll = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Delete All Backups", color = IgniteColors.MutedText, fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(IgniteColors.GoldTint)
                            .clickable(enabled = !state.busy, onClick = onSignOut)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sign Out", color = IgniteColors.Gold, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        state.message?.let {
            Text(it, color = IgniteColors.MutedText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
        }
    }

    pendingRestore?.let { point ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore this recovery point?") },
            text = {
                Text(
                    "This will replace the current local Gym Bros data with the backup from ${formatBackupTime(point.createdAtMillis)}."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    onRestorePoint(point.id)
                }) { Text("Restore", color = IgniteColors.Gold, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } }
        )
    }

    pendingRename?.let { point ->
        AlertDialog(
            onDismissRequest = { pendingRename = null },
            title = { Text("Name recovery point") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(40) },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = { TextButton(onClick = { onRenamePoint(point.id, renameText); pendingRename = null }) { Text("Save", color = IgniteColors.Gold) } },
            dismissButton = { TextButton(onClick = { pendingRename = null }) { Text("Cancel") } }
        )
    }

    pendingDelete?.let { point ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete recovery point?") },
            text = { Text("${point.label.ifBlank { formatBackupTime(point.createdAtMillis) }} will be permanently removed from your cloud backups.") },
            confirmButton = { TextButton(onClick = { onDeletePoint(point.id); pendingDelete = null }) { Text("Delete", color = IgniteColors.Gold) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Delete all cloud backups?") },
            text = { Text("This permanently deletes your latest backup and every recovery point. Local Gym Bros data on this phone will stay untouched.") },
            confirmButton = { TextButton(onClick = { confirmDeleteAll = false; onDeleteAll() }) { Text("Delete all", color = IgniteColors.Gold) } },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RecoveryPointRow(
    point: CloudRecoveryPoint,
    newest: Boolean,
    enabled: Boolean,
    onRestore: () -> Unit,
    onRename: (() -> Unit)?,
    onPin: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(IgniteColors.Surface3)
            .padding(horizontal = 13.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(point.label.ifBlank { formatBackupTime(point.createdAtMillis) }, color = IgniteColors.Text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    if (newest) Text("LATEST", color = IgniteColors.Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    if (point.pinned) Text("PINNED", color = IgniteColors.Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                if (point.label.isNotBlank()) Text(formatBackupTime(point.createdAtMillis), color = IgniteColors.MutedText, style = MaterialTheme.typography.bodySmall)
                val details = buildString {
                    append(if (point.safety) "Safety backup" else if (point.automatic) "Automatic" else "Manual")
                    if (point.sizeBytes > 0L) append(" • ${formatBytes(point.sizeBytes)}")
                }
                Text(details, color = IgniteColors.MutedText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
            }
            Text("Restore", color = IgniteColors.Gold, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(enabled = enabled, onClick = onRestore).padding(6.dp))
        }
        if (onRename != null || onPin != null || onDelete != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                onRename?.let { Text("Rename", color = IgniteColors.MutedText, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable(enabled = enabled, onClick = it).padding(vertical = 6.dp)) }
                onPin?.let { Text(if (point.pinned) "Unpin" else "Pin", color = IgniteColors.MutedText, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable(enabled = enabled, onClick = it).padding(vertical = 6.dp)) }
                onDelete?.let { Text("Delete", color = IgniteColors.MutedText, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable(enabled = enabled, onClick = it).padding(vertical = 6.dp)) }
            }
        }
    }
}

private fun formatBackupTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576L -> String.format("%.1f MB", bytes / 1_048_576.0)
    bytes >= 1024L -> String.format("%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}

@Composable
private fun ActionButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val baseModifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(15.dp))
    val backgroundModifier = if (enabled) Modifier.background(IgniteBrushes.Heat) else Modifier.background(IgniteColors.Surface3)
    Box(
        modifier = baseModifier.then(backgroundModifier).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = IgniteColors.TextOnGradient, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.height(48.dp).clip(RoundedCornerShape(14.dp)).background(IgniteColors.Surface3)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = IgniteColors.Gold, modifier = Modifier.size(19.dp))
        Spacer(Modifier.size(7.dp))
        Text(text, color = IgniteColors.Text, style = MaterialTheme.typography.labelLarge)
    }
}
