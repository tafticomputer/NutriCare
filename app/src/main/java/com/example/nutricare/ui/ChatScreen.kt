package com.example.nutricare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nutricare.data.ConsultRequest
import com.example.nutricare.data.Repository
import com.example.nutricare.data.Status
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(repo: Repository, request: ConsultRequest, onBack: () -> Unit) {
    val me = repo.uid
    val messages by remember { repo.messages(request.id) }.collectAsState(initial = emptyList())
    var text by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    val title = if (me == request.patientId) request.doctorName else request.patientName

    AppScaffold(title = title, onBack = onBack) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(messages, key = { it.id }) { m ->
                    val mine = m.senderId == me
                    Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = if (mine) Alignment.CenterStart else Alignment.CenterEnd,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (mine) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.widthIn(max = 280.dp),
                        ) {
                            Text(m.text, Modifier.padding(10.dp))
                        }
                    }
                }
            }

            if (request.status == Status.ACCEPTED) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("پیام خود را بنویسید...") },
                        maxLines = 4,
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = text.isNotBlank(),
                        onClick = {
                            val t = text.trim()
                            text = ""
                            scope.launch { runCatching { repo.sendMessage(request.id, t) } }
                        },
                    ) { Text("ارسال") }
                }
            } else {
                Text(
                    "این مشاوره پایان یافته است و امکان ارسال پیام وجود ندارد.",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
