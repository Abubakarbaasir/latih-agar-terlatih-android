package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import com.example.data.CurriculumSeedData
import com.example.domain.engine.ArabicHarakat
import com.example.domain.engine.EvaluationStatus
import com.example.domain.engine.NahwuGradingEngine
import com.example.domain.engine.PlayerTokenInput
import com.example.domain.model.*
import com.example.ui.screens.EvaluationResultContent
import com.example.ui.screens.GamePlayScreen
import com.example.ui.theme.LatihNahwuTheme
import com.example.ui.viewmodel.GameSessionUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LatihNahwuRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `evaluation screen renders all 11 sections A through K and action buttons`() {
        val seed = CurriculumSeedData.buildInitialQuestions().first()
        val content = seed.versionContent
        val question = Question(
            id = seed.id,
            code = seed.code,
            categoryId = seed.categoryId,
            categoryCode = seed.categoryCode,
            categoryName = seed.categoryName,
            title = seed.title,
            instruction = seed.instruction,
            difficulty = seed.difficulty.value,
            status = QuestionStatus.PUBLISHED.value,
            latestVersionData = content,
            publishedVersionData = content
        )
        val inputs = content.baseTokens.map { tok ->
            PlayerTokenInput(tok.tokenId, tok.position, tok.baseText, ArabicHarakat.initialPlayerSlots(tok))
        }
        val eval = NahwuGradingEngine.evaluate(content, inputs)
        assertEquals(EvaluationStatus.INCOMPLETE, eval.overallStatus)

        var nextClicked = false
        composeRule.setContent {
            LatihNahwuTheme {
                EvaluationResultContent(
                    question = question,
                    evaluation = eval,
                    onNextQuestion = { nextClicked = true },
                    onBackToGame = {}
                )
            }
        }
        composeRule.waitForIdle()

        val sectionKeys = listOf(
            "section_a" to "eval_section_a",
            "section_b" to "eval_section_b",
            "section_c" to "eval_section_c",
            "section_d" to "eval_section_d",
            "section_e" to "eval_section_e",
            "section_f" to "eval_section_f",
            "section_g" to "eval_section_g",
            "section_h" to "eval_section_h",
            "section_i" to "eval_section_i",
            "section_j" to "eval_section_j",
            "section_k" to "eval_section_k"
        )
        sectionKeys.forEach { (key, tag) ->
            composeRule.onNodeWithTag("evaluation_list").performScrollToKey(key)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(tag).assertExists()
        }

        composeRule.onNodeWithTag("next_question_button").performScrollTo().performClick()
        assertTrue(nextClicked)
    }

    @Test
    fun `gameplay screen displays 10 Arabic tokens and handles token selection and PERIKSA`() {
        val seed = CurriculumSeedData.buildInitialQuestions().first()
        val content = seed.versionContent
        val question = Question(
            id = seed.id,
            code = seed.code,
            categoryId = seed.categoryId,
            categoryCode = seed.categoryCode,
            categoryName = seed.categoryName,
            title = seed.title,
            instruction = seed.instruction,
            difficulty = seed.difficulty.value,
            status = QuestionStatus.PUBLISHED.value,
            latestVersionData = content,
            publishedVersionData = content
        )
        val inputs = content.baseTokens.map { tok ->
            PlayerTokenInput(tok.tokenId, tok.position, tok.baseText, ArabicHarakat.initialPlayerSlots(tok))
        }
        val state = GameSessionUiState(
            activeCategory = Category(id = "cat_mub", code = "MUB", name = "Mubtada dan Khabar"),
            session = PlayerSession(id = "sess_1", userId = "u1", categoryId = "cat_mub", categoryName = "Mubtada dan Khabar"),
            currentQuestion = question,
            playerInputs = inputs,
            selectedTokenIndex = 0,
            selectedLetterIndex = inputs.first().slots.lastIndex
        )

        var selectedTokenIdx = -1
        var selectedHarakat = ""
        var checkClicked = false

        composeRule.setContent {
            LatihNahwuTheme {
                GamePlayScreen(
                    gameState = state,
                    onSelectToken = { selectedTokenIdx = it },
                    onSelectLetter = {},
                    onApplyVowel = { selectedHarakat = it },
                    onToggleShadda = {},
                    onClearLetter = {},
                    onResetToken = {},
                    onAppendSharafLetter = {},
                    onRemoveSharafLetter = {},
                    onCheckAnswer = { checkClicked = true },
                    onOpenHistoryItem = {},
                    onEndSession = {}
                )
            }
        }
        composeRule.waitForIdle()

        for (i in 1..10) {
            composeRule.onNodeWithTag("arabic_token_$i").performScrollTo().assertIsDisplayed()
        }

        composeRule.onNodeWithTag("arabic_token_2").performScrollTo().performClick()
        assertEquals(1, selectedTokenIdx)

        composeRule.onNodeWithTag("harakat_dhammah").performScrollTo().performClick()
        assertEquals(ArabicHarakat.DHAMMAH, selectedHarakat)

        composeRule.onNodeWithTag("check_answer_button").performClick()
        assertTrue(checkClicked)
    }
}
