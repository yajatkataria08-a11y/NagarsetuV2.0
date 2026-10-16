package com.nagarsetu.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nagarsetu.core.ui.theme.AlertRed

@Composable
fun ErrorCard(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Error", style = MaterialTheme.typography.titleMedium, color = AlertRed)
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
