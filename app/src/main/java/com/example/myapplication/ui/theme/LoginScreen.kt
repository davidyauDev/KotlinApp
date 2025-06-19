package com.example.myapplication.ui.theme

import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.ui.theme.viewmodel.LoginState
import com.example.myapplication.ui.theme.viewmodel.LoginViewModel

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, viewModel: LoginViewModel = viewModel()) {
    var email by remember { mutableStateOf("admin@example.com") }
    var password by remember { mutableStateOf("12345678") }
    var acceptTerms by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var termsError by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }

    val loginState by viewModel.loginState.collectAsState()

    val focusManager = LocalFocusManager.current

    val logoBlue = Color(0xFF0051A8)
    val background = Color(0xFFF9FBF6)

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) onLoginSuccess()
    }

    Box(

        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_cechriza),
                contentDescription = "Logo CECHRIZA",
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                label = { Text("Nombre de Usuario") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                isError = emailError != null,
                modifier = Modifier.fillMaxWidth()
            )
            emailError?.let {
                Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                },
                label = { Text("Contraseña") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                visualTransformation = PasswordVisualTransformation(),
                isError = passwordError != null,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus() // ✅ Esto cierra el teclado
                })
            )
            passwordError?.let {
                Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                Text("Recordarme")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    var valid = true
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailError = "Correo inválido"
                        valid = false
                    }
                    if (password.isBlank()) {
                        passwordError = "Contraseña requerida"
                        valid = false
                    }
                    if (valid) {
                        ///showCamera = true
                        viewModel.login(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = logoBlue),
                enabled = loginState !is LoginState.Loading
            ) {
                Text(if (loginState is LoginState.Loading) "Iniciando..." else "Iniciar Sesión")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "¿Olvidó la contraseña?",
                modifier = Modifier.clickable { /* TODO */ },
                color = logoBlue,
                style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline)
            )
        }


    }


}




