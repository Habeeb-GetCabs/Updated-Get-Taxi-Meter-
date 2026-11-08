package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedometerGauge(
    currentSpeedKmH: Double,
    modifier: Modifier = Modifier,
    maxSpeed: Double = 120.0
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeedKmH.coerceIn(0.0, maxSpeed).toFloat(),
        animationSpec = tween(durationMillis = 300),
        label = "SpeedometerAnimation"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Dark Slate-900 container
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Highway Road Background Image for Speedometer
            Image(
                painter = painterResource(id = R.drawable.img_highway_background),
                contentDescription = "Highway Speedometer Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
            )

            // Semi-transparent dark scrim overlay for visual clarity
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xCC0F172A))
                    .clip(RoundedCornerShape(24.dp))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
            // Speed gauge drawing
            Box(
                modifier = Modifier
                    .size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                    val radius = (canvasWidth / 2f) - 12f

                    // Background Track Arc (135 degrees to 405 degrees -> total 270 deg)
                    drawArc(
                        color = Color(0xFF1E293B),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )

                    // Active Speed Arc with Gradient
                    val sweepAngle = (animatedSpeed / maxSpeed.toFloat()) * 270f
                    val gradient = Brush.sweepGradient(
                        0.0f to Color(0xFF22C55E),  // Green
                        0.5f to Color(0xFFF59E0B),  // Amber
                        1.0f to Color(0xFFEF4444)   // Red
                    )

                    drawArc(
                        brush = gradient,
                        startAngle = 135f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 14f, cap = StrokeCap.Round)
                    )

                    // Speedometer Needle Pointer
                    val needleAngleRad = Math.toRadians((135 + sweepAngle).toDouble())
                    val needleLength = radius - 10f
                    val needleEnd = Offset(
                        (center.x + needleLength * cos(needleAngleRad)).toFloat(),
                        (center.y + needleLength * sin(needleAngleRad)).toFloat()
                    )

                    drawLine(
                        color = Color(0xFFE53935),
                        start = center,
                        end = needleEnd,
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )

                    // Needle Pivot Pin
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = center
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Digital speed readout
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "LIVE SPEED",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.0f", currentSpeedKmH),
                        color = if (currentSpeedKmH > 80.0) Color(0xFFEF4444) else Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "KM/H",
                        color = Color(0xFF38BDF8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (currentSpeedKmH > 80.0) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "HIGH SPEED ALERT",
                            color = Color(0xFFEF4444),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Real-time GPS Speed",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
}
