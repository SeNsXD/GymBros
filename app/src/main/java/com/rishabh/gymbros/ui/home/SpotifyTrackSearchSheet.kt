package com.rishabh.gymbros.ui.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishabh.gymbros.R
import com.rishabh.gymbros.features.spotify.SpotifyManager
import com.rishabh.gymbros.features.spotify.SpotifyWebApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private object TrackSheetColors {
    val Background = Color(0xFF0A0A0C)
    val Panel = Color(0xFF1B1B1F)
    val Secondary = Color(0xFF232327)
    val Field = Color(0xFF141417)
    val Border = Color(0xFF2C2C31)
    val Text = Color(0xFFF3F0E9)
    val Muted = Color(0xFF96959B)
    val Amber = Color(0xFF1ED760)
    val Accent = Brush.linearGradient(listOf(Amber, Color(0xFF1DB954)))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
internal fun SpotifyTrackSearchSheet(
    state: SpotifyWebApiManager.UiState,
    player: SpotifyManager.UiState,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onClearRecent: () -> Unit,
    onAuthorize: () -> Unit,
    onQueue: (SpotifyWebApiManager.TrackSearchResult) -> Unit,
    onStopPreview: (String) -> Unit,
    onSelect: (SpotifyWebApiManager.TrackSearchResult) -> Unit
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val heading = remember { FontFamily(Font("fonts/sora.ttf", context.assets, weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)))) }
    val body = remember { FontFamily(listOf(400, 500, 600).map { weight ->
        Font("fonts/inter.ttf", context.assets, weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
    }) }
    var query by rememberSaveable { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    var lastSubmitted by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf(false) }
    val currentSearch by rememberUpdatedState(onSearch)
    val currentClear by rememberUpdatedState(onClearSearch)
    fun submit() {
        val clean = query.trim()
        if (clean.isNotEmpty()) {
            lastSubmitted = clean
            currentSearch(clean)
        }
    }
    LaunchedEffect(query, state.authorized) {
        val clean = query.trim()
        if (clean.isEmpty()) {
            lastSubmitted = null
            currentClear()
        } else if (state.authorized) {
            delay(300)
            if (lastSubmitted != clean) submit()
        }
    }
    DisposableEffect(Unit) {
        onDispose { currentClear() }
    }
    // Material's native sheet animation follows Android's animator-duration scale,
    // including the system setting to remove animations; it also handles IME insets.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = 430.dp,
        containerColor = TrackSheetColors.Panel,
        contentColor = TrackSheetColors.Text,
        scrimColor = TrackSheetColors.Background.copy(alpha = 0.82f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = { WindowInsets.safeDrawing },
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 16.dp).size(32.dp, 4.dp)
                .background(TrackSheetColors.Border, CircleShape))
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Add a track", fontFamily = heading, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Set the pace for today's workout", fontFamily = body, fontSize = 12.5.sp,
                        color = TrackSheetColors.Muted, modifier = Modifier.padding(top = 3.dp))
                }
                IconButton(onClick = onDismiss) {
                    Box(Modifier.size(32.dp).background(TrackSheetColors.Secondary, CircleShape)
                        .border(1.dp, TrackSheetColors.Border, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Close, "Close search", tint = TrackSheetColors.Muted, modifier = Modifier.size(17.dp))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            val inputShape = RoundedCornerShape(16.dp)
            BasicTextField(
                value = query,
                onValueChange = {
                    query = it.take(200)
                    lastSubmitted = null
                    currentClear() // invalidate older requests immediately, before debounce
                },
                singleLine = true,
                textStyle = TextStyle(fontFamily = body, fontSize = 14.sp, color = TrackSheetColors.Text),
                cursorBrush = SolidColor(TrackSheetColors.Amber),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit(); keyboard?.hide() },
                    onDone = { submit(); keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth().height(50.dp)
                    .onFocusChanged { focused = it.isFocused }
                    .shadow(if (focused) 8.dp else 0.dp, inputShape,
                        ambientColor = TrackSheetColors.Amber.copy(alpha = 0.2f),
                        spotColor = TrackSheetColors.Amber.copy(alpha = 0.2f))
                    .background(TrackSheetColors.Field, inputShape)
                    .border(1.5.dp, if (focused) TrackSheetColors.Amber else TrackSheetColors.Border, inputShape),
                decorationBox = { input ->
                    Row(Modifier.fillMaxSize().padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Search, null, tint = TrackSheetColors.Muted, modifier = Modifier.size(19.dp))
                        Box(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            if (query.isEmpty()) Text("Search songs or artists", fontFamily = body, fontSize = 14.sp, color = TrackSheetColors.Muted)
                            input()
                        }
                        if (query.isNotEmpty()) IconButton(onClick = { query = ""; currentClear() }) {
                            Box(Modifier.size(22.dp).background(TrackSheetColors.Secondary, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Close, "Clear search", tint = TrackSheetColors.Muted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (query.isBlank()) "Recently searched" else "Search results", fontFamily = body,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TrackSheetColors.Muted,
                    modifier = Modifier.weight(1f))
                if (query.isBlank() && state.recentSearchTracks.isNotEmpty()) {
                    Text(
                        text = "Clear all",
                        color = TrackSheetColors.Amber,
                        fontFamily = body,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onClearRecent)
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                    )
                } else if (state.searchLoading) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = TrackSheetColors.Amber, strokeWidth = 2.dp)
                }
            }
            Spacer(Modifier.height(8.dp))
            val tracks = if (query.isBlank()) state.recentSearchTracks else state.searchResults
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                if (!state.authorized) item {
                    TextButton(onClick = onAuthorize) {
                        Text("Allow Spotify access to search", color = TrackSheetColors.Amber, fontFamily = body)
                    }
                }
                state.searchErrorMessage?.let { message -> item {
                    Text(message, color = TrackSheetColors.Muted, fontFamily = body, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                } }
                if (tracks.isEmpty() && !state.searchLoading && state.searchErrorMessage == null) item {
                    Text(if (query.isBlank()) "Your last five selected songs will appear here."
                        else if (lastSubmitted == query.trim()) "No tracks found. Try another title or artist." else "Searching…",
                        fontFamily = body, fontSize = 12.sp, color = TrackSheetColors.Muted, modifier = Modifier.padding(vertical = 14.dp))
                }
                items(tracks, key = { it.id }) { track ->
                    val playing = player.connected && player.trackUri == track.uri && !player.isPaused
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(if (playing) TrackSheetColors.Secondary else Color.Transparent)
                        .clickable { selected = true; keyboard?.hide(); onSelect(track) }
                        .padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TrackArtwork(track.imageUrl)
                        Column(Modifier.weight(1f).padding(start = 11.dp, end = 4.dp)) {
                            Text(track.name, fontFamily = heading, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artistName, fontFamily = body, fontSize = 12.sp, color = TrackSheetColors.Muted,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                        }
                        IconButton(onClick = { onQueue(track) }) {
                            Box(Modifier.size(34.dp).background(TrackSheetColors.Accent, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Add,
                                    "Add ${track.name} to queue",
                                    tint = TrackSheetColors.Background,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }
                }
            }
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.spotify_logo), "Spotify", modifier = Modifier.size(13.dp),
                    colorFilter = ColorFilter.tint(Color(0xFF1ED760)))
                Text("Powered by your Spotify account", fontFamily = body, fontSize = 10.sp,
                    color = TrackSheetColors.Muted, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

private val artworkCache = android.util.LruCache<String, ImageBitmap>(30)

@Composable
private fun TrackArtwork(url: String?) {
    val bitmap by produceState<ImageBitmap?>(url?.let(artworkCache::get), url) {
        if (url != null && value == null) value = withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                try {
                    connection.inputStream.use { BitmapFactory.decodeStream(it, null,
                        BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap() }
                } finally { connection.disconnect() }
            }.getOrNull()?.also { artworkCache.put(url, it) }
        }
    }
    Box(Modifier.size(46.dp).clip(RoundedCornerShape(10.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF39301D), TrackSheetColors.Secondary))), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!, "Album artwork", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Outlined.MusicNote, null, tint = TrackSheetColors.Amber, modifier = Modifier.size(23.dp))
    }
}
