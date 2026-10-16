package com.nagarsetu.raksha.presentation.emergencyguide

/**
 * EmergencyGuideScreen.kt
 *
 * Ported from Raksha/SafePath's EmergencyGuideActivity into Jetpack Compose.
 * Provides 14 detailed safety guides (self-defense, legal rights, FIR filing,
 * POSH Act, etc.) entirely offline — no network required.
 *
 * Connected to NagarSetu via:
 *  • MainActivity composable("emergency_guide")
 *  • RakshaScreen "Guide" quick-action card
 *  • NagarSetuColors design system
 */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.core.ui.theme.AlertRed
import com.nagarsetu.core.ui.theme.PrimaryBlue
import com.nagarsetu.raksha.data.guide.EmergencyGuideRepository
import com.nagarsetu.raksha.domain.model.EmergencyGuideItem

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyGuideScreen(
    onBack: () -> Unit = {}
) {
    val repository = remember { EmergencyGuideRepository() }
    var query by remember { mutableStateOf("") }
    val items = remember(query) { repository.search(query) }

    Scaffold(
        containerColor = NagarSetuColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Emergency Guide",
                            fontWeight = FontWeight.Bold,
                            style      = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Safety tips & legal rights — offline",
                            color = NagarSetuColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NagarSetuColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NagarSetuColors.Surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Search bar ──────────────────────────────────────────────────
            item(key = "search") {
                OutlinedTextField(
                    value         = query,
                    onValueChange = { query = it },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder   = { Text("Search guides…", color = NagarSetuColors.TextSecondary) },
                    leadingIcon   = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = NagarSetuColors.TextSecondary)
                    },
                    trailingIcon  = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine    = true,
                    shape         = RoundedCornerShape(16.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryBlue,
                        unfocusedBorderColor = NagarSetuColors.SurfaceVariant,
                        focusedContainerColor   = NagarSetuColors.Surface,
                        unfocusedContainerColor = NagarSetuColors.Surface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            }

            // ── Emergency callout banner ────────────────────────────────────
            item(key = "banner") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape  = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.10f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.22f))
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚨", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Emergency? Call 100 or 112 immediately",
                                fontWeight = FontWeight.SemiBold,
                                color      = AlertRed,
                                style      = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Women's Helpline: 1091  •  Ambulance: 108",
                                color = NagarSetuColors.TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Empty state ────────────────────────────────────────────────
            if (items.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔍", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No guides found for \"$query\"",
                            color = NagarSetuColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ── Guide items ─────────────────────────────────────────────────
            items(items, key = { it.question }) { item ->
                GuideCard(item)
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

// ── Guide Card (expandable accordion) ────────────────────────────────────────

@Composable
private fun GuideCard(item: EmergencyGuideItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .animateContentSize(tween(220))
            .semantics { contentDescription = "Guide: ${item.question}" },
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface)
    ) {
        // Header row (always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji bubble
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.emoji, fontSize = 20.sp)
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text       = item.question,
                modifier   = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp,
                color      = NagarSetuColors.TextPrimary
            )

            Icon(
                imageVector        = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint               = NagarSetuColors.TextSecondary
            )
        }

        // Expandable answer
        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically(tween(220)),
            exit    = shrinkVertically(tween(180))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NagarSetuColors.Background.copy(alpha = 0.45f))
                    .padding(start = 70.dp, end = 16.dp, bottom = 14.dp)
            ) {
                HorizontalDivider(color = NagarSetuColors.SurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text(
                    text  = item.answer,
                    color = NagarSetuColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 19.sp
                )
            }
        }
    }
}
