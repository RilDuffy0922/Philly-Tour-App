package com.owlhacks.phillytour.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/** Wraps FusedLocationProviderClient. Call [start] only after location permission has been granted. */
class LocationTracker(context: Context) {
    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    var location by mutableStateOf<Location?>(null)
        private set

    private var isRequesting = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location = it }
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRequesting) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateDistanceMeters(5f)
            .build()
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        isRequesting = true
    }

    fun stop() {
        client.removeLocationUpdates(callback)
        isRequesting = false
    }
}
