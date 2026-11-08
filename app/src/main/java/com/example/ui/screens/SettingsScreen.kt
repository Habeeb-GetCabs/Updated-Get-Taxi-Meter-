package com.example.ui.screens

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.LocationTrackingService
import com.example.viewmodel.SettingsViewModel

private fun formatDoubleInput(value: Double?): String {
    if (value == null) return ""
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDriverProfile: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToVendorManagement: () -> Unit = {}
) {
    val appLanguageCode by viewModel.appLanguage.collectAsStateWithLifecycle()
    val strings = com.example.util.appStrings()

    // Local / Metered states
    val baseFareState by viewModel.baseFare.collectAsStateWithLifecycle()
    val farePerKmState by viewModel.farePerKm.collectAsStateWithLifecycle()
    val waitFarePerMinState by viewModel.waitFarePerMin.collectAsStateWithLifecycle()
    val speedThresholdState by viewModel.speedThreshold.collectAsStateWithLifecycle()
    val audioEnabledState by viewModel.audioEnabled.collectAsStateWithLifecycle()
    val floatingBubbleEnabledState by viewModel.floatingBubbleEnabled.collectAsStateWithLifecycle()
    val currencyState by viewModel.currency.collectAsStateWithLifecycle()

    // Hourly Rental states
    val rentalHourlyRateState by viewModel.rentalHourlyRate.collectAsStateWithLifecycle()
    val rentalMinHoursState by viewModel.rentalMinHours.collectAsStateWithLifecycle()
    val rentalExtraHrRateState by viewModel.rentalExtraHrRate.collectAsStateWithLifecycle()
    val rentalExtraKmRateState by viewModel.rentalExtraKmRate.collectAsStateWithLifecycle()

    // Outstation states
    val outstationFarePerKmState by viewModel.outstationFarePerKm.collectAsStateWithLifecycle()
    val outstationMinFareState by viewModel.outstationMinFare.collectAsStateWithLifecycle()
    val outstationWaitRateState by viewModel.outstationWaitRate.collectAsStateWithLifecycle()
    val outstationDriverBataState by viewModel.outstationDriverBata.collectAsStateWithLifecycle()
    val outstationAdditionalChargesState by viewModel.outstationAdditionalCharges.collectAsStateWithLifecycle()
    val distanceCalibrationFactorState by viewModel.distanceCalibrationFactor.collectAsStateWithLifecycle()

    // Local inputs
    var baseFareInput by remember { mutableStateOf("") }
    var farePerKmInput by remember { mutableStateOf("") }
    var waitFarePerMinInput by remember { mutableStateOf("") }
    var speedThresholdInput by remember { mutableStateOf("") }
    var currencyInput by remember { mutableStateOf("") }
    var audioEnabled by remember { mutableStateOf(true) }
    var floatingBubbleEnabled by remember { mutableStateOf(true) }
    var distanceCalibrationFactorInput by remember { mutableStateOf("1.0") }

    // Hourly Rental inputs
    var rentalHourlyRateInput by remember { mutableStateOf("") }
    var rentalMinHoursInput by remember { mutableStateOf("") }
    var rentalExtraHrRateInput by remember { mutableStateOf("") }
    var rentalExtraKmRateInput by remember { mutableStateOf("") }

    // Outstation inputs
    var outstationFarePerKmInput by remember { mutableStateOf("") }
    var outstationMinFareInput by remember { mutableStateOf("") }
    var outstationWaitRateInput by remember { mutableStateOf("") }
    var outstationDriverBataInput by remember { mutableStateOf("") }
    var outstationAdditionalChargesInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    LaunchedEffect(
        baseFareState, farePerKmState, waitFarePerMinState, speedThresholdState, currencyState, audioEnabledState,
        floatingBubbleEnabledState,
        rentalHourlyRateState, rentalMinHoursState, rentalExtraHrRateState, rentalExtraKmRateState,
        outstationFarePerKmState, outstationMinFareState, outstationWaitRateState, outstationDriverBataState, outstationAdditionalChargesState,
        distanceCalibrationFactorState
    ) {
        baseFareInput = formatDoubleInput(baseFareState)
        farePerKmInput = formatDoubleInput(farePerKmState)
        waitFarePerMinInput = formatDoubleInput(waitFarePerMinState)
        speedThresholdInput = formatDoubleInput(speedThresholdState)
        currencyInput = currencyState
        audioEnabled = audioEnabledState
        floatingBubbleEnabled = floatingBubbleEnabledState
        distanceCalibrationFactorInput = formatDoubleInput(distanceCalibrationFactorState)

        rentalHourlyRateInput = formatDoubleInput(rentalHourlyRateState)
        rentalMinHoursInput = formatDoubleInput(rentalMinHoursState)
        rentalExtraHrRateInput = formatDoubleInput(rentalExtraHrRateState)
        rentalExtraKmRateInput = formatDoubleInput(rentalExtraKmRateState)

        outstationFarePerKmInput = formatDoubleInput(outstationFarePerKmState)
        outstationMinFareInput = formatDoubleInput(outstationMinFareState)
        outstationWaitRateInput = formatDoubleInput(outstationWaitRateState)
        outstationDriverBataInput = formatDoubleInput(outstationDriverBataState)
        outstationAdditionalChargesInput = formatDoubleInput(outstationAdditionalChargesState)
    }

    var showSavedMessage by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsScreenTitle, color = Color(0xFF0F172A), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = Color(0xFF475569)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("reset_tariffs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = strings.refillButton,
                            tint = Color(0xFFE53935)
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
                    val bf = baseFareInput.trim().toDoubleOrNull()
                    val fk = farePerKmInput.trim().toDoubleOrNull()
                    val wf = waitFarePerMinInput.trim().toDoubleOrNull()
                    val st = speedThresholdInput.trim().toDoubleOrNull() ?: 5.0
                    val curr = currencyInput.ifBlank { "₹" }

                    val rRate = rentalHourlyRateInput.trim().toDoubleOrNull()
                    val rMinHrs = rentalMinHoursInput.trim().toDoubleOrNull()
                    val rExtraHr = rentalExtraHrRateInput.trim().toDoubleOrNull()
                    val rExtraKm = rentalExtraKmRateInput.trim().toDoubleOrNull()

                    val oRate = outstationFarePerKmInput.trim().toDoubleOrNull()
                    val oMinFare = outstationMinFareInput.trim().toDoubleOrNull()
                    val oWaitRate = outstationWaitRateInput.trim().toDoubleOrNull()
                    val oBata = outstationDriverBataInput.trim().toDoubleOrNull()
                    val oAddn = outstationAdditionalChargesInput.trim().toDoubleOrNull()
                    val calFactor = distanceCalibrationFactorInput.trim().toDoubleOrNull() ?: 1.0

                    viewModel.updateBaseFare(bf)
                    viewModel.updateFarePerKm(fk)
                    viewModel.updateWaitFarePerMin(wf)
                    viewModel.updateSpeedThreshold(st)
                    viewModel.updateCurrency(curr)
                    viewModel.updateAudioEnabled(audioEnabled)
                    viewModel.updateFloatingBubbleEnabled(floatingBubbleEnabled)
                    viewModel.updateDistanceCalibrationFactor(calFactor)

                    viewModel.updateRentalHourlyRate(rRate)
                    viewModel.updateRentalMinHours(rMinHrs)
                    viewModel.updateRentalExtraHrRate(rExtraHr)
                    viewModel.updateRentalExtraKmRate(rExtraKm)

                    viewModel.updateOutstationFarePerKm(oRate)
                    viewModel.updateOutstationMinFare(oMinFare)
                    viewModel.updateOutstationWaitRate(oWaitRate)
                    viewModel.updateOutstationDriverBata(oBata)
                    viewModel.updateOutstationAdditionalCharges(oAddn)

                    LocationTrackingService.startMonitoring(
                        context = context,
                        baseFare = bf ?: 100.0,
                        farePerKm = fk ?: 30.0,
                        waitFarePerMin = wf ?: 2.0,
                        currency = curr,
                        speedThreshold = st,
                        calibrationFactor = calFactor
                    )

                    showSavedMessage = true
                },
                icon = { Icon(Icons.Default.Save, contentDescription = null, tint = Color.White) },
                text = { Text(strings.saveSettingsButton, fontWeight = FontWeight.Black, letterSpacing = if (strings.isTamil) 0.sp else 1.sp) },
                containerColor = Color(0xFFE53935),
                contentColor = Color.White,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.testTag("save_settings_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (showSavedMessage) {
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
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF065F46))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            strings.settingsSavedSuccess,
                            color = Color(0xFF065F46),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // --------------------------------
            // BILINGUAL LANGUAGE SELECTOR CARD (ENGLISH & TAMIL)
            // --------------------------------
            val isTamilSelected = com.example.util.AppLanguage.fromCode(appLanguageCode).isTamil || strings.isTamil
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFFFECDD3), RoundedCornerShape(20.dp))
                    .testTag("language_selector_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE53935), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.languageSectionTitle,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A),
                                letterSpacing = if (strings.isTamil) 0.sp else 0.5.sp
                            )
                            Text(
                                text = strings.languageSectionSubtitle,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // English Option
                        val isEnglishActive = !isTamilSelected
                        Button(
                            onClick = { viewModel.updateAppLanguage("en") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEnglishActive) Color(0xFFE53935) else Color(0xFFF8FAFC),
                                contentColor = if (isEnglishActive) Color.White else Color(0xFF334155)
                            ),
                            border = if (isEnglishActive) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("language_option_en")
                        ) {
                            if (isEnglishActive) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "English",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }

                        // Tamil Option (தமிழ்)
                        Button(
                            onClick = { viewModel.updateAppLanguage("ta") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTamilSelected) Color(0xFFE53935) else Color(0xFFF8FAFC),
                                contentColor = if (isTamilSelected) Color.White else Color(0xFF334155)
                            ),
                            border = if (isTamilSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("language_option_ta")
                        ) {
                            if (isTamilSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "தமிழ்",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // QUICK RESET TO STANDARD PREFILLED TARIFF BANNER
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFEA580C), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.standardTariffsTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF9A3412),
                                letterSpacing = if (strings.isTamil) 0.sp else 0.5.sp
                            )
                            Text(
                                text = strings.standardTariffsSubtitle,
                                fontSize = 11.sp,
                                color = Color(0xFFC2410C),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.resetToDefaultTariffs()
                            showSavedMessage = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC2410C)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDBA74))
                    ) {
                        Text(strings.refillButton, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // DRIVER & VEHICLE PROFILE
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE53935), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.driverVehicleProfileTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A),
                                letterSpacing = if (strings.isTamil) 0.sp else 0.5.sp
                            )
                            Text(
                                text = strings.driverVehicleProfileSubtitle,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Button(
                        onClick = onNavigateToDriverProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.editProfileButton, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // VENDOR MANAGEMENT (ADMIN MULTI-VENDOR & INVOICE BRANDING)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(20.dp))
                    .testTag("vendor_management_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFCA1F2F), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.vendorManagementTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A),
                                letterSpacing = if (strings.isTamil) 0.sp else 0.5.sp
                            )
                            Text(
                                text = strings.vendorManagementSubtitle,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Button(
                        onClick = onNavigateToVendorManagement,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCA1F2F)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("open_vendor_management_button")
                    ) {
                        Text(strings.manageVendorsButton, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ABOUT US (CLIENT OPERATOR)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFEF2F2), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFE53935))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.aboutUsCardTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A),
                                letterSpacing = if (strings.isTamil) 0.sp else 0.5.sp
                            )
                            Text(
                                text = strings.aboutUsCardSubtitle,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onNavigateToAbout,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A)),
                        modifier = Modifier.testTag("about_us_settings_button")
                    ) {
                        Text(strings.viewAboutButton, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // FLOATING SPEEDOMETER BUBBLE OVERLAY CARD (AUTO ON MINIMIZE + TOGGLE)
            val hasOverlayPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.provider.Settings.canDrawOverlays(context)
            } else {
                true
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("floating_bubble_settings_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFE53935), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = strings.floatingBubbleCardTitle,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    letterSpacing = if (strings.isTamil) 0.sp else 0.5.sp
                                )
                                Text(
                                    text = strings.floatingBubbleCardSubtitle,
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }

                        Switch(
                            checked = floatingBubbleEnabled,
                            onCheckedChange = { checked ->
                                floatingBubbleEnabled = checked
                                viewModel.updateFloatingBubbleEnabled(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFE53935)
                            ),
                            modifier = Modifier.testTag("toggle_floating_bubble")
                        )
                    }

                    if (!hasOverlayPermission) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    try {
                                        val intent = android.content.Intent(
                                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.grantOverlayPermissionButton, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            val currSym = currencyInput.ifBlank { "₹" }

            // --------------------------------
            // LOCAL / METERED TARIFF
            // --------------------------------
            Text(
                text = strings.localTariffHeader,
                color = Color(0xFFE53935),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = if (strings.isTamil) 0.sp else 1.2.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column {
                        Text(
                            text = String.format(strings.baseFareSettingLabel, currSym),
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = baseFareInput,
                            onValueChange = { baseFareInput = it },
                            placeholder = { Text("100") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE53935),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_base_fare")
                        )
                    }

                    Column {
                        Text(
                            text = String.format(strings.distanceRateSettingLabel, currSym),
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = farePerKmInput,
                            onValueChange = { farePerKmInput = it },
                            placeholder = { Text("30") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE53935),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_fare_per_km")
                        )
                    }

                    Column {
                        Text(
                            text = String.format(strings.waitingRateSettingLabel, currSym),
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = waitFarePerMinInput,
                            onValueChange = { waitFarePerMinInput = it },
                            placeholder = { Text("2") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE53935),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_wait_fare_per_min")
                        )
                    }
                }
            }

            // --------------------------------
            // HOURLY RENTAL TARIFF
            // --------------------------------
            Text(
                text = strings.rentalTariffHeader,
                color = Color(0xFFD97706),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = if (strings.isTamil) 0.sp else 1.2.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column {
                        Text(
                            text = String.format(strings.rentalHourlyRateSettingLabel, currSym),
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = rentalHourlyRateInput,
                            onValueChange = { rentalHourlyRateInput = it },
                            placeholder = { Text("350") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD97706),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_rental_hourly_rate")
                        )
                    }

                    Column {
                        Text(
                            text = strings.rentalMinHoursSettingLabel,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = rentalMinHoursInput,
                            onValueChange = { rentalMinHoursInput = it },
                            placeholder = { Text("1") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD97706),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_rental_min_hours")
                        )
                    }

                    Column {
                        Text(
                            text = String.format(strings.rentalExtraHrSettingLabel, currSym),
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = rentalExtraHrRateInput,
                            onValueChange = { rentalExtraHrRateInput = it },
                            placeholder = { Text("350") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD97706),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_rental_extra_hr_rate")
                        )
                    }

                    Column {
                        Text(
                            text = String.format(strings.rentalExtraKmSettingLabel, currSym),
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = rentalExtraKmRateInput,
                            onValueChange = { rentalExtraKmRateInput = it },
                            placeholder = { Text("20") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD97706),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_rental_extra_km_rate")
                        )
                    }
                }
            }

            // --------------------------------
            // OUTSTATION TARIFF
            // --------------------------------
            Text(
                text = strings.outstationTariffHeader,
                color = Color(0xFF4F46E5),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = if (strings.isTamil) 0.sp else 1.2.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column {
                        Text(
                            text = String.format(strings.outstationRateSettingLabel, currSym),
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = outstationFarePerKmInput,
                            onValueChange = { outstationFarePerKmInput = it },
                            placeholder = { Text("30") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_outstation_fare_per_km")
                        )
                    }

                    Column {
                        Text(
                            text = String.format(strings.outstationBataSettingLabel, currSym),
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = outstationDriverBataInput,
                            onValueChange = { outstationDriverBataInput = it },
                            placeholder = { Text("300") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_outstation_driver_bata")
                        )
                    }

                    Column {
                        Text(
                            text = String.format(strings.outstationMinFareSettingLabel, currSym),
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = outstationMinFareInput,
                            onValueChange = { outstationMinFareInput = it },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_outstation_min_fare")
                        )
                    }

                    Column {
                        Text(
                            text = String.format(strings.outstationWaitRateSettingLabel, currSym),
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = outstationWaitRateInput,
                            onValueChange = { outstationWaitRateInput = it },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_outstation_wait_rate")
                        )
                    }

                    Column {
                        Text(
                            text = String.format(strings.outstationAddnChargesSettingLabel, currSym),
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = outstationAdditionalChargesInput,
                            onValueChange = { outstationAdditionalChargesInput = it },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_outstation_additional_charges")
                        )
                    }
                }
            }

            // --------------------------------
            // OTHER SETTINGS
            // --------------------------------
            Text(
                text = strings.otherSettingsHeader,
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = if (strings.isTamil) 0.sp else 1.5.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = strings.waitSpeedThresholdLabel,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = speedThresholdInput,
                            onValueChange = { speedThresholdInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE53935),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_speed_threshold")
                        )
                    }

                    Column {
                        Text(
                            text = strings.gpsCalibrationFactorLabel,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = strings.gpsCalibrationFactorDesc,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = distanceCalibrationFactorInput,
                            onValueChange = { distanceCalibrationFactorInput = it },
                            placeholder = { Text("1.000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE53935),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_calibration_factor")
                        )
                    }

                    Column {
                        Text(
                            text = strings.currencySymbolLabel,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("₹", "$", "€", "£", "AED", "SAR").forEach { symbol ->
                                FilterChip(
                                    selected = currencyInput == symbol,
                                    onClick = { currencyInput = symbol },
                                    label = { Text(symbol, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE53935),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        OutlinedTextField(
                            value = currencyInput,
                            onValueChange = { currencyInput = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE53935),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_currency")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.soundVoiceTitle,
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = strings.soundVoiceSubtitle,
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = audioEnabled,
                            onCheckedChange = { audioEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFE53935)
                            ),
                            modifier = Modifier.testTag("toggle_audio")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(strings.resetDialogTitle) },
            text = {
                Text(strings.resetDialogMessage)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToDefaultTariffs()
                        showResetDialog = false
                        showSavedMessage = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text(strings.refillStandardButton)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
