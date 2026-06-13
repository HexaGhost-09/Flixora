package com.hexaghost.flixora.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hexaghost.flixora.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDialog(
    onDismiss: () -> Unit,
    viewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSignUpMode by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    var localError by remember { mutableStateOf<String?>(null) }

    val errorToDisplay = uiState.error ?: localError

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = FlixoraDarkSurface,
            border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Title
                Text(
                    text = if (isSignUpMode) "Create Account" else "Welcome Back",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = FlixoraWhite,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = if (isSignUpMode) "Sign up with Neon Auth" else "Login using Neon Auth",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlixoraWhite60,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Auth Mode Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FlixoraDarkBg, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    val tabModifier = Modifier
                        .weight(1f)
                        .height(36.dp)

                    Button(
                        onClick = {
                            isSignUpMode = false
                            localError = null
                            viewModel.clearError()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isSignUpMode) FlixoraPurple else Color.Transparent,
                            contentColor = FlixoraWhite
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = tabModifier,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Sign In", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            isSignUpMode = true
                            localError = null
                            viewModel.clearError()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSignUpMode) FlixoraPurple else Color.Transparent,
                            contentColor = FlixoraWhite
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = tabModifier,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Sign Up", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Input fields
                if (isSignUpMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name", color = FlixoraWhite60) },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = FlixoraCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlixoraCyan,
                            unfocusedBorderColor = FlixoraWhite40,
                            focusedTextColor = FlixoraWhite,
                            unfocusedTextColor = FlixoraWhite,
                            cursorColor = FlixoraCyan,
                            focusedContainerColor = FlixoraDarkCard,
                            unfocusedContainerColor = FlixoraDarkCard
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", color = FlixoraWhite60) },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = FlixoraCyan) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlixoraCyan,
                        unfocusedBorderColor = FlixoraWhite40,
                        focusedTextColor = FlixoraWhite,
                        unfocusedTextColor = FlixoraWhite,
                        cursorColor = FlixoraCyan,
                        focusedContainerColor = FlixoraDarkCard,
                        unfocusedContainerColor = FlixoraDarkCard
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = FlixoraWhite60) },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = FlixoraCyan) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlixoraCyan,
                        unfocusedBorderColor = FlixoraWhite40,
                        focusedTextColor = FlixoraWhite,
                        unfocusedTextColor = FlixoraWhite,
                        cursorColor = FlixoraCyan,
                        focusedContainerColor = FlixoraDarkCard,
                        unfocusedContainerColor = FlixoraDarkCard
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Error Message
                if (errorToDisplay != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorToDisplay,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, FlixoraWhite40),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FlixoraWhite)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank() || (isSignUpMode && name.isBlank())) {
                                localError = "Please fill in all details"
                            } else {
                                localError = null
                                if (isSignUpMode) {
                                    viewModel.signUp(email, password, name)
                                } else {
                                    viewModel.signIn(email, password)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FlixoraCyan,
                            contentColor = FlixoraDarkBg
                        ),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = FlixoraDarkBg,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUpMode) "Register" else "Login",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    val context = LocalContext.current
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AuthUiEvent.SignUpSuccess -> {
                    Toast.makeText(context, "Sign up successful! Welcome to Flixora", Toast.LENGTH_SHORT).show()
                }
                is AuthUiEvent.SignInSuccess -> {
                    Toast.makeText(context, "Welcome back! Login successful", Toast.LENGTH_SHORT).show()
                }
                is AuthUiEvent.Error -> {
                    Toast.makeText(context, "Authentication failed: ${event.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Automatically close dialog upon successful login
    LaunchedEffect(viewModel.isLoggedIn.collectAsState().value) {
        if (viewModel.isLoggedIn.value) {
            onDismiss()
        }
    }
}
