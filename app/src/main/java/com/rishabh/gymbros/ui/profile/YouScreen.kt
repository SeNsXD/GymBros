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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
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
import com.rishabh.gymbros.ui.components.IgniteCard
import com.rishabh.gymbros.ui.components.SectionHeader
import com.rishabh.gymbros.ui.theme.IgniteBrushes
import com.rishabh.gymbros.ui.theme.IgniteColors
import com.rishabh.gymbros.ui.theme.GymBrosAccent
import com.rishabh.gymbros.ui.theme.Ui2Colors
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private const val KG_TO_LB = 2.2046226f
private const val CM_PER_INCH = 2.54f

@Composable
fun YouScreen(
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
            .background(if (settings.uiVersion == UiVersion.V2) Brush.verticalGradient(listOf(Ui2Colors.Background, Ui2Colors.Background)) else IgniteBrushes.Screen)
            .padding(horizontal = if (settings.uiVersion == UiVersion.V2) 18.dp else 20.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "You",
                color = if (settings.uiVersion == UiVersion.V2) Ui2Colors.Text else IgniteColors.Text,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Your GymBros profile and preferences",
                color = if (settings.uiVersion == UiVersion.V2) Ui2Colors.Muted else IgniteColors.MutedText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 3.dp)
            )

            Spacer(Modifier.height(22.dp))
            SectionHeader(title = "Profile & Account")
            Spacer(Modifier.height(10.dp))

            IgniteCard(
                modifier = Modifier.fillMaxWidth(),
                brush = if (settings.uiVersion == UiVersion.V2) {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.075f), Ui2Colors.Card, Ui2Colors.Card))
                } else {
                    Brush.linearGradient(listOf(IgniteColors.Ember.copy(alpha = 0.14f), IgniteColors.CardBottom, Color(0xFF11100F)))
                },
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp)
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
                                color = if (status == "Profile saved") IgniteColors.Gold else IgniteColors.MutedText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 9.dp)
                            )
                        }


                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader(title = "Connected accounts")
            Spacer(Modifier.height(10.dp))
            IgniteCard(modifier = Modifier.fillMaxWidth(), brush = if (settings.uiVersion == UiVersion.V2) Brush.linearGradient(listOf(Color.White.copy(alpha=.075f), Ui2Colors.Card)) else IgniteBrushes.Card) {
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
                Spacer(Modifier.height(8.dp))
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
            IgniteCard(modifier = Modifier.fillMaxWidth(), brush = if (settings.uiVersion == UiVersion.V2) Brush.linearGradient(listOf(Color.White.copy(alpha=.075f), Ui2Colors.Card)) else IgniteBrushes.Card) {
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
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader(title = "Interface")
            Spacer(Modifier.height(10.dp))
            InterfaceVersionCard(
                selected = settings.uiVersion,
                onSelected = onUiVersionChange
            )

            Spacer(Modifier.height(24.dp))
            SectionHeader(title = "Backup & sync")
            Spacer(Modifier.height(10.dp))
            CloudBackupCard(
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
            SectionHeader(title = "Community & support")
            Spacer(Modifier.height(10.dp))
            ExternalLinkCard(
                title = "GitHub",
                subtitle = "Follow GymBros development on GitHub",
                icon = {
                    Image(
                        painter = painterResource(R.drawable.github_icon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(IgniteColors.Gold),
                        modifier = Modifier.size(25.dp)
                    )
                },
                onClick = { openExternalLink(context, "https://github.com/SeNsXD") }
            )
            Spacer(Modifier.height(10.dp))
            ExternalLinkCard(
                title = "Join Discord",
                subtitle = "Join Discord for help and support",
                icon = {
                    Image(
                        painter = painterResource(R.drawable.discord_icon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(IgniteColors.Gold),
                        modifier = Modifier.size(25.dp)
                    )
                },
                onClick = { openExternalLink(context, "https://discord.gg/UJqeVBjY") }
            )

            Spacer(Modifier.height(24.dp))
            SectionHeader(title = "About")
            Spacer(Modifier.height(10.dp))
            ExternalLinkCard(
                title = "About GymBros",
                subtitle = "Ignite · Made by RIXH4BH",
                icon = {
                    Image(painterResource(com.rishabh.gymbros.R.drawable.gymbros_about_icon), null,
                        Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)))
                },
                onClick = { showAbout = true }
            )
        }
    }
    if (showAbout) {
        AboutGymBros(
            onBack = { showAbout = false },
            onTwitter = { openExternalLink(context, "https://x.com/rixh4bh") },
            onOtherApps = { openExternalLink(context, "https://github.com/SeNsXD/WidgetLab-v10/releases/tag/Altair") }
        )
    }
}
@Composable
private fun InterfaceVersionCard(
    selected: UiVersion,
    onSelected: (UiVersion) -> Unit
) {
    IgniteCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Interface Version",
                    color = IgniteColors.Text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(IgniteColors.Surface3)
                    .border(1.dp, IgniteColors.Hairline, RoundedCornerShape(50))
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
        color = if (selected) IgniteColors.TextOnGradient else IgniteColors.MutedText,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) IgniteColors.Gold else Color.Transparent)
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
            .clip(RoundedCornerShape(14.dp))
            .background(IgniteColors.Surface3)
            .border(1.dp, IgniteColors.Hairline, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = "$name icon",
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = IgniteColors.Text, fontWeight = FontWeight.SemiBold)
            Text(detail, color = IgniteColors.MutedText, style = MaterialTheme.typography.bodySmall)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(if (connected) IgniteColors.GoldTint else IgniteColors.Surface2)
                .clickable(onClick = onClick)
                .padding(horizontal = 13.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (connected) "Disconnect" else "Connect",
                color = if (connected) IgniteColors.Gold else IgniteColors.Text,
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(modifier = Modifier.size(122.dp), contentAlignment = Alignment.Center) {
                    // Profile page intentionally uses a clean avatar. XP progress belongs to Home only.
                    Box(Modifier.size(108.dp).clip(CircleShape).background(IgniteColors.Surface3), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Outlined.PersonOutline,
                            "Profile picture",
                            tint = IgniteColors.Gold,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }
                }
                Box(
                    modifier = Modifier
                        .offset(x = 2.dp, y = 2.dp)
                        .size(38.dp)
                        .background(IgniteColors.Background, CircleShape)
                        .border(1.5.dp, IgniteColors.Gold, CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = onImageClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.CameraAlt,
                        "Change profile picture",
                        tint = IgniteColors.Text,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    settings.userName.ifBlank { "Add your name" }.uppercase(Locale.getDefault()),
                    color = IgniteColors.Text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    settings.ageYears?.let { "$it years old" } ?: "Complete your profile",
                    color = IgniteColors.MutedText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(IgniteColors.Surface3)
                        .border(1.dp, IgniteColors.Hairline, RoundedCornerShape(100.dp))
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(Modifier.size(7.dp).background(IgniteColors.Gold, CircleShape))
                    Text(
                        "Stay Consistent",
                        color = IgniteColors.MutedText,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileStatCard(Icons.Outlined.PersonOutline, settings.ageYears?.toString() ?: "—", "Age", Modifier.weight(1f))
            ProfileStatCard(Icons.Outlined.MonitorWeight, "$weight kg", "Weight", Modifier.weight(1f))
            ProfileStatCard(Icons.Outlined.Straighten, "$feet' $inches\"", "Height", Modifier.weight(1f))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(IgniteColors.Surface3)
                .border(1.dp, IgniteColors.Hairline, RoundedCornerShape(14.dp))
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                if (expanded) "Collapse profile" else "Expand profile",
                tint = IgniteColors.Gold,
                modifier = Modifier.size(27.dp)
            )
        }
    }
}

