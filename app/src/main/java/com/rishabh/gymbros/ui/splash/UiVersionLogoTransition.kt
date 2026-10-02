package com.rishabh.gymbros.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rishabh.gymbros.R
import com.rishabh.gymbros.features.settings.model.UiVersion
import com.rishabh.gymbros.ui.theme.IgniteColors
import com.rishabh.gymbros.ui.theme.Ui2Colors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private val ClassicGlow = Color(0xFFFF7A00)
private val Ui2Glow = Color(0xFF38E89F)

private fun logoFor(version: UiVersion): Int =
    if (version == UiVersion.V2) R.drawable.gymbros_ui2_icon else R.drawable.gymbros_about_icon

private fun backgroundFor(version: UiVersion): Color =
    if (version == UiVersion.V2) Ui2Colors.Background else IgniteColors.Background

private fun glowFor(version: UiVersion): Color =
    if (version == UiVersion.V2) Ui2Glow else ClassicGlow

/**
 * Full-screen logo morph played when the interface version is switched in Settings.
 *
 * 1.0 -> 2.0 plays the orange logo turning into the blue/mint one, 2.0 -> 1.0 plays it in reverse.
 * The screen is fully covered before [onCovered] fires, so the actual UI swap happens out of sight.
 */
@Composable
fun UiVersionLogoTransition(
    from: UiVersion,
    to: UiVersion,
    onCovered: () -> Unit,
    onFinished: () -> Unit
) {
    val cover = remember { Animatable(0f) }
    val sweep = remember { Animatable(0f) }
    val latestOnCovered by rememberUpdatedState(onCovered)
    val latestOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(from, to) {
        cover.animateTo(1f, tween(durationMillis = 220, easing = LinearEasing))
        latestOnCovered()
        delay(140)
        sweep.animateTo(1f, tween(durationMillis = 800, easing = FastOutSlowInEasing))
        delay(360)
        cover.animateTo(0f, tween(durationMillis = 280, easing = LinearEasing))
        latestOnFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = cover.value }
            .drawBehind { drawRect(lerp(backgroundFor(from), backgroundFor(to), sweep.value)) }
            // Swallow touches while the transition is on screen.
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .drawBehind {
                    val glow = lerp(glowFor(from), glowFor(to), sweep.value)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glow.copy(alpha = 0.30f), Color.Transparent)
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .graphicsLayer {
                        val pop = 1f + 0.06f * sin(PI.toFloat() * sweep.value)
                        scaleX = pop
                        scaleY = pop
                    }
                    .clip(RoundedCornerShape(47.dp))
            ) {
                Image(
                    painter = painterResource(logoFor(from)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                Image(
                    painter = painterResource(logoFor(to)),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            val progress = sweep.value
                            if (progress > 0f) {
                                drawContent()
                                // Soft diagonal wipe, top-left -> bottom-right.
                                val front = -0.2f + 1.4f * progress
                                val solidUntil = (front - 0.12f).coerceIn(0f, 1f)
                                val clearFrom = (front + 0.12f).coerceIn(0f, 1f)
                                drawRect(
                                    brush = Brush.linearGradient(
                                        0f to Color.Black,
                                        solidUntil to Color.Black,
                                        clearFrom to Color.Transparent,
                                        1f to Color.Transparent,
                                        start = Offset.Zero,
                                        end = Offset(size.width, size.height)
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        }
                )
            }
        }
    }
}
