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

/**
 * Progress through one tour: which stops were reached and how trivia went. Saved to SharedPreferences.
 *
 * The "next" stop is not a fixed sequence — it's always whichever remaining stop is currently closest
 * to the rider, recalculated live as [updateLocation] is called, so the route adapts as you move.
 */
class TourSession(context: Context, val tour: Tour) {
    private val prefs = context.applicationContext.getSharedPreferences("waterway_tours", Context.MODE_PRIVATE)
    private val prefsKey = "progress_${tour.id}"

    /** Fixes worse than this are too fuzzy to trigger a geofence. */
    private val maxUsableAccuracy = 100f

    var visited by mutableStateOf<Set<String>>(emptySet())
        private set
    var skipped by mutableStateOf<Set<String>>(emptySet())
        private set
    var answers by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    /** The rider's last known location (device, manual, or demo), used to pick the closest remaining stop. */
    var currentLocation by mutableStateOf<Location?>(null)
        private set

    init {
        load()
    }

    fun updateLocation(location: Location) {
        currentLocation = location
    }

    /** The remaining stop closest to [currentLocation], or the first remaining stop if location isn't known yet. */
    val nextStop: Stop?
        get() {
            val remaining = tour.stops.filter { it.id !in visited && it.id !in skipped }
            if (remaining.isEmpty()) return null
            val location = currentLocation ?: return remaining.first()
            return remaining.minByOrNull { distanceMeters(location, it) }
        }

    val isComplete: Boolean
        get() = nextStop == null

    val correctAnswers: Int
        get() = tour.stops.count { answers[it.id] == it.trivia.correctIndex }

    /** The stop's fixed position in the authored tour, for its map pin — unrelated to visiting order. */
    fun number(of: Stop): Int = tour.stops.indexOf(of) + 1

    /** The closest unvisited, unskipped stop whose geofence contains [location], if any. */
    fun stopArrived(location: Location): Stop? {
        if (location.accuracy > maxUsableAccuracy) return null
        return tour.stops
            .filter { it.id !in visited && it.id !in skipped }
            .map { stop -> stop to distanceMeters(location, stop) }
            .filter { (stop, distance) -> distance <= stop.radius }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    fun markVisited(stop: Stop) {
        visited = visited + stop.id
        skipped = skipped - stop.id
        save()
    }

    /** Drops a stop the rider isn't interested in; it no longer counts as "next" or triggers on arrival. */
    fun skip(stop: Stop) {
        if (stop.id in visited) return
        skipped = skipped + stop.id
        save()
    }

    fun answer(optionIndex: Int, stop: Stop) {
        if (answers.containsKey(stop.id)) return
        answers = answers + (stop.id to optionIndex)
        save()
    }

    fun reset() {
        visited = emptySet()
        skipped = emptySet()
        answers = emptyMap()
        save()
    }

    private fun distanceMeters(location: Location, stop: Stop): Float {
        val result = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, stop.latitude, stop.longitude, result)
        return result[0]
    }

    private fun load() {
        val raw = prefs.getString(prefsKey, null) ?: return
        try {
            val obj = JSONObject(raw)
            obj.optJSONArray("visited")?.let { array ->
                visited = (0 until array.length()).map { array.getString(it) }.toSet()
            }
            obj.optJSONArray("skipped")?.let { array ->
                skipped = (0 until array.length()).map { array.getString(it) }.toSet()
            }
            obj.optJSONObject("answers")?.let { answersObj ->
                val map = mutableMapOf<String, Int>()
                answersObj.keys().forEach { key -> map[key] = answersObj.getInt(key) }
                answers = map
            }
        } catch (e: Exception) {
            // Corrupt or missing progress; start fresh.
        }
    }

    private fun save() {
        val obj = JSONObject()
        obj.put("visited", JSONArray(visited.toList()))
        obj.put("skipped", JSONArray(skipped.toList()))
        val answersObj = JSONObject()
        answers.forEach { (id, index) -> answersObj.put(id, index) }
        obj.put("answers", answersObj)
        prefs.edit().putString(prefsKey, obj.toString()).apply()
    }
}
