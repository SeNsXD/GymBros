package com.rishabh.gymbros.ui.profile

import com.rishabh.gymbros.R

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rishabh.gymbros.features.settings.model.AppSettings
import com.rishabh.gymbros.features.settings.model.AccentTheme
import com.rishabh.gymbros.features.cloud.CloudBackupState
import com.rishabh.gymbros.features.spotify.SpotifyManager
import com.rishabh.gymbros.features.settings.model.HeightUnit
import com.rishabh.gymbros.features.settings.model.WaterUnit
import com.rishabh.gymbros.features.settings.model.WeightUnit
import com.rishabh.gymbros.features.settings.model.UiVersion
import com.rishabh.gymbros.features.settings.model.formatVolume
import com.rishabh.gymbros.ui.theme.GymBrosAccent
import com.rishabh.gymbros.ui.theme.Ui2Colors
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private const val KG_TO_LB = 2.2046226f
private const val CM_PER_INCH = 2.54f

@Composable
fun YouScreenV2(
    settings: AppSettings,
    onProfileSave: (String, Int?, Float?, Int?) -> Unit,
    onProfileImageChange: (String) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onQuickAddChange: (Int, Int) -> Unit,
    onQuickAddLabelChange: (Int, String) -> Unit,
    onWidgetButtonChange: (Int) -> Unit,
    onUnitChange: (WaterUnit) -> Unit,
    onWeightUnitChange: (WeightUnit) -> Unit,
    onHeightUnitChange: (HeightUnit) -> Unit,
    onAccentThemeChange: (AccentTheme) -> Unit,
    onUiVersionChange: (UiVersion) -> Unit,
    cloudBackupState: CloudBackupState,
    onCloudSignIn: () -> Unit,
    onCloudSignOut: () -> Unit,
    onCloudAutoBackupChange: (Boolean) -> Unit,
    onCloudBackupNow: () -> Unit,
    onCloudRestore: () -> Unit,
    onCloudRestorePoint: (String) -> Unit,
    onCloudRenamePoint: (String, String) -> Unit,
    onCloudPinPoint: (String, Boolean) -> Unit,
    onCloudDeletePoint: (String) -> Unit,
    onCloudDeleteAll: () -> Unit,
    onCloudSetRecoveryPassword: (String) -> Unit,
    onCloudUnlockRecoveryPassword: (String) -> Unit,
    onCloudDismissRestoreSuggestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Ui2ProfileTheme {

        var profileExpanded by rememberSaveable { mutableStateOf(false) }
        var nameDraft by rememberSaveable(settings.userName) { mutableStateOf(settings.userName) }
        var ageDraft by rememberSaveable(settings.ageYears) {
            mutableStateOf(settings.ageYears?.toString().orEmpty())
        }
        var weightDraft by rememberSaveable(settings.weightKg) {
            mutableStateOf(settings.weightKg?.let { formatWeightInput(it, WeightUnit.KG) }.orEmpty())
        }
        var heightPrimaryDraft by rememberSaveable(settings.heightCm) {
            mutableStateOf(heightPrimaryInput(settings.heightCm, HeightUnit.FT))
        }
        var heightSecondaryDraft by rememberSaveable(settings.heightCm) {
            mutableStateOf(heightSecondaryInput(settings.heightCm, HeightUnit.FT))
        }
        var profileStatus by rememberSaveable { mutableStateOf<String?>(null) }
        var localProfileImageUri by rememberSaveable(settings.profileImageUri) { mutableStateOf(settings.profileImageUri) }
        var unitsExpanded by rememberSaveable { mutableStateOf(false) }
        var showAbout by rememberSaveable { mutableStateOf(false) }

        val context = LocalContext.current
        val spotifyManager = remember(context) { SpotifyManager.getInstance(context.applicationContext) }
        val spotifyState by spotifyManager.state.collectAsState()
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        val profileImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { selected ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        selected,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                // Copy the picked image into app-private storage. Photo Picker URIs can be
                // temporary/provider-backed, so keeping our own copy makes the avatar render
                // immediately and survive app restarts without depending on URI permissions.
                val storedUri = persistProfileImage(context, selected) ?: selected.toString()
                localProfileImageUri = storedUri
                onProfileImageChange(storedUri)
                profileExpanded = true
            }
        }


        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Ui2Colors.Background)
                .drawBehind {
                    drawRect(Brush.radialGradient(
                        listOf(Ui2Colors.Primary.copy(alpha = .12f), Color.Transparent),
                        center = Offset(size.width, 0f), radius = 280.dp.toPx()))
                    drawRect(Brush.radialGradient(
                        listOf(Ui2Colors.Lavender.copy(alpha = .12f), Color.Transparent),
                        center = Offset(0f, 640.dp.toPx()), radius = 300.dp.toPx()))
                }
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "You",
                    color = Ui2ProfileColors.Text,
                    fontSize = 34.sp, fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Your GymBros profile and preferences",
                    color = Ui2ProfileColors.MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 3.dp)
                )

                Spacer(Modifier.height(22.dp))
                Ui2SectionHeader(title = "Profile & Account")
                Spacer(Modifier.height(10.dp))

                Ui2GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 34.dp,
                    contentPadding = PaddingValues(20.dp)
                ) {
                    ProfileHero(
                        settings = settings,
                        profileImageUri = localProfileImageUri,
                        expanded = profileExpanded,
                        onToggle = {
                            profileExpanded = !profileExpanded
                            profileStatus = null
                        },
                        onImageClick = {
                            profileImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )

                    AnimatedVisibility(
                        visible = profileExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            Spacer(Modifier.height(2.dp))
                            ProfileTextField(
                                value = nameDraft,
                                onValueChange = { nameDraft = it.take(30); profileStatus = null },
                                label = "Name",
                                placeholder = "Enter your name",
                                leadingIcon = Icons.Outlined.PersonOutline
                            )

                            Spacer(Modifier.height(10.dp))
                            ProfileTextField(
                                value = ageDraft,
                                onValueChange = { ageDraft = it.filter(Char::isDigit).take(3); profileStatus = null },
                                label = "Age",
                                placeholder = "e.g. 23",
                                keyboardType = KeyboardType.Number,
                                leadingIcon = Icons.Outlined.CalendarMonth
                            )

                            Spacer(Modifier.height(10.dp))
                            ProfileTextField(
                                value = weightDraft,
                                onValueChange = { weightDraft = sanitizeDecimal(it, 7); profileStatus = null },
                                label = "Weight (kg)",
                                placeholder = "e.g. 65",
                                keyboardType = KeyboardType.Decimal,
                                leadingIcon = Icons.Outlined.MonitorWeight
                            )

                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ProfileTextField(
                                    value = heightPrimaryDraft,
                                    onValueChange = { heightPrimaryDraft = it.filter(Char::isDigit).take(1); profileStatus = null },
                                    label = "Feet",
                                    placeholder = "5",
                                    keyboardType = KeyboardType.Number,
                                    leadingIcon = Icons.Outlined.Straighten,
                                    modifier = Modifier.weight(1f)
                                )
                                ProfileTextField(
                                    value = heightSecondaryDraft,
                                    onValueChange = { heightSecondaryDraft = it.filter(Char::isDigit).take(2); profileStatus = null },
                                    label = "Inches",
                                    placeholder = "5",
                                    keyboardType = KeyboardType.Number,
                                    leadingIcon = Icons.Outlined.Straighten,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(Modifier.height(14.dp))
                            ProfileSaveButton {
                                val age = ageDraft.toIntOrNull()
                                val enteredWeight = weightDraft.toFloatOrNull()
                                val weightKg = enteredWeight
                                val heightResult = parseHeightCm(heightPrimaryDraft, heightSecondaryDraft, HeightUnit.FT)
                                profileStatus = when {
                                    ageDraft.isNotBlank() && (age == null || age !in 10..120) -> "Enter an age between 10 and 120."
                                    weightDraft.isNotBlank() && (weightKg == null || weightKg !in 20f..500f) -> "Enter a weight between 20 and 500 kg."
                                    heightResult.error != null -> heightResult.error
                                    else -> {
                                        onProfileSave(nameDraft.trim(), age, weightKg, heightResult.heightCm)
                                        focusManager.clearFocus(force = true)
                                        keyboardController?.hide()
                                        profileExpanded = false
                                        "Profile saved"
                                    }
                                }
                            }
                            profileStatus?.let { status ->
                                Text(
                                    text = status,
                                    color = if (status == "Profile saved") Ui2ProfileColors.Gold else Ui2ProfileColors.MutedText,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 9.dp)
                                )
                            }


                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Ui2SectionHeader(title = "Connected accounts")
                Spacer(Modifier.height(10.dp))
                Ui2GlassCard(modifier = Modifier.fillMaxWidth()) {
                    AccountConnectionRow(
                        name = "Spotify",
                        iconRes = R.drawable.spotify_logo,
                        detail = if (spotifyState.connected) "Connected" else "Not connected",
                        connected = spotifyState.connected,
                        onClick = {
                            if (spotifyState.connected) spotifyManager.disconnect()
                            else spotifyManager.connect(context)
                        }
                    )
                    Ui2Divider()
                    AccountConnectionRow(
                        name = "Google",
                        iconRes = R.drawable.ic_google_account,
                        detail = if (cloudBackupState.signedIn) cloudBackupState.email.ifBlank { "Connected" } else "Not connected",
                        connected = cloudBackupState.signedIn,
                        onClick = {
                            if (cloudBackupState.signedIn) onCloudSignOut()
                            else onCloudSignIn()
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))
                Ui2SectionHeader(title = "Preferences")
                Spacer(Modifier.height(10.dp))
                Ui2GlassCard(modifier = Modifier.fillMaxWidth()) {
                    DropdownSettingHeader(
                        icon = Icons.Outlined.Straighten,
                        title = "Units",
                        subtitle = "${if (settings.unit == WaterUnit.ML) "ml" else "fl oz"} • ${if (settings.weightUnit == WeightUnit.KG) "kg" else "lb"} • ${if (settings.heightUnit == HeightUnit.FT) "ft" else "cm"}",
                        expanded = unitsExpanded,
                        onClick = { unitsExpanded = !unitsExpanded }
                    )
                    AnimatedVisibility(visible = unitsExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            UnitSectionLabel("Water")
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                UnitChoice("ml", settings.unit == WaterUnit.ML, Modifier.weight(1f)) { onUnitChange(WaterUnit.ML) }
                                UnitChoice("fl oz", settings.unit == WaterUnit.FL_OZ, Modifier.weight(1f)) { onUnitChange(WaterUnit.FL_OZ) }
                            }
                            Spacer(Modifier.height(14.dp))
                            UnitSectionLabel("Weight")
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                UnitChoice("kg", settings.weightUnit == WeightUnit.KG, Modifier.weight(1f)) { onWeightUnitChange(WeightUnit.KG) }
                                UnitChoice("lb", settings.weightUnit == WeightUnit.LB, Modifier.weight(1f)) { onWeightUnitChange(WeightUnit.LB) }
                            }
                            Spacer(Modifier.height(14.dp))
                            UnitSectionLabel("Height")
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                UnitChoice("ft", settings.heightUnit == HeightUnit.FT, Modifier.weight(1f)) { onHeightUnitChange(HeightUnit.FT) }
                                UnitChoice("cm", settings.heightUnit == HeightUnit.CM, Modifier.weight(1f)) { onHeightUnitChange(HeightUnit.CM) }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Ui2Divider()
                }

                Spacer(Modifier.height(24.dp))
                Ui2SectionHeader(title = "Backup & sync")
                Spacer(Modifier.height(10.dp))
                CloudBackupCardV2(
                    state = cloudBackupState,
                    onSignIn = onCloudSignIn,
                    onSignOut = onCloudSignOut,
                    onAutoBackupChange = onCloudAutoBackupChange,
                    onBackupNow = onCloudBackupNow,
                    onRestore = onCloudRestore,
                    onRestorePoint = onCloudRestorePoint,
                    onRenamePoint = onCloudRenamePoint,
                    onPinPoint = onCloudPinPoint,
                    onDeletePoint = onCloudDeletePoint,
                    onDeleteAll = onCloudDeleteAll,
                    onSetRecoveryPassword = onCloudSetRecoveryPassword,
                    onUnlockRecoveryPassword = onCloudUnlockRecoveryPassword,
                    onDismissRestoreSuggestion = onCloudDismissRestoreSuggestion
                )

                Spacer(Modifier.height(24.dp))
                Ui2SectionHeader(title = "Community & support")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Ui2CommunityTile("GitHub", "Follow development", Icons.Outlined.Code, Ui2Colors.Primary,
                        Modifier.weight(1f), iconRes = R.drawable.github_icon) { openExternalLink(context, "https://github.com/SeNsXD") }
                    Ui2CommunityTile("Discord", "Help & support", Icons.Outlined.Forum, Ui2Colors.Lavender,
                        Modifier.weight(1f), iconRes = R.drawable.discord_icon) { openExternalLink(context, "https://discord.gg/UJqeVBjY") }
                }
                Spacer(Modifier.height(12.dp))
                ExternalLinkCard(
                    title = "About GymBros",
                    subtitle = "Arctic · Made by RIXH4BH",
                    icon = {
                        Image(painterResource(com.rishabh.gymbros.R.drawable.gymbros_ui2_icon), null,
                            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)))
                    },
                    onClick = { showAbout = true }
                )
            }
        }
        if (showAbout) {
            AboutGymBrosV2(
                onBack = { showAbout = false },
                onTwitter = { openExternalLink(context, "https://x.com/rixh4bh") },
                onOtherApps = { openExternalLink(context, "https://github.com/SeNsXD/WidgetLab-v10/releases/tag/Altair") }
            )
        }
    }
}
@Composable
private fun InterfaceVersionCard(
    selected: UiVersion,
    onSelected: (UiVersion) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Interface version",
                    color = Ui2ProfileColors.Text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Ui2ProfileColors.Surface3)
                    .border(1.dp, Ui2ProfileColors.Hairline, RoundedCornerShape(50))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                InterfaceVersionChoice("Ignite", selected == UiVersion.V1) { onSelected(UiVersion.V1) }
                InterfaceVersionChoice("Arctic", selected == UiVersion.V2) { onSelected(UiVersion.V2) }
            }
        }
    }
}

