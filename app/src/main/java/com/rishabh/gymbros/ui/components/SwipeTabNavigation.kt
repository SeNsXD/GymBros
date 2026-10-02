package com.rishabh.gymbros.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Finger-following page switcher for the bottom-bar tabs.
 *
 * While you drag, the current page and its neighbour move together under your finger (like the
 * X / Instagram tab pager). On release the pages settle with the speed of your flick: a quick flick
 * or a drag past about a third of the screen switches page, otherwise they spring back. You can
 * grab the pages again in the middle of the animation, drag back and forth freely, and the first /
 * last tab resist with a soft rubber-band.
 *
 * Tapping a bottom-bar item still slides between pages with a smooth ease.
 *
 * Only drags that nothing underneath has consumed are used, so horizontally scrolling rows,
 * sliders, text selection and vertical lists keep working exactly as before, and a mostly
 * vertical scroll never turns into a page swipe.
 *
 * [onSwipeSelect] is called once a swipe has finished and the new tab is on screen (do the same
 * bookkeeping as a bottom-bar tap there).
 */
@Composable
fun <T> SwipeTabPager(
    tabs: List<T>,
    selected: T,
    enabled: Boolean,
    onSwipeSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    val scope = rememberCoroutineScope()
    val state = remember { SwipePagerState(scope) }
    // The page sitting in the middle of the screen (can lag `selected` while a tap slides).
    var shown by remember { mutableStateOf(selected) }
    // Page sliding in after a bottom-bar tap (may be more than one tab away).
    var tapTarget by remember { mutableStateOf<T?>(null) }

    SideEffect {
        state.enabled = enabled
        state.canGo = { dir -> tabs.indexOf(shown) + dir in tabs.indices }
        state.onCommit = { dir ->
            val target = tabs.getOrNull(tabs.indexOf(shown) + dir)
            if (target != null) {
                shown = target
                state.offset = 0f
                onSwipeSelect(target)
            } else {
                state.offset = 0f
            }
        }
    }

    // Selection changed from outside (bottom-bar tap, back button, widget shortcut...).
    LaunchedEffect(selected) {
        if (selected == shown) {
            if (tapTarget != null) { tapTarget = null; state.offset = 0f }
            return@LaunchedEffect
        }
        val from = tabs.indexOf(shown)
        val to = tabs.indexOf(selected)
        if (from < 0 || to < 0 || state.offset != 0f || state.dragging || state.width <= 0f) {
            state.cancelAll()
            shown = selected
            tapTarget = null
            state.offset = 0f
            return@LaunchedEffect
        }
        val dir = if (to > from) 1 else -1
        tapTarget = selected
        animate(
            initialValue = 0f,
            targetValue = -dir * state.width,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) { value, _ -> state.offset = value }
        shown = selected
        tapTarget = null
        state.offset = 0f
    }

    val offsetSign by remember(state) {
        derivedStateOf { if (state.offset < 0f) -1 else if (state.offset > 0f) 1 else 0 }
    }
    val shownIndex = tabs.indexOf(shown)
    val target = tapTarget
    val neighborDir = when {
        target != null -> if (tabs.indexOf(target) > shownIndex) 1 else -1
        offsetSign < 0 -> 1
        offsetSign > 0 -> -1
        else -> 0
    }
    val neighbor: T? = when {
        target != null -> target
        neighborDir != 0 -> tabs.getOrNull(shownIndex + neighborDir)
        else -> null
    }

    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { state.width = it.width.toFloat() }
            .pageDrag(state)
    ) {
        tabs.forEach { tab ->
            key(tab) {
                val isShown = tab == shown
                if (isShown || (neighbor != null && tab == neighbor)) {
                    Box(
                        Modifier.fillMaxSize().graphicsLayer {
                            translationX = if (isShown) state.offset else state.offset + neighborDir * state.width
                        }
                    ) { content(tab) }
                }
            }
        }
    }
}

