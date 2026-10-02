package com.rishabh.gymbros.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rishabh.gymbros.ui.theme.IgniteBrushes
import com.rishabh.gymbros.ui.theme.IgniteColors

private val CardShape = RoundedCornerShape(22.dp)
private val ButtonShape = RoundedCornerShape(13.dp)
private val ChipShape = RoundedCornerShape(100.dp)

@Composable
fun IgniteCard(
    modifier: Modifier = Modifier,
    borderColor: Color = IgniteColors.Hairline,
    brush: Brush = IgniteBrushes.Card,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .clip(CardShape)
            .background(brush)
            .border(1.dp, borderColor, CardShape)
            .then(clickableModifier)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun HeatButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(ButtonShape)
            .background(IgniteBrushes.Heat)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = IgniteColors.TextOnGradient,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GoldChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = ChipShape,
        color = IgniteColors.Surface2,
        border = BorderStroke(1.dp, IgniteColors.Hairline)
    ) {
        Text(
            text = text,
            color = IgniteColors.Gold,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun PercentageChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(IgniteColors.GoldTint)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = IgniteColors.Gold,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HeatProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(IgniteColors.Surface3)
    ) {
        if (safeProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(safeProgress)
                    .height(7.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(IgniteBrushes.Heat)
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = IgniteColors.Text,
            style = MaterialTheme.typography.titleMedium
        )
        action?.invoke(this)
    }
}

@Composable
fun QuietPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(ChipShape)
            .background(IgniteColors.Surface3)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = IgniteColors.MutedText,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
