package com.example.util

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class SearchPlaceResult(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double
)

object DestinationSearchService {

    private const val TAG = "DestSearchService"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private fun getOlaMapsApiKey(): String {
        return try {
            val key = BuildConfig.OLA_MAPS_API_KEY
            if (key.isNotBlank() && !key.contains("DEFAULT", ignoreCase = true)) {
                key.trim()
            } else "Joy3ugL9iGWXa7RSTvWSEcNkM7gx2iFPObU8R3Gy"
        } catch (e: Exception) {
            "Joy3ugL9iGWXa7RSTvWSEcNkM7gx2iFPObU8R3Gy"
        }
    }

    /**
     * Searches places, streets, landmarks using Ola Maps Autocomplete API.
     */
    suspend fun searchPlaces(
        query: String,
        userLat: Double?,
        userLon: Double?
    ): List<SearchPlaceResult> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()

        val apiKey = getOlaMapsApiKey()
        val results = mutableListOf<SearchPlaceResult>()

        if (apiKey.isNotBlank()) {
            try {
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                val urlBuilder = StringBuilder("https://api.olamaps.io/places/v1/autocomplete?input=$encodedQuery&api_key=$apiKey")
                if (userLat != null && userLon != null && userLat != 0.0 && userLon != 0.0) {
                    urlBuilder.append("&location=$userLat,$userLon")
                }

                val request = Request.Builder()
                    .url(urlBuilder.toString())
                    .header("User-Agent", "GetTaxiApp/1.0 (Android)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val predictions = json.optJSONArray("predictions")
                        if (predictions != null) {
                            for (i in 0 until predictions.length()) {
                                val item = predictions.optJSONObject(i) ?: continue
                                val desc = item.optString("description", "")
                                val structured = item.optJSONObject("structured_formatting")
                                val mainText = structured?.optString("main_text") ?: item.optString("description", "Location")
                                val secondaryText = structured?.optString("secondary_text") ?: desc

                                val geometry = item.optJSONObject("geometry")
                                val location = geometry?.optJSONObject("location")
                                val lat = location?.optDouble("lat", 0.0) ?: 0.0
                                val lng = location?.optDouble("lng", 0.0) ?: 0.0

                                if (lat != 0.0 && lng != 0.0) {
                                    results.add(
                                        SearchPlaceResult(
                                            name = mainText,
                                            description = secondaryText.ifBlank { "$lat, $lng" },
                                            latitude = lat,
                                            longitude = lng
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Ola Maps autocomplete error: ${e.message}")
            }
        }

        // Fallback search if Ola Maps API response is empty or unconfigured
        if (results.isEmpty()) {
            try {
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                val urlBuilder = StringBuilder("https://photon.komoot.io/api/?q=$encodedQuery&limit=6")
                if (userLat != null && userLon != null && userLat != 0.0 && userLon != 0.0) {
                    urlBuilder.append("&lat=$userLat&lon=$userLon")
                }

                val request = Request.Builder()
                    .url(urlBuilder.toString())
                    .header("User-Agent", "GetTaxiApp/1.0 (Android)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val features = json.optJSONArray("features") ?: return@use
                        for (i in 0 until features.length()) {
                            val feature = features.optJSONObject(i) ?: continue
                            val geometry = feature.optJSONObject("geometry") ?: continue
                            val coords = geometry.optJSONArray("coordinates") ?: continue
                            if (coords.length() < 2) continue
                            val lon = coords.getDouble(0)
                            val lat = coords.getDouble(1)

                            val properties = feature.optJSONObject("properties") ?: continue
                            val name = properties.optString("name").ifBlank {
                                properties.optString("street", "Location")
                            }
                            val street = properties.optString("street")
                            val city = properties.optString("city")
                            val details = listOfNotNull(street.takeIf { it.isNotBlank() && it != name }, city.takeIf { it.isNotBlank() }).joinToString(", ")

                            results.add(
                                SearchPlaceResult(
                                    name = name,
                                    description = details.ifBlank { "$lat, $lon" },
                                    latitude = lat,
                                    longitude = lon
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallback search error: ${e.message}")
            }
        }

        return@withContext results
    }

    /**
     * Calculates a suggested driving route geometry from vehicle location to selected destination.
     * CRITICAL: Solely for visual driver guidance on the map (never for fare or meter distance).
     */
    suspend fun fetchSuggestedDrivingRoute(
        startLat: Double,
        startLon: Double,
        destLat: Double,
        destLon: Double
    ): List<Pair<Double, Double>> = withContext(Dispatchers.IO) {
        val apiKey = getOlaMapsApiKey()
        val routePoints = mutableListOf<Pair<Double, Double>>()

        if (apiKey.isNotBlank()) {
            try {
                val url = "https://api.olamaps.io/routing/v1/directions?origin=$startLat,$startLon&destination=$destLat,$destLon&api_key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "GetTaxiApp/1.0 (Android)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val routes = json.optJSONArray("routes")
                        if (routes != null && routes.length() > 0) {
                            val route = routes.getJSONObject(0)
                            val legs = route.optJSONArray("legs")
                            if (legs != null && legs.length() > 0) {
                                val leg = legs.getJSONObject(0)
                                val steps = leg.optJSONArray("steps")
                                if (steps != null) {
                                    for (i in 0 until steps.length()) {
                                        val step = steps.optJSONObject(i) ?: continue
                                        val startLoc = step.optJSONObject("start_location")
                                        if (startLoc != null) {
                                            val lat = startLoc.optDouble("lat", 0.0)
                                            val lng = startLoc.optDouble("lng", 0.0)
                                            if (lat != 0.0 && lng != 0.0) {
                                                routePoints.add(Pair(lat, lng))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Ola Maps routing error: ${e.message}")
            }
        }

        // Fallback routing if Ola Maps directions response is empty
        if (routePoints.isEmpty()) {
            try {
                val url = "https://router.project-osrm.org/route/v1/driving/$startLon,$startLat;$destLon,$destLat?overview=full&geometries=geojson"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "GetTaxiApp/1.0 (Android)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val routes = json.optJSONArray("routes") ?: return@use
                        if (routes.length() == 0) return@use
                        val firstRoute = routes.getJSONObject(0)
                        val geometry = firstRoute.optJSONObject("geometry") ?: return@use
                        val coordinates = geometry.optJSONArray("coordinates") ?: return@use

                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.optJSONArray(i) ?: continue
                            if (coord.length() >= 2) {
                                val lon = coord.getDouble(0)
                                val lat = coord.getDouble(1)
                                routePoints.add(Pair(lat, lon))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallback route calculation error: ${e.message}")
            }
        }

        return@withContext routePoints
    }
}
