package com.example.ui.components

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.TripState
import com.example.util.SearchPlaceResult
import org.osmdroid.config.Configuration
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun OlaMapView(
    tripState: TripState,
    orientationMode: MapOrientationMode,
    destination: SearchPlaceResult? = null,
    suggestedRoute: List<Pair<Double, Double>> = emptyList(),
    isFollowingVehicle: Boolean = true,
    recenterTrigger: Long = 0L,
    zoomInTrigger: Long = 0L,
    zoomOutTrigger: Long = 0L,
    onUserPan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }

    val lat = tripState.latitude ?: 11.0168
    val lon = tripState.longitude ?: 76.9558
    val bearing = tripState.bearing

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var driverMarkerRef by remember { mutableStateOf<Marker?>(null) }
    var destinationMarkerRef by remember { mutableStateOf<Marker?>(null) }
    var actualPolylineRef by remember { mutableStateOf<Polyline?>(null) }
    var suggestedPolylineRef by remember { mutableStateOf<Polyline?>(null) }

    val vehicleIcon = remember(context) { createVehicleIcon(context) }
    val destinationIcon = remember(context) { createDestinationIcon(context) }

    // Live driver position, orientation & camera tracking
    LaunchedEffect(lat, lon, bearing, isFollowingVehicle, mapViewRef) {
        val mv = mapViewRef ?: return@LaunchedEffect
        val curPoint = GeoPoint(lat, lon)

        driverMarkerRef?.let { marker ->
            marker.position = curPoint
            marker.rotation = 360f - bearing // osmdroid rotation angle
            mv.invalidate()
        }

        if (isFollowingVehicle) {
            mv.controller.animateTo(curPoint)
        }
    }

    // Actual driven GPS polyline
    LaunchedEffect(tripState.routePoints, mapViewRef) {
        val mv = mapViewRef ?: return@LaunchedEffect
        val poly = actualPolylineRef ?: return@LaunchedEffect
        val geoPoints = tripState.routePoints.map { GeoPoint(it.first, it.second) }
        poly.setPoints(geoPoints)
        mv.invalidate()
    }

    // Suggested guidance polyline
    LaunchedEffect(suggestedRoute, mapViewRef) {
        val mv = mapViewRef ?: return@LaunchedEffect
        val poly = suggestedPolylineRef ?: return@LaunchedEffect
        val geoPoints = suggestedRoute.map { GeoPoint(it.first, it.second) }
        poly.setPoints(geoPoints)
        mv.invalidate()
    }

    // Destination pin marker
    LaunchedEffect(destination, mapViewRef) {
        val mv = mapViewRef ?: return@LaunchedEffect
        val destMarker = destinationMarkerRef ?: return@LaunchedEffect
        if (destination != null) {
            destMarker.position = GeoPoint(destination.latitude, destination.longitude)
            destMarker.title = destination.name
            destMarker.snippet = destination.description
            destMarker.setVisible(true)
        } else {
            destMarker.setVisible(false)
        }
        mv.invalidate()
    }

    // Camera re-center trigger
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0L) {
            mapViewRef?.let { mv ->
                mv.controller.animateTo(GeoPoint(lat, lon))
            }
        }
    }

    // Zoom triggers
    LaunchedEffect(zoomInTrigger) {
        if (zoomInTrigger > 0L) {
            mapViewRef?.controller?.zoomIn()
        }
    }

    LaunchedEffect(zoomOutTrigger) {
        if (zoomOutTrigger > 0L) {
            mapViewRef?.controller?.zoomOut()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true

                    controller.setZoom(17.0)
                    controller.setCenter(GeoPoint(lat, lon))

                    // Suggested Route Polyline
                    val suggestedPoly = Polyline(this).apply {
                        outlinePaint.color = Color.parseColor("#1A73E8")
                        outlinePaint.strokeWidth = 14f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                    }
                    overlays.add(suggestedPoly)
                    suggestedPolylineRef = suggestedPoly

                    // Actual Driven Route Polyline
                    val actualPoly = Polyline(this).apply {
                        outlinePaint.color = Color.parseColor("#DC2626")
                        outlinePaint.strokeWidth = 12f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                    }
                    overlays.add(actualPoly)
                    actualPolylineRef = actualPoly

                    // Destination Marker
                    val destMarker = Marker(this).apply {
                        icon = destinationIcon
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setVisible(false)
                    }
                    overlays.add(destMarker)
                    destinationMarkerRef = destMarker

                    // Driver Vehicle Marker
                    val driverMarker = Marker(this).apply {
                        icon = vehicleIcon
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        position = GeoPoint(lat, lon)
                        rotation = 360f - bearing
                        title = "Vehicle Location"
                    }
                    overlays.add(driverMarker)
                    driverMarkerRef = driverMarker

                    // Detect user manual panning gesture
                    addMapListener(object : org.osmdroid.events.MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            if (event?.source?.isAnimating == false) {
                                onUserPan()
                            }
                            return false
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            return false
                        }
                    })

                    mapViewRef = this
                }
            },
            update = { mv ->
                mapViewRef = mv
            },
            onRelease = { mv ->
                mv.onDetach()
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("osmdroid_native_mapview")
        )
    }
}

private fun createVehicleIcon(context: Context): Drawable {
    val size = (36 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DC2626")
        style = Paint.Style.FILL
    }
    val path = Path().apply {
        moveTo(size / 2f, 2f)
        lineTo(size * 0.85f, size * 0.9f)
        lineTo(size / 2f, size * 0.75f)
        lineTo(size * 0.15f, size * 0.9f)
        close()
    }
    canvas.drawPath(path, paint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawPath(path, strokePaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createDestinationIcon(context: Context): Drawable {
    val size = (36 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paintCircle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1557B0")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 3f, size / 3.2f, paintCircle)

    val paintTriangle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1557B0")
        style = Paint.Style.FILL
    }
    val path = Path().apply {
        moveTo(size * 0.22f, size / 3f)
        lineTo(size * 0.78f, size / 3f)
        lineTo(size / 2f, size * 0.92f)
        close()
    }
    canvas.drawPath(path, paintTriangle)

    val paintDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 3f, size / 7f, paintDot)

    return BitmapDrawable(context.resources, bitmap)
}
