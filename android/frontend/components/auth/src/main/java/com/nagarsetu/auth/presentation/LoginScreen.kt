package com.nagarsetu.auth.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nagarsetu.auth.domain.model.OtpState
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.NagarSetuColors
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    onLoginSuccess: (uid: String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val s = LocalAppStrings.current
    val state by viewModel.otpState.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is OtpState.Verified) {
            onLoginSuccess((state as OtpState.Verified).profile.uid)
        }
    }

    var screenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        screenVisible = true
    }

    val screenAlpha by animateFloatAsState(
        targetValue = if (screenVisible) 1f else 0f,
        animationSpec = tween(600, easing = LinearOutSlowInEasing),
        label = "fade"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NagarSetuColors.Background)
            .alpha(screenAlpha)
    ) {
        // Decorative background elements
        LoginBackgroundDecor()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationCity,
                contentDescription = null,
                tint = NagarSetuColors.Accent,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                s.auth.loginTitle,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = NagarSetuColors.TextPrimary
            )
            Text(
                s.auth.loginSub,
                style = MaterialTheme.typography.bodyMedium,
                color = NagarSetuColors.TextSecondary
            )

            Spacer(Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        (fadeIn(tween(300)) + scaleIn(initialScale = 0.92f))
                            .togetherWith(fadeOut(tween(200)))
                    },
                    label = "auth_step"
                ) { currentState ->
                    when (currentState) {
                        is OtpState.Idle, is OtpState.SendingOtp, is OtpState.Error -> {
                            PhoneInputStep(
                                isLoading = currentState is OtpState.SendingOtp,
                                error = (currentState as? OtpState.Error)?.message,
                                onSend = viewModel::sendOtp,
                                onGuest = viewModel::loginAsGuest
                            )
                        }
                        is OtpState.OtpSent, is OtpState.VerifyingOtp -> {
                            OtpVerifyStep(
                                phone = (currentState as? OtpState.OtpSent)?.phone ?: "",
                                isLoading = currentState is OtpState.VerifyingOtp,
                                onVerify = viewModel::verifyOtp,
                                onBack = viewModel::goBackToPhone
                            )
                        }
                        is OtpState.GuestMode -> {
                            Box(Modifier.padding(40.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = NagarSetuColors.Accent)
                            }
                        }
                        is OtpState.Verified -> {
                            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, null, tint = NagarSetuColors.SuccessGreen, modifier = Modifier.size(48.dp))
                                Text(s.auth.success, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneInputStep(
    isLoading: Boolean,
    error: String?,
    onSend: (String) -> Unit,
    onGuest: () -> Unit
) {
    val s = LocalAppStrings.current
    var phone by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.padding(24.dp)) {
        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.length <= 10) phone = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(s.auth.phoneNumber, color = NagarSetuColors.TextSecondary) },
            prefix = { Text("+91 ", color = NagarSetuColors.Accent, fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { 
                focusManager.clearFocus()
                onSend("+91$phone") 
            }),
            isError = error != null,
            supportingText = error?.let { { Text(it, color = NagarSetuColors.SOSRed) } },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NagarSetuColors.Accent,
                unfocusedBorderColor = NagarSetuColors.SurfaceVariant,
                focusedTextColor = NagarSetuColors.TextPrimary,
                unfocusedTextColor = NagarSetuColors.TextPrimary,
                focusedContainerColor = NagarSetuColors.Background,
                unfocusedContainerColor = NagarSetuColors.Background
            )
        )
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = { onSend("+91$phone") },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = phone.length == 10 && !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent)
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = NagarSetuColors.Background)
            else Text(s.auth.sendOtp, color = NagarSetuColors.Background, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onGuest, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(s.auth.continueGuest, color = NagarSetuColors.TextSecondary)
        }
    }
}

@Composable
private fun OtpVerifyStep(
    phone: String,
    isLoading: Boolean,
    onVerify: (String) -> Unit,
    onBack: () -> Unit
) {
    val s = LocalAppStrings.current
    var otp by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(s.auth.verifyPhone, color = NagarSetuColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("${s.auth.sentTo} $phone", style = MaterialTheme.typography.bodySmall, color = NagarSetuColors.TextSecondary)
        
        Spacer(Modifier.height(24.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(6) { index ->
                OtpBox(
                    char = otp.getOrNull(index)?.toString() ?: "",
                    isFocused = otp.length == index
                )
            }
        }
        
        // Hidden input
        TextField(
            value = otp,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otp = it },
            modifier = Modifier.size(1.dp).focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (otp.length == 6) onVerify(otp) })
        )

        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = { onVerify(otp) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = otp.length == 6 && !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent)
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = NagarSetuColors.Background)
            else Text(s.auth.verifyLogin, color = NagarSetuColors.Background, fontWeight = FontWeight.Bold)
        }
        
        TextButton(onClick = onBack) {
            Text(s.auth.editPhone, color = NagarSetuColors.TextSecondary)
        }
    }
}

@Composable
private fun OtpBox(char: String, isFocused: Boolean) {
    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, label = "scale")
    Box(
        modifier = Modifier
            .size(44.dp, 54.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) NagarSetuColors.Accent.copy(alpha = 0.1f) else NagarSetuColors.Background)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) NagarSetuColors.Accent else NagarSetuColors.SurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(char, color = NagarSetuColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LoginBackgroundDecor() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .size(300.dp)
                .offset(x = (-150).dp, y = (-100).dp)
                .background(NagarSetuColors.Accent.copy(alpha = 0.05f), CircleShape)
        )
        Box(
            Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .background(NagarSetuColors.Accent.copy(alpha = 0.05f), CircleShape)
        )
    }
}
