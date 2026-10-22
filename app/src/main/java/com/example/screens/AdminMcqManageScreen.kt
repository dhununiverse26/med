package com.example.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MedRepository
import com.example.model.McqItem
import com.example.model.McqQuestion
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMcqManageScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allMcqs by MedRepository.mcqItems.collectAsState()

    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var selectedSubfolder by remember { mutableStateOf<String?>(null) }
    var selectedSubSubfolder by remember { mutableStateOf<String?>(null) }
    var managingChapterItem by remember { mutableStateOf<McqItem?>(null) }

    // Dialog state for Renaming Folder
    var renameFolderTarget by remember { mutableStateOf<String?>(null) }
    var newFolderNameInput by remember { mutableStateOf("") }

    // Dialog state for Deleting Folder
    var deleteFolderTarget by remember { mutableStateOf<String?>(null) }

    // Dialog state for Renaming Subfolder
    var renameSubfolderTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // folder, subfolder
    var newSubfolderNameInput by remember { mutableStateOf("") }

    // Dialog state for Deleting Subfolder
    var deleteSubfolderTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Dialog state for Renaming SubSubfolder
    var renameSubSubTarget by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var newSubSubNameInput by remember { mutableStateOf("") }

    // Dialog state for Deleting SubSubfolder
    var deleteSubSubTarget by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    // Dialog state for Renaming Chapter
    var renameChapterTarget by remember { mutableStateOf<McqItem?>(null) }
    var newChapterNameInput by remember { mutableStateOf("") }

    // Dialog state for Deleting Chapter
    var deleteChapterTarget by remember { mutableStateOf<McqItem?>(null) }

    // Dialog state for Editing a specific Question
    var editingQuestionIndex by remember { mutableStateOf<Int?>(null) }
    var isAddingNewQuestion by remember { mutableStateOf(false) }

    // Back navigation handling
    BackHandler {
        when {
            managingChapterItem != null -> managingChapterItem = null
            selectedSubSubfolder != null -> selectedSubSubfolder = null
            selectedSubfolder != null -> selectedSubfolder = null
            selectedFolder != null -> selectedFolder = null
            else -> onBackClick()
        }
    }

    // --- DIALOGS ---

    // Rename Folder Dialog
    if (renameFolderTarget != null) {
        AlertDialog(
            onDismissRequest = { renameFolderTarget = null },
            title = { Text("Rename Folder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderNameInput,
                    onValueChange = { newFolderNameInput = it },
                    label = { Text("New Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFolderNameInput.isNotBlank()) {
                        MedRepository.renameMcqFolder(renameFolderTarget!!, newFolderNameInput.trim())
                        if (selectedFolder == renameFolderTarget) selectedFolder = newFolderNameInput.trim()
                        android.widget.Toast.makeText(context, "Folder renamed & R2 paths updated!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    renameFolderTarget = null
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renameFolderTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Folder Dialog
    if (deleteFolderTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteFolderTarget = null },
            title = { Text("Delete Folder & Cloudflare Files?", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = { Text("This will permanently delete \"${deleteFolderTarget}\" and remove all its JSON chapter files from Cloudflare R2.") },
            confirmButton = {
                Button(
                    onClick = {
                        MedRepository.deleteMcqFolder(deleteFolderTarget!!)
                        if (selectedFolder == deleteFolderTarget) {
                            selectedFolder = null
                            selectedSubfolder = null
                            selectedSubSubfolder = null
                        }
                        deleteFolderTarget = null
                        android.widget.Toast.makeText(context, "Folder & R2 files deleted!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("Delete All") }
            },
            dismissButton = {
                TextButton(onClick = { deleteFolderTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Rename Subfolder Dialog
    if (renameSubfolderTarget != null) {
        AlertDialog(
            onDismissRequest = { renameSubfolderTarget = null },
            title = { Text("Rename Subfolder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSubfolderNameInput,
                    onValueChange = { newSubfolderNameInput = it },
                    label = { Text("New Subfolder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newSubfolderNameInput.isNotBlank()) {
                        val (f, s) = renameSubfolderTarget!!
                        MedRepository.renameMcqSubfolder(f, s, newSubfolderNameInput.trim())
                        if (selectedSubfolder == s) selectedSubfolder = newSubfolderNameInput.trim()
                        android.widget.Toast.makeText(context, "Subfolder renamed & R2 synced!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    renameSubfolderTarget = null
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renameSubfolderTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Subfolder Dialog
    if (deleteSubfolderTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteSubfolderTarget = null },
            title = { Text("Delete Subfolder?", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = { Text("Delete \"${deleteSubfolderTarget!!.second}\" and all its MCQ chapters from Cloudflare R2?") },
            confirmButton = {
                Button(
                    onClick = {
                        val (f, s) = deleteSubfolderTarget!!
                        MedRepository.deleteMcqSubfolder(f, s)
                        if (selectedSubfolder == s) {
                            selectedSubfolder = null
                            selectedSubSubfolder = null
                        }
                        deleteSubfolderTarget = null
                        android.widget.Toast.makeText(context, "Subfolder & chapters deleted!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteSubfolderTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Rename SubSubfolder Dialog
    if (renameSubSubTarget != null) {
        AlertDialog(
            onDismissRequest = { renameSubSubTarget = null },
            title = { Text("Rename Subfolder 2", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSubSubNameInput,
                    onValueChange = { newSubSubNameInput = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newSubSubNameInput.isNotBlank()) {
                        val (f, s, subSub) = renameSubSubTarget!!
                        MedRepository.renameMcqSubSubfolder(f, s, subSub, newSubSubNameInput.trim())
                        if (selectedSubSubfolder == subSub) selectedSubSubfolder = newSubSubNameInput.trim()
                    }
                    renameSubSubTarget = null
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renameSubSubTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Delete SubSubfolder Dialog
    if (deleteSubSubTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteSubSubTarget = null },
            title = { Text("Delete Subfolder 2?", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = { Text("Delete \"${deleteSubSubTarget!!.third}\" and its chapters?") },
            confirmButton = {
                Button(
                    onClick = {
                        val (f, s, subSub) = deleteSubSubTarget!!
                        MedRepository.deleteMcqSubSubfolder(f, s, subSub)
                        if (selectedSubSubfolder == subSub) selectedSubSubfolder = null
                        deleteSubSubTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteSubSubTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Rename Chapter Dialog
    if (renameChapterTarget != null) {
        AlertDialog(
            onDismissRequest = { renameChapterTarget = null },
            title = { Text("Rename Chapter", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newChapterNameInput,
                    onValueChange = { newChapterNameInput = it },
                    label = { Text("New Chapter Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newChapterNameInput.isNotBlank()) {
                        MedRepository.renameMcqChapter(renameChapterTarget!!.id, newChapterNameInput.trim())
                        android.widget.Toast.makeText(context, "Chapter renamed & R2 key updated!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    renameChapterTarget = null
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renameChapterTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Chapter Dialog
    if (deleteChapterTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteChapterTarget = null },
            title = { Text("Delete Chapter?", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = { Text("Delete \"${deleteChapterTarget!!.chapterName}\" and remove its JSON file from Cloudflare R2?") },
            confirmButton = {
                Button(
                    onClick = {
                        MedRepository.deleteMcqChapterById(deleteChapterTarget!!.id)
                        if (managingChapterItem?.id == deleteChapterTarget!!.id) managingChapterItem = null
                        deleteChapterTarget = null
                        android.widget.Toast.makeText(context, "Chapter deleted & R2 removed!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteChapterTarget = null }) { Text("Cancel") }
            }
        )
    }

    // LEVEL 5: QUESTION MANAGEMENT WITHIN CHAPTER
    if (managingChapterItem != null) {
        val chapter = managingChapterItem!!
        val liveChapter = allMcqs.firstOrNull { it.id == chapter.id } ?: chapter
        var currentQuestions by remember(liveChapter) { mutableStateOf(liveChapter.questions) }
        var isSavingToCloudflare by remember { mutableStateOf(false) }
        var isLoadingQuestions by remember { mutableStateOf(currentQuestions.isEmpty()) }

        LaunchedEffect(liveChapter.id) {
            if (currentQuestions.isEmpty()) {
                isLoadingQuestions = true
                val loaded = MedRepository.loadMcqQuestionsIfNeeded(liveChapter)
                currentQuestions = loaded
                isLoadingQuestions = false
            }
        }

        if (isLoadingQuestions) {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator()
             }
        } else {
            // Edit / Add Question Dialog
            if (editingQuestionIndex != null || isAddingNewQuestion) {
                val isEdit = editingQuestionIndex != null
                val initialQ = if (isEdit) currentQuestions.getOrNull(editingQuestionIndex!!) else null

                var eqText by remember { mutableStateOf(initialQ?.questionText.orEmpty()) }
                var eOptA by remember { mutableStateOf(initialQ?.optionA.orEmpty()) }
                var eOptB by remember { mutableStateOf(initialQ?.optionB.orEmpty()) }
                var eOptC by remember { mutableStateOf(initialQ?.optionC.orEmpty()) }
                var eOptD by remember { mutableStateOf(initialQ?.optionD.orEmpty()) }
                var eCorrect by remember { mutableStateOf(initialQ?.correctOption ?: "A") }
                var eExplanation by remember { mutableStateOf(initialQ?.explanation.orEmpty()) }

                AlertDialog(
                    onDismissRequest = {
                        editingQuestionIndex = null
                        isAddingNewQuestion = false
                    },
                    title = { Text(if (isEdit) "Edit Question #${editingQuestionIndex!! + 1}" else "Add New Question", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = eqText,
                                onValueChange = { eqText = it },
                                label = { Text("Question Text") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(value = eOptA, onValueChange = { eOptA = it }, label = { Text("Option A") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = eOptB, onValueChange = { eOptB = it }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = eOptC, onValueChange = { eOptC = it }, label = { Text("Option C") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = eOptD, onValueChange = { eOptD = it }, label = { Text("Option D") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Correct:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                listOf("A", "B", "C", "D").forEach { opt ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = eCorrect == opt, onClick = { eCorrect = opt })
                                        Text(opt, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = eExplanation,
                                onValueChange = { eExplanation = it },
                                label = { Text("Explanation") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (eqText.isBlank()) {
                                android.widget.Toast.makeText(context, "Question text cannot be blank", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val updatedQ = McqQuestion(
                                id = initialQ?.id ?: java.util.UUID.randomUUID().toString(),
                                questionText = eqText.trim(),
                                optionA = eOptA.trim(),
                                optionB = eOptB.trim(),
                                optionC = eOptC.trim(),
                                optionD = eOptD.trim(),
                                correctOption = eCorrect,
                                explanation = eExplanation.trim(),
                                questionImageUrl = initialQ?.questionImageUrl.orEmpty(),
                                answerImageUrl = initialQ?.answerImageUrl.orEmpty()
                            )

                            val list = currentQuestions.toMutableList()
                            if (isEdit) {
                                list[editingQuestionIndex!!] = updatedQ
                            } else {
                                list.add(updatedQ)
                            }
                            currentQuestions = list
                            MedRepository.updateMcqQuestions(chapter.id, list)

                            editingQuestionIndex = null
                            isAddingNewQuestion = false
                            android.widget.Toast.makeText(context, "Question updated & saved!", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Text(if (isEdit) "Update" else "Add")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            editingQuestionIndex = null
                            isAddingNewQuestion = false
                        }) { Text("Cancel") }
                    }
                )
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(chapter.chapterName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E3A8A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${currentQuestions.size} Questions", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { managingChapterItem = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E3A8A))
                            }
                        },
                        actions = {
                            IconButton(onClick = { isAddingNewQuestion = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Question", tint = Color(0xFF2563EB))
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
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cloudflare R2 Sync Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("☁️ Cloudflare Key: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(chapter.r2Key.ifBlank { MedRepository.buildMcqR2Key(chapter.folderName, chapter.subfolderName, chapter.subSubfolderName, chapter.chapterName) },
                                    fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF2563EB), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = {
                                    isSavingToCloudflare = true
                                    coroutineScope.launch {
                                        try {
                                            MedRepository.updateMcqQuestions(chapter.id, currentQuestions)
                                            android.widget.Toast.makeText(context, "✓ Synced ${currentQuestions.size} questions to Cloudflare R2!", android.widget.Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Sync failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                        } finally {
                                            isSavingToCloudflare = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isSavingToCloudflare,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Force Re-Sync Chapter to Cloudflare", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (currentQuestions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("❓", fontSize = 36.sp)
                                Text("No questions in this chapter.", color = Color(0xFF64748B))
                                Button(onClick = { isAddingNewQuestion = true }) {
                                    Text("+ Add Question")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(currentQuestions.size, key = { it }) { idx ->
                                val q = currentQuestions[idx]
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = Color(0xFFDBEAFE),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("Q${idx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                            Row {
                                                IconButton(
                                                    onClick = { editingQuestionIndex = idx },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val list = currentQuestions.toMutableList().apply { removeAt(idx) }
                                                        currentQuestions = list
                                                        MedRepository.updateMcqQuestions(chapter.id, list)
                                                        android.widget.Toast.makeText(context, "Question deleted & chapter updated!", android.widget.Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }

                                        Text(q.questionText, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            listOf("A" to q.optionA, "B" to q.optionB, "C" to q.optionC, "D" to q.optionD).forEach { (optKey, optVal) ->
                                                val isCorrect = optKey.equals(q.correctOption, ignoreCase = true)
                                                Text(
                                                    text = "$optKey) $optVal ${if (isCorrect) "✓ (Correct)" else ""}",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCorrect) Color(0xFF15803D) else Color(0xFF64748B)
                                                )
                                            }
                                        }

                                        if (q.explanation.isNotBlank()) {
                                            Text("Rationale: ${q.explanation}", fontSize = 11.sp, color = Color(0xFF475569))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // MAIN HIERARCHY NAVIGATION (Folders -> Subfolders -> SubSubfolders -> Chapters)
    val currentTitle = when {
        selectedSubSubfolder != null -> "$selectedSubfolder > $selectedSubSubfolder"
        selectedSubfolder != null -> selectedSubfolder!!
        selectedFolder != null -> selectedFolder!!
        else -> "Manage MCQ Library"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E3A8A), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Breadcrumbs Bar
            if (selectedFolder != null) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📁 All Folders",
                            fontSize = 11.sp,
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                selectedFolder = null
                                selectedSubfolder = null
                                selectedSubSubfolder = null
                            }
                        )
                        if (selectedFolder != null) {
                            Text(" > ", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = selectedFolder!!,
                                fontSize = 11.sp,
                                color = if (selectedSubfolder == null) Color(0xFF0F172A) else Color(0xFF2563EB),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    selectedSubfolder = null
                                    selectedSubSubfolder = null
                                }
                            )
                        }
                        if (selectedSubfolder != null) {
                            Text(" > ", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = selectedSubfolder!!,
                                fontSize = 11.sp,
                                color = if (selectedSubSubfolder == null) Color(0xFF0F172A) else Color(0xFF2563EB),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    selectedSubSubfolder = null
                                }
                            )
                        }
                    }
                }
            }

            if (allMcqs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("❓", fontSize = 42.sp)
                        Text("No MCQ practice sets uploaded yet.", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                        Text("Use 'Upload MCQ' in Admin to add chapters.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                }
            } else if (selectedFolder == null) {
                // LEVEL 1: FOLDERS
                val folders = allMcqs.map { it.folderName }.distinct()
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(folders) { folder ->
                        val matchingItems = allMcqs.filter { it.folderName == folder }
                        val subfolderCount = matchingItems.map { it.subfolderName }.distinct().size
                        val totalQuestions = matchingItems.sumOf { it.questionCount.coerceAtLeast(it.questions.size) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFolder = folder },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFDBEAFE),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("📁", fontSize = 22.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(folder, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                    Text("$subfolderCount Subfolders • $totalQuestions MCQs", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                                IconButton(onClick = {
                                    renameFolderTarget = folder
                                    newFolderNameInput = folder
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { deleteFolderTarget = folder }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            } else if (selectedSubfolder == null) {
                // LEVEL 2: SUBFOLDERS
                val subfolders = allMcqs.filter { it.folderName == selectedFolder }.map { it.subfolderName }.distinct()
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(subfolders) { sub ->
                        val matchingItems = allMcqs.filter { it.folderName == selectedFolder && it.subfolderName == sub }
                        val chapterCount = matchingItems.size
                        val totalQuestions = matchingItems.sumOf { it.questionCount.coerceAtLeast(it.questions.size) }
                        val hasSubSub = matchingItems.any { it.subSubfolderName.isNotBlank() }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSubfolder = sub
                                    if (!hasSubSub) selectedSubSubfolder = null
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFDCFCE7),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("📂", fontSize = 22.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sub, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                    Text("$chapterCount Chapters • $totalQuestions MCQs", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                                IconButton(onClick = {
                                    renameSubfolderTarget = selectedFolder!! to sub
                                    newSubfolderNameInput = sub
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { deleteSubfolderTarget = selectedFolder!! to sub }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            } else {
                // LEVEL 3 or 4: SUB-SUBFOLDER OR CHAPTERS
                val subItems = allMcqs.filter { it.folderName == selectedFolder && it.subfolderName == selectedSubfolder }
                val hasSubSub = subItems.any { it.subSubfolderName.isNotBlank() }

                if (hasSubSub && selectedSubSubfolder == null) {
                    // Show Sub-subfolders
                    val subSubList = subItems.map { it.subSubfolderName }.distinct()
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(subSubList) { subSub ->
                            val label = if (subSub.isBlank()) "(General)" else subSub
                            val count = subItems.count { it.subSubfolderName == subSub }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSubSubfolder = subSub },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🗂️", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                        Text("$count Chapters", fontSize = 12.sp, color = Color(0xFF64748B))
                                    }
                                    if (subSub.isNotBlank()) {
                                        IconButton(onClick = {
                                            renameSubSubTarget = Triple(selectedFolder!!, selectedSubfolder!!, subSub)
                                            newSubSubNameInput = subSub
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { deleteSubSubTarget = Triple(selectedFolder!!, selectedSubfolder!!, subSub) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }
                } else {
                    // LEVEL 4: CHAPTERS LIST
                    val chapters = subItems.filter {
                        if (hasSubSub) it.subSubfolderName == (selectedSubSubfolder ?: "")
                        else true
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(chapters, key = { it.id }) { chapter ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { managingChapterItem = chapter },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF3E8FF),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("🩺", fontSize = 20.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(chapter.chapterName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                            Text("${chapter.questionCount.coerceAtLeast(chapter.questions.size)} Questions / MCQs", fontSize = 12.sp, color = Color(0xFF64748B))
                                        }
                                        IconButton(onClick = {
                                            renameChapterTarget = chapter
                                            newChapterNameInput = chapter.chapterName
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { deleteChapterTarget = chapter }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    // Cloudflare R2 path pill
                                    val r2Key = chapter.r2Key.ifBlank {
                                        MedRepository.buildMcqR2Key(chapter.folderName, chapter.subfolderName, chapter.subSubfolderName, chapter.chapterName)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("☁️ ", fontSize = 11.sp)
                                            Text(r2Key, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF2563EB), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                            Text("Tap to edit questions ›", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
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
}
