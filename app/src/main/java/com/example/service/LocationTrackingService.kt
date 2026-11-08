package com.example.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.database.ActiveTripRecord
import com.example.data.database.TripDatabase
import com.example.data.database.TripEntity
import com.example.data.model.ExtraChargeItem
import com.example.data.model.ServiceType
import com.example.data.model.TripState
import com.example.data.model.TripStatus
import com.example.data.repository.TripRepository
import com.example.util.AppLanguageManager
import com.example.util.FareCalculator
import com.example.util.GpsDistanceEngine
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationManager: android.location.LocationManager? = null
    private var locationCallback: LocationCallback? = null
    private var locationListener: android.location.LocationListener? = null
    private var timerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private lateinit var repository: TripRepository
    private lateinit var floatingSpeedOverlay: FloatingSpeedOverlayManager

    // Core high-accuracy GPS distance engine
    private val gpsDistanceEngine = GpsDistanceEngine()
    private var lastLocationCallbackTimestamp = 0L

    companion object {
        const val TAG = "LocationTrackingService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "get_taxi_tracking_channel"

        private val _tripState = MutableStateFlow(TripState())
        val tripState: StateFlow<TripState> = _tripState.asStateFlow()

        @Volatile
        private var userExplicitlyStartedTrip: Boolean = false

        fun isTripActivelyRunningForBubble(): Boolean {
            val status = _tripState.value.status
            return userExplicitlyStartedTrip && (status == TripStatus.RUNNING || status == TripStatus.PAUSED)
        }

        // Lightweight bounded diagnostic logs (last 25 events, no customer PII)
        private val _diagnostics = MutableStateFlow<List<String>>(emptyList())
        val diagnostics: StateFlow<List<String>> = _diagnostics.asStateFlow()

        fun recordDiagnostic(message: String) {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            val entry = "[$timestamp] $message"
            Log.d(TAG, "DIAGNOSTIC: $entry")
            val currentList = _diagnostics.value
            _diagnostics.value = (listOf(entry) + currentList).take(25)
        }

        fun generateTripNumber(timestamp: Long): String {
            val dateStr = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(timestamp))
            return "GTM-$dateStr"
        }

        fun startTrip(
            context: Context,
            baseFare: Double = 0.0,
            farePerKm: Double = 0.0,
            waitFarePerMin: Double = 0.0,
            currency: String = "₹",
            speedThreshold: Double = 5.0,
            serviceType: String = "LOCAL",
            rentalHourlyRate: Double = 0.0,
            rentalIncludedKmPerHour: Double = 10.0,
            rentalMinHours: Double = 1.0,
            rentalExtraHrRate: Double = 0.0,
            rentalExtraKmRate: Double = 0.0,
            outstationRatePerKm: Double = 0.0,
            outstationMinFare: Double = 0.0,
            outstationWaitRate: Double = 0.0,
            outstationDriverBata: Double = 0.0,
            outstationAdditionalCharges: Double = 0.0,
            calibrationFactor: Double = 1.0,
            vendorId: String = "get_taxi",
            vendorName: String = "GET TAXI",
            vendorLogoAsset: String = "drawable/get_taxi_header_logo",
            vendorContactNumber: String = "",
            vendorBusinessAddress: String = "",
            vendorEmail: String = "",
            vendorGstin: String = "",
            vendorInvoiceDetails: String = ""
        ) {
            userExplicitlyStartedTrip = true
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "START"
                putExtra("baseFare", baseFare)
                putExtra("farePerKm", farePerKm)
                putExtra("waitFarePerMin", waitFarePerMin)
                putExtra("currency", currency)
                putExtra("speedThreshold", speedThreshold)
                putExtra("serviceType", serviceType)
                putExtra("rentalHourlyRate", rentalHourlyRate)
                putExtra("rentalIncludedKmPerHour", rentalIncludedKmPerHour)
                putExtra("rentalMinHours", rentalMinHours)
                putExtra("rentalExtraHrRate", rentalExtraHrRate)
                putExtra("rentalExtraKmRate", rentalExtraKmRate)
                putExtra("outstationRatePerKm", outstationRatePerKm)
                putExtra("outstationMinFare", outstationMinFare)
                putExtra("outstationWaitRate", outstationWaitRate)
                putExtra("outstationDriverBata", outstationDriverBata)
                putExtra("outstationAdditionalCharges", outstationAdditionalCharges)
                putExtra("calibrationFactor", calibrationFactor)
                putExtra("vendorId", vendorId)
                putExtra("vendorName", vendorName)
                putExtra("vendorLogoAsset", vendorLogoAsset)
                putExtra("vendorContactNumber", vendorContactNumber)
                putExtra("vendorBusinessAddress", vendorBusinessAddress)
                putExtra("vendorEmail", vendorEmail)
                putExtra("vendorGstin", vendorGstin)
                putExtra("vendorInvoiceDetails", vendorInvoiceDetails)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startMonitoring(
            context: Context,
            baseFare: Double = 0.0,
            farePerKm: Double = 0.0,
            waitFarePerMin: Double = 0.0,
            currency: String = "₹",
            speedThreshold: Double = 5.0,
            serviceType: String = "LOCAL",
            rentalHourlyRate: Double = 0.0,
            rentalIncludedKmPerHour: Double = 10.0,
            rentalMinHours: Double = 1.0,
            rentalExtraHrRate: Double = 0.0,
            rentalExtraKmRate: Double = 0.0,
            outstationRatePerKm: Double = 0.0,
            outstationMinFare: Double = 0.0,
            outstationWaitRate: Double = 0.0,
            outstationDriverBata: Double = 0.0,
            outstationAdditionalCharges: Double = 0.0,
            calibrationFactor: Double = 1.0
        ) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "MONITOR"
                putExtra("baseFare", baseFare)
                putExtra("farePerKm", farePerKm)
                putExtra("waitFarePerMin", waitFarePerMin)
                putExtra("currency", currency)
                putExtra("speedThreshold", speedThreshold)
                putExtra("serviceType", serviceType)
                putExtra("rentalHourlyRate", rentalHourlyRate)
                putExtra("rentalIncludedKmPerHour", rentalIncludedKmPerHour)
                putExtra("rentalMinHours", rentalMinHours)
                putExtra("rentalExtraHrRate", rentalExtraHrRate)
                putExtra("rentalExtraKmRate", rentalExtraKmRate)
                putExtra("outstationRatePerKm", outstationRatePerKm)
                putExtra("outstationMinFare", outstationMinFare)
                putExtra("outstationWaitRate", outstationWaitRate)
                putExtra("outstationDriverBata", outstationDriverBata)
                putExtra("outstationAdditionalCharges", outstationAdditionalCharges)
                putExtra("calibrationFactor", calibrationFactor)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "startService failed in startMonitoring", e)
            }
        }

        fun pauseTrip(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "PAUSE"
            }
            context.startService(intent)
        }

        fun resumeTrip(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "RESUME"
            }
            context.startService(intent)
        }

        fun stopTrip(
            context: Context,
            customerName: String = "",
            customerMobile: String = "",
            customerAddress: String = ""
        ) {
            userExplicitlyStartedTrip = false
            FloatingSpeedOverlayManager.forceHide(context)
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "STOP"
                putExtra("customerName", customerName)
                putExtra("customerMobile", customerMobile)
                putExtra("customerAddress", customerAddress)
            }
            context.startService(intent)
        }

        fun stopServiceCompletely(context: Context) {
            userExplicitlyStartedTrip = false
            FloatingSpeedOverlayManager.forceHide(context)
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "STOP_SERVICE"
            }
            context.startService(intent)
        }

        fun recoveryTrip(context: Context) {
            userExplicitlyStartedTrip = true
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "RECOVER"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun refreshOverlay(context: Context) {
            if (!isTripActivelyRunningForBubble()) {
                FloatingSpeedOverlayManager.forceHide(context)
                return
            }
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "REFRESH_OVERLAY"
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send REFRESH_OVERLAY action", e)
            }
        }

        fun onAppMinimized(context: Context) {
            if (!FloatingSpeedOverlayManager.isBubbleEnabled(context)) return
            if (!isTripActivelyRunningForBubble()) {
                FloatingSpeedOverlayManager.forceHide(context)
                return
            }
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "APP_MINIMIZED"
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send APP_MINIMIZED action", e)
            }
        }

        fun setFloatingBubbleEnabled(context: Context, enabled: Boolean) {
            FloatingSpeedOverlayManager.setBubbleEnabled(context, enabled)
            if (!isTripActivelyRunningForBubble()) {
                FloatingSpeedOverlayManager.forceHide(context)
                return
            }
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "SET_BUBBLE_ENABLED"
                putExtra("enabled", enabled)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SET_BUBBLE_ENABLED action", e)
            }
        }

        fun computeUpdatedStateWithFare(state: TripState): TripState {
            val liveExtraTotal = ExtraChargeItem.totalAmount(state.extraChargeItems)
            if (state.status == TripStatus.IDLE) {
                return state.copy(
                    liveExtraChargesTotal = liveExtraTotal,
                    currentFare = liveExtraTotal
                )
            }
            return when (ServiceType.fromId(state.serviceType)) {
                ServiceType.LOCAL -> {
                    val waitMinutes = state.waitingSeconds / 60.0
                    val base = state.baseFare
                    val distCharge = state.distanceKm * state.farePerKm
                    val waitCharge = waitMinutes * state.waitFarePerMin
                    val total = base + distCharge + waitCharge + liveExtraTotal

                    state.copy(
                        currentFare = total,
                        liveExtraChargesTotal = liveExtraTotal,
                        outOfCitySurchargeAmount = 0.0
                    )
                }
                ServiceType.RENTAL -> {
                    val result = FareCalculator.calculateHourlyRentalFare(
                        durationSeconds = state.durationSeconds,
                        distanceKm = state.distanceKm,
                        hourlyRate = state.rentalHourlyRate,
                        includedKmPerHour = state.rentalIncludedKmPerHour,
                        extraKmRate = state.rentalExtraKmRate
                    )

                    state.copy(
                        billableHours = result.billableHours,
                        rentalTotalIncludedKm = result.totalIncludedKm,
                        rentalExtraDistanceKm = result.extraDistanceKm,
                        rentalHourlyCharge = result.hourlyCharge,
                        rentalExtraDistanceCharge = result.extraDistanceCharge,
                        currentFare = result.totalFare + liveExtraTotal,
                        liveExtraChargesTotal = liveExtraTotal,
                        outOfCitySurchargeAmount = 0.0
                    )
                }
                ServiceType.OUTSTATION -> {
                    val distCharge = state.distanceKm * state.outstationRatePerKm
                    val waitHours = state.waitingSeconds / 3600.0
                    val waitCharge = if (state.outstationWaitRate > 0.0) waitHours * state.outstationWaitRate else 0.0
                    val driverBata = state.outstationDriverBata
                    val addnCharges = state.outstationAdditionalCharges

                    val calculated = distCharge + waitCharge + driverBata + addnCharges
                    val baseWithFloor = if (state.outstationMinFare > 0.0) calculated.coerceAtLeast(state.outstationMinFare) else calculated
                    val total = baseWithFloor + liveExtraTotal

                    state.copy(
                        currentFare = total,
                        liveExtraChargesTotal = liveExtraTotal,
                        outOfCitySurchargeAmount = 0.0
                    )
                }
            }
        }

        fun addExtraCharge(context: Context, label: String, amount: Double) {
            if (amount <= 0.0) return
            val cleanLabel = label.trim().ifBlank { "Extra Charge" }
            val newItem = ExtraChargeItem(label = cleanLabel, amount = amount)
            val current = _tripState.value
            val updatedItems = current.extraChargeItems + newItem
            _tripState.value = computeUpdatedStateWithFare(current.copy(extraChargeItems = updatedItems))
            recordDiagnostic("Extra charge added: $cleanLabel (${current.currency}${String.format(Locale.US, "%.2f", amount)})")
            if (current.status == TripStatus.RUNNING || current.status == TripStatus.PAUSED) {
                try {
                    val intent = Intent(context, LocationTrackingService::class.java).apply {
                        action = "SYNC_EXTRA_CHARGES"
                    }
                    context.startService(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync extra charges with service", e)
                }
            }
        }

        fun removeExtraCharge(context: Context, chargeId: String) {
            val current = _tripState.value
            val updatedItems = current.extraChargeItems.filterNot { it.id == chargeId }
            _tripState.value = computeUpdatedStateWithFare(current.copy(extraChargeItems = updatedItems))
            if (current.status == TripStatus.RUNNING || current.status == TripStatus.PAUSED) {
                try {
                    val intent = Intent(context, LocationTrackingService::class.java).apply {
                        action = "SYNC_EXTRA_CHARGES"
                    }
                    context.startService(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync extra charges removal with service", e)
                }
            }
        }

        fun clearExtraCharges(context: Context) {
            val current = _tripState.value
            _tripState.value = computeUpdatedStateWithFare(current.copy(extraChargeItems = emptyList()))
            if (current.status == TripStatus.RUNNING || current.status == TripStatus.PAUSED) {
                try {
                    val intent = Intent(context, LocationTrackingService::class.java).apply {
                        action = "SYNC_EXTRA_CHARGES"
                    }
                    context.startService(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync clear extra charges with service", e)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppLanguageManager.init(this)
        Log.d(TAG, "onCreate: Initializing Get Taxi Tracking Service")
        recordDiagnostic("Get Taxi tracking service created")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager

        val database = TripDatabase.getDatabase(this)
        repository = TripRepository(database.tripDao())
        floatingSpeedOverlay = FloatingSpeedOverlayManager(this)

        createNotificationChannel()

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TaxiMeterPro::TrackingLock").apply {
            setReferenceCounted(false)
        }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isTtsInitialized = true
                Log.d(TAG, "TTS Initialized successfully")
            } else {
                Log.e(TAG, "TTS Initialization failed")
            }
        }
    }

    private fun safeStartForeground(id: Int, notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasLocationPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (hasLocationPerm) {
                    startForeground(id, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(id, notification)
                }
            } else {
                startForeground(id, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "safeStartForeground caught exception safely", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand Action: $action")

        // Immediately guarantee foreground service state to avoid Android 8+/14+ termination window
        safeStartForeground(NOTIFICATION_ID, buildNotification())

        // Handle system restart with null intent (START_STICKY recovery)
        if (intent == null || action == null) {
            Log.w(TAG, "onStartCommand called with null intent (Service restarted by Android OS)")
            recordDiagnostic("Service restarted by Android OS (START_STICKY)")
            serviceScope.launch {
                val record = if (userExplicitlyStartedTrip) repository.getActiveTrip() else null
                if (record != null) {
                    recordDiagnostic("Active trip found in DB, restoring...")
                    recoverSavedTrip()
                } else {
                    withContext(Dispatchers.Main) {
                        floatingSpeedOverlay.hide()
                    }
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            return START_STICKY
        }

        when (action) {
            "START" -> {
                val base = intent.getDoubleExtra("baseFare", 0.0)
                val rate = intent.getDoubleExtra("farePerKm", 0.0)
                val wait = intent.getDoubleExtra("waitFarePerMin", 0.0)
                val curr = intent.getStringExtra("currency") ?: "₹"
                val speedLim = intent.getDoubleExtra("speedThreshold", 5.0)
                val sType = intent.getStringExtra("serviceType") ?: "LOCAL"
                val calFactor = intent.getDoubleExtra("calibrationFactor", 1.0)

                val rHourlyRate = intent.getDoubleExtra("rentalHourlyRate", 0.0)
                val rIncludedKm = intent.getDoubleExtra("rentalIncludedKmPerHour", 10.0)
                val rMinHours = intent.getDoubleExtra("rentalMinHours", 1.0)
                val rExtraHr = intent.getDoubleExtra("rentalExtraHrRate", 0.0)
                val rExtraKm = intent.getDoubleExtra("rentalExtraKmRate", 0.0)

                val oRateKm = intent.getDoubleExtra("outstationRatePerKm", 0.0)
                val oMinFare = intent.getDoubleExtra("outstationMinFare", 0.0)
                val oWaitRate = intent.getDoubleExtra("outstationWaitRate", 0.0)
                val oDriverBata = intent.getDoubleExtra("outstationDriverBata", 0.0)
                val oAddn = intent.getDoubleExtra("outstationAdditionalCharges", 0.0)

                val vId = intent.getStringExtra("vendorId") ?: "get_taxi"
                val vName = intent.getStringExtra("vendorName") ?: "GET TAXI"
                val vLogo = intent.getStringExtra("vendorLogoAsset") ?: "drawable/get_taxi_header_logo"
                val vPhone = intent.getStringExtra("vendorContactNumber") ?: ""
                val vAddr = intent.getStringExtra("vendorBusinessAddress") ?: ""
                val vEmail = intent.getStringExtra("vendorEmail") ?: ""
                val vGstin = intent.getStringExtra("vendorGstin") ?: ""
                val vInvoiceDetails = intent.getStringExtra("vendorInvoiceDetails") ?: ""

                initiateNewTrip(
                    base = base, rate = rate, wait = wait, curr = curr, speedLim = speedLim,
                    serviceType = sType,
                    rentalHourlyRate = rHourlyRate, rentalIncludedKmPerHour = rIncludedKm, rentalMinHours = rMinHours,
                    rentalExtraHrRate = rExtraHr, rentalExtraKmRate = rExtraKm,
                    outstationRatePerKm = oRateKm, outstationMinFare = oMinFare,
                    outstationWaitRate = oWaitRate, outstationDriverBata = oDriverBata,
                    outstationAdditionalCharges = oAddn,
                    calibrationFactor = calFactor,
                    vendorId = vId,
                    vendorName = vName,
                    vendorLogoAsset = vLogo,
                    vendorContactNumber = vPhone,
                    vendorBusinessAddress = vAddr,
                    vendorEmail = vEmail,
                    vendorGstin = vGstin,
                    vendorInvoiceDetails = vInvoiceDetails
                )
            }
            "MONITOR" -> {
                val base = intent.getDoubleExtra("baseFare", 0.0)
                val rate = intent.getDoubleExtra("farePerKm", 0.0)
                val wait = intent.getDoubleExtra("waitFarePerMin", 0.0)
                val curr = intent.getStringExtra("currency") ?: "₹"
                val speedLim = intent.getDoubleExtra("speedThreshold", 5.0)
                val sType = intent.getStringExtra("serviceType") ?: "LOCAL"
                val calFactor = intent.getDoubleExtra("calibrationFactor", 1.0)

                val rHourlyRate = intent.getDoubleExtra("rentalHourlyRate", 0.0)
                val rIncludedKm = intent.getDoubleExtra("rentalIncludedKmPerHour", 10.0)
                val rMinHours = intent.getDoubleExtra("rentalMinHours", 1.0)
                val rExtraHr = intent.getDoubleExtra("rentalExtraHrRate", 0.0)
                val rExtraKm = intent.getDoubleExtra("rentalExtraKmRate", 0.0)

                val oRateKm = intent.getDoubleExtra("outstationRatePerKm", 0.0)
                val oMinFare = intent.getDoubleExtra("outstationMinFare", 0.0)
                val oWaitRate = intent.getDoubleExtra("outstationWaitRate", 0.0)
                val oDriverBata = intent.getDoubleExtra("outstationDriverBata", 0.0)
                val oAddn = intent.getDoubleExtra("outstationAdditionalCharges", 0.0)

                if (_tripState.value.status == TripStatus.IDLE) {
                    _tripState.value = _tripState.value.copy(
                        baseFare = base,
                        farePerKm = rate,
                        waitFarePerMin = wait,
                        currency = curr,
                        speedThreshold = speedLim,
                        serviceType = sType,
                        rentalHourlyRate = rHourlyRate,
                        rentalIncludedKmPerHour = rIncludedKm,
                        rentalMinHours = rMinHours,
                        rentalExtraHrRate = rExtraHr,
                        rentalExtraKmRate = rExtraKm,
                        outstationRatePerKm = oRateKm,
                        outstationMinFare = oMinFare,
                        outstationWaitRate = oWaitRate,
                        outstationDriverBata = oDriverBata,
                        outstationAdditionalCharges = oAddn
                    )
                    gpsDistanceEngine.speedThresholdKmH = speedLim
                    gpsDistanceEngine.calibrationFactor = calFactor
                    recalculateFare()
                    floatingSpeedOverlay.hide()
                }
                safeStartForeground(NOTIFICATION_ID, buildNotification())
                startTrackingLocation()
                startTimers()
            }
            "PAUSE" -> {
                pauseActiveTrip()
            }
            "RESUME" -> {
                resumeActiveTrip()
            }
            "STOP" -> {
                val custName = intent.getStringExtra("customerName") ?: ""
                val custMobile = intent.getStringExtra("customerMobile") ?: ""
                val custAddress = intent.getStringExtra("customerAddress") ?: ""
                stopAndSaveTrip(custName, custMobile, custAddress)
            }
            "STOP_SERVICE" -> {
                stopTrackingLocation()
                stopTimers()
                releaseWakeLock()
                floatingSpeedOverlay.hide()
                _tripState.value = TripState(status = TripStatus.IDLE)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            "RECOVER" -> {
                recoverSavedTrip()
            }
            "REFRESH_OVERLAY", "APP_MINIMIZED" -> {
                val state = _tripState.value
                val isTripActive = state.status == TripStatus.RUNNING || state.status == TripStatus.PAUSED
                if (isTripActive) {
                    acquireWakeLock()
                    startTrackingLocation()
                    startTimers()
                    if (floatingSpeedOverlay.isOverlayEnabled()) {
                        floatingSpeedOverlay.show()
                        floatingSpeedOverlay.updateSpeed(state.speedKmH)
                    }
                } else {
                    floatingSpeedOverlay.hide()
                }
            }
            "SET_BUBBLE_ENABLED" -> {
                val enabled = intent.getBooleanExtra("enabled", true)
                floatingSpeedOverlay.setOverlayEnabled(enabled)
                val state = _tripState.value
                val isTripActive = state.status == TripStatus.RUNNING || state.status == TripStatus.PAUSED
                if (enabled && isTripActive) {
                    acquireWakeLock()
                    startTrackingLocation()
                    startTimers()
                    floatingSpeedOverlay.show()
                    floatingSpeedOverlay.updateSpeed(state.speedKmH)
                } else {
                    floatingSpeedOverlay.hide()
                }
            }
            "SYNC_EXTRA_CHARGES" -> {
                recalculateFare()
                updateNotification()
                saveSnapshotToDatabase()
            }
        }

        return START_STICKY
    }

    private fun announceVoice(message: String) {
        if (isTtsInitialized && _tripState.value.status != TripStatus.IDLE) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun resolveAddress(lat: Double, lng: Double): String {
        if (lat == 0.0 && lng == 0.0) return "GPS Location"
        return try {
            val geocoder = android.location.Geocoder(this, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val line = addr.getAddressLine(0)
                if (!line.isNullOrBlank()) {
                    line
                } else {
                    val subLoc = addr.subLocality ?: addr.locality ?: ""
                    val city = addr.adminArea ?: ""
                    if (subLoc.isNotEmpty()) "$subLoc, $city" else String.format(Locale.US, "Lat: %.4f, Lng: %.4f", lat, lng)
                }
            } else {
                String.format(Locale.US, "Lat: %.4f, Lng: %.4f", lat, lng)
            }
        } catch (e: Exception) {
            String.format(Locale.US, "Lat: %.4f, Lng: %.4f", lat, lng)
        }
    }

    private fun initiateNewTrip(
        base: Double, rate: Double, wait: Double, curr: String, speedLim: Double,
        serviceType: String = "LOCAL",
        rentalHourlyRate: Double = 0.0,
        rentalIncludedKmPerHour: Double = 10.0,
        rentalMinHours: Double = 1.0,
        rentalExtraHrRate: Double = 0.0,
        rentalExtraKmRate: Double = 0.0,
        outstationRatePerKm: Double = 0.0,
        outstationMinFare: Double = 0.0,
        outstationWaitRate: Double = 0.0,
        outstationDriverBata: Double = 0.0,
        outstationAdditionalCharges: Double = 0.0,
        calibrationFactor: Double = 1.0,
        vendorId: String = "get_taxi",
        vendorName: String = "GET TAXI",
        vendorLogoAsset: String = "drawable/get_taxi_header_logo",
        vendorContactNumber: String = "",
        vendorBusinessAddress: String = "",
        vendorEmail: String = "",
        vendorGstin: String = "",
        vendorInvoiceDetails: String = ""
    ) {
        acquireWakeLock()

        gpsDistanceEngine.reset(0.0)
        gpsDistanceEngine.speedThresholdKmH = speedLim
        gpsDistanceEngine.calibrationFactor = calibrationFactor

        val initialPoints = if (_tripState.value.latitude != null && _tripState.value.longitude != null) {
            listOf(Pair(_tripState.value.latitude!!, _tripState.value.longitude!!))
        } else {
            emptyList()
        }

        val startLat = _tripState.value.latitude ?: 0.0
        val startLng = _tripState.value.longitude ?: 0.0
        val startTimeNow = System.currentTimeMillis()
        val preAddedExtras = if (_tripState.value.status == TripStatus.IDLE) {
            _tripState.value.extraChargeItems
        } else {
            emptyList()
        }

        _tripState.value = TripState(
            status = TripStatus.RUNNING,
            tripNumber = "",
            startTime = startTimeNow,
            baseFare = base,
            farePerKm = rate,
            waitFarePerMin = wait,
            currency = curr,
            speedThreshold = speedLim,
            serviceType = serviceType,
            rentalHourlyRate = rentalHourlyRate,
            rentalIncludedKmPerHour = if (rentalIncludedKmPerHour > 0.0) rentalIncludedKmPerHour else 10.0,
            rentalMinHours = rentalMinHours,
            rentalExtraHrRate = rentalExtraHrRate,
            rentalExtraKmRate = rentalExtraKmRate,
            billableHours = 1L,
            outstationRatePerKm = outstationRatePerKm,
            outstationMinFare = outstationMinFare,
            outstationWaitRate = outstationWaitRate,
            outstationDriverBata = outstationDriverBata,
            outstationAdditionalCharges = outstationAdditionalCharges,
            extraChargeItems = preAddedExtras,
            liveExtraChargesTotal = ExtraChargeItem.totalAmount(preAddedExtras),
            currentFare = 0.0,
            distanceKm = 0.0,
            latitude = _tripState.value.latitude,
            longitude = _tripState.value.longitude,
            pickupAddress = "Acquiring Pickup Address...",
            routePoints = initialPoints,
            vendorId = vendorId,
            vendorName = vendorName,
            vendorLogoAsset = vendorLogoAsset,
            vendorContactNumber = vendorContactNumber,
            vendorBusinessAddress = vendorBusinessAddress,
            vendorEmail = vendorEmail,
            vendorGstin = vendorGstin,
            vendorInvoiceDetails = vendorInvoiceDetails
        )

        safeStartForeground(NOTIFICATION_ID, buildNotification())
        recalculateFare()

        // Resolve pickup address asynchronously off the main thread
        serviceScope.launch(Dispatchers.IO) {
            val pickupAddr = resolveAddress(startLat, startLng)
            _tripState.value = _tripState.value.copy(pickupAddress = pickupAddr)
            val tripNum = repository.generateDeterministicTripNumber(startTimeNow)
            _tripState.value = _tripState.value.copy(tripNumber = tripNum)
            saveSnapshotToDatabase()
            recordDiagnostic("Trip started: $tripNum, Mode: $serviceType")
        }

        startTrackingLocation()
        startTimers()
        floatingSpeedOverlay.show()
        floatingSpeedOverlay.updateSpeed(0.0)

        when (ServiceType.fromId(serviceType)) {
            ServiceType.RENTAL -> announceVoice("Hourly rental started. Package active.")
            ServiceType.OUTSTATION -> announceVoice("Outstation trip started. Happy journey.")
            ServiceType.LOCAL -> announceVoice("Ride started. Meter is running.")
        }
    }

    private fun pauseActiveTrip() {
        if (_tripState.value.status == TripStatus.RUNNING) {
            _tripState.value = _tripState.value.copy(
                status = TripStatus.PAUSED,
                speedKmH = 0f
            )
            stopTrackingLocation()
            announceVoice("Ride paused.")
            recordDiagnostic("Ride paused by driver")
            updateNotification()
            saveSnapshotToDatabase()
        }
    }

    private fun resumeActiveTrip() {
        if (_tripState.value.status == TripStatus.PAUSED) {
            _tripState.value = _tripState.value.copy(
                status = TripStatus.RUNNING
            )
            safeStartForeground(NOTIFICATION_ID, buildNotification())
            // Restore distance into engine so next sample resumes smoothly
            gpsDistanceEngine.restoreDistance(_tripState.value.distanceKm)
            startTrackingLocation()
            announceVoice("Ride resumed.")
            recordDiagnostic("Ride resumed by driver")
            updateNotification()
            saveSnapshotToDatabase()
        }
    }

    private fun recoverSavedTrip() {
        serviceScope.launch {
            val record = repository.getActiveTrip()
            if (record != null) {
                val tripNum = if (record.tripNumber.isNotBlank()) {
                    record.tripNumber
                } else {
                    repository.generateDeterministicTripNumber(record.startTime)
                }

                gpsDistanceEngine.reset(record.distanceKm)

                val recoveredExtras = ExtraChargeItem.deserialize(record.extraChargesJson)
                _tripState.value = TripState(
                    status = if (record.isPaused) TripStatus.PAUSED else TripStatus.RUNNING,
                    tripNumber = tripNum,
                    startTime = record.startTime,
                    durationSeconds = record.durationSeconds,
                    waitingSeconds = record.waitingSeconds,
                    distanceKm = record.distanceKm,
                    baseFare = record.baseFare,
                    farePerKm = record.farePerKm,
                    waitFarePerMin = record.waitFarePerMin,
                    currency = record.currency,
                    pickupAddress = record.pickupAddress,
                    serviceType = record.serviceType,
                    rentalHourlyRate = record.rentalBaseFare,
                    rentalMinHours = record.rentalBaseHours,
                    rentalExtraHrRate = record.rentalExtraHrRate,
                    rentalExtraKmRate = record.rentalExtraKmRate,
                    billableHours = record.billableHours,
                    outstationRatePerKm = record.outstationRatePerKm,
                    outstationMinFare = record.outstationMinFare,
                    outstationWaitRate = record.outstationWaitRate,
                    outstationDriverBata = record.outstationDriverBata,
                    outstationAdditionalCharges = record.outstationAdditionalCharges,
                    extraChargeItems = recoveredExtras,
                    liveExtraChargesTotal = ExtraChargeItem.totalAmount(recoveredExtras),
                    currentFare = record.currentFare,
                    vendorId = record.vendorId,
                    vendorName = record.vendorName,
                    vendorLogoAsset = record.vendorLogoAsset,
                    vendorContactNumber = record.vendorContactNumber,
                    vendorBusinessAddress = record.vendorBusinessAddress,
                    vendorEmail = record.vendorEmail,
                    vendorGstin = record.vendorGstin,
                    vendorInvoiceDetails = record.vendorInvoiceDetails,
                    routePoints = com.example.util.RouteCoordinatesSerializer.deserialize(record.routePointsJson)
                )

                recalculateFare()
                acquireWakeLock()

                if (_tripState.value.status == TripStatus.RUNNING) {
                    startTrackingLocation()
                    startTimers()
                }
                safeStartForeground(NOTIFICATION_ID, buildNotification())
                floatingSpeedOverlay.show()
                floatingSpeedOverlay.updateSpeed(_tripState.value.speedKmH)
                announceVoice("Ride tracking recovered.")
                recordDiagnostic("Ride recovered: $tripNum (${String.format(Locale.US, "%.2f", record.distanceKm)} km preserved)")
            } else {
                stopSelf()
            }
        }
    }

    private fun stopAndSaveTrip(customerName: String = "", customerMobile: String = "", customerAddress: String = "") {
        val currentState = _tripState.value
        if (currentState.status != TripStatus.IDLE) {
            val startLat = currentState.routePoints.firstOrNull()?.first ?: (currentState.latitude ?: 0.0)
            val startLng = currentState.routePoints.firstOrNull()?.second ?: (currentState.longitude ?: 0.0)
            val endLat = currentState.latitude ?: 0.0
            val endLng = currentState.longitude ?: 0.0

            val pAddress = if (currentState.pickupAddress.isNotBlank() && currentState.pickupAddress != "GPS Location" && !currentState.pickupAddress.startsWith("Acquiring")) {
                currentState.pickupAddress
            } else {
                resolveAddress(startLat, startLng)
            }
            val dAddress = resolveAddress(endLat, endLng)

            val extraItems = currentState.extraChargeItems
            val extraItemsTotal = ExtraChargeItem.totalAmount(extraItems)
            val serializedExtras = ExtraChargeItem.serialize(extraItems)
            val combinedAdditionalCharges = currentState.outstationAdditionalCharges + extraItemsTotal

            _tripState.value = currentState.copy(
                status = TripStatus.FINISHED,
                pickupAddress = pAddress,
                dropAddress = dAddress,
                customerName = customerName,
                customerMobile = customerMobile,
                customerAddress = customerAddress,
                extraChargeItems = emptyList(),
                liveExtraChargesTotal = 0.0
            )

            announceVoice("Ride completed. Total fare ${currentState.currency}${String.format(Locale.US, "%.2f", currentState.currentFare)}")
            recordDiagnostic("Ride completed: ${currentState.tripNumber}, Final distance: ${String.format(Locale.US, "%.2f", currentState.distanceKm)} km")

            serviceScope.launch {
                val driverProfile = repository.getDriverProfile()
                val finalTripNum = if (currentState.tripNumber.isNotBlank()) {
                    currentState.tripNumber
                } else {
                    repository.generateDeterministicTripNumber(currentState.startTime)
                }

                _tripState.value = _tripState.value.copy(tripNumber = finalTripNum)

                val nowTime = System.currentTimeMillis()
                val defaultDateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(nowTime))
                val defaultTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(nowTime))
                val serializedRoute = com.example.util.RouteCoordinatesSerializer.serialize(currentState.routePoints)

                val tripEntity = TripEntity(
                    tripNumber = finalTripNum,
                    startTime = currentState.startTime,
                    endTime = nowTime,
                    distanceKm = currentState.distanceKm,
                    durationSeconds = currentState.durationSeconds,
                    waitingSeconds = currentState.waitingSeconds,
                    totalFare = currentState.currentFare,
                    startLatitude = startLat,
                    startLongitude = startLng,
                    endLatitude = endLat,
                    endLongitude = endLng,
                    customerName = customerName,
                    customerMobile = customerMobile,
                    customerAddress = customerAddress,
                    driverName = driverProfile?.driverName ?: "",
                    driverMobile = driverProfile?.driverMobile ?: "",
                    vehicleNumber = driverProfile?.vehicleNumber ?: "",
                    vehicleModel = driverProfile?.vehicleModel ?: "",
                    vehicleType = driverProfile?.vehicleType ?: "Sedan Taxi",
                    pickupAddress = pAddress,
                    dropAddress = dAddress,
                    isOutOfCity = false,
                    outOfCitySurcharge = 0.0,
                    baseFare = currentState.baseFare,
                    farePerKm = currentState.farePerKm,
                    waitFarePerMin = currentState.waitFarePerMin,
                    currency = currentState.currency,
                    serviceType = currentState.serviceType,
                    rentalBaseHours = currentState.rentalMinHours,
                    rentalBaseKm = currentState.rentalIncludedKmPerHour,
                    rentalBaseFare = currentState.rentalHourlyRate,
                    rentalExtraKmRate = currentState.rentalExtraKmRate,
                    rentalExtraHrRate = currentState.rentalExtraHrRate,
                    billableHours = currentState.billableHours,
                    outstationRatePerKm = currentState.outstationRatePerKm,
                    outstationMinKm = 0.0,
                    outstationDriverBata = currentState.outstationDriverBata,
                    outstationMinFare = currentState.outstationMinFare,
                    outstationWaitRate = currentState.outstationWaitRate,
                    outstationAdditionalCharges = combinedAdditionalCharges,
                    extraChargesJson = serializedExtras,
                    routePointsJson = serializedRoute,
                    routeMapImagePath = "",
                    invoiceTimestamp = nowTime,
                    invoiceDateText = defaultDateStr,
                    invoiceTimeText = defaultTimeStr,
                    vendorId = currentState.vendorId.ifBlank { "get_taxi" },
                    vendorName = currentState.vendorName.ifBlank { "GET TAXI" },
                    vendorLogoAsset = currentState.vendorLogoAsset.ifBlank { "drawable/get_taxi_header_logo" },
                    vendorContactNumber = currentState.vendorContactNumber,
                    vendorBusinessAddress = currentState.vendorBusinessAddress,
                    vendorEmail = currentState.vendorEmail,
                    vendorGstin = currentState.vendorGstin,
                    vendorInvoiceDetails = currentState.vendorInvoiceDetails,
                    vendorInvoiceNumber = finalTripNum
                )

                val dbId = repository.insertTrip(tripEntity)
                repository.clearActiveTrip()
                Log.d(TAG, "Completed trip $finalTripNum saved in Room with DB ID $dbId")
            }

            userExplicitlyStartedTrip = false
            stopTrackingLocation()
            stopTimers()
            releaseWakeLock()
            floatingSpeedOverlay.hide()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startTrackingLocation() {
        val hasFineLoc = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarseLoc = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasFineLoc && !hasCoarseLoc) {
            Log.w(TAG, "Location permissions not granted yet, skipping location tracking request")
            recordDiagnostic("Location permissions missing, waiting for grant")
            return
        }

        stopTrackingLocation()

        // Request 1000ms (1 Hz) high-accuracy GPS updates to capture continuous road geometry
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).apply {
            setMinUpdateIntervalMillis(800L)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                lastLocationCallbackTimestamp = System.currentTimeMillis()
                for (location in result.locations) {
                    processIncomingLocation(location)
                }
            }
        }

        var requestedSuccessfully = false
        if (fusedLocationClient != null) {
            try {
                fusedLocationClient?.requestLocationUpdates(
                    request,
                    locationCallback!!,
                    Looper.getMainLooper()
                )
                requestedSuccessfully = true
                recordDiagnostic("FusedLocationClient updates requested (1Hz)")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed requesting Fused GPS updates", e)
                recordDiagnostic("Fused GPS request failed: ${e.message}")
            }
        }

        // Register LocationManager GPS_PROVIDER fallback if fused client failed
        if (!requestedSuccessfully && locationManager != null) {
            try {
                locationListener = object : android.location.LocationListener {
                    override fun onLocationChanged(location: Location) {
                        lastLocationCallbackTimestamp = System.currentTimeMillis()
                        processIncomingLocation(location)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                val provider = when {
                    locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ->
                        android.location.LocationManager.GPS_PROVIDER
                    locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true ->
                        android.location.LocationManager.NETWORK_PROVIDER
                    else -> null
                }

                if (provider != null) {
                    locationManager?.requestLocationUpdates(
                        provider,
                        1000L,
                        0.5f,
                        locationListener!!,
                        Looper.getMainLooper()
                    )
                    recordDiagnostic("LocationManager fallback requested ($provider)")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed requesting LocationManager updates", e)
            }
        }
    }

    private fun stopTrackingLocation() {
        try {
            locationCallback?.let {
                fusedLocationClient?.removeLocationUpdates(it)
                locationCallback = null
            }
            locationListener?.let {
                locationManager?.removeUpdates(it)
                locationListener = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
    }

    private fun processIncomingLocation(location: Location) {
        val currentState = _tripState.value
        val isRunning = currentState.status == TripStatus.RUNNING

        // If no trip is actively running (IDLE, FINISHED, or PAUSED),
        // we still compute live GPS coordinates/speed for the in-app GPS status indicator
        // WITHOUT accumulating distance or starting/resuming the fare meter.
        // The Floating Speedometer Bubble is ONLY updated if a trip is active (PAUSED).
        if (!isRunning) {
            gpsDistanceEngine.processLocation(location, isTripRunning = false)
            if (location.hasAccuracy() && location.accuracy > 50f) {
                return
            }
            val rawSpeedKmh = if (location.hasSpeed()) {
                (location.speed * 3.6f).coerceIn(0f, 220f)
            } else {
                0f
            }
            val liveSpeedKmh = if (rawSpeedKmh < 0.8f) 0f else rawSpeedKmh
            _tripState.value = currentState.copy(
                speedKmH = liveSpeedKmh,
                latitude = location.latitude,
                longitude = location.longitude
            )
            if (currentState.status == TripStatus.PAUSED) {
                floatingSpeedOverlay.updateSpeed(liveSpeedKmh)
                updateNotification()
            } else {
                floatingSpeedOverlay.hide()
            }
            return
        }

        val processResult = gpsDistanceEngine.processLocation(location, isTripRunning = true)

        if (processResult.gapDetected) {
            recordDiagnostic("GPS gap detected. Preserved ${String.format(Locale.US, "%.2f", processResult.totalDistanceKm)} km without interpolating.")
        }

        if (!processResult.accepted) {
            // Sample rejected due to accuracy (>35m), outlier speed, non-advancing time, or idle state
            Log.d(TAG, "Sample rejected: ${processResult.rejectionReason}")
            return
        }

        val currentPoint = Pair(location.latitude, location.longitude)
        val updatedRoutePoints = if (currentState.routePoints.isEmpty()) {
            listOf(currentPoint)
        } else {
            val lastPt = currentState.routePoints.last()
            val results = FloatArray(1)
            Location.distanceBetween(lastPt.first, lastPt.second, location.latitude, location.longitude, results)
            if (results[0] >= 2.5) {
                currentState.routePoints + currentPoint
            } else {
                currentState.routePoints
            }
        }

        _tripState.value = currentState.copy(
            distanceKm = processResult.totalDistanceKm,
            speedKmH = processResult.speedKmH,
            isMoving = processResult.isMoving,
            isWaitingPaused = processResult.isMoving,
            latitude = location.latitude,
            longitude = location.longitude,
            routePoints = updatedRoutePoints
        )

        recalculateFare()
        updateNotification()
        floatingSpeedOverlay.updateSpeed(processResult.speedKmH)
    }

    private fun recalculateFare() {
        _tripState.value = computeUpdatedStateWithFare(_tripState.value)
    }

    private fun startTimers() {
        if (timerJob?.isActive == true) return
        timerJob = serviceScope.launch {
            var tickCount = 0L
            while (isActive) {
                delay(1000L)
                tickCount++
                val state = _tripState.value
                if (state.status == TripStatus.RUNNING) {
                    val nextElapsed = state.durationSeconds + 1

                    var nextWait = state.waitingSeconds
                    val isMoving = state.isMoving || state.speedKmH >= state.speedThreshold
                    if (!isMoving) {
                        nextWait += 1
                    }

                    _tripState.value = state.copy(
                        durationSeconds = nextElapsed,
                        waitingSeconds = nextWait,
                        isWaitingPaused = isMoving
                    )

                    recalculateFare()
                    updateNotification()

                    if (nextElapsed % 5 == 0L) {
                        saveSnapshotToDatabase()
                    }
                }

                // Periodic self-healing for WakeLock, Foreground Service, Floating Speedometer Bubble & GPS Watchdog during active trips
                if (tickCount % 5 == 0L) {
                    val isTripActive = state.status == TripStatus.RUNNING || state.status == TripStatus.PAUSED
                    if (isTripActive) {
                        acquireWakeLock()
                        safeStartForeground(NOTIFICATION_ID, buildNotification())
                        if (floatingSpeedOverlay.isOverlayEnabled()) {
                            floatingSpeedOverlay.ensureShowing()
                        }
                    } else {
                        floatingSpeedOverlay.hide()
                    }
                }

                // Background Location Watchdog:
                // If no location callback received in past 25 seconds, refresh provider subscription
                val now = System.currentTimeMillis()
                if (lastLocationCallbackTimestamp > 0 && now - lastLocationCallbackTimestamp > 25000L) {
                    recordDiagnostic("Watchdog: GPS callbacks idle for >25s, refreshing subscription")
                    lastLocationCallbackTimestamp = now
                    withContext(Dispatchers.Main) {
                        startTrackingLocation()
                    }
                }
            }
        }
    }

    private fun stopTimers() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun saveSnapshotToDatabase() {
        val state = _tripState.value
        if (state.status == TripStatus.RUNNING || state.status == TripStatus.PAUSED) {
            serviceScope.launch(Dispatchers.IO) {
                repository.saveActiveTrip(
                    ActiveTripRecord(
                        tripNumber = state.tripNumber,
                        startTime = state.startTime,
                        distanceKm = state.distanceKm,
                        durationSeconds = state.durationSeconds,
                        waitingSeconds = state.waitingSeconds,
                        currentFare = state.currentFare,
                        isPaused = state.status == TripStatus.PAUSED,
                        baseFare = state.baseFare,
                        farePerKm = state.farePerKm,
                        waitFarePerMin = state.waitFarePerMin,
                        currency = state.currency,
                        isOutOfCity = false,
                        pickupAddress = state.pickupAddress,
                        serviceType = state.serviceType,
                        rentalBaseHours = state.rentalMinHours,
                        rentalBaseKm = state.rentalIncludedKmPerHour,
                        rentalBaseFare = state.rentalHourlyRate,
                        rentalExtraKmRate = state.rentalExtraKmRate,
                        rentalExtraHrRate = state.rentalExtraHrRate,
                        billableHours = state.billableHours,
                        outstationRatePerKm = state.outstationRatePerKm,
                        outstationMinKm = 0.0,
                        outstationDriverBata = state.outstationDriverBata,
                        outstationMinFare = state.outstationMinFare,
                        outstationWaitRate = state.outstationWaitRate,
                        outstationAdditionalCharges = state.outstationAdditionalCharges,
                        extraChargesJson = ExtraChargeItem.serialize(state.extraChargeItems),
                        routePointsJson = com.example.util.RouteCoordinatesSerializer.serialize(state.routePoints),
                        vendorId = state.vendorId,
                        vendorName = state.vendorName,
                        vendorLogoAsset = state.vendorLogoAsset,
                        vendorContactNumber = state.vendorContactNumber,
                        vendorBusinessAddress = state.vendorBusinessAddress,
                        vendorEmail = state.vendorEmail,
                        vendorGstin = state.vendorGstin,
                        vendorInvoiceDetails = state.vendorInvoiceDetails
                    )
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val strings = AppLanguageManager.currentStrings
            val channel = NotificationChannel(
                CHANNEL_ID,
                strings.notifChannelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = strings.notifReadyContent
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val state = _tripState.value
        val strings = AppLanguageManager.currentStrings
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (state.status) {
            TripStatus.RUNNING -> String.format(Locale.US, strings.notifRunningTitleFormat, Math.round(state.speedKmH))
            TripStatus.PAUSED -> strings.notifPausedTitle
            else -> strings.notifReadyTitle
        }

        val content = if (state.status == TripStatus.RUNNING || state.status == TripStatus.PAUSED) {
            "${state.currency}${String.format(Locale.US, "%.2f", state.currentFare)} • ${String.format(Locale.US, "%.2f", state.distanceKm)} km • ${state.durationSeconds / 60} min"
        } else {
            strings.notifReadyContent
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(12 * 60 * 60 * 1000L) // 12 hours max, auto-renewed by watchdog
                Log.d(TAG, "WakeLock acquired")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed acquiring WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed releasing WakeLock", e)
        }
    }

    private fun scheduleServiceKeepAliveAlarm() {
        try {
            val restartIntent = Intent(applicationContext, LocationTrackingService::class.java).apply {
                action = "APP_MINIMIZED"
            }
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    applicationContext,
                    2002,
                    restartIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    applicationContext,
                    2002,
                    restartIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                android.os.SystemClock.elapsedRealtime() + 1000L,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed scheduling service keep-alive alarm", e)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val state = _tripState.value
        val isTripActive = state.status == TripStatus.RUNNING || state.status == TripStatus.PAUSED

        if (isTripActive) {
            Log.d(TAG, "onTaskRemoved: Keeping service and speedometer bubble alive in background during active trip")
            recordDiagnostic("App minimized/task removed — keeping active trip meter & bubble alive")
            acquireWakeLock()
            if (floatingSpeedOverlay.isOverlayEnabled()) {
                floatingSpeedOverlay.show()
                floatingSpeedOverlay.updateSpeed(state.speedKmH)
            }
            scheduleServiceKeepAliveAlarm()
        } else {
            floatingSpeedOverlay.hide()
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        val state = _tripState.value
        val shouldRestart = state.status == TripStatus.RUNNING || state.status == TripStatus.PAUSED

        super.onDestroy()
        Log.d(TAG, "onDestroy: Stopping tracking service (shouldRestart=$shouldRestart)")
        recordDiagnostic("Tracking service destroyed (shouldRestart=$shouldRestart)")
        stopTrackingLocation()
        stopTimers()
        releaseWakeLock()
        if (!shouldRestart) {
            floatingSpeedOverlay.hide()
        } else {
            scheduleServiceKeepAliveAlarm()
        }
        serviceScope.cancel()
        tts?.shutdown()
    }

    override fun onBind(intent: Intent?) = null
}
