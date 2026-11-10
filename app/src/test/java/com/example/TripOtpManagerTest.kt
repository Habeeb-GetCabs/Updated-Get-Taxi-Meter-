package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.OtpValidationResult
import com.example.data.preferences.TripOtpManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TripOtpManagerTest {

    private lateinit var otpManager: TripOtpManager

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        otpManager = TripOtpManager(context)
        otpManager.destroyTripAccessSession()
        otpManager.clearConsumedHistory()
    }

    @Test
    fun `test admin password verification`() {
        assertTrue(otpManager.verifyAdminPassword("0710"))
        assertFalse(otpManager.verifyAdminPassword("0000"))
        assertFalse(otpManager.verifyAdminPassword("1234"))
        assertFalse(otpManager.verifyAdminPassword(""))
    }

    @Test
    fun `test generate 4-digit OTP with leading zeros preserved`() = runBlocking {
        val otp = otpManager.generateNewOtp()
        assertEquals("OTP must be exactly 4 digits", 4, otp.length)
        assertTrue("OTP must be numeric", otp.all { it.isDigit() })
        assertEquals(otp, otpManager.getActiveOtp())
    }

    @Test
    fun `test valid OTP authorizes trip and does not automatically start meter`() = runBlocking {
        val otp = otpManager.generateNewOtp()

        // Before validation, session is inactive
        assertFalse(otpManager.isSessionActive())

        val result = otpManager.validateAndAuthorizeTrip(otp)
        assertTrue("Validation should succeed", result is OtpValidationResult.Success)

        // Session is now active/authorized for driver
        assertTrue(otpManager.isSessionActive())

        // Active OTP should be cleared after consumption
        assertNull(otpManager.getActiveOtp())
    }

    @Test
    fun `test invalid OTP rejected with clear message`() = runBlocking {
        otpManager.generateNewOtp()

        val wrongResult = otpManager.validateAndAuthorizeTrip("9999")
        assertTrue("Wrong OTP must fail", wrongResult is OtpValidationResult.Failure)
        val msg = (wrongResult as OtpValidationResult.Failure).message
        assertTrue(msg.contains("Invalid Trip OTP"))

        // Invalid length
        val shortResult = otpManager.validateAndAuthorizeTrip("12")
        assertTrue(shortResult is OtpValidationResult.Failure)
        assertTrue((shortResult as OtpValidationResult.Failure).message.contains("4-digit"))
    }

    @Test
    fun `test consumed OTP cannot be reused and is rejected with specific message`() = runBlocking {
        val otp = otpManager.generateNewOtp()

        // Consume OTP first time
        val firstResult = otpManager.validateAndAuthorizeTrip(otp)
        assertTrue(firstResult is OtpValidationResult.Success)

        // Reset session (e.g. trip ended)
        otpManager.destroyTripAccessSession()
        assertFalse(otpManager.isSessionActive())

        // Attempt to reuse same OTP for next trip
        val reuseResult = otpManager.validateAndAuthorizeTrip(otp)
        assertTrue("Reusing consumed OTP must fail", reuseResult is OtpValidationResult.Failure)
        val failMsg = (reuseResult as OtpValidationResult.Failure).message
        assertTrue(
            "Message must specify OTP was already used",
            failMsg.contains("already been used and consumed")
        )
    }

    @Test
    fun `test session destruction relocks meter for next trip`() = runBlocking {
        val otp = otpManager.generateNewOtp()
        otpManager.validateAndAuthorizeTrip(otp)
        assertTrue(otpManager.isSessionActive())

        // Trip ends
        otpManager.destroyTripAccessSession()
        assertFalse(otpManager.isSessionActive())
    }

    @Test
    fun `test 5 permanent OTPs work repeatedly without expiring or being consumed`() = runBlocking {
        assertEquals(5, TripOtpManager.PERMANENT_OTPS.size)

        for (permanentOtp in TripOtpManager.PERMANENT_OTPS) {
            // First trip with this permanent OTP
            otpManager.destroyTripAccessSession()
            assertFalse(otpManager.isSessionActive())

            val firstResult = otpManager.validateAndAuthorizeTrip(permanentOtp)
            assertTrue("Permanent OTP $permanentOtp must succeed on first use", firstResult is OtpValidationResult.Success)
            assertTrue(otpManager.isSessionActive())

            // End trip and reuse the exact same permanent OTP for a second trip
            otpManager.destroyTripAccessSession()
            assertFalse(otpManager.isSessionActive())

            val secondResult = otpManager.validateAndAuthorizeTrip(permanentOtp)
            assertTrue("Permanent OTP $permanentOtp must succeed on repeated use", secondResult is OtpValidationResult.Success)
            assertTrue(otpManager.isSessionActive())
        }
    }
}
