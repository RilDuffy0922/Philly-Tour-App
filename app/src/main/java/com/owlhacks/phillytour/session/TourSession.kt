package com.owlhacks.phillytour.session

import android.content.Context
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.owlhacks.phillytour.model.Stop
import com.owlhacks.phillytour.model.Tour
import org.json.JSONArray
import org.json.JSONObject

/** Progress through one tour: which stops were reached and how trivia went. Saved to SharedPreferences. */
class TourSession(context: Context, val tour: Tour) {
    private val prefs = context.applicationContext.getSharedPreferences("waterway_tours", Context.MODE_PRIVATE)
    private val prefsKey = "progress_${tour.id}"

    /** Fixes worse than this are too fuzzy to trigger a geofence. */
    private val maxUsableAccuracy = 100f

    var visited by mutableStateOf<Set<String>>(emptySet())
        private set
    var answers by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    init {
        load()
    }

    val nextStop: Stop?
        get() = tour.stops.firstOrNull { it.id !in visited }

    val isComplete: Boolean
        get() = visited.size == tour.stops.size

    val correctAnswers: Int
        get() = tour.stops.count { answers[it.id] == it.trivia.correctIndex }

    fun number(of: Stop): Int = tour.stops.indexOf(of) + 1

    /** The closest unvisited stop whose geofence contains [location], if any. */
    fun stopArrived(location: Location): Stop? {
        if (location.accuracy > maxUsableAccuracy) return null
        return tour.stops
            .filter { it.id !in visited }
            .map { stop ->
                val result = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, stop.latitude, stop.longitude, result)
                stop to result[0]
            }
            .filter { (stop, distance) -> distance <= stop.radius }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    fun markVisited(stop: Stop) {
        visited = visited + stop.id
        save()
    }

    fun answer(optionIndex: Int, stop: Stop) {
        if (answers.containsKey(stop.id)) return
        answers = answers + (stop.id to optionIndex)
        save()
    }

    fun reset() {
        visited = emptySet()
        answers = emptyMap()
        save()
    }

    private fun load() {
        val raw = prefs.getString(prefsKey, null) ?: return
        try {
            val obj = JSONObject(raw)
            val visitedArray = obj.getJSONArray("visited")
            visited = (0 until visitedArray.length()).map { visitedArray.getString(it) }.toSet()
            val answersObj = obj.getJSONObject("answers")
            val answersMap = mutableMapOf<String, Int>()
            answersObj.keys().forEach { key -> answersMap[key] = answersObj.getInt(key) }
            answers = answersMap
        } catch (e: Exception) {
            // Corrupt or missing progress; start fresh.
        }
    }

    private fun save() {
        val obj = JSONObject()
        obj.put("visited", JSONArray(visited.toList()))
        val answersObj = JSONObject()
        answers.forEach { (id, index) -> answersObj.put(id, index) }
        obj.put("answers", answersObj)
        prefs.edit().putString(prefsKey, obj.toString()).apply()
    }
}
