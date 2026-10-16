package com.nagarsetu.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.core.ui.strings.LocalAppStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val s = LocalAppStrings.current
    val loginVm: LoginViewModel = hiltViewModel()
    val user = loginVm.currentUser()
    
    var name by remember(user) { mutableStateOf(user?.name ?: "") }
    var email by remember(user) { mutableStateOf(user?.email ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.profile.personalInfoTitle, color = NagarSetuColors.TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = NagarSetuColors.Accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NagarSetuColors.Background)
            )
        },
        containerColor = NagarSetuColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                s.profile.updateIdentityHint,
                color = NagarSetuColors.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(s.profile.fullName, color = NagarSetuColors.TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, null, tint = NagarSetuColors.Accent) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NagarSetuColors.Accent,
                    unfocusedBorderColor = NagarSetuColors.SurfaceVariant,
                    focusedTextColor = NagarSetuColors.TextPrimary,
                    unfocusedTextColor = NagarSetuColors.TextPrimary,
                    focusedContainerColor = NagarSetuColors.Surface,
                    unfocusedContainerColor = NagarSetuColors.Surface
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(s.profile.emailAddress, color = NagarSetuColors.TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Email, null, tint = NagarSetuColors.Accent) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NagarSetuColors.Accent,
                    unfocusedBorderColor = NagarSetuColors.SurfaceVariant,
                    focusedTextColor = NagarSetuColors.TextPrimary,
                    unfocusedTextColor = NagarSetuColors.TextPrimary,
                    focusedContainerColor = NagarSetuColors.Surface,
                    unfocusedContainerColor = NagarSetuColors.Surface
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    isLoading = true
                    viewModel.updateProfile(name, email)
                    scope.launch {
                        delay(1000)
                        isLoading = false
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent),
                enabled = !isLoading && name.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(s.profile.saveChanges, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
