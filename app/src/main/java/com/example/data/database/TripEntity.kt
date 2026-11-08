package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tripNumber: String = "",
    val startTime: Long,
    val endTime: Long,
    val distanceKm: Double,
    val durationSeconds: Long,
    val waitingSeconds: Long,
    val totalFare: Double,
    val startLatitude: Double,
    val startLongitude: Double,
    val endLatitude: Double,
    val endLongitude: Double,
    val customerName: String = "",
    val customerMobile: String = "",
    val customerAddress: String = "",
    val driverName: String = "",
    val driverMobile: String = "",
    val vehicleNumber: String = "",
    val vehicleModel: String = "",
    val vehicleType: String = "",
    val passengerNotes: String = "",
    val pickupAddress: String = "",
    val dropAddress: String = "",
    val isOutOfCity: Boolean = false,
    val outOfCitySurcharge: Double = 0.0,
    val baseFare: Double = 0.0,
    val farePerKm: Double = 0.0,
    val waitFarePerMin: Double = 0.0,
    val currency: String = "₹",
    val serviceType: String = "LOCAL",
    // Hourly rental snapshot
    val rentalBaseHours: Double = 1.0,
    val rentalBaseKm: Double = 0.0,
    val rentalBaseFare: Double = 0.0,
    val rentalExtraKmRate: Double = 0.0,
    val rentalExtraHrRate: Double = 0.0,
    val billableHours: Long = 1L,
    // Outstation snapshot
    val outstationRatePerKm: Double = 0.0,
    val outstationMinKm: Double = 0.0,
    val outstationDriverBata: Double = 0.0,
    val outstationMinFare: Double = 0.0,
    val outstationWaitRate: Double = 0.0,
    val outstationAdditionalCharges: Double = 0.0,
    val extraChargesJson: String = "",
    // Actual GPS recorded journey points (for offline PDF route map)
    val routePointsJson: String = "",
    val routeMapImagePath: String = "",
    // Editable invoice date and time (immutable actual GPS timestamps remain in startTime & endTime)
    val invoiceTimestamp: Long = 0L,
    val invoiceDateText: String = "",
    val invoiceTimeText: String = "",
    // Immutable per-trip vendor identity snapshot (Historical Invoice Protection)
    val vendorId: String = "get_taxi",
    val vendorName: String = "GET TAXI",
    val vendorLogoAsset: String = "drawable/get_taxi_header_logo",
    val vendorContactNumber: String = "",
    val vendorBusinessAddress: String = "",
    val vendorEmail: String = "",
    val vendorGstin: String = "",
    val vendorInvoiceDetails: String = "",
    val vendorInvoiceNumber: String = ""
)

@Entity(tableName = "driver_profile")
data class DriverProfileEntity(
    @PrimaryKey val id: Int = 1,
    val driverName: String = "",
    val driverMobile: String = "",
    val vehicleNumber: String = "",
    val vehicleModel: String = "",
    val vehicleType: String = "Sedan Taxi",
    val driverId: String = "",
    val photoUri: String? = null,
    val qrCodeUri: String? = null,
    val upiId: String = ""
)

@Entity(tableName = "active_trip")
data class ActiveTripRecord(
    @PrimaryKey val id: Int = 1,
    val startTime: Long,
    val distanceKm: Double,
    val durationSeconds: Long,
    val waitingSeconds: Long,
    val currentFare: Double,
    val isPaused: Boolean,
    val baseFare: Double,
    val farePerKm: Double,
    val waitFarePerMin: Double,
    val currency: String,
    val isOutOfCity: Boolean = false,
    val pickupAddress: String = "",
    val tripNumber: String = "",
    val serviceType: String = "LOCAL",
    val rentalBaseHours: Double = 1.0,
    val rentalBaseKm: Double = 0.0,
    val rentalBaseFare: Double = 0.0,
    val rentalExtraKmRate: Double = 0.0,
    val rentalExtraHrRate: Double = 0.0,
    val billableHours: Long = 1L,
    val outstationRatePerKm: Double = 0.0,
    val outstationMinKm: Double = 0.0,
    val outstationDriverBata: Double = 0.0,
    val outstationMinFare: Double = 0.0,
    val outstationWaitRate: Double = 0.0,
    val outstationAdditionalCharges: Double = 0.0,
    val extraChargesJson: String = "",
    val routePointsJson: String = "",
    // Locked per-trip vendor snapshot
    val vendorId: String = "get_taxi",
    val vendorName: String = "GET TAXI",
    val vendorLogoAsset: String = "drawable/get_taxi_header_logo",
    val vendorContactNumber: String = "",
    val vendorBusinessAddress: String = "",
    val vendorEmail: String = "",
    val vendorGstin: String = "",
    val vendorInvoiceDetails: String = ""
)

@Entity(tableName = "vendor_profiles")
data class VendorProfileEntity(
    @PrimaryKey val vendorId: String,
    val vendorName: String,
    val logoAsset: String,
    val contactNumber: String = "",
    val businessAddress: String = "",
    val email: String = "",
    val gstin: String = "",
    val invoiceDisplayDetails: String = "",
    val isActive: Boolean = true,
    val isOriginalGetTaxi: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val logoAssetPath: String
        get() = logoAsset

    val isBuiltIn: Boolean
        get() = isOriginalGetTaxi || sortOrder in 1..20
}
