package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.TripState
import com.example.util.SearchPlaceResult
import org.json.JSONArray

@SuppressLint("SetJavaScriptEnabled")
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
    var isMapLoaded by remember { mutableStateOf(false) }
    var mapErrorMessage by remember { mutableStateOf<String?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val lat = tripState.latitude ?: 11.0168
    val lon = tripState.longitude ?: 76.9558
    val bearing = tripState.bearing

    val actualRouteJson = remember(tripState.routePoints) {
        val array = JSONArray()
        tripState.routePoints.forEach { pt ->
            val coord = JSONArray()
            coord.put(pt.second) // lon
            coord.put(pt.first)  // lat
            array.put(coord)
        }
        array.toString()
    }

    val suggestedRouteJson = remember(suggestedRoute) {
        val array = JSONArray()
        suggestedRoute.forEach { pt ->
            val coord = JSONArray()
            coord.put(pt.second) // lon
            coord.put(pt.first)  // lat
            array.put(coord)
        }
        array.toString()
    }

    // Effect: Update live driver position & vehicle rotation in JavaScript map instance
    LaunchedEffect(lat, lon, bearing, isFollowingVehicle, isMapLoaded) {
        if (isMapLoaded) {
            webViewInstance?.evaluateJavascript(
                "if (typeof updateDriverLocation === 'function') { updateDriverLocation($lat, $lon, $bearing, $isFollowingVehicle); }",
                null
            )
        }
    }

    // Effect: Update map orientation (Heading-Up vs North-Up)
    LaunchedEffect(bearing, orientationMode, isMapLoaded) {
        if (isMapLoaded) {
            val isHeadingUp = orientationMode == MapOrientationMode.HEADING_UP
            webViewInstance?.evaluateJavascript(
                "if (typeof setMapOrientation === 'function') { setMapOrientation($bearing, $isHeadingUp); }",
                null
            )
        }
    }

    // Effect: Update actual driven GPS route polyline
    LaunchedEffect(actualRouteJson, isMapLoaded) {
        if (isMapLoaded) {
            webViewInstance?.evaluateJavascript(
                "if (typeof updateActualRoute === 'function') { updateActualRoute('$actualRouteJson'); }",
                null
            )
        }
    }

    // Effect: Update suggested guidance route polyline
    LaunchedEffect(suggestedRouteJson, isMapLoaded) {
        if (isMapLoaded) {
            webViewInstance?.evaluateJavascript(
                "if (typeof updateSuggestedRoute === 'function') { updateSuggestedRoute('$suggestedRouteJson'); }",
                null
            )
        }
    }

    // Effect: Update destination pin marker
    LaunchedEffect(destination, isMapLoaded) {
        if (isMapLoaded) {
            if (destination != null) {
                webViewInstance?.evaluateJavascript(
                    "if (typeof setDestinationMarker === 'function') { setDestinationMarker(${destination.latitude}, ${destination.longitude}); }",
                    null
                )
            } else {
                webViewInstance?.evaluateJavascript(
                    "if (typeof setDestinationMarker === 'function') { setDestinationMarker(null, null); }",
                    null
                )
            }
        }
    }

    // Effect: Re-center trigger
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0L && isMapLoaded) {
            webViewInstance?.evaluateJavascript(
                "if (typeof recenter === 'function') { recenter($lat, $lon); }",
                null
            )
        }
    }

    // Effect: Zoom In / Out triggers
    LaunchedEffect(zoomInTrigger) {
        if (zoomInTrigger > 0L && isMapLoaded) {
            webViewInstance?.evaluateJavascript("if (typeof zoomIn === 'function') { zoomIn(); }", null)
        }
    }

    LaunchedEffect(zoomOutTrigger) {
        if (zoomOutTrigger > 0L && isMapLoaded) {
            webViewInstance?.evaluateJavascript("if (typeof zoomOut === 'function') { zoomOut(); }", null)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.useWideViewPort = true
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(AndroidColor.parseColor("#E2E8F0"))
                    overScrollMode = View.OVER_SCROLL_NEVER

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onMapLoaded() {
                            isMapLoaded = true
                            mapErrorMessage = null
                        }

                        @JavascriptInterface
                        fun onMapError(errorMsg: String?) {
                            isMapLoaded = false
                            mapErrorMessage = errorMsg ?: "Map load error."
                        }

                        @JavascriptInterface
                        fun onUserPan() {
                            onUserPan()
                        }
                    }, "AndroidBridge")

                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                mapErrorMessage = "Map Connection Error: ${error?.description ?: "Unable to connect"}"
                            }
                        }
                    }

                    val htmlContent = buildLeafletCartoMapHtml(lat, lon)
                    loadDataWithBaseURL("https://basemaps.cartocdn.com/", htmlContent, "text/html", "UTF-8", null)
                    webViewInstance = this
                }
            },
            update = { wv ->
                webViewInstance = wv
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("vector_maps_webview")
        )

        // Error Card (Displayed only if network is completely disconnected)
        if (mapErrorMessage != null) {
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444)),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .testTag("vector_maps_error_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Map Error",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Map Connection Error",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = mapErrorMessage ?: "Check internet connection to render live street map.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            mapErrorMessage = null
                            webViewInstance?.reload()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Map", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun buildLeafletCartoMapHtml(startLat: Double, startLon: Double): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <style>
            * { box-sizing: border-box; }
            body, html, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #F8FAFC; overflow: hidden; }
            .leaflet-control-container .leaflet-routing-container { display: none !important; }
            .leaflet-control-attribution { display: none !important; }
            
            .driver-marker-container {
              background: none;
              border: none;
            }
            
            .driver-marker {
              width: 36px;
              height: 36px;
              background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%23DC2626"><path d="M12 2L4.5 20.29l.71.71L12 18l6.79 3 .71-.71z"/></svg>');
              background-size: contain;
              background-repeat: no-repeat;
              background-position: center;
              filter: drop-shadow(0px 2px 5px rgba(0,0,0,0.4));
              transition: transform 0.2s linear;
            }
            
            .dest-marker-container {
              background: none;
              border: none;
            }

            .dest-marker {
              width: 36px;
              height: 36px;
              background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%231557B0"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>');
              background-size: contain;
              background-repeat: no-repeat;
              background-position: center;
              filter: drop-shadow(0px 2px 5px rgba(0,0,0,0.4));
            }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            var map = null;
            var driverMarker = null;
            var destMarker = null;
            var actualRoutePolyline = null;
            var suggestedRoutePolyline = null;

            function initMap() {
              try {
                map = L.map('map', {
                  zoomControl: false,
                  attributionControl: false,
                  fadeAnimation: false,
                  zoomAnimation: true
                }).setView([$startLat, $startLon], 16.5);

                // High-resolution, crisp CartoDB Voyager tiles (100% Free, No API Key Required)
                var mainTileLayer = L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                  maxZoom: 19,
                  subdomains: 'abcd'
                }).addTo(map);

                // Tile error fallback to OpenStreetMap standard tiles
                mainTileLayer.on('tileerror', function() {
                  if (!window.hasTileFallback) {
                    window.hasTileFallback = true;
                    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
                  }
                });

                // Polylines for driven GPS route and suggested route
                actualRoutePolyline = L.polyline([], { color: '#DC2626', weight: 6, opacity: 0.9, lineCap: 'round', lineJoin: 'round' }).addTo(map);
                suggestedRoutePolyline = L.polyline([], { color: '#1A73E8', weight: 6, opacity: 0.85, lineCap: 'round', lineJoin: 'round' }).addTo(map);

                // Create Driver Marker
                var driverIcon = L.divIcon({
                  className: 'driver-marker-container',
                  html: '<div id="driver-icon" class="driver-marker"></div>',
                  iconSize: [36, 36],
                  iconAnchor: [18, 18]
                });
                driverMarker = L.marker([$startLat, $startLon], { icon: driverIcon }).addTo(map);

                map.on('dragstart', function() {
                  if (window.AndroidBridge && window.AndroidBridge.onUserPan) {
                    window.AndroidBridge.onUserPan();
                  }
                });

                if (window.AndroidBridge && window.AndroidBridge.onMapLoaded) {
                  window.AndroidBridge.onMapLoaded();
                }

              } catch (err) {
                if (window.AndroidBridge && window.AndroidBridge.onMapError) {
                  window.AndroidBridge.onMapError(err.message || "Failed to initialize map.");
                }
              }
            }

            function updateDriverLocation(lat, lon, bearing, isFollowing) {
              if (!map) return;
              if (driverMarker) {
                driverMarker.setLatLng([lat, lon]);
              }
              var el = document.getElementById('driver-icon');
              if (el) {
                el.style.transform = 'rotate(' + (bearing || 0) + 'deg)';
              }
              if (isFollowing) {
                map.panTo([lat, lon], { animate: true, duration: 0.3 });
              }
            }

            function setMapOrientation(bearing, isHeadingUp) {
              // Heading orientation visual cue on driver icon
            }

            function updateActualRoute(coordsJson) {
              if (!map) return;
              try {
                var coords = JSON.parse(coordsJson);
                var latLngs = coords.map(function(c) { return [c[1], c[0]]; });
                actualRoutePolyline.setLatLngs(latLngs);
              } catch (e) {}
            }

            function updateSuggestedRoute(coordsJson) {
              if (!map) return;
              try {
                var coords = JSON.parse(coordsJson);
                var latLngs = coords.map(function(c) { return [c[1], c[0]]; });
                suggestedRoutePolyline.setLatLngs(latLngs);
              } catch (e) {}
            }

            function setDestinationMarker(lat, lon) {
              if (!map) return;
              if (destMarker) {
                map.removeLayer(destMarker);
                destMarker = null;
              }
              if (lat && lon) {
                var destIcon = L.divIcon({
                  className: 'dest-marker-container',
                  html: '<div class="dest-marker"></div>',
                  iconSize: [36, 36],
                  iconAnchor: [18, 36]
                });
                destMarker = L.marker([lat, lon], { icon: destIcon }).addTo(map);
              }
            }

            function zoomIn() { if (map) map.zoomIn(); }
            function zoomOut() { if (map) map.zoomOut(); }
            function recenter(lat, lon) { if (map) map.setView([lat, lon], 16.5); }

            document.addEventListener('DOMContentLoaded', initMap);
          </script>
        </body>
        </html>
    """.trimIndent()
}


