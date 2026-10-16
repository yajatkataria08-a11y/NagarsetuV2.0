package com.nagarsetu.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.core.ui.strings.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.alertSettings.collectAsState()
    val s = LocalAppStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.profile.alertSettings, color = NagarSetuColors.TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = NagarSetuColors.Accent) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NagarSetuColors.Background)
            )
        },
        containerColor = NagarSetuColors.Background
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            AlertToggleItem(s.profile.emergencySosAlerts, s.profile.emergencySosAlertsSub, settings["emergency"] ?: true) {
                viewModel.updateAlertSettings("emergency", it)
            }
            AlertToggleItem(s.profile.civicReportsUpdate, s.profile.civicReportsUpdateSub, settings["civic"] ?: true) {
                viewModel.updateAlertSettings("civic", it)
            }
            AlertToggleItem(s.profile.cityNewsAdvisories, s.profile.cityNewsAdvisoriesSub, settings["news"] ?: true) {
                viewModel.updateAlertSettings("news", it)
            }
        }
    }
}

@Composable
private fun AlertToggleItem(title: String, desc: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = NagarSetuColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(desc, color = NagarSetuColors.TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = NagarSetuColors.Accent)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val selectedLang by viewModel.selectedLanguage.collectAsState()
    val s = LocalAppStrings.current
    // Always show both options with their native name regardless of current locale
    val languages = listOf("en" to "English", "hi" to "हिन्दी")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.profile.appLanguageTitle, color = NagarSetuColors.TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = NagarSetuColors.Accent) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NagarSetuColors.Background)
            )
        },
        containerColor = NagarSetuColors.Background
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            languages.forEach { (code, name) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedLang == code) NagarSetuColors.Accent.copy(alpha = 0.1f) else Color.Transparent)
                        .border(if (selectedLang == code) BorderStroke(1.dp, NagarSetuColors.Accent) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
                        .clickable { viewModel.setLanguage(code) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = name, 
                        color = if (selectedLang == code) NagarSetuColors.Accent else NagarSetuColors.TextPrimary, 
                        fontSize = 16.sp,
                        fontWeight = if (selectedLang == code) FontWeight.Bold else FontWeight.Normal
                    )
                    if (selectedLang == code) {
                        Icon(Icons.Default.Check, null, tint = NagarSetuColors.Accent)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
