package com.rishabh.gymbros.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rishabh.gymbros.R
import com.rishabh.gymbros.ui.theme.IgniteColors

@Composable
internal fun AboutGymBros(onBack: () -> Unit, onTwitter: () -> Unit, onOtherApps: () -> Unit) {
    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = IgniteColors.Background) {
            Column(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(IgniteColors.Ember.copy(alpha = 0.09f), Color.Transparent), endY = 650f))
                    .safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.background(IgniteColors.Surface3, RoundedCornerShape(14.dp))) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = IgniteColors.Text)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text("About", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(40.dp))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.gymbros_about_icon), "GymBros logo",
                        Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)))
                    Spacer(Modifier.height(20.dp))
                    Text("GymBros", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Ignite", color = IgniteColors.MutedText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                    Text("Your daily fitness tracker.", color = IgniteColors.SoftText, style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 26.dp))
                    Text("Made by RIXH4BH", color = IgniteColors.Gold, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 14.dp))
                }
                Spacer(Modifier.height(34.dp))
                AboutAction("Twitter / X", onTwitter) {
                    Text("𝕏", style = MaterialTheme.typography.titleLarge, color = IgniteColors.Text)
                }
                Spacer(Modifier.height(10.dp))
                AboutAction("Try Widget Labs", onOtherApps) {
                    Image(painterResource(R.drawable.widgetlab_app_icon), null, Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)))
                }
                Spacer(Modifier.height(28.dp))
                Text("Local by default. Optional Google cloud backup.", color = IgniteColors.MutedText,
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text("Exercise data & illustrations: RepDB (repdb.co)", color = IgniteColors.MutedText,
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp))
            }
        }
    }
}

@Composable
private fun AboutAction(title: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.horizontalGradient(listOf(IgniteColors.Surface1, IgniteColors.Surface3)))
            .border(1.dp, IgniteColors.Hairline, RoundedCornerShape(28.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(16.dp))
        Text(title, color = IgniteColors.Text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
