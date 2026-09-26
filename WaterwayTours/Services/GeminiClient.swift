import Foundation

/// Writes the otter's spoken lines with the Gemini API, grounded in each stop's own facts.
struct GeminiClient {
    let apiKey: String
    let model: String

    enum GeminiError: Error { case badResponse(Int), emptyResponse }

    private static let systemInstruction = """
    You are the cheerful otter mascot of a walking and biking tour of Philadelphia's rivers. \
    Speak in first person to the rider, in a warm, light-hearted, playful voice. \
    Each line is one or two short sentences, at most 30 words, and works when read aloud. \
    Use at most one otter or water pun across the whole set. \
    Only use facts stated in the source material you are given. Do not mention any place, ship, landmark, person, date, or number that is not in the source material, even if you know it is true. \
    Do not use emoji, hashtags, or stage directions.
    """

    /// Returns several fresh fun-fact lines about `stop`, ready to be read aloud.
    func funFactLines(for stop: Stop, count: Int = 4) async throws -> [String] {
        var source = "Stop: \(stop.name)\nBackground: \(stop.narrationScript)"
        if let facts = stop.funFacts, !facts.isEmpty {
            source += "\nKnown fun facts:\n" + facts.map { "- \($0)" }.joined(separator: "\n")
        }
        let prompt = "\(source)\n\nWrite \(count) different fun-fact lines about this stop as the otter, each about a different detail."

        let body: [String: Any] = [
            "systemInstruction": ["parts": [["text": Self.systemInstruction]]],
            "contents": [["parts": [["text": prompt]]]],
            "generationConfig": [
                "temperature": 1.0,
                "responseMimeType": "application/json",
                "responseSchema": ["type": "ARRAY", "items": ["type": "STRING"]],
            ],
        ]

        // The newer "AQ." style keys are only accepted as a query parameter, not the x-goog-api-key header.
        var components = URLComponents(string: "https://generativelanguage.googleapis.com/v1beta/models/\(model):generateContent")!
        components.queryItems = [URLQueryItem(name: "key", value: apiKey)]
        var request = URLRequest(url: components.url!)
        request.httpMethod = "POST"
        request.timeoutInterval = 20
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let data = try await send(request)

        let reply = try JSONDecoder().decode(Reply.self, from: data)
        guard let text = reply.candidates.first?.content.parts.compactMap(\.text).first,
              let lines = try? JSONDecoder().decode([String].self, from: Data(text.utf8)) else {
            throw GeminiError.emptyResponse
        }
        let cleaned = lines.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
        guard !cleaned.isEmpty else { throw GeminiError.emptyResponse }
        return cleaned
    }

    /// Sends the request, retrying a few times: the free tier occasionally answers 404, 429 or 503 and then succeeds.
    private func send(_ request: URLRequest, attempts: Int = 4) async throws -> Data {
        var lastStatus = 0
        for attempt in 0..<attempts {
            if attempt > 0 { try await Task.sleep(for: .seconds(Double(attempt) * 2)) }
            let (data, response) = try await URLSession.shared.data(for: request)
            let status = (response as? HTTPURLResponse)?.statusCode ?? 0
            if status == 200 { return data }
            lastStatus = status
            if status == 400 || status == 401 || status == 403 { break }
        }
        throw GeminiError.badResponse(lastStatus)
    }

    private struct Reply: Decodable {
        struct Candidate: Decodable {
            struct Content: Decodable {
                struct Part: Decodable { let text: String? }
                let parts: [Part]
            }
            let content: Content
        }
        let candidates: [Candidate]
    }
}
