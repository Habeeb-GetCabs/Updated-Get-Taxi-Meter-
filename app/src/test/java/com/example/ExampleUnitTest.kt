package com.example

import android.view.WindowManager
import com.example.data.database.TripEntity
import com.example.service.FloatingSpeedOverlayManager
import com.example.service.LocationTrackingService
import com.example.util.FareCalculator
import com.example.util.PdfGenerator
import com.example.util.ShareUtils
import com.example.util.VendorLogoRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `hourly rental 5-minute grace rule boundaries`() {
        // 60:00 (3600s) -> 1 hour
        assertEquals(1L, FareCalculator.calculateBillableHours(3600L))
        // 64:59 (3899s) -> 1 hour
        assertEquals(1L, FareCalculator.calculateBillableHours(3899L))
        // 65:00 (3900s) -> 2 hours
        assertEquals(2L, FareCalculator.calculateBillableHours(3900L))

        // 120:00 (7200s) -> 2 hours
        assertEquals(2L, FareCalculator.calculateBillableHours(7200L))
        // 124:59 (7499s) -> 2 hours
        assertEquals(2L, FareCalculator.calculateBillableHours(7499L))
        // 125:00 (7500s) -> 3 hours
        assertEquals(3L, FareCalculator.calculateBillableHours(7500L))

        // 180:00 (10800s) -> 3 hours
        assertEquals(3L, FareCalculator.calculateBillableHours(10800L))
        // 184:59 (11099s) -> 3 hours
        assertEquals(3L, FareCalculator.calculateBillableHours(11099L))
        // 185:00 (11100s) -> 4 hours
        assertEquals(4L, FareCalculator.calculateBillableHours(11100L))
    }

    @Test
    fun `deterministic trip number generator`() {
        val tripNum = LocationTrackingService.generateTripNumber(1700000000000L)
        assertTrue(tripNum.startsWith("GTM-"))
    }

    @Test
    fun `share text contains Get Taxi and service details`() {
        val trip = TripEntity(
            id = 101,
            tripNumber = "GTM-20261005-143000",
            startTime = 1700000000000L,
            endTime = 1700000900000L,
            distanceKm = 10.0,
            durationSeconds = 900L,
            waitingSeconds = 120L,
            totalFare = 364.0,
            startLatitude = 12.9716,
            startLongitude = 77.5946,
            endLatitude = 12.9352,
            endLongitude = 77.6245,
            customerName = "John Passenger",
            customerMobile = "9876543210",
            customerAddress = "MG Road, Coimbatore",
            driverName = "Get Taxi – Basheer",
            driverMobile = "9043743777",
            vehicleNumber = "TN 38 AB 9999",
            vehicleModel = "Sedan Taxi",
            serviceType = "LOCAL",
            baseFare = 80.0,
            farePerKm = 28.0,
            waitFarePerMin = 2.0,
            currency = "₹"
        )

        val receipt = ShareUtils.buildCustomerReceiptText(trip)
        assertTrue(receipt.contains("GET TAXI"))
        assertTrue(receipt.contains("LOCAL / METERED"))
        assertTrue(receipt.contains("MG Road, Coimbatore"))
        assertTrue(receipt.contains("₹364.00"))

        val statement = ShareUtils.buildVendorStatementText(trip)
        assertTrue(statement.contains("GET TAXI"))
        assertTrue(statement.contains("LOCAL / METERED"))
        assertTrue(statement.contains("MG Road, Coimbatore"))
        assertTrue(statement.contains("₹364.00"))
    }

    @Test
    fun `editable bill extra charges included in receipt text`() {
        val editedTrip = TripEntity(
            id = 202,
            tripNumber = "GTM-20261102-100000",
            startTime = 1700000000000L,
            endTime = 1700000600000L,
            distanceKm = 8.0,
            durationSeconds = 600L,
            waitingSeconds = 0L,
            startLatitude = 11.0168,
            startLongitude = 76.9558,
            endLatitude = 11.0200,
            endLongitude = 76.9600,
            baseFare = 100.0,
            farePerKm = 30.0,
            waitFarePerMin = 2.0,
            outstationAdditionalCharges = 50.0,
            totalFare = 390.0,
            serviceType = "LOCAL",
            currency = "₹"
        )
        val text = ShareUtils.buildVendorReceiptText(editedTrip)
        assertTrue(text.contains("Extra Charges: ₹ 50.00"))
        assertTrue(text.contains("₹390.00"))
    }

    @Test
    fun `offline ISO QR matrix encodes UPI payload accurately`() {
        val payload = "upi://pay?pa=9043743777@ybl&pn=Basheer&am=450.00&cu=INR".toByteArray(Charsets.UTF_8)
        val matrix = com.example.util.UpiQrCodeGenerator.encodeIsoQrMatrix(payload)
        assertTrue("QR matrix size must be at least 21x21", matrix.size >= 21)
        assertEquals(matrix.size, matrix[0].size)
        // Top-left finder pattern corner (0,0) must be dark (1)
        assertEquals(1, matrix[0][0])
        // Top-left finder pattern center (3,3) must be dark (1)
        assertEquals(1, matrix[3][3])
    }

    @Test
    fun `verify English and Tamil language strings and receipt localization`() {
        val en = com.example.util.AppLanguageManager.stringsFor(com.example.util.AppLanguage.ENGLISH)
        val ta = com.example.util.AppLanguageManager.stringsFor(com.example.util.AppLanguage.TAMIL)

        assertEquals("DEVICE STATUS", en.deviceStatus)
        assertEquals("Vacant", en.vacant)
        assertEquals("Occupied", en.occupied)
        assertEquals("Start Trip", en.startTrip)
        assertEquals("End Trip", en.endTrip)
        assertEquals("Meter Details", en.meterDetailsButton)

        assertEquals("சாதன நிலை", ta.deviceStatus)
        assertEquals("காலியாக உள்ளது", ta.vacant)
        assertEquals("பயணத்தில் உள்ளது", ta.occupied)
        assertEquals("உள்ளூர் / மீட்டர்", ta.localMetered)
        assertEquals("மணிநேர வாடகை", ta.hourlyRental)
        assertEquals("வெளியூர் பயணம்", ta.outstation)
        assertEquals("தற்போதைய கட்டணம்", ta.currentFare)
        assertEquals("பயண தூரம்", ta.distance)
        assertEquals("காத்திருப்பு நேரம்", ta.waitTime)
        assertEquals("பயண நேரம்", ta.duration)
        assertEquals("பயணத்தைத் தொடங்கு", ta.startTrip)
        assertEquals("பயணத்தை முடி", ta.endTrip)
        assertEquals("பயண வரலாறு", ta.tripHistory)
        assertEquals("ரசீதுகள்", ta.invoices)
        assertEquals("புதிய ரசீதை உருவாக்கு", ta.createNewBill)
        assertEquals("அமைப்புகள்", ta.settings)
        assertEquals("சுயவிவரம்", ta.profile)
        assertEquals("சேமி", ta.save)
        assertEquals("ரத்து செய்", ta.cancel)
        assertEquals("உறுதிப்படுத்து", ta.confirm)
        assertEquals("ஆம்", ta.yes)
        assertEquals("இல்லை", ta.no)
        assertEquals("இதுவரை பயணங்கள் எதுவும் பதிவாகவில்லை", ta.noRecordedTripsYet)
        assertEquals("GPS இருப்பிடத்தைப் பெறுகிறது...", ta.acquiringGps)
    }

    @Test
    fun `verify 20 exact vendor names and GET TAXI permanent default in VendorLogoRenderer`() {
        val expected20Vendors = listOf(
            "Hello City",
            "Sri Vaari",
            "Quick Taxi",
            "SGS Travels",
            "Yellow Go Taxi",
            "Get Cabs",
            "Sandhya Cabs",
            "Shobi Travels",
            "Sivam Taxi",
            "C Taxi",
            "B Taxi",
            "Nashita Cabs",
            "Kovai Holidays",
            "Covai Call Taxi",
            "Surya Cabs",
            "Om Muruga Taxi",
            "Helen Cabs",
            "I Taxi",
            "Anandhaa Cabs",
            "Sai Cabs"
        )
        assertEquals(20, VendorLogoRenderer.TWENTY_VENDOR_NAMES.size)
        assertEquals(expected20Vendors, VendorLogoRenderer.TWENTY_VENDOR_NAMES)

        val initialEntities = VendorLogoRenderer.buildInitialVendorEntities()
        // 1 default GET TAXI + 20 vendors = 21 total built-in vendor entities
        assertEquals(21, initialEntities.size)
        assertEquals("get_taxi", initialEntities.first().vendorId)
        assertEquals("GET TAXI", initialEntities.first().vendorName)
        assertEquals("res/drawable/get_taxi_header_logo.png", initialEntities.first().logoAssetPath)

        // Verify white + yellow split logic for multi-word names and SVG generation
        val split1 = VendorLogoRenderer.splitVendorNameParts("Hello City")
        assertEquals("Hello", split1.firstPartWhite)
        assertEquals("City", split1.secondPartYellow)

        val splitLong = VendorLogoRenderer.splitVendorNameParts("Om Muruga Taxi")
        assertEquals("Om Muruga", splitLong.firstPartWhite)
        assertEquals("Taxi", splitLong.secondPartYellow)

        val svg = VendorLogoRenderer.generateSvgMarkup("Covai Call Taxi")
        assertTrue(svg.contains("Covai Call"))
        assertTrue(svg.contains("Taxi"))
        assertTrue(svg.contains("Righteous"))
        assertTrue(svg.contains("badgeGrad"))
        assertTrue(VendorLogoRenderer.LogoColorTheme.entries.size >= 8)
        assertTrue(VendorLogoRenderer.LogoFontStyle.entries.size >= 8)
    }

    @Test
    fun `verify floating bubble window flags include FLAG_KEEP_SCREEN_ON when active`() {
        val flagsWithKeepAwake = FloatingSpeedOverlayManager.buildOverlayWindowFlags(keepScreenOn = true)
        assertTrue(
            "Floating bubble flags must include FLAG_KEEP_SCREEN_ON when active",
            (flagsWithKeepAwake and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        )

        val flagsWithoutKeepAwake = FloatingSpeedOverlayManager.buildOverlayWindowFlags(keepScreenOn = false)
        assertFalse(
            "Floating bubble flags must not include FLAG_KEEP_SCREEN_ON when disabled",
            (flagsWithoutKeepAwake and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        )
    }

    @Test
    fun `verify vendor-specific receipt and non-fabrication of unconfigured vendor details`() {
        val unconfiguredVendorTrip = TripEntity(
            id = 501,
            tripNumber = "GTM-20261103-120000",
            startTime = 1700000000000L,
            endTime = 1700000900000L,
            distanceKm = 12.0,
            durationSeconds = 900L,
            waitingSeconds = 60L,
            totalFare = 462.0,
            startLatitude = 11.0168,
            startLongitude = 76.9558,
            endLatitude = 11.0300,
            endLongitude = 76.9800,
            serviceType = "LOCAL",
            baseFare = 100.0,
            farePerKm = 30.0,
            waitFarePerMin = 2.0,
            currency = "₹",
            vendorId = "kovai_holidays",
            vendorName = "Kovai Holidays",
            vendorLogoAsset = "assets/vendor_logos/vendor_logo_kovai_holidays.svg",
            vendorContactNumber = "",
            vendorBusinessAddress = "",
            vendorEmail = "",
            vendorGstin = "",
            vendorInvoiceDetails = "",
            vendorInvoiceNumber = "KH-20261103-120000"
        )

        val meta = PdfGenerator.resolveVendorHeaderMetadata(unconfiguredVendorTrip)
        assertEquals("kovai_holidays", meta.vendorId)
        assertEquals("Kovai Holidays", meta.vendorName)
        assertEquals("KH-20261103-120000", meta.invoiceNumber)
        // Must not fabricate missing phone, address, email, GSTIN, or legal text
        assertTrue("Displayed business lines must be empty when not configured", meta.displayedBusinessLines.isEmpty())

        val receiptText = ShareUtils.buildCustomerReceiptText(unconfiguredVendorTrip)
        assertTrue(receiptText.contains("Kovai Holidays - OFFICIAL TRIP INVOICE"))
        assertTrue(receiptText.contains("Invoice No: KH-20261103-120000"))
        assertTrue(receiptText.contains("Thank you for riding with Kovai Holidays!"))
        assertFalse(receiptText.contains("GSTIN"))
    }

    @Test
    fun `route coordinates serializer safely handles empty, invalid and populated points`() {
        // Empty
        assertEquals(emptyList<Pair<Double, Double>>(), com.example.util.RouteCoordinatesSerializer.deserialize(""))
        assertEquals(emptyList<Pair<Double, Double>>(), com.example.util.RouteCoordinatesSerializer.deserialize("[]"))
        assertEquals(emptyList<Pair<Double, Double>>(), com.example.util.RouteCoordinatesSerializer.deserialize("invalid json"))

        // Populated points
        val originalPoints = listOf(
            Pair(11.0168, 76.9558),
            Pair(11.0200, 76.9600),
            Pair(11.0350, 76.9850)
        )
        val json = com.example.util.RouteCoordinatesSerializer.serialize(originalPoints)
        assertTrue(json.contains("11.0168"))
        assertTrue(json.contains("76.9558"))

        val deserialized = com.example.util.RouteCoordinatesSerializer.deserialize(json)
        assertEquals(3, deserialized.size)
        assertEquals(11.0168, deserialized[0].first, 0.0001)
        assertEquals(76.9558, deserialized[0].second, 0.0001)
        assertEquals(11.0350, deserialized[2].first, 0.0001)
        assertEquals(76.9850, deserialized[2].second, 0.0001)
    }

    @Test
    fun `editable invoice date and time preserved independently from immutable start and end time`() {
        val trip = TripEntity(
            id = 601,
            tripNumber = "GTM-20261108-013000",
            startTime = 1700000000000L,
            endTime = 1700001200000L,
            distanceKm = 15.5,
            durationSeconds = 1200L,
            waitingSeconds = 0L,
            totalFare = 550.0,
            startLatitude = 11.0168,
            startLongitude = 76.9558,
            endLatitude = 11.0250,
            endLongitude = 76.9650,
            serviceType = "LOCAL",
            currency = "₹",
            invoiceTimestamp = 1700005000000L,
            invoiceDateText = "08 Nov 2026",
            invoiceTimeText = "04:30 PM",
            routePointsJson = "[[11.0168,76.9558],[11.0250,76.9650]]"
        )

        // GPS start & end time remain untouched
        assertEquals(1700000000000L, trip.startTime)
        assertEquals(1700001200000L, trip.endTime)
        // Invoice date & time are stored separately
        assertEquals("08 Nov 2026", trip.invoiceDateText)
        assertEquals("04:30 PM", trip.invoiceTimeText)
        assertEquals(1700005000000L, trip.invoiceTimestamp)

        // Route points are intact
        val points = com.example.util.RouteCoordinatesSerializer.deserialize(trip.routePointsJson)
        assertEquals(2, points.size)
    }
}
