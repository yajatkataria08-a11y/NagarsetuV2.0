/**
 * MainActivity.kt — Ultimate Merged Version
 */
package com.nagarsetu.main

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.nagarsetu.assistant.presentation.AssistantScreen
import com.nagarsetu.auth.domain.model.OtpState
import com.nagarsetu.auth.presentation.LoginScreen
import com.nagarsetu.auth.presentation.LoginViewModel
import com.nagarsetu.auth.presentation.SettingsViewModel
import com.nagarsetu.auth.presentation.AboutAppScreen
import com.nagarsetu.auth.presentation.AlertSettingsScreen
import com.nagarsetu.auth.presentation.HelpFaqScreen
import com.nagarsetu.auth.presentation.LanguageSettingsScreen
import com.nagarsetu.auth.presentation.MedicalInfoScreen
import com.nagarsetu.auth.presentation.PersonalInfoScreen
import com.nagarsetu.auth.presentation.PrivacyPolicyScreen
import com.nagarsetu.auth.presentation.ProfileScreen
import com.nagarsetu.auth.presentation.TrustedContactsScreen
import com.nagarsetu.chargeup.presentation.ChargeUpScreen
import com.nagarsetu.core.ui.components.ServiceScreen
import com.nagarsetu.core.ui.components.ServicesMenuScreen
import com.nagarsetu.core.ui.strings.EnglishStrings
import com.nagarsetu.core.ui.strings.HindiStrings
import com.nagarsetu.core.ui.strings.LocalAppStrings
import com.nagarsetu.core.ui.map.InAppRouteMapScreen
import com.nagarsetu.core.ui.map.RouteDestinationType
import com.nagarsetu.core.ui.map.RouteMapArgs
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.core.ui.theme.NagarSetuTheme
import com.nagarsetu.core.utils.LocationProvider
import com.nagarsetu.dashboard.presentation.DashboardScreen
import com.nagarsetu.drivelegal.presentation.DriveLegalScreen
import com.nagarsetu.greenroute.presentation.GreenRouteScreen
import com.nagarsetu.healthwatch.presentation.HealthWatchScreen
import com.nagarsetu.core.data.network.ApiClient
import com.nagarsetu.core.data.network.Content
import com.nagarsetu.core.data.network.EmailUser
import com.nagarsetu.core.data.network.Personalization
import com.nagarsetu.core.data.network.SendGridRequest
import com.nagarsetu.parkease.presentation.ParkEaseScreen
import com.nagarsetu.predictive.presentation.PredictiveScreen
import com.nagarsetu.raksha.presentation.RakshaScreen
import com.nagarsetu.raksha.presentation.RakshaViewModel
import com.nagarsetu.raksha.presentation.SosTriggerSource
import com.nagarsetu.raksha.presentation.livetrack.LiveTrackScreen
import com.nagarsetu.raksha.presentation.emergencyguide.EmergencyGuideScreen
import com.nagarsetu.raksha.presentation.settings.RakshaSettingsScreen
import com.nagarsetu.reportit.presentation.ReportItScreen
import com.nagarsetu.roadwatch.presentation.RoadWatchScreen
import com.nagarsetu.utils.Secrets
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Credentials
import javax.inject.Inject


sealed class BottomNav(
    val route:  String,
    val label:  String,
    val icon:   ImageVector,
    val cdesc:  String
) {
    object Home     : BottomNav("dashboard", "Home",     Icons.Filled.Home,                   "Home dashboard")
    object Map      : BottomNav("city_map",  "Map",      Icons.Filled.Map,                    "City map and tracking")
    object Raksha   : BottomNav("raksha",    "Raksha",   Icons.Filled.Shield,                 "Raksha personal safety")
    object Reports  : BottomNav("report_it", "Reports",  Icons.AutoMirrored.Filled.Article,   "Report civic issues")
    object Profile  : BottomNav("profile",   "Profile",  Icons.Filled.Person,                 "Your profile")
}

