package com.owlhacks.phillytour.model

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

enum class TravelMode(val label: String) {
    WALK("Walking"),
    BIKE("Biking"),
    BOAT("By boat");

    companion object {
        fun fromJson(value: String): TravelMode = when (value.lowercase()) {
            "bike" -> BIKE
            "boat" -> BOAT
            else -> WALK
        }
    }
}

data class Tour(
    val id: String,
    val city: String,
    val waterway: String,
    val name: String,
    val summary: String,
    val mode: TravelMode,
    val stops: List<Stop>
) {
    val coordinates: List<LatLng>
        get() = stops.map { it.latLng }

    /** Straight-line length of the route between consecutive stops, in meters. */
    val lengthMeters: Double
        get() = stops.zipWithNext().sumOf { (a, b) ->
            val result = FloatArray(1)
            Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
            result[0].toDouble()
        }

    /** Bounds containing every stop; the caller applies padding when moving the camera. */
    fun bounds(): LatLngBounds {
        val builder = LatLngBounds.Builder()
        stops.forEach { builder.include(it.latLng) }
        return builder.build()
    }
}
