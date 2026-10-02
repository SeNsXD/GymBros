package com.rishabh.gymbros.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.rishabh.gymbros.features.cloud.CloudBackupState

@Composable
internal fun SecureBackupSecuritySection(
    state: CloudBackupState,
    onSetRecoveryPassword: (String) -> Unit,
    onUnlockRecoveryPassword: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    val setup = state.securitySetupRequired
    val unlock = state.securityUnlockRequired

    if (setup || unlock) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                if (setup) "Secure encrypted backup" else "Encrypted backups are locked",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Text(
                if (setup)
                    "Create a recovery password. GymBros encrypts your data on this phone before anything is uploaded to Google Drive."
                else
                    "Enter your recovery password to unlock backups on this device. The password is never stored in Google Drive.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    password = ""
                    confirm = ""
                    passwordVisible = false
                    confirmVisible = false
                    showDialog = true
                },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (setup) "Create recovery password" else "Unlock encrypted backups")
            }
            Spacer(Modifier.height(14.dp))
        }
    }

    if (showDialog) {
        val valid = if (setup) password.length >= 12 && password == confirm else password.isNotEmpty()
        AlertDialog(
            onDismissRequest = { if (!state.busy) showDialog = false },
            title = { Text(if (setup) "Recovery password" else "Unlock backups") },
            text = {
                Column {
                    Text(
                        if (setup)
                            "Use at least 12 characters or a memorable passphrase. You will need this password to restore your encrypted backup on a new phone. GymBros cannot recover it for you."
                        else
                            "Enter the recovery password you created when secure backup was set up."
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Recovery password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Hide recovery password" else "Show recovery password"
                                )
                            }
                        }
                    )
                    if (setup) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirm,
                            onValueChange = { confirm = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Confirm password") },
                            visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                    Icon(
                                        imageVector = if (confirmVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (confirmVisible) "Hide confirmation password" else "Show confirmation password"
                                    )
                                }
                            },
                            isError = confirm.isNotEmpty() && confirm != password
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid && !state.busy,
                    onClick = {
                        if (setup) onSetRecoveryPassword(password) else onUnlockRecoveryPassword(password)
                        showDialog = false
                        password = ""
                        confirm = ""
                        passwordVisible = false
                        confirmVisible = false
                    }
                ) { Text(if (setup) "Secure backups" else "Unlock") }
            },
            dismissButton = {
                TextButton(enabled = !state.busy, onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}
