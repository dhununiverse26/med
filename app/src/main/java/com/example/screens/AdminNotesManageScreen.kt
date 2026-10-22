package com.example.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MedRepository
import com.example.model.AdminNoteItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotesManageScreen(
    onBackClick: () -> Unit,
    onNavigateToUpload: (() -> Unit)? = null
) {
    val adminNotes by MedRepository.adminNotes.collectAsState()

    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var selectedSubfolder by remember { mutableStateOf<String?>(null) }
    var selectedSubSubfolder by remember { mutableStateOf<String?>(null) }

    // Dialog state for Renaming Folder
    var renameFolderTarget by remember { mutableStateOf<String?>(null) }
    var newFolderNameInput by remember { mutableStateOf("") }

    // Dialog state for Deleting Folder
    var deleteFolderTarget by remember { mutableStateOf<String?>(null) }

    // Dialog state for Renaming Subfolder
    var renameSubfolderTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var newSubfolderNameInput by remember { mutableStateOf("") }

    // Dialog state for Deleting Subfolder
    var deleteSubfolderTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Dialog state for Editing Chapter
    var editingChapterItem by remember { mutableStateOf<AdminNoteItem?>(null) }
    var newChapterNameInput by remember { mutableStateOf("") }

    // Dialog state for Deleting Chapter
    var deletingChapterItem by remember { mutableStateOf<AdminNoteItem?>(null) }

    // Back handling
    BackHandler {
        when {
            selectedSubSubfolder != null -> selectedSubSubfolder = null
            selectedSubfolder != null -> selectedSubfolder = null
            selectedFolder != null -> selectedFolder = null
            else -> onBackClick()
        }
    }

    // Rename Folder Dialog
    if (renameFolderTarget != null) {
        AlertDialog(
            onDismissRequest = { renameFolderTarget = null },
            title = { Text("Edit Folder Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderNameInput,
                    onValueChange = { newFolderNameInput = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFolderNameInput.isNotBlank()) {
                        MedRepository.renameAdminFolder(renameFolderTarget!!, newFolderNameInput.trim())
                        if (selectedFolder == renameFolderTarget) {
                            selectedFolder = newFolderNameInput.trim()
                        }
                    }
                    renameFolderTarget = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameFolderTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Folder Dialog
    if (deleteFolderTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteFolderTarget = null },
            title = { Text("Delete Folder?", fontWeight = FontWeight.Bold) },
            text = { Text("Delete \"${deleteFolderTarget}\" and all subfolders/notes inside it?") },
            confirmButton = {
                Button(
                    onClick = {
                        MedRepository.deleteAdminFolder(deleteFolderTarget!!)
                        if (selectedFolder == deleteFolderTarget) {
                            selectedFolder = null
                            selectedSubfolder = null
                            selectedSubSubfolder = null
                        }
                        deleteFolderTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteFolderTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Subfolder Dialog
    if (renameSubfolderTarget != null) {
        AlertDialog(
            onDismissRequest = { renameSubfolderTarget = null },
            title = { Text("Edit Subfolder Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSubfolderNameInput,
                    onValueChange = { newSubfolderNameInput = it },
                    label = { Text("Subfolder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newSubfolderNameInput.isNotBlank()) {
                        val (folder, oldSub) = renameSubfolderTarget!!
                        MedRepository.renameAdminSubfolder(folder, oldSub, newSubfolderNameInput.trim())
                        if (selectedSubfolder == oldSub) {
                            selectedSubfolder = newSubfolderNameInput.trim()
                        }
                    }
                    renameSubfolderTarget = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameSubfolderTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Subfolder Dialog
    if (deleteSubfolderTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteSubfolderTarget = null },
            title = { Text("Delete Subfolder?", fontWeight = FontWeight.Bold) },
            text = { Text("Delete subfolder \"${deleteSubfolderTarget?.second}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        val (folder, sub) = deleteSubfolderTarget!!
                        MedRepository.deleteAdminSubfolder(folder, sub)
                        if (selectedSubfolder == sub) {
                            selectedSubfolder = null
                            selectedSubSubfolder = null
                        }
                        deleteSubfolderTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteSubfolderTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Chapter Dialog
    if (editingChapterItem != null) {
        AlertDialog(
            onDismissRequest = { editingChapterItem = null },
            title = { Text("Edit Chapter Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newChapterNameInput,
                    onValueChange = { newChapterNameInput = it },
                    label = { Text("Chapter Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    val item = editingChapterItem!!
                    if (newChapterNameInput.isNotBlank()) {
                        MedRepository.editAdminNote(
                            id = item.id,
                            folderName = item.folderName,
                            subfolderName = item.subfolderName,
                            subSubfolderName = item.subSubfolderName,
                            chapterName = newChapterNameInput.trim(),
                            pdfTitle = item.pdfTitle
                        )
                    }
                    editingChapterItem = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingChapterItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Chapter Dialog
    if (deletingChapterItem != null) {
        AlertDialog(
            onDismissRequest = { deletingChapterItem = null },
            title = { Text("Delete Chapter?", fontWeight = FontWeight.Bold) },
            text = { Text("Delete chapter \"${deletingChapterItem?.chapterName}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        val item = deletingChapterItem!!
                        MedRepository.deleteAdminNote(item.id)
                        deletingChapterItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingChapterItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Manage Notes (Admin)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = when {
                                selectedSubSubfolder != null -> "${selectedFolder} > ${selectedSubfolder} > ${selectedSubSubfolder}"
                                selectedSubfolder != null -> "${selectedFolder} > ${selectedSubfolder}"
                                selectedFolder != null -> "${selectedFolder}"
                                else -> "All Folders"
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC))
            )
        },
        floatingActionButton = {
            if (onNavigateToUpload != null) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToUpload,
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                    text = { Text("Upload Note", fontWeight = FontWeight.Bold) }
                )
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // LEVEL 1: FOLDERS LIST
            if (selectedFolder == null) {
                val distinctFolders = adminNotes.map { it.folderName }.filter { it.isNotBlank() }.distinct()
                if (distinctFolders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No folders created yet.", color = Color(0xFF64748B), fontSize = 14.sp)
                        }
                    }
                } else {
                    items(distinctFolders) { folderName ->
                        val count = adminNotes.count { it.folderName == folderName }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFolder = folderName }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFEFF6FF),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("📁", fontSize = 22.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = folderName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "$count item(s)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(
                                        onClick = { MedRepository.moveAdminFolderUp(folderName) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { MedRepository.moveAdminFolderDown(folderName) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            newFolderNameInput = folderName
                                            renameFolderTarget = folderName
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { deleteFolderTarget = folderName },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // LEVEL 2: SUBFOLDERS LIST
            else if (selectedSubfolder == null) {
                val notesInFolder = adminNotes.filter { it.folderName == selectedFolder }
                val distinctSubfolders = notesInFolder.map { it.subfolderName }.filter { it.isNotBlank() }.distinct()

                if (distinctSubfolders.isEmpty()) {
                    // Display chapters directly under this folder
                    items(notesInFolder, key = { it.id }) { item ->
                        ChapterManageCard(
                            item = item,
                            onEdit = {
                                newChapterNameInput = item.chapterName
                                editingChapterItem = item
                            },
                            onDelete = { deletingChapterItem = item },
                            onMoveUp = { MedRepository.moveAdminNoteUp(item.id) },
                            onMoveDown = { MedRepository.moveAdminNoteDown(item.id) }
                        )
                    }
                } else {
                    items(distinctSubfolders) { subfolderName ->
                        val subCount = notesInFolder.count { it.subfolderName == subfolderName }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSubfolder = subfolderName }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF0FDF4),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("📂", fontSize = 22.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = subfolderName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "$subCount item(s)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(
                                        onClick = { MedRepository.moveAdminSubfolderUp(selectedFolder!!, subfolderName) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { MedRepository.moveAdminSubfolderDown(selectedFolder!!, subfolderName) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            newSubfolderNameInput = subfolderName
                                            renameSubfolderTarget = Pair(selectedFolder!!, subfolderName)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { deleteSubfolderTarget = Pair(selectedFolder!!, subfolderName) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // LEVEL 3: SUB-SUBFOLDERS OR CHAPTERS
            else if (selectedSubSubfolder == null) {
                val notesInSub = adminNotes.filter { it.folderName == selectedFolder && it.subfolderName == selectedSubfolder }
                val distinctSubSub = notesInSub.map { it.subSubfolderName }.filter { it.isNotBlank() }.distinct()

                if (distinctSubSub.isNotEmpty()) {
                    items(distinctSubSub) { subSubName ->
                        val count = notesInSub.count { it.subSubfolderName == subSubName }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSubSubfolder = subSubName }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFFEF3C7),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("📁", fontSize = 22.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = subSubName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "$count item(s)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF2563EB))
                            }
                        }
                    }
                } else {
                    // Display Chapters under this subfolder
                    items(notesInSub, key = { it.id }) { item ->
                        ChapterManageCard(
                            item = item,
                            onEdit = {
                                newChapterNameInput = item.chapterName
                                editingChapterItem = item
                            },
                            onDelete = { deletingChapterItem = item },
                            onMoveUp = { MedRepository.moveAdminNoteUp(item.id) },
                            onMoveDown = { MedRepository.moveAdminNoteDown(item.id) }
                        )
                    }
                }
            }
            // LEVEL 4: INSIDE SUB-SUBFOLDER (CHAPTERS)
            else {
                val notesInSubSub = adminNotes.filter {
                    it.folderName == selectedFolder &&
                    it.subfolderName == selectedSubfolder &&
                    it.subSubfolderName == selectedSubSubfolder
                }
                items(notesInSubSub, key = { it.id }) { item ->
                    ChapterManageCard(
                        item = item,
                        onEdit = {
                            newChapterNameInput = item.chapterName
                            editingChapterItem = item
                        },
                        onDelete = { deletingChapterItem = item },
                        onMoveUp = { MedRepository.moveAdminNoteUp(item.id) },
                        onMoveDown = { MedRepository.moveAdminNoteDown(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterManageCard(
    item: AdminNoteItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFEFF6FF),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("📄", fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.chapterName.ifBlank { item.pdfTitle },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
                if (item.pdfTitle.isNotBlank() && item.pdfTitle != item.chapterName) {
                    Text(
                        text = item.pdfTitle,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
