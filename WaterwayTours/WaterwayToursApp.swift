import SwiftUI

@main
struct WaterwayToursApp: App {
    @State private var location = LocationService()
    @State private var narrator = Narrator()
    @State private var otter = OtterDialogue()
    private let tours = TourLibrary.load()

    var body: some Scene {
        WindowGroup {
            TourListView(tours: tours)
                .environment(location)
                .environment(narrator)
                .environment(otter)
        }
    }
}
