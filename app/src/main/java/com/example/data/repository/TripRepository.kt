package com.example.data.repository

import com.example.data.database.ActiveTripRecord
import com.example.data.database.DriverProfileEntity
import com.example.data.database.TripDao
import com.example.data.database.TripEntity
import com.example.data.database.VendorProfileEntity
import com.example.util.VendorLogoRenderer
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TripRepository(private val tripDao: TripDao) {

    val allTrips: Flow<List<TripEntity>> = tripDao.getAllTrips()

    val driverProfile: Flow<DriverProfileEntity?> = tripDao.getDriverProfileFlow()

    val allVendors: Flow<List<VendorProfileEntity>> = tripDao.getAllVendorsFlow()

    val activeVendors: Flow<List<VendorProfileEntity>> = tripDao.getActiveVendorsFlow()

    suspend fun ensureVendorsSeeded() {
        val existing = tripDao.getAllVendors()
        if (existing.size < 21) {
            tripDao.insertVendorsIgnore(VendorLogoRenderer.buildInitialSeedVendors())
        }
    }

    suspend fun getAllVendorsList(): List<VendorProfileEntity> {
        ensureVendorsSeeded()
        return tripDao.getAllVendors()
    }

    suspend fun getActiveVendorsList(): List<VendorProfileEntity> {
        ensureVendorsSeeded()
        return tripDao.getActiveVendors()
    }

    suspend fun getVendorById(vendorId: String): VendorProfileEntity? {
        ensureVendorsSeeded()
        return tripDao.getVendorById(vendorId)
    }

    suspend fun upsertVendor(vendor: VendorProfileEntity) {
        // Protect original GET TAXI identity & active status
        val safeVendor = if (vendor.vendorId == VendorLogoRenderer.GET_TAXI_VENDOR_ID || vendor.isOriginalGetTaxi) {
            vendor.copy(
                vendorId = VendorLogoRenderer.GET_TAXI_VENDOR_ID,
                vendorName = VendorLogoRenderer.GET_TAXI_VENDOR_NAME,
                logoAsset = VendorLogoRenderer.GET_TAXI_LOGO_ASSET,
                isActive = true,
                isOriginalGetTaxi = true,
                sortOrder = 0
            )
        } else {
            vendor
        }
        tripDao.upsertVendor(safeVendor)
    }

    suspend fun setVendorActiveStatus(vendorId: String, isActive: Boolean) {
        if (vendorId == VendorLogoRenderer.GET_TAXI_VENDOR_ID) return
        tripDao.setVendorActiveStatus(vendorId, isActive)
    }

    suspend fun getTripById(id: Int): TripEntity? {
        return tripDao.getTripById(id)
    }

    fun getTripByIdFlow(id: Int): Flow<TripEntity?> {
        return tripDao.getTripByIdFlow(id)
    }

    suspend fun insertTrip(trip: TripEntity): Long {
        return tripDao.insertTrip(trip)
    }

    suspend fun updateTrip(trip: TripEntity) {
        tripDao.updateTrip(trip)
    }

    suspend fun deleteTripById(id: Int) {
        tripDao.deleteTripById(id)
    }

    suspend fun getActiveTrip(): ActiveTripRecord? {
        return tripDao.getActiveTrip()
    }

    suspend fun saveActiveTrip(activeTrip: ActiveTripRecord) {
        tripDao.saveActiveTrip(activeTrip)
    }

    suspend fun clearActiveTrip() {
        tripDao.clearActiveTrip()
    }

    suspend fun getDriverProfile(): DriverProfileEntity? {
        return tripDao.getDriverProfile()
    }

    suspend fun saveDriverProfile(profile: DriverProfileEntity) {
        tripDao.saveDriverProfile(profile)
    }

    suspend fun generateDeterministicTripNumber(timestamp: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000L)

        var count = tripDao.getDailyTripCount(startOfDay, endOfDay)
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(timestamp))
        var candidate = String.format(Locale.US, "GTM-%s-%04d", dateStr, count + 1)

        while (tripDao.getTripByTripNumber(candidate) != null) {
            count++
            candidate = String.format(Locale.US, "GTM-%s-%04d", dateStr, count + 1)
        }
        return candidate
    }
}
