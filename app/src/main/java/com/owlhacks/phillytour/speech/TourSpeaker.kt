package com.owlhacks.phillytour.speech

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.owlhacks.phillytour.data.Secrets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * Reads text aloud, ducking any audio that's playing. Uses the ElevenLabs voice when a key is
 * configured, and falls back to on-device text-to-speech so narration always works.
 */
class TourSpeaker(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null
    private var loadingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { }

    private val apiKey: String? = Secrets.elevenLabsApiKey
    private val defaultVoiceId: String = Secrets.elevenLabsVoiceId
    private val prefs = context.applicationContext.getSharedPreferences("waterway_tours", Context.MODE_PRIVATE)

    /** The text currently being read aloud (or fetched for reading), if any. */
    var currentText by mutableStateOf<String?>(null)
        private set

    val hasCloudVoice: Boolean get() = apiKey != null

    /** The voice the rider picked in Settings, or the default. */
    val voiceId: String get() = prefs.getString(OtterVoice.STORAGE_KEY, null) ?: defaultVoiceId

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
                finish()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                finish()
            }
        })
    }

    fun isSpeaking(text: String): Boolean = currentText == text

    /** Speaks [text]. When [systemVoiceFallback] is false and no ElevenLabs voice is available, stays silent. */
    fun speak(text: String, systemVoiceFallback: Boolean = true, voiceIdOverride: String? = null) {
        cancelCurrent()
        currentText = text

        val key = apiKey
        if (key == null) {
            if (systemVoiceFallback) speakWithSystemVoice(text) else currentText = null
            return
        }

        val eleven = ElevenLabsClient(key, voiceIdOverride ?: voiceId, context)
        loadingJob = scope.launch {
            try {
                val file = eleven.speech(text)
                play(file)
            } catch (e: Exception) {
                if (systemVoiceFallback) speakWithSystemVoice(text) else finish()
            }
        }
    }

    fun stop() {
        cancelCurrent()
        finish()
    }

    fun shutdown() {
        cancelCurrent()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun cancelCurrent() {
        loadingJob?.cancel()
        loadingJob = null
        tts?.stop()
        player?.release()
        player = null
    }

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

    private fun abandonFocus() {
        try {
            audioManager?.abandonAudioFocus(focusChangeListener)
        } catch (e: Exception) {
        }
    }

    private fun play(file: File) {
        requestFocus()
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        mediaPlayer.setOnPreparedListener { it.start() }
        mediaPlayer.setOnCompletionListener {
            player = null
            finish()
        }
        mediaPlayer.setOnErrorListener { _, _, _ ->
            player = null
            finish()
            true
        }
        mediaPlayer.setDataSource(file.absolutePath)
        mediaPlayer.prepareAsync()
    }

    private fun speakWithSystemVoice(text: String) {
        requestFocus()
        val utterance = TextToSpeech.QUEUE_FLUSH
        tts?.speak(text, utterance, null, "TourNarration")
    }

    private fun finish() {
        currentText = null
        abandonFocus()
    }
}
