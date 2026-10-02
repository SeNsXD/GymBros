package com.rishabh.gymbros.ui.splash

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Plays Rishabh's exact "Meet Arctic" update animation.
 *
 * UI 1.0 is retired; tapping Ignite in the retained interface selector opens this
 * animation instead of switching the running app back to the old interface.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArcticUpdateAnimation(onFinished: () -> Unit) {
    BackHandler(onBack = onFinished)

    val webViewHolder = remember { arrayOfNulls<WebView>(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(0xFF0A0B10))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(Color.rgb(10, 11, 16))
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = false
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val uri = request?.url ?: return false
                            if (uri.scheme == "gymbros" && uri.host == "arctic-done") {
                                onFinished()
                                return true
                            }
                            return false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            val uri = url?.let(Uri::parse) ?: return false
                            if (uri.scheme == "gymbros" && uri.host == "arctic-done") {
                                onFinished()
                                return true
                            }
                            return false
                        }
                    }
                    loadUrl("file:///android_asset/gymbros_meet_arctic.html")
                    webViewHolder[0] = this
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewHolder[0]?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            webViewHolder[0] = null
        }
    }
}
