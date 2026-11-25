package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NahwuRepository
import com.example.domain.engine.ArabicHarakat
import com.example.domain.engine.EvaluationResult
import com.example.domain.engine.EvaluationStatus
import com.example.domain.engine.NahwuGradingEngine
import com.example.domain.engine.PlayerLetterSlot
import com.example.domain.engine.PlayerTokenInput
import com.example.domain.model.*
import com.example.firebase.toFriendlyFirestoreMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class SessionEvaluationHistoryItem(
    val attemptNumber: Int,
    val question: Question,
    val playerInputs: List<PlayerTokenInput>,
    val evaluation: EvaluationResult
)

data class GameSessionUiState(
    val activeCategory: Category? = null,
    val session: PlayerSession? = null,
    val availableQuestions: List<Question> = emptyList(),
    val currentQuestion: Question? = null,
    val playerInputs: List<PlayerTokenInput> = emptyList(),
    val selectedTokenIndex: Int = 0,
    val selectedLetterIndex: Int = 0,
    val elapsedSeconds: Long = 0L,
    val currentDifficulty: QuestionDifficulty = QuestionDifficulty.BEGINNER,
    val questionsAnswered: Int = 0,
    val correctCount: Int = 0,
    val currentEvaluation: EvaluationResult? = null,
    val evaluationHistory: List<SessionEvaluationHistoryItem> = emptyList(),
    val answeredQuestionIds: Set<String> = emptySet(),
    val isStartingSession: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

class PlayerViewModel(
    private val repository: NahwuRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _profileSyncDone = MutableStateFlow(false)
    private val _retryTrigger = MutableStateFlow(0)

    val myProfile: StateFlow<UserProfile?> = repository.observeMyProfile(currentUserId)
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing profile", fsErr ?: error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val myAdminRecord: StateFlow<AdminRecord?> = repository.observeMyAdminRecord(currentUserId)
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing admin record", fsErr ?: error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val categoriesState: StateFlow<UiState<List<Category>>> = _retryTrigger
        .flatMapLatest {
            repository.observeActiveCategories()
                .map<List<Category>, UiState<List<Category>>> { UiState.Success(it) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val root = fsErr ?: error
                    Log.w(TAG, "Error observing active categories", root)
                    emit(UiState.Error(root.toFriendlyFirestoreMessage("Gagal memuat kategori latihan.")))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val difficultyConfig: StateFlow<DifficultyConfig> = repository.observeDifficultyConfig()
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(DifficultyConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DifficultyConfig())

    private val _gameState = MutableStateFlow(GameSessionUiState())
    val gameState: StateFlow<GameSessionUiState> = _gameState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureUserProfileAndBootstrapAdmin()
            _profileSyncDone.value = true
        }
    }

    fun retryCategories() {
        _retryTrigger.value += 1
    }

    fun startCategorySession(category: Category, onSessionStarted: () -> Unit) {
        viewModelScope.launch {
            _gameState.value = GameSessionUiState(
                activeCategory = category,
                isStartingSession = true
            )
            val questionsResult = repository.getPublishedQuestionsForCategory(category.id)
            if (questionsResult.isFailure) {
                val msg = questionsResult.exceptionOrNull()?.toFriendlyFirestoreMessage("Gagal memuat soal.")
                    ?: "Gagal memuat soal."
                _gameState.update { it.copy(isStartingSession = false, errorMessage = msg) }
                return@launch
            }
            val questions = questionsResult.getOrDefault(emptyList())
            if (questions.isEmpty()) {
                _gameState.update {
                    it.copy(
                        isStartingSession = false,
                        errorMessage = "Belum ada soal berstatus Dipublikasikan pada kategori '${category.name}'. Silakan hubungi Admin atau pilih kategori lain."
                    )
                }
                return@launch
            }

            val sessionResult = repository.startPlayerSession(category)
            val session = sessionResult.getOrElse {
                _gameState.update { state ->
                    state.copy(
                        isStartingSession = false,
                        errorMessage = it.toFriendlyFirestoreMessage("Gagal memulai sesi permainan.")
                    )
                }
                return@launch
            }

            val initialQuestion = pickNextQuestion(questions, QuestionDifficulty.BEGINNER, emptySet())
            val inputs = buildInitialInputs(initialQuestion)
            val lastIdx = inputs.firstOrNull()?.slots?.lastIndex?.coerceAtLeast(0) ?: 0

            _gameState.value = GameSessionUiState(
                activeCategory = category,
                session = session,
                availableQuestions = questions,
                currentQuestion = initialQuestion,
                playerInputs = inputs,
                selectedTokenIndex = 0,
                selectedLetterIndex = lastIdx,
                elapsedSeconds = 0L,
                currentDifficulty = QuestionDifficulty.BEGINNER,
                isStartingSession = false
            )
            startSessionTimer()
            onSessionStarted()
        }
    }

    private fun startSessionTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _gameState.update { state ->
                    if (state.session == null) return@update state
                    val nextElapsed = state.elapsedSeconds + 1L
                    val nextDiff = difficultyConfig.value.difficultyForElapsedSeconds(nextElapsed)
                    state.copy(elapsedSeconds = nextElapsed, currentDifficulty = nextDiff)
                }
            }
        }
    }

    private fun buildInitialInputs(question: Question): List<PlayerTokenInput> {
        val content = question.activePlayContent()
        return content.baseTokens.sortedBy { it.position }.map { token ->
            val slots = ArabicHarakat.initialPlayerSlots(token)
            PlayerTokenInput(
                tokenId = token.tokenId,
                position = token.position,
                baseText = token.baseText,
                slots = slots
            )
        }
    }

    private fun pickNextQuestion(
        questions: List<Question>,
        targetDifficulty: QuestionDifficulty,
        answeredIds: Set<String>
    ): Question {
        val unanswered = questions.filter { it.id !in answeredIds }.ifEmpty { questions }
        val exactMatch = unanswered.firstOrNull { it.difficultyEnum() == targetDifficulty }
        if (exactMatch != null) return exactMatch
        return when (targetDifficulty) {
            QuestionDifficulty.ADVANCED ->
                unanswered.firstOrNull { it.difficultyEnum() == QuestionDifficulty.INTERMEDIATE }
                    ?: unanswered.first()
            QuestionDifficulty.INTERMEDIATE ->
                unanswered.firstOrNull { it.difficultyEnum() == QuestionDifficulty.BEGINNER }
                    ?: unanswered.first()
            QuestionDifficulty.BEGINNER -> unanswered.first()
        }
    }

    fun selectToken(tokenIndex: Int) {
        _gameState.update { state ->
            val token = state.playerInputs.getOrNull(tokenIndex) ?: return@update state
            val defaultLetterIdx = token.slots.lastIndex.coerceAtLeast(0)
            state.copy(selectedTokenIndex = tokenIndex, selectedLetterIndex = defaultLetterIdx)
        }
    }

    fun selectLetter(letterIndex: Int) {
        _gameState.update { state ->
            val token = state.playerInputs.getOrNull(state.selectedTokenIndex) ?: return@update state
            if (letterIndex !in token.slots.indices) return@update state
            state.copy(selectedLetterIndex = letterIndex)
        }
    }

    fun applyVowelMark(vowelCode: String) {
        updateSelectedSlot { slot ->
            slot.copy(vowelMark = if (slot.vowelMark == vowelCode) "" else vowelCode)
        }
    }

    fun toggleShadda() {
        updateSelectedSlot { slot ->
            slot.copy(hasShadda = !slot.hasShadda)
        }
    }

    fun clearSelectedLetterMarks() {
        updateSelectedSlot { slot ->
            slot.copy(vowelMark = "", hasShadda = false)
        }
    }

    fun resetSelectedToken() {
        _gameState.update { state ->
            val currentQ = state.currentQuestion ?: return@update state
            val baseToken = currentQ.activePlayContent().baseTokens.getOrNull(state.selectedTokenIndex)
                ?: return@update state
            val freshSlots = ArabicHarakat.initialPlayerSlots(baseToken)
            val updatedInputs = state.playerInputs.toMutableList()
            updatedInputs[state.selectedTokenIndex] = PlayerTokenInput(
                tokenId = baseToken.tokenId,
                position = baseToken.position,
                baseText = baseToken.baseText,
                slots = freshSlots
            )
            state.copy(
                playerInputs = updatedInputs,
                selectedLetterIndex = freshSlots.lastIndex.coerceAtLeast(0)
            )
        }
    }

    fun appendSharafLetter(letter: String) {
        _gameState.update { state ->
            val token = state.playerInputs.getOrNull(state.selectedTokenIndex) ?: return@update state
            if (token.slots.size >= 16) return@update state
            val newSlots = token.slots + PlayerLetterSlot(
                slotIndex = token.slots.size,
                baseChar = letter,
                isAddedLetter = true,
                vowelMark = "",
                hasShadda = false
            )
            val updatedInputs = state.playerInputs.toMutableList()
            updatedInputs[state.selectedTokenIndex] = token.copy(slots = newSlots)
            state.copy(
                playerInputs = updatedInputs,
                selectedLetterIndex = newSlots.lastIndex
            )
        }
    }

    fun removeAddedSharafLetter(slotIndex: Int) {
        _gameState.update { state ->
            val token = state.playerInputs.getOrNull(state.selectedTokenIndex) ?: return@update state
            val slot = token.slots.getOrNull(slotIndex) ?: return@update state
            if (!slot.isAddedLetter) return@update state
            val newSlots = token.slots.filterIndexed { idx, _ -> idx != slotIndex }
                .mapIndexed { idx, s -> s.copy(slotIndex = idx) }
            val updatedInputs = state.playerInputs.toMutableList()
            updatedInputs[state.selectedTokenIndex] = token.copy(slots = newSlots)
            state.copy(
                playerInputs = updatedInputs,
                selectedLetterIndex = newSlots.lastIndex.coerceAtLeast(0)
            )
        }
    }

    private fun updateSelectedSlot(transform: (PlayerLetterSlot) -> PlayerLetterSlot) {
        _gameState.update { state ->
            val token = state.playerInputs.getOrNull(state.selectedTokenIndex) ?: return@update state
            val slot = token.slots.getOrNull(state.selectedLetterIndex) ?: return@update state
            val newSlots = token.slots.toMutableList()
            newSlots[state.selectedLetterIndex] = transform(slot)
            val updatedInputs = state.playerInputs.toMutableList()
            updatedInputs[state.selectedTokenIndex] = token.copy(slots = newSlots)
            state.copy(playerInputs = updatedInputs)
        }
    }

    fun checkAnswer(onEvaluated: () -> Unit) {
        val state = _gameState.value
        val question = state.currentQuestion ?: return
        val session = state.session ?: return
        val content = question.activePlayContent()

        viewModelScope.launch {
            _gameState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val evaluation = NahwuGradingEngine.evaluate(content, state.playerInputs)
            val newAnswered = state.questionsAnswered + 1
            val newCorrect = state.correctCount + if (evaluation.overallStatus == EvaluationStatus.CORRECT) 1 else 0

            repository.recordPlayerAnswerAndProgress(
                sessionId = session.id,
                question = question,
                evaluation = evaluation,
                playerInputs = state.playerInputs,
                elapsedSeconds = state.elapsedSeconds,
                currentDifficulty = state.currentDifficulty,
                questionsAnswered = newAnswered.toLong(),
                correctCount = newCorrect.toLong()
            )

            val historyItem = SessionEvaluationHistoryItem(
                attemptNumber = state.evaluationHistory.size + 1,
                question = question,
                playerInputs = state.playerInputs,
                evaluation = evaluation
            )

            _gameState.update { current ->
                current.copy(
                    questionsAnswered = newAnswered,
                    correctCount = newCorrect,
                    currentEvaluation = evaluation,
                    evaluationHistory = listOf(historyItem) + current.evaluationHistory,
                    answeredQuestionIds = current.answeredQuestionIds + question.id,
                    isSubmitting = false
                )
            }
            onEvaluated()
        }
    }

    fun openHistoryEvaluation(item: SessionEvaluationHistoryItem, onReady: () -> Unit) {
        _gameState.update {
            it.copy(
                currentQuestion = item.question,
                playerInputs = item.playerInputs,
                currentEvaluation = item.evaluation
            )
        }
        onReady()
    }

    fun nextQuestion() {
        _gameState.update { state ->
            val questions = state.availableQuestions
            if (questions.isEmpty()) return@update state
            val nextQ = pickNextQuestion(questions, state.currentDifficulty, state.answeredQuestionIds)
            val freshInputs = buildInitialInputs(nextQ)
            state.copy(
                currentQuestion = nextQ,
                playerInputs = freshInputs,
                selectedTokenIndex = 0,
                selectedLetterIndex = freshInputs.firstOrNull()?.slots?.lastIndex?.coerceAtLeast(0) ?: 0,
                currentEvaluation = null
            )
        }
    }

    fun clearError() {
        _gameState.update { it.copy(errorMessage = null) }
    }

    fun endSession(onEnded: () -> Unit) {
        timerJob?.cancel()
        val state = _gameState.value
        val session = state.session
        viewModelScope.launch {
            if (session != null) {
                repository.endPlayerSession(
                    sessionId = session.id,
                    elapsedSeconds = state.elapsedSeconds,
                    currentDifficulty = state.currentDifficulty,
                    questionsAnswered = state.questionsAnswered.toLong(),
                    correctCount = state.correctCount.toLong()
                )
            }
            _gameState.value = GameSessionUiState()
            onEnded()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    private companion object {
        const val TAG = "PlayerViewModel"
    }
}
