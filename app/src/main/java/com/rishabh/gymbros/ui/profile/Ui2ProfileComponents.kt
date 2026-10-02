package com.rishabh.gymbros.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishabh.gymbros.ui.theme.Ui2Colors
import com.rishabh.gymbros.ui.theme.Ui2Theme

// Tokens from the You mockup, confined to the V2 profile flow.
internal object Ui2ProfileColors {
    val Background = Ui2Colors.Background
    val Text = Color(0xFFF5F7FA)
    val MutedText = Color(0xFF8B91A1)
    val SoftText = MutedText
    val Surface1 = Ui2Colors.Card
    val Surface2 = Ui2Colors.CardRaised
    val Surface3 = Color.White.copy(alpha = .05f).compositeOver(Ui2Colors.Card)
    val CardBottom = Ui2Colors.Card
    val Gold = Ui2Colors.Primary
    val Ember = Ui2Colors.Primary
    val GoldTint = Ui2Colors.Primary.copy(alpha = .10f)
    val Hairline = Color.White.copy(alpha = .08f)
    val TextOnGradient = Color(0xFF06110C)
}
internal object Ui2ProfileBrushes {
    val Card = Brush.linearGradient(listOf(Color.White.copy(alpha = .075f), Color.White.copy(alpha = .02f)))
    val Heat = Brush.linearGradient(listOf(Ui2Colors.Primary, Ui2Colors.Cyan))
    val Screen = Brush.verticalGradient(listOf(Ui2Colors.Primary.copy(alpha = .035f).compositeOver(Ui2Colors.Background), Ui2Colors.Background))
}

@Composable
internal fun Ui2ProfileTheme(content: @Composable () -> Unit) {
    val assets = LocalContext.current.assets
    val font = remember(assets) {
        FontFamily(listOf(400, 500, 600, 700, 800).map { weight ->
            Font("fonts/manrope.ttf", assets, weight = FontWeight(weight),
                variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
        })
    }
    val base = MaterialTheme.typography
    val typography = remember(base, font) { base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = font), displayMedium = base.displayMedium.copy(fontFamily = font),
        displaySmall = base.displaySmall.copy(fontFamily = font), headlineLarge = base.headlineLarge.copy(fontFamily = font),
        headlineMedium = base.headlineMedium.copy(fontFamily = font), headlineSmall = base.headlineSmall.copy(fontFamily = font),
        titleLarge = base.titleLarge.copy(fontFamily = font), titleMedium = base.titleMedium.copy(fontFamily = font, fontSize = 16.sp),
        titleSmall = base.titleSmall.copy(fontFamily = font), bodyLarge = base.bodyLarge.copy(fontFamily = font),
        bodyMedium = base.bodyMedium.copy(fontFamily = font), bodySmall = base.bodySmall.copy(fontFamily = font, fontSize = 13.sp),
        labelLarge = base.labelLarge.copy(fontFamily = font), labelMedium = base.labelMedium.copy(fontFamily = font),
        labelSmall = base.labelSmall.copy(fontFamily = font)
    ) }
    Ui2Theme { MaterialTheme(typography = typography, content = content) }
}

@Composable
internal fun Ui2GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Ui2ProfileColors.Hairline,
    brush: Brush = Ui2ProfileBrushes.Card,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    radius: Dp = 28.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Column(modifier.clip(shape).background(Ui2Colors.Card).background(brush)
        .border(1.dp, borderColor, shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(contentPadding), content = content)
}

@Composable
internal fun Ui2SectionHeader(title: String) {
    Text(title.uppercase(), color = Ui2ProfileColors.MutedText, fontSize = 12.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.padding(horizontal = 6.dp))
}

@Composable
internal fun Ui2Divider() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = .06f)))
}

@Composable
internal fun Ui2CommunityTile(
    title: String, subtitle: String, icon: ImageVector, accent: Color,
    modifier: Modifier = Modifier, iconRes: Int? = null, onClick: () -> Unit
) {
    Ui2GlassCard(modifier.heightIn(min = 132.dp), radius = 26.dp, onClick = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Ui2ProfileColors.Surface3),
                contentAlignment = Alignment.Center) {
                if (iconRes != null) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(accent),
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                }
            }
            Icon(Icons.Outlined.OpenInNew, null, tint = Ui2ProfileColors.MutedText, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = Ui2ProfileColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Ui2ProfileColors.MutedText, fontSize = 12.5.sp)
    }
}
