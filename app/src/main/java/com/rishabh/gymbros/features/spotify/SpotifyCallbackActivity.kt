package com.rishabh.gymbros.features.spotify

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.rishabh.gymbros.MainActivity

class SpotifyCallbackActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data?.let { SpotifyWebApiManager.getInstance(applicationContext).handleCallback(it) }
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
        finish()
    }
}
