package com.rishabh.gymbros.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rishabh.gymbros.ui.theme.IgniteColors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Reusable animated hydration bottle used by both Home and Water.
 * The fill animates to [progress] and the surface gently waves unless system animations are disabled.
 */
@Composable
fun AnimatedWaterBottle(
    progress: Float,
    modifier: Modifier = Modifier,
    width: Dp = 92.dp,
    bodyHeight: Dp = 171.dp,
    capWidth: Dp = 34.dp,
    capHeight: Dp = 13.dp
) {
    val context = LocalContext.current
    val animationsEnabled = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        }.getOrDefault(true)
    }

    val animatedProgress = if (animationsEnabled) {
        val value by animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 700),
            label = "waterFill"
        )
        value
    } else {
        progress.coerceIn(0f, 1f)
    }

    val wavePhase = if (animationsEnabled && animatedProgress > 0f) {
        val transition = rememberInfiniteTransition(label = "waterWave")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wavePhase"
        )
        phase
    } else {
        0f
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .width(capWidth)
                .height(capHeight)
                .align(androidx.compose.ui.Alignment.CenterHorizontally)
                .clip(
                    RoundedCornerShape(
                        topStart = 5.dp,
                        topEnd = 5.dp,
                        bottomStart = 2.dp,
                        bottomEnd = 2.dp
                    )
                )
                .background(IgniteColors.Surface3)
                .border(1.dp, IgniteColors.Hairline, RoundedCornerShape(5.dp))
        )

        val topRadius = (width.value * 0.28f).dp.coerceAtLeast(12.dp)
        val bottomRadius = (width.value * 0.24f).dp.coerceAtLeast(10.dp)
        val bottleShape = RoundedCornerShape(
            topStart = topRadius,
            topEnd = topRadius,
            bottomStart = bottomRadius,
            bottomEnd = bottomRadius
        )

        Box(
            modifier = Modifier
                .width(width)
                .height(bodyHeight)
                .clip(bottleShape)
                .background(IgniteColors.Background)
                .border(2.dp, IgniteColors.BottleBorder, bottleShape)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                if (animatedProgress > 0f) {
                    val waterTop = size.height * (1f - animatedProgress)
                    val amplitude = if (width < 70.dp) 3.dp.toPx() else 5.dp.toPx()
                    val wavePath = Path().apply {
                        moveTo(0f, waterTop)
                        val steps = 32
                        for (step in 0..steps) {
                            val x = size.width * step / steps.toFloat()
                            val angle = (step / steps.toFloat()) * (PI * 4.0) + wavePhase
                            val y = waterTop + (sin(angle).toFloat() * amplitude)
                            lineTo(x, y)
                        }
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }

                    drawPath(
                        path = wavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                IgniteColors.Gold,
                                IgniteColors.Ember,
                                IgniteColors.DeepEmber
                            ),
                            startY = waterTop,
                            endY = size.height
                        )
                    )
                }

                val tickColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.30f)
                listOf(0.20f, 0.40f, 0.60f, 0.80f).forEach { fraction ->
                    val y = size.height * fraction
                    drawLine(
                        color = tickColor,
                        start = Offset(size.width - 13.dp.toPx(), y),
                        end = Offset(size.width - 5.dp.toPx(), y),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }
    }
}
