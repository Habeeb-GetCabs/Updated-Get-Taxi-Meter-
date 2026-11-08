package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlayfairDisplayFontFamily = FontFamily(
  Font(R.font.playfair_display, FontWeight.Normal),
  Font(R.font.playfair_display, FontWeight.Bold),
  Font(R.font.playfair_display, FontWeight.Black)
)

val CinzelFontFamily = FontFamily(
  Font(R.font.cinzel, FontWeight.Normal),
  Font(R.font.cinzel, FontWeight.Bold),
  Font(R.font.cinzel, FontWeight.Black)
)

val GreatVibesFontFamily = FontFamily(
  Font(R.font.great_vibes, FontWeight.Normal)
)

// Set of Material typography styles to start with
val Typography =
  Typography(
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      )
  )

