package com.example.nutricare.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.nutricare.data.Status

fun statusLabel(status: String) = when (status) {
    Status.PENDING -> "در انتظار بررسی"
    Status.ACCEPTED -> "پذیرفته شد"
    Status.REJECTED -> "رد شد"
    Status.COMPLETED -> "مشاوره تکمیل شد"
    else -> status
}

@Composable
fun StatusText(status: String) {
    val color: Color = when (status) {
        Status.REJECTED -> MaterialTheme.colorScheme.error
        Status.ACCEPTED -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.primary
    }
    Text(statusLabel(status), color = color, style = MaterialTheme.typography.labelLarge)
}

@Composable
fun Empty(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onSignOut: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) TextButton(onClick = onBack) { Text("بازگشت") }
                },
                actions = {
                    if (onSignOut != null) TextButton(onClick = onSignOut) { Text("خروج") }
                },
            )
        },
        bottomBar = bottomBar,
        content = content,
    )
}

@Composable
fun TabsBar(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar {
        tabs.forEachIndexed { i, label ->
            NavigationBarItem(
                selected = i == selected,
                onClick = { onSelect(i) },
                icon = {},
                label = { Text(label) },
            )
        }
    }
}
