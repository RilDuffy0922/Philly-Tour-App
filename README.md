# Philly Tour App 🔔🗺️ (Android Edition)

An audio tour and trivia guide app centered around the historic **Schuylkill River in Philadelphia**, built for OwlHacks.

This project is now fully converted to **Native Android (Kotlin + Jetpack Compose + Maps Compose)**.

---

## Android Project Architecture & Mapping

| Swift / iOS Original | Android (Kotlin / Jetpack Compose) | Description |
| :--- | :--- | :--- |
| `Stop.swift` | [`Stop.kt`](file:///c:/Users/jhona/Desktop/Coding%20Projects/Owlhacks/Philly%20Tour%20App/app/src/main/java/com/owlhacks/phillytour/model/Stop.kt) | Data models for `Stop` and `TriviaQuestion`. |
| `StopData.swift` | [`TourData.kt`](file:///c:/Users/jhona/Desktop/Coding%20Projects/Owlhacks/Philly%20Tour%20App/app/src/main/java/com/owlhacks/phillytour/data/TourData.kt) | 6 Schuylkill River tour stops & trivia dataset. |
| `GoogleMapView.swift` | [`GoogleMapView.kt`](file:///c:/Users/jhona/Desktop/Coding%20Projects/Owlhacks/Philly%20Tour%20App/app/src/main/java/com/owlhacks/phillytour/ui/GoogleMapView.kt) | **Stationary Map**: Uses Google Maps Compose with locked pan/zoom gestures and geofence circles. |
| `TriviaCardView.swift` | [`TriviaCardView.kt`](file:///c:/Users/jhona/Desktop/Coding%20Projects/Owlhacks/Philly%20Tour%20App/app/src/main/java/com/owlhacks/phillytour/ui/TriviaCardView.kt) | Interactive multiple-choice trivia card with instant correct/incorrect feedback. |
| `StopDetailView.swift` | [`StopDetailSheet.kt`](file:///c:/Users/jhona/Desktop/Coding%20Projects/Owlhacks/Philly%20Tour%20App/app/src/main/java/com/owlhacks/phillytour/ui/StopDetailSheet.kt) | Bottom sheet with stop info, geofence radius, and narration player. |
| *(AVSpeechSynthesizer)* | [`TourSpeaker.kt`](file:///c:/Users/jhona/Desktop/Coding%20Projects/Owlhacks/Philly%20Tour%20App/app/src/main/java/com/owlhacks/phillytour/speech/TourSpeaker.kt) | Android `TextToSpeech` manager to read narration aloud. |
| `ContentView.swift` | [`MainActivity.kt`](file:///c:/Users/jhona/Desktop/Coding%20Projects/Owlhacks/Philly%20Tour%20App/app/src/main/java/com/owlhacks/phillytour/MainActivity.kt) | Main activity combining map, top status bar, and stop carousel. |

---

## Schuylkill River Tour Stops Included

1. **Philadelphia Museum of Art & Rocky Steps** (`39.9656, -75.1810`)
2. **Fairmount Water Works** (`39.9678, -75.1831`)
3. **Boathouse Row** (`39.9702, -75.1887`)
4. **Schuylkill Banks Boardwalk** (`39.9482, -75.1804`)
5. **30th Street River Overlook** (`39.9558, -75.1820`)
6. **Schuylkill River Park** (`39.9470, -75.1818`)

---

## How to Run in Android Studio

1. **Open the Project**:
   - Open **Android Studio**.
   - Select **Open** and choose the `Philly Tour App` folder (`c:\Users\jhona\Desktop\Coding Projects\Owlhacks\Philly Tour App`).
   - Android Studio will automatically sync the Gradle project.

2. **Add Your Google Maps API Key**:
   - Open or create `local.properties` in the root folder:
     ```properties
     MAPS_API_KEY=AIzaSyYourActualGoogleMapsApiKeyHere
     ```
   - Make sure **Maps SDK for Android** is enabled in your [Google Cloud Console](https://console.cloud.google.com/google/maps-apis/).

3. **Run the App**:
   - Click the green **Run (▶)** button in Android Studio to launch on an Android Emulator or connected physical Android device.

---

## Quick Testing on Any Device

You can also open [`web-preview.html`](file:///c:/Users/jhona/Desktop/Coding%20Projects/Owlhacks/Philly%20Tour%20App/web-preview.html) in Chrome or Edge to interact with the exact same stationary Schuylkill River map, audio narration, and trivia right now.
