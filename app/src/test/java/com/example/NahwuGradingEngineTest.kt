package com.example

import com.example.data.CurriculumSeedData
import com.example.domain.engine.*
import com.example.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class NahwuGradingEngineTest {

    private fun buildCorrectEndingOnlyInputs(versionContent: QuestionVersionContent): List<PlayerTokenInput> {
        val pattern = versionContent.answerPatterns.first()
        return versionContent.baseTokens.map { token ->
            val analysis = pattern.tokenAnalyses.first { it.tokenId == token.tokenId }
            val parsed = ArabicHarakat.extractMarksPerBaseLetter(analysis.voweledArabic)
            val slots = ArabicHarakat.initialPlayerSlots(token).mapIndexed { idx, slot ->
                if (idx == parsed.lastIndex) {
                    val mark = parsed[idx].second
                    slot.copy(
                        hasShadda = mark.contains(ArabicHarakat.SHADDA),
                        vowelMark = mark.replace(ArabicHarakat.SHADDA, "")
                    )
                } else {
                    slot // intentionally empty optional beginning & middle!
                }
            }
            PlayerTokenInput(
                tokenId = token.tokenId,
                position = token.position,
                baseText = token.baseText,
                slots = slots
            )
        }
    }

    @Test
    fun `all curriculum seed questions have strictly 10 tokens and complete patterns`() {
        val seeds = CurriculumSeedData.buildInitialQuestions()
        assertTrue(seeds.isNotEmpty())
        seeds.forEach { spec ->
            assertEquals("Question ${spec.code} must have 10 baseTokens", 10, spec.versionContent.baseTokens.size)
            assertTrue("Question ${spec.code} must have at least 1 answerPattern", spec.versionContent.answerPatterns.isNotEmpty())
            spec.versionContent.answerPatterns.forEach { pat ->
                assertEquals("Pattern ${pat.patternId} on ${spec.code} must have 10 tokenAnalyses", 10, pat.tokenAnalyses.size)
            }
        }
    }

    @Test
    fun `ending harakat only with empty optional letters is evaluated as CORRECT`() {
        val q1 = CurriculumSeedData.buildInitialQuestions().first().versionContent
        val inputs = buildCorrectEndingOnlyInputs(q1)

        val result = NahwuGradingEngine.evaluate(q1, inputs)

        assertEquals(EvaluationStatus.CORRECT, result.overallStatus)
        assertEquals(100, result.score)
        assertEquals(10, result.correctWordCount)
        assertEquals(0, result.wrongWordCount)
        assertEquals(0, result.incompleteWordCount)
        assertTrue(result.tokenResults.any { it.skippedOptionalCount > 0 })
    }

    @Test
    fun `empty required ending harakat is marked INCOMPLETE and not WRONG`() {
        val q1 = CurriculumSeedData.buildInitialQuestions().first().versionContent
        val inputs = buildCorrectEndingOnlyInputs(q1).toMutableList()
        // Clear the ending mark of token 1 (المبتدا)
        val firstToken = inputs[0]
        val clearedSlots = firstToken.slots.map { it.copy(vowelMark = "", hasShadda = false) }
        inputs[0] = firstToken.copy(slots = clearedSlots)

        val result = NahwuGradingEngine.evaluate(q1, inputs)

        assertEquals(EvaluationStatus.INCOMPLETE, result.overallStatus)
        assertEquals(0, result.wrongWordCount)
        assertEquals(1, result.incompleteWordCount)
        assertEquals(EvaluationStatus.INCOMPLETE, result.tokenResults[0].status)
        assertTrue(result.tokenResults[0].incompleteLetters.isNotEmpty())
    }

    @Test
    fun `wrong optional beginning or middle harakat is marked WRONG with letter explanation`() {
        val q1 = CurriculumSeedData.buildInitialQuestions().first().versionContent
        val inputs = buildCorrectEndingOnlyInputs(q1).toMutableList()
        // Token 2 is نور -> نُورٌ; put wrong vowel (Kasrah) on first letter ن
        val tok2 = inputs[1]
        val modifiedSlots = tok2.slots.toMutableList()
        modifiedSlots[0] = modifiedSlots[0].copy(vowelMark = ArabicHarakat.KASRAH)
        inputs[1] = tok2.copy(slots = modifiedSlots)

        val result = NahwuGradingEngine.evaluate(q1, inputs)

        assertEquals(EvaluationStatus.WRONG, result.overallStatus)
        assertEquals(1, result.wrongWordCount)
        val tok2Res = result.tokenResults[1]
        assertEquals(EvaluationStatus.WRONG, tok2Res.status)
        assertEquals(1, tok2Res.wrongLetters.size)
        assertEquals(0, tok2Res.wrongLetters[0].letterIndex)
    }

    @Test
    fun `correct optional beginning and middle harakat is accepted as CORRECT`() {
        val q1 = CurriculumSeedData.buildInitialQuestions().first().versionContent
        val inputs = buildCorrectEndingOnlyInputs(q1).toMutableList()
        // Token 2 is نُورٌ -> fill first letter ن with Dhammah correctly
        val tok2 = inputs[1]
        val modifiedSlots = tok2.slots.toMutableList()
        modifiedSlots[0] = modifiedSlots[0].copy(vowelMark = ArabicHarakat.DHAMMAH)
        inputs[1] = tok2.copy(slots = modifiedSlots)

        val result = NahwuGradingEngine.evaluate(q1, inputs)

        assertEquals(EvaluationStatus.CORRECT, result.overallStatus)
        assertEquals(10, result.correctWordCount)
    }

    @Test
    fun `multiple valid answer patterns accept non-primary verified pattern`() {
        val multiPatternQuestion = CurriculumSeedData.buildInitialQuestions()
            .firstOrNull { it.versionContent.answerPatterns.size > 1 }
            ?: error("Expected at least one multi-pattern seed question")
        val content = multiPatternQuestion.versionContent
        val altPattern = content.answerPatterns.first { !it.primary }

        // Build inputs matching ONLY the alternative pattern
        val altInputs = content.baseTokens.map { token ->
            val analysis = altPattern.tokenAnalyses.first { it.tokenId == token.tokenId }
            val parsed = ArabicHarakat.extractMarksPerBaseLetter(analysis.voweledArabic)
            val slots = ArabicHarakat.initialPlayerSlots(token).mapIndexed { idx, slot ->
                if (idx == parsed.lastIndex) {
                    val mark = parsed[idx].second
                    slot.copy(
                        hasShadda = mark.contains(ArabicHarakat.SHADDA),
                        vowelMark = mark.replace(ArabicHarakat.SHADDA, "")
                    )
                } else slot
            }
            PlayerTokenInput(token.tokenId, token.position, token.baseText, slots)
        }

        val result = NahwuGradingEngine.evaluate(content, altInputs)

        assertEquals(EvaluationStatus.CORRECT, result.overallStatus)
        assertEquals(altPattern.patternId, result.matchedPattern.patternId)
        assertTrue(result.matchedAlternativePattern)
    }
}
