# Waterway Tours

An iOS app for self-guided tours along a city's waterways. Pick a tour, hit Start tour, and as you walk or bike past each stop the app notices you've arrived (using GPS), reads you the stop's story out loud, and asks a trivia question about it. An otter mascot walks you between stops and shares extra facts and things to do along the way.

Ships with two Philadelphia tours: Schuylkill Banks and Delaware Waterfront.

This README has two parts: how to get the app running on your own iPhone, and how it was actually built, for anyone new to mobile development who wants to look under the hood.

## Running it on your iPhone

You do not need to publish anything to the App Store to try this out. Xcode can install it directly onto a phone over a cable, using your own free Apple ID. It is good for about a week before it needs reinstalling, which is normal for this kind of install.

What you need: a Mac with Xcode installed, an iPhone, and a cable to connect them.

1. Open `WaterwayTours.xcodeproj` in Xcode.
2. Plug in the iPhone. The first time, it will ask "Trust This Computer?" — tap Trust and enter the phone's passcode.
3. In Xcode's top toolbar, there is a device picker. Select your iPhone from the list instead of a simulator.
4. Click the target named WaterwayTours in the file list on the left, then open the "Signing & Capabilities" tab. Make sure "Automatically manage signing" is checked, and pick your own Apple ID under Team.
5. Press the Run button (the triangle in the top left, or Cmd-R).

The first time you do this on a given phone, two extra prompts show up:

- The phone may say Developer Mode is required. If so, go to Settings, search for "Developer Mode", turn it on, and let the phone restart. Then run again from Xcode.
- After the app installs, opening it may show "Untrusted Developer." Go to Settings, then General, then VPN & Device Management, tap the entry under Developer App, and tap Trust.

After that, the app behaves like any other app on the phone. It does not need to stay connected to the Mac, and it works without Xcode running.

## Using the app

- The list screen shows the available tours, grouped by city. Tap one to open its map.
- Tap "Start tour" to let the app follow your location. The otter narrates as you go, and each stop's story and trivia question appear automatically when you arrive.
- Not interested in a stop? Tap Skip, either on the map, in the stop's own screen, or in the status bar at the bottom.
- The "Directions" button opens Apple Maps with walking or biking directions to your next stop.
- "Play demo" on the list screen is a guided walkthrough of the whole app, useful for showing it to someone for the first time without needing to actually be near the river.
- The gear icon opens Settings, where you can change the otter's voice or turn its voice off.

## How this was built

This section is written for someone who has not built a mobile app before. The short version: it is a SwiftUI app, meaning it is written entirely in Swift, Apple's programming language, using SwiftUI to describe what the screens look like. There is no separate design tool or web technology involved. Everything below is a standard Apple framework except for two optional add-ons.

**The map and location.** The app uses MapKit for the map itself and CoreLocation for reading the phone's GPS position. When your location comes within a stop's radius, the app treats that as "arrived" the same idea as a geofence, which is just a name for "an invisible circle on the map that triggers something when you enter it."

**The voice.** By default, narration is read aloud using AVSpeechSynthesizer, which is the same text-to-speech engine behind VoiceOver and Siri, built into every iPhone for free. Optionally, the app can instead send that same text to ElevenLabs, a paid service that generates much more natural-sounding speech, and play that back. If no ElevenLabs key is configured, it silently falls back to the built-in voice, so the app always works either way.

**The otter's extra lines.** Each stop has a few hand-written fun facts and suggestions built into the app's content file. Optionally, the app can instead ask Google's Gemini API to write fresh lines about a stop on the fly, mixing history with things to do there. Those generated lines are cached on the device so each stop is only ever generated once. Again, without a Gemini key, the app just uses the hand-written lines instead, so this is a nice-to-have, not a requirement.

**The tour content.** Every tour, stop, narration script, and trivia question lives in one file: `WaterwayTours/Resources/tours.json`. Nothing about a tour is hard-coded into the Swift code, so adding a new city is a matter of editing that file (see below), not writing new features.

**Saved progress.** Which stops you have visited and how you did on the trivia is saved on the device using UserDefaults, which is the standard, simple way iOS apps remember small bits of information between launches.

Here is roughly where things live in the project:

```
WaterwayTours/
├── Models/          What a Tour and a Stop are made of
├── Services/         The pieces above: location, narration, saved progress, otter content
├── Views/            The actual screens: the tour list, the map, the stop sheet, settings
└── Resources/        tours.json, the app icon, and the otter image
```

## Adding a city or tour

No coding needed. Open `WaterwayTours/Resources/tours.json` and add an entry following the same shape as the existing tours: a name, a travel mode (walk, bike, or boat), and a list of stops, each with coordinates, a radius in meters for how close counts as "arrived," a narration script, and a trivia question. Coordinates can be found by right-clicking a spot in Apple Maps or Google Maps.

## Turning on the otter's AI voice (optional)

1. Copy `Secrets.example.plist` to `WaterwayTours/Resources/Secrets.plist`. This file is git-ignored, meaning it stays on your machine and never gets committed.
2. Fill in a Gemini API key (free, from Google AI Studio) and an ElevenLabs API key.
3. Run `scripts/check-keys.sh` to confirm both keys actually work before you rely on them. It never prints the keys themselves.

## If you want it on the App Store or TestFlight instead

That requires a paid Apple Developer Program membership ($99/year), which this project does not assume you have. If you get one later, the signing setup in this project (team `9YP4VNUU95`, bundle ID `com.bramillan.waterwaytours`) is a starting point, but a different account will need its own bundle ID and team selected in Signing & Capabilities.

## Team: Devon Haydan, Bavanan Bramillan, Riley Duffy, Channtera Kong, Tai Do