import Foundation
import CoreLocation
import MapKit

struct Tour: Identifiable, Codable, Hashable {
    let id: String
    let city: String
    let waterway: String
    let name: String
    let summary: String
    let mode: TravelMode
    let stops: [Stop]

    var coordinates: [CLLocationCoordinate2D] { stops.map(\.coordinate) }

    /// Straight-line length of the route between consecutive stops.
    var lengthMeters: CLLocationDistance {
        zip(stops, stops.dropFirst()).reduce(0) { total, pair in
            total + pair.0.location.distance(from: pair.1.location)
        }
    }

    /// Map region that fits every stop with some padding.
    var mapRect: MKMapRect {
        let rect = stops.reduce(MKMapRect.null) { rect, stop in
            rect.union(MKMapRect(origin: MKMapPoint(stop.coordinate), size: MKMapSize(width: 1, height: 1)))
        }
        return rect.insetBy(dx: -rect.width * 0.3 - 800, dy: -rect.height * 0.3 - 800)
    }
}

enum TravelMode: String, Codable {
    case walk, bike, boat

    var label: String {
        switch self {
        case .walk: "Walking"
        case .bike: "Biking"
        case .boat: "By boat"
        }
    }

    var symbol: String {
        switch self {
        case .walk: "figure.walk"
        case .bike: "bicycle"
        case .boat: "sailboat"
        }
    }
}