@Composable
private fun ProfileStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(IgniteColors.Surface2.copy(alpha = .92f))
            .border(1.dp, IgniteColors.Hairline, RoundedCornerShape(15.dp))
            .padding(horizontal = 9.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IgniteColors.GoldTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = IgniteColors.Gold, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                value,
                color = IgniteColors.Text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(label, color = IgniteColors.MutedText, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}

@Composable
private fun ProfileSaveButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(IgniteBrushes.Heat)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Save, null, tint = IgniteColors.TextOnGradient, modifier = Modifier.size(22.dp))
            Text("Save profile", color = IgniteColors.TextOnGradient, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            Modifier.size(54.dp).clip(RoundedCornerShape(15.dp)).background(IgniteColors.GoldTint),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = IgniteColors.Gold, modifier = Modifier.size(27.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, color = IgniteColors.Text, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = IgniteColors.MutedText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
        }
        val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "dropdownRotation")
        Icon(
            Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = IgniteColors.Gold,
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
        leadingIcon = leadingIcon?.let { vector -> { Icon(vector, contentDescription = null, tint = IgniteColors.SoftText) } },
        shape = RoundedCornerShape(15.dp),
        colors = igniteTextFieldColors()
    )
}

@Composable
private fun igniteTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = IgniteColors.Text,
    unfocusedTextColor = IgniteColors.Text,
    focusedBorderColor = IgniteColors.Gold,
    unfocusedBorderColor = IgniteColors.Hairline,
    cursorColor = IgniteColors.Gold,
    focusedLabelColor = IgniteColors.Gold,
    unfocusedLabelColor = IgniteColors.MutedText,
    focusedContainerColor = IgniteColors.Surface3,
    unfocusedContainerColor = IgniteColors.Surface3
)

