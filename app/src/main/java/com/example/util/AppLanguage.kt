package com.example.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.model.ServiceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val code: String, val englishName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    TAMIL("ta", "Tamil", "தமிழ்");

    val isTamil: Boolean
        get() = this == TAMIL

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return when (code?.trim()?.lowercase()) {
                "ta", "tamil", "தமிழ்" -> TAMIL
                else -> ENGLISH
            }
        }
    }
}

open class AppStrings(val language: AppLanguage = AppLanguage.ENGLISH) {
    // Common
    open val save: String = "Save"
    open val cancel: String = "Cancel"
    open val confirm: String = "Confirm"
    open val yes: String = "Yes"
    open val no: String = "No"
    open val close: String = "Close"
    open val done: String = "Done"
    open val back: String = "Back"
    open val edit: String = "Edit"
    open val delete: String = "Delete"
    open val settings: String = "Settings"
    open val profile: String = "Profile"
    open val aboutUs: String = "About Us"

    // Splash Screen
    open val splashSubtitle: String = "Professional Commercial Fare Meter"

    // Home / Main Dashboard
    open val deviceStatus: String = "DEVICE STATUS"
    open val acquiringGps: String = "Acquiring GPS..."
    open val gpsSignalStrong: String = "GPS Signal Strong"
    open val vacant: String = "Vacant"
    open val occupied: String = "Occupied"
    open val paused: String = "Paused"
    open val serviceTypeHeader: String = "SERVICE TYPE"
    open val fixedForTrip: String = "FIXED FOR TRIP"
    open val localMetered: String = "Local / Metered"
    open val hourlyRental: String = "Hourly Rental"
    open val outstation: String = "Outstation"
    open val localShort: String = "Local"
    open val rentalShort: String = "Rental"
    open val outstationShort: String = "Outstation"
    open val currentFare: String = "CURRENT FARE"
    open val distance: String = "DISTANCE"
    open val waitTime: String = "WAIT TIME"
    open val billableHoursLabel: String = "BILLABLE HRS"
    open val waitHaltLabel: String = "WAIT / HALT"
    open val duration: String = "DURATION"
    open val unitKm: String = "KM"
    open val unitMin: String = "MIN"
    open val unitHr: String = "HR"
    open val startTrip: String = "Start Trip"
    open val startLocalTrip: String = "START LOCAL / METERED"
    open val startRentalTrip: String = "START HOURLY RENTAL"
    open val startOutstationTrip: String = "START OUTSTATION"
    open val pauseAction: String = "PAUSE"
    open val resumeAction: String = "RESUME"
    open val endTrip: String = "End Trip"
    open val tripHistoryAndInvoices: String = "TRIP HISTORY & INVOICES"
    open val tripHistory: String = "Trip History"
    open val invoices: String = "Invoices"
    open val recordedCountFormat: String = "%d Recorded"
    open val createNewBill: String = "Create New Bill"
    open val noRecordedTripsYet: String = "No recorded trips yet."
    open val customerPrefix: String = "Customer"

    // Unfinished Ride Recovery Card
    open val unfinishedRideDetected: String = "Unfinished Ride Detected"
    open val unfinishedRideDesc: String = "A previous ride was interrupted. Would you like to restore and continue tracking?"
    open val discard: String = "Discard"
    open val resumeRide: String = "Resume Ride"

    // Battery & Overlay Permission Cards
    open val batteryCardTitle: String = "Keep GET TAXI Running Uninterrupted"
    open val batteryCardDesc: String = "Battery optimization can pause background GPS tracking. Allow unrestricted background usage to keep distance and fare accurate."
    open val allowBackgroundRunning: String = "Allow Background Running"
    open val floatingSpeedTitle: String = "Floating Speedometer"
    open val floatingSpeedSubtitle: String = "Show live speed on top of other apps"
    open val floatingSpeedDesc: String = "Allow GET TAXI to display over other apps so you can see your live speed in km/h while using Google Maps or taking calls."
    open val allowFloatingSpeed: String = "Allow Floating Speedometer"

    // Map Controls & Active Trip Map-First / Meter Details Switch
    open val mapStreetMode: String = "Street Mode"
    open val mapDarkMode: String = "Dark Mode"
    open val mapVectorRadar: String = "Vector Radar"
    open val mapGoogleView: String = "Google View"
    open val meterDetailsButton: String = "Meter Details"
    open val returnToMapButton: String = "Back to Live Map"
    open val liveMapActiveBadge: String = "LIVE CONSOLE MAP"
    open val tripTrackingActiveNote: String = "Live GPS route & fare tracking active in background"

    // Multi-Vendor Selection & Management
    open val selectVendorHeader: String = "SELECT VENDOR (FOR THIS TRIP)"
    open val activeTripVendorHeader: String = "ACTIVE TRIP VENDOR"
    open val changeVendorButton: String = "Change Vendor"
    open val manageVendorsButton: String = "Manage Vendors"
    open val vendorLockedForTrip: String = "LOCKED FOR TRIP"
    open val perTripVendorHint: String = "Applies to this trip & its invoice only"
    open val selectVendorDialogTitle: String = "Select Trip Vendor"
    open val selectVendorDialogSubtitle: String = "Choose the vendor brand for this trip's header, summary & official invoice."
    open val vendorManagementTitle: String = "Vendor Management"
    open val vendorManagementSubtitle: String = "Add, edit, enable/disable vendors & invoice details"
    open val addNewVendorButton: String = "Add Vendor"
    open val editVendorDetailsTitle: String = "Edit Vendor & Invoice Details"
    open val addNewVendorTitle: String = "Add New Vendor"
    open val vendorBusinessNameLabel: String = "Business / Vendor Name *"
    open val vendorContactNumberLabel: String = "Contact Number (Optional)"
    open val vendorBusinessAddressLabel: String = "Business Address (Optional)"
    open val vendorEmailLabel: String = "Email Address (Optional)"
    open val vendorGstinLabel: String = "Tax / GSTIN Number (Optional)"
    open val vendorInvoiceNotesLabel: String = "Invoice Footer / Display Notes (Optional)"
    open val vendorActiveStatusLabel: String = "Active in Trip Selector"
    open val vendorInactiveStatusLabel: String = "Inactive"
    open val vendorPermanentDefaultBadge: String = "DEFAULT • PERMANENT"
    open val vendorHistoricalProtectionNote: String = "Changes apply to future trips only. Previously completed trips & invoices remain unchanged."
    open val vendorSavedToast: String = "Vendor profile saved!"
    open val vendorBrandingLabel: String = "Vendor Brand"

    // Active Meter Extra Charges Dialog
    open val addExtraChargesTitle: String = "Add Extra Charges"
    open val addExtraChargesDesc: String = "Add toll, parking, permit, or custom charges. They update the live meter fare immediately and appear itemized on the final receipt."
    open val presetToll: String = "Toll"
    open val presetParking: String = "Parking"
    open val presetPermit: String = "Permit"
    open val presetAirportFee: String = "Airport Fee"
    open val presetWaiting: String = "Waiting"
    open val presetLuggage: String = "Luggage"
    open val chargeTypeLabel: String = "Charge Type / Label"
    open val chargeTypePlaceholder: String = "e.g. Toll, Parking, Permit"
    open val amountLabelFormat: String = "Amount (%s) *"
    open val amountErrorInvalid: String = "Please enter a valid amount greater than 0."
    open val addToLiveMeterFare: String = "Add to Live Meter Fare"
    open val addedExtraChargesHeader: String = "ADDED EXTRA CHARGES"
    open val totalPrefix: String = "Total"
    open val extrasPillPrefix: String = "Extras"
    open val extraChargeFallback: String = "Extra Charge"

