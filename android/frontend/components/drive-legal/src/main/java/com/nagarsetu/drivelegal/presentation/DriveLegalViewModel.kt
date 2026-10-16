package com.nagarsetu.drivelegal.presentation

import androidx.lifecycle.ViewModel
import com.nagarsetu.drivelegal.data.DriveLegalRepository
import com.nagarsetu.drivelegal.domain.config.DriveLegalConfig
import com.nagarsetu.drivelegal.domain.model.FineCalculation
import com.nagarsetu.drivelegal.domain.model.ViolationType
import com.nagarsetu.drivelegal.domain.model.OffenceRepeat
import com.nagarsetu.drivelegal.domain.model.VehicleCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class ChatMessage(val user: String, val bot: String)

enum class VehicleType(val label: String, val emoji: String) {
    BIKE("Bike", "🏍️"),
    CAR("Car", "🚗"),
    TRUCK("Truck", "🚛"),
    AUTO("Auto", "🛺")
}

data class BimstecCountry(
    val code: String,
    val name: String,
    val flag: String,
    val currency: String,
    val currencySymbol: String,
    val emergencyNumber: String
)

@HiltViewModel
class DriveLegalViewModel @Inject constructor(
    private val repository: DriveLegalRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _lastFine = MutableStateFlow<FineCalculation?>(null)
    val lastFine = _lastFine.asStateFlow()

    private val _showOcr = MutableStateFlow(false)
    val showOcr = _showOcr.asStateFlow()

    private val _selectedCountry = MutableStateFlow("IN")
    val selectedCountry = _selectedCountry.asStateFlow()

    private val _selectedVehicle = MutableStateFlow(VehicleType.CAR)
    val selectedVehicle = _selectedVehicle.asStateFlow()

    val bimstecCountries = listOf(
        BimstecCountry("IN", "India", "🇮🇳", "INR", "₹", "112"),
        BimstecCountry("BD", "Bangladesh", "🇧🇩", "BDT", "৳", "999"),
        BimstecCountry("BT", "Bhutan", "🇧🇹", "BTN", "Nu", "112"),
        BimstecCountry("MM", "Myanmar", "🇲🇲", "MMK", "K", "999"),
        BimstecCountry("NP", "Nepal", "🇳🇵", "NPR", "Rs", "100"),
        BimstecCountry("LK", "Sri Lanka", "🇱🇰", "LKR", "Rs", "1990"),
        BimstecCountry("TH", "Thailand", "🇹🇭", "THB", "฿", "1669")
    )

    val quickSuggestions = listOf(
        "Helmet fine kitna hai?",
        "Red light jump penalty",
        "Drunk driving challan",
        "Speeding fine for bike",
        "No parking challan",
        "Overloading truck fine"
    )

    fun currentCountry(): BimstecCountry =
        bimstecCountries.find { it.code == _selectedCountry.value } ?: bimstecCountries[0]

    fun selectCountry(code: String) {
        _selectedCountry.value = code
        // Recalculate last fine in new currency if available
        _lastFine.value?.let {
            val country = bimstecCountries.find { c -> c.code == code } ?: return
            val rate = DriveLegalConfig.EXCHANGE_RATES[country.currency] ?: 1.0
            _lastFine.value = it.copy(
                baseAmount = it.baseAmount / (DriveLegalConfig.EXCHANGE_RATES[it.currency] ?: 1.0) * rate,
                totalAmount = it.totalAmount / (DriveLegalConfig.EXCHANGE_RATES[it.currency] ?: 1.0) * rate,
                currency = country.currency
            )
        }
    }

    fun selectVehicle(type: VehicleType) {
        _selectedVehicle.value = type
    }

    fun sendMessage(query: String) {
        if (query.isBlank()) return
        val vehicleType = detectVehicleType(query)
        if (vehicleType != null) _selectedVehicle.value = vehicleType

        val response = repository.chatResponse(query)
        _messages.value = _messages.value + ChatMessage(query, response)

        // Auto-detect violation and calculate fine
        val violation = detectViolation(query)
        if (violation != null) {
            val country = currentCountry()
            _lastFine.value = repository.calculateFine(
                violation = violation,
                offenceRepeat = OffenceRepeat.FIRST,
                vehicleCategory = mapToDomainVehicle(_selectedVehicle.value),
                targetCurrency = country.currency
            )
        }
    }

    fun sampleHelmetFine() {
        val country = currentCountry()
        _lastFine.value = repository.calculateFine(
            violation = ViolationType.NO_HELMET,
            offenceRepeat = OffenceRepeat.SECOND,
            vehicleCategory = mapToDomainVehicle(_selectedVehicle.value),
            targetCurrency = country.currency
        )
    }

    private fun mapToDomainVehicle(type: VehicleType): VehicleCategory {
        return when (type) {
            VehicleType.BIKE -> VehicleCategory.BIKE
            VehicleType.CAR -> VehicleCategory.CAR
            VehicleType.TRUCK -> VehicleCategory.TRUCK
            VehicleType.AUTO -> VehicleCategory.AUTO
        }
    }

    fun openOcr() { _showOcr.value = true }
    fun closeOcr() { _showOcr.value = false }

    private fun detectVehicleType(query: String): VehicleType? {
        val q = query.lowercase()
        return when {
            q.contains("bike") || q.contains("motorcycle") || q.contains("scooty") || q.contains("two wheeler") -> VehicleType.BIKE
            q.contains("truck") || q.contains("lorry") || q.contains("overload") -> VehicleType.TRUCK
            q.contains("auto") || q.contains("rickshaw") -> VehicleType.AUTO
            q.contains("car") || q.contains("four wheeler") -> VehicleType.CAR
            else -> null
        }
    }

    private fun detectViolation(query: String): ViolationType? {
        val q = query.lowercase()
        return when {
            q.contains("helmet") || q.contains("no helmet") -> ViolationType.NO_HELMET
            q.contains("speed") || q.contains("overspeed") -> ViolationType.SPEEDING
            q.contains("red light") || q.contains("signal jump") || q.contains("traffic signal") -> ViolationType.SIGNAL_JUMP
            q.contains("parking") || q.contains("no parking") -> ViolationType.PARKING
            q.contains("drunk") || q.contains("alcohol") || q.contains("dui") -> ViolationType.DRUNK_DRIVING
            else -> null
        }
    }
}
