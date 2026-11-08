package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.VendorLogoRenderer

/**
 * Renders the brand logo for any vendor (including GET TAXI and all 20 built-in vendors)
 * using the active LogoColorTheme and custom bundled Google Font (LogoFontStyle).
 */
@Composable
fun VendorLogoBadge(
    vendorId: String?,
    vendorName: String,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    width: Dp = (height.value * 2.65f).dp,
    overrideColorTheme: VendorLogoRenderer.LogoColorTheme? = null,
    overrideFontStyle: VendorLogoRenderer.LogoFontStyle? = null,
    testTag: String? = null
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        VendorLogoRenderer.ensureInitialized(context)
    }
    val styleState by VendorLogoRenderer.logoStyleState.collectAsStateWithLifecycle()

    val sizedModifier = modifier
        .width(width)
        .height(height)
    val boxModifier = if (testTag != null) sizedModifier.testTag(testTag) else sizedModifier

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Read styleState.revision to trigger recomposition/redraw whenever color or font changes
            val rev = styleState.revision
            val w = size.width
            val h = size.height
            if (w > 0f && h > 0f && rev >= 0) {
                VendorLogoRenderer.drawTemplatedVendorLogoOnCanvas(
                    canvas = drawContext.canvas.nativeCanvas,
                    vendorName = vendorName,
                    width = w,
                    height = h,
                    context = context,
                    vendorId = vendorId,
                    overrideColorTheme = overrideColorTheme,
                    overrideFontStyle = overrideFontStyle
                )
            }
        }
    }
}

/**
 * Interactive Dialog allowing the user to customize the Logo Color Theme and Font Style
 * with a live real-time preview.
 */
@Composable
fun LogoStyleCustomizerDialog(
    vendorId: String,
    vendorName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        VendorLogoRenderer.ensureInitialized(context)
    }
    val styleState by VendorLogoRenderer.logoStyleState.collectAsStateWithLifecycle()

    var selectedColorTheme by remember(vendorId) {
        mutableStateOf(VendorLogoRenderer.resolveColorThemeForVendor(vendorId, vendorName))
    }
    var selectedFontStyle by remember(vendorId) {
        mutableStateOf(VendorLogoRenderer.resolveFontStyleForVendor(vendorId, vendorName))
    }
    var applyToAllLogos by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = Color(selectedColorTheme.bgStartInt),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Logo Color & Font",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live Logo Preview Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LIVE LOGO PREVIEW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    VendorLogoBadge(
                        vendorId = vendorId,
                        vendorName = vendorName,
                        height = 58.dp,
                        overrideColorTheme = selectedColorTheme,
                        overrideFontStyle = selectedFontStyle,
                        testTag = "customizer_live_preview_badge"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${selectedColorTheme.displayName} • ${selectedFontStyle.displayName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                }

                // Color Theme Selector
                Text(
                    text = "SELECT LOGO COLOR THEME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF475569),
                    letterSpacing = 0.8.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(VendorLogoRenderer.LogoColorTheme.entries) { theme ->
                        val isSelected = theme == selectedColorTheme
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(84.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0xFFF1F5F9) else Color.Transparent)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(theme.bgStartInt) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedColorTheme = theme }
                                .padding(8.dp)
                                .testTag("logo_color_option_${theme.id}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(theme.bgStartInt),
                                                Color(theme.bgEndInt)
                                            )
                                        )
                                    )
                                    .border(2.dp, Color(theme.accentTextInt), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(theme.accentTextInt),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = theme.displayName.substringBefore(" &"),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = Color(0xFF0F172A),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Font Family Selector
                Text(
                    text = "SELECT LOGO FONT STYLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF475569),
                    letterSpacing = 0.8.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VendorLogoRenderer.LogoFontStyle.entries.forEach { fontOption ->
                        val isSelected = fontOption == selectedFontStyle
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(selectedColorTheme.bgStartInt) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFontStyle = fontOption }
                                .testTag("logo_font_option_${fontOption.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = vendorName,
                                        fontFamily = FontFamily(Font(fontOption.fontResId)),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = fontOption.displayName,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(selectedColorTheme.bgStartInt),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Scope toggle: Apply to all vs only current vendor
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Apply to all brand logos",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (applyToAllLogos) {
                                "Updates main header & vendor logos"
                            } else {
                                "Only updates $vendorName"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Switch(
                        checked = applyToAllLogos,
                        onCheckedChange = { applyToAllLogos = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (applyToAllLogos) {
                        VendorLogoRenderer.updateGlobalLogoStyle(
                            context = context,
                            colorTheme = selectedColorTheme,
                            fontStyle = selectedFontStyle,
                            useDistinctVendorColors = styleState.useDistinctVendorColors,
                            clearVendorOverrides = false
                        )
                        VendorLogoRenderer.setVendorLogoStyle(
                            context = context,
                            vendorId = vendorId,
                            colorTheme = selectedColorTheme,
                            fontStyle = selectedFontStyle
                        )
                    } else {
                        VendorLogoRenderer.setVendorLogoStyle(
                            context = context,
                            vendorId = vendorId,
                            colorTheme = selectedColorTheme,
                            fontStyle = selectedFontStyle
                        )
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(selectedColorTheme.bgStartInt),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("apply_logo_style_button")
            ) {
                Text("Apply Color & Font", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )
}
