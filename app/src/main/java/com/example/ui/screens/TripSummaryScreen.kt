package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
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
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSummaryScreen(
    tripId: Int,
    viewModel: MeterViewModel? = null,
    currencySymbol: String = "₹",
    onNavigateBack: () -> Unit = {},
    onNavigateToInvoice: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val strings = com.example.util.appStrings()

    var trip by remember { mutableStateOf<TripEntity?>(null) }
    var driverProfile by remember { mutableStateOf<DriverProfileEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var showEditBillDialog by remember { mutableStateOf(false) }
    var showEditUpiDialog by remember { mutableStateOf(false) }
    var showFullScreenQrDialog by remember { mutableStateOf(false) }

    var editNameInput by remember(trip) { mutableStateOf(trip?.customerName ?: "") }
    var editMobileInput by remember(trip) { mutableStateOf(trip?.customerMobile ?: "") }
    var editAddressInput by remember(trip) { mutableStateOf(trip?.customerAddress ?: "") }
    var editPickupInput by remember(trip) { mutableStateOf(trip?.pickupAddress ?: "") }
    var editDropInput by remember(trip) { mutableStateOf(trip?.dropAddress ?: "") }
    var editBaseFareInput by remember(trip) { mutableStateOf(trip?.let { String.format(Locale.US, "%.2f", it.baseFare) } ?: "0.00") }
    var editDistanceInput by remember(trip) { mutableStateOf(trip?.let { String.format(Locale.US, "%.2f", it.distanceKm) } ?: "0.00") }
    var editExtraChargesInput by remember(trip) { mutableStateOf(trip?.let { String.format(Locale.US, "%.2f", it.outstationAdditionalCharges) } ?: "0.00") }
    var editTotalFareInput by remember(trip) { mutableStateOf(trip?.let { String.format(Locale.US, "%.2f", it.totalFare) } ?: "0.00") }

    var upiIdInput by remember(driverProfile) { mutableStateOf(driverProfile?.upiId ?: "") }

    // Gallery-ONLY launcher for uploading Static Payment QR code directly from Summary if needed (NO camera)
    val galleryStaticQrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val savedPath = withContext(Dispatchers.IO) {
                        context.filesDir.listFiles()?.forEach { existing ->
                            if (existing.name.startsWith("driver_payment_qr")) {
                                existing.delete()
                            }
                        }
                        val destFile = File(context.filesDir, "driver_payment_qr_${System.currentTimeMillis()}.jpg")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (destFile.exists() && destFile.length() > 0L) destFile.absolutePath else uri.toString()
                    }
                    val db = TripDatabase.getDatabase(context)
                    val currentProf = db.tripDao().getDriverProfile() ?: DriverProfileEntity()
                    val updatedProf = currentProf.copy(qrCodeUri = savedPath)
                    db.tripDao().saveDriverProfile(updatedProf)
                    driverProfile = updatedProf
                    viewModel?.saveDriverProfile(updatedProf)
                    Toast.makeText(context, strings.paymentQrSavedToast, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, strings.qrCouldNotSaveToast, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(tripId) {
        isLoading = true
        val database = TripDatabase.getDatabase(context)
        trip = database.tripDao().getTripById(tripId)
        driverProfile = database.tripDao().getDriverProfile()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.tripSummaryScreenTitle,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("summary_back_button")
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
                                val t = trip!!
                                editNameInput = t.customerName
                                editMobileInput = t.customerMobile
                                editAddressInput = t.customerAddress
                                editPickupInput = t.pickupAddress
                                editDropInput = t.dropAddress
                                editBaseFareInput = String.format(Locale.US, "%.2f", t.baseFare)
                                editDistanceInput = String.format(Locale.US, "%.2f", t.distanceKm)
                                editExtraChargesInput = String.format(Locale.US, "%.2f", t.outstationAdditionalCharges)
                                editTotalFareInput = String.format(Locale.US, "%.2f", t.totalFare)
                                showEditBillDialog = true
                            },
                            modifier = Modifier.testTag("summary_topbar_edit_bill_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.editBillButton, color = Color(0xFFE53935), fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFDFBFF))
            )
        },
        containerColor = Color(0xFFFDFBFF)
    ) { innerPadding ->
        if (isLoading) {
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
                Text(
                    text = strings.tripNotFoundMessage,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            val currentTrip = trip!!
            val dateFormatter = remember { SimpleDateFormat("EEEE, MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
            val formattedDate = dateFormatter.format(Date(currentTrip.startTime))

            val durationMin = currentTrip.durationSeconds / 60
            val durationSec = currentTrip.durationSeconds % 60
            val waitMin = currentTrip.waitingSeconds / 60
            val waitSec = currentTrip.waitingSeconds % 60

            val tripNumberDisplay = if (currentTrip.tripNumber.isNotBlank()) currentTrip.tripNumber else String.format(Locale.US, "GTM-%s-%04d", SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(currentTrip.startTime)), currentTrip.id)
            val curr = if (currentTrip.currency.isNotBlank()) currentTrip.currency else currencySymbol

            val staticQrUri = driverProfile?.qrCodeUri?.trim() ?: ""
            val activeUpiId = driverProfile?.upiId?.trim() ?: ""
            val activeDriverName = currentTrip.driverName.ifBlank { driverProfile?.driverName ?: "GET TAXI Driver" }

            val itemizedExtras = remember(currentTrip.extraChargesJson) {
                ExtraChargeItem.deserialize(currentTrip.extraChargesJson)
            }

            val upiUri = remember(activeUpiId, activeDriverName, currentTrip.totalFare, tripNumberDisplay) {
                if (activeUpiId.isNotBlank()) {
                    UpiQrCodeGenerator.buildUpiUri(
                        upiId = activeUpiId,
                        driverName = activeDriverName,
                        fareAmount = currentTrip.totalFare,
                        transactionNote = "Trip $tripNumberDisplay"
                    )
                } else ""
            }

            val qrBitmap = remember(upiUri) {
                if (upiUri.isNotBlank()) {
                    UpiQrCodeGenerator.generateQrCodeBitmap(upiUri, sizePx = 560)
                } else null
            }

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // HERO FINAL BILL AMOUNT & STATIC QR CODE SIDE-BY-SIDE CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(28.dp))
                        .testTag("summary_hero_fare_and_qr_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val vendorMeta = remember(currentTrip) {
                            PdfGenerator.resolveVendorHeaderMetadata(currentTrip)
                        }

                        // Trip Vendor Branding Row (Immutable Snapshot from TripEntity)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                com.example.ui.components.VendorLogoBadge(
                                    vendorId = vendorMeta.vendorId,
                                    vendorName = vendorMeta.vendorName,
                                    height = 42.dp,
                                    testTag = "summary_vendor_logo_badge"
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = vendorMeta.vendorName,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.testTag("summary_vendor_name_text")
                                    )
                                    Text(
                                        text = "Invoice: ${vendorMeta.invoiceNumber}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF64748B)
                                    )
                                    vendorMeta.displayedBusinessLines.forEach { line ->
                                        Text(
                                            text = line,
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }

                        // Top Status Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = strings.tripCompletedBadge,
                                        color = Color(0xFF15803D),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        letterSpacing = if (strings.isTamil) 0.sp else 0.8.sp
                                    )
                                }
                            }

                            val modeEnum = ServiceType.fromId(currentTrip.serviceType)
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
                                        ServiceType.LOCAL -> Color(0xFFCBD5E1)
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
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // SIDE-BY-SIDE ROW: Final Bill Amount (Left) + Uploaded Static QR Code (Right)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(22.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // LEFT COLUMN: FINAL BILL AMOUNT
                            Column(
                                modifier = Modifier.weight(1.05f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = strings.finalBillAmountLabel,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = if (strings.isTamil) 0.sp else 1.1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(
                                        text = curr,
                                        color = Color(0xFFE53935),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(top = 4.dp, end = 2.dp)
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.2f", currentTrip.totalFare),
                                        color = Color(0xFF0F172A),
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.testTag("summary_fare_text")
                                    )
                                }
                                if (currentTrip.outstationAdditionalCharges > 0.0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = Color(0xFFFEF2F2),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                                    ) {
                                        Text(
                                            text = String.format(strings.includesExtraFormat, curr, String.format(Locale.US, "%.2f", currentTrip.outstationAdditionalCharges)),
                                            color = Color(0xFFDC2626),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${strings.tripIdPrefix}: $tripNumberDisplay",
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = formattedDate,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // RIGHT COLUMN: UPLOADED STATIC QR CODE IMAGE RIGHT NEXT TO AMOUNT
                            Column(
                                modifier = Modifier.width(136.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = strings.scanToPay,
                                    color = Color(0xFF15803D),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = if (strings.isTamil) 0.sp else 0.8.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                if (staticQrUri.isNotBlank()) {
                                    val qrImageModel: Any = remember(staticQrUri) {
                                        if (staticQrUri.startsWith("/")) File(staticQrUri) else Uri.parse(staticQrUri)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(124.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White)
                                            .border(2.dp, Color(0xFF16A34A), RoundedCornerShape(16.dp))
                                            .clickable { showFullScreenQrDialog = true }
                                            .padding(6.dp)
                                            .testTag("summary_static_qr_container"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = qrImageModel,
                                            contentDescription = strings.scanToPay,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(10.dp))
                                                .testTag("summary_static_qr_image"),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = strings.tapQrToEnlarge,
                                        color = Color(0xFF64748B),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    // PLACEHOLDER WHEN NO STATIC QR CODE IS UPLOADED
                                    Box(
                                        modifier = Modifier
                                            .size(124.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                                            .clickable { galleryStaticQrLauncher.launch("image/*") }
                                            .padding(8.dp)
                                            .testTag("no_qr_uploaded_placeholder"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCode2,
                                                contentDescription = strings.noQrUploaded,
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = strings.noQrUploaded,
                                                color = Color(0xFF475569),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 13.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = strings.tapToUpload,
                                                color = Color(0xFF16A34A),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                editNameInput = currentTrip.customerName
                                editMobileInput = currentTrip.customerMobile
                                editAddressInput = currentTrip.customerAddress
                                editPickupInput = currentTrip.pickupAddress
                                editDropInput = currentTrip.dropAddress
                                editBaseFareInput = String.format(Locale.US, "%.2f", currentTrip.baseFare)
                                editDistanceInput = String.format(Locale.US, "%.2f", currentTrip.distanceKm)
                                editExtraChargesInput = String.format(Locale.US, "%.2f", currentTrip.outstationAdditionalCharges)
                                editTotalFareInput = String.format(Locale.US, "%.2f", currentTrip.totalFare)
                                showEditBillDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE53935)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("summary_edit_bill_button")
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.editBillSummaryButton, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // TELEMETRY SUMMARY GRID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.distance, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = if (strings.isTamil) 0.sp else 0.5.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.2f", currentTrip.distanceKm)} km",
                                color = Color(0xFF0F172A),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.testTag("summary_distance_text")
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.duration, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = if (strings.isTamil) 0.sp else 0.5.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${durationMin}m ${durationSec}s",
                                color = Color(0xFF0F172A),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.testTag("summary_duration_text")
                            )
                        }
                    }
                }

                // CUSTOMER & DRIVER CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.tripPartiesDetailsHeader,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = if (strings.isTamil) 0.sp else 1.5.sp
                            )
                            TextButton(
                                onClick = {
                                    editNameInput = currentTrip.customerName
                                    editMobileInput = currentTrip.customerMobile
                                    editAddressInput = currentTrip.customerAddress
                                    editPickupInput = currentTrip.pickupAddress
                                    editDropInput = currentTrip.dropAddress
                                    editBaseFareInput = String.format(Locale.US, "%.2f", currentTrip.baseFare)
                                    editDistanceInput = String.format(Locale.US, "%.2f", currentTrip.distanceKm)
                                    editExtraChargesInput = String.format(Locale.US, "%.2f", currentTrip.outstationAdditionalCharges)
                                    editTotalFareInput = String.format(Locale.US, "%.2f", currentTrip.totalFare)
                                    showEditBillDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("summary_edit_customer_button")
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

                        if (currentTrip.customerName.isNotBlank()) {
                            SummaryLineRow(strings.customerNameLabel, currentTrip.customerName)
                        } else {
                            SummaryLineRow(strings.customerNameLabel, strings.notSpecifiedTapEdit)
                        }
                        if (currentTrip.customerMobile.isNotBlank()) {
                            SummaryLineRow(strings.customerMobileLabel, currentTrip.customerMobile)
                        }
                        if (currentTrip.customerAddress.isNotBlank()) {
                            SummaryLineRow(strings.passengerAddressLabel, currentTrip.customerAddress)
                        }
                        if (currentTrip.pickupAddress.isNotBlank()) {
                            SummaryLineRow(strings.pickupPointLabel, currentTrip.pickupAddress)
                        }
                        if (currentTrip.dropAddress.isNotBlank()) {
                            SummaryLineRow(strings.dropPointLabel, currentTrip.dropAddress)
                        }
                        if (currentTrip.driverName.isNotBlank()) {
                            SummaryLineRow(strings.driverNameLabel, currentTrip.driverName)
                        }
                        if (currentTrip.vehicleNumber.isNotBlank()) {
                            SummaryLineRow(strings.vehicleRegLabel, currentTrip.vehicleNumber)
                        }
                    }
                }

                // DETAILED BILLING BREAKDOWN CARD WITH ITEMIZED EXTRA CHARGES
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                        .testTag("summary_fare_breakdown_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = strings.calculatedFareDetailsHeader,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = if (strings.isTamil) 0.sp else 1.5.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        when (ServiceType.fromId(currentTrip.serviceType)) {
                            ServiceType.LOCAL -> {
                                SummaryLineRow(
                                    label = strings.baseFlagDropFareLabel,
                                    value = "$curr${String.format(Locale.US, "%.2f", currentTrip.baseFare)}"
                                )
                                SummaryLineRow(
                                    label = "${strings.distanceFareLabel} (${String.format(Locale.US, "%.2f", currentTrip.distanceKm)} km @ $curr${String.format(Locale.US, "%.2f", currentTrip.farePerKm)}/km)",
                                    value = "$curr${String.format(Locale.US, "%.2f", currentTrip.distanceKm * currentTrip.farePerKm)}"
                                )
                                SummaryLineRow(
                                    label = "${strings.waitingDelayLabel} (${waitMin}m ${waitSec}s @ $curr${String.format(Locale.US, "%.2f", currentTrip.waitFarePerMin)}/min)",
                                    value = "$curr${String.format(Locale.US, "%.2f", (currentTrip.waitingSeconds / 60.0) * currentTrip.waitFarePerMin)}"
                                )
                            }
                            ServiceType.RENTAL -> {
                                val calc = FareCalculator.calculateHourlyRentalFare(
                                    durationSeconds = currentTrip.durationSeconds,
                                    distanceKm = currentTrip.distanceKm,
                                    hourlyRate = currentTrip.rentalBaseFare,
                                    includedKmPerHour = if (currentTrip.rentalBaseKm > 0.0) currentTrip.rentalBaseKm else 10.0,
                                    extraKmRate = currentTrip.rentalExtraKmRate
                                )

                                SummaryLineRow(
                                    label = "${strings.hourlyChargeLabel} (${calc.billableHours} hr(s) @ $curr${String.format(Locale.US, "%.2f", calc.hourlyRate)}/hr)",
                                    value = "$curr${String.format(Locale.US, "%.2f", calc.hourlyCharge)}"
                                )
                                SummaryLineRow(
                                    label = strings.includedDistanceAllowanceLabel,
                                    value = "${String.format(Locale.US, "%.2f", calc.totalIncludedKm)} km (${String.format(Locale.US, "%.2f", calc.includedKmPerHour)} km/hr)"
                                )
                                SummaryLineRow(
                                    label = strings.actualDistanceDrivenLabel,
                                    value = "${String.format(Locale.US, "%.2f", calc.actualDistanceKm)} km"
                                )
                                if (calc.extraDistanceKm > 0.0) {
                                    SummaryLineRow(
                                        label = "${strings.extraDistanceChargeLabel} (${String.format(Locale.US, "%.2f", calc.extraDistanceKm)} km @ $curr${String.format(Locale.US, "%.2f", calc.extraKmRate)}/km)",
                                        value = "$curr${String.format(Locale.US, "%.2f", calc.extraDistanceCharge)}"
                                    )
                                } else {
                                    SummaryLineRow(
                                        label = "${strings.extraDistanceChargeLabel} (${strings.withinIncludedLimitLabel})",
                                        value = "$curr 0.00"
                                    )
                                }
                            }
                            ServiceType.OUTSTATION -> {
                                SummaryLineRow(
                                    label = "${strings.outstationDistanceChargeLabel} (${String.format(Locale.US, "%.2f", currentTrip.distanceKm)} km @ $curr${String.format(Locale.US, "%.2f", currentTrip.outstationRatePerKm)}/km)",
                                    value = "$curr${String.format(Locale.US, "%.2f", currentTrip.distanceKm * currentTrip.outstationRatePerKm)}"
                                )
                                if (currentTrip.outstationDriverBata > 0.0) {
                                    SummaryLineRow(
                                        label = strings.driverAllowanceBataLabel,
                                        value = "$curr${String.format(Locale.US, "%.2f", currentTrip.outstationDriverBata)}"
                                    )
                                }
                                if (currentTrip.outstationWaitRate > 0.0) {
                                    val waitHrs = currentTrip.waitingSeconds / 3600.0
                                    SummaryLineRow(
                                        label = "${strings.waitHaltLabel} (${String.format(Locale.US, "%.2f", waitHrs)} hr @ $curr${String.format(Locale.US, "%.2f", currentTrip.outstationWaitRate)}/hr)",
                                        value = "$curr${String.format(Locale.US, "%.2f", waitHrs * currentTrip.outstationWaitRate)}"
                                    )
                                }
                            }
                        }

                        // ITEMIZED EXTRA CHARGES LINE ITEMS
                        val itemizedSum = ExtraChargeItem.totalAmount(itemizedExtras)
                        if (itemizedExtras.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.itemizedExtraChargesHeader,
                                color = Color(0xFFE53935),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = if (strings.isTamil) 0.sp else 1.1.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                            itemizedExtras.forEach { item ->
                                SummaryLineRow(
                                    label = "${strings.translateExtraChargePreset(item.label)} (${strings.extraChargeFallback})",
                                    value = "$curr${String.format(Locale.US, "%.2f", item.amount)}"
                                )
                            }
                            val remainderExtra = currentTrip.outstationAdditionalCharges - itemizedSum
                            if (remainderExtra > 0.01) {
                                SummaryLineRow(
                                    label = strings.additionalChargesLabel,
                                    value = "$curr${String.format(Locale.US, "%.2f", remainderExtra)}"
                                )
                            }
                        } else if (currentTrip.outstationAdditionalCharges > 0.0) {
                            SummaryLineRow(
                                label = strings.extraChargesLabel,
                                value = "$curr${String.format(Locale.US, "%.2f", currentTrip.outstationAdditionalCharges)}"
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        SummaryDottedDivider(color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.finalBillTotalLabel,
                                color = Color(0xFF0F172A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "$curr${String.format(Locale.US, "%.2f", currentTrip.totalFare)}",
                                color = Color(0xFFE53935),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // EXPORT & SHARE ACTIONS (WHATSAPP, SMS, SHARE, PDF)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val textMsg = ShareUtils.buildVendorReceiptText(currentTrip)
                            ShareUtils.shareText(context, textMsg, "Trip Receipt ${currentTrip.tripNumber}")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp)
                            .testTag("summary_share_whatsapp_text_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.whatsappShareButton, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            val textMsg = ShareUtils.buildVendorReceiptText(currentTrip)
                            ShareUtils.shareViaSms(context, currentTrip.customerMobile, textMsg)
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF0284C7)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7)),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("summary_share_sms_button")
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.sendSmsButton, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF0284C7))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val pdf = withContext(Dispatchers.IO) {
                                    PdfGenerator.generateCustomerInvoicePdf(context, currentTrip)
                                }
                                ShareUtils.sharePdf(context, pdf, "Customer Invoice ${currentTrip.tripNumber}")
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE53935)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFE53935))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.customerPdfButton, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                    }

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val pdf = withContext(Dispatchers.IO) {
                                    PdfGenerator.generateVendorStatementPdf(context, currentTrip)
                                }
                                ShareUtils.sharePdf(context, pdf, "Vendor Statement ${currentTrip.tripNumber}")
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF0F172A)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.vendorPdfButton, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                }

                if (onNavigateToInvoice != null) {
                    OutlinedButton(
                        onClick = { onNavigateToInvoice(currentTrip.id) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.5.dp, Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("summary_view_invoice_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.viewOfficialInvoiceButton, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                }

                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("summary_return_home_button")
                ) {
                    Text(strings.doneReturnToMeterButton, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // EDIT BILL / CUSTOMIZE INVOICE BEFORE PDF GENERATION DIALOG
    if (showEditBillDialog && trip != null) {
        val currSym = if (trip!!.currency.isNotBlank()) trip!!.currency else currencySymbol
        Dialog(onDismissRequest = { showEditBillDialog = false }) {
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
                            modifier = Modifier.weight(1f).testTag("summary_edit_total_fare_input")
                        )
                        OutlinedTextField(
                            value = editExtraChargesInput,
                            onValueChange = { newExtra ->
                                val oldExtra = editExtraChargesInput.toDoubleOrNull() ?: 0.0
                                val currentTotal = editTotalFareInput.toDoubleOrNull() ?: (trip?.totalFare ?: 0.0)
                                editExtraChargesInput = newExtra
                                val parsedNewExtra = newExtra.toDoubleOrNull() ?: 0.0
                                val updatedTotal = (currentTotal - oldExtra + parsedNewExtra).coerceAtLeast(0.0)
                                editTotalFareInput = String.format(Locale.US, "%.2f", updatedTotal)
                            },
                            label = { Text(String.format(strings.extraChargesLabelFormat, currSym)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("summary_edit_extra_charges_input")
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
                        modifier = Modifier.fillMaxWidth().testTag("summary_edit_customer_name_input")
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
                        modifier = Modifier.fillMaxWidth().testTag("summary_edit_customer_mobile_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editAddressInput,
                        onValueChange = { editAddressInput = it },
                        label = { Text(strings.customerAddressNotesLabel) },
                        placeholder = { Text("e.g. RS Puram, Coimbatore") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE53935)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("summary_edit_customer_address_input")
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
                            onClick = { showEditBillDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.cancel, color = Color(0xFF64748B))
                        }

                        Button(
                            onClick = {
                                val currentTripItem = trip!!
                                val newTotalFare = editTotalFareInput.toDoubleOrNull() ?: currentTripItem.totalFare
                                val newExtraCharges = editExtraChargesInput.toDoubleOrNull() ?: currentTripItem.outstationAdditionalCharges
                                val newBaseFare = editBaseFareInput.toDoubleOrNull() ?: currentTripItem.baseFare
                                val newDist = editDistanceInput.toDoubleOrNull() ?: currentTripItem.distanceKm

                                val updatedTrip = currentTripItem.copy(
                                    customerName = editNameInput.trim(),
                                    customerMobile = editMobileInput.trim(),
                                    customerAddress = editAddressInput.trim(),
                                    pickupAddress = editPickupInput.trim().ifBlank { currentTripItem.pickupAddress },
                                    dropAddress = editDropInput.trim().ifBlank { currentTripItem.dropAddress },
                                    baseFare = newBaseFare,
                                    distanceKm = newDist,
                                    outstationAdditionalCharges = newExtraCharges,
                                    totalFare = newTotalFare
                                )
                                trip = updatedTrip
                                showEditBillDialog = false

                                coroutineScope.launch {
                                    val db = TripDatabase.getDatabase(context)
                                    db.tripDao().updateTrip(updatedTrip)
                                    Toast.makeText(context, strings.billUpdatedToast, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f).testTag("summary_save_customer_button")
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
                        modifier = Modifier.fillMaxWidth().testTag("summary_upi_id_dialog_input")
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
                                coroutineScope.launch {
                                    val db = TripDatabase.getDatabase(context)
                                    val currentProf = db.tripDao().getDriverProfile() ?: DriverProfileEntity()
                                    val updatedProf = currentProf.copy(upiId = cleanedUpi)
                                    db.tripDao().saveDriverProfile(updatedProf)
                                    driverProfile = updatedProf
                                    showEditUpiDialog = false
                                    Toast.makeText(context, strings.upiSavedToast, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f).testTag("summary_save_upi_id_button")
                        ) {
                            Text(strings.saveUpiIdButton, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // FULL-SCREEN STATIC QR CODE VIEWER DIALOG
    val currentStaticQrUri = driverProfile?.qrCodeUri?.trim() ?: ""
    if (showFullScreenQrDialog && currentStaticQrUri.isNotBlank()) {
        val qrModel: Any = remember(currentStaticQrUri) {
            if (currentStaticQrUri.startsWith("/")) File(currentStaticQrUri) else Uri.parse(currentStaticQrUri)
        }
        Dialog(onDismissRequest = { showFullScreenQrDialog = false }) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .testTag("full_screen_static_qr_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.scanToPay,
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = if (strings.isTamil) 0.sp else 1.2.sp
                    )
                    if (trip != null) {
                        val c = if (trip!!.currency.isNotBlank()) trip!!.currency else currencySymbol
                        Text(
                            text = "$c${String.format(Locale.US, "%.2f", trip!!.totalFare)}",
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(2.dp, Color(0xFF16A34A), RoundedCornerShape(20.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = qrModel,
                            contentDescription = strings.scanToPay,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showFullScreenQrDialog = false
                                galleryStaticQrLauncher.launch("image/*")
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF16A34A)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16A34A)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.changeQrButton, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showFullScreenQrDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.close, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryLineRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF64748B), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.2f))
        Text(text = value, color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SummaryDottedDivider(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        )
    }
}
