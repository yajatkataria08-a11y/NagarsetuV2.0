package com.nagarsetu.raksha.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Android Quick Settings Tile providing instant 1-tap SOS trigger
 * directly from the system notification shade, even when device is locked.
 */
@RequiresApi(Build.VERSION_CODES.N)
class EmergencyTileService : TileService() {

    companion object {
        private const val TAG = "EmergencyTileService"
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = "NagarSetu SOS"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "1-Tap Emergency"
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        Log.w(TAG, "🚨 Quick Settings SOS Tile Clicked!")

        val tile = qsTile
        if (tile != null) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "SOS DISPATCHED"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Broadcasting GPS"
            }
            tile.updateTile()
        }

        // Trigger emergency SOS via background service
        EmergencyTriggerService.triggerSos(applicationContext)

        // Reset tile after 3 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val currentTile = qsTile ?: return@postDelayed
            currentTile.state = Tile.STATE_INACTIVE
            currentTile.label = "NagarSetu SOS"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                currentTile.subtitle = "1-Tap Emergency"
            }
            currentTile.updateTile()
        }, 4000)
    }

    override fun onStopListening() {
        super.onStopListening()
    }
}
