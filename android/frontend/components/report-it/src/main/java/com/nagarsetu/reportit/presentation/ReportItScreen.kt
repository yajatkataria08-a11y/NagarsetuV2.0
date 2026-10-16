package com.nagarsetu.reportit.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.core.ui.components.AppWatermark
import com.nagarsetu.core.ui.components.NagarSetuScreenBackground
import com.nagarsetu.core.ui.map.MapMarker
import com.nagarsetu.core.ui.map.OSMMapView
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.reportit.domain.model.CivicIssueType
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportItScreen(
    viewModel: ReportItViewModel = hiltViewModel(),
    onReportSubmitted: () -> Unit = {}
) {
    val step by viewModel.step.collectAsState()
    val issueType by viewModel.issueType.collectAsState()
    val description by viewModel.description.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val lat by viewModel.selectedLat.collectAsState()
    val lng by viewModel.selectedLng.collectAsState()
    val s = LocalAppStrings.current

    NagarSetuScreenBackground {
        Column(Modifier.fillMaxSize()) {
            // Header with Deep Background
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NagarSetuColors.Primary,
                contentColor = NagarSetuColors.LightText,
                shadowElevation = 4.dp
            ) {
                Row(
                    Modifier.statusBarsPadding().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step !is ReportItStep.Capture) {
                        IconButton(onClick = { viewModel.reset() }, enabled = !isSubmitting) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = NagarSetuColors.LightText)
                        }
                    }
                    Text(
                        text = s.nav.navReports,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (step !is ReportItStep.Success) {
                // Stepper
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepIcon(1, s.roadWatch.stepType, step is ReportItStep.Capture)
                    Spacer(Modifier.weight(1f).height(2.dp).background(NagarSetuColors.SurfaceVariant))
                    StepIcon(2, s.roadWatch.stepPhoto, step is ReportItStep.Describe)
                    Spacer(Modifier.weight(1f).height(2.dp).background(NagarSetuColors.SurfaceVariant))
                    StepIcon(3, s.roadWatch.location, step is ReportItStep.Location)
                }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "step_content",
                modifier = Modifier.weight(1f)
            ) { currentStep ->
                when (currentStep) {
                    is ReportItStep.Capture -> CategoriesDashboardStep(
                        onTypeSelect = { type ->
                            viewModel.setType(type)
                            viewModel.toDescribe() 
                        }
                    )
                    is ReportItStep.Describe -> DescribeStep(
                        selectedType = issueType,
                        description = description,
                        onTypeSelect = viewModel::setType,
                        onDescChange = viewModel::setDescription,
                        onNext = viewModel::toLocation,
                        viewModel = viewModel
                    )
                    is ReportItStep.Location -> LocationStep(
                        lat = lat,
                        lng = lng,
                        ward = viewModel.wardPreview,
                        isSubmitting = isSubmitting,
                        viewModel = viewModel,
                        onSubmit = {
                            viewModel.submit()
                        }
                    )
                    is ReportItStep.Success -> SuccessStep(onBack = { viewModel.reset() })
                }
            }
        }
    }

    if (step is ReportItStep.Success) {
        LaunchedEffect(Unit) {
            onReportSubmitted()
        }
    }
}

@Composable
private fun CategoriesDashboardStep(onTypeSelect: (CivicIssueType) -> Unit) {
    val s = LocalAppStrings.current
    Column(
        Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            s.roadWatch.reportHeading,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val categories = listOf(
            Triple(CivicIssueType.POTHOLE, s.dashboard.potholes, Icons.Filled.Report),
            Triple(CivicIssueType.GARBAGE, s.dashboard.garbage, Icons.Filled.Delete),
            Triple(CivicIssueType.STREETLIGHT, s.dashboard.streetlights, Icons.Filled.Lightbulb),
            Triple(CivicIssueType.DRAIN, s.roadWatch.drainage, Icons.Filled.Waves),
            Triple(CivicIssueType.ENCROACHMENT, s.roadWatch.encroachment, Icons.Filled.Fence),
            Triple(CivicIssueType.OTHER, s.roadWatch.otherIssue, Icons.Filled.MoreHoriz)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(categories.chunked(2)) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { (type, label, icon) ->
                        CategoryCard(
                            label = label,
                            icon = icon,
                            color = when(type) {
                                CivicIssueType.POTHOLE -> NagarSetuColors.WarningOrange
                                CivicIssueType.GARBAGE -> NagarSetuColors.SuccessGreen
                                CivicIssueType.STREETLIGHT -> Color(0xFFFFD54F)
                                else -> NagarSetuColors.Accent
                            },
                            modifier = Modifier.weight(1f),
                            onClick = { onTypeSelect(type) }
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // Professional Branding Watermark
        AppWatermark(
            alignment = Alignment.BottomEnd,
            opacity = 0.12f,
            modifier = Modifier.padding(bottom = 16.dp, end = 16.dp)
        )
    }
}

@Composable
private fun CategoryCard(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun StepIcon(index: Int, label: String, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(32.dp).clip(CircleShape)
                .background(if (isActive) NagarSetuColors.Accent else NagarSetuColors.SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(index.toString(), color = if (isActive) Color.White else NagarSetuColors.TextSecondary, fontWeight = FontWeight.Bold)
        }
        Text(label, fontSize = 10.sp, color = if (isActive) NagarSetuColors.Accent else NagarSetuColors.TextSecondary, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun CaptureStep(onCapture: () -> Unit) {
    val s = LocalAppStrings.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp))
                .background(NagarSetuColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CameraAlt, null, tint = NagarSetuColors.TextSecondary, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onCapture,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent)
        ) {
            Text(s.roadWatch.captureEvidence, color = NagarSetuColors.Background, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DescribeStep(
    selectedType: CivicIssueType,
    description: String,
    onTypeSelect: (CivicIssueType) -> Unit,
    onDescChange: (String) -> Unit,
    onNext: () -> Unit,
    viewModel: ReportItViewModel
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            capturedBitmap = bitmap
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(s.roadWatch.captureEvidence, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NagarSetuColors.Surface)
                .clickable { cameraLauncher.launch() },
            contentAlignment = Alignment.Center
        ) {
            if (capturedBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, tint = NagarSetuColors.Accent, modifier = Modifier.size(48.dp))
                    Text(s.roadWatch.captureEvidence, color = LocalContentColor.current.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }
        
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { cameraLauncher.launch() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, null)
                Spacer(Modifier.width(8.dp))
                Text("Camera")
            }
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, null)
                Spacer(Modifier.width(8.dp))
                Text("Gallery")
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(s.roadWatch.selectCategory, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(CivicIssueType.entries) { type ->
                val isSelected = type == selectedType
                
                val typeLabel = when(type) {
                    CivicIssueType.POTHOLE -> s.dashboard.potholes
                    CivicIssueType.GARBAGE -> s.dashboard.garbage
                    CivicIssueType.STREETLIGHT -> s.dashboard.streetlights
                    CivicIssueType.DRAIN -> s.roadWatch.drainage
                    CivicIssueType.ENCROACHMENT -> s.roadWatch.encroachment
                    CivicIssueType.OTHER -> s.roadWatch.otherIssue
                    else -> type.name
                }

                Box(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) NagarSetuColors.Accent.copy(0.12f) else NagarSetuColors.SurfaceVariant)
                        .border(1.dp, if (isSelected) NagarSetuColors.Accent else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { onTypeSelect(type) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = typeLabel, 
                        color = if (isSelected) NagarSetuColors.Accent else NagarSetuColors.TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(s.roadWatch.description, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescChange,
            modifier = Modifier.fillMaxWidth().height(150.dp),
            placeholder = { Text("${s.roadWatch.describeIssue}...", color = NagarSetuColors.TextSecondary) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NagarSetuColors.Accent,
                unfocusedBorderColor = NagarSetuColors.SurfaceVariant,
                focusedTextColor = NagarSetuColors.TextPrimary,
                unfocusedTextColor = NagarSetuColors.TextPrimary,
                focusedContainerColor = NagarSetuColors.Surface,
                unfocusedContainerColor = NagarSetuColors.Surface
            )
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent),
            enabled = description.isNotBlank()
        ) {
            Text(s.common.nextBtn, color = NagarSetuColors.Background, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LocationStep(
    lat: Double,
    lng: Double,
    ward: String,
    isSubmitting: Boolean,
    viewModel: ReportItViewModel,
    onSubmit: () -> Unit
) {
    val s = LocalAppStrings.current
    val useLiveLocation by viewModel.useLiveLocation.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(s.roadWatch.pinLocation, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))

        // Location Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(NagarSetuColors.Surface, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LocationModeButton(
                text = "Live Location",
                isSelected = useLiveLocation,
                onClick = { viewModel.setUseLiveLocation(true) },
                modifier = Modifier.weight(1f)
            )
            LocationModeButton(
                text = "Select on Map",
                isSelected = !useLiveLocation,
                onClick = { viewModel.setUseLiveLocation(false) },
                modifier = Modifier.weight(1f)
            )
        }
        
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(NagarSetuColors.Surface)
        ) {
            OSMMapView(
                modifier = Modifier.fillMaxSize(),
                markers = listOf(MapMarker(lat, lng, s.roadWatch.issueLocation, s.nav.navReports)),
                enableGps = useLiveLocation,
                locationProvider = viewModel.locationProvider,
                onMapClick = { newLat, newLng ->
                    viewModel.setLocation(newLat, newLng)
                }
            )
            
            if (!useLiveLocation) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .background(Color.Black.copy(0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Tap on map to move pin", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        
        // Removed Nearest Authority Card as per requirement
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NagarSetuColors.Surface),
            border = BorderStroke(1.dp, NagarSetuColors.SurfaceVariant)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MyLocation, null, tint = NagarSetuColors.Accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = String.format("%.4f, %.4f", lat, lng),
                    color = NagarSetuColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = NagarSetuColors.Background, modifier = Modifier.size(24.dp))
            } else {
                Text(s.common.submitBtn, color = NagarSetuColors.Background, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LocationModeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) NagarSetuColors.Accent else Color.Transparent,
        contentColor = if (isSelected) Color.White else NagarSetuColors.TextSecondary
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun SuccessStep(onBack: () -> Unit) {
    val s = LocalAppStrings.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = NagarSetuColors.SuccessGreen, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(16.dp))
        Text(s.roadWatch.submittedSuccessfully, style = MaterialTheme.typography.headlineSmall, color = NagarSetuColors.TextPrimary, fontWeight = FontWeight.Bold)
        Text(s.roadWatch.municipalNotified, color = NagarSetuColors.TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onBack, 
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NagarSetuColors.Accent)
        ) {
            Text(s.common.backBtn, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
