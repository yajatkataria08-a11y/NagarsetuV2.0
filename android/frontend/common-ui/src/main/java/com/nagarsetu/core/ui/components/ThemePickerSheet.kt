package com.nagarsetu.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nagarsetu.core.ui.theme.AppTheme
import com.nagarsetu.core.ui.theme.colorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerSheet(
    current: AppTheme,
    onSelect: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("App theme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Choose a look for NagarSetu", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 360.dp)
            ) {
                items(AppTheme.entries) { theme ->
                    ThemePreviewCard(
                        theme = theme,
                        selected = theme == current,
                        onClick = { onSelect(theme); onDismiss() }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemePreviewCard(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    val scheme = theme.colorScheme()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(2.dp, scheme.primary, RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(scheme.primary))
                Box(Modifier.size(24.dp).clip(CircleShape).background(scheme.secondary))
                Box(Modifier.size(24.dp).clip(CircleShape).background(scheme.background))
            }
            Spacer(Modifier.height(8.dp))
            Text(theme.displayName, fontWeight = FontWeight.Medium)
            if (selected) {
                Text("Active", style = MaterialTheme.typography.labelSmall, color = scheme.primary)
            }
        }
    }
}
