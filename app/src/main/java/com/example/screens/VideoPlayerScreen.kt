package com.example.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.util.extractYouTubeVideoId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    title: String,
    videoUrl: String,
    chapterName: String = "",
    subjectName: String = "",
    folderId: String = "",
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val ytVideoId = remember(videoUrl) {
        extractYouTubeVideoId(videoUrl)
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isTheaterMode by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }

    // Handle Lifecycle: pause video when app goes to background
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        webViewRef?.evaluateJavascript("pauseVideo();", null)
        webViewRef?.onPause()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        webViewRef?.onResume()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.stopLoading()
                webViewRef?.loadUrl("about:blank")
                webViewRef?.destroy()
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            if (!isTheaterMode) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = title.ifBlank { "Video Lecture" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (subjectName.isNotBlank() || chapterName.isNotBlank()) {
                                Text(
                                    text = listOf(subjectName, chapterName).filter { it.isNotBlank() }.joinToString(" • "),
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isTheaterMode = !isTheaterMode }) {
                            Icon(
                                imageVector = if (isTheaterMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Theater Mode",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            try {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, title)
                                    putExtra(Intent.EXTRA_TEXT, "🎬 MBBS Lecture: $title\n\nLink: $videoUrl")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Video Lecture"))
                            } catch (_: Exception) {}
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
                )
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isTheaterMode) PaddingValues(0.dp) else padding)
                .background(Color.Black)
        ) {
            // Main Video Player Container
            Box(
                modifier = if (isTheaterMode) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                },
                contentAlignment = Alignment.Center
            ) {
                if (ytVideoId != null) {
                    // IN-APP YOUTUBE PLAYER
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            createInAppYouTubePlayer(
                                context = ctx,
                                videoId = ytVideoId
                            ).also {
                                webViewRef = it
                            }
                        },
                        update = { view ->
                            webViewRef = view
                        }
                    )
                } else if (videoUrl.isNotBlank()) {
                    // DIRECT VIDEO STREAM FALLBACK
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            createGenericVideoPlayer(
                                context = ctx,
                                mediaUrl = videoUrl
                            ).also {
                                webViewRef = it
                            }
                        },
                        update = { view ->
                            webViewRef = view
                        }
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayDisabled,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No video source provided",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                }

                // In theater mode, show floating exit button
                if (isTheaterMode) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(16.dp)
                            .size(42.dp)
                    ) {
                        IconButton(onClick = { isTheaterMode = false }) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Exit Theater Mode",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Below Player Content (Hidden in Theater Mode for distraction-free study)
            if (!isTheaterMode) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title and Badges
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (ytVideoId != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDC2626)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        Text("YouTube In-App", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = "⚡ High-Yield Lecture",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            lineHeight = 24.sp
                        )

                        if (chapterName.isNotBlank() || subjectName.isNotBlank()) {
                            Text(
                                text = listOf(subjectName, chapterName).filter { it.isNotBlank() }.joinToString(" • "),
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Study Speed Controls Row (1x, 1.25x, 1.5x, 1.75x, 2x)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡ Study Playback Speed:",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${currentSpeed}x",
                                    color = Color(0xFF60A5FA),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val speedOptions = listOf(1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                                speedOptions.forEach { speed ->
                                    val isSelected = currentSpeed == speed
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF334155),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                currentSpeed = speed
                                                webViewRef?.evaluateJavascript("setSpeed($speed);", null)
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${speed}x",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick Jump Controls (-10s, +10s, Replay)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { webViewRef?.evaluateJavascript("seekRelative(-10);", null) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.Replay10, contentDescription = "-10s", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("-10s", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { webViewRef?.evaluateJavascript("seekRelative(10);", null) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.Forward10, contentDescription = "+10s", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+10s", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { webViewRef?.evaluateJavascript("restartAndPlay();", null) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = "Restart", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Study Info Note Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("💡", fontSize = 14.sp)
                                Text("Study Tip for Video Lectures", color = Color(0xFFFDE047), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(
                                text = "Use 1.25x or 1.5x speed to revise faster. You can also tap the Fullscreen button on the player or the Theater mode icon on top for an immersive study experience.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Creates and configures the WebView embedding YouTube IFrame Player with full lecture controls.
 */
@SuppressLint("SetJavaScriptEnabled")
private fun createInAppYouTubePlayer(
    context: Context,
    videoId: String
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.BLACK)

        settings.apply {
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
        }

        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // Keep playback strictly inside the app
                return true
            }
        }

        val htmlContent = buildYouTubeLectureHtml(videoId)
        loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
    }
}

/**
 * Fallback WebView for non-YouTube direct video streams (e.g. mp4)
 */
@SuppressLint("SetJavaScriptEnabled")
private fun createGenericVideoPlayer(
    context: Context,
    mediaUrl: String
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.BLACK)

        settings.apply {
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
            domStorageEnabled = true
        }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin:0; padding:0; box-sizing:border-box; }
                    html, body { width:100%; height:100%; background-color:#000; overflow:hidden; display:flex; align-items:center; justify-content:center; }
                    video { width:100%; height:100%; object-fit:contain; }
                </style>
            </head>
            <body>
                <video id="vid" src="$mediaUrl" autoplay controls playsinline style="width:100%; height:100%;"></video>
                <script>
                    var v = document.getElementById('vid');
                    function setSpeed(rate) { if (v) v.playbackRate = rate; }
                    function seekRelative(sec) { if (v) v.currentTime = Math.max(0, v.currentTime + sec); }
                    function restartAndPlay() { if (v) { v.currentTime = 0; v.play(); } }
                    function pauseVideo() { if (v) v.pause(); }
                    function resumeVideo() { if (v) v.play(); }
                </script>
            </body>
            </html>
        """.trimIndent()

        loadDataWithBaseURL("https://localhost", html, "text/html", "UTF-8", null)
    }
}

/**
 * Builds HTML for YouTube IFrame Player with full controls, seekbar, resolution, and JavaScript API.
 */
private fun buildYouTubeLectureHtml(videoId: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body {
                    width: 100%;
                    height: 100%;
                    background-color: #000000;
                    overflow: hidden;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                .wrapper {
                    position: relative;
                    width: 100%;
                    height: 100%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                #player {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    border: none;
                }
            </style>
        </head>
        <body>
            <div class="wrapper">
                <div id="player"></div>
            </div>
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player = null;
                var isReady = false;

                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        videoId: '$videoId',
                        playerVars: {
                            'autoplay': 1,
                            'controls': 1,
                            'rel': 0,
                            'playsinline': 1,
                            'modestbranding': 1,
                            'fs': 1,
                            'origin': 'https://www.youtube.com'
                        },
                        events: {
                            'onReady': onPlayerReady
                        }
                    });
                }

                function onPlayerReady(event) {
                    isReady = true;
                    try { player.playVideo(); } catch(e) {}
                }

                function setSpeed(rate) {
                    if (isReady && player && player.setPlaybackRate) {
                        try { player.setPlaybackRate(rate); } catch(e) {}
                    }
                }

                function seekRelative(offsetSec) {
                    if (isReady && player && player.getCurrentTime) {
                        try {
                            var current = player.getCurrentTime();
                            player.seekTo(Math.max(0, current + offsetSec), true);
                        } catch(e) {}
                    }
                }

                function restartAndPlay() {
                    if (isReady && player && player.seekTo) {
                        try {
                            player.seekTo(0, true);
                            player.playVideo();
                        } catch(e) {}
                    }
                }

                function pauseVideo() {
                    if (isReady && player && player.pauseVideo) {
                        try { player.pauseVideo(); } catch(e) {}
                    }
                }

                function resumeVideo() {
                    if (isReady && player && player.playVideo) {
                        try { player.playVideo(); } catch(e) {}
                    }
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
