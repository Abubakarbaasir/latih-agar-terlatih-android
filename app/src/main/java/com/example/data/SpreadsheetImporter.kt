package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.example.domain.engine.ArabicHarakat
import com.example.domain.model.*
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

data class SpreadsheetPreviewRow(
    val rowNumber: Int,
    val categoryCode: String,
    val title: String,
    val difficulty: String,
    val tokensCount: Int,
    val errors: List<String>,
    val parsedDraft: ParsedQuestionDraft?
) {
    val isValid: Boolean get() = errors.isEmpty() && parsedDraft != null
}

data class ParsedQuestionDraft(
    val categoryCode: String,
    val title: String,
    val instruction: String,
    val difficulty: QuestionDifficulty,
    val versionContent: QuestionVersionContent
)

/**
 * Parses both .csv (UTF-8) and .xlsx (Excel OpenXML via ZipInputStream + XmlPullParser)
 * and generates downloadable UTF-8 BOM CSV templates compatible with Excel/Sheets on Android.
 */
object SpreadsheetImporter {

    val REQUIRED_HEADERS = listOf(
        "category_code",
        "title",
        "instruction",
        "difficulty",
        "sentence_unvoweled",
        "sentence_voweled",
        "translation_natural",
        "translation_gandul",
        "tarkib_type",
        "tarkib_summary",
        "kaidah_name",
        "kaidah_explanation",
        "token_meanings_pipe",
        "token_positions_pipe",
        "token_hukum_pipe",
        "token_signs_pipe",
        "token_reasons_pipe"
    )

