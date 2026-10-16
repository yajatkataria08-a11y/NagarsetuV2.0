package com.nagarsetu.roadwatch.presentation.report

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagarsetu.core.ui.components.*
import com.nagarsetu.core.ui.strings.AppStrings
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.*
import com.nagarsetu.roadwatch.domain.model.*
import com.nagarsetu.roadwatch.presentation.RoadWatchViewModel
import com.nagarsetu.roadwatch.presentation.components.*
import kotlinx.coroutines.launch

@Composable
fun ReportTab(viewModel: RoadWatchViewModel) {
    val wizard by viewModel.wizard.collectAsState()
    val s = LocalAppStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        if (wizard.submittedReport != null) {
            val report = wizard.submittedReport!!
            val sla = viewModel.slaFor(report)
            val authority = viewModel.authorityFor(report)
            val contractor = viewModel.contractorFor(report)

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SubmissionSuccessCard(
                    report = report,
                    sla = sla,
                    onTrack = { viewModel.setTab(1) },
                    onNewReport = { viewModel.resetWizard() }
                )
                RichReportCard(
                    report = report,
                    sla = sla,
                    authority = authority,
                    contractor = contractor,
                    onEscalate = { viewModel.escalateReport(report.id) },
                    onShare = {}
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                WizardStepIndicator(s, currentStep = wizard.step)
                Spacer(Modifier.height(8.dp))

                AnimatedContent(
                    targetState = wizard.step,
                    transitionSpec = {
                        if (targetState > initialState)
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        else
                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    },
                    label = "wizard_step"
                ) { step ->
                    when (step) {
                        0 -> Step1TypeSelection(s, viewModel, snackbarHostState)
                        1 -> Step2PhotoAndLocation(s, viewModel, wizard, snackbarHostState)
                        2 -> Step3SeverityAndDetails(s, viewModel, wizard, snackbarHostState)
                        3 -> Step4Confirm(s, viewModel, wizard)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        )
    }
}

@Composable
fun WizardStepIndicator(s: AppStrings, currentStep: Int) {
    val steps = listOf(s.roadWatch.stepType, s.roadWatch.stepPhoto, s.roadWatch.stepDetails, s.roadWatch.stepConfirm)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val isDone = index < currentStep
            val isActive = index == currentStep
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        isDone -> EmeraldGreen
                        isActive -> PrimaryBlue
                        else -> DividerSoft
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isDone) Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = Color.White)
                        else Text("${index + 1}", color = if (isActive) Color.White else TextSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) NagarSetuColors.Accent else if (isDone) EmeraldGreen else NagarSetuColors.TextSecondary,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (index < steps.lastIndex) {
                Box(Modifier.weight(1f).height(2.dp).padding(bottom = 18.dp)) {
                    HorizontalDivider(color = if (isDone) EmeraldGreen else DividerSoft, thickness = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun Step1TypeSelection(s: AppStrings, viewModel: RoadWatchViewModel, snackbarHostState: SnackbarHostState) {
    val wizard by viewModel.wizard.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(s.roadWatch.reportHeading)

        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(ReportType.entries) { type ->
                ReportTypeTile(type = type, selected = wizard.type == type, onClick = { viewModel.selectType(type) })
            }
        }

        Card(
            onClick = { 
                viewModel.selectType(ReportType.OPEN_MANHOLE)
                viewModel.setSeverity(Severity.CRITICAL)
                scope.launch { snackbarHostState.showSnackbar("Emergency mode: Open Manhole selected.") }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NagarSetuColors.SOSRed.copy(0.1f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, NagarSetuColors.SOSRed.copy(0.3f))
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = NagarSetuColors.SOSRed, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(s.dashboard.tapForHelp, fontWeight = FontWeight.ExtraBold, color = NagarSetuColors.SOSRed, fontSize = 15.sp)
                    Text("Report life-threatening hazards immediately", color = NagarSetuColors.SOSRed.copy(0.8f), fontSize = 12.sp)
                }
            }
        }

        wizard.type?.let { selectedType ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Accent.copy(0.06f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, NagarSetuColors.Accent.copy(0.12f))
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("${selectedType.emoji} ${selectedType.label}", fontWeight = FontWeight.Bold, color = NagarSetuColors.TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(descSuggest(selectedType), style = MaterialTheme.typography.bodySmall, color = NagarSetuColors.TextSecondary)
                }
            }

            Button(
                onClick = { viewModel.setStep(1) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("${s.common.nextBtn}: ${s.roadWatch.addPhotoLocHeading}", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun Step2PhotoAndLocation(s: AppStrings, viewModel: RoadWatchViewModel, wizard: com.nagarsetu.roadwatch.presentation.ReportWizardState, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) viewModel.onPhotoCaptured(bitmap)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) viewModel.onPhotoCaptured(bitmap)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(s.roadWatch.addPhotoLocHeading)

        Card(
            Modifier.fillMaxWidth().height(200.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NagarSetuColors.SurfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, NagarSetuColors.TextDisabled.copy(0.3f))
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (wizard.isDetecting) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NagarSetuColors.Accent)
                        Spacer(Modifier.height(12.dp))
                        Text(s.roadWatch.aiAnalysing, color = NagarSetuColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                } else if (wizard.photo != null) {
                    Image(bitmap = wizard.photo.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.3f)).padding(12.dp), contentAlignment = Alignment.BottomEnd) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color.White, CircleShape).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ready", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, null, tint = NagarSetuColors.TextDisabled, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Evidence photo required", color = NagarSetuColors.TextSecondary, fontSize = 14.sp)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { cameraLauncher.launch() },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NagarSetuColors.Accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp), tint = NagarSetuColors.Accent)
                Spacer(Modifier.width(8.dp))
                Text(s.common.camera, color = NagarSetuColors.Accent, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NagarSetuColors.Accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp), tint = NagarSetuColors.Accent)
                Spacer(Modifier.width(8.dp))
                Text(s.common.gallery, color = NagarSetuColors.Accent, fontWeight = FontWeight.Bold)
            }
        }

        wizard.detectionResult?.let { det ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(0.08f)), border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(0.2f))) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = EmeraldGreen, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("AI Verified: ${det.suggestedType.label}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NagarSetuColors.TextPrimary)
                        Text("${det.criticality.label} criticality • Confidence ${(det.confidence * 100).toInt()}%", fontSize = 12.sp, color = NagarSetuColors.TextSecondary)
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Accent.copy(0.06f)), border = androidx.compose.foundation.BorderStroke(1.dp, NagarSetuColors.Accent.copy(0.15f))) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MyLocation, null, tint = NagarSetuColors.Accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.roadWatch.location, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NagarSetuColors.TextPrimary)
                    Text("${wizard.latitude.form(4)}, ${wizard.longitude.form(4)}", style = MaterialTheme.typography.labelSmall, color = NagarSetuColors.TextSecondary)
                }
                TextButton(onClick = { viewModel.resetWizard(); scope.launch { snackbarHostState.showSnackbar("Location updated") } }) {
                    Text(s.roadWatch.useGps, fontWeight = FontWeight.Bold)
                }
            }
        }

        Button(
            onClick = { if (wizard.photo != null) viewModel.setStep(2) else scope.launch { snackbarHostState.showSnackbar("Please upload evidence photo") } },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("${s.common.nextBtn}: ${s.roadWatch.stepDetails}", fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp))
        }

        TextButton(onClick = { viewModel.setStep(0) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Default.ArrowBack, null, Modifier.size(16.dp), tint = NagarSetuColors.TextSecondary)
            Spacer(Modifier.width(4.dp))
            Text(s.common.backBtn, color = NagarSetuColors.TextSecondary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(100.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step3SeverityAndDetails(s: AppStrings, viewModel: RoadWatchViewModel, wizard: com.nagarsetu.roadwatch.presentation.ReportWizardState, snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(s.roadWatch.severityHeading)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Priority Level", fontWeight = FontWeight.Bold, color = NagarSetuColors.TextPrimary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Severity.entries.forEach { sev ->
                    val isSelected = wizard.severity == sev
                    Surface(
                        modifier = Modifier.weight(1f).clickable { viewModel.setSeverity(sev) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) sev.color().copy(0.15f) else NagarSetuColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) sev.color() else NagarSetuColors.SurfaceVariant)
                    ) {
                        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(sev.label, fontWeight = FontWeight.Bold, color = if(isSelected) sev.color() else NagarSetuColors.TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = wizard.severity.color().copy(0.05f), border = androidx.compose.foundation.BorderStroke(1.dp, wizard.severity.color().copy(0.2f))) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = wizard.severity.color(), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Service Level: ${wizard.severity.slaDays} days resolution target", fontSize = 12.sp, color = NagarSetuColors.TextPrimary, fontWeight = FontWeight.Medium)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(s.roadWatch.description, fontWeight = FontWeight.Bold, color = NagarSetuColors.TextPrimary)
            OutlinedTextField(
                value = wizard.description, onValueChange = { viewModel.setDescription(it) },
                modifier = Modifier.fillMaxWidth(), placeholder = { Text(s.roadWatch.describeIssue, color = NagarSetuColors.TextDisabled) },
                minLines = 4, maxLines = 6, shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = NagarSetuColors.TextPrimary, unfocusedTextColor = NagarSetuColors.TextPrimary, focusedBorderColor = NagarSetuColors.Accent, unfocusedBorderColor = NagarSetuColors.SurfaceVariant, focusedContainerColor = NagarSetuColors.Surface, unfocusedContainerColor = NagarSetuColors.Surface)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Quick Tags", style = MaterialTheme.typography.labelLarge, color = NagarSetuColors.TextSecondary, fontWeight = FontWeight.Bold)
            androidx.compose.foundation.layout.FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                wizard.type?.let { type ->
                    qDescriptions(type).forEach { suggestion ->
                        SuggestionChip(onClick = { viewModel.setDescription(suggestion) }, label = { Text(suggestion, fontWeight = FontWeight.Medium) }, shape = RoundedCornerShape(8.dp), colors = SuggestionChipDefaults.suggestionChipColors(labelColor = NagarSetuColors.Accent))
                    }
                }
            }
        }

        Button(
            onClick = { if (wizard.description.length >= 10) viewModel.setStep(3) else scope.launch { snackbarHostState.showSnackbar("Please describe the issue (min 10 chars)") } },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(s.roadWatch.reviewSubmit, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp))
        }

        TextButton(onClick = { viewModel.setStep(1) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Default.ArrowBack, null, Modifier.size(16.dp), tint = NagarSetuColors.TextSecondary)
            Spacer(Modifier.width(4.dp))
            Text(s.common.backBtn, color = NagarSetuColors.TextSecondary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun Step4Confirm(s: AppStrings, viewModel: RoadWatchViewModel, wizard: com.nagarsetu.roadwatch.presentation.ReportWizardState) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(s.roadWatch.stepConfirm)

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(NagarSetuColors.Accent.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(wizard.type?.emoji ?: "❓", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Report Type", fontSize = 11.sp, color = NagarSetuColors.TextSecondary)
                        Text(wizard.type?.label ?: "Unknown", fontWeight = FontWeight.ExtraBold, color = NagarSetuColors.TextPrimary)
                    }
                }
                HorizontalDivider(color = NagarSetuColors.SurfaceVariant)
                ConfirmRow(s.roadWatch.severityHeading, wizard.severity.label)
                ConfirmRow("Location", "Bhopal Hub (${wizard.latitude.form(3)}, ${wizard.longitude.form(3)})")
                Column {
                    Text(s.roadWatch.description, fontSize = 11.sp, color = NagarSetuColors.TextSecondary)
                    Text(wizard.description, fontWeight = FontWeight.Medium, color = NagarSetuColors.TextPrimary, lineHeight = 18.sp)
                }
                if (wizard.detectionResult != null) {
                    Surface(color = EmeraldGreen.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AI Verified: ${(wizard.detectionResult.confidence * 100).toInt()}% Confidence", fontSize = 10.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (wizard.isSubmitting) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Accent.copy(0.08f))) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp, color = NagarSetuColors.Accent)
                    Spacer(Modifier.width(16.dp))
                    Text("Securely uploading report...", fontWeight = FontWeight.Bold, color = NagarSetuColors.Accent)
                }
            }
        } else {
            Button(onClick = { viewModel.submitReport() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.CloudUpload, null, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("SUBMIT TO MUNICIPALITY", fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }

        TextButton(onClick = { viewModel.setStep(2) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Default.ArrowBack, null, Modifier.size(16.dp), tint = NagarSetuColors.TextSecondary)
            Spacer(Modifier.width(4.dp))
            Text("Modify Details", color = NagarSetuColors.TextSecondary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun ConfirmRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = NagarSetuColors.TextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NagarSetuColors.TextPrimary)
    }
}

private fun descSuggest(type: ReportType): String = when (type) {
    ReportType.POTHOLE -> "Report a dangerous pothole. AI will estimate depth and severity from your photo."
    ReportType.STREET_LIGHT_DOWN -> "Report a non-functional street light for prompt repair."
    ReportType.WATER_LOGGING -> "Report stagnant water accumulation on the road."
    ReportType.OPEN_MANHOLE -> "CRITICAL: Open manholes pose serious danger. Report immediately."
    ReportType.ROAD_DAMAGE -> "Report damaged road surface, broken asphalt or missing sections."
    ReportType.BROKEN_SIGNAL -> "Report a malfunctioning traffic signal or zebra crossing lights."
    ReportType.ENCROACHMENT -> "Report illegal structures or encroachments on public roads."
    ReportType.GARBAGE_DUMP -> "Report illegal garbage dumping on the roadside."
    ReportType.OTHER -> "Describe any other road or civic issue."
}

private fun qDescriptions(type: ReportType): List<String> = when (type) {
    ReportType.POTHOLE -> listOf("Large pothole causing accidents", "Deep pothole near school", "Dangerous at night")
    ReportType.STREET_LIGHT_DOWN -> listOf("Light not working for 3+ days", "Dark stretch at night", "Safety hazard")
    ReportType.WATER_LOGGING -> listOf("Water logging after rain", "Blocked drain flooding", "Road waterlogged")
    ReportType.OPEN_MANHOLE -> listOf("Open manhole — DANGER", "Cover missing", "Accident risk")
    ReportType.ROAD_DAMAGE -> listOf("Road completely broken", "Asphalt missing", "Damaged after rain")
    ReportType.BROKEN_SIGNAL -> listOf("Signal not working", "Signal stuck on red", "Light broken")
    else -> listOf("Issue reported from app")
}

private fun Double.form(digits: Int) = "%.${digits}f".format(this)