    // Create Manual Bill Dialog
    open val createManualBillTitle: String = "Create New Bill / Invoice"
    open val createManualBillDesc: String = "Enter trip, passenger, and fare details to generate an editable PDF invoice & QR receipt."
    open val customerNameLabel: String = "Customer Name"
    open val customerNamePlaceholder: String = "e.g. Ramesh Kumar"
    open val customerMobileLabel: String = "Customer Mobile"
    open val customerMobileWhatsappLabel: String = "Customer Mobile (WhatsApp/SMS)"
    open val customerAddressOptionalLabel: String = "Customer Address / Company (Optional)"
    open val customerAddressNotesLabel: String = "Customer Address / Notes"
    open val pickupPointLabel: String = "Pickup Point"
    open val dropPointLabel: String = "Drop Point"
    open val distanceKmLabel: String = "Distance (km)"
    open val durationMinLabel: String = "Duration (min)"
    open val baseFareLabelFormat: String = "Base Fare (%s)"
    open val extraChargesLabelFormat: String = "Extra Charges (%s)"
    open val finalTotalFareLabelFormat: String = "Final Total Fare (%s) *"
    open val generateBillButton: String = "Generate Bill & Open Invoice"

    // Settings Screen
    open val settingsScreenTitle: String = "Settings"
    open val saveSettingsButton: String = "Save"
    open val settingsSavedSuccess: String = "Tariff settings saved successfully!"
    open val languageSectionTitle: String = "APP LANGUAGE / மொழி"
    open val languageSectionSubtitle: String = "Select your preferred display language"
    open val standardTariffsTitle: String = "Standard Commercial Tariffs"
    open val standardTariffsSubtitle: String = "Local ₹100/₹30/₹2 • Rental ₹350 • Outstation ₹30/₹300"
    open val refillButton: String = "Refill"
    open val driverVehicleProfileTitle: String = "Driver & Vehicle Profile"
    open val driverVehicleProfileSubtitle: String = "Photo, Registration, UPI ID & Permissions Wizard"
    open val editProfileButton: String = "Profile"
    open val aboutUsCardTitle: String = "About Us"
    open val aboutUsCardSubtitle: String = "GET TAXI • Company & App Information"
    open val viewAboutButton: String = "View"
    open val floatingBubbleCardTitle: String = "Floating Speedometer Bubble"
    open val floatingBubbleCardSubtitle: String = "Show live km/h bubble on home screen when minimized"
    open val grantOverlayPermissionButton: String = "Grant Display Over Other Apps Permission"
    open val localTariffHeader: String = "Local / Metered Tariff (Base ₹100, ₹30/km, ₹2/min)"
    open val baseFareSettingLabel: String = "Base / Flag Drop Fare (%s) - Default: 100"
    open val distanceRateSettingLabel: String = "Distance Rate (%s / km) - Default: 30"
    open val waitingRateSettingLabel: String = "Waiting Charge (%s / min) - Default: 2"
    open val rentalTariffHeader: String = "Hourly Rental Tariff (₹350/hr, ₹20/extra km)"
    open val rentalHourlyRateSettingLabel: String = "1 Hour Base Package Rate (%s / hr) - Default: 350"
    open val rentalMinHoursSettingLabel: String = "Minimum Rental Hours - Default: 1"
    open val rentalExtraHrSettingLabel: String = "Additional Hour Charge (%s / hr) - Default: 350"
    open val rentalExtraKmSettingLabel: String = "Extra Distance Rate (%s / km) - Default: 20"
    open val outstationTariffHeader: String = "Outstation Tariff (₹30/km, ₹300 Driver Bata)"
    open val outstationRateSettingLabel: String = "Outstation Distance Rate (%s / km) - Default: 30"
    open val outstationBataSettingLabel: String = "Driver Allowance / Bata (%s) - Default: 300"
    open val outstationMinFareSettingLabel: String = "Minimum Outstation Fare (%s) (Optional)"
    open val outstationWaitRateSettingLabel: String = "Waiting / Halt Rate (%s / hr) (Optional)"
    open val outstationAddnChargesSettingLabel: String = "Additional Charges (Toll / Permit) (%s) (Optional)"
    open val otherSettingsHeader: String = "Other Settings"
    open val waitSpeedThresholdLabel: String = "Waiting Speed Threshold (km/h)"
    open val gpsCalibrationFactorLabel: String = "GPS Distance Calibration Factor (Default: 1.000)"
    open val gpsCalibrationFactorDesc: String = "Keep at 1.000 for standard GPS accuracy. Adjust slightly (0.900 - 1.150) only if calibrating against a certified vehicle odometer."
    open val currencySymbolLabel: String = "Currency Symbol"
    open val soundVoiceTitle: String = "Sound & Voice Alerts"
    open val soundVoiceSubtitle: String = "Announce meter start, pause, and completion"
    open val resetDialogTitle: String = "Refill Standard Tariffs?"
    open val resetDialogMessage: String = "This will populate the fields with the standard commercial rates:\n• Local: ₹100 base, ₹30/km, ₹2/min wait\n• Rental: ₹350/hr, ₹20/extra km\n• Outstation: ₹30/km, ₹300 Driver Bata"
    open val refillStandardButton: String = "Refill Standard"

