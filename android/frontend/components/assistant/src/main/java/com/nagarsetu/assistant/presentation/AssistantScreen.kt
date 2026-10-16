package com.nagarsetu.assistant.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.backend.core.assistant.AssistantRole
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.*

import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    onBack: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val s = LocalAppStrings.current
    val ui by viewModel.state.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(ui.messages.size) {
        if (ui.messages.isNotEmpty()) listState.animateScrollToItem(ui.messages.lastIndex)
    }

    Column(Modifier.fillMaxSize().background(NagarSetuColors.Background)) {
        // Header
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, s.common.backBtn, tint = NagarSetuColors.Accent)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(if(s.profile.langHindi=="हिन्दी") "नगरसेतु" else "NAGARSETU", fontSize = 10.sp, color = NagarSetuColors.Accent,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text(s.service.aiAssistant, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = NagarSetuColors.TextPrimary)
            }
            IconButton(onClick = { viewModel.clearChat() }) {
                Icon(Icons.Default.DeleteSweep, s.common.closeBtn, tint = NagarSetuColors.TextSecondary)
            }
        }

        // Welcome Box
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NagarSetuColors.Surface)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(NagarSetuColors.Accent.copy(0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = NagarSetuColors.Accent, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    s.generic.aiAssistantWelcome,
                    fontSize = 13.sp,
                    color = NagarSetuColors.TextSecondary
                )
            }
        }

        // Suggestions
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ui.quickPrompts) { (label, query) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(NagarSetuColors.SurfaceVariant)
                        .clickable { viewModel.sendQuick(query) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(label, fontSize = 12.sp, color = NagarSetuColors.TextPrimary)
                }
            }
        }

        // Chat History
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ui.messages) { msg ->
                ChatBubble(msg.text, msg.role == AssistantRole.USER)
            }
            if (ui.isLoading) {
                item {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = NagarSetuColors.Accent, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(s.generic.thinking, fontSize = 12.sp, color = NagarSetuColors.TextSecondary)
                    }
                }
            }
        }

        // Action Button from AI
        ui.lastSuggestedRoute?.let { route ->
            Button(
                onClick = { onNavigate(route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.OpenInNew, null, Modifier.size(18.dp), tint = NagarSetuColors.Background)
                Spacer(Modifier.width(8.dp))
                
                val routeName = route.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                val translatedRoute = if (s.profile.langHindi == "हिन्दी") {
                    routeName.replace("Road watch", s.roadWatch.roadWatchTitle)
                        .replace("Predictive", s.service.predictiveTitle)
                        .replace("Charge up", s.service.chargeUpTitle)
                        .replace("Park ease", s.service.parkEaseTitle)
                        .replace("Green route", s.service.greenRouteTitle)
                        .replace("Health watch", s.service.healthWatchTitle)
                        .replace("Authority", s.roadWatch.nearestAuthority)
                } else routeName

                Text("${s.generic.goTo} $translatedRoute", color = NagarSetuColors.Background)
            }
        }

        // Input Field
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(NagarSetuColors.Surface)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if(s.profile.langHindi=="हिन्दी") "नगरसेतु AI से पूछें..." else "Ask NagarSetu AI...", color = NagarSetuColors.TextSecondary) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = NagarSetuColors.TextPrimary,
                    unfocusedTextColor = NagarSetuColors.TextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                maxLines = 3
            )
            IconButton(
                onClick = {
                    viewModel.send(input)
                    input = ""
                },
                enabled = input.isNotBlank() && !ui.isLoading
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send, s.common.submitBtn,
                    tint = if (input.isNotBlank()) NagarSetuColors.Accent else NagarSetuColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(text: String, isUser: Boolean) {
    val bg = if (isUser) NagarSetuColors.Accent else NagarSetuColors.Surface
    val fg = if (isUser) NagarSetuColors.Background else NagarSetuColors.TextPrimary
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(color = bg, shape = shape) {
            Text(
                text = text.replace("**", ""),
                modifier = Modifier.padding(12.dp).widthIn(max = 280.dp),
                color = fg,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
