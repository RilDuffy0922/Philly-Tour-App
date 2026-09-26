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
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.owlhacks.phillytour.location.LocationTracker
import com.owlhacks.phillytour.model.Stop
import com.owlhacks.phillytour.model.Tour
import com.owlhacks.phillytour.session.TourSession
import com.owlhacks.phillytour.speech.TourSpeaker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourScreen(tour: Tour, speaker: TourSpeaker, onBack: () -> Unit) {
    val context = LocalContext.current
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

    LaunchedEffect(locationTracker.location) {
        if (!isTracking) return@LaunchedEffect
        val location = locationTracker.location ?: return@LaunchedEffect
        session.stopArrived(location)?.let { arrive(it) }
    }

    DisposableEffect(tour.id) {
        onDispose {
            locationTracker.stop()
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
                            DropdownMenuItem(
                                text = { Text("Reset progress") },
                                onClick = {
                                    showMenu = false
                                    showResetDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            TourMapView(
                tour = tour,
                visited = session.visited,
                onStopClick = { presentedStop = it },
                modifier = Modifier.fillMaxSize()
            )

            TourStatusPanel(
                session = session,
                isTracking = isTracking,
                hasLocationPermission = hasLocationPermission,
                onNextStopClick = { session.nextStop?.let { presentedStop = it } },
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
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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
private fun TourStatusPanel(
    session: TourSession,
    isTracking: Boolean,
    hasLocationPermission: Boolean,
    onNextStopClick: () -> Unit,
    onToggleTracking: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${session.visited.size} of ${session.tour.stops.size} stops",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.QuestionAnswer, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("${session.correctAnswers}/${session.answers.size}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            val progress = if (session.tour.stops.isEmpty()) {
                0f
            } else {
                session.visited.size / session.tour.stops.size.toFloat()
            }
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())

            if (session.isComplete) {
                Text(
                    "Tour complete! You got ${session.correctAnswers} of ${session.tour.stops.size} trivia questions right.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                session.nextStop?.let { next ->
                    TextButton(onClick = onNextStopClick) {
                        Text("Next: ${next.name}")
                    }
                }
            }

            if (!hasLocationPermission) {
                Text(
                    "Location is off, so stops won't start automatically. Tap a stop on the map to play it, or grant location access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!session.isComplete) {
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
