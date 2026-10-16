package com.nagarsetu.auth.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.core.ui.strings.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpFaqScreen(onBack: () -> Unit) {
    val s = LocalAppStrings.current
    val faqs = s.profile.faqItems

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.profile.helpFaqTitle, color = NagarSetuColors.TextPrimary, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = NagarSetuColors.Accent) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NagarSetuColors.Background)
            )
        },
        containerColor = NagarSetuColors.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NagarSetuColors.Accent.copy(alpha = 0.1f))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QuestionAnswer, null, tint = NagarSetuColors.Accent, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Knowledge Hub", fontWeight = FontWeight.Bold, color = NagarSetuColors.TextPrimary, fontSize = 18.sp)
                            Text("Find answers to safety & civic queries", color = NagarSetuColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            items(faqs) { (q, a) ->
                FaqItem(question = q, answer = a)
            }
            
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun FaqItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = question,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    color = NagarSetuColors.Accent,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = NagarSetuColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = NagarSetuColors.SurfaceVariant, thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = answer,
                        color = NagarSetuColors.TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            val s = LocalAppStrings.current
            TopAppBar(
                title = { Text(s.profile.privacyPolicyTitle, color = NagarSetuColors.TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = NagarSetuColors.Accent) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NagarSetuColors.Background)
            )
        },
        containerColor = NagarSetuColors.Background
    ) { padding ->
        val s = LocalAppStrings.current
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Text(
                    text = s.profile.privacyPolicyBody,
                    color = NagarSetuColors.TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
fun AboutAppScreen(onBack: () -> Unit) {
    val s = LocalAppStrings.current
    Column(
        Modifier.fillMaxSize().background(NagarSetuColors.Background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Info, null, tint = NagarSetuColors.Accent, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(16.dp))
        Text("NagarSetu", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NagarSetuColors.TextPrimary)
        Text(s.profile.aboutEdition, fontSize = 14.sp, color = NagarSetuColors.Accent)
        Spacer(Modifier.height(32.dp))
        Text("Bhopal Smart City Companion", color = NagarSetuColors.TextSecondary)
        Text(s.profile.aboutPoweredBy, color = NagarSetuColors.TextSecondary)
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(s.common.closeBtn, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
