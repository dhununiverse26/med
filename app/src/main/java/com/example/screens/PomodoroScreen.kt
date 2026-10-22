package com.example.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PomodoroManager
import kotlinx.coroutines.delay

/**
 * Dedicated Pomodoro Timer
 * - Portrait Mode: Circular progress ring with Neon Blue color that sweeps as timer runs
 * - Landscape Mode: Pure black screen, massive full-screen timer text, NO neon color, controls completely hidden while running
 * - Auto-switches to Landscape on Play
 * - Tap to reveal controls (Stop/Pause, Back, Rotate)
 * - Vibrate & completion notification alert when finished
 */
@Composable
fun PomodoroScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current

    val isRunning by PomodoroManager.isRunning.collectAsState()
    val timeLeft by PomodoroManager.timeLeftSeconds.collectAsState()
    val totalTime by PomodoroManager.totalWorkTimeSeconds.collectAsState()
    val isCompleted by PomodoroManager.isCompleted.collectAsState()

    // Determine current landscape state
    val isSystemLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isForcedLandscape by remember { mutableStateOf(false) }
    val isLandscape = isSystemLandscape || isForcedLandscape

    // Controls visibility in landscape: hidden while running, visible when paused or tapped
    var controlsVisible by remember { mutableStateOf(!isRunning) }

    // Auto-update controls visibility when running status changes
    LaunchedEffect(isRunning) {
        if (!isRunning) {
            controlsVisible = true
        } else {
            controlsVisible = false
        }
    }

    // Auto-hide controls in landscape after 4 seconds of inactivity if running
    LaunchedEffect(controlsVisible, isRunning, isLandscape) {
        if (isRunning && controlsVisible && isLandscape) {
            delay(4000L)
            controlsVisible = false
        }
    }

    // Keep screen awake while in Pomodoro screen
    DisposableEffect(Unit) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Function to handle clean exit
    val handleExit = {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onBackClick()
    }

    // Intercept back button to restore portrait orientation and exit
    BackHandler(enabled = true) {
        if (isLandscape && isForcedLandscape && !isSystemLandscape) {
            // Revert to portrait first
            isForcedLandscape = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            handleExit()
        }
    }

    // Time calculations
    val mins = (timeLeft / 60).coerceAtLeast(0)
    val secs = (timeLeft % 60).coerceAtLeast(0)
    val timeFormatted = "%02d:%02d".format(mins, secs)

    // Progress for circular ring (0f to 1f)
    val safeTotal = totalTime.coerceAtLeast(1)
    val targetProgress = ((safeTotal - timeLeft).toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = "neon_ring_progress"
    )

    // Root Pitch-Black OLED Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Tap anywhere on screen to toggle controls visibility
            .pointerInput(isRunning, isLandscape) {
                detectTapGestures(
                    onTap = {
                        if (isLandscape) {
                            controlsVisible = !controlsVisible
                        }
                    }
                )
            }
    ) {
        if (isLandscape) {
            // ==========================================
            // LANDSCAPE MODE: Full Screen Giant Text, Black Screen, No Neon Ring
            // ==========================================
            LandscapeClockView(
                timeFormatted = timeFormatted,
                isRunning = isRunning,
                timeLeft = timeLeft,
                controlsVisible = controlsVisible,
                onBack = { handleExit() },
                onToggleOrientation = {
                    isForcedLandscape = false
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                },
                onToggleTimer = {
                    vibrateSimpleHaptic(context)
                    PomodoroManager.toggleTimer()
                },
                onResetTimer = {
                    vibrateSimpleHaptic(context)
                    PomodoroManager.resetTimer()
                }
            )
        } else {
            // ==========================================
            // PORTRAIT MODE: Neon Blue Circular Progress Ring
            // ==========================================
            PortraitClockView(
                timeFormatted = timeFormatted,
                progress = animatedProgress,
                isRunning = isRunning,
                timeLeft = timeLeft,
                totalTime = totalTime,
                onBack = { handleExit() },
                onToggleOrientation = {
                    isForcedLandscape = true
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                },
                onPlayClick = {
                    vibrateSimpleHaptic(context)
                    if (isRunning) {
                        PomodoroManager.pauseTimer()
                    } else {
                        // Automatically switch to landscape mode on play as requested!
                        isForcedLandscape = true
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        PomodoroManager.startTimer()
                    }
                },
                onResetClick = {
                    vibrateSimpleHaptic(context)
                    PomodoroManager.resetTimer()
                }
            )
        }

        // ==========================================
        // COMPLETION DIALOG / NOTIFICATION
        // ==========================================
        if (isCompleted) {
            AlertDialog(
                onDismissRequest = { PomodoroManager.dismissCompleted() },
                containerColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "🎉 Session Completed!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Text(
                        text = "Great focus! Your 25-minute Pomodoro is complete. Take a short break or restart the timer.",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            PomodoroManager.dismissCompleted()
                            PomodoroManager.resetTimer()
                            PomodoroManager.startTimer()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Restart", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            PomodoroManager.dismissCompleted()
                            PomodoroManager.resetTimer()
                        }
                    ) {
                        Text("Dismiss", color = Color(0xFF64748B))
                    }
                }
            )
        }
    }
}

