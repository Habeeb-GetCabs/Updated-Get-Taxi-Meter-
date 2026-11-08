package com.example.util

import android.util.Log
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
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    /**
     * Searches places, streets, landmarks, and business names using Photon (OpenStreetMap data),
     * biased towards the driver's current coordinates.
     *
     * - No API key required.
     * - Free under OpenStreetMap ODbL.
     * - Specifically designed for autocomplete.
     */
    suspend fun searchPlaces(
        query: String,
        userLat: Double?,
        userLon: Double?
    ): List<SearchPlaceResult> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()

        val results = mutableListOf<SearchPlaceResult>()
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
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val features = json.optJSONArray("features") ?: return@withContext emptyList()

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

                    val details = mutableListOf<String>()
                    val street = properties.optString("street")
                    val district = properties.optString("district")
                    val city = properties.optString("city")
                    val state = properties.optString("state")
                    if (street.isNotBlank() && street != name) details.add(street)
                    if (district.isNotBlank()) details.add(district)
                    if (city.isNotBlank()) details.add(city)
                    if (state.isNotBlank() && details.size < 2) details.add(state)

                    results.add(
                        SearchPlaceResult(
                            name = name,
                            description = details.joinToString(", ").ifBlank { "$lat, $lon" },
                            latitude = lat,
                            longitude = lon
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Destination search error for '$query': ${e.message}")
        }
        return@withContext results
    }

    /**
     * Calculates a suggested driving route geometry from vehicle location to selected destination
     * using the public Open Source Routing Machine (OSRM) driving API.
     *
     * CRITICAL: This is solely for visual driver guidance on the map.
     * It is NEVER used for fare calculation or billing!
     */
    suspend fun fetchSuggestedDrivingRoute(
        startLat: Double,
        startLon: Double,
        destLat: Double,
        destLon: Double
    ): List<Pair<Double, Double>> = withContext(Dispatchers.IO) {
        val routePoints = mutableListOf<Pair<Double, Double>>()
        try {
            val url = "https://router.project-osrm.org/route/v1/driving/$startLon,$startLat;$destLon,$destLat?overview=full&geometries=geojson"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "GetTaxiApp/1.0 (Android)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val routes = json.optJSONArray("routes") ?: return@withContext emptyList()
                if (routes.length() == 0) return@withContext emptyList()
                val firstRoute = routes.getJSONObject(0)
                val geometry = firstRoute.optJSONObject("geometry") ?: return@withContext emptyList()
                val coordinates = geometry.optJSONArray("coordinates") ?: return@withContext emptyList()

                for (i in 0 until coordinates.length()) {
                    val coord = coordinates.optJSONArray(i) ?: continue
                    if (coord.length() >= 2) {
                        val lon = coord.getDouble(0)
                        val lat = coord.getDouble(1)
                        routePoints.add(Pair(lat, lon))
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Suggested route calculation error: ${e.message}")
        }
        return@withContext routePoints
    }
}
