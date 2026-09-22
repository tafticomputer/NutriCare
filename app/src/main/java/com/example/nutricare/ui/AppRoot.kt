package com.example.nutricare.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.nutricare.data.Repository
import com.example.nutricare.data.Role
import com.example.nutricare.data.Session
import kotlinx.coroutines.launch

@Composable
fun AppRoot() {
    val repo = remember { Repository() }
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<Session>(Session.Loading) }

    LaunchedEffect(Unit) { session = repo.loadSession() }

    // پس از ورود، توکن اعلان دستگاه ذخیره می‌شود
    LaunchedEffect(session) {
        if (session is Session.SignedIn) runCatching { repo.saveFcmToken() }
    }

    val reload: () -> Unit = {
        scope.launch {
            session = Session.Loading
            session = repo.loadSession()
        }
    }
    val signOut: () -> Unit = {
        scope.launch {
            repo.signOut()
            session = Session.SignedOut()
        }
    }

    when (val s = session) {
        Session.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        is Session.SignedOut -> AuthScreen(repo, s.message, reload)
        is Session.SignedIn -> when (s.user.role) {
            Role.ADMIN -> AdminHome(repo, s.user, signOut)
            Role.DOCTOR -> DoctorHome(repo, s.user, signOut)
            else -> PatientHome(repo, s.user, signOut)
        }
    }
}
