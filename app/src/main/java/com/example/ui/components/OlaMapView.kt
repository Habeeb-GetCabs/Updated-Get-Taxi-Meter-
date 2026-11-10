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
                "if (typeof updateActualRoute === 'function') { updateActualRoute($actualRouteJson); }",
                null
            )
        }
    }

    // Effect: Update suggested guidance route polyline
    LaunchedEffect(suggestedRouteJson, isMapLoaded) {
        if (isMapLoaded) {
            webViewInstance?.evaluateJavascript(
                "if (typeof updateSuggestedRoute === 'function') { updateSuggestedRoute($suggestedRouteJson); }",
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
                    settings.loadWithOverviewMode = true
                    setBackgroundColor(AndroidColor.parseColor("#F8FAFC"))
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
                                mapErrorMessage = "Map Network Error: ${error?.description ?: "Unable to connect"}"
                            }
                        }
                    }

                    val htmlContent = buildCartoVectorMapHtml(lat, lon)
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
                        text = mapErrorMessage ?: "Check internet connection to render live vector tiles.",
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

private fun buildCartoVectorMapHtml(startLat: Double, startLon: Double): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <link rel="stylesheet" href="https://unpkg.com/maplibre-gl@3.6.2/dist/maplibre-gl.css" />
          <script src="https://unpkg.com/maplibre-gl@3.6.2/dist/maplibre-gl.js"></script>
          <style>
            * { box-sizing: border-box; }
            body, html, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #F8FAFC; overflow: hidden; }
            .maplibregl-ctrl-attrib, .maplibregl-ctrl-logo { display: none !important; }
            
            .driver-marker {
              width: 34px;
              height: 34px;
              background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%23DC2626"><path d="M12 2L4.5 20.29l.71.71L12 18l6.79 3 .71-.71z"/></svg>');
              background-size: contain;
              background-repeat: no-repeat;
              background-position: center;
              filter: drop-shadow(0px 2px 4px rgba(0,0,0,0.3));
              transition: transform 0.3s ease;
            }
            
            .dest-marker {
              width: 34px;
              height: 34px;
              background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%231557B0"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>');
              background-size: contain;
              background-repeat: no-repeat;
              background-position: center;
              filter: drop-shadow(0px 2px 4px rgba(0,0,0,0.3));
            }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            var map = null;
            var driverMarkerElement = null;
            var driverMarker = null;
            var destMarker = null;
            var isMapReady = false;

            function initCartoMap() {
              try {
                // Free, high-performance CartoDB Voyager GL style (OpenStreetMap vector tiles)
                var styleUrl = 'https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json';
                map = new maplibregl.Map({
                  container: 'map',
                  style: styleUrl,
                  center: [$startLon, $startLat],
                  zoom: 16.5,
                  bearing: 0,
                  attributionControl: false
                });

                map.on('load', function() {
                  isMapReady = true;

                  // Source & layer for actual driven GPS route
                  map.addSource('route-source', {
                    type: 'geojson',
                    data: {
                      type: 'Feature',
                      geometry: { type: 'LineString', coordinates: [] }
                    }
                  });
                  map.addLayer({
                    id: 'route-layer',
                    type: 'line',
                    source: 'route-source',
                    layout: { 'line-join': 'round', 'line-cap': 'round' },
                    paint: {
                      'line-color': '#DC2626',
                      'line-width': 6
                    }
                  });

                  // Source & layer for suggested guidance route
                  map.addSource('suggested-route-source', {
                    type: 'geojson',
                    data: {
                      type: 'Feature',
                      geometry: { type: 'LineString', coordinates: [] }
                    }
                  });
                  map.addLayer({
                    id: 'suggested-route-layer',
                    type: 'line',
                    source: 'suggested-route-source',
                    layout: { 'line-join': 'round', 'line-cap': 'round' },
                    paint: {
                      'line-color': '#1A73E8',
                      'line-width': 6
                    }
                  });

                  // Create Driver Marker
                  driverMarkerElement = document.createElement('div');
                  driverMarkerElement.className = 'driver-marker';
                  driverMarker = new maplibregl.Marker({ element: driverMarkerElement })
                    .setLngLat([$startLon, $startLat])
                    .addTo(map);

                  if (window.AndroidBridge && window.AndroidBridge.onMapLoaded) {
                    window.AndroidBridge.onMapLoaded();
                  }
                });

                map.on('dragstart', function() {
                  if (window.AndroidBridge && window.AndroidBridge.onUserPan) {
                    window.AndroidBridge.onUserPan();
                  }
                });

              } catch (err) {
                if (window.AndroidBridge && window.AndroidBridge.onMapError) {
                  window.AndroidBridge.onMapError(err.message || "Failed to initialize MapLibre Vector Map.");
                }
              }
            }

            function updateDriverLocation(lat, lon, bearing, isFollowing) {
              if (!map || !isMapReady) return;
              if (driverMarker) {
                driverMarker.setLngLat([lon, lat]);
              }
              if (driverMarkerElement) {
                driverMarkerElement.style.transform = 'rotate(' + (bearing || 0) + 'deg)';
              }
              if (isFollowing) {
                map.easeTo({ center: [lon, lat], duration: 400 });
              }
            }

            function setMapOrientation(bearing, isHeadingUp) {
              if (!map || !isMapReady) return;
              if (isHeadingUp) {
                map.rotateTo(bearing || 0, { duration: 300 });
              } else {
                map.rotateTo(0, { duration: 300 });
              }
            }

            function updateActualRoute(coordsJson) {
              if (!map || !isMapReady) return;
              try {
                var coords = JSON.parse(coordsJson);
                var geojson = {
                  type: 'Feature',
                  geometry: { type: 'LineString', coordinates: coords }
                };
                var src = map.getSource('route-source');
                if (src) src.setData(geojson);
              } catch (e) {}
            }

            function updateSuggestedRoute(coordsJson) {
              if (!map || !isMapReady) return;
              try {
                var coords = JSON.parse(coordsJson);
                var geojson = {
                  type: 'Feature',
                  geometry: { type: 'LineString', coordinates: coords }
                };
                var src = map.getSource('suggested-route-source');
                if (src) src.setData(geojson);
              } catch (e) {}
            }

            function setDestinationMarker(lat, lon) {
              if (!map || !isMapReady) return;
              if (destMarker) {
                destMarker.remove();
                destMarker = null;
              }
              if (lat && lon) {
                var el = document.createElement('div');
                el.className = 'dest-marker';
                destMarker = new maplibregl.Marker({ element: el })
                  .setLngLat([lon, lat])
                  .addTo(map);
              }
            }

            function zoomIn() { if (map) map.zoomIn(); }
            function zoomOut() { if (map) map.zoomOut(); }
            function recenter(lat, lon) { if (map) map.flyTo({ center: [lon, lat], zoom: 16.5 }); }

            document.addEventListener('DOMContentLoaded', initCartoMap);
          </script>
        </body>
        </html>
    """.trimIndent()
}