@Composable
private fun InterfaceVersionChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (selected) Ui2ProfileColors.TextOnGradient else Ui2ProfileColors.MutedText,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Ui2ProfileBrushes.Heat else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
    )
}

@Composable
private fun AccountConnectionRow(
    name: String,
    detail: String,
    connected: Boolean,
    iconRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = "$name icon",
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Ui2ProfileColors.Surface3).padding(9.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = Ui2ProfileColors.Text, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Ui2ProfileColors.MutedText, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(if (connected) Ui2ProfileColors.Surface3 else Ui2ProfileColors.GoldTint)
                .clickable(onClick = onClick)
                .padding(horizontal = 13.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (connected) "Disconnect" else "Connect",
                color = if (connected) Ui2ProfileColors.Text else Ui2ProfileColors.Gold,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun ProfileHero(
    settings: AppSettings,
    profileImageUri: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onImageClick: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(profileImageUri) { loadProfileBitmap(context, profileImageUri) }
    val feet = heightPrimaryInput(settings.heightCm, HeightUnit.FT).ifBlank { "—" }
    val inches = heightSecondaryInput(settings.heightCm, HeightUnit.FT).ifBlank { "—" }
    val weight = settings.weightKg?.let { formatWeightInput(it, WeightUnit.KG) } ?: "—"
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(Modifier.size(98.dp).clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(Ui2Colors.Primary, Ui2Colors.Lavender, Ui2Colors.Cyan, Ui2Colors.Primary)))
                    .padding(3.dp).clip(CircleShape).background(Ui2Colors.Background),
                    contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        Image(bitmap, "Profile picture", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text(settings.userName.trim().take(1).uppercase().ifBlank { "G" },
                            fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Ui2Colors.Primary)
                    }
                }
                Box(Modifier.offset(3.dp, 3.dp).size(32.dp).clip(CircleShape)
                    .background(Ui2Colors.Card).border(2.dp, Ui2Colors.Background, CircleShape)
                    .clickable(onClick = onImageClick), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CameraAlt, "Change profile picture", tint = Ui2Colors.Primary, modifier = Modifier.size(16.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(settings.userName.ifBlank { "Your name" }.uppercase(Locale.getDefault()),
                    color = Ui2ProfileColors.Text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(settings.ageYears?.let { "$it years old" } ?: "Complete your profile",
                    color = Ui2ProfileColors.MutedText, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                Row(Modifier.padding(top = 10.dp).clip(RoundedCornerShape(50))
                    .background(Ui2Colors.Primary.copy(alpha = .10f))
                    .border(1.dp, Ui2Colors.Primary.copy(alpha = .25f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.size(6.dp).background(Ui2Colors.Primary, CircleShape))
                    Text("Stay Consistent", color = Ui2Colors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileStatCard(Icons.Outlined.PersonOutline, settings.ageYears?.toString() ?: "—", "Age", Ui2Colors.Primary, Modifier.weight(1f))
            ProfileStatCard(Icons.Outlined.MonitorWeight, "$weight kg", "Weight", Ui2Colors.Lavender, Modifier.weight(1f))
            ProfileStatCard(Icons.Outlined.Straighten, "$feet′ $inches″", "Height", Ui2Colors.Cyan, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = .04f))
            .border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(50))
            .clickable(onClick = onToggle).padding(13.dp), contentAlignment = Alignment.Center) {
            Text(if (expanded) "Close profile editor" else "Edit profile", color = Ui2ProfileColors.Text,
                fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String, label: String, accent: Color, modifier: Modifier = Modifier
) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .05f))
        .border(1.dp, Color.White.copy(alpha = .05f), RoundedCornerShape(20.dp))
        .padding(horizontal = 12.dp, vertical = 12.dp)) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(10.dp))
        Text(value, color = Ui2ProfileColors.Text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, color = Ui2ProfileColors.MutedText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProfileSaveButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Ui2ProfileBrushes.Heat)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Save, null, tint = Ui2ProfileColors.TextOnGradient, modifier = Modifier.size(22.dp))
            Text("Save profile", color = Ui2ProfileColors.TextOnGradient, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}




@Composable
private fun DropdownSettingHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Ui2ProfileColors.Surface3),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = Ui2Colors.Lavender, modifier = Modifier.size(27.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, color = Ui2ProfileColors.Text, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = Ui2ProfileColors.MutedText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
        }
        val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "dropdownRotation")
        Icon(
            Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = Ui2ProfileColors.Gold,
            modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = rotation }
        )
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        leadingIcon = leadingIcon?.let { vector -> { Icon(vector, contentDescription = null, tint = Ui2ProfileColors.SoftText) } },
        shape = RoundedCornerShape(15.dp),
        colors = igniteTextFieldColors()
    )
}

