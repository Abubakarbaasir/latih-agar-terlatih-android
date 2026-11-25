package com.example.data

import android.util.Log
import com.example.domain.engine.ArabicHarakat
import com.example.domain.model.*
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiGeneratedQuestionBundle(
    val title: String,
    val instruction: String,
    val difficulty: QuestionDifficulty,
    val versionContent: QuestionVersionContent
)

/**
 * Generates draft 10-token Nahwu questions using Gemini via Firebase AI Logic.
 * All generated questions MUST remain status = "draft" until reviewed by a Reviewer.
 */
class AiQuestionGenerator {

    private val tokenSchema = Schema.obj(
        properties = mapOf(
            "position" to Schema.integer(),
            "baseWord" to Schema.string(description = "Arabic word WITHOUT harakat"),
            "voweledArabic" to Schema.string(description = "Arabic word WITH complete harakat"),
            "indonesianMeaning" to Schema.string(),
            "wordType" to Schema.string(description = "Isim, Fi'il, or Harf"),
            "nahwuPosition" to Schema.string(description = "Kedudukan Nahwu e.g. Mubtada', Fa'il, Maf'ul Bih"),
            "irabOrBinaStatus" to Schema.string(description = "Mu'rab or Mabni"),
            "irabHukum" to Schema.string(description = "Marfu', Manshub, Majrur, Majzum, or Mabni"),
            "irabSign" to Schema.string(description = "Dhammah, Fathah, Kasrah, Sukun, Alif, Wawu, Ya, etc."),
            "signType" to Schema.string(description = "Tanda Asli, Tanda Pengganti (Far'i), or Bina"),
            "manifestation" to Schema.string(description = "Lafzhi, Taqdiri, or Mahalli"),
            "nahwuReason" to Schema.string(),
            "amil" to Schema.string(),
            "whyNotOtherSign" to Schema.string(),
            "relationToOtherWords" to Schema.string(),
            "gandulMeaning" to Schema.string(description = "Pesantren gandul gloss like 'utawi...', 'iku...', 'sopo...', 'ing...'"),
            "binaType" to Schema.string(),
            "binaReason" to Schema.string(),
            "mahalIrab" to Schema.string()
        )
    )

    private val relationSchema = Schema.obj(
        properties = mapOf(
            "fromPos" to Schema.integer(),
            "toPos" to Schema.integer(),
            "connectedWords" to Schema.string(),
            "relationType" to Schema.string(),
            "irabEffect" to Schema.string(),
            "reason" to Schema.string(),
            "meaningImpact" to Schema.string()
        )
    )

