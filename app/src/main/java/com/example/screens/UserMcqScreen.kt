package com.example.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.example.model.McqHistoryItem
import com.example.model.QuestionAttemptRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MedRepository
import com.example.model.McqItem
import com.example.model.McqQuestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMcqScreen(
    onBackClick: () -> Unit
) {
    LaunchedEffect(Unit) {
        MedRepository.fetchCatalog()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allMcqs by MedRepository.mcqItems.collectAsState()
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var selectedSubfolder by remember { mutableStateOf<String?>(null) }
    var selectedSubSubfolder by remember { mutableStateOf<String?>(null) }
    var selectedChapter by remember { mutableStateOf<McqItem?>(null) }
    var isLoadingQuestionsForChapter by remember { mutableStateOf<String?>(null) }

    BackHandler {
        when {
            selectedChapter != null -> selectedChapter = null
            selectedSubSubfolder != null -> selectedSubSubfolder = null
            selectedSubfolder != null -> selectedSubfolder = null
            selectedFolder != null -> selectedFolder = null
            else -> onBackClick()
        }
    }

    if (selectedChapter != null) {
        // Active MCQ Practice Quiz Session
        McqQuizSessionScreen(
            chapter = selectedChapter!!,
            onBackClick = { selectedChapter = null }
        )
        return
    }

    // Loading questions dialog when fetching chapter from Cloudflare R2 on demand
    if (isLoadingQuestionsForChapter != null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFF2563EB), modifier = Modifier.size(36.dp))
                    Column {
                        Text("Loading Chapter Questions...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Fetching individual catalog from Cloudflare R2", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when {
                        selectedSubSubfolder != null -> "$selectedSubfolder > $selectedSubSubfolder"
                        selectedSubfolder != null -> selectedSubfolder!!
                        selectedFolder != null -> selectedFolder!!
                        else -> "MCQ Practice Hub"
                    }
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E3A8A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            selectedSubSubfolder != null -> selectedSubSubfolder = null
                            selectedSubfolder != null -> selectedSubfolder = null
                            selectedFolder != null -> selectedFolder = null
                            else -> onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E3A8A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFEFF6FF))
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (allMcqs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📝", fontSize = 48.sp)
                        Text("No MCQ Practice sets available yet.", color = Color(0xFF64748B), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("Admin can upload MCQ chapters via Admin Panel.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            } else if (selectedFolder == null) {
                // Level 1: Folders
                val folders = allMcqs.map { it.folderName }.distinct()
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(folders) { folder ->
                        val matching = allMcqs.filter { it.folderName == folder }
                        val count = matching.size
                        val totalQuestions = matching.sumOf { it.questionCount.coerceAtLeast(it.questions.size) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFolder = folder },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFDBEAFE),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("📁", fontSize = 22.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(folder, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                    Text("$count Chapters • $totalQuestions MCQs", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF2563EB))
                            }
                        }
                    }
                }
            } else if (selectedSubfolder == null) {
                // Level 2: Subfolders
                val subfolders = allMcqs.filter { it.folderName == selectedFolder }.map { it.subfolderName }.distinct()
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(subfolders) { sub ->
                        val matching = allMcqs.filter { it.folderName == selectedFolder && it.subfolderName == sub }
                        val count = matching.size
                        val totalQuestions = matching.sumOf { it.questionCount.coerceAtLeast(it.questions.size) }
                        val hasSubSub = matching.any { it.subSubfolderName.isNotBlank() }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSubfolder = sub
                                    if (!hasSubSub) selectedSubSubfolder = null
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFDCFCE7),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("📂", fontSize = 22.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sub, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                    Text("$count Chapters • $totalQuestions MCQs", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF16A34A))
                            }
                        }
                    }
                }
            } else {
                // Level 3 or 4: Sub-subfolders or Chapters
                val subItems = allMcqs.filter { it.folderName == selectedFolder && it.subfolderName == selectedSubfolder }
                val hasSubSub = subItems.any { it.subSubfolderName.isNotBlank() }

                if (hasSubSub && selectedSubSubfolder == null) {
                    val subSubList = subItems.map { it.subSubfolderName }.distinct()
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(subSubList) { subSub ->
                            val label = if (subSub.isBlank()) "(General)" else subSub
                            val count = subItems.count { it.subSubfolderName == subSub }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSubSubfolder = subSub },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFFEF3C7),
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("🗂️", fontSize = 22.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                        Text("$count Chapters", fontSize = 12.sp, color = Color(0xFF64748B))
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFD97706))
                                }
                            }
                        }
                    }
                } else {
                    // Chapters / Units
                    val chapters = subItems.filter {
                        if (hasSubSub) it.subSubfolderName == (selectedSubSubfolder ?: "")
                        else true
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(chapters, key = { it.id }) { chapter ->
                            val qCount = chapter.questionCount.coerceAtLeast(chapter.questions.size)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (chapter.questions.isNotEmpty()) {
                                            selectedChapter = chapter
                                        } else {
                                            // On-demand fetch from Cloudflare R2
                                            isLoadingQuestionsForChapter = chapter.id
                                            coroutineScope.launch {
                                                val loaded = MedRepository.loadMcqQuestionsIfNeeded(chapter)
                                                isLoadingQuestionsForChapter = null
                                                if (loaded.isNotEmpty()) {
                                                    selectedChapter = chapter.copy(questions = loaded)
                                                } else {
                                                    android.widget.Toast.makeText(context, "Could not load questions. Check network.", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF3E8FF),
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("🩺", fontSize = 22.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(chapter.chapterName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                        Text("$qCount Questions", fontSize = 12.sp, color = Color(0xFF64748B))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF7C3AED)
                                    ) {
                                        Text("Practice", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McqQuizSessionScreen(
    chapter: McqItem,
    onBackClick: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val selectedAnswers = remember(chapter) { mutableStateMapOf<Int, String>() } // index -> "A"/"B"/"C"/"D"
    var isSubmitted by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }

    val bookmarks by MedRepository.mcqBookmarks.collectAsState()
    val questions = chapter.questions
    val currentQ = questions.getOrNull(currentIndex)
    val isCurrentBookmarked = currentQ != null && bookmarks.any { it.question.id == currentQ.id }

    var resultHistoryItem by remember { mutableStateOf<McqHistoryItem?>(null) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    fun submitTest() {
        var correct = 0
        var incorrect = 0
        var unattempted = 0
        val records = mutableListOf<QuestionAttemptRecord>()

        questions.forEachIndexed { idx, q ->
            val chosen = selectedAnswers[idx]
            if (chosen.isNullOrBlank()) {
                unattempted++
                records.add(QuestionAttemptRecord(question = q, selectedOption = "", isCorrect = false))
            } else if (chosen.equals(q.correctOption, ignoreCase = true)) {
                correct++
                records.add(QuestionAttemptRecord(question = q, selectedOption = chosen, isCorrect = true))
            } else {
                incorrect++
                records.add(QuestionAttemptRecord(question = q, selectedOption = chosen, isCorrect = false))
            }
        }

        val totalScore = (correct * 4) - (incorrect * 1)
        val historyItem = McqHistoryItem(
            chapterName = chapter.chapterName,
            folderName = chapter.folderName,
            subfolderName = chapter.subfolderName,
            totalQuestions = questions.size,
            correctCount = correct,
            incorrectCount = incorrect,
            unattemptedCount = unattempted,
            score = totalScore,
            timestamp = System.currentTimeMillis(),
            records = records
        )
        MedRepository.saveTestHistory(historyItem)
        resultHistoryItem = historyItem
        isSubmitted = true
    }

    BackHandler {
        if (isSubmitted) {
            onBackClick()
        } else if (selectedAnswers.isNotEmpty()) {
            showSubmitDialog = true
        } else {
            onBackClick()
        }
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Submit Test?") },
            text = {
                Text(
                    "You have answered ${selectedAnswers.size} of ${questions.size} questions.\n\nScoring Scheme:\n• +4 Marks for each correct answer\n• -1 Mark for each incorrect answer\n• 0 Marks for unattempted questions",
                    fontSize = 13.sp,
                    color = Color(0xFF334155),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        submitTest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Submit Test")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("Continue Test")
                }
            }
        )
    }

    if (isSubmitted && resultHistoryItem != null) {
        McqResultReviewScreen(
            historyItem = resultHistoryItem!!,
            onRetake = {
                selectedAnswers.clear()
                currentIndex = 0
                isSubmitted = false
                resultHistoryItem = null
            },
            onBackClick = onBackClick
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chapter.chapterName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E3A8A), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedAnswers.isNotEmpty()) showSubmitDialog = true
                        else onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E3A8A))
                    }
                },
                actions = {
                    // Single Bookmark Icon in Top Right Corner
                    if (currentQ != null) {
                        IconButton(onClick = {
                            MedRepository.toggleBookmark(chapter.chapterName, chapter.folderName, chapter.subfolderName, currentQ)
                        }) {
                            Icon(
                                imageVector = if (isCurrentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isCurrentBookmarked) Color(0xFFEAB308) else Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Single Submit Button in Top Right Corner
                    Button(
                        onClick = { showSubmitDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("SUBMIT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFEFF6FF))
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        if (currentQ == null || questions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No questions in this chapter.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress Bar & Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Question ${currentIndex + 1} of ${questions.size}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64748B))
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDBEAFE)) {
                    Text("Attempted: ${selectedAnswers.size}/${questions.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / questions.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Color(0xFF2563EB)
            )

            // Question Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Q${currentIndex + 1}. ${currentQ.questionText}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A),
                        lineHeight = 22.sp
                    )

                    if (currentQ.questionImageUrl.isNotBlank()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            coil.compose.AsyncImage(
                                model = currentQ.questionImageUrl,
                                contentDescription = "Question Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { fullScreenImageUrl = currentQ.questionImageUrl },
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.clickable { fullScreenImageUrl = currentQ.questionImageUrl }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = "Full Screen",
                                            tint = Color(0xFF334155),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Full Screen",
                                            fontSize = 11.sp,
                                            color = Color(0xFF334155),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val chosenOpt = selectedAnswers[currentIndex]
                    val options = listOf("A" to currentQ.optionA, "B" to currentQ.optionB, "C" to currentQ.optionC, "D" to currentQ.optionD)

                    options.forEach { (optLetter, optText) ->
                        if (optText.isNotBlank()) {
                            val isSelected = chosenOpt == optLetter

                            val bg = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                            val borderCol = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // User selecting option during test (no immediate answer reveal)
                                        if (isSelected) {
                                            selectedAnswers.remove(currentIndex)
                                        } else {
                                            selectedAnswers[currentIndex] = optLetter
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = bg,
                                border = BorderStroke(1.5.dp, borderCol)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                optLetter,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isSelected) Color.White else Color(0xFF475569)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        optText,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Bar with ONLY ICONS for Previous & Next
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Icon Button (ONLY ICON, no text)
                    FilledTonalIconButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                    }

                    // Next Icon Button (ONLY ICON, no text)
                    FilledIconButton(
                        onClick = {
                            if (currentIndex < questions.size - 1) {
                                currentIndex++
                            }
                        },
                        enabled = currentIndex < questions.size - 1,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }

            // Full Screen Image Dialog
            FullScreenImageViewerDialog(
                imageUrl = fullScreenImageUrl,
                onDismiss = { fullScreenImageUrl = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McqResultReviewScreen(
    historyItem: McqHistoryItem,
    onRetake: () -> Unit,
    onBackClick: () -> Unit
) {
    val bookmarks by MedRepository.mcqBookmarks.collectAsState()
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    val dateStr = remember(historyItem.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(historyItem.timestamp))
    }

    val totalPossible = historyItem.totalQuestions * 4

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Result & Review", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E3A8A)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Result Summary Card with +4 / -1 Scheme & Visual Progress Bar
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(historyItem.chapterName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        Text(dateStr, fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    // Score Display
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Final Score", fontSize = 12.sp, color = Color(0xFF15803D), fontWeight = FontWeight.SemiBold)
                                Text("${historyItem.score} / $totalPossible", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color(0xFF166534))
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFDCFCE7)) {
                                Text("+4 Correct • -1 Incorrect", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }

                    // Circular Pie Chart Form for Result & Performance
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularResultPieChart(
                            correctCount = historyItem.correctCount,
                            incorrectCount = historyItem.incorrectCount,
                            unattemptedCount = historyItem.unattemptedCount,
                            totalQuestions = historyItem.totalQuestions,
                            size = 86.dp,
                            strokeWidth = 10.dp
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${historyItem.score}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = if (historyItem.score >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                                Text("marks", fontSize = 9.sp, color = Color(0xFF64748B))
                            }
                        }

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
                                    Text("Correct (${historyItem.correctCount})", fontSize = 12.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                }
                                Text("+${historyItem.correctCount * 4}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFDC2626)))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Incorrect (${historyItem.incorrectCount})", fontSize = 12.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                }
                                Text(if (historyItem.incorrectCount > 0) "-${historyItem.incorrectCount * 1}" else "0", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFCBD5E1)))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Unattempted (${historyItem.unattemptedCount})", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                                Text("0", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onRetake,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retake Test")
                        }
                        Button(
                            onClick = onBackClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Back to Chapters")
                        }
                    }
                }
            }

            Text("Detailed Question Review (${historyItem.records.size}):", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))

            // Full Question by Question Review
            historyItem.records.forEachIndexed { idx, record ->
                val q = record.question
                val isBookmarked = bookmarks.any { it.question.id == q.id }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status badge
                            val (badgeBg, badgeText, badgeColor) = when {
                                record.selectedOption.isBlank() -> Triple(Color(0xFFF1F5F9), "Unattempted (0)", Color(0xFF64748B))
                                record.isCorrect -> Triple(Color(0xFFDCFCE7), "Correct (+4)", Color(0xFF15803D))
                                else -> Triple(Color(0xFFFEE2E2), "Incorrect (-1)", Color(0xFFDC2626))
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = badgeBg) {
                                Text(badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }

                            // Bookmark toggle
                            IconButton(
                                onClick = {
                                    MedRepository.toggleBookmark(historyItem.chapterName, historyItem.folderName, historyItem.subfolderName, q)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (isBookmarked) Color(0xFFEAB308) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text("Q${idx + 1}. ${q.questionText}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))

                        if (q.questionImageUrl.isNotBlank()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                coil.compose.AsyncImage(
                                    model = q.questionImageUrl,
                                    contentDescription = "Question Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { fullScreenImageUrl = q.questionImageUrl },
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier.clickable { fullScreenImageUrl = q.questionImageUrl }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fullscreen,
                                                contentDescription = "Full Screen",
                                                tint = Color(0xFF334155),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Full Screen",
                                                fontSize = 10.sp,
                                                color = Color(0xFF334155),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val options = listOf("A" to q.optionA, "B" to q.optionB, "C" to q.optionC, "D" to q.optionD)
                        options.forEach { (optLetter, optText) ->
                            if (optText.isNotBlank()) {
                                val isCorrectOpt = q.correctOption.equals(optLetter, ignoreCase = true)
                                val isUserChoice = record.selectedOption.equals(optLetter, ignoreCase = true)

                                val optBg = when {
                                    isCorrectOpt -> Color(0xFFDCFCE7)
                                    isUserChoice && !isCorrectOpt -> Color(0xFFFEE2E2)
                                    else -> Color(0xFFF8FAFC)
                                }
                                val optBorder = when {
                                    isCorrectOpt -> Color(0xFF16A34A)
                                    isUserChoice && !isCorrectOpt -> Color(0xFFDC2626)
                                    else -> Color(0xFFE2E8F0)
                                }

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
                                        Text(optLetter, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isCorrectOpt) Color(0xFF16A34A) else Color(0xFF475569))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(optText, fontSize = 13.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
                                        if (isCorrectOpt) {
                                            Icon(Icons.Default.Check, contentDescription = "Correct", tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                        } else if (isUserChoice) {
                                            Icon(Icons.Default.Close, contentDescription = "Your choice", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
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
                            Column(modifier = Modifier.fillMaxWidth()) {
                                coil.compose.AsyncImage(
                                    model = q.answerImageUrl,
                                    contentDescription = "Answer Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { fullScreenImageUrl = q.answerImageUrl },
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier.clickable { fullScreenImageUrl = q.answerImageUrl }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fullscreen,
                                                contentDescription = "Full Screen",
                                                tint = Color(0xFF334155),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Full Screen",
                                                fontSize = 10.sp,
                                                color = Color(0xFF334155),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            FullScreenImageViewerDialog(
                imageUrl = fullScreenImageUrl,
                onDismiss = { fullScreenImageUrl = null }
            )
        }
    }
}

