package com.example.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.data.MedRepository
import com.example.model.ShortItem
import com.example.util.extractYouTubeVideoId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ShortsScreen(
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    LaunchedEffect(Unit) {
        MedRepository.fetchCatalog()
    }

    val shorts by MedRepository.shorts.collectAsState()
    var isMuted by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (shorts.isEmpty()) {
            EmptyShortsState(onBackClick = onBackClick)
        } else {
            val pagerState = rememberPagerState(
                initialPage = 0,
                pageCount = { shorts.size }
            )

            // beyondViewportPageCount = 3 preloads the next 3 videos for zero buffer lag on swipe
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 3,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val short = shorts[pageIndex]
                val isCurrentPage = pagerState.currentPage == pageIndex

                ShortPlayerItem(
                    short = short,
                    pageIndex = pageIndex,
                    totalCount = shorts.size,
                    isCurrentPage = isCurrentPage,
                    isMuted = isMuted,
                    onToggleMute = { isMuted = !isMuted },
                    onBackClick = onBackClick,
                    pagerState = pagerState
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ShortPlayerItem(
    short: ShortItem,
    pageIndex: Int,
    totalCount: Int,
    isCurrentPage: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onBackClick: () -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val videoId = remember(short.id, short.youtubeVideoId, short.youtubeUrl) {
        short.youtubeVideoId.ifBlank {
            extractYouTubeVideoId(short.youtubeUrl) ?: ""
        }
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isManuallyPaused by remember { mutableStateOf(false) }
    var tapFeedbackIcon by remember { mutableStateOf<String?>(null) }
    var hasLiked by remember(short.id) { mutableStateOf(false) }
    var showHeartAnim by remember { mutableStateOf(false) }

    // Clear feedback animation after brief delay
    LaunchedEffect(tapFeedbackIcon) {
        if (tapFeedbackIcon != null) {
            delay(750)
            tapFeedbackIcon = null
        }
    }

    LaunchedEffect(showHeartAnim) {
        if (showHeartAnim) {
            delay(900)
            showHeartAnim = false
        }
    }

    // Playback control on page change:
    // When page becomes active (or user scrolls back to it): start playing from the beginning (0:00).
    // When page is not active: pause video immediately so audio and video stop.
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            isManuallyPaused = false
            webViewRef?.evaluateJavascript("playVideo();", null)
        } else {
            webViewRef?.evaluateJavascript("pauseVideo();", null)
        }
    }

    // Update mute status dynamically
    LaunchedEffect(isMuted) {
        if (isCurrentPage) {
            if (isMuted) webViewRef?.evaluateJavascript("mute();", null) else webViewRef?.evaluateJavascript("unMute();", null)
        }
    }

    // Handle Android Lifecycle events (pause video when app is backgrounded)
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        webViewRef?.evaluateJavascript("pauseVideo();", null)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isCurrentPage && !isManuallyPaused) {
            webViewRef?.evaluateJavascript("playVideo();", null)
        }
    }

    // Clean up YouTubePlayer on disposal
    DisposableEffect(Unit) {
        onDispose {
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
    // Embedded YouTube Player
    Box(modifier = Modifier.fillMaxSize()) {
        if (videoId.isNotBlank()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setupYouTubeWebView(this)
                        loadDataWithBaseURL("https://www.youtube.com", buildYouTubeEmbedHtml(videoId), "text/html", "UTF-8", null)
                    }.also { webViewRef = it }
                }
            )
        }
    }
    
    if (short.youtubeUrl.isNotBlank() && videoId.isBlank()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "External video not supported in-app",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No Video Source Available",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }

        // Center Tap Feedback Icon Animation (Play / Pause indicator)
        AnimatedVisibility(
            visible = tapFeedbackIcon != null,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.7f),
            exit = fadeOut(tween(250)) + scaleOut(targetScale = 1.2f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (tapFeedbackIcon == "pause") Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        // Center Double-Tap Heart Animation
        AnimatedVisibility(
            visible = showHeartAnim,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.5f),
            exit = fadeOut(tween(300)) + scaleOut(targetScale = 1.4f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(90.dp)
            )
        }

        // Paused persistent badge when manually paused
        if (isManuallyPaused && tapFeedbackIcon == null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable {
                        isManuallyPaused = false
                        webViewRef?.evaluateJavascript("playVideo();", null)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text("Paused • Tap to resume", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Top Gradient & Header Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }

                    Text(
                        text = "⚡ Shorts",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2563EB).copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = "${pageIndex + 1} / $totalCount",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(onClick = onToggleMute) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Right-Side Vertical Floating Action Column (Like, Replay, Share)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Share Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .size(44.dp)
                        .clickable {
                            try {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, short.title)
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "${short.title}\n\n${short.youtubeUrl}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Short"))
                            } catch (_: Exception) {}
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Share",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Bottom Gradient & Metadata Overlay (Title Only - Lifted higher to avoid channel overlap)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 72.dp, top = 24.dp, bottom = 75.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Short Title Only
                Text(
                    text = short.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}





// 1. WebView Settings Configuration
@SuppressLint("SetJavaScriptEnabled")
fun setupYouTubeWebView(webView: WebView) {
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webView, true)

    val settings = webView.settings
    val rawUA = settings.userAgentString
    // Strip WebView signature to prevent YouTube bot blocking
    settings.userAgentString = rawUA.replace("; wv", "").replace("Version/4.0 ", "")

    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.databaseEnabled = true
    settings.mediaPlaybackRequiresUserGesture = false
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
}

// 2. YouTube / Shorts HTML Embed Template
fun buildYouTubeEmbedHtml(videoId: String): String {
    val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?playsinline=1&controls=1&fs=1&rel=0&enablejsapi=1&origin=https://www.youtube-nocookie.com&widget_referrer=https://www.youtube-nocookie.com"
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background-color: #000000;
                    overflow: hidden;
                }
                iframe {
                    width: 100%;
                    height: 100%;
                    border: 0;
                }
            </style>
        </head>
        <body>
            <iframe id="player" src="$embedUrl"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen"
                    referrerpolicy="strict-origin-when-cross-origin"
                    allowfullscreen>
            </iframe>
            <script>
                var iframe = document.getElementById('player');
                function postMessage(command) {
                    iframe.contentWindow.postMessage(JSON.stringify({
                        'event': 'command',
                        'func': command,
                        'args': []
                    }), '*');
                }
                window.playVideo = function() { postMessage('playVideo'); };
                window.pauseVideo = function() { postMessage('pauseVideo'); };
                window.mute = function() { postMessage('mute'); };
                window.unMute = function() { postMessage('unMute'); };
            </script>
        </body>
        </html>
    """.trimIndent()
}

@Composable
private fun EmptyShortsState(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Empty",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