    // Driver Profile Screen
    open val driverProfileScreenTitle: String = "Profile"
    open val registerSaveProfileButton: String = "Save"
    open val profileSavedBannerText: String = "Driver profile, vehicle & UPI details registered!"
    open val profileSavedToast: String = "Driver Profile Registered & Saved!"
    open val paymentQrSavedToast: String = "Payment QR Code saved to device!"
    open val qrCouldNotSaveToast: String = "Could not save QR image"
    open val driverProfilePictureHeader: String = "Driver Profile Picture"
    open val uploadProfilePicGallery: String = "Upload Profile Picture from Gallery"
    open val changeProfilePicGallery: String = "Change Profile Picture from Gallery"
    open val removePicture: String = "Remove Picture"
    open val driverVehicleRegHeader: String = "Driver & Vehicle Registration"
    open val driverNameRequiredLabel: String = "Driver Full Name *"
    open val mobileNumberRequiredLabel: String = "Mobile Number *"
    open val vehicleModelRequiredLabel: String = "Vehicle Make & Model *"
    open val vehicleNumberRequiredLabel: String = "Vehicle Registration Number *"
    open val driverLicenseOptionalLabel: String = "Driver License / Badge No. (Optional)"
    open val vehicleCategoryLabel: String = "Vehicle Category"
    open val uploadPaymentQrHeader: String = "Upload Payment QR Code (Gallery)"
    open val uploadPaymentQrDesc: String = "Upload your GPay, PhonePe, or Paytm merchant QR code screenshot/photo from your phone gallery. It will be shown directly beside the final fare after every trip."
    open val noQrUploaded: String = "No QR Image Uploaded"
    open val tapBelowToChooseGallery: String = "Tap below to choose from Gallery"
    open val tapToUpload: String = "Tap to Upload"
    open val tapQrToEnlarge: String = "Tap QR to enlarge"
    open val uploadPaymentQrGalleryButton: String = "Upload Payment QR Code from Gallery"
    open val changePaymentQrGalleryButton: String = "Change Payment QR Code from Gallery"
    open val removeUploadedQrButton: String = "Remove Uploaded QR Image"
    open val driverUpiOptionalLabel: String = "Driver Payment UPI ID (Optional Fallback)"
    open val permissionsOnboardingHeader: String = "Permissions Onboarding"
    open val permissionsOnboardingTitle: String = "4-Step Setup for Uninterrupted Tracking & Bubble"
    open val startWizardButton: String = "Start Wizard"
    open val permStep1Title: String = "Location Permission (Allow All the Time)"
    open val permStep1Subtitle: String = "Required for background GPS distance & speed calculation"
    open val permStep2Title: String = "Overlay Permission (Display Over Other Apps)"
    open val permStep2Subtitle: String = "Required for floating speedometer bubble on home screen"
    open val permStep3Title: String = "Ignore Battery Optimizations"
    open val permStep3Subtitle: String = "Prevents Android OS from killing the meter during long trips"
    open val permStep4Title: String = "Notification Permission"
    open val permStep4Subtitle: String = "Displays live fare & speed in the status bar"
    open val grantedStatus: String = "Granted"
    open val allowAction: String = "Allow"
    open val stepOf4Format: String = "STEP %d OF 4 • DRIVER ONBOARDING"
    open val wizardStep1Desc: String = "To calculate accurate distance and speed even when your screen is locked or you switch to Google Maps, please grant Location permission and select 'Allow all the time'."
    open val grantLocationPermButton: String = "Grant Location Permission"
    open val locationPermGrantedBadge: String = "Location Permission Granted"
    open val wizardStep2Desc: String = "Allow GET TAXI to display the floating speedometer bubble on top of your home screen or navigation apps when you minimize the meter."
    open val enableOverlayPermButton: String = "Enable Display Over Other Apps"
    open val overlayPermGrantedBadge: String = "Overlay Permission Granted"
    open val wizardStep3Title: String = "3. Disable Battery Optimization"
    open val wizardStep3Desc: String = "Prevent Android from putting the taxi meter to sleep during long outstation or waiting trips by allowing unrestricted battery usage."
    open val disableBatteryOptButton: String = "Disable Battery Optimization"
    open val batteryExemptionActiveBadge: String = "Battery Exemption Active"
    open val wizardStep4Desc: String = "Allow notifications so the foreground service can display your live fare, distance, and speed in the notification bar."
    open val allowNotificationsButton: String = "Allow Notifications"
    open val notificationsEnabledBadge: String = "Notifications Enabled"
    open val previousButton: String = "Previous"
    open val nextStepButton: String = "Next Step →"
    open val finishSetupButton: String = "Finish Setup"
    open val onboardingCompleteToast: String = "Onboarding setup complete!"

    // Trip Summary & Receipt Screens
    open val tripSummaryScreenTitle: String = "Trip Summary & Payment"
    open val tripReceiptScreenTitle: String = "Invoices"
    open val editBillButton: String = "Edit Bill"
    open val tripNotFoundMessage: String = "Trip record could not be found."
    open val backToHomeButton: String = "Back to Home"
    open val tripCompletedBadge: String = "TRIP COMPLETED"
    open val servicePrefix: String = "SERVICE"
    open val finalBillAmountLabel: String = "FINAL BILL AMOUNT"
    open val includesExtraFormat: String = "Includes +%s%s Extra Charges"
    open val tripIdPrefix: String = "TRIP ID"
    open val scanToPay: String = "SCAN TO PAY"
    open val editBillSummaryButton: String = "Edit Final Bill / Extra Charges / Customer Info"
    open val tripPartiesDetailsHeader: String = "TRIP & PARTIES DETAILS"
    open val partiesDetailsHeader: String = "PARTIES & TRIP DETAILS"
    open val editBillCustomerButton: String = "Edit Bill / Customer"
    open val notSpecifiedTapEdit: String = "Not specified (Tap Edit to add)"
    open val driverNameLabel: String = "Driver Name"
    open val vehicleRegLabel: String = "Vehicle Reg No."
    open val vehicleTypeLabel: String = "Vehicle Type"
    open val passengerNameLabel: String = "Passenger Name"
    open val passengerPhoneLabel: String = "Passenger Phone"
    open val passengerAddressLabel: String = "Passenger Address"
    open val tripMetricsHeader: String = "TRIP METRICS"
    open val startTimeLabel: String = "Start Time"
    open val endTimeLabel: String = "End Time"
    open val totalDistanceLabel: String = "Total Distance"
    open val totalDurationLabel: String = "Total Duration"
    open val totalWaitingTimeLabel: String = "Total Waiting Time"
    open val gpsCoordinatesFallback: String = "GPS Coordinates"
    open val calculatedFareDetailsHeader: String = "CALCULATED FARE DETAILS (EDITABLE FOR PDF)"
    open val fareBreakdownHeader: String = "FARE BREAKDOWN"
    open val baseFlagDropFareLabel: String = "Base / Flag Drop Fare"
    open val baseFareLabel: String = "Base Fare"
    open val distanceFareLabel: String = "Distance Fare"
    open val waitingDelayLabel: String = "Waiting / Traffic Delay"
    open val waitingFareLabel: String = "Waiting Fare"
    open val hourlyChargeLabel: String = "Hourly Package Charge"
    open val includedDistanceAllowanceLabel: String = "Included Distance Allowance"
    open val actualDistanceDrivenLabel: String = "Actual Distance Driven"
    open val extraDistanceChargeLabel: String = "Extra Distance Charge"
    open val withinIncludedLimitLabel: String = "Within Included Limit"
    open val outstationDistanceChargeLabel: String = "Outstation Distance Charge"
    open val driverAllowanceBataLabel: String = "Driver Allowance (Bata)"
    open val itemizedExtraChargesHeader: String = "ITEMIZED EXTRA CHARGES"
    open val additionalChargesLabel: String = "Additional Charges"
    open val extraChargesLabel: String = "Extra Charges"
    open val finalBillTotalLabel: String = "Final Bill Total"
    open val totalPayableLabel: String = "TOTAL PAYABLE"
    open val allTariffsIncludedLabel: String = "All Applicable Tariffs Included"
    open val officialTripInvoiceHeader: String = "OFFICIAL TRIP INVOICE"
    open val upiQrPaymentHeader: String = "UPI QR CODE PAYMENT"
    open val setUpiIdButton: String = "SET UPI ID"
    open val editUpiButton: String = "EDIT UPI"
    open val saveQrButton: String = "Save QR"
    open val shareQrButton: String = "Share QR"
    open val qrSavedGalleryToast: String = "QR saved to Gallery!"
    open val setUpiPromptText: String = "Set your UPI ID in Driver Profile or tap 'SET UPI ID' above to display a payment QR code."
    open val shareBillHeader: String = "SHARE BILL (WHATSAPP / SMS / SHARE)"
    open val whatsappShareButton: String = "WhatsApp / Share"
    open val shareReceiptWhatsappTelegramButton: String = "Share Receipt (WhatsApp / Telegram)"
    open val sendSmsButton: String = "Send SMS"
    open val copyTextButton: String = "Copy Text"
    open val receiptCopiedToast: String = "Receipt text copied to clipboard!"
    open val formalPdfHeader: String = "FORMAL PDF DOCUMENTS (EDITABLE BEFORE GENERATION)"
    open val customerPdfButton: String = "Customer PDF"
    open val vendorPdfButton: String = "Vendor PDF"
    open val customerInvoiceA4PdfButton: String = "Customer Invoice A4 PDF"
    open val vendorAuditPdfButton: String = "Vendor Audit Statement PDF"
    open val viewOfficialInvoiceButton: String = "View Official Invoice / Tax Bill"
    open val doneReturnToMeterButton: String = "DONE (RETURN TO METER)"
    open val editBillDialogTitle: String = "Edit Bill & Customer Info"
    open val editBillDialogDesc: String = "Customize final fare, add extra charges, or edit customer details before generating PDF / sharing."
    open val saveBillButton: String = "Save"
    open val billUpdatedToast: String = "Bill & Invoice details updated!"
    open val driverUpiDialogTitle: String = "Driver Payment UPI ID"
    open val driverUpiDialogDesc: String = "Enter your UPI ID (GPay / PhonePe / Paytm) to generate an offline scan-and-pay QR code."
    open val yourUpiIdLabel: String = "Your UPI ID"
    open val saveUpiIdButton: String = "Save"
    open val upiSavedToast: String = "Driver UPI ID saved!"
    open val changeQrButton: String = "Change QR"

