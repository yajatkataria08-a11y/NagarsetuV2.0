package com.nagarsetu.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorityScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val wards = viewModel.allWards()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ward Authorities", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NagarSetuColors.Background)
            )
        },
        containerColor = NagarSetuColors.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    "Contact your ward officers for civic issues and local assistance.",
                    color = NagarSetuColors.TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            items(wards) { ward ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(NagarSetuColors.Accent.copy(0.15f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, null, tint = NagarSetuColors.Accent)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(ward.wardName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Text("Officer: ${ward.authorityName}", color = NagarSetuColors.TextSecondary, fontSize = 13.sp)
                                Text(ward.zone, color = NagarSetuColors.Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        
                        IconButton(
                            onClick = { /* Call officer phone */ },
                            modifier = Modifier.background(NagarSetuColors.Accent.copy(0.1f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Call, "Call", tint = NagarSetuColors.Accent)
                        }
                    }
                }
            }
        }
    }
}
