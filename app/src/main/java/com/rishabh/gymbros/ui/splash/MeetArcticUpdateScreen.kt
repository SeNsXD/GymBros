package com.rishabh.gymbros.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishabh.gymbros.R
import com.rishabh.gymbros.ui.theme.Ui2Colors
import kotlinx.coroutines.delay

/** Native version of the supplied “Meet Arctic” update presentation. */
@Composable
fun MeetArcticUpdateScreen(onFinished: () -> Unit) {
    var stage by remember { mutableStateOf(0) }
    var replayKey by remember { mutableStateOf(0) }

    LaunchedEffect(replayKey) {
        stage = 0
        delay(100)
        stage = 1                 // Things just got / out of hand
        delay(2800)
        stage = 2                 // Meet the new / Gym Bros / Arctic
        delay(4200)
        stage = 3                 // Stay Cool, / Lift Hard
    }

    Box(
        Modifier.fillMaxSize().background(Ui2Colors.Background),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(320.dp).background(
                Brush.radialGradient(
                    if (stage <= 1) listOf(Color(0x33FF7A3C), Color.Transparent)
                    else listOf(Ui2Colors.Cyan.copy(alpha = .18f), Ui2Colors.Primary.copy(alpha = .10f), Color.Transparent)
                ), CircleShape
            )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.gymbros_launch_logo_transparent),
                contentDescription = "GymBros Arctic",
                modifier = Modifier.size(190.dp)
            )
            Spacer(Modifier.height(34.dp))

            AnimatedVisibility(stage == 1, enter = fadeIn() + slideInVertically { it / 3 }, exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Things just got", color = Ui2Colors.Text, fontSize = 25.sp, fontWeight = FontWeight.Medium)
                    Text("out of hand", color = Color(0xFFFF7A3C), fontSize = 42.sp, fontWeight = FontWeight.Black)
                }
            }
            AnimatedVisibility(stage == 2, enter = fadeIn() + scaleIn(initialScale = .92f), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Meet the new", color = Ui2Colors.Muted, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("GYM BROS", color = Ui2Colors.Text, fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Text("ARCTIC", color = Ui2Colors.Cyan, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 7.sp)
                }
            }
            AnimatedVisibility(stage >= 3, enter = fadeIn() + slideInVertically { it / 3 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("STAY COOL,", color = Ui2Colors.Text, fontSize = 29.sp, fontWeight = FontWeight.Black)
                    Text("LIFT HARD", color = Ui2Colors.Primary, fontSize = 43.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(26.dp))
                    Button(
                        onClick = onFinished,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(
                            Brush.horizontalGradient(listOf(Ui2Colors.Primary, Ui2Colors.Cyan))
                        )
                    ) {
                        Text("LET'S GO", color = Ui2Colors.Background, fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 34.dp, vertical = 14.dp))
                    }
                }
            }
        }

        Row(
            Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            repeat(3) { i ->
                val active = when (stage) { 0,1 -> i == 0; 2 -> i == 1; else -> i == 2 }
                Box(Modifier.size(if (active) 18.dp else 7.dp, 7.dp).clip(CircleShape)
                    .background(if (active) Ui2Colors.Cyan else Ui2Colors.Dim))
            }
        }
        Text(
            "Replay", color = Ui2Colors.Muted, fontSize = 13.sp,
            modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(18.dp)
                .clickable { replayKey++ }.padding(8.dp)
        )
    }
}
