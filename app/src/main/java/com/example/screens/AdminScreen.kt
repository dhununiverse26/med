package com.example.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.util.extractYouTubeVideoId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import com.example.data.MedRepository
import com.example.model.*
import com.example.data.cloud.CloudflareR2Manager


import com.example.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

private val adminUploadScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

const val ADMIN_SECURITY_PASSWORD = "526255"

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
    return name ?: "notes_${System.currentTimeMillis()}.pdf"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLockView(
    onUnlock: () -> Unit,
    onBackClick: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun verifyPin(input: String) {
        if (input == ADMIN_SECURITY_PASSWORD) {
            isError = false
            errorMessage = ""
            onUnlock()
        } else {
            isError = true
            errorMessage = "Incorrect Password! Access Denied."
            pin = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        text = "Admin Security Lock",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Security Shield Icon
            Surface(
                shape = CircleShape,
                color = Color(0xFFDBEAFE),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF93C5FD)),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Admin Access Protected",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Please enter 6-digit security password to unlock Admin controls & uploads.",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PIN Indicator Boxes (6 Digits)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val isFilled = i < pin.length
                    val isCurrent = i == pin.length
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isError -> Color(0xFFFEE2E2)
                            isFilled -> Color(0xFF2563EB)
                            isCurrent -> Color(0xFFEFF6FF)
                            else -> Color(0xFFF1F5F9)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isCurrent) 2.dp else 1.dp,
                            color = when {
                                isError -> Color(0xFFEF4444)
                                isFilled -> Color(0xFF2563EB)
                                isCurrent -> Color(0xFF3B82F6)
                                else -> Color(0xFFCBD5E1)
                            }
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isFilled) {
                                if (showPassword) {
                                    Text(
                                        text = pin[i].toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }

            if (isError) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️ ", fontSize = 13.sp)
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Direct keyboard textfield
            OutlinedTextField(
                value = pin,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }.take(6)
                    pin = filtered
                    isError = false
                    errorMessage = ""
                    if (filtered.length == 6) {
                        verifyPin(filtered)
                    }
                },
                label = { Text("Enter 6-Digit Password") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        verifyPin(pin)
                    }
                ),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle visibility",
                            tint = Color(0xFF64748B)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tactile Number Pad
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val keyRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("Clear", "0", "⌫")
                    )

                    keyRows.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { key ->
                                Surface(
                                    onClick = {
                                        when (key) {
                                            "Clear" -> {
                                                pin = ""
                                                isError = false
                                                errorMessage = ""
                                            }
                                            "⌫" -> {
                                                if (pin.isNotEmpty()) {
                                                    pin = pin.dropLast(1)
                                                    isError = false
                                                    errorMessage = ""
                                                }
                                            }
                                            else -> {
                                                if (pin.length < 6) {
                                                    val nextPin = pin + key
                                                    pin = nextPin
                                                    isError = false
                                                    errorMessage = ""
                                                    if (nextPin.length == 6) {
                                                        verifyPin(nextPin)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = when (key) {
                                        "Clear", "⌫" -> Color(0xFFF1F5F9)
                                        else -> Color(0xFFF8FAFC)
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            fontSize = if (key.length > 1) 13.sp else 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (key) {
                                                "Clear" -> Color(0xFFDC2626)
                                                "⌫" -> Color(0xFF1E293B)
                                                else -> Color(0xFF0F172A)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { verifyPin(pin) },
                enabled = pin.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock Admin Panel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel & Go Back", color = Color(0xFF64748B), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

enum class AdminScreenSection {
    MAIN_MENU,
    NOTES_MENU,
    NOTES_UPLOAD,
    NOTES_MANAGE,
    MCQ_MENU,
    MCQ_UPLOAD,
    MCQ_MANAGE,
    FOLDER_CONTENT_MENU,
    FOLDER_CONTENT_MANAGE,
    FOLDER_CONTENT_UPLOAD,
    SHORTS_MENU,
    SHORTS_MANAGE,
    SHORTS_UPLOAD
}

@Composable
fun AdminMainCategoryCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = iconBg,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = iconBg
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconTint,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun AdminSubActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBg,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBackClick: () -> Unit,
    navigateTo: (Screen) -> Unit
) {
    val isUnlocked by MedRepository.isAdminUnlocked.collectAsState()

    if (!isUnlocked) {
        AdminLockView(
            onUnlock = { MedRepository.unlockAdmin() },
            onBackClick = onBackClick
        )
        return
    }

    // Main Admin Section Navigation State
    var currentAdminScreen by remember { mutableStateOf(AdminScreenSection.MAIN_MENU) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    fun handleAdminBack() {
        when (currentAdminScreen) {
            AdminScreenSection.NOTES_UPLOAD, AdminScreenSection.NOTES_MANAGE -> currentAdminScreen = AdminScreenSection.NOTES_MENU
            AdminScreenSection.NOTES_MENU -> currentAdminScreen = AdminScreenSection.MAIN_MENU
            AdminScreenSection.MCQ_UPLOAD, AdminScreenSection.MCQ_MANAGE -> currentAdminScreen = AdminScreenSection.MCQ_MENU
            AdminScreenSection.MCQ_MENU -> currentAdminScreen = AdminScreenSection.MAIN_MENU
            AdminScreenSection.FOLDER_CONTENT_UPLOAD, AdminScreenSection.FOLDER_CONTENT_MANAGE -> currentAdminScreen = AdminScreenSection.FOLDER_CONTENT_MENU
            AdminScreenSection.FOLDER_CONTENT_MENU -> currentAdminScreen = AdminScreenSection.MAIN_MENU
            AdminScreenSection.SHORTS_UPLOAD, AdminScreenSection.SHORTS_MANAGE -> currentAdminScreen = AdminScreenSection.SHORTS_MENU
            AdminScreenSection.SHORTS_MENU -> currentAdminScreen = AdminScreenSection.MAIN_MENU
            AdminScreenSection.MAIN_MENU -> onBackClick()
        }
    }

    // Intercept hardware and gesture back press
    BackHandler(enabled = isUnlocked) {
        handleAdminBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showR2ConfigDialog by remember { mutableStateOf(false) }
    var showAppInfoConfigDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete All Content & Cloud Bucket?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ALL content, folders, and shorts? This will permanently wipe local data and empty the Cloudflare R2 storage catalog for all users.") },
            confirmButton = {
                TextButton(onClick = {
                    MedRepository.masterDeleteAll()
                    showDeleteConfirmation = false
                }) {
                    Text("Delete All Fresh", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val topBarTitle = when (currentAdminScreen) {
        AdminScreenSection.MAIN_MENU -> "Admin Panel"
        AdminScreenSection.NOTES_MENU -> "Notes Section"
        AdminScreenSection.NOTES_UPLOAD -> "Upload Notes (PDF)"
        AdminScreenSection.NOTES_MANAGE -> "Manage Notes"
        AdminScreenSection.MCQ_MENU -> "MCQ Section"
        AdminScreenSection.MCQ_UPLOAD -> "Upload MCQs"
        AdminScreenSection.MCQ_MANAGE -> "Manage MCQs"
        AdminScreenSection.FOLDER_CONTENT_MENU -> "Folder & Content"
        AdminScreenSection.FOLDER_CONTENT_UPLOAD -> "Upload to Folder"
        AdminScreenSection.FOLDER_CONTENT_MANAGE -> "Manage Folders"
        AdminScreenSection.SHORTS_MENU -> "Shorts Section"
        AdminScreenSection.SHORTS_UPLOAD -> "Upload Short"
        AdminScreenSection.SHORTS_MANAGE -> "Manage Shorts"
    }

    val showOuterTopBar = currentAdminScreen != AdminScreenSection.NOTES_MANAGE &&
            currentAdminScreen != AdminScreenSection.NOTES_UPLOAD

    Scaffold(
        topBar = {
            if (showOuterTopBar) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    TopAppBar(
                        modifier = Modifier.height(52.dp),
                        title = {
                            Text(
                                text = topBarTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { handleAdminBack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF1E3A8A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        actions = {
                            var showAdminMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showAdminMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Admin Menu", tint = Color(0xFF1E3A8A))
                                }
                                DropdownMenu(expanded = showAdminMenu, onDismissRequest = { showAdminMenu = false }) {
                                    DropdownMenuItem(text = { Text("Cloudflare R2 Storage") }, onClick = { showAdminMenu = false; showR2ConfigDialog = true }, leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF2563EB)) })
                                    DropdownMenuItem(text = { Text("Delete All Content") }, onClick = { showAdminMenu = false; showDeleteConfirmation = true }, leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) })
                                    DropdownMenuItem(text = { Text("Lock Admin") }, onClick = { showAdminMenu = false; MedRepository.lockAdmin() }, leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) })
                                    DropdownMenuItem(text = { Text("App Info & Notice URLs") }, onClick = { showAdminMenu = false; showAppInfoConfigDialog = true }, leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) })
                                }
                            }
                        },
                        windowInsets = WindowInsets(0.dp),
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFEFF6FF))
                    )
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFBFDBFE))
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        val innerPadding = if (showOuterTopBar) paddingValues else PaddingValues(0.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentAdminScreen) {
                // ==========================================
                // 1. MAIN VERTICAL SECTIONS
                // ==========================================
                AdminScreenSection.MAIN_MENU -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = "Admin Management",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Choose a section below to upload or manage content:",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // 1. NOTES
                        item {
                            AdminMainCategoryCard(
                                title = "NOTES",
                                subtitle = "Upload & Manage MBBS Notes, PDFs & Study Material",
                                badgeText = "PDF Notes",
                                icon = Icons.Default.Description,
                                iconTint = Color(0xFF2563EB),
                                iconBg = Color(0xFFDBEAFE),
                                onClick = { currentAdminScreen = AdminScreenSection.NOTES_MENU }
                            )
                        }

                        // 2. MCQ
                        item {
                            AdminMainCategoryCard(
                                title = "MCQ",
                                subtitle = "Upload MCQs & Manage Question Bank Sets",
                                badgeText = "Question Bank",
                                icon = Icons.Default.Quiz,
                                iconTint = Color(0xFF7C3AED),
                                iconBg = Color(0xFFF3E8FF),
                                onClick = { currentAdminScreen = AdminScreenSection.MCQ_MENU }
                            )
                        }

                        // 3. FOLDER AND CONTENT
                        item {
                            AdminMainCategoryCard(
                                title = "FOLDER AND CONTENT",
                                subtitle = "Hierarchical management: Profs, Subjects, Folders & Lectures",
                                badgeText = "Folder Wise",
                                icon = Icons.Default.FolderSpecial,
                                iconTint = Color(0xFF059669),
                                iconBg = Color(0xFFD1FAE5),
                                onClick = { currentAdminScreen = AdminScreenSection.FOLDER_CONTENT_MENU }
                            )
                        }

                        // 4. SHORTS
                        item {
                            AdminMainCategoryCard(
                                title = "SHORTS",
                                subtitle = "Upload YouTube clinical shorts & manage video reels",
                                badgeText = "Video Shorts",
                                icon = Icons.Default.VideoLibrary,
                                iconTint = Color(0xFFDC2626),
                                iconBg = Color(0xFFFEE2E2),
                                onClick = { currentAdminScreen = AdminScreenSection.SHORTS_MENU }
                            )
                        }
                    }
                }

                // ==========================================
                // 2. NOTES SECTION (SUB-INTERFACE)
                // ==========================================
                AdminScreenSection.NOTES_MENU -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Notes Section",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Choose an action for MBBS Notes:",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Manage Notes
                        AdminSubActionCard(
                            title = "Manage Notes & Folders",
                            subtitle = "Browse, edit, rename, delete or hide notes folder-wise",
                            icon = Icons.Default.FolderOpen,
                            iconTint = Color(0xFF0D9488),
                            iconBg = Color(0xFFCCFBF1),
                            onClick = { currentAdminScreen = AdminScreenSection.NOTES_MANAGE }
                        )

                        // Upload Notes
                        AdminSubActionCard(
                            title = "Upload Notes (PDF)",
                            subtitle = "Upload new notes: Folder -> Subfolder -> Chapter -> PDF",
                            icon = Icons.Default.UploadFile,
                            iconTint = Color(0xFF2563EB),
                            iconBg = Color(0xFFDBEAFE),
                            onClick = { currentAdminScreen = AdminScreenSection.NOTES_UPLOAD }
                        )
                    }
                }
                AdminScreenSection.NOTES_MANAGE -> {
                    AdminNotesManageScreen(
                        onBackClick = { currentAdminScreen = AdminScreenSection.NOTES_MENU },
                        onNavigateToUpload = { currentAdminScreen = AdminScreenSection.NOTES_UPLOAD }
                    )
                }
                AdminScreenSection.NOTES_UPLOAD -> {
                    AdminNotesUploadScreen(onBackClick = { currentAdminScreen = AdminScreenSection.NOTES_MENU })
                }

                // ==========================================
                // 3. MCQ SECTION (SUB-INTERFACE)
                // ==========================================
                AdminScreenSection.MCQ_MENU -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "MCQ Section",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Choose an action for MCQs:",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Manage MCQs
                        AdminSubActionCard(
                            title = "Manage MCQs",
                            subtitle = "View, edit, search, and delete questions by subject",
                            icon = Icons.Default.ListAlt,
                            iconTint = Color(0xFF4F46E5),
                            iconBg = Color(0xFFEEF2FF),
                            onClick = { currentAdminScreen = AdminScreenSection.MCQ_MANAGE }
                        )

                        // Upload MCQs
                        AdminSubActionCard(
                            title = "Upload New MCQs",
                            subtitle = "Add single MCQ or bulk import question sets",
                            icon = Icons.Default.AddCircleOutline,
                            iconTint = Color(0xFF7C3AED),
                            iconBg = Color(0xFFF3E8FF),
                            onClick = { currentAdminScreen = AdminScreenSection.MCQ_UPLOAD }
                        )
                    }
                }
                AdminScreenSection.MCQ_MANAGE -> {
                    AdminMcqManageScreen(onBackClick = { currentAdminScreen = AdminScreenSection.MCQ_MENU })
                }
                AdminScreenSection.MCQ_UPLOAD -> {
                    AdminMcqUploadScreen(onBackClick = { currentAdminScreen = AdminScreenSection.MCQ_MENU })
                }

                // ==========================================
                // 4. FOLDER & CONTENT SECTION (SUB-INTERFACE)
                // ==========================================
                AdminScreenSection.FOLDER_CONTENT_MENU -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Folder & Content Section",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Hierarchical management for all MBBS subjects & lectures:",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Manage Folders & Content
                        AdminSubActionCard(
                            title = "Manage Folders & Content",
                            subtitle = "Browse folders, subfolders, chapters, rename, delete & organize",
                            icon = Icons.Default.FolderSpecial,
                            iconTint = Color(0xFF059669),
                            iconBg = Color(0xFFD1FAE5),
                            onClick = { currentAdminScreen = AdminScreenSection.FOLDER_CONTENT_MANAGE }
                        )

                        // Upload to Folder
                        AdminSubActionCard(
                            title = "Upload to Folder",
                            subtitle = "Upload new PDFs or video lecture links into any folder hierarchy",
                            icon = Icons.Default.UploadFile,
                            iconTint = Color(0xFF2563EB),
                            iconBg = Color(0xFFDBEAFE),
                            onClick = { currentAdminScreen = AdminScreenSection.FOLDER_CONTENT_UPLOAD }
                        )
                    }
                }
                AdminScreenSection.FOLDER_CONTENT_MANAGE -> {
                    ManagePdfsAndVideosView()
                }
                AdminScreenSection.FOLDER_CONTENT_UPLOAD -> {
                    HierarchyUploadView(onOpenR2Settings = { showR2ConfigDialog = true })
                }

                // ==========================================
                // 5. SHORTS SECTION (SUB-INTERFACE)
                // ==========================================
                AdminScreenSection.SHORTS_MENU -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Shorts Section",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Manage clinical and revision video shorts:",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Manage Shorts
                        AdminSubActionCard(
                            title = "Manage Shorts",
                            subtitle = "View, edit titles, delete or reorder uploaded shorts",
                            icon = Icons.Default.VideoLibrary,
                            iconTint = Color(0xFFDC2626),
                            iconBg = Color(0xFFFEE2E2),
                            onClick = { currentAdminScreen = AdminScreenSection.SHORTS_MANAGE }
                        )

                        // Upload Short
                        AdminSubActionCard(
                            title = "Upload New Short",
                            subtitle = "Add YouTube Shorts link with title and mnemonic tip",
                            icon = Icons.Default.Add,
                            iconTint = Color(0xFFEA580C),
                            iconBg = Color(0xFFFFEDD5),
                            onClick = { currentAdminScreen = AdminScreenSection.SHORTS_UPLOAD }
                        )
                    }
                }
                AdminScreenSection.SHORTS_MANAGE -> {
                    ManageShortsView()
                }
                AdminScreenSection.SHORTS_UPLOAD -> {
                    UploadShortsView()
                }
            }
        }
    }

    if (showAppInfoConfigDialog) {
        AppInfoSettingsDialog(onDismiss = { showAppInfoConfigDialog = false })
    }

    val currentR2AccountId by CloudflareR2Manager.accountId.collectAsState()
    val currentR2AccessKey by CloudflareR2Manager.accessKeyId.collectAsState()
    val currentR2SecretKey by CloudflareR2Manager.secretAccessKey.collectAsState()
    val currentR2Bucket by CloudflareR2Manager.bucketName.collectAsState()
    val currentR2PublicBaseUrl by CloudflareR2Manager.publicBaseUrl.collectAsState()

    if (showR2ConfigDialog) {
        var r2AccountId by remember(currentR2AccountId) { mutableStateOf(currentR2AccountId) }
        var r2AccessKey by remember(currentR2AccessKey) { mutableStateOf(currentR2AccessKey) }
        var r2SecretKey by remember(currentR2SecretKey) { mutableStateOf(currentR2SecretKey) }
        var r2Bucket by remember(currentR2Bucket) { mutableStateOf(currentR2Bucket) }
        var r2PublicBaseUrl by remember(currentR2PublicBaseUrl) { mutableStateOf(currentR2PublicBaseUrl) }
        var testStatus by remember { mutableStateOf<String?>(null) }
        var isTesting by remember { mutableStateOf(false) }
        val dialogScope = rememberCoroutineScope()

        Dialog(onDismissRequest = { showR2ConfigDialog = false }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth().padding(10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("☁️", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cloudflare R2 Bucket Setup",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                        IconButton(onClick = { showR2ConfigDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))

                    // Test Status Banner
                    testStatus?.let { status ->
                        Surface(
                            color = if (status.startsWith("✓")) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = status,
                                color = if (status.startsWith("✓")) Color(0xFF15803D) else Color(0xFFDC2626),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = r2AccountId,
                        onValueChange = { r2AccountId = it },
                        label = { Text("Cloudflare Account ID *") },
                        textStyle = com.example.ui.theme.medInputTextStyle(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = r2AccessKey,
                        onValueChange = { r2AccessKey = it },
                        label = { Text("R2 Access Key ID *") },
                        placeholder = { Text("e.g. 8f4e9123abc...", color = Color(0xFF94A3B8)) },
                        textStyle = com.example.ui.theme.medInputTextStyle(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = r2SecretKey,
                        onValueChange = { r2SecretKey = it },
                        label = { Text("R2 Secret Access Key *") },
                        placeholder = { Text("e.g. 7c3d1045def...", color = Color(0xFF94A3B8)) },
                        textStyle = com.example.ui.theme.medInputTextStyle(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = r2Bucket,
                        onValueChange = { r2Bucket = it },
                        label = { Text("Bucket Name *") },
                        placeholder = { Text("e.g. medxmed", color = Color(0xFF94A3B8)) },
                        textStyle = com.example.ui.theme.medInputTextStyle(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = r2PublicBaseUrl,
                        onValueChange = { r2PublicBaseUrl = it },
                        label = { Text("Public CDN / r2.dev Base URL") },
                        placeholder = { Text("https://pub-...r2.dev", color = Color(0xFF94A3B8)) },
                        textStyle = com.example.ui.theme.medInputTextStyle(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("S3 API Endpoint:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Text(
                                text = "https://${r2AccountId.trim().ifBlank { CloudflareR2Manager.DEFAULT_ACCOUNT_ID }}.r2.cloudflarestorage.com/${r2Bucket.trim().ifBlank { CloudflareR2Manager.DEFAULT_BUCKET_NAME }}",
                                fontSize = 10.sp,
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Test Connection Button
                    OutlinedButton(
                        onClick = {
                            isTesting = true
                            testStatus = "Testing connection to Cloudflare R2 bucket '${r2Bucket.trim()}'..."
                            dialogScope.launch {
                                CloudflareR2Manager.saveCredentials(r2AccountId, r2AccessKey, r2SecretKey, r2Bucket, r2PublicBaseUrl)
                                val res = CloudflareR2Manager.testConnection()
                                isTesting = false
                                testStatus = if (res.isSuccess) {
                                    "✓ ${res.getOrThrow()}"
                                } else {
                                    "⚠️ ${res.exceptionOrNull()?.localizedMessage ?: "Connection failed"}"
                                }
                            }
                        },
                        enabled = !isTesting,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testing Connection...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF2563EB))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⚡ Test Cloudflare Connection", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        }
                    }


                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showR2ConfigDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                CloudflareR2Manager.saveCredentials(r2AccountId, r2AccessKey, r2SecretKey, r2Bucket, r2PublicBaseUrl)
                                showR2ConfigDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Save & Connect", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }


}

@Composable
fun HierarchyUploadView(onOpenR2Settings: () -> Unit = {}) {
    val folders by MedRepository.folders.collectAsState()
    val allContents by MedRepository.contents.collectAsState()

    // 1. Content / Section Type (PDF Notes vs Video Lectures)
    var contentType by remember { mutableStateOf(StudyContentType.PDF) } // Default to PDF Notes
    val targetSectionType = if (contentType == StudyContentType.PDF) "PDF" else "VIDEO"

    // 2. Prof
    var selectedProf by remember { mutableStateOf(ProfYear.FIRST_PROF) }
    val subjects = remember(selectedProf) { MedRepository.getSubjectsForProf(selectedProf) }

    // 3. Subject
    var selectedSubject by remember(selectedProf) {
        mutableStateOf(subjects.firstOrNull() ?: MedRepository.allSubjects.first())
    }

    // Filtered folders strictly for selected prof + subject + sectionType (PDF vs Video)
    val availableFolders = remember(folders, selectedProf, selectedSubject, targetSectionType) {
        folders.filter {
            it.prof == selectedProf && it.subjectId == selectedSubject.id &&
            (it.sectionType == targetSectionType || it.sectionType == "ALL")
        }
    }

    // 4. Folder mode: Existing vs Create New
    var isCreatingNewFolder by remember { mutableStateOf(availableFolders.isEmpty()) }
    var selectedFolderId by remember(availableFolders) { mutableStateOf(availableFolders.firstOrNull()?.id ?: "") }
    var newFolderName by remember { mutableStateOf("") }
    var selectedFolderEmoji by remember { mutableStateOf("📁") }
    val emojiChoices = listOf("📁", "📚", "🔬", "🫀", "🧠", "🦴", "🧪", "💊", "👁️", "👶")

    val currentFolderLabel = if (isCreatingNewFolder) newFolderName.trim() else (availableFolders.firstOrNull { it.id == selectedFolderId }?.name ?: "")

    // 5. Subfolder (Optional - Inside Folder)
    val existingSubfoldersInFolder = remember(allContents, currentFolderLabel, selectedProf, selectedSubject) {
        if (currentFolderLabel.isBlank()) emptyList()
        else allContents.filter {
            it.prof == selectedProf && it.subjectId == selectedSubject.id &&
            it.folderName.equals(currentFolderLabel, ignoreCase = true) &&
            it.subfolderName.isNotBlank()
        }.map { it.subfolderName.trim() }.distinct()
    }

    var subfolderMode by remember(currentFolderLabel) { mutableIntStateOf(0) } // 0 = None, 1 = Existing, 2 = New
    var selectedExistingSubfolder by remember(existingSubfoldersInFolder) {
        mutableStateOf(existingSubfoldersInFolder.firstOrNull() ?: "")
    }
    var newSubfolderName by remember { mutableStateOf("") }

    val currentSubfolderLabel = when (subfolderMode) {
        1 -> selectedExistingSubfolder.trim()
        2 -> newSubfolderName.trim()
        else -> ""
    }

    // 6. Chapter (Optional - Inside Subfolder)
    val existingChaptersInFolder = remember(allContents, currentFolderLabel, currentSubfolderLabel, selectedProf, selectedSubject) {
        if (currentFolderLabel.isBlank()) emptyList()
        else allContents.filter {
            it.prof == selectedProf && it.subjectId == selectedSubject.id &&
            it.folderName.equals(currentFolderLabel, ignoreCase = true) &&
            (currentSubfolderLabel.isBlank() || it.subfolderName.equals(currentSubfolderLabel, ignoreCase = true)) &&
            it.chapterName.isNotBlank()
        }.map { it.chapterName.trim() }.distinct()
    }

    var chapterMode by remember(currentFolderLabel, currentSubfolderLabel) {
        mutableIntStateOf(0) // Default: None
    }
    var selectedExistingChapter by remember(existingChaptersInFolder) {
        mutableStateOf(existingChaptersInFolder.firstOrNull() ?: "")
    }
    var newChapterName by remember { mutableStateOf("") }

    // 6. Content Details
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var customUrlOrFile by remember { mutableStateOf("") }

    val context = LocalContext.current
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfFileName by remember { mutableStateOf<String?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPdfUri = uri
            val fname = getFileNameFromUri(context, uri)
            selectedPdfFileName = fname
            if (title.isBlank()) {
                title = fname.substringBeforeLast(".").replace("_", " ").replace("-", " ")
            }
        }
    }

    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Update isCreatingNewFolder when availableFolders changes
    LaunchedEffect(availableFolders) {
        if (availableFolders.isEmpty()) {
            isCreatingNewFolder = true
        } else if (selectedFolderId.isBlank() || availableFolders.none { it.id == selectedFolderId }) {
            selectedFolderId = availableFolders.first().id
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        statusMessage?.let { msg ->
            item {
                val isUploading = msg.contains("Uploading")
                val isError = msg.startsWith("⚠️") || msg.startsWith("❌")
                Surface(
                    color = when {
                        isUploading -> Color(0xFFEFF6FF)
                        isError -> Color(0xFFFEE2E2)
                        else -> Color(0xFFDCFCE7)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when {
                            isUploading -> Color(0xFFBFDBFE)
                            isError -> Color(0xFFFCA5A5)
                            else -> Color(0xFF86EFAC)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF2563EB)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = msg,
                            color = when {
                                isUploading -> Color(0xFF1D4ED8)
                                isError -> Color(0xFFDC2626)
                                else -> Color(0xFF15803D)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // STEP 1: SECTION / CONTENT TYPE (PDF Notes vs Video Lectures)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Target Section", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { contentType = StudyContentType.PDF },
                            shape = RoundedCornerShape(10.dp),
                            color = if (contentType == StudyContentType.PDF) Color(0xFF059669) else Color(0xFFF1F5F9),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📝 PDF Notes Section", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (contentType == StudyContentType.PDF) Color.White else Color(0xFF334155))
                            }
                        }

                        Surface(
                            onClick = { contentType = StudyContentType.VIDEO },
                            shape = RoundedCornerShape(10.dp),
                            color = if (contentType == StudyContentType.VIDEO) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎥 Videos Section", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (contentType == StudyContentType.VIDEO) Color.White else Color(0xFF334155))
                            }
                        }
                    }
                }
            }
        }

        // STEP 2: PROF
        item {
            Text("Select Prof", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProfYear.values().forEach { prof ->
                    val isSelected = selectedProf == prof
                    Surface(
                        onClick = {
                            selectedProf = prof
                            selectedSubject = MedRepository.getSubjectsForProf(prof).firstOrNull() ?: MedRepository.allSubjects.first()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF1E293B) else Color.White,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = prof.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // STEP 3: SUBJECT
        item {
            Text("Select Subject", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(subjects) { sub ->
                    val isSelected = selectedSubject.id == sub.id
                    Surface(
                        onClick = { selectedSubject = sub },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF2563EB) else Color.White,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(sub.emoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                sub.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        }

        // STEP 4: FOLDER (Existing vs Create New - Strictly for this Section)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Main Folder", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))

                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = !isCreatingNewFolder,
                                onClick = { isCreatingNewFolder = false },
                                label = { Text("Existing", fontSize = 11.sp) },
                                enabled = availableFolders.isNotEmpty()
                            )
                            FilterChip(
                                selected = isCreatingNewFolder,
                                onClick = { isCreatingNewFolder = true },
                                label = { Text("+ Create New", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!isCreatingNewFolder && availableFolders.isNotEmpty()) {
                        // Select from existing section folders
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableFolders) { folder ->
                                val isSelected = selectedFolderId == folder.id
                                Surface(
                                    onClick = { selectedFolderId = folder.id },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2563EB)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text(folder.emoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            folder.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Create New Folder
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            label = { Text("Folder Name") },
                            placeholder = { Text("Enter folder title", color = Color(0xFF94A3B8)) },
                            textStyle = com.example.ui.theme.medInputTextStyle(),
                            colors = com.example.ui.theme.medTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // STEP 5: SUBFOLDER (Inside Folder)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Subfolder (Optional)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = subfolderMode == 0,
                            onClick = { subfolderMode = 0 },
                            label = { Text("None (Direct)", fontSize = 11.sp) }
                        )
                        if (existingSubfoldersInFolder.isNotEmpty()) {
                            FilterChip(
                                selected = subfolderMode == 1,
                                onClick = { subfolderMode = 1 },
                                label = { Text("Existing (${existingSubfoldersInFolder.size})", fontSize = 11.sp) }
                            )
                        }
                        FilterChip(
                            selected = subfolderMode == 2,
                            onClick = { subfolderMode = 2 },
                            label = { Text("+ New Subfolder", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (subfolderMode) {
                        1 -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(existingSubfoldersInFolder) { sub ->
                                    val isSelected = selectedExistingSubfolder.equals(sub, ignoreCase = true)
                                    Surface(
                                        onClick = { selectedExistingSubfolder = sub },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF2563EB)) else BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Text(
                                            text = "📂 $sub",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF334155),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            OutlinedTextField(
                                value = newSubfolderName,
                                onValueChange = { newSubfolderName = it },
                                label = { Text("New Subfolder Name") },
                                placeholder = { Text("e.g. Bones & Joints, Cranial Nerves", color = Color(0xFF94A3B8)) },
                                textStyle = com.example.ui.theme.medInputTextStyle(),
                                colors = com.example.ui.theme.medTextFieldColors(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }

        // STEP 6: CHAPTER (Inside Subfolder)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Chapter / Topic (Optional)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = chapterMode == 0,
                            onClick = { chapterMode = 0 },
                            label = { Text("None (Direct)", fontSize = 11.sp) }
                        )
                        if (existingChaptersInFolder.isNotEmpty()) {
                            FilterChip(
                                selected = chapterMode == 1,
                                onClick = { chapterMode = 1 },
                                label = { Text("Existing (${existingChaptersInFolder.size})", fontSize = 11.sp) }
                            )
                        }
                        FilterChip(
                            selected = chapterMode == 2,
                            onClick = { chapterMode = 2 },
                            label = { Text("+ New Chapter", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (chapterMode) {
                        1 -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(existingChaptersInFolder) { ch ->
                                    val isSelected = selectedExistingChapter.equals(ch, ignoreCase = true)
                                    Surface(
                                        onClick = { selectedExistingChapter = ch },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF2563EB)) else BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Text(
                                            text = "📑 $ch",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF334155),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            OutlinedTextField(
                                value = newChapterName,
                                onValueChange = { newChapterName = it },
                                label = { Text("New Chapter / Topic Name") },
                                placeholder = { Text("e.g. Chapter 1: Brachial Plexus, Cardiac Cycle", color = Color(0xFF94A3B8)) },
                                textStyle = com.example.ui.theme.medInputTextStyle(),
                                colors = com.example.ui.theme.medTextFieldColors(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }

        // STEP 6: CONTENT DETAILS & FILE PICKER
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("6. File & Details (Cloudflare R2):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(10.dp))

                    if (contentType == StudyContentType.VIDEO) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Video Title *") },
                            placeholder = { Text("e.g. Brachial Plexus Masterclass", color = Color(0xFF94A3B8)) },
                            textStyle = com.example.ui.theme.medInputTextStyle(),
                            colors = com.example.ui.theme.medTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (Optional)") },
                            textStyle = com.example.ui.theme.medInputTextStyle(),
                            colors = com.example.ui.theme.medTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            minLines = 2
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customUrlOrFile,
                            onValueChange = { customUrlOrFile = it },
                            label = { Text("YouTube URL / Video File Link *") },
                            placeholder = { Text("e.g. https://youtu.be/... or lecture.mp4", color = Color(0xFF94A3B8)) },
                            textStyle = com.example.ui.theme.medInputTextStyle(),
                            colors = com.example.ui.theme.medTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        if (customUrlOrFile.isNotBlank()) {
                            val ytId = extractYouTubeVideoId(customUrlOrFile)
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (ytId != null) "🎬 Detected YouTube Video (Plays in App):" else "🎥 Video Stream URL (Plays in App):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF2563EB)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (ytId != null) {
                                        coil.compose.AsyncImage(
                                            model = "https://img.youtube.com/vi/$ytId/hqdefault.jpg",
                                            contentDescription = "Video preview",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 9f)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // PDF Notes - Direct file upload from device / gallery
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (selectedPdfUri != null) {
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Text("📄", fontSize = 28.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = selectedPdfFileName ?: "Document.pdf",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF15803D),
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "✓ PDF Selected from Device Storage",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF16A34A)
                                                )
                                            }
                                        }

                                        IconButton(onClick = {
                                            selectedPdfUri = null
                                            selectedPdfFileName = null
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFF15803D))
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Choose Different PDF from Device", fontSize = 13.sp)
                                }
                            } else {
                                Button(
                                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("📁 Select & Upload PDF from Device", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("PDF Title / Subject Topic") },
                                placeholder = { Text(selectedPdfFileName?.substringBeforeLast(".") ?: "e.g. Anatomy High Yield Notes", color = Color(0xFF94A3B8)) },
                                textStyle = com.example.ui.theme.medInputTextStyle(),
                                colors = com.example.ui.theme.medTextFieldColors(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Description / Notes Summary (Optional)") },
                                textStyle = com.example.ui.theme.medInputTextStyle(),
                                colors = com.example.ui.theme.medTextFieldColors(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                minLines = 2
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cloudflare R2 Storage Preview Path
                    val folderLabel = if (isCreatingNewFolder) newFolderName.ifBlank { "Folder" } else (availableFolders.firstOrNull { it.id == selectedFolderId }?.name ?: "Folder")
                    val fileNamePreview = if (contentType == StudyContentType.PDF) (selectedPdfFileName ?: (title.ifBlank { "document" } + ".pdf")) else (customUrlOrFile.ifBlank { title.ifBlank { "video" } + ".mp4" })
                    val cloudflarePreview = MedRepository.buildCloudflareUrl(selectedProf, selectedSubject.id, folderLabel, fileNamePreview)

                    Text(
                        text = "Cloudflare R2 Key: $cloudflarePreview",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        maxLines = 2
                    )
                }
            }
        }

        // ACTION BUTTONS: Upload & Save + Add Another
        item {
            val canSubmit = (!isCreatingNewFolder || newFolderName.isNotBlank()) &&
                ((contentType == StudyContentType.PDF && (selectedPdfUri != null || title.isNotBlank())) ||
                 (contentType == StudyContentType.VIDEO && title.isNotBlank()))

            val isR2Configured = false

            fun executeUpload(onSuccessReset: () -> Unit) {
                if (!canSubmit) {
                    statusMessage = if (contentType == StudyContentType.PDF && selectedPdfUri == null && title.isBlank()) {
                        "Please select a PDF file from your device!"
                    } else {
                        "Please provide Title and Folder Name!"
                    }
                    return
                }

                // Determine final folder ID and Name
                val finalFolderId: String
                val finalFolderName: String

                if (isCreatingNewFolder) {
                    val folderId = "f_${UUID.randomUUID().toString().take(6)}"
                    val folder = ContentFolder(
                        id = folderId,
                        prof = selectedProf,
                        subjectId = selectedSubject.id,
                        name = newFolderName.trim(),
                        emoji = listOf("📂", "📚", "🔬", "🧪", "🧬", "🩺", "🫀", "🧠", "🦴", "💊", "🧫", "📊", "👁️", "👂").random(),
                        sectionType = targetSectionType
                    )
                    MedRepository.createFolder(folder)
                    finalFolderId = folderId
                    finalFolderName = newFolderName.trim()
                    isCreatingNewFolder = false
                    selectedFolderId = folderId
                } else {
                    finalFolderId = selectedFolderId
                    finalFolderName = availableFolders.firstOrNull { it.id == selectedFolderId }?.name ?: "General"
                }

                // Determine final Subfolder and Chapter name
                val finalSubfolderName = when (subfolderMode) {
                    0 -> ""
                    1 -> selectedExistingSubfolder.trim()
                    2 -> newSubfolderName.trim()
                    else -> ""
                }

                val finalChapterName = when (chapterMode) {
                    0 -> ""
                    1 -> selectedExistingChapter.trim()
                    2 -> newChapterName.trim()
                    else -> ""
                }

                val contentId = "c_${UUID.randomUUID().toString().take(6)}"
                val fileName = if (contentType == StudyContentType.PDF) {
                    selectedPdfFileName ?: (title.trim() + ".pdf")
                } else {
                    customUrlOrFile.ifBlank { title.trim() + ".mp4" }
                }

                val finalTitle = title.ifBlank {
                    selectedPdfFileName?.substringBeforeLast(".")?.replace("_", " ") ?: "Study Content"
                }

                val cloudflareUrl = MedRepository.buildCloudflareUrl(selectedProf, selectedSubject.id, finalFolderName, fileName)

                // 1. Save locally FIRST so content is immediately secure in Room database
                val initialMediaUrl = if (customUrlOrFile.startsWith("http")) customUrlOrFile else cloudflareUrl
                val studyItem = StudyContentItem(
                    id = contentId,
                    prof = selectedProf,
                    subjectId = selectedSubject.id,
                    folderId = finalFolderId,
                    folderName = finalFolderName,
                    subfolderName = finalSubfolderName,
                    chapterName = finalChapterName,
                    type = contentType,
                    title = finalTitle.trim(),
                    description = description.trim(),
                    mediaUrl = initialMediaUrl,
                    cloudflareKey = cloudflareUrl
                )
                MedRepository.addStudyContent(studyItem)

                if (contentType == StudyContentType.PDF && selectedPdfUri != null) {
                    statusMessage = "Uploading PDF to Cloudflare R2... 0%"
                    val uploadUri = selectedPdfUri!!
                    adminUploadScope.launch {
                        try {
                            val cleanProf = selectedProf.code
                            val cleanSubject = selectedSubject.id.trim().replace(" ", "_")
                            val cleanFolder = finalFolderName.trim().replace(" ", "_")
                            val cleanFile = fileName.trim().replace(" ", "_")
                            val objectKey = "$cleanProf/$cleanSubject/$cleanFolder/$cleanFile"

                            val uploadRes = CloudflareR2Manager.uploadCompressedPdf(
                                context = context,
                                pdfUri = uploadUri,
                                objectKey = objectKey
                            ) { progress ->
                                statusMessage = "Uploading PDF to Cloudflare R2... ${(progress * 100).toInt()}%"
                            }

                            if (uploadRes.isSuccess) {
                                val publicR2Url = uploadRes.getOrThrow()
                                val updatedItem = studyItem.copy(
                                    mediaUrl = publicR2Url,
                                    cloudflareKey = publicR2Url
                                )
                                MedRepository.addStudyContent(updatedItem)
                                MedRepository.triggerCatalogPublish("contents")
                                statusMessage = "✓ Successfully uploaded to Cloudflare R2 bucket & saved!"
                                onSuccessReset()
                            } else {
                                val err = uploadRes.exceptionOrNull()?.localizedMessage ?: "Upload failed"
                                statusMessage = "⚠️ Saved locally, but Cloudflare upload error: $err"
                                onSuccessReset()
                            }
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            e.printStackTrace()
                            statusMessage = "⚠️ Saved locally in app, but upload error: ${e.localizedMessage}"
                        }
                    }
                } else {
                    MedRepository.triggerCatalogPublish("contents")
                    statusMessage = "✓ Successfully saved!"
                    onSuccessReset()
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        executeUpload {
                            title = ""
                            description = ""
                            customUrlOrFile = ""
                            selectedPdfUri = null
                            selectedPdfFileName = null
                            newSubfolderName = ""
                            newChapterName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (contentType == StudyContentType.PDF) Color(0xFF059669) else Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload & Save to Cloud", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Add Other Button for quick bulk upload in same section
                OutlinedButton(
                    onClick = {
                        executeUpload {
                            title = ""
                            description = ""
                            customUrlOrFile = ""
                            selectedPdfUri = null
                            selectedPdfFileName = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Add Other (Same Section)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ==========================================
// 🌟 ADD / MANAGE CUSTOM SECTIONS VIEW
// ==========================================
@Composable
fun AddCustomSectionView() {
    val customSections by MedRepository.customSections.collectAsState()
    val folders by MedRepository.folders.collectAsState()

    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🌟") }
    var selectedLayout by remember { mutableStateOf("GRID") }
    var selectedColor by remember { mutableStateOf("#2563EB") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var editingSection by remember { mutableStateOf<com.example.model.CustomSection?>(null) }
    var addingFolderToSection by remember { mutableStateOf<com.example.model.CustomSection?>(null) }
    var newFolderInsideSectionName by remember { mutableStateOf("") }
    var newFolderInsideSectionEmoji by remember { mutableStateOf("📁") }

    val emojiList = listOf("🌟", "🏥", "🩺", "⚡", "🔬", "🧪", "🗂️", "🔥", "🎯", "🧠", "📚", "💊", "🫀", "🧬")
    val colorList = listOf(
        "#2563EB" to "Royal Blue",
        "#059669" to "Emerald Green",
        "#7C3AED" to "Purple",
        "#D97706" to "Amber",
        "#DC2626" to "Crimson",
        "#0891B2" to "Cyan",
        "#4F46E5" to "Indigo"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        statusMessage?.let { msg ->
            item {
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // CREATE NEW SECTION CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌟", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Create New Big Section (Homepage)",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Creates a major section with full folder & subfolder hierarchy",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Section Title * (e.g. SPECIAL CLINICAL MODULES)") },
                        placeholder = { Text("e.g. OSCE & PRACTICAL ESSENTIALS", color = Color(0xFF94A3B8)) },
                        textStyle = com.example.ui.theme.medInputTextStyle(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Subtitle
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Subtitle / Description") },
                        placeholder = { Text("e.g. High-yield case discussions, algorithms & notes", color = Color(0xFF94A3B8)) },
                        textStyle = com.example.ui.theme.medInputTextStyle(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Select Emoji Icon
                    Text("Select Emoji / Icon:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(emojiList) { em ->
                            val isSelected = selectedEmoji == em
                            Surface(
                                onClick = { selectedEmoji = em },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2563EB)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(em, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Color theme
                    Text("Select Theme Color:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(colorList) { (hex, name) ->
                            val isSelected = selectedColor == hex
                            val col = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color(0xFF2563EB) }
                            Surface(
                                onClick = { selectedColor = hex },
                                shape = RoundedCornerShape(8.dp),
                                color = col,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(3.dp, Color.Black) else null,
                                modifier = Modifier.size(36.dp)
                            ) {
                                if (isSelected) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                statusMessage = "Please enter section title!"
                                return@Button
                            }

                            val newSec = com.example.model.CustomSection(
                                id = "sec_${UUID.randomUUID().toString().take(6)}",
                                title = title.trim(),
                                subtitle = subtitle.trim(),
                                emoji = selectedEmoji,
                                colorHex1 = selectedColor,
                                colorHex2 = selectedColor,
                                description = subtitle.trim(),
                                orderIndex = customSections.size
                            )
                            MedRepository.addCustomSection(newSec)
                            statusMessage = "✓ Section '${title.trim()}' created and synced with Cloudflare & Homepage!"
                            title = ""
                            subtitle = ""
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create & Publish Section", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // EXISTING SECTIONS LIST & HIERARCHY
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "All Custom Sections (${customSections.size})",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
            }
        }

        if (customSections.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No custom sections created yet. Create one above!", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(customSections) { sec ->
                val sectionFolders = folders.filter { it.subjectId == sec.id }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(sec.emoji, fontSize = 20.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        sec.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (sec.subtitle.isNotBlank()) {
                                        Text(sec.subtitle, fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 1)
                                    }
                                    Text(
                                        "📁 ${sectionFolders.size} Folders inside",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = { editingSection = sec }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { MedRepository.deleteCustomSection(sec.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Folders list inside section
                        if (sectionFolders.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(sectionFolders) { f ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(f.emoji, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(f.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Add folder inside this section button
                        OutlinedButton(
                            onClick = {
                                addingFolderToSection = sec
                                newFolderInsideSectionName = ""
                            },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Add Folder in ${sec.title}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }




    // Add Folder Inside Section Dialog
    addingFolderToSection?.let { sec ->
        Dialog(onDismissRequest = { addingFolderToSection = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Folder in ${sec.title}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newFolderInsideSectionName,
                        onValueChange = { newFolderInsideSectionName = it },
                        label = { Text("Folder Name (e.g. OSCE Stations, Case Studies)") },
                        textStyle = com.example.ui.theme.medInputTextStyle(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Select Folder Emoji:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("📁", "📂", "🗂️", "📚", "🩺", "🔬", "🧪", "⚡", "💊", "🫀", "🧠")) { em ->
                            Surface(
                                onClick = { newFolderInsideSectionEmoji = em },
                                shape = RoundedCornerShape(6.dp),
                                color = if (newFolderInsideSectionEmoji == em) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                border = if (newFolderInsideSectionEmoji == em) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2563EB)) else null,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) { Text(em, fontSize = 16.sp) }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { addingFolderToSection = null }) {
                            Text("Cancel", color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newFolderInsideSectionName.isNotBlank()) {
                                    val newFolder = ContentFolder(
                                        id = "f_${UUID.randomUUID().toString().take(6)}",
                                        prof = ProfYear.FIRST_PROF,
                                        subjectId = sec.id,
                                        sectionType = "CUSTOM_SECTION",
                                        name = newFolderInsideSectionName.trim(),
                                        emoji = newFolderInsideSectionEmoji
                                    )
                                    MedRepository.createFolder(newFolder)
                                    addingFolderToSection = null
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Create Folder", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Edit Custom Section Dialog
    editingSection?.let { sec ->
        var editTitle by remember { mutableStateOf(sec.title) }
        var editSubtitle by remember { mutableStateOf(sec.subtitle) }
        var editEmoji by remember { mutableStateOf(sec.emoji) }

        Dialog(onDismissRequest = { editingSection = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Edit Custom Section", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        textStyle = com.example.ui.theme.medInputTextStyle(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editSubtitle,
                        onValueChange = { editSubtitle = it },
                        label = { Text("Subtitle") },
                        textStyle = com.example.ui.theme.medInputTextStyle(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(emojiList) { em ->
                            Surface(
                                onClick = { editEmoji = em },
                                shape = RoundedCornerShape(6.dp),
                                color = if (editEmoji == em) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                border = if (editEmoji == em) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2563EB)) else null,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) { Text(em, fontSize = 16.sp) }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editingSection = null }) {
                            Text("Cancel", color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (editTitle.isNotBlank()) {
                                    val updated = sec.copy(
                                        title = editTitle.trim(),
                                        subtitle = editSubtitle.trim(),
                                        emoji = editEmoji
                                    )
                                    MedRepository.updateCustomSection(updated)
                                    editingSection = null
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

}

sealed class DeletionTarget {
    data class Folder(val folder: ContentFolder, val count: Int = 0) : DeletionTarget()
    data class StudyContent(val item: StudyContentItem) : DeletionTarget()
}

@Composable
fun ManageLibraryCategoryList(
    onCategorySelected: (Int) -> Unit,
    navigateTo: (Screen) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val manageOptions = listOf(
            Triple(0, "📁 NOTES MANAGE SECTION", { navigateTo(Screen.AdminNotesManage) }),
            Triple(1, "🎯 MCQ MANAGE SECTION", { navigateTo(Screen.AdminMcqManage) }),
            Triple(2, "📚 MBBS Subjects Content", { onCategorySelected(0) }),
            Triple(3, "🎬 Shorts", { onCategorySelected(1) })
        )
        manageOptions.forEach { (_, title, onClickAction) ->
            Surface(
                onClick = onClickAction,
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}


@Composable
fun ManageSectionView(
    navigateTo: (Screen) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<Int?>(null) }
    
    if (selectedCategory != null) {
        BackHandler { selectedCategory = null }
        
        // Full screen detail view
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with Back Button
                Surface(
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedCategory = null }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF2563EB))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (selectedCategory) {
                                0 -> "📚 PDFs & Videos"
                                1 -> "🎬 Shorts"
                                else -> "Manage Library"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                }
                
                // Detail content
                when (selectedCategory) {
                    0 -> ManagePdfsAndVideosView()
                    1 -> ManageShortsView()
                }
            }
        }
    } else {
        // Category list
        ManageLibraryCategoryList(
            onCategorySelected = { selectedCategory = it },
            navigateTo = navigateTo
        )
    }
}

@Composable
fun AdminDeletionDialogs(
    deletionTarget: DeletionTarget?,
    onDismiss: () -> Unit
) {
    if (deletionTarget != null) {
        val target = deletionTarget
        val (dialogTitle, dialogMessage, confirmButtonText) = when (target) {
            is DeletionTarget.Folder -> Triple(
                "Delete Folder?",
                "Are you sure you want to permanently delete folder \"${target.folder.name}\" and all ${target.count} file(s) in it?\n\nThis cannot be undone.",
                "Delete Folder"
            )
            is DeletionTarget.StudyContent -> Triple(
                "Delete File?",
                "Are you sure you want to permanently delete \"${target.item.title}\"?\n\nThis cannot be undone.",
                "Delete File"
            )
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Delete Icon",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = dialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = dialogMessage,
                    fontSize = 13.5.sp,
                    color = Color(0xFF475569),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (val t = deletionTarget) {
                            is DeletionTarget.Folder -> {
                                MedRepository.deleteFolder(t.folder.id, t.folder.name)
                            }
                            is DeletionTarget.StudyContent -> MedRepository.deleteStudyContent(t.item)
                            null -> {}
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(confirmButtonText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}

    
@Composable
fun ManagePdfsAndVideosView() {
    val folders by MedRepository.folders.collectAsState()
    val contents by MedRepository.contents.collectAsState()

    var selectedProf by remember { mutableStateOf(ProfYear.FIRST_PROF) }
    val subjects = remember(selectedProf) { MedRepository.getSubjectsForProf(selectedProf) }
    var selectedSubject by remember(selectedProf) {
        mutableStateOf(subjects.firstOrNull() ?: MedRepository.allSubjects.first())
    }

    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var selectedSubfolder by remember { mutableStateOf<String?>(null) }
    var selectedChapter by remember { mutableStateOf<String?>(null) }

    // Dialog state for Creating Folder
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var createFolderNameInput by remember { mutableStateOf("") }
    var createFolderEmojiInput by remember { mutableStateOf("📁") }

    // Dialog state for Renaming Folder
    var renameFolderTarget by remember { mutableStateOf<String?>(null) }
    var newFolderNameInput by remember { mutableStateOf("") }

    // Dialog state for Deleting Folder
    var deleteFolderTarget by remember { mutableStateOf<String?>(null) }

    // Dialog state for Renaming Subfolder
    var renameSubfolderTarget by remember { mutableStateOf<String?>(null) }
    var newSubfolderNameInput by remember { mutableStateOf("") }

    // Dialog state for Deleting Subfolder
    var deleteSubfolderTarget by remember { mutableStateOf<String?>(null) }

    // Dialog state for Renaming Chapter
    var renameChapterTarget by remember { mutableStateOf<String?>(null) }
    var newChapterNameInput by remember { mutableStateOf("") }

    // Dialog state for Deleting Chapter
    var deleteChapterTarget by remember { mutableStateOf<String?>(null) }

    // Dialog state for Editing Study Item
    var editingItem by remember { mutableStateOf<StudyContentItem?>(null) }
    var editTitleInput by remember { mutableStateOf("") }
    var editDescInput by remember { mutableStateOf("") }

    // Dialog state for Deleting Study Item
    var deletingItem by remember { mutableStateOf<StudyContentItem?>(null) }

    // Filter contents for selected prof & subject
    val subjectContents = remember(contents, selectedProf, selectedSubject) {
        contents.filter { it.prof == selectedProf && it.subjectId == selectedSubject.id }
    }
    val subjectFolders = remember(folders, selectedProf, selectedSubject) {
        folders.filter { it.prof == selectedProf && it.subjectId == selectedSubject.id }
    }

    // Back handling within manage
    BackHandler(enabled = selectedFolder != null) {
        when {
            selectedChapter != null -> selectedChapter = null
            selectedSubfolder != null -> selectedSubfolder = null
            selectedFolder != null -> selectedFolder = null
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create New Folder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Subject: ${selectedSubject.name} (${selectedProf.title})", fontSize = 12.sp, color = Color(0xFF64748B))
                    OutlinedTextField(
                        value = createFolderNameInput,
                        onValueChange = { createFolderNameInput = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Text("Choose Emoji:", fontSize = 12.sp, color = Color(0xFF64748B))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("📁", "📚", "🔬", "🫀", "🧠", "🦴", "🧪", "💊").forEach { em ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (createFolderEmojiInput == em) Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                                border = if (createFolderEmojiInput == em) BorderStroke(1.5.dp, Color(0xFF2563EB)) else null,
                                modifier = Modifier.size(36.dp).clickable { createFolderEmojiInput = em }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(em, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (createFolderNameInput.isNotBlank()) {
                            MedRepository.createFolder(
                                ContentFolder(
                                    id = java.util.UUID.randomUUID().toString(),
                                    name = createFolderNameInput.trim(),
                                    emoji = createFolderEmojiInput,
                                    prof = selectedProf,
                                    subjectId = selectedSubject.id
                                )
                            )
                            createFolderNameInput = ""
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") }
            }
        )
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
                Button(
                    onClick = {
                        if (newFolderNameInput.isNotBlank()) {
                            MedRepository.renameMbbsFolder(selectedProf, selectedSubject.id, renameFolderTarget!!, newFolderNameInput.trim())
                            if (selectedFolder == renameFolderTarget) selectedFolder = newFolderNameInput.trim()
                        }
                        renameFolderTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Save")
                }
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
            title = { Text("Delete Folder?", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = { Text("Are you sure you want to delete '$deleteFolderTarget' and all files inside it?") },
            confirmButton = {
                Button(
                    onClick = {
                        MedRepository.deleteMbbsFolder(selectedProf, selectedSubject.id, deleteFolderTarget!!)
                        if (selectedFolder == deleteFolderTarget) selectedFolder = null
                        deleteFolderTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
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
                Button(
                    onClick = {
                        if (newSubfolderNameInput.isNotBlank() && selectedFolder != null) {
                            MedRepository.renameMbbsSubfolder(selectedProf, selectedSubject.id, selectedFolder!!, renameSubfolderTarget!!, newSubfolderNameInput.trim())
                            if (selectedSubfolder == renameSubfolderTarget) selectedSubfolder = newSubfolderNameInput.trim()
                        }
                        renameSubfolderTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Save")
                }
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
            text = { Text("Are you sure you want to delete subfolder '$deleteSubfolderTarget' and all its contents?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedFolder != null) {
                            MedRepository.deleteMbbsSubfolder(selectedProf, selectedSubject.id, selectedFolder!!, deleteSubfolderTarget!!)
                            if (selectedSubfolder == deleteSubfolderTarget) selectedSubfolder = null
                        }
                        deleteSubfolderTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteSubfolderTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Rename Chapter Dialog
    if (renameChapterTarget != null) {
        AlertDialog(
            onDismissRequest = { renameChapterTarget = null },
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
                Button(
                    onClick = {
                        if (newChapterNameInput.isNotBlank() && selectedFolder != null) {
                            MedRepository.renameMbbsChapter(selectedProf, selectedSubject.id, selectedFolder!!, selectedSubfolder ?: "", renameChapterTarget!!, newChapterNameInput.trim())
                            if (selectedChapter == renameChapterTarget) selectedChapter = newChapterNameInput.trim()
                        }
                        renameChapterTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Save")
                }
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
            text = { Text("Are you sure you want to delete chapter '$deleteChapterTarget' and all files inside it?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedFolder != null) {
                            MedRepository.deleteMbbsChapter(selectedProf, selectedSubject.id, selectedFolder!!, selectedSubfolder ?: "", deleteChapterTarget!!)
                            if (selectedChapter == deleteChapterTarget) selectedChapter = null
                        }
                        deleteChapterTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteChapterTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Edit Item Dialog
    editingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Edit File Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitleInput,
                        onValueChange = { editTitleInput = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = editDescInput,
                        onValueChange = { editDescInput = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitleInput.isNotBlank()) {
                            MedRepository.editStudyContent(item.id, editTitleInput.trim(), editDescInput.trim())
                        }
                        editingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Item Dialog
    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text("Delete File?", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = { Text("Are you sure you want to delete '${item.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        MedRepository.deleteStudyContent(item)
                        deletingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // TOP CONTROLS: PROF CHIPS & SUBJECT CHIPS
        item {
            // Prof selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProfYear.values().forEach { prof ->
                    val isSelected = selectedProf == prof
                    Surface(
                        onClick = {
                            selectedProf = prof
                            selectedSubject = MedRepository.getSubjectsForProf(prof).firstOrNull() ?: MedRepository.allSubjects.first()
                            selectedFolder = null
                            selectedSubfolder = null
                            selectedChapter = null
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF1E293B) else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = prof.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subject chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(subjects) { sub ->
                    val isSelected = selectedSubject.id == sub.id
                    Surface(
                        onClick = {
                            selectedSubject = sub
                            selectedFolder = null
                            selectedSubfolder = null
                            selectedChapter = null
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF2563EB) else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(sub.emoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sub.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Breadcrumb path & Navigation bar
            Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (selectedFolder != null) {
                            IconButton(
                                onClick = {
                                    when {
                                        selectedChapter != null -> selectedChapter = null
                                        selectedSubfolder != null -> selectedSubfolder = null
                                        selectedFolder != null -> selectedFolder = null
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = when {
                                selectedChapter != null -> "${selectedFolder} > ${selectedSubfolder ?: ""} > $selectedChapter"
                                selectedSubfolder != null -> "${selectedFolder} > $selectedSubfolder"
                                selectedFolder != null -> "${selectedFolder}"
                                else -> "${selectedSubject.name} Folders"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (selectedFolder == null) {
                        FilledTonalButton(
                            onClick = {
                                createFolderNameInput = ""
                                showCreateFolderDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Folder", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // LEVEL 1: FOLDERS
        if (selectedFolder == null) {
            val distinctFolderNames = (subjectFolders.map { it.name } + subjectContents.map { it.folderName })
                .filter { it.isNotBlank() }
                .distinct()

            if (distinctFolderNames.isEmpty() && subjectContents.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No folders created yet in ${selectedSubject.name}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                }
            } else {
                items(distinctFolderNames) { folderName ->
                    val folderObj = subjectFolders.firstOrNull { it.name.equals(folderName, ignoreCase = true) }
                    val count = subjectContents.count { it.folderName.equals(folderName, ignoreCase = true) }
                    val emoji = folderObj?.emoji ?: "📁"

                    Card(
                        onClick = { selectedFolder = folderName },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(emoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(folderName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("$count file(s)", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            IconButton(
                                onClick = {
                                    renameFolderTarget = folderName
                                    newFolderNameInput = folderName
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { deleteFolderTarget = folderName },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Direct items with no folder
                val unassigned = subjectContents.filter { it.folderName.isBlank() }
                if (unassigned.isNotEmpty()) {
                    item {
                        Text("Direct Files", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    items(unassigned, key = { it.id }) { item ->
                        ManageStudyContentItemCard(
                            item = item,
                            onEdit = {
                                editingItem = item
                                editTitleInput = item.title
                                editDescInput = item.description
                            },
                            onDelete = { deletingItem = item }
                        )
                    }
                }
            }
        }
        // LEVEL 2: SUBFOLDERS
        else if (selectedSubfolder == null) {
            val folderContents = subjectContents.filter { it.folderName.equals(selectedFolder, ignoreCase = true) }
            val distinctSubfolders = folderContents.map { it.subfolderName }.filter { it.isNotBlank() }.distinct()

            if (distinctSubfolders.isEmpty()) {
                val distinctChapters = folderContents.map { it.chapterName }.filter { it.isNotBlank() }.distinct()
                if (distinctChapters.isEmpty()) {
                    if (folderContents.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Folder is empty", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(folderContents, key = { it.id }) { item ->
                            ManageStudyContentItemCard(
                                item = item,
                                onEdit = {
                                    editingItem = item
                                    editTitleInput = item.title
                                    editDescInput = item.description
                                },
                                onDelete = { deletingItem = item }
                            )
                        }
                    }
                } else {
                    items(distinctChapters) { chapterName ->
                        val count = folderContents.count { it.chapterName.equals(chapterName, ignoreCase = true) }
                        Card(
                            onClick = { selectedChapter = chapterName },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📑", fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(chapterName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Text("$count file(s)", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                                IconButton(
                                    onClick = {
                                        renameChapterTarget = chapterName
                                        newChapterNameInput = chapterName
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { deleteChapterTarget = chapterName },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    val noChapterItems = folderContents.filter { it.chapterName.isBlank() }
                    items(noChapterItems, key = { it.id }) { item ->
                        ManageStudyContentItemCard(
                            item = item,
                            onEdit = {
                                editingItem = item
                                editTitleInput = item.title
                                editDescInput = item.description
                            },
                            onDelete = { deletingItem = item }
                        )
                    }
                }
            } else {
                items(distinctSubfolders) { subfolder ->
                    val count = folderContents.count { it.subfolderName.equals(subfolder, ignoreCase = true) }
                    Card(
                        onClick = { selectedSubfolder = subfolder },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📂", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(subfolder, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("$count file(s)", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            IconButton(
                                onClick = {
                                    renameSubfolderTarget = subfolder
                                    newSubfolderNameInput = subfolder
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { deleteSubfolderTarget = subfolder },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        }
                    }
                }
                val directInFolder = folderContents.filter { it.subfolderName.isBlank() && it.chapterName.isBlank() }
                items(directInFolder, key = { it.id }) { item ->
                    ManageStudyContentItemCard(
                        item = item,
                        onEdit = {
                            editingItem = item
                            editTitleInput = item.title
                            editDescInput = item.description
                        },
                        onDelete = { deletingItem = item }
                    )
                }
            }
        }
        // LEVEL 3: CHAPTERS
        else if (selectedChapter == null) {
            val subfolderContents = subjectContents.filter {
                it.folderName.equals(selectedFolder, ignoreCase = true) &&
                it.subfolderName.equals(selectedSubfolder, ignoreCase = true)
            }
            val distinctChapters = subfolderContents.map { it.chapterName }.filter { it.isNotBlank() }.distinct()

            if (distinctChapters.isEmpty()) {
                if (subfolderContents.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Subfolder is empty", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                    }
                } else {
                    items(subfolderContents, key = { it.id }) { item ->
                        ManageStudyContentItemCard(
                            item = item,
                            onEdit = {
                                editingItem = item
                                editTitleInput = item.title
                                editDescInput = item.description
                            },
                            onDelete = { deletingItem = item }
                        )
                    }
                }
            } else {
                items(distinctChapters) { chapterName ->
                    val count = subfolderContents.count { it.chapterName.equals(chapterName, ignoreCase = true) }
                    Card(
                        onClick = { selectedChapter = chapterName },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📑", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(chapterName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("$count file(s)", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            IconButton(
                                onClick = {
                                    renameChapterTarget = chapterName
                                    newChapterNameInput = chapterName
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { deleteChapterTarget = chapterName },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        }
                    }
                }
                val directInSub = subfolderContents.filter { it.chapterName.isBlank() }
                items(directInSub, key = { it.id }) { item ->
                    ManageStudyContentItemCard(
                        item = item,
                        onEdit = {
                            editingItem = item
                            editTitleInput = item.title
                            editDescInput = item.description
                        },
                        onDelete = { deletingItem = item }
                    )
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
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No items inside this chapter", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                }
            } else {
                items(chapterContents, key = { it.id }) { item ->
                    ManageStudyContentItemCard(
                        item = item,
                        onEdit = {
                            editingItem = item
                            editTitleInput = item.title
                            editDescInput = item.description
                        },
                        onDelete = { deletingItem = item }
                    )
                }
            }
        }
    }
}

@Composable
fun ManageStudyContentItemCard(
    item: StudyContentItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isPdf = item.type == StudyContentType.PDF
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isPdf) Color(0xFFDCFCE7) else Color(0xFFEFF6FF),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (isPdf) "📄" else "🎬", fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            }
        }
    }
}
