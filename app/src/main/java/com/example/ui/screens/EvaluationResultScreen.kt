package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.engine.ArabicHarakat
import com.example.domain.engine.EvaluationResult
import com.example.domain.engine.EvaluationStatus
import com.example.domain.engine.TokenEvaluationResult
import com.example.domain.model.Question
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationResultScreen(
    question: Question?,
    evaluation: EvaluationResult?,
    onNextQuestion: () -> Unit,
    onBackToGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackToGame()
    }

    if (question == null || evaluation == null) {
        Scaffold(modifier = modifier.fillMaxSize()) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Belum ada hasil evaluasi untuk ditampilkan.")
                    Button(onClick = onBackToGame) {
                        Text("Kembali ke Permainan")
                    }
                }
            }
        }
        return
    }

    EvaluationResultContent(
        question = question,
        evaluation = evaluation,
        onNextQuestion = onNextQuestion,
        onBackToGame = onBackToGame,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationResultContent(
    question: Question,
    evaluation: EvaluationResult,
    onNextQuestion: () -> Unit,
    onBackToGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pembahasan Nahwu Lengkap",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${question.code} • ${question.categoryName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToGame,
                        modifier = Modifier.testTag("eval_top_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Permainan"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = 780.dp)
                .testTag("evaluation_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A. Ringkasan Hasil Umum
            item(key = "section_a") {
                SectionAGeneralSummary(evaluation = evaluation)
            }

            // B. Kalimat Berharakat Lengkap & Artinya
            item(key = "section_b") {
                SectionBFullVoweledSentence(evaluation = evaluation)
            }

            // C. Perbandingan Jawaban Pemain vs Jawaban yang Diterima
            item(key = "section_c") {
                SectionCComparisonList(tokenResults = evaluation.tokenResults)
            }

            // D. Tabel Analisis Kata per Kata (10 Kata)
            item(key = "section_d") {
                SectionDTokenMatrixTable(tokenResults = evaluation.tokenResults)
            }

            // E. Pembahasan I'rab Lengkap per Kata
            item(key = "section_e") {
                SectionEFullIrabBreakdown(tokenResults = evaluation.tokenResults)
            }

            // F. Pembahasan Tarkib (Struktur Kalimat)
            item(key = "section_f") {
                SectionFTarkibAnalysis(pattern = evaluation.matchedPattern)
            }

            // G. Pembahasan Hubungan Antarkata
            item(key = "section_g") {
                SectionGWordRelations(pattern = evaluation.matchedPattern)
            }

            // H. Pembahasan Gandul (Gaya Pesantren)
            item(key = "section_h") {
                SectionHGandulBreakdown(pattern = evaluation.matchedPattern)
            }

            // I. Terjemahan Lengkap
            item(key = "section_i") {
                SectionITranslations(pattern = evaluation.matchedPattern)
            }

            // J. Kaidah Nahwu yang Terkandung
            item(key = "section_j") {
                SectionJKaidahCards(pattern = evaluation.matchedPattern)
            }

            // K. Kesimpulan Pembelajaran & Tombol Aksi
            item(key = "section_k") {
                SectionKConclusionAndActions(
                    pattern = evaluation.matchedPattern,
                    onNextQuestion = onNextQuestion,
                    onRepeatReview = {
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                    onBackToGame = onBackToGame
                )
            }
        }
    }
}

@Composable
internal fun EvaluationSectionHeader(letter: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primary
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionAGeneralSummary(evaluation: EvaluationResult) {
    val (bgColor, fgColor, icon) = when (evaluation.overallStatus) {
        EvaluationStatus.CORRECT -> Triple(CorrectGreenContainer, CorrectGreen, Icons.Default.CheckCircle)
        EvaluationStatus.INCOMPLETE -> Triple(IncompleteAmberContainer, IncompleteAmber, Icons.Default.HourglassEmpty)
        EvaluationStatus.WRONG -> Triple(WrongCrimsonContainer, WrongCrimson, Icons.Default.Error)
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, fgColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("eval_section_a")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EvaluationSectionHeader("A", "Ringkasan Hasil Umum")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = fgColor, modifier = Modifier.size(28.dp))
                    Text(
                        text = evaluation.overallStatus.labelId,
                        style = MaterialTheme.typography.headlineSmall,
                        color = fgColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = fgColor
                ) {
                    Text(
                        text = "Skor ${evaluation.score}%",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Text(
                text = evaluation.errorSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryStatBadge("Benar", "${evaluation.correctWordCount}", CorrectGreen, Modifier.weight(1f))
                SummaryStatBadge("Salah", "${evaluation.wrongWordCount}", WrongCrimson, Modifier.weight(1f))
                SummaryStatBadge("Belum Lengkap", "${evaluation.incompleteWordCount}", IncompleteAmber, Modifier.weight(1f))
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ) {
                Text(
                    text = "Pola Jawaban Tervalidasi: ${evaluation.matchedPattern.label}" +
                        if (evaluation.matchedAlternativePattern) " (Pola Alternatif Sah)" else "",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryStatBadge(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionBFullVoweledSentence(evaluation: EvaluationResult) {
    val pattern = evaluation.matchedPattern
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("eval_section_b")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EvaluationSectionHeader("B", "Kalimat Berharakat Lengkap & Artinya")

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = pattern.voweledSentence,
                        style = ArabicDisplayStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            LabeledParagraph("Terjemahan Alami", pattern.translationNatural)
            if (pattern.sentenceMeaningExplanation.isNotBlank()) {
                LabeledParagraph("Penjelasan Makna Kalimat", pattern.sentenceMeaningExplanation)
            }
            if (pattern.structureDifferenceNote.isNotBlank()) {
                LabeledParagraph("Catatan Variasi Susunan / I'rab", pattern.structureDifferenceNote)
            }
        }
    }
}

@Composable
private fun SectionCComparisonList(tokenResults: List<TokenEvaluationResult>) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("eval_section_c")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EvaluationSectionHeader("C", "Perbandingan Jawaban Pemain vs Diterima")

            tokenResults.forEach { res ->
                val (badgeBg, badgeFg) = when (res.status) {
                    EvaluationStatus.CORRECT -> CorrectGreenContainer to CorrectGreen
                    EvaluationStatus.INCOMPLETE -> IncompleteAmberContainer to IncompleteAmber
                    EvaluationStatus.WRONG -> WrongCrimsonContainer to WrongCrimson
                }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, badgeFg.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kata #${res.position} • ${res.tokenAnalysis.nahwuPosition}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Surface(color = badgeBg, shape = MaterialTheme.shapes.small) {
                                Text(
                                    text = res.status.labelId,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = badgeFg,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Jawaban Anda", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text(res.playerArabic, style = ArabicTokenStyle)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Jawaban Diterima", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text(res.acceptedArabic, style = ArabicTokenStyle, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Text(
                            text = res.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        res.wrongLetters.forEach { err ->
                            Text(
                                text = "• Huruf Salah (${err.baseLetter} — ${err.roleLabel}): Anda mengisi ${ArabicHarakat.describeMark(err.playerMark)}, seharusnya ${
                                    err.expectedMarks.joinToString(" / ") { ArabicHarakat.describeMark(it) }
                                }.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WrongCrimson
                            )
                        }
                        res.incompleteLetters.forEach { inc ->
                            Text(
                                text = "• Wajib Belum Diisi (${inc.baseLetter} — ${inc.roleLabel}): Seharusnya ${
                                    inc.expectedMarks.joinToString(" / ") { ArabicHarakat.describeMark(it) }
                                }.",
                                style = MaterialTheme.typography.bodySmall,
                                color = IncompleteAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionDTokenMatrixTable(tokenResults: List<TokenEvaluationResult>) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("eval_section_d")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EvaluationSectionHeader("D", "Tabel Analisis Kata per Kata (10 Kata)")
            Text(
                text = "Geser tabel ke samping untuk melihat seluruh kolom analisis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                    Row(modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)) {
                        TableCell("No", 36.dp, bold = true)
                        TableCell("Kata Arab", 110.dp, bold = true)
                        TableCell("Arti", 120.dp, bold = true)
                        TableCell("Jenis", 84.dp, bold = true)
                        TableCell("Kedudukan", 130.dp, bold = true)
                        TableCell("Status", 76.dp, bold = true)
                        TableCell("Hukum", 90.dp, bold = true)
                        TableCell("Tanda I'rab", 120.dp, bold = true)
                    }
                }

                tokenResults.forEach { res ->
                    val a = res.tokenAnalysis
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell("${res.position}", 36.dp)
                        Box(modifier = Modifier.width(110.dp)) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(res.acceptedArabic, style = ArabicInlineStyle)
                            }
                        }
                        TableCell(a.indonesianMeaning, 120.dp)
                        TableCell(a.wordType, 84.dp)
                        TableCell(a.nahwuPosition, 130.dp)
                        TableCell(a.irabOrBinaStatus, 76.dp)
                        TableCell(a.irabHukum, 90.dp)
                        TableCell(a.irabSign, 120.dp)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TableCell(text: String, width: androidx.compose.ui.unit.Dp, bold: Boolean = false) {
    Text(
        text = text.ifBlank { "-" },
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .width(width)
            .padding(end = 6.dp)
    )
}

@Composable
private fun SectionEFullIrabBreakdown(tokenResults: List<TokenEvaluationResult>) {
    var expandAll by remember { mutableStateOf(true) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("eval_section_e")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EvaluationSectionHeader("E", "Pembahasan I'rab Lengkap per Kata")
                TextButton(onClick = { expandAll = !expandAll }) {
                    Text(if (expandAll) "Ringkas" else "Buka Semua")
                }
            }

            tokenResults.forEach { res ->
                var expanded by remember(expandAll) { mutableStateOf(expandAll) }
                val a = res.tokenAnalysis
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${res.position}.",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text(res.acceptedArabic, style = ArabicTokenStyle)
                                }
                                Text(
                                    text = "(${a.nahwuPosition})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Rincian kata ke-${res.position}"
                                )
                            }
                        }

                        if (expanded) {
                            Spacer(Modifier.height(6.dp))
                            DetailKeyValue("1. Jenis Kata", a.wordType)
                            DetailKeyValue("2. Kedudukan Nahwu", a.nahwuPosition)
                            DetailKeyValue("3. Status Mu'rab / Mabni", a.irabOrBinaStatus)
                            DetailKeyValue("4. Hukum I'rab", a.irabHukum)
                            DetailKeyValue("5. Tanda I'rab & Jenisnya", "${a.irabSign} (${a.signType} • ${a.manifestation})")
                            DetailKeyValue("6. Alasan Nahwu", a.nahwuReason)
                            DetailKeyValue("7. Penyebab / Amil", a.amil)
                            DetailKeyValue("8. Mengapa Tidak Tanda Lain", a.whyNotOtherSign)
                            if (a.binaType.isNotBlank() || a.mahalIrab.isNotBlank()) {
                                DetailKeyValue("9. Rincian Mabni & Mahal", "${a.binaType} — ${a.binaReason} | Mahal: ${a.mahalIrab}")
                            }
                            DetailKeyValue("10. Hubungan dengan Kata Lain", a.relationToOtherWords)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LabeledParagraph(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(text.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
internal fun DetailKeyValue(key: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$key:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(148.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
