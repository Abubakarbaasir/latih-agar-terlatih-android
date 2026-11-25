package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.engine.ArabicHarakat
import com.example.domain.engine.EvaluationStatus
import com.example.domain.engine.PlayerLetterSlot
import com.example.domain.engine.PlayerTokenInput
import com.example.domain.model.Question
import com.example.domain.model.QuestionDifficulty
import com.example.ui.theme.*
import com.example.ui.viewmodel.GameSessionUiState
import com.example.ui.viewmodel.SessionEvaluationHistoryItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GamePlayScreen(
    gameState: GameSessionUiState,
    onSelectToken: (Int) -> Unit,
    onSelectLetter: (Int) -> Unit,
    onApplyVowel: (String) -> Unit,
    onToggleShadda: () -> Unit,
    onClearLetter: () -> Unit,
    onResetToken: () -> Unit,
    onAppendSharafLetter: (String) -> Unit,
    onRemoveSharafLetter: (Int) -> Unit,
    onCheckAnswer: () -> Unit,
    onOpenHistoryItem: (SessionEvaluationHistoryItem) -> Unit,
    onEndSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEndSessionDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showSharafLetters by remember { mutableStateOf(false) }

    BackHandler {
        showEndSessionDialog = true
    }

    val question = gameState.currentQuestion
    val selectedToken = gameState.playerInputs.getOrNull(gameState.selectedTokenIndex)
    val selectedSlot = selectedToken?.slots?.getOrNull(gameState.selectedLetterIndex)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Kategori terkunci dalam sesi",
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = gameState.activeCategory?.name ?: "Latihan Nahwu",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${formatDuration(gameState.elapsedSeconds)} • ${gameState.currentDifficulty.labelId}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                actions = {
                    if (gameState.evaluationHistory.isNotEmpty()) {
                        IconButton(
                            onClick = { showHistorySheet = true },
                            modifier = Modifier.testTag("session_history_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge { Text("${gameState.evaluationHistory.size}") }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Riwayat Pembahasan Sesi"
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = { showEndSessionDialog = true },
                        modifier = Modifier.testTag("end_session_button")
                    ) {
                        Text("Akhiri Sesi")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dijawab: ${gameState.questionsAnswered} | Benar: ${gameState.correctCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val filledEndings = gameState.playerInputs.count { it.slots.lastOrNull()?.hasAnyMark() == true }
                        Text(
                            text = "Harakat akhir terisi: $filledEndings / ${gameState.playerInputs.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onCheckAnswer,
                        enabled = question != null && !gameState.isSubmitting,
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("check_answer_button")
                    ) {
                        if (gameState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Memeriksa...")
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "PERIKSA",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (question == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            AnimatedContent(
                targetState = question.id,
                label = "question_transition",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { _ ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QuestionBannerCard(question = question, activeDifficulty = gameState.currentDifficulty)

                    // 10-Token RTL Arabic Sentence Board
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kalimat Latihan (10 Kata — Tap Kata)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Kanan → Kiri",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    gameState.playerInputs.forEachIndexed { idx, tokenInput ->
                                        ArabicTokenChip(
                                            index = idx,
                                            tokenInput = tokenInput,
                                            isSelected = idx == gameState.selectedTokenIndex,
                                            onClick = { onSelectToken(idx) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Active Word Letter Picker & Harakat Editor
                    if (selectedToken != null) {
                        WordEditorCard(
                            tokenInput = selectedToken,
                            selectedLetterIndex = gameState.selectedLetterIndex,
                            selectedSlot = selectedSlot,
                            sharafNotes = question.activePlayContent().baseTokens
                                .getOrNull(gameState.selectedTokenIndex)?.sharafNotes.orEmpty(),
                            showSharafLetters = showSharafLetters,
                            onToggleSharafLetters = { showSharafLetters = !showSharafLetters },
                            onSelectLetter = onSelectLetter,
                            onApplyVowel = onApplyVowel,
                            onToggleShadda = onToggleShadda,
                            onClearLetter = onClearLetter,
                            onResetToken = onResetToken,
                            onAppendSharafLetter = onAppendSharafLetter,
                            onRemoveSharafLetter = onRemoveSharafLetter
                        )
                    }
                }
            }
        }
    }

    if (showEndSessionDialog) {
        AlertDialog(
            onDismissRequest = { showEndSessionDialog = false },
            title = { Text("Akhiri Sesi Latihan?") },
            text = {
                Text(
                    "Anda telah menjawab ${gameState.questionsAnswered} soal dalam kategori ${gameState.activeCategory?.name ?: "ini"}. Mengakhiri sesi memungkinkan Anda memilih kategori lain dan mengatur ulang waktu kesulitan."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndSessionDialog = false
                        onEndSession()
                    },
                    modifier = Modifier.testTag("confirm_end_session_button")
                ) {
                    Text("Ya, Akhiri Sesi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndSessionDialog = false }) {
                    Text("Lanjut Bermain")
                }
            }
        )
    }

    if (showHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Riwayat Pembahasan Sesi Ini",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Pilih soal untuk membuka kembali evaluasi Nahwu 11 bagiannya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gameState.evaluationHistory, key = { it.attemptNumber }) { item ->
                        val statusColor = when (item.evaluation.overallStatus) {
                            EvaluationStatus.CORRECT -> CorrectGreen
                            EvaluationStatus.INCOMPLETE -> IncompleteAmber
                            EvaluationStatus.WRONG -> WrongCrimson
                        }
                        Card(
                            onClick = {
                                showHistorySheet = false
                                onOpenHistoryItem(item)
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "#${item.attemptNumber} • ${item.question.code} (${item.question.title})",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Skor ${item.evaluation.score}% • ${item.evaluation.correctWordCount}/10 kata tepat",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = item.evaluation.overallStatus.labelId,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun QuestionBannerCard(question: Question, activeDifficulty: QuestionDifficulty) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = question.code,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "Tingkat Soal: ${question.difficultyEnum().labelId} (Sesi: ${activeDifficulty.labelId})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = question.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = question.instruction,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ArabicTokenChip(
    index: Int,
    tokenInput: PlayerTokenInput,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val endingFilled = tokenInput.slots.lastOrNull()?.hasAnyMark() == true
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        endingFilled -> CorrectGreenContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val border = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else if (endingFilled) {
        BorderStroke(1.dp, CorrectGreen)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = bgColor,
        border = border,
        modifier = Modifier
            .heightIn(min = 58.dp)
            .testTag("arabic_token_${index + 1}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = tokenInput.renderArabic(),
                style = ArabicTokenStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordEditorCard(
    tokenInput: PlayerTokenInput,
    selectedLetterIndex: Int,
    selectedSlot: PlayerLetterSlot?,
    sharafNotes: String,
    showSharafLetters: Boolean,
    onToggleSharafLetters: () -> Unit,
    onSelectLetter: (Int) -> Unit,
    onApplyVowel: (String) -> Unit,
    onToggleShadda: () -> Unit,
    onClearLetter: () -> Unit,
    onResetToken: () -> Unit,
    onAppendSharafLetter: (String) -> Unit,
    onRemoveSharafLetter: (Int) -> Unit
) {
    val sharafPalette = remember { listOf("ا", "و", "ي", "ن", "ت", "ة", "ه", "م") }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Editor Huruf — Kata ke-${tokenInput.position}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap huruf akhir (Mahalul I'rab) atau huruf lain untuk memberi harakat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onResetToken,
                    modifier = Modifier.testTag("reset_token_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset Kata")
                }
            }

            if (sharafNotes.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Catatan Sharaf: $sharafNotes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Per-letter interactive slots (RTL)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tokenInput.slots.forEachIndexed { idx, slot ->
                        val isLast = idx == tokenInput.slots.lastIndex
                        val isLetterSelected = idx == selectedLetterIndex
                        val container = when {
                            isLetterSelected -> MaterialTheme.colorScheme.primaryContainer
                            isLast -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val border = when {
                            isLetterSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            isLast -> BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        }
                        Surface(
                            onClick = { onSelectLetter(idx) },
                            shape = MaterialTheme.shapes.small,
                            color = container,
                            border = border,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 52.dp, minHeight = 64.dp)
                                .testTag("letter_slot_${slot.slotIndex}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = slot.renderArabic(),
                                    style = ArabicTokenStyle,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = if (isLast) "Akhir" else "${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isLast) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (slot.isAddedLetter) {
                                        IconButton(
                                            onClick = { onRemoveSharafLetter(idx) },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Hapus huruf tambahan",
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedSlot != null) {
                Text(
                    text = "Huruf dipilih: ${selectedSlot.baseChar} (${ArabicHarakat.describeMark(selectedSlot.canonicalMark())})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Harakat Palette
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArabicHarakat.VOWEL_OPTIONS.forEach { option ->
                    val isActive = selectedSlot?.vowelMark == option.code
                    FilterChip(
                        selected = isActive,
                        onClick = { onApplyVowel(option.code) },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ب${option.preview}",
                                    style = ArabicInlineStyle
                                )
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("harakat_${option.label.lowercase().replace(" ", "_")}")
                    )
                }

                FilterChip(
                    selected = selectedSlot?.hasShadda == true,
                    onClick = onToggleShadda,
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "بّ", style = ArabicInlineStyle)
                            Text("Tasydid", style = MaterialTheme.typography.labelMedium)
                        }
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("harakat_tasydid")
                )

                OutlinedButton(
                    onClick = onClearLetter,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("clear_letter_harakat")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kosongkan Huruf")
                }
            }

            // Optional Sharaf Letter Addition Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleSharafLetters) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showSharafLetters) "Sembunyikan Tambah Huruf Sharaf" else "Tambah Huruf Sharaf (Opsional)")
                }
            }

            if (showSharafLetters) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sharafPalette.forEach { letter ->
                            FilledTonalButton(
                                onClick = { onAppendSharafLetter(letter) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(
                                    text = letter,
                                    style = ArabicTokenStyle,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
