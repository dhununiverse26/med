package com.example.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McqBookmarksScreen(
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val bookmarks by MedRepository.mcqBookmarks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Saved Bookmarks (${bookmarks.size})",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFEFF6FF))
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = CircleShape, color = Color(0xFFFEF9C3), modifier = Modifier.size(64.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = Color(0xFFCA8A04), modifier = Modifier.size(32.dp))
                        }
                    }
                    Text("No Bookmarked MCQs", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Text("Tap the bookmark icon during MCQ practice to save high-yield questions for revision.", fontSize = 13.sp, color = Color(0xFF64748B), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
                items(bookmarks, key = { it.id }) { bm ->
                    val q = bm.question
                    val dateStr = remember(bm.timestamp) {
                        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        sdf.format(Date(bm.timestamp))
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDBEAFE)) {
                                    Text(
                                        text = bm.chapterName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(dateStr, fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { MedRepository.deleteBookmark(bm.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BookmarkRemove,
                                            contentDescription = "Remove Bookmark",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = q.questionText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A),
                                lineHeight = 20.sp
                            )

                            if (q.questionImageUrl.isNotBlank()) {
                                coil.compose.AsyncImage(
                                    model = q.questionImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(8.dp))
                                )
                            }

                            val options = listOf("A" to q.optionA, "B" to q.optionB, "C" to q.optionC, "D" to q.optionD)
                            options.forEach { (optLetter, optText) ->
                                if (optText.isNotBlank()) {
                                    val isCorrect = q.correctOption.equals(optLetter, ignoreCase = true)
                                    val optBg = if (isCorrect) Color(0xFFDCFCE7) else Color(0xFFF8FAFC)
                                    val optBorder = if (isCorrect) Color(0xFF16A34A) else Color(0xFFE2E8F0)

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = optBg,
                                        border = BorderStroke(1.dp, optBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                optLetter,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isCorrect) Color(0xFF16A34A) else Color(0xFF64748B)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(optText, fontSize = 13.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
                                            if (isCorrect) {
                                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF16A34A)) {
                                                    Text("Correct Answer", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (q.explanation.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("💡 Clinical Rationale / Explanation:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1E3A8A))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(q.explanation, fontSize = 12.sp, color = Color(0xFF1E293B), lineHeight = 16.sp)
                                    }
                                }
                            }

                            if (q.answerImageUrl.isNotBlank()) {
                                coil.compose.AsyncImage(
                                    model = q.answerImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
