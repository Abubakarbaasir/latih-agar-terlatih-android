package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiGeneratedQuestionBundle
import com.example.data.AiQuestionGenerator
import com.example.data.NahwuRepository
import com.example.data.SpreadsheetImporter
import com.example.data.SpreadsheetPreviewRow
import com.example.domain.model.*
import com.example.firebase.toFriendlyFirestoreMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminDashboardStats(
    val totalCategories: Int = 0,
    val totalQuestions: Int = 0,
    val draftCount: Int = 0,
    val reviewCount: Int = 0,
    val needsRepairCount: Int = 0,
    val approvedCount: Int = 0,
    val publishedCount: Int = 0
)

data class AdminActionFeedback(
    val isError: Boolean = false,
    val message: String = ""
)

class AdminViewModel(
    private val repository: NahwuRepository,
    private val aiGenerator: AiQuestionGenerator,
    private val currentUserId: String
) : ViewModel() {

    val myAdminRecord: StateFlow<AdminRecord?> = repository.observeMyAdminRecord(currentUserId)
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val categories: StateFlow<UiState<List<Category>>> = repository.observeAllCategoriesAdmin()
        .map<List<Category>, UiState<List<Category>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(UiState.Error((fsErr ?: error).toFriendlyFirestoreMessage("Gagal memuat kategori admin.")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val questions: StateFlow<UiState<List<Question>>> = repository.observeAllQuestionsAdmin()
        .map<List<Question>, UiState<List<Question>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(UiState.Error((fsErr ?: error).toFriendlyFirestoreMessage("Gagal memuat daftar soal.")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val logs: StateFlow<List<AdminLogEntry>> = repository.observeAdminLogs()
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val users: StateFlow<List<UserProfile>> = repository.observeAllUsersForAdmin()
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val difficultyConfig: StateFlow<DifficultyConfig> = repository.observeDifficultyConfig()
        .catch { emit(DifficultyConfig()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DifficultyConfig())

    val stats: StateFlow<AdminDashboardStats> = combine(categories, questions) { catState, qState ->
        val catCount = (catState as? UiState.Success)?.data?.size ?: 0
        val qList = (qState as? UiState.Success)?.data ?: emptyList()
        AdminDashboardStats(
            totalCategories = catCount,
            totalQuestions = qList.size,
            draftCount = qList.count { it.status == QuestionStatus.DRAFT.value },
            reviewCount = qList.count { it.status == QuestionStatus.REVIEW.value },
            needsRepairCount = qList.count { it.status == QuestionStatus.NEEDS_REPAIR.value },
            approvedCount = qList.count { it.status == QuestionStatus.APPROVED.value },
            publishedCount = qList.count { it.status == QuestionStatus.PUBLISHED.value }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AdminDashboardStats())

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _feedback = MutableStateFlow<AdminActionFeedback?>(null)
    val feedback: StateFlow<AdminActionFeedback?> = _feedback.asStateFlow()

    // AI Generator State
    private val _aiState = MutableStateFlow<UiState<List<AiGeneratedQuestionBundle>>>(UiState.Success(emptyList()))
    val aiState: StateFlow<UiState<List<AiGeneratedQuestionBundle>>> = _aiState.asStateFlow()

    // Excel Importer State
    private val _excelPreview = MutableStateFlow<List<SpreadsheetPreviewRow>>(emptyList())
    val excelPreview: StateFlow<List<SpreadsheetPreviewRow>> = _excelPreview.asStateFlow()

    // Detail Versions & Reviews
    private val _versions = MutableStateFlow<List<QuestionVersionRecord>>(emptyList())
    val versions: StateFlow<List<QuestionVersionRecord>> = _versions.asStateFlow()

    private val _reviews = MutableStateFlow<List<QuestionReviewRecord>>(emptyList())
    val reviews: StateFlow<List<QuestionReviewRecord>> = _reviews.asStateFlow()

    fun clearFeedback() {
        _feedback.value = null
    }

    fun saveCategory(
        categoryId: String?,
        code: String,
        name: String,
        description: String,
        sortOrder: Long,
        active: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveCategory(categoryId, code, name, description, sortOrder, active)
            _isBusy.value = false
            _feedback.value = if (res.isSuccess) {
                AdminActionFeedback(false, "Kategori berhasil disimpan.")
            } else {
                AdminActionFeedback(true, res.exceptionOrNull()?.toFriendlyFirestoreMessage() ?: "Gagal menyimpan kategori.")
            }
        }
    }

    fun saveManualQuestion(
        existing: Question?,
        category: Category,
        title: String,
        instruction: String,
        difficulty: QuestionDifficulty,
        versionContent: QuestionVersionContent,
        changeSummary: String,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = if (existing == null) {
                repository.createDraftQuestionTransactional(
                    category = category,
                    title = title,
                    instruction = instruction,
                    difficulty = difficulty,
                    source = QuestionSource.MANUAL,
                    versionContent = versionContent
                ).map { Unit }
            } else {
                repository.updateQuestionNewVersion(
                    existing = existing,
                    category = category,
                    title = title,
                    instruction = instruction,
                    difficulty = difficulty,
                    versionContent = versionContent,
                    changeSummary = changeSummary
                )
            }
            _isBusy.value = false
            if (res.isSuccess) {
                _feedback.value = AdminActionFeedback(false, "Soal disimpan sebagai Draft.")
                onSaved()
            } else {
                _feedback.value = AdminActionFeedback(true, res.exceptionOrNull()?.toFriendlyFirestoreMessage() ?: "Gagal menyimpan soal.")
            }
        }
    }

    fun generateQuestionsWithAi(
        category: Category,
        kaidahFocus: String,
        difficulty: QuestionDifficulty,
        count: Int,
        topicMaterial: String
    ) {
        viewModelScope.launch {
            _aiState.value = UiState.Loading
            val res = aiGenerator.generateDraftQuestions(
                categoryName = category.name,
                kaidahFocus = kaidahFocus,
                difficulty = difficulty,
                count = count,
                topicMaterial = topicMaterial
            )
            _aiState.value = if (res.isSuccess) {
                UiState.Success(res.getOrDefault(emptyList()))
            } else {
                val msg = res.exceptionOrNull()?.localizedMessage ?: "Gagal menghasilkan soal AI."
                Log.e(TAG, "AI generation error: $msg")
                UiState.Error(msg)
            }
        }
    }

    fun saveAiDrafts(category: Category, bundles: List<AiGeneratedQuestionBundle>, onDone: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            var saved = 0
            var lastErr: Throwable? = null
            for (b in bundles) {
                val res = repository.createDraftQuestionTransactional(
                    category = category,
                    title = b.title,
                    instruction = b.instruction,
                    difficulty = b.difficulty,
                    source = QuestionSource.AI,
                    versionContent = b.versionContent
                )
                if (res.isSuccess) saved++ else lastErr = res.exceptionOrNull()
            }
            _isBusy.value = false
            if (saved > 0) {
                _aiState.value = UiState.Success(emptyList())
                _feedback.value = AdminActionFeedback(false, "$saved soal AI berhasil disimpan sebagai Draft (menunggu pemeriksaan Reviewer).")
                onDone()
            } else {
                _feedback.value = AdminActionFeedback(true, lastErr?.toFriendlyFirestoreMessage() ?: "Gagal menyimpan soal AI.")
            }
        }
    }

    fun parseSpreadsheetFile(context: Context, uri: Uri) {
        val catList = (categories.value as? UiState.Success)?.data.orEmpty()
        val validCodes = catList.map { it.code.uppercase() }.toSet()
        viewModelScope.launch {
            _isBusy.value = true
            val res = SpreadsheetImporter.parseUri(context, uri, validCodes)
            _isBusy.value = false
            if (res.isSuccess) {
                _excelPreview.value = res.getOrDefault(emptyList())
            } else {
                _excelPreview.value = emptyList()
                _feedback.value = AdminActionFeedback(true, res.exceptionOrNull()?.message ?: "Gagal membaca file Excel/CSV.")
            }
        }
    }

    fun confirmExcelImport(onDone: () -> Unit) {
        val catList = (categories.value as? UiState.Success)?.data.orEmpty()
        val catByCode = catList.associateBy { it.code.uppercase() }
        val validRows = _excelPreview.value.mapNotNull { it.parsedDraft }
        if (validRows.isEmpty()) {
            _feedback.value = AdminActionFeedback(true, "Tidak ada baris valid untuk diimpor.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            var imported = 0
            var lastErr: Throwable? = null
            for (draft in validRows) {
                val cat = catByCode[draft.categoryCode.uppercase()] ?: continue
                val res = repository.createDraftQuestionTransactional(
                    category = cat,
                    title = draft.title,
                    instruction = draft.instruction,
                    difficulty = draft.difficulty,
                    source = QuestionSource.EXCEL,
                    versionContent = draft.versionContent
                )
                if (res.isSuccess) imported++ else lastErr = res.exceptionOrNull()
            }
            _isBusy.value = false
            if (imported > 0) {
                _excelPreview.value = emptyList()
                _feedback.value = AdminActionFeedback(false, "$imported soal dari Excel berhasil diimpor sebagai Draft.")
                onDone()
            } else {
                _feedback.value = AdminActionFeedback(true, lastErr?.toFriendlyFirestoreMessage() ?: "Impor gagal.")
            }
        }
    }

    fun loadQuestionAuditHistory(questionId: String) {
        viewModelScope.launch {
            _versions.value = repository.fetchQuestionVersions(questionId).getOrDefault(emptyList())
            _reviews.value = repository.fetchQuestionReviews(questionId).getOrDefault(emptyList())
        }
    }

    fun submitForReview(question: Question) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.submitQuestionForReview(question)
            _isBusy.value = false
            _feedback.value = if (res.isSuccess) {
                AdminActionFeedback(false, "Soal ${question.code} dikirim ke Reviewer.")
            } else {
                AdminActionFeedback(true, res.exceptionOrNull()?.toFriendlyFirestoreMessage() ?: "Gagal mengirim review.")
            }
        }
    }

    fun decideReview(question: Question, decision: QuestionStatus, notes: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.reviewQuestionDecision(question, decision, notes)
            _isBusy.value = false
            _feedback.value = if (res.isSuccess) {
                AdminActionFeedback(false, "Status soal ${question.code} diperbarui menjadi ${decision.labelId}.")
            } else {
                AdminActionFeedback(true, res.exceptionOrNull()?.toFriendlyFirestoreMessage() ?: "Gagal menyimpan hasil review.")
            }
        }
    }

    fun publishQuestion(question: Question) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.publishQuestion(question)
            _isBusy.value = false
            _feedback.value = if (res.isSuccess) {
                AdminActionFeedback(false, "Soal ${question.code} telah dipublikasikan ke pemain.")
            } else {
                AdminActionFeedback(true, res.exceptionOrNull()?.toFriendlyFirestoreMessage() ?: "Gagal mempublikasikan soal.")
            }
        }
    }

    fun disableQuestion(question: Question) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.disableQuestion(question)
            _isBusy.value = false
            _feedback.value = if (res.isSuccess) {
                AdminActionFeedback(false, "Soal ${question.code} telah dinonaktifkan.")
            } else {
                AdminActionFeedback(true, res.exceptionOrNull()?.toFriendlyFirestoreMessage() ?: "Gagal menonaktifkan soal.")
            }
        }
    }

    fun updateDifficultyThresholds(beginnerMax: Long, intermediateMax: Long) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveDifficultyConfig(beginnerMax, intermediateMax)
            _isBusy.value = false
            _feedback.value = if (res.isSuccess) {
                AdminActionFeedback(false, "Ambang waktu kesulitan berhasil diperbarui.")
            } else {
                AdminActionFeedback(true, res.exceptionOrNull()?.toFriendlyFirestoreMessage() ?: "Gagal menyimpan konfigurasi.")
            }
        }
    }

    fun updateAdminAccess(user: UserProfile, role: UserRole, permissions: List<String>, active: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveAdminRoleAndPermissions(user, role, permissions, active)
            _isBusy.value = false
            _feedback.value = if (res.isSuccess) {
                AdminActionFeedback(false, "Hak akses ${user.displayName} diperbarui menjadi ${role.labelId}.")
            } else {
                AdminActionFeedback(true, res.exceptionOrNull()?.toFriendlyFirestoreMessage() ?: "Gagal memperbarui hak akses.")
            }
        }
    }

    private companion object {
        const val TAG = "AdminViewModel"
    }
}
