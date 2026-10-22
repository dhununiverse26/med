package com.example.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class ProfYear(
    val title: String,
    val subtitle: String,
    val code: String,
    val iconEmoji: String,
    val gradientColors: List<Color>
) {
    FIRST_PROF(
        title = "1ST PROF",
        subtitle = "PRE-CLINICAL (1ST YEAR)",
        code = "prof1",
        iconEmoji = "🦴",
        gradientColors = listOf(Color(0xFF0284C7), Color(0xFF0369A1))
    ),
    SECOND_PROF(
        title = "2ND PROF",
        subtitle = "PARA-CLINICAL (2ND YEAR)",
        code = "prof2",
        iconEmoji = "💊",
        gradientColors = listOf(Color(0xFF15803D), Color(0xFF166534))
    ),
    THIRD_PROF_1(
        title = "3RD PROF I",
        subtitle = "CLINICAL PART 1 (3RD YEAR)",
        code = "prof3_1",
        iconEmoji = "🩺",
        gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))
    ),
    THIRD_PROF_2(
        title = "3RD PROF II",
        subtitle = "FINAL CLINICAL",
        code = "prof3_2",
        iconEmoji = "🫀",
        gradientColors = listOf(Color(0xFFE11D48), Color(0xFF9F1239))
    );

    val gradientBrush: Brush
        get() = Brush.verticalGradient(gradientColors)
}

data class Subject(
    val id: String,
    val name: String,
    val emoji: String,
    val prof: ProfYear,
    val description: String,
    val totalChapters: Int,
    val highYieldTopicCount: Int
)

@Serializable
data class CustomSection(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val emoji: String = "📚",
    val colorHex1: String = "#0284C7",
    val colorHex2: String = "#0369A1",
    val description: String = "",
    val orderIndex: Int = 0
) {
    val gradientColors: List<Color>
        get() = try {
            listOf(
                Color(android.graphics.Color.parseColor(colorHex1)),
                Color(android.graphics.Color.parseColor(colorHex2))
            )
        } catch (e: Exception) {
            listOf(Color(0xFF0284C7), Color(0xFF0369A1))
        }

    val gradientBrush: Brush
        get() = Brush.verticalGradient(gradientColors)
}

@Serializable
enum class StudyContentType {
    VIDEO,
    PDF
}

@Serializable
data class ContentFolder(
    val id: String,
    val prof: ProfYear,
    val subjectId: String,
    val name: String,
    val emoji: String = "📁",
    val sectionType: String = "ALL" // "PDF", "VIDEO", or "ALL"
)

@Serializable
data class StudyContentItem(
    val id: String,
    val prof: ProfYear,
    val subjectId: String,
    val folderId: String,
    val folderName: String,
    val subfolderName: String = "",
    val subSubfolderName: String = "",
    val chapterName: String = "",
    val type: StudyContentType,
    val title: String,
    val description: String = "",
    val mediaUrl: String,
    val cloudflareKey: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class LectureItem(
    val id: String,
    val subjectId: String,
    val prof: ProfYear,
    val title: String,
    val chapter: String,
    val faculty: String,
    val duration: String,
    val videoUrl: String,
    val isWatched: Boolean = false,
    val tags: List<String> = listOf("High Yield", "NMC")
)

data class NoteItem(
    val id: String,
    val subjectId: String,
    val prof: ProfYear,
    val title: String,
    val chapter: String,
    val pagesCount: Int,
    val readMinutes: Int,
    val highYieldSummary: String,
    val pdfMockUrl: String = "",
    val isCachedOffline: Boolean = true
)

@Serializable
data class ShortItem(
    val id: String,
    val title: String,
    val description: String = "",
    val youtubeUrl: String = "",
    val youtubeVideoId: String = "",
    val thumbnailUrl: String = "",
    val cloudflareKey: String = "",
    val subjectName: String = "",
    val emoji: String = "",
    val prof: ProfYear = ProfYear.FIRST_PROF,
    val doctorName: String = "",
    val durationSec: Int = 0,
    val likesCount: Int = 0,
    val mnemonicTip: String = "",
    val audioTitle: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class AdminNoteItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val folderName: String,
    val subfolderName: String,
    val subSubfolderName: String = "",
    val chapterName: String,
    val pdfTitle: String,
    val pdfUri: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class McqQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOption: String, // "A", "B", "C", or "D"
    val explanation: String = "",
    val questionImageUrl: String = "",
    val answerImageUrl: String = ""
)

@Serializable
data class McqItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val folderName: String,
    val subfolderName: String,
    val subSubfolderName: String = "",
    val chapterName: String,
    @Transient val questions: List<McqQuestion> = emptyList(),
    val questionCount: Int = questions.size,
    val r2Key: String = "",
    val r2Url: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class CatalogTombstones(
    val folderIds: Set<String> = emptySet(),
    val folderNames: Set<String> = emptySet(),
    val contentIds: Set<String> = emptySet(),
    val shortIds: Set<String> = emptySet(),
    val adminNoteIds: Set<String> = emptySet(),
    val mcqIds: Set<String> = emptySet()
)

@Serializable
data class Catalog(
    val adminNotes: List<AdminNoteItem> = emptyList(),
    val mcqItems: List<McqItem> = emptyList(),
    val shorts: List<ShortItem> = emptyList(),
    val folders: List<ContentFolder> = emptyList(),
    val contents: List<StudyContentItem> = emptyList(),
    val tombstones: CatalogTombstones? = null
)

@Serializable
data class McqBookmarkItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val chapterName: String,
    val folderName: String = "",
    val subfolderName: String = "",
    val question: McqQuestion,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class QuestionAttemptRecord(
    val question: McqQuestion,
    val selectedOption: String = "",
    val isCorrect: Boolean = false
)

@Serializable
data class McqHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val chapterName: String,
    val folderName: String = "",
    val subfolderName: String = "",
    val totalQuestions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val unattemptedCount: Int,
    val score: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val records: List<QuestionAttemptRecord> = emptyList()
)

