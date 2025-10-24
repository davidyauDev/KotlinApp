package com.example.myapplication.ui.login

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
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.data.preferences.SessionManager
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.ui.user.UserViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    userViewModel: UserViewModel = viewModel()
) {
    val api = remember { RetrofitClient.apiWithoutToken }
    val repository = remember { AuthRepository(api) }

    val viewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(repository)
    )

    var email by remember { mutableStateOf("admin@example.com") }
    var password by remember { mutableStateOf("password") }
    var rememberMe by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val loginState by viewModel.loginState.collectAsState()
    val focusManager = LocalFocusManager.current
    val systemUiController = rememberSystemUiController()
    val logoBlue = Color(0xFF22446C)
    val background = Color(0xFFF9FBF6)
    SideEffect {
        systemUiController.setStatusBarColor(
            color = background,
            darkIcons = true
        )
    }
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            val successState = loginState as? LoginState.Success
            successState?.let {
                // Always set session in memory so Home and Camera see the logged user immediately
                userViewModel.saveUser(it.user.name, it.token, it.user.id, it.user.email)
                SessionManager.setSession(it.user.id, it.token, it.user.name, it.user.email)
                // Persist only if user checked "Recordarme"
                if (rememberMe) {
                    userViewModel.saveUser(it.user.name, it.token, it.user.id, it.user.email)
                }
            }
            onLoginSuccess()
        }
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
            val errorState = loginState as? LoginState.Error
            errorState?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it.message,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "¿Olvidó la contraseña?",
                modifier = Modifier.clickable { /* TODO */ },
                color = logoBlue,
                style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline)
            )
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))

        }
    }
}
