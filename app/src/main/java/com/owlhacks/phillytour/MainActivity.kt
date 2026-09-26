package com.owlhacks.phillytour

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.owlhacks.phillytour.data.TourLibrary
import com.owlhacks.phillytour.model.Tour
import com.owlhacks.phillytour.speech.TourSpeaker
import com.owlhacks.phillytour.ui.TourListScreen
import com.owlhacks.phillytour.ui.TourScreen
import com.owlhacks.phillytour.ui.theme.PhillyTourTheme

class MainActivity : ComponentActivity() {
    private lateinit var speaker: TourSpeaker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speaker = TourSpeaker(this)

        setContent {
            PhillyTourTheme {
                val tours = remember { TourLibrary.load(applicationContext) }
                var selectedTour by remember { mutableStateOf<Tour?>(null) }
                val tour = selectedTour

                if (tour == null) {
                    TourListScreen(tours = tours, onTourSelected = { selectedTour = it })
                } else {
                    TourScreen(tour = tour, speaker = speaker, onBack = { selectedTour = null })
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speaker.shutdown()
    }
}
