package com.rishabh.gymbros.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rishabh.gymbros.ui.components.GoldChip
import com.rishabh.gymbros.ui.components.IgniteCard
import com.rishabh.gymbros.ui.theme.IgniteBrushes
import com.rishabh.gymbros.ui.theme.IgniteColors

@Composable
fun ComingSoonScreen(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IgniteBrushes.Screen)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        IgniteCard(modifier = Modifier.width(330.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(IgniteColors.EmberTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = IgniteColors.Gold,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.size(18.dp))
                GoldChip(text = "Coming soon")
                Spacer(Modifier.size(13.dp))
                Text(
                    text = title,
                    color = IgniteColors.Text,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = message,
                    color = IgniteColors.MutedText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
