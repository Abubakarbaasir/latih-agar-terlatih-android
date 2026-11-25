package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.model.AnswerPattern
import com.example.ui.theme.ArabicInlineStyle

@Composable
internal fun SectionFTarkibAnalysis(pattern: AnswerPattern) {
    val t = pattern.tarkibAnalysis
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("eval_section_f")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EvaluationSectionHeader("F", "Pembahasan Tarkib (Struktur Kalimat)")
            LabeledParagraph("Jenis Kalimat", t.sentenceType)
            LabeledParagraph("Unsur Utama Kalimat", t.mainElements)
            if (t.structureHierarchy.isNotEmpty()) {
                Text(
                    text = "Struktur Kalimat Bertingkat",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                t.structureHierarchy.forEach { step ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            LabeledParagraph("Hubungan Antarunsur", t.elementRelationships)
            LabeledParagraph("Konstruksi Makna", t.meaningConstruction)
        }
    }
}

@Composable
internal fun SectionGWordRelations(pattern: AnswerPattern) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("eval_section_g")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EvaluationSectionHeader("G", "Pembahasan Hubungan Antarkata")
            if (pattern.wordRelations.isEmpty()) {
                Text(
                    text = "Hubungan antarkata terangkum pada tabel I'rab dan Tarkib di atas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                pattern.wordRelations.forEach { rel ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${rel.connectedWords} (${rel.relationType})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            DetailKeyValue("Pengaruh I'rab", rel.irabEffect)
                            DetailKeyValue("Alasan", rel.reason)
                            DetailKeyValue("Dampak Makna", rel.meaningImpact)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionHGandulBreakdown(pattern: AnswerPattern) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("eval_section_h")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EvaluationSectionHeader("H", "Pembahasan Gandul (Gaya Pesantren)")
            pattern.gandulEntries.forEach { entry ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${entry.tokenPosition}.",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = entry.arabicWord,
                                style = ArabicInlineStyle,
                                modifier = Modifier.widthIn(min = 68.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${entry.nahwuFunction} → \"${entry.gandulMeaning}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (entry.relationNote.isNotBlank()) {
                                Text(
                                    text = entry.relationNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionITranslations(pattern: AnswerPattern) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("eval_section_i")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EvaluationSectionHeader("I", "Terjemahan Lengkap")
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("1. Terjemahan Gandul (Pesantren)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Text(pattern.translationGandul.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("2. Terjemahan Bahasa Indonesia Alami", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(pattern.translationNatural.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
internal fun SectionJKaidahCards(pattern: AnswerPattern) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("eval_section_j")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EvaluationSectionHeader("J", "Kaidah Nahwu yang Terkandung")
            pattern.kaidahList.forEach { k ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(k.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        DetailKeyValue("Definisi", k.definition)
                        DetailKeyValue("Rumus / Pola", k.formula)
                        DetailKeyValue("Syarat Berlaku", k.conditions)
                        DetailKeyValue("Pengaruh I'rab", k.irabEffect)
                        DetailKeyValue("Penerapan Kalimat", k.applicationInSentence)
                        if (k.extraExamples.isNotEmpty()) {
                            Text("Contoh Tambahan:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            k.extraExamples.forEach { ex ->
                                Text("• $ex", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionKConclusionAndActions(
    pattern: AnswerPattern,
    onNextQuestion: () -> Unit,
    onRepeatReview: () -> Unit,
    onBackToGame: () -> Unit
) {
    val summary = pattern.learningSummary
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("eval_section_k")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EvaluationSectionHeader("K", "Kesimpulan Pembelajaran & Aksi")

            if (summary.mainRulesLearned.isNotEmpty()) {
                Text("Poin Kaidah Utama:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                summary.mainRulesLearned.forEach { rule ->
                    Text("• $rule", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            if (summary.structureConclusion.isNotBlank()) {
                Text("Kesimpulan Struktur: ${summary.structureConclusion}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            if (summary.keyHarakatReasons.isNotEmpty()) {
                Text("Ringkasan Alasan Harakat:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                summary.keyHarakatReasons.forEach { reason ->
                    Text("• $reason", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = onNextQuestion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("next_question_button")
            ) {
                Text("Soal Berikutnya", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRepeatReview,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("repeat_review_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ulangi Pembahasan")
                }
                OutlinedButton(
                    onClick = onBackToGame,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("back_to_game_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kembali ke Permainan")
                }
            }
        }
    }
}
