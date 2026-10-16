package com.nagarsetu.drivelegal.domain.config

/**
 * Centralised configuration for DriveLegal: exchange rates, multipliers,
 * SLA timelines, and BIMSTEC jurisdiction metadata.
 */
object DriveLegalConfig {

    // ── Repeat-offence multipliers ─────────────────────────────────────────
    const val FIRST_TIME_MULTIPLIER  = 1.0
    const val SECOND_TIME_MULTIPLIER = 2.0
    const val THIRD_TIME_PLUS_MULTIPLIER = 4.0

    // ── Supported currencies ───────────────────────────────────────────────
    val SUPPORTED_CURRENCIES = listOf("INR", "BDT", "BTN", "MMK", "NPR", "LKR", "THB")

    /**
     * Approximate exchange rates relative to INR (1 INR = X units of target).
     * Update periodically; used only for indicative conversion, not financial advice.
     */
    val EXCHANGE_RATES: Map<String, Double> = mapOf(
        "INR" to 1.0,
        "BDT" to 1.32,   // Bangladeshi Taka
        "BTN" to 1.0,    // Bhutanese Ngultrum (pegged to INR)
        "MMK" to 25.0,   // Myanmar Kyat
        "NPR" to 1.6,    // Nepalese Rupee
        "LKR" to 3.8,    // Sri Lankan Rupee
        "THB" to 0.43    // Thai Baht
    )

    /** Currency symbols for display */
    val CURRENCY_SYMBOLS: Map<String, String> = mapOf(
        "INR" to "₹",
        "BDT" to "৳",
        "BTN" to "Nu ",
        "MMK" to "K ",
        "NPR" to "Rs ",
        "LKR" to "Rs ",
        "THB" to "฿"
    )

    // ── Country → jurisdiction metadata ──────────────────────────────────
    data class CountryInfo(
        val code: String,
        val name: String,
        val flag: String,
        val currency: String,
        val currencySymbol: String,
        val trafficHelpline: String,
        val emergency: String,
        val lawReference: String
    )

    val BIMSTEC_COUNTRIES = listOf(
        CountryInfo("IN", "India",      "🇮🇳", "INR", "₹",   "0755-2443344", "112",  "Motor Vehicles Act 1988"),
        CountryInfo("BD", "Bangladesh", "🇧🇩", "BDT", "৳",   "9999",         "999",  "Motor Vehicles Ordinance 1983"),
        CountryInfo("BT", "Bhutan",     "🇧🇹", "BTN", "Nu ", "112",          "112",  "Road Safety & Transport Act 1999"),
        CountryInfo("MM", "Myanmar",    "🇲🇲", "MMK", "K ",  "199",          "999",  "Motor Vehicles Law 2011"),
        CountryInfo("NP", "Nepal",      "🇳🇵", "NPR", "Rs ", "103",          "100",  "Motor Vehicles Act 2049 BS"),
        CountryInfo("LK", "Sri Lanka",  "🇱🇰", "LKR", "Rs ", "1990",         "1990", "Motor Traffic Act 1951"),
        CountryInfo("TH", "Thailand",   "🇹🇭", "THB", "฿",   "1192",         "1669", "Land Traffic Act B.E. 2522")
    )

    /** Default city constants (extensible) */
    const val DEFAULT_CITY   = "Bhopal"
    const val DEFAULT_STATE  = "Madhya Pradesh"
    const val DEFAULT_COUNTRY = "IN"

    // ── Vehicle type fine multipliers ──────────────────────────────────────
    val VEHICLE_FINE_MULTIPLIERS: Map<String, Double> = mapOf(
        "BIKE"  to 1.0,
        "CAR"   to 1.0,
        "TRUCK" to 2.5,
        "AUTO"  to 1.2,
        "OTHER" to 1.5
    )

    // ── Base fine map (INR) for quick offline lookup ───────────────────────
    val BASE_FINES_INR: Map<String, Double> = mapOf(
        "SPEEDING"      to 1000.0,
        "PARKING"       to 500.0,
        "SIGNAL_JUMP"   to 1000.0,
        "NO_HELMET"     to 1000.0,
        "DRUNK_DRIVING" to 10000.0,
        "NO_INSURANCE"  to 2000.0,
        "NO_LICENSE"    to 5000.0,
        "NO_SEATBELT"   to 1000.0,
        "MOBILE_DRIVING" to 1000.0,
        "OVERLOADING"   to 20000.0,
        "TRIPLE_RIDING" to 1000.0,
        "EXPIRED_RC"    to 5000.0
    )
}
