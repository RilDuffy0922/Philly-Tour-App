package com.owlhacks.phillytour.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.owlhacks.phillytour.location.LocationTracker
import com.owlhacks.phillytour.model.Stop
import com.owlhacks.phillytour.model.Tour
import com.owlhacks.phillytour.otter.OtterDialogue
import com.owlhacks.phillytour.session.TourSession
import com.owlhacks.phillytour.speech.TourSpeaker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourScreen(tour: Tour, speaker: TourSpeaker, otterDialogue: OtterDialogue, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember(tour.id) { TourSession(context, tour) }
    val locationTracker = remember(tour.id) { LocationTracker(context) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isTracking by remember { mutableStateOf(false) }
    var presentedStop by remember { mutableStateOf<Stop?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showPlaceNearMenu by remember { mutableStateOf(false) }
    var pickingLocation by remember { mutableStateOf(false) }
    var demoJob by remember { mutableStateOf<Job?>(null) }
    val isDemoRunning = demoJob != null

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocationPermission = granted
        if (granted) {
            locationTracker.start()
            isTracking = true
        }
    }

    fun arrive(stop: Stop) {
        session.markVisited(stop)
        presentedStop = stop
        speaker.speak(stop.narrationScript)
    }

    fun stopDemo() {
        demoJob?.cancel()
        demoJob = null
        speaker.stop()
    }

    fun placeNear(stop: Stop) {
        stopDemo()
        locationTracker.setManual(LatLng(stop.latitude - 0.0018, stop.longitude))
        isTracking = true
    }

    fun startDemo() {
        stopDemo()
        speaker.stop()
        session.reset()
        demoJob = scope.launch {
            val first = tour.stops.first()
            var here = LatLng(first.latitude - 0.003, first.longitude - 0.002)
            locationTracker.setManual(here)
            isTracking = true
            delay(2500)

            while (isActive) {
                val next = session.nextStop ?: break
                val start = here
                val steps = 40
                for (step in 1..steps) {
                    if (!isActive || presentedStop != null) break
                    val t = step / steps.toDouble()
                    here = LatLng(
                        start.latitude + (next.latitude - start.latitude) * t,
                        start.longitude + (next.longitude - start.longitude) * t
                    )
                    locationTracker.setManual(here)
                    delay(200)
                }
                if (!isActive) break

                delay(1000)
                var waited = 0.0
                while (speaker.currentText != null && waited < 60 && isActive) {
                    delay(500)
                    waited += 0.5
                }
                delay(2000)
                presentedStop = null
                delay(1000)
            }
            demoJob = null
        }
    }

    LaunchedEffect(locationTracker.location) {
        val location = locationTracker.location ?: return@LaunchedEffect
        if (session.needsOrdering) session.orderStops(location)
        if (!isTracking) return@LaunchedEffect
        session.stopArrived(location)?.let { arrive(it) }
    }

    DisposableEffect(tour.id) {
        onDispose {
            demoJob?.cancel()
            locationTracker.stop()
            locationTracker.clearManual()
            speaker.stop()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tour.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (isDemoRunning) {
                                DropdownMenuItem(
                                    text = { Text("Stop demo") },
                                    leadingIcon = { Icon(Icons.Filled.StopCircle, contentDescription = null) },
                                    onClick = { showMenu = false; stopDemo() }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Run a demo") },
                                    leadingIcon = { Icon(Icons.Filled.PlayCircle, contentDescription = null) },
                                    onClick = { showMenu = false; startDemo() }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Tap the map to set my location") },
                                leadingIcon = { Icon(Icons.Filled.TouchApp, contentDescription = null) },
                                onClick = { showMenu = false; stopDemo(); pickingLocation = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Place me near a stop") },
                                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                                onClick = { showMenu = false; showPlaceNearMenu = true }
                            )
                            if (locationTracker.isManual) {
                                DropdownMenuItem(
                                    text = { Text("Use my real location") },
                                    onClick = {
                                        showMenu = false
                                        stopDemo()
                                        locationTracker.clearManual()
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Reset progress") },
                                onClick = { showMenu = false; showResetDialog = true }
                            )
                        }
                        DropdownMenu(expanded = showPlaceNearMenu, onDismissRequest = { showPlaceNearMenu = false }) {
                            session.stops.forEach { stop ->
                                DropdownMenuItem(
                                    text = { Text(stop.name) },
                                    onClick = { showPlaceNearMenu = false; placeNear(stop) }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            TourMapView(
                tour = tour,
                stops = session.stops,
                visited = session.visited,
                skipped = session.skipped,
                manualLocation = locationTracker.manualLocation?.let { LatLng(it.latitude, it.longitude) },
                onStopClick = { presentedStop = it },
                onMapClick = { latLng ->
                    if (pickingLocation) {
                        pickingLocation = false
                        locationTracker.setManual(latLng)
                        isTracking = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            TourBanner(
                pickingLocation = pickingLocation,
                isDemoRunning = isDemoRunning,
                isManual = locationTracker.isManual,
                onCancelPicking = { pickingLocation = false },
                onStopDemo = { stopDemo() },
                onUseRealLocation = { locationTracker.clearManual() },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
            )

            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!session.isComplete) {
                    session.nextStop?.let { next ->
                        OtterGuide(
                            stop = next,
                            isTracking = isTracking,
                            isPaused = presentedStop != null,
                            speaker = speaker,
                            dialogue = otterDialogue,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                TourStatusPanel(
                    session = session,
                    isTracking = isTracking,
                    hasLocationPermission = hasLocationPermission,
                    isManualLocation = locationTracker.isManual,
                    location = locationTracker.location,
                    onNextStopClick = { session.nextStop?.let { presentedStop = it } },
                    onSkipNext = { session.nextStop?.let { session.skip(it) } },
                    onToggleTracking = {
                        if (!hasLocationPermission) {
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        } else if (isTracking) {
                            locationTracker.stop()
                            isTracking = false
                        } else {
                            locationTracker.start()
                            isTracking = true
                        }
                    }
                )
            }
        }
    }

    presentedStop?.let { stop ->
        StopSheet(
            stop = stop,
            session = session,
            speaker = speaker,
            onArrive = { arrive(stop) },
            onDismiss = { presentedStop = null }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset progress for this tour?") },
            confirmButton = {
                TextButton(onClick = {
                    session.reset()
                    showResetDialog = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TourBanner(
    pickingLocation: Boolean,
    isDemoRunning: Boolean,
    isManual: Boolean,
    onCancelPicking: () -> Unit,
    onStopDemo: () -> Unit,
    onUseRealLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (text, buttonTitle, action) = when {
        pickingLocation -> Triple("Tap the map to set where you are", "Cancel", onCancelPicking)
        isDemoRunning -> Triple("Demo running", "Stop", onStopDemo)
        isManual -> Triple("Using a location you set", "Use real", onUseRealLocation)
        else -> return
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            OutlinedButton(onClick = action) { Text(buttonTitle) }
        }
    }
}

@Composable
private fun TourStatusPanel(
    session: TourSession,
    isTracking: Boolean,
    hasLocationPermission: Boolean,
    isManualLocation: Boolean,
    location: android.location.Location?,
    onNextStopClick: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleTracking: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${session.visited.size} of ${session.stops.size} stops",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (session.skipped.isNotEmpty()) {
                        Text(
                            text = "· ${session.skipped.size} skipped",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.QuestionAnswer, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("${session.correctAnswers}/${session.answers.size}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            val progress = if (session.stops.isEmpty()) {
                0f
            } else {
                (session.visited.size + session.skipped.size) / session.stops.size.toFloat()
            }
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())

            if (session.isComplete) {
                Text(
                    "Tour complete! You got ${session.correctAnswers} of ${session.answers.size} trivia questions right.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                session.nextStop?.let { next ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onNextStopClick, modifier = Modifier.weight(1f)) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("Next: ${next.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                location?.let { here ->
                                    val result = FloatArray(1)
                                    android.location.Location.distanceBetween(
                                        here.latitude, here.longitude, next.latitude, next.longitude, result
                                    )
                                    Text(
                                        text = formatDistance(result[0]),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        OutlinedButton(onClick = onSkipNext) {
                            Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Skip")
                        }
                    }
                }
            }

            if (!hasLocationPermission && !isManualLocation) {
                Text(
                    "Location is off, so stops won't start automatically. Tap a stop on the map to play it, " +
                        "set a location from the ⋮ menu, or grant location access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!session.isComplete) {
                Button(onClick = onToggleTracking, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = if (isTracking) Icons.Filled.Pause else Icons.Filled.LocationOn,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isTracking) "Pause tour" else "Start tour")
                }
            }
        }
    }
}

private fun formatDistance(meters: Float): String {
    return if (meters >= 1000f) "%.1f km".format(meters / 1000f) else "${meters.toInt()} m"
}