@Composable
private fun igniteTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Ui2ProfileColors.Text,
    unfocusedTextColor = Ui2ProfileColors.Text,
    focusedBorderColor = Ui2ProfileColors.Gold,
    unfocusedBorderColor = Ui2ProfileColors.Hairline,
    cursorColor = Ui2ProfileColors.Gold,
    focusedLabelColor = Ui2ProfileColors.Gold,
    unfocusedLabelColor = Ui2ProfileColors.MutedText,
    focusedContainerColor = Ui2ProfileColors.Surface3,
    unfocusedContainerColor = Ui2ProfileColors.Surface3
)

private data class Ui2HeightParseResult(
    val heightCm: Int?,
    val error: String? = null
)

private fun parseHeightCm(
    primary: String,
    secondary: String,
    unit: HeightUnit
): Ui2HeightParseResult {
    if (primary.isBlank() && secondary.isBlank()) return Ui2HeightParseResult(null)

    return when (unit) {
        HeightUnit.CM -> {
            val cm = primary.toIntOrNull()
            if (cm == null || cm !in 80..250) {
                Ui2HeightParseResult(null, "Enter a height between 80 and 250 cm.")
            } else {
                Ui2HeightParseResult(cm)
            }
        }
        HeightUnit.FT -> {
            val feet = primary.toIntOrNull()
            val inches = secondary.toIntOrNull()
            if (feet == null || inches == null || inches !in 0..11) {
                Ui2HeightParseResult(null, "Enter height using feet and inches (0–11 inches).")
            } else {
                val cm = ((feet * 12 + inches) * CM_PER_INCH).roundToInt()
                if (cm !in 80..250) {
                    Ui2HeightParseResult(null, "Enter a height between about 2 ft 8 in and 8 ft 2 in.")
                } else {
                    Ui2HeightParseResult(cm)
                }
            }
        }
    }
}

