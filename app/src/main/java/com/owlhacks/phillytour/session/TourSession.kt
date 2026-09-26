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
    var skipped by mutableStateOf<Set<String>>(emptySet())
        private set
    var answers by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    /** Stops in tour order. Starts as the authored order, then is re-sorted from the rider's position. */
    var stops by mutableStateOf(tour.stops)
        private set
    private var isOrdered = false

    init {
        load()
    }

    /** True until the stops have been sorted from the rider's location. */
    val needsOrdering: Boolean
        get() = !isOrdered && visited.isEmpty() && skipped.isEmpty()

    /** Makes the stop closest to [location] the first stop, then chains each next-closest stop after it. */
    fun orderStops(location: Location) {
        if (!needsOrdering) return
        val remaining = tour.stops.toMutableList()
        val ordered = mutableListOf<Stop>()
        var current = location
        while (remaining.isNotEmpty()) {
            val nearest = remaining.minByOrNull { distanceMeters(current, it) } ?: break
            remaining.remove(nearest)
            ordered.add(nearest)
            current = stopLocation(nearest)
        }
        stops = ordered
        isOrdered = true
        save()
    }

    val nextStop: Stop?
        get() = stops.firstOrNull { it.id !in visited && it.id !in skipped }

    val isComplete: Boolean
        get() = nextStop == null

    val correctAnswers: Int
        get() = tour.stops.count { answers[it.id] == it.trivia.correctIndex }

    fun number(of: Stop): Int = stops.indexOf(of) + 1

    /** The closest unvisited, unskipped stop whose geofence contains [location], if any. */
    fun stopArrived(location: Location): Stop? {
        if (location.accuracy > maxUsableAccuracy) return null
        return stops
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
        stops = tour.stops
        isOrdered = false
        save()
    }

    private fun distanceMeters(location: Location, stop: Stop): Float {
        val result = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, stop.latitude, stop.longitude, result)
        return result[0]
    }

    private fun stopLocation(stop: Stop): Location = Location("stop").apply {
        latitude = stop.latitude
        longitude = stop.longitude
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
            obj.optJSONArray("order")?.let { orderArray ->
                val byId = tour.stops.associateBy { it.id }
                val restored = (0 until orderArray.length()).mapNotNull { byId[orderArray.getString(it)] }
                if (restored.size == tour.stops.size) {
                    stops = restored
                    isOrdered = true
                }
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
        if (isOrdered) obj.put("order", JSONArray(stops.map { it.id }))
        prefs.edit().putString(prefsKey, obj.toString()).apply()
    }
}
