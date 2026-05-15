package com.example.nothingpodcast.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nothingpodcast.ui.theme.*

@Composable
fun LoginScreen(
    onContinueAsGuest: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var showPwd     by remember { mutableStateOf(false) }
    var showNewPwd  by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Back Arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (uiState.isRegisterMode || uiState.isResetMode) {
                            if (uiState.isRegisterMode) viewModel.toggleRegisterMode()
                            if (uiState.isResetMode) viewModel.toggleResetMode()
                        } else {
                            onContinueAsGuest()
                        }
                    },
                    modifier = Modifier.offset(x = (-12).dp)
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = NothingWhite)
                }
            }

            // Title
            Text(
                text = when {
                    uiState.isResetMode    -> "Reimposta"
                    uiState.isRegisterMode -> "Crea account"
                    else                   -> "Accedi"
                },
                style = MaterialTheme.typography.displayMedium,
                color = NothingWhite,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(Modifier.height(48.dp))

            // Email field
            NothingTextField(
                value         = email,
                onValueChange = { email = it },
                label         = "Indirizzo e-mail",
                keyboardType  = KeyboardType.Email,
                imeAction     = ImeAction.Next,
                onImeAction   = { focusManager.moveFocus(FocusDirection.Down) }
            )

            Spacer(Modifier.height(16.dp))

            // Password field (hidden in reset mode)
            AnimatedVisibility(visible = !uiState.isResetMode) {
                NothingTextField(
                    value                = password,
                    onValueChange        = { password = it },
                    label                = "Password",
                    keyboardType         = KeyboardType.Password,
                    visualTransformation = if (showPwd) VisualTransformation.None
                                          else PasswordVisualTransformation(),
                    imeAction            = ImeAction.Done,
                    onImeAction          = { focusManager.clearFocus() },
                    trailingIcon         = {
                        IconButton(onClick = { showPwd = !showPwd }) {
                            Icon(
                                if (showPwd) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = NothingWhite
                            )
                        }
                    }
                )
            }

            // New password field (reset mode only)
            AnimatedVisibility(visible = uiState.isResetMode) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    NothingTextField(
                        value                = newPassword,
                        onValueChange        = { newPassword = it },
                        label                = "Nuova Password",
                        keyboardType         = KeyboardType.Password,
                        visualTransformation = if (showNewPwd) VisualTransformation.None
                                              else PasswordVisualTransformation(),
                        imeAction            = ImeAction.Done,
                        onImeAction          = { focusManager.clearFocus() },
                        trailingIcon         = {
                            IconButton(onClick = { showNewPwd = !showNewPwd }) {
                                Icon(
                                    if (showNewPwd) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    tint = NothingWhite
                                )
                            }
                        }
                    )
                }
            }

            // Terms Checkbox (Only for SignIn and Register)
            AnimatedVisibility(visible = !uiState.isResetMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = acceptedTerms,
                        onCheckedChange = { acceptedTerms = it },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                            checkedColor = NothingWhite,
                            checkmarkColor = NothingBlack,
                            uncheckedColor = NothingWhite
                        ),
                        modifier = Modifier.offset(x = (-12).dp, y = (-12).dp)
                    )
                    Text(
                        text = buildAnnotatedString {
                            append("Ho letto e accetto l'")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = NothingWhite)) { append("Accordo con l'utente") }
                            append(" e l'")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = NothingWhite)) { append("Informativa sulla privacy") }
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = NothingOnSurfaceDim,
                        modifier = Modifier.offset(x = (-16).dp),
                        lineHeight = 18.sp
                    )
                }
            }
            
            if (uiState.isResetMode) {
                Spacer(Modifier.height(24.dp))
            }

            // Primary action button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    when {
                        uiState.isResetMode    -> viewModel.resetPassword(email, newPassword)
                        uiState.isRegisterMode -> viewModel.createAccount(email, password)
                        else                   -> viewModel.signInWithEmail(email, password)
                    }
                },
                enabled = uiState.isResetMode || acceptedTerms,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = NothingWhite,
                    contentColor = NothingBlack,
                    disabledContainerColor = androidx.compose.ui.graphics.Color(0xFF7A7A7A),
                    disabledContentColor = NothingBlack
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color       = NothingBlack,
                        strokeWidth = 2.dp,
                        modifier    = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Continua",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        fontSize = 16.sp
                    )
                }
            }

            // Error / success banner
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                val isSuccess = (uiState.errorMessage ?: "").contains("✓")
                Text(
                    text      = uiState.errorMessage ?: "",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = if (isSuccess) NothingAccentDim else NothingError,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .border(1.dp, if (isSuccess) NothingAccentDim else NothingError, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .padding(14.dp)
                )
            }

            // Forgot password
            AnimatedVisibility(visible = !uiState.isResetMode && !uiState.isRegisterMode) {
                TextButton(
                    onClick = { viewModel.toggleResetMode() },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = "Hai dimenticato la password?",
                        color = NothingWhite,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Register/SignIn Toggle
            if (!uiState.isResetMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = NothingBorderDim, thickness = 0.5.dp)
                    Text(
                        text = "OPPURE",
                        color = NothingOnSurfaceDim,
                        letterSpacing = 2.sp,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = NothingBorderDim, thickness = 0.5.dp)
                }

                Button(
                    onClick = { 
                        if (uiState.isRegisterMode) viewModel.toggleRegisterMode() // back to sign in
                        else viewModel.toggleRegisterMode() // to register
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF1F1F1F),
                        contentColor = NothingWhite
                    )
                ) {
                    Text(
                        text = if (uiState.isRegisterMode) "Accedi al tuo account" else "Crea un account",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Guest access button
            OutlinedButton(
                onClick  = onContinueAsGuest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                border = BorderStroke(1.dp, NothingBorderDim),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = NothingWhite
                )
            ) {
                Text(
                    text  = "Continua senza account",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Reusable field ────────────────────────────────────────────────────────────

@Composable
private fun NothingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType         = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    imeAction: ImeAction               = ImeAction.Next,
    onImeAction: () -> Unit            = {},
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value              = value,
        onValueChange      = onValueChange,
        label              = { Text(label, style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color(0xFFCCCCCC)) },
        singleLine         = true,
        visualTransformation = visualTransformation,
        keyboardOptions    = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions    = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }),
        trailingIcon       = trailingIcon,
        shape              = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors             = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = NothingWhite,
            unfocusedBorderColor    = androidx.compose.ui.graphics.Color(0xFF888888),
            cursorColor             = NothingWhite,
            focusedTextColor        = NothingWhite,
            unfocusedTextColor      = NothingWhite,
            focusedContainerColor   = NothingBlack,
            unfocusedContainerColor = NothingBlack
        ),
        textStyle = MaterialTheme.typography.bodyLarge,
        modifier  = Modifier.fillMaxWidth()
    )
}
