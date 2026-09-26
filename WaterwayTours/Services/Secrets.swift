import Foundation

/// API keys and voice settings, read from a git-ignored `Secrets.plist` in the app bundle.
/// Copy `Secrets.example.plist` to `WaterwayTours/Resources/Secrets.plist` to enable them.
/// Without keys the app still works: it uses the written fun facts and the on-device voice.
struct Secrets {
    let geminiAPIKey: String?
    let geminiModel: String
    let elevenLabsAPIKey: String?
    let elevenLabsVoiceID: String

    static let shared = Secrets(bundle: .main)

    init(bundle: Bundle) {
        let values = bundle.url(forResource: "Secrets", withExtension: "plist")
            .flatMap { NSDictionary(contentsOf: $0) as? [String: String] } ?? [:]

        func value(_ key: String) -> String? {
            // Pasted keys often pick up stray spaces, newlines, or quote marks.
            guard let raw = values[key]?.trimmingCharacters(in: .whitespacesAndNewlines.union(CharacterSet(charactersIn: "\"'"))),
                  !raw.isEmpty, !raw.hasPrefix("YOUR_") else { return nil }
            return raw
        }

        geminiAPIKey = value("GeminiAPIKey")
        geminiModel = value("GeminiModel") ?? "gemini-3.8-flash"
        elevenLabsAPIKey = value("ElevenLabsAPIKey")
        elevenLabsVoiceID = value("ElevenLabsVoiceID") ?? "cgSgspJ2msm6clMCkdW9"
    }
}
