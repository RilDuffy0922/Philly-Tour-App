//
//  Stop.swift
//  
//
//  Created by Bavanan Bramillan on 9/26/26.
//

import Foundation
import CoreLocation

struct Stop: Identifiable {
    let id: UUID
    let name: String
    let coordinate: CLLocationCoordinate2D
    let radius: CLLocationDistance // meters
    let narrationScript: String
    let trivia: TriviaQuestion

    init(name: String, latitude: Double, longitude: Double, radius: CLLocationDistance, narrationScript: String, trivia: TriviaQuestion) {
        self.id = UUID()
        self.name = name
        self.coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        self.radius = radius
        self.narrationScript = narrationScript
        self.trivia = trivia
    }
}

struct TriviaQuestion {
    let question: String
    let options: [String]
    let correctIndex: Int
}