    private val kaidahSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(),
            "definition" to Schema.string(),
            "formula" to Schema.string(),
            "conditions" to Schema.string(),
            "irabEffect" to Schema.string(),
            "applicationInSentence" to Schema.string(),
            "extraExamples" to Schema.array(items = Schema.string())
        )
    )

    private val questionItemSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(),
            "instruction" to Schema.string(),
            "sentenceUnvoweled" to Schema.string(description = "EXACTLY 10 space-separated Arabic words without harakat"),
            "voweledSentence" to Schema.string(description = "EXACTLY 10 space-separated Arabic words with complete harakat"),
            "translationNatural" to Schema.string(),
            "translationGandul" to Schema.string(),
            "sentenceMeaningExplanation" to Schema.string(),
            "structureDifferenceNote" to Schema.string(),
            "tarkibSentenceType" to Schema.string(),
            "tarkibMainElements" to Schema.string(),
            "tarkibHierarchy" to Schema.array(items = Schema.string()),
            "tarkibRelationships" to Schema.string(),
            "tarkibMeaningConstruction" to Schema.string(),
            "tokens" to Schema.array(items = tokenSchema, description = "EXACTLY 10 token objects in order 1..10"),
            "wordRelations" to Schema.array(items = relationSchema),
            "kaidahList" to Schema.array(items = kaidahSchema),
            "mainRulesLearned" to Schema.array(items = Schema.string()),
            "structureConclusion" to Schema.string(),
            "keyHarakatReasons" to Schema.array(items = Schema.string())
        )
    )

    private val rootSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(items = questionItemSchema)
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = rootSchema
            }
        )
    }

    suspend fun generateDraftQuestions(
        categoryName: String,
        kaidahFocus: String,
        difficulty: QuestionDifficulty,
        count: Int,
        topicMaterial: String
    ): Result<List<AiGeneratedQuestionBundle>> {
        val safeCount = count.coerceIn(1, 3)
        val prompt = """
            Anda adalah pakar bahasa Arab, Ilmu Nahwu, Sharaf, Tarkib, dan I'rab klasik (standar pesantren dan akademik).
            Buat $safeCount soal latihan Nahwu dalam bahasa Indonesia dengan ketentuan MUTLAK:
            1. Kategori: $categoryName
            2. Kaidah fokus: $kaidahFocus
            3. Tingkat kesulitan: ${difficulty.labelId} (${difficulty.value})
            4. Materi/Tema kalimat: ${topicMaterial.ifBlank { "Kehidupan pelajar dan ilmu pengetahuan" }}
            5. SETIAP kalimat Arab WAJIB memiliki TEPAT 10 kata dasar (dipisahkan spasi, tanpa harakat pada sentenceUnvoweled, dan berharakat lengkap pada voweledSentence). JANGAN 9 kata atau 11 kata — HARUS TEPAT 10 KATA.
            6. Array 'tokens' WAJIB berisi TEPAT 10 elemen berurutan 1 sampai 10 yang cocok 1:1 dengan 10 kata kalimat.
            7. Jelaskan I'rab/Bina setiap kata secara akurat sesuai kedudukannya (jangan memberi kedudukan sama untuk semua kata; kata mabni wajib dijelaskan jenis bina, alasan, dan mahallul i'rab).
            8. Sertakan terjemahan gandul pesantren (menggunakan penanda utawi, iku, sopo, ing, hale, kelawan, ing dalem) dan terjemahan bahasa Indonesia alami.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("AI mengembalikan respons kosong.")
            val root = JSONObject(raw)
            val arr = root.getJSONArray("questions")
            val parsed = mutableListOf<AiGeneratedQuestionBundle>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val tokenArr = obj.getJSONArray("tokens")
                if (tokenArr.length() != 10) {
                    throw IllegalStateException("Soal AI ke-${i + 1} tidak memiliki tepat 10 token (ditemukan ${tokenArr.length()}). Silakan coba ulang.")
                }

                val keyPrefix = "ai_${System.currentTimeMillis()}_$i"
                val baseTokens = mutableListOf<QuestionToken>()
                val patternTokens = mutableListOf<AnswerPatternToken>()
                val gandulEntries = mutableListOf<GandulEntry>()

                for (tIdx in 0 until 10) {
                    val tObj = tokenArr.getJSONObject(tIdx)
                    val pos = tIdx + 1
                    val baseWord = ArabicHarakat.stripHarakat(tObj.getString("baseWord").trim())
                    val voweled = tObj.getString("voweledArabic").trim()
                    val reason = tObj.getString("nahwuReason")
                    val tokenId = "${keyPrefix}_t$pos"

                    baseTokens.add(QuestionToken(tokenId = tokenId, position = pos, baseText = baseWord))
                    patternTokens.add(
                        AnswerPatternToken(
                            tokenId = tokenId,
                            position = pos,
                            baseText = baseWord,
                            voweledArabic = voweled,
                            indonesianMeaning = tObj.getString("indonesianMeaning"),
                            wordType = tObj.getString("wordType"),
                            nahwuPosition = tObj.getString("nahwuPosition"),
                            irabOrBinaStatus = tObj.getString("irabOrBinaStatus"),
                            irabHukum = tObj.getString("irabHukum"),
                            irabSign = tObj.getString("irabSign"),
                            signType = tObj.getString("signType"),
                            manifestation = tObj.getString("manifestation"),
                            nahwuReason = reason,
                            amil = tObj.getString("amil"),
                            whyNotOtherSign = tObj.getString("whyNotOtherSign"),
                            binaType = tObj.optString("binaType", ""),
                            binaReason = tObj.optString("binaReason", ""),
                            mahalIrab = tObj.optString("mahalIrab", ""),
                            relationToOtherWords = tObj.getString("relationToOtherWords"),
                            letterRules = ArabicHarakat.buildDefaultLetterRules(voweled, reason)
                        )
                    )
                    gandulEntries.add(
                        GandulEntry(
                            tokenPosition = pos,
                            arabicWord = voweled,
                            nahwuFunction = tObj.getString("nahwuPosition"),
                            gandulMeaning = tObj.optString("gandulMeaning", tObj.getString("indonesianMeaning")),
                            relationNote = tObj.getString("relationToOtherWords")
                        )
                    )
                }

                val relArr = obj.getJSONArray("wordRelations")
                val wordRelations = (0 until relArr.length()).map { rIdx ->
                    val r = relArr.getJSONObject(rIdx)
                    WordRelation(
                        fromTokenPosition = r.optInt("fromPos", 1),
                        toTokenPosition = r.optInt("toPos", 2),
                        connectedWords = r.getString("connectedWords"),
                        relationType = r.getString("relationType"),
                        irabEffect = r.getString("irabEffect"),
                        reason = r.getString("reason"),
                        meaningImpact = r.getString("meaningImpact")
                    )
                }

                val kaidahArr = obj.getJSONArray("kaidahList")
                val kaidahList = (0 until kaidahArr.length()).map { kIdx ->
                    val k = kaidahArr.getJSONObject(kIdx)
                    val exArr = k.optJSONArray("extraExamples")
                    val examples = if (exArr != null) (0 until exArr.length()).map { exArr.getString(it) } else emptyList()
                    KaidahRule(
                        name = k.getString("name"),
                        definition = k.getString("definition"),
                        formula = k.getString("formula"),
                        conditions = k.getString("conditions"),
                        irabEffect = k.getString("irabEffect"),
                        applicationInSentence = k.getString("applicationInSentence"),
                        extraExamples = examples
                    )
                }

                val hierarchyArr = obj.getJSONArray("tarkibHierarchy")
                val hierarchy = (0 until hierarchyArr.length()).map { hierarchyArr.getString(it) }
                val rulesLearnedArr = obj.getJSONArray("mainRulesLearned")
                val rulesLearned = (0 until rulesLearnedArr.length()).map { rulesLearnedArr.getString(it) }
                val keyReasonsArr = obj.getJSONArray("keyHarakatReasons")
                val keyReasons = (0 until keyReasonsArr.length()).map { keyReasonsArr.getString(it) }

                val transNat = obj.getString("translationNatural")
                val transGan = obj.getString("translationGandul")
                val meaningExp = obj.getString("sentenceMeaningExplanation")

                val pattern = AnswerPattern(
                    patternId = "pat_ai_1",
                    primary = true,
                    label = "Pola Utama (Draft AI)",
                    voweledSentence = patternTokens.joinToString(" ") { it.voweledArabic },
                    translationNatural = transNat,
                    translationGandul = transGan,
                    sentenceMeaningExplanation = meaningExp,
                    structureDifferenceNote = obj.getString("structureDifferenceNote"),
                    tarkibAnalysis = TarkibAnalysis(
                        sentenceType = obj.getString("tarkibSentenceType"),
                        mainElements = obj.getString("tarkibMainElements"),
                        structureHierarchy = hierarchy,
                        elementRelationships = obj.getString("tarkibRelationships"),
                        meaningConstruction = obj.getString("tarkibMeaningConstruction")
                    ),
                    wordRelations = wordRelations,
                    gandulEntries = gandulEntries,
                    kaidahList = kaidahList,
                    learningSummary = LearningSummary(
                        mainRulesLearned = rulesLearned,
                        structureConclusion = obj.getString("structureConclusion"),
                        keyHarakatReasons = keyReasons
                    ),
                    tokenAnalyses = patternTokens
                )

                parsed.add(
                    AiGeneratedQuestionBundle(
                        title = obj.getString("title"),
                        instruction = obj.getString("instruction"),
                        difficulty = difficulty,
                        versionContent = QuestionVersionContent(
                            versionNumber = 1,
                            sentenceUnvoweled = baseTokens.joinToString(" ") { it.baseText },
                            translationNatural = transNat,
                            translationGandul = transGan,
                            sentenceMeaningExplanation = meaningExp,
                            baseTokens = baseTokens,
                            answerPatterns = listOf(pattern)
                        )
                    )
                )
            }
            Result.success(parsed)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI generation timed out", e)
            Result.failure(Exception("Pembuatan soal AI melebihi batas waktu 60 detik. Silakan coba lagi."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI generation failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "AiQuestionGenerator"
    }
}
