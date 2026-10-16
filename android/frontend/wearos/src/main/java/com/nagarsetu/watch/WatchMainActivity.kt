package com.nagarsetu.watch

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WatchMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WatchApp()
        }
    }

    private fun triggerHapticSiren() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                val effect = VibrationEffect.createWaveform(longArrayOf(0, 250, 150, 250, 150, 500), -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun sendSosToPhone(heartRate: Int = 110): Boolean {
        return try {
            val nodeClient = Wearable.getNodeClient(this)
            val messageClient = Wearable.getMessageClient(this)
            
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                Log.e("WatchSOS", "No paired phone found!")
                return false
            }
            
            var success = false
            val payload = "TRIGGER_SOS:HR=$heartRate:BATTERY=88".toByteArray()
            nodes.forEach { node: Node ->
                try {
                    messageClient.sendMessage(node.id, "/raksha/sos", payload).await()
                    Log.i("WatchSOS", "SOS with HR sent to: ${node.displayName}")
                    success = true
                } catch (e: Exception) {
                    Log.e("WatchSOS", "Failed to send to ${node.displayName}: ${e.message}")
                }
            }
            success
        } catch (e: Exception) {
            Log.e("WatchSOS", "Wearable API Error: ${e.message}")
            false
        }
    }

    @Composable
    fun WatchApp() {
        val coroutineScope = rememberCoroutineScope()
        var status by remember { mutableStateOf("Ready") }
        var isSending by remember { mutableStateOf(false) }
        val simulatedHeartRate = 112

        Scaffold(
            timeText = { TimeText() }
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        if (!isSending) {
                            isSending = true
                            triggerHapticSiren()
                            status = "SIGNALING PHONE..."
                            coroutineScope.launch {
                                val ok = sendSosToPhone(simulatedHeartRate)
                                if (ok) {
                                    status = "🚨 BEACON ACTIVE"
                                    delay(2500)
                                    status = "Ready"
                                } else {
                                    status = "PHONE NOT FOUND"
                                    delay(3000)
                                    status = "Ready"
                                }
                                isSending = false
                            }
                        }
                    },
                    modifier = Modifier.size(110.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFDC2626)),
                    enabled = !isSending
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SOS", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("❤️ $simulatedHeartRate", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    status,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (status.contains("ACTIVE")) Color(0xFFEF4444) else if (status.contains("NOT")) Color.Yellow else Color.LightGray
                )
            }
        }
    }
}
