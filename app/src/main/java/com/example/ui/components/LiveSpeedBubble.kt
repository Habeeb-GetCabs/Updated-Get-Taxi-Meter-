package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Live Vehicle Speed Bubble
 *
 * Displays a small, elegant circular speed indicator showing current GPS vehicle speed.
 * - In-app only (no SYSTEM_ALERT_WINDOW).
 * - Visible only during active trip states (RUNNING or PAUSED).
 * - Purely a display component; continuous tracking is performed by LocationTrackingService.
 * - Stationary speed is filtered to 0 km/h.
 */
@Composable
fun LiveSpeedBubble(
    speedKmH: Float,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        // Safe speed formatting: filter negative/tiny speeds to 0
        val displaySpeed = if (speedKmH <= 0.5f) 0 else speedKmH.roundToInt()

        val isHighSpeed = displaySpeed >= 80
        val isMediumSpeed = displaySpeed in 40..79

        val bubbleBorderColor = when {
            isHighSpeed -> Color(0xFFEF4444)
            isMediumSpeed -> Color(0xFF3B82F6)
            displaySpeed > 0 -> Color(0xFF10B981)
            else -> Color(0xFFCBD5E1)
        }

        val speedNumberColor = when {
            isHighSpeed -> Color(0xFFDC2626)
            displaySpeed > 0 -> Color(0xFF0F172A)
            else -> Color(0xFF64748B)
        }

        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFF8FAFC)
                        )
                    )
                )
                .border(width = 2.dp, color = bubbleBorderColor, shape = CircleShape)
                .testTag("live_speed_bubble"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$displaySpeed",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = speedNumberColor,
                    lineHeight = 18.sp,
                    modifier = Modifier.testTag("live_speed_value")
                )
                Text(
                    text = "km/h",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.2.sp,
                    lineHeight = 9.sp
                )
            }
        }
    }
}
