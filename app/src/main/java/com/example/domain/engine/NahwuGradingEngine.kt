package com.example.domain.engine

import com.example.domain.model.AnswerPattern
import com.example.domain.model.AnswerPatternToken
import com.example.domain.model.QuestionVersionContent
import com.example.domain.model.TokenLetterRule

enum class EvaluationStatus(val labelId: String) {
    CORRECT("BENAR"),
    WRONG("SALAH"),
    INCOMPLETE("BELUM LENGKAP")
}

data class LetterErrorDetail(
    val letterIndex: Int,
    val baseLetter: String,
    val roleLabel: String,
    val playerMark: String,
    val expectedMarks: List<String>,
    val reason: String
)

data class LetterIncompleteDetail(
    val letterIndex: Int,
    val baseLetter: String,
    val roleLabel: String,
    val expectedMarks: List<String>,
    val reason: String
)

data class TokenEvaluationResult(
    val tokenId: String,
    val position: Int,
    val baseText: String,
    val playerArabic: String,
    val acceptedArabic: String,
    val status: EvaluationStatus,
    val wrongLetters: List<LetterErrorDetail>,
    val incompleteLetters: List<LetterIncompleteDetail>,
    val skippedOptionalCount: Int,
    val explanation: String,
    val tokenAnalysis: AnswerPatternToken
)

data class EvaluationResult(
    val overallStatus: EvaluationStatus,
    val score: Int,
    val matchedPattern: AnswerPattern,
    val matchedAlternativePattern: Boolean,
    val correctWordCount: Int,
    val wrongWordCount: Int,
    val incompleteWordCount: Int,
    val errorSummary: String,
    val tokenResults: List<TokenEvaluationResult>
)

/**
 * Deterministic rule-based Nahwu answer evaluation engine implementing Bagian 6 and Bagian 8.
 * Never compares raw Arabic strings blindly; inspects tokenId, base letters, added letters,
 * letter positions, harakat canonicalization, and required vs optional rules across all verified patterns.
 */
object NahwuGradingEngine {

    fun evaluate(
        versionContent: QuestionVersionContent,
        playerInputs: List<PlayerTokenInput>
    ): EvaluationResult {
        val patterns = versionContent.answerPatterns
        require(patterns.isNotEmpty()) { "Soal tidak memiliki pola jawaban yang terverifikasi." }

        val evaluations = patterns.map { pattern ->
            evaluateAgainstPattern(pattern, playerInputs)
        }

        // Priority:
        // 1. Any pattern that yields overallStatus == CORRECT (preferring primary == true).
        // 2. Highest score, fewest wrong words, fewest incomplete words, preferring primary == true.
        val chosen = evaluations.sortedWith(
            compareByDescending<EvaluationResult> { it.overallStatus == EvaluationStatus.CORRECT }
                .thenByDescending { it.correctWordCount }
                .thenBy { it.wrongWordCount }
                .thenBy { it.incompleteWordCount }
                .thenByDescending { it.matchedPattern.primary }
        ).first()

        return chosen
    }

