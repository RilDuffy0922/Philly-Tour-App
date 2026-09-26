//
//  Stop.swift
//
//
//  Created by Bavanan Bramillan on 9/26/26.
//

import Foundation
import CoreLocation

struct Stop: Identifiable, Codable, Hashable {
    /// Stable identifier (e.g. "boathouse-row") so saved progress survives app launches.
    let id: String
    let name: String
    let latitude: Double
    let longitude: Double
    let radius: CLLocationDistance // meters
    let narrationScript: String
    let funFacts: [String]?
    let trivia: TriviaQuestion

    var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }

    var location: CLLocation {
        CLLocation(latitude: latitude, longitude: longitude)
    }
}

struct TriviaQuestion: Codable, Hashable {
    let question: String
    let options: [String]
    let correctIndex: Int
}
