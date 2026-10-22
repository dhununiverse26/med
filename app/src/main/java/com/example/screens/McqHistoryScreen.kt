package com.example.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MedRepository
import com.example.model.McqHistoryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McqHistoryScreen(
    onBackClick: () -> Unit
) {
    val history by MedRepository.mcqHistory.collectAsState()
    var selectedItemForReview by remember { mutableStateOf<McqHistoryItem?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (selectedItemForReview != null) {
            selectedItemForReview = null
        } else {
            onBackClick()
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History?") },
            text = { Text("This will permanently remove all past MCQ practice records.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        MedRepository.clearAllHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear All", color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (selectedItemForReview != null) {
        McqResultReviewScreen(
            historyItem = selectedItemForReview!!,
            onRetake = { selectedItemForReview = null },
            onBackClick = { selectedItemForReview = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Results & History (${history.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E3A8A)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E3A8A))
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = Color(0xFFDC2626))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFEFF6FF))
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = CircleShape, color = Color(0xFFFEF3C7), modifier = Modifier.size(64.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(32.dp))
                        }
                    }
                    Text("No Test History Yet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Text("Complete any MCQ Practice test to see your scores, breakdown, and detailed reviews here.", fontSize = 13.sp, color = Color(0xFF64748B), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    val dateStr = remember(item.timestamp) {
                        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        sdf.format(Date(item.timestamp))
                    }
                    val totalPossible = item.totalQuestions * 4

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedItemForReview = item },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.chapterName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(dateStr, fontSize = 11.sp, color = Color(0xFF64748B))
                            }

                            // Score row with Scheme tag
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Score: ",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "${item.score} / $totalPossible",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = if (item.score >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                    )
                                }
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                                    Text(
                                        text = "+4 / -1 Scheme",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Circular Pie Chart Form for Result & Performance (Correct +4, Incorrect -1, Unattempted 0)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Circular Pie Chart
                                CircularResultPieChart(
                                    correctCount = item.correctCount,
                                    incorrectCount = item.incorrectCount,
                                    unattemptedCount = item.unattemptedCount,
                                    totalQuestions = item.totalQuestions,
                                    size = 78.dp,
                                    strokeWidth = 9.dp
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${item.score}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 17.sp,
                                            color = if (item.score >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                        )
                                        Text(
                                            text = "marks",
                                            fontSize = 9.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Legend & Marking Scheme Breakdown
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF16A34A)))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Correct (${item.correctCount})", fontSize = 12.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                        }
                                        Text("+${item.correctCount * 4}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFDC2626)))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Incorrect (${item.incorrectCount})", fontSize = 12.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                        }
                                        Text(if (item.incorrectCount > 0) "-${item.incorrectCount * 1}" else "0", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFCBD5E1)))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Unattempted (${item.unattemptedCount})", fontSize = 12.sp, color = Color(0xFF64748B))
                                        }
                                        Text("0", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                                    }
                                }
                            }

                            // Tap to review
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tap to Review", fontSize = 11.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
