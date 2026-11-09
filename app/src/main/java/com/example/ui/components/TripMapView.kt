package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.R
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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

enum class MapTypeMode {
    NAVIGATION_STREET, // CartoDB Voyager: Google Maps clean road network, readable street names, building footprints
    NAVIGATION_DARK,   // CartoDB Dark Matter: Night navigation mode
    OSM_CLASSIC,       // OpenStreetMap Standard Mapnik
    GOOGLE_MAPS_SDK    // Google Maps SDK (if API key is supplied)
}

enum class MapOrientationMode {
    NORTH_UP,          // Map orientation fixed at 0.0 (North is up)
    HEADING_UP         // Map rotates with vehicle heading, road ahead points towards top of screen
}

// Tile sources for clean, modern Google Maps navigation-style road presentation
private val TILE_SOURCE_VOYAGER = XYTileSource(
    "CartoVoyager",
    0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
    ),
    "© OpenStreetMap contributors, © CARTO"
)

private val TILE_SOURCE_DARK = XYTileSource(
    "CartoDark",
    0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/dark_all/",
        "https://b.basemaps.cartocdn.com/rastertiles/dark_all/",
        "https://c.basemaps.cartocdn.com/rastertiles/dark_all/"
    ),
    "© OpenStreetMap contributors, © CARTO"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripMapView(
    tripState: TripState,
    modifier: Modifier = Modifier,
    isNavigationConsoleMode: Boolean = false,
    onOpenMeterDetails: (() -> Unit)? = null,
    onEndTrip: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    // Detect Google Maps API Key configuration
    val mapsApiKey = try { BuildConfig.MAPS_API_KEY } catch (e: Exception) { "" }
    val isGoogleKeyConfigured = mapsApiKey.isNotBlank() &&
            !mapsApiKey.contains("DEFAULT_MAPS_KEY", ignoreCase = true) &&
            !mapsApiKey.contains("MY_GEMINI_API_KEY", ignoreCase = true) &&
            !mapsApiKey.contains("AIzaSyB7j5s8T369OKe4H69e3jhkGfM2sJhniCo", ignoreCase = true)

    // Primary default map style is Navigation Street (clean Google Maps aesthetic)
    var selectedMode by remember { mutableStateOf(MapTypeMode.NAVIGATION_STREET) }
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

    // Explicit function to calculate suggested guidance route (called only when destination is chosen or recalculated)
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

    // Trigger route calculation ONLY when destination is selected (NEVER called repeatedly on every GPS tick)
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
                MapTypeMode.NAVIGATION_STREET -> {
                    OpenStreetMapTileView(
                        tripState = tripState,
                        tileSourceType = MapTypeMode.NAVIGATION_STREET,
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
                MapTypeMode.NAVIGATION_DARK -> {
                    OpenStreetMapTileView(
                        tripState = tripState,
                        tileSourceType = MapTypeMode.NAVIGATION_DARK,
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
                MapTypeMode.OSM_CLASSIC -> {
                    OpenStreetMapTileView(
                        tripState = tripState,
                        tileSourceType = MapTypeMode.OSM_CLASSIC,
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
                            // Actual GPS Driven Route (Crimson Red with rounded finish)
                            if (latLngRoute.size >= 2) {
                                Polyline(
                                    points = latLngRoute,
                                    color = Color(0xFFDC2626),
                                    width = 10f
                                )
                            }
                            // Suggested Driving Guidance Route (Google Maps Blue with casing)
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
                            // Live Vehicle Marker
                            if (tripState.latitude != null && tripState.longitude != null) {
                                Marker(
                                    state = rememberMarkerState(position = currentLatLng),
                                    title = "Vehicle Location",
                                    rotation = if (orientationMode == MapOrientationMode.HEADING_UP) 0f else tripState.bearing,
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                                )
                            }
                            // Destination Marker
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
                        OpenStreetMapTileView(
                            tripState = tripState,
                            tileSourceType = MapTypeMode.NAVIGATION_STREET,
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
                                        text = "Search destination (streets, areas, landmarks)...",
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

                // Selected Destination Driving Guidance Banner
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
                                        text = "Route Guidance (Visual Only)",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Explicit recalculate route button
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
                // Compass / Orientation Control (North-Up vs Heading-Up)
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

                // Zoom Controls Stack
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

            // RE-CENTER TO VEHICLE BUTTON (Prominently displayed when user pans/inspects)
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
                    // Recenter on vehicle standard 48dp FAB if already following
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

                    // Map Style Switcher (Voyager Clean Road / Night / OSM)
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.90f),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 3.dp,
                        modifier = Modifier.clickable {
                            selectedMode = when (selectedMode) {
                                MapTypeMode.NAVIGATION_STREET -> MapTypeMode.NAVIGATION_DARK
                                MapTypeMode.NAVIGATION_DARK -> MapTypeMode.OSM_CLASSIC
                                MapTypeMode.OSM_CLASSIC -> if (isGoogleKeyConfigured) MapTypeMode.GOOGLE_MAPS_SDK else MapTypeMode.NAVIGATION_STREET
                                MapTypeMode.GOOGLE_MAPS_SDK -> MapTypeMode.NAVIGATION_STREET
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
                            Text(
                                text = when (selectedMode) {
                                    MapTypeMode.NAVIGATION_STREET -> "Google Style"
                                    MapTypeMode.NAVIGATION_DARK -> "Night Nav"
                                    MapTypeMode.OSM_CLASSIC -> "OSM Classic"
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

            // NAVIGATION CONSOLE HUD: COMPACT BOTTOM BAR DURING ACTIVE TRIP
            if (isNavigationConsoleMode) {
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.94f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        // Metrics Row (Speed, Fare, Distance, Duration)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Live Speed
                            Column {
                                Text(
                                    text = "SPEED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "${tripState.speedKmH.toInt()} km/h",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            // Current Fare
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "LIVE FARE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "${tripState.currency} ${String.format(Locale.US, "%.2f", tripState.currentFare)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFF8B814)
                                )
                            }

                            // Distance
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "DISTANCE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", tripState.distanceKm)} km",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF38BDF8)
                                )
                            }

                            // Duration
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "TIME",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                val mins = tripState.durationSeconds / 60
                                val secs = tripState.durationSeconds % 60
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", mins, secs),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Compact Action Buttons Row (Meter Details + End Trip)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Meter Details Button
                            if (onOpenMeterDetails != null) {
                                Button(
                                    onClick = onOpenMeterDetails,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF334155),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("meter_details_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = Color(0xFFF8B814),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Meter Details",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // End Trip Button
                            if (onEndTrip != null) {
                                Button(
                                    onClick = onEndTrip,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFDC2626),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("end_trip_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "End Trip",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
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

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun OpenStreetMapTileView(
    tripState: TripState,
    tileSourceType: MapTypeMode,
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
    val lat = tripState.latitude ?: 11.0168
    val lon = tripState.longitude ?: 76.9558
    val routePoints = tripState.routePoints

    LaunchedEffect(Unit) {
        org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName
    }

    AndroidView(
        factory = { ctx ->
            org.osmdroid.config.Configuration.getInstance().userAgentValue = ctx.packageName
            MapView(ctx).apply {
                val tileSource = when (tileSourceType) {
                    MapTypeMode.NAVIGATION_DARK -> TILE_SOURCE_DARK
                    MapTypeMode.OSM_CLASSIC -> TileSourceFactory.MAPNIK
                    else -> TILE_SOURCE_VOYAGER
                }
                setTileSource(tileSource)
                setMultiTouchControls(true)
                isTilesScaledToDpi = true
                controller.setZoom(17.5)
                controller.setCenter(GeoPoint(lat, lon))

                // Detect driver manual pan/drag gesture to toggle off auto-follow
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_MOVE) {
                        onUserPan()
                    }
                    false
                }
            }
        },
        update = { mapView ->
            // Switch tile source if style changed
            val targetTileSource = when (tileSourceType) {
                MapTypeMode.NAVIGATION_DARK -> TILE_SOURCE_DARK
                MapTypeMode.OSM_CLASSIC -> TileSourceFactory.MAPNIK
                else -> TILE_SOURCE_VOYAGER
            }
            if (mapView.tileProvider.tileSource.name() != targetTileSource.name()) {
                mapView.setTileSource(targetTileSource)
            }

            mapView.overlays.clear()

            val geoPoint = GeoPoint(lat, lon)

            // Handle Zoom Controls
            if (zoomInTrigger > 0L) {
                mapView.controller.zoomIn()
            }
            if (zoomOutTrigger > 0L) {
                mapView.controller.zoomOut()
            }

            // Map Orientation: Heading-Up vs North-Up
            if (orientationMode == MapOrientationMode.HEADING_UP && tripState.bearing != 0f) {
                mapView.mapOrientation = -tripState.bearing
            } else {
                mapView.mapOrientation = 0f
            }

            // Smooth vehicle centering / auto-following
            if (isFollowingVehicle || recenterTrigger > 0L) {
                mapView.controller.animateTo(geoPoint)
            }

            // 1. Draw Suggested Guidance Route (Google Maps Blue with rounded edges & under-casing)
            if (suggestedRoute.size >= 2) {
                // Outer casing for contrast against white streets
                val outerCasing = Polyline(mapView).apply {
                    outlinePaint.color = android.graphics.Color.parseColor("#1557B0")
                    outlinePaint.strokeWidth = 16f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    setPoints(suggestedRoute.map { GeoPoint(it.first, it.second) })
                }
                mapView.overlays.add(outerCasing)

                // Inner Electric Blue Polyline
                val innerLine = Polyline(mapView).apply {
                    outlinePaint.color = android.graphics.Color.parseColor("#1A73E8")
                    outlinePaint.strokeWidth = 10f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    setPoints(suggestedRoute.map { GeoPoint(it.first, it.second) })
                }
                mapView.overlays.add(innerLine)
            }

            // 2. Draw Actual GPS Recorded Route (Crimson Red with rounded finish)
            if (routePoints.size >= 2) {
                val polyline = Polyline(mapView).apply {
                    outlinePaint.color = android.graphics.Color.parseColor("#DC2626")
                    outlinePaint.strokeWidth = 10f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    setPoints(routePoints.map { GeoPoint(it.first, it.second) })
                }
                mapView.overlays.add(polyline)

                // Trip Start Marker
                val startPoint = GeoPoint(routePoints.first().first, routePoints.first().second)
                val startMarker = Marker(mapView).apply {
                    position = startPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_start_flag)
                    title = "Trip Start"
                }
                mapView.overlays.add(startMarker)
            }

            // 3. Destination Pin Marker if destination is selected
            destination?.let { dest ->
                val destPoint = GeoPoint(dest.latitude, dest.longitude)
                val destMarker = Marker(mapView).apply {
                    position = destPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_destination_pin)
                    title = dest.name
                    snippet = dest.description
                }
                mapView.overlays.add(destMarker)
            }

            // 4. Live Vehicle Navigation Marker with Directional Orientation
            val navVehicleMarker = Marker(mapView).apply {
                position = geoPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_nav_vehicle_arrow)
                // In Heading-Up mode, the map rotates and the vehicle arrow points straight forward (0f)
                // In North-Up mode, the vehicle arrow rotates to match the GPS bearing
                rotation = if (orientationMode == MapOrientationMode.HEADING_UP) 0f else tripState.bearing
                title = "Live Vehicle Location"
            }
            mapView.overlays.add(navVehicleMarker)

            mapView.invalidate()
        },
        modifier = modifier
    )
}
