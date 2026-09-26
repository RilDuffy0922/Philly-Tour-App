import CoreGraphics
import Foundation
import Observation

/// Things the guided demo can point at.
enum DemoTarget: Hashable {
    case tourList, helpButton, settingsButton, playDemo
    case map, menuButton, otter, eta, traveled, stopsProgress, skip, directions, startTour
    case story, narrationButton, trivia
}

enum DemoScreen { case list, tour, sheet }

enum DemoAction { case walkToFirstStop, closeSheet }

struct DemoStep {
    let target: DemoTarget
    let screen: DemoScreen
    let text: String
    /// Runs after the otter finishes talking. Steps with an action move on by themselves; the rest wait for a tap.
    var action: DemoAction?
}

/// Runs the guided demo: the otter explains one feature at a time (spoken, with the feature highlighted),
/// waits for a tap, and moves on. The screens react to `screen` and perform `actionHandler` actions.
@MainActor @Observable
final class DemoController {
    private(set) var isActive = false
    private(set) var current: DemoStep?
    private(set) var screen: DemoScreen = .list
    private(set) var awaitingTap = false
    var showConfirm = false
    var showFinished = false
    var frames: [DemoTarget: CGRect] = [:]
    /// Set by the tour screen so the demo can make it walk to the first stop and close the stop sheet.
    @ObservationIgnored var actionHandler: ((DemoAction) async -> Void)?

    @ObservationIgnored private var task: Task<Void, Never>?
    @ObservationIgnored private var tapContinuation: CheckedContinuation<Void, Never>?

    static let steps: [DemoStep] = [
        DemoStep(target: .tourList, screen: .list,
                 text: "Welcome! This is the tour selection page. Each row is a river tour. Tap one to open its map."),
        DemoStep(target: .helpButton, screen: .list,
                 text: "This question mark replays the short tutorial any time you want it."),
        DemoStep(target: .settingsButton, screen: .list,
                 text: "In Settings you can pick my voice, or turn me off if you would rather just read."),
        DemoStep(target: .playDemo, screen: .list,
                 text: "And this is Play demo, the walkthrough you are taking right now. Let's open a tour."),
        DemoStep(target: .map, screen: .tour,
                 text: "This is the tour map. The numbered pins are the stops, and the dotted line connects them in the order we will visit. The stop closest to you becomes number one."),
        DemoStep(target: .menuButton, screen: .tour,
                 text: "This menu lets you place yourself near a stop to try things out, switch back to your real location, or reset your progress."),
        DemoStep(target: .directions, screen: .tour,
                 text: "Tap Directions to open Apple Maps with directions to your next stop, starting with the first one."),
        DemoStep(target: .eta, screen: .tour,
                 text: "This estimate works like the arrival time in Apple Maps. It shows about how long the rest of the tour will take, and when you would finish."),
        DemoStep(target: .traveled, screen: .tour,
                 text: "And this counts the miles you have traveled so far on this tour."),
        DemoStep(target: .stopsProgress, screen: .tour,
                 text: "Here you can see how many stops you have visited, plus your trivia score."),
        DemoStep(target: .skip, screen: .tour,
                 text: "Not interested in a stop? Tap Skip and I will move on to the next one."),
        DemoStep(target: .otter, screen: .tour,
                 text: "That's me! While you travel, I share fun facts about the next stop, and I read them out loud. Tap my bubble to hear the next one, or the speaker to mute me."),
        DemoStep(target: .startTour, screen: .tour,
                 text: "Tap Start tour and I will follow your location. Watch, I will walk to the first stop for you.",
                 action: .walkToFirstStop),
        DemoStep(target: .story, screen: .sheet,
                 text: "When you arrive at a stop, its story appears here, and I read it aloud."),
        DemoStep(target: .narrationButton, screen: .sheet,
                 text: "Use this button to play or stop the narration whenever you like."),
        DemoStep(target: .trivia, screen: .sheet,
                 text: "Then answer a trivia question about the stop. Every right answer adds to your score. That's the whole app!"),
    ]

    func begin(narrator: Narrator) {
        guard !isActive else { return }
        isActive = true
        screen = .list
        current = nil
        task = Task { await run(narrator: narrator) }
    }

    /// Called when the rider taps the screen. Moves on if the otter has finished talking.
    func tap() {
        guard awaitingTap else { return }
        awaitingTap = false
        tapContinuation?.resume()
        tapContinuation = nil
    }

    /// Ends the demo and returns to the tour selection page.
    func end(narrator: Narrator) {
        task?.cancel()
        task = nil
        tapContinuation?.resume()
        tapContinuation = nil
        narrator.stop()
        awaitingTap = false
        current = nil
        isActive = false
        showFinished = false
        screen = .list
    }

    private func run(narrator: Narrator) async {
        for step in Self.steps {
            if Task.isCancelled { return }
            if step.screen != screen {
                current = nil
                screen = step.screen
                await pause(step.screen == .sheet ? 0.6 : 1.2)   // let the screen settle so the highlight lands
            }
            current = step
            await say(step.text, narrator: narrator)
            if Task.isCancelled { return }
            if let action = step.action {
                await actionHandler?(action)
                await pause(0.8)
            } else {
                await waitForTap()
            }
        }
        if Task.isCancelled { return }
        current = nil
        await actionHandler?(.closeSheet)
        screen = .list
        await pause(0.5)
        showFinished = true
    }

    /// Speaks `text` (when the otter's voice is on) and returns once it has finished.
    private func say(_ text: String, narrator: Narrator) async {
        let voiceOn = UserDefaults.standard.object(forKey: "otterVoiceOn") as? Bool ?? true
        guard voiceOn else {
            await pause(max(2, Double(text.count) * 0.045))
            return
        }
        narrator.speak(text)
        var waited = 0.0
        while narrator.isSpeaking(text), waited < 60, !Task.isCancelled {
            await pause(0.25)
            waited += 0.25
        }
        await pause(0.3)
    }

    private func waitForTap() async {
        awaitingTap = true
        await withCheckedContinuation { tapContinuation = $0 }
    }

    private func pause(_ seconds: Double) async {
        try? await Task.sleep(for: .seconds(seconds))
    }
}
