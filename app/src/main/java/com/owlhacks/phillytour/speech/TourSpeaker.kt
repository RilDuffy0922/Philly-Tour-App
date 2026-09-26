package com.owlhacks.phillytour.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

class TourSpeaker(context: Context) {
    private var tts: TextToSpeech? = null
    var isSpeaking by mutableStateOf(false)
        private set

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.92f)
                tts?.setPitch(1.05f)
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }
        })
    }

    fun toggle(text: String) {
        if (tts?.isSpeaking == true) {
            stop()
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TourNarration")
            isSpeaking = true
        }
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
