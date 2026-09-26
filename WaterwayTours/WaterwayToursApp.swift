import SwiftUI

@main
struct WaterwayToursApp: App {
    @State private var location = LocationService()
    @State private var narrator = Narrator()
    private let tours = TourLibrary.load()

    var body: some Scene {
        WindowGroup {
            TourListView(tours: tours)
                .environment(location)
                .environment(narrator)
        }
    }
}
