package com.nagarsetu.drivelegal.data

import com.google.gson.JsonObject
import com.nagarsetu.core.data.AssetDataRepository
import com.nagarsetu.drivelegal.data.chatbot.TfIdfBot
import com.nagarsetu.drivelegal.domain.config.DriveLegalConfig
import com.nagarsetu.drivelegal.domain.model.*
import com.nagarsetu.drivelegal.domain.usecase.CalculateFineUseCase
import com.nagarsetu.drivelegal.domain.usecase.SearchViolationUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveLegalRepository @Inject constructor(
    private val assets: AssetDataRepository,
    private val bot: TfIdfBot,
    private val calculateFineUseCase: CalculateFineUseCase,
    private val searchViolation: SearchViolationUseCase
) {

    // ── Chat ─────────────────────────────────────────────────────────────────

    fun chatResponse(query: String): String = bot.query(query)

    fun quickSuggestions(): List<String> = bot.quickSuggestions().takeIf { it.isNotEmpty() }
        ?: DriveLegalConfig.let {
            listOf(
                "Helmet fine kitna hai?",
                "Red light jump penalty",
                "Drunk driving challan",
                "Speeding fine for bike",
                "No parking challan",
                "Triple riding fine",
                "Bina license ke fine",
                "Challan kaise bhare"
            )
        }

    // ── Fine calculation ─────────────────────────────────────────────────────

    fun calculateFine(
        violation: ViolationType,
        offenceRepeat: OffenceRepeat = OffenceRepeat.FIRST,
        vehicleCategory: VehicleCategory = VehicleCategory.CAR,
        targetCurrency: String = "INR",
        countryCode: String = "IN"
    ): FineCalculation = calculateFineUseCase.execute(
        violation, offenceRepeat, vehicleCategory, targetCurrency, countryCode
    )

    fun calculateMultiViolation(
        violations: List<ViolationType>,
        offenceRepeat: OffenceRepeat = OffenceRepeat.FIRST,
        vehicleCategory: VehicleCategory = VehicleCategory.CAR,
        targetCurrency: String = "INR",
        countryCode: String = "IN"
    ): MultiViolationResult = calculateFineUseCase.executeMulti(
        violations, offenceRepeat, vehicleCategory, targetCurrency, countryCode
    )

    // ── Country info ─────────────────────────────────────────────────────────

    fun countryInfo(code: String): DriveLegalConfig.CountryInfo? =
        DriveLegalConfig.BIMSTEC_COUNTRIES.find { it.code == code }

    fun allCountries(): List<DriveLegalConfig.CountryInfo> = DriveLegalConfig.BIMSTEC_COUNTRIES

    // ── Violation search ─────────────────────────────────────────────────────

    fun searchViolations(query: String): List<ViolationType> = searchViolation.execute(query)

    fun allViolations(): List<ViolationType> = ViolationType.entries

    // ── Asset data ──────────────────────────────────────────────────────────

    fun speedCameras(): List<JsonObject> {
        val arr = assets.loadAppData()
            .getAsJsonObject("driveLegal")
            ?.getAsJsonArray("speedCameras") ?: return emptyList()
        return arr.map { it.asJsonObject }
    }

    fun redLightCameras(): List<JsonObject> {
        val arr = assets.loadAppData()
            .getAsJsonObject("driveLegal")
            ?.getAsJsonArray("redLightCameras") ?: return emptyList()
        return arr.map { it.asJsonObject }
    }

    fun trafficFinesForCategory(vehicleKey: String): Map<String, Int> {
        val fines = assets.loadAppData()
            .getAsJsonObject("driveLegal")
            ?.getAsJsonObject("trafficFines")
            ?.getAsJsonObject(vehicleKey.lowercase()) ?: return emptyMap()
        return fines.entrySet().associate { it.key to it.value.asInt }
    }

    // ── OCR fraud detection ──────────────────────────────────────────────────

    /**
     * Validates an OCR result for possible fraud indicators:
     *  - Vehicle number format mismatch
     *  - Expired/future dates
     *  - Amount outside known ranges
     */
    fun validateOcrResult(raw: OcrScanResult): OcrScanResult {
        val fraudReasons = mutableListOf<String>()

        // Check vehicle number format (India: MP-05-AB-1234)
        raw.vehicleNumber?.let { num ->
            val cleanNum = num.replace(Regex("\\s"), "").uppercase()
            val indianPattern = Regex("^[A-Z]{2}\\d{2}[A-Z]{1,2}\\d{1,4}$")
            if (!indianPattern.matches(cleanNum)) {
                fraudReasons.add("Vehicle number format mismatch")
            }
        }

        // Check date (must be recent, not future)
        raw.date?.let { date ->
            try {
                val parts = date.split("/", "-")
                if (parts.size >= 3) {
                    val year = parts.lastOrNull()?.toIntOrNull() ?: 2026
                    if (year > 2026 || year < 2020) {
                        fraudReasons.add("Suspicious date: $date")
                    }
                }
            } catch (_: Exception) { /* ignore parse errors */ }
        }

        // Check amount range
        raw.fineAmount?.replace(",", "")?.replace("₹", "")?.trim()?.toDoubleOrNull()?.let { amt ->
            if (amt > 100000 || amt < 100) {
                fraudReasons.add("Unusual fine amount: ₹${amt.toLong()}")
            }
        }

        return raw.copy(
            isSuspectFraud = fraudReasons.isNotEmpty(),
            fraudReason = fraudReasons.joinToString("; ").ifEmpty { null }
        )
    }
}
