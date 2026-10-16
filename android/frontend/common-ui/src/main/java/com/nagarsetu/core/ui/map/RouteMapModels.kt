package com.nagarsetu.core.ui.map

import org.osmdroid.util.GeoPoint

data class RouteMapArgs(
    val destinationLat: Double,
    val destinationLng: Double,
    val destinationName: String,
    val destinationSubtitle: String = "",   // e.g. "DC Fast · ₹15/unit"
    val iconType: RouteDestinationType = RouteDestinationType.EV_STATION
)

enum class RouteDestinationType {
    EV_STATION, PARKING, HOSPITAL, BUS_STOP, GENERAL
}

fun RouteDestinationType.markerDrawableRes(): Int = when (this) {
    RouteDestinationType.EV_STATION -> com.nagarsetu.core.R.drawable.ic_marker_ev
    RouteDestinationType.PARKING -> com.nagarsetu.core.R.drawable.ic_marker_ev
    RouteDestinationType.HOSPITAL -> com.nagarsetu.core.R.drawable.ic_marker_hospital
    RouteDestinationType.BUS_STOP -> com.nagarsetu.core.R.drawable.ic_marker_ev
    RouteDestinationType.GENERAL -> org.osmdroid.library.R.drawable.marker_default
}

data class RouteStep(
    val instruction: String,          // "Turn left onto MP Nagar Road"
    val distanceMeters: Double,       // distance of this step
    val distanceText: String,         // "350 m" or "1.2 km"
    val maneuverType: String,         // "turn", "depart", "arrive", "roundabout", etc.
    val maneuverModifier: String,     // "left", "right", "straight", "slight left", etc.
    val startPoint: GeoPoint          // where this step begins (for proximity trigger)
)

data class RouteInfo(
    val polylinePoints: List<GeoPoint>,     // decoded OSRM geometry
    val distanceMeters: Double,
    val durationSeconds: Double,
    val distanceText: String,               // "2.4 km"
    val etaText: String,                    // "~8 min"
    val nextTurnInstruction: String = "",
    val nextTurnDistanceText: String = "",
    val steps: List<RouteStep> = emptyList()
)
