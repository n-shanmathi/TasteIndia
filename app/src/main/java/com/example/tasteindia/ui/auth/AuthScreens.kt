package com.example.tasteindia.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.tasteindia.data.auth.FirebaseConfigurationException
import com.example.tasteindia.data.auth.toAuthErrorMessage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthGate(
    viewModel: AuthViewModel,
    content: @Composable () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val user = state.user
    when {
        state.isInitializing -> LoadingScreen()
        user == null -> SignInScreen(viewModel)
        user.isEmailVerified || user.providerData.any { it.providerId != "password" } ->
            content()
        else -> VerificationScreen(viewModel)
    }
}

@Composable
fun SignInScreen(viewModel: AuthViewModel) {
    var showSignUp by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    when {
        showSignUp -> SignUpScreen(viewModel, onBack = { showSignUp = false })
        showForgotPassword -> ForgotPasswordScreen(viewModel, onBack = { showForgotPassword = false })
        else -> AuthForm(
            title = "Welcome back",
            subtitle = "Sign in to save your favourite Indian recipes.",
            submitLabel = "Sign in",
            viewModel = viewModel,
            onSubmit = { email, password, _, _ -> viewModel.signIn(email, password) },
            onSecondary = { showSignUp = true },
            secondaryText = "Create an account",
            onForgotPassword = { showForgotPassword = true }
        )
    }
}

@Composable
private fun SignUpScreen(viewModel: AuthViewModel, onBack: () -> Unit) {
    AuthForm(
        title = "Create your account",
        subtitle = "Save recipes and pick up where you left off.",
        submitLabel = "Create account",
        viewModel = viewModel,
        showName = true,
        showConfirmation = true,
        onSubmit = { email, password, confirmation, name ->
            if (password != confirmation) {
                viewModel.setValidationMessage("Passwords do not match.")
            } else {
                viewModel.signUp(name, email, password)
            }
        },
        onSecondary = onBack,
        secondaryText = "Already have an account? Sign in",
        onForgotPassword = null
    )
}

@Composable
private fun ForgotPasswordScreen(viewModel: AuthViewModel, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.clearMessage()
    }
    AuthScaffold {
        AuthHeader("Reset your password", "We will send a secure reset link to your email.")
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        AuthMessage(state.message, state.isSuccess, "Reset email sent. Check your inbox.")
        Button(
            onClick = {
                if (android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                    viewModel.sendPasswordReset(email)
                } else {
                    viewModel.setValidationMessage("Enter a valid email address.")
                }
            },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSubmitting) CircularProgressIndicator(strokeWidth = 2.dp)
            else Text("Send reset email")
        }
        TextButton(onClick = onBack) { Text("Back to sign in") }
    }
}

@Composable
private fun AuthForm(
    title: String,
    subtitle: String,
    submitLabel: String,
    viewModel: AuthViewModel,
    showName: Boolean = false,
    showConfirmation: Boolean = false,
    onSubmit: (String, String, String, String) -> Unit,
    onSecondary: () -> Unit,
    secondaryText: String,
    onForgotPassword: (() -> Unit)?
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmationVisible by remember { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val token = account.idToken
            if (token.isNullOrBlank()) {
                viewModel.setValidationMessage("Google did not return an ID token.")
            } else {
                viewModel.signInWithGoogle(token)
            }
        } catch (error: ApiException) {
            viewModel.setValidationMessage(error.toAuthErrorMessage())
        }
    }

    AuthScaffold {
        AuthHeader(title, subtitle)
        if (showName) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        PasswordField(
            value = password,
            label = "Password",
            visible = passwordVisible,
            onVisibleChange = { passwordVisible = !passwordVisible },
            onValueChange = { password = it }
        )
        if (showConfirmation) {
            Spacer(Modifier.height(12.dp))
            PasswordField(
                value = confirmation,
                label = "Confirm password",
                visible = confirmationVisible,
                onVisibleChange = { confirmationVisible = !confirmationVisible },
                onValueChange = { confirmation = it }
            )
        }
        Spacer(Modifier.height(16.dp))
        AuthMessage(state.message, state.isSuccess)
        Button(
            onClick = {
                val validEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
                when {
                    !validEmail -> viewModel.setValidationMessage("Enter a valid email address.")
                    password.length < 6 -> viewModel.setValidationMessage("Password must be at least 6 characters.")
                    showName && name.isBlank() -> viewModel.setValidationMessage("Enter your name.")
                    else -> onSubmit(email, password, confirmation, name)
                }
            },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSubmitting) CircularProgressIndicator(strokeWidth = 2.dp)
            else Text(submitLabel)
        }
        if (onForgotPassword != null) {
            TextButton(
                onClick = onForgotPassword,
                modifier = Modifier.align(Alignment.End)
            ) { Text("Forgot password?") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f))
            Text("  or  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                val options = viewModel.googleSignInOptions(context)
                if (options == null) {
                    viewModel.setValidationMessage(
                        FirebaseConfigurationException(
                            "Google sign-in is not configured."
                        ).toAuthErrorMessage()
                    )
                } else {
                    googleLauncher.launch(GoogleSignIn.getClient(context, options).signInIntent)
                }
            },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue with Google") }
        TextButton(onClick = onSecondary) { Text(secondaryText) }
    }
}

@Composable
private fun PasswordField(
    value: String,
    label: String,
    visible: Boolean,
    onVisibleChange: () -> Unit,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onVisibleChange) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun VerificationScreen(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsState()
    AuthScaffold {
        AuthHeader(
            "Verify your email",
            "Check your inbox for a verification link before continuing."
        )
        AuthMessage(state.message, state.isSuccess)
        Button(
            onClick = { viewModel.reloadUser() },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) { Text("I verified my email") }
        OutlinedButton(
            onClick = { viewModel.resendVerificationEmail() },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Resend verification email") }
        TextButton(onClick = viewModel::signOut) { Text("Use a different account") }
    }
}

@Composable
private fun AuthScaffold(content: @Composable ColumnScope.() -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun AuthHeader(title: String, subtitle: String) {
    Text("TasteIndia", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(24.dp))
    Text(title, style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun AuthMessage(message: String?, isSuccess: Boolean, successMessage: String = "Done.") {
    if (!message.isNullOrBlank()) {
        Text(
            text = if (isSuccess) successMessage else message,
            color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text("Loading TasteIndia...")
    }
}
