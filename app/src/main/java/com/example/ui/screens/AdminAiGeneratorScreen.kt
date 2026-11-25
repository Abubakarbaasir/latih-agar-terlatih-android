package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.AiGeneratedQuestionBundle
import com.example.domain.model.Category
import com.example.domain.model.QuestionDifficulty
import com.example.ui.theme.ArabicInlineStyle
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAiGeneratorScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val categoriesState by adminViewModel.categories.collectAsState()
    val aiState by adminViewModel.aiState.collectAsState()
    val isBusy by adminViewModel.isBusy.collectAsState()

    val categories = (categoriesState as? UiState.Success)?.data.orEmpty()
    var selectedCategory by remember(categories) { mutableStateOf(categories.firstOrNull()) }
    var kaidahFocus by remember { mutableStateOf("Mubtada' dan Khabar serta Fa'il") }
    var difficulty by remember { mutableStateOf(QuestionDifficulty.BEGINNER) }
    var count by remember { mutableIntStateOf(1) }
    var topicMaterial by remember { mutableStateOf("Aktivitas santri menuntut ilmu di pesantren") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Buat Soal dengan AI") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = 780.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Pembuatan Draft Nahwu Berbasis AI", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "AI akan menyusun kalimat tepat 10 kata, analisis I'rab per kata, Tarkib, Terjemah Gandul, dan Kaidah. Semua hasil AI disimpan sebagai DRAFT dan wajib diperiksa Reviewer sebelum dipublikasikan.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Text("Pilih Kategori:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.let { "${it.code} • ${it.name}" } ?: "Pilih Kategori",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.code} • ${cat.name}") },
                                    onClick = {
                                        selectedCategory = cat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = kaidahFocus,
                    onValueChange = { kaidahFocus = it },
                    label = { Text("Kaidah Nahwu Fokus") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = topicMaterial,
                    onValueChange = { topicMaterial = it },
                    label = { Text("Materi / Tema Kalimat") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
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

            item {
                Text("Jumlah Soal Draft (1–3):", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3).forEach { n ->
                        FilterChip(
                            selected = count == n,
                            onClick = { count = n },
                            label = { Text("$n Soal") }
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val cat = selectedCategory ?: return@Button
                        adminViewModel.generateQuestionsWithAi(
                            category = cat,
                            kaidahFocus = kaidahFocus,
                            difficulty = difficulty,
                            count = count,
                            topicMaterial = topicMaterial
                        )
                    },
                    enabled = selectedCategory != null && aiState !is UiState.Loading && !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("generate_ai_questions_button")
                ) {
                    if (aiState is UiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Menyusun Soal Nahwu 10 Kata...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Hasilkan Draft Soal AI")
                    }
                }
            }

            when (val st = aiState) {
                is UiState.Error -> item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            text = st.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val bundles = st.data
                    if (bundles.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pratinjau Hasil AI (${bundles.size} Soal)", style = MaterialTheme.typography.titleMedium)
                                Button(
                                    onClick = {
                                        val cat = selectedCategory ?: return@Button
                                        adminViewModel.saveAiDrafts(cat, bundles, onDone = onBack)
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier.testTag("save_ai_drafts_button")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Simpan Semua sebagai Draft")
                                }
                            }
                        }

                        itemsIndexed(bundles) { idx, bundle ->
                            AiPreviewBundleCard(index = idx, bundle = bundle)
                        }
                    }
                }
                UiState.Loading -> Unit
            }
        }
    }
}

@Composable
private fun AiPreviewBundleCard(index: Int, bundle: AiGeneratedQuestionBundle) {
    val pattern = bundle.versionContent.answerPatterns.firstOrNull()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("#${index + 1} • ${bundle.title} (${bundle.versionContent.baseTokens.size} kata)", style = MaterialTheme.typography.titleSmall)
            if (pattern != null) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(pattern.voweledSentence, style = ArabicInlineStyle)
                }
                Text("Arti: ${pattern.translationNatural}", style = MaterialTheme.typography.bodySmall)
                Text("Gandul: ${pattern.translationGandul}", style = MaterialTheme.typography.bodySmall)
                Text("Tarkib: ${pattern.tarkibAnalysis.sentenceType} — ${pattern.tarkibAnalysis.mainElements}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
