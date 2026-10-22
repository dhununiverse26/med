package com.example.data

import android.content.Context
import com.example.data.local.*
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.example.model.Catalog


object MedRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    var database: MedXMedDatabase? = null

    // User Profile state
    private var sharedPrefs: android.content.SharedPreferences? = null

    private val _userName = MutableStateFlow("Dr. Viv")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userAvatar = MutableStateFlow("👨‍⚕️")
    val userAvatar: StateFlow<String> = _userAvatar.asStateFlow()

    private val _userPhotoUri = MutableStateFlow<String?>(null)
    val userPhotoUri: StateFlow<String?> = _userPhotoUri.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _currentTheme = MutableStateFlow(0)
    val currentTheme: StateFlow<Int> = _currentTheme.asStateFlow()

    // Admin Session Authentication State
    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    fun unlockAdmin() {
        _isAdminUnlocked.value = true
    }

    fun lockAdmin() {
        _isAdminUnlocked.value = false
    }

    // Hidden Items (Sections, Folders, Subfolders, Chapters)
    private val _hiddenItemKeys = MutableStateFlow<Set<String>>(emptySet())
    val hiddenItemKeys: StateFlow<Set<String>> = _hiddenItemKeys.asStateFlow()

    fun toggleHideItem(key: String) {
        val current = _hiddenItemKeys.value
        val updated = if (current.contains(key)) current - key else current + key
        _hiddenItemKeys.value = updated
        sharedPrefs?.edit()?.putStringSet("hidden_items_keys", updated)?.apply()
    }

    fun isItemHidden(key: String): Boolean {
        return _hiddenItemKeys.value.contains(key)
    }

    // App Info & Settings (About Us, Notice, Privacy, Refer, Rating)
    private val defaultAboutUs = "MedXMed is a dedicated MBBS learning & revision platform designed by doctors for medical students. Access high-yield clinical notes, video lectures, shorts, and Pomodoro study tools all in one place."
    private val defaultNotice = "All MBBS study modules, notes, and video lectures are actively updated and synchronized."
    private val defaultPrivacy = "MedXMed Privacy Policy: Your data is stored locally. We do not collect it."

    private val defaultPrivacyUrl = "https://sites.google.com/view/medxmed-privacy-policy/home"
    private val defaultRefer = "Study MBBS smartly with MedXMed App! Access high-yield notes, lectures, and shorts."
    private val defaultReferUrl = "https://play.google.com/store/apps/details?id=com.example.medxmed"
    private val defaultRatingUrl = "https://play.google.com/store/apps/details?id=com.example.medxmed"

    private val _aboutUsText = MutableStateFlow(defaultAboutUs)
    val aboutUsText: StateFlow<String> = _aboutUsText.asStateFlow()

    private val _aboutUsUrl = MutableStateFlow("")
    val aboutUsUrl: StateFlow<String> = _aboutUsUrl.asStateFlow()

    private val _noticeText = MutableStateFlow(defaultNotice)
    val noticeText: StateFlow<String> = _noticeText.asStateFlow()

    private val _noticeUrl = MutableStateFlow("")
    val noticeUrl: StateFlow<String> = _noticeUrl.asStateFlow()

    private val _privacyText = MutableStateFlow(defaultPrivacy)
    val privacyText: StateFlow<String> = _privacyText.asStateFlow()

    private val _privacyUrl = MutableStateFlow(defaultPrivacyUrl)
    val privacyUrl: StateFlow<String> = _privacyUrl.asStateFlow()

    private val _referText = MutableStateFlow(defaultRefer)
    val referText: StateFlow<String> = _referText.asStateFlow()

    private val _referUrl = MutableStateFlow(defaultReferUrl)
    val referUrl: StateFlow<String> = _referUrl.asStateFlow()

    private val _ratingUrl = MutableStateFlow(defaultRatingUrl)
    val ratingUrl: StateFlow<String> = _ratingUrl.asStateFlow()

    fun updateAppInfoSettings(
        aboutText: String,
        aboutUrl: String,
        noticeTextVal: String,
        noticeUrlVal: String,
        privacyTextVal: String,
        privacyUrlVal: String,
        referTextVal: String,
        referUrlVal: String,
        ratingUrlVal: String
    ) {
        _aboutUsText.value = aboutText
        _aboutUsUrl.value = aboutUrl
        _noticeText.value = noticeTextVal
        _noticeUrl.value = noticeUrlVal
        _privacyText.value = privacyTextVal
        _privacyUrl.value = privacyUrlVal
        _referText.value = referTextVal
        _referUrl.value = referUrlVal
        _ratingUrl.value = ratingUrlVal

        sharedPrefs?.edit()
            ?.putString("about_us_text", aboutText)
            ?.putString("about_us_url", aboutUrl)
            ?.putString("notice_text", noticeTextVal)
            ?.putString("notice_url", noticeUrlVal)
            ?.putString("privacy_text", privacyTextVal)
            ?.putString("privacy_url", privacyUrlVal)
            ?.putString("refer_text", referTextVal)
            ?.putString("refer_url", referUrlVal)
            ?.putString("rating_url", ratingUrlVal)
            ?.apply()
    }

    fun updateTheme(themeIndex: Int) {
        _currentTheme.value = themeIndex
        sharedPrefs?.edit()?.putInt("app_theme", themeIndex)?.apply()
    }

    fun updateOnlineStatus(isOnline: Boolean) {
        _isOnline.value = isOnline
    }

    fun checkNetworkOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNetwork)
            val hasInternet = caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            _isOnline.value = hasInternet
            hasInternet
        } catch (e: Exception) {
            _isOnline.value = false
            false
        }
    }

    fun clearLocalTempPdfFiles(context: Context) {
        try {
            context.cacheDir.listFiles { _, name -> name.endsWith(".pdf") || name.startsWith("pdf_") || name.startsWith("temp_") }?.forEach {
                it.delete()
            }
        } catch (_: Exception) {}
    }

    fun updateProfile(name: String, avatar: String, photoUri: String? = null) {
        _userName.value = name.ifBlank { "Dr. Student" }
        _userAvatar.value = avatar
        _userPhotoUri.value = photoUri
        sharedPrefs?.edit()
            ?.putString("user_name", _userName.value)
            ?.putString("user_avatar", avatar)
            ?.putString("user_photo_uri", photoUri ?: "")
            ?.apply()
    }

    val allSubjects: List<Subject> = listOf(
        // 1st Prof
        Subject("anat", "Anatomy", "🦴", ProfYear.FIRST_PROF, "Gross Anatomy, Embryology, Histology", 28, 142),
        Subject("physio", "Physiology", "🧪", ProfYear.FIRST_PROF, "General, CVS, CNS, Renal, Endocrine", 24, 120),
        Subject("biochem", "Biochemistry", "🧬", ProfYear.FIRST_PROF, "Metabolism, Molecular Genetics", 20, 98),

        // 2nd Prof
        Subject("path", "Pathology", "🔬", ProfYear.SECOND_PROF, "General Pathology, Hematology, Systemic", 32, 160),
        Subject("pharm", "Pharmacology", "💊", ProfYear.SECOND_PROF, "Autonomic, CNS, CVS, Chemotherapy", 30, 155),
        Subject("micro", "Microbiology", "🧫", ProfYear.SECOND_PROF, "Bacteriology, Virology, Immunology", 26, 135),

        // 3rd Prof I
        Subject("opht", "Ophthalmology", "👁️", ProfYear.THIRD_PROF_1, "Cornea, Cataract, Glaucoma, Retina", 18, 85),
        Subject("ent", "ENT", "👂", ProfYear.THIRD_PROF_1, "Otology, Rhinology, Larynx, Head & Neck", 16, 80),
        Subject("psm", "Community Medicine", "📊", ProfYear.THIRD_PROF_1, "Epidemiology, Biostats, Health Programs", 25, 110),
        Subject("fmt", "Forensic Medicine", "⚖️", ProfYear.THIRD_PROF_1, "Thanatology, Toxicology, Legal", 15, 75),

        // 3rd Prof II (Final Prof)
        Subject("med", "General Medicine", "🩺", ProfYear.THIRD_PROF_2, "Cardiology, Neurology, Pulmonology", 40, 220),
        Subject("surg", "General Surgery", "🔪", ProfYear.THIRD_PROF_2, "Trauma, GI, Urology, Orthopedics", 38, 205),
        Subject("obg", "Obstetrics & Gynecology", "👶", ProfYear.THIRD_PROF_2, "Obstetrics, Labor, Gynecology", 30, 150),
        Subject("peds", "Pediatrics", "🍼", ProfYear.THIRD_PROF_2, "Neonatology, Growth & Milestones", 22, 115)
    )

    const val CLOUDFLARE_R2_BASE_URL = "https://pub-e13713dacf53452ebe6b596d008341b8.r2.dev"
    const val CLOUDFLARE_BUCKET_NAME = "medxmed"

    private val _folders = MutableStateFlow<List<ContentFolder>>(emptyList())
    val folders: StateFlow<List<ContentFolder>> = _folders.asStateFlow()

    private val _contents = MutableStateFlow<List<StudyContentItem>>(emptyList())
    val contents: StateFlow<List<StudyContentItem>> = _contents.asStateFlow()

    private val _notes = MutableStateFlow<List<NoteItem>>(emptyList())
    val notes: StateFlow<List<NoteItem>> = _notes.asStateFlow()

    private val _cachedOfflineNotes = MutableStateFlow<List<NoteItem>>(emptyList())
    val cachedOfflineNotes: StateFlow<List<NoteItem>> = _cachedOfflineNotes.asStateFlow()

    private val _lectures = MutableStateFlow<List<LectureItem>>(emptyList())
    val lectures: StateFlow<List<LectureItem>> = _lectures.asStateFlow()

    private val _shorts = MutableStateFlow<List<ShortItem>>(emptyList())
    val shorts: StateFlow<List<ShortItem>> = _shorts.asStateFlow()

    private val _adminNotes = MutableStateFlow<List<AdminNoteItem>>(emptyList())
    val adminNotes: StateFlow<List<AdminNoteItem>> = _adminNotes.asStateFlow()

    private val _mcqItems = MutableStateFlow<List<McqItem>>(emptyList())
    val mcqItems: StateFlow<List<McqItem>> = _mcqItems.asStateFlow()

    private val _mcqBookmarks = MutableStateFlow<List<McqBookmarkItem>>(emptyList())
    val mcqBookmarks: StateFlow<List<McqBookmarkItem>> = _mcqBookmarks.asStateFlow()

    private val _mcqHistory = MutableStateFlow<List<McqHistoryItem>>(emptyList())
    val mcqHistory: StateFlow<List<McqHistoryItem>> = _mcqHistory.asStateFlow()

    private val _customSections = MutableStateFlow<List<CustomSection>>(emptyList())
    val customSections: StateFlow<List<CustomSection>> = _customSections.asStateFlow()

    fun initialize(context: Context) {
        if (database != null) return
        sharedPrefs = context.getSharedPreferences("medxmed_prefs", Context.MODE_PRIVATE)
        _userName.value = sharedPrefs?.getString("user_name", "Dr. Viv") ?: "Dr. Viv"
        _currentTheme.value = 0
        sharedPrefs?.edit()?.putInt("app_theme", 0)?.apply()
        val savedPhoto = sharedPrefs?.getString("user_photo_uri", null)
        if (!savedPhoto.isNullOrBlank()) {
            _userPhotoUri.value = savedPhoto
        }

        val savedHidden = sharedPrefs?.getStringSet("hidden_items_keys", emptySet()) ?: emptySet()
        _hiddenItemKeys.value = savedHidden

        // Load App Info Settings (About Us, Notice, Privacy, Refer, Rating)
        _aboutUsText.value = sharedPrefs?.getString("about_us_text", defaultAboutUs) ?: defaultAboutUs
        _aboutUsUrl.value = sharedPrefs?.getString("about_us_url", "") ?: ""
        _noticeText.value = sharedPrefs?.getString("notice_text", defaultNotice) ?: defaultNotice
        _noticeUrl.value = sharedPrefs?.getString("notice_url", "") ?: ""
        _privacyText.value = sharedPrefs?.getString("privacy_text", defaultPrivacy) ?: defaultPrivacy
        val loadedPrivacyUrl = sharedPrefs?.getString("privacy_url", null)
        _privacyUrl.value = if (loadedPrivacyUrl.isNullOrBlank()) defaultPrivacyUrl else loadedPrivacyUrl
        _referText.value = sharedPrefs?.getString("refer_text", defaultRefer) ?: defaultRefer
        _referUrl.value = sharedPrefs?.getString("refer_url", defaultReferUrl) ?: defaultReferUrl
        _ratingUrl.value = sharedPrefs?.getString("rating_url", defaultRatingUrl) ?: defaultRatingUrl
        
        fetchCatalog()

        // Initialize Cloudflare R2
        com.example.data.cloud.CloudflareR2Manager.initialize(context)

        val db = MedXMedDatabase.getDatabase(context)
        database = db

        // Bind Room DAOs to StateFlows with tombstone filtering
        repositoryScope.launch {
            db.folderDao().getAllFolders()
                .map { entities -> entities.map { it.toDomain() }.filterNot { isFolderDeleted(it.id, it.name) } }
                .collect { list -> _folders.value = list }
        }

        repositoryScope.launch {
            db.noteDao().getAllNotes()
                .map { entities -> entities.map { it.toDomain() } }
                .collect { list ->
                    if (list.isNotEmpty() || _contents.value.none { it.type == StudyContentType.PDF }) {
                        _notes.value = list
                    }
                }
        }

        repositoryScope.launch {
            db.noteDao().getCachedOfflineNotes()
                .map { entities -> entities.map { it.toDomain() } }
                .collect { list -> _cachedOfflineNotes.value = list }
        }

        repositoryScope.launch {
            db.studyContentDao().getAllContents()
                .map { entities -> entities.map { it.toDomain() }.filterNot { isContentDeleted(it.id) || isFolderDeleted(it.folderId, it.folderName) } }
                .collect { list ->
                    _contents.value = list
                    // Also populate _notes and _lectures for cross-compatibility
                    val notesList = list.filter { it.type == StudyContentType.PDF }.map {
                        NoteItem(
                            id = it.id,
                            subjectId = it.subjectId,
                            prof = it.prof,
                            title = it.title,
                            chapter = if (it.chapterName.isNotBlank()) it.chapterName else it.folderName,
                            pagesCount = 10,
                            readMinutes = 15,
                            highYieldSummary = it.description.ifBlank { "High-Yield MBBS lecture notes and clinical companion for ${it.title}." },
                            pdfMockUrl = it.mediaUrl,
                            isCachedOffline = true
                        )
                    }
                    val lecturesList = list.filter { it.type == StudyContentType.VIDEO }.map {
                        LectureItem(
                            id = it.id,
                            subjectId = it.subjectId,
                            prof = it.prof,
                            title = it.title,
                            chapter = if (it.chapterName.isNotBlank()) it.chapterName else it.folderName,
                            faculty = "Faculty",
                            duration = "Video",
                            videoUrl = it.mediaUrl
                        )
                    }
                    if (notesList.isNotEmpty()) {
                        _notes.value = notesList
                        repositoryScope.launch {
                            db.noteDao().insertNotes(notesList.map { NoteEntity.fromDomain(it) })
                        }
                    }
                    _lectures.value = lecturesList
                }
        }

        repositoryScope.launch {
            db.shortDao().getAllShorts()
                .map { entities -> entities.map { it.toDomain() }.filterNot { isShortDeleted(it.id) } }
                .collect { list -> _shorts.value = list }
        }

        repositoryScope.launch {
            db.adminNoteDao().getAllAdminNotes()
                .map { entities -> entities.map { it.toDomain() }.filterNot { isAdminNoteDeleted(it.id) } }
                .collect { list -> _adminNotes.value = list }
        }

        repositoryScope.launch {
            db.mcqDao().getAllMcqs()
                .map { entities -> entities.map { it.toDomain() }.filterNot { isMcqDeleted(it.id) } }
                .collect { list -> _mcqItems.value = list }
        }

        repositoryScope.launch {
            db.mcqBookmarkDao().getAllBookmarks()
                .map { entities -> entities.map { it.toDomain() } }
                .collect { list -> _mcqBookmarks.value = list }
        }

        repositoryScope.launch {
            db.mcqHistoryDao().getAllHistory()
                .map { entities -> entities.map { it.toDomain() } }
                .collect { list -> _mcqHistory.value = list }
        }
    }

    fun addAdminNote(item: AdminNoteItem) {
        clearDeletedRecord(item.id)
        _adminNotes.update { listOf(item) + it }
        database?.let { db ->
            repositoryScope.launch {
                db.adminNoteDao().insertAdminNote(AdminNoteEntity.fromDomain(item))
            }
        }
        triggerCatalogPublish("adminNotes")
    }

    fun editAdminNote(id: String, folderName: String, subfolderName: String, subSubfolderName: String, chapterName: String, pdfTitle: String) {
        _adminNotes.update { list ->
            list.map { if (it.id == id) it.copy(folderName = folderName, subfolderName = subfolderName, subSubfolderName = subSubfolderName, chapterName = chapterName, pdfTitle = pdfTitle) else it }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.adminNoteDao().updateAdminNote(id, folderName, subfolderName, subSubfolderName, chapterName, pdfTitle)
            }
        }
        triggerCatalogPublish("adminNotes")
    }

    fun deleteAdminNote(id: String) {
        recordDeletedAdminNote(id)
        _adminNotes.update { list -> list.filterNot { it.id == id } }
        database?.let { db ->
            repositoryScope.launch {
                db.adminNoteDao().deleteAdminNoteById(id)
            }
        }
        triggerCatalogPublish("adminNotes")
    }

    fun moveAdminNoteUp(id: String) {
        val list = _adminNotes.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index > 0) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            list.forEachIndexed { idx, it -> list[idx] = it.copy(sortOrder = idx) }
            _adminNotes.value = list
            database?.let { db ->
                repositoryScope.launch {
                    list.forEach { db.adminNoteDao().updateSortOrder(it.id, it.sortOrder) }
                }
            }
            triggerCatalogPublish("adminNotes")
        }
    }

    fun moveAdminNoteDown(id: String) {
        val list = _adminNotes.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0 && index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            list.forEachIndexed { idx, it -> list[idx] = it.copy(sortOrder = idx) }
            _adminNotes.value = list
            database?.let { db ->
                repositoryScope.launch {
                    list.forEach { db.adminNoteDao().updateSortOrder(it.id, it.sortOrder) }
                }
            }
            triggerCatalogPublish("adminNotes")
        }
    }

    fun renameAdminFolder(oldName: String, newName: String) {
        _adminNotes.update { list ->
            list.map { if (it.folderName == oldName) it.copy(folderName = newName) else it }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.adminNoteDao().renameFolder(oldName, newName)
            }
        }
        triggerCatalogPublish("adminNotes")
    }

    fun deleteAdminFolder(folderName: String) {
        val toDelete = _adminNotes.value.filter { it.folderName == folderName }
        toDelete.forEach { recordDeletedAdminNote(it.id) }
        _adminNotes.update { list -> list.filterNot { it.folderName == folderName } }
        database?.let { db ->
            repositoryScope.launch {
                db.adminNoteDao().deleteFolder(folderName)
            }
        }
        triggerCatalogPublish("adminNotes")
    }

    fun renameAdminSubfolder(folderName: String, oldSub: String, newSub: String) {
        _adminNotes.update { list ->
            list.map { if (it.folderName == folderName && it.subfolderName == oldSub) it.copy(subfolderName = newSub) else it }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.adminNoteDao().renameSubfolder(folderName, oldSub, newSub)
            }
        }
        triggerCatalogPublish("adminNotes")
    }

    fun deleteAdminSubfolder(folderName: String, subName: String) {
        val toDelete = _adminNotes.value.filter { it.folderName == folderName && it.subfolderName == subName }
        toDelete.forEach { recordDeletedAdminNote(it.id) }
        _adminNotes.update { list -> list.filterNot { it.folderName == folderName && it.subfolderName == subName } }
        database?.let { db ->
            repositoryScope.launch {
                db.adminNoteDao().deleteSubfolder(folderName, subName)
            }
        }
        triggerCatalogPublish("adminNotes")
    }

    fun moveAdminFolderUp(folderName: String) {
        val distinctFolders = _adminNotes.value.map { it.folderName }.distinct().toMutableList()
        val index = distinctFolders.indexOf(folderName)
        if (index > 0) {
            val f = distinctFolders.removeAt(index)
            distinctFolders.add(index - 1, f)
            val reordered = _adminNotes.value.sortedWith(compareBy({ distinctFolders.indexOf(it.folderName) }, { it.sortOrder }))
            val updated = reordered.mapIndexed { idx, it -> it.copy(sortOrder = idx) }
            _adminNotes.value = updated
            database?.let { db ->
                repositoryScope.launch {
                    updated.forEach { db.adminNoteDao().updateSortOrder(it.id, it.sortOrder) }
                }
            }
            triggerCatalogPublish("adminNotes")
        }
    }

    fun moveAdminFolderDown(folderName: String) {
        val distinctFolders = _adminNotes.value.map { it.folderName }.distinct().toMutableList()
        val index = distinctFolders.indexOf(folderName)
        if (index >= 0 && index < distinctFolders.size - 1) {
            val f = distinctFolders.removeAt(index)
            distinctFolders.add(index + 1, f)
            val reordered = _adminNotes.value.sortedWith(compareBy({ distinctFolders.indexOf(it.folderName) }, { it.sortOrder }))
            val updated = reordered.mapIndexed { idx, it -> it.copy(sortOrder = idx) }
            _adminNotes.value = updated
            database?.let { db ->
                repositoryScope.launch {
                    updated.forEach { db.adminNoteDao().updateSortOrder(it.id, it.sortOrder) }
                }
            }
            triggerCatalogPublish("adminNotes")
        }
    }

    fun moveAdminSubfolderUp(folderName: String, subfolderName: String) {
        val subfolders = _adminNotes.value.filter { it.folderName == folderName }.map { it.subfolderName }.distinct().toMutableList()
        val index = subfolders.indexOf(subfolderName)
        if (index > 0) {
            val item = subfolders.removeAt(index)
            subfolders.add(index - 1, item)
            val updated = _adminNotes.value.sortedWith(compareBy(
                { it.folderName },
                { if (it.folderName == folderName) subfolders.indexOf(it.subfolderName) else 0 },
                { it.sortOrder }
            )).mapIndexed { idx, it -> it.copy(sortOrder = idx) }
            _adminNotes.value = updated
            database?.let { db ->
                repositoryScope.launch {
                    updated.forEach { db.adminNoteDao().updateSortOrder(it.id, it.sortOrder) }
                }
            }
            triggerCatalogPublish("adminNotes")
        }
    }

    fun moveAdminSubfolderDown(folderName: String, subfolderName: String) {
        val subfolders = _adminNotes.value.filter { it.folderName == folderName }.map { it.subfolderName }.distinct().toMutableList()
        val index = subfolders.indexOf(subfolderName)
        if (index >= 0 && index < subfolders.size - 1) {
            val item = subfolders.removeAt(index)
            subfolders.add(index + 1, item)
            val updated = _adminNotes.value.sortedWith(compareBy(
                { it.folderName },
                { if (it.folderName == folderName) subfolders.indexOf(it.subfolderName) else 0 },
                { it.sortOrder }
            )).mapIndexed { idx, it -> it.copy(sortOrder = idx) }
            _adminNotes.value = updated
            database?.let { db ->
                repositoryScope.launch {
                    updated.forEach { db.adminNoteDao().updateSortOrder(it.id, it.sortOrder) }
                }
            }
            triggerCatalogPublish("adminNotes")
        }
    }

    fun renameAdminChapter(folderName: String, subName: String, oldChapter: String, newChapter: String) {
        _adminNotes.update { list ->
            list.map {
                if (it.folderName == folderName && it.subfolderName == subName && it.chapterName == oldChapter) {
                    it.copy(chapterName = newChapter)
                } else it
            }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.adminNoteDao().renameChapter(folderName, subName, oldChapter, newChapter)
            }
        }
        triggerCatalogPublish("adminNotes")
    }

    fun deleteAdminChapter(folderName: String, subName: String, chapterName: String) {
        val toDelete = _adminNotes.value.filter { it.folderName == folderName && it.subfolderName == subName && it.chapterName == chapterName }
        toDelete.forEach { recordDeletedAdminNote(it.id) }
        _adminNotes.update { list ->
            list.filterNot { it.folderName == folderName && it.subfolderName == subName && it.chapterName == chapterName }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.adminNoteDao().deleteChapter(folderName, subName, chapterName)
            }
        }
        triggerCatalogPublish("adminNotes")
    }



    fun buildCloudflareUrl(prof: ProfYear, subjectId: String, folderName: String, fileName: String): String {

        val cleanProf = prof.code
        val cleanSubject = subjectId.trim().replace(" ", "_")
        val cleanFolder = folderName.trim().replace(" ", "_")
        val cleanFile = fileName.trim().replace(" ", "_")
        val baseUrl = com.example.data.cloud.CloudflareR2Manager.publicBaseUrl.value.trimEnd('/')
        return "$baseUrl/$cleanProf/$cleanSubject/$cleanFolder/$cleanFile"
    }

    fun buildMcqR2Key(folder: String, subfolder: String, subSubfolder: String = "", chapter: String): String {
        val cleanFolder = folder.trim().replace(" ", "_")
        val cleanSub = subfolder.trim().replace(" ", "_")
        val cleanSubSub = subSubfolder.trim().replace(" ", "_")
        val cleanChap = chapter.trim().replace(" ", "_").removeSuffix(".json")
        return if (cleanSubSub.isNotBlank()) {
            "mcqs/$cleanFolder/$cleanSub/$cleanSubSub/$cleanChap.json"
        } else {
            "mcqs/$cleanFolder/$cleanSub/$cleanChap.json"
        }
    }

    fun addMcqItem(item: McqItem, syncToCloudflareR2: Boolean = true) {
        val computedKey = if (item.r2Key.isNotBlank()) item.r2Key
        else buildMcqR2Key(item.folderName, item.subfolderName, item.subSubfolderName, item.chapterName)
        val computedUrl = if (item.r2Url.isNotBlank()) item.r2Url
        else "${com.example.data.cloud.CloudflareR2Manager.publicBaseUrl.value.trimEnd('/')}/$computedKey"

        val finalizedItem = item.copy(
            r2Key = computedKey,
            r2Url = computedUrl,
            questionCount = if (item.questions.isNotEmpty()) item.questions.size else item.questionCount
        )

        clearDeletedRecord(finalizedItem.id)
        _mcqItems.update { list ->
            val idx = list.indexOfFirst { it.id == finalizedItem.id ||
                (it.folderName.equals(finalizedItem.folderName, ignoreCase = true) &&
                 it.subfolderName.equals(finalizedItem.subfolderName, ignoreCase = true) &&
                 it.subSubfolderName.equals(finalizedItem.subSubfolderName, ignoreCase = true) &&
                 it.chapterName.equals(finalizedItem.chapterName, ignoreCase = true))
            }
            if (idx >= 0) {
                list.toMutableList().apply { set(idx, finalizedItem) }
            } else {
                listOf(finalizedItem) + list
            }
        }

        database?.let { db ->
            repositoryScope.launch {
                db.mcqDao().insertMcq(McqEntity.fromDomain(finalizedItem))
            }
        }

        if (syncToCloudflareR2) {
            repositoryScope.launch {
                try {
                    val jsonQuestions = Json.encodeToString(finalizedItem.questions)
                    com.example.data.cloud.CloudflareR2Manager.uploadString(computedKey, jsonQuestions)
                } catch (e: Exception) {
                    android.util.Log.e("MedRepository", "Failed to upload chapter json to R2: $computedKey", e)
                }
            }
        }
        triggerCatalogPublish("mcqItems")
    }

    fun updateMcqQuestions(id: String, updatedQuestions: List<McqQuestion>) {
        val item = _mcqItems.value.firstOrNull { it.id == id } ?: return
        val updated = item.copy(
            questions = updatedQuestions,
            questionCount = updatedQuestions.size
        )
        addMcqItem(updated, syncToCloudflareR2 = true)
    }

    fun renameMcqFolder(oldName: String, newName: String) {
        val affected = _mcqItems.value.filter { it.folderName == oldName }
        _mcqItems.update { list ->
            list.map {
                if (it.folderName == oldName) {
                    val newKey = buildMcqR2Key(newName, it.subfolderName, it.subSubfolderName, it.chapterName)
                    val newUrl = "${com.example.data.cloud.CloudflareR2Manager.publicBaseUrl.value.trimEnd('/')}/$newKey"
                    it.copy(folderName = newName, r2Key = newKey, r2Url = newUrl)
                } else it
            }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.mcqDao().renameFolder(oldName, newName)
            }
        }
        // Sync re-keyed files to Cloudflare R2
        repositoryScope.launch {
            affected.forEach { item ->
                try {
                    val oldKey = item.r2Key.ifBlank { buildMcqR2Key(oldName, item.subfolderName, item.subSubfolderName, item.chapterName) }
                    val newKey = buildMcqR2Key(newName, item.subfolderName, item.subSubfolderName, item.chapterName)
                    val jsonStr = Json.encodeToString(item.questions)
                    com.example.data.cloud.CloudflareR2Manager.uploadString(newKey, jsonStr)
                    com.example.data.cloud.CloudflareR2Manager.deleteFile(oldKey)
                } catch (_: Exception) {}
            }
        }
        triggerCatalogPublish("mcqItems")
    }

    fun deleteMcqFolder(folderName: String) {
        val toDelete = _mcqItems.value.filter { it.folderName == folderName }
        toDelete.forEach { recordDeletedMcq(it.id) }
        _mcqItems.update { list -> list.filterNot { it.folderName == folderName } }
        database?.let { db ->
            repositoryScope.launch {
                db.mcqDao().deleteFolder(folderName)
            }
        }
        repositoryScope.launch {
            toDelete.forEach { item ->
                try {
                    val key = item.r2Key.ifBlank { buildMcqR2Key(item.folderName, item.subfolderName, item.subSubfolderName, item.chapterName) }
                    com.example.data.cloud.CloudflareR2Manager.deleteFile(key)
                } catch (_: Exception) {}
            }
        }
        triggerCatalogPublish("mcqItems")
    }

    fun renameMcqSubfolder(folderName: String, oldSub: String, newSub: String) {
        val affected = _mcqItems.value.filter { it.folderName == folderName && it.subfolderName == oldSub }
        _mcqItems.update { list ->
            list.map {
                if (it.folderName == folderName && it.subfolderName == oldSub) {
                    val newKey = buildMcqR2Key(folderName, newSub, it.subSubfolderName, it.chapterName)
                    val newUrl = "${com.example.data.cloud.CloudflareR2Manager.publicBaseUrl.value.trimEnd('/')}/$newKey"
                    it.copy(subfolderName = newSub, r2Key = newKey, r2Url = newUrl)
                } else it
            }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.mcqDao().renameSubfolder(folderName, oldSub, newSub)
            }
        }
        repositoryScope.launch {
            affected.forEach { item ->
                try {
                    val oldKey = item.r2Key.ifBlank { buildMcqR2Key(folderName, oldSub, item.subSubfolderName, item.chapterName) }
                    val newKey = buildMcqR2Key(folderName, newSub, item.subSubfolderName, item.chapterName)
                    val jsonStr = Json.encodeToString(item.questions)
                    com.example.data.cloud.CloudflareR2Manager.uploadString(newKey, jsonStr)
                    com.example.data.cloud.CloudflareR2Manager.deleteFile(oldKey)
                } catch (_: Exception) {}
            }
        }
        triggerCatalogPublish("mcqItems")
    }

    fun deleteMcqSubfolder(folderName: String, subName: String) {
        val toDelete = _mcqItems.value.filter { it.folderName == folderName && it.subfolderName == subName }
        toDelete.forEach { recordDeletedMcq(it.id) }
        _mcqItems.update { list -> list.filterNot { it.folderName == folderName && it.subfolderName == subName } }
        database?.let { db ->
            repositoryScope.launch {
                db.mcqDao().deleteSubfolder(folderName, subName)
            }
        }
        repositoryScope.launch {
            toDelete.forEach { item ->
                try {
                    val key = item.r2Key.ifBlank { buildMcqR2Key(item.folderName, item.subfolderName, item.subSubfolderName, item.chapterName) }
                    com.example.data.cloud.CloudflareR2Manager.deleteFile(key)
                } catch (_: Exception) {}
            }
        }
        triggerCatalogPublish("mcqItems")
    }

    fun renameMcqSubSubfolder(folderName: String, subName: String, oldSubSub: String, newSubSub: String) {
        val affected = _mcqItems.value.filter { it.folderName == folderName && it.subfolderName == subName && it.subSubfolderName == oldSubSub }
        _mcqItems.update { list ->
            list.map {
                if (it.folderName == folderName && it.subfolderName == subName && it.subSubfolderName == oldSubSub) {
                    val newKey = buildMcqR2Key(folderName, subName, newSubSub, it.chapterName)
                    val newUrl = "${com.example.data.cloud.CloudflareR2Manager.publicBaseUrl.value.trimEnd('/')}/$newKey"
                    it.copy(subSubfolderName = newSubSub, r2Key = newKey, r2Url = newUrl)
                } else it
            }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.mcqDao().renameSubSubfolder(folderName, subName, oldSubSub, newSubSub)
            }
        }
        repositoryScope.launch {
            affected.forEach { item ->
                try {
                    val oldKey = item.r2Key.ifBlank { buildMcqR2Key(folderName, subName, oldSubSub, item.chapterName) }
                    val newKey = buildMcqR2Key(folderName, subName, newSubSub, item.chapterName)
                    val jsonStr = Json.encodeToString(item.questions)
                    com.example.data.cloud.CloudflareR2Manager.uploadString(newKey, jsonStr)
                    com.example.data.cloud.CloudflareR2Manager.deleteFile(oldKey)
                } catch (_: Exception) {}
            }
        }
        triggerCatalogPublish("mcqItems")
    }

    fun deleteMcqSubSubfolder(folderName: String, subName: String, subSubName: String) {
        val toDelete = _mcqItems.value.filter { it.folderName == folderName && it.subfolderName == subName && it.subSubfolderName == subSubName }
        toDelete.forEach { recordDeletedMcq(it.id) }
        _mcqItems.update { list -> list.filterNot { it.folderName == folderName && it.subfolderName == subName && it.subSubfolderName == subSubName } }
        database?.let { db ->
            repositoryScope.launch {
                db.mcqDao().deleteSubSubfolder(folderName, subName, subSubName)
            }
        }
        repositoryScope.launch {
            toDelete.forEach { item ->
                try {
                    val key = item.r2Key.ifBlank { buildMcqR2Key(item.folderName, item.subfolderName, item.subSubfolderName, item.chapterName) }
                    com.example.data.cloud.CloudflareR2Manager.deleteFile(key)
                } catch (_: Exception) {}
            }
        }
        triggerCatalogPublish("mcqItems")
    }

    fun renameMcqChapter(id: String, newChapterName: String) {
        val item = _mcqItems.value.firstOrNull { it.id == id } ?: return
        val oldKey = item.r2Key.ifBlank { buildMcqR2Key(item.folderName, item.subfolderName, item.subSubfolderName, item.chapterName) }
        val newKey = buildMcqR2Key(item.folderName, item.subfolderName, item.subSubfolderName, newChapterName)
        val newUrl = "${com.example.data.cloud.CloudflareR2Manager.publicBaseUrl.value.trimEnd('/')}/$newKey"

        val updated = item.copy(
            chapterName = newChapterName,
            r2Key = newKey,
            r2Url = newUrl
        )

        _mcqItems.update { list ->
            list.map { if (it.id == id) updated else it }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.mcqDao().renameChapterById(id, newChapterName)
            }
        }
        repositoryScope.launch {
            try {
                val jsonStr = Json.encodeToString(item.questions)
                com.example.data.cloud.CloudflareR2Manager.uploadString(newKey, jsonStr)
                com.example.data.cloud.CloudflareR2Manager.deleteFile(oldKey)
            } catch (_: Exception) {}
        }
        triggerCatalogPublish("mcqItems")
    }

    fun deleteMcqChapter(folderName: String, subName: String, chapterName: String) {
        val matching = _mcqItems.value.filter { it.folderName == folderName && it.subfolderName == subName && it.chapterName == chapterName }
        matching.forEach { recordDeletedMcq(it.id) }
        val firstMatch = matching.firstOrNull()
        if (firstMatch != null) {
            deleteMcqChapterById(firstMatch.id)
        } else {
            _mcqItems.update { list ->
                list.filterNot { it.folderName == folderName && it.subfolderName == subName && it.chapterName == chapterName }
            }
            database?.let { db ->
                repositoryScope.launch {
                    db.mcqDao().deleteChapter(folderName, subName, chapterName)
                }
            }
            triggerCatalogPublish("mcqItems")
        }
    }

    fun deleteMcqChapterById(id: String) {
        recordDeletedMcq(id)
        val item = _mcqItems.value.firstOrNull { it.id == id }
        _mcqItems.update { list -> list.filterNot { it.id == id } }
        database?.let { db ->
            repositoryScope.launch {
                db.mcqDao().deleteMcqById(id)
            }
        }
        if (item != null) {
            repositoryScope.launch {
                try {
                    val key = item.r2Key.ifBlank { buildMcqR2Key(item.folderName, item.subfolderName, item.subSubfolderName, item.chapterName) }
                    com.example.data.cloud.CloudflareR2Manager.deleteFile(key)
                } catch (_: Exception) {}
            }
        }
        triggerCatalogPublish("mcqItems")
    }

    suspend fun loadMcqQuestionsIfNeeded(chapter: McqItem): List<McqQuestion> = withContext(Dispatchers.IO) {
        if (chapter.questions.isNotEmpty()) {
            return@withContext chapter.questions
        }
        val key = chapter.r2Key.ifBlank {
            buildMcqR2Key(chapter.folderName, chapter.subfolderName, chapter.subSubfolderName, chapter.chapterName)
        }
        val res = com.example.data.cloud.CloudflareR2Manager.downloadString(key)
        if (res.isSuccess) {
            val jsonStr = res.getOrNull().orEmpty()
            if (jsonStr.isNotBlank()) {
                try {
                    val parsed = Json { ignoreUnknownKeys = true; isLenient = true }.decodeFromString<List<McqQuestion>>(jsonStr)
                    val updated = chapter.copy(questions = parsed, questionCount = parsed.size, r2Key = key)
                    _mcqItems.update { list ->
                        list.map { if (it.id == chapter.id) updated else it }
                    }
                    database?.mcqDao()?.insertMcq(McqEntity.fromDomain(updated))
                    return@withContext parsed
                } catch (e: Exception) {
                    android.util.Log.e("MedRepository", "Failed to parse loaded questions for $key", e)
                }
            }
        }
        return@withContext emptyList()
    }

    fun toggleBookmark(chapterName: String, folderName: String, subfolderName: String, question: McqQuestion) {
        val existing = _mcqBookmarks.value.firstOrNull { it.question.id == question.id }
        if (existing != null) {
            _mcqBookmarks.update { list -> list.filterNot { it.id == existing.id } }
            database?.let { db ->
                repositoryScope.launch {
                    db.mcqBookmarkDao().deleteBookmark(existing.id)
                }
            }
        } else {
            val item = McqBookmarkItem(
                chapterName = chapterName,
                folderName = folderName,
                subfolderName = subfolderName,
                question = question
            )
            _mcqBookmarks.update { listOf(item) + it }
            database?.let { db ->
                repositoryScope.launch {
                    db.mcqBookmarkDao().insertBookmark(McqBookmarkEntity.fromDomain(item))
                }
            }
        }
    }

    fun isBookmarked(questionId: String): Boolean {
        return _mcqBookmarks.value.any { it.question.id == questionId }
    }

    fun deleteBookmark(id: String) {
        _mcqBookmarks.update { list -> list.filterNot { it.id == id } }
        database?.let { db ->
            repositoryScope.launch {
                db.mcqBookmarkDao().deleteBookmark(id)
            }
        }
    }

    fun saveTestHistory(history: McqHistoryItem) {
        _mcqHistory.update { listOf(history) + it }
        database?.let { db ->
            repositoryScope.launch {
                db.mcqHistoryDao().insertHistory(McqHistoryEntity.fromDomain(history))
            }
        }
    }

    fun deleteHistory(id: String) {
        _mcqHistory.update { list -> list.filterNot { it.id == id } }
        database?.let { db ->
            repositoryScope.launch {
                db.mcqHistoryDao().deleteHistory(id)
            }
        }
    }

    fun clearAllHistory() {
        _mcqHistory.value = emptyList()
        database?.let { db ->
            repositoryScope.launch {
                db.mcqHistoryDao().clearAllHistory()
            }
        }
    }


    fun triggerCatalogPublish(section: String = "all") {
        repositoryScope.launch {
            try {
                val currentTombstones = getTombstones()
                val tombstonesJson = Json.encodeToString(currentTombstones)
                com.example.data.cloud.CloudflareR2Manager.uploadString("catalog_tombstones.json", tombstonesJson)

                if (section == "all" || section == "adminNotes") {
                    val notesJson = Json.encodeToString(Catalog(adminNotes = _adminNotes.value, tombstones = currentTombstones))
                    com.example.data.cloud.CloudflareR2Manager.uploadString("catalog_adminNotes.json", notesJson)
                }
                if (section == "all" || section == "mcqItems") {
                    val mcqJson = Json.encodeToString(Catalog(mcqItems = _mcqItems.value, tombstones = currentTombstones))
                    com.example.data.cloud.CloudflareR2Manager.uploadString("catalog_mcqItems.json", mcqJson)
                }
                if (section == "all" || section == "shorts") {
                    val shortsJson = Json.encodeToString(Catalog(shorts = _shorts.value, tombstones = currentTombstones))
                    com.example.data.cloud.CloudflareR2Manager.uploadString("catalog_shorts.json", shortsJson)
                    com.example.data.cloud.CloudflareR2Manager.uploadString("shorts/catalog.json", shortsJson)
                }
                if (section == "all" || section == "contents" || section == "folders") {
                    val contentsJson = Json.encodeToString(Catalog(contents = _contents.value, folders = _folders.value, tombstones = currentTombstones))
                    com.example.data.cloud.CloudflareR2Manager.uploadString("catalog_contents.json", contentsJson)
                    com.example.data.cloud.CloudflareR2Manager.uploadString("catalog_folders.json", contentsJson)
                }

                val fullCatalog = Json.encodeToString(Catalog(
                    adminNotes = _adminNotes.value,
                    mcqItems = _mcqItems.value,
                    shorts = _shorts.value,
                    folders = _folders.value,
                    contents = _contents.value,
                    tombstones = currentTombstones
                ))
                com.example.data.cloud.CloudflareR2Manager.uploadString("catalog-index.json", fullCatalog)
            } catch (e: Exception) {
                android.util.Log.e("MedRepository", "triggerCatalogPublish error", e)
            }
        }
    }


    fun recordDeletedFolder(folderId: String, folderName: String) {
        sharedPrefs?.let { prefs ->
            val deletedFolderIds = prefs.getStringSet("deleted_folder_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            val deletedFolderNames = prefs.getStringSet("deleted_folder_names", emptySet())?.toMutableSet() ?: mutableSetOf()
            if (folderId.isNotBlank()) deletedFolderIds.add(folderId)
            if (folderName.isNotBlank()) deletedFolderNames.add(folderName.trim().lowercase())
            prefs.edit()
                .putStringSet("deleted_folder_ids", deletedFolderIds)
                .putStringSet("deleted_folder_names", deletedFolderNames)
                .apply()
        }
    }

    fun recordDeletedContent(contentId: String) {
        sharedPrefs?.let { prefs ->
            val deletedContentIds = prefs.getStringSet("deleted_content_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            if (contentId.isNotBlank()) deletedContentIds.add(contentId)
            prefs.edit()
                .putStringSet("deleted_content_ids", deletedContentIds)
                .apply()
        }
    }

    fun recordDeletedShort(shortId: String) {
        sharedPrefs?.let { prefs ->
            val deletedShortIds = prefs.getStringSet("deleted_short_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            if (shortId.isNotBlank()) deletedShortIds.add(shortId)
            prefs.edit()
                .putStringSet("deleted_short_ids", deletedShortIds)
                .apply()
        }
    }

    fun recordDeletedAdminNote(noteId: String) {
        sharedPrefs?.let { prefs ->
            val deletedAdminNoteIds = prefs.getStringSet("deleted_admin_note_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            if (noteId.isNotBlank()) deletedAdminNoteIds.add(noteId)
            prefs.edit()
                .putStringSet("deleted_admin_note_ids", deletedAdminNoteIds)
                .apply()
        }
    }

    fun recordDeletedMcq(mcqId: String) {
        sharedPrefs?.let { prefs ->
            val deletedMcqIds = prefs.getStringSet("deleted_mcq_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            if (mcqId.isNotBlank()) deletedMcqIds.add(mcqId)
            prefs.edit()
                .putStringSet("deleted_mcq_ids", deletedMcqIds)
                .apply()
        }
    }

    fun isFolderDeleted(folderId: String, folderName: String): Boolean {
        val prefs = sharedPrefs ?: return false
        val deletedFolderIds = prefs.getStringSet("deleted_folder_ids", emptySet()) ?: emptySet()
        val deletedFolderNames = prefs.getStringSet("deleted_folder_names", emptySet()) ?: emptySet()
        return (folderId.isNotBlank() && deletedFolderIds.contains(folderId)) ||
                (folderName.isNotBlank() && deletedFolderNames.contains(folderName.trim().lowercase()))
    }

    fun isContentDeleted(contentId: String): Boolean {
        val prefs = sharedPrefs ?: return false
        val deletedContentIds = prefs.getStringSet("deleted_content_ids", emptySet()) ?: emptySet()
        return contentId.isNotBlank() && deletedContentIds.contains(contentId)
    }

    fun isShortDeleted(shortId: String): Boolean {
        val prefs = sharedPrefs ?: return false
        val deletedShortIds = prefs.getStringSet("deleted_short_ids", emptySet()) ?: emptySet()
        return shortId.isNotBlank() && deletedShortIds.contains(shortId)
    }

    fun isAdminNoteDeleted(noteId: String): Boolean {
        val prefs = sharedPrefs ?: return false
        val deletedAdminNoteIds = prefs.getStringSet("deleted_admin_note_ids", emptySet()) ?: emptySet()
        return noteId.isNotBlank() && deletedAdminNoteIds.contains(noteId)
    }

    fun isMcqDeleted(mcqId: String): Boolean {
        val prefs = sharedPrefs ?: return false
        val deletedMcqIds = prefs.getStringSet("deleted_mcq_ids", emptySet()) ?: emptySet()
        return mcqId.isNotBlank() && deletedMcqIds.contains(mcqId)
    }

    fun getTombstones(): CatalogTombstones {
        val prefs = sharedPrefs ?: return CatalogTombstones()
        return CatalogTombstones(
            folderIds = prefs.getStringSet("deleted_folder_ids", emptySet()) ?: emptySet(),
            folderNames = prefs.getStringSet("deleted_folder_names", emptySet()) ?: emptySet(),
            contentIds = prefs.getStringSet("deleted_content_ids", emptySet()) ?: emptySet(),
            shortIds = prefs.getStringSet("deleted_short_ids", emptySet()) ?: emptySet(),
            adminNoteIds = prefs.getStringSet("deleted_admin_note_ids", emptySet()) ?: emptySet(),
            mcqIds = prefs.getStringSet("deleted_mcq_ids", emptySet()) ?: emptySet()
        )
    }

    fun mergeTombstones(cloud: CatalogTombstones) {
        val prefs = sharedPrefs ?: return
        val editor = prefs.edit()
        if (cloud.folderIds.isNotEmpty()) {
            val local = prefs.getStringSet("deleted_folder_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            local.addAll(cloud.folderIds)
            editor.putStringSet("deleted_folder_ids", local)
        }
        if (cloud.folderNames.isNotEmpty()) {
            val local = prefs.getStringSet("deleted_folder_names", emptySet())?.toMutableSet() ?: mutableSetOf()
            local.addAll(cloud.folderNames.map { it.trim().lowercase() })
            editor.putStringSet("deleted_folder_names", local)
        }
        if (cloud.contentIds.isNotEmpty()) {
            val local = prefs.getStringSet("deleted_content_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            local.addAll(cloud.contentIds)
            editor.putStringSet("deleted_content_ids", local)
        }
        if (cloud.shortIds.isNotEmpty()) {
            val local = prefs.getStringSet("deleted_short_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            local.addAll(cloud.shortIds)
            editor.putStringSet("deleted_short_ids", local)
        }
        if (cloud.adminNoteIds.isNotEmpty()) {
            val local = prefs.getStringSet("deleted_admin_note_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            local.addAll(cloud.adminNoteIds)
            editor.putStringSet("deleted_admin_note_ids", local)
        }
        if (cloud.mcqIds.isNotEmpty()) {
            val local = prefs.getStringSet("deleted_mcq_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            local.addAll(cloud.mcqIds)
            editor.putStringSet("deleted_mcq_ids", local)
        }
        editor.apply()
    }

    fun clearDeletedRecord(id: String, name: String = "") {
        sharedPrefs?.let { prefs ->
            val editor = prefs.edit()
            val deletedFolderIds = prefs.getStringSet("deleted_folder_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            val deletedFolderNames = prefs.getStringSet("deleted_folder_names", emptySet())?.toMutableSet() ?: mutableSetOf()
            val deletedContentIds = prefs.getStringSet("deleted_content_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            val deletedShortIds = prefs.getStringSet("deleted_short_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            val deletedAdminNoteIds = prefs.getStringSet("deleted_admin_note_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            val deletedMcqIds = prefs.getStringSet("deleted_mcq_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

            deletedFolderIds.remove(id)
            if (name.isNotBlank()) deletedFolderNames.remove(name.trim().lowercase())
            deletedContentIds.remove(id)
            deletedShortIds.remove(id)
            deletedAdminNoteIds.remove(id)
            deletedMcqIds.remove(id)

            editor.putStringSet("deleted_folder_ids", deletedFolderIds)
            editor.putStringSet("deleted_folder_names", deletedFolderNames)
            editor.putStringSet("deleted_content_ids", deletedContentIds)
            editor.putStringSet("deleted_short_ids", deletedShortIds)
            editor.putStringSet("deleted_admin_note_ids", deletedAdminNoteIds)
            editor.putStringSet("deleted_mcq_ids", deletedMcqIds)
            editor.apply()
        }
    }

    fun createFolder(folder: ContentFolder) {
        clearDeletedRecord(folder.id, folder.name)
        _folders.update { current ->
            if (current.any { it.id == folder.id || (it.name.equals(folder.name, ignoreCase = true) && it.subjectId == folder.subjectId) }) {
                current
            } else {
                current + folder
            }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.folderDao().insertFolder(ContentFolderEntity.fromDomain(folder))
                triggerCatalogPublish()
            }
        }
    }

    fun renameFolder(folderId: String, newName: String, newEmoji: String) {
        _folders.update { list ->
            list.map {
                if (it.id == folderId) it.copy(name = newName, emoji = newEmoji) else it
            }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.folderDao().renameFolder(folderId, newName, newEmoji)
                triggerCatalogPublish()
            }
        }
    }

    fun deleteR2AssetIfPresent(url: String?) {
        if (url.isNullOrBlank() || !com.example.data.cloud.CloudflareR2Manager.isConfigured()) return
        repositoryScope.launch(Dispatchers.IO) {
            try {
                val baseUrl = com.example.data.cloud.CloudflareR2Manager.publicBaseUrl.value.trimEnd('/')
                if (baseUrl.isNotBlank() && url.startsWith(baseUrl)) {
                    val key = url.removePrefix(baseUrl).trimStart('/')
                    if (key.isNotBlank()) {
                        com.example.data.cloud.CloudflareR2Manager.deleteFile(key)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteFolder(folderId: String, folderName: String = "", triggerPublish: Boolean = true) {
        recordDeletedFolder(folderId, folderName)
        val nameToMatch = folderName.trim().lowercase()

        val matchingContents = _contents.value.filter {
            it.folderId == folderId || (nameToMatch.isNotBlank() && it.folderName.trim().lowercase() == nameToMatch)
        }
        matchingContents.forEach { item ->
            deleteR2AssetIfPresent(item.mediaUrl)
            deleteR2AssetIfPresent(item.cloudflareKey)
            recordDeletedContent(item.id)
        }

        _folders.update { list ->
            list.filterNot { it.id == folderId || (nameToMatch.isNotBlank() && it.name.trim().lowercase() == nameToMatch) }
        }
        _contents.update { list ->
            list.filterNot { it.folderId == folderId || (nameToMatch.isNotBlank() && it.folderName.trim().lowercase() == nameToMatch) }
        }

        database?.let { db ->
            repositoryScope.launch {
                db.folderDao().deleteFolderByIdOrName(folderId, folderName)
                db.studyContentDao().deleteContentsByFolderIdOrName(folderId, folderName)
                if (triggerPublish) triggerCatalogPublish()
            }
        }
    }

    fun deleteAllFolders() {
        _folders.value.forEach { recordDeletedFolder(it.id, it.name) }
        _folders.value = emptyList()
        database?.let { db ->
            repositoryScope.launch {
                db.folderDao().clearAllFolders()
                triggerCatalogPublish()
            }
        }
    }

    fun addStudyContent(item: StudyContentItem) {
        clearDeletedRecord(item.id)
        _contents.update { listOf(item) + it }
        database?.let { db ->
            repositoryScope.launch {
                db.studyContentDao().insertContent(StudyContentEntity.fromDomain(item))
                triggerCatalogPublish()
            }
        }
    }

    fun editStudyContent(id: String, newTitle: String, newDesc: String) {
        _contents.update { list ->
            list.map { if (it.id == id) it.copy(title = newTitle, description = newDesc) else it }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.studyContentDao().updateContent(id, newTitle, newDesc)
                triggerCatalogPublish()
            }
        }
    }

    fun updateStudyContentMediaUrl(id: String, newUrl: String) {
        _contents.update { list ->
            list.map { if (it.id == id) it.copy(mediaUrl = newUrl, cloudflareKey = newUrl) else it }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.studyContentDao().updateMediaUrl(id, newUrl)
                triggerCatalogPublish()
            }
        }
    }

    fun deleteStudyContent(item: StudyContentItem) {
        recordDeletedContent(item.id)
        deleteR2AssetIfPresent(item.mediaUrl)
        deleteR2AssetIfPresent(item.cloudflareKey)
        _contents.update { it.filterNot { c -> c.id == item.id } }
        database?.let { db ->
            repositoryScope.launch {
                db.studyContentDao().deleteContentById(item.id)
                triggerCatalogPublish()
            }
        }
    }

    fun deleteStudyContent(id: String) {
        recordDeletedContent(id)
        val item = _contents.value.firstOrNull { it.id == id }
        if (item != null) {
            deleteR2AssetIfPresent(item.mediaUrl)
            deleteR2AssetIfPresent(item.cloudflareKey)
        }
        _contents.update { it.filterNot { c -> c.id == id } }
        database?.let { db ->
            repositoryScope.launch {
                db.studyContentDao().deleteContentById(id)
                triggerCatalogPublish()
            }
        }
    }

    fun renameMbbsFolder(prof: ProfYear, subjectId: String, oldFolder: String, newFolder: String, newEmoji: String = "📁") {
        val trimmedOld = oldFolder.trim()
        val trimmedNew = newFolder.trim()
        if (trimmedOld.isBlank() || trimmedNew.isBlank()) return

        _folders.update { list ->
            list.map {
                if (it.prof == prof && it.subjectId == subjectId && it.name.equals(trimmedOld, ignoreCase = true)) {
                    it.copy(name = trimmedNew, emoji = newEmoji)
                } else it
            }
        }
        _contents.update { list ->
            list.map {
                if (it.prof == prof && it.subjectId == subjectId && it.folderName.equals(trimmedOld, ignoreCase = true)) {
                    it.copy(folderName = trimmedNew)
                } else it
            }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.studyContentDao().clearAllContents()
                db.studyContentDao().insertContents(_contents.value.map { StudyContentEntity.fromDomain(it) })
                db.folderDao().clearAllFolders()
                db.folderDao().insertFolders(_folders.value.map { ContentFolderEntity.fromDomain(it) })
                triggerCatalogPublish("contents")
            }
        }
    }

    fun deleteMbbsFolder(prof: ProfYear, subjectId: String, folderName: String) {
        val trimmedFolder = folderName.trim()
        val matchingFolders = _folders.value.filter { it.prof == prof && it.subjectId == subjectId && it.name.equals(trimmedFolder, ignoreCase = true) }
        matchingFolders.forEach { recordDeletedFolder(it.id, it.name) }
        if (matchingFolders.isEmpty()) {
            recordDeletedFolder("", trimmedFolder)
        }
        val toDelete = _contents.value.filter {
            it.prof == prof && it.subjectId == subjectId && it.folderName.equals(trimmedFolder, ignoreCase = true)
        }
        toDelete.forEach { item ->
            deleteR2AssetIfPresent(item.mediaUrl)
            deleteR2AssetIfPresent(item.cloudflareKey)
            recordDeletedContent(item.id)
        }
        _folders.update { list ->
            list.filterNot { it.prof == prof && it.subjectId == subjectId && it.name.equals(trimmedFolder, ignoreCase = true) }
        }
        _contents.update { list ->
            list.filterNot { it.prof == prof && it.subjectId == subjectId && it.folderName.equals(trimmedFolder, ignoreCase = true) }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.studyContentDao().clearAllContents()
                db.studyContentDao().insertContents(_contents.value.map { StudyContentEntity.fromDomain(it) })
                db.folderDao().clearAllFolders()
                db.folderDao().insertFolders(_folders.value.map { ContentFolderEntity.fromDomain(it) })
                triggerCatalogPublish("contents")
            }
        }
    }

    fun renameMbbsSubfolder(prof: ProfYear, subjectId: String, folderName: String, oldSub: String, newSub: String) {
        val trimmedOld = oldSub.trim()
        val trimmedNew = newSub.trim()
        _contents.update { list ->
            list.map {
                if (it.prof == prof && it.subjectId == subjectId &&
                    it.folderName.equals(folderName.trim(), ignoreCase = true) &&
                    it.subfolderName.equals(trimmedOld, ignoreCase = true)) {
                    it.copy(subfolderName = trimmedNew)
                } else it
            }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.studyContentDao().clearAllContents()
                db.studyContentDao().insertContents(_contents.value.map { StudyContentEntity.fromDomain(it) })
                triggerCatalogPublish("contents")
            }
        }
    }

    fun deleteMbbsSubfolder(prof: ProfYear, subjectId: String, folderName: String, subfolderName: String) {
        val trimmedSub = subfolderName.trim()
        val toDelete = _contents.value.filter {
            it.prof == prof && it.subjectId == subjectId &&
            it.folderName.equals(folderName.trim(), ignoreCase = true) &&
            it.subfolderName.equals(trimmedSub, ignoreCase = true)
        }
        toDelete.forEach { item ->
            deleteR2AssetIfPresent(item.mediaUrl)
            deleteR2AssetIfPresent(item.cloudflareKey)
            recordDeletedContent(item.id)
        }
        _contents.update { list ->
            list.filterNot {
                it.prof == prof && it.subjectId == subjectId &&
                it.folderName.equals(folderName.trim(), ignoreCase = true) &&
                it.subfolderName.equals(trimmedSub, ignoreCase = true)
            }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.studyContentDao().clearAllContents()
                db.studyContentDao().insertContents(_contents.value.map { StudyContentEntity.fromDomain(it) })
                triggerCatalogPublish("contents")
            }
        }
    }

    fun renameMbbsChapter(prof: ProfYear, subjectId: String, folderName: String, subfolderName: String, oldChapter: String, newChapter: String) {
        val trimmedOld = oldChapter.trim()
        val trimmedNew = newChapter.trim()
        _contents.update { list ->
            list.map {
                val matchesFolder = it.folderName.equals(folderName.trim(), ignoreCase = true)
                val matchesSub = subfolderName.isBlank() || it.subfolderName.equals(subfolderName.trim(), ignoreCase = true)
                val matchesChapter = it.chapterName.equals(trimmedOld, ignoreCase = true)
                if (it.prof == prof && it.subjectId == subjectId && matchesFolder && matchesSub && matchesChapter) {
                    it.copy(chapterName = trimmedNew)
                } else it
            }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.studyContentDao().clearAllContents()
                db.studyContentDao().insertContents(_contents.value.map { StudyContentEntity.fromDomain(it) })
                triggerCatalogPublish("contents")
            }
        }
    }

    fun deleteMbbsChapter(prof: ProfYear, subjectId: String, folderName: String, subfolderName: String, chapterName: String) {
        val trimmedChapter = chapterName.trim()
        val toDelete = _contents.value.filter {
            val matchesFolder = it.folderName.equals(folderName.trim(), ignoreCase = true)
            val matchesSub = subfolderName.isBlank() || it.subfolderName.equals(subfolderName.trim(), ignoreCase = true)
            val matchesChapter = it.chapterName.equals(trimmedChapter, ignoreCase = true)
            it.prof == prof && it.subjectId == subjectId && matchesFolder && matchesSub && matchesChapter
        }
        toDelete.forEach { item ->
            deleteR2AssetIfPresent(item.mediaUrl)
            deleteR2AssetIfPresent(item.cloudflareKey)
            recordDeletedContent(item.id)
        }
        _contents.update { list ->
            list.filterNot {
                val matchesFolder = it.folderName.equals(folderName.trim(), ignoreCase = true)
                val matchesSub = subfolderName.isBlank() || it.subfolderName.equals(subfolderName.trim(), ignoreCase = true)
                val matchesChapter = it.chapterName.equals(trimmedChapter, ignoreCase = true)
                it.prof == prof && it.subjectId == subjectId && matchesFolder && matchesSub && matchesChapter
            }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.studyContentDao().clearAllContents()
                db.studyContentDao().insertContents(_contents.value.map { StudyContentEntity.fromDomain(it) })
                triggerCatalogPublish("contents")
            }
        }
    }

    fun addNote(item: NoteItem) {
        _notes.update { listOf(item) + it }
        database?.let { db ->
            repositoryScope.launch {
                db.noteDao().insertNote(NoteEntity.fromDomain(item))
            }
        }
    }

    fun addLecture(item: LectureItem) {
        _lectures.update { listOf(item) + it }
        database?.let { db ->
            repositoryScope.launch {
                db.lectureDao().insertLecture(LectureEntity.fromDomain(item))
            }
        }
    }

    fun addShort(item: ShortItem) {
        clearDeletedRecord(item.id)
        _shorts.update { listOf(item) + it }
        database?.let { db ->
            repositoryScope.launch {
                db.shortDao().insertShort(ShortEntity.fromDomain(item))
            }
        }
        triggerCatalogPublish("shorts")
    }

    fun editShort(id: String, newTitle: String, newDesc: String) {
        _shorts.update { list ->
            list.map { if (it.id == id) it.copy(title = newTitle, description = newDesc) else it }
        }
        database?.let { db ->
            repositoryScope.launch {
                db.shortDao().updateShort(id, newTitle, newDesc)
            }
        }
        triggerCatalogPublish("shorts")
    }

    fun deleteShort(id: String) {
        recordDeletedShort(id)
        _shorts.update { it.filterNot { s -> s.id == id } }
        database?.let { db ->
            repositoryScope.launch {
                db.shortDao().deleteShortById(id)
            }
        }
        triggerCatalogPublish("shorts")
    }

    fun buildCloudflareShortUrl(shortId: String, videoId: String): String {
        val cleanShortId = shortId.trim().replace(" ", "_")
        val cleanVideoId = videoId.trim().replace(" ", "_")
        val baseUrl = com.example.data.cloud.CloudflareR2Manager.publicBaseUrl.value.trimEnd('/')
        return "$baseUrl/shorts/$cleanShortId/$cleanVideoId.mp4"
    }

    fun deleteItem(type: String, id: String) {
        when (type.lowercase()) {
            "short" -> {
                recordDeletedShort(id)
                _shorts.update { it.filterNot { item -> item.id == id } }
                database?.let { repositoryScope.launch { it.shortDao().deleteShortById(id); triggerCatalogPublish() } }
            }
            "folder" -> {
                deleteFolder(id)
            }
            "note", "content", "study_content" -> {
                recordDeletedContent(id)
                _contents.update { it.filterNot { item -> item.id == id } }
                database?.let { repositoryScope.launch { it.studyContentDao().deleteContentById(id); triggerCatalogPublish() } }
            }
        }
    }

    fun getSubjectsForProf(prof: ProfYear): List<Subject> {
        return allSubjects.filter { it.prof == prof }
    }

    fun addCustomSection(section: CustomSection) {
        _customSections.update { list ->
            if (list.any { it.id == section.id }) list else list + section
        }
    }

    fun updateCustomSection(section: CustomSection) {
        _customSections.update { list ->
            list.map { if (it.id == section.id) section else it }
        }
    }

    fun deleteCustomSection(sectionId: String) {
        _customSections.update { list -> list.filterNot { it.id == sectionId } }
        _folders.update { list -> list.filterNot { it.subjectId == sectionId } }
        _contents.update { list -> list.filterNot { it.subjectId == sectionId } }
        database?.let { db ->
            repositoryScope.launch {
                db.folderDao().deleteFoldersBySubjectId(sectionId)
                db.studyContentDao().deleteContentsBySubjectId(sectionId)
                triggerCatalogPublish()
            }
        }
    }

    fun updateCustomSections(sections: List<CustomSection>) {
        _customSections.value = sections
    }

    fun deleteAllCustomSections() {
        val secIds = _customSections.value.map { it.id }
        _customSections.value = emptyList()
        _folders.update { it.filterNot { f -> secIds.contains(f.subjectId) } }
        _contents.update { it.filterNot { c -> secIds.contains(c.subjectId) } }
        database?.let { db ->
            repositoryScope.launch {
                secIds.forEach {
                    db.folderDao().deleteFoldersBySubjectId(it)
                    db.studyContentDao().deleteContentsBySubjectId(it)
                }
                triggerCatalogPublish()
            }
        }
    }

    fun purgeAdminExcludedData() {
        deleteAllCustomSections()
        triggerCatalogPublish()
    }

    fun masterDeleteAll() {
        val allContentList = _contents.value
        allContentList.forEach { item ->
            deleteR2AssetIfPresent(item.mediaUrl)
            deleteR2AssetIfPresent(item.cloudflareKey)
            recordDeletedContent(item.id)
        }
        _folders.value.forEach { recordDeletedFolder(it.id, it.name) }
        _shorts.value.forEach { recordDeletedShort(it.id) }

        _contents.value = emptyList()
        _folders.value = emptyList()
        _notes.value = emptyList()
        _cachedOfflineNotes.value = emptyList()
        _lectures.value = emptyList()
        _shorts.value = emptyList()
        _customSections.value = emptyList()

        database?.let { db ->
            repositoryScope.launch {
                db.studyContentDao().clearAllContents()
                db.folderDao().clearAllFolders()
                db.noteDao().clearAllNotes()
                db.lectureDao().clearAllLectures()
                db.shortDao().clearAllShorts()
                db.pomodoroSessionDao().clearAllSessions()
            }
        }
    }

    fun fetchCatalog() {
        repositoryScope.launch {
            try {
                // 1. Download & merge global tombstones from Cloudflare R2
                val tombRes = com.example.data.cloud.CloudflareR2Manager.downloadString("catalog_tombstones.json")
                if (tombRes.isSuccess) {
                    val tombJson = tombRes.getOrNull()
                    if (!tombJson.isNullOrBlank()) {
                        try {
                            val parsedTombstones = Json { ignoreUnknownKeys = true; isLenient = true }.decodeFromString<CatalogTombstones>(tombJson)
                            mergeTombstones(parsedTombstones)
                        } catch (e: Exception) {
                            android.util.Log.w("MedRepository", "Could not parse catalog_tombstones.json", e)
                        }
                    }
                }
            } catch (_: Exception) {}

            val resIndex = com.example.data.cloud.CloudflareR2Manager.downloadString("catalog-index.json")
            if (resIndex.isSuccess) {
                val jsonStr = resIndex.getOrNull()
                if (!jsonStr.isNullOrBlank()) {
                    try {
                        val catalog = Json { ignoreUnknownKeys = true; isLenient = true }.decodeFromString<Catalog>(jsonStr)
                        catalog.tombstones?.let { mergeTombstones(it) }

                        // 1. Filter and update Admin Notes
                        val activeAdminNotes = catalog.adminNotes.filterNot { isAdminNoteDeleted(it.id) }
                        _adminNotes.value = activeAdminNotes
                        database?.let { db ->
                            db.adminNoteDao().clearAllAdminNotes()
                            db.adminNoteDao().insertAdminNotes(activeAdminNotes.map { AdminNoteEntity.fromDomain(it) })
                        }

                        // 2. Filter and update MCQ Items
                        val activeMcqItems = catalog.mcqItems.filterNot { isMcqDeleted(it.id) }
                        _mcqItems.value = activeMcqItems
                        database?.let { db ->
                            db.mcqDao().clearAllMcqs()
                            db.mcqDao().insertMcqs(activeMcqItems.map { McqEntity.fromDomain(it) })
                        }

                        // 3. Filter and update Folders
                        val activeFolders = catalog.folders.filterNot { isFolderDeleted(it.id, it.name) }
                        _folders.value = activeFolders
                        database?.let { db ->
                            db.folderDao().clearAllFolders()
                            db.folderDao().insertFolders(activeFolders.map { ContentFolderEntity.fromDomain(it) })
                        }

                        // 4. Filter and update Contents
                        val activeContents = catalog.contents.filterNot {
                            isContentDeleted(it.id) || isFolderDeleted(it.folderId, it.folderName)
                        }
                        _contents.value = activeContents
                        database?.let { db ->
                            db.studyContentDao().clearAllContents()
                            db.studyContentDao().insertContents(activeContents.map { StudyContentEntity.fromDomain(it) })
                        }

                        // 5. Filter and update Shorts
                        val activeShorts = catalog.shorts.filterNot { isShortDeleted(it.id) }
                        _shorts.value = activeShorts
                        database?.let { db ->
                            db.shortDao().clearAllShorts()
                            db.shortDao().insertShorts(activeShorts.map { ShortEntity.fromDomain(it) })
                        }

                        return@launch
                    } catch (e: Exception) {
                        android.util.Log.e("MedRepository", "Failed to decode catalog-index.json", e)
                    }
                }
            }

            // Fallback to individual catalogs if catalog-index.json is unavailable
            android.util.Log.w("MedRepository", "catalog-index.json not found, falling back to legacy individual catalogs")

            // Fallback 1: Admin Notes
            try {
                val resNotes = com.example.data.cloud.CloudflareR2Manager.downloadString("catalog_adminNotes.json")
                if (resNotes.isSuccess) {
                    val jsonStr = resNotes.getOrNull()
                    if (!jsonStr.isNullOrBlank()) {
                        val catalog = Json { ignoreUnknownKeys = true; isLenient = true }.decodeFromString<Catalog>(jsonStr)
                        catalog.tombstones?.let { mergeTombstones(it) }
                        val activeNotes = catalog.adminNotes.filterNot { isAdminNoteDeleted(it.id) }
                        _adminNotes.value = activeNotes
                        database?.let { db ->
                            db.adminNoteDao().clearAllAdminNotes()
                            db.adminNoteDao().insertAdminNotes(activeNotes.map { AdminNoteEntity.fromDomain(it) })
                        }
                    }
                }
            } catch (_: Exception) {}

            // Fallback 2: MCQ Items
            try {
                val resMcq = com.example.data.cloud.CloudflareR2Manager.downloadString("catalog_mcqItems.json")
                if (resMcq.isSuccess) {
                    val jsonStr = resMcq.getOrNull()
                    if (!jsonStr.isNullOrBlank()) {
                        val catalog = Json { ignoreUnknownKeys = true; isLenient = true }.decodeFromString<Catalog>(jsonStr)
                        catalog.tombstones?.let { mergeTombstones(it) }
                        val activeMcqs = catalog.mcqItems.filterNot { isMcqDeleted(it.id) }
                        _mcqItems.value = activeMcqs
                        database?.let { db ->
                            db.mcqDao().clearAllMcqs()
                            db.mcqDao().insertMcqs(activeMcqs.map { McqEntity.fromDomain(it) })
                        }
                    }
                }
            } catch (_: Exception) {}

            // Fallback 3: Shorts (tries shorts/catalog.json inside shorts folder, then catalog_shorts.json)
            try {
                var resShorts = com.example.data.cloud.CloudflareR2Manager.downloadString("shorts/catalog.json")
                if (resShorts.isFailure || resShorts.getOrNull().isNullOrBlank()) {
                    resShorts = com.example.data.cloud.CloudflareR2Manager.downloadString("catalog_shorts.json")
                }
                if (resShorts.isSuccess) {
                    val jsonStr = resShorts.getOrNull()
                    if (!jsonStr.isNullOrBlank()) {
                        val catalog = Json { ignoreUnknownKeys = true; isLenient = true }.decodeFromString<Catalog>(jsonStr)
                        catalog.tombstones?.let { mergeTombstones(it) }
                        val activeShorts = catalog.shorts.filterNot { isShortDeleted(it.id) }
                        _shorts.value = activeShorts
                        database?.let { db ->
                            db.shortDao().clearAllShorts()
                            db.shortDao().insertShorts(activeShorts.map { ShortEntity.fromDomain(it) })
                        }
                    }
                }
            } catch (_: Exception) {}

            // Fallback 4: Contents & Folders
            try {
                val resContents = com.example.data.cloud.CloudflareR2Manager.downloadString("catalog_contents.json")
                if (resContents.isSuccess) {
                    val jsonStr = resContents.getOrNull()
                    if (!jsonStr.isNullOrBlank()) {
                        val catalog = Json { ignoreUnknownKeys = true; isLenient = true }.decodeFromString<Catalog>(jsonStr)
                        catalog.tombstones?.let { mergeTombstones(it) }
                        val activeFolders = catalog.folders.filterNot { isFolderDeleted(it.id, it.name) }
                        val activeContents = catalog.contents.filterNot { isContentDeleted(it.id) || isFolderDeleted(it.folderId, it.folderName) }
                        _folders.value = activeFolders
                        _contents.value = activeContents
                        database?.let { db ->
                            db.folderDao().clearAllFolders()
                            db.folderDao().insertFolders(activeFolders.map { ContentFolderEntity.fromDomain(it) })
                            db.studyContentDao().clearAllContents()
                            db.studyContentDao().insertContents(activeContents.map { StudyContentEntity.fromDomain(it) })
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }
}

