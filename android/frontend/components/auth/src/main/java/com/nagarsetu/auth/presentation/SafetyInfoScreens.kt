package com.nagarsetu.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.auth.domain.model.MedicalInfo
import com.nagarsetu.auth.domain.model.TrustedContact
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.core.ui.strings.LocalAppStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustedContactsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val contacts by viewModel.trustedContacts.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val s = LocalAppStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.profile.trustedContacts, color = NagarSetuColors.TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = NagarSetuColors.Accent) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NagarSetuColors.Background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { if (contacts.size < 5) showDialog = true }, containerColor = NagarSetuColors.Accent) {
                Icon(Icons.Default.Add, null, tint = NagarSetuColors.Background)
            }
        },
        containerColor = NagarSetuColors.Background
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            item { Text(s.profile.trustedContactsSub, color = NagarSetuColors.TextSecondary, fontSize = 14.sp) }
            item { Spacer(Modifier.height(24.dp)) }
            itemsIndexed(contacts) { index, contact ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface),
                    border = BorderStroke(1.dp, NagarSetuColors.SurfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(contact.name, fontWeight = FontWeight.Bold, color = NagarSetuColors.TextPrimary)
                            Text(contact.phone, color = NagarSetuColors.TextSecondary, fontSize = 13.sp)
                            Text(contact.relation, color = NagarSetuColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        IconButton(onClick = { 
                            val newList = contacts.toMutableList().apply { removeAt(index) }
                            viewModel.saveTrustedContacts(newList)
                        }) {
                            Icon(Icons.Default.Delete, null, tint = NagarSetuColors.SOSRed)
                        }
                    }
                }
            }
        }

        if (showDialog) {
            var name by remember { mutableStateOf("") }
            var phone by remember { mutableStateOf("") }
            var rel by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(s.profile.addContactTitle, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(s.profile.nameLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(s.profile.phoneLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = rel,
                            onValueChange = { rel = it },
                            label = { Text(s.profile.relationLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                val newList = contacts + TrustedContact(name, phone, rel)
                                viewModel.saveTrustedContacts(newList)
                                showDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent)
                    ) {
                        Text(s.common.addBtn, color = NagarSetuColors.Background)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text(s.common.cancelBtn)
                    }
                }
            )
        }
    }
}

@Composable
private fun ContactEditCard(
    index: Int,
    name: String,
    phone: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Contact #$index", color = NagarSetuColors.Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Name", color = NagarSetuColors.TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NagarSetuColors.Accent,
                    unfocusedBorderColor = NagarSetuColors.SurfaceVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Phone Number", color = NagarSetuColors.TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NagarSetuColors.Accent,
                    unfocusedBorderColor = NagarSetuColors.SurfaceVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalInfoScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val info by viewModel.medicalInfo.collectAsState()
    val s = LocalAppStrings.current
    var bloodGroup by remember(info) { mutableStateOf(info.bloodGroup) }
    var allergies by remember(info) { mutableStateOf(info.allergies) }
    var meds by remember(info) { mutableStateOf(info.medications) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.profile.medicalInfoTitle, color = NagarSetuColors.TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = NagarSetuColors.Accent) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NagarSetuColors.Background)
            )
        },
        containerColor = NagarSetuColors.Background
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            OutlinedTextField(
                value = bloodGroup, onValueChange = { bloodGroup = it },
                label = { Text(s.profile.bloodGroup) }, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NagarSetuColors.TextPrimary, 
                    unfocusedTextColor = NagarSetuColors.TextPrimary,
                    focusedContainerColor = NagarSetuColors.Surface,
                    unfocusedContainerColor = NagarSetuColors.Surface
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = allergies, onValueChange = { allergies = it },
                label = { Text(s.profile.allergies) }, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NagarSetuColors.TextPrimary, 
                    unfocusedTextColor = NagarSetuColors.TextPrimary,
                    focusedContainerColor = NagarSetuColors.Surface,
                    unfocusedContainerColor = NagarSetuColors.Surface
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = meds, onValueChange = { meds = it },
                label = { Text(s.profile.currentMedications) }, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NagarSetuColors.TextPrimary, 
                    unfocusedTextColor = NagarSetuColors.TextPrimary,
                    focusedContainerColor = NagarSetuColors.Surface,
                    unfocusedContainerColor = NagarSetuColors.Surface
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { 
                    viewModel.saveMedicalInfo(info.copy(bloodGroup = bloodGroup, allergies = allergies, medications = meds))
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent)
            ) {
                Text(s.profile.saveMedicalInfo, color = NagarSetuColors.Background, fontWeight = FontWeight.Bold)
            }
        }
    }
}