private data class HeightParseResult(
    val heightCm: Int?,
    val error: String? = null
)

private fun parseHeightCm(
    primary: String,
    secondary: String,
    unit: HeightUnit
): HeightParseResult {
    if (primary.isBlank() && secondary.isBlank()) return HeightParseResult(null)

    return when (unit) {
        HeightUnit.CM -> {
            val cm = primary.toIntOrNull()
            if (cm == null || cm !in 80..250) {
                HeightParseResult(null, "Enter a height between 80 and 250 cm.")
            } else {
                HeightParseResult(cm)
            }
        }
        HeightUnit.FT -> {
            val feet = primary.toIntOrNull()
            val inches = secondary.toIntOrNull()
            if (feet == null || inches == null || inches !in 0..11) {
                HeightParseResult(null, "Enter height using feet and inches (0–11 inches).")
            } else {
                val cm = ((feet * 12 + inches) * CM_PER_INCH).roundToInt()
                if (cm !in 80..250) {
                    HeightParseResult(null, "Enter a height between about 2 ft 8 in and 8 ft 2 in.")
                } else {
                    HeightParseResult(cm)
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
    IgniteCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
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
                    .background(IgniteColors.GoldTint),
                contentAlignment = Alignment.Center
            ) { icon() }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = IgniteColors.Text, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    color = IgniteColors.MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                Icons.Outlined.OpenInNew,
                contentDescription = null,
                tint = IgniteColors.MutedText,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

internal fun persistProfileImage(context: Context, source: Uri): String? {
    return runCatching {
        val profileDir = java.io.File(context.filesDir, "profile").apply { mkdirs() }
        // Use a new filename for every selection. Reusing the exact same URI means
        // remember(profileImageUri) sees no key change, so Compose keeps showing the
        // previous bitmap until another navigation event forces the screen to rebuild.
        val target = java.io.File(profileDir, "profile_picture_${System.currentTimeMillis()}")
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null

        // Keep only the newly selected profile picture.
        profileDir.listFiles()?.forEach { file ->
            if (file != target && file.name.startsWith("profile_picture")) file.delete()
        }
        Uri.fromFile(target).toString()
    }.getOrNull()
}


internal fun loadProfileBitmap(context: Context, uriString: String): androidx.compose.ui.graphics.ImageBitmap? {
    if (uriString.isBlank()) return null
    return runCatching {
        val uri = Uri.parse(uriString)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 720 || bounds.outHeight / sample > 720) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
        }
    }.getOrNull()
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
private fun SettingTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = IgniteColors.Gold
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = IgniteColors.Text,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                color = IgniteColors.MutedText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun UnitSectionLabel(text: String) {
    Text(
        text = text,
        color = IgniteColors.MutedText,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun QuickAddSettingRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = IgniteColors.Text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        StepperButton("−", onMinus)
        Text(
            text = value,
            color = IgniteColors.Gold,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(90.dp)
        )
        StepperButton("+", onPlus)
    }
}

@Composable
private fun HourSettingRow(
    label: String,
    hour: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val time = LocalTime.of(hour.coerceIn(0, 23), 0)
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
    QuickAddSettingRow(
        label = label,
        value = time,
        onMinus = onMinus,
        onPlus = onPlus
    )
}

@Composable
private fun StepperButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(IgniteColors.Surface3)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = IgniteColors.Gold,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AccentThemeChoice(
    label: String,
    theme: AccentTheme,
    selectedTheme: AccentTheme,
    onSelect: (AccentTheme) -> Unit
) {
    val preview = when (theme) {
        AccentTheme.ORANGE -> Color(0xFFFF7A00)
        AccentTheme.BLUE -> Color(0xFF4D9CFF)
        AccentTheme.LIGHT_GREEN -> Color(0xFF9EDB72)
        AccentTheme.MONOCHROME -> Color(0xFFF2F2F2)
    }
    val selected = theme == selectedTheme
    Column(
        modifier = Modifier.clickable { onSelect(theme) }.padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 42.dp else 38.dp)
                .clip(CircleShape)
                .background(preview)
                .border(if (selected) 3.dp else 1.dp, if (selected) IgniteColors.Text else IgniteColors.Hairline, CircleShape)
        )
        Spacer(Modifier.height(6.dp))
        Text(label, color = if (selected) IgniteColors.Text else IgniteColors.MutedText, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
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
            .background(if (selected) IgniteColors.GoldTint else IgniteColors.Surface3)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) IgniteColors.Gold else IgniteColors.MutedText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
