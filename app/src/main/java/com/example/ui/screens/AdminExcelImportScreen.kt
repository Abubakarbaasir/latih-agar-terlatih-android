package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.SpreadsheetImporter
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.WrongCrimson
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminExcelImportScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val previewRows by adminViewModel.excelPreview.collectAsState()
    val isBusy by adminViewModel.isBusy.collectAsState()
    var localStatusMessage by remember { mutableStateOf<String?>(null) }

    val templateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(SpreadsheetImporter.generateCsvTemplate().toByteArray(Charsets.UTF_8))
                }
                localStatusMessage = "Template CSV berhasil diunduh. Buka dengan Excel atau Google Sheets."
            } catch (e: Exception) {
                localStatusMessage = "Gagal menyimpan template: ${e.localizedMessage}"
            }
        }
    }

    val importPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            localStatusMessage = null
            adminViewModel.parseSpreadsheetFile(context, uri)
        }
    }

    val validCount = previewRows.count { it.isValid }
    val errorCount = previewRows.count { !it.isValid }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Impor Soal Excel / CSV") },
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Panduan Impor Soal Massal", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "1. Unduh template CSV UTF-8 agar semua kolom wajib dan contoh kalimat 10 kata sesuai format.\n" +
                                "2. Edit melalui Excel/Google Sheets, lalu simpan sebagai .xlsx atau .csv.\n" +
                                "3. Setiap soal wajib memiliki tepat 10 kata dan daftar arti/kedudukan/hukum/tanda/alasan dipisah tanda '|'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { templateLauncher.launch("template_soal_latih_nahwu.csv") },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("download_excel_template_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Unduh Template")
                            }
                            Button(
                                onClick = {
                                    importPickerLauncher.launch(
                                        arrayOf(
                                            "text/csv",
                                            "text/comma-separated-values",
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                            "*/*"
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("pick_excel_file_button")
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Pilih .XLSX / .CSV")
                            }
                        }
                    }
                }
            }

            if (!localStatusMessage.isNullOrBlank()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Text(
                            text = localStatusMessage!!,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (previewRows.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Pratinjau Impor (${previewRows.size} Baris)", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Valid: $validCount baris • Error: $errorCount baris",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Button(
                                onClick = { adminViewModel.confirmExcelImport(onDone = onBack) },
                                enabled = validCount > 0 && !isBusy,
                                modifier = Modifier.testTag("confirm_excel_import_button")
                            ) {
                                Text("Impor $validCount Draft")
                            }
                        }
                    }
                }

                items(previewRows, key = { it.rowNumber }) { row ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (row.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (row.isValid) CorrectGreen else WrongCrimson
                                )
                                Text(
                                    text = "Baris #${row.rowNumber}: ${row.categoryCode} • ${row.title.ifBlank { "(Tanpa Judul)" }} (${row.tokensCount} kata)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            if (row.isValid) {
                                Text(
                                    text = "Siap diimpor sebagai Draft (${row.difficulty}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CorrectGreen
                                )
                            } else {
                                row.errors.forEach { err ->
                                    Text(
                                        text = "• $err",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WrongCrimson
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
