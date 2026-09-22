package com.example.nutricare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nutricare.data.AppUser
import com.example.nutricare.data.Repository
import com.example.nutricare.data.Status
import kotlinx.coroutines.launch

@Composable
fun AdminHome(repo: Repository, user: AppUser, onSignOut: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    AppScaffold(
        title = "پنل مدیریت",
        onSignOut = onSignOut,
        bottomBar = { TabsBar(listOf("کاربران", "پزشکان", "درخواست‌ها"), tab) { tab = it } },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> UsersTab(repo)
                1 -> DoctorsAdminTab(repo)
                else -> RequestsAdminTab(repo)
            }
        }
    }
}

@Composable
private fun UsersTab(repo: Repository) {
    val users by remember { repo.patients() }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var promote by remember { mutableStateOf<AppUser?>(null) }

    if (users.isEmpty()) {
        Empty("کاربری (بیماری) ثبت‌نام نکرده است")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(users, key = { it.id }) { u ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(u.name, style = MaterialTheme.typography.titleMedium)
                        Text(u.email, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { promote = u }) { Text("تبدیل به پزشک") }
                            OutlinedButton(onClick = {
                                scope.launch { runCatching { repo.setUserActive(u.id, !u.active) } }
                            }) { Text(if (u.active) "مسدود کردن" else "فعال کردن") }
                        }
                    }
                }
            }
        }
    }

    promote?.let { u ->
        var specialty by remember { mutableStateOf("") }
        var license by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { promote = null },
            title = { Text("تبدیل ${u.name} به پزشک") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = specialty, onValueChange = { specialty = it },
                        label = { Text("تخصص (مثلاً متخصص تغذیه)") }, singleLine = true,
                    )
                    OutlinedTextField(
                        value = license, onValueChange = { license = it },
                        label = { Text("شماره نظام پزشکی") }, singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = specialty.isNotBlank(),
                    onClick = {
                        val s = specialty.trim()
                        val l = license.trim()
                        promote = null
                        scope.launch { runCatching { repo.promoteToDoctor(u, s, l) } }
                    },
                ) { Text("تأیید") }
            },
            dismissButton = { TextButton(onClick = { promote = null }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun DoctorsAdminTab(repo: Repository) {
    val doctors by remember { repo.allDoctors() }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    if (doctors.isEmpty()) {
        Empty("هنوز پزشکی تعریف نشده است")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(doctors, key = { it.id }) { d ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(d.name, style = MaterialTheme.typography.titleMedium)
                        Text(d.specialty, style = MaterialTheme.typography.bodyMedium)
                        if (d.licenseNumber.isNotBlank()) {
                            Text("نظام پزشکی: ${d.licenseNumber}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Switch(
                            checked = d.approved,
                            onCheckedChange = { v ->
                                scope.launch { runCatching { repo.setDoctorApproved(d.id, v) } }
                            },
                        )
                        Text(if (d.approved) "فعال" else "غیرفعال", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestsAdminTab(repo: Repository) {
    val requests by remember { repo.allRequests() }.collectAsState(initial = emptyList())

    if (requests.isEmpty()) {
        Empty("درخواستی ثبت نشده است")
        return
    }
    val pending = requests.count { it.status == Status.PENDING }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "کل درخواست‌ها: ${requests.size}   |   در انتظار: $pending",
                style = MaterialTheme.typography.titleSmall,
            )
        }
        items(requests, key = { it.id }) { r ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${r.patientName} ← ${r.doctorName}", style = MaterialTheme.typography.titleSmall)
                        StatusText(r.status)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(r.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
