package com.rishabh.gymbros.ui.home

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.rishabh.gymbros.ui.theme.Ui2Colors
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val MoveCardBackground = Color(0xFF12151A)
private val MoveCardBorder = Color(0xFF1F242C)
private val MoveHeaderText = Color(0xFFE6E9F0)
private val MoveChipBackground = Color.White.copy(alpha = 0.10f).compositeOver(MoveCardBackground)
private val MoveChipText = Color.White
private val MoveCalories = Color(0xFFFA2A55)
private val MoveCardio = Color(0xFF3FD6F5)
private val MoveTime = Color(0xFF8F7BFF)
private val MoveTimeLabel = Color(0xFFA898FF)
private val MoveTrack = Color(0xFF1C2027)
private val MovePercentText = Color(0xFFF2F4F8)
private val MoveCaptionText = Color(0xFF7B8194)
private val MoveTagBackground = Color(0xFF0D1014)
private val MoveTip = Color.White
private val MovePreviewBackground = Color(0xFF08090B)

private const val GaugeWidth = 290f
private const val GaugeHeight = 226f
private const val RingRadius = 78f
private const val SlotSweep = 104f
private const val DrawDurationMillis = 1_300
private val MoveEasing = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f)

/**
 * Stateless with respect to fitness data; only presentation animation is remembered.
 * Home's current navigation removes this from composition when leaving the tab, so
 * re-entering Home replays it. ON_RESUME also replays after app foregrounding.
 */
