package com.example.domain.engine

import com.example.domain.model.QuestionToken
import com.example.domain.model.TokenLetterRule

object ArabicHarakat {
    const val FATHAH = "\u064E"          // َ
    const val KASRAH = "\u0650"          // ِ
    const val DHAMMAH = "\u064F"         // ُ
    const val SUKUN = "\u0652"           // ْ
    const val SHADDA = "\u0651"          // ّ
    const val TANWIN_FATHAH = "\u064B"   // ً
    const val TANWIN_KASRAH = "\u064D"   // ٍ
    const val TANWIN_DHAMMAH = "\u064C"  // ٌ
    const val ALIF_KHANJARIYAH = "\u0670"// ٰ

    val ALL_MARKS: Set<Char> = setOf(
        '\u064B', '\u064C', '\u064D', '\u064E', '\u064F',
        '\u0650', '\u0651', '\u0652', '\u0670', '\u0653', '\u0654', '\u0655'
    )

    val VOWEL_OPTIONS = listOf(
        HarakatOption("Fathah", FATHAH, "َ"),
        HarakatOption("Kasrah", KASRAH, "ِ"),
        HarakatOption("Dhammah", DHAMMAH, "ُ"),
        HarakatOption("Sukun", SUKUN, "ْ"),
        HarakatOption("Tanwin Fathah", TANWIN_FATHAH, "ً"),
        HarakatOption("Tanwin Kasrah", TANWIN_KASRAH, "ٍ"),
        HarakatOption("Tanwin Dhammah", TANWIN_DHAMMAH, "ٌ"),
        HarakatOption("Alif Khanjariyah", ALIF_KHANJARIYAH, "ٰ")
    )

    data class HarakatOption(
        val label: String,
        val code: String,
        val preview: String
    )

    fun stripHarakat(input: String): String =
        input.filter { it !in ALL_MARKS }

    fun canonicalizeMark(rawMark: String): String {
        if (rawMark.isBlank()) return ""
        val hasShadda = rawMark.contains(SHADDA)
        val nonShadda = rawMark.filter { it != '\u0651' && it in ALL_MARKS }
        return buildString {
            if (hasShadda) append(SHADDA)
            append(nonShadda)
        }
    }

    fun describeMark(mark: String): String {
        val canonical = canonicalizeMark(mark)
        if (canonical.isEmpty()) return "Tanpa harakat"
        val hasShadda = canonical.contains(SHADDA)
        val vowel = canonical.replace(SHADDA, "")
        val vowelName = when (vowel) {
            FATHAH -> "Fathah"
            KASRAH -> "Kasrah"
            DHAMMAH -> "Dhammah"
            SUKUN -> "Sukun"
            TANWIN_FATHAH -> "Tanwin Fathah"
            TANWIN_KASRAH -> "Tanwin Kasrah"
            TANWIN_DHAMMAH -> "Tanwin Dhammah"
            ALIF_KHANJARIYAH -> "Alif Khanjariyah"
            "" -> ""
            else -> "Harakat ($vowel)"
        }
        return when {
            hasShadda && vowelName.isNotEmpty() -> "Tasydid + $vowelName"
            hasShadda -> "Tasydid"
            else -> vowelName
        }
    }

    /**
     * Parses a voweled Arabic word into per-base-letter canonical harakat strings.
     */
    fun extractMarksPerBaseLetter(voweledWord: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        var currentLetter: Char? = null
        val marks = StringBuilder()
        for (ch in voweledWord) {
            if (ch in ALL_MARKS) {
                if (currentLetter != null) {
                    marks.append(ch)
                }
            } else if (!ch.isWhitespace()) {
                if (currentLetter != null) {
                    result.add(currentLetter.toString() to canonicalizeMark(marks.toString()))
                    marks.clear()
                }
                currentLetter = ch
            }
        }
        if (currentLetter != null) {
            result.add(currentLetter.toString() to canonicalizeMark(marks.toString()))
        }
        return result
    }

    /**
     * Generates default TokenLetterRules from a voweled Arabic word:
     * Only the last letter (mahalul i'rab) is required by default, while beginning & middle letters
     * remain optional unless explicitly set otherwise by the author/reviewer.
     */
    fun buildDefaultLetterRules(
        voweledWord: String,
        endingExplanation: String = "Harakat akhir sesuai kedudukan i'rab kata dalam kalimat."
    ): List<TokenLetterRule> {
        val parsed = extractMarksPerBaseLetter(voweledWord)
        if (parsed.isEmpty()) return emptyList()
        return parsed.mapIndexed { index, (letter, mark) ->
            val role = when (index) {
                0 -> "beginning"
                parsed.lastIndex -> "ending"
                else -> "middle"
            }
            val isEnding = index == parsed.lastIndex
            val accepted = if (mark.isEmpty()) listOf("") else listOf(mark)
            TokenLetterRule(
                letterIndex = index,
                baseLetter = letter,
                role = role,
                required = isEnding,
                acceptedMarks = accepted,
                explanation = if (isEnding) endingExplanation else "Harakat internal huruf '$letter' (${describeMark(mark)})."
            )
        }
    }

    fun initialPlayerSlots(token: QuestionToken): List<PlayerLetterSlot> {
        val letters = stripHarakat(token.baseText)
        return letters.mapIndexed { index, ch ->
            PlayerLetterSlot(
                slotIndex = index,
                baseChar = ch.toString(),
                isAddedLetter = false,
                vowelMark = "",
                hasShadda = false
            )
        }
    }
}

data class PlayerLetterSlot(
    val slotIndex: Int,
    val baseChar: String,
    val isAddedLetter: Boolean = false,
    val vowelMark: String = "",
    val hasShadda: Boolean = false
) {
    fun canonicalMark(): String =
        ArabicHarakat.canonicalizeMark((if (hasShadda) ArabicHarakat.SHADDA else "") + vowelMark)

    fun hasAnyMark(): Boolean = hasShadda || vowelMark.isNotEmpty()

    fun renderArabic(): String = baseChar + canonicalMark()
}

data class PlayerTokenInput(
    val tokenId: String,
    val position: Int,
    val baseText: String,
    val slots: List<PlayerLetterSlot>
) {
    fun renderArabic(): String = slots.joinToString(separator = "") { it.renderArabic() }
    fun renderedBaseWithoutHarakat(): String = slots.joinToString(separator = "") { it.baseChar }
}
