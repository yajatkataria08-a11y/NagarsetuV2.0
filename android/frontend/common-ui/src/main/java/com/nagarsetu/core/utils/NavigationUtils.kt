package com.nagarsetu.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object NavigationUtils {
    /**
     * Reusable navigation helper.
     * Starts Google Maps navigation from current location to destination.
     */
    fun openNavigation(context: Context, destLat: Double, destLng: Double) {
        if (destLat == 0.0 || destLng == 0.0) {
            Toast.makeText(context, "Invalid coordinates", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = Uri.parse("google.navigation:q=$destLat,$destLng&mode=d")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback to Browser if Maps app is missing
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open maps", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Use openNavigation instead", ReplaceWith("openNavigation(context, destLat, destLng)"))
    fun launchGoogleMapsNavigation(context: Context, destLat: Double, destLng: Double) {
        openNavigation(context, destLat, destLng)
    }
}
