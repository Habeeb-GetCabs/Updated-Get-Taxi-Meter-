package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.random.Random

private val Context.otpDataStore: DataStore<Preferences> by preferencesDataStore(name = "trip_otp_prefs")

/**
 * Result of validating a Trip OTP.
 */
sealed class OtpValidationResult {
    data object Success : OtpValidationResult()
    data class Failure(val message: String) : OtpValidationResult()
}

/**
 * Manages local, secure Trip OTP generation and Driver Trip Authorization.
 * Completely local (NO backend, NO cloud, NO Firebase, NO Supabase).
 *
 * =========================================================================================
 * SECURITY LIMITATION:
 * Local validation cannot guarantee secure one-time enforcement across multiple devices
 * or prevent a modified app from bypassing its checks. Because there is no centralized
 * server/cloud authority, the OTP state resides exclusively in the client device's
 * DataStore. A modified APK or rooted device can inspect or override local preferences,
 * and an OTP generated on one phone cannot be synchronized with another device.
 * =========================================================================================
 */
class TripOtpManager(private val context: Context) {

    companion object {
        val KEY_ACTIVE_OTP = stringPreferencesKey("trip_active_otp")
        val KEY_IS_SESSION_ACTIVE = booleanPreferencesKey("trip_access_session_active")
        val KEY_LAST_CONSUMED_OTP = stringPreferencesKey("trip_last_consumed_otp")
        val KEY_CONSUMED_OTPS_SET = stringSetPreferencesKey("trip_consumed_otps_history")

        const val ADMIN_PASSWORD = "0710"
        const val CUSTOMER_OTP_MESSAGE = "Please share your trip OTP with the driver before starting your journey."

        /**
         * 20 permanent, easy-to-note 4-digit Trip OTP codes that can be shared with
         * drivers and customers at any time. Reusable, no burn/expiration, no backend needed.
         */
        val PERMANENT_OTPS: List<String> = listOf("1981", "1974", "1983", "1984", "2002")
    }

    /**
     * Active unused OTP flow. Empty/null means no unused OTP.
     */
    val activeOtpFlow: Flow<String?> = context.otpDataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_OTP]
    }

    /**
     * Whether the driver currently has an authorized session for this trip.
     */
    val isTripAccessGrantedFlow: Flow<Boolean> = context.otpDataStore.data.map { prefs ->
        prefs[KEY_IS_SESSION_ACTIVE] ?: false
    }

    /**
     * Consumed OTP history set.
     */
    val consumedOtpsFlow: Flow<Set<String>> = context.otpDataStore.data.map { prefs ->
        prefs[KEY_CONSUMED_OTPS_SET] ?: emptySet()
    }

    /**
     * Check if admin password is valid (0710).
     */
    fun verifyAdminPassword(input: String): Boolean {
        return input == ADMIN_PASSWORD
    }

    /**
     * Generates a new 4-digit numeric OTP with leading zeros supported (e.g. "0427", "4827").
     * If an unused OTP already exists, this overwrites/invalidates it.
     */
    suspend fun generateNewOtp(): String {
        val consumed = getConsumedOtps()
        var otpString: String
        var attempts = 0
        do {
            val randomNum = Random.nextInt(0, 10000)
            otpString = String.format("%04d", randomNum)
            attempts++
        } while ((consumed.contains(otpString) || PERMANENT_OTPS.contains(otpString)) && attempts < 25)

        context.otpDataStore.edit { prefs ->
            prefs[KEY_ACTIVE_OTP] = otpString
        }
        return otpString
    }

    suspend fun getActiveOtp(): String? {
        val prefs = context.otpDataStore.data.first()
        return prefs[KEY_ACTIVE_OTP]
    }

    suspend fun getConsumedOtps(): Set<String> {
        val prefs = context.otpDataStore.data.first()
        return prefs[KEY_CONSUMED_OTPS_SET] ?: emptySet()
    }

    /**
     * Authorizes trip with driver-entered OTP.
     * Accepts any of the 10 permanent reusable OTPs (PERMANENT_OTPS) unlimited times,
     * as well as the currently active one-time generated OTP.
     * Rejects invalid OTPs with clear, specific messages.
     * Rejects reuse of one-time consumed OTPs.
     *
     * Validating the Trip OTP does NOT start the meter. The driver must explicitly press START.
     */
    suspend fun validateAndAuthorizeTrip(enteredOtp: String): OtpValidationResult {
        val cleanOtp = enteredOtp.trim()
        if (cleanOtp.length != 4 || !cleanOtp.all { it.isDigit() }) {
            return OtpValidationResult.Failure("Please enter a valid 4-digit numeric Trip OTP.")
        }

        // 1. Check if the entered code is one of the 10 permanent reusable OTPs
        if (PERMANENT_OTPS.contains(cleanOtp)) {
            context.otpDataStore.edit { prefs ->
                prefs[KEY_IS_SESSION_ACTIVE] = true
            }
            return OtpValidationResult.Success
        }

        // 2. Otherwise validate against one-time generated OTPs
        val consumed = getConsumedOtps()
        if (consumed.contains(cleanOtp)) {
            return OtpValidationResult.Failure("This Trip OTP has already been used and consumed. Please request a new Trip OTP.")
        }

        val activeOtp = getActiveOtp()
        if (activeOtp.isNullOrBlank()) {
            return OtpValidationResult.Failure("Invalid Trip OTP. Enter a valid permanent OTP or generate a one-time OTP.")
        }

        if (activeOtp == cleanOtp) {
            context.otpDataStore.edit { prefs ->
                prefs.remove(KEY_ACTIVE_OTP)
                prefs[KEY_LAST_CONSUMED_OTP] = cleanOtp
                val currentSet = prefs[KEY_CONSUMED_OTPS_SET] ?: emptySet()
                prefs[KEY_CONSUMED_OTPS_SET] = currentSet + cleanOtp
                prefs[KEY_IS_SESSION_ACTIVE] = true
            }
            return OtpValidationResult.Success
        }

        return OtpValidationResult.Failure("Invalid Trip OTP. Please check the 4-digit code and try again.")
    }

    /**
     * Backward-compatible boolean validation.
     */
    suspend fun validateAndConsumeOtp(enteredOtp: String): Boolean {
        return validateAndAuthorizeTrip(enteredOtp) is OtpValidationResult.Success
    }

    /**
     * Ends and destroys the temporary Trip Access Session.
     * Locks the meter for the next trip.
     */
    suspend fun destroyTripAccessSession() {
        context.otpDataStore.edit { prefs ->
            prefs[KEY_IS_SESSION_ACTIVE] = false
        }
    }

    suspend fun isSessionActive(): Boolean {
        val prefs = context.otpDataStore.data.first()
        return prefs[KEY_IS_SESSION_ACTIVE] ?: false
    }

    /**
     * Clears all consumed OTP history (for testing or administrative reset).
     */
    suspend fun clearConsumedHistory() {
        context.otpDataStore.edit { prefs ->
            prefs.remove(KEY_CONSUMED_OTPS_SET)
            prefs.remove(KEY_LAST_CONSUMED_OTP)
        }
    }
}
