package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.Category
import com.example.ui.theme.CorrectGreen
import com.example.ui.viewmodel.UiState

@Composable
fun AdminCategoriesTab(
    categoriesState: UiState<List<Category>>,
    canManageCategories: Boolean,
    onSaveCategory: (
        categoryId: String?,
        code: String,
        name: String,
        description: String,
        sortOrder: Long,
        active: Boolean
    ) -> Unit
) {
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kategori Nahwu", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "Kelola nama, kode unik, urutan, dan status aktif kategori.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (canManageCategories) {
                        Button(
                            onClick = {
                                editingCategory = null
                                showDialog = true
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("add_category_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Tambah")
                        }
                    }
                }
            }

            when (categoriesState) {
                is UiState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> item {
                    Text(categoriesState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    items(categoriesState.data, key = { it.id }) { cat ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = cat.code,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cat.name, style = MaterialTheme.typography.titleMedium)
                                    if (cat.description.isNotBlank()) {
                                        Text(
                                            text = cat.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "Urutan #${cat.sortOrder} • Soal dibuat: ${cat.questionCounter} • ${if (cat.active) "Aktif" else "Nonaktif"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (cat.active) CorrectGreen else MaterialTheme.colorScheme.error
                                    )
                                }
                                if (canManageCategories) {
                                    IconButton(
                                        onClick = {
                                            editingCategory = cat
                                            showDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit ${cat.name}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        CategoryEditDialog(
            initial = editingCategory,
            onDismiss = { showDialog = false },
            onSave = { code, name, desc, sortOrder, active ->
                showDialog = false
                onSaveCategory(editingCategory?.id, code, name, desc, sortOrder, active)
            }
        )
    }
}

@Composable
private fun CategoryEditDialog(
    initial: Category?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, Boolean) -> Unit
) {
    var code by remember { mutableStateOf(initial?.code.orEmpty()) }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var sortText by remember { mutableStateOf((initial?.sortOrder ?: 1L).toString()) }
    var active by remember { mutableStateOf(initial?.active ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Kategori" else "Edit Kategori") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(16) },
                    label = { Text("Kode Kategori (misal: MUB)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kategori") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi Singkat") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sortText,
                    onValueChange = { sortText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Urutan Tampil") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kategori Aktif untuk Pemain", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                if (!validationError.isNullOrBlank()) {
                    Text(validationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanCode = code.trim().uppercase()
                    if (cleanCode.length < 2) {
                        validationError = "Kode kategori minimal 2 huruf."
                        return@Button
                    }
                    if (name.trim().length < 2) {
                        validationError = "Nama kategori minimal 2 karakter."
                        return@Button
                    }
                    onSave(cleanCode, name.trim(), description.trim(), sortText.toLongOrNull() ?: 1L, active)
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