    private fun evaluateAgainstPattern(
        pattern: AnswerPattern,
        playerInputs: List<PlayerTokenInput>
    ): EvaluationResult {
        val analysisByTokenId = pattern.tokenAnalyses.associateBy { it.tokenId }
        val analysisByPos = pattern.tokenAnalyses.associateBy { it.position }

        val tokenResults = playerInputs.sortedBy { it.position }.map { input ->
            val tokenAnalysis = analysisByTokenId[input.tokenId]
                ?: analysisByPos[input.position]
                ?: AnswerPatternToken(
                    tokenId = input.tokenId,
                    position = input.position,
                    baseText = input.baseText,
                    voweledArabic = input.baseText
                )
            evaluateSingleToken(input, tokenAnalysis)
        }

        val correctCount = tokenResults.count { it.status == EvaluationStatus.CORRECT }
        val wrongCount = tokenResults.count { it.status == EvaluationStatus.WRONG }
        val incompleteCount = tokenResults.count { it.status == EvaluationStatus.INCOMPLETE }

        val overallStatus = when {
            wrongCount > 0 -> EvaluationStatus.WRONG
            incompleteCount > 0 -> EvaluationStatus.INCOMPLETE
            else -> EvaluationStatus.CORRECT
        }

        val score = if (tokenResults.isEmpty()) 0 else ((correctCount * 100) / tokenResults.size)

        val summary = buildString {
            when (overallStatus) {
                EvaluationStatus.CORRECT -> {
                    append("MasyaAllah! Seluruh $correctCount kata memenuhi kaidah Nahwu pada pola yang diterima.")
                    if (!pattern.primary) {
                        append(" Jawaban Anda sesuai dengan pola alternatif sah (${pattern.label}).")
                    }
                }
                EvaluationStatus.INCOMPLETE -> {
                    append("Ada $incompleteCount kata yang harakat wajibnya (mahalul i'rab) belum diisi. Lengkapi bagian wajib terlebih dahulu sebelum menilai kesalahan.")
                }
                EvaluationStatus.WRONG -> {
                    append("Terdapat $wrongCount kata dengan harakat yang tidak sesuai kaidah")
                    if (incompleteCount > 0) {
                        append(" serta $incompleteCount kata yang harakat wajibnya belum dilengkapi")
                    }
                    append(". Periksa rincian per huruf pada tabel perbandingan di bawah.")
                }
            }
        }

        return EvaluationResult(
            overallStatus = overallStatus,
            score = score,
            matchedPattern = pattern,
            matchedAlternativePattern = !pattern.primary && overallStatus == EvaluationStatus.CORRECT,
            correctWordCount = correctCount,
            wrongWordCount = wrongCount,
            incompleteWordCount = incompleteCount,
            errorSummary = summary,
            tokenResults = tokenResults
        )
    }

    private fun evaluateSingleToken(
        input: PlayerTokenInput,
        analysis: AnswerPatternToken
    ): TokenEvaluationResult {
        val rules = analysis.letterRules.ifEmpty {
            ArabicHarakat.buildDefaultLetterRules(analysis.voweledArabic.ifBlank { analysis.baseText })
        }

        val wrongLetters = mutableListOf<LetterErrorDetail>()
        val incompleteLetters = mutableListOf<LetterIncompleteDetail>()
        var skippedOptionalCount = 0

        // Check if player altered base letters (Sharaf / extra letter check)
        val playerBaseNoMarks = ArabicHarakat.stripHarakat(input.renderedBaseWithoutHarakat())
        val expectedBaseForms = buildSet {
            add(ArabicHarakat.stripHarakat(analysis.baseText))
            analysis.acceptedForms.forEach { add(ArabicHarakat.stripHarakat(it)) }
            add(ArabicHarakat.stripHarakat(analysis.voweledArabic))
        }.filter { it.isNotBlank() }

        val letterShapeValid = expectedBaseForms.isEmpty() || expectedBaseForms.contains(playerBaseNoMarks)
        if (!letterShapeValid) {
            wrongLetters.add(
                LetterErrorDetail(
                    letterIndex = (input.slots.lastIndex).coerceAtLeast(0),
                    baseLetter = playerBaseNoMarks,
                    roleLabel = "Bentuk Huruf / Sharaf",
                    playerMark = playerBaseNoMarks,
                    expectedMarks = expectedBaseForms,
                    reason = "Bentuk susunan huruf '$playerBaseNoMarks' tidak cocok dengan bentuk Sharaf yang diterima (${expectedBaseForms.joinToString(" / ")})."
                )
            )
        } else {
            // Check per-letter rules
            // Match against slots corresponding to either base or accepted form
            val targetParsed = ArabicHarakat.extractMarksPerBaseLetter(
                analysis.voweledArabic.ifBlank { analysis.baseText }
            )
            rules.forEach { rule ->
                val slot = input.slots.getOrNull(rule.letterIndex)
                val roleLabel = when (rule.role) {
                    "beginning" -> "Awal kata"
                    "middle" -> "Tengah kata"
                    "ending" -> "Akhir kata (Mahalul I'rab)"
                    else -> "Huruf ke-${rule.letterIndex + 1}"
                }

                if (slot == null) {
                    if (rule.required) {
                        incompleteLetters.add(
                            LetterIncompleteDetail(
                                letterIndex = rule.letterIndex,
                                baseLetter = rule.baseLetter,
                                roleLabel = roleLabel,
                                expectedMarks = rule.acceptedMarks,
                                reason = "Huruf '${rule.baseLetter}' ($roleLabel) wajib diberi harakat (${rule.acceptedMarks.joinToString(" / ") { ArabicHarakat.describeMark(it) }})."
                            )
                        )
                    }
                    return@forEach
                }

                val playerMark = slot.canonicalMark()
                val acceptedCanonical = rule.acceptedMarks.map { ArabicHarakat.canonicalizeMark(it) }
                    .ifEmpty {
                        listOf(targetParsed.getOrNull(rule.letterIndex)?.second.orEmpty())
                    }

                if (playerMark.isEmpty()) {
                    // Rule 1, 2, 6, 10:
                    // Unfilled optional letter is NOT wrong and NOT incomplete!
                    // Unfilled required letter IS incomplete!
                    if (rule.required && !acceptedCanonical.contains("")) {
                        incompleteLetters.add(
                            LetterIncompleteDetail(
                                letterIndex = rule.letterIndex,
                                baseLetter = slot.baseChar,
                                roleLabel = roleLabel,
                                expectedMarks = acceptedCanonical,
                                reason = "Bagian wajib ($roleLabel) pada huruf '${slot.baseChar}' belum diberi harakat. Seharusnya: ${
                                    acceptedCanonical.joinToString(" atau ") { ArabicHarakat.describeMark(it) }
                                }."
                            )
                        )
                    } else {
                        skippedOptionalCount++
                    }
                } else {
                    // Player entered a harakat on this letter (required or optional):
                    // Rule 4 & 5: If valid -> accepted; if wrong -> MUST be marked wrong!
                    val isAccepted = acceptedCanonical.contains(playerMark)
                    if (!isAccepted) {
                        wrongLetters.add(
                            LetterErrorDetail(
                                letterIndex = rule.letterIndex,
                                baseLetter = slot.baseChar,
                                roleLabel = roleLabel,
                                playerMark = playerMark,
                                expectedMarks = acceptedCanonical,
                                reason = "Harakat ${ArabicHarakat.describeMark(playerMark)} pada huruf '${slot.baseChar}' ($roleLabel) tidak sesuai. Seharusnya ${
                                    acceptedCanonical.joinToString(" atau ") { ArabicHarakat.describeMark(it) }
                                }. ${rule.explanation}"
                            )
                        )
                    }
                }
            }

