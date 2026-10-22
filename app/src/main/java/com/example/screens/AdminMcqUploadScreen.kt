package com.example.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MedRepository
import com.example.data.cloud.CloudflareR2Manager
import com.example.model.McqItem
import com.example.model.McqQuestion
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

/**
 * Intelligent parser supporting:
 * - Direct array of McqQuestion
 * - Markdown backticks (```json ... ```)
 * - Object with "questions" or "mcqs" array
 * - Alternative key names (question, question_text, option_a, answer, etc.)
 */
fun parseMcqJsonText(rawInput: String): Result<List<McqQuestion>> {
    var text = rawInput.trim()
    if (text.isBlank()) return Result.failure(IllegalArgumentException("JSON text is empty"))

    // Strip markdown code fences if present
    if (text.startsWith("```")) {
        text = text.substringAfter("\n", text.removePrefix("```json").removePrefix("```"))
    }
    if (text.endsWith("```")) {
        text = text.substringBeforeLast("```").trim()
    }

    val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // 1. Try direct deserialization
    try {
        val directList = jsonConfig.decodeFromString<List<McqQuestion>>(text)
        if (directList.isNotEmpty()) {
            return Result.success(sanitizeQuestions(directList))
        }
    } catch (_: Exception) {}

    // 2. Try generic JsonElement parsing
    try {
        val rootElement = jsonConfig.parseToJsonElement(text)
        if (rootElement is JsonObject) {
            val array = rootElement["questions"] ?: rootElement["mcqs"] ?: rootElement["data"] ?: rootElement["items"]
            if (array is JsonArray) {
                val list = parseJsonArrayOfQuestions(array)
                if (list.isNotEmpty()) return Result.success(list)
            }
        } else if (rootElement is JsonArray) {
            val list = parseJsonArrayOfQuestions(rootElement)
            if (list.isNotEmpty()) return Result.success(list)
        }
    } catch (e: Exception) {
        return Result.failure(e)
    }

    return Result.failure(IllegalArgumentException("Could not extract valid MCQ questions from JSON. Please verify structure."))
}

private fun parseJsonArrayOfQuestions(array: JsonArray): List<McqQuestion> {
    val result = mutableListOf<McqQuestion>()
    for (elem in array) {
        if (elem is JsonObject) {
            val qText = elem["questionText"]?.jsonPrimitive?.contentOrNull
                ?: elem["question"]?.jsonPrimitive?.contentOrNull
                ?: elem["question_text"]?.jsonPrimitive?.contentOrNull ?: ""

            val optA = elem["optionA"]?.jsonPrimitive?.contentOrNull
                ?: elem["option_a"]?.jsonPrimitive?.contentOrNull
                ?: elem["optA"]?.jsonPrimitive?.contentOrNull
                ?: elem["a"]?.jsonPrimitive?.contentOrNull ?: ""

            val optB = elem["optionB"]?.jsonPrimitive?.contentOrNull
                ?: elem["option_b"]?.jsonPrimitive?.contentOrNull
                ?: elem["optB"]?.jsonPrimitive?.contentOrNull
                ?: elem["b"]?.jsonPrimitive?.contentOrNull ?: ""

            val optC = elem["optionC"]?.jsonPrimitive?.contentOrNull
                ?: elem["option_c"]?.jsonPrimitive?.contentOrNull
                ?: elem["optC"]?.jsonPrimitive?.contentOrNull
                ?: elem["c"]?.jsonPrimitive?.contentOrNull ?: ""

            val optD = elem["optionD"]?.jsonPrimitive?.contentOrNull
                ?: elem["option_d"]?.jsonPrimitive?.contentOrNull
                ?: elem["optD"]?.jsonPrimitive?.contentOrNull
                ?: elem["d"]?.jsonPrimitive?.contentOrNull ?: ""

            val correct = elem["correctOption"]?.jsonPrimitive?.contentOrNull
                ?: elem["correct_option"]?.jsonPrimitive?.contentOrNull
                ?: elem["answer"]?.jsonPrimitive?.contentOrNull
                ?: elem["correct"]?.jsonPrimitive?.contentOrNull ?: "A"

            val explanation = elem["explanation"]?.jsonPrimitive?.contentOrNull
                ?: elem["rationale"]?.jsonPrimitive?.contentOrNull ?: ""

            if (qText.isNotBlank()) {
                result.add(
                    McqQuestion(
                        id = java.util.UUID.randomUUID().toString(),
                        questionText = qText.trim(),
                        optionA = optA.trim(),
                        optionB = optB.trim(),
                        optionC = optC.trim(),
                        optionD = optD.trim(),
                        correctOption = normalizeCorrectOption(correct),
                        explanation = explanation.trim()
                    )
                )
            }
        }
    }
    return result
}

