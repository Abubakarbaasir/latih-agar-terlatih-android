package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AdminLogEntry
import com.example.domain.model.AdminPermission
import com.example.domain.model.AdminRecord
import com.example.domain.model.Question
import com.example.ui.viewmodel.AdminDashboardStats
import com.example.ui.viewmodel.AdminViewModel

enum class AdminMenuTab(val label: String) {
    BERANDA("Beranda"),
    KATEGORI("Kategori"),
    KELOLA_SOAL("Kelola Soal"),
    KELOLA_ADMIN("Kelola Admin"),
    KELUAR("Keluar")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    adminRecord: AdminRecord?,
    onBackToPlayer: () -> Unit,
    onOpenManualEditor: (Question?) -> Unit,
    onOpenAiGenerator: () -> Unit,
    onOpenExcelImporter: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackToPlayer()
    }

    var selectedTab by remember { mutableStateOf(AdminMenuTab.BERANDA) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    val stats by adminViewModel.stats.collectAsState()
    val categoriesState by adminViewModel.categories.collectAsState()
    val questionsState by adminViewModel.questions.collectAsState()
    val logs by adminViewModel.logs.collectAsState()
    val users by adminViewModel.users.collectAsState()
    val difficultyConfig by adminViewModel.difficultyConfig.collectAsState()
    val versions by adminViewModel.versions.collectAsState()
    val reviews by adminViewModel.reviews.collectAsState()
    val isBusy by adminViewModel.isBusy.collectAsState()
    val feedback by adminViewModel.feedback.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedback) {
        val fb = feedback ?: return@LaunchedEffect
        if (fb.message.isNotBlank()) {
            snackbarHostState.showSnackbar(fb.message)
            adminViewModel.clearFeedback()
        }
    }

    if (adminRecord == null || !adminRecord.active) {
        Scaffold(modifier = modifier.fillMaxSize()) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Akses Admin Tidak Tersedia",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Akun Anda belum memiliki hak akses administrator aktif.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onBackToPlayer) {
                        Text("Kembali ke Halaman Pemain")
                    }
                }
            }
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dashboard Admin", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${adminRecord.displayName} • ${adminRecord.roleEnum().labelId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToPlayer,
                        modifier = Modifier.testTag("admin_back_to_player_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Mode Pemain"
                        )
                    }
                },
                actions = {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val tabs = listOf(
                    Triple(AdminMenuTab.BERANDA, Icons.Default.Dashboard, "admin_tab_beranda"),
                    Triple(AdminMenuTab.KATEGORI, Icons.Default.Category, "admin_tab_kategori"),
                    Triple(AdminMenuTab.KELOLA_SOAL, Icons.Default.Description, "admin_tab_soal"),
                    Triple(AdminMenuTab.KELOLA_ADMIN, Icons.Default.ManageAccounts, "admin_tab_admin"),
                    Triple(AdminMenuTab.KELUAR, Icons.AutoMirrored.Filled.Logout, "admin_tab_keluar")
                )
                tabs.forEach { (tab, icon, tag) ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            if (tab == AdminMenuTab.KELUAR) {
                                showSignOutDialog = true
                            } else {
                                selectedTab = tab
                            }
                        },
                        icon = { Icon(icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1) },
                        modifier = Modifier.testTag(tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AdminMenuTab.BERANDA -> AdminHomeTab(
                    stats = stats,
                    adminRecord = adminRecord,
                    logs = logs,
                    onGoToCategories = { selectedTab = AdminMenuTab.KATEGORI },
                    onGoToQuestions = { selectedTab = AdminMenuTab.KELOLA_SOAL }
                )
                AdminMenuTab.KATEGORI -> AdminCategoriesTab(
                    categoriesState = categoriesState,
                    canManageCategories = adminRecord.hasPermission(AdminPermission.MANAGE_CATEGORIES),
                    onSaveCategory = { id, code, name, desc, sort, active ->
                        adminViewModel.saveCategory(id, code, name, desc, sort, active)
                    }
                )
                AdminMenuTab.KELOLA_SOAL -> AdminQuestionsTab(
                    questionsState = questionsState,
                    categoriesState = categoriesState,
                    adminRecord = adminRecord,
                    versions = versions,
                    reviews = reviews,
                    onCreateManual = { onOpenManualEditor(null) },
                    onEditManual = { q -> onOpenManualEditor(q) },
                    onOpenAiGenerator = onOpenAiGenerator,
                    onOpenExcelImporter = onOpenExcelImporter,
                    onLoadAuditHistory = { qId -> adminViewModel.loadQuestionAuditHistory(qId) },
                    onSubmitReview = { q -> adminViewModel.submitForReview(q) },
                    onDecideReview = { q, dec, note -> adminViewModel.decideReview(q, dec, note) },
                    onPublish = { q -> adminViewModel.publishQuestion(q) },
                    onDisable = { q -> adminViewModel.disableQuestion(q) }
                )
                AdminMenuTab.KELOLA_ADMIN -> AdminAdminsTab(
                    adminRecord = adminRecord,
                    users = users,
                    difficultyConfig = difficultyConfig,
                    onSaveDifficultyConfig = { b, i -> adminViewModel.updateDifficultyThresholds(b, i) },
                    onUpdateAdminAccess = { u, role, perms, active ->
                        adminViewModel.updateAdminAccess(u, role, perms, active)
                    }
                )
                AdminMenuTab.KELUAR -> Unit
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Keluar dari Akun?") },
            text = { Text("Pilih 'Kembali ke Pemain' jika hanya ingin keluar dari dashboard admin, atau 'Keluar dari Akun' untuk logout Google.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    }
                ) {
                    Text("Keluar dari Akun")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onBackToPlayer()
                    }
                ) {
                    Text("Ke Mode Pemain")
                }
            }
        )
    }
}

@Composable
private fun AdminHomeTab(
    stats: AdminDashboardStats,
    adminRecord: AdminRecord,
    logs: List<AdminLogEntry>,
    onGoToCategories: () -> Unit,
    onGoToQuestions: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 800.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Ringkasan Pengelolaan",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("Total Kategori", "${stats.totalCategories}", Modifier.weight(1f))
                StatCard("Total Soal", "${stats.totalQuestions}", Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("Draft / Perbaikan", "${stats.draftCount + stats.needsRepairCount}", Modifier.weight(1f))
                StatCard("Menunggu Review", "${stats.reviewCount}", Modifier.weight(1f))
                StatCard("Dipublikasikan", "${stats.publishedCount}", Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onGoToQuestions,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text("Kelola Soal")
                }
                OutlinedButton(
                    onClick = onGoToCategories,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text("Kelola Kategori")
                }
            }
        }

        if (adminRecord.hasPermission(AdminPermission.VIEW_LOGS)) {
            item {
                Text(
                    text = "Aktivitas Terbaru",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (logs.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada log aktivitas tercatat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(logs, key = { it.id }) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${log.action} • ${log.actorName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(log.summary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
