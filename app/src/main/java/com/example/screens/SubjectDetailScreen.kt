package com.example.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MedRepository
import com.example.model.StudyContentItem
import com.example.model.StudyContentType
import com.example.model.Subject

enum class SubjectTab {
    NOTES,
    VIDEOS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subject: Subject,
    onBackClick: () -> Unit,
    onPdfClick: (title: String, uri: String) -> Unit,
    onVideoClick: (title: String, url: String, ch: String, sub: String, fId: String) -> Unit
) {
    LaunchedEffect(Unit) {
        MedRepository.fetchCatalog()
    }

    val folders by MedRepository.folders.collectAsState()
    val contents by MedRepository.contents.collectAsState()

    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var selectedSubfolder by remember { mutableStateOf<String?>(null) }
    var selectedChapter by remember { mutableStateOf<String?>(null) }

    // Section tab: Notes (PDF) or Videos only (No 'All' option)
    var selectedTab by remember { mutableStateOf(SubjectTab.NOTES) }

    // Step-by-step back navigation
    BackHandler {
        when {
            selectedChapter != null -> selectedChapter = null
            selectedSubfolder != null -> selectedSubfolder = null
            selectedFolder != null -> selectedFolder = null
            else -> onBackClick()
        }
    }

    // Filter contents for this subject & selected section (Notes vs Videos)
    val subjectContents = remember(contents, subject, selectedTab) {
        contents.filter { item ->
            item.subjectId == subject.id &&
            when (selectedTab) {
                SubjectTab.NOTES -> item.type == StudyContentType.PDF
                SubjectTab.VIDEOS -> item.type == StudyContentType.VIDEO
            }
        }
    }

    // Counts for badge display
    val totalNotesCount = remember(contents, subject) {
        contents.count { it.subjectId == subject.id && it.type == StudyContentType.PDF }
    }
    val totalVideosCount = remember(contents, subject) {
        contents.count { it.subjectId == subject.id && it.type == StudyContentType.VIDEO }
    }

    // Folders created for this subject
    val subjectFolders = remember(folders, subject) {
        folders.filter { it.subjectId == subject.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(subject.emoji, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = subject.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                        Text(
                            text = when {
                                selectedChapter != null -> "${selectedFolder ?: ""} > ${selectedSubfolder ?: ""} > $selectedChapter"
                                selectedSubfolder != null -> "${selectedFolder ?: ""} > $selectedSubfolder"
                                selectedFolder != null -> "${selectedFolder ?: ""}"
                                else -> if (selectedTab == SubjectTab.NOTES) "Notes & Folders" else "Videos & Folders"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            selectedChapter != null -> selectedChapter = null
                            selectedSubfolder != null -> selectedSubfolder = null
                            selectedFolder != null -> selectedFolder = null
                            else -> onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E3A8A)
                        )
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
        ) {
            // Dedicated Dual Section Selector (Notes vs Videos only)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE2E8F0)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Notes Tab Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = SubjectTab.NOTES },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == SubjectTab.NOTES) Color.White else Color.Transparent,
                        shadowElevation = if (selectedTab == SubjectTab.NOTES) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = if (selectedTab == SubjectTab.NOTES) Color(0xFF2563EB) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Notes",
                                fontWeight = if (selectedTab == SubjectTab.NOTES) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                                color = if (selectedTab == SubjectTab.NOTES) Color(0xFF1E3A8A) else Color(0xFF64748B)
                            )
                            if (totalNotesCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedTab == SubjectTab.NOTES) Color(0xFFDBEAFE) else Color(0xFFCBD5E1)
                                ) {
                                    Text(
                                        text = "$totalNotesCount",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTab == SubjectTab.NOTES) Color(0xFF1D4ED8) else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Videos Tab Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = SubjectTab.VIDEOS },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == SubjectTab.VIDEOS) Color.White else Color.Transparent,
                        shadowElevation = if (selectedTab == SubjectTab.VIDEOS) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = if (selectedTab == SubjectTab.VIDEOS) Color(0xFFDC2626) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Videos",
                                fontWeight = if (selectedTab == SubjectTab.VIDEOS) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                                color = if (selectedTab == SubjectTab.VIDEOS) Color(0xFF991B1B) else Color(0xFF64748B)
                            )
                            if (totalVideosCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedTab == SubjectTab.VIDEOS) Color(0xFFFEE2E2) else Color(0xFFCBD5E1)
                                ) {
                                    Text(
                                        text = "$totalVideosCount",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTab == SubjectTab.VIDEOS) Color(0xFFB91C1C) else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // LEVEL 1: FOLDERS LIST
                if (selectedFolder == null) {
                    val distinctFolderNames = (subjectFolders.map { it.name } + subjectContents.map { it.folderName })
                        .filter { it.isNotBlank() }
                        .distinct()

                    if (distinctFolderNames.isEmpty() && subjectContents.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (selectedTab == SubjectTab.NOTES) "📄" else "🎬",
                                        fontSize = 32.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (selectedTab == SubjectTab.NOTES)
                                            "No notes uploaded yet for ${subject.name}."
                                        else
                                            "No video lectures uploaded yet for ${subject.name}.",
                                        color = Color(0xFF64748B),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(distinctFolderNames) { folderName ->
                            val folderObj = subjectFolders.firstOrNull { it.name.equals(folderName, ignoreCase = true) }
                            val count = subjectContents.count { it.folderName.equals(folderName, ignoreCase = true) }
                            val emoji = folderObj?.emoji ?: "📁"

                            Card(
                                onClick = { selectedFolder = folderName },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFEFF6FF),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(emoji, fontSize = 22.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = folderName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "$count file(s)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF2563EB))
                                }
                            }
                        }

                        // Also show any unassigned items (items with blank folderName)
                        val unassigned = subjectContents.filter { it.folderName.isBlank() }
                        if (unassigned.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Direct Files", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64748B))
                            }
                            items(unassigned, key = { it.id }) { item ->
                                StudyContentCard(item = item, subject = subject, onPdfClick = onPdfClick, onVideoClick = onVideoClick)
                            }
                        }
                    }
                }
                // LEVEL 2: SUBFOLDERS LIST
                else if (selectedSubfolder == null) {
                    val folderContents = subjectContents.filter { it.folderName.equals(selectedFolder, ignoreCase = true) }
                    val distinctSubfolders = folderContents
                        .map { it.subfolderName }
                        .filter { it.isNotBlank() }
                        .distinct()

                    if (distinctSubfolders.isEmpty()) {
                        // If no subfolders exist, check if chapters exist directly
                        val distinctChapters = folderContents
                            .map { it.chapterName }
                            .filter { it.isNotBlank() }
                            .distinct()

                        if (distinctChapters.isEmpty()) {
                            // Direct files inside this folder
                            if (folderContents.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Folder is empty", color = Color(0xFF64748B), fontSize = 13.sp)
                                    }
                                }
                            } else {
                                items(folderContents, key = { it.id }) { item ->
                                    StudyContentCard(item = item, subject = subject, onPdfClick = onPdfClick, onVideoClick = onVideoClick)
                                }
                            }
                        } else {
                            // Show chapters
                            items(distinctChapters) { chapterName ->
                                val count = folderContents.count { it.chapterName.equals(chapterName, ignoreCase = true) }
                                Card(
                                    onClick = { selectedChapter = chapterName },
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
                                            color = Color(0xFFF0FDF4),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("📑", fontSize = 22.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = chapterName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "$count file(s)",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF16A34A))
                                    }
                                }
                            }
                            // Also any items in this folder without chapterName
                            val noChapterItems = folderContents.filter { it.chapterName.isBlank() }
                            if (noChapterItems.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Other Files", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64748B))
                                }
                                items(noChapterItems, key = { it.id }) { item ->
                                    StudyContentCard(item = item, subject = subject, onPdfClick = onPdfClick, onVideoClick = onVideoClick)
                                }
                            }
                        }
                    } else {
                        // Display subfolders
                        items(distinctSubfolders) { subfolder ->
                            val count = folderContents.count { it.subfolderName.equals(subfolder, ignoreCase = true) }
                            Card(
                                onClick = { selectedSubfolder = subfolder },
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
                                        color = Color(0xFFF0FDF4),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("📂", fontSize = 22.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = subfolder,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "$count file(s)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF16A34A))
                                }
                            }
                        }
                        // Any files with blank subfolderName in this folder
                        val noSubfolderItems = folderContents.filter { it.subfolderName.isBlank() }
                        if (noSubfolderItems.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Direct Files", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64748B))
                            }
                            items(noSubfolderItems, key = { it.id }) { item ->
                                StudyContentCard(item = item, subject = subject, onPdfClick = onPdfClick, onVideoClick = onVideoClick)
                            }
                        }
                    }
                }
                // LEVEL 3: CHAPTERS LIST
                else if (selectedChapter == null) {
                    val subfolderContents = subjectContents.filter {
                        it.folderName.equals(selectedFolder, ignoreCase = true) &&
                        it.subfolderName.equals(selectedSubfolder, ignoreCase = true)
                    }
                    val distinctChapters = subfolderContents
                        .map { it.chapterName }
                        .filter { it.isNotBlank() }
                        .distinct()

                    if (distinctChapters.isEmpty()) {
                        // Direct files in this subfolder
                        if (subfolderContents.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Subfolder is empty", color = Color(0xFF64748B), fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(subfolderContents, key = { it.id }) { item ->
                                StudyContentCard(item = item, subject = subject, onPdfClick = onPdfClick, onVideoClick = onVideoClick)
                            }
                        }
                    } else {
                        items(distinctChapters) { chapterName ->
                            val count = subfolderContents.count { it.chapterName.equals(chapterName, ignoreCase = true) }
                            Card(
                                onClick = { selectedChapter = chapterName },
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
                                        color = Color(0xFFEFF6FF),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("📑", fontSize = 22.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = chapterName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "$count file(s)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF2563EB))
                                }
                            }
                        }
                        val noChapterItems = subfolderContents.filter { it.chapterName.isBlank() }
                        if (noChapterItems.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Other Files", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64748B))
                            }
                            items(noChapterItems, key = { it.id }) { item ->
                                StudyContentCard(item = item, subject = subject, onPdfClick = onPdfClick, onVideoClick = onVideoClick)
                            }
                        }
                    }
                }
                // LEVEL 4: ITEMS IN CHAPTER
                else {
                    val chapterContents = subjectContents.filter {
                        it.folderName.equals(selectedFolder, ignoreCase = true) &&
                        (selectedSubfolder == null || it.subfolderName.isBlank() || it.subfolderName.equals(selectedSubfolder, ignoreCase = true)) &&
                        it.chapterName.equals(selectedChapter, ignoreCase = true)
                    }

                    if (chapterContents.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No items inside this chapter.", color = Color(0xFF64748B), fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(chapterContents, key = { it.id }) { item ->
                            StudyContentCard(item = item, subject = subject, onPdfClick = onPdfClick, onVideoClick = onVideoClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudyContentCard(
    item: StudyContentItem,
    subject: Subject,
    onPdfClick: (title: String, uri: String) -> Unit,
    onVideoClick: (title: String, url: String, ch: String, sub: String, fId: String) -> Unit
) {
    val isPdf = item.type == StudyContentType.PDF
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isPdf) {
                    onPdfClick(item.title, item.mediaUrl)
                } else {
                    onVideoClick(item.title, item.mediaUrl, item.chapterName, subject.name, item.folderId)
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isPdf) Color(0xFFDCFCE7) else Color(0xFFEFF6FF),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPdf) Icons.Default.Description else Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = if (isPdf) Color(0xFF15803D) else Color(0xFF2563EB),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isPdf) Color(0xFFF0FDF4) else Color(0xFFEFF6FF)
            ) {
                Text(
                    text = if (isPdf) "Read" else "Watch",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPdf) Color(0xFF15803D) else Color(0xFF2563EB),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
