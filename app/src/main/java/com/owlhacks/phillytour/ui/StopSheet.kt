package com.owlhacks.phillytour.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop as StopIcon
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlhacks.phillytour.model.Stop
import com.owlhacks.phillytour.session.TourSession
import com.owlhacks.phillytour.speech.TourSpeaker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopSheet(
    stop: Stop,
    session: TourSession,
    speaker: TourSpeaker,
    onArrive: () -> Unit,
    onDismiss: () -> Unit
) {
    val isVisited = stop.id in session.visited
    val isPlaying = speaker.isSpeaking(stop.narrationScript)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${session.number(stop)}. ${stop.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(text = stop.narrationScript, style = MaterialTheme.typography.bodyLarge)

            OutlinedButton(onClick = { if (isPlaying) speaker.stop() else speaker.speak(stop.narrationScript) }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.StopIcon else Icons.Filled.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isPlaying) "Stop narration" else "Play narration")
            }

            Divider()

            if (isVisited) {
                TriviaCard(
                    trivia = stop.trivia,
                    selected = session.answers[stop.id],
                    onSelect = { index -> session.answer(index, stop) }
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Narration starts automatically when you're within ${stop.radius.toInt()} m of this stop. Trivia unlocks when you arrive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onArrive) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("I'm here")
                        }
                        if (stop.id !in session.skipped) {
                            OutlinedButton(onClick = {
                                session.skip(stop)
                                onDismiss()
                            }) {
                                Icon(Icons.Filled.SkipNext, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Skip this stop")
                            }
                        }
                    }
                }
            }
        }
    }
}
