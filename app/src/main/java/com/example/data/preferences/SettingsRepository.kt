package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taxi_meter_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        // Standard Refilled Default Values:
        // Local: 100 Base, 30 / km, 2 / min wait
        const val DEFAULT_BASE_FARE = 100.0
        const val DEFAULT_FARE_PER_KM = 30.0
        const val DEFAULT_WAIT_FARE_PER_MIN = 2.0
        const val DEFAULT_SPEED_THRESHOLD = 5.0
        const val DEFAULT_AUDIO_ENABLED = true
        const val DEFAULT_CURRENCY = "₹"

        // Hourly Rental: 350 / hr, 10 km included per hour, 20 / km extra
        const val DEFAULT_RENTAL_HOURLY_RATE = 350.0
        const val DEFAULT_RENTAL_INCLUDED_KM_PER_HOUR = 10.0
        const val DEFAULT_RENTAL_MIN_HOURS = 1.0
        const val DEFAULT_RENTAL_EXTRA_HR_RATE = 350.0
        const val DEFAULT_RENTAL_EXTRA_KM_RATE = 20.0

        // Outstation: 30 / km, 300 Driver Bata
        const val DEFAULT_OUTSTATION_FARE_PER_KM = 30.0
        const val DEFAULT_OUTSTATION_DRIVER_BATA = 300.0
        const val DEFAULT_OUTSTATION_MIN_FARE = 0.0
        const val DEFAULT_OUTSTATION_WAIT_RATE = 0.0
        const val DEFAULT_OUTSTATION_ADDITIONAL_CHARGES = 0.0

        // Local / Metered Tariff Keys
        val KEY_BASE_FARE = doublePreferencesKey("base_fare")
        val KEY_FARE_PER_KM = doublePreferencesKey("fare_per_km")
        val KEY_WAIT_FARE_PER_MIN = doublePreferencesKey("wait_fare_per_min")
        val KEY_SPEED_THRESHOLD = doublePreferencesKey("speed_threshold") // in km/h
        val KEY_AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
        val KEY_CURRENCY = stringPreferencesKey("currency")

        // Hourly Rental Tariff Keys
        val KEY_RENTAL_HOURLY_RATE = doublePreferencesKey("rental_hourly_rate")
        val KEY_RENTAL_INCLUDED_KM_PER_HOUR = doublePreferencesKey("rental_included_km_per_hour")
        val KEY_RENTAL_MIN_HOURS = doublePreferencesKey("rental_min_hours")
        val KEY_RENTAL_EXTRA_HR_RATE = doublePreferencesKey("rental_extra_hr_rate")
        val KEY_RENTAL_EXTRA_KM_RATE = doublePreferencesKey("rental_extra_km_rate")

        // Outstation Tariff Keys
        val KEY_OUTSTATION_FARE_PER_KM = doublePreferencesKey("outstation_fare_per_km")
        val KEY_OUTSTATION_MIN_FARE = doublePreferencesKey("outstation_min_fare")
        val KEY_OUTSTATION_WAIT_RATE = doublePreferencesKey("outstation_wait_rate")
        val KEY_OUTSTATION_DRIVER_BATA = doublePreferencesKey("outstation_driver_bata")
        val KEY_OUTSTATION_ADDITIONAL_CHARGES = doublePreferencesKey("outstation_additional_charges")

        // Distance Calibration Factor (Default 1.0 = no calibration)
        const val DEFAULT_DISTANCE_CALIBRATION_FACTOR = 1.0
        val KEY_DISTANCE_CALIBRATION_FACTOR = doublePreferencesKey("distance_calibration_factor")

        // Floating Speedometer Bubble Overlay (Default true = automatically show on minimize)
        const val DEFAULT_FLOATING_BUBBLE_ENABLED = true
        val KEY_FLOATING_BUBBLE_ENABLED = booleanPreferencesKey("floating_bubble_enabled")

        // Application Language (Default "en" = English, "ta" = Tamil)
        const val DEFAULT_APP_LANGUAGE = "en"
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    init {
        com.example.util.AppLanguageManager.init(context)
    }

    // Application Language Flow
    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        val stored = preferences[KEY_APP_LANGUAGE]
        if (stored != null) {
            com.example.util.AppLanguageManager.setLanguageByCode(context, stored)
            stored
        } else {
            com.example.util.AppLanguageManager.getLanguage(context).code
        }
    }

    // Floating Speedometer Bubble Flow
    val floatingBubbleEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_FLOATING_BUBBLE_ENABLED] ?: DEFAULT_FLOATING_BUBBLE_ENABLED
    }

    // Distance Calibration Flow
    val distanceCalibrationFactor: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_DISTANCE_CALIBRATION_FACTOR] ?: DEFAULT_DISTANCE_CALIBRATION_FACTOR
    }

    // Local / Metered Flows
    val baseFare: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_BASE_FARE] ?: DEFAULT_BASE_FARE
    }

    val farePerKm: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_FARE_PER_KM] ?: DEFAULT_FARE_PER_KM
    }

    val waitFarePerMin: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_WAIT_FARE_PER_MIN] ?: DEFAULT_WAIT_FARE_PER_MIN
    }

    val speedThreshold: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_SPEED_THRESHOLD] ?: DEFAULT_SPEED_THRESHOLD
    }

    val audioEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUDIO_ENABLED] ?: DEFAULT_AUDIO_ENABLED
    }

    val currency: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CURRENCY] ?: DEFAULT_CURRENCY
    }

    // Hourly Rental Flows
    val rentalHourlyRate: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_RENTAL_HOURLY_RATE] ?: DEFAULT_RENTAL_HOURLY_RATE
    }

    val rentalIncludedKmPerHour: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_RENTAL_INCLUDED_KM_PER_HOUR] ?: DEFAULT_RENTAL_INCLUDED_KM_PER_HOUR
    }

    val rentalMinHours: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_RENTAL_MIN_HOURS] ?: DEFAULT_RENTAL_MIN_HOURS
    }

    val rentalExtraHrRate: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_RENTAL_EXTRA_HR_RATE] ?: DEFAULT_RENTAL_EXTRA_HR_RATE
    }

    val rentalExtraKmRate: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_RENTAL_EXTRA_KM_RATE] ?: DEFAULT_RENTAL_EXTRA_KM_RATE
    }

    // Outstation Flows
    val outstationFarePerKm: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_OUTSTATION_FARE_PER_KM] ?: DEFAULT_OUTSTATION_FARE_PER_KM
    }

    val outstationMinFare: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_OUTSTATION_MIN_FARE] ?: DEFAULT_OUTSTATION_MIN_FARE
    }

    val outstationWaitRate: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_OUTSTATION_WAIT_RATE] ?: DEFAULT_OUTSTATION_WAIT_RATE
    }

    val outstationDriverBata: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_OUTSTATION_DRIVER_BATA] ?: DEFAULT_OUTSTATION_DRIVER_BATA
    }

    val outstationAdditionalCharges: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_OUTSTATION_ADDITIONAL_CHARGES] ?: DEFAULT_OUTSTATION_ADDITIONAL_CHARGES
    }

    // Tariff setters
    suspend fun updateBaseFare(value: Double?) {
        context.dataStore.edit { preferences ->
            if (value != null) preferences[KEY_BASE_FARE] = value else preferences.remove(KEY_BASE_FARE)
        }
    }

    suspend fun updateFarePerKm(value: Double?) {
        context.dataStore.edit { preferences ->
            if (value != null) preferences[KEY_FARE_PER_KM] = value else preferences.remove(KEY_FARE_PER_KM)
        }
    }

    suspend fun updateWaitFarePerMin(value: Double?) {
        context.dataStore.edit { preferences ->
            if (value != null) preferences[KEY_WAIT_FARE_PER_MIN] = value else preferences.remove(KEY_WAIT_FARE_PER_MIN)
        }
    }

    suspend fun updateSpeedThreshold(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SPEED_THRESHOLD] = value
        }
    }

    suspend fun updateAudioEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUDIO_ENABLED] = value
        }
    }

    suspend fun updateFloatingBubbleEnabled(value: Boolean) {
        com.example.service.FloatingSpeedOverlayManager.setBubbleEnabled(context, value)
        context.dataStore.edit { preferences ->
            preferences[KEY_FLOATING_BUBBLE_ENABLED] = value
        }
    }

    suspend fun updateCurrency(value: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CURRENCY] = value
        }
    }

    suspend fun updateAppLanguage(languageCode: String) {
        val normalized = com.example.util.AppLanguage.fromCode(languageCode).code
        com.example.util.AppLanguageManager.setLanguageByCode(context, normalized)
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_LANGUAGE] = normalized
        }
    }

    suspend fun updateRentalHourlyRate(value: Double?) {
        context.dataStore.edit { preferences ->
            if (value != null) preferences[KEY_RENTAL_HOURLY_RATE] = value else preferences.remove(KEY_RENTAL_HOURLY_RATE)
        }
    }

    suspend fun updateRentalIncludedKmPerHour(value: Double?) {
        context.dataStore.edit { preferences ->
            if (value != null) {
                preferences[KEY_RENTAL_INCLUDED_KM_PER_HOUR] = value
            } else {
                preferences.remove(KEY_RENTAL_INCLUDED_KM_PER_HOUR)
            }
        }
    }

    suspend fun updateRentalMinHours(value: Double?) {
        context.dataStore.edit { preferences ->
            if (value != null) preferences[KEY_RENTAL_MIN_HOURS] = value else preferences.remove(KEY_RENTAL_MIN_HOURS)
        }
    }

    suspend fun updateRentalExtraHrRate(value: Double?) {
        context.dataStore.edit { preferences ->
            if (value != null) preferences[KEY_RENTAL_EXTRA_HR_RATE] = value else preferences.remove(KEY_RENTAL_EXTRA_HR_RATE)
        }
    }

    suspend fun updateRentalExtraKmRate(value: Double?) {
        context.dataStore.edit { preferences ->
            if (value != null) preferences[KEY_RENTAL_EXTRA_KM_RATE] = value else preferences.remove(KEY_RENTAL_EXTRA_KM_RATE)
        }
    }

    suspend fun updateOutstationFarePerKm(value: Double?) {
        context.dataStore.edit { preferences ->
            if (value != null) preferences[KEY_OUTSTATION_FARE_PER_KM] = value else preferences.remove(KEY_OUTSTATION_FARE_PER_KM)
        }
    }

    suspend fun updateOutstationMinFare(value: Double?) {
        context.dataStore.edit { preferences ->
            if (value != null) preferences[KEY_OUTSTATION_MIN_FARE] = value else preferences.remove(KEY_OUTSTATION_MIN_FARE)
        }
    }

    suspend fun updateOutstationWaitRate(value: Double?) {
        context.dataStore.edit { preferences ->
            if (value != null) preferences[KEY_OUTSTATION_WAIT_RATE] = value else preferences.remove(KEY_OUTSTATION_WAIT_RATE)
        }
    }

    suspend fun updateOutstationDriverBata(value: Double?) {
        context.dataStore.edit { preferences ->
            if (value != null) preferences[KEY_OUTSTATION_DRIVER_BATA] = value else preferences.remove(KEY_OUTSTATION_DRIVER_BATA)
        }
    }

    suspend fun updateOutstationAdditionalCharges(value: Double?) {
        context.dataStore.edit { preferences ->
            if (value != null) preferences[KEY_OUTSTATION_ADDITIONAL_CHARGES] = value else preferences.remove(KEY_OUTSTATION_ADDITIONAL_CHARGES)
        }
    }

    suspend fun updateDistanceCalibrationFactor(value: Double?) {
        context.dataStore.edit { preferences ->
            if (value != null) {
                // Constrain reasonable calibration factor between 0.90 and 1.15
                val clamped = value.coerceIn(0.90, 1.15)
                preferences[KEY_DISTANCE_CALIBRATION_FACTOR] = clamped
            } else {
                preferences.remove(KEY_DISTANCE_CALIBRATION_FACTOR)
            }
        }
    }

    suspend fun resetToDefaultTariffs() {
        context.dataStore.edit { preferences ->
            preferences[KEY_BASE_FARE] = DEFAULT_BASE_FARE
            preferences[KEY_FARE_PER_KM] = DEFAULT_FARE_PER_KM
            preferences[KEY_WAIT_FARE_PER_MIN] = DEFAULT_WAIT_FARE_PER_MIN
            preferences[KEY_RENTAL_HOURLY_RATE] = DEFAULT_RENTAL_HOURLY_RATE
            preferences[KEY_RENTAL_INCLUDED_KM_PER_HOUR] = DEFAULT_RENTAL_INCLUDED_KM_PER_HOUR
            preferences[KEY_RENTAL_MIN_HOURS] = DEFAULT_RENTAL_MIN_HOURS
            preferences[KEY_RENTAL_EXTRA_HR_RATE] = DEFAULT_RENTAL_EXTRA_HR_RATE
            preferences[KEY_RENTAL_EXTRA_KM_RATE] = DEFAULT_RENTAL_EXTRA_KM_RATE
            preferences[KEY_OUTSTATION_FARE_PER_KM] = DEFAULT_OUTSTATION_FARE_PER_KM
            preferences[KEY_OUTSTATION_DRIVER_BATA] = DEFAULT_OUTSTATION_DRIVER_BATA
            preferences[KEY_OUTSTATION_MIN_FARE] = DEFAULT_OUTSTATION_MIN_FARE
            preferences[KEY_OUTSTATION_WAIT_RATE] = DEFAULT_OUTSTATION_WAIT_RATE
            preferences[KEY_OUTSTATION_ADDITIONAL_CHARGES] = DEFAULT_OUTSTATION_ADDITIONAL_CHARGES
            preferences[KEY_SPEED_THRESHOLD] = DEFAULT_SPEED_THRESHOLD
            preferences[KEY_CURRENCY] = DEFAULT_CURRENCY
            preferences[KEY_DISTANCE_CALIBRATION_FACTOR] = DEFAULT_DISTANCE_CALIBRATION_FACTOR
        }
    }
}
