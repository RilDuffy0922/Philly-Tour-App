package com.owlhacks.phillytour.otter

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import com.owlhacks.phillytour.data.Secrets
import com.owlhacks.phillytour.model.Stop
import org.json.JSONArray

/**
 * The otter's lines for each stop. Uses Gemini when a key is configured, and falls back to the
 * written fun facts and things-to-do, interleaved so the otter talks about more than just history.
 */
class OtterDialogue(private val context: Context) {
    private val generated = mutableStateMapOf<String, List<String>>()
    private val inFlight = mutableSetOf<String>()
    private val gemini: GeminiClient? = Secrets.geminiApiKey?.let { GeminiClient(it, Secrets.geminiModel) }
    private val prefs by lazy { context.applicationContext.getSharedPreferences("otter_dialogue", Context.MODE_PRIVATE) }

    /** Lines the otter can say about [stop] right now. */
    fun lines(stop: Stop): List<String> = generated[stop.id] ?: fallbackLines(stop)

    /** Alternates fun facts and things-to-do so the otter isn't only talking about history. */
    private fun fallbackLines(stop: Stop): List<String> {
        val facts = stop.funFacts.orEmpty()
        val activities = stop.thingsToDo.orEmpty()
        val merged = mutableListOf<String>()
        val max = maxOf(facts.size, activities.size)
        for (i in 0 until max) {
            facts.getOrNull(i)?.let { merged += it }
            activities.getOrNull(i)?.let { merged += it }
        }
        return merged
    }

    /** Generates (or loads a saved copy of) Gemini lines for [stop]. Quietly does nothing on failure. */
    suspend fun prepare(stop: Stop) {
        val gemini = gemini ?: return
        if (generated.containsKey(stop.id) || stop.id in inFlight) return

        val key = "otter.lines.v3.${stop.id}"
        prefs.getString(key, null)?.let { saved ->
            val array = JSONArray(saved)
            if (array.length() > 0) {
                generated[stop.id] = (0 until array.length()).map { array.getString(it) }
                return
            }
        }

        inFlight.add(stop.id)
        try {
            val lines = gemini.funFactLines(stop)
            generated[stop.id] = lines
            prefs.edit().putString(key, JSONArray(lines).toString()).apply()
        } catch (e: Exception) {
            Log.w("OtterDialogue", "Gemini failed for ${stop.id}: $e")
        } finally {
            inFlight.remove(stop.id)
        }
    }
}
