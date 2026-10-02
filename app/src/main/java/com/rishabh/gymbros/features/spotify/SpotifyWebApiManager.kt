package com.rishabh.gymbros.features.spotify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.text.Normalizer
import java.util.Locale

class SpotifyWebApiManager private constructor(context: Context) {

    data class Playlist(
        val id: String,
        val name: String,
        val uri: String,
        val ownerName: String,
        val imageUrl: String?
    )

    data class TrackSearchResult(
        val id: String,
        val name: String,
        val uri: String,
        val artistName: String,
        val albumName: String,
        val imageUrl: String?
    )

    data class UiState(
        val authorized: Boolean = false,
        val loading: Boolean = false,
        val playlists: List<Playlist> = emptyList(),
        val selectedPlaylist: Playlist? = null,
        val searchResults: List<TrackSearchResult> = emptyList(),
        val recentSearchTracks: List<TrackSearchResult> = emptyList(),
        val searchLoading: Boolean = false,
        val searchErrorMessage: String? = null,
        val checkedTrackUri: String? = null,
        val checkedPlaylistId: String? = null,
        val trackSaved: Boolean? = null,
        val membershipLoading: Boolean = false,
        val addSongLoading: Boolean = false,
        val actionMessage: String? = null,
        val errorMessage: String? = null
    )

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor()
    private val searchGeneration = AtomicLong()
    private val _state = MutableStateFlow(
        UiState(
            authorized = !preferences.getString(KEY_REFRESH_TOKEN, null).isNullOrBlank() && hasRequiredScopes(),
            selectedPlaylist = loadSelectedPlaylist(),
            recentSearchTracks = loadRecentSearchTracks()
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun openPlaylistAuthorization(context: Context) {
        val verifier = createCodeVerifier()
        val challenge = createCodeChallenge(verifier)
        val stateValue = UUID.randomUUID().toString()
        preferences.edit()
            .putString(KEY_CODE_VERIFIER, verifier)
            .putString(KEY_OAUTH_STATE, stateValue)
            .apply()

        val scopes = REQUIRED_SCOPES.joinToString(" ")
        val authorizationUri = Uri.parse(AUTH_URL).buildUpon()
            .appendQueryParameter("client_id", SpotifyManager.CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", WEB_REDIRECT_URI)
            .appendQueryParameter("scope", scopes)
            .appendQueryParameter("show_dialog", "true")
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("state", stateValue)
            .build()

        context.startActivity(Intent(Intent.ACTION_VIEW, authorizationUri))
    }

    fun handleCallback(uri: Uri) {
        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            _state.value = _state.value.copy(loading = false, errorMessage = "Spotify playlist access was not granted. Try authorizing again.")
            return
        }

        val expectedState = preferences.getString(KEY_OAUTH_STATE, null)
        val returnedState = uri.getQueryParameter("state")
        val code = uri.getQueryParameter("code")
        val verifier = preferences.getString(KEY_CODE_VERIFIER, null)

        if (expectedState.isNullOrBlank() || returnedState != expectedState || code.isNullOrBlank() || verifier.isNullOrBlank()) {
            _state.value = _state.value.copy(loading = false, errorMessage = "Spotify authorization response could not be verified.")
            return
        }

        _state.value = _state.value.copy(loading = true, errorMessage = null)
        executor.execute {
            try {
                val body = formEncode(
                    mapOf(
                        "client_id" to SpotifyManager.CLIENT_ID,
                        "grant_type" to "authorization_code",
                        "code" to code,
                        "redirect_uri" to WEB_REDIRECT_URI,
                        "code_verifier" to verifier
                    )
                )
                val json = requestJson(TOKEN_URL, "POST", body, null)
                saveTokens(json)
                preferences.edit().remove(KEY_CODE_VERIFIER).remove(KEY_OAUTH_STATE).apply()
                if (!hasRequiredScopes()) throw AuthorizationRequired()
                _state.value = _state.value.copy(authorized = hasRequiredScopes(), loading = false, errorMessage = null)
                fetchPlaylists()
            } catch (t: Throwable) {
                Log.e(TAG, "Spotify PKCE token exchange failed", t)
                _state.value = _state.value.copy(loading = false, errorMessage = friendlyApiError(t))
            }
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        preferences.edit()
            .putString(KEY_SELECTED_PLAYLIST_ID, playlist.id)
            .putString(KEY_SELECTED_PLAYLIST_NAME, playlist.name)
            .putString(KEY_SELECTED_PLAYLIST_URI, playlist.uri)
            .putString(KEY_SELECTED_PLAYLIST_OWNER, playlist.ownerName)
            .putString(KEY_SELECTED_PLAYLIST_IMAGE, playlist.imageUrl)
            .apply()
        _state.update { it.copy(selectedPlaylist = playlist, checkedPlaylistId = null, trackSaved = null) }
    }

    private fun loadSelectedPlaylist(): Playlist? {
        val id = preferences.getString(KEY_SELECTED_PLAYLIST_ID, null) ?: return null
        val name = preferences.getString(KEY_SELECTED_PLAYLIST_NAME, null) ?: return null
        val uri = preferences.getString(KEY_SELECTED_PLAYLIST_URI, null) ?: return null
        return Playlist(
            id = id,
            name = name,
            uri = uri,
            ownerName = preferences.getString(KEY_SELECTED_PLAYLIST_OWNER, "").orEmpty(),
            imageUrl = preferences.getString(KEY_SELECTED_PLAYLIST_IMAGE, null)
        )
    }

    fun fetchPlaylists() {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true, errorMessage = null)
        executor.execute {
            try {
                val token = validAccessToken()
                if (token == null) {
                    _state.value = _state.value.copy(authorized = false, loading = false, playlists = emptyList())
                    return@execute
                }

                val all = mutableListOf<Playlist>()
                var offset = 0
                do {
                    val json = requestJson("$API_URL/me/playlists?limit=50&offset=$offset", "GET", null, token)
                    val items = json.optJSONArray("items") ?: break
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val id = item.optString("id")
                        val name = item.optString("name")
                        val uri = item.optString("uri")
                        if (id.isBlank() || name.isBlank() || uri.isBlank()) continue
                        val owner = item.optJSONObject("owner")?.optString("display_name").orEmpty()
                        val images = item.optJSONArray("images")
                        val imageUrl = if (images != null && images.length() > 0) images.optJSONObject(0)?.optString("url") else null
                        all += Playlist(id, name, uri, owner, imageUrl)
                    }
                    offset += items.length()
                    if (items.length() < 50) break
                } while (offset < 1000)

                val savedSelection = _state.value.selectedPlaylist
                val refreshedSelection = savedSelection?.let { saved -> all.firstOrNull { it.id == saved.id } ?: saved }
                _state.value = _state.value.copy(
                    authorized = true,
                    loading = false,
                    playlists = all,
                    selectedPlaylist = refreshedSelection,
                    errorMessage = null
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Could not load Spotify playlists", t)
                _state.value = _state.value.copy(loading = false, errorMessage = friendlyApiError(t))
            }
        }
    }

    fun searchTracks(query: String) {
        val generation = searchGeneration.incrementAndGet()
        val cleanQuery = query.trim().take(200)
        if (cleanQuery.isBlank()) {
            _state.value = _state.value.copy(
                searchResults = emptyList(),
                searchLoading = false,
                searchErrorMessage = null
            )
            return
        }

        _state.value = _state.value.copy(searchLoading = true, searchErrorMessage = null)
        executor.execute {
            if (generation != searchGeneration.get()) return@execute
            try {
                val token = validAccessToken()
                if (token == null) {
                    updateSearch(generation) { it.copy(authorized = false, searchLoading = false,
                        searchResults = emptyList(), searchErrorMessage = "Allow Spotify access to search songs.") }
                    return@execute
                }

                fun searchPage(searchQuery: String): List<TrackSearchResult> {
                    val encoded = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.name())
                    // Spotify reduced Search's maximum page size to 10 in 2026.
                    val json = requestJson(
                        "$API_URL/search?q=$encoded&type=track&limit=10",
                        "GET",
                        null,
                        token
                    )
                    val items = json.optJSONObject("tracks")?.optJSONArray("items")
                    val results = mutableListOf<TrackSearchResult>()
                    if (items != null) {
                        for (i in 0 until items.length()) {
                            val item = items.optJSONObject(i) ?: continue
                            val id = item.optString("id")
                            val name = item.optString("name")
                            val uri = item.optString("uri")
                            if (id.isBlank() || name.isBlank() || uri.isBlank()) continue

                            val artists = item.optJSONArray("artists")
                            val artistNames = buildList {
                                if (artists != null) {
                                    for (artistIndex in 0 until artists.length()) {
                                        artists.optJSONObject(artistIndex)
                                            ?.optString("name")
                                            ?.takeIf { it.isNotBlank() }
                                            ?.let(::add)
                                    }
                                }
                            }.joinToString(", ")

                            val album = item.optJSONObject("album")
                            val albumName = album?.optString("name").orEmpty()
                            val images = album?.optJSONArray("images")
                            val imageUrl = if (images != null && images.length() > 0) {
                                images.optJSONObject(0)?.optString("url")?.takeIf { it.isNotBlank() }
                            } else null

                            results += TrackSearchResult(
                                id = id,
                                name = name,
                                uri = uri,
                                artistName = artistNames,
                                albumName = albumName,
                                imageUrl = imageUrl
                            )
                        }
                    }

                    return results
                }
                val direct = searchPage(cleanQuery)
                if (generation != searchGeneration.get()) return@execute
                val candidates = mutableListOf<TrackSearchResult>()
                candidates += _state.value.recentSearchTracks.filter { fuzzyMatches(cleanQuery, it) }
                // Spotify's public API has no spelling-correction option. Use a small
                // number of broader keyword searches, then reject unrelated candidates.
                val closeDirectMatches = direct.filter { fuzzyMatches(cleanQuery, it) }
                if (closeDirectMatches.isEmpty()) {
                    for (fallback in fallbackQueries(cleanQuery)) {
                        if (generation != searchGeneration.get()) return@execute
                        candidates += searchPage(fallback).filter { fuzzyMatches(cleanQuery, it) }
                    }
                }
                val results = (closeDirectMatches + candidates + direct).distinctBy { it.id }.take(20)
                if (generation != searchGeneration.get()) return@execute
                updateSearch(generation) { it.copy(
                    searchResults = results,
                    searchLoading = false,
                    searchErrorMessage = null
                ) }
            } catch (t: Throwable) {
                if (generation != searchGeneration.get()) return@execute
                Log.e(TAG, "Spotify track search failed", t)
                updateSearch(generation) { it.copy(
                    searchLoading = false,
                    searchResults = emptyList(),
                    searchErrorMessage = friendlyApiError(t)
                ) }
            }
        }
    }

    private fun updateSearch(generation: Long, transform: (UiState) -> UiState) {
        _state.update { if (generation == searchGeneration.get()) transform(it) else it }
    }

    fun clearTrackSearch() {
        searchGeneration.incrementAndGet()
        _state.value = _state.value.copy(
            searchResults = emptyList(),
            searchLoading = false,
            searchErrorMessage = null
        )
    }

    fun rememberSearchedTrack(track: TrackSearchResult) {
        val recent = (listOf(track) + _state.value.recentSearchTracks.filterNot { it.id == track.id }).take(5)
        val json = org.json.JSONArray()
        recent.forEach { item -> json.put(JSONObject().put("id", item.id).put("name", item.name)
            .put("uri", item.uri).put("artist", item.artistName).put("album", item.albumName)
            .put("image", item.imageUrl)) }
        preferences.edit().putString(KEY_RECENT_SEARCH_TRACKS, json.toString()).apply()
        _state.update { it.copy(recentSearchTracks = recent) }
    }

    fun clearRecentSearchTracks() {
        preferences.edit().remove(KEY_RECENT_SEARCH_TRACKS).apply()
        _state.update { it.copy(recentSearchTracks = emptyList()) }
    }

    private fun loadRecentSearchTracks(): List<TrackSearchResult> = runCatching {
        val json = org.json.JSONArray(preferences.getString(KEY_RECENT_SEARCH_TRACKS, "[]"))
        buildList {
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                if (!TRACK_URI.matches(item.optString("uri"))) continue
                add(TrackSearchResult(item.getString("id"), item.getString("name"), item.getString("uri"),
                    item.optString("artist"), item.optString("album"),
                    item.optString("image").takeIf { it.isNotBlank() && it != "null" }))
            }
        }.distinctBy { it.id }.take(5)
    }.getOrDefault(emptyList())

