package com.owlhacks.phillytour.speech

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Reads stop narration aloud with on-device text-to-speech, ducking any audio that's playing. */
class TourSpeaker(context: Context) {
    private var tts: TextToSpeech? = null
    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { }

    /** The text currently being read aloud, if any. */
    var currentText by mutableStateOf<String?>(null)
        private set

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.95f)
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                currentText = null
                abandonFocus()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                currentText = null
                abandonFocus()
            }
        })
    }

    fun isSpeaking(text: String): Boolean = currentText == text

    fun speak(text: String) {
        requestFocus()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TourNarration")
        currentText = text
    }

    fun stop() {
        tts?.stop()
        currentText = null
        abandonFocus()
    }

    @Suppress("DEPRECATION")
    private fun requestFocus() {
        try {
            audioManager?.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        } catch (e: Exception) {
            // Ducking is a nice-to-have; narration still plays without it.
        }
    }

    @Suppress("DEPRECATION")
    private fun abandonFocus() {
        try {
            audioManager?.abandonAudioFocus(focusChangeListener)
        } catch (e: Exception) {
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
