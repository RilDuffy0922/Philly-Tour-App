package com.owlhacks.phillytour.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.owlhacks.phillytour.data.TourData
import com.owlhacks.phillytour.model.Stop

@Composable
fun GoogleMapView(
    stops: List<Stop>,
    selectedStop: Stop?,
    onStopSelected: (Stop) -> Unit,
    onMapClick: () -> Unit,
    isStationary: Boolean = true,
    centerCoordinate: LatLng = TourData.schuylkillCenter,
    zoomLevel: Float = TourData.defaultZoom,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerCoordinate, zoomLevel)
    }

    // When stationary is true, lock user from panning, zooming, rotating the map
    val uiSettings = remember(isStationary) {
        MapUiSettings(
            scrollGesturesEnabled = !isStationary,
            zoomGesturesEnabled = !isStationary,
            tiltGesturesEnabled = !isStationary,
            rotationGesturesEnabled = !isStationary,
            scrollGesturesEnabledDuringRotateOrZoom = !isStationary,
            zoomControlsEnabled = false,
            mapToolbarEnabled = false
        )
    }

    val mapProperties = remember {
        MapProperties(
            isBuildingEnabled = true
        )
    }

    // Reset camera to center if stationary mode is toggled back on
    LaunchedEffect(isStationary) {
        if (isStationary) {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(centerCoordinate, zoomLevel)
                ),
                600
            )
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings,
        properties = mapProperties,
        onMapClick = { onMapClick() }
    ) {
        stops.forEach { stop ->
            val isSelected = selectedStop?.id == stop.id

            // Geofence Circle
            Circle(
                center = stop.latLng,
                radius = stop.radius,
                fillColor = Color(0x332563EB),
                strokeColor = Color(0xAA2563EB),
                strokeWidth = 4f
            )

            // Tour Stop Marker
            Marker(
                state = rememberMarkerState(position = stop.latLng),
                title = stop.name,
                snippet = "Tap to explore audio narration & trivia",
                icon = if (isSelected) {
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                } else {
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                },
                onClick = {
                    onStopSelected(stop)
                    true
                }
            )
        }
    }
}
