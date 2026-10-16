package com.nagarsetu.backend.core

/**
 * Fix #15: Single source of truth for Bhopal city coordinates shared across
 * all backend components.
 *
 * Previously, backend classes imported BHOPAL_LAT/BHOPAL_LNG from
 * `core.ui.theme.Color` (a UI-layer file), which is an architecture smell —
 * backend modules should never depend on UI theme files. They also sometimes
 * hardcoded 23.2599 / 77.4126 inline.
 *
 * Migrate all backend usages here. Frontend modules should continue to use
 * `com.nagarsetu.core.ui.theme.BHOPAL_LAT / BHOPAL_LNG`.
 */
object CivicConstants {
    /** Bhopal city centre latitude (°N). */
    const val BHOPAL_LAT: Double = 23.2599

    /** Bhopal city centre longitude (°E). */
    const val BHOPAL_LNG: Double = 77.4126

    /** Default search/render radius in kilometres for city-wide queries. */
    const val CITY_RADIUS_KM: Double = 15.0
}