    // Service Notification Strings
    open val notifChannelName: String = "GET TAXI Trip Meter Service"
    open val notifRunningTitleFormat: String = "GET TAXI — Occupied (%d km/h)"
    open val notifPausedTitle: String = "GET TAXI — Ride Paused"
    open val notifReadyTitle: String = "GET TAXI — Vacant"
    open val notifReadyContent: String = "GPS ready • Start a trip on the meter screen"

    val isTamil: Boolean
        get() = language == AppLanguage.TAMIL

    fun serviceTypeTitle(mode: ServiceType): String {
        return when (mode) {
            ServiceType.LOCAL -> localMetered
            ServiceType.RENTAL -> hourlyRental
            ServiceType.OUTSTATION -> outstation
        }
    }

    fun serviceTypeShort(mode: ServiceType): String {
        return when (mode) {
            ServiceType.LOCAL -> localShort
            ServiceType.RENTAL -> rentalShort
            ServiceType.OUTSTATION -> outstationShort
        }
    }

    fun startServiceButtonText(mode: ServiceType): String {
        return when (mode) {
            ServiceType.LOCAL -> startLocalTrip
            ServiceType.RENTAL -> startRentalTrip
            ServiceType.OUTSTATION -> startOutstationTrip
        }
    }

    fun startTripLabelForMode(mode: ServiceType): String {
        return startServiceButtonText(mode)
    }

    fun translateExtraChargePreset(label: String): String {
        if (!isTamil) return label
        return when (label.trim().lowercase()) {
            "toll", "சுங்கக் கட்டணம்" -> presetToll
            "parking", "பார்க்கிங்" -> presetParking
            "permit", "பெர்மிட்" -> presetPermit
            "airport fee", "விமான நிலைய கட்டணம்" -> presetAirportFee
            "waiting", "காத்திருப்பு" -> presetWaiting
            "luggage", "லக்கேஜ்" -> presetLuggage
            else -> label
        }
    }

    fun translateVehicleCategory(category: String): String {
        if (!isTamil) return category
        return when (category.trim().lowercase()) {
            "sedan", "sedan taxi" -> "செடான்"
            "suv", "suv / muv" -> "எஸ்யூவி (SUV)"
            "hatchback", "mini / hatchback" -> "ஹேட்ச்பேக்"
            "muv / innova", "innova", "muv" -> "இன்னோவா / MUV"
            "tempo traveller" -> "டெம்போ டிராவலர்"
            else -> category
        }
    }
}

val EnglishStrings: AppStrings = AppStrings(AppLanguage.ENGLISH)

