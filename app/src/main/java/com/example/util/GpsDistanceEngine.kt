package com.example.util

import android.location.Location

/**
 * High-accuracy, outlier-protected GPS Distance Engine for Get Taxi.
 *
 * Implements:
 * 1. Horizontal accuracy filtering (rejects noisy multipath / cell-tower samples).
 * 2. Stationary jitter suppression (suppresses micro-flutter when stopped or creeping below threshold).
 * 3. Outlier / teleportation jump protection (rejects impossible speeds > 220 km/h).
 * 4. Temporal gap protection (does NOT interpolate or fabricate distance across long GPS gaps > 30s).
 * 5. Deterministic distance accumulation with configurable calibration factor.
 * 6. Restoration of accumulated distance without reset or double-counting.
 * 7. Lightweight diagnostic counters for audit tracking.
 */
class GpsDistanceEngine(
    initialDistanceKm: Double = 0.0,
    var calibrationFactor: Double = 1.0,
    var speedThresholdKmH: Double = 5.0,
    private val maxHorizontalAccuracyMeters: Float = 35.0f,
    private val maxSpeedKmH: Double = 220.0,
    private val maxGpsGapSeconds: Double = 30.0
) {
    var accumulatedDistanceKm: Double = initialDistanceKm
        private set

    var lastValidLocation: Location? = null
        private set

    var lastValidTimestamp: Long = 0L
        private set

    var totalGapsDetected: Int = 0
        private set

    var totalOutliersRejected: Int = 0
        private set

    var totalInaccurateSamplesRejected: Int = 0
        private set

    private var stationaryAnchor: Location? = null

    data class ProcessResult(
        val distanceIncrementM: Double,
        val totalDistanceKm: Double,
        val speedKmH: Float,
        val isMoving: Boolean,
        val accepted: Boolean,
        val rejectionReason: String? = null,
        val gapDetected: Boolean = false
    )

    fun reset(newDistanceKm: Double = 0.0) {
        accumulatedDistanceKm = newDistanceKm
        lastValidLocation = null
        lastValidTimestamp = 0L
        stationaryAnchor = null
    }

    fun restoreDistance(distanceKm: Double) {
        accumulatedDistanceKm = distanceKm
    }

    /**
     * Processes an incoming raw GPS location.
     * Returns a [ProcessResult] detailing the decision and any distance increment added.
     */
    fun processLocation(location: Location, isTripRunning: Boolean = true): ProcessResult {
        // 1. If trip is not running, do not accumulate distance
        if (!isTripRunning) {
            lastValidLocation = location
            lastValidTimestamp = location.time
            stationaryAnchor = location
            return ProcessResult(
                distanceIncrementM = 0.0,
                totalDistanceKm = accumulatedDistanceKm,
                speedKmH = 0f,
                isMoving = false,
                accepted = false,
                rejectionReason = "Trip not running"
            )
        }

        // 2. Reject location with non-advancing or duplicate timestamp
        val prev = lastValidLocation
        if (prev != null && location.time <= prev.time) {
            return ProcessResult(
                distanceIncrementM = 0.0,
                totalDistanceKm = accumulatedDistanceKm,
                speedKmH = 0f,
                isMoving = false,
                accepted = false,
                rejectionReason = "Non-advancing timestamp"
            )
        }

        // 3. Reject location with poor horizontal accuracy (> 35m)
        if (location.hasAccuracy() && location.accuracy > maxHorizontalAccuracyMeters) {
            totalInaccurateSamplesRejected++
            return ProcessResult(
                distanceIncrementM = 0.0,
                totalDistanceKm = accumulatedDistanceKm,
                speedKmH = 0f,
                isMoving = false,
                accepted = false,
                rejectionReason = "Inaccurate sample (${location.accuracy}m > ${maxHorizontalAccuracyMeters}m)"
            )
        }

        // 4. Initial point setup
        if (prev == null) {
            lastValidLocation = location
            lastValidTimestamp = location.time
            stationaryAnchor = location
            val speed = if (location.hasSpeed()) location.speed * 3.6f else 0f
            return ProcessResult(
                distanceIncrementM = 0.0,
                totalDistanceKm = accumulatedDistanceKm,
                speedKmH = speed,
                isMoving = speed >= speedThresholdKmH,
                accepted = true
            )
        }

        val deltaDistanceM = location.distanceTo(prev)
        val timeDeltaS = (location.time - prev.time) / 1000.0

        // 5. Impossible speed jump protection (GPS multipath glitch)
        if (timeDeltaS > 0.1) {
            val calcSpeedKmh = (deltaDistanceM / timeDeltaS) * 3.6
            if (calcSpeedKmh > maxSpeedKmH) {
                totalOutliersRejected++
                return ProcessResult(
                    distanceIncrementM = 0.0,
                    totalDistanceKm = accumulatedDistanceKm,
                    speedKmH = 0f,
                    isMoving = false,
                    accepted = false,
                    rejectionReason = "Speed jump outlier (${calcSpeedKmh.toInt()} km/h > $maxSpeedKmH km/h)"
                )
            }
        }

        // 6. Long GPS gap handling (tunnels, loss of lock, background gap)
        // Critical requirement: Do not fabricate missing distance across long gap!
        if (timeDeltaS > maxGpsGapSeconds) {
            totalGapsDetected++
            lastValidLocation = location
            lastValidTimestamp = location.time
            stationaryAnchor = location
            val speed = if (location.hasSpeed()) location.speed * 3.6f else 0f
            return ProcessResult(
                distanceIncrementM = 0.0,
                totalDistanceKm = accumulatedDistanceKm,
                speedKmH = speed,
                isMoving = speed >= speedThresholdKmH,
                accepted = true,
                rejectionReason = "GPS gap of ${timeDeltaS.toInt()}s detected: resuming without interpolating",
                gapDetected = true
            )
        }

        // 7. Calculate Speed
        val speedKmh = if (location.hasSpeed()) {
            location.speed * 3.6f
        } else if (timeDeltaS > 0.0) {
            ((deltaDistanceM / timeDeltaS) * 3.6).toFloat()
        } else {
            0f
        }

        val isMoving = speedKmh >= speedThresholdKmH

        // 8. Stationary Jitter Suppression:
        // When stationary (< speedThresholdKmH), suppress satellite drift flutter
        if (!isMoving) {
            val anchor = stationaryAnchor ?: prev
            val distFromAnchor = location.distanceTo(anchor)
            if (distFromAnchor < 4.0) {
                // Stationary noise: do not accumulate distance, do not advance anchor
                lastValidLocation = location
                lastValidTimestamp = location.time
                return ProcessResult(
                    distanceIncrementM = 0.0,
                    totalDistanceKm = accumulatedDistanceKm,
                    speedKmH = speedKmh,
                    isMoving = false,
                    accepted = true
                )
            }
        }

        // Vehicle is moving: update stationary anchor
        stationaryAnchor = location

        // 9. Accumulate distance with configurable calibration factor
        val factor = calibrationFactor.coerceIn(0.90, 1.15)
        val calibratedIncrementM = deltaDistanceM.toDouble() * factor
        val incrementKm = calibratedIncrementM / 1000.0

        accumulatedDistanceKm += incrementKm
        lastValidLocation = location
        lastValidTimestamp = location.time

        return ProcessResult(
            distanceIncrementM = calibratedIncrementM,
            totalDistanceKm = accumulatedDistanceKm,
            speedKmH = speedKmh,
            isMoving = true,
            accepted = true
        )
    }
}