private val NAV_ITEMS = listOf(
    BottomNav.Home,
    BottomNav.Map,
    BottomNav.Raksha,
    BottomNav.Reports,
    BottomNav.Profile
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var apiClient: ApiClient

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            var showSplash by remember { mutableStateOf(true) }
            val selectedLang by settingsVm.selectedLanguage.collectAsState()
            val appStrings = if (selectedLang == "hi") HindiStrings else EnglishStrings

            LaunchedEffect(Unit) {
                delay(2_000)
                showSplash = false
            }

            CompositionLocalProvider(LocalAppStrings provides appStrings) {
                NagarSetuTheme {
                    Crossfade(targetState = showSplash, animationSpec = tween(400)) { isSplashing ->
                        if (isSplashing) SplashScreen()
                        else             AppShell()
                    }
                }
            }
        }
    }

    private fun sendReportEmail() {
        lifecycleScope.launch {
            try {
                if (Secrets.sendGridApiKey.startsWith("YOUR_")) {
                    Toast.makeText(this@MainActivity, "SendGrid key not configured", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val req = SendGridRequest(
                    personalizations = listOf(Personalization(to = listOf(EmailUser("admin@nagarsetu.com")))),
                    from    = EmailUser("app@nagarsetu.com", "NagarSetu App"),
                    subject = "Civic Issue Reported via NagarSetu",
                    content = listOf(Content("text/plain", "A civic issue has been reported by a user."))
                )
                val response = apiClient.sendGridService.sendEmail(
                    authHeader = "Bearer ${Secrets.sendGridApiKey}",
                    request    = req
                )
                if (response.isSuccessful)
                    Toast.makeText(this@MainActivity, "Report email sent!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Email error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun AppShell() {
        val loginVm: LoginViewModel = hiltViewModel()
        val rakshaVm: RakshaViewModel = hiltViewModel()
        val s = LocalAppStrings.current
        val snackbarHostState = remember { SnackbarHostState() }
        val context = androidx.compose.ui.platform.LocalContext.current

        LaunchedEffect(Unit) {
            rakshaVm.sosEvent.collect { msg ->
                snackbarHostState.showSnackbar(msg)
            }
        }

        val permissions = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        )
        LaunchedEffect(Unit) {
            if (!permissions.allPermissionsGranted) {
                permissions.launchMultiplePermissionRequest()
            }
        }

        val locationProvider: LocationProvider = remember(context) {
            dagger.hilt.android.EntryPointAccessors.fromApplication(
                context.applicationContext,
                LocationProviderEntryPoint::class.java
            ).locationProvider()
        }

        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination?.route ?: "dashboard"
        val hideBottomBarRoutes = setOf("assistant", "livetrack", "emergency_guide", "raksha_settings")

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = NagarSetuColors.Background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (currentRoute !in hideBottomBarRoutes) {
                        val isSosActive by rakshaVm.sosTriggered.collectAsState()
                        val sosSource by rakshaVm.sosTriggerSource.collectAsState()

                        NagarSetuBottomBar(
                            currentRoute  = currentRoute,
                            isSosActive   = isSosActive,
                            sosSource     = sosSource,
                            onNavSelected = { item ->
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            onSosTriggered = {
                                rakshaVm.triggerSos(SosTriggerSource.MANUAL_TAP)
                                if (!permissions.allPermissionsGranted) {
                                    permissions.launchMultiplePermissionRequest()
                                }
                                navController.navigate("raksha") {
                                    launchSingleTop = true
                                }
                            },
                            onSosCancel = {
                                rakshaVm.clearSos()
                            }
                        )
                    }
                }
            ) { padding ->
                NavHost(
                    navController    = navController,
                    startDestination = "dashboard",
                    modifier         = Modifier.padding(padding)
                ) {
                    composable("dashboard") {
                        val user by loginVm.currentUserFlow.collectAsState()
                        DashboardScreen(
                            userName        = user?.name,
                            onEmergencyFab  = {
                                rakshaVm.triggerSos(SosTriggerSource.MANUAL_TAP)
                                navController.navigate("raksha") {
                                    launchSingleTop = true
                                }
                            },
                            onNavigate      = { route -> navController.navigate(route) },
                            onReportIssue   = {
                                sendReportEmail()
                                navController.navigate("report_it")
                            },
                            onOpenAssistant = { navController.navigate("assistant") },
                            locationProvider = locationProvider
                        )
                    }
                    composable("raksha") {
                        RakshaScreen(
                            onNavigateToLiveTrack       = { navController.navigate("livetrack") },
                            onNavigateToEmergencyGuide  = { navController.navigate("emergency_guide") },
                            onNavigateToSettings        = { navController.navigate("raksha_settings") },
                            viewModel = rakshaVm,
                            permissionsState = permissions
                        )
                    }
                    composable("services") {
                        ServicesMenuScreen(onServiceSelected = { screen ->
                            val route = when (screen) {
                                ServiceScreen.PARK_EASE    -> "park_ease"
                                ServiceScreen.CHARGE_UP    -> "charge_up"
                                ServiceScreen.GREEN_ROUTE  -> "green_route"
                                ServiceScreen.DRIVE_LEGAL  -> "drive_legal"
                                ServiceScreen.HEALTH_WATCH -> "health_watch"
                                ServiceScreen.ROAD_WATCH   -> "road_watch"
                                ServiceScreen.ASSISTANT    -> "assistant"
                                ServiceScreen.PREDICTIVE   -> "predictive"
                                ServiceScreen.RAKSHA       -> "raksha"
                                else                       -> "dashboard"
                            }
                            navController.navigate(route)
                        })
                    }
                    composable("report_it") {
                        ReportItScreen(onReportSubmitted = { sendReportEmail() })
                    }
                    composable("city_map") {
                        com.nagarsetu.dashboard.presentation.CityMapScreen(
                            locationProvider = locationProvider,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("profile") {
                        val state by loginVm.otpState.collectAsState()
                        if (state is OtpState.Verified) {
                            ProfileScreen(
                                viewModel = loginVm,
                                onNavigate = { route -> navController.navigate(route) }
                            )
                        } else {
                            LoginScreen(
                                onLoginSuccess = { /* state already observed */ },
                                viewModel      = loginVm
                            )
                        }
                    }
                    composable("profile_personal") { PersonalInfoScreen(onBack = { navController.popBackStack() }) }
                    composable("profile_contacts") { TrustedContactsScreen(onBack = { navController.popBackStack() }) }
                    composable("profile_medical")  { MedicalInfoScreen(onBack = { navController.popBackStack() }) }
                    composable("profile_alerts")   { AlertSettingsScreen(onBack = { navController.popBackStack() }) }
                    composable("profile_language") { LanguageSettingsScreen(onBack = { navController.popBackStack() }) }
                    composable("profile_help")     { HelpFaqScreen(onBack = { navController.popBackStack() }) }
                    composable("profile_privacy")  { PrivacyPolicyScreen(onBack = { navController.popBackStack() }) }
                    composable("profile_about")    { AboutAppScreen(onBack = { navController.popBackStack() }) }
                    composable("assistant") {
                        AssistantScreen(
                            onBack     = { navController.popBackStack() },
                            onNavigate = { route ->
                                navController.popBackStack()
                                navController.navigate(route)
                            }
                        )
                    }
                    composable("livetrack") { LiveTrackScreen(onBack = { navController.popBackStack() }) }
                    composable("emergency_guide") { EmergencyGuideScreen(onBack = { navController.popBackStack() }) }
                    composable("raksha_settings") { RakshaSettingsScreen(onBack = { navController.popBackStack() }) }
                    composable("authority") {
                        com.nagarsetu.dashboard.presentation.AuthorityScreen(onBack = { navController.popBackStack() })
                    }
                    composable("predictive")   { PredictiveScreen(locationProvider = locationProvider) }
                    composable("drive_legal")  { DriveLegalScreen() }
                    composable("road_watch")   { RoadWatchScreen() }
                    composable("park_ease")    { 
                        ParkEaseScreen(
                            locationProvider = locationProvider,
                            onNavigate = { route -> navController.navigate(route) }
                        )
                    }
                    composable("green_route")  { GreenRouteScreen() }
                    composable("charge_up")    { ChargeUpScreen(onNavigate = { route -> navController.navigate(route) }) }
                    composable("health_watch") { HealthWatchScreen(onBack = { navController.popBackStack() }) }

                    composable(
                        route = "route_map/{destLat}/{destLng}/{destName}/{destSubtitle}/{iconType}",
                        arguments = listOf(
                            navArgument("destLat") { type = NavType.StringType },
                            navArgument("destLng") { type = NavType.StringType },
                            navArgument("destName") { type = NavType.StringType },
                            navArgument("destSubtitle") { type = NavType.StringType },
                            navArgument("iconType") { type = NavType.StringType; defaultValue = "GENERAL" }
                        )
                    ) { backStack ->
                        val destLat = backStack.arguments?.getString("destLat")?.toDoubleOrNull() ?: 0.0
                        val destLng = backStack.arguments?.getString("destLng")?.toDoubleOrNull() ?: 0.0
                        val destName = backStack.arguments?.getString("destName") ?: ""
                        val destSubtitle = backStack.arguments?.getString("destSubtitle") ?: ""
                        val iconTypeStr = backStack.arguments?.getString("iconType") ?: "GENERAL"
                        val iconType = try { RouteDestinationType.valueOf(iconTypeStr) } catch(e: Exception) { RouteDestinationType.GENERAL }
                        
                        InAppRouteMapScreen(
                            args = RouteMapArgs(destLat, destLng, destName, destSubtitle, iconType),
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SplashScreen() {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A1A3E), Color(0xFF0D0D2B)))), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocationCity, null, tint = NagarSetuColors.Accent, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text("नगर सेतु", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp)
                Text("NagarSetu · Bhopal", fontSize = 14.sp, color = NagarSetuColors.Accent, letterSpacing = 4.sp)
                Spacer(Modifier.height(40.dp))
                CircularProgressIndicator(color = NagarSetuColors.Accent.copy(alpha = 0.6f), strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun NagarSetuBottomBar(
    currentRoute:  String,
    isSosActive:   Boolean = false,
    sosSource:     SosTriggerSource? = null,
    onNavSelected: (BottomNav) -> Unit,
    onSosTriggered: () -> Unit = {},
    onSosCancel: () -> Unit = {}
) {
    val s = com.nagarsetu.core.ui.strings.LocalAppStrings.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isHolding by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxWidth().wrapContentHeight(), contentAlignment = Alignment.BottomCenter) {
        Surface(color = NagarSetuColors.Surface, tonalElevation = 8.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), modifier = Modifier.fillMaxWidth().height(84.dp)) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                NAV_ITEMS.forEachIndexed { index, item ->
                    if (index == 2) { Box(modifier = Modifier.weight(1f)) } else {
                        val selected = currentRoute == item.route
                        Column(modifier = Modifier.weight(1f).clickable { onNavSelected(item) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(item.icon, item.cdesc, tint = if (selected) NagarSetuColors.Accent else NagarSetuColors.TextSecondary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(item.label, style = MaterialTheme.typography.labelSmall, color = if (selected) NagarSetuColors.Accent else NagarSetuColors.TextSecondary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        Box(modifier = Modifier.offset(y = (-36).dp).size(80.dp).background(NagarSetuColors.Background, CircleShape).padding(4.dp).clip(CircleShape).pointerInput(isSosActive) {
            if (isSosActive) { detectTapGestures(onTap = { onSosCancel() }) } else {
                detectTapGestures(onPress = {
                    isHolding = true; progress = 0f; val startTime = System.currentTimeMillis()
                    val holdJob = scope.launch {
                        while (isHolding && progress < 1f) {
                            delay(30); progress = (System.currentTimeMillis() - startTime) / 3000f
                            if (progress >= 1f) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onSosTriggered(); isHolding = false; progress = 0f
                            }
                        }
                    }
                    try { awaitRelease() } finally { isHolding = false; progress = 0f; holdJob.cancel() }
                }, onTap = { onNavSelected(BottomNav.Raksha) })
            }
        }.background(if (isSosActive || isHolding) Color(0xFFB71C1C) else Color(0xFFE53935)), contentAlignment = Alignment.Center) {
            if (isHolding) { CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), color = Color.White, strokeWidth = 4.dp, trackColor = Color.Transparent) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(if (isSosActive) Icons.Default.Cancel else Icons.Filled.Shield, "SOS", tint = Color.White, modifier = Modifier.size(32.dp))
                Text(if (isHolding) s.common.status.uppercase() else if (isSosActive) s.common.cancelBtn.uppercase() else "SOS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
