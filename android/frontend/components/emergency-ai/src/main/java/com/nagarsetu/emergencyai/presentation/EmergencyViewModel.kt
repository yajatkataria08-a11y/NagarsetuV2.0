package com.nagarsetu.emergencyai.presentation

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nagarsetu.core.data.supabase.SupabaseUserRepository
import com.nagarsetu.core.data.local.ProfileLocalRepository
import com.nagarsetu.emergencyai.data.EmergencyRepository
import com.nagarsetu.emergencyai.data.sensor.ShakeDetector
import com.nagarsetu.emergencyai.domain.model.EmergencyStatus
import com.nagarsetu.emergencyai.domain.model.TriggerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencyViewModel @Inject constructor(
    application: Application,
    private val repository: EmergencyRepository,
    private val userRepository: SupabaseUserRepository,   // Fix #2: needed to persist SOS
    private val profileRepo: ProfileLocalRepository
) : AndroidViewModel(application) {

    val activeEmergency = repository.activeEmergency
    val dialNumbers = repository.bimstecDialer
    val goldenHourTotal = EmergencyRepository.GOLDEN_HOUR_SECONDS

    val trustedContacts = profileRepo.trustedContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _goldenHourRemaining = MutableStateFlow(0)
    val goldenHourRemaining = _goldenHourRemaining.asStateFlow()

    private var countdownJob: Job? = null
    private var shakeDetector: ShakeDetector? = null

    init {
        shakeDetector = ShakeDetector(getApplication()) {
            triggerSos(TriggerType.SHAKE)
        }
        shakeDetector?.start()
    }

    fun triggerSos(type: TriggerType) {
        val event = repository.triggerSos(type)
        startGoldenHourCountdown()
        viewModelScope.launch {
            // Fix #2: persist SOS event to Supabase so it survives app close
            val uid = userRepository.profileFlow.value?.uid ?: "guest_user"
            repository.saveEmergencyEvent(uid, event)

            delay(1200)
            repository.updateStatus(EmergencyStatus.DISPATCHED)
        }
    }

    private fun startGoldenHourCountdown() {
        countdownJob?.cancel()
        _goldenHourRemaining.value = goldenHourTotal
        countdownJob = viewModelScope.launch {
            while (_goldenHourRemaining.value > 0) {
                delay(1000)
                _goldenHourRemaining.value = (_goldenHourRemaining.value - 1).coerceAtLeast(0)
            }
        }
    }

    fun processTriage(text: String) = repository.applyTriage(text)
    fun cancelSos() {
        repository.cancel()
        countdownJob?.cancel()
        _goldenHourRemaining.value = 0
    }

    fun traumaCentreName(): String = repository.nearestTraumaCentre()

    fun dial(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    override fun onCleared() {
        shakeDetector?.stop()
        super.onCleared()
    }
}
