package com.rishabh.gymbros.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishabh.gymbros.features.settings.model.UiVersion
import com.rishabh.gymbros.navigation.AppDestination
import com.rishabh.gymbros.ui.theme.Ui2Colors

// "Curved bar" navigation: the bar has a circular notch in its top edge and the
// Start button sits inside it. The active tab shows a colored icon, a colored
// label and a small dot underneath. No borders or divider lines.
private val BarHeight = 72.dp
private val BarCornerRadius = 22.dp
private val FabSize = 56.dp
// How far the Start button rises above the bar. This space is part of the bar's
// measured height, so screen content (e.g. the Spotify player) stops above the
// button instead of sitting underneath it.
private val FabOverhang = 22.dp
private val InactiveTint = Color(0xFF6B7086)
private val BarColor = Ui2Colors.CardRaised

@Composable
fun BottomTabBar(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
    onStartWorkout: () -> Unit = {},
    isWorkoutActive: Boolean = false,
    activeWorkoutScreenVisible: Boolean = false,
    contextAddExerciseVisible: Boolean = false,
    modifier: Modifier = Modifier,
    uiVersion: UiVersion = UiVersion.V2
) {
    val tabs = listOf(
        Triple(AppDestination.HOME, "Home", Icons.Outlined.Home),
        Triple(AppDestination.WATER, "Water", Icons.Outlined.WaterDrop),
        Triple(AppDestination.TRAIN, "Workout", Icons.Outlined.FitnessCenter),
        Triple(AppDestination.YOU, "Profile", Icons.Outlined.Person)
    )
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier.fillMaxWidth().height(FabOverhang + BarHeight + bottomInset)) {
        // Curved bar background. It extends under the system navigation area so
        // there is no visible seam at the bottom edge.
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(BarHeight + bottomInset)
                .align(Alignment.BottomCenter)
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val r = BarCornerRadius.toPx()
            // Notch circle is concentric with the Start button (center 6dp below the bar's top edge).
            val notchCenterY = 6.dp.toPx()
            val notchRadius = 32.dp.toPx()
            val notchRect = Rect(
                center = Offset(cx, notchCenterY),
                radius = notchRadius
            )
            val path = Path().apply {
                moveTo(0f, h)
                lineTo(0f, r)
                quadraticBezierTo(0f, 0f, r, 0f)
                lineTo(cx - 54.dp.toPx(), 0f)
                cubicTo(
                    cx - 40.dp.toPx(), 0f,
                    cx - 35.dp.toPx(), 6.dp.toPx(),
                    cx - 30.7.dp.toPx(), 15.dp.toPx()
                )
                // Concave arc dipping below the top edge (left point -> right point).
                arcTo(notchRect, 163.7f, -147.4f, false)
                cubicTo(
                    cx + 35.dp.toPx(), 6.dp.toPx(),
                    cx + 40.dp.toPx(), 0f,
                    cx + 54.dp.toPx(), 0f
                )
                lineTo(w - r, 0f)
                quadraticBezierTo(w, 0f, w, r)
                lineTo(w, h)
                close()
            }
            drawPath(path, BarColor)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(BarHeight + bottomInset)
                .align(Alignment.BottomCenter)
                .padding(start = 6.dp, end = 6.dp, bottom = bottomInset + 6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            NavItem(tabs[0], selected, onSelect, Modifier.weight(1f))
            NavItem(tabs[1], selected, onSelect, Modifier.weight(1f))
            Spacer(Modifier.weight(1.25f))
            NavItem(
                tabs[2],
                selected,
                onSelect,
                Modifier.weight(1f),
                forceInactive = activeWorkoutScreenVisible
            )
            NavItem(tabs[3], selected, onSelect, Modifier.weight(1f))
        }

        // Start button seated in the notch.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(FabSize)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(Ui2Colors.Primary)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onStartWorkout() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    contextAddExerciseVisible || (isWorkoutActive && activeWorkoutScreenVisible) -> Icons.Outlined.Add
                    isWorkoutActive -> Icons.Outlined.PlayArrow
                    else -> Icons.Outlined.Add
                },
                contentDescription = when {
                    contextAddExerciseVisible || (isWorkoutActive && activeWorkoutScreenVisible) -> "Add Exercise"
                    isWorkoutActive -> "Resume Workout"
                    else -> "Start Workout"
                },
                tint = com.rishabh.gymbros.ui.theme.GymBrosAccent.onAccent,
                modifier = Modifier.size(30.dp)
            )
        }

        Text(
            text = when {
                contextAddExerciseVisible || (isWorkoutActive && activeWorkoutScreenVisible) -> "Add Exercise"
                isWorkoutActive -> "Resume"
                else -> "Start"
            },
            color = Ui2Colors.Text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset + 9.dp)
        )
    }
}

@Composable
private fun NavItem(
    tab: Triple<AppDestination, String, ImageVector>,
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    forceInactive: Boolean = false
) {
    val active = !forceInactive && selected == tab.first

    // Each destination keeps its own accent when selected.
    val activeColor = when (tab.first) {
        AppDestination.WATER -> Ui2Colors.Cyan
        AppDestination.TRAIN -> Color(0xFFA78BFA)
        AppDestination.YOU -> Color(0xFFFA2A55)
        else -> Ui2Colors.Primary
    }
    val color = if (active) activeColor else InactiveTint

    Column(
        modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSelect(tab.first) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Icon(tab.third, tab.second, tint = color, modifier = Modifier.size(22.dp))
        Text(
            tab.second,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
        )
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (active) activeColor else Color.Transparent)
        )
    }
}