package com.example

import com.example.util.FareCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class FareCalculatorTest {

    private val hourlyRate = 350.0
    private val includedKmPerHour = 10.0
    private val extraKmRate = 20.0

    @Test
    fun testBillableHours_graceRuleBoundaries() {
        // 0 to 60 minutes -> 1 billable hour
        assertEquals(1L, FareCalculator.calculateBillableHours(0L))
        assertEquals(1L, FareCalculator.calculateBillableHours(30 * 60L)) // 30 min
        assertEquals(1L, FareCalculator.calculateBillableHours(58 * 60L)) // 58 min
        assertEquals(1L, FareCalculator.calculateBillableHours(60 * 60L)) // 60:00 (3600s)

        // 60:00 to 64:59 (3899s) -> still 1 billable hour (5-min grace period)
        assertEquals(1L, FareCalculator.calculateBillableHours(64 * 60L + 59L)) // 64:59

        // 65:00 (3900s) -> crosses grace threshold into 2 billable hours
        assertEquals(2L, FareCalculator.calculateBillableHours(65 * 60L)) // 65:00 (3900s)

        // 120:00 to 124:59 -> 2 billable hours
        assertEquals(2L, FareCalculator.calculateBillableHours(120 * 60L)) // 120:00 (7200s)
        assertEquals(2L, FareCalculator.calculateBillableHours(124 * 60L + 59L)) // 124:59 (7499s)

        // 125:00 (7500s) -> crosses into 3 billable hours
        assertEquals(3L, FareCalculator.calculateBillableHours(125 * 60L)) // 125:00 (7500s)

        // 180:00 to 184:59 -> 3 billable hours
        assertEquals(3L, FareCalculator.calculateBillableHours(180 * 60L)) // 180:00 (10800s)
        assertEquals(3L, FareCalculator.calculateBillableHours(184 * 60L + 59L)) // 184:59 (11099s)

        // 185:00 (11100s) -> 4 billable hours
        assertEquals(4L, FareCalculator.calculateBillableHours(185 * 60L)) // 185:00 (11100s)
    }

    @Test
    fun testHourlyRental_Test1_30min_5km() {
        // TEST 1: Duration = 30 min, Distance = 5 km
        // Billable: 1 hr, Included: 10 km, Extra: 0 km, Hourly: ₹350, Extra dist: ₹0, Total: ₹350
        val result = FareCalculator.calculateHourlyRentalFare(
            durationSeconds = 30 * 60L,
            distanceKm = 5.0,
            hourlyRate = hourlyRate,
            includedKmPerHour = includedKmPerHour,
            extraKmRate = extraKmRate
        )

        assertEquals(1L, result.billableHours)
        assertEquals(10.0, result.totalIncludedKm, 0.001)
        assertEquals(0.0, result.extraDistanceKm, 0.001)
        assertEquals(350.0, result.hourlyCharge, 0.001)
        assertEquals(0.0, result.extraDistanceCharge, 0.001)
        assertEquals(350.0, result.totalFare, 0.001)
    }

    @Test
    fun testHourlyRental_Test2_58min_15km() {
        // TEST 2: Duration = 58 min, Distance = 15 km
        // Billable: 1 hr, Included: 10 km, Extra: 5 km, Hourly: ₹350, Extra dist: ₹100, Total: ₹450
        val result = FareCalculator.calculateHourlyRentalFare(
            durationSeconds = 58 * 60L,
            distanceKm = 15.0,
            hourlyRate = hourlyRate,
            includedKmPerHour = includedKmPerHour,
            extraKmRate = extraKmRate
        )

        assertEquals(1L, result.billableHours)
        assertEquals(10.0, result.totalIncludedKm, 0.001)
        assertEquals(5.0, result.extraDistanceKm, 0.001)
        assertEquals(350.0, result.hourlyCharge, 0.001)
        assertEquals(100.0, result.extraDistanceCharge, 0.001)
        assertEquals(450.0, result.totalFare, 0.001)
    }

    @Test
    fun testHourlyRental_Test3_64min_15km() {
        // TEST 3: Duration = 64 min, Distance = 15 km
        // Billable: 1 hr, Included: 10 km, Extra: 5 km, Hourly: ₹350, Extra dist: ₹100, Total: ₹450
        val result = FareCalculator.calculateHourlyRentalFare(
            durationSeconds = 64 * 60L,
            distanceKm = 15.0,
            hourlyRate = hourlyRate,
            includedKmPerHour = includedKmPerHour,
            extraKmRate = extraKmRate
        )

        assertEquals(1L, result.billableHours)
        assertEquals(10.0, result.totalIncludedKm, 0.001)
        assertEquals(5.0, result.extraDistanceKm, 0.001)
        assertEquals(350.0, result.hourlyCharge, 0.001)
        assertEquals(100.0, result.extraDistanceCharge, 0.001)
        assertEquals(450.0, result.totalFare, 0.001)
    }

    @Test
    fun testHourlyRental_Test4_65min_15km() {
        // TEST 4: Duration = exactly 65 min, Distance = 15 km
        // Billable: 2 hrs, Included: 20 km, Extra: 0 km, Hourly: ₹700, Extra dist: ₹0, Total: ₹700
        val result = FareCalculator.calculateHourlyRentalFare(
            durationSeconds = 65 * 60L,
            distanceKm = 15.0,
            hourlyRate = hourlyRate,
            includedKmPerHour = includedKmPerHour,
            extraKmRate = extraKmRate
        )

        assertEquals(2L, result.billableHours)
        assertEquals(20.0, result.totalIncludedKm, 0.001)
        assertEquals(0.0, result.extraDistanceKm, 0.001)
        assertEquals(700.0, result.hourlyCharge, 0.001)
        assertEquals(0.0, result.extraDistanceCharge, 0.001)
        assertEquals(700.0, result.totalFare, 0.001)
    }

    @Test
    fun testHourlyRental_Test5_65min_25km() {
        // TEST 5: Duration = 65 min, Distance = 25 km
        // Billable: 2 hrs, Included: 20 km, Extra: 5 km, Hourly: ₹700, Extra dist: ₹100, Total: ₹800
        val result = FareCalculator.calculateHourlyRentalFare(
            durationSeconds = 65 * 60L,
            distanceKm = 25.0,
            hourlyRate = hourlyRate,
            includedKmPerHour = includedKmPerHour,
            extraKmRate = extraKmRate
        )

        assertEquals(2L, result.billableHours)
        assertEquals(20.0, result.totalIncludedKm, 0.001)
        assertEquals(5.0, result.extraDistanceKm, 0.001)
        assertEquals(700.0, result.hourlyCharge, 0.001)
        assertEquals(100.0, result.extraDistanceCharge, 0.001)
        assertEquals(800.0, result.totalFare, 0.001)
    }

    @Test
    fun testHourlyRental_Test6_124min_25km() {
        // TEST 6: Duration = 124 min, Distance = 25 km
        // Billable: 2 hrs, Included: 20 km, Extra: 5 km, Hourly: ₹700, Extra dist: ₹100, Total: ₹800
        val result = FareCalculator.calculateHourlyRentalFare(
            durationSeconds = 124 * 60L,
            distanceKm = 25.0,
            hourlyRate = hourlyRate,
            includedKmPerHour = includedKmPerHour,
            extraKmRate = extraKmRate
        )

        assertEquals(2L, result.billableHours)
        assertEquals(20.0, result.totalIncludedKm, 0.001)
        assertEquals(5.0, result.extraDistanceKm, 0.001)
        assertEquals(700.0, result.hourlyCharge, 0.001)
        assertEquals(100.0, result.extraDistanceCharge, 0.001)
        assertEquals(800.0, result.totalFare, 0.001)
    }

    @Test
    fun testHourlyRental_Test7_125min_25km() {
        // TEST 7: Duration = exactly 125 min, Distance = 25 km
        // Billable: 3 hrs, Included: 30 km, Extra: 0 km, Hourly: ₹1,050, Extra dist: ₹0, Total: ₹1,050
        val result = FareCalculator.calculateHourlyRentalFare(
            durationSeconds = 125 * 60L,
            distanceKm = 25.0,
            hourlyRate = hourlyRate,
            includedKmPerHour = includedKmPerHour,
            extraKmRate = extraKmRate
        )

        assertEquals(3L, result.billableHours)
        assertEquals(30.0, result.totalIncludedKm, 0.001)
        assertEquals(0.0, result.extraDistanceKm, 0.001)
        assertEquals(1050.0, result.hourlyCharge, 0.001)
        assertEquals(0.0, result.extraDistanceCharge, 0.001)
        assertEquals(1050.0, result.totalFare, 0.001)
    }

    @Test
    fun testHourlyRental_BugScenario_58min_15_19km() {
        // Real-world scenario from bug report:
        // Duration: 58 minutes, Distance: 15.19 km
        // Tariff: ₹350/hr, 10 km/hr included, ₹20/km extra
        // Result: Billable: 1 hr, Included: 10 km, Extra: 5.19 km, Hourly: ₹350, Extra dist charge: ₹103.80, Total: ₹453.80
        val result = FareCalculator.calculateHourlyRentalFare(
            durationSeconds = 58 * 60L,
            distanceKm = 15.19,
            hourlyRate = 350.0,
            includedKmPerHour = 10.0,
            extraKmRate = 20.0
        )

        assertEquals(1L, result.billableHours)
        assertEquals(10.0, result.totalIncludedKm, 0.001)
        assertEquals(5.19, result.extraDistanceKm, 0.001)
        assertEquals(350.0, result.hourlyCharge, 0.001)
        assertEquals(103.80, result.extraDistanceCharge, 0.001)
        assertEquals(453.80, result.totalFare, 0.001)
    }

    @Test
    fun testExtraChargeItem_serializeAndDeserializeAndLiveFare() {
        val charges = listOf(
            com.example.data.model.ExtraChargeItem(id = "c1", label = "Toll Gate", amount = 65.0),
            com.example.data.model.ExtraChargeItem(id = "c2", label = "Airport Parking", amount = 40.0)
        )
        val encoded = com.example.data.model.ExtraChargeItem.serialize(charges)
        val decoded = com.example.data.model.ExtraChargeItem.deserialize(encoded)

        assertEquals(2, decoded.size)
        assertEquals("Toll Gate", decoded[0].label)
        assertEquals(65.0, decoded[0].amount, 0.001)
        assertEquals("Airport Parking", decoded[1].label)
        assertEquals(40.0, decoded[1].amount, 0.001)
        assertEquals(105.0, com.example.data.model.ExtraChargeItem.totalAmount(decoded), 0.001)

        // Verify live meter fare includes extra charges immediately
        val initialState = com.example.data.model.TripState(
            status = com.example.data.model.TripStatus.RUNNING,
            baseFare = 100.0,
            distanceKm = 2.0,
            farePerKm = 15.0,
            extraChargeItems = decoded
        )
        val updatedState = com.example.service.LocationTrackingService.computeUpdatedStateWithFare(initialState)
        assertEquals(105.0, updatedState.liveExtraChargesTotal, 0.001)
        // Base 100 + Dist (2 * 15 = 30) + Extra (105) = 235.0
        assertEquals(235.0, updatedState.currentFare, 0.001)
    }
}
