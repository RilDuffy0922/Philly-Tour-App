package com.owlhacks.phillytour.speech

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Turns text into speech with ElevenLabs. Audio is cached on disk so each line is only generated once. */
class ElevenLabsClient(private val apiKey: String, private val voiceId: String, context: Context) {

    class ElevenLabsException(message: String) : Exception(message)

    /** Fast model tuned for real-time use. */
    private val modelId = "eleven_flash_v2_5"

    private val cacheDir: File = File(context.applicationContext.cacheDir, "otter-voice").apply { mkdirs() }

    /** MP3 audio file for [text], from the cache if we've already generated it. */
    suspend fun speech(text: String): File = withContext(Dispatchers.IO) {
        val cacheFile = File(cacheDir, "${cacheKey(text)}.mp3")
        if (cacheFile.exists() && cacheFile.length() > 0) return@withContext cacheFile

        val body = JSONObject().apply {
            put("text", text)
            put("model_id", modelId)
            put(
                "voice_settings",
                JSONObject().apply {
                    put("stability", 0.35)
                    put("similarity_boost", 0.75)
                    put("speed", 1.05)
                }
            )
        }

        val url = URL("https://api.elevenlabs.io/v1/text-to-speech/$voiceId?output_format=mp3_44100_128")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "audio/mpeg")
            connection.setRequestProperty("xi-api-key", apiKey)
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toString().toByteArray()) }

            val status = connection.responseCode
            if (status != 200) throw ElevenLabsException("bad response: $status")

            val temp = File(cacheDir, "${cacheKey(text)}.tmp")
            connection.inputStream.use { input -> temp.outputStream().use { output -> input.copyTo(output) } }
            temp.renameTo(cacheFile)
            cacheFile
        } finally {
            connection.disconnect()
        }
    }

    private fun cacheKey(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest("$voiceId|$modelId|$text".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