    private fun searchWords(value: String): List<String> = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
        .split(Regex("[^\\p{L}\\p{N}]+" )).filter { it.isNotBlank() }

    private fun fallbackQueries(query: String): List<String> {
        val words = searchWords(query)
        return if (words.size > 1) words.indices.map { skip -> words.filterIndexed { index, _ -> index != skip }.joinToString(" ") }
            .distinct().take(2)
        else words.singleOrNull()?.takeIf { it.length >= 5 }?.let { listOf(it.take((it.length / 2).coerceAtLeast(3))) }.orEmpty()
    }

    private fun fuzzyMatches(query: String, track: TrackSearchResult): Boolean {
        val queryWords = searchWords(query)
        val candidateWords = searchWords("${track.name} ${track.artistName}")
        return queryWords.isNotEmpty() && queryWords.all { word -> candidateWords.any { candidate ->
            word == candidate || (word.length >= 3 && candidate.startsWith(word)) ||
                (word.length >= 3 && editDistance(word, candidate) <= if (word.length >= 6) 2 else 1)
        } }
    }

    // Damerau-Levenshtein also handles adjacent swapped letters ("ligths").
    private fun editDistance(a: String, b: String): Int {
        if (kotlin.math.abs(a.length - b.length) > 2) return 3
        val distances = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) distances[i][0] = i
        for (j in 0..b.length) distances[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            distances[i][j] = minOf(distances[i - 1][j] + 1, distances[i][j - 1] + 1,
                distances[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1])
                distances[i][j] = minOf(distances[i][j], distances[i - 2][j - 2] + 1)
        }
        return distances[a.length][b.length]
    }

    fun clearActionMessage() {
        _state.value = _state.value.copy(actionMessage = null)
    }

    /** Refresh membership for the exact track/playlist pair, never a local optimistic flag. */
    fun refreshTrackMembership(trackUri: String?) {
        val playlistId = _state.value.selectedPlaylist?.id
        _state.update { it.copy(checkedTrackUri = trackUri, checkedPlaylistId = playlistId,
            trackSaved = null, membershipLoading = false) }
        if (trackUri == null || playlistId == null || !TRACK_URI.matches(trackUri) || !PLAYLIST_ID.matches(playlistId) || !hasRequiredScopes()) return
        _state.update { it.copy(membershipLoading = true) }
        executor.execute {
            try {
                val token = validAccessToken() ?: throw AuthorizationRequired()
                val saved = containsTrack(playlistId, trackUri, token)
                publishMembership(playlistId, trackUri, saved)
            } catch (t: Exception) {
                Log.e(TAG, "Spotify membership check failed for playlist=$playlistId track=$trackUri", t)
                publishMembership(playlistId, trackUri, null)
            }
        }
    }

    private fun publishMembership(playlistId: String, trackUri: String, saved: Boolean?) {
        _state.update {
            if (it.selectedPlaylist?.id == playlistId && it.checkedPlaylistId == playlistId && it.checkedTrackUri == trackUri)
                it.copy(trackSaved = saved, membershipLoading = false) else it
        }
    }

    private fun containsTrack(playlistId: String, trackUri: String, token: String): Boolean {
        var offset = 0
        do {
            val page = requestJson("$API_URL/playlists/$playlistId/items?limit=50&offset=$offset", "GET", null, token)
            val items = page.getJSONArray("items")
            for (index in 0 until items.length()) {
                val entry = items.optJSONObject(index) ?: continue
                val track = entry.optJSONObject("item") ?: entry.optJSONObject("track") ?: continue
                if (track.optString("uri") == trackUri || track.optJSONObject("linked_from")?.optString("uri") == trackUri) return true
            }
            offset += items.length()
            // Follow every page; a missing track on the first page does not mean unsaved.
        } while (items.length() > 0 && !page.isNull("next") && page.optString("next").isNotBlank())
        return false
    }

    fun toggleTrackInSelectedPlaylist(context: Context, trackUri: String) {
        val playlist = _state.value.selectedPlaylist ?: run {
            _state.update { it.copy(actionMessage = "Select a playlist first.") }
            return
        }
        if (!TRACK_URI.matches(trackUri) || !PLAYLIST_ID.matches(playlist.id)) {
            _state.update { it.copy(actionMessage = "Choose a Spotify track and playlist first.") }
            return
        }
        if (_state.value.addSongLoading) return
        if (!hasRequiredScopes()) {
            _state.update { it.copy(authorized = false, actionMessage = "Allow playlist access, then tap Save again.") }
            openPlaylistAuthorization(context)
            return
        }
        _state.update { it.copy(addSongLoading = true, actionMessage = null) }
        executor.execute {
            try {
                val token = validAccessToken() ?: throw AuthorizationRequired()
                val userId = requestJson("$API_URL/me", "GET", null, token).getString("id")
                val details = requestJson("$API_URL/playlists/${playlist.id}", "GET", null, token)
                val ownerId = details.optJSONObject("owner")?.optString("id")
                if (ownerId != userId && !details.optBoolean("collaborative")) {
                    throw PlaylistNotEditable()
                }
                // Recheck before each mutation, including after failed requests or external edits.
                val alreadySaved = containsTrack(playlist.id, trackUri, token)
                val body = if (alreadySaved) {
                    JSONObject().put("items", org.json.JSONArray().put(JSONObject().put("uri", trackUri)))
                } else {
                    JSONObject().put("uris", org.json.JSONArray().put(trackUri))
                }
                requestJson("$API_URL/playlists/${playlist.id}/items",
                    if (alreadySaved) "DELETE" else "POST", body.toString(), token, "application/json")
                publishMembership(playlist.id, trackUri, !alreadySaved)
                _state.update { it.copy(addSongLoading = false,
                    actionMessage = if (alreadySaved) "Removed from ${playlist.name}" else "Added to ${playlist.name}") }
            } catch (t: Exception) {
                Log.e(TAG, "Spotify playlist update failed for playlist=${playlist.id} track=$trackUri", t)
                _state.update { it.copy(addSongLoading = false, actionMessage = friendlyApiError(t)) }
            }
        }
    }

    private class AuthorizationRequired : Exception()
    private class PlaylistNotEditable : Exception()
    private class ApiFailure(val status: Int) : Exception("Spotify HTTP $status")

    private fun friendlyApiError(t: Throwable): String = when {
        t is AuthorizationRequired -> "Reconnect Spotify playlist access and try again."
        t is PlaylistNotEditable -> "You can't edit this playlist. Select one you own or can collaborate on."
        t is ApiFailure && t.status == 403 -> "Spotify denied access. Check playlist permissions and your Spotify app access."
        t is ApiFailure && t.status == 404 -> "This playlist or track is no longer available."
        t is ApiFailure && t.status == 429 -> "Spotify is busy. Please try again shortly."
        t is java.io.IOException -> "Couldn't reach Spotify. Check your connection and try again."
        else -> "Couldn't complete the Spotify request. Please try again."
    }

    private fun hasRequiredScopes(): Boolean {
        val granted = preferences.getString(KEY_GRANTED_SCOPES, "").orEmpty().split(" ").toSet()
        return granted.containsAll(REQUIRED_SCOPES)
    }
    private fun validAccessToken(): String? {
        if (!hasRequiredScopes()) return null
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null)
        val expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L)
        if (!accessToken.isNullOrBlank() && System.currentTimeMillis() < expiresAt - 60_000L) return accessToken

        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val body = formEncode(
            mapOf(
                "client_id" to SpotifyManager.CLIENT_ID,
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken
            )
        )
        val json = requestJson(TOKEN_URL, "POST", body, null)
        saveTokens(json, refreshToken)
        if (!hasRequiredScopes()) {
            _state.update { it.copy(authorized = false, trackSaved = null) }
            throw AuthorizationRequired()
        }
        return preferences.getString(KEY_ACCESS_TOKEN, null)
    }

    private fun saveTokens(json: JSONObject, existingRefreshToken: String? = null) {
        val accessToken = json.getString("access_token")
        val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() } ?: existingRefreshToken
        val expiresIn = json.optLong("expires_in", 3600L)
        val scopes = if (json.has("scope")) json.optString("scope")
            else if (existingRefreshToken != null) preferences.getString(KEY_GRANTED_SCOPES, "").orEmpty()
            else ""
        preferences.edit()
            .putString(KEY_GRANTED_SCOPES, scopes)
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + expiresIn * 1000L)
            .apply()
    }

    private fun requestJson(
        url: String,
        method: String,
        body: String?,
        bearerToken: String?,
        contentType: String = "application/x-www-form-urlencoded"
    ): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            if (!bearerToken.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $bearerToken")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType)
            }
        }
        return try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.use { input -> BufferedReader(InputStreamReader(input)).readText() }.orEmpty()
            if (status !in 200..299) {
                // Never log Authorization headers, access tokens, or token response bodies.
                Log.e(TAG, "$method ${url.substringBefore('?')} HTTP $status: ${if (url == TOKEN_URL) "Token exchange rejected" else response}")
                if (status == 401 || (status == 403 && response.contains("scope", ignoreCase = true)) ||
                    (url == TOKEN_URL && status == 400)) {
                    preferences.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).remove(KEY_GRANTED_SCOPES).apply()
                    _state.update { it.copy(authorized = false, trackSaved = null) }
                    throw AuthorizationRequired()
                }
                throw ApiFailure(status)
            }
            if (response.isBlank()) JSONObject() else JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun formEncode(values: Map<String, String>): String = values.entries.joinToString("&") {
        "${URLEncoder.encode(it.key, "UTF-8") }=${URLEncoder.encode(it.value, "UTF-8") }"
    }

    private fun createCodeVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun createCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    companion object {
        const val WEB_REDIRECT_URI = "gymbros://spotify/callback"
        private const val AUTH_URL = "https://accounts.spotify.com/authorize"
        private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
        private const val API_URL = "https://api.spotify.com/v1"
        private const val PREFS_NAME = "gymbros_spotify_web_api"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_GRANTED_SCOPES = "granted_scopes"
        private const val KEY_RECENT_SEARCH_TRACKS = "recent_search_tracks"
        private val REQUIRED_SCOPES = setOf("playlist-read-private", "playlist-read-collaborative", "playlist-modify-private", "playlist-modify-public")
        private val TRACK_URI = Regex("spotify:track:[A-Za-z0-9]{22}")
        private val PLAYLIST_ID = Regex("[A-Za-z0-9]{22}")
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_CODE_VERIFIER = "code_verifier"
        private const val KEY_OAUTH_STATE = "oauth_state"
        private const val KEY_SELECTED_PLAYLIST_ID = "selected_playlist_id"
        private const val KEY_SELECTED_PLAYLIST_NAME = "selected_playlist_name"
        private const val KEY_SELECTED_PLAYLIST_URI = "selected_playlist_uri"
        private const val KEY_SELECTED_PLAYLIST_OWNER = "selected_playlist_owner"
        private const val KEY_SELECTED_PLAYLIST_IMAGE = "selected_playlist_image"
        private const val TAG = "GymBrosSpotifyWeb"

        @Volatile private var INSTANCE: SpotifyWebApiManager? = null
        fun getInstance(context: Context): SpotifyWebApiManager = INSTANCE ?: synchronized(this) {
            INSTANCE ?: SpotifyWebApiManager(context.applicationContext).also { INSTANCE = it }
        }
    }
}
