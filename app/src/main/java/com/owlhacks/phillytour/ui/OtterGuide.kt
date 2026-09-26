package com.owlhacks.phillytour.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.owlhacks.phillytour.R
import com.owlhacks.phillytour.model.Stop
import com.owlhacks.phillytour.otter.OtterDialogue
import com.owlhacks.phillytour.speech.TourSpeaker
import kotlinx.coroutines.delay

/**
 * The otter mascot. While a tour is running it walks you to the next stop, sharing a fun fact in a
 * speech bubble (written by Gemini, read aloud by ElevenLabs when keys are configured).
 */
@Composable
fun OtterGuide(
    stop: Stop,
    isTracking: Boolean,
    isPaused: Boolean,
    speaker: TourSpeaker,
    dialogue: OtterDialogue,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("waterway_tours", Context.MODE_PRIVATE) }
    var voiceOn by remember { mutableStateOf(prefs.getBoolean("otterVoiceOn", true)) }
    var index by remember(stop.id) { mutableStateOf(0) }
    var restartSignal by remember { mutableStateOf(0) }
    var lines by remember(stop.id) { mutableStateOf(dialogue.lines(stop)) }

    val messages = remember(isTracking, stop.id, lines) {
        if (isTracking) listOf(openingLine(stop)) + lines
        else listOf("Tap Start tour and I'll guide you to ${stop.name}!")
    }
    val message = messages[index % messages.size]
    val speaks = voiceOn && speaker.hasCloudVoice

    LaunchedEffect(stop.id) {
        dialogue.prepare(stop)
        lines = dialogue.lines(stop)
    }

    LaunchedEffect(stop.id, isTracking, isPaused, speaks, restartSignal) {
        if (!isTracking || isPaused) return@LaunchedEffect
        var lastSpoken: String? = null
        try {
            while (true) {
                if (speaks) {
                    val line = messages[index % messages.size]
                    lastSpoken = line
                    speaker.speak(line, systemVoiceFallback = false)
                    var waited = 0
                    while (speaker.isSpeaking(line) && waited < 160) {
                        delay(250)
                        waited++
                    }
                    delay(3000)
                } else {
                    delay(10_000)
                }
                if (messages.size > 1) index = (index + 1) % messages.size
            }
        } finally {
            lastSpoken?.let { if (speaker.isSpeaking(it)) speaker.stop() }
        }
    }

    Row(
        modifier = modifier.clickable {
            if (messages.size > 1) index = (index + 1) % messages.size
            restartSignal++
        },
        verticalAlignment = Alignment.Bottom
    ) {
        Image(
            painter = painterResource(R.drawable.otter),
            contentDescription = null,
            modifier = Modifier.size(56.dp).clip(CircleShape)
        )
        Surface(
            modifier = Modifier.padding(start = 8.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 3.dp,
            shadowElevation = 2.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
                if (speaker.hasCloudVoice) {
                    IconButton(onClick = {
                        voiceOn = !voiceOn
                        prefs.edit().putBoolean("otterVoiceOn", voiceOn).apply()
                    }) {
                        Icon(
                            imageVector = if (voiceOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                            contentDescription = if (voiceOn) "Mute otter voice" else "Unmute otter voice"
                        )
                    }
                }
            }
        }
    }
}

/** A few different ways to announce the next stop, picked per-stop so it's not the same line every time. */
private val OPENING_LINES = listOf(
    "Let's go check out %s!",
    "Next up: %s — this way!",
    "Onward to %s!",
    "Come on, %s is just ahead!",
    "This way to %s!",
)

private fun openingLine(stop: Stop): String {
    val index = Math.floorMod(stop.id.hashCode(), OPENING_LINES.size)
    return OPENING_LINES[index].format(stop.name)
}
