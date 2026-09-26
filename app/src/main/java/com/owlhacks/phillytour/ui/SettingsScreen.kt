package com.owlhacks.phillytour.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.owlhacks.phillytour.speech.OtterVoice
import com.owlhacks.phillytour.speech.TourSpeaker

/** Otter voice options and a way to replay the tutorial. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(speaker: TourSpeaker, onReplayTutorial: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("waterway_tours", Context.MODE_PRIVATE) }
    var voiceOn by remember { mutableStateOf(prefs.getBoolean("otterVoiceOn", true)) }
    var voiceId by remember { mutableStateOf(prefs.getString(OtterVoice.STORAGE_KEY, null) ?: speaker.voiceId) }
    var customId by remember { mutableStateOf("") }

    fun choose(id: String) {
        if (id.isBlank()) return
        voiceId = id
        prefs.edit().putString(OtterVoice.STORAGE_KEY, id).apply()
        speaker.speak("Hi! I'm your otter guide. Ready to explore?", systemVoiceFallback = false, voiceIdOverride = id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Filled.ArrowBack, contentDescription = "Done") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (speaker.hasCloudVoice) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Otter speaks aloud")
                    Switch(
                        checked = voiceOn,
                        onCheckedChange = {
                            voiceOn = it
                            prefs.edit().putBoolean("otterVoiceOn", it).apply()
                        }
                    )
                }
                Divider()

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Otter voice",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    OtterVoice.all.forEach { voice ->
                        ListItem(
                            headlineContent = { Text(voice.name) },
                            supportingContent = { Text(voice.blurb) },
                            trailingContent = {
                                if (voiceId == voice.id) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.clickable { choose(voice.id) }
                        )
                    }
                    Text(
                        text = "Tap a voice to hear it and pick it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Divider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Custom voice",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    OutlinedTextField(
                        value = customId,
                        onValueChange = { customId = it },
                        label = { Text("ElevenLabs voice ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { choose(customId.trim()) }, enabled = customId.isNotBlank()) {
                        Text("Use this voice")
                    }
                    Text(
                        text = "Paste any voice ID from your ElevenLabs account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Add your ElevenLabs key to .env to give the otter a voice.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider()
            TextButton(onClick = onReplayTutorial) {
                Text("Replay the tutorial")
            }
        }
    }
}
