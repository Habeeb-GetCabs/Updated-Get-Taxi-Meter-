package com.example

import android.location.Location
import com.example.util.GpsDistanceEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GpsDistanceEngineTest {

    private lateinit var engine: GpsDistanceEngine

    @Before
    fun setup() {
        engine = GpsDistanceEngine(
            initialDistanceKm = 0.0,
            calibrationFactor = 1.0,
            speedThresholdKmH = 5.0,
            maxHorizontalAccuracyMeters = 35.0f,
            maxSpeedKmH = 220.0,
            maxGpsGapSeconds = 30.0
        )
    }

    private fun createLocation(
        lat: Double,
        lng: Double,
        timeMs: Long,
        accuracy: Float = 5.0f,
        speedMps: Float = 15.0f
    ): Location {
        return Location("gps").apply {
            latitude = lat
            longitude = lng
            time = timeMs
            setAccuracy(accuracy)
            speed = speedMps
        }
    }

    @Test
    fun `test valid distance increments`() {
        val t0 = 1000000L
        val loc1 = createLocation(12.971600, 77.594600, t0, accuracy = 5f, speedMps = 20f)
        val res1 = engine.processLocation(loc1, isTripRunning = true)
        assertTrue(res1.accepted)
        assertEquals(0.0, engine.accumulatedDistanceKm, 0.0001)

        // Point 2: 1 second later, moved ~25 meters north (approx 0.000225 deg lat)
        val loc2 = createLocation(12.971825, 77.594600, t0 + 1000L, accuracy = 5f, speedMps = 25f)
        val res2 = engine.processLocation(loc2, isTripRunning = true)
        assertTrue(res2.accepted)
        assertTrue("Distance should have incremented", res2.distanceIncrementM > 20.0)
        assertTrue("Total distance should be > 0", engine.accumulatedDistanceKm > 0.02)
        assertEquals(engine.accumulatedDistanceKm, res2.totalDistanceKm, 0.0001)
    }

    @Test
    fun `test inaccurate samples are rejected without corrupting tracking`() {
        val t0 = 1000000L
        val loc1 = createLocation(12.971600, 77.594600, t0, accuracy = 5f)
        engine.processLocation(loc1, isTripRunning = true)

        // Point 2: Poor horizontal accuracy (65 meters > 35m threshold)
        val loc2Inaccurate = createLocation(12.972000, 77.594600, t0 + 1000L, accuracy = 65.0f)
        val res2 = engine.processLocation(loc2Inaccurate, isTripRunning = true)
        assertFalse("Inaccurate sample must be rejected", res2.accepted)
        assertEquals(1, engine.totalInaccurateSamplesRejected)
        assertEquals(0.0, engine.accumulatedDistanceKm, 0.0001)

        // Point 3: Subsequent accurate sample should cleanly measure from loc1
        val loc3Accurate = createLocation(12.971825, 77.594600, t0 + 2000L, accuracy = 8.0f)
        val res3 = engine.processLocation(loc3Accurate, isTripRunning = true)
        assertTrue("Accurate sample following rejected sample must be accepted", res3.accepted)
        assertTrue(engine.accumulatedDistanceKm > 0.02)
    }

    @Test
    fun `test stationary movement and jitter is suppressed`() {
        val t0 = 1000000L
        val loc1 = createLocation(12.971600, 77.594600, t0, accuracy = 5f, speedMps = 0f)
        engine.processLocation(loc1, isTripRunning = true)

        // Stationary jitter: vehicle stopped at red light, GPS flutters 1.5 meters away with speed = 0.5 km/h
        val loc2Jitter = createLocation(12.971610, 77.594605, t0 + 1000L, accuracy = 6f, speedMps = 0.2f)
        val res2 = engine.processLocation(loc2Jitter, isTripRunning = true)
        assertTrue(res2.accepted)
        assertFalse("Vehicle should be detected as stationary", res2.isMoving)
        assertEquals(0.0, res2.distanceIncrementM, 0.0001)
        assertEquals(0.0, engine.accumulatedDistanceKm, 0.0001)
    }

    @Test
    fun `test impossible jumps and speed outliers are rejected`() {
        val t0 = 1000000L
        val loc1 = createLocation(12.971600, 77.594600, t0, accuracy = 5f)
        engine.processLocation(loc1, isTripRunning = true)

        // Impossible jump: 2 km away in 1 second = 7200 km/h (multipath teleportation glitch)
        val loc2Jump = createLocation(12.990000, 77.594600, t0 + 1000L, accuracy = 10f)
        val res2 = engine.processLocation(loc2Jump, isTripRunning = true)
        assertFalse("Teleportation jump must be rejected", res2.accepted)
        assertEquals(1, engine.totalOutliersRejected)
        assertEquals(0.0, engine.accumulatedDistanceKm, 0.0001)
    }

    @Test
    fun `test long GPS gap preserves distance without blind interpolation`() {
        val t0 = 1000000L
        val loc1 = createLocation(12.971600, 77.594600, t0, accuracy = 5f)
        engine.processLocation(loc1, isTripRunning = true)

        // Move 100 meters
        val loc2 = createLocation(12.972500, 77.594600, t0 + 5000L, accuracy = 5f)
        val res2 = engine.processLocation(loc2, isTripRunning = true)
        val distanceBeforeGap = engine.accumulatedDistanceKm
        assertTrue(distanceBeforeGap > 0.0)

        // Long gap: 90 seconds later (e.g. tunnel, temporary loss of lock)
        val loc3AfterGap = createLocation(12.985000, 77.594600, t0 + 95000L, accuracy = 6f)
        val res3 = engine.processLocation(loc3AfterGap, isTripRunning = true)
        assertTrue(res3.accepted)
        assertTrue("Gap must be detected and flagged", res3.gapDetected)
        assertEquals("Distance across gap must NOT be blindly fabricated", distanceBeforeGap, engine.accumulatedDistanceKm, 0.0001)
        assertEquals(1, engine.totalGapsDetected)

        // Subsequent sample 1s later resumes normal accumulation
        val loc4 = createLocation(12.985200, 77.594600, t0 + 96000L, accuracy = 5f)
        val res4 = engine.processLocation(loc4, isTripRunning = true)
        assertTrue(res4.accepted)
        assertTrue(engine.accumulatedDistanceKm > distanceBeforeGap)
    }

    @Test
    fun `test restoration of accumulated distance on service recovery`() {
        // Engine restored after service restart with 14.85 km previously saved in DB
        val restoredEngine = GpsDistanceEngine(initialDistanceKm = 14.85)
        assertEquals(14.85, restoredEngine.accumulatedDistanceKm, 0.0001)

        val t0 = 1000000L
        val loc1 = createLocation(12.971600, 77.594600, t0, accuracy = 5f)
        val res1 = restoredEngine.processLocation(loc1, isTripRunning = true)
        assertTrue(res1.accepted)
        // First location after recovery establishes anchor, does not reset or double count
        assertEquals(14.85, restoredEngine.accumulatedDistanceKm, 0.0001)

        // Next valid sample adds smoothly on top of restored distance
        val loc2 = createLocation(12.971825, 77.594600, t0 + 1000L, accuracy = 5f)
        val res2 = restoredEngine.processLocation(loc2, isTripRunning = true)
        assertTrue(res2.accepted)
        assertTrue("Should add on top of restored distance", restoredEngine.accumulatedDistanceKm > 14.87)
    }

    @Test
    fun `test configurable calibration factor`() {
        val calibratedEngine = GpsDistanceEngine(
            initialDistanceKm = 0.0,
            calibrationFactor = 1.02 // +2% calibration
        )

        val t0 = 1000000L
        val loc1 = createLocation(12.971600, 77.594600, t0, accuracy = 5f)
        calibratedEngine.processLocation(loc1, isTripRunning = true)

        val loc2 = createLocation(12.972600, 77.594600, t0 + 4000L, accuracy = 5f)
        val res = calibratedEngine.processLocation(loc2, isTripRunning = true)
        assertTrue(res.accepted)

        val standardEngine = GpsDistanceEngine(calibrationFactor = 1.0)
        standardEngine.processLocation(loc1, isTripRunning = true)
        val stdRes = standardEngine.processLocation(loc2, isTripRunning = true)

        // Calibrated distance should be exactly 1.02 times standard distance
        assertEquals(stdRes.distanceIncrementM * 1.02, res.distanceIncrementM, 0.01)
    }
}
