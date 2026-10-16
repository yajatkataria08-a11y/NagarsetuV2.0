package com.nagarsetu.raksha.presentation.fakecall

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Fake Call Deterrent Screen (Inspired by Raksha)
 * Simulates an incoming call with system ringtone and in-call timer
 * to deter potential harassers in dark/isolated corridors.
 */
@Composable
fun FakeCallScreen(
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    var isAnswered by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var ringtonePlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Start ringtone on launch
    DisposableEffect(Unit) {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val player = MediaPlayer().apply {
                setDataSource(context, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            ringtonePlayer = player
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            ringtonePlayer?.stop()
            ringtonePlayer?.release()
            ringtonePlayer = null
        }
    }

    // Call timer once answered
    LaunchedEffect(isAnswered) {
        if (isAnswered) {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Caller Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 64.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("M", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(Modifier.height(16.dp))
                Text("Mom", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))

                if (!isAnswered) {
                    Text("Incoming Call...", fontSize = 16.sp, color = Color(0xFF94A3B8))
                } else {
                    val min = elapsedSeconds / 60
                    val sec = elapsedSeconds % 60
                    Text(
                        String.format("%02d:%02d", min, sec),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            // Call Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline / End Call
                IconButton(
                    onClick = {
                        ringtonePlayer?.stop()
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "Decline",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Accept Call (only when ringing)
                if (!isAnswered) {
                    IconButton(
                        onClick = {
                            ringtonePlayer?.stop()
                            isAnswered = true
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Accept",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
