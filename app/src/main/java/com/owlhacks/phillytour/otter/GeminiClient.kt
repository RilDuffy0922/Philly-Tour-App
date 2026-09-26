package com.owlhacks.phillytour.otter

import com.owlhacks.phillytour.model.Stop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Writes the otter's spoken lines with the Gemini API, grounded in each stop's own facts. */
class GeminiClient(private val apiKey: String, private val model: String) {

    class GeminiException(message: String) : Exception(message)

    /** Returns several fresh lines about [stop] — a mix of history and things to do — ready to be read aloud. */
    suspend fun funFactLines(stop: Stop, count: Int = 4): List<String> = withContext(Dispatchers.IO) {
        var source = "Stop: ${stop.name}\nBackground: ${stop.narrationScript}"
        val facts = stop.funFacts
        if (!facts.isNullOrEmpty()) {
            source += "\nKnown history/fun facts:\n" + facts.joinToString("\n") { "- $it" }
        }
        val activities = stop.thingsToDo
        if (!activities.isNullOrEmpty()) {
            source += "\nKnown things to do here:\n" + activities.joinToString("\n") { "- $it" }
        }
        val prompt = "$source\n\nWrite $count different lines about this stop as the otter. Mix it up: " +
            "some lines should share a history or fun fact, and some should suggest a specific thing the " +
            "rider could do or look for right here. Each line should cover a different detail or suggestion."

        val body = JSONObject().apply {
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_INSTRUCTION))))
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", 1.0)
                    put("responseMimeType", "application/json")
                    put(
                        "responseSchema",
                        JSONObject().apply {
                            put("type", "ARRAY")
                            put("items", JSONObject().put("type", "STRING"))
                        }
                    )
                }
            )
        }

        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
        val responseText = send(url, body.toString())

        val reply = JSONObject(responseText)
        val candidates = reply.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) throw GeminiException("empty response")
        val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        var text: String? = null
        for (i in 0 until parts.length()) {
            val partText = parts.getJSONObject(i).optString("text", "")
            if (partText.isNotEmpty()) {
                text = partText
                break
            }
        }
        if (text == null) throw GeminiException("empty response")

        val linesArray = JSONArray(text)
        val cleaned = (0 until linesArray.length())
            .map { linesArray.getString(it).trim() }
            .filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) throw GeminiException("empty response")
        cleaned
    }

    /** Sends the request, retrying a few times: the free tier occasionally answers 404, 429 or 503 and then succeeds. */
    private suspend fun send(url: URL, body: String, attempts: Int = 4): String {
        var lastStatus = 0
        for (attempt in 0 until attempts) {
            if (attempt > 0) delay(attempt * 2000L)
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 20_000
                connection.readTimeout = 20_000
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray()) }
                val status = connection.responseCode
                if (status == 200) {
                    return connection.inputStream.bufferedReader().use { it.readText() }
                }
                lastStatus = status
                if (status == 400 || status == 401 || status == 403) break
            } catch (e: Exception) {
                lastStatus = -1
            } finally {
                connection.disconnect()
            }
        }
        throw GeminiException("bad response: $lastStatus")
    }

    companion object {
        private const val SYSTEM_INSTRUCTION = "You are the cheerful otter mascot of a walking and biking tour " +
            "of Philadelphia's rivers. Speak in first person to the rider, in a warm, light-hearted, playful " +
            "voice. Each line is one or two short sentences, at most 30 words, and works when read aloud. " +
            "Use at most one otter or water pun across the whole set. Give a mix of history/fun facts and " +
            "concrete suggestions for things to do or look for right at this stop, not just history. Only use " +
            "facts and suggestions stated in the source material you are given. Do not mention any place, " +
            "ship, landmark, person, date, or number that is not in the source material, even if you know it " +
            "is true. Do not use emoji, hashtags, or stage directions."
    }
}
