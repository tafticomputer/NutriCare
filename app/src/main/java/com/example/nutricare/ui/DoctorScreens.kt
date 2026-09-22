package com.example.nutricare.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nutricare.data.AppUser
import com.example.nutricare.data.ConsultRequest
import com.example.nutricare.data.Repository
import com.example.nutricare.data.Status
import kotlinx.coroutines.launch

@Composable
fun DoctorHome(repo: Repository, user: AppUser, onSignOut: () -> Unit) {
    var chat by remember { mutableStateOf<ConsultRequest?>(null) }
    val open = chat

    if (open != null) {
        BackHandler { chat = null }
        ChatScreen(repo, open) { chat = null }
    } else {
        AppScaffold(title = "کارتابل ${user.name}", onSignOut = onSignOut) { pad ->
            Box(Modifier.padding(pad)) { InboxTab(repo) { chat = it } }
        }
    }
}

@Composable
private fun InboxTab(repo: Repository, onOpenChat: (ConsultRequest) -> Unit) {
    val requests by remember { repo.doctorInbox() }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var rejecting by remember { mutableStateOf<ConsultRequest?>(null) }

    if (requests.isEmpty()) {
        Empty("درخواستی در کارتابل شما وجود ندارد")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(requests, key = { it.id }) { r ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.patientName, style = MaterialTheme.typography.titleMedium)
                            StatusText(r.status)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(r.description)
                        Spacer(Modifier.height(10.dp))

                        when (r.status) {
                            Status.PENDING -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    scope.launch { runCatching { repo.updateStatus(r.id, Status.ACCEPTED) } }
                                }) { Text("پذیرش") }
                                OutlinedButton(onClick = { rejecting = r }) { Text("رد") }
                            }
                            Status.ACCEPTED -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onOpenChat(r) }) { Text("گفتگو") }
                                OutlinedButton(onClick = {
                                    scope.launch { runCatching { repo.updateStatus(r.id, Status.COMPLETED) } }
                                }) { Text("اتمام مشاوره") }
                            }
                            Status.COMPLETED -> OutlinedButton(onClick = { onOpenChat(r) }) { Text("مشاهده گفتگو") }
                        }
                    }
                }
            }
        }
    }

    rejecting?.let { req ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rejecting = null },
            title = { Text("رد درخواست ${req.patientName}") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("دلیل (اختیاری)") },
                    minLines = 2,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val r = reason.trim()
                    rejecting = null
                    scope.launch { runCatching { repo.updateStatus(req.id, Status.REJECTED, r) } }
                }) { Text("رد درخواست") }
            },
            dismissButton = { TextButton(onClick = { rejecting = null }) { Text("انصراف") } },
        )
    }
}
