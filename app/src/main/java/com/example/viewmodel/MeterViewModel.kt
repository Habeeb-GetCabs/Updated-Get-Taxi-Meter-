package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.DriverProfileEntity
import com.example.data.database.TripDatabase
import com.example.data.database.TripEntity
import com.example.data.database.VendorProfileEntity
import com.example.data.model.ActiveTripDisplayMode
import com.example.data.model.ServiceType
import com.example.data.model.TripState
import com.example.data.model.TripStatus
import com.example.data.preferences.SettingsRepository
import com.example.data.repository.TripRepository
import com.example.service.LocationTrackingService
import com.example.util.VendorLogoRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeterViewModel(application: Application) : AndroidViewModel(application) {

    private val tripRepository: TripRepository
    private val settingsRepository: SettingsRepository

    val tripState: StateFlow<TripState> = LocationTrackingService.tripState

    val allTrips: StateFlow<List<TripEntity>>
    val allVendors: StateFlow<List<VendorProfileEntity>>
    val activeVendors: StateFlow<List<VendorProfileEntity>>

    private val _selectedServiceType = MutableStateFlow(ServiceType.LOCAL)
    val selectedServiceType: StateFlow<ServiceType> = _selectedServiceType.asStateFlow()

    /**
     * Per-trip vendor selection before starting a trip.
     * Defaults to GET TAXI and resets per trip (never locks the driver permanently into one vendor).
     */
    private val _selectedVendor = MutableStateFlow(VendorLogoRenderer.defaultGetTaxiProfile())
    val selectedVendor: StateFlow<VendorProfileEntity> = _selectedVendor.asStateFlow()

    /**
     * Controls whether the active trip screen displays the enlarged live centre-console map
     * (`MAP_FIRST`, automatically opened when starting a trip) or the full running meter breakdown
     * (`METER_DETAILS`). Switching modes never restarts the trip, resets fare, or interrupts GPS.
     */
    private val _activeTripDisplayMode = MutableStateFlow(ActiveTripDisplayMode.MAP_FIRST)
    val activeTripDisplayMode: StateFlow<ActiveTripDisplayMode> = _activeTripDisplayMode.asStateFlow()

    private val _hasActiveSessionBackup = MutableStateFlow(false)
    val hasActiveSessionBackup: StateFlow<Boolean> = _hasActiveSessionBackup.asStateFlow()

    private val _driverProfile = MutableStateFlow<DriverProfileEntity?>(null)
    val driverProfile: StateFlow<DriverProfileEntity?> = _driverProfile.asStateFlow()

    private val _tariffValidationMessage = MutableStateFlow<String?>(null)
    val tariffValidationMessage: StateFlow<String?> = _tariffValidationMessage.asStateFlow()

    init {
        val database = TripDatabase.getDatabase(application)
        tripRepository = TripRepository(database.tripDao())
        settingsRepository = SettingsRepository(application)

        allTrips = tripRepository.allTrips
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allVendors = tripRepository.allVendors
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                VendorLogoRenderer.buildInitialVendorEntities()
            )

        activeVendors = tripRepository.activeVendors
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                VendorLogoRenderer.buildInitialVendorEntities()
            )

        viewModelScope.launch {
            tripRepository.ensureVendorsSeeded()
            refreshDefaultSelectedVendor()
        }

        checkForActiveBackup()
        loadDriverProfile()
    }

    private suspend fun refreshDefaultSelectedVendor() {
        val currentStatus = tripState.value.status
        if (currentStatus == TripStatus.IDLE || currentStatus == TripStatus.FINISHED) {
            val latestSelected = tripRepository.getVendorById(_selectedVendor.value.vendorId)
                ?: tripRepository.getVendorById(VendorLogoRenderer.DEFAULT_VENDOR_ID)
                ?: VendorLogoRenderer.defaultGetTaxiProfile()
            _selectedVendor.value = if (latestSelected.isActive) {
                latestSelected
            } else {
                tripRepository.getVendorById(VendorLogoRenderer.DEFAULT_VENDOR_ID)
                    ?: VendorLogoRenderer.defaultGetTaxiProfile()
            }
        }
    }

    fun setServiceType(type: ServiceType) {
        val status = tripState.value.status
        if (status == TripStatus.RUNNING || status == TripStatus.PAUSED) {
            return
        }
        _selectedServiceType.value = type
        clearValidationMessage()
    }

    /**
     * Selects the vendor for the upcoming trip.
     * Locked during an active trip (`RUNNING` or `PAUSED`) so the vendor cannot be switched mid-trip.
     */
    fun selectVendorForNextTrip(vendor: VendorProfileEntity): Boolean {
        val status = tripState.value.status
        if (status == TripStatus.RUNNING || status == TripStatus.PAUSED) {
            Log.w("MeterViewModel", "Cannot change vendor while a trip is active")
            return false
        }
        if (!vendor.isActive) {
            return false
        }
        _selectedVendor.value = vendor
        return true
    }

    /**
     * Switches the active trip view to the full Meter Details interface without restarting the trip,
     * resetting fare, or interrupting GPS tracking.
     */
    fun showMeterDetails() {
        _activeTripDisplayMode.value = ActiveTripDisplayMode.METER_DETAILS
    }

    /**
     * Returns from Meter Details to the enlarged active-trip map display without restarting the trip,
     * resetting fare, or interrupting GPS tracking.
     */
    fun showMapDisplay() {
        _activeTripDisplayMode.value = ActiveTripDisplayMode.MAP_FIRST
    }

    fun clearValidationMessage() {
        _tariffValidationMessage.value = null
    }

    fun loadDriverProfile() {
        viewModelScope.launch {
            _driverProfile.value = tripRepository.getDriverProfile()
        }
    }

    fun saveDriverProfile(profile: DriverProfileEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            tripRepository.saveDriverProfile(profile)
            _driverProfile.value = profile
            onSaved()
        }
    }

    fun checkForActiveBackup() {
        viewModelScope.launch {
            val backup = tripRepository.getActiveTrip()
            _hasActiveSessionBackup.value = backup != null
            if (backup != null) {
                _selectedServiceType.value = ServiceType.fromId(backup.serviceType)
                _selectedVendor.value = VendorProfileEntity(
                    vendorId = backup.vendorId.ifBlank { VendorLogoRenderer.DEFAULT_VENDOR_ID },
                    vendorName = backup.vendorName.ifBlank { VendorLogoRenderer.DEFAULT_VENDOR_NAME },
                    logoAsset = backup.vendorLogoAsset.ifBlank {
                        VendorLogoRenderer.assetPathForVendor(backup.vendorId, backup.vendorName)
                    },
                    contactNumber = backup.vendorContactNumber,
                    businessAddress = backup.vendorBusinessAddress,
                    email = backup.vendorEmail,
                    gstin = backup.vendorGstin,
                    invoiceDisplayDetails = backup.vendorInvoiceDetails,
                    isActive = true
                )
            }
            Log.d("MeterViewModel", "Checked active session backup: ${backup != null}")
        }
    }

    /**
     * Immediately starts the meter without any OTP or password requirement,
     * automatically opens the enlarged active-trip map screen (`MAP_FIRST`),
     * and locks the selected vendor for the duration of the trip.
     */
    fun startTrip() {
        // Automatically open the enlarged active-trip map screen synchronously when trip starts
        _activeTripDisplayMode.value = ActiveTripDisplayMode.MAP_FIRST

        viewModelScope.launch {
            val baseFareVal = settingsRepository.baseFare.first()
            val farePerKmVal = settingsRepository.farePerKm.first()
            val waitFareVal = settingsRepository.waitFarePerMin.first()
            val currencyVal = settingsRepository.currency.first()
            val thresholdVal = settingsRepository.speedThreshold.first()

            val rHourlyRate = settingsRepository.rentalHourlyRate.first()
            val rIncludedKm = settingsRepository.rentalIncludedKmPerHour.first()
            val rMinHours = settingsRepository.rentalMinHours.first()
            val rExtraHr = settingsRepository.rentalExtraHrRate.first()
            val rExtraKm = settingsRepository.rentalExtraKmRate.first()

            val oRateKm = settingsRepository.outstationFarePerKm.first()
            val oMinFare = settingsRepository.outstationMinFare.first()
            val oWaitRate = settingsRepository.outstationWaitRate.first()
            val oDriverBata = settingsRepository.outstationDriverBata.first()
            val oAddn = settingsRepository.outstationAdditionalCharges.first()
            val calFactor = settingsRepository.distanceCalibrationFactor.first()

            // Fetch fresh vendor record from DB before locking snapshot for this trip
            val chosenVendor = tripRepository.getVendorById(_selectedVendor.value.vendorId)
                ?: _selectedVendor.value

            _tariffValidationMessage.value = null

            LocationTrackingService.startTrip(
                getApplication(),
                baseFare = baseFareVal,
                farePerKm = farePerKmVal,
                waitFarePerMin = waitFareVal,
                currency = currencyVal,
                speedThreshold = thresholdVal,
                serviceType = _selectedServiceType.value.id,
                rentalHourlyRate = rHourlyRate,
                rentalIncludedKmPerHour = rIncludedKm,
                rentalMinHours = rMinHours,
                rentalExtraHrRate = rExtraHr,
                rentalExtraKmRate = rExtraKm,
                outstationRatePerKm = oRateKm,
                outstationMinFare = oMinFare,
                outstationWaitRate = oWaitRate,
                outstationDriverBata = oDriverBata,
                outstationAdditionalCharges = oAddn,
                calibrationFactor = calFactor,
                vendorId = chosenVendor.vendorId,
                vendorName = chosenVendor.vendorName,
                vendorLogoAsset = chosenVendor.logoAssetPath,
                vendorContactNumber = chosenVendor.contactNumber,
                vendorBusinessAddress = chosenVendor.businessAddress,
                vendorEmail = chosenVendor.email,
                vendorGstin = chosenVendor.gstin,
                vendorInvoiceDetails = chosenVendor.invoiceDisplayDetails
            )
            _hasActiveSessionBackup.value = false
        }
    }

    fun pauseTrip() {
        LocationTrackingService.pauseTrip(getApplication())
    }

    fun resumeTrip() {
        LocationTrackingService.resumeTrip(getApplication())
    }

    fun addExtraCharge(label: String, amount: Double) {
        LocationTrackingService.addExtraCharge(getApplication(), label, amount)
    }

    fun removeExtraCharge(chargeId: String) {
        LocationTrackingService.removeExtraCharge(getApplication(), chargeId)
    }

    fun clearExtraCharges() {
        LocationTrackingService.clearExtraCharges(getApplication())
    }

    fun stopTrip(customerName: String = "", customerMobile: String = "", customerAddress: String = "") {
        LocationTrackingService.stopTrip(
            context = getApplication(),
            customerName = customerName,
            customerMobile = customerMobile,
            customerAddress = customerAddress
        )
        _activeTripDisplayMode.value = ActiveTripDisplayMode.MAP_FIRST
        viewModelScope.launch {
            _hasActiveSessionBackup.value = false
            // Reset per-trip vendor selection back to GET TAXI default for the next trip
            val defaultVendor = tripRepository.getVendorById(VendorLogoRenderer.DEFAULT_VENDOR_ID)
                ?: VendorLogoRenderer.defaultGetTaxiProfile()
            _selectedVendor.value = defaultVendor
        }
    }

    fun completeTripAndSave(
        customerName: String = "",
        customerMobile: String = "",
        customerAddress: String = "",
        onCompleted: (Long) -> Unit
    ) {
        val app = getApplication<Application>()

        LocationTrackingService.stopTrip(
            context = app,
            customerName = customerName,
            customerMobile = customerMobile,
            customerAddress = customerAddress
        )
        _activeTripDisplayMode.value = ActiveTripDisplayMode.MAP_FIRST

        viewModelScope.launch {
            _hasActiveSessionBackup.value = false
            kotlinx.coroutines.delay(250)
            val latestTrips = tripRepository.allTrips.first()
            val latestTrip = latestTrips.firstOrNull()
            val tripId = latestTrip?.id?.toLong() ?: 1L
            // Reset per-trip vendor selection back to GET TAXI default for the next trip
            val defaultVendor = tripRepository.getVendorById(VendorLogoRenderer.DEFAULT_VENDOR_ID)
                ?: VendorLogoRenderer.defaultGetTaxiProfile()
            _selectedVendor.value = defaultVendor
            onCompleted(tripId)
        }
    }

    /**
     * Creates a new manual trip bill in Room database with an immutable snapshot of the selected vendor
     * so drivers can generate custom vendor-specific invoices/PDFs at any time.
     */
    fun createManualBill(
        serviceType: ServiceType,
        customerName: String,
        customerMobile: String,
        customerAddress: String,
        pickupAddress: String,
        dropAddress: String,
        distanceKm: Double,
        durationMinutes: Long,
        waitingMinutes: Long,
        baseFare: Double,
        extraCharges: Double,
        finalTotalFare: Double,
        extraChargesJson: String = "",
        vendor: VendorProfileEntity? = null,
        invoiceDateText: String = "",
        invoiceTimeText: String = "",
        onCreated: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val durationSecs = (durationMinutes.coerceAtLeast(1L)) * 60L
            val waitingSecs = (waitingMinutes.coerceAtLeast(0L)) * 60L
            val startTime = now - (durationSecs * 1000L)

            val profile = tripRepository.getDriverProfile()
            val tripNumber = tripRepository.generateDeterministicTripNumber(now)
            val currencyVal = settingsRepository.currency.first()
            val farePerKmVal = settingsRepository.farePerKm.first()
            val waitFareVal = settingsRepository.waitFarePerMin.first()
            val rHourlyRate = settingsRepository.rentalHourlyRate.first()
            val rIncludedKm = settingsRepository.rentalIncludedKmPerHour.first()
            val rExtraKm = settingsRepository.rentalExtraKmRate.first()
            val oRateKm = settingsRepository.outstationFarePerKm.first()
            val oDriverBata = settingsRepository.outstationDriverBata.first()

            val chosenVendor = vendor
                ?: tripRepository.getVendorById(_selectedVendor.value.vendorId)
                ?: _selectedVendor.value
            val vendorInvoiceNo = VendorLogoRenderer.buildVendorInvoiceNumber(chosenVendor.vendorName, tripNumber)

            val defaultDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(now))
            val defaultTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))
            val finalInvoiceDate = invoiceDateText.trim().ifBlank { defaultDate }
            val finalInvoiceTime = invoiceTimeText.trim().ifBlank { defaultTime }

            val billableHrs = ((durationMinutes + 54) / 60).coerceAtLeast(1L)

            val serializedExtras = if (extraChargesJson.isNotBlank()) {
                extraChargesJson
            } else if (extraCharges > 0.0) {
                com.example.data.model.ExtraChargeItem.serialize(
                    listOf(com.example.data.model.ExtraChargeItem(label = "Extra Charges", amount = extraCharges))
                )
            } else {
                ""
            }

            val manualTrip = TripEntity(
                tripNumber = tripNumber,
                startTime = startTime,
                endTime = now,
                distanceKm = distanceKm,
                durationSeconds = durationSecs,
                waitingSeconds = waitingSecs,
                totalFare = finalTotalFare,
                startLatitude = 0.0,
                startLongitude = 0.0,
                endLatitude = 0.0,
                endLongitude = 0.0,
                customerName = customerName.trim(),
                customerMobile = customerMobile.trim(),
                customerAddress = customerAddress.trim(),
                driverName = profile?.driverName ?: "",
                driverMobile = profile?.driverMobile ?: "",
                vehicleNumber = profile?.vehicleNumber ?: "",
                vehicleModel = profile?.vehicleModel ?: "",
                vehicleType = profile?.vehicleType ?: "Sedan Taxi",
                pickupAddress = pickupAddress.trim().ifBlank { "Manual Entry" },
                dropAddress = dropAddress.trim().ifBlank { "Manual Entry" },
                baseFare = baseFare,
                farePerKm = farePerKmVal,
                waitFarePerMin = waitFareVal,
                currency = currencyVal,
                serviceType = serviceType.id,
                rentalBaseHours = 1.0,
                rentalBaseKm = rIncludedKm,
                rentalBaseFare = rHourlyRate,
                rentalExtraKmRate = rExtraKm,
                rentalExtraHrRate = rHourlyRate,
                billableHours = billableHrs,
                outstationRatePerKm = oRateKm,
                outstationDriverBata = if (serviceType == ServiceType.OUTSTATION) oDriverBata else 0.0,
                outstationAdditionalCharges = extraCharges,
                extraChargesJson = serializedExtras,
                vendorId = chosenVendor.vendorId,
                vendorName = chosenVendor.vendorName,
                vendorLogoAsset = chosenVendor.logoAssetPath,
                vendorContactNumber = chosenVendor.contactNumber,
                vendorBusinessAddress = chosenVendor.businessAddress,
                vendorEmail = chosenVendor.email,
                vendorGstin = chosenVendor.gstin,
                vendorInvoiceDetails = chosenVendor.invoiceDisplayDetails,
                vendorInvoiceNumber = vendorInvoiceNo,
                invoiceTimestamp = now,
                invoiceDateText = finalInvoiceDate,
                invoiceTimeText = finalInvoiceTime
            )

            val insertedId = tripRepository.insertTrip(manualTrip).toInt()
            onCreated(insertedId)
        }
    }

    /**
     * Saves or updates a vendor profile in Vendor Management.
     * Only affects future trips; existing TripEntity records store their own immutable vendor snapshot.
     */
    fun saveVendorProfile(
        vendor: VendorProfileEntity,
        onSaved: (VendorProfileEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val cleanName = if (VendorLogoRenderer.isGetTaxiVendor(vendor.vendorId, vendor.vendorName)) {
                VendorLogoRenderer.DEFAULT_VENDOR_NAME
            } else {
                vendor.vendorName.trim()
            }
            val updated = vendor.copy(
                vendorName = cleanName,
                contactNumber = vendor.contactNumber.trim(),
                businessAddress = vendor.businessAddress.trim(),
                email = vendor.email.trim(),
                gstin = vendor.gstin.trim(),
                invoiceDisplayDetails = vendor.invoiceDisplayDetails.trim(),
                isActive = if (VendorLogoRenderer.isGetTaxiVendor(vendor.vendorId, cleanName)) true else vendor.isActive
            )
            tripRepository.upsertVendor(updated)
            if (_selectedVendor.value.vendorId == updated.vendorId) {
                _selectedVendor.value = if (updated.isActive) {
                    updated
                } else {
                    tripRepository.getVendorById(VendorLogoRenderer.DEFAULT_VENDOR_ID)
                        ?: VendorLogoRenderer.defaultGetTaxiProfile()
                }
            }
            onSaved(updated)
        }
    }

    /**
     * Adds a new custom vendor in Vendor Management using the reusable GET TAXI-style badge template.
     */
    fun addCustomVendor(
        businessName: String,
        contactNumber: String = "",
        businessAddress: String = "",
        email: String = "",
        gstin: String = "",
        invoiceDisplayDetails: String = "",
        onAdded: (VendorProfileEntity) -> Unit = {}
    ) {
        val trimmedName = businessName.trim()
        if (trimmedName.isBlank()) return
        viewModelScope.launch {
            val slug = VendorLogoRenderer.slugForVendorName(trimmedName)
            val existing = tripRepository.getVendorById(slug)
            val sortOrder = (tripRepository.getAllVendorsList().maxOfOrNull { it.sortOrder } ?: 20) + 1
            val newVendor = VendorProfileEntity(
                vendorId = slug,
                vendorName = trimmedName,
                logoAsset = VendorLogoRenderer.assetPathForVendor(slug, trimmedName),
                contactNumber = contactNumber.trim(),
                businessAddress = businessAddress.trim(),
                email = email.trim(),
                gstin = gstin.trim(),
                invoiceDisplayDetails = invoiceDisplayDetails.trim(),
                isActive = true,
                isOriginalGetTaxi = false,
                sortOrder = existing?.sortOrder ?: sortOrder
            )
            tripRepository.upsertVendor(newVendor)
            onAdded(newVendor)
        }
    }

    fun toggleVendorActiveStatus(vendor: VendorProfileEntity, isActive: Boolean) {
        if (VendorLogoRenderer.isGetTaxiVendor(vendor.vendorId, vendor.vendorName)) {
            return // GET TAXI is permanently active
        }
        viewModelScope.launch {
            tripRepository.setVendorActiveStatus(vendor.vendorId, isActive)
            if (!isActive && _selectedVendor.value.vendorId == vendor.vendorId) {
                _selectedVendor.value = tripRepository.getVendorById(VendorLogoRenderer.DEFAULT_VENDOR_ID)
                    ?: VendorLogoRenderer.defaultGetTaxiProfile()
            }
        }
    }

    fun recoverTrip() {
        _activeTripDisplayMode.value = ActiveTripDisplayMode.MAP_FIRST
        LocationTrackingService.recoveryTrip(getApplication())
        _hasActiveSessionBackup.value = false
    }

    fun discardBackup() {
        viewModelScope.launch {
            tripRepository.clearActiveTrip()
            _hasActiveSessionBackup.value = false
        }
    }

    suspend fun getTripById(id: Int): TripEntity? {
        return tripRepository.getTripById(id)
    }

    fun updateCustomerDetails(
        tripId: Int,
        name: String,
        mobile: String,
        address: String,
        onUpdated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val existing = tripRepository.getTripById(tripId)
            if (existing != null) {
                val updated = existing.copy(
                    customerName = name.trim(),
                    customerMobile = mobile.trim(),
                    customerAddress = address.trim()
                )
                tripRepository.updateTrip(updated)
                onUpdated()
            }
        }
    }

    /**
     * Updates editable invoice/bill fields (final fare amount, extra charges, customer info,
     * pickup/drop address, distance, base fare) before generating PDFs or sharing receipts.
     */
    fun updateEditableBillDetails(
        updatedTrip: TripEntity,
        onUpdated: (TripEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            tripRepository.updateTrip(updatedTrip)
            onUpdated(updatedTrip)
        }
    }

    fun deleteTrip(id: Int) {
        viewModelScope.launch {
            tripRepository.deleteTripById(id)
        }
    }
}
