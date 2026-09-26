import CryptoKit
import Foundation

/// Turns text into speech with ElevenLabs. Audio is cached on disk so each line is only generated once.
struct ElevenLabsClient {
    let apiKey: String
    let voiceID: String

    /// Fast model tuned for real-time use.
    private let modelID = "eleven_flash_v2_5"

    enum ElevenLabsError: Error { case badResponse(Int) }

    /// MP3 audio for `text`, from the cache if we've already generated it.
    func speech(for text: String) async throws -> Data {
        let cacheURL = Self.cacheDirectory.appendingPathComponent(cacheKey(for: text)).appendingPathExtension("mp3")
        if let cached = try? Data(contentsOf: cacheURL) { return cached }

        var components = URLComponents(string: "https://api.elevenlabs.io/v1/text-to-speech/\(voiceID)")!
        components.queryItems = [URLQueryItem(name: "output_format", value: "mp3_44100_128")]

        let body: [String: Any] = [
            "text": text,
            "model_id": modelID,
            "voice_settings": ["stability": 0.35, "similarity_boost": 0.75, "speed": 1.05],
        ]

        var request = URLRequest(url: components.url!)
        request.httpMethod = "POST"
        request.timeoutInterval = 30
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("audio/mpeg", forHTTPHeaderField: "Accept")
        request.setValue(apiKey, forHTTPHeaderField: "xi-api-key")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await URLSession.shared.data(for: request)
        if let status = (response as? HTTPURLResponse)?.statusCode, status != 200 {
            throw ElevenLabsError.badResponse(status)
        }
        try? data.write(to: cacheURL, options: .atomic)
        return data
    }

    private func cacheKey(for text: String) -> String {
        let digest = SHA256.hash(data: Data("\(voiceID)|\(modelID)|\(text)".utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
    }

    private static let cacheDirectory: URL = {
        let directory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("otter-voice", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory
    }()
}
