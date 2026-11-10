package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.TripEntity
import com.example.data.database.VendorProfileEntity
import com.example.data.model.ActiveTripDisplayMode
import com.example.data.model.ExtraChargeItem
import com.example.data.model.ServiceType
import com.example.data.model.TripStatus
import com.example.data.preferences.TripOtpManager
import com.example.ui.components.BatteryOptimizationCard
import com.example.ui.components.CreatorSignatureBanner
import com.example.ui.components.FloatingSpeedPermissionCard
import com.example.ui.components.LiveSpeedBubble
import com.example.ui.components.LogoStyleCustomizerDialog
import com.example.ui.components.TripMapView
import com.example.ui.components.VendorLogoBadge
import com.example.util.VendorLogoRenderer
import com.example.viewmodel.MeterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMeterScreen(
    viewModel: MeterViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToVendorManagement: () -> Unit = {},
    onNavigateToReceipt: (Int) -> Unit,
    onNavigateToSummary: (Int) -> Unit = onNavigateToReceipt
) {
    val context = LocalContext.current
    val strings = com.example.util.appStrings()
    val tripState by viewModel.tripState.collectAsStateWithLifecycle()
    val allTrips by viewModel.allTrips.collectAsStateWithLifecycle()
    val activeVendors by viewModel.activeVendors.collectAsStateWithLifecycle()
    val selectedVendor by viewModel.selectedVendor.collectAsStateWithLifecycle()
    val activeTripDisplayMode by viewModel.activeTripDisplayMode.collectAsStateWithLifecycle()
    val hasBackup by viewModel.hasActiveSessionBackup.collectAsStateWithLifecycle()
    val selectedServiceType by viewModel.selectedServiceType.collectAsStateWithLifecycle()
    val validationMessage by viewModel.tariffValidationMessage.collectAsStateWithLifecycle()

    var showCreateManualBillDialog by remember { mutableStateOf(false) }
    var showExtraChargesDialog by remember { mutableStateOf(false) }
    var showVendorSelectorDialog by remember { mutableStateOf(false) }
    var showLogoStyleDialog by remember { mutableStateOf(false) }
    var isMapExpanded by remember { mutableStateOf(false) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }

    val isTripActive = tripState.status == TripStatus.RUNNING || tripState.status == TripStatus.PAUSED

    // Active vendor identity (locked to tripState during an active trip, or per-trip selector when idle)
    val currentHeaderVendorId = if (isTripActive) {
        tripState.vendorId.ifBlank { VendorLogoRenderer.DEFAULT_VENDOR_ID }
    } else {
        selectedVendor.vendorId
    }
    val currentHeaderVendorName = if (isTripActive) {
        tripState.vendorName.ifBlank { VendorLogoRenderer.DEFAULT_VENDOR_NAME }
    } else {
        selectedVendor.vendorName
    }

    // Sequential permission handling
    var locationPermissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationPermissionsGranted = fineGranted || coarseGranted
        if (fineGranted || coarseGranted) {
            com.example.service.LocationTrackingService.refreshOverlay(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.checkForActiveBackup()
        viewModel.loadDriverProfile()

        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            com.example.service.LocationTrackingService.refreshOverlay(context)
        }

        if (!hasFine || !hasCoarse) {
            locationLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showLogoStyleDialog = true }
                            .testTag("brand_header_title")
                    ) {
                        VendorLogoBadge(
                            vendorId = VendorLogoRenderer.GET_TAXI_VENDOR_ID,
                            vendorName = VendorLogoRenderer.GET_TAXI_VENDOR_NAME,
                            height = 42.dp,
                            testTag = "header_vendor_logo_badge"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier
                            .testTag("driver_profile_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = strings.profile,
                            tint = Color(0xFFE53935)
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .testTag("settings_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = strings.settings,
                            tint = Color(0xFF475569)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showTopMenu = true },
                            modifier = Modifier
                                .testTag("top_overflow_menu_button")
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color(0xFF0F296B)
                            )
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Change Vendor") },
                                onClick = {
                                    showTopMenu = false
                                    showVendorSelectorDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = Color(0xFFCA1F2F)
                                    )
                                },
                                modifier = Modifier.testTag("menu_change_vendor_item")
                            )

                            DropdownMenuItem(
                                text = { Text(strings.vendorManagementTitle) },
                                onClick = {
                                    showTopMenu = false
                                    onNavigateToVendorManagement()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = Color(0xFF0F296B)
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Logo Color & Font") },
                                onClick = {
                                    showTopMenu = false
                                    showLogoStyleDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = Color(0xFF0F296B)
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(strings.aboutUs) },
                                onClick = {
                                    showTopMenu = false
                                    onNavigateToAbout()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF475569)
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDFBFF)
                )
            )
        },
        containerColor = Color(0xFFFDFBFF)
    ) { innerPadding ->
        val currentMode = if (isTripActive) {
            ServiceType.fromId(tripState.serviceType)
        } else {
            selectedServiceType
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (!isTripActive) {
                // BATTERY OPTIMIZATION EXEMPTION CARD
                BatteryOptimizationCard()

                // FLOATING SPEED OVERLAY PERMISSION CARD
                FloatingSpeedPermissionCard()

                // Backup Active Session Card
                if (hasBackup && tripState.status == TripStatus.IDLE) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = strings.unfinishedRideDetected,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.unfinishedRideDetected,
                                color = Color(0xFF991B1B),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = strings.unfinishedRideDesc,
                                color = Color(0xFFB91C1C),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { viewModel.discardBackup() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(strings.discard, color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.recoverTrip() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(strings.resumeRide, color = Color.White, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }

                // Tariff Validation Banner
                if (validationMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = validationMessage!!,
                                    color = Color(0xFF991B1B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearValidationMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = strings.close, tint = Color(0xFF991B1B), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // GPS diagnostics & Status Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.deviceStatus,
                        fontSize = if (strings.isTamil) 11.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = if (strings.isTamil) 0.2.sp else 1.5.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (tripState.latitude != null) Color(0xFF22C55E) else Color(0xFFEF4444),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (tripState.latitude != null) strings.gpsSignalStrong else strings.acquiringGps,
                            color = Color(0xFF334155),
                            fontSize = if (strings.isTamil) 12.sp else 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(
                            color = when (tripState.status) {
                                TripStatus.RUNNING -> Color(0xFFFEF2F2)
                                TripStatus.PAUSED -> Color(0xFFFEF3C7)
                                else -> Color(0xFFF1F5F9)
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = when (tripState.status) {
                                TripStatus.RUNNING -> Color(0xFFFEE2E2)
                                TripStatus.PAUSED -> Color(0xFFFEF3C7)
                                else -> Color(0xFFE2E8F0)
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("trip_status_badge")
                ) {
                    Text(
                        text = when (tripState.status) {
                            TripStatus.RUNNING -> strings.occupied
                            TripStatus.PAUSED -> strings.paused
                            else -> strings.vacant
                        },
                        color = when (tripState.status) {
                            TripStatus.RUNNING -> Color(0xFFDC2626)
                            TripStatus.PAUSED -> Color(0xFFD97706)
                            else -> Color(0xFF475569)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (strings.isTamil) 0.sp else 0.5.sp
                    )
                }
            }

            // =========================================================================
            // PART 1: ENLARGED MAP-FIRST ACTIVE TRIP DISPLAY vs METER DETAILS SWITCH
            // =========================================================================
            if (isTripActive && activeTripDisplayMode == ActiveTripDisplayMode.MAP_FIRST) {
                // ENLARGED VEHICLE CENTRE-CONSOLE MAP DISPLAY (AUTOMATICALLY OPENED WHEN TRIP STARTS)
                // Does NOT prominently display the running fare; provides "Meter Details" switch button.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("enlarged_active_trip_map_section"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // LIVE CENTRE-CONSOLE MAP WITH CLEAN MAP-FIRST CONTROLS
                    TripMapView(
                        tripState = tripState,
                        isNavigationConsoleMode = true,
                        isExpanded = false,
                        onToggleExpand = { isMapExpanded = true },
                        onOpenMeterDetails = { viewModel.showMeterDetails() },
                        onPauseTrip = { viewModel.pauseTrip() },
                        onResumeTrip = { viewModel.resumeTrip() },
                        onEndTrip = {
                            viewModel.completeTripAndSave("", "", "") { savedId ->
                                onNavigateToSummary(savedId.toInt())
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(540.dp)
                            .testTag("active_trip_enlarged_map")
                    )
                }
            } else {
                // PRE-TRIP DASHBOARD OR ACTIVE TRIP "METER DETAILS" INTERFACE
                if (isTripActive && activeTripDisplayMode == ActiveTripDisplayMode.METER_DETAILS) {
                    // RETURN TO LIVE MAP BUTTON AT TOP OF METER DETAILS VIEW
                    Button(
                        onClick = { viewModel.showMapDisplay() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F172A),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(52.dp)
                            .testTag("return_to_map_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = Color(0xFFF8B814),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.returnToMapButton,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else {
                    // PRE-TRIP LIVE MAP PREVIEW
                    TripMapView(
                        tripState = tripState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(vertical = 4.dp)
                    )
                }

                // SERVICE TYPE SELECTION COMPONENT
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.serviceTypeHeader,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = if (strings.isTamil) 0.2.sp else 1.5.sp
                            )
                            if (isTripActive) {
                                Surface(
                                    color = Color(0xFFFEF2F2),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFEE2E2))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = strings.fixedForTrip,
                                            tint = Color(0xFFE53935),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = strings.fixedForTrip,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE53935)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ServiceType.entries.forEach { mode ->
                                val isSelected = currentMode == mode
                                val modeLabel = strings.serviceTypeTitle(mode)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(enabled = !isTripActive) {
                                            viewModel.setServiceType(mode)
                                        }
                                        .testTag("service_type_${mode.id.lowercase()}"),
                                    color = if (isSelected) Color(0xFFE53935) else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFFE53935) else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = when (mode) {
                                                ServiceType.LOCAL -> Icons.Default.DirectionsCar
                                                ServiceType.RENTAL -> Icons.Default.Schedule
                                                ServiceType.OUTSTATION -> Icons.AutoMirrored.Filled.AltRoute
                                            },
                                            contentDescription = modeLabel,
                                            tint = if (isSelected) Color.White else Color(0xFF64748B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = modeLabel,
                                            color = if (isSelected) Color.White else Color(0xFF334155),
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // GIGANTIC FARE GAUGE DISPLAY CARD (SHOWN IN PRE-TRIP & IN "METER DETAILS" MODE)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(32.dp))
                        .testTag("meter_details_fare_card")
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = strings.currentFare,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                letterSpacing = if (strings.isTamil) 0.3.sp else 2.5.sp
                            )

                            Box(
                                modifier = Modifier.padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(
                                        text = String.format(Locale.US, "%.2f", tripState.currentFare),
                                        color = Color(0xFF0F172A),
                                        fontSize = 64.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.testTag("fare_text")
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tripState.currency,
                                        color = Color(0xFFE53935),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }

                            // Live Itemized Extra Charges Pill (shown when extra charges are added)
                            if (tripState.extraChargeItems.isNotEmpty()) {
                                Surface(
                                    color = Color(0xFFFEF2F2),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                    modifier = Modifier
                                        .padding(top = 4.dp, bottom = 2.dp)
                                        .clickable { showExtraChargesDialog = true }
                                        .testTag("live_extra_charges_summary_pill")
                                ) {
                                    val summaryText = tripState.extraChargeItems.joinToString(" • ") {
                                        "${strings.translateExtraChargePreset(it.label)}: ${tripState.currency}${String.format(Locale.US, "%.0f", it.amount)}"
                                    }
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircle,
                                            contentDescription = null,
                                            tint = Color(0xFFE53935),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${strings.extrasPillPrefix} (+${tripState.currency}${String.format(Locale.US, "%.2f", tripState.liveExtraChargesTotal)}): $summaryText",
                                            color = Color(0xFF991B1B),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(20.dp))
                                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = strings.distance,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = if (strings.isTamil) 0.sp else 0.5.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = String.format(Locale.US, "%.2f", tripState.distanceKm),
                                                color = Color(0xFF1E293B),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(strings.unitKm, color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(20.dp))
                                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        val secondTitle = when (currentMode) {
                                            ServiceType.RENTAL -> strings.billableHoursLabel
                                            ServiceType.OUTSTATION -> strings.waitHaltLabel
                                            ServiceType.LOCAL -> strings.waitTime
                                        }
                                        Text(
                                            text = secondTitle,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = if (strings.isTamil) 0.sp else 0.5.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            when (currentMode) {
                                                ServiceType.RENTAL -> {
                                                    Text(
                                                        text = "${tripState.billableHours}",
                                                        color = Color(0xFF1E293B),
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(strings.unitHr, color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                                ServiceType.OUTSTATION -> {
                                                    val waitMins = tripState.waitingSeconds / 60
                                                    Text(
                                                        text = "$waitMins",
                                                        color = Color(0xFF1E293B),
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(strings.unitMin, color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                                ServiceType.LOCAL -> {
                                                    val formattedWait = formatDuration(tripState.waitingSeconds).substring(3)
                                                    Text(
                                                        text = formattedWait,
                                                        color = Color(0xFF1E293B),
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(strings.unitMin, color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(20.dp))
                                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = strings.duration,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = if (strings.isTamil) 0.sp else 0.5.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            val durMins = tripState.durationSeconds / 60
                                            Text(
                                                text = "$durMins",
                                                color = Color(0xFF1E293B),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(strings.unitMin, color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        LiveSpeedBubble(
                            speedKmH = tripState.speedKmH,
                            visible = isTripActive,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 14.dp, start = 14.dp)
                        )

                        // RIGHT-SIDE MENU ICON (3-DOTS) TO ADD EXTRA CHARGES (TOLLS, PARKING, PERMIT, ETC.)
                        IconButton(
                            onClick = { showExtraChargesDialog = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 10.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (tripState.extraChargeItems.isNotEmpty()) Color(0xFFFEF2F2) else Color(0xFFF8FAFC)
                                )
                                .border(
                                    1.dp,
                                    if (tripState.extraChargeItems.isNotEmpty()) Color(0xFFFECACA) else Color(0xFFE2E8F0),
                                    CircleShape
                                )
                                .testTag("active_meter_extra_charges_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = strings.addExtraChargesTitle,
                                tint = if (tripState.extraChargeItems.isNotEmpty()) Color(0xFFE53935) else Color(0xFF334155)
                            )
                        }
                    }
                }

                // PRIMARY ACTION BUTTONS (IMMEDIATE START / PAUSE / END TRIP)
                ActiveTripActionButtonsRow(
                    status = tripState.status,
                    currentMode = currentMode,
                    onStartTrip = { viewModel.startTrip() },
                    onStartOtpTrip = { showOtpDialog = true },
                    onPauseTrip = { viewModel.pauseTrip() },
                    onResumeTrip = { viewModel.resumeTrip() },
                    onEndTrip = {
                        viewModel.completeTripAndSave("", "", "") { savedId ->
                            onNavigateToSummary(savedId.toInt())
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TRIP HISTORY HEADER + DEDICATED "CREATE NEW BILL" BUTTON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = strings.tripHistoryAndInvoices,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (strings.isTamil) 0.2.sp else 1.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (allTrips.isNotEmpty()) {
                        Text(
                            text = String.format(Locale.US, strings.recordedCountFormat, allTrips.size),
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showCreateManualBillDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                    modifier = Modifier.testTag("create_manual_bill_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = strings.createNewBill,
                        fontSize = if (strings.isTamil) 11.sp else 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (allTrips.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.noRecordedTripsYet,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trips_history_list")
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    allTrips.forEach { trip ->
                        HistoryTripRow(
                            trip = trip,
                            currency = tripState.currency,
                            onRowClick = { onNavigateToSummary(trip.id) },
                            onDelete = { viewModel.deleteTrip(trip.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // PREMIUM CREATOR CREDIT AT THE BOTTOM OF MAIN HOME METER SCREEN
            CreatorSignatureBanner(
                testTag = "home_creator_credit_banner"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // LOGO COLOR & FONT CUSTOMIZER DIALOG
    if (showLogoStyleDialog) {
        LogoStyleCustomizerDialog(
            vendorId = currentHeaderVendorId,
            vendorName = currentHeaderVendorName,
            onDismiss = { showLogoStyleDialog = false }
        )
    }

    // PER-TRIP VENDOR SELECTOR DIALOG
    if (showVendorSelectorDialog) {
        SelectTripVendorDialog(
            vendors = activeVendors,
            selectedVendorId = selectedVendor.vendorId,
            onSelectVendor = { chosen ->
                viewModel.selectVendorForNextTrip(chosen)
                showVendorSelectorDialog = false
            },
            onManageVendors = {
                showVendorSelectorDialog = false
                onNavigateToVendorManagement()
            },
            onDismiss = { showVendorSelectorDialog = false }
        )
    }

    // MANUAL BILL CREATION DIALOG
    if (showCreateManualBillDialog) {
        CreateManualBillDialog(
            currency = tripState.currency,
            selectedVendor = selectedVendor,
            onDismiss = { showCreateManualBillDialog = false },
            onCreateBill = { mode, custName, custMobile, custAddr, pickup, drop, distKm, durMin, waitMin, baseFare, extraCharges, finalFare, invDate, invTime ->
                viewModel.createManualBill(
                    serviceType = mode,
                    customerName = custName,
                    customerMobile = custMobile,
                    customerAddress = custAddr,
                    pickupAddress = pickup,
                    dropAddress = drop,
                    distanceKm = distKm,
                    durationMinutes = durMin,
                    waitingMinutes = waitMin,
                    baseFare = baseFare,
                    extraCharges = extraCharges,
                    finalTotalFare = finalFare,
                    vendor = selectedVendor,
                    invoiceDateText = invDate,
                    invoiceTimeText = invTime
                ) { createdTripId ->
                    showCreateManualBillDialog = false
                    onNavigateToSummary(createdTripId)
                }
            }
        )
    }

    // LIVE EXTRA CHARGES DIALOG (OPENED VIA RIGHT-SIDE 3-DOTS MENU ON ACTIVE METER)
    if (showExtraChargesDialog) {
        ActiveMeterExtraChargesDialog(
            currency = tripState.currency,
            currentItems = tripState.extraChargeItems,
            onAddCharge = { label, amount ->
                viewModel.addExtraCharge(label, amount)
            },
            onRemoveCharge = { id ->
                viewModel.removeExtraCharge(id)
            },
            onDismiss = { showExtraChargesDialog = false }
        )
    }

    // OTP TRIP AUTHORIZATION DIALOG
    if (showOtpDialog) {
        OtpTripDialog(
            onDismiss = { showOtpDialog = false },
            onOtpVerifiedAndStart = {
                showOtpDialog = false
                viewModel.startTrip()
            }
        )
    }

    // EXPANDED LIVE MAP FULL-SCREEN OVERLAY
    if (isMapExpanded && isTripActive) {
        Dialog(
            onDismissRequest = { isMapExpanded = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .statusBarsPadding()
            ) {
                TripMapView(
                    tripState = tripState,
                    isNavigationConsoleMode = true,
                    isExpanded = true,
                    onToggleExpand = { isMapExpanded = false },
                    onOpenMeterDetails = {
                        isMapExpanded = false
                        viewModel.showMeterDetails()
                    },
                    onPauseTrip = { viewModel.pauseTrip() },
                    onResumeTrip = { viewModel.resumeTrip() },
                    onEndTrip = {
                        isMapExpanded = false
                        viewModel.completeTripAndSave("", "", "") { savedId ->
                            onNavigateToSummary(savedId.toInt())
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("expanded_active_trip_map")
                )
            }
        }
    }
}

@Composable
private fun ActiveTripActionButtonsRow(
    status: TripStatus,
    currentMode: ServiceType,
    onStartTrip: () -> Unit,
    onStartOtpTrip: (() -> Unit)? = null,
    onPauseTrip: () -> Unit,
    onResumeTrip: () -> Unit,
    onEndTrip: () -> Unit
) {
    val strings = com.example.util.appStrings()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(vertical = 4.dp)
    ) {
        AnimatedContent(
            targetState = status,
            transitionSpec = {
                slideInVertically { height -> height } + fadeIn() togetherWith
                    slideOutVertically { height -> -height } + fadeOut()
            },
            label = "MainActionControls"
        ) { targetStatus ->
            when (targetStatus) {
                TripStatus.IDLE, TripStatus.FINISHED -> {
                    val startButtonLabel = strings.startTripLabelForMode(currentMode)
                    if (onStartOtpTrip != null) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onStartTrip,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE53935),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(32.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .fillMaxHeight()
                                    .testTag("start_trip_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "START (DIRECT)",
                                        fontSize = if (strings.isTamil) 12.sp else 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Button(
                                onClick = onStartOtpTrip,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0F296B),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(32.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .fillMaxHeight()
                                    .testTag("start_otp_trip_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pin,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "START WITH OTP",
                                        fontSize = if (strings.isTamil) 12.sp else 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = onStartTrip,
                            enabled = true,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53935),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(32.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("start_trip_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = startButtonLabel,
                                    fontSize = if (strings.isTamil) 13.sp else 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = if (strings.isTamil) 0.sp else 1.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                TripStatus.RUNNING -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onPauseTrip,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(32.dp),
                            border = BorderStroke(2.dp, Color(0xFFE53935)),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("pause_trip_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Pause, contentDescription = null, tint = Color(0xFFE53935))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.pauseAction,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE53935),
                                    fontSize = if (strings.isTamil) 12.sp else 14.sp,
                                    letterSpacing = if (strings.isTamil) 0.sp else 1.sp
                                )
                            }
                        }

                        Button(
                            onClick = onEndTrip,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(32.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
                                .testTag("stop_trip_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.endTrip,
                                    fontWeight = FontWeight.Black,
                                    fontSize = if (strings.isTamil) 12.sp else 14.sp,
                                    letterSpacing = if (strings.isTamil) 0.sp else 1.sp
                                )
                            }
                        }
                    }
                }

                TripStatus.PAUSED -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onResumeTrip,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(32.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                                .testTag("resume_trip_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.resumeAction,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (strings.isTamil) 12.sp else 14.sp,
                                    letterSpacing = if (strings.isTamil) 0.sp else 1.sp
                                )
                            }
                        }

                        Button(
                            onClick = onEndTrip,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(32.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                                .testTag("stop_trip_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.endTrip,
                                    fontWeight = FontWeight.Black,
                                    fontSize = if (strings.isTamil) 12.sp else 14.sp,
                                    letterSpacing = if (strings.isTamil) 0.sp else 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectTripVendorDialog(
    vendors: List<VendorProfileEntity>,
    selectedVendorId: String,
    onSelectVendor: (VendorProfileEntity) -> Unit,
    onManageVendors: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = com.example.util.appStrings()
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .testTag("vendor_selector_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.selectVendorDialogTitle,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = strings.selectVendorDialogSubtitle,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = strings.close, tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vendors, key = { it.vendorId }) { vendor ->
                        val isSelected = vendor.vendorId == selectedVendorId
                        Surface(
                            color = if (isSelected) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFFCA1F2F) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectVendor(vendor) }
                                .testTag("vendor_option_${vendor.vendorId}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    VendorLogoBadge(
                                        vendorId = vendor.vendorId,
                                        vendorName = vendor.vendorName,
                                        height = 38.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = vendor.vendorName,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        if (VendorLogoRenderer.isGetTaxiVendor(vendor.vendorId, vendor.vendorName)) {
                                            Text(
                                                text = strings.vendorPermanentDefaultBadge,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFCA1F2F)
                                            )
                                        }
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFCA1F2F),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onManageVendors,
                        modifier = Modifier.testTag("dialog_manage_vendors_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color(0xFFCA1F2F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.manageVendorsButton,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCA1F2F)
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(strings.close, color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveMeterExtraChargesDialog(
    currency: String,
    currentItems: List<ExtraChargeItem>,
    onAddCharge: (String, Double) -> Unit,
    onRemoveCharge: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = com.example.util.appStrings()
    var chargeLabel by remember { mutableStateOf(strings.presetToll) }
    var chargeAmountInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quickPresets = listOf(
        strings.presetToll,
        strings.presetParking,
        strings.presetPermit,
        strings.presetAirportFee,
        strings.presetWaiting,
        strings.presetLuggage
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("active_meter_extra_charges_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.AddCard,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.addExtraChargesTitle,
                            fontWeight = FontWeight.Black,
                            fontSize = if (strings.isTamil) 16.sp else 18.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = strings.close, tint = Color(0xFF64748B))
                    }
                }

                Text(
                    text = strings.addExtraChargesDesc,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp)
                )

                // Quick Preset Chips
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPresets.chunked(3).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowPresets.forEach { preset ->
                                FilterChip(
                                    selected = chargeLabel.equals(preset, ignoreCase = true),
                                    onClick = { chargeLabel = preset },
                                    label = {
                                        Text(
                                            text = preset,
                                            fontSize = if (strings.isTamil) 10.sp else 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
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

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = chargeLabel,
                    onValueChange = {
                        chargeLabel = it
                        errorMessage = null
                    },
                    label = { Text(strings.chargeTypeLabel) },
                    placeholder = { Text(strings.chargeTypePlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("extra_charge_label_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = chargeAmountInput,
                    onValueChange = {
                        chargeAmountInput = it
                        errorMessage = null
                    },
                    label = { Text(String.format(Locale.US, strings.amountLabelFormat, currency)) },
                    placeholder = { Text("e.g. 50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("extra_charge_amount_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFDC2626),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val parsedAmount = chargeAmountInput.trim().toDoubleOrNull()
                        if (parsedAmount == null || parsedAmount <= 0.0) {
                            errorMessage = strings.amountErrorInvalid
                        } else {
                            onAddCharge(chargeLabel.trim().ifBlank { strings.extraChargeFallback }, parsedAmount)
                            chargeAmountInput = ""
                            errorMessage = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_extra_charge_submit_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.addToLiveMeterFare, fontWeight = FontWeight.Black, color = Color.White)
                }

                if (currentItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.addedExtraChargesHeader,
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = if (strings.isTamil) 0.sp else 1.sp
                        )
                        Text(
                            text = "${strings.totalPrefix}: $currency${String.format(Locale.US, "%.2f", ExtraChargeItem.totalAmount(currentItems))}",
                            color = Color(0xFFE53935),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentItems.forEach { item ->
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.translateExtraChargePreset(item.label),
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$currency${String.format(Locale.US, "%.2f", item.amount)}",
                                            color = Color(0xFFE53935),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { onRemoveCharge(item.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "${strings.delete} ${item.label}",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        val pendingAmount = chargeAmountInput.trim().toDoubleOrNull()
                        if (pendingAmount != null && pendingAmount > 0.0) {
                            onAddCharge(chargeLabel.trim().ifBlank { strings.extraChargeFallback }, pendingAmount)
                        }
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("extra_charges_done_button")
                ) {
                    Text(strings.done, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                }
            }
        }
    }
}

@Composable
private fun OtpTripDialog(
    onDismiss: () -> Unit,
    onOtpVerifiedAndStart: () -> Unit
) {
    var enteredOtp by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFFFEF2F2), CircleShape)
                        .border(1.dp, Color(0xFFFEE2E2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pin,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Trip Authorization OTP",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF1E40AF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = TripOtpManager.CUSTOMER_OTP_MESSAGE,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E40AF),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = enteredOtp,
                    onValueChange = {
                        if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                            enteredOtp = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Enter 4-Digit OTP") },
                    placeholder = { Text("e.g. 1010, 2020, 1234") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp,
                        textAlign = TextAlign.Center
                    ),
                    shape = RoundedCornerShape(14.dp),
                    isError = errorMessage != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_otp_input_field")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = {
                            val clean = enteredOtp.trim()
                            if (clean.length != 4) {
                                errorMessage = "Please enter all 4 digits."
                            } else if (TripOtpManager.PERMANENT_OTPS.contains(clean)) {
                                onOtpVerifiedAndStart()
                            } else {
                                errorMessage = "Invalid Trip OTP. Please ask customer for correct 4-digit code."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("verify_otp_start_button")
                    ) {
                        Text("Verify & Start", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateManualBillDialog(
    currency: String,
    selectedVendor: VendorProfileEntity,
    onDismiss: () -> Unit,
    onCreateBill: (
        mode: ServiceType,
        custName: String,
        custMobile: String,
        custAddress: String,
        pickup: String,
        drop: String,
        distanceKm: Double,
        durationMin: Long,
        waitMin: Long,
        baseFare: Double,
        extraCharges: Double,
        finalFare: Double,
        invoiceDate: String,
        invoiceTime: String
    ) -> Unit
) {
    val context = LocalContext.current
    val strings = com.example.util.appStrings()
    var selectedMode by remember { mutableStateOf(ServiceType.LOCAL) }
    var custName by remember { mutableStateOf("") }
    var custMobile by remember { mutableStateOf("") }
    var custAddress by remember { mutableStateOf("") }
    var pickupAddr by remember { mutableStateOf("") }
    var dropAddr by remember { mutableStateOf("") }

    // Date & Time Picker Setup
    val calendar = remember { java.util.Calendar.getInstance() }
    var invoiceDateInput by remember {
        mutableStateOf(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time))
    }
    var invoiceTimeInput by remember {
        mutableStateOf(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time))
    }

    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(java.util.Calendar.YEAR, year)
                calendar.set(java.util.Calendar.MONTH, month)
                calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                invoiceDateInput = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    val timePickerDialog = remember {
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(java.util.Calendar.MINUTE, minute)
                invoiceTimeInput = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
            },
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE),
            false
        )
    }

    // Common Waiting Charge inputs for all 3 modes
    var waitMinutesInput by remember { mutableStateOf("0") }
    var waitRatePerMinInput by remember { mutableStateOf("2.0") }

    // Local Mode Inputs
    var localDistanceInput by remember { mutableStateOf("10.0") }
    var localRatePerKmInput by remember { mutableStateOf("15.0") }
    var localBaseFareInput by remember { mutableStateOf("100.0") }
    var localExtraChargesInput by remember { mutableStateOf("0.0") }

    // Hourly Rental Mode Inputs (Test scenario: 4 hrs @ ₹350 = 1,400. 60 km with 10 km/hr free = 40 km free. Chargeable = 20 km @ ₹20 = 400. Total = 1,800)
    var rentalHoursInput by remember { mutableStateOf("4") }
    var rentalHourlyRateInput by remember { mutableStateOf("350.0") }
    var rentalTotalDistanceInput by remember { mutableStateOf("60.0") }
    var rentalFreeKmPerHourInput by remember { mutableStateOf("10.0") }
    var rentalExtraKmRateInput by remember { mutableStateOf("20.0") }
    var rentalExtraChargesInput by remember { mutableStateOf("0.0") }

    // Outstation Mode Inputs
    var outstationDistanceInput by remember { mutableStateOf("100.0") }
    var outstationRatePerKmInput by remember { mutableStateOf("14.0") }
    var outstationDriverBattaInput by remember { mutableStateOf("400.0") }
    var outstationTollInput by remember { mutableStateOf("0.0") }
    var outstationPermitInput by remember { mutableStateOf("0.0") }
    var outstationParkingInput by remember { mutableStateOf("0.0") }
    var outstationOtherExtraInput by remember { mutableStateOf("0.0") }

    // Shared Duration in minutes for local/outstation
    var localDurationInput by remember { mutableStateOf("30") }

    // Calculations based on mode
    val waitMins = waitMinutesInput.toDoubleOrNull() ?: 0.0
    val waitRate = waitRatePerMinInput.toDoubleOrNull() ?: 2.0
    val computedWaitCharge = (waitMins * waitRate).coerceAtLeast(0.0)

    val (computedAutoTotal, computedBase, computedDist, computedExtras) = when (selectedMode) {
        ServiceType.LOCAL -> {
            val dist = localDistanceInput.toDoubleOrNull() ?: 0.0
            val rate = localRatePerKmInput.toDoubleOrNull() ?: 15.0
            val base = localBaseFareInput.toDoubleOrNull() ?: 100.0
            val extra = localExtraChargesInput.toDoubleOrNull() ?: 0.0
            val distCharge = dist * rate
            val total = base + distCharge + computedWaitCharge + extra
            listOf(total, base, dist, extra + computedWaitCharge)
        }
        ServiceType.RENTAL -> {
            val hrs = rentalHoursInput.toDoubleOrNull() ?: 1.0
            val hourlyRate = rentalHourlyRateInput.toDoubleOrNull() ?: 350.0
            val totalDist = rentalTotalDistanceInput.toDoubleOrNull() ?: 0.0
            val freeKmPerHr = rentalFreeKmPerHourInput.toDoubleOrNull() ?: 10.0
            val extraKmRate = rentalExtraKmRateInput.toDoubleOrNull() ?: 20.0
            val extra = rentalExtraChargesInput.toDoubleOrNull() ?: 0.0

            val hourlyFare = hrs * hourlyRate
            val freeKm = hrs * freeKmPerHr
            val chargeableKm = (totalDist - freeKm).coerceAtLeast(0.0)
            val extraKmCharge = chargeableKm * extraKmRate
            val total = hourlyFare + extraKmCharge + computedWaitCharge + extra
            listOf(total, hourlyFare, totalDist, extraKmCharge + computedWaitCharge + extra)
        }
        ServiceType.OUTSTATION -> {
            val dist = outstationDistanceInput.toDoubleOrNull() ?: 0.0
            val rate = outstationRatePerKmInput.toDoubleOrNull() ?: 14.0
            val batta = outstationDriverBattaInput.toDoubleOrNull() ?: 400.0
            val toll = outstationTollInput.toDoubleOrNull() ?: 0.0
            val permit = outstationPermitInput.toDoubleOrNull() ?: 0.0
            val parking = outstationParkingInput.toDoubleOrNull() ?: 0.0
            val otherExtra = outstationOtherExtraInput.toDoubleOrNull() ?: 0.0

            val distCharge = dist * rate
            val combinedExtras = toll + permit + parking + otherExtra + computedWaitCharge
            val total = distCharge + batta + combinedExtras
            listOf(total, batta, dist, combinedExtras)
        }
    }

    // Editable Final Total with live sync
    var finalFareInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", computedAutoTotal)) }
    var lastComputedTotal by remember { mutableDoubleStateOf(computedAutoTotal) }

    LaunchedEffect(computedAutoTotal) {
        if (computedAutoTotal != lastComputedTotal) {
            finalFareInput = String.format(Locale.US, "%.2f", computedAutoTotal)
            lastComputedTotal = computedAutoTotal
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VendorLogoBadge(
                    vendorId = selectedVendor.vendorId,
                    vendorName = selectedVendor.vendorName,
                    height = 38.dp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.createManualBillTitle,
                    fontWeight = FontWeight.Black,
                    fontSize = if (strings.isTamil) 16.sp else 18.sp,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${selectedVendor.vendorName} • ${strings.createManualBillDesc}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // 1. EDITABLE INVOICE DATE & TIME PICKER SECTION
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "BILL DATE & TIME (CALENDAR & CLOCK PICKERS)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = invoiceDateInput,
                                onValueChange = { invoiceDateInput = it },
                                label = { Text("Date") },
                                trailingIcon = {
                                    IconButton(onClick = { datePickerDialog.show() }) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = "Pick Date",
                                            tint = Color(0xFF1E3A8A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("manual_bill_invoice_date")
                            )
                            OutlinedTextField(
                                value = invoiceTimeInput,
                                onValueChange = { invoiceTimeInput = it },
                                label = { Text("Time") },
                                trailingIcon = {
                                    IconButton(onClick = { timePickerDialog.show() }) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = "Pick Time",
                                            tint = Color(0xFF1E3A8A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_bill_invoice_time")
                            )
                        }
                    }
                }

                // 2. SERVICE MODE SELECTOR (Local, Rental, Outstation)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ServiceType.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = {
                                Text(
                                    text = strings.serviceTypeShort(mode),
                                    fontSize = if (strings.isTamil) 10.sp else 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE53935),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Customer Info Fields
                OutlinedTextField(
                    value = custName,
                    onValueChange = { custName = it },
                    label = { Text(strings.customerNameLabel) },
                    placeholder = { Text(strings.customerNamePlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_bill_customer_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = custMobile,
                    onValueChange = { custMobile = it },
                    label = { Text(strings.customerMobileWhatsappLabel) },
                    placeholder = { Text("e.g. 9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_bill_customer_mobile")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = custAddress,
                    onValueChange = { custAddress = it },
                    label = { Text(strings.customerAddressOptionalLabel) },
                    placeholder = { Text("e.g. RS Puram, Coimbatore") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pickupAddr,
                        onValueChange = { pickupAddr = it },
                        label = { Text(strings.pickupPointLabel) },
                        placeholder = { Text("RS Puram") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dropAddr,
                        onValueChange = { dropAddr = it },
                        label = { Text(strings.dropPointLabel) },
                        placeholder = { Text("Airport") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. MODE-SPECIFIC TARIFF & FARE CALCULATOR INPUTS
                when (selectedMode) {
                    ServiceType.LOCAL -> {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "LOCAL METERED FARE CALCULATOR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = localDistanceInput,
                                        onValueChange = { localDistanceInput = it },
                                        label = { Text("Distance (KM)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = localRatePerKmInput,
                                        onValueChange = { localRatePerKmInput = it },
                                        label = { Text("Rate ($currency/KM)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = localBaseFareInput,
                                        onValueChange = { localBaseFareInput = it },
                                        label = { Text("Base Fare ($currency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = localExtraChargesInput,
                                        onValueChange = { localExtraChargesInput = it },
                                        label = { Text("Extra Charges ($currency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    ServiceType.RENTAL -> {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "HOURLY RENTAL FARE CALCULATOR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = rentalHoursInput,
                                        onValueChange = { rentalHoursInput = it },
                                        label = { Text("Duration (Hours)") },
                                        placeholder = { Text("e.g. 4") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = rentalHourlyRateInput,
                                        onValueChange = { rentalHourlyRateInput = it },
                                        label = { Text("Rate ($currency/Hr)") },
                                        placeholder = { Text("e.g. 350") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = rentalTotalDistanceInput,
                                        onValueChange = { rentalTotalDistanceInput = it },
                                        label = { Text("Total Distance (KM)") },
                                        placeholder = { Text("e.g. 60") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = rentalFreeKmPerHourInput,
                                        onValueChange = { rentalFreeKmPerHourInput = it },
                                        label = { Text("Free KM/Hr") },
                                        placeholder = { Text("e.g. 10") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = rentalExtraKmRateInput,
                                        onValueChange = { rentalExtraKmRateInput = it },
                                        label = { Text("Extra KM Rate ($currency/KM)") },
                                        placeholder = { Text("e.g. 20") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = rentalExtraChargesInput,
                                        onValueChange = { rentalExtraChargesInput = it },
                                        label = { Text("Extra Charges ($currency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    ServiceType.OUTSTATION -> {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "OUTSTATION FARE CALCULATOR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = outstationDistanceInput,
                                        onValueChange = { outstationDistanceInput = it },
                                        label = { Text("Distance (KM)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = outstationRatePerKmInput,
                                        onValueChange = { outstationRatePerKmInput = it },
                                        label = { Text("Rate ($currency/KM)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = outstationDriverBattaInput,
                                        onValueChange = { outstationDriverBattaInput = it },
                                        label = { Text("Driver Batta / Base ($currency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = outstationTollInput,
                                        onValueChange = { outstationTollInput = it },
                                        label = { Text("Toll ($currency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = outstationPermitInput,
                                        onValueChange = { outstationPermitInput = it },
                                        label = { Text("Permit ($currency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = outstationParkingInput,
                                        onValueChange = { outstationParkingInput = it },
                                        label = { Text("Parking ($currency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = outstationOtherExtraInput,
                                    onValueChange = { outstationOtherExtraInput = it },
                                    label = { Text("Other Extra Charges ($currency)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. ITEMIZED WAITING TIME INPUT FOR ALL MODES
                Surface(
                    color = Color(0xFFFEF3C7).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ITEMIZED WAITING CHARGE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "= $currency ${String.format(Locale.US, "%.2f", computedWaitCharge)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF92400E)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = waitMinutesInput,
                                onValueChange = { waitMinutesInput = it },
                                label = { Text("Wait Time (Mins)") },
                                placeholder = { Text("e.g. 15") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = waitRatePerMinInput,
                                onValueChange = { waitRatePerMinInput = it },
                                label = { Text("Wait Rate ($currency/Min)") },
                                placeholder = { Text("e.g. 2.0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. LIVE AUTOMATIC FARE BREAKDOWN & TOTAL CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CALCULATED TOTAL FARE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "$currency ${String.format(Locale.US, "%.2f", computedAutoTotal)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF8B814)
                            )
                        }
                        if (selectedMode == ServiceType.RENTAL) {
                            val hrs = rentalHoursInput.toDoubleOrNull() ?: 0.0
                            val hrRate = rentalHourlyRateInput.toDoubleOrNull() ?: 0.0
                            val tDist = rentalTotalDistanceInput.toDoubleOrNull() ?: 0.0
                            val freeKm = hrs * (rentalFreeKmPerHourInput.toDoubleOrNull() ?: 10.0)
                            val chKm = (tDist - freeKm).coerceAtLeast(0.0)
                            val extraDistRate = rentalExtraKmRateInput.toDoubleOrNull() ?: 20.0
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${hrs.toInt()} hrs @ $currency$hrRate = $currency${(hrs * hrRate).toInt()} | " +
                                        "${tDist.toInt()} KM with ${freeKm.toInt()} KM free = ${chKm.toInt()} KM extra @ $currency$extraDistRate = $currency${(chKm * extraDistRate).toInt()}",
                                fontSize = 10.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Final Total Input (editable if driver wants to apply custom rounding/discount)
                OutlinedTextField(
                    value = finalFareInput,
                    onValueChange = { finalFareInput = it },
                    label = { Text(String.format(Locale.US, strings.finalTotalFareLabelFormat, currency)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53935),
                        unfocusedBorderColor = Color(0xFFE53935)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_bill_final_fare")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.cancel, color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = {
                            val finalTotal = (finalFareInput.toDoubleOrNull() ?: computedAutoTotal).coerceAtLeast(0.0)
                            val durMinutes = when (selectedMode) {
                                ServiceType.RENTAL -> ((rentalHoursInput.toDoubleOrNull() ?: 1.0) * 60).toLong()
                                else -> localDurationInput.toLongOrNull() ?: 30L
                            }

                            onCreateBill(
                                selectedMode,
                                custName,
                                custMobile,
                                custAddress,
                                pickupAddr,
                                dropAddr,
                                computedDist,
                                durMinutes,
                                waitMins.toLong(),
                                computedBase,
                                computedExtras,
                                finalTotal,
                                invoiceDateInput.trim(),
                                invoiceTimeInput.trim()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("manual_bill_submit_button")
                    ) {
                        Text(
                            text = strings.generateBillButton,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = if (strings.isTamil) 12.sp else 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTripRow(
    trip: TripEntity,
    currency: String,
    onRowClick: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = com.example.util.appStrings()
    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val formattedDate = dateFormatter.format(Date(trip.startTime))
    val tripNum = if (trip.tripNumber.isNotBlank()) trip.tripNumber else String.format(Locale.US, "GTM-%d", trip.id)
    val mode = ServiceType.fromId(trip.serviceType)
    val modeTitle = strings.serviceTypeTitle(mode)
    val vendorNameDisplay = trip.vendorName.ifBlank { VendorLogoRenderer.DEFAULT_VENDOR_NAME }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRowClick() }
            .testTag("history_row_${trip.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            when (mode) {
                                ServiceType.RENTAL -> Color(0xFFFEF3C7)
                                ServiceType.OUTSTATION -> Color(0xFFEEF2FF)
                                ServiceType.LOCAL -> Color(0xFFFEF2F2)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (mode) {
                            ServiceType.RENTAL -> Icons.Default.Schedule
                            ServiceType.OUTSTATION -> Icons.AutoMirrored.Filled.AltRoute
                            ServiceType.LOCAL -> Icons.Default.DirectionsCar
                        },
                        contentDescription = null,
                        tint = when (mode) {
                            ServiceType.RENTAL -> Color(0xFFD97706)
                            ServiceType.OUTSTATION -> Color(0xFF4F46E5)
                            ServiceType.LOCAL -> Color(0xFFE53935)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tripNum,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = vendorNameDisplay,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFCA1F2F),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = when (mode) {
                                ServiceType.RENTAL -> Color(0xFFFEF3C7)
                                ServiceType.OUTSTATION -> Color(0xFFEEF2FF)
                                ServiceType.LOCAL -> Color(0xFFF1F5F9)
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = modeTitle,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (mode) {
                                    ServiceType.RENTAL -> Color(0xFFB45309)
                                    ServiceType.OUTSTATION -> Color(0xFF4338CA)
                                    ServiceType.LOCAL -> Color(0xFF475569)
                                },
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "$formattedDate • ${String.format(Locale.US, "%.2f", trip.distanceKm)} km • ${trip.durationSeconds / 60}m",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )

                    if (trip.customerName.isNotBlank()) {
                        Text(
                            text = "${strings.customerPrefix}: ${trip.customerName}",
                            fontSize = 10.sp,
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${trip.currency}${String.format(Locale.US, "%.2f", trip.totalFare)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color(0xFFE53935)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = strings.delete,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "00:%02d:%02d", mins, secs)
    }
}