/** Drag + settle state shared by the gesture and the pager. */
@Stable
private class SwipePagerState(private val scope: CoroutineScope) {
    /** How far the middle page is pushed sideways, in px (negative = towards the next tab). */
    var offset by mutableFloatStateOf(0f)
    var width by mutableFloatStateOf(0f)
    var enabled = true
    var dragging = false
    var canGo: (Int) -> Boolean = { false }
    var onCommit: (Int) -> Unit = {}
    private var job: Job? = null

    fun cancelAll() {
        job?.cancel()
        job = null
        dragging = false
    }

    fun dragStart() {
        job?.cancel()
        job = null
        dragging = true
    }

    fun drag(delta: Float) {
        if (!dragging || width <= 0f) return
        val next = offset + delta
        val dir = if (next < 0f) 1 else if (next > 0f) -1 else 0
        // No page in that direction (first / last tab): follow the finger only a little.
        val factor = if (dir == 0 || canGo(dir)) 1f else 0.35f
        offset = (offset + delta * factor).coerceIn(-width, width)
    }

    fun dragEnd(velocity: Float, flingVelocity: Float) {
        if (!dragging) return
        dragging = false
        val dir = if (offset < 0f) 1 else if (offset > 0f) -1 else 0
        if (dir == 0 || width <= 0f || !canGo(dir)) {
            settle(0f, velocity, 0, false)
            return
        }
        val sameWay = (velocity < 0f) == (offset < 0f)
        val commit = if (abs(velocity) >= flingVelocity) sameWay else abs(offset) >= width * 0.35f
        settle(if (commit) -dir * width else 0f, velocity, dir, commit)
    }

    fun cancelDrag() {
        if (!dragging) return
        dragging = false
        settle(0f, 0f, 0, false)
    }

    private fun settle(target: Float, velocity: Float, dir: Int, commit: Boolean) {
        job?.cancel()
        job = scope.launch {
            animate(
                initialValue = offset,
                targetValue = target,
                initialVelocity = velocity,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = 500f,
                    visibilityThreshold = 0.5f
                )
            ) { value, _ -> offset = value.coerceIn(-width, width) }
            if (commit) onCommit(dir) else offset = 0f
            job = null
        }
    }
}

private fun Modifier.pageDrag(state: SwipePagerState): Modifier = pointerInput(state) {
    val slop = viewConfiguration.touchSlop
    val flingVelocity = 500.dp.toPx()

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!state.enabled || state.width <= 0f) return@awaitEachGesture

        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var total = Offset.Zero
        var locked = false
        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                // A second finger, or the pointer vanishing, ends our interest.
                if (event.changes.count { it.pressed } > 1) return@awaitEachGesture
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                // Something underneath (row, slider, list...) took this drag: leave it alone.
                if (change.isConsumed) return@awaitEachGesture
                if (!state.enabled) return@awaitEachGesture

                tracker.addPosition(change.uptimeMillis, change.position)
                val delta = change.positionChange()
                total += delta

                if (!locked) {
                    val ax = abs(total.x)
                    val ay = abs(total.y)
                    // Clearly vertical first: this is a scroll, never a page swipe.
                    if (ay > slop && ay > ax) return@awaitEachGesture
                    if (ax > slop && ax > ay * 1.5f) {
                        locked = true
                        state.dragStart()
                        val sign = if (total.x < 0f) -1f else 1f
                        state.drag(total.x - sign * slop)
                        if (change.pressed) change.consume()
                    }
                } else {
                    state.drag(delta.x)
                    if (change.pressed) change.consume()
                }

                if (!change.pressed) {
                    if (locked) state.dragEnd(tracker.calculateVelocity().x, flingVelocity)
                    return@awaitEachGesture
                }
            }
        } finally {
            // Gesture ended abnormally while dragging: let the pages settle back.
            if (state.dragging) state.cancelDrag()
        }
    }
}