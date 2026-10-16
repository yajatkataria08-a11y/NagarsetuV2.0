package com.nagarsetu.predictive.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nagarsetu.core.ui.theme.*
import com.nagarsetu.predictive.domain.model.RAGResult

/**
 * Civic RAG Intelligence query box.
 * Features:
 * - Suggested queries as chip pills
 * - Animated loading state
 * - Result card with confidence meter, sources, incidents & legal refs
 */
@Composable
fun RAGQueryBox(
    ragResult: RAGResult?,
    isLoading: Boolean,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var queryText by remember { mutableStateOf("") }

    val suggestions = listOf(
        "Biggest risks in MP Nagar this week",
        "Flood zones near Upper Lake",
        "Accident hotspots in Bhopal",
        "Ward with most SLA breaches"
    )

    Column(modifier) {
        // ── Input Row ──────────────────────────────────────────────────────
        OutlinedTextField(
            value = queryText,
            onValueChange = { queryText = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Ask about Bhopal's civic risks…") },
            leadingIcon = {
                Icon(Icons.Default.Psychology, null, tint = PrimaryBlue)
            },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp).padding(2.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryBlue
                    )
                } else {
                    IconButton(
                        onClick = { if (queryText.isNotBlank()) onQuery(queryText) },
                        enabled = queryText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, "Search", tint = PrimaryBlue)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(Modifier.height(8.dp))

        // ── Suggestion chips ───────────────────────────────────────────────
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(suggestions.size) { i ->
                SuggestionChip(
                    onClick = {
                        queryText = suggestions[i]
                        onQuery(suggestions[i])
                    },
                    label = {
                        Text(suggestions[i], style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    },
                    icon = { Icon(Icons.Default.TipsAndUpdates, null,
                        modifier = Modifier.size(14.dp), tint = WarnAmber) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Result ─────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = ragResult != null && !isLoading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            ragResult?.let { result ->
                RAGResultCard(result = result, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        // ── Loading state ──────────────────────────────────────────────────
        AnimatedVisibility(visible = isLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(80.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                    Spacer(Modifier.width(12.dp))
                    Text("NagarSetu AI is retrieving civic data…",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun RAGResultCard(result: RAGResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(
                            Brush.linearGradient(listOf(PrimaryBlue, CivicBlueLight)),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SmartToy, null, tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("NagarSetu Intelligence", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                // Confidence badge
                Box(
                    Modifier
                        .background(EmeraldGreen.copy(0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("${(result.confidence * 100).toInt()}% confident",
                        style = MaterialTheme.typography.labelSmall, color = EmeraldGreen,
                        fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Query echo
            Text(
                "\"${result.query}\"",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // Answer
            Text(result.answer, style = MaterialTheme.typography.bodySmall)

            // Related incidents
            if (result.relatedIncidents.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("📍 Related Incidents", fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium)
                result.relatedIncidents.forEach { incident ->
                    Text("• $incident", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }

            // Legal sections
            if (result.legalSections.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("⚖️ Applicable Sections", fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium)
                result.legalSections.forEach { section ->
                    Box(
                        Modifier
                            .padding(vertical = 2.dp)
                            .background(PrimaryBlue.copy(0.08f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(section, style = MaterialTheme.typography.labelSmall, color = PrimaryBlue)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Sources
            Text(
                "Sources: ${result.sources.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(0.6f)
            )
        }
    }
}
