package com.owlhacks.phillytour.model

import com.google.android.gms.maps.model.LatLng

data class Stop(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double, // meters (geofence radius)
    val narrationScript: String,
    val funFacts: List<String>?,
    val trivia: TriviaQuestion
) {
    val latLng: LatLng
        get() = LatLng(latitude, longitude)
}

data class TriviaQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)
