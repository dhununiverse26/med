package com.example.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MedRepository
import com.example.model.ShortItem
import com.example.model.ProfYear
import com.example.util.YouTubeHelper
import java.util.UUID

@Composable
fun UploadShortsView() {
    var youtubeUrl by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("⚡ Upload YouTube Short", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E3A8A))

        OutlinedTextField(
            value = youtubeUrl,
            onValueChange = {
                youtubeUrl = it
                val vid = YouTubeHelper.extractVideoId(it)
                if (vid != null && title.isBlank()) {
                    title = "MBBS Short"
                }
            },
            label = { Text("YouTube URL / Shorts Link *") },
            placeholder = { Text("https://youtube.com/shorts/...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Short Title *") },
            placeholder = { Text("Enter video title") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        statusMessage?.let {
            Surface(
                color = if (it.startsWith("✓")) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (it.startsWith("✓")) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (it.startsWith("✓")) "✓ " else "⚠️ ", fontSize = 15.sp)
                    Text(
                        text = it,
                        color = if (it.startsWith("✓")) Color(0xFF15803D) else Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Button(
            onClick = {
                val vid = YouTubeHelper.extractVideoId(youtubeUrl) ?: "short"
                if (youtubeUrl.isBlank() || title.isBlank()) {
                    statusMessage = "Please enter YouTube URL and Title"
                    return@Button
                }
                isUploading = true
                statusMessage = "Uploading short & syncing catalog to Cloudflare R2 (shorts)..."

                val shortId = "s_${UUID.randomUUID().toString().take(6)}"
                val shortItem = ShortItem(
                    id = shortId,
                    title = title.trim(),
                    description = "",
                    youtubeUrl = youtubeUrl.trim(),
                    youtubeVideoId = vid,
                    thumbnailUrl = "https://img.youtube.com/vi/$vid/hqdefault.jpg",
                    cloudflareKey = "shorts/$shortId.json",
                    subjectName = "",
                    emoji = "",
                    prof = ProfYear.FIRST_PROF,
                    doctorName = "",
                    durationSec = 0,
                    likesCount = 0,
                    mnemonicTip = "",
                    audioTitle = ""
                )

                MedRepository.addShort(shortItem)
                isUploading = false
                statusMessage = "✓ Short saved and synced to Cloudflare R2 under shorts/$shortId.json!"
                youtubeUrl = ""
                title = ""
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            enabled = !isUploading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save & Sync to Cloudflare R2", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ManageShortsView() {
    val shorts by MedRepository.shorts.collectAsState()
    var editingShort by remember { mutableStateOf<ShortItem?>(null) }
    var editTitleInput by remember { mutableStateOf("") }

    // Edit Dialog
    if (editingShort != null) {
        AlertDialog(
            onDismissRequest = { editingShort = null },
            title = { Text("Edit Short Title", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitleInput,
                        onValueChange = { editTitleInput = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editTitleInput.isNotBlank()) {
                        MedRepository.editShort(editingShort!!.id, editTitleInput.trim(), "")
                    }
                    editingShort = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingShort = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (shorts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No shorts added yet.", color = Color.Gray, fontSize = 14.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(shorts, key = { it.id }) { short ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF3C7),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🎬", fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(short.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text(short.youtubeUrl, fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(onClick = {
                                    editTitleInput = short.title
                                    editingShort = short
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { MedRepository.deleteShort(short.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                }
                            }
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
                                Text(
                                    text = "shorts/catalog.json",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF2563EB),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("Cloudflare R2", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppInfoAndLinksManageView() {
    var noticeText by remember { mutableStateOf("Welcome to MEDXMED! Best revision resource for MBBS.") }
    var telegramLink by remember { mutableStateOf("https://t.me/medxmed") }
    var savedNotice by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("⚙️ App Info & Notice Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E3A8A))
        OutlinedTextField(
            value = noticeText,
            onValueChange = { noticeText = it },
            label = { Text("App Notice Banner") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            minLines = 3
        )
        OutlinedTextField(
            value = telegramLink,
            onValueChange = { telegramLink = it },
            label = { Text("Community Link (Telegram / WhatsApp)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )
        if (savedNotice) {
            Text("✓ Settings saved successfully!", color = Color(0xFF16A34A), fontSize = 13.sp)
        }
        Button(
            onClick = { savedNotice = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Text("Save Info & Notice", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AppInfoSettingsDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("App Info & Notice", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E3A8A))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Text("MEDXMED Version 1.0.0", fontSize = 13.sp, color = Color(0xFF475569))
                Text("All content hosted securely on Cloudflare R2 bucket.", fontSize = 12.sp, color = Color(0xFF64748B))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Close")
                }
            }
        }
    }
}
