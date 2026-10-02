package com.rishabh.gymbros.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishabh.gymbros.R
import com.rishabh.gymbros.features.settings.model.UiVersion
import com.rishabh.gymbros.ui.theme.IgniteColors
import com.rishabh.gymbros.ui.theme.Ui2Colors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GymBrosLaunchScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    uiVersion: UiVersion = UiVersion.V1
) {
    val isUi2 = uiVersion == UiVersion.V2
    val overlayAlpha = remember { Animatable(1f) }
    val iconAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.78f) }
    val titleAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val taglineOffset = remember { Animatable(18f) }
    val reveal = remember { Animatable(0f) }
    val ringAlpha = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (isUi2) {
            coroutineScope {
                launch { iconAlpha.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
                launch { iconScale.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
                launch { glowAlpha.animateTo(.72f, tween(1200, easing = FastOutSlowInEasing)) }
            }
            delay(160)
            coroutineScope {
                launch { reveal.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
                launch {
                    delay(980)
                    ringAlpha.snapTo(.9f)
                    ringAlpha.animateTo(0f, tween(650, easing = FastOutSlowInEasing))
                }
            }
            coroutineScope {
                launch { titleAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
                launch { taglineAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
                launch { taglineOffset.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
            }
            delay(620)
        } else {
            coroutineScope {
                launch { iconAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
                launch { iconScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
            }
            delay(90)
            titleAlpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
            delay(70)
            coroutineScope {
                launch { taglineAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) }
                launch { taglineOffset.animateTo(0f, tween(320, easing = FastOutSlowInEasing)) }
            }
            delay(700)
        }
        overlayAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(
                alpha = overlayAlpha.value
            )
            .background(if (isUi2) Ui2Colors.Background else IgniteColors.Background),
        contentAlignment = Alignment.Center
    ) {
        if (isUi2) {
            Column(
                modifier = Modifier.offset(y = (-8).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(270.dp)
                            .graphicsLayer(alpha = glowAlpha.value)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Ui2Colors.Primary.copy(alpha = .26f),
                                        Ui2Colors.Cyan.copy(alpha = .10f),
                                        Color.Transparent
                                    )
                                ),
                                RoundedCornerShape(135.dp)
                            )
                    )
                    Box(
                        Modifier
                            .size(194.dp)
                            .graphicsLayer(
                                alpha = ringAlpha.value,
                                scaleX = 1f + (1f - ringAlpha.value) * .35f,
                                scaleY = 1f + (1f - ringAlpha.value) * .35f
                            )
                            .border(2.dp, Ui2Colors.Primary, RoundedCornerShape(48.dp))
                    )
                    Image(
                        painter = painterResource(id = R.drawable.gymbros_launch_logo_transparent),
                        contentDescription = "GymBros",
                        colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                        modifier = Modifier
                            .size(184.dp)
                            .graphicsLayer(alpha = iconAlpha.value, scaleX = iconScale.value, scaleY = iconScale.value)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.gymbros_launch_logo_transparent),
                        contentDescription = null,
                        modifier = Modifier
                            .size(184.dp)
                            .graphicsLayer(alpha = iconAlpha.value)
                            .drawWithContent {
                                val visibleTop = size.height * (1f - reveal.value)
                                clipRect(top = visibleTop) { this@drawWithContent.drawContent() }
                            }
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "GYMBROS",
                    color = Ui2Colors.Primary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 5.sp,
                    modifier = Modifier.graphicsLayer(alpha = titleAlpha.value, translationY = (1f - titleAlpha.value) * 16f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "YOUR GO TO FITNESS TRACKER",
                    color = Ui2Colors.Text.copy(alpha = .78f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer(alpha = taglineAlpha.value, translationY = taglineOffset.value)
                )
            }
        } else {
            // Keep the original UI 1.0 launch composition unchanged.
            Box(
                modifier = Modifier.offset(y = (-28).dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "GymBros",
                    modifier = Modifier
                        .size(172.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .graphicsLayer(
                            alpha = iconAlpha.value,
                            scaleX = iconScale.value,
                            scaleY = iconScale.value
                        )
                )

                Text(
                    text = "GymBros",
                    color = IgniteColors.Text,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.offset(y = (-92).dp).graphicsLayer(
                        alpha = titleAlpha.value
                    )
                )

                Text(
                    text = "Your Daily Fitness tracker",
                    color = IgniteColors.HydrationAmber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = 66.dp).graphicsLayer(
                        alpha = taglineAlpha.value,
                        translationY = taglineOffset.value
                    )
                )
            }
        }
    }
}
