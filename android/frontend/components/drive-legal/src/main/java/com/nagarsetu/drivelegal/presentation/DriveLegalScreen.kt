package com.nagarsetu.drivelegal.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.core.ui.components.NagarSetuScreenBackground
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.NagarSetuColors

@Composable
fun DriveLegalScreen(viewModel: DriveLegalViewModel = hiltViewModel()) {
    var chatInput by remember { mutableStateOf("") }
    val chatMessages by viewModel.messages.collectAsState()
    val lastFine by viewModel.lastFine.collectAsState()
    val listState = rememberLazyListState()
    val s = LocalAppStrings.current

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.lastIndex)
        }
    }

    NagarSetuScreenBackground {
        Column(Modifier.fillMaxSize()) {
            // Header
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(NagarSetuColors.Surface, NagarSetuColors.Background)))
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("NAGARSETU", fontSize = 10.sp, color = NagarSetuColors.Accent, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        Text(s.service.serviceDriveLegal, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    val country = viewModel.currentCountry()
                    Text(country.flag, fontSize = 24.sp, modifier = Modifier.clickable { /* Country picker? */ })
                }
            }

            // Quick Actions & Fine Display
            Column(Modifier.padding(horizontal = 16.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        ActionChip(s.service.scanPlate, Icons.Default.QrCodeScanner, NagarSetuColors.Accent) { viewModel.openOcr() }
                    }
                    item {
                        ActionChip(s.service.finesInfo, Icons.Default.Info, NagarSetuColors.WarningOrange) { viewModel.sampleHelmetFine() }
                    }
                }
                
                lastFine?.let { fine ->
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NagarSetuColors.SurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gavel, null, tint = NagarSetuColors.SOSRed)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                val violationName = if (s.profile.langHindi == "हिन्दी") {
                                    fine.violation.name.replace("HELMET", "हेलमेट")
                                        .replace("SPEEDING", "ओवरस्पीडिंग")
                                        .replace("SIGNAL", "सिग्नल जंप")
                                        .replace("_", " ")
                                } else fine.violation.name.replace("_", " ")

                                Text(violationName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${s.service.estimatedFine}: ${viewModel.currentCountry().currencySymbol}${fine.totalAmount.toInt()}", color = NagarSetuColors.SOSRed, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // Chat Feed
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    ChatBubble(s.service.driveLegalGreeting, false)
                }
                items(chatMessages) { msg ->
                    ChatBubble(msg.user, true)
                    Spacer(Modifier.height(8.dp))
                    ChatBubble(msg.bot, false)
                }
            }

            // Suggestions
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.quickSuggestions) { suggestion ->
                    SuggestionChip(suggestion) { 
                        viewModel.sendMessage(suggestion)
                    }
                }
            }

            // Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NagarSetuColors.Surface,
                tonalElevation = 8.dp
            ) {
                Row(
                    Modifier.padding(12.dp).navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("${s.common.search}...", color = NagarSetuColors.TextSecondary, fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = NagarSetuColors.Background.copy(alpha = 0.5f),
                            unfocusedContainerColor = NagarSetuColors.Background.copy(alpha = 0.3f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (chatInput.isNotBlank()) {
                                viewModel.sendMessage(chatInput)
                                chatInput = ""
                            }
                        },
                        modifier = Modifier.background(NagarSetuColors.Accent, CircleShape).size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = NagarSetuColors.Background)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(NagarSetuColors.SurfaceVariant)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = NagarSetuColors.TextPrimary, fontSize = 12.sp)
    }
}

@Composable
private fun ChatBubble(text: String, isUser: Boolean) {
    val bg = if (isUser) NagarSetuColors.Accent else NagarSetuColors.SurfaceVariant
    val color = if (isUser) NagarSetuColors.Background else Color.White
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val shape = if (isUser) RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Box(
            modifier = Modifier.widthIn(max = 280.dp).clip(shape)
                .background(bg).padding(12.dp)
        ) {
            Text(text, color = color, fontSize = 14.sp)
        }
    }
}
