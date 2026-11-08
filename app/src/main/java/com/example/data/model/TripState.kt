package com.example.data.model

data class TripState(
    val status: TripStatus = TripStatus.IDLE,
    val distanceKm: Double = 0.0,
    val durationSeconds: Long = 0L,
    val waitingSeconds: Long = 0L,
    val currentFare: Double = 0.0,
    val speedKmH: Float = 0f,
    val isMoving: Boolean = false,
    val isWaitingPaused: Boolean = false,
    val baseFare: Double = 0.0,
    val farePerKm: Double = 0.0,
    val waitFarePerMin: Double = 0.0,
    val currency: String = "₹",
    val speedThreshold: Double = 5.0,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isSimulationEnabled: Boolean = false,
    val isOutOfCity: Boolean = false,
    val outOfCitySurchargePercent: Double = 0.0,
    val outOfCitySurchargeAmount: Double = 0.0,
    val pickupAddress: String = "",
    val dropAddress: String = "",
    val tripNumber: String = "",
    val customerName: String = "",
    val customerMobile: String = "",
    val customerAddress: String = "",
    val serviceType: String = "LOCAL",
    // Hourly Rental
    val rentalHourlyRate: Double = 0.0,
    val rentalMinHours: Double = 1.0,
    val rentalIncludedKmPerHour: Double = 10.0,
    val rentalExtraHrRate: Double = 0.0,
    val rentalExtraKmRate: Double = 0.0,
    val billableHours: Long = 1L,
    val rentalTotalIncludedKm: Double = 10.0,
    val rentalExtraDistanceKm: Double = 0.0,
    val rentalHourlyCharge: Double = 0.0,
    val rentalExtraDistanceCharge: Double = 0.0,
    // Outstation
    val outstationRatePerKm: Double = 0.0,
    val outstationMinFare: Double = 0.0,
    val outstationWaitRate: Double = 0.0,
    val outstationDriverBata: Double = 0.0,
    val outstationAdditionalCharges: Double = 0.0,
    val extraChargeItems: List<ExtraChargeItem> = emptyList(),
    val liveExtraChargesTotal: Double = 0.0,
    val routePoints: List<Pair<Double, Double>> = emptyList(),
    // Per-trip locked vendor branding & billing identity
    val vendorId: String = "get_taxi",
    val vendorName: String = "GET TAXI",
    val vendorLogoAsset: String = "drawable/get_taxi_header_logo",
    val vendorContactNumber: String = "",
    val vendorBusinessAddress: String = "",
    val vendorEmail: String = "",
    val vendorGstin: String = "",
    val vendorInvoiceDetails: String = ""
)

enum class TripStatus {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

enum class ActiveTripDisplayMode {
    MAP_FIRST,
    METER_DETAILS
}
