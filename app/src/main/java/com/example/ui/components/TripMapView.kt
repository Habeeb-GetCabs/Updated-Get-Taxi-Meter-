package com.example.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.example.data.model.TripState
import com.example.util.DestinationSearchService
import com.example.util.SearchPlaceResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MapTypeMode {
    OSM_FREE_STREET,
    OSM_FREE_DARK,
    VECTOR_RADAR,
    GOOGLE_MAPS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripMapView(
    tripState: TripState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Check if MAPS_API_KEY is configured with a real key
    val mapsApiKey = try { BuildConfig.MAPS_API_KEY } catch (e: Exception) { "" }
    val isGoogleKeyConfigured = mapsApiKey.isNotBlank() &&
            !mapsApiKey.contains("DEFAULT_MAPS_KEY", ignoreCase = true) &&
            !mapsApiKey.contains("MY_GEMINI_API_KEY", ignoreCase = true) &&
            !mapsApiKey.contains("AIzaSyB7j5s8T369OKe4H69e3jhkGfM2sJhniCo", ignoreCase = true)

    // Default map mode is OpenStreetMap
    var selectedMode by remember { mutableStateOf(MapTypeMode.OSM_FREE_STREET) }

    // Destination search & suggested guidance state (solely for driver visual guidance; never used in fare engine)
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchPlaceResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedDestination by remember { mutableStateOf<SearchPlaceResult?>(null) }
    var suggestedRoute by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var recenterTrigger by remember { mutableLongStateOf(0L) }

    // LatLng for Google Maps if used
    val currentLatLng = if (tripState.latitude != null && tripState.longitude != null) {
        LatLng(tripState.latitude, tripState.longitude)
    } else {
        LatLng(11.0168, 76.9558)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLatLng, 16.5f)
    }

    LaunchedEffect(currentLatLng) {
        if (tripState.latitude != null && tripState.longitude != null && selectedMode == MapTypeMode.GOOGLE_MAPS) {
            try {
                cameraPositionState.animate(CameraUpdateFactory.newLatLng(currentLatLng))
            } catch (_: Exception) {}
        }
    }

    // Trigger suggested route calculation whenever a destination is selected or driver moves significantly
    LaunchedEffect(selectedDestination, tripState.latitude, tripState.longitude) {
        val dest = selectedDestination
        val curLat = tripState.latitude
        val curLon = tripState.longitude
        if (dest != null && curLat != null && curLon != null) {
            val route = DestinationSearchService.fetchSuggestedDrivingRoute(
                startLat = curLat,
                startLon = curLon,
                destLat = dest.latitude,
                destLon = dest.longitude
            )
            suggestedRoute = route
        } else if (dest == null) {
            suggestedRoute = emptyList()
        }
    }

    val latLngRoute = remember(tripState.routePoints) {
        tripState.routePoints.map { LatLng(it.first, it.second) }
    }
    val latLngSuggested = remember(suggestedRoute) {
        suggestedRoute.map { LatLng(it.first, it.second) }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedMode) {
                MapTypeMode.OSM_FREE_STREET -> {
                    OpenStreetMapTileView(
                        tripState = tripState,
                        useDarkStyle = false,
                        destination = selectedDestination,
                        suggestedRoute = suggestedRoute,
                        recenterTrigger = recenterTrigger,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MapTypeMode.OSM_FREE_DARK -> {
                    OpenStreetMapTileView(
                        tripState = tripState,
                        useDarkStyle = true,
                        destination = selectedDestination,
                        suggestedRoute = suggestedRoute,
                        recenterTrigger = recenterTrigger,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MapTypeMode.VECTOR_RADAR -> {
                    GpsVectorRouteCanvas(
                        tripState = tripState,
                        destination = selectedDestination,
                        suggestedRoute = suggestedRoute,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MapTypeMode.GOOGLE_MAPS -> {
                    if (isGoogleKeyConfigured) {
                        GoogleMap(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("google_trip_map"),
                            cameraPositionState = cameraPositionState
                        ) {
                            // Actual GPS Driven Route (Crimson Red)
                            if (latLngRoute.size >= 2) {
                                Polyline(
                                    points = latLngRoute,
                                    color = Color(0xFFDC2626),
                                    width = 12f
                                )
                            }
                            // Suggested Driving Guidance Route (Electric Blue)
                            if (latLngSuggested.size >= 2) {
                                Polyline(
                                    points = latLngSuggested,
                                    color = Color(0xFF0284C7),
                                    width = 8f
                                )
                            }
                            // Live Vehicle Marker
                            if (tripState.latitude != null && tripState.longitude != null) {
                                Marker(
                                    state = rememberMarkerState(position = currentLatLng),
                                    title = "Vehicle Location",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                                )
                            }
                            // Destination Marker
                            selectedDestination?.let { dest ->
                                Marker(
                                    state = rememberMarkerState(position = LatLng(dest.latitude, dest.longitude)),
                                    title = dest.name,
                                    snippet = dest.description,
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                                )
                            }
                        }
                    } else {
                        OpenStreetMapTileView(
                            tripState = tripState,
                            useDarkStyle = false,
                            destination = selectedDestination,
                            suggestedRoute = suggestedRoute,
                            recenterTrigger = recenterTrigger,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // DESTINATION SEARCH BAR AT TOP OF MAP
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Destination",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { newQuery ->
                                searchQuery = newQuery
                                searchJob?.cancel()
                                if (newQuery.trim().length >= 2) {
                                    isSearching = true
                                    searchJob = coroutineScope.launch {
                                        delay(350) // debounce
                                        val results = DestinationSearchService.searchPlaces(
                                            query = newQuery,
                                            userLat = tripState.latitude,
                                            userLon = tripState.longitude
                                        )
                                        searchResults = results
                                        isSearching = false
                                    }
                                } else {
                                    searchResults = emptyList()
                                    isSearching = false
                                }
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color(0xFF0F172A),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("destination_search_input"),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search destination (streets, areas, landmarks)...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (isSearching) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Color(0xFFE53935),
                                modifier = Modifier.size(16.dp)
                            )
                        } else if (searchQuery.isNotEmpty() || selectedDestination != null) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    searchResults = emptyList()
                                    selectedDestination = null
                                    suggestedRoute = emptyList()
                                },
                                modifier = Modifier.size(28.dp).testTag("clear_destination_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Autocomplete Suggestions Dropdown Box
                AnimatedVisibility(
                    visible = searchResults.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .heightIn(max = 210.dp)
                            .testTag("destination_search_results_card")
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(searchResults) { place ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedDestination = place
                                            searchQuery = place.name
                                            searchResults = emptyList()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = place.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = place.description,
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }

                // Selected Destination Guidance Banner
                selectedDestination?.let { dest ->
                    Surface(
                        color = Color(0xFF0284C7),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Guidance Route: ${dest.name}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    selectedDestination = null
                                    suggestedRoute = emptyList()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Guidance",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // BOTTOM CONTROLS ROW (Re-center to Vehicle + Map Style Switcher)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Re-center on vehicle button (48dp touch target)
                FloatingActionButton(
                    onClick = {
                        recenterTrigger = System.currentTimeMillis()
                        if (tripState.latitude != null && tripState.longitude != null && selectedMode == MapTypeMode.GOOGLE_MAPS) {
                            coroutineScope.launch {
                                try {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                                } catch (_: Exception) {}
                            }
                        }
                    },
                    containerColor = Color.White,
                    contentColor = Color(0xFFE53935),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("recenter_vehicle_location_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Return to Live Vehicle Location",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Map Style Switcher
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 3.dp,
                    modifier = Modifier.clickable {
                        selectedMode = when (selectedMode) {
                            MapTypeMode.OSM_FREE_STREET -> MapTypeMode.OSM_FREE_DARK
                            MapTypeMode.OSM_FREE_DARK -> MapTypeMode.VECTOR_RADAR
                            MapTypeMode.VECTOR_RADAR -> if (isGoogleKeyConfigured) MapTypeMode.GOOGLE_MAPS else MapTypeMode.OSM_FREE_STREET
                            MapTypeMode.GOOGLE_MAPS -> MapTypeMode.OSM_FREE_STREET
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Switch Map Style",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val strings = com.example.util.appStrings()
                        Text(
                            text = when (selectedMode) {
                                MapTypeMode.OSM_FREE_STREET -> strings.mapStreetMode
                                MapTypeMode.OSM_FREE_DARK -> strings.mapDarkMode
                                MapTypeMode.VECTOR_RADAR -> strings.mapVectorRadar
                                MapTypeMode.GOOGLE_MAPS -> strings.mapGoogleView
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenStreetMapTileView(
    tripState: TripState,
    useDarkStyle: Boolean,
    destination: SearchPlaceResult? = null,
    suggestedRoute: List<Pair<Double, Double>> = emptyList(),
    recenterTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lat = tripState.latitude ?: 11.0168
    val lon = tripState.longitude ?: 76.9558
    val routePoints = tripState.routePoints

    LaunchedEffect(Unit) {
        org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName
    }

    AndroidView(
        factory = { ctx ->
            org.osmdroid.config.Configuration.getInstance().userAgentValue = ctx.packageName
            org.osmdroid.views.MapView(ctx).apply {
                setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isTilesScaledToDpi = true
                controller.setZoom(17.0)
                controller.setCenter(org.osmdroid.util.GeoPoint(lat, lon))
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            if (useDarkStyle) {
                mapView.overlayManager.tilesOverlay.setColorFilter(
                    android.graphics.ColorMatrixColorFilter(
                        floatArrayOf(
                            -0.8f, 0f, 0f, 0f, 240f,
                            0f, -0.8f, 0f, 0f, 240f,
                            0f, 0f, -0.8f, 0f, 240f,
                            0f, 0f, 0f, 1.0f, 0f
                        )
                    )
                )
            } else {
                mapView.overlayManager.tilesOverlay.setColorFilter(null)
            }

            val geoPoint = org.osmdroid.util.GeoPoint(lat, lon)

            // Re-center if requested or on coordinate update
            if (recenterTrigger > 0L) {
                mapView.controller.animateTo(geoPoint)
                mapView.controller.setZoom(17.5)
            } else {
                mapView.controller.animateTo(geoPoint)
            }

            // 1. Draw suggested driving route polyline in distinct Cyan-Blue (#0284C7)
            if (suggestedRoute.size >= 2) {
                val suggestedPolyline = org.osmdroid.views.overlay.Polyline(mapView).apply {
                    outlinePaint.color = android.graphics.Color.parseColor("#0284C7")
                    outlinePaint.strokeWidth = 10f
                    setPoints(suggestedRoute.map { org.osmdroid.util.GeoPoint(it.first, it.second) })
                }
                mapView.overlays.add(suggestedPolyline)
            }

            // 2. Draw actual GPS recorded route polyline in Crimson Red (#DC2626)
            if (routePoints.size >= 2) {
                val polyline = org.osmdroid.views.overlay.Polyline(mapView).apply {
                    outlinePaint.color = android.graphics.Color.parseColor("#DC2626")
                    outlinePaint.strokeWidth = 14f
                    setPoints(routePoints.map { org.osmdroid.util.GeoPoint(it.first, it.second) })
                }
                mapView.overlays.add(polyline)

                // Start marker
                val startPoint = org.osmdroid.util.GeoPoint(routePoints.first().first, routePoints.first().second)
                val startMarker = org.osmdroid.views.overlay.Marker(mapView).apply {
                    position = startPoint
                    setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_CENTER)
                    icon = androidx.core.content.ContextCompat.getDrawable(mapView.context, com.example.R.drawable.ic_start_flag)
                    title = "Trip Start"
                }
                mapView.overlays.add(startMarker)
            }

            // 3. Destination Marker if selected
            destination?.let { dest ->
                val destPoint = org.osmdroid.util.GeoPoint(dest.latitude, dest.longitude)
                val destMarker = org.osmdroid.views.overlay.Marker(mapView).apply {
                    position = destPoint
                    setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                    icon = androidx.core.content.ContextCompat.getDrawable(mapView.context, com.example.R.drawable.ic_start_flag)
                    title = dest.name
                    snippet = dest.description
                }
                mapView.overlays.add(destMarker)
            }

            // 4. Live Taxi Car marker at current GPS location
            val taxiMarker = org.osmdroid.views.overlay.Marker(mapView).apply {
                position = geoPoint
                setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_CENTER)
                icon = androidx.core.content.ContextCompat.getDrawable(mapView.context, com.example.R.drawable.ic_taxi_marker)
                title = "Taxi Location"
            }
            mapView.overlays.add(taxiMarker)

            mapView.invalidate()
        },
        modifier = modifier
    )
}

@Composable
private fun GpsVectorRouteCanvas(
    tripState: TripState,
    destination: SearchPlaceResult? = null,
    suggestedRoute: List<Pair<Double, Double>> = emptyList(),
    modifier: Modifier = Modifier
) {
    val route = tripState.routePoints

    Canvas(modifier = modifier.background(Color(0xFF0F172A))) {
        val width = size.width
        val height = size.height

        val gridStep = 40.dp.toPx()
        var x = 0f
        while (x < width) {
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
            x += gridStep
        }
        var y = 0f
        while (y < height) {
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            y += gridStep
        }

        if (route.isEmpty()) {
            val centerX = width / 2f
            val centerY = height / 2f
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                radius = 36.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color(0xFF38BDF8),
                radius = 8.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            return@Canvas
        }

        val allLats = (route.map { it.first } + (destination?.latitude?.let { listOf(it) } ?: emptyList()))
        val allLons = (route.map { it.second } + (destination?.longitude?.let { listOf(it) } ?: emptyList()))
        val minLat = allLats.minOrNull() ?: 0.0
        val maxLat = allLats.maxOrNull() ?: 0.0
        val minLon = allLons.minOrNull() ?: 0.0
        val maxLon = allLons.maxOrNull() ?: 0.0

        val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
        val lonRange = (maxLon - minLon).coerceAtLeast(0.0001)

        val padding = 48.dp.toPx()
        val usableW = width - (padding * 2)
        val usableH = height - (padding * 2)

        fun mapToScreen(lat: Double, lon: Double): Offset {
            val normX = ((lon - minLon) / lonRange).toFloat()
            val normY = (1.0f - ((lat - minLat) / latRange)).toFloat()
            return Offset(
                x = padding + (normX * usableW),
                y = padding + (normY * usableH)
            )
        }

        // Draw suggested guidance line if available
        if (suggestedRoute.size >= 2) {
            val sugPath = Path()
            val firstSug = mapToScreen(suggestedRoute.first().first, suggestedRoute.first().second)
            sugPath.moveTo(firstSug.x, firstSug.y)
            for (i in 1 until suggestedRoute.size) {
                val pt = mapToScreen(suggestedRoute[i].first, suggestedRoute[i].second)
                sugPath.lineTo(pt.x, pt.y)
            }
            drawPath(
                path = sugPath,
                color = Color(0xFF0284C7),
                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Draw actual GPS driven polyline
        val path = Path()
        val firstPoint = mapToScreen(route.first().first, route.first().second)
        path.moveTo(firstPoint.x, firstPoint.y)

        for (i in 1 until route.size) {
            val pt = mapToScreen(route[i].first, route[i].second)
            path.lineTo(pt.x, pt.y)
        }

        drawPath(
            path = path,
            color = Color(0xFFEF4444),
            style = Stroke(
                width = 8f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        val startScreen = mapToScreen(route.first().first, route.first().second)
        drawCircle(
            color = Color(0xFF22C55E),
            radius = 10.dp.toPx(),
            center = startScreen
        )
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = startScreen
        )

        val currentScreen = mapToScreen(route.last().first, route.last().second)
        drawCircle(
            color = Color(0xFFEF4444).copy(alpha = 0.3f),
            radius = 18.dp.toPx(),
            center = currentScreen
        )
        drawCircle(
            color = Color(0xFFEF4444),
            radius = 10.dp.toPx(),
            center = currentScreen
        )
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = currentScreen
        )

        // Draw destination dot if set
        destination?.let { dest ->
            val destScreen = mapToScreen(dest.latitude, dest.longitude)
            drawCircle(
                color = Color(0xFF0284C7),
                radius = 12.dp.toPx(),
                center = destScreen
            )
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = destScreen
            )
        }
    }
}
