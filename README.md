# Philly Tour App — Waterway Tours (Android Edition)

An Android app for self-guided tours along a city's waterways, built with **Kotlin + Jetpack Compose + Google Maps Compose**. Pick a tour, hit **Start tour**, and as you walk, bike, or paddle past each stop the app detects you've arrived (GPS geofence), reads the stop's story aloud, and unlocks a trivia question.

Ships with two Philadelphia tours (Schuylkill Banks, Delaware Waterfront). When a tour starts, the stop closest to you becomes stop 1. An otter mascot walks you between stops, sharing fun facts along the way. Same tour content and feature set as the [iOS version](../../tree/iOS), built independently in Kotlin.

## Running it

Requirements: Android Studio (Koala or newer), a device or emulator running Android 7.0 (API 24) or higher.

1. **Get a Google Maps API key** — create one in the [Google Cloud Console](https://console.cloud.google.com/google/maps-apis/) and enable **Maps SDK for Android**.
2. Copy [`.env.example`](.env.example) to `.env` in the project root and paste your key in, or run `./scripts/set-keys.sh` to be prompted for it (input hidden):
   ```properties
   MAPS_API_KEY=AIzaSyYourActualGoogleMapsApiKeyHere
   ```
   (`.env` is git-ignored, so your key is never committed.)
3. Open the project folder in Android Studio and let Gradle sync.
4. Press **Run (▶)** to launch on an emulator or connected device.

### Testing geofencing

The app itself has a **⋮ menu** on the tour screen for trying it without walking anywhere:
- **Run a demo** walks a pretend rider through the whole tour automatically.
- **Place me near a stop** teleports you about 200 m from any stop so you can walk the rest of the way in.
- **Tap the map to set my location** lets you place yourself anywhere by hand.

You can also use the emulator's own **Extended Controls (⋯) → Location**, enter a stop's coordinates (e.g. `39.9695, -75.1874` for Boathouse Row, stop 1 of the Schuylkill tour), and click **Send**. Or tap any stop's pin and press **I'm here**.

## How it's organized

```
app/src/main/
├── assets/tours.json                         All tour content
├── AndroidManifest.xml                        Permissions + Maps API key
└── java/com/owlhacks/phillytour/
    ├── MainActivity.kt                        Switches between the tour list and an open tour
    ├── model/
    │   ├── Stop.kt                            A stop: coordinate, geofence radius, narration, trivia
    │   └── Tour.kt                            A tour: city, waterway, travel mode, ordered stops
    ├── data/
    │   ├── TourLibrary.kt                     Loads tours from assets/tours.json
    │   └── Secrets.kt                         Reads the optional Gemini/ElevenLabs keys from BuildConfig
    ├── location/
    │   └── LocationTracker.kt                 FusedLocationProviderClient wrapper; supports a manual/demo location
    ├── speech/
    │   ├── TourSpeaker.kt                     Narration: ElevenLabs voice when configured, on-device TTS otherwise
    │   ├── ElevenLabsClient.kt                 Text-to-speech over HTTP, with on-disk audio caching
    │   └── OtterVoice.kt                      The list of pickable ElevenLabs voices
    ├── otter/
    │   ├── GeminiClient.kt                    Writes the otter's fun-fact lines, grounded in each stop's facts
    │   └── OtterDialogue.kt                   Fetches and caches a stop's otter lines
    ├── session/
    │   └── TourSession.kt                     Progress: stop order, visited/skipped stops, trivia score (saved)
    └── ui/
        ├── TourListScreen.kt                  Tours grouped by city; hosts the tutorial and settings dialogs
        ├── TourScreen.kt                      Map with route, stops, geofences, the otter, and the status panel
        ├── TourMapView.kt                     The Google Map: route line, geofence circles, numbered pins
        ├── OtterGuide.kt                      The otter's speech-bubble banner while a tour is running
        ├── StopSheet.kt                       Narration + trivia bottom sheet for one stop
        ├── TriviaCard.kt                      The trivia question card
        ├── TutorialScreen.kt                  First-run walkthrough (also reachable from the "?" button)
        └── SettingsScreen.kt                  Otter voice picker and a way to replay the tutorial
```

## Adding a city or tour

Just edit `app/src/main/assets/tours.json` — no code changes. It uses the exact same format as the iOS app, so a tour written for one platform works on the other unchanged:

```json
{
  "id": "pittsburgh-three-rivers",
  "city": "Pittsburgh",
  "waterway": "Allegheny & Monongahela Rivers",
  "name": "Three Rivers Walk",
  "summary": "One-line description shown to users.",
  "mode": "walk",
  "stops": [
    {
      "id": "point-state-park",
      "name": "Point State Park",
      "latitude": 40.4417,
      "longitude": -80.0122,
      "radius": 100,
      "narrationScript": "What gets read aloud when the user arrives.",
      "funFacts": ["Optional otter-voiced fun facts, used when no Gemini key is configured."],
      "trivia": {
        "question": "…?",
        "options": ["A", "B", "C", "D"],
        "correctIndex": 0
      }
    }
  ]
}
```

- `mode` is `walk`, `bike`, or `boat`.
- `radius` is in meters. Use ~60–100 m for walking, larger (120–200 m) for boats or wide spots where GPS is fuzzy.
- `id`s must stay stable; saved progress is keyed on them.
- Grab coordinates by right-clicking a spot in Google Maps or Apple Maps.

## Otter voice (Gemini + ElevenLabs)

The otter mascot's lines are written by the Gemini API and read aloud with an ElevenLabs voice. Both are optional. Without keys the app uses the written fun facts in `tours.json` and the on-device voice.

1. Run `./scripts/set-keys.sh` (prompts for keys, input hidden) or add `GEMINI_API_KEY` and `ELEVEN_LABS_API_KEY` to `.env` by hand.
2. Change `ELEVEN_LABS_VOICE_ID` in `.env` to pick a different default voice, or pick one in-app under Settings.
3. Rebuild and run.

Generated lines are saved to SharedPreferences and audio is cached to disk, so each line is only generated once.

**Before shipping:** keys built into an app can be extracted. For a public release, route these calls through your own backend and keep the keys there.

## Ideas for next steps

- **Background tracking** so narration fires with the phone in a pocket (a foreground service with a location notification).
- **Recorded audio** instead of text-to-speech (add an optional `audioFile` field to `Stop`).
- **Photos** per stop, historical "then vs. now" images.
- **Remote tour content** (fetch `tours.json` from a server so tours update without a Play Store release).
- **Boat-specific features**: tide/current info, launch points, kayak rental locations.
- **Play Internal Testing** track for sharing builds with the team before a public release, the Android equivalent of TestFlight.
