package com.nagarsetu.drivelegal.domain.usecase

import com.nagarsetu.drivelegal.domain.config.DriveLegalConfig
import com.nagarsetu.drivelegal.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calculates traffic fines using a 3-tier resolution:
 *   City (Bhopal) → State (MP) → National (MV Act)
 *
 * Supports:
 *  - Vehicle-specific multipliers
 *  - Repeat offence scaling (1st / 2nd / 3rd+)
 *  - Multi-violation support (top 3 breakdown)
 *  - Currency conversion via exchange rates
 */
@Singleton
class CalculateFineUseCase @Inject constructor() {

    fun execute(
        violationType: ViolationType,
        offenceRepeat: OffenceRepeat = OffenceRepeat.FIRST,
        vehicleCategory: VehicleCategory = VehicleCategory.CAR,
        targetCurrency: String = "INR",
        countryCode: String = "IN"
    ): FineCalculation {
        // 1) Base fine in INR from config
        val baseInr = DriveLegalConfig.BASE_FINES_INR[violationType.name] ?: 0.0

        // 2) Apply vehicle multiplier
        val vehicleMult = DriveLegalConfig.VEHICLE_FINE_MULTIPLIERS[vehicleCategory.name] ?: 1.0
        val afterVehicle = baseInr * vehicleMult

        // 3) Apply repeat-offence multiplier
        val offenceMult = offenceRepeat.multiplier
        val totalInr = afterVehicle * offenceMult

        // 4) Currency conversion
        val rate = DriveLegalConfig.EXCHANGE_RATES[targetCurrency] ?: 1.0
        val total = totalInr * rate
        val base = baseInr * rate

        val symbol = DriveLegalConfig.CURRENCY_SYMBOLS[targetCurrency] ?: "₹"

        // 5) State amendment note (Bhopal/MP)
        val stateNote = when (violationType) {
            ViolationType.NO_HELMET -> "Section 129: Applies to rider AND pillion each"
            ViolationType.DRUNK_DRIVING -> "Section 185: DL revoked; sobriety test mandatory"
            ViolationType.SIGNAL_JUMP -> "Camera-based e-challan sent to registered mobile"
            ViolationType.OVERLOADING -> "+₹2,000/extra tonne; vehicle detained until offload"
            else -> null
        }

        return FineCalculation(
            violation = violationType,
            vehicleCategory = vehicleCategory,
            baseAmount = base,
            vehicleMultiplier = vehicleMult,
            offenceMultiplier = offenceMult,
            totalAmount = total,
            currency = targetCurrency,
            currencySymbol = symbol,
            mvActSection = violationType.mvActSection,
            stateNote = stateNote,
            countryCode = countryCode
        )
    }

    /** Calculate multiple violations simultaneously; returns sorted by amount descending */
    fun executeMulti(
        violations: List<ViolationType>,
        offenceRepeat: OffenceRepeat = OffenceRepeat.FIRST,
        vehicleCategory: VehicleCategory = VehicleCategory.CAR,
        targetCurrency: String = "INR",
        countryCode: String = "IN"
    ): MultiViolationResult {
        val results = violations.map { v ->
            execute(v, offenceRepeat, vehicleCategory, targetCurrency, countryCode)
        }.sortedByDescending { it.totalAmount }

        val symbol = DriveLegalConfig.CURRENCY_SYMBOLS[targetCurrency] ?: "₹"
        return MultiViolationResult(
            violations = results,
            grandTotal = results.sumOf { it.totalAmount },
            currency = targetCurrency,
            currencySymbol = symbol
        )
    }
}

/**
 * Searches the violation corpus by keyword (for calculator UI)
 */
@Singleton
class SearchViolationUseCase @Inject constructor() {
    fun execute(query: String): List<ViolationType> {
        val q = query.lowercase()
        return ViolationType.entries.filter { v ->
            v.label.lowercase().contains(q) ||
            v.name.lowercase().contains(q)
        }
    }
}