/**
 * Portrait Layout with Neon Blue Circular Ring
 */
@Composable
private fun PortraitClockView(
    timeFormatted: String,
    progress: Float,
    isRunning: Boolean,
    timeLeft: Int,
    totalTime: Int,
    onBack: () -> Unit,
    onToggleOrientation: () -> Unit,
    onPlayClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Orientation switch button to switch to Landscape anytime
                IconButton(
                    onClick = onToggleOrientation,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "Switch to Landscape",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            val ringDiameter = (maxWidth * 0.78f).coerceIn(240.dp, 330.dp)

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Circular Ring with Neon Blue Color
                Box(
                    modifier = Modifier.size(ringDiameter),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        val arcRadius = (size.minDimension - strokeWidth) / 2f
                        val topLeft = (size.minDimension - arcRadius * 2f) / 2f

                        // Background inactive track
                        drawCircle(
                            color = Color(0xFF1E293B).copy(alpha = 0.55f),
                            radius = arcRadius,
                            style = Stroke(width = strokeWidth)
                        )

                        // Glowing Neon Blue Progress Arc
                        if (progress > 0.001f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF00F0FF),
                                        Color(0xFF38BDF8),
                                        Color(0xFF2563EB),
                                        Color(0xFF00F0FF)
                                    )
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Large Digital Text inside the ring
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = timeFormatted,
                            fontSize = (ringDiameter.value * 0.22f).coerceIn(52f, 76f).sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRunning) "FOCUSING" else "READY",
                            color = if (isRunning) Color(0xFF00F0FF) else Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }

                // Bottom Controls: Play/Pause and Reset
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset Button (visible when paused and elapsed)
                    if (!isRunning && timeLeft != totalTime) {
                        IconButton(
                            onClick = onResetClick,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Timer",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Main Play/Pause Button
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(if (isRunning) Color(0xFFEF4444) else Color(0xFF2563EB))
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Landscape Layout: Pure Black Screen, Giant Full-Screen Text, No Neon Ring, Controls Hidden While Running
 */
@Composable
private fun LandscapeClockView(
    timeFormatted: String,
    isRunning: Boolean,
    timeLeft: Int,
    controlsVisible: Boolean,
    onBack: () -> Unit,
    onToggleOrientation: () -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val availableWidth = maxWidth
        val availableHeight = maxHeight

        // BADA SIZE FULL SCREEN COVER KARE TEXT (No Neon Color, Clean White)
        // Dynamically compute font size to maximally fill screen height and width
        val byHeight = availableHeight.value * 0.65f
        val byWidth = availableWidth.value / 3.4f
        val giantFontSize = minOf(byHeight, byWidth).coerceIn(90f, 240f).sp

        Text(
            text = timeFormatted,
            fontSize = giantFontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )

        // Top Controls: Back and Orientation Toggle (Hidden when running unless tapped)
        AnimatedVisibility(
            visible = controlsVisible || !isRunning,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.75f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onToggleOrientation,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.75f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "Portrait Mode",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Bottom Controls: Pause/Stop & Reset (Hidden during running unless tapped)
        AnimatedVisibility(
            visible = controlsVisible || !isRunning,
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 },
            exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isRunning && timeLeft != 25 * 60) {
                    IconButton(
                        onClick = onResetTimer,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Timer",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleTimer,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) Color(0xFFEF4444) else Color(0xFF2563EB))
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

/**
 * Lightweight haptic vibration feedback for button presses
 */
private fun vibrateSimpleHaptic(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        }
    } catch (_: Exception) {}
}