    fun generateCsvTemplate(): String {
        val bom = "\uFEFF"
        val header = REQUIRED_HEADERS.joinToString(",")
        val exampleRow = listOf(
            "FAI",
            "Contoh Soal Impor Excel",
            "Berilah harakat akhir yang tepat pada setiap kata.",
            "beginner",
            "كتب الطالب الدرس الجديد في الفصل صباحا مع زميله المجتهد",
            "كَتَبَ الطَّالِبُ الدَّرْسَ الْجَدِيدَ فِي الْفَصْلِ صَبَاحًا مَعَ زَمِيلِهِ الْمُجْتَهِدِ",
            "Pelajar itu menulis pelajaran baru di kelas pada pagi hari bersama temannya yang tekun.",
            "Wis nulis sopo murid ing pelajaran kang anyar ing dalem kelas ing wektu esuk sarta kancane kang temen-temen.",
            "Jumlah Fi'liyah",
            "Fi'il (كتب) + Fa'il (الطالب) + Maf'ul Bih (الدرس)",
            "Kaidah Fa'il dan Maf'ul Bih",
            "Fa'il wajib Marfu' setelah fi'il ma'lum dan Maf'ul Bih wajib Manshub.",
            "Menulis|Pelajar|Pelajaran|Yang baru|Di|Kelas|Pagi hari|Bersama|Temannya|Yang tekun",
            "Fi'il Madhi|Fa'il|Maf'ul Bih|Na'at|Harf Jar|Isim Majrur|Zharaf Zaman|Zharaf Makan|Mudhaf Ilaih|Na'at",
            "Mabni|Marfu'|Manshub|Manshub|Mabni|Majrur|Manshub|Manshub|Majrur|Majrur",
            "Fathah|Dhammah|Fathah|Fathah|Sukun|Kasrah|Tanwin Fathah|Fathah|Kasrah|Kasrah",
            "Fi'il madhi mabni|Pelaku pekerjaan|Objek penderita|Mengikuti الدرس|Huruf jar|Didahului في|Keterangan waktu|Keterangan tempat|Disandari مع|Mengikuti زميله"
        ).joinToString(",") { escapeCsv(it) }
        return "$bom$header\n$exampleRow\n"
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n")
        return if (needsQuotes) "\"${value.replace("\"", "\"\"")}\"" else value
    }

    fun parseUri(context: Context, uri: Uri, validCategoryCodes: Set<String>): Result<List<SpreadsheetPreviewRow>> {
        return try {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val name = uri.lastPathSegment.orEmpty().lowercase()
            val rows: List<List<String>> = if (mime.contains("spreadsheetml") || name.endsWith(".xlsx")) {
                context.contentResolver.openInputStream(uri)?.use { parseXlsx(it) }
                    ?: return Result.failure(IllegalStateException("File Excel tidak dapat dibuka."))
            } else {
                context.contentResolver.openInputStream(uri)?.use { parseCsv(it) }
                    ?: return Result.failure(IllegalStateException("File CSV tidak dapat dibuka."))
            }

            if (rows.isEmpty()) {
                return Result.failure(IllegalArgumentException("File kosong dan tidak memiliki header."))
            }
            val headers = rows.first().map { it.trim().lowercase().removePrefix("\uFEFF") }
            val missingHeaders = REQUIRED_HEADERS.filter { it !in headers }
            if (missingHeaders.isNotEmpty()) {
                return Result.failure(
                    IllegalArgumentException("Kolom wajib tidak ditemukan: ${missingHeaders.joinToString(", ")}")
                )
            }

            val headerIndex = headers.withIndex().associate { it.value to it.index }
            val preview = rows.drop(1)
                .filter { row -> row.any { it.isNotBlank() } }
                .mapIndexed { idx, row ->
                    validateAndParseRow(idx + 2, row, headerIndex, validCategoryCodes)
                }

            if (preview.isEmpty()) {
                return Result.failure(IllegalArgumentException("Tidak ada baris data di bawah header."))
            }
            Result.success(preview)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateAndParseRow(
        rowNumber: Int,
        row: List<String>,
        col: Map<String, Int>,
        validCategoryCodes: Set<String>
    ): SpreadsheetPreviewRow {
        fun cell(key: String): String = col[key]?.let { row.getOrNull(it) }?.trim().orEmpty()

        val errors = mutableListOf<String>()
        val catCode = cell("category_code").uppercase()
        val title = cell("title")
        val instruction = cell("instruction").ifBlank { "Tentukan harakat akhir setiap kata sesuai kaidah Nahwu." }
        val diffRaw = cell("difficulty").lowercase()
        val unvoweledRaw = cell("sentence_unvoweled")
        val voweledRaw = cell("sentence_voweled")
        val transNatural = cell("translation_natural")
        val transGandul = cell("translation_gandul")
        val tarkibType = cell("tarkib_type").ifBlank { "Jumlah Fi'liyah" }
        val tarkibSummary = cell("tarkib_summary")
        val kaidahName = cell("kaidah_name").ifBlank { "Kaidah Nahwu Kategori $catCode" }
        val kaidahExplanation = cell("kaidah_explanation")

        if (catCode.isBlank()) errors.add("category_code kosong")
        else if (validCategoryCodes.isNotEmpty() && catCode !in validCategoryCodes) {
            errors.add("Kode kategori '$catCode' tidak ditemukan")
        }
        if (title.length < 2) errors.add("title minimal 2 karakter")
        val difficulty = QuestionDifficulty.entries.firstOrNull { it.value == diffRaw }
        if (difficulty == null) errors.add("difficulty harus 'beginner', 'intermediate', atau 'advanced'")

        val unvoweledTokens = unvoweledRaw.split(Regex("\\s+")).filter { it.isNotBlank() }
        val voweledTokens = voweledRaw.split(Regex("\\s+")).filter { it.isNotBlank() }

        if (unvoweledTokens.size != 10) {
            errors.add("sentence_unvoweled harus tepat 10 kata (ditemukan ${unvoweledTokens.size})")
        }
        if (voweledTokens.size != 10) {
            errors.add("sentence_voweled harus tepat 10 kata (ditemukan ${voweledTokens.size})")
        }
        if (transNatural.isBlank()) errors.add("translation_natural wajib diisi")

        val meanings = cell("token_meanings_pipe").split("|").map { it.trim() }
        val positions = cell("token_positions_pipe").split("|").map { it.trim() }
        val hukums = cell("token_hukum_pipe").split("|").map { it.trim() }
        val signs = cell("token_signs_pipe").split("|").map { it.trim() }
        val reasons = cell("token_reasons_pipe").split("|").map { it.trim() }

        if (meanings.size != 10) errors.add("token_meanings_pipe harus 10 item dipisah '|'")
        if (positions.size != 10) errors.add("token_positions_pipe harus 10 item dipisah '|'")
        if (hukums.size != 10) errors.add("token_hukum_pipe harus 10 item dipisah '|'")
        if (signs.size != 10) errors.add("token_signs_pipe harus 10 item dipisah '|'")
        if (reasons.size != 10) errors.add("token_reasons_pipe harus 10 item dipisah '|'")

        val draft = if (errors.isEmpty()) {
            val baseKey = "xls_${rowNumber}_${System.currentTimeMillis()}"
            val baseTokens = unvoweledTokens.mapIndexed { idx, w ->
                QuestionToken(
                    tokenId = "${baseKey}_t${idx + 1}",
                    position = idx + 1,
                    baseText = ArabicHarakat.stripHarakat(w)
                )
            }
            val tokenAnalyses = voweledTokens.mapIndexed { idx, vw ->
                val baseWord = baseTokens[idx].baseText
                val hukum = hukums[idx]
                val isMabni = hukum.equals("Mabni", ignoreCase = true)
                AnswerPatternToken(
                    tokenId = baseTokens[idx].tokenId,
                    position = idx + 1,
                    baseText = baseWord,
                    voweledArabic = vw,
                    indonesianMeaning = meanings[idx],
                    wordType = if (isMabni) "Fi'il / Harf" else "Isim",
                    nahwuPosition = positions[idx],
                    irabOrBinaStatus = if (isMabni) "Mabni" else "Mu'rab",
                    irabHukum = hukum,
                    irabSign = signs[idx],
                    signType = if (isMabni) "Bina" else "Tanda Asli",
                    manifestation = "Lafzhi",
                    nahwuReason = reasons[idx],
                    amil = "Sesuai struktur kalimat",
                    whyNotOtherSign = "Karena kedudukannya ${positions[idx]} dengan hukum $hukum.",
                    binaType = if (isMabni) "Mabni ${signs[idx]}" else "",
                    binaReason = if (isMabni) reasons[idx] else "",
                    mahalIrab = if (isMabni) "Sesuai konteks" else "",
                    relationToOtherWords = "Terhubung dalam $tarkibType",
                    letterRules = ArabicHarakat.buildDefaultLetterRules(vw, reasons[idx])
                )
            }
            val pattern = AnswerPattern(
                patternId = "pat_xls_1",
                primary = true,
                label = "Pola Utama (Impor Excel)",
                voweledSentence = voweledRaw,
                translationNatural = transNatural,
                translationGandul = transGandul.ifBlank { transNatural },
                sentenceMeaningExplanation = transNatural,
                structureDifferenceNote = "Susunan kata mengikuti kaidah $tarkibType bahasa Arab.",
                tarkibAnalysis = TarkibAnalysis(
                    sentenceType = tarkibType,
                    mainElements = tarkibSummary.ifBlank { positions.take(3).joinToString(" + ") },
                    structureHierarchy = tokenAnalyses.map { "${it.position}. ${it.voweledArabic} (${it.nahwuPosition})" },
                    elementRelationships = tarkibSummary.ifBlank { kaidahExplanation },
                    meaningConstruction = transNatural
                ),
                wordRelations = listOf(
                    WordRelation(
                        1, 2,
                        "${voweledTokens[0]} ↔ ${voweledTokens[1]}",
                        "${positions[0]} & ${positions[1]}",
                        "Memengaruhi hukum ${hukums[1]}",
                        reasons[1],
                        transNatural
                    )
                ),
                gandulEntries = tokenAnalyses.map {
                    GandulEntry(it.position, it.voweledArabic, it.nahwuPosition, it.indonesianMeaning, it.nahwuReason)
                },
                kaidahList = listOf(
                    KaidahRule(
                        name = kaidahName,
                        definition = kaidahExplanation.ifBlank { "Kaidah Nahwu terkait $kaidahName." },
                        formula = positions.take(4).joinToString(" + "),
                        conditions = "Berlaku pada susunan $tarkibType",
                        irabEffect = "Menentukan tanda i'rab pada masing-masing kedudukan.",
                        applicationInSentence = voweledRaw,
                        extraExamples = emptyList()
                    )
                ),
                learningSummary = LearningSummary(
                    mainRulesLearned = listOf(kaidahName),
                    structureConclusion = tarkibSummary.ifBlank { tarkibType },
                    keyHarakatReasons = tokenAnalyses.take(5).map { "${it.voweledArabic}: ${it.nahwuReason}" }
                ),
                tokenAnalyses = tokenAnalyses
            )
            ParsedQuestionDraft(
                categoryCode = catCode,
                title = title,
                instruction = instruction,
                difficulty = difficulty!!,
                versionContent = QuestionVersionContent(
                    versionNumber = 1,
                    sentenceUnvoweled = unvoweledTokens.joinToString(" "),
                    translationNatural = transNatural,
                    translationGandul = transGandul.ifBlank { transNatural },
                    sentenceMeaningExplanation = transNatural,
                    baseTokens = baseTokens,
                    answerPatterns = listOf(pattern)
                )
            )
        } else null

        return SpreadsheetPreviewRow(
            rowNumber = rowNumber,
            categoryCode = catCode,
            title = title,
            difficulty = diffRaw,
            tokensCount = unvoweledTokens.size,
            errors = errors,
            parsedDraft = draft
        )
    }

    private fun parseCsv(stream: java.io.InputStream): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
        val text = reader.readText()
        var currentField = StringBuilder()
        var currentRow = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < text.length && text[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    currentRow.add(currentField.toString())
                    currentField = StringBuilder()
                }
                (ch == '\n' || ch == '\r') && !inQuotes -> {
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    currentRow.add(currentField.toString())
                    rows.add(currentRow)
                    currentRow = mutableListOf()
                    currentField = StringBuilder()
                }
                else -> currentField.append(ch)
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow)
        }
        return rows
    }

    private fun parseXlsx(stream: java.io.InputStream): List<List<String>> {
        val sharedStrings = mutableListOf<String>()
        var sheetXmlBytes: ByteArray? = null
        ZipInputStream(stream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "xl/sharedStrings.xml" -> {
                        val bytes = zis.readBytes()
                        sharedStrings.addAll(parseSharedStringsXml(bytes))
                    }
                    "xl/worksheets/sheet1.xml" -> {
                        sheetXmlBytes = zis.readBytes()
                    }
                }
                entry = zis.nextEntry
            }
        }
        val bytes = sheetXmlBytes ?: return emptyList()
        return parseWorksheetXml(bytes, sharedStrings)
    }

    private fun parseSharedStringsXml(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        var inSi = false
        var inT = false
        val sb = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "si" -> { inSi = true; sb.clear() }
                    "t" -> if (inSi) inT = true
                }
                XmlPullParser.TEXT -> if (inSi && inT) sb.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inT = false
                    "si" -> { result.add(sb.toString()); inSi = false }
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseWorksheetXml(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        var currentRow = mutableListOf<String>()
        var cellType = ""
        var cellRef = ""
        var inValue = false
        var inInlineT = false
        val valBuilder = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> currentRow = mutableListOf()
                    "c" -> {
                        cellType = parser.getAttributeValue(null, "t").orEmpty()
                        cellRef = parser.getAttributeValue(null, "r").orEmpty()
                        valBuilder.clear()
                    }
                    "v" -> inValue = true
                    "t" -> inInlineT = true
                }
                XmlPullParser.TEXT -> if (inValue || inInlineT) valBuilder.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v" -> inValue = false
                    "t" -> inInlineT = false
                    "c" -> {
                        val colIdx = columnIndexFromRef(cellRef)
                        while (currentRow.size < colIdx) currentRow.add("")
                        val raw = valBuilder.toString()
                        val resolved = if (cellType == "s") {
                            raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) }.orEmpty()
                        } else raw
                        currentRow.add(resolved)
                    }
                    "row" -> rows.add(currentRow)
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun columnIndexFromRef(ref: String): Int {
        val letters = ref.takeWhile { it.isLetter() }.uppercase()
        var idx = 0
        for (ch in letters) {
            idx = idx * 26 + (ch - 'A' + 1)
        }
        return (idx - 1).coerceAtLeast(0)
    }
}
