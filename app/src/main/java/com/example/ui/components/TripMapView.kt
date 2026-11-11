package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.Locale

enum class MapTypeMode {
    OLA_MAPS_LIGHT,    // Ola Maps Official Vector Map (Light Standard style)
    GOOGLE_MAPS_SDK    // Google Maps SDK (if API key is supplied)
}

enum class MapOrientationMode {
    NORTH_UP,          // Map orientation fixed at 0.0 (North is up)
    HEADING_UP         // Map rotates with vehicle heading, road ahead points towards top of screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripMapView(
    tripState: TripState,
    modifier: Modifier = Modifier,
    isNavigationConsoleMode: Boolean = false,
    onOpenMeterDetails: (() -> Unit)? = null,
    onEndTrip: (() -> Unit)? = null,
    onPauseTrip: (() -> Unit)? = null,
    onResumeTrip: (() -> Unit)? = null,
    isExpanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    // Detect Google Maps API Key configuration
    val mapsApiKey = try { BuildConfig.MAPS_API_KEY } catch (e: Exception) { "" }
    val isGoogleKeyConfigured = mapsApiKey.isNotBlank() &&
            !mapsApiKey.contains("DEFAULT_MAPS_KEY", ignoreCase = true) &&
            !mapsApiKey.contains("MY_GEMINI_API_KEY", ignoreCase = true) &&
            !mapsApiKey.contains("AIzaSyB7j5s8T369OKe4H69e3jhkGfM2sJhniCo", ignoreCase = true)

    // Primary default map style is Ola Maps Official Vector Map
    var selectedMode by remember { mutableStateOf(MapTypeMode.OLA_MAPS_LIGHT) }
    var orientationMode by remember { mutableStateOf(MapOrientationMode.NORTH_UP) }
    var isFollowingVehicle by remember { mutableStateOf(true) }

    // Destination search & suggested driving guidance state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchPlaceResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedDestination by remember { mutableStateOf<SearchPlaceResult?>(null) }
    var suggestedRoute by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    var isRouteCalculating by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var recenterTrigger by remember { mutableLongStateOf(0L) }
    var zoomInTrigger by remember { mutableLongStateOf(0L) }
    var zoomOutTrigger by remember { mutableLongStateOf(0L) }