private fun normalizeCorrectOption(raw: String): String {
    val clean = raw.trim().uppercase()
    return when {
        clean.startsWith("A") || clean == "OPTION A" || clean == "1" -> "A"
        clean.startsWith("B") || clean == "OPTION B" || clean == "2" -> "B"
        clean.startsWith("C") || clean == "OPTION C" || clean == "3" -> "C"
        clean.startsWith("D") || clean == "OPTION D" || clean == "4" -> "D"
        else -> "A"
    }
}

private fun sanitizeQuestions(list: List<McqQuestion>): List<McqQuestion> {
    return list.map { q ->
        q.copy(
            id = if (q.id.isNotBlank()) q.id else java.util.UUID.randomUUID().toString(),
            questionText = q.questionText.trim(),
            optionA = q.optionA.trim(),
            optionB = q.optionB.trim(),
            optionC = q.optionC.trim(),
            optionD = q.optionD.trim(),
            correctOption = normalizeCorrectOption(q.correctOption),
            explanation = q.explanation.trim()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMcqUploadScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var uploadMode by remember { mutableIntStateOf(0) } // 0 = JSON Paste, 1 = Manual Entry

    var folderName by remember { mutableStateOf("") }
    var subfolderName by remember { mutableStateOf("") }
    var showSubSubfolder by remember { mutableStateOf(false) }
    var subSubfolderName by remember { mutableStateOf("") }
    var chapterName by remember { mutableStateOf("") }

    // JSON Paste State
    var jsonPasteText by remember { mutableStateOf("") }
    var showParsedPreview by remember { mutableStateOf(false) }

    // Manual MCQ Question states
    var qText by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correctOpt by remember { mutableStateOf("A") }
    var explanation by remember { mutableStateOf("") }
    var qImgUri by remember { mutableStateOf<Uri?>(null) }
    var aImgUri by remember { mutableStateOf<Uri?>(null) }

    val manualQuestionsList = remember { mutableStateListOf<McqQuestion>() }

    var isUploading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val allExistingMcqs by MedRepository.mcqItems.collectAsState()

    val qImgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> qImgUri = uri }
    val aImgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> aImgUri = uri }

    val parsedJsonResult = remember(jsonPasteText) {
        if (jsonPasteText.isBlank()) null else parseMcqJsonText(jsonPasteText)
    }

    val computedR2Key = remember(folderName, subfolderName, subSubfolderName, chapterName) {
        if (folderName.isNotBlank() && subfolderName.isNotBlank() && chapterName.isNotBlank()) {
            MedRepository.buildMcqR2Key(folderName, subfolderName, subSubfolderName, chapterName)
        } else ""
    }

    BackHandler { onBackClick() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload MCQ Practice", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E3A8A)) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mode Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { uploadMode = 0 },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (uploadMode == 0) Color(0xFF2563EB) else Color.Transparent)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (uploadMode == 0) Color.White else Color(0xFF475569))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste JSON Text", fontSize = 13.sp, color = if (uploadMode == 0) Color.White else Color(0xFF475569), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { uploadMode = 1 },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (uploadMode == 1) Color(0xFF2563EB) else Color.Transparent)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (uploadMode == 1) Color.White else Color(0xFF475569))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manual Entry", fontSize = 13.sp, color = if (uploadMode == 1) Color.White else Color(0xFF475569), fontWeight = FontWeight.Bold)
                }
            }

            // Hierarchy Folders
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📁 Cloudflare Hierarchy & Storage Path", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E3A8A))

                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder Name (e.g. Anatomy)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = subfolderName,
                        onValueChange = { subfolderName = it },
                        label = { Text("Subfolder Name (e.g. General Anatomy)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    if (!showSubSubfolder) {
                        OutlinedButton(
                            onClick = { showSubSubfolder = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Optional Subfolder 2", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = subSubfolderName,
                                onValueChange = { subSubfolderName = it },
                                label = { Text("Subfolder 2 (Optional)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            IconButton(onClick = {
                                showSubSubfolder = false
                                subSubfolderName = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFDC2626))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = chapterName,
                        onValueChange = { chapterName = it },
                        label = { Text("Chapter / Unit Name (e.g. Osteology)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    if (computedR2Key.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("☁️ R2 Key: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(computedR2Key, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF2563EB), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // Check if Chapter already exists and offer merge/replace option
            val existingChapterMatch = remember(folderName, subfolderName, subSubfolderName, chapterName, allExistingMcqs) {
                allExistingMcqs.firstOrNull {
                    it.folderName.trim().equals(folderName.trim(), ignoreCase = true) &&
                    it.subfolderName.trim().equals(subfolderName.trim(), ignoreCase = true) &&
                    it.subSubfolderName.trim().equals(subSubfolderName.trim(), ignoreCase = true) &&
                    it.chapterName.trim().equals(chapterName.trim(), ignoreCase = true)
                }
            }

            var mergeOption by remember { mutableStateOf(true) } // true = Append, false = Replace
            if (existingChapterMatch != null && chapterName.isNotBlank()) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("⚠️ Chapter already exists (${existingChapterMatch.questions.size} questions).", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = mergeOption, onClick = { mergeOption = true })
                            Text("Append new questions", fontSize = 12.sp, color = Color(0xFF78350F))
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(selected = !mergeOption, onClick = { mergeOption = false })
                            Text("Replace all", fontSize = 12.sp, color = Color(0xFF78350F))
                        }
                    }
                }
            }

            if (uploadMode == 0) {
                // Direct JSON Copy-Paste Area
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Direct JSON Paste (ChatGPT)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E3A8A))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        val clipText = clipboardManager.getText()?.text.orEmpty()
                                        if (clipText.isNotBlank()) {
                                            jsonPasteText = clipText
                                            android.widget.Toast.makeText(context, "Pasted from clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Clipboard is empty", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Paste Clipboard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (jsonPasteText.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { jsonPasteText = "" },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Clear", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = jsonPasteText,
                            onValueChange = { jsonPasteText = it },
                            label = { Text("Paste Raw JSON Text Here (from ChatGPT)") },
                            placeholder = {
                                Text(
                                    "[\n  {\n    \"questionText\": \"What is the longest bone?\",\n    \"optionA\": \"Femur\",\n    \"optionB\": \"Tibia\",\n    \"optionC\": \"Humerus\",\n    \"optionD\": \"Fibula\",\n    \"correctOption\": \"A\",\n    \"explanation\": \"Femur is the thigh bone.\"\n  }\n]",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 320.dp),
                            shape = RoundedCornerShape(10.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )

                        // Real-time Parser Status Indicator
                        if (parsedJsonResult != null) {
                            if (parsedJsonResult.isSuccess) {
                                val count = parsedJsonResult.getOrThrow().size
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("✓ ", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                                            Text(
                                                "Valid JSON: $count Questions parsed successfully!",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D)
                                            )
                                        }
                                        TextButton(onClick = { showParsedPreview = !showParsedPreview }) {
                                            Text(if (showParsedPreview) "Hide Preview" else "Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = showParsedPreview) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Preview of Parsed Questions:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                        parsedJsonResult.getOrThrow().take(5).forEachIndexed { idx, q ->
                                            Surface(
                                                color = Color(0xFFF8FAFC),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text("Q${idx + 1}: ${q.questionText}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                    Text("A) ${q.optionA}   B) ${q.optionB}", fontSize = 11.sp, color = Color(0xFF64748B))
                                                    Text("C) ${q.optionC}   D) ${q.optionD}", fontSize = 11.sp, color = Color(0xFF64748B))
                                                    Text("✓ Correct: ${q.correctOption}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                                }
                                            }
                                        }
                                        if (parsedJsonResult.getOrThrow().size > 5) {
                                            Text("+ ${parsedJsonResult.getOrThrow().size - 5} more questions...", fontSize = 11.sp, color = Color(0xFF64748B))
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    color = Color(0xFFFEE2E2),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚠️ ", color = Color(0xFFDC2626))
                                        Text(
                                            "JSON Error: ${parsedJsonResult.exceptionOrNull()?.localizedMessage ?: "Invalid format"}",
                                            fontSize = 12.sp,
                                            color = Color(0xFFB91C1C),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Manual MCQ Form
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Manual Question #${manualQuestionsList.size + 1}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E3A8A))
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFDBEAFE)) {
                                Text("In Queue: ${manualQuestionsList.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }

                        OutlinedTextField(
                            value = qText,
                            onValueChange = { qText = it },
                            label = { Text("Question Text") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option A") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option C") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option D") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Correct Option:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            listOf("A", "B", "C", "D").forEach { opt ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = correctOpt == opt, onClick = { correctOpt = opt })
                                    Text(opt, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = explanation,
                            onValueChange = { explanation = it },
                            label = { Text("Explanation / Clinical rationale") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Image Uploaders
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { qImgLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (qImgUri != null) "Q. Image Added" else "Q. Image", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { aImgLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (aImgUri != null) "Ans. Image Added" else "Ans. Image", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = {
                                if (qText.isBlank() || optA.isBlank() || optB.isBlank()) {
                                    android.widget.Toast.makeText(context, "Please enter question and options A & B", android.widget.Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                coroutineScope.launch {
                                    var qImgUrl = ""
                                    var aImgUrl = ""
                                    if (qImgUri != null) {
                                        val res = CloudflareR2Manager.uploadCompressedImage(context, qImgUri!!, "mcq_imgs/${System.currentTimeMillis()}_q.jpg")
                                        if (res.isSuccess) qImgUrl = res.getOrThrow()
                                    }
                                    if (aImgUri != null) {
                                        val res = CloudflareR2Manager.uploadCompressedImage(context, aImgUri!!, "mcq_imgs/${System.currentTimeMillis()}_a.jpg")
                                        if (res.isSuccess) aImgUrl = res.getOrThrow()
                                    }

                                    manualQuestionsList.add(
                                        McqQuestion(
                                            questionText = qText.trim(),
                                            optionA = optA.trim(),
                                            optionB = optB.trim(),
                                            optionC = optC.trim(),
                                            optionD = optD.trim(),
                                            correctOption = correctOpt,
                                            explanation = explanation.trim(),
                                            questionImageUrl = qImgUrl,
                                            answerImageUrl = aImgUrl
                                        )
                                    )
                                    qText = ""
                                    optA = ""
                                    optB = ""
                                    optC = ""
                                    optD = ""
                                    explanation = ""
                                    qImgUri = null
                                    aImgUri = null
                                    android.widget.Toast.makeText(context, "Question added to batch!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("+ Add Question #${manualQuestionsList.size + 1} to Batch", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                if (manualQuestionsList.isNotEmpty()) {
                    Text("Queued Questions (${manualQuestionsList.size}):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    manualQuestionsList.forEachIndexed { idx, q ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Q${idx + 1}: ${q.questionText}", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                IconButton(onClick = { manualQuestionsList.removeAt(idx) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isUploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(6.dp), color = Color(0xFF2563EB))
                Text("Saving to Cloudflare R2 hierarchy: $computedR2Key...", fontSize = 12.sp, color = Color(0xFF2563EB))
            } else if (statusMessage != null) {
                Surface(
                    color = if (statusMessage!!.startsWith("✓")) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (statusMessage!!.startsWith("✓")) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (statusMessage!!.startsWith("✓")) "✓ " else "⚠️ ", fontSize = 15.sp)
                        Text(
                            text = statusMessage!!,
                            color = if (statusMessage!!.startsWith("✓")) Color(0xFF15803D) else Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Primary Upload Button
            Button(
                onClick = {
                    if (folderName.isBlank() || subfolderName.isBlank() || chapterName.isBlank()) {
                        android.widget.Toast.makeText(context, "Please fill Folder, Subfolder, and Chapter name", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val incomingQuestions: List<McqQuestion> = if (uploadMode == 0) {
                        if (parsedJsonResult == null || parsedJsonResult.isFailure) {
                            android.widget.Toast.makeText(context, "Please paste valid JSON text first", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        parsedJsonResult.getOrThrow()
                    } else {
                        if (manualQuestionsList.isEmpty()) {
                            android.widget.Toast.makeText(context, "Please add at least one question", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        manualQuestionsList.toList()
                    }

                    if (incomingQuestions.isEmpty()) {
                        android.widget.Toast.makeText(context, "No questions found to upload", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isUploading = true
                    statusMessage = "Uploading chapter to Cloudflare R2: $computedR2Key..."

                    coroutineScope.launch {
                        try {
                            val targetQuestions = if (existingChapterMatch != null && mergeOption) {
                                existingChapterMatch.questions + incomingQuestions
                            } else {
                                incomingQuestions
                            }

                            val cleanChap = chapterName.trim()
                            val item = McqItem(
                                id = existingChapterMatch?.id ?: java.util.UUID.randomUUID().toString(),
                                folderName = folderName.trim(),
                                subfolderName = subfolderName.trim(),
                                subSubfolderName = subSubfolderName.trim(),
                                chapterName = cleanChap,
                                questions = targetQuestions,
                                questionCount = targetQuestions.size,
                                r2Key = computedR2Key,
                                r2Url = "${CloudflareR2Manager.publicBaseUrl.value.trimEnd('/')}/$computedR2Key"
                            )

                            // Save to Cloudflare R2 at mcqs/<folder>/<subfolder>/[subfolder2]/<chapter>.json
                            MedRepository.addMcqItem(item, syncToCloudflareR2 = true)

                            android.widget.Toast.makeText(context, "✓ Chapter saved to Cloudflare R2 successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            statusMessage = "✓ Successfully uploaded to Cloudflare R2: $computedR2Key (${targetQuestions.size} questions). Ready for next upload."

                            // Reset question inputs but KEEP folder context for fast repeated uploads!
                            jsonPasteText = ""
                            manualQuestionsList.clear()
                            qText = ""
                            optA = ""
                            optB = ""
                            optC = ""
                            optD = ""
                            correctOpt = "A"
                            explanation = ""
                            qImgUri = null
                            aImgUri = null
                            chapterName = ""
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                            statusMessage = "Error: ${e.localizedMessage}"
                        } finally {
                            isUploading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Sync to Cloudflare R2", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Done / Back to Admin Panel", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}
