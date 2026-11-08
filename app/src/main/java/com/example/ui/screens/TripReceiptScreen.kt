package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.net.Uri
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.database.DriverProfileEntity
import com.example.data.database.TripDatabase
import com.example.data.database.TripEntity
import com.example.data.model.ExtraChargeItem
import com.example.data.model.ServiceType
import com.example.util.FareCalculator
import com.example.util.PdfGenerator
import com.example.util.ShareUtils
import com.example.util.UpiQrCodeGenerator
import com.example.viewmodel.MeterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripReceiptScreen(
    viewModel: MeterViewModel,
    tripId: Int,
    currencySymbol: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val strings = com.example.util.appStrings()
    val allTrips by viewModel.allTrips.collectAsStateWithLifecycle()
    val vmDriverProfile by viewModel.driverProfile.collectAsStateWithLifecycle()
    var directTrip by remember { mutableStateOf<TripEntity?>(null) }
    var isLoading by remember { mutableStateOf(tripId > 0) }

    LaunchedEffect(tripId) {
        if (tripId > 0) {
            directTrip = viewModel.getTripById(tripId)
        }
        isLoading = false
    }

    val trip = directTrip ?: remember(allTrips, tripId) {
        if (tripId > 0) allTrips.find { it.id == tripId }
        else allTrips.firstOrNull()
    }

    var isGeneratingPdf by remember { mutableStateOf(false) }
    var showEditCustomerDialog by remember { mutableStateOf(false) }
    var showEditUpiDialog by remember { mutableStateOf(false) }

    var editNameInput by remember(trip) { mutableStateOf(trip?.customerName ?: "") }
    var editMobileInput by remember(trip) { mutableStateOf(trip?.customerMobile ?: "") }
    var editAddressInput by remember(trip) { mutableStateOf(trip?.customerAddress ?: "") }
    var editPickupInput by remember(trip) { mutableStateOf(trip?.pickupAddress ?: "") }
    var editDropInput by remember(trip) { mutableStateOf(trip?.dropAddress ?: "") }
    var editBaseFareInput by remember(trip) { mutableStateOf(trip?.let { String.format(Locale.US, "%.2f", it.baseFare) } ?: "0.00") }
    var editDistanceInput by remember(trip) { mutableStateOf(trip?.let { String.format(Locale.US, "%.2f", it.distanceKm) } ?: "0.00") }
    var editExtraChargesInput by remember(trip) { mutableStateOf(trip?.let { String.format(Locale.US, "%.2f", it.outstationAdditionalCharges) } ?: "0.00") }
    var editTotalFareInput by remember(trip) { mutableStateOf(trip?.let { String.format(Locale.US, "%.2f", it.totalFare) } ?: "0.00") }
    var editInvoiceDateInput by remember(trip) {
        mutableStateOf(
            if (trip != null && trip.invoiceDateText.isNotBlank()) trip.invoiceDateText
            else if (trip != null && trip.invoiceTimestamp > 0) SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(trip.invoiceTimestamp))
            else if (trip != null) SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(trip.startTime))
            else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        )
    }
    var editInvoiceTimeInput by remember(trip) {
        mutableStateOf(
            if (trip != null && trip.invoiceTimeText.isNotBlank()) trip.invoiceTimeText
            else if (trip != null && trip.invoiceTimestamp > 0) SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(trip.invoiceTimestamp))
            else if (trip != null) SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(trip.startTime))
            else SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        )
    }

    var upiIdInput by remember(vmDriverProfile) { mutableStateOf(vmDriverProfile?.upiId ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.tripReceiptScreenTitle, color = Color(0xFF0F172A), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("receipt_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = Color(0xFF475569)
                        )
                    }
                },
                actions = {
                    if (trip != null) {
                        TextButton(
                            onClick = {
                                editNameInput = trip.customerName
                                editMobileInput = trip.customerMobile
                                editAddressInput = trip.customerAddress
                                editPickupInput = trip.pickupAddress
                                editDropInput = trip.dropAddress
                                editBaseFareInput = String.format(Locale.US, "%.2f", trip.baseFare)
                                editDistanceInput = String.format(Locale.US, "%.2f", trip.distanceKm)
                                editExtraChargesInput = String.format(Locale.US, "%.2f", trip.outstationAdditionalCharges)
                                editTotalFareInput = String.format(Locale.US, "%.2f", trip.totalFare)
                                editInvoiceDateInput = if (trip.invoiceDateText.isNotBlank()) trip.invoiceDateText else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(if (trip.invoiceTimestamp > 0) trip.invoiceTimestamp else trip.startTime))
                                editInvoiceTimeInput = if (trip.invoiceTimeText.isNotBlank()) trip.invoiceTimeText else SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(if (trip.invoiceTimestamp > 0) trip.invoiceTimestamp else trip.startTime))
                                showEditCustomerDialog = true
                            },
                            modifier = Modifier.testTag("topbar_edit_bill_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.editBillButton, color = Color(0xFFE53935), fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = {
                                val textMsg = ShareUtils.buildVendorReceiptText(trip)
                                ShareUtils.shareText(context, textMsg, "Trip Receipt ${trip.tripNumber}")
                            },
                            modifier = Modifier.testTag("topbar_share_text_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = strings.whatsappShareButton,
                                tint = Color(0xFFE53935)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFDFBFF))
            )
        },
        containerColor = Color(0xFFFDFBFF)
    ) { innerPadding ->
        if (isLoading && trip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFE53935))
            }
        } else if (trip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(strings.tripNotFoundMessage, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Text(strings.backToHomeButton)
                    }
                }
            }
        } else {
            val dateOnlyFormatter = remember { SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()) }
            val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

            val formattedDate = if (trip.invoiceDateText.isNotBlank()) {
                if (trip.invoiceTimeText.isNotBlank()) "${trip.invoiceDateText}, ${trip.invoiceTimeText}" else trip.invoiceDateText
            } else {
                dateOnlyFormatter.format(Date(trip.startTime))
            }
            val startTimeStr = timeFormatter.format(Date(trip.startTime))
            val endTimeStr = if (trip.endTime > 0) timeFormatter.format(Date(trip.endTime)) else timeFormatter.format(Date(trip.startTime + (trip.durationSeconds * 1000)))

            val durationMin = trip.durationSeconds / 60
            val durationSec = trip.durationSeconds % 60
            val waitingMin = trip.waitingSeconds / 60
            val waitingSec = trip.waitingSeconds % 60

            val tripNumberDisplay = if (trip.tripNumber.isNotBlank()) trip.tripNumber else String.format(Locale.US, "GTM-%s-%04d", SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(trip.startTime)), trip.id)
            val curr = if (trip.currency.isNotBlank()) trip.currency else currencySymbol

            val activeUpiId = vmDriverProfile?.upiId?.trim() ?: ""
            val activeDriverName = trip.driverName.ifBlank { vmDriverProfile?.driverName ?: "GET TAXI Driver" }

            val upiUri = remember(activeUpiId, activeDriverName, trip.totalFare, tripNumberDisplay) {
                if (activeUpiId.isNotBlank()) {
                    UpiQrCodeGenerator.buildUpiUri(
                        upiId = activeUpiId,
                        driverName = activeDriverName,
                        fareAmount = trip.totalFare,
                        transactionNote = "Trip $tripNumberDisplay"
                    )
                } else ""
            }

            val qrBitmap = remember(upiUri) {
                if (upiUri.isNotBlank()) {
                    UpiQrCodeGenerator.generateQrCodeBitmap(upiUri, sizePx = 520)
                } else null
            }

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ELEGANT OFFICIAL TAXI BILL CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBF9)),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(28.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val vendorMeta = remember(trip) {
                            PdfGenerator.resolveVendorHeaderMetadata(trip)
                        }

                        com.example.ui.components.VendorLogoBadge(
                            vendorId = vendorMeta.vendorId,
                            vendorName = vendorMeta.vendorName,
                            height = 50.dp,
                            testTag = "receipt_vendor_logo_badge"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = vendorMeta.vendorName,
                            color = Color(0xFFCA1F2F),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.testTag("receipt_vendor_name_text")
                        )

                        if (vendorMeta.displayedBusinessLines.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            vendorMeta.displayedBusinessLines.forEach { line ->
                                Text(
                                    text = line,
                                    color = Color(0xFF475569),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Text(
                            text = strings.officialTripInvoiceHeader,
                            color = Color(0xFF0F172A),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = if (strings.isTamil) 0.sp else 0.5.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val modeEnum = ServiceType.fromId(trip.serviceType)
                        Surface(
                            color = when (modeEnum) {
                                ServiceType.RENTAL -> Color(0xFFFEF3C7)
                                ServiceType.OUTSTATION -> Color(0xFFE0E7FF)
                                ServiceType.LOCAL -> Color(0xFFF1F5F9)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                when (modeEnum) {
                                    ServiceType.RENTAL -> Color(0xFFFDE68A)
                                    ServiceType.OUTSTATION -> Color(0xFFC7D2FE)
                                    ServiceType.LOCAL -> Color(0xFFE2E8F0)
                                }
                            )
                        ) {
                            Text(
                                text = "${strings.servicePrefix}: ${strings.serviceTypeTitle(modeEnum)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = if (strings.isTamil) 0.sp else 0.8.sp,
                                color = when (modeEnum) {
                                    ServiceType.RENTAL -> Color(0xFFB45309)
                                    ServiceType.OUTSTATION -> Color(0xFF4338CA)
                                    ServiceType.LOCAL -> Color(0xFF334155)
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = "Invoice: ${vendorMeta.invoiceNumber} • ${strings.tripIdPrefix}: $tripNumberDisplay • $formattedDate",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                        )

                        DottedDivider(color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(12.dp))

                        // CUSTOMER & DRIVER SUMMARY SECTION
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.partiesDetailsHeader,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = if (strings.isTamil) 0.sp else 1.2.sp
                            )
                            TextButton(
                                onClick = {
                                    editNameInput = trip.customerName
                                    editMobileInput = trip.customerMobile
                                    editAddressInput = trip.customerAddress
                                    editPickupInput = trip.pickupAddress
                                    editDropInput = trip.dropAddress
                                    editBaseFareInput = String.format(Locale.US, "%.2f", trip.baseFare)
                                    editDistanceInput = String.format(Locale.US, "%.2f", trip.distanceKm)
                                    editExtraChargesInput = String.format(Locale.US, "%.2f", trip.outstationAdditionalCharges)
                                    editTotalFareInput = String.format(Locale.US, "%.2f", trip.totalFare)
                                    showEditCustomerDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("edit_customer_details_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.editBillCustomerButton,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE53935)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        if (trip.customerName.isNotBlank()) {
                            ReceiptLineItem(strings.passengerNameLabel, trip.customerName)
                        } else {
                            ReceiptLineItem(strings.passengerNameLabel, strings.notSpecifiedTapEdit)
                        }
                        if (trip.customerMobile.isNotBlank()) {
                            ReceiptLineItem(strings.passengerPhoneLabel, trip.customerMobile)
                        }
                        if (trip.customerAddress.isNotBlank()) {
                            ReceiptLineItem(strings.passengerAddressLabel, trip.customerAddress)
                        }
                        if (trip.driverName.isNotBlank()) {
                            ReceiptLineItem(strings.driverNameLabel, trip.driverName)
                        }
                        if (trip.vehicleNumber.isNotBlank()) {
                            ReceiptLineItem(strings.vehicleRegLabel, trip.vehicleNumber)
                        }
                        if (trip.vehicleModel.isNotBlank() || trip.vehicleType.isNotBlank()) {
                            val vType = strings.translateVehicleCategory(trip.vehicleType.ifBlank { "Taxi" })
                            val vMod = if (trip.vehicleModel.isNotBlank()) " (${trip.vehicleModel})" else ""
                            ReceiptLineItem(strings.vehicleTypeLabel, "$vType$vMod")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        DottedDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(12.dp))

                        // TRIP METRICS
                        Text(
                            text = strings.tripMetricsHeader,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = if (strings.isTamil) 0.sp else 1.2.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        ReceiptLineItem(strings.startTimeLabel, startTimeStr)
                        ReceiptLineItem(strings.endTimeLabel, endTimeStr)
                        ReceiptLineItem(strings.totalDistanceLabel, "${String.format(Locale.US, "%.2f", trip.distanceKm)} KM")
                        ReceiptLineItem(strings.totalDurationLabel, "${durationMin}m ${durationSec}s")

                        if (modeEnum == ServiceType.RENTAL) {
                            ReceiptLineItem(strings.billableHoursLabel, "${trip.billableHours} ${strings.unitHr}")
                        } else {
                            ReceiptLineItem(strings.totalWaitingTimeLabel, "${waitingMin}m ${waitingSec}s")
                        }

                        ReceiptLineItem(strings.pickupPointLabel, trip.pickupAddress.ifBlank { strings.gpsCoordinatesFallback })
                        ReceiptLineItem(strings.dropPointLabel, trip.dropAddress.ifBlank { strings.gpsCoordinatesFallback })

                        Spacer(modifier = Modifier.height(12.dp))
                        DottedDivider(color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(12.dp))

                        // FARE BREAKDOWN SECTION
                        Text(
                            text = strings.fareBreakdownHeader,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = if (strings.isTamil) 0.sp else 1.2.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val itemizedExtras = remember(trip.extraChargesJson) {
                            ExtraChargeItem.deserialize(trip.extraChargesJson)
                        }

                        when (modeEnum) {
                            ServiceType.LOCAL -> {
                                val distFare = trip.distanceKm * trip.farePerKm
                                val waitMinutes = trip.waitingSeconds / 60.0
                                val waitFare = waitMinutes * trip.waitFarePerMin
                                ReceiptLineItem(strings.baseFareLabel, "$curr${String.format(Locale.US, "%.2f", trip.baseFare)}")
                                ReceiptLineItem(strings.distanceFareLabel, "$curr${String.format(Locale.US, "%.2f", distFare)}")
                                ReceiptLineItem(strings.waitingFareLabel, "$curr${String.format(Locale.US, "%.2f", waitFare)}")
                            }
                            ServiceType.RENTAL -> {
                                val calc = FareCalculator.calculateHourlyRentalFare(
                                    durationSeconds = trip.durationSeconds,
                                    distanceKm = trip.distanceKm,
                                    hourlyRate = trip.rentalBaseFare,
                                    includedKmPerHour = if (trip.rentalBaseKm > 0.0) trip.rentalBaseKm else 10.0,
                                    extraKmRate = trip.rentalExtraKmRate
                                )

                                ReceiptLineItem("${strings.hourlyRental} (${calc.billableHours} ${strings.unitHr})", "$curr${String.format(Locale.US, "%.2f", calc.hourlyCharge)}")
                                ReceiptLineItem(strings.includedDistanceAllowanceLabel, "${String.format(Locale.US, "%.2f", calc.totalIncludedKm)} KM (${String.format(Locale.US, "%.2f", calc.includedKmPerHour)} km/hr)")
                                ReceiptLineItem(strings.actualDistanceDrivenLabel, "${String.format(Locale.US, "%.2f", calc.actualDistanceKm)} KM")
                                if (calc.extraDistanceKm > 0.0) {
                                    ReceiptLineItem("${strings.extraDistanceChargeLabel} (${String.format(Locale.US, "%.2f", calc.extraDistanceKm)} km @ $curr${String.format(Locale.US, "%.2f", calc.extraKmRate)}/km)", "$curr${String.format(Locale.US, "%.2f", calc.extraDistanceCharge)}")
                                } else {
                                    ReceiptLineItem(strings.extraDistanceChargeLabel, "$curr 0.00 (${strings.withinIncludedLimitLabel})")
                                }
                            }
                            ServiceType.OUTSTATION -> {
                                val distFare = trip.distanceKm * trip.outstationRatePerKm
                                val waitHours = trip.waitingSeconds / 3600.0
                                val waitFare = if (trip.outstationWaitRate > 0) waitHours * trip.outstationWaitRate else 0.0

                                ReceiptLineItem(strings.distanceFareLabel, "$curr${String.format(Locale.US, "%.2f", distFare)}")
                                ReceiptLineItem(strings.driverAllowanceBataLabel, "$curr${String.format(Locale.US, "%.2f", trip.outstationDriverBata)}")
                                if (waitFare > 0) {
                                    ReceiptLineItem(strings.waitingFareLabel, "$curr${String.format(Locale.US, "%.2f", waitFare)}")
                                }
                            }
                        }

                        // ITEMIZED EXTRA CHARGES IN RECEIPT FARE BREAKDOWN
                        val itemizedSum = ExtraChargeItem.totalAmount(itemizedExtras)
                        if (itemizedExtras.isNotEmpty()) {
                            itemizedExtras.forEach { item ->
                                ReceiptLineItem(
                                    "${strings.translateExtraChargePreset(item.label)} (${strings.extraChargeFallback})",
                                    "$curr${String.format(Locale.US, "%.2f", item.amount)}"
                                )
                            }
                            val remainder = trip.outstationAdditionalCharges - itemizedSum
                            if (remainder > 0.01) {
                                ReceiptLineItem(strings.additionalChargesLabel, "$curr${String.format(Locale.US, "%.2f", remainder)}")
                            }
                        } else if (trip.outstationAdditionalCharges > 0.0) {
                            ReceiptLineItem(strings.extraChargesLabel, "$curr${String.format(Locale.US, "%.2f", trip.outstationAdditionalCharges)}")
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        DottedDivider(color = Color(0xFFE53935))
                        Spacer(modifier = Modifier.height(14.dp))

                        // TOTAL PAYABLE + STATIC QR CODE SIDE-BY-SIDE ROW
                        val staticQrUri = vmDriverProfile?.qrCodeUri?.trim() ?: ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(18.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.totalPayableLabel,
                                    color = Color(0xFF0F172A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = if (strings.isTamil) 0.sp else 0.8.sp
                                )
                                Text(
                                    text = strings.allTariffsIncludedLabel,
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$curr${String.format(Locale.US, "%.2f", trip.totalFare)}",
                                    color = Color(0xFFE53935),
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.testTag("receipt_fare_display")
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Uploaded Static QR Code or Placeholder right next to final bill amount
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(116.dp)
                            ) {
                                Text(
                                    text = strings.scanToPay,
                                    color = Color(0xFF15803D),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = if (strings.isTamil) 0.sp else 0.6.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                if (staticQrUri.isNotBlank()) {
                                    val qrModel: Any = remember(staticQrUri) {
                                        if (staticQrUri.startsWith("/")) File(staticQrUri) else Uri.parse(staticQrUri)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(104.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .border(1.5.dp, Color(0xFF16A34A), RoundedCornerShape(12.dp))
                                            .padding(4.dp)
                                            .testTag("receipt_static_qr_image"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = qrModel,
                                            contentDescription = strings.scanToPay,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(104.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                            .padding(6.dp)
                                            .testTag("receipt_no_qr_uploaded_placeholder"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCode2,
                                                contentDescription = null,
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = strings.noQrUploaded,
                                                color = Color(0xFF475569),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // UPI QR CODE PAYMENT CARD ON INVOICE SCREEN
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_upi_qr_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.upiQrPaymentHeader,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = if (strings.isTamil) 0.sp else 0.8.sp
                                )
                            }
                            TextButton(
                                onClick = {
                                    upiIdInput = activeUpiId
                                    showEditUpiDialog = true
                                }
                            ) {
                                Text(
                                    text = if (activeUpiId.isBlank()) strings.setUpiIdButton else strings.editUpiButton,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (activeUpiId.isNotBlank() && qrBitmap != null) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${strings.scanToPay} $curr${String.format(Locale.US, "%.2f", trip.totalFare)}",
                                        color = Color(0xFFE53935),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = strings.upiQrPaymentHeader,
                                        modifier = Modifier.size(180.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$activeDriverName • $activeUpiId",
                                        color = Color(0xFF334155),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val cardBmp = withContext(Dispatchers.Default) {
                                                UpiQrCodeGenerator.generatePaymentCardBitmap(
                                                    upiId = activeUpiId,
                                                    driverName = activeDriverName,
                                                    vehicleNumber = trip.vehicleNumber.ifBlank { vmDriverProfile?.vehicleNumber ?: "" },
                                                    fareAmount = trip.totalFare,
                                                    currencySymbol = curr,
                                                    tripNumber = tripNumberDisplay
                                                )
                                            }
                                            val saved = withContext(Dispatchers.IO) {
                                                UpiQrCodeGenerator.saveQrBitmapToGallery(
                                                    context,
                                                    cardBmp,
                                                    "GET_TAXI_QR_${tripNumberDisplay}.png"
                                                )
                                            }
                                            if (saved) {
                                                Toast.makeText(context, strings.qrSavedGalleryToast, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.saveQrButton, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val cardBmp = withContext(Dispatchers.Default) {
                                                UpiQrCodeGenerator.generatePaymentCardBitmap(
                                                    upiId = activeUpiId,
                                                    driverName = activeDriverName,
                                                    vehicleNumber = trip.vehicleNumber.ifBlank { vmDriverProfile?.vehicleNumber ?: "" },
                                                    fareAmount = trip.totalFare,
                                                    currencySymbol = curr,
                                                    tripNumber = tripNumberDisplay
                                                )
                                            }
                                            UpiQrCodeGenerator.shareQrBitmap(
                                                context,
                                                cardBmp,
                                                "GET TAXI Payment QR - Trip $tripNumberDisplay ($curr${String.format(Locale.US, "%.2f", trip.totalFare)})"
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.shareQrButton, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                text = strings.setUpiPromptText,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // SHARE AS TEXT MESSAGE (WHATSAPP, TELEGRAM & SMS)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.shareBillHeader,
                                color = Color(0xFF047857),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = if (strings.isTamil) 0.sp else 1.sp
                            )
                        }

                        Button(
                            onClick = {
                                val textMsg = ShareUtils.buildVendorReceiptText(trip)
                                ShareUtils.shareText(context, textMsg, "Trip Receipt ${trip.tripNumber}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("share_text_whatsapp_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.shareReceiptWhatsappTelegramButton, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val textMsg = ShareUtils.buildVendorReceiptText(trip)
                                    ShareUtils.shareViaSms(context, trip.customerMobile, textMsg)
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, Color(0xFF0284C7)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("share_sms_button")
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.sendSmsButton, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val textMsg = ShareUtils.buildVendorReceiptText(trip)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Trip Receipt", textMsg)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, strings.receiptCopiedToast, Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, Color(0xFF059669)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("copy_receipt_text_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.copyTextButton, fontWeight = FontWeight.Bold, color = Color(0xFF059669), fontSize = 12.sp)
                            }
                        }
                    }
                }

                // DOCUMENT EXPORT (OPTIONAL PDF ARCHIVE)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = strings.formalPdfHeader,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = if (strings.isTamil) 0.sp else 1.2.sp
                        )

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isGeneratingPdf = true
                                    val pdf = withContext(Dispatchers.IO) {
                                        PdfGenerator.generateCustomerInvoicePdf(context, trip)
                                    }
                                    isGeneratingPdf = false
                                    shareFile(context, pdf, "Customer Invoice (${trip.tripNumber.ifBlank { "Trip" }})")
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE53935)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("generate_customer_pdf_button"),
                            enabled = !isGeneratingPdf
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color(0xFFE53935))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.customerInvoiceA4PdfButton, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isGeneratingPdf = true
                                    val pdf = withContext(Dispatchers.IO) {
                                        PdfGenerator.generateVendorStatementPdf(context, trip)
                                    }
                                    isGeneratingPdf = false
                                    shareFile(context, pdf, "Vendor Audit Statement (${trip.tripNumber.ifBlank { "Trip" }})")
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF0F172A)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("generate_vendor_pdf_button"),
                            enabled = !isGeneratingPdf
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.vendorAuditPdfButton, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // POST-TRIP EDIT BILL, EXTRA CHARGES & CUSTOMER DETAILS DIALOG
    if (showEditCustomerDialog && trip != null) {
        val currSym = if (trip.currency.isNotBlank()) trip.currency else currencySymbol
        Dialog(onDismissRequest = { showEditCustomerDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.editBillDialogTitle,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = strings.editBillDialogDesc,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    // Editable Invoice Date & Time (Preserves immutable GPS timestamps)
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Invoice Date & Time (Editable for PDF & Receipt)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = "Actual GPS travel times remain separately and immutably recorded.",
                                fontSize = 9.5.sp,
                                color = Color(0xFFB91C1C)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = editInvoiceDateInput,
                                    onValueChange = { editInvoiceDateInput = it },
                                    label = { Text("Invoice Date") },
                                    placeholder = { Text("e.g. 08 Nov 2026") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.3f).testTag("edit_invoice_date_input")
                                )
                                OutlinedTextField(
                                    value = editInvoiceTimeInput,
                                    onValueChange = { editInvoiceTimeInput = it },
                                    label = { Text("Time") },
                                    placeholder = { Text("e.g. 03:45 PM") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("edit_invoice_time_input")
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editTotalFareInput,
                            onValueChange = { editTotalFareInput = it },
                            label = { Text(String.format(strings.finalTotalFareLabelFormat, currSym)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("edit_total_fare_input")
                        )
                        OutlinedTextField(
                            value = editExtraChargesInput,
                            onValueChange = { newExtra ->
                                val oldExtra = editExtraChargesInput.toDoubleOrNull() ?: 0.0
                                val currentTotal = editTotalFareInput.toDoubleOrNull() ?: trip.totalFare
                                editExtraChargesInput = newExtra
                                val parsedNewExtra = newExtra.toDoubleOrNull() ?: 0.0
                                val updatedTotal = (currentTotal - oldExtra + parsedNewExtra).coerceAtLeast(0.0)
                                editTotalFareInput = String.format(Locale.US, "%.2f", updatedTotal)
                            },
                            label = { Text(String.format(strings.extraChargesLabelFormat, currSym)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("edit_extra_charges_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editBaseFareInput,
                            onValueChange = { editBaseFareInput = it },
                            label = { Text(String.format(strings.baseFareLabelFormat, currSym)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editDistanceInput,
                            onValueChange = { editDistanceInput = it },
                            label = { Text(strings.distanceKmLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text(strings.customerNameLabel) },
                        placeholder = { Text(strings.customerNamePlaceholder) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFE53935)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_customer_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editMobileInput,
                        onValueChange = { editMobileInput = it },
                        label = { Text(strings.customerMobileLabel) },
                        placeholder = { Text("e.g. 9876543210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFFE53935)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_customer_mobile_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editAddressInput,
                        onValueChange = { editAddressInput = it },
                        label = { Text(strings.customerAddressNotesLabel) },
                        placeholder = { Text("e.g. Hotel Grand, Room 204") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE53935)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_customer_address_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editPickupInput,
                        onValueChange = { editPickupInput = it },
                        label = { Text(strings.pickupPointLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editDropInput,
                        onValueChange = { editDropInput = it },
                        label = { Text(strings.dropPointLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditCustomerDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.cancel, color = Color(0xFF64748B))
                        }

                        Button(
                            onClick = {
                                val parsedTotal = editTotalFareInput.toDoubleOrNull() ?: trip.totalFare
                                val parsedExtra = editExtraChargesInput.toDoubleOrNull() ?: trip.outstationAdditionalCharges
                                val parsedBase = editBaseFareInput.toDoubleOrNull() ?: trip.baseFare
                                val parsedDist = editDistanceInput.toDoubleOrNull() ?: trip.distanceKm

                                val updatedTripEntity = trip.copy(
                                    customerName = editNameInput.trim(),
                                    customerMobile = editMobileInput.trim(),
                                    customerAddress = editAddressInput.trim(),
                                    pickupAddress = editPickupInput.trim().ifBlank { trip.pickupAddress },
                                    dropAddress = editDropInput.trim().ifBlank { trip.dropAddress },
                                    baseFare = parsedBase,
                                    distanceKm = parsedDist,
                                    outstationAdditionalCharges = parsedExtra,
                                    totalFare = parsedTotal,
                                    invoiceDateText = editInvoiceDateInput.trim(),
                                    invoiceTimeText = editInvoiceTimeInput.trim()
                                )

                                viewModel.updateEditableBillDetails(updatedTripEntity) { updated ->
                                    directTrip = updated
                                    showEditCustomerDialog = false
                                    Toast.makeText(context, strings.billUpdatedToast, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f).testTag("save_customer_details_button")
                        ) {
                            Text(strings.saveBillButton, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // INLINE SET / EDIT DRIVER UPI ID DIALOG
    if (showEditUpiDialog) {
        Dialog(onDismissRequest = { showEditUpiDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.driverUpiDialogTitle,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = strings.driverUpiDialogDesc,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    OutlinedTextField(
                        value = upiIdInput,
                        onValueChange = { upiIdInput = it },
                        label = { Text(strings.yourUpiIdLabel) },
                        placeholder = { Text("e.g. 9043743777@ybl or driver@okaxis") },
                        leadingIcon = { Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color(0xFF16A34A)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditUpiDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.cancel, color = Color(0xFF64748B))
                        }

                        Button(
                            onClick = {
                                val cleanedUpi = upiIdInput.trim()
                                val currentProf = vmDriverProfile ?: DriverProfileEntity()
                                viewModel.saveDriverProfile(currentProf.copy(upiId = cleanedUpi))
                                showEditUpiDialog = false
                                Toast.makeText(context, strings.upiSavedToast, Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text(strings.saveUpiIdButton, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptLineItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF475569),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color(0xFF0F172A),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DottedDivider(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = pathEffect,
            strokeWidth = 2f
        )
    }
}

private fun shareFile(context: Context, file: java.io.File, title: String) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, title)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share $title"))
    } catch (e: Exception) {
        android.util.Log.e("TripReceiptScreen", "Failed sharing PDF file", e)
    }
}
