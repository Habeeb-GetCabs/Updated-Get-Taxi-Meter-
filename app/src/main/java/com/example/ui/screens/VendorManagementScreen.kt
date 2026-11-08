package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.VendorProfileEntity
import com.example.ui.components.CreatorSignatureBanner
import com.example.ui.components.LogoStyleCustomizerDialog
import com.example.ui.components.VendorLogoBadge
import com.example.util.VendorLogoRenderer
import com.example.util.appStrings
import com.example.viewmodel.MeterViewModel

private val BrandRed = Color(0xFF0F296B)
private val DarkNavy = Color(0xFF0F172A)
private val BgLight = Color(0xFFF8FAFC)
private val MutedSlate = Color(0xFF475569)
private val SubtleSlate = Color(0xFF64748B)
private val BorderSlate = Color(0xFFE2E8F0)
private val DividerSlate = Color(0xFFF1F5F9)
private val ActiveGreen = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorManagementScreen(
    viewModel: MeterViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val strings = appStrings()
    val allVendors by viewModel.allVendors.collectAsStateWithLifecycle()

    var editingVendor by remember { mutableStateOf<VendorProfileEntity?>(null) }
    var customizingLogoVendor by remember { mutableStateOf<VendorProfileEntity?>(null) }
    var showAddVendorDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = BrandRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.vendorManagementTitle,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("vendor_management_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = DarkNavy
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { showAddVendorDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("add_vendor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.addNewVendorButton,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = DarkNavy
                )
            )
        },
        containerColor = BgLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = BrandRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.vendorManagementSubtitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.vendorHistoricalProtectionNote,
                                fontSize = 11.sp,
                                color = MutedSlate
                            )
                        }
                    }
                }
            }

            items(allVendors, key = { it.vendorId }) { vendor ->
                val isGetTaxiDefault = VendorLogoRenderer.isGetTaxiVendor(vendor.vendorId, vendor.vendorName)
                VendorManagementItemCard(
                    vendor = vendor,
                    isGetTaxiDefault = isGetTaxiDefault,
                    onEditClick = { editingVendor = vendor },
                    onStyleLogoClick = { customizingLogoVendor = vendor },
                    onToggleActive = { active ->
                        viewModel.toggleVendorActiveStatus(vendor, active)
                    }
                )
            }

            item {
                CreatorSignatureBanner(
                    testTag = "vendor_management_creator_banner"
                )
            }
        }
    }

    if (editingVendor != null) {
        val currentVendor = editingVendor!!
        VendorEditDialog(
            title = strings.editVendorDetailsTitle,
            initialVendor = currentVendor,
            isNewVendor = false,
            onDismiss = { editingVendor = null },
            onSave = { updated ->
                viewModel.saveVendorProfile(updated) {
                    editingVendor = null
                    Toast.makeText(context, strings.vendorSavedToast, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showAddVendorDialog) {
        VendorEditDialog(
            title = strings.addNewVendorTitle,
            initialVendor = VendorProfileEntity(
                vendorId = "",
                vendorName = "",
                logoAsset = "",
                isActive = true,
                isOriginalGetTaxi = false
            ),
            isNewVendor = true,
            onDismiss = { showAddVendorDialog = false },
            onSave = { created ->
                viewModel.addCustomVendor(
                    businessName = created.vendorName,
                    contactNumber = created.contactNumber,
                    businessAddress = created.businessAddress,
                    email = created.email,
                    gstin = created.gstin,
                    invoiceDisplayDetails = created.invoiceDisplayDetails
                ) {
                    showAddVendorDialog = false
                    Toast.makeText(context, strings.vendorSavedToast, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    if (customizingLogoVendor != null) {
        val target = customizingLogoVendor!!
        LogoStyleCustomizerDialog(
            vendorId = target.vendorId,
            vendorName = target.vendorName,
            onDismiss = { customizingLogoVendor = null }
        )
    }
}

@Composable
private fun VendorManagementItemCard(
    vendor: VendorProfileEntity,
    isGetTaxiDefault: Boolean,
    onEditClick: () -> Unit,
    onStyleLogoClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    val strings = appStrings()

    // Collect only non-blank configured fields — never fabricate missing details
    val configuredLines = remember(vendor) {
        buildList {
            if (vendor.contactNumber.isNotBlank()) add("Phone: ${vendor.contactNumber}")
            if (vendor.businessAddress.isNotBlank()) add("Address: ${vendor.businessAddress}")
            if (vendor.email.isNotBlank()) add("Email: ${vendor.email}")
            if (vendor.gstin.isNotBlank()) add("GSTIN: ${vendor.gstin}")
            if (vendor.invoiceDisplayDetails.isNotBlank()) add("Invoice Note: ${vendor.invoiceDisplayDetails}")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vendor_card_${vendor.vendorId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = if (isGetTaxiDefault) 1.5.dp else 1.dp,
            color = if (isGetTaxiDefault) BrandRed.copy(alpha = 0.45f) else BorderSlate
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    VendorLogoBadge(
                        vendorId = vendor.vendorId,
                        vendorName = vendor.vendorName,
                        height = 42.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = vendor.vendorName,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = DarkNavy
                        )
                        Text(
                            text = "ID: ${vendor.vendorId}",
                            fontSize = 11.sp,
                            color = SubtleSlate
                        )
                        if (isGetTaxiDefault) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = strings.vendorPermanentDefaultBadge,
                                    color = BrandRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isGetTaxiDefault) {
                        Switch(
                            checked = vendor.isActive,
                            onCheckedChange = onToggleActive,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ActiveGreen
                            ),
                            modifier = Modifier.testTag("vendor_toggle_${vendor.vendorId}")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = strings.vendorActiveStatusLabel,
                            tint = ActiveGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = onStyleLogoClick,
                        modifier = Modifier.testTag("style_vendor_logo_${vendor.vendorId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Change Logo Color & Font",
                            tint = BrandRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("edit_vendor_${vendor.vendorId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = strings.edit,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.edit,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (configuredLines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DividerSlate)
                Spacer(modifier = Modifier.height(8.dp))
                configuredLines.forEach { detail ->
                    Text(
                        text = detail,
                        fontSize = 12.sp,
                        color = MutedSlate,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VendorEditDialog(
    title: String,
    initialVendor: VendorProfileEntity,
    isNewVendor: Boolean,
    onDismiss: () -> Unit,
    onSave: (VendorProfileEntity) -> Unit
) {
    val strings = appStrings()
    val isGetTaxiDefault = !isNewVendor &&
        VendorLogoRenderer.isGetTaxiVendor(initialVendor.vendorId, initialVendor.vendorName)

    var vendorName by remember { mutableStateOf(initialVendor.vendorName) }
    var contactNumber by remember { mutableStateOf(initialVendor.contactNumber) }
    var businessAddress by remember { mutableStateOf(initialVendor.businessAddress) }
    var email by remember { mutableStateOf(initialVendor.email) }
    var gstin by remember { mutableStateOf(initialVendor.gstin) }
    var invoiceNotes by remember { mutableStateOf(initialVendor.invoiceDisplayDetails) }
    var isActive by remember { mutableStateOf(initialVendor.isActive) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val previewName = vendorName.trim().ifBlank { "Vendor Taxi" }
    val previewId = if (isNewVendor) {
        VendorLogoRenderer.slugForVendorName(previewName)
    } else {
        initialVendor.vendorId
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = DarkNavy
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Logo Preview using the reusable GET TAXI badge template
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgLight, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VendorLogoBadge(
                        vendorId = previewId,
                        vendorName = previewName,
                        height = 50.dp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Logo Asset: ${VendorLogoRenderer.assetPathForVendor(previewId, previewName)}",
                        fontSize = 10.sp,
                        color = SubtleSlate
                    )
                }

                OutlinedTextField(
                    value = vendorName,
                    onValueChange = {
                        if (!isGetTaxiDefault) {
                            vendorName = it
                            errorText = null
                        }
                    },
                    enabled = !isGetTaxiDefault,
                    label = { Text(strings.vendorBusinessNameLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vendor_name_input")
                )

                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    label = { Text(strings.vendorContactNumberLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vendor_contact_input")
                )

                OutlinedTextField(
                    value = businessAddress,
                    onValueChange = { businessAddress = it },
                    label = { Text(strings.vendorBusinessAddressLabel) },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vendor_address_input")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(strings.vendorEmailLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vendor_email_input")
                )

                OutlinedTextField(
                    value = gstin,
                    onValueChange = { gstin = it },
                    label = { Text(strings.vendorGstinLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vendor_gstin_input")
                )

                OutlinedTextField(
                    value = invoiceNotes,
                    onValueChange = { invoiceNotes = it },
                    label = { Text(strings.vendorInvoiceNotesLabel) },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vendor_invoice_notes_input")
                )

                if (!isGetTaxiDefault && !isNewVendor) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.vendorActiveStatusLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it }
                        )
                    }
                }

                Text(
                    text = strings.vendorHistoricalProtectionNote,
                    fontSize = 11.sp,
                    color = SubtleSlate
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        fontSize = 12.sp,
                        color = BrandRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = vendorName.trim()
                    if (trimmedName.isBlank()) {
                        errorText = "Please enter a valid vendor name."
                        return@Button
                    }
                    val resolvedId = if (isNewVendor) {
                        VendorLogoRenderer.slugForVendorName(trimmedName)
                    } else {
                        initialVendor.vendorId
                    }
                    onSave(
                        initialVendor.copy(
                            vendorId = resolvedId,
                            vendorName = if (isGetTaxiDefault) VendorLogoRenderer.DEFAULT_VENDOR_NAME else trimmedName,
                            logoAsset = VendorLogoRenderer.assetPathForVendor(resolvedId, trimmedName),
                            contactNumber = contactNumber.trim(),
                            businessAddress = businessAddress.trim(),
                            email = email.trim(),
                            gstin = gstin.trim(),
                            invoiceDisplayDetails = invoiceNotes.trim(),
                            isActive = if (isGetTaxiDefault) true else isActive
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed, contentColor = Color.White),
                modifier = Modifier.testTag("save_vendor_dialog_button")
            ) {
                Text(strings.save, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = MutedSlate)
            }
        }
    )
}
