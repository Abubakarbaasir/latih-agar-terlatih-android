package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminQuestionsTab(
    questionsState: UiState<List<Question>>,
    categoriesState: UiState<List<Category>>,
    adminRecord: AdminRecord,
    versions: List<QuestionVersionRecord>,
    reviews: List<QuestionReviewRecord>,
    onCreateManual: () -> Unit,
    onEditManual: (Question) -> Unit,
    onOpenAiGenerator: () -> Unit,
    onOpenExcelImporter: () -> Unit,
    onLoadAuditHistory: (String) -> Unit,
    onSubmitReview: (Question) -> Unit,
    onDecideReview: (Question, QuestionStatus, String) -> Unit,
    onPublish: (Question) -> Unit,
    onDisable: (Question) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<QuestionStatus?>(null) }
    var categoryFilterId by remember { mutableStateOf<String?>(null) }
    var reviewingQuestion by remember { mutableStateOf<Question?>(null) }
    var auditQuestion by remember { mutableStateOf<Question?>(null) }

    val canCreate = adminRecord.hasPermission(AdminPermission.CREATE_QUESTION)
    val canEdit = adminRecord.hasPermission(AdminPermission.EDIT_QUESTION)
    val canSubmit = adminRecord.hasPermission(AdminPermission.SUBMIT_REVIEW)
    val canReview = adminRecord.hasPermission(AdminPermission.REVIEW_QUESTION) ||
        adminRecord.hasPermission(AdminPermission.APPROVE_QUESTION) ||
        adminRecord.hasPermission(AdminPermission.REJECT_QUESTION)
    val canPublish = adminRecord.hasPermission(AdminPermission.PUBLISH_QUESTION)
    val canDisable = adminRecord.hasPermission(AdminPermission.DISABLE_QUESTION)

    val categories = (categoriesState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Kelola Soal Nahwu", style = MaterialTheme.typography.headlineSmall)
        }

        if (canCreate) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCreateManual,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("create_manual_question_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Buat Manual")
                    }
                    FilledTonalButton(
                        onClick = onOpenAiGenerator,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("open_ai_generator_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Buat dengan AI")
                    }
                    OutlinedButton(
                        onClick = onOpenExcelImporter,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("open_excel_importer_button")
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Impor Excel")
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Cari kode, judul, atau kalimat Arab") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("Semua Status") }
                )
                QuestionStatus.entries.forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = if (statusFilter == st) null else st },
                        label = { Text(st.labelId) }
                    )
                }
            }
        }

        if (categories.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = categoryFilterId == null,
                        onClick = { categoryFilterId = null },
                        label = { Text("Semua Kategori") }
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = categoryFilterId == cat.id,
                            onClick = { categoryFilterId = if (categoryFilterId == cat.id) null else cat.id },
                            label = { Text("${cat.code} • ${cat.name}") }
                        )
                    }
                }
            }
        }

        when (questionsState) {
            is UiState.Loading -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> item {
                Text(questionsState.message, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> {
                val filtered = questionsState.data.filter { q ->
                    val matchesStatus = statusFilter == null || q.statusEnum() == statusFilter
                    val matchesCat = categoryFilterId == null || q.categoryId == categoryFilterId
                    val query = searchQuery.trim()
                    val matchesSearch = query.isBlank() ||
                        q.code.contains(query, ignoreCase = true) ||
                        q.title.contains(query, ignoreCase = true) ||
                        q.latestVersionData.sentenceUnvoweled.contains(query)
                    matchesStatus && matchesCat && matchesSearch
                }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            text = "Tidak ada soal yang cocok dengan filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { q ->
                        QuestionAdminCard(
                            question = q,
                            canEdit = canEdit,
                            canSubmit = canSubmit,
                            canReview = canReview,
                            canPublish = canPublish,
                            canDisable = canDisable,
                            onEdit = { onEditManual(q) },
                            onSubmitReview = { onSubmitReview(q) },
                            onOpenReview = { reviewingQuestion = q },
                            onPublish = { onPublish(q) },
                            onDisable = { onDisable(q) },
                            onOpenHistory = {
                                onLoadAuditHistory(q.id)
                                auditQuestion = q
                            }
                        )
                    }
                }
            }
        }
    }

    if (reviewingQuestion != null) {
        val q = reviewingQuestion!!
        ReviewerDecisionDialog(
            question = q,
            canApprove = adminRecord.hasPermission(AdminPermission.APPROVE_QUESTION),
            canReject = adminRecord.hasPermission(AdminPermission.REJECT_QUESTION),
            onDismiss = { reviewingQuestion = null },
            onDecide = { status, note ->
                reviewingQuestion = null
                onDecideReview(q, status, note)
            }
        )
    }

    if (auditQuestion != null) {
        AuditHistoryDialog(
            question = auditQuestion!!,
            versions = versions,
            reviews = reviews,
            onDismiss = { auditQuestion = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionAdminCard(
    question: Question,
    canEdit: Boolean,
    canSubmit: Boolean,
    canReview: Boolean,
    canPublish: Boolean,
    canDisable: Boolean,
    onEdit: () -> Unit,
    onSubmitReview: () -> Unit,
    onOpenReview: () -> Unit,
    onPublish: () -> Unit,
    onDisable: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val status = question.statusEnum()
    val statusColor = when (status) {
        QuestionStatus.PUBLISHED -> CorrectGreen
        QuestionStatus.APPROVED -> MaterialTheme.colorScheme.primary
        QuestionStatus.REVIEW -> IncompleteAmber
        QuestionStatus.NEEDS_REPAIR, QuestionStatus.REJECTED, QuestionStatus.DISABLED -> WrongCrimson
        QuestionStatus.DRAFT -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${question.code} • v${question.currentVersionNumber}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = status.labelId,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(question.title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${question.categoryName} • ${question.difficultyEnum().labelId} • Sumber: ${question.sourceEnum().labelId} • ${question.latestVersionData.baseTokens.size} kata",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = question.latestVersionData.sentenceUnvoweled,
                    style = ArabicInlineStyle,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!question.lastReviewNote.isNullOrBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = "Catatan Reviewer (${question.reviewedBy ?: "-"}): ${question.lastReviewNote}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (canEdit) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.heightIn(min = 40.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }
                }
                if (canSubmit && (status == QuestionStatus.DRAFT || status == QuestionStatus.NEEDS_REPAIR)) {
                    FilledTonalButton(onClick = onSubmitReview, modifier = Modifier.heightIn(min = 40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Kirim Review")
                    }
                }
                if (canReview && status == QuestionStatus.REVIEW) {
                    Button(onClick = onOpenReview, modifier = Modifier.heightIn(min = 40.dp)) {
                        Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Periksa")
                    }
                }
                if (canPublish && (status == QuestionStatus.APPROVED || status == QuestionStatus.DISABLED)) {
                    Button(onClick = onPublish, modifier = Modifier.heightIn(min = 40.dp)) {
                        Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Publikasikan")
                    }
                }
                if (canDisable && status == QuestionStatus.PUBLISHED) {
                    OutlinedButton(onClick = onDisable, modifier = Modifier.heightIn(min = 40.dp)) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nonaktifkan")
                    }
                }
                TextButton(onClick = onOpenHistory, modifier = Modifier.heightIn(min = 40.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Riwayat")
                }
            }
        }
    }
}

@Composable
private fun ReviewerDecisionDialog(
    question: Question,
    canApprove: Boolean,
    canReject: Boolean,
    onDismiss: () -> Unit,
    onDecide: (QuestionStatus, String) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    var selectedDecision by remember {
        mutableStateOf(
            if (canApprove) QuestionStatus.APPROVED else QuestionStatus.NEEDS_REPAIR
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Periksa Soal ${question.code}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(question.title, style = MaterialTheme.typography.titleSmall)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = question.latestVersionData.answerPatterns.firstOrNull()?.voweledSentence
                            ?: question.latestVersionData.sentenceUnvoweled,
                        style = ArabicInlineStyle
                    )
                }
                Text(
                    text = "Terjemah: ${question.latestVersionData.translationNatural}",
                    style = MaterialTheme.typography.bodySmall
                )
                HorizontalDivider()
                Text("Keputusan Reviewer:", style = MaterialTheme.typography.labelLarge)
                if (canApprove) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedDecision == QuestionStatus.APPROVED,
                            onClick = { selectedDecision = QuestionStatus.APPROVED }
                        )
                        Text("Setujui (Siap Dipublikasikan)")
                    }
                }
                if (canReject) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedDecision == QuestionStatus.NEEDS_REPAIR,
                            onClick = { selectedDecision = QuestionStatus.NEEDS_REPAIR }
                        )
                        Text("Minta Perbaikan")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedDecision == QuestionStatus.REJECTED,
                            onClick = { selectedDecision = QuestionStatus.REJECTED }
                        )
                        Text("Tolak Soal")
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Pemeriksaan") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val safeNote = notes.ifBlank { "Pemeriksaan selesai (${selectedDecision.labelId})." }
                    onDecide(selectedDecision, safeNote)
                }
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Simpan Keputusan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun AuditHistoryDialog(
    question: Question,
    versions: List<QuestionVersionRecord>,
    reviews: List<QuestionReviewRecord>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Riwayat Versi & Review: ${question.code}") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Riwayat Versi (${versions.size}):", style = MaterialTheme.typography.titleSmall)
                if (versions.isEmpty()) {
                    Text("Memuat riwayat versi...", style = MaterialTheme.typography.bodySmall)
                } else {
                    versions.forEach { v ->
                        Text(
                            text = "• v${v.versionNumber} oleh ${v.createdByName}: ${v.changeSummary}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                HorizontalDivider()
                Text("Catatan Pemeriksaan (${reviews.size}):", style = MaterialTheme.typography.titleSmall)
                if (reviews.isEmpty()) {
                    Text("Belum ada catatan review.", style = MaterialTheme.typography.bodySmall)
                } else {
                    reviews.forEach { r ->
                        Text(
                            text = "• [${r.decision}] v${r.versionNumber} oleh ${r.reviewerName}: ${r.notes}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
