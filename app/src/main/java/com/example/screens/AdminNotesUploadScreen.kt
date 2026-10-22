package com.example.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MedRepository
import com.example.data.cloud.CloudflareR2Manager
import com.example.model.AdminNoteItem
import kotlinx.coroutines.launch

private fun getFileNameFromUri(context: Context, uri: Uri): String {
    var name: String? = null
    if (uri.scheme == "content") {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
        } catch (_: Exception) {}
    }
    if (name.isNullOrBlank()) {
        name = uri.lastPathSegment?.substringAfterLast('/')
    }
    return name ?: "note_${System.currentTimeMillis()}.pdf"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotesUploadScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allNotes by MedRepository.adminNotes.collectAsState()

    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var folderName by remember { mutableStateOf("") }
    var subfolderName by remember { mutableStateOf("") }
    var showSubSubfolder by remember { mutableStateOf(false) }
    var subSubfolderName by remember { mutableStateOf("") }
    var chapterName by remember { mutableStateOf("") }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfName by remember { mutableStateOf("") }

    val existingFolders = remember(allNotes) {
        allNotes.map { it.folderName }.filter { it.isNotBlank() }.distinct()
    }
    val existingSubfolders = remember(allNotes, folderName) {
        if (folderName.isBlank()) emptyList()
        else allNotes.filter { it.folderName.equals(folderName, ignoreCase = true) }
            .map { it.subfolderName }
            .filter { it.isNotBlank() }
            .distinct()
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPdfUri = uri
            selectedPdfName = getFileNameFromUri(context, uri)
        }
    }

    BackHandler {
        onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Notes (PDF)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E3A8A)) },
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
            // STEP 1: FOLDER
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("1. Folder Name *", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        placeholder = { Text("e.g. Medicine, Surgery, Pharmacology", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    if (existingFolders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Or select existing folder:", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(existingFolders) { f ->
                                val isSelected = folderName.equals(f, ignoreCase = true)
                                Surface(
                                    onClick = { folderName = f },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1))
                                ) {
                                    Text(
                                        text = "📁 $f",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // STEP 2: SUBFOLDER
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("2. Subfolder Name *", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = subfolderName,
                        onValueChange = { subfolderName = it },
                        placeholder = { Text("e.g. Cardiology, Neurology, General", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    if (existingSubfolders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Or select existing subfolder:", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(existingSubfolders) { sub ->
                                val isSelected = subfolderName.equals(sub, ignoreCase = true)
                                Surface(
                                    onClick = { subfolderName = sub },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFF0FDF4) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF16A34A) else Color(0xFFCBD5E1))
                                ) {
                                    Text(
                                        text = "📂 $sub",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF15803D) else Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ADD MORE SUBFOLDER ICON / BUTTON
                    if (!showSubSubfolder) {
                        OutlinedButton(
                            onClick = { showSubSubfolder = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add More", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Add More Subfolder (Nested)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Text("Nested Subfolder (Optional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = subSubfolderName,
                                    onValueChange = { subSubfolderName = it },
                                    placeholder = { Text("e.g. ECG, Heart Failure, Arrhythmia", color = Color(0xFF94A3B8)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    showSubSubfolder = false
                                    subSubfolderName = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFDC2626))
                                }
                            }
                        }
                    }
                }
            }

            // STEP 3: CHAPTER NAME
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("3. Chapter Name *", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = chapterName,
                        onValueChange = { chapterName = it },
                        placeholder = { Text("e.g. Chapter 1: Acute Coronary Syndrome", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }

            // STEP 4: PDF FILE UPLOAD
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("4. Select PDF Document *", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedPdfUri != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedPdfName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF14532D)
                                    )
                                    Text(
                                        text = "✓ PDF Selected & Ready to Upload",
                                        fontSize = 11.sp,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                                IconButton(onClick = {
                                    selectedPdfUri = null
                                    selectedPdfName = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFFDC2626))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { pdfLauncher.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Different PDF File", fontSize = 13.sp)
                        }
                    } else {
                        Button(
                            onClick = { pdfLauncher.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("📁 Select PDF from Device", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // Cloudflare R2 Path Preview
            if (folderName.isNotBlank() && subfolderName.isNotBlank() && chapterName.isNotBlank()) {
                val cleanFolder = folderName.trim().replace(" ", "_").replace("/", "_")
                val cleanSub = subfolderName.trim().replace(" ", "_").replace("/", "_")
                val cleanSubSub = subSubfolderName.trim().replace(" ", "_").replace("/", "_")
                val cleanChapter = chapterName.trim().replace(" ", "_").replace("/", "_")
                val cleanFileName = if (selectedPdfName.isNotBlank()) selectedPdfName.replace(" ", "_").replace("/", "_") else "$cleanChapter.pdf"
                val pathPreview = if (cleanSubSub.isNotBlank()) {
                    "notes/$cleanFolder/$cleanSub/$cleanSubSub/$cleanChapter/$cleanFileName"
                } else {
                    "notes/$cleanFolder/$cleanSub/$cleanChapter/$cleanFileName"
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("☁️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Cloudflare R2 Storage Hierarchy:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                            Text(pathPreview, fontSize = 11.sp, color = Color(0xFF1E3A8A), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (isUploading) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Color(0xFF2563EB)
                        )
                        Text(
                            text = statusMessage ?: "Uploading PDF to Cloudflare R2 bucket...",
                            fontSize = 12.sp,
                            color = Color(0xFF1E40AF),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (statusMessage != null) {
                Surface(
                    color = if (statusMessage!!.startsWith("✓")) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (statusMessage!!.startsWith("✓")) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage!!,
                        fontSize = 13.sp,
                        color = if (statusMessage!!.startsWith("✓")) Color(0xFF15803D) else Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // ACTION: UPLOAD & SAVE
            Button(
                onClick = {
                    if (folderName.isBlank() || subfolderName.isBlank() || chapterName.isBlank() || selectedPdfUri == null) {
                        android.widget.Toast.makeText(context, "Please fill Folder, Subfolder, Chapter & select a PDF", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (isUploading) return@Button

                    isUploading = true
                    uploadProgress = 0f
                    statusMessage = "Uploading PDF to Cloudflare R2 bucket... 0%"

                    coroutineScope.launch {
                        try {
                            val cleanFolder = folderName.trim().replace(" ", "_").replace("/", "_")
                            val cleanSub = subfolderName.trim().replace(" ", "_").replace("/", "_")
                            val cleanSubSub = subSubfolderName.trim().replace(" ", "_").replace("/", "_")
                            val cleanChapter = chapterName.trim().replace(" ", "_").replace("/", "_")
                            val originalFileName = if (selectedPdfName.isNotBlank()) selectedPdfName else "$cleanChapter.pdf"
                            val cleanFileName = originalFileName.replace(" ", "_").replace("/", "_")

                            val pathPrefix = if (cleanSubSub.isNotBlank()) {
                                "notes/$cleanFolder/$cleanSub/$cleanSubSub/$cleanChapter"
                            } else {
                                "notes/$cleanFolder/$cleanSub/$cleanChapter"
                            }
                            val objectKey = "$pathPrefix/$cleanFileName"

                            val uploadRes = CloudflareR2Manager.uploadCompressedPdf(
                                context = context,
                                pdfUri = selectedPdfUri!!,
                                objectKey = objectKey
                            ) { progress ->
                                uploadProgress = progress
                                statusMessage = "Uploading to Cloudflare R2... ${(progress * 100).toInt()}%"
                            }

                            val finalPdfUrl = if (uploadRes.isSuccess) {
                                uploadRes.getOrThrow()
                            } else {
                                selectedPdfUri.toString()
                            }

                            val item = AdminNoteItem(
                                folderName = folderName.trim(),
                                subfolderName = subfolderName.trim(),
                                subSubfolderName = subSubfolderName.trim(),
                                chapterName = chapterName.trim(),
                                pdfTitle = if (selectedPdfName.isNotBlank()) selectedPdfName else chapterName.trim(),
                                pdfUri = finalPdfUrl
                            )
                            MedRepository.addAdminNote(item)

                            if (uploadRes.isSuccess) {
                                android.widget.Toast.makeText(context, "✓ Note uploaded to Cloudflare & Catalog updated!", android.widget.Toast.LENGTH_SHORT).show()
                                statusMessage = "✓ Note \"$cleanChapter\" successfully saved to Cloudflare & Catalog! Ready for next upload."
                            } else {
                                val err = uploadRes.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                                android.widget.Toast.makeText(context, "Saved locally & Catalog (Cloudflare: $err)", android.widget.Toast.LENGTH_LONG).show()
                                statusMessage = "✓ Note \"$cleanChapter\" saved locally & Catalog (Cloudflare: $err). Ready for next upload."
                            }
                            // Reset chapter & PDF so user can upload next chapter to the same folder/subfolder!
                            chapterName = ""
                            selectedPdfUri = null
                            selectedPdfName = ""
                            uploadProgress = 0f
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            e.printStackTrace()
                            statusMessage = "Error: ${e.localizedMessage}"
                        } finally {
                            isUploading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uploading to Cloudflare...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload & Save Note", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Done / Back to Notes Menu", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}
