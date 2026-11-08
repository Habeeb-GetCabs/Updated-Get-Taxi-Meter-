package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Log
import com.example.data.database.TripEntity
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object TripRouteMapGenerator {

    private const val TAG = "TripRouteMapGen"
    private const val BITMAP_WIDTH = 1046 // 2x density for crisp PDF rendering
    private const val BITMAP_HEIGHT = 280

    /**
     * Retrieves a cached route map Bitmap for the completed trip, or generates and saves one
     * using the actual GPS coordinates recorded during the trip.
     *
     * Returns null if insufficient GPS coordinates are available (< 2 points), allowing the caller
     * to safely display "Route map unavailable for this trip" without generating a misleading line.
     */
    fun getOrGenerateRouteMapBitmap(
        context: Context,
        trip: TripEntity
    ): Bitmap? {
        val cacheDir = File(context.filesDir, "trip_route_maps").apply { if (!exists()) mkdirs() }
        val cachedFile = File(cacheDir, "trip_${trip.id}_route_map.png")

        if (cachedFile.exists() && cachedFile.length() > 500) {
            try {
                val cachedBitmap = BitmapFactory.decodeFile(cachedFile.absolutePath)
                if (cachedBitmap != null) {
                    return cachedBitmap
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read cached route map file, regenerating", e)
            }
        }

        val points = RouteCoordinatesSerializer.deserialize(trip.routePointsJson)
        if (points.size < 2) {
            Log.d(TAG, "Trip ${trip.id} has insufficient GPS points (${points.size}). Route map unavailable.")
            return null
        }

        return try {
            val bitmap = renderActualTravelledRouteBitmap(points, trip)
            // Persist compressed image to disk
            FileOutputStream(cachedFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error generating route map bitmap for trip ${trip.id}", e)
            null
        }
    }

    private fun renderActualTravelledRouteBitmap(
        points: List<Pair<Double, Double>>,
        trip: TripEntity
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // 1. MAP BACKGROUND & GEOGRAPHIC GRID (Clean modern road-map aesthetic)
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, BITMAP_HEIGHT.toFloat(),
                Color.rgb(248, 250, 252), // #F8FAFC
                Color.rgb(241, 245, 249), // #F1F5F9
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, BITMAP_WIDTH.toFloat(), BITMAP_HEIGHT.toFloat(), bgPaint)

        // Draw soft street/road grid lines
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240) // #E2E8F0
            strokeWidth = 2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
        }
        val gridStep = 70f
        var gx = gridStep
        while (gx < BITMAP_WIDTH) {
            canvas.drawLine(gx, 0f, gx, BITMAP_HEIGHT.toFloat(), gridPaint)
            gx += gridStep
        }
        var gy = gridStep
        while (gy < BITMAP_HEIGHT) {
            canvas.drawLine(0f, gy, BITMAP_WIDTH.toFloat(), gy, gridPaint)
            gy += gridStep
        }

        // 2. BOUNDING BOX & COORDINATE NORMALIZATION
        val lats = points.map { it.first }
        val lons = points.map { it.second }
        val minLat = lats.minOrNull() ?: 0.0
        val maxLat = lats.maxOrNull() ?: 0.0
        val minLon = lons.minOrNull() ?: 0.0
        val maxLon = lons.maxOrNull() ?: 0.0

        val latSpan = (maxLat - minLat).coerceAtLeast(0.00015)
        val lonSpan = (maxLon - minLon).coerceAtLeast(0.00015)

        // Padding around route to fit markers cleanly
        val padX = 90f
        val padY = 56f
        val usableW = BITMAP_WIDTH - (padX * 2)
        val usableH = BITMAP_HEIGHT - (padY * 2)

        fun projectPoint(lat: Double, lon: Double): Pair<Float, Float> {
            val normX = ((lon - minLon) / lonSpan).toFloat().coerceIn(0f, 1f)
            val normY = (1.0f - ((lat - minLat) / latSpan).toFloat()).coerceIn(0f, 1f)
            val px = padX + (normX * usableW)
            val py = padY + (normY * usableH)
            return Pair(px, py)
        }

        // 3. DRAW ACTUAL RECORDED GPS ROUTE POLYLINE
        val routePath = Path()
        val firstProj = projectPoint(points.first().first, points.first().second)
        routePath.moveTo(firstProj.first, firstProj.second)
        for (i in 1 until points.size) {
            val pt = projectPoint(points[i].first, points[i].second)
            routePath.lineTo(pt.first, pt.second)
        }

        // Outer white casing for sharp contrast against map lines
        val casingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 14f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(routePath, casingPaint)

        // Route shadow/glow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 2, 132, 199)
            strokeWidth = 18f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(routePath, shadowPaint)

        // Main Route line: Bold vibrant Crimson-Red to Deep-Royal-Blue
        val routeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(202, 31, 47) // #CA1F2F Brand Red
            strokeWidth = 8f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(routePath, routeLinePaint)

        // 4. PICKUP MARKER A (START) - GREEN PIN
        val startProj = projectPoint(points.first().first, points.first().second)
        drawPinMarker(
            canvas = canvas,
            centerX = startProj.first,
            centerY = startProj.second,
            label = "A",
            subLabel = "START",
            fillColor = Color.rgb(22, 163, 74) // Green
        )

        // 5. DROP-OFF MARKER B (END) - RED PIN
        val endProj = projectPoint(points.last().first, points.last().second)
        drawPinMarker(
            canvas = canvas,
            centerX = endProj.first,
            centerY = endProj.second,
            label = "B",
            subLabel = "END",
            fillColor = Color.rgb(220, 38, 38) // Red
        )

        // 6. MAP WATERMARK & TELEMETRY BADGE
        val badgeRect = RectF(16f, 14f, 380f, 44f)
        val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 15, 23, 42) // Slate 900
        }
        canvas.drawRoundRect(badgeRect, 10f, 10f, badgeBg)

        textPaint.color = Color.rgb(248, 184, 20) // Yellow accent
        textPaint.textSize = 15f
        canvas.drawText("ACTUAL TRAVELLED ROUTE", 28f, 34f, textPaint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 13f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("• ${String.format(Locale.US, "%.2f", trip.distanceKm)} km GPS Tracked", 226f, 34f, textPaint)

        // North direction compass arrow at top right
        val northBadgeRect = RectF(BITMAP_WIDTH - 64f, 14f, BITMAP_WIDTH - 16f, 44f)
        canvas.drawRoundRect(northBadgeRect, 10f, 10f, badgeBg)
        textPaint.color = Color.WHITE
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("N ↑", BITMAP_WIDTH - 52f, 34f, textPaint)

        // 7. OUTER BORDER
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        canvas.drawRect(1f, 1f, BITMAP_WIDTH - 1f, BITMAP_HEIGHT - 1f, borderPaint)

        return bitmap
    }

    private fun drawPinMarker(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        label: String,
        subLabel: String,
        fillColor: Int
    ) {
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Marker drop shadow
        pinPaint.color = Color.argb(50, 0, 0, 0)
        canvas.drawCircle(centerX, centerY + 3f, 16f, pinPaint)

        // Outer White ring
        pinPaint.color = Color.WHITE
        canvas.drawCircle(centerX, centerY, 15f, pinPaint)

        // Main colored circle
        pinPaint.color = fillColor
        canvas.drawCircle(centerX, centerY, 12f, pinPaint)

        // Pin letter ("A" or "B")
        textPaint.textSize = 14f
        canvas.drawText(label, centerX, centerY + 5f, textPaint)

        // Tag label ("START" or "END") above/below pin
        val tagY = if (centerY < 60f) centerY + 28f else centerY - 18f
        val tagBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 15, 23, 42)
        }
        val tagRect = RectF(centerX - 24f, tagY - 12f, centerX + 24f, tagY + 4f)
        canvas.drawRoundRect(tagRect, 6f, 6f, tagBg)

        textPaint.textSize = 10f
        canvas.drawText(subLabel, centerX, tagY, textPaint)
    }
}
