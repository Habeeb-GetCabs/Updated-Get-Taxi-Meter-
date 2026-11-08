package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.database.DriverProfileEntity
import com.example.util.UpiQrCodeGenerator
import com.example.viewmodel.MeterViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverProfileScreen(
    viewModel: MeterViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentProfile by viewModel.driverProfile.collectAsStateWithLifecycle()
    val strings = com.example.util.appStrings()

    var nameInput by remember { mutableStateOf("") }
    var mobileInput by remember { mutableStateOf("") }
    var vehicleNumberInput by remember { mutableStateOf("") }
    var vehicleModelInput by remember { mutableStateOf("") }
    var vehicleTypeInput by remember { mutableStateOf("Sedan Taxi") }
    var driverIdInput by remember { mutableStateOf("") }
    var upiIdInput by remember { mutableStateOf("") }
    var photoUriString by remember { mutableStateOf<String?>(null) }
    var qrCodeUriString by remember { mutableStateOf<String?>(null) }

    var showSuccessMessage by remember { mutableStateOf(false) }
    var showOnboardingWizard by remember { mutableStateOf(false) }
    var onboardingStep by remember { mutableIntStateOf(1) }

    // Live permission states refreshed on resume
    var hasFineLocation by remember { mutableStateOf(checkFineLocation(context)) }
    var hasBackgroundLocation by remember { mutableStateOf(checkBackgroundLocation(context)) }
    var hasOverlayPermission by remember { mutableStateOf(checkOverlayPermission(context)) }
    var hasBatteryExemption by remember { mutableStateOf(checkBatteryOptimizationExemption(context)) }
    var hasNotificationPermission by remember { mutableStateOf(checkNotificationPermission(context)) }

    fun refreshPermissionStates() {
        hasFineLocation = checkFineLocation(context)
        hasBackgroundLocation = checkBackgroundLocation(context)
        hasOverlayPermission = checkOverlayPermission(context)
        hasBatteryExemption = checkBatteryOptimizationExemption(context)
        hasNotificationPermission = checkNotificationPermission(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentProfile) {
        currentProfile?.let {
            nameInput = it.driverName
            mobileInput = it.driverMobile
            vehicleNumberInput = it.vehicleNumber
            vehicleModelInput = it.vehicleModel
            vehicleTypeInput = if (it.vehicleType.isNotBlank()) it.vehicleType else "Sedan Taxi"
            driverIdInput = it.driverId
            upiIdInput = it.upiId
            photoUriString = it.photoUri
            qrCodeUriString = it.qrCodeUri
        }
    }

    // GALLERY-ONLY Photo Picker Launcher (Strictly Gallery, NO Camera Intent)
    val galleryPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = copyGalleryUriToInternalStorage(
                context,
                uri,
                "driver_profile_photo_${System.currentTimeMillis()}.jpg",
                prefixToCleanup = "driver_profile_photo_"
            )
            photoUriString = savedPath ?: uri.toString()
        }
    }

    // GALLERY-ONLY Static Payment QR Code Picker Launcher (Strictly Gallery, NO Camera Intent)
    val galleryQrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = copyGalleryUriToInternalStorage(
                context,
                uri,
                "driver_payment_qr_${System.currentTimeMillis()}.jpg",
                prefixToCleanup = "driver_payment_qr_"
            )
            val newQrPath = savedPath ?: uri.toString()
            qrCodeUriString = newQrPath
            val updated = DriverProfileEntity(
                id = 1,
                driverName = nameInput.trim(),
                driverMobile = mobileInput.trim(),
                vehicleNumber = vehicleNumberInput.trim(),
                vehicleModel = vehicleModelInput.trim(),
                vehicleType = vehicleTypeInput.trim(),
                driverId = driverIdInput.trim(),
                photoUri = photoUriString,
                qrCodeUri = newQrPath,
                upiId = upiIdInput.trim()
            )
            viewModel.saveDriverProfile(updated) {
                Toast.makeText(context, strings.paymentQrSavedToast, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Step 4 Launcher: Notification Permission
    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        refreshPermissionStates()
    }

    // Step 1b Launcher: Background Location ("Allow all the time")
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        refreshPermissionStates()
    }

    // Step 1a Launcher: Foreground Location
    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        refreshPermissionStates()
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !checkBackgroundLocation(context)) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    val vehicleTypePresets = listOf(
        "Sedan Taxi",
        "Hatchback Taxi",
        "SUV Taxi",
        "Auto Rickshaw",
        "Van / MPV",
        "Luxury Cab"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.driverProfileScreenTitle,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("driver_profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = Color(0xFF475569)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFDFBFF))
            )
        },
        containerColor = Color(0xFFFDFBFF),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val updated = DriverProfileEntity(
                        id = 1,
                        driverName = nameInput.trim(),
                        driverMobile = mobileInput.trim(),
                        vehicleNumber = vehicleNumberInput.trim(),
                        vehicleModel = vehicleModelInput.trim(),
                        vehicleType = vehicleTypeInput.trim(),
                        driverId = driverIdInput.trim(),
                        photoUri = photoUriString,
                        qrCodeUri = qrCodeUriString,
                        upiId = upiIdInput.trim()
                    )
                    viewModel.saveDriverProfile(updated) {
                        showSuccessMessage = true
                        refreshPermissionStates()
                        val allGranted = hasFineLocation && hasBackgroundLocation &&
                            hasOverlayPermission && hasBatteryExemption && hasNotificationPermission
                        if (!allGranted) {
                            onboardingStep = when {
                                !hasFineLocation || !hasBackgroundLocation -> 1
                                !hasOverlayPermission -> 2
                                !hasBatteryExemption -> 3
                                else -> 4
                            }
                            showOnboardingWizard = true
                        } else {
                            Toast.makeText(context, strings.profileSavedToast, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                icon = { Icon(Icons.Default.Save, contentDescription = null, tint = Color.White) },
                text = { Text(strings.registerSaveProfileButton, fontWeight = FontWeight.Black, letterSpacing = if (strings.isTamil) 0.sp else 0.8.sp) },
                containerColor = Color(0xFFE53935),
                contentColor = Color.White,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.testTag("save_driver_profile_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showSuccessMessage) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF065F46))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.profileSavedBannerText,
                            color = Color(0xFF065F46),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // 1. PROFILE PICTURE (GALLERY UPLOAD ONLY - NO CAMERA INTENT)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.driverProfilePictureHeader,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (strings.isTamil) 0.sp else 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .border(2.5.dp, Color(0xFFE53935), CircleShape)
                            .clickable {
                                galleryPhotoLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!photoUriString.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(
                                        if (photoUriString!!.startsWith("/")) File(photoUriString!!)
                                        else Uri.parse(photoUriString)
                                    )
                                    .crossfade(true)
                                    .build(),
                                contentDescription = strings.driverProfilePictureHeader,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = strings.driverProfilePictureHeader,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(68.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            // Strictly Gallery picker only (no camera intent)
                            galleryPhotoLauncher.launch("image/*")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                        border = BorderStroke(1.5.dp, Color(0xFFE53935)),
                        modifier = Modifier.testTag("pick_photo_button")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (photoUriString.isNullOrBlank()) strings.uploadProfilePicGallery else strings.changeProfilePicGallery,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!photoUriString.isNullOrBlank()) {
                        TextButton(onClick = { photoUriString = null }) {
                            Text(strings.removePicture, color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }
            }

            // 2. DRIVER & VEHICLE REGISTRATION FIELDS
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = strings.driverVehicleRegHeader,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (strings.isTamil) 0.sp else 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(strings.driverNameRequiredLabel) },
                        placeholder = { Text("e.g. Basheer") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFE53935)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_driver_name")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = mobileInput,
                        onValueChange = { mobileInput = it },
                        label = { Text(strings.mobileNumberRequiredLabel) },
                        placeholder = { Text("e.g. 9043743777") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFFE53935)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_driver_mobile")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = vehicleModelInput,
                        onValueChange = { vehicleModelInput = it },
                        label = { Text(strings.vehicleModelRequiredLabel) },
                        placeholder = { Text("e.g. Toyota Etios / Maruti Dzire / Innova") },
                        leadingIcon = { Icon(Icons.Default.LocalTaxi, contentDescription = null, tint = Color(0xFFE53935)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_vehicle_model")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = vehicleNumberInput,
                        onValueChange = { vehicleNumberInput = it },
                        label = { Text(strings.vehicleNumberRequiredLabel) },
                        placeholder = { Text("e.g. TN 38 AB 1234") },
                        leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFFE53935)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_vehicle_number")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = driverIdInput,
                        onValueChange = { driverIdInput = it },
                        label = { Text(strings.driverLicenseOptionalLabel) },
                        placeholder = { Text("e.g. TN-38-2023001") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFFE53935)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_driver_id")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = strings.vehicleCategoryLabel,
                        color = Color(0xFF1E293B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        vehicleTypePresets.chunked(2).forEach { rowPresets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPresets.forEach { preset ->
                                    FilterChip(
                                        selected = vehicleTypeInput == preset,
                                        onClick = { vehicleTypeInput = preset },
                                        label = { Text(strings.translateVehicleCategory(preset), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFE53935),
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. UPLOAD PAYMENT QR CODE (STATIC QR FROM GALLERY - SAVED LOCALLY)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                    .testTag("upload_payment_qr_section_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.uploadPaymentQrHeader,
                            color = Color(0xFF0F172A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = if (strings.isTamil) 0.sp else 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = strings.uploadPaymentQrDesc,
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Static QR Code Preview or Placeholder
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(2.dp, if (!qrCodeUriString.isNullOrBlank()) Color(0xFF16A34A) else Color(0xFFCBD5E1), RoundedCornerShape(18.dp))
                            .clickable {
                                galleryQrLauncher.launch("image/*")
                            }
                            .padding(8.dp)
                            .testTag("driver_profile_qr_preview_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!qrCodeUriString.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(
                                        if (qrCodeUriString!!.startsWith("/")) File(qrCodeUriString!!)
                                        else Uri.parse(qrCodeUriString)
                                    )
                                    .crossfade(true)
                                    .build(),
                                contentDescription = strings.uploadPaymentQrHeader,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = strings.noQrUploaded,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = strings.noQrUploaded,
                                    color = Color(0xFF475569),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = strings.tapBelowToChooseGallery,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            // Strictly Gallery picker only (NO camera intent)
                            galleryQrLauncher.launch("image/*")
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("upload_payment_qr_button")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (qrCodeUriString.isNullOrBlank()) strings.uploadPaymentQrGalleryButton else strings.changePaymentQrGalleryButton,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    if (!qrCodeUriString.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                qrCodeUriString = null
                                val updated = DriverProfileEntity(
                                    id = 1,
                                    driverName = nameInput.trim(),
                                    driverMobile = mobileInput.trim(),
                                    vehicleNumber = vehicleNumberInput.trim(),
                                    vehicleModel = vehicleModelInput.trim(),
                                    vehicleType = vehicleTypeInput.trim(),
                                    driverId = driverIdInput.trim(),
                                    photoUri = photoUriString,
                                    qrCodeUri = null,
                                    upiId = upiIdInput.trim()
                                )
                                viewModel.saveDriverProfile(updated)
                            },
                            modifier = Modifier.testTag("remove_payment_qr_button")
                        ) {
                            Text(strings.removeUploadedQrButton, color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = upiIdInput,
                        onValueChange = { upiIdInput = it },
                        label = { Text(strings.driverUpiOptionalLabel) },
                        placeholder = { Text("e.g. 9043743777@ybl or driver@okaxis") },
                        leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFE53935)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_driver_upi_id")
                    )
                }
            }

            // 4. STEP-BY-STEP PERMISSIONS ONBOARDING FLOW CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("permissions_onboarding_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.permissionsOnboardingHeader,
                                color = Color(0xFFF87171),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = if (strings.isTamil) 0.sp else 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.permissionsOnboardingTitle,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Button(
                            onClick = {
                                refreshPermissionStates()
                                onboardingStep = 1
                                showOnboardingWizard = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("launch_onboarding_wizard_button")
                        ) {
                            Text(strings.startWizardButton, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    PermissionStepRow(
                        stepNumber = 1,
                        title = strings.permStep1Title,
                        subtitle = strings.permStep1Subtitle,
                        isGranted = hasFineLocation && hasBackgroundLocation,
                        grantedLabel = strings.grantedStatus,
                        allowLabel = strings.allowAction,
                        onAction = {
                            if (!hasFineLocation) {
                                foregroundLocationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocation) {
                                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PermissionStepRow(
                        stepNumber = 2,
                        title = strings.permStep2Title,
                        subtitle = strings.permStep2Subtitle,
                        isGranted = hasOverlayPermission,
                        grantedLabel = strings.grantedStatus,
                        allowLabel = strings.allowAction,
                        onAction = { requestOverlayPermission(context) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PermissionStepRow(
                        stepNumber = 3,
                        title = strings.permStep3Title,
                        subtitle = strings.permStep3Subtitle,
                        isGranted = hasBatteryExemption,
                        grantedLabel = strings.grantedStatus,
                        allowLabel = strings.allowAction,
                        onAction = { requestBatteryOptimizationExemption(context) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PermissionStepRow(
                        stepNumber = 4,
                        title = strings.permStep4Title,
                        subtitle = strings.permStep4Subtitle,
                        isGranted = hasNotificationPermission,
                        grantedLabel = strings.grantedStatus,
                        allowLabel = strings.allowAction,
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    // STEP-BY-STEP ONBOARDING MODAL DIALOG
    if (showOnboardingWizard) {
        Dialog(onDismissRequest = { showOnboardingWizard = false }) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = String.format(strings.stepOf4Format, onboardingStep),
                            color = Color(0xFFE53935),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = if (strings.isTamil) 0.sp else 1.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (onboardingStep) {
                        1 -> {
                            val granted = hasFineLocation && hasBackgroundLocation
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (granted) Color(0xFF10B981) else Color(0xFFE53935),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "1. ${strings.permStep1Title}",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.wizardStep1Desc,
                                fontSize = 13.sp,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            if (!granted) {
                                Button(
                                    onClick = {
                                        if (!hasFineLocation) {
                                            foregroundLocationLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(strings.grantLocationPermButton, fontWeight = FontWeight.Black)
                                }
                            } else {
                                StatusGrantedBadge(strings.locationPermGrantedBadge)
                            }
                        }

                        2 -> {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = if (hasOverlayPermission) Color(0xFF10B981) else Color(0xFFE53935),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "2. ${strings.permStep2Title}",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.wizardStep2Desc,
                                fontSize = 13.sp,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            if (!hasOverlayPermission) {
                                Button(
                                    onClick = { requestOverlayPermission(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(strings.enableOverlayPermButton, fontWeight = FontWeight.Black)
                                }
                            } else {
                                StatusGrantedBadge(strings.overlayPermGrantedBadge)
                            }
                        }

                        3 -> {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = if (hasBatteryExemption) Color(0xFF10B981) else Color(0xFFE53935),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = strings.wizardStep3Title,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.wizardStep3Desc,
                                fontSize = 13.sp,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            if (!hasBatteryExemption) {
                                Button(
                                    onClick = { requestBatteryOptimizationExemption(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(strings.disableBatteryOptButton, fontWeight = FontWeight.Black)
                                }
                            } else {
                                StatusGrantedBadge(strings.batteryExemptionActiveBadge)
                            }
                        }

                        4 -> {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (hasNotificationPermission) Color(0xFF10B981) else Color(0xFFE53935),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "4. ${strings.permStep4Title}",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.wizardStep4Desc,
                                fontSize = 13.sp,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            if (!hasNotificationPermission) {
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(strings.allowNotificationsButton, fontWeight = FontWeight.Black)
                                }
                            } else {
                                StatusGrantedBadge(strings.notificationsEnabledBadge)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onboardingStep > 1) {
                            OutlinedButton(
                                onClick = { onboardingStep -= 1 },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(strings.previousButton)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showOnboardingWizard = false },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(strings.close)
                            }
                        }

                        Button(
                            onClick = {
                                refreshPermissionStates()
                                if (onboardingStep < 4) {
                                    onboardingStep += 1
                                } else {
                                    showOnboardingWizard = false
                                    Toast.makeText(context, strings.onboardingCompleteToast, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text(
                                text = if (onboardingStep < 4) strings.nextStepButton else strings.finishSetupButton,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusGrantedBadge(text: String) {
    Surface(
        color = Color(0xFFDCFCE7),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, color = Color(0xFF15803D), fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PermissionStepRow(
    stepNumber: Int,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    grantedLabel: String = "GRANTED",
    allowLabel: String = "ALLOW",
    onAction: () -> Unit
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) Color(0xFF10B981) else Color(0xFFE53935)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGranted) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("$stepNumber", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isGranted) {
                Text(
                    text = grantedLabel,
                    color = Color(0xFF34D399),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            } else {
                TextButton(
                    onClick = onAction,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF87171))
                ) {
                    Text(allowLabel, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        }
    }
}

private fun checkFineLocation(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

private fun checkBackgroundLocation(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkOverlayPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

private fun checkBatteryOptimizationExemption(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    } else {
        true
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun requestOverlayPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            } catch (_: Exception) {}
        }
    }
}

@android.annotation.SuppressLint("BatteryLife")
private fun requestBatteryOptimizationExemption(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {}
        }
    }
}

private fun copyGalleryUriToInternalStorage(
    context: Context,
    uri: Uri,
    fileName: String,
    prefixToCleanup: String? = null
): String? {
    return try {
        if (!prefixToCleanup.isNullOrBlank()) {
            context.filesDir.listFiles()?.forEach { existing ->
                if (existing.name.startsWith(prefixToCleanup) && existing.name != fileName) {
                    try { existing.delete() } catch (_: Exception) {}
                }
            }
        }
        val destFile = File(context.filesDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        null
    }
}
