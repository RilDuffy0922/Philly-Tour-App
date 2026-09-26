import CoreLocation
import Foundation
import Observation

/// Progress through one tour: which stops were reached and how trivia went. Saved to UserDefaults.
@MainActor @Observable
final class TourSession {
    let tour: Tour
    private(set) var visited: Set<Stop.ID>
    private(set) var answers: [Stop.ID: Int]
    var presentedStop: Stop?

    /// Fixes worse than this are too fuzzy to trigger a geofence.
    private let maxUsableAccuracy: CLLocationDistance = 100

    init(tour: Tour) {
        self.tour = tour
        let saved = Progress.load(tourID: tour.id)
        visited = saved.visited
        answers = saved.answers
    }

    var nextStop: Stop? { tour.stops.first { !visited.contains($0.id) } }
    var isComplete: Bool { visited.count == tour.stops.count }
    var correctAnswers: Int {
        tour.stops.filter { answers[$0.id] == $0.trivia.correctIndex }.count
    }

    func number(of stop: Stop) -> Int {
        (tour.stops.firstIndex(of: stop) ?? 0) + 1
    }

    /// The closest unvisited stop whose geofence contains `location`, if any.
    func stopArrived(at location: CLLocation) -> Stop? {
        guard location.horizontalAccuracy >= 0, location.horizontalAccuracy <= maxUsableAccuracy else { return nil }
        return tour.stops
            .filter { !visited.contains($0.id) }
            .map { (stop: $0, distance: location.distance(from: $0.location)) }
            .filter { $0.distance <= $0.stop.radius }
            .min { $0.distance < $1.distance }?
            .stop
    }

    func markVisited(_ stop: Stop) {
        visited.insert(stop.id)
        save()
    }

    func answer(_ optionIndex: Int, for stop: Stop) {
        guard answers[stop.id] == nil else { return }
        answers[stop.id] = optionIndex
        save()
    }

    func reset() {
        visited = []
        answers = [:]
        save()
    }

    private func save() {
        Progress(visited: visited, answers: answers).save(tourID: tour.id)
    }
}

private struct Progress: Codable {
    var visited: Set<String> = []
    var answers: [String: Int] = [:]

    static func key(_ tourID: String) -> String { "progress.\(tourID)" }

    static func load(tourID: String) -> Progress {
        guard let data = UserDefaults.standard.data(forKey: key(tourID)),
              let progress = try? JSONDecoder().decode(Progress.self, from: data) else { return Progress() }
        return progress
    }

    func save(tourID: String) {
        guard let data = try? JSONEncoder().encode(self) else { return }
        UserDefaults.standard.set(data, forKey: Self.key(tourID))
    }
}
