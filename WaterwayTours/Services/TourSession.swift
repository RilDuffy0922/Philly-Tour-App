import CoreLocation
import Foundation
import Observation

/// Progress through one tour: which stops were reached and how trivia went. Saved to UserDefaults.
@MainActor @Observable
final class TourSession {
    let tour: Tour
    private(set) var visited: Set<Stop.ID>
    private(set) var skipped: Set<Stop.ID>
    private(set) var answers: [Stop.ID: Int]
    /// Stops in tour order. Starts as the authored order, then is re-sorted from the rider's position.
    private(set) var stops: [Stop]
    private var isOrdered: Bool
    var presentedStop: Stop?

    /// Fixes worse than this are too fuzzy to trigger a geofence.
    private let maxUsableAccuracy: CLLocationDistance = 100

    init(tour: Tour) {
        self.tour = tour
        let saved = Progress.load(tourID: tour.id)
        visited = saved.visited
        skipped = saved.skipped ?? []
        answers = saved.answers
        if let order = saved.order {
            let byID = Dictionary(uniqueKeysWithValues: tour.stops.map { ($0.id, $0) })
            let restored = order.compactMap { byID[$0] }
            stops = restored.count == tour.stops.count ? restored : tour.stops
            isOrdered = restored.count == tour.stops.count
        } else {
            stops = tour.stops
            isOrdered = false
        }
    }

    /// True until the stops have been sorted from the rider's location.
    var needsOrdering: Bool { !isOrdered && visited.isEmpty && skipped.isEmpty }

    /// Makes the stop closest to `location` the first stop, then chains each next-closest stop after it.
    func orderStops(from location: CLLocation) {
        guard needsOrdering, location.horizontalAccuracy >= 0 else { return }
        var remaining = tour.stops
        var ordered: [Stop] = []
        var current = location
        while !remaining.isEmpty {
            let nearest = remaining.enumerated().min { current.distance(from: $0.element.location) < current.distance(from: $1.element.location) }!
            ordered.append(remaining.remove(at: nearest.offset))
            current = ordered.last!.location
        }
        stops = ordered
        isOrdered = true
        save()
    }

    var nextStop: Stop? { stops.first { !visited.contains($0.id) && !skipped.contains($0.id) } }
    var isComplete: Bool { nextStop == nil }
    var correctAnswers: Int {
        tour.stops.filter { answers[$0.id] == $0.trivia.correctIndex }.count
    }

    func number(of stop: Stop) -> Int {
        (stops.firstIndex(of: stop) ?? 0) + 1
    }

    /// The closest unvisited stop whose geofence contains `location`, if any.
    func stopArrived(at location: CLLocation) -> Stop? {
        guard location.horizontalAccuracy >= 0, location.horizontalAccuracy <= maxUsableAccuracy else { return nil }
        return stops
            .filter { !visited.contains($0.id) && !skipped.contains($0.id) }
            .map { (stop: $0, distance: location.distance(from: $0.location)) }
            .filter { $0.distance <= $0.stop.radius }
            .min { $0.distance < $1.distance }?
            .stop
    }

    func markVisited(_ stop: Stop) {
        visited.insert(stop.id)
        skipped.remove(stop.id)
        save()
    }

    /// Drops a stop the rider isn't interested in; it no longer counts as "next" or triggers on arrival.
    func skip(_ stop: Stop) {
        guard !visited.contains(stop.id) else { return }
        skipped.insert(stop.id)
        save()
    }

    func answer(_ optionIndex: Int, for stop: Stop) {
        guard answers[stop.id] == nil else { return }
        answers[stop.id] = optionIndex
        save()
    }

    func reset() {
        visited = []
        skipped = []
        answers = [:]
        stops = tour.stops
        isOrdered = false
        save()
    }

    private func save() {
        Progress(visited: visited, skipped: skipped, answers: answers, order: isOrdered ? stops.map(\.id) : nil).save(tourID: tour.id)
    }
}

private struct Progress: Codable {
    var visited: Set<String> = []
    var skipped: Set<String>?
    var answers: [String: Int] = [:]
    var order: [String]?

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
