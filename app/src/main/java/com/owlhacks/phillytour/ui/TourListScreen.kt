package com.owlhacks.phillytour.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.owlhacks.phillytour.model.TravelMode
import com.owlhacks.phillytour.model.Tour
import com.owlhacks.phillytour.speech.TourSpeaker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourListScreen(tours: List<Tour>, speaker: TourSpeaker, onTourSelected: (Tour) -> Unit) {
    val cities = tours.map { it.city }.distinct()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("waterway_tours", Context.MODE_PRIVATE) }
    var showingTutorial by remember { mutableStateOf(false) }
    var showingSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!prefs.getBoolean("hasSeenTutorial", false)) showingTutorial = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Waterway Tours") },
                navigationIcon = {
                    IconButton(onClick = { showingTutorial = true }) {
                        Icon(Icons.Filled.QuestionMark, contentDescription = "How it works")
                    }
                },
                actions = {
                    IconButton(onClick = { showingSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        if (tours.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No tours yet. Add tours to assets/tours.json.")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(cities) { city ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = city,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column {
                            val cityTours = tours.filter { it.city == city }
                            cityTours.forEachIndexed { index, tour ->
                                if (index > 0) Divider()
                                TourRow(tour = tour, onClick = { onTourSelected(tour) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showingTutorial) {
        Dialog(
            onDismissRequest = {
                showingTutorial = false
                prefs.edit().putBoolean("hasSeenTutorial", true).apply()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            TutorialScreen(onDone = {
                showingTutorial = false
                prefs.edit().putBoolean("hasSeenTutorial", true).apply()
            })
        }
    }

    if (showingSettings) {
        Dialog(
            onDismissRequest = { showingSettings = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            SettingsScreen(
                speaker = speaker,
                onReplayTutorial = {
                    showingSettings = false
                    showingTutorial = true
                },
                onDone = { showingSettings = false }
            )
        }
    }
}

@Composable
private fun TourRow(tour: Tour, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tour.mode.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = tour.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = tour.waterway,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val miles = tour.lengthMeters / 1609.34
            Text(
                text = "${tour.stops.size} stops · ${String.format("%.1f", miles)} mi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

fun TravelMode.icon(): ImageVector = when (this) {
    TravelMode.WALK -> Icons.Filled.DirectionsWalk
    TravelMode.BIKE -> Icons.Filled.DirectionsBike
    TravelMode.BOAT -> Icons.Filled.DirectionsBoat
}
