package com.owlhacks.phillytour.data

import com.google.android.gms.maps.model.LatLng
import com.owlhacks.phillytour.model.Stop
import com.owlhacks.phillytour.model.TriviaQuestion

object TourData {
    // Schuylkill River Center Coordinates (Center City / Fairmount corridor)
    val schuylkillCenter = LatLng(39.9605, -75.1830)
    const val defaultZoom: Float = 14.3f

    // Authentic Schuylkill River tour stops in Philadelphia
    val schuylkillStops: List<Stop> = listOf(
        Stop(
            name = "Philadelphia Museum of Art",
            latitude = 39.9656,
            longitude = -75.1810,
            radius = 80.0,
            narrationScript = "Welcome to the Philadelphia Museum of Art, perched high above the Schuylkill River. Famous worldwide for its iconic 72 stone steps immortalized by Rocky Balboa, it houses world-class collections spanning over 2,000 years of creativity.",
            trivia = TriviaQuestion(
                question = "How many stone steps lead up to the Philadelphia Museum of Art's east entrance?",
                options = listOf("54", "72", "88", "100"),
                correctIndex = 1
            )
        ),
        Stop(
            name = "Fairmount Water Works",
            latitude = 39.9678,
            longitude = -75.1831,
            radius = 65.0,
            narrationScript = "Directly behind the Art Museum lies the Fairmount Water Works. Built between 1812 and 1815, this was America's first municipal water system to harness the power of the Schuylkill River with innovative paddlewheels and turbines.",
            trivia = TriviaQuestion(
                question = "What river powered the Fairmount Water Works system?",
                options = listOf("Delaware River", "Schuylkill River", "Susquehanna River", "Lehigh River"),
                correctIndex = 1
            )
        ),
        Stop(
            name = "Boathouse Row",
            latitude = 39.9702,
            longitude = -75.1887,
            radius = 75.0,
            narrationScript = "Looking upriver, you see the iconic Boathouse Row along Kelly Drive. Dating back to the 19th century, these 15 historic boathouses line the Schuylkill and light up the riverbank at night, home to generations of champion collegiate and Olympic rowers.",
            trivia = TriviaQuestion(
                question = "What sport is famously practiced from Boathouse Row on the Schuylkill?",
                options = listOf("Kayaking polo", "Rowing / Crew", "Water skiing", "Sailing"),
                correctIndex = 1
            )
        ),
        Stop(
            name = "Schuylkill Banks Boardwalk",
            latitude = 39.9482,
            longitude = -75.1804,
            radius = 70.0,
            narrationScript = "Heading south down the river, the Schuylkill Banks Boardwalk is a concrete pathway floating directly 50 feet out over the Schuylkill River. It connects Locust Street to the South Street Bridge with stunning skyline views.",
            trivia = TriviaQuestion(
                question = "How far out into the Schuylkill River does the Schuylkill Boardwalk extend?",
                options = listOf("10 feet", "50 feet", "120 feet", "200 feet"),
                correctIndex = 1
            )
        ),
        Stop(
            name = "30th Street River Overlook",
            latitude = 39.9558,
            longitude = -75.1820,
            radius = 60.0,
            narrationScript = "Positioned directly between University City and Center City, this bridge overlook crosses the Schuylkill right next to Amtrak's historic 30th Street Station, built in 1933 with towering 95-foot Corinthian columns.",
            trivia = TriviaQuestion(
                question = "Which major rail hub sits just west of the Schuylkill River at Market Street?",
                options = listOf("Suburban Station", "Jefferson Station", "30th Street Station", "North Philadelphia Station"),
                correctIndex = 2
            )
        ),
        Stop(
            name = "Schuylkill River Park",
            latitude = 39.9470,
            longitude = -75.1818,
            radius = 60.0,
            narrationScript = "Schuylkill River Park offers lush green space, sports courts, and a community garden right along the riverbanks, serving as a hub for cyclists and runners along the Schuylkill River Trail.",
            trivia = TriviaQuestion(
                question = "Which popular trail runs along this park alongside the river?",
                options = listOf("Appalachian Trail", "Schuylkill River Trail", "Wissahickon Gorge Trail", "Delaware Canal Path"),
                correctIndex = 1
            )
        )
    )
}
