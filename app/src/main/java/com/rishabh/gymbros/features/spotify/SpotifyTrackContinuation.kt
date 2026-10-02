package com.rishabh.gymbros.features.spotify

/** Playlist handoff is driven by confirmed player states, never by the Play button. */
internal class SpotifyTrackContinuation {
    private data class Sample(val position: Long, val duration: Long, val paused: Boolean, val time: Long)
    private var trackUri: String? = null
    private var playlistUri: String? = null
    private var previous: Sample? = null
    val active: Boolean get() = trackUri != null

    fun arm(track: String, playlist: String?) {
        trackUri = track
        playlistUri = playlist
        previous = null
    }

    fun setPlaylist(playlist: String?) { playlistUri = playlist }
    fun resetObservation() { previous = null }
    fun clear() { trackUri = null; playlistUri = null; previous = null }

    fun observe(uri: String?, position: Long, duration: Long, paused: Boolean, repeatOne: Boolean, now: Long): String? {
        val target = trackUri ?: return null
        val old = previous
        // Only infer elapsed progress while the last state was actually playing.
        val reachedEnd = old != null && old.duration > 0 && !old.paused &&
            old.position + (now - old.time).coerceAtLeast(0) >= old.duration
        if (uri == target) {
            val stoppedAtEnd = paused && duration > 0 && position >= duration
            val resetAtEnd = old != null && old.position > 0 && position == 0L && reachedEnd
            if (!repeatOne && (stoppedAtEnd || resetAtEnd)) return finish()
            previous = Sample(position, duration, paused, now)
        } else if (old != null) {
            // An early change is a skip/another selection, not a natural completion.
            if (reachedEnd && !repeatOne) return finish()
            clear()
        }
        return null
    }

    private fun finish(): String? {
        val playlist = playlistUri
        clear() // consume once before issuing a playback command
        return playlist
    }
}
