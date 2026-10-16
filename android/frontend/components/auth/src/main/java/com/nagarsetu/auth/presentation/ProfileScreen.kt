package com.nagarsetu.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagarsetu.core.ui.theme.NagarSetuColors
import com.nagarsetu.core.ui.strings.LocalAppStrings

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val s = LocalAppStrings.current
    val user = viewModel.currentUser()
    val isGuest = user?.isGuest ?: true

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NagarSetuColors.Background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ... (Header stays same) ...
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(NagarSetuColors.SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGuest) Icons.Filled.PersonOutline else Icons.Filled.Person,
                        contentDescription = null,
                        tint = if (isGuest) NagarSetuColors.TextSecondary else NagarSetuColors.Accent,
                        modifier = Modifier.size(50.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (isGuest) s.profile.guestUser
                           else user?.name?.takeIf { it.isNotBlank() } ?: user?.phone ?: s.profile.verifiedUser,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = NagarSetuColors.TextPrimary
                )
                if (!isGuest && user?.name?.isNotBlank() == true && user.phone.isNotBlank()) {
                    Text(
                        text = user.phone,
                        fontSize = 14.sp,
                        color = NagarSetuColors.TextSecondary
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                // MAIN ACTION BUTTON (LOGIN / LOGOUT)
                Button(
                    onClick = { if (isGuest) viewModel.startPhoneLink() else viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGuest) NagarSetuColors.Accent else NagarSetuColors.SOSRed.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = if (isGuest) Icons.Filled.Phone else Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = if (isGuest) Color.White else NagarSetuColors.SOSRed
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (isGuest) s.profile.signInWithPhone else s.profile.logoutSession,
                        color = if (isGuest) Color.White else NagarSetuColors.SOSRed,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (isGuest) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s.profile.signInToSync,
                        fontSize = 12.sp,
                        color = NagarSetuColors.TextSecondary
                    )
                }
            }
        }

        // ── PROFILE SECTIONS ──
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                ProfileSection(s.profile.sectionAccountSettings) {
                    ProfileOption(Icons.Filled.Badge, s.profile.personalInfo, if (isGuest) s.profile.personalInfoSub else user?.name ?: s.profile.updateNameSub) {
                        onNavigate("profile_personal")
                    }
                    ProfileOption(Icons.Filled.NotificationsActive, s.profile.alertSettings, s.profile.alertSettingsSub) {
                        onNavigate("profile_alerts")
                    }
                    ProfileOption(Icons.Filled.Language, s.profile.language, s.profile.languageSub) {
                        onNavigate("profile_language")
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                ProfileSection(s.profile.sectionSafetyEmergency) {
                    ProfileOption(Icons.Filled.Contacts, s.profile.trustedContacts, s.profile.trustedContactsSub) {
                        onNavigate("profile_contacts")
                    }
                    ProfileOption(Icons.Filled.HealthAndSafety, s.profile.medicalEmergencyInfo, s.profile.medicalEmergencyInfoSub) {
                        onNavigate("profile_medical")
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                ProfileSection(s.profile.sectionSupportLegal) {
                    ProfileOption(Icons.Filled.HelpCenter, s.profile.helpFaq, s.profile.helpFaqSub) {
                        onNavigate("profile_help")
                    }
                    ProfileOption(Icons.Filled.Gavel, s.profile.privacyPolicy, s.profile.privacyPolicySub) {
                        onNavigate("profile_privacy")
                    }
                    ProfileOption(Icons.Filled.Info, s.profile.aboutApp, s.profile.aboutAppSub) {
                        onNavigate("profile_about")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NagarSetuColors.Surface)
    ) {
        Text(
            text = title.uppercase(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            fontSize = 11.sp,
            color = NagarSetuColors.Accent,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        HorizontalDivider(color = NagarSetuColors.SurfaceVariant, thickness = 1.dp)
        content()
    }
}

@Composable
fun ProfileOption(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NagarSetuColors.Background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = NagarSetuColors.Accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NagarSetuColors.TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = NagarSetuColors.TextSecondary)
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = NagarSetuColors.TextDisabled,
            modifier = Modifier.size(20.dp)
        )
    }
}
