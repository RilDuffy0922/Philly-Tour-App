package com.owlhacks.phillytour.data

import android.content.Context
import android.util.Log
import com.owlhacks.phillytour.model.Stop
import com.owlhacks.phillytour.model.Tour
import com.owlhacks.phillytour.model.TravelMode
import com.owlhacks.phillytour.model.TriviaQuestion
import org.json.JSONArray

/** Loads the bundled tours from assets/tours.json. Add a new city there — no code changes needed. */
object TourLibrary {
    private const val TAG = "TourLibrary"

    fun load(context: Context): List<Tour> {
        return try {
            val json = context.assets.open("tours.json").bufferedReader().use { it.readText() }
            parseTours(JSONArray(json))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load tours.json", e)
            emptyList()
        }
    }

    private fun parseTours(array: JSONArray): List<Tour> {
        val tours = mutableListOf<Tour>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            tours += Tour(
                id = obj.getString("id"),
                city = obj.getString("city"),
                waterway = obj.getString("waterway"),
                name = obj.getString("name"),
                summary = obj.getString("summary"),
                mode = TravelMode.fromJson(obj.getString("mode")),
                stops = parseStops(obj.getJSONArray("stops"))
            )
        }
        return tours
    }

    private fun parseStops(array: JSONArray): List<Stop> {
        val stops = mutableListOf<Stop>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val triviaObj = obj.getJSONObject("trivia")
            val optionsArray = triviaObj.getJSONArray("options")
            val options = (0 until optionsArray.length()).map { optionsArray.getString(it) }
            val funFactsArray = obj.optJSONArray("funFacts")
            val funFacts = funFactsArray?.let { array -> (0 until array.length()).map { array.getString(it) } }
            stops += Stop(
                id = obj.getString("id"),
                name = obj.getString("name"),
                latitude = obj.getDouble("latitude"),
                longitude = obj.getDouble("longitude"),
                radius = obj.getDouble("radius"),
                narrationScript = obj.getString("narrationScript"),
                funFacts = funFacts,
                trivia = TriviaQuestion(
                    question = triviaObj.getString("question"),
                    options = options,
                    correctIndex = triviaObj.getInt("correctIndex")
                )
            )
        }
        return stops
    }
}
