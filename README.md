# Philly Tour App — Waterway Tours

An iOS app for self-guided tours along a city's waterways. Pick a tour, hit **Start tour**, and as you walk, bike, or paddle past each stop the app detects you've arrived (GPS geofence), reads the stop's story aloud, and unlocks a trivia question.

Ships with two Philadelphia tours (Schuylkill Banks, Delaware Waterfront) and a Chicago Riverwalk tour to show it works for any city.

## Running it

Requirements: Xcode 16+ (iOS 17+ target).

1. Open `WaterwayTours.xcodeproj` in Xcode.
2. Pick an iPhone simulator and press **Run** (⌘R).
3. To test geofencing on the simulator, set a fake location: **Features → Location → Custom Location…** in Simulator, or:

   ```bash
   xcrun simctl location booted set 39.9695,-75.1874
   ```

   (That's Boathouse Row, stop 1 of the Schuylkill tour.) You can also tap any stop on the map and press **I'm here**.

To run on a real iPhone, set your team under *Signing & Capabilities* in Xcode.

## Shipping to TestFlight

One-time setup:

1. **App icon** — export a 1024×1024 PNG (no transparency, no rounded corners — iOS rounds them) and drag it into `Assets.xcassets → AppIcon` in Xcode. Uploads are rejected without one.
2. **Signing** — target → *Signing & Capabilities* → pick your team, and change the bundle ID (`com.phillytour.WaterwayTours`) to one you own.
3. **App Store Connect** — create the app at appstoreconnect.apple.com using that bundle ID.

Every build:

1. Bump the build number:

   ```bash
   agvtool next-version -all
   ```

2. In Xcode, set the destination to **Any iOS Device (arm64)**, then **Product → Archive → Distribute App → TestFlight & App Store**.

The export-compliance question is pre-answered (`ITSAppUsesNonExemptEncryption = NO`) since the app uses no custom encryption.

## How it's organized

```
WaterwayTours/
├── WaterwayToursApp.swift     App entry; shares LocationService + Narrator via the environment
├── Models/
│   ├── Stop.swift             A stop: coordinate, geofence radius, narration, trivia
│   └── Tour.swift             A tour: city, waterway, travel mode, ordered stops
├── Services/
│   ├── TourLibrary.swift      Loads tours from Resources/tours.json
│   ├── LocationService.swift  CoreLocation wrapper
│   ├── Narrator.swift         Text-to-speech narration (AVSpeechSynthesizer)
│   └── TourSession.swift      Progress: arrival detection, visited stops, trivia score (saved)
├── Views/
│   ├── TourListView.swift     Tours grouped by city
│   ├── TourView.swift         Map with route, stops, geofences, and the status panel
│   └── StopSheet.swift        Narration + trivia for one stop
└── Resources/tours.json       All tour content
```

The Xcode project uses a synchronized folder, so any file you add under `WaterwayTours/` is picked up automatically — no need to drag it into Xcode.

## Adding a city or tour

Just edit `WaterwayTours/Resources/tours.json` — no code changes. Add an entry like:

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
- Grab coordinates by right-clicking a spot in Apple Maps or Google Maps.

## Ideas for next steps

- **Background tracking** so narration fires with the phone in a pocket (add the Location + Audio background modes and request "Always" permission).
- **Recorded audio** instead of text-to-speech (add an optional `audioFile` field to `Stop`).
- **Photos** per stop, historical "then vs. now" images.
- **Remote tour content** (fetch `tours.json` from a server so tours update without an app release).
- **Boat-specific features**: tide/current info, launch points, kayak rental locations.
