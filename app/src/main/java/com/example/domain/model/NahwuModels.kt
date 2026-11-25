package com.example.domain.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val value: String, val labelId: String) {
    PLAYER("player", "Pemain"),
    REVIEWER("reviewer", "Reviewer"),
    QUESTION_MANAGER("question_manager", "Pengelola Soal"),
    SUPER_ADMIN("super_admin", "Super Admin");

    companion object {
        fun fromValue(raw: String?): UserRole =
            entries.firstOrNull { it.value == raw } ?: PLAYER
    }
}

object AdminPermission {
    const val MANAGE_CATEGORIES = "manage_categories"
    const val CREATE_QUESTION = "create_question"
    const val EDIT_QUESTION = "edit_question"
    const val SUBMIT_REVIEW = "submit_review"
    const val REVIEW_QUESTION = "review_question"
    const val APPROVE_QUESTION = "approve_question"
    const val REJECT_QUESTION = "reject_question"
    const val PUBLISH_QUESTION = "publish_question"
    const val DISABLE_QUESTION = "disable_question"
    const val MANAGE_ADMINS = "manage_admins"
    const val VIEW_LOGS = "view_logs"

    val ALL = listOf(
        MANAGE_CATEGORIES to "Kelola Kategori",
        CREATE_QUESTION to "Buat Soal",
        EDIT_QUESTION to "Edit Soal",
        SUBMIT_REVIEW to "Kirim untuk Review",
        REVIEW_QUESTION to "Periksa Soal",
        APPROVE_QUESTION to "Setujui Soal",
        REJECT_QUESTION to "Tolak / Minta Perbaikan",
        PUBLISH_QUESTION to "Publikasikan Soal",
        DISABLE_QUESTION to "Nonaktifkan Soal",
        MANAGE_ADMINS to "Kelola Admin & Izin",
        VIEW_LOGS to "Lihat Aktivitas"
    )

    fun defaultsForRole(role: UserRole): List<String> = when (role) {
        UserRole.SUPER_ADMIN -> ALL.map { it.first }
        UserRole.QUESTION_MANAGER -> listOf(
            MANAGE_CATEGORIES, CREATE_QUESTION, EDIT_QUESTION, SUBMIT_REVIEW
        )
        UserRole.REVIEWER -> listOf(
            REVIEW_QUESTION, APPROVE_QUESTION, REJECT_QUESTION
        )
        UserRole.PLAYER -> emptyList()
    }
}

enum class QuestionDifficulty(val value: String, val labelId: String) {
    BEGINNER("beginner", "Level Awal"),
    INTERMEDIATE("intermediate", "Level Menengah"),
    ADVANCED("advanced", "Level Tinggi");

    companion object {
        fun fromValue(raw: String?): QuestionDifficulty =
            entries.firstOrNull { it.value == raw } ?: BEGINNER
    }
}

enum class QuestionSource(val value: String, val labelId: String) {
    MANUAL("manual", "Manual"),
    AI("ai", "AI Generator"),
    EXCEL("excel", "Impor Excel");

    companion object {
        fun fromValue(raw: String?): QuestionSource =
            entries.firstOrNull { it.value == raw } ?: MANUAL
    }
}

enum class QuestionStatus(val value: String, val labelId: String) {
    DRAFT("draft", "Draft"),
    REVIEW("review", "Menunggu Review"),
    NEEDS_REPAIR("needs_repair", "Perlu Perbaikan"),
    APPROVED("approved", "Disetujui"),
    PUBLISHED("published", "Dipublikasikan"),
    DISABLED("disabled", "Dinonaktifkan"),
    REJECTED("rejected", "Ditolak");

    companion object {
        fun fromValue(raw: String?): QuestionStatus =
            entries.firstOrNull { it.value == raw } ?: DRAFT
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = UserRole.PLAYER.value,
    val permissions: List<String> = emptyList(),
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun roleEnum(): UserRole = UserRole.fromValue(role)
}

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = UserRole.REVIEWER.value,
    val permissions: List<String> = emptyList(),
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun roleEnum(): UserRole = UserRole.fromValue(role)
    fun hasPermission(permission: String): Boolean {
        if (!active) return false
        if (role == UserRole.SUPER_ADMIN.value) return true
        return permissions.contains(permission)
    }
}

@Keep
@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val code: String = "",
    val name: String = "",
    val description: String = "",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    val questionCounter: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuestionToken(
    val tokenId: String = "",
    val position: Int = 1,
    val baseText: String = "",
    val sharafNotes: String = "",
    val allowedExtraLetters: List<String> = emptyList()
)

@Keep
@IgnoreExtraProperties
data class TokenLetterRule(
    val letterIndex: Int = 0,
    val baseLetter: String = "",
    val role: String = "ending", // "beginning", "middle", "ending"
    val required: Boolean = false,
    val acceptedMarks: List<String> = emptyList(),
    val explanation: String = ""
)

@Keep
@IgnoreExtraProperties
data class AnswerPatternToken(
    val tokenId: String = "",
    val position: Int = 1,
    val baseText: String = "",
    val acceptedForms: List<String> = emptyList(),
    val voweledArabic: String = "",
    val indonesianMeaning: String = "",
    val wordType: String = "",
    val nahwuPosition: String = "",
    val irabOrBinaStatus: String = "",
    val irabHukum: String = "",
    val irabSign: String = "",
    val signType: String = "",
    val manifestation: String = "",
    val nahwuReason: String = "",
    val amil: String = "",
    val whyNotOtherSign: String = "",
    val binaType: String = "",
    val binaReason: String = "",
    val mahalIrab: String = "",
    val relationToOtherWords: String = "",
    val letterRules: List<TokenLetterRule> = emptyList()
)

