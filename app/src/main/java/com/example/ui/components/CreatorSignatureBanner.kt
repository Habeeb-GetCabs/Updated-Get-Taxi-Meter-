package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.util.VendorLogoRenderer

private val SignatureDisplayFont = FontFamily(
    Font(R.font.montserrat, FontWeight.Bold)
)

/**
 * Minimal, premium two-level developer attribution footer:
 * Level 1: "Designed & Developed by Basheer" in clean custom Montserrat typography.
 * Level 2: Styled GET TAXI logo badge directly underneath matching the active logo color & font.
 */
@Composable
fun CreatorSignatureBanner(
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 16.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Designed & Developed by Basheer",
            fontFamily = SignatureDisplayFont,
            color = Color(0xFF0F296B),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        VendorLogoBadge(
            vendorId = VendorLogoRenderer.GET_TAXI_VENDOR_ID,
            vendorName = VendorLogoRenderer.GET_TAXI_VENDOR_NAME,
            height = 30.dp,
            testTag = "${testTag}_logo"
        )
    }
}
