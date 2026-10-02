package com.rishabh.gymbros.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rishabh.gymbros.ui.theme.Ui2Colors

@Composable
fun DialogActionPill(
    text: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = androidx.compose.foundation.shape.CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (destructive) Ui2Colors.Danger.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.12f),
            contentColor = if (destructive) Ui2Colors.Danger else Color.White,
            disabledContainerColor = if (destructive) Ui2Colors.Danger.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.06f),
            disabledContentColor = if (destructive) Ui2Colors.Danger.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.45f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}
