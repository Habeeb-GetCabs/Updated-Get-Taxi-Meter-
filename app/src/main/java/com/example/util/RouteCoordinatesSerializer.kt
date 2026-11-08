package com.example.util

import java.util.Locale

object RouteCoordinatesSerializer {

    fun serialize(points: List<Pair<Double, Double>>): String {
        if (points.isEmpty()) return ""
        val sb = StringBuilder("[")
        for (i in points.indices) {
            val pt = points[i]
            sb.append(String.format(Locale.US, "[%.6f,%.6f]", pt.first, pt.second))
            if (i < points.size - 1) sb.append(",")
        }
        sb.append("]")
        return sb.toString()
    }

    fun deserialize(json: String): List<Pair<Double, Double>> {
        val trimmed = json.trim()
        if (trimmed.length < 5 || !trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return emptyList()
        }
        val list = mutableListOf<Pair<Double, Double>>()
        try {
            val regex = Regex("""\[\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*\]""")
            val matches = regex.findAll(trimmed)
            for (match in matches) {
                val lat = match.groupValues[1].toDoubleOrNull()
                val lng = match.groupValues[2].toDoubleOrNull()
                if (lat != null && lng != null) {
                    list.add(Pair(lat, lng))
                }
            }
        } catch (_: Exception) {
            // Safe fallback
        }
        return list
    }
}
