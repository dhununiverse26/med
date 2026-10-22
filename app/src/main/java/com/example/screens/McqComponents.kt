package com.example.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * Circular / Pie chart (donut-style) displaying proportion of
 * Correct (green), Incorrect (red), and Unattempted (grey) questions.
 */
@Composable
fun CircularResultPieChart(
    correctCount: Int,
    incorrectCount: Int,
    unattemptedCount: Int,
    totalQuestions: Int,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    strokeWidth: Dp = 10.dp,
    centerContent: @Composable () -> Unit = {}
) {
    val total = totalQuestions.coerceAtLeast(1)
    val correctAngle = (correctCount.toFloat() / total) * 360f
    val incorrectAngle = (incorrectCount.toFloat() / total) * 360f
    val unattemptedAngle = (unattemptedCount.toFloat() / total) * 360f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val stroke = Stroke(width = strokePx, cap = StrokeCap.Butt)
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            if (totalQuestions <= 0) {
                drawArc(
                    color = Color(0xFFE2E8F0),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            } else {
                var currentAngle = -90f
                if (correctAngle > 0f) {
                    drawArc(
                        color = Color(0xFF16A34A), // Green
                        startAngle = currentAngle,
                        sweepAngle = correctAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                    currentAngle += correctAngle
                }
                if (incorrectAngle > 0f) {
                    drawArc(
                        color = Color(0xFFDC2626), // Red
                        startAngle = currentAngle,
                        sweepAngle = incorrectAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                    currentAngle += incorrectAngle
                }
                if (unattemptedAngle > 0f) {
                    drawArc(
                        color = Color(0xFFCBD5E1), // Slate Grey
                        startAngle = currentAngle,
                        sweepAngle = unattemptedAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                }
            }
        }
        centerContent()
    }
}

/**
 * Full screen image viewer dialog with zoom support and dismiss button.
 */
@Composable
fun FullScreenImageViewerDialog(
    imageUrl: String?,
    onDismiss: () -> Unit
) {
    if (imageUrl.isNullOrBlank()) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .clickable(onClick = onDismiss)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full Screen Preview",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.25f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Full Screen",
                    tint = Color.White
                )
            }
        }
    }
}
