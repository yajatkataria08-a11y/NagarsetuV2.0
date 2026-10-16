package com.nagarsetu.drivelegal.presentation.ocr

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun VehicleOcrScreen(
    onBack: () -> Unit,
    viewModel: VehicleOcrViewModel = hiltViewModel()
) {
    val result by viewModel.ocrResult.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("ML Kit — Number Plate OCR", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.mockScan() }, Modifier.fillMaxWidth()) {
            Text("Simulate Camera Scan")
        }
        Spacer(Modifier.height(16.dp))
        result?.let { Text("Detected: $it", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, Modifier.fillMaxWidth()) { Text("Back to Chat") }
    }
}