@Composable
fun TodaysMoveCard(
    caloriesDone: Int,
    caloriesGoal: Int,
    cardioKm: Float,
    cardioGoalKm: Float,
    minutesDone: Int,
    minutesGoal: Int,
    isRestDay: Boolean = false,
    onCenterClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inspectionMode = LocalInspectionMode.current
    var reducedMotion by remember(context, inspectionMode) {
        mutableStateOf(inspectionMode || motionDisabled(context))
    }
    var replayKey by remember { mutableIntStateOf(0) }

    // Observe accessibility/developer settings without polling or per-frame composition.
    DisposableEffect(context, inspectionMode) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reducedMotion = inspectionMode || motionDisabled(context)
            }
        }
        if (!inspectionMode) {
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                observer
            )
        }
        onDispose {
            if (!inspectionMode) context.contentResolver.unregisterContentObserver(observer)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        reducedMotion = inspectionMode || motionDisabled(context)
        replayKey++
    }

    val targets = MoveTargets(
        progress(caloriesDone.toFloat(), caloriesGoal.toFloat()),
        progress(cardioKm, cardioGoalKm),
        progress(minutesDone.toFloat(), minutesGoal.toFloat())
    )
    val calorieArc = remember { Animatable(0f) }
    val cardioArc = remember { Animatable(0f) }
    val timeArc = remember { Animatable(0f) }
    val centerProgress = remember { Animatable(0f) }
    val detailsAlpha = remember { Animatable(0f) }
    val replay = remember { MoveReplay() }

    // One owner for all animatables: data updates cancel old targets but preserve
    // current values; only a new replay key resets them to zero.
    LaunchedEffect(replayKey, targets, reducedMotion) {
        val restart = replay.lastKey != replayKey
        replay.lastKey = replayKey
        if (reducedMotion) {
            calorieArc.snapTo(targets.calories)
            cardioArc.snapTo(targets.cardio)
            timeArc.snapTo(targets.time)
            centerProgress.snapTo(targets.average)
            detailsAlpha.snapTo(1f)
            return@LaunchedEffect
        }
        if (restart) {
            calorieArc.snapTo(0f)
            cardioArc.snapTo(0f)
            timeArc.snapTo(0f)
            centerProgress.snapTo(0f)
            detailsAlpha.snapTo(0f)
        }
        coroutineScope {
            launch { calorieArc.animateTo(targets.calories, tween(DrawDurationMillis, easing = MoveEasing)) }
            launch {
                cardioArc.animateTo(targets.cardio, tween(DrawDurationMillis, if (restart) 150 else 0, MoveEasing))
            }
            launch {
                timeArc.animateTo(targets.time, tween(DrawDurationMillis, if (restart) 300 else 0, MoveEasing))
            }
            launch { centerProgress.animateTo(targets.average, tween(DrawDurationMillis, easing = MoveEasing)) }
            launch {
                // Matches the supplied mockup: fade starts at 1300 ms even though
                // the staggered final arc continues until 1600 ms.
                detailsAlpha.animateTo(1f, tween(400, if (restart) DrawDurationMillis else 0))
            }
        }
    }

    val kmLabel = formatMoveKm(cardioKm)
    val goalKmLabel = formatMoveKm(cardioGoalKm)
    val description = if (isRestDay) "Today's move. Rest Day." else "Today's move, ${(targets.average * 100).roundToInt()} percent complete. " +
            "Calories $caloriesDone of $caloriesGoal, cardio $kmLabel of $goalKmLabel kilometres, " +
            "time $minutesDone of $minutesGoal minutes."
    val measurer = rememberTextMeasurer(cacheSize = 128)
    val shape = RoundedCornerShape(22.dp)
    val latestOnCenterClick by rememberUpdatedState(onCenterClick)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description }
            .background(MoveCardBackground, shape)
            .border(0.5.dp, MoveCardBorder, shape)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Today's move", color = MoveHeaderText, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Text(
                "Keep it moving",
                color = MoveChipText,
                fontSize = 12.sp,
                modifier = Modifier
                    .background(MoveChipBackground, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(GaugeWidth / GaugeHeight)
                .pointerInput(Unit) {
                    // Goal editing now lives in one settings card opened from the
                    // centre of the activity rings. The old individual tag tap targets
                    // are intentionally removed.
                    detectTapGestures { offset ->
                        val scale = size.width / GaugeWidth
                        if (scale <= 0f) return@detectTapGestures
                        val dx = offset.x / scale
                        val dy = offset.y / scale
                        val fromCenterX = dx - 145f
                        val fromCenterY = dy - 112f
                        if ((fromCenterX * fromCenterX) + (fromCenterY * fromCenterY) <= 58f * 58f) {
                            latestOnCenterClick()
                        }
                    }
                }
        ) {
            val scale = size.width / GaugeWidth
            withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
                // All animated values are read only in draw, never in composition.
                val calorieValue = if (reducedMotion) targets.calories else calorieArc.value
                val cardioValue = if (reducedMotion) targets.cardio else cardioArc.value
                val timeValue = if (reducedMotion) targets.time else timeArc.value
                val percent = if (reducedMotion) targets.average else centerProgress.value
                val alpha = if (reducedMotion) 1f else detailsAlpha.value

                drawMoveSlot(8f, cardioValue, MoveCardio, alpha)
                drawMoveSlot(128f, calorieValue, MoveCalories, alpha)
                drawMoveSlot(248f, timeValue, MoveTime, alpha)

                if (isRestDay) {
                    // Two-line rest state stays optically centered in the activity ring.
                    drawMoveText(measurer, "Rest", 145f, 108f, 28f, MovePercentText)
                    drawMoveText(measurer, "Day", 145f, 136f, 28f, MovePercentText)
                } else {
                    drawMoveText(measurer, "${(percent * 100).roundToInt()}%", 145f, 120f, 42f, MovePercentText)
                    drawMoveText(measurer, "Complete", 145f, 142f, 12f, MoveCaptionText, FontWeight.Normal)
                }

                drawLine(MoveCardio.copy(alpha = 0.6f * alpha), Offset(220f, 68f), Offset(238f, 54f), 1f)
                drawLine(MoveTime.copy(alpha = 0.6f * alpha), Offset(70f, 68f), Offset(52f, 54f), 1f)
                drawLine(MoveCalories.copy(alpha = 0.6f * alpha), Offset(162f, 194f), Offset(204f, 204f), 1f)

                drawMoveTag(measurer, "$kmLabel km", 228f, 32f, 60f, MoveCardio, MoveCardio, alpha)
                drawMoveTag(measurer, "$minutesDone min", 2f, 32f, 52f, MoveTime, MoveTimeLabel, alpha)
                drawMoveTag(measurer, "$caloriesDone kcal", 204f, 192f, 60f, MoveCalories, MoveCalories, alpha)
            }
        }
    }
}