    // LatLng for Google Maps SDK fallback if activated
    val currentLatLng = if (tripState.latitude != null && tripState.longitude != null) {
        LatLng(tripState.latitude, tripState.longitude)
    } else {
        LatLng(11.0168, 76.9558)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLatLng, 17f)
    }

    LaunchedEffect(currentLatLng) {
        if (tripState.latitude != null && tripState.longitude != null && selectedMode == MapTypeMode.GOOGLE_MAPS_SDK && isFollowingVehicle) {
            try {
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(currentLatLng)
                            .zoom(17.5f)
                            .bearing(if (orientationMode == MapOrientationMode.HEADING_UP) tripState.bearing else 0f)
                            .tilt(if (orientationMode == MapOrientationMode.HEADING_UP) 30f else 0f)
                            .build()
                    )
                )
            } catch (_: Exception) {}
        }
    }

    // Explicit function to calculate suggested guidance route
    fun calculateRouteToDestination(dest: SearchPlaceResult) {
        val curLat = tripState.latitude ?: 11.0168
        val curLon = tripState.longitude ?: 76.9558
        isRouteCalculating = true
        coroutineScope.launch {
            val route = DestinationSearchService.fetchSuggestedDrivingRoute(
                startLat = curLat,
                startLon = curLon,
                destLat = dest.latitude,
                destLon = dest.longitude
            )
            suggestedRoute = route
            isRouteCalculating = false
        }
    }

    // Trigger route calculation ONLY when destination is selected
    LaunchedEffect(selectedDestination) {
        val dest = selectedDestination
        if (dest != null) {
            calculateRouteToDestination(dest)
        } else {
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
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedMode) {
                MapTypeMode.OLA_MAPS_LIGHT -> {
                    OlaMapView(
                        tripState = tripState,
                        orientationMode = orientationMode,
                        destination = selectedDestination,
                        suggestedRoute = suggestedRoute,
                        isFollowingVehicle = isFollowingVehicle,
                        recenterTrigger = recenterTrigger,
                        zoomInTrigger = zoomInTrigger,
                        zoomOutTrigger = zoomOutTrigger,
                        onUserPan = { isFollowingVehicle = false },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MapTypeMode.GOOGLE_MAPS_SDK -> {
                    if (isGoogleKeyConfigured) {
                        GoogleMap(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("google_trip_map"),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(isMyLocationEnabled = false)
                        ) {
                            if (latLngRoute.size >= 2) {
                                Polyline(
                                    points = latLngRoute,
                                    color = Color(0xFFDC2626),
                                    width = 10f
                                )
                            }
                            if (latLngSuggested.size >= 2) {
                                Polyline(
                                    points = latLngSuggested,
                                    color = Color(0xFF1557B0),
                                    width = 16f
                                )
                                Polyline(
                                    points = latLngSuggested,
                                    color = Color(0xFF1A73E8),
                                    width = 10f
                                )
                            }
                            if (tripState.latitude != null && tripState.longitude != null) {
                                Marker(
                                    state = rememberMarkerState(position = currentLatLng),
                                    title = "Vehicle Location",
                                    rotation = if (orientationMode == MapOrientationMode.HEADING_UP) 0f else tripState.bearing,
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                                )
                            }
                            selectedDestination?.let { dest ->
                                Marker(
                                    state = rememberMarkerState(position = LatLng(dest.latitude, dest.longitude)),
                                    title = dest.name,
                                    snippet = dest.description,
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                                )
                            }
                        }
                    } else {
                        OlaMapView(
                            tripState = tripState,
                            orientationMode = orientationMode,
                            destination = selectedDestination,
                            suggestedRoute = suggestedRoute,
                            isFollowingVehicle = isFollowingVehicle,
                            recenterTrigger = recenterTrigger,
                            zoomInTrigger = zoomInTrigger,
                            zoomOutTrigger = zoomOutTrigger,
                            onUserPan = { isFollowingVehicle = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // TOP FLOATING HEADER: DESTINATION SEARCH BAR & GUIDANCE BANNER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (isExpanded && onToggleExpand != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VendorLogoBadge(
                            vendorId = com.example.util.VendorLogoRenderer.GET_TAXI_VENDOR_ID,
                            vendorName = com.example.util.VendorLogoRenderer.GET_TAXI_VENDOR_NAME,
                            height = 38.dp,
                            testTag = "expanded_map_get_taxi_logo"
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            IconButton(
                                onClick = onToggleExpand,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("collapse_map_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "Collapse Map",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 5.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                            tint = Color(0xFF1A73E8),
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
                                        delay(300) // debounce
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
                                        text = "Search destination...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (isSearching || isRouteCalculating) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Color(0xFF1A73E8),
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
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("clear_destination_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (!isExpanded && onToggleExpand != null) {
                            IconButton(
                                onClick = onToggleExpand,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("expand_map_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Expand Map",
                                    tint = Color(0xFF1A73E8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = searchResults.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
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
                                            isFollowingVehicle = true
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color(0xFFEA4335),
                                        modifier = Modifier.size(20.dp)
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

                selectedDestination?.let { dest ->
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0xFF1A73E8), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = dest.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Ola Maps Guidance",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { calculateRouteToDestination(dest) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Recalculate Route",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = {
                                        selectedDestination = null
                                        suggestedRoute = emptyList()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Guidance",
                                        tint = Color(0xFFE2E8F0),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // RIGHT SIDE COMPACT MAP CONTROLS: COMPASS / ORIENTATION + ZOOM IN / OUT
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    onClick = {
                        orientationMode = if (orientationMode == MapOrientationMode.NORTH_UP) {
                            MapOrientationMode.HEADING_UP
                        } else {
                            MapOrientationMode.NORTH_UP
                        }
                    },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("map_orientation_control_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val rotationAngle = if (orientationMode == MapOrientationMode.HEADING_UP) {
                            tripState.bearing
                        } else {
                            0f
                        }
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Map Orientation",
                            tint = if (orientationMode == MapOrientationMode.HEADING_UP) Color(0xFFDC2626) else Color(0xFF1A73E8),
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(-rotationAngle)
                        )
                        Text(
                            text = if (orientationMode == MapOrientationMode.HEADING_UP) "H" else "N",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF64748B),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.width(44.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { zoomInTrigger = System.currentTimeMillis() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Zoom In",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        IconButton(
                            onClick = { zoomOutTrigger = System.currentTimeMillis() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Zoom Out",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // RE-CENTER TO VEHICLE BUTTON
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = if (isNavigationConsoleMode) 90.dp else 56.dp)
            ) {
                if (!isFollowingVehicle) {
                    FloatingActionButton(
                        onClick = {
                            isFollowingVehicle = true
                            recenterTrigger = System.currentTimeMillis()
                        },
                        containerColor = Color(0xFF1A73E8),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("recenter_vehicle_location_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Return to Vehicle",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Re-center",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // BOTTOM CONTROLS & STYLE SWITCHER ROW (When not in console mode)
            if (!isNavigationConsoleMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFollowingVehicle) {
                        FloatingActionButton(
                            onClick = { recenterTrigger = System.currentTimeMillis() },
                            containerColor = Color.White,
                            contentColor = Color(0xFF1A73E8),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("recenter_vehicle_location_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Center on Vehicle",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.90f),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 3.dp,
                        modifier = Modifier.clickable {
                            selectedMode = when (selectedMode) {
                                MapTypeMode.OLA_MAPS_LIGHT -> if (isGoogleKeyConfigured) MapTypeMode.GOOGLE_MAPS_SDK else MapTypeMode.OLA_MAPS_LIGHT
                                MapTypeMode.GOOGLE_MAPS_SDK -> MapTypeMode.OLA_MAPS_LIGHT
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Switch Map Engine",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (selectedMode) {
                                    MapTypeMode.OLA_MAPS_LIGHT -> "Vector Map"
                                    MapTypeMode.GOOGLE_MAPS_SDK -> "Google Maps"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            }

            // NAVIGATION CONSOLE HUD: CLEAN BOTTOM CONTROL BAR DURING ACTIVE TRIP
            if (isNavigationConsoleMode) {
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onPauseTrip != null || onResumeTrip != null) {
                                val isPaused = tripState.status == com.example.data.model.TripStatus.PAUSED
                                Button(
                                    onClick = { if (isPaused) onResumeTrip?.invoke() else onPauseTrip?.invoke() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPaused) Color(0xFF16A34A) else Color(0xFFF59E0B),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(44.dp)
                                        .testTag(if (isPaused) "resume_trip_button" else "pause_trip_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isPaused) "Resume" else "Pause",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            if (onOpenMeterDetails != null) {
                                Button(
                                    onClick = onOpenMeterDetails,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF334155),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(44.dp)
                                        .testTag("meter_details_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = Color(0xFFF8B814),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Meter Details",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            if (onEndTrip != null) {
                                Button(
                                    onClick = onEndTrip,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFDC2626),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("end_trip_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "End Trip",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
