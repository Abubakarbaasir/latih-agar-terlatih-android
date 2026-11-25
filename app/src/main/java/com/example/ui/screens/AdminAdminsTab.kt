package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.*
import com.example.ui.theme.CorrectGreen

@Composable
fun AdminAdminsTab(
    adminRecord: AdminRecord,
    users: List<UserProfile>,
    difficultyConfig: DifficultyConfig,
    onSaveDifficultyConfig: (beginnerMax: Long, intermediateMax: Long) -> Unit,
    onUpdateAdminAccess: (UserProfile, UserRole, List<String>, Boolean) -> Unit
) {
    val canManageAdmins = adminRecord.hasPermission(AdminPermission.MANAGE_ADMINS)
    var beginnerMax by remember(difficultyConfig) { mutableStateOf(difficultyConfig.beginnerMaxSeconds.toString()) }
    var intermediateMax by remember(difficultyConfig) { mutableStateOf(difficultyConfig.intermediateMaxSeconds.toString()) }
    var editingUser by remember { mutableStateOf<UserProfile?>(null) }

    if (!canManageAdmins) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Pengaturan admin dan ambang kesulitan hanya dapat diakses oleh Super Admin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Pengaturan Ambang Waktu Kesulitan (Bagian 9)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Menentukan kapan tingkat kesulitan otomatis naik selama satu sesi permainan berlangsung.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = beginnerMax,
                            onValueChange = { beginnerMax = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Batas Awal (detik)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = intermediateMax,
                            onValueChange = { intermediateMax = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Batas Menengah (detik)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            val b = beginnerMax.toLongOrNull() ?: 120L
                            val i = intermediateMax.toLongOrNull() ?: 300L
                            onSaveDifficultyConfig(b, i)
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Simpan Ambang Waktu")
                    }
                }
            }
        }

        item {
            Text("Daftar Akun & Hak Akses Admin", style = MaterialTheme.typography.headlineSmall)
        }

        items(users, key = { it.id }) { user ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.displayName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Peran: ${user.roleEnum().labelId} • ${user.permissions.size} izin • ${if (user.active) "Aktif" else "Nonaktif"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.roleEnum() != UserRole.PLAYER) CorrectGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { editingUser = user }) {
                        Icon(Icons.Default.Edit, contentDescription = "Atur izin ${user.displayName}")
                    }
                }
            }
        }
    }

    if (editingUser != null) {
        val target = editingUser!!
        AdminRoleDialog(
            user = target,
            onDismiss = { editingUser = null },
            onSave = { role, perms, active ->
                editingUser = null
                onUpdateAdminAccess(target, role, perms, active)
            }
        )
    }
}

@Composable
private fun AdminRoleDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserRole, List<String>, Boolean) -> Unit
) {
    var selectedRole by remember { mutableStateOf(user.roleEnum()) }
    var selectedPerms by remember {
        mutableStateOf(
            if (user.permissions.isNotEmpty()) user.permissions.toSet()
            else AdminPermission.defaultsForRole(user.roleEnum()).toSet()
        )
    }
    var active by remember { mutableStateOf(user.active) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kelola Hak Akses: ${user.displayName}") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Pilih Peran:", style = MaterialTheme.typography.labelLarge)
                UserRole.entries.forEach { role ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedRole == role,
                            onClick = {
                                selectedRole = role
                                selectedPerms = AdminPermission.defaultsForRole(role).toSet()
                            }
                        )
                        Text(role.labelId, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (selectedRole != UserRole.PLAYER && selectedRole != UserRole.SUPER_ADMIN) {
                    HorizontalDivider()
                    Text("Izin Granular:", style = MaterialTheme.typography.labelLarge)
                    AdminPermission.ALL.forEach { (permKey, permLabel) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = permKey in selectedPerms,
                                onCheckedChange = { checked ->
                                    selectedPerms = if (checked) selectedPerms + permKey else selectedPerms - permKey
                                }
                            )
                            Text(permLabel, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Status Akun Aktif", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPerms = if (selectedRole == UserRole.PLAYER) emptyList() else selectedPerms.toList()
                    onSave(selectedRole, finalPerms, active)
                }
            ) {
                Text("Simpan Hak Akses")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
