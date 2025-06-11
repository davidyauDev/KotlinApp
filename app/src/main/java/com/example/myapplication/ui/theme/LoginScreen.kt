package com.example.myapplication.ui.theme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.viewmodel.LoginState
import com.example.myapplication.ui.theme.viewmodel.LoginViewModel
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val loginState by viewModel.loginState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Iniciar Sesión", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.login(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = loginState !is LoginState.Loading
        ) {
            Text("Entrar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (loginState) {
            is LoginState.Loading -> CircularProgressIndicator()
            is LoginState.Success -> Text("¡Bienvenido, ${(loginState as LoginState.Success).username}!")
            is LoginState.Error -> Text((loginState as LoginState.Error).message, color = MaterialTheme.colorScheme.error)
            else -> {}
        }
    }
}