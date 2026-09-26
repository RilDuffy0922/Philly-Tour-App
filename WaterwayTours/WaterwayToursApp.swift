import SwiftUI

@main
struct WaterwayToursApp: App {
    @State private var location = LocationService()
    @State private var narrator = Narrator()
    @State private var otter = OtterDialogue()
    @State private var demo = DemoController()
    private let tours = TourLibrary.load()

    var body: some Scene {
        WindowGroup {
            ZStack {
                TourListView(tours: tours)
                DemoOverlay(scope: .root)
            }
            .environment(location)
            .environment(narrator)
            .environment(otter)
            .environment(demo)
        }
    }
}
