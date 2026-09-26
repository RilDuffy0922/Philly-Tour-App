import Foundation
import Observation

/// The otter's lines for each stop. Uses Gemini when a key is configured, and falls back to the written fun facts.
@MainActor @Observable
final class OtterDialogue {
    private var generated: [Stop.ID: [String]] = [:]
    @ObservationIgnored private var inFlight: Set<Stop.ID> = []
    @ObservationIgnored private let gemini: GeminiClient?

    init(secrets: Secrets = .shared) {
        gemini = secrets.geminiAPIKey.map { GeminiClient(apiKey: $0, model: secrets.geminiModel) }
    }

    /// Lines the otter can say about `stop` right now.
    func lines(for stop: Stop) -> [String] {
        generated[stop.id] ?? stop.funFacts ?? []
    }

    /// Generates (or loads a saved copy of) Gemini lines for `stop`. Quietly does nothing on failure.
    func prepare(_ stop: Stop) async {
        guard let gemini, generated[stop.id] == nil, !inFlight.contains(stop.id) else { return }
        let key = "otter.lines.v2.\(stop.id)"
        if let saved = UserDefaults.standard.stringArray(forKey: key), !saved.isEmpty {
            generated[stop.id] = saved
            return
        }
        inFlight.insert(stop.id)
        defer { inFlight.remove(stop.id) }
        do {
            let lines = try await gemini.funFactLines(for: stop)
            generated[stop.id] = lines
            UserDefaults.standard.set(lines, forKey: key)
        } catch {
            print("OtterDialogue: Gemini failed for \(stop.id): \(error)")
        }
    }
}
