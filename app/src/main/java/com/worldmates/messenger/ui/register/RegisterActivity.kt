package com.worldmates.messenger.ui.register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.worldmates.messenger.ui.chats.ChatsActivity
import com.worldmates.messenger.ui.components.GradientButton
import com.worldmates.messenger.ui.theme.*
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var viewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(RegisterViewModel::class.java)

        setContent {
            WorldMatesTheme {
                RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = { navigateToChats() },
                    onBackToLogin = { finish() }
                )
            }
        }

        lifecycleScope.launch {
            viewModel.registerState.collect { state ->
                when (state) {
                    is RegisterState.Success -> {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Реєстрація успішна!",
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateToChats()
                    }
                    is RegisterState.Error -> {
                        Toast.makeText(
                            this@RegisterActivity,
                            state.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun navigateToChats() {
        startActivity(Intent(this, ChatsActivity::class.java))
        finish()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val registerState by viewModel.registerState.collectAsState()
    val isLoading = registerState is RegisterState.Loading

    // Анимация фона
    val infiniteTransition = rememberInfiniteTransition()
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GradientStart,
                        GradientEnd,
                        WMPrimary
                    ),
                    startY = gradientOffset,
                    endY = gradientOffset + 1000f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Заголовок
            Text(
                "Створити акаунт",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Приєднуйтесь до WorldMates",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Форма регистрации
            RegisterFormCard(
                username = username,
                onUsernameChange = { username = it },
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                confirmPassword = confirmPassword,
                onConfirmPasswordChange = { confirmPassword = it },
                passwordVisible = passwordVisible,
                onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                confirmPasswordVisible = confirmPasswordVisible,
                onConfirmPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                isLoading = isLoading,
                onRegisterClick = {
                    if (username.isNotEmpty() && email.isNotEmpty() &&
                        password.isNotEmpty() && password == confirmPassword) {
                        viewModel.register(username, email, password, confirmPassword)
                    }
                },
                registerState = registerState
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопка назад
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Вже маєте акаунт? ",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
                TextButton(
                    onClick = onBackToLogin
                ) {
                    Text(
                        "Увійти",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RegisterFormCard(
    username: String,
    onUsernameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    confirmPasswordVisible: Boolean,
    onConfirmPasswordVisibilityToggle: () -> Unit,
    isLoading: Boolean,
    onRegisterClick: () -> Unit,
    registerState: RegisterState
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Username field
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("Ім'я користувача") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WMPrimary,
                    focusedLabelColor = WMPrimary,
                    cursorColor = WMPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !isLoading,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WMPrimary,
                    focusedLabelColor = WMPrimary,
                    cursorColor = WMPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Пароль") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Сховати пароль" else "Показати пароль"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !isLoading,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WMPrimary,
                    focusedLabelColor = WMPrimary,
                    cursorColor = WMPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Підтвердіть пароль") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = onConfirmPasswordVisibilityToggle) {
                        Icon(
                            if (confirmPasswordVisible) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Сховати пароль" else "Показати пароль"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (confirmPasswordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !isLoading,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WMPrimary,
                    focusedLabelColor = WMPrimary,
                    cursorColor = WMPrimary
                ),
                isError = confirmPassword.isNotEmpty() && password != confirmPassword
            )

            if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Паролі не співпадають",
                    color = Error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Register button
            GradientButton(
                text = "Зареєструватися",
                onClick = onRegisterClick,
                enabled = username.isNotEmpty() && email.isNotEmpty() &&
                         password.isNotEmpty() && password == confirmPassword && !isLoading,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            // Error message
            if (registerState is RegisterState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Error.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = registerState.message,
                        color = Error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
