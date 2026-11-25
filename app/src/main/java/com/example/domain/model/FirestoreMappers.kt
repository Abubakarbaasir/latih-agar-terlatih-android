package com.example.domain.model

fun QuestionToken.toMap(): Map<String, Any> = mapOf(
    "tokenId" to tokenId,
    "position" to position,
    "baseText" to baseText,
    "sharafNotes" to sharafNotes,
    "allowedExtraLetters" to allowedExtraLetters
)

fun TokenLetterRule.toMap(): Map<String, Any> = mapOf(
    "letterIndex" to letterIndex,
    "baseLetter" to baseLetter,
    "role" to role,
    "required" to required,
    "acceptedMarks" to acceptedMarks,
    "explanation" to explanation
)

fun AnswerPatternToken.toMap(): Map<String, Any> = mapOf(
    "tokenId" to tokenId,
    "position" to position,
    "baseText" to baseText,
    "acceptedForms" to acceptedForms,
    "voweledArabic" to voweledArabic,
    "indonesianMeaning" to indonesianMeaning,
    "wordType" to wordType,
    "nahwuPosition" to nahwuPosition,
    "irabOrBinaStatus" to irabOrBinaStatus,
    "irabHukum" to irabHukum,
    "irabSign" to irabSign,
    "signType" to signType,
    "manifestation" to manifestation,
    "nahwuReason" to nahwuReason,
    "amil" to amil,
    "whyNotOtherSign" to whyNotOtherSign,
    "binaType" to binaType,
    "binaReason" to binaReason,
    "mahalIrab" to mahalIrab,
    "relationToOtherWords" to relationToOtherWords,
    "letterRules" to letterRules.map { it.toMap() }
)

fun TarkibAnalysis.toMap(): Map<String, Any> = mapOf(
    "sentenceType" to sentenceType,
    "mainElements" to mainElements,
    "structureHierarchy" to structureHierarchy,
    "elementRelationships" to elementRelationships,
    "meaningConstruction" to meaningConstruction
)

fun WordRelation.toMap(): Map<String, Any> = mapOf(
    "fromTokenPosition" to fromTokenPosition,
    "toTokenPosition" to toTokenPosition,
    "connectedWords" to connectedWords,
    "relationType" to relationType,
    "irabEffect" to irabEffect,
    "reason" to reason,
    "meaningImpact" to meaningImpact
)

fun GandulEntry.toMap(): Map<String, Any> = mapOf(
    "tokenPosition" to tokenPosition,
    "arabicWord" to arabicWord,
    "nahwuFunction" to nahwuFunction,
    "gandulMeaning" to gandulMeaning,
    "relationNote" to relationNote
)

fun KaidahRule.toMap(): Map<String, Any> = mapOf(
    "name" to name,
    "definition" to definition,
    "formula" to formula,
    "conditions" to conditions,
    "irabEffect" to irabEffect,
    "applicationInSentence" to applicationInSentence,
    "extraExamples" to extraExamples
)

fun LearningSummary.toMap(): Map<String, Any> = mapOf(
    "mainRulesLearned" to mainRulesLearned,
    "structureConclusion" to structureConclusion,
    "keyHarakatReasons" to keyHarakatReasons
)

fun AnswerPattern.toMap(): Map<String, Any> = mapOf(
    "patternId" to patternId,
    "primary" to primary,
    "label" to label,
    "voweledSentence" to voweledSentence,
    "translationNatural" to translationNatural,
    "translationGandul" to translationGandul,
    "sentenceMeaningExplanation" to sentenceMeaningExplanation,
    "structureDifferenceNote" to structureDifferenceNote,
    "tarkibAnalysis" to tarkibAnalysis.toMap(),
    "wordRelations" to wordRelations.map { it.toMap() },
    "gandulEntries" to gandulEntries.map { it.toMap() },
    "kaidahList" to kaidahList.map { it.toMap() },
    "learningSummary" to learningSummary.toMap(),
    "tokenAnalyses" to tokenAnalyses.map { it.toMap() }
)

fun QuestionVersionContent.toMap(): Map<String, Any> = mapOf(
    "versionNumber" to versionNumber,
    "sentenceUnvoweled" to sentenceUnvoweled,
    "translationNatural" to translationNatural,
    "translationGandul" to translationGandul,
    "sentenceMeaningExplanation" to sentenceMeaningExplanation,
    "baseTokens" to baseTokens.map { it.toMap() },
    "answerPatterns" to answerPatterns.map { it.toMap() }
)