@Keep
@IgnoreExtraProperties
data class TarkibAnalysis(
    val sentenceType: String = "",
    val mainElements: String = "",
    val structureHierarchy: List<String> = emptyList(),
    val elementRelationships: String = "",
    val meaningConstruction: String = ""
)

@Keep
@IgnoreExtraProperties
data class WordRelation(
    val fromTokenPosition: Int = 1,
    val toTokenPosition: Int = 2,
    val connectedWords: String = "",
    val relationType: String = "",
    val irabEffect: String = "",
    val reason: String = "",
    val meaningImpact: String = ""
)

@Keep
@IgnoreExtraProperties
data class GandulEntry(
    val tokenPosition: Int = 1,
    val arabicWord: String = "",
    val nahwuFunction: String = "",
    val gandulMeaning: String = "",
    val relationNote: String = ""
)

@Keep
@IgnoreExtraProperties
data class KaidahRule(
    val name: String = "",
    val definition: String = "",
    val formula: String = "",
    val conditions: String = "",
    val irabEffect: String = "",
    val applicationInSentence: String = "",
    val extraExamples: List<String> = emptyList()
)

@Keep
@IgnoreExtraProperties
data class LearningSummary(
    val mainRulesLearned: List<String> = emptyList(),
    val structureConclusion: String = "",
    val keyHarakatReasons: List<String> = emptyList()
)

@Keep
@IgnoreExtraProperties
data class AnswerPattern(
    val patternId: String = "pat_1",
    val primary: Boolean = true,
    val label: String = "Pola Utama",
    val voweledSentence: String = "",
    val translationNatural: String = "",
    val translationGandul: String = "",
    val sentenceMeaningExplanation: String = "",
    val structureDifferenceNote: String = "",
    val tarkibAnalysis: TarkibAnalysis = TarkibAnalysis(),
    val wordRelations: List<WordRelation> = emptyList(),
    val gandulEntries: List<GandulEntry> = emptyList(),
    val kaidahList: List<KaidahRule> = emptyList(),
    val learningSummary: LearningSummary = LearningSummary(),
    val tokenAnalyses: List<AnswerPatternToken> = emptyList()
)

@Keep
@IgnoreExtraProperties
data class QuestionVersionContent(
    val versionNumber: Int = 1,
    val sentenceUnvoweled: String = "",
    val translationNatural: String = "",
    val translationGandul: String = "",
    val sentenceMeaningExplanation: String = "",
    val baseTokens: List<QuestionToken> = emptyList(),
    val answerPatterns: List<AnswerPattern> = emptyList()
)

@Keep
@IgnoreExtraProperties
data class Question(
    @DocumentId val id: String = "",
    val code: String = "",
    val categoryId: String = "",
    val categoryCode: String = "",
    val categoryName: String = "",
    val title: String = "",
    val instruction: String = "",
    val difficulty: String = QuestionDifficulty.BEGINNER.value,
    val source: String = QuestionSource.MANUAL.value,
    val status: String = QuestionStatus.DRAFT.value,
    val createdBy: String = "",
    val createdByName: String = "",
    val currentVersionNumber: Long = 1L,
    val publishedVersionNumber: Long = 0L,
    val latestVersionData: QuestionVersionContent = QuestionVersionContent(),
    val publishedVersionData: QuestionVersionContent? = null,
    val lastReviewNote: String? = null,
    val reviewedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun difficultyEnum(): QuestionDifficulty = QuestionDifficulty.fromValue(difficulty)
    fun sourceEnum(): QuestionSource = QuestionSource.fromValue(source)
    fun statusEnum(): QuestionStatus = QuestionStatus.fromValue(status)
    fun activePlayContent(): QuestionVersionContent = publishedVersionData ?: latestVersionData
}

@Keep
@IgnoreExtraProperties
data class QuestionVersionRecord(
    @DocumentId val id: String = "",
    val versionNumber: Long = 1L,
    val createdBy: String = "",
    val createdByName: String = "",
    val changeSummary: String = "",
    val versionData: QuestionVersionContent = QuestionVersionContent(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuestionReviewRecord(
    @DocumentId val id: String = "",
    val reviewerUid: String = "",
    val reviewerName: String = "",
    val decision: String = "",
    val notes: String = "",
    val versionNumber: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminLogEntry(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorName: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DifficultyConfig(
    @DocumentId val id: String = "difficulty",
    val beginnerMaxSeconds: Long = 120L,
    val intermediateMaxSeconds: Long = 300L,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun difficultyForElapsedSeconds(elapsedSeconds: Long): QuestionDifficulty = when {
        elapsedSeconds < beginnerMaxSeconds -> QuestionDifficulty.BEGINNER
        elapsedSeconds < intermediateMaxSeconds -> QuestionDifficulty.INTERMEDIATE
        else -> QuestionDifficulty.ADVANCED
    }
}

@Keep
@IgnoreExtraProperties
data class PlayerSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val status: String = "active",
    val elapsedSeconds: Long = 0L,
    val currentDifficulty: String = QuestionDifficulty.BEGINNER.value,
    val questionsAnswered: Long = 0L,
    val correctCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlayerAnswerRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sessionId: String = "",
    val questionId: String = "",
    val questionCode: String = "",
    val versionNumber: Long = 1L,
    val matchedPatternId: String = "",
    val evaluationStatus: String = "",
    val score: Long = 0L,
    val correctWordCount: Long = 0L,
    val wrongWordCount: Long = 0L,
    val incompleteWordCount: Long = 0L,
    val playerTokensSummary: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)
