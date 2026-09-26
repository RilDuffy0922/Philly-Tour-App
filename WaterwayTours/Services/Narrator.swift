import AVFoundation
import Observation

/// Reads text aloud, ducking any music that's playing. Uses the ElevenLabs voice when a key is configured,
/// and falls back to on-device text-to-speech so narration always works.
@MainActor @Observable
final class Narrator: NSObject {
    /// The text currently being read aloud (or fetched for reading), if any.
    private(set) var currentText: String?

    @ObservationIgnored private let synthesizer = AVSpeechSynthesizer()
    @ObservationIgnored private var player: AVAudioPlayer?
    @ObservationIgnored private var loading: Task<Void, Never>?
    @ObservationIgnored private let apiKey: String?
    @ObservationIgnored private let defaultVoiceID: String

    var hasCloudVoice: Bool { apiKey != nil }

    /// The voice the rider picked in Settings, or the default.
    var voiceID: String { UserDefaults.standard.string(forKey: OtterVoice.storageKey) ?? defaultVoiceID }

    init(secrets: Secrets = .shared) {
        apiKey = secrets.elevenLabsAPIKey
        defaultVoiceID = secrets.elevenLabsVoiceID
        super.init()
        synthesizer.delegate = self
    }

    /// Speaks `text`. When `systemVoiceFallback` is false and no ElevenLabs voice is available, stays silent.
    func speak(_ text: String, systemVoiceFallback: Bool = true, voiceID override: String? = nil) {
        cancelCurrent()
        currentText = text

        guard let apiKey else {
            if systemVoiceFallback { speakWithSystemVoice(text) } else { currentText = nil }
            return
        }

        let eleven = ElevenLabsClient(apiKey: apiKey, voiceID: override ?? voiceID)
        loading = Task { [weak self] in
            do {
                let audio = try await eleven.speech(for: text)
                guard !Task.isCancelled, let self else { return }
                try self.play(audio)
                self.loading = nil
            } catch {
                guard !Task.isCancelled, let self else { return }
                self.loading = nil
                if systemVoiceFallback { self.speakWithSystemVoice(text) } else { self.finish() }
            }
        }
    }

    func stop() {
        cancelCurrent()
        finish()
    }

    func isSpeaking(_ text: String) -> Bool { currentText == text }

    private func cancelCurrent() {
        loading?.cancel()
        loading = nil
        synthesizer.stopSpeaking(at: .immediate)
        player?.stop()
        player = nil
    }

    private func activateSession() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
        try? session.setActive(true)
    }

    private func play(_ audio: Data) throws {
        activateSession()
        let player = try AVAudioPlayer(data: audio)
        player.delegate = self
        self.player = player
        player.play()
    }

    private func speakWithSystemVoice(_ text: String) {
        activateSession()
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "en-US")
        synthesizer.speak(utterance)
    }

    private func finish() {
        currentText = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    private func utteranceEnded() {
        // A cancelled utterance can report in after a new one has started, so ask the synthesizer.
        guard !synthesizer.isSpeaking, player?.isPlaying != true, loading == nil else { return }
        finish()
    }
}

extension Narrator: AVSpeechSynthesizerDelegate {
    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        Task { @MainActor in self.utteranceEnded() }
    }

    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        Task { @MainActor in self.utteranceEnded() }
    }
}

extension Narrator: AVAudioPlayerDelegate {
    nonisolated func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        Task { @MainActor in
            self.player = nil
            self.loading = nil
            self.finish()
        }
    }
}
