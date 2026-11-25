package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.engine.ArabicHarakat
import com.example.domain.model.*
import com.example.ui.theme.ArabicInlineStyle
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.UiState

private data class EditableTokenDraft(
    val tokenId: String,
    val baseText: String,
    val voweledArabic: String,
    val meaning: String,
    val wordType: String,
    val nahwuPosition: String,
    val statusIrab: String,
    val hukum: String,
    val sign: String,
    val reason: String,
    val amil: String,
    val endingRequired: Boolean = true,
    val middleRequired: Boolean = false
)

private data class EditablePatternDraft(
    val patternId: String,
    val label: String,
    val primary: Boolean,
    val translationNatural: String,
    val translationGandul: String,
    val tarkibType: String,
    val tarkibMainElements: String,
    val kaidahName: String,
    val kaidahDefinition: String,
    val tokens: List<EditableTokenDraft>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManualEditorScreen(
    adminViewModel: AdminViewModel,
    existingQuestion: Question?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val categoriesState by adminViewModel.categories.collectAsState()
    val isBusy by adminViewModel.isBusy.collectAsState()
    val categories = (categoriesState as? UiState.Success)?.data.orEmpty()

    val existingContent = existingQuestion?.latestVersionData
    var selectedCategory by remember(categories, existingQuestion) {
        mutableStateOf(
            categories.firstOrNull { it.id == existingQuestion?.categoryId } ?: categories.firstOrNull()
        )
    }
    var title by remember(existingQuestion) { mutableStateOf(existingQuestion?.title.orEmpty()) }
    var instruction by remember(existingQuestion) {
        mutableStateOf(
            existingQuestion?.instruction?.ifBlank { null }
                ?: "Berilah harakat akhir yang tepat pada setiap kata sesuai kedudukan Nahwunya."
        )
    }
    var difficulty by remember(existingQuestion) {
        mutableStateOf(existingQuestion?.difficultyEnum() ?: QuestionDifficulty.BEGINNER)
    }
    var changeSummary by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val initialPatterns = remember(existingQuestion) {
        val srcPatterns = existingContent?.answerPatterns.orEmpty()
        if (srcPatterns.isEmpty()) {
            listOf(createBlankPattern("pat_1", "Pola Utama", true))
        } else {
            srcPatterns.map { pat ->
                val paddedTokens = (0 until 10).map { idx ->
                    val existingT = pat.tokenAnalyses.getOrNull(idx)
                    val baseT = existingContent?.baseTokens?.getOrNull(idx)
                    val tokenId = existingT?.tokenId?.ifBlank { null }
                        ?: baseT?.tokenId?.ifBlank { null }
                        ?: "tok_${idx + 1}"
                    val hasMiddleReq = existingT?.letterRules?.any { it.role == "middle" && it.required } == true
                    val hasEndingReq = existingT?.letterRules?.lastOrNull()?.required ?: true
                    EditableTokenDraft(
                        tokenId = tokenId,
                        baseText = existingT?.baseText ?: baseT?.baseText.orEmpty(),
                        voweledArabic = existingT?.voweledArabic.orEmpty(),
                        meaning = existingT?.indonesianMeaning.orEmpty(),
                        wordType = existingT?.wordType?.ifBlank { "Isim" } ?: "Isim",
                        nahwuPosition = existingT?.nahwuPosition.orEmpty(),
                        statusIrab = existingT?.irabOrBinaStatus?.ifBlank { "Mu'rab" } ?: "Mu'rab",
                        hukum = existingT?.irabHukum?.ifBlank { "Marfu'" } ?: "Marfu'",
                        sign = existingT?.irabSign?.ifBlank { "Dhammah" } ?: "Dhammah",
                        reason = existingT?.nahwuReason.orEmpty(),
                        amil = existingT?.amil.orEmpty(),
                        endingRequired = hasEndingReq,
                        middleRequired = hasMiddleReq
                    )
                }
                EditablePatternDraft(
                    patternId = pat.patternId,
                    label = pat.label,
                    primary = pat.primary,
                    translationNatural = pat.translationNatural,
                    translationGandul = pat.translationGandul,
                    tarkibType = pat.tarkibAnalysis.sentenceType,
                    tarkibMainElements = pat.tarkibAnalysis.mainElements,
                    kaidahName = pat.kaidahList.firstOrNull()?.name.orEmpty(),
                    kaidahDefinition = pat.kaidahList.firstOrNull()?.definition.orEmpty(),
                    tokens = paddedTokens
                )
            }
        }
    }

    var patterns by remember(initialPatterns) { mutableStateOf(initialPatterns) }
    var activePatternIndex by remember { mutableIntStateOf(0) }
    val activePattern = patterns.getOrNull(activePatternIndex) ?: patterns.first()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existingQuestion == null) "Buat Soal Manual (10 Kata)"
                        else "Edit ${existingQuestion.code}"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val cat = selectedCategory
                            if (cat == null) {
                                validationError = "Kategori wajib dipilih."
                                return@Button
                            }
                            if (title.trim().length < 2) {
                                validationError = "Judul soal minimal 2 karakter."
                                return@Button
                            }
                            val firstPat = patterns.first()
                            val missingTokens = firstPat.tokens.count { it.voweledArabic.isBlank() }
                            if (missingTokens > 0) {
                                validationError = "Semua 10 token pada pola utama harus diisi kata Arab berharakat."
                                return@Button
                            }
                            if (firstPat.translationNatural.isBlank()) {
                                validationError = "Terjemahan alami pada pola utama wajib diisi."
                                return@Button
                            }
                            validationError = null

                            val baseTokens = firstPat.tokens.mapIndexed { idx, t ->
                                val stripped = ArabicHarakat.stripHarakat(t.baseText.ifBlank { t.voweledArabic })
                                QuestionToken(
                                    tokenId = t.tokenId,
                                    position = idx + 1,
                                    baseText = stripped
                                )
                            }

                            val builtPatterns = patterns.mapIndexed { pIdx, p ->
                                val tokenAnalyses = p.tokens.mapIndexed { idx, t ->
                                    val base = baseTokens[idx]
                                    val vw = t.voweledArabic.ifBlank { base.baseText }
                                    val rules = ArabicHarakat.buildDefaultLetterRules(vw, t.reason).map { r ->
                                        when (r.role) {
                                            "ending" -> r.copy(required = t.endingRequired)
                                            "middle" -> r.copy(required = t.middleRequired)
                                            else -> r
                                        }
                                    }
                                    AnswerPatternToken(
                                        tokenId = base.tokenId,
                                        position = idx + 1,
                                        baseText = base.baseText,
                                        voweledArabic = vw,
                                        indonesianMeaning = t.meaning,
                                        wordType = t.wordType,
                                        nahwuPosition = t.nahwuPosition,
                                        irabOrBinaStatus = t.statusIrab,
                                        irabHukum = t.hukum,
                                        irabSign = t.sign,
                                        signType = if (t.statusIrab.equals("Mabni", true)) "Bina" else "Tanda I'rab",
                                        manifestation = "Lafzhi",
                                        nahwuReason = t.reason,
                                        amil = t.amil,
                                        whyNotOtherSign = "Sesuai kedudukan ${t.nahwuPosition} dengan hukum ${t.hukum}.",
                                        relationToOtherWords = "Bagian dari ${p.tarkibType}",
                                        letterRules = rules
                                    )
                                }
                                AnswerPattern(
                                    patternId = p.patternId,
                                    primary = pIdx == 0 || p.primary,
                                    label = p.label.ifBlank { "Pola ${pIdx + 1}" },
                                    voweledSentence = tokenAnalyses.joinToString(" ") { it.voweledArabic },
                                    translationNatural = p.translationNatural,
                                    translationGandul = p.translationGandul.ifBlank { p.translationNatural },
                                    sentenceMeaningExplanation = p.translationNatural,
                                    structureDifferenceNote = "Pola susunan ${p.label}.",
                                    tarkibAnalysis = TarkibAnalysis(
                                        sentenceType = p.tarkibType.ifBlank { "Jumlah Fi'liyah" },
                                        mainElements = p.tarkibMainElements,
                                        structureHierarchy = tokenAnalyses.map { "${it.position}. ${it.voweledArabic} (${it.nahwuPosition})" },
                                        elementRelationships = p.tarkibMainElements,
                                        meaningConstruction = p.translationNatural
                                    ),
                                    gandulEntries = tokenAnalyses.map {
                                        GandulEntry(it.position, it.voweledArabic, it.nahwuPosition, it.indonesianMeaning, it.nahwuReason)
                                    },
                                    kaidahList = listOf(
                                        KaidahRule(
                                            name = p.kaidahName.ifBlank { cat.name },
                                            definition = p.kaidahDefinition.ifBlank { "Kaidah pada kategori ${cat.name}." },
                                            formula = p.tarkibMainElements,
                                            conditions = "Sesuai struktur kalimat",
                                            irabEffect = "Menentukan tanda i'rab kata dalam kalimat.",
                                            applicationInSentence = tokenAnalyses.joinToString(" ") { it.voweledArabic }
                                        )
                                    ),
                                    learningSummary = LearningSummary(
                                        mainRulesLearned = listOf(p.kaidahName.ifBlank { cat.name }),
                                        structureConclusion = p.tarkibMainElements,
                                        keyHarakatReasons = tokenAnalyses.take(4).map { "${it.voweledArabic}: ${it.nahwuReason}" }
                                    ),
                                    tokenAnalyses = tokenAnalyses
                                )
                            }

                            val content = QuestionVersionContent(
                                versionNumber = (existingQuestion?.currentVersionNumber?.toInt() ?: 0) + 1,
                                sentenceUnvoweled = baseTokens.joinToString(" ") { it.baseText },
                                translationNatural = firstPat.translationNatural,
                                translationGandul = firstPat.translationGandul.ifBlank { firstPat.translationNatural },
                                sentenceMeaningExplanation = firstPat.translationNatural,
                                baseTokens = baseTokens,
                                answerPatterns = builtPatterns
                            )

                            adminViewModel.saveManualQuestion(
                                existing = existingQuestion,
                                category = cat,
                                title = title,
                                instruction = instruction,
                                difficulty = difficulty,
                                versionContent = content,
                                changeSummary = changeSummary,
                                onSaved = onBack
                            )
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_manual_question_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Simpan Draft")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!validationError.isNullOrBlank()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Metadata Soal", style = MaterialTheme.typography.titleMedium)

                        var catExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = catExpanded,
                            onExpandedChange = { catExpanded = !catExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedCategory?.let { "${it.code} • ${it.name}" } ?: "Pilih Kategori",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kategori") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = catExpanded,
                                onDismissRequest = { catExpanded = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text("${cat.code} • ${cat.name}") },
                                        onClick = {
                                            selectedCategory = cat
                                            catExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Judul Soal") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = instruction,
                            onValueChange = { instruction = it },
                            label = { Text("Instruksi Pemain") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (existingQuestion != null) {
                            OutlinedTextField(
                                value = changeSummary,
                                onValueChange = { changeSummary = it },
                                label = { Text("Ringkasan Perubahan Versi Baru") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Text("Tingkat Kesulitan:", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuestionDifficulty.entries.forEach { diff ->
                                FilterChip(
                                    selected = difficulty == diff,
                                    onClick = { difficulty = diff },
                                    label = { Text(diff.labelId) }
                                )
                            }
                        }
                    }
                }
            }

            // Multi-Pattern Selector
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pola Jawaban Sah (${patterns.size})", style = MaterialTheme.typography.titleSmall)
                            TextButton(
                                onClick = {
                                    val newPat = activePattern.copy(
                                        patternId = "pat_${patterns.size + 1}",
                                        label = "Pola Alternatif ${patterns.size}",
                                        primary = false
                                    )
                                    patterns = patterns + newPat
                                    activePatternIndex = patterns.lastIndex
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Tambah Pola Alternatif")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            patterns.forEachIndexed { idx, pat ->
                                FilterChip(
                                    selected = idx == activePatternIndex,
                                    onClick = { activePatternIndex = idx },
                                    label = { Text(if (idx == 0) "Utama" else "Alt $idx") }
                                )
                            }
                            if (patterns.size > 1 && activePatternIndex > 0) {
                                IconButton(
                                    onClick = {
                                        patterns = patterns.filterIndexed { i, _ -> i != activePatternIndex }
                                        activePatternIndex = 0
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus pola alternatif")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = activePattern.label,
                            onValueChange = { v -> updateActivePattern(patterns, activePatternIndex) { it.copy(label = v) }.also { patterns = it } },
                            label = { Text("Label Pola Jawaban") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = activePattern.translationNatural,
                            onValueChange = { v -> updateActivePattern(patterns, activePatternIndex) { it.copy(translationNatural = v) }.also { patterns = it } },
                            label = { Text("Terjemahan Bahasa Indonesia Alami") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = activePattern.translationGandul,
                            onValueChange = { v -> updateActivePattern(patterns, activePatternIndex) { it.copy(translationGandul = v) }.also { patterns = it } },
                            label = { Text("Terjemahan Gandul (Pesantren)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = activePattern.tarkibType,
                            onValueChange = { v -> updateActivePattern(patterns, activePatternIndex) { it.copy(tarkibType = v) }.also { patterns = it } },
                            label = { Text("Jenis Kalimat / Tarkib") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = activePattern.tarkibMainElements,
                            onValueChange = { v -> updateActivePattern(patterns, activePatternIndex) { it.copy(tarkibMainElements = v) }.also { patterns = it } },
                            label = { Text("Unsur Utama Kalimat") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = activePattern.kaidahName,
                            onValueChange = { v -> updateActivePattern(patterns, activePatternIndex) { it.copy(kaidahName = v) }.also { patterns = it } },
                            label = { Text("Nama Kaidah Utama") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = activePattern.kaidahDefinition,
                            onValueChange = { v -> updateActivePattern(patterns, activePatternIndex) { it.copy(kaidahDefinition = v) }.also { patterns = it } },
                            label = { Text("Penjelasan Kaidah") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Text("10 Token Kata & Pembahasan I'rab", style = MaterialTheme.typography.titleMedium)
            }

            itemsIndexed(activePattern.tokens) { tIdx, token ->
                TokenEditorCard(
                    index = tIdx,
                    token = token,
                    onUpdate = { updatedToken ->
                        patterns = updateActivePattern(patterns, activePatternIndex) { pat ->
                            val nextTokens = pat.tokens.toMutableList()
                            nextTokens[tIdx] = updatedToken
                            pat.copy(tokens = nextTokens)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TokenEditorCard(
    index: Int,
    token: EditableTokenDraft,
    onUpdate: (EditableTokenDraft) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Kata #${index + 1} (${token.tokenId})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    OutlinedTextField(
                        value = token.voweledArabic,
                        onValueChange = { v ->
                            onUpdate(
                                token.copy(
                                    voweledArabic = v,
                                    baseText = ArabicHarakat.stripHarakat(v)
                                )
                            )
                        },
                        label = { Text("Arab Berharakat") },
                        textStyle = ArabicInlineStyle,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = token.meaning,
                    onValueChange = { onUpdate(token.copy(meaning = it)) },
                    label = { Text("Arti Kata") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = token.nahwuPosition,
                    onValueChange = { onUpdate(token.copy(nahwuPosition = it)) },
                    label = { Text("Kedudukan Nahwu") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = token.hukum,
                    onValueChange = { onUpdate(token.copy(hukum = it)) },
                    label = { Text("Hukum (Marfu'/dll)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = token.sign,
                    onValueChange = { onUpdate(token.copy(sign = it)) },
                    label = { Text("Tanda I'rab") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = token.amil,
                    onValueChange = { onUpdate(token.copy(amil = it)) },
                    label = { Text("Amil") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = token.reason,
                onValueChange = { onUpdate(token.copy(reason = it)) },
                label = { Text("Alasan Nahwu") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = token.endingRequired,
                        onCheckedChange = { onUpdate(token.copy(endingRequired = it)) }
                    )
                    Text("Akhir Wajib", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = token.middleRequired,
                        onCheckedChange = { onUpdate(token.copy(middleRequired = it)) }
                    )
                    Text("Tengah Wajib", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun updateActivePattern(
    list: List<EditablePatternDraft>,
    index: Int,
    transform: (EditablePatternDraft) -> EditablePatternDraft
): List<EditablePatternDraft> {
    val copy = list.toMutableList()
    if (index in copy.indices) copy[index] = transform(copy[index])
    return copy
}

private fun createBlankPattern(patternId: String, label: String, primary: Boolean): EditablePatternDraft {
    val tokens = (1..10).map { pos ->
        EditableTokenDraft(
            tokenId = "man_${System.currentTimeMillis()}_t$pos",
            baseText = "",
            voweledArabic = "",
            meaning = "",
            wordType = "Isim",
            nahwuPosition = "",
            statusIrab = "Mu'rab",
            hukum = "Marfu'",
            sign = "Dhammah",
            reason = "",
            amil = ""
        )
    }
    return EditablePatternDraft(
        patternId = patternId,
        label = label,
        primary = primary,
        translationNatural = "",
        translationGandul = "",
        tarkibType = "Jumlah Fi'liyah",
        tarkibMainElements = "",
        kaidahName = "",
        kaidahDefinition = "",
        tokens = tokens
    )
}
