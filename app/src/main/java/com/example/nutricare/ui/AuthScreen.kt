package com.example.nutricare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.nutricare.data.Repository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.launch

private fun friendlyError(e: Exception): String = when (e) {
    is FirebaseAuthWeakPasswordException -> "رمز عبور باید حداقل ۶ کاراکتر باشد"
    is FirebaseAuthUserCollisionException -> "این ایمیل قبلاً ثبت شده است"
    is FirebaseAuthInvalidCredentialsException,
    is FirebaseAuthInvalidUserException -> "ایمیل یا رمز عبور نادرست است"
    else -> "خطا: ${e.localizedMessage ?: "نامشخص"}"
}

@Composable
fun AuthScreen(repo: Repository, initialMessage: String?, onAuthenticated: () -> Unit) {
    var registerMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(initialMessage) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(48.dp))
        Text("مشاوره تغذیه و پزشکی", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(if (registerMode) "ثبت‌نام بیمار جدید" else "ورود به حساب کاربری")
        Spacer(Modifier.height(24.dp))

        if (registerMode) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("نام و نام خانوادگی") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = email, onValueChange = { email = it.trim() },
            label = { Text("ایمیل") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("رمز عبور") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            enabled = !loading && email.isNotBlank() && password.isNotBlank() &&
                (!registerMode || name.isNotBlank()),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                error = null
                loading = true
                scope.launch {
                    try {
                        if (registerMode) repo.register(name.trim(), email, password)
                        else repo.login(email, password)
                        onAuthenticated()
                    } catch (e: Exception) {
                        error = friendlyError(e)
                    } finally {
                        loading = false
                    }
                }
            },
        ) { Text(if (registerMode) "ثبت‌نام" else "ورود") }

        TextButton(onClick = { registerMode = !registerMode; error = null }) {
            Text(if (registerMode) "حساب دارید؟ وارد شوید" else "حساب ندارید؟ ثبت‌نام کنید")
        }
    }
}