val TamilStrings: AppStrings = object : AppStrings(AppLanguage.TAMIL) {
    override val save = "சேமி"
    override val cancel = "ரத்து செய்"
    override val confirm = "உறுதிப்படுத்து"
    override val yes = "ஆம்"
    override val no = "இல்லை"
    override val close = "மூடு"
    override val done = "முடிந்தது"
    override val back = "பின்செல்"
    override val edit = "திருத்து"
    override val delete = "நீக்கு"
    override val settings = "அமைப்புகள்"
    override val profile = "சுயவிவரம்"
    override val aboutUs = "எங்களைப் பற்றி"

    override val splashSubtitle = "தொழில்முறை டாக்ஸி கட்டண மீட்டர்"

    override val deviceStatus = "சாதன நிலை"
    override val acquiringGps = "GPS இருப்பிடத்தைப் பெறுகிறது..."
    override val gpsSignalStrong = "GPS இணைப்பு சீராக உள்ளது"
    override val vacant = "காலியாக உள்ளது"
    override val occupied = "பயணத்தில் உள்ளது"
    override val paused = "இடைநிறுத்தப்பட்டது"
    override val serviceTypeHeader = "சேவை வகை"
    override val fixedForTrip = "பயணத்திற்கு நிலையானது"
    override val localMetered = "உள்ளூர் / மீட்டர்"
    override val hourlyRental = "மணிநேர வாடகை"
    override val outstation = "வெளியூர் பயணம்"
    override val localShort = "உள்ளூர்"
    override val rentalShort = "வாடகை"
    override val outstationShort = "வெளியூர்"
    override val currentFare = "தற்போதைய கட்டணம்"
    override val distance = "பயண தூரம்"
    override val waitTime = "காத்திருப்பு நேரம்"
    override val billableHoursLabel = "கணக்கிடப்படும் மணிநேரம்"
    override val waitHaltLabel = "காத்திருப்பு நேரம்"
    override val duration = "பயண நேரம்"
    override val unitKm = "கி.மீ"
    override val unitMin = "நிமி"
    override val unitHr = "மணி"
    override val startTrip = "பயணத்தைத் தொடங்கு"
    override val startLocalTrip = "பயணத்தைத் தொடங்கு (உள்ளூர் / மீட்டர்)"
    override val startRentalTrip = "பயணத்தைத் தொடங்கு (மணிநேர வாடகை)"
    override val startOutstationTrip = "பயணத்தைத் தொடங்கு (வெளியூர் பயணம்)"
    override val pauseAction = "இடைநிறுத்து"
    override val resumeAction = "தொடர்"
    override val endTrip = "பயணத்தை முடி"
    override val tripHistoryAndInvoices = "பயண வரலாறு & ரசீதுகள்"
    override val tripHistory = "பயண வரலாறு"
    override val invoices = "ரசீதுகள்"
    override val recordedCountFormat = "%d பதிவுகள்"
    override val createNewBill = "புதிய ரசீதை உருவாக்கு"
    override val noRecordedTripsYet = "இதுவரை பயணங்கள் எதுவும் பதிவாகவில்லை"
    override val customerPrefix = "வாடிக்கையாளர்"

    override val unfinishedRideDetected = "முடிக்கப்படாத பயணம் கண்டறியப்பட்டது"
    override val unfinishedRideDesc = "கடந்த பயணம் பாதியில் நின்றது. அதை மீண்டும் தொடர்ந்து கணக்கிட விரும்புகிறீர்களா?"
    override val discard = "வேண்டாம்"
    override val resumeRide = "பயணத்தைத் தொடர்"

    override val batteryCardTitle = "பேட்டரி தடையின்றி GET TAXI இயங்க அனுமதிக்கவும்"
    override val batteryCardDesc = "பேட்டரி கட்டுப்பாடு பின்னணி GPS கணக்கீட்டை நிறுத்தக்கூடும். இதை அனுமதிப்பதன் மூலம் பயண தூரம் துல்லியமாக கணக்கிடப்படும்."
    override val allowBackgroundRunning = "பின்னணியில் இயங்க அனுமதி"
    override val floatingSpeedTitle = "மிதக்கும் வேகக் காட்டி"
    override val floatingSpeedSubtitle = "மற்ற செயலிகளின் மேல் நேரடி வேகத்தைக் காட்டு"
    override val floatingSpeedDesc = "பயணம் நடக்கும்போது மற்ற செயலிகளின் மேல் வாகனத்தின் வேகத்தைக் காட்ட GET TAXI-க்கு அனுமதி அளிக்கவும்."
    override val allowFloatingSpeed = "மிதக்கும் வேகக் காட்டியை அனுமதி"

    override val mapStreetMode = "சாலை வரைபடம்"
    override val mapDarkMode = "இருள் வரைபடம்"
    override val mapVectorRadar = "ரேடார் காட்சி"
    override val mapGoogleView = "கூகுள் வரைபடம்"
    override val meterDetailsButton = "Meter Details • மீட்டர் விவரங்கள்"
    override val returnToMapButton = "வரைபடத்திற்குத் திரும்பு (Back to Map)"
    override val liveMapActiveBadge = "நேரடி பயண வரைபடம்"
    override val tripTrackingActiveNote = "GPS பயண தூரம் மற்றும் கட்டணக் கணக்கீடு செயலில் உள்ளது"

    override val selectVendorHeader = "பயண நிறுவனத்தைத் தேர்ந்தெடுக்கவும் (இந்தப் பயணத்திற்கு)"
    override val activeTripVendorHeader = "பயண நிறுவனம் (VENDOR)"
    override val changeVendorButton = "நிறுவனத்தை மாற்று"
    override val manageVendorsButton = "நிறுவன நிர்வாகம்"
    override val vendorLockedForTrip = "பயணத்திற்கு நிலையானது"
    override val perTripVendorHint = "இந்தப் பயணத்திற்கும் அதன் ரசீதுக்கும் மட்டும் பொருந்தும்"
    override val selectVendorDialogTitle = "பயண நிறுவனத்தைத் தேர்ந்தெடுக்கவும்"
    override val selectVendorDialogSubtitle = "இந்தப் பயணத்தின் தலைப்பு மற்றும் ரசீதுக்கான நிறுவனத்தைத் தேர்வு செய்யவும்."
    override val vendorManagementTitle = "நிறுவன நிர்வாகம் (Vendor Management)"
    override val vendorManagementSubtitle = "நிறுவனங்களைச் சேர்க்க, திருத்த மற்றும் ரசீது விவரங்களை நிர்வகிக்க"
    override val addNewVendorButton = "புதிய நிறுவனம் சேர்"
    override val editVendorDetailsTitle = "நிறுவனம் & ரசீது விவரங்களைத் திருத்து"
    override val addNewVendorTitle = "புதிய நிறுவனத்தைச் சேர்"
    override val vendorBusinessNameLabel = "நிறுவனத்தின் பெயர் *"
    override val vendorContactNumberLabel = "தொடர்பு எண் (விருப்பப்பட்டால்)"
    override val vendorBusinessAddressLabel = "நிறுவன முகவரி (விருப்பப்பட்டால்)"
    override val vendorEmailLabel = "மின்னஞ்சல் (விருப்பப்பட்டால்)"
    override val vendorGstinLabel = "GSTIN / வரி எண் (விருப்பப்பட்டால்)"
    override val vendorInvoiceNotesLabel = "ரசீது குறிப்புகள் (விருப்பப்பட்டால்)"
    override val vendorActiveStatusLabel = "பயணத் தேர்வில் செயலில் உள்ளது"
    override val vendorInactiveStatusLabel = "செயலற்றது"
    override val vendorPermanentDefaultBadge = "நிலையான முதன்மை நிறுவனம்"
    override val vendorHistoricalProtectionNote = "மாற்றங்கள் எதிர்காலப் பயணங்களுக்கு மட்டுமே பொருந்தும். முந்தைய ரசீதுகள் மாறாது."
    override val vendorSavedToast = "நிறுவன விவரங்கள் சேமிக்கப்பட்டன!"
    override val vendorBrandingLabel = "பயண நிறுவனம்"

    override val addExtraChargesTitle = "கூடுதல் கட்டணங்கள் சேர்"
    override val addExtraChargesDesc = "சுங்கக் கட்டணம், பார்க்கிங், பெர்மிட் போன்ற கூடுதல் கட்டணங்களைச் சேர்க்கவும். இவை உடனடியாக மீட்டர் கட்டணத்திலும் இறுதி ரசீதிலும் சேர்க்கப்படும்."
    override val presetToll = "சுங்கக் கட்டணம்"
    override val presetParking = "பார்க்கிங்"
    override val presetPermit = "பெர்மிட்"
    override val presetAirportFee = "விமான நிலைய கட்டணம்"
    override val presetWaiting = "காத்திருப்பு"
    override val presetLuggage = "லக்கேஜ்"
    override val chargeTypeLabel = "கட்டண வகை / விவரம்"
    override val chargeTypePlaceholder = "எ.கா. சுங்கக் கட்டணம், பார்க்கிங், பெர்மிட்"
    override val amountLabelFormat = "தொகை (%s) *"
    override val amountErrorInvalid = "சரியான தொகையை உள்ளிடவும்."
    override val addToLiveMeterFare = "தற்போதைய கட்டணத்தில் சேர்"
    override val addedExtraChargesHeader = "சேர்க்கப்பட்ட கூடுதல் கட்டணங்கள்"
    override val totalPrefix = "மொத்தம்"
    override val extrasPillPrefix = "கூடுதல்"
    override val extraChargeFallback = "கூடுதல் கட்டணம்"

    override val createManualBillTitle = "புதிய ரசீதை உருவாக்கு"
    override val createManualBillDesc = "பயணம், வாடிக்கையாளர் மற்றும் கட்டண விவரங்களை உள்ளிட்டு PDF ரசீது மற்றும் QR ரசீதை உருவாக்கவும்."
    override val customerNameLabel = "வாடிக்கையாளர் பெயர்"
    override val customerNamePlaceholder = "எ.கா. ரமேஷ் குமார்"
    override val customerMobileLabel = "வாடிக்கையாளர் கைபேசி எண்"
    override val customerMobileWhatsappLabel = "வாடிக்கையாளர் கைபேசி எண் (WhatsApp/SMS)"
    override val customerAddressOptionalLabel = "வாடிக்கையாளர் முகவரி / நிறுவனம் (விருப்பப்பட்டால்)"
    override val customerAddressNotesLabel = "வாடிக்கையாளர் முகவரி / குறிப்புகள்"
    override val pickupPointLabel = "புறப்பட்ட இடம்"
    override val dropPointLabel = "சேருமிடம்"
    override val distanceKmLabel = "பயண தூரம் (கி.மீ)"
    override val durationMinLabel = "பயண நேரம் (நிமிடம்)"
    override val baseFareLabelFormat = "அடிப்படை கட்டணம் (%s)"
    override val extraChargesLabelFormat = "கூடுதல் கட்டணம் (%s)"
    override val finalTotalFareLabelFormat = "மொத்த கட்டணத் தொகை (%s) *"
    override val generateBillButton = "புதிய ரசீதை உருவாக்கு"

    override val settingsScreenTitle = "அமைப்புகள்"
    override val saveSettingsButton = "சேமி"
    override val settingsSavedSuccess = "கட்டண அமைப்புகள் வெற்றிகரமாக சேமிக்கப்பட்டன!"
    override val languageSectionTitle = "பயன்பாட்டு மொழி / LANGUAGE"
    override val languageSectionSubtitle = "உங்களுக்கு விருப்பமான மொழியைத் தேர்ந்தெடுக்கவும்"
    override val standardTariffsTitle = "நிலையான கட்டண விவரங்கள்"
    override val standardTariffsSubtitle = "உள்ளூர் ₹100/₹30/₹2 • வாடகை ₹350 • வெளியூர் ₹30/₹300"
    override val refillButton = "மீட்டமை"
    override val driverVehicleProfileTitle = "ஓட்டுநர் & வாகன சுயவிவரம்"
    override val driverVehicleProfileSubtitle = "ஓட்டுநர் விவரம், புகைப்படம், UPI மற்றும் அனுமதிகள்"
    override val editProfileButton = "சுயவிவரம்"
    override val aboutUsCardTitle = "எங்களைப் பற்றி"
    override val aboutUsCardSubtitle = "GET TAXI • நிறுவனம் & செயலி விவரங்கள்"
    override val viewAboutButton = "விவரம் காண்க"
    override val floatingBubbleCardTitle = "மிதக்கும் வேகக் காட்டி"
    override val floatingBubbleCardSubtitle = "பயணத்தின் போது செயலியைச் சுருக்கினால் முகப்புத் திரையில் வேகத்தைக் காட்டும்"
    override val grantOverlayPermissionButton = "மேலடுக்கு அனுமதியை வழங்கவும்"
    override val localTariffHeader = "உள்ளூர் / மீட்டர் கட்டணம் (அடிப்படை ₹100, ₹30/கி.மீ, ₹2/நிமி)"
    override val baseFareSettingLabel = "அடிப்படை கட்டணம் (%s) - வழக்கம்: 100"
    override val distanceRateSettingLabel = "ஒரு கி.மீ கட்டணம் (%s / கி.மீ) - வழக்கம்: 30"
    override val waitingRateSettingLabel = "காத்திருப்பு கட்டணம் (%s / நிமிடம்) - வழக்கம்: 2"
    override val rentalTariffHeader = "மணிநேர வாடகை கட்டணம் (₹350/மணி, கூடுதல் ₹20/கி.மீ)"
    override val rentalHourlyRateSettingLabel = "1 மணிநேர அடிப்படை வாடகை (%s / மணி) - வழக்கம்: 350"
    override val rentalMinHoursSettingLabel = "குறைந்தபட்ச வாடகை மணிநேரம் - வழக்கம்: 1"
    override val rentalExtraHrSettingLabel = "கூடுதல் மணிநேர கட்டணம் (%s / மணி) - வழக்கம்: 350"
    override val rentalExtraKmSettingLabel = "கூடுதல் தூர கட்டணம் (%s / கி.மீ) - வழக்கம்: 20"
    override val outstationTariffHeader = "வெளியூர் பயண கட்டணம் (₹30/கி.மீ, ₹300 ஓட்டுநர் பேட்டா)"
    override val outstationRateSettingLabel = "வெளியூர் கி.மீ கட்டணம் (%s / கி.மீ) - வழக்கம்: 30"
    override val outstationBataSettingLabel = "ஓட்டுநர் பேட்டா (%s) - வழக்கம்: 300"
    override val outstationMinFareSettingLabel = "குறைந்தபட்ச வெளியூர் கட்டணம் (%s) (விருப்பப்பட்டால்)"
    override val outstationWaitRateSettingLabel = "காத்திருப்பு கட்டணம் (%s / மணி) (விருப்பப்பட்டால்)"
    override val outstationAddnChargesSettingLabel = "கூடுதல் கட்டணங்கள் (%s) (விருப்பப்பட்டால்)"
    override val otherSettingsHeader = "மற்ற அமைப்புகள்"
    override val waitSpeedThresholdLabel = "காத்திருப்பு வேக வரம்பு (கி.மீ/மணி)"
    override val gpsCalibrationFactorLabel = "GPS தூர துல்லிய அளவு (வழக்கம்: 1.000)"
    override val gpsCalibrationFactorDesc = "வழக்கமான GPS கணக்கீட்டிற்கு 1.000 என வைக்கவும். வாகன ஓடோமீட்டருடன் ஒப்பிடும்போது மட்டும் மாற்றவும் (0.900 - 1.150)."
    override val currencySymbolLabel = "நாணயக் குறியீடு"
    override val soundVoiceTitle = "ஒலி & குரல் அறிவிப்புகள்"
    override val soundVoiceSubtitle = "மீட்டர் தொடக்கம், இடைநிறுத்தம் மற்றும் முடிவில் குரல் அறிவிப்பு"
    override val resetDialogTitle = "நிலையான கட்டணத்திற்கு மாற்றவா?"
    override val resetDialogMessage = "கீழ்க்கண்ட நிலையான கட்டணங்கள் நிரப்பப்படும்:\n• உள்ளூர்: ₹100 அடிப்படை, ₹30/கி.மீ, ₹2/நிமி காத்திருப்பு\n• வாடகை: ₹350/மணி, கூடுதல் ₹20/கி.மீ\n• வெளியூர்: ₹30/கி.மீ, ₹300 ஓட்டுநர் பேட்டா"
    override val refillStandardButton = "உறுதிப்படுத்து"

    override val driverProfileScreenTitle = "சுயவிவரம்"
    override val registerSaveProfileButton = "சேமி"
    override val profileSavedBannerText = "ஓட்டுநர் சுயவிவரம் மற்றும் UPI விவரங்கள் சேமிக்கப்பட்டன!"
    override val profileSavedToast = "ஓட்டுநர் சுயவிவரம் சேமிக்கப்பட்டது!"
    override val paymentQrSavedToast = "கட்டண QR குறியீடு சாதனத்தில் சேமிக்கப்பட்டது!"
    override val qrCouldNotSaveToast = "QR படத்தைச் சேமிக்க முடியவில்லை"
    override val driverProfilePictureHeader = "ஓட்டுநர் புகைப்படம்"
    override val uploadProfilePicGallery = "கேலரியிலிருந்து புகைப்படத்தை பதிவேற்று"
    override val changeProfilePicGallery = "கேலரியிலிருந்து புகைப்படத்தை மாற்று"
    override val removePicture = "புகைப்படத்தை நீக்கு"
    override val driverVehicleRegHeader = "ஓட்டுநர் & வாகன பதிவு விவரங்கள்"
    override val driverNameRequiredLabel = "ஓட்டுநர் பெயர் *"
    override val mobileNumberRequiredLabel = "கைபேசி எண் *"
    override val vehicleModelRequiredLabel = "வாகன பெயர் & மாடல் *"
    override val vehicleNumberRequiredLabel = "வாகன பதிவு எண் *"
    override val driverLicenseOptionalLabel = "ஓட்டுநர் உரிமம் / பேட்ஜ் எண் (விருப்பப்பட்டால்)"
    override val vehicleCategoryLabel = "வாகன வகை"
    override val uploadPaymentQrHeader = "கட்டண QR குறியீடு பதிவேற்றம்"
    override val uploadPaymentQrDesc = "உங்கள் GPay, PhonePe அல்லது Paytm QR குறியீடு படத்தை கேலரியிலிருந்து பதிவேற்றவும். இது ஒவ்வொரு பயண முடிவிலும் கட்டணத் தொகைக்கு அருகில் காட்டப்படும்."
    override val noQrUploaded = "QR குறியீடு பதிவேற்றப்படவில்லை"
    override val tapBelowToChooseGallery = "கேலரியிலிருந்து தேர்வு செய்ய கீழே தொடவும்"
    override val tapToUpload = "பதிவேற்ற தொடவும்"
    override val tapQrToEnlarge = "பெரிதாக்க QR-ஐ தொடவும்"
    override val uploadPaymentQrGalleryButton = "கேலரியிலிருந்து கட்டண QR குறியீட்டைப் பதிவேற்று"
    override val changePaymentQrGalleryButton = "கட்டண QR குறியீட்டை மாற்று"
    override val removeUploadedQrButton = "பதிவேற்றிய QR குறியீட்டை நீக்கு"
    override val driverUpiOptionalLabel = "ஓட்டுநர் UPI ஐடி (விருப்பப்பட்டால்)"
    override val permissionsOnboardingHeader = "அனுமதிகள் அமைப்பு"
    override val permissionsOnboardingTitle = "தடையற்ற மீட்டர் & வேகக் காட்டிக்கான 4-படி அனுமதிகள்"
    override val startWizardButton = "தொடங்கு"
    override val permStep1Title = "இருப்பிட அனுமதி (எப்போதும் அனுமதிக்கவும்)"
    override val permStep1Subtitle = "பின்னணி GPS பயண தூரம் மற்றும் வேகக் கணக்கீட்டிற்கு அவசியம்"
    override val permStep2Title = "மேலடுக்கு அனுமதி (மற்ற செயலிகளின் மேல் காட்ட)"
    override val permStep2Subtitle = "முகப்புத் திரையில் மிதக்கும் வேகக் காட்டிக்கு அவசியம்"
    override val permStep3Title = "பேட்டரி கட்டுப்பாடு விலக்கு"
    override val permStep3Subtitle = "நீண்ட பயணங்களின் போது மீட்டர் நிற்காமல் இருக்க உதவும்"
    override val permStep4Title = "அறிவிப்பு அனுமதி"
    override val permStep4Subtitle = "அறிவிப்புப் பட்டியில் தற்போதைய கட்டணம் மற்றும் வேகத்தைக் காட்டும்"
    override val grantedStatus = "அனுமதிக்கப்பட்டது"
    override val allowAction = "அனுமதி"
    override val stepOf4Format = "படி %d / 4 • ஓட்டுநர் அனுமதிகள்"
    override val wizardStep1Desc = "திரை அணைந்திருந்தாலும் அல்லது செயலியைச் சுருக்கினாலும் பயண தூரமும் வேகமும் துல்லியமாகக் கணக்கிட 'எப்போதும் அனுமதி' (Allow all the time) இருப்பிட அனுமதியை வழங்கவும்."
    override val grantLocationPermButton = "இருப்பிட அனுமதி வழங்கு"
    override val locationPermGrantedBadge = "இருப்பிட அனுமதி வழங்கப்பட்டுள்ளது"
    override val wizardStep2Desc = "பயணத்தின் போது செயலியைச் சுருக்கினால் முகப்புத் திரையில் மிதக்கும் வேகக் காட்டி தோன்ற 'மற்ற செயலிகளின் மேல் காட்டு' அனுமதியை வழங்கவும்."
    override val enableOverlayPermButton = "மேலடுக்கு அனுமதியை இயக்கு"
    override val overlayPermGrantedBadge = "மேலடுக்கு அனுமதி வழங்கப்பட்டுள்ளது"
    override val wizardStep3Title = "3. பேட்டரி கட்டுப்பாட்டை நீக்கு"
    override val wizardStep3Desc = "நீண்ட நேர பயணங்களின் போது ஆண்ட்ராய்டு சிஸ்டம் மீட்டரை நிறுத்தாமல் இருக்க பேட்டரி கட்டுப்பாட்டிலிருந்து விலக்கு அளிக்கவும்."
    override val disableBatteryOptButton = "பேட்டரி கட்டுப்பாட்டை நீக்கு"
    override val batteryExemptionActiveBadge = "பேட்டரி விலக்கு செயலில் உள்ளது"
    override val wizardStep4Desc = "பயணத்தின் போது தற்போதைய கட்டணம், பயண தூரம் மற்றும் வேகத்தை அறிவிப்புப் பட்டியில் தொடர்ந்து காட்ட அறிவிப்பு அனுமதியை வழங்கவும்."
    override val allowNotificationsButton = "அறிவிப்புகளை அனுமதி"
    override val notificationsEnabledBadge = "அறிவிப்புகள் இயக்கப்பட்டுள்ளன"
    override val previousButton = "முந்தைய"
    override val nextStepButton = "அடுத்த படி →"
    override val finishSetupButton = "நிறைவு செய்"
    override val onboardingCompleteToast = "அனுமதிகள் அமைப்பு நிறைவடைந்தது!"

    override val tripSummaryScreenTitle = "பயண சுருக்கம் & கட்டணம்"
    override val tripReceiptScreenTitle = "ரசீதுகள்"
    override val editBillButton = "ரசீதைத் திருத்து"
    override val tripNotFoundMessage = "பயண விவரங்கள் கிடைக்கவில்லை."
    override val backToHomeButton = "முகப்புக்குச் செல்"
    override val tripCompletedBadge = "பயணம் நிறைவடைந்தது"
    override val servicePrefix = "சேவை"
    override val finalBillAmountLabel = "மொத்த கட்டணத் தொகை"
    override val includesExtraFormat = "+%s%s கூடுதல் கட்டணம் உட்பட"
    override val tripIdPrefix = "பயண எண்"
    override val scanToPay = "ஸ்கேன் செய்து செலுத்தவும்"
    override val editBillSummaryButton = "இறுதி கட்டணம் / கூடுதல் கட்டணம் / வாடிக்கையாளர் விவரம் திருத்து"
    override val tripPartiesDetailsHeader = "பயண விவரங்கள்"
    override val partiesDetailsHeader = "பயண & ஓட்டுநர் விவரங்கள்"
    override val editBillCustomerButton = "ரசீது / வாடிக்கையாளர் திருத்து"
    override val notSpecifiedTapEdit = "குறிப்பிடப்படவில்லை (சேர்க்க திருத்து என்பதைத் தொடவும்)"
    override val driverNameLabel = "ஓட்டுநர் பெயர்"
    override val vehicleRegLabel = "வாகன எண்"
    override val vehicleTypeLabel = "வாகன வகை"
    override val passengerNameLabel = "பயணி பெயர்"
    override val passengerPhoneLabel = "பயணி கைபேசி எண்"
    override val passengerAddressLabel = "பயணி முகவரி"
    override val tripMetricsHeader = "பயண அளவீடுகள்"
    override val startTimeLabel = "தொடங்கிய நேரம்"
    override val endTimeLabel = "முடிந்த நேரம்"
    override val totalDistanceLabel = "மொத்த பயண தூரம்"
    override val totalDurationLabel = "மொத்த பயண நேரம்"
    override val totalWaitingTimeLabel = "மொத்த காத்திருப்பு நேரம்"
    override val gpsCoordinatesFallback = "GPS இருப்பிடம்"
    override val calculatedFareDetailsHeader = "கட்டண விவரங்கள் (PDF எடுக்கும் முன் திருத்தலாம்)"
    override val fareBreakdownHeader = "கட்டண விவரம்"
    override val baseFlagDropFareLabel = "அடிப்படை கட்டணம்"
    override val baseFareLabel = "அடிப்படை கட்டணம்"
    override val distanceFareLabel = "பயண தூர கட்டணம்"
    override val waitingDelayLabel = "காத்திருப்பு கட்டணம்"
    override val waitingFareLabel = "காத்திருப்பு கட்டணம்"
    override val hourlyChargeLabel = "மணிநேர வாடகை கட்டணம்"
    override val includedDistanceAllowanceLabel = "அனுமதிக்கப்பட்ட பயண தூரம்"
    override val actualDistanceDrivenLabel = "மொத்த பயணித்த தூரம்"
    override val extraDistanceChargeLabel = "கூடுதல் தூர கட்டணம்"
    override val withinIncludedLimitLabel = "அனுமதிக்கப்பட்ட வரம்பிற்குள்"
    override val outstationDistanceChargeLabel = "வெளியூர் தூர கட்டணம்"
    override val driverAllowanceBataLabel = "ஓட்டுநர் பேட்டா"
    override val itemizedExtraChargesHeader = "கூடுதல் கட்டண விவரங்கள்"
    override val additionalChargesLabel = "கூடுதல் கட்டணங்கள்"
    override val extraChargesLabel = "கூடுதல் கட்டணம்"
    override val finalBillTotalLabel = "மொத்த ரசீது தொகை"
    override val totalPayableLabel = "செலுத்த வேண்டிய மொத்த தொகை"
    override val allTariffsIncludedLabel = "அனைத்து கட்டணங்களும் அடங்கும்"
    override val officialTripInvoiceHeader = "அதிகாரப்பூர்வ பயண ரசீது"
    override val upiQrPaymentHeader = "UPI QR கட்டண வசதி"
    override val setUpiIdButton = "UPI ஐடி சேர்"
    override val editUpiButton = "UPI மாற்று"
    override val saveQrButton = "QR சேமி"
    override val shareQrButton = "QR பகிர்"
    override val qrSavedGalleryToast = "QR படம் கேலரியில் சேமிக்கப்பட்டது!"
    override val setUpiPromptText = "QR குறியீட்டைக் காட்ட சுயவிவரத்தில் அல்லது மேலே உள்ள 'UPI ஐடி சேர்' பொத்தானை அழுத்தி உங்கள் UPI ஐடியை உள்ளிடவும்."
    override val shareBillHeader = "ரசீதைப் பகிர் (WHATSAPP / SMS)"
    override val whatsappShareButton = "WhatsApp / பகிர்"
    override val shareReceiptWhatsappTelegramButton = "ரசீதைப் பகிர் (WhatsApp / Telegram)"
    override val sendSmsButton = "SMS அனுப்பு"
    override val copyTextButton = "நகலெடு"
    override val receiptCopiedToast = "ரசீது விவரம் நகலெடுக்கப்பட்டது!"
    override val formalPdfHeader = "அதிகாரப்பூர்வ PDF ரசீதுகள்"
    override val customerPdfButton = "வாடிக்கையாளர் PDF"
    override val vendorPdfButton = "நிறுவன PDF"
    override val customerInvoiceA4PdfButton = "வாடிக்கையாளர் A4 PDF ரசீது"
    override val vendorAuditPdfButton = "நிறுவன கணக்கு PDF ரசீது"
    override val viewOfficialInvoiceButton = "அதிகாரப்பூர்வ ரசீதைப் பார்க்க"
    override val doneReturnToMeterButton = "முடிந்தது (மீட்டருக்குத் திரும்பு)"
    override val editBillDialogTitle = "ரசீது & வாடிக்கையாளர் விவரம் திருத்து"
    override val editBillDialogDesc = "PDF உருவாக்கும் முன் இறுதி கட்டணம், கூடுதல் கட்டணம் மற்றும் வாடிக்கையாளர் விவரங்களை மாற்றலாம்."
    override val saveBillButton = "சேமி"
    override val billUpdatedToast = "ரசீது விவரங்கள் புதுப்பிக்கப்பட்டன!"
    override val driverUpiDialogTitle = "ஓட்டுநர் UPI ஐடி"
    override val driverUpiDialogDesc = "வாடிக்கையாளர் ஸ்கேன் செய்து பணம் செலுத்த உங்கள் UPI ஐடியை (GPay / PhonePe / Paytm) உள்ளிடவும்."
    override val yourUpiIdLabel = "உங்கள் UPI ஐடி"
    override val saveUpiIdButton = "சேமி"
    override val upiSavedToast = "ஓட்டுநர் UPI ஐடி சேமிக்கப்பட்டது!"
    override val changeQrButton = "QR மாற்று"

    override val notifChannelName = "GET TAXI மீட்டர் சேவை"
    override val notifRunningTitleFormat = "GET TAXI — பயணத்தில் உள்ளது (%d கி.மீ/மணி)"
    override val notifPausedTitle = "GET TAXI — பயணம் இடைநிறுத்தப்பட்டது"
    override val notifReadyTitle = "GET TAXI — காலியாக உள்ளது"
    override val notifReadyContent = "GPS தயார் • மீட்டர் திரையில் பயணத்தைத் தொடங்கவும்"
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }

@Composable
@ReadOnlyComposable
fun appStrings(): AppStrings = LocalAppStrings.current

object AppLanguageManager {
    private const val PREFS_NAME = "get_taxi_language_prefs"
    private const val KEY_LANGUAGE_CODE = "selected_language_code"

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    val currentStrings: AppStrings
        get() = stringsFor(_currentLanguage.value)

    fun stringsFor(language: AppLanguage): AppStrings {
        return when (language) {
            AppLanguage.TAMIL -> TamilStrings
            AppLanguage.ENGLISH -> EnglishStrings
        }
    }

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs.getString(KEY_LANGUAGE_CODE, AppLanguage.ENGLISH.code)
        _currentLanguage.value = AppLanguage.fromCode(savedCode)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        _currentLanguage.value = language
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE_CODE, language.code).apply()
    }

    fun setLanguageByCode(context: Context, code: String) {
        setLanguage(context, AppLanguage.fromCode(code))
    }

    fun getLanguage(context: Context): AppLanguage {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs.getString(KEY_LANGUAGE_CODE, _currentLanguage.value.code)
        val resolved = AppLanguage.fromCode(savedCode)
        if (_currentLanguage.value != resolved) {
            _currentLanguage.value = resolved
        }
        return resolved
    }
}
