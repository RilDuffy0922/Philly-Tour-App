import Foundation

/// Loads the bundled tours. Add a new city by adding entries to `tours.json` — no code changes needed.
enum TourLibrary {
    static func load(from bundle: Bundle = .main) -> [Tour] {
        guard let url = bundle.url(forResource: "tours", withExtension: "json") else {
            assertionFailure("tours.json missing from bundle")
            return []
        }
        do {
            return try JSONDecoder().decode([Tour].self, from: Data(contentsOf: url))
        } catch {
            assertionFailure("Failed to decode tours.json: \(error)")
            return []
        }
    }
}
