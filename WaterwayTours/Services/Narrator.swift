import AVFoundation
import Observation

/// Reads stop narration aloud with on-device text-to-speech, ducking any music that's playing.
@MainActor @Observable
final class Narrator: NSObject {
    /// The text currently being read aloud, if any.
    private(set) var currentText: String?

    @ObservationIgnored private let synthesizer = AVSpeechSynthesizer()

    override init() {
        super.init()
        synthesizer.delegate = self
    }

    func speak(_ text: String) {
        synthesizer.stopSpeaking(at: .immediate)
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
        try? session.setActive(true)

        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "en-US")
        synthesizer.speak(utterance)
        currentText = text
    }

    func stop() {
        synthesizer.stopSpeaking(at: .immediate)
        currentText = nil
    }

    func isSpeaking(_ text: String) -> Bool { currentText == text }

    private func utteranceEnded() {
        // A cancelled utterance can report in after a new one has started, so ask the synthesizer.
        guard !synthesizer.isSpeaking else { return }
        currentText = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
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