private fun DrawScope.drawMoveSlot(startFromTop: Float, progress: Float, color: Color, alpha: Float) {
    val start = startFromTop - 90f
    val origin = Offset(145f - RingRadius, 112f - RingRadius)
    val bounds = Size(RingRadius * 2, RingRadius * 2)
    val stroke = Stroke(width = 16f, cap = StrokeCap.Round)
    drawArc(MoveTrack, start, SlotSweep, false, origin, bounds, style = stroke)
    if (progress > 0f) {
        val sweep = SlotSweep * progress.coerceIn(0f, 1f)
        drawArc(color, start, sweep, false, origin, bounds, style = stroke)
        val radians = Math.toRadians((start + sweep).toDouble())
        drawCircle(
            MoveTip.copy(alpha = alpha),
            radius = 3.5f,
            center = Offset(145f + RingRadius * cos(radians).toFloat(), 112f + RingRadius * sin(radians).toFloat())
        )
    }
}

private fun DrawScope.drawMoveTag(
    measurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    width: Float,
    borderColor: Color,
    textColor: Color,
    alpha: Float
) {
    drawRoundRect(MoveTagBackground.copy(alpha = alpha), Offset(x, y), Size(width, 22f), CornerRadius(11f))
    drawRoundRect(
        borderColor.copy(alpha = 0.5f * alpha), Offset(x, y), Size(width, 22f),
        CornerRadius(11f), style = Stroke(1f)
    )
    drawMoveText(measurer, text, x + width / 2f, y + 15f, 12f, textColor.copy(alpha = alpha), maxWidth = width - 6f)
}

private fun DrawScope.drawMoveText(
    measurer: TextMeasurer,
    text: String,
    centerX: Float,
    baselineY: Float,
    fontPixels: Float,
    color: Color,
    weight: FontWeight = FontWeight.Medium,
    maxWidth: Float = Float.POSITIVE_INFINITY
) {
    // Gauge typography uses design-space pixels; the outer Canvas transform scales
    // both glyphs and geometry together. TalkBack receives the complete values.
    val style = TextStyle(fontSize = fontPixels.toSp(), fontWeight = weight)
    var layout = measurer.measure(text, style, softWrap = false, maxLines = 1)
    if (layout.size.width > maxWidth) {
        layout = measurer.measure(
            text, style.copy(fontSize = (fontPixels * maxWidth / layout.size.width).toSp()),
            softWrap = false, maxLines = 1
        )
    }
    drawText(layout, color = color, topLeft = Offset(centerX - layout.size.width / 2f, baselineY - layout.firstBaseline))
}

private fun progress(done: Float, goal: Float): Float =
    if (!done.isFinite() || !goal.isFinite() || goal <= 0f) 0f else (done / goal).coerceIn(0f, 1f)

private fun formatMoveKm(value: Float): String = when {
    !value.isFinite() -> "0"
    value % 1f == 0f -> String.format(Locale.US, "%.0f", value)
    else -> String.format(Locale.US, "%.1f", value)
}

private fun motionDisabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

private data class MoveTargets(val calories: Float, val cardio: Float, val time: Float) {
    val average: Float get() = (calories + cardio + time) / 3f
}

private class MoveReplay(var lastKey: Int = -1)

@Preview(name = "Today's move", widthDp = 340, showBackground = true)
@Composable
private fun TodaysMoveCardPreview() {
    MaterialTheme {
        TodaysMoveCard(
            caloriesDone = 160,
            caloriesGoal = 325,
            cardioKm = 2f,
            cardioGoalKm = 5f,
            minutesDone = 90,
            minutesGoal = 120,
            modifier = Modifier.background(MovePreviewBackground).padding(10.dp)
        )
    }
}