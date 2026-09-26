package com.owlhacks.phillytour.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.owlhacks.phillytour.model.Stop
import com.owlhacks.phillytour.model.Tour

@Composable
fun TourMapView(
    tour: Tour,
    visited: Set<String>,
    skipped: Set<String>,
    nextStop: Stop?,
    currentLocation: LatLng?,
    isManualLocation: Boolean,
    isTracking: Boolean,
    isAtStop: Boolean,
    onStopClick: (Stop) -> Unit,
    onMapClick: (LatLng) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(tour.stops.first().latLng, 14f)
    }

    val uiSettings = remember {
        MapUiSettings(myLocationButtonEnabled = hasLocationPermission, mapToolbarEnabled = false)
    }
    val properties = remember(hasLocationPermission) {
        MapProperties(isMyLocationEnabled = hasLocationPermission)
    }

    // Follow the rider while the tour is running, zooming in close once they've arrived at a stop.
    LaunchedEffect(isTracking, currentLocation, isAtStop) {
        if (isTracking && currentLocation != null) {
            val zoom = if (isAtStop) 18f else 16f
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(currentLocation, zoom), 600)
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings,
        properties = properties,
        onMapClick = onMapClick,
        onMapLoaded = {
            val builder = LatLngBounds.Builder()
            tour.coordinates.forEach { builder.include(it) }
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(builder.build(), 140))
        }
    ) {
        // Before tracking starts: a preview of the whole route. Once tracking, a live line to the
        // closest remaining stop, since stops are visited by proximity rather than a fixed order.
        if (isTracking && currentLocation != null && nextStop != null) {
            Polyline(
                points = listOf(currentLocation, nextStop.latLng),
                color = Color(0xFF0088A3),
                width = 8f,
                pattern = listOf(Dash(30f), Gap(20f))
            )
        } else {
            Polyline(
                points = tour.stops.map { it.latLng },
                color = Color(0xFF0088A3),
                width = 8f,
                pattern = listOf(Dash(30f), Gap(20f))
            )
        }

        tour.stops.forEach { stop ->
            Circle(
                center = stop.latLng,
                radius = stop.radius,
                fillColor = Color(0x1F00A3B8),
                strokeColor = Color(0x5500A3B8),
                strokeWidth = 2f
            )
        }

        tour.stops.forEachIndexed { index, stop ->
            val isVisited = stop.id in visited
            val isSkipped = stop.id in skipped
            val icon = rememberNumberedMarkerIcon(number = index + 1, visited = isVisited, skipped = isSkipped)
            Marker(
                state = rememberMarkerState(position = stop.latLng),
                title = stop.name,
                icon = icon,
                onClick = {
                    onStopClick(stop)
                    true
                }
            )
        }

        // The real device location already shows as the platform's own blue dot; only draw our own
        // marker for a manual/demo location, which the blue dot can't represent.
        if (isManualLocation && currentLocation != null) {
            Marker(
                state = rememberMarkerState(position = currentLocation),
                title = "You",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
            )
        }
    }
}

@Composable
private fun rememberNumberedMarkerIcon(number: Int, visited: Boolean, skipped: Boolean): BitmapDescriptor {
    return remember(number, visited, skipped) {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fillColor = when {
            visited -> AndroidColor.parseColor("#22C55E")
            skipped -> AndroidColor.parseColor("#9CA3AF")
            else -> AndroidColor.parseColor("#00A3B8")
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, fillPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, strokePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val label = when {
            visited -> "✓"
            skipped -> "»"
            else -> number.toString()
        }
        val textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, size / 2f, textY, textPaint)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