            // Also check any player-added extra slots beyond rules count
            if (input.slots.size > rules.size) {
                for (i in rules.size until input.slots.size) {
                    val extraSlot = input.slots[i]
                    val targetMark = targetParsed.getOrNull(i)?.second.orEmpty()
                    val playerMark = extraSlot.canonicalMark()
                    if (playerMark.isNotEmpty() && playerMark != targetMark) {
                        wrongLetters.add(
                            LetterErrorDetail(
                                letterIndex = i,
                                baseLetter = extraSlot.baseChar,
                                roleLabel = "Huruf tambahan",
                                playerMark = playerMark,
                                expectedMarks = listOf(targetMark),
                                reason = "Harakat ${ArabicHarakat.describeMark(playerMark)} pada huruf tambahan '${extraSlot.baseChar}' tidak tepat."
                            )
                        )
                    }
                }
            }
        }

        val tokenStatus = when {
            wrongLetters.isNotEmpty() -> EvaluationStatus.WRONG
            incompleteLetters.isNotEmpty() -> EvaluationStatus.INCOMPLETE
            else -> EvaluationStatus.CORRECT
        }

        val explanation = when (tokenStatus) {
            EvaluationStatus.CORRECT -> buildString {
                append("Tepat! Kedudukan: ${analysis.nahwuPosition} (${analysis.irabHukum}, tanda ${analysis.irabSign}).")
                if (skippedOptionalCount > 0) {
                    append(" Bagian awal/tengah yang tidak wajib diisi tetap diterima sesuai kaidah.")
                }
            }
            EvaluationStatus.INCOMPLETE -> incompleteLetters.joinToString(" ") { it.reason }
            EvaluationStatus.WRONG -> buildString {
                append(wrongLetters.joinToString(" ") { it.reason })
                if (incompleteLetters.isNotEmpty()) {
                    append(" ")
                    append(incompleteLetters.joinToString(" ") { it.reason })
                }
            }
        }

        return TokenEvaluationResult(
            tokenId = input.tokenId,
            position = input.position,
            baseText = input.baseText,
            playerArabic = input.renderArabic(),
            acceptedArabic = analysis.voweledArabic.ifBlank { analysis.baseText },
            status = tokenStatus,
            wrongLetters = wrongLetters,
            incompleteLetters = incompleteLetters,
            skippedOptionalCount = skippedOptionalCount,
            explanation = explanation,
            tokenAnalysis = analysis
        )
    }
}
