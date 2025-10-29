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
import androidx.compose.ui.text.style.TextAlign
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

    var email by remember { mutableStateOf("porteria@cechriza.net") }
    var password by remember { mutableStateOf("password123") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val loginState by viewModel.loginState.collectAsState()
    val focusManager = LocalFocusManager.current
    val systemUiController = rememberSystemUiController()

    val primaryBlue = Color(0xFF22446C)
    val background = Color(0xFFF9FBF6)

    SideEffect {
        systemUiController.setStatusBarColor(color = background, darkIcons = true)
    }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            val state = loginState as LoginState.Success
            userViewModel.setUserInMemory(state.user.name, state.token, state.user.id, state.user.email)
            userViewModel.setEmpCodeInMemory(state.user.empCode)
            SessionManager.setSession(state.user.id, state.token, state.user.name, state.user.email, state.user.empCode)
            userViewModel.saveEmpCode(state.user.empCode)
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
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                label = { Text("Correo electrónico") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                isError = emailError != null,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
            )

            emailError?.let {
                Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                isError = passwordError != null,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            passwordError?.let {
                Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val emailTrimmed = email.trim()
                    var isValid = true

                    if (emailTrimmed.isBlank()) {
                        emailError = "Correo requerido"
                        isValid = false
                    } else if (!Patterns.EMAIL_ADDRESS.matcher(emailTrimmed).matches()) {
                        emailError = "Correo inválido"
                        isValid = false
                    }

                    if (password.isBlank()) {
                        passwordError = "Contraseña requerida"
                        isValid = false
                    } else if (password.length < 6) {
                        passwordError = "Mínimo 6 caracteres"
                        isValid = false
                    }

                    if (isValid) {
                        viewModel.login(emailTrimmed, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                enabled = loginState !is LoginState.Loading
            ) {
                if (loginState is LoginState.Loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Iniciar Sesión")
            }

            Spacer(modifier = Modifier.height(12.dp))

            val errorState = loginState as? LoginState.Error
            errorState?.let {
                Text(
                    text = it.message,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "¿Olvidó su contraseña?",
                modifier = Modifier
                    .clickable { /* TODO: Navegar a pantalla de recuperación */ }
                    .padding(4.dp),
                color = primaryBlue,
                style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline)
            )

            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}
