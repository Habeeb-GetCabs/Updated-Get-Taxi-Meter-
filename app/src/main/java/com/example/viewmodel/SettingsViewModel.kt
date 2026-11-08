package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    // Local / Metered Flows
    val baseFare: StateFlow<Double> = settingsRepository.baseFare
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_BASE_FARE)

    val farePerKm: StateFlow<Double> = settingsRepository.farePerKm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_FARE_PER_KM)

    val waitFarePerMin: StateFlow<Double> = settingsRepository.waitFarePerMin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_WAIT_FARE_PER_MIN)

    val speedThreshold: StateFlow<Double> = settingsRepository.speedThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_SPEED_THRESHOLD)

    val audioEnabled: StateFlow<Boolean> = settingsRepository.audioEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_AUDIO_ENABLED)

    val floatingBubbleEnabled: StateFlow<Boolean> = settingsRepository.floatingBubbleEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_FLOATING_BUBBLE_ENABLED)

    val currency: StateFlow<String> = settingsRepository.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_CURRENCY)

    val appLanguage: StateFlow<String> = settingsRepository.appLanguage
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            com.example.util.AppLanguageManager.getLanguage(application).code
        )

    // Hourly Rental Flows
    val rentalHourlyRate: StateFlow<Double> = settingsRepository.rentalHourlyRate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_RENTAL_HOURLY_RATE)

    val rentalIncludedKmPerHour: StateFlow<Double> = settingsRepository.rentalIncludedKmPerHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_RENTAL_INCLUDED_KM_PER_HOUR)

    val rentalMinHours: StateFlow<Double> = settingsRepository.rentalMinHours
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_RENTAL_MIN_HOURS)

    val rentalExtraHrRate: StateFlow<Double> = settingsRepository.rentalExtraHrRate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_RENTAL_EXTRA_HR_RATE)

    val rentalExtraKmRate: StateFlow<Double> = settingsRepository.rentalExtraKmRate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_RENTAL_EXTRA_KM_RATE)

    // Outstation Flows
    val outstationFarePerKm: StateFlow<Double> = settingsRepository.outstationFarePerKm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_OUTSTATION_FARE_PER_KM)

    val outstationMinFare: StateFlow<Double> = settingsRepository.outstationMinFare
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_OUTSTATION_MIN_FARE)

    val outstationWaitRate: StateFlow<Double> = settingsRepository.outstationWaitRate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_OUTSTATION_WAIT_RATE)

    val outstationDriverBata: StateFlow<Double> = settingsRepository.outstationDriverBata
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_OUTSTATION_DRIVER_BATA)

    val outstationAdditionalCharges: StateFlow<Double> = settingsRepository.outstationAdditionalCharges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_OUTSTATION_ADDITIONAL_CHARGES)

    val distanceCalibrationFactor: StateFlow<Double> = settingsRepository.distanceCalibrationFactor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_DISTANCE_CALIBRATION_FACTOR)

    // Update Functions
    fun updateBaseFare(value: Double?) {
        viewModelScope.launch {
            settingsRepository.updateBaseFare(value)
        }
    }

    fun updateFarePerKm(value: Double?) {
        viewModelScope.launch {
            settingsRepository.updateFarePerKm(value)
        }
    }

    fun updateWaitFarePerMin(value: Double?) {
        viewModelScope.launch {
            settingsRepository.updateWaitFarePerMin(value)
        }
    }

    fun updateSpeedThreshold(value: Double) {
        viewModelScope.launch {
            settingsRepository.updateSpeedThreshold(value)
        }
    }

    fun updateAudioEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAudioEnabled(value)
        }
    }

    fun updateFloatingBubbleEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateFloatingBubbleEnabled(value)
            com.example.service.LocationTrackingService.setFloatingBubbleEnabled(getApplication(), value)
        }
    }

    fun updateCurrency(value: String) {
        viewModelScope.launch {
            settingsRepository.updateCurrency(value)
        }
    }

    fun updateAppLanguage(languageCode: String) {
        com.example.util.AppLanguageManager.setLanguageByCode(getApplication(), languageCode)
        viewModelScope.launch {
            settingsRepository.updateAppLanguage(languageCode)
        }
    }

    fun updateRentalHourlyRate(value: Double?) {
        viewModelScope.launch {
            settingsRepository.updateRentalHourlyRate(value)
        }
    }

    fun updateRentalIncludedKmPerHour(value: Double?) {
        viewModelScope.launch {
            settingsRepository.updateRentalIncludedKmPerHour(value)
        }
    }

    fun updateRentalMinHours(value: Double?) {
        viewModelScope.launch {
            settingsRepository.updateRentalMinHours(value)
        }
    }

    fun updateRentalExtraHrRate(value: Double?) {
        viewModelScope.launch {
            settingsRepository.updateRentalExtraHrRate(value)
        }
    }

    fun updateRentalExtraKmRate(value: Double?) {
        viewModelScope.launch {
            settingsRepository.updateRentalExtraKmRate(value)
        }
    }

    fun updateOutstationFarePerKm(value: Double?) {
        viewModelScope.launch {
            settingsRepository.updateOutstationFarePerKm(value)
        }
    }

    fun updateOutstationMinFare(value: Double?) {
        viewModelScope.launch {
            settingsRepository.updateOutstationMinFare(value)
        }
    }

    fun updateOutstationWaitRate(value: Double?) {
        viewModelScope.launch {
            settingsRepository.updateOutstationWaitRate(value)
        }
    }

    fun updateOutstationDriverBata(value: Double?) {
        viewModelScope.launch {
            settingsRepository.updateOutstationDriverBata(value)
        }
    }

    fun updateOutstationAdditionalCharges(value: Double?) {
        viewModelScope.launch {
            settingsRepository.updateOutstationAdditionalCharges(value)
        }
    }

    fun updateDistanceCalibrationFactor(value: Double?) {
        viewModelScope.launch {
            settingsRepository.updateDistanceCalibrationFactor(value)
        }
    }

    fun resetToDefaultTariffs() {
        viewModelScope.launch {
            settingsRepository.resetToDefaultTariffs()
        }
    }
}