private fun heightPrimaryInput(heightCm: Int?, unit: HeightUnit): String {
    if (heightCm == null) return ""
    return when (unit) {
        HeightUnit.CM -> heightCm.toString()
        HeightUnit.FT -> {
            val totalInches = (heightCm / CM_PER_INCH).roundToInt()
            (totalInches / 12).toString()
        }
    }
}

private fun heightSecondaryInput(heightCm: Int?, unit: HeightUnit): String {
    if (heightCm == null || unit == HeightUnit.CM) return ""
    val totalInches = (heightCm / CM_PER_INCH).roundToInt()
    return (totalInches % 12).toString()
}

@Composable
private fun ExternalLinkCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Ui2GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Ui2ProfileColors.GoldTint),
                contentAlignment = Alignment.Center
            ) { icon() }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Ui2ProfileColors.Text, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    color = Ui2ProfileColors.MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                Icons.Outlined.OpenInNew,
                contentDescription = null,
                tint = Ui2ProfileColors.MutedText,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

private fun openExternalLink(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

private fun formatWeightInput(weightKg: Float, unit: WeightUnit): String {
    val value = if (unit == WeightUnit.KG) weightKg else weightKg * KG_TO_LB
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

private fun sanitizeDecimal(value: String, maxLength: Int): String {
    val filtered = value.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    val normalized = if (firstDot >= 0) {
        filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
    } else {
        filtered
    }
    return normalized.take(maxLength)
}

@Composable
private fun UnitSectionLabel(text: String) {
    Text(
        text = text,
        color = Ui2ProfileColors.MutedText,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun UnitChoice(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) Ui2ProfileColors.GoldTint else Ui2ProfileColors.Surface3)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Ui2ProfileColors.Gold else Ui2ProfileColors.MutedText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
