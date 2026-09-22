package com.example.nutricare.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.nutricare.data.AppUser
import com.example.nutricare.data.ConsultRequest
import com.example.nutricare.data.Doctor
import com.example.nutricare.data.Repository
import com.example.nutricare.data.Status
import kotlinx.coroutines.launch

@Composable
fun PatientHome(repo: Repository, user: AppUser, onSignOut: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var chat by remember { mutableStateOf<ConsultRequest?>(null) }
    val open = chat

    if (open != null) {
        BackHandler { chat = null }
        ChatScreen(repo, open) { chat = null }
    } else {
        AppScaffold(
            title = "سلام ${user.name}",
            onSignOut = onSignOut,
            bottomBar = { TabsBar(listOf("انتخاب پزشک", "درخواست‌های من"), tab) { tab = it } },
        ) { pad ->
            Box(Modifier.padding(pad)) {
                if (tab == 0) DoctorsTab(repo, user) else MyRequestsTab(repo) { chat = it }
            }
        }
    }
}

@Composable
private fun DoctorsTab(repo: Repository, user: AppUser) {
    val doctors by remember { repo.approvedDoctors() }.collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf<Doctor?>(null) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    if (doctors.isEmpty()) {
        Empty("هنوز پزشکی ثبت نشده است")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(doctors, key = { it.id }) { d ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(d.name, style = MaterialTheme.typography.titleMedium)
                        Text(d.specialty, style = MaterialTheme.typography.bodyMedium)
                        if (d.bio.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(d.bio, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { selected = d }) { Text("ارسال درخواست") }
                    }
                }
            }
        }
    }

    selected?.let { doctor ->
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("درخواست برای ${doctor.name}") },
            text = {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("مشکل یا هدف خود را بنویسید") },
                    minLines = 3,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = description.isNotBlank(),
                    onClick = {
                        val d = description.trim()
                        selected = null
                        scope.launch {
                            try {
                                repo.sendRequest(user, doctor, d)
                                Toast.makeText(ctx, "درخواست ارسال شد", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "خطا در ارسال درخواست", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                ) { Text("ارسال") }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun MyRequestsTab(repo: Repository, onOpenChat: (ConsultRequest) -> Unit) {
    val requests by remember { repo.myRequests() }.collectAsState(initial = emptyList())

    if (requests.isEmpty()) {
        Empty("هنوز درخواستی ثبت نکرده‌اید")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(requests, key = { it.id }) { r ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(r.doctorName, style = MaterialTheme.typography.titleMedium)
                        StatusText(r.status)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(r.description)
                    if (r.status == Status.REJECTED && r.rejectReason.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("دلیل رد: ${r.rejectReason}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (r.status == Status.ACCEPTED || r.status == Status.COMPLETED) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { onOpenChat(r) }) { Text("گفتگو با پزشک") }
                    }
                }
            }
        }
    }
}
