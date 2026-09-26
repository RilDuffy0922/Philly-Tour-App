import SwiftUI

struct TourListView: View {
    let tours: [Tour]

    @AppStorage("hasSeenTutorial") private var hasSeenTutorial = false
    @State private var showingTutorial = false
    @State private var showingSettings = false
    @State private var path: [Tour] = []
    @Environment(DemoController.self) private var demo
    @Environment(Narrator.self) private var narrator

    /// Cities in the order they first appear in tours.json.
    private var toursByCity: [(city: String, tours: [Tour])] {
        var cities: [String] = []
        for tour in tours where !cities.contains(tour.city) { cities.append(tour.city) }
        return cities.map { city in (city: city, tours: tours.filter { $0.city == city }) }
    }

    var body: some View {
        @Bindable var demo = demo

        NavigationStack(path: $path) {
            List {
                Section {
                    Button {
                        demo.showConfirm = true
                    } label: {
                        Label {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Play demo").font(.headline)
                                Text("A guided walkthrough of every feature")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        } icon: {
                            Image(systemName: "play.circle.fill")
                                .font(.title2)
                        }
                    }
                    .demoTarget(.playDemo)
                }

                ForEach(toursByCity, id: \.city) { group in
                    Section(group.city) {
                        ForEach(group.tours) { tour in
                            NavigationLink(value: tour) {
                                TourRow(tour: tour)
                            }
                            .demoTarget(tour.id == tours.first?.id ? .tourList : nil)
                        }
                    }
                }
            }
            .navigationTitle("Waterway Tours")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("How it works", systemImage: "questionmark.circle") { showingTutorial = true }
                        .demoTarget(.helpButton)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Settings", systemImage: "gearshape") { showingSettings = true }
                        .demoTarget(.settingsButton)
                }
            }
            .sheet(isPresented: $showingTutorial, onDismiss: { hasSeenTutorial = true }) {
                TutorialView()
            }
            .sheet(isPresented: $showingSettings) {
                SettingsView { showingTutorial = true }
            }
            .onAppear {
                if !hasSeenTutorial { showingTutorial = true }
            }
            .navigationDestination(for: Tour.self) { tour in
                TourView(tour: tour, isDemo: demo.isActive)
            }
            .onChange(of: demo.screen) { _, screen in
                // The demo opens the first tour and later brings us back to this page.
                if screen == .list {
                    path = []
                } else if path.isEmpty, let first = tours.first {
                    path = [first]
                }
            }
            .alert("Enter demo mode?", isPresented: $demo.showConfirm) {
                Button("Yes") { demo.begin(narrator: narrator) }
                Button("No", role: .cancel) {}
            } message: {
                Text("The app will go into demo mode, and the otter will walk you through every feature. Your real progress won't be changed. Do you wish to continue?")
            }
            .alert("Demo finished", isPresented: $demo.showFinished) {
                Button("OK") { demo.end(narrator: narrator) }
            } message: {
                Text("You've seen everything the app can do. Pick a tour to get started!")
            }
            .overlay {
                if tours.isEmpty {
                    ContentUnavailableView("No tours yet", systemImage: "water.waves",
                                           description: Text("Add tours to tours.json."))
                }
            }
        }
    }
}

private struct TourRow: View {
    let tour: Tour

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: tour.mode.symbol)
                .font(.title3)
                .foregroundStyle(.white)
                .frame(width: 44, height: 44)
                .background(.tint, in: RoundedRectangle(cornerRadius: 10))

            VStack(alignment: .leading, spacing: 3) {
                Text(tour.name)
                    .font(.headline)
                Text(tour.waterway)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Text("\(tour.stops.count) stops · \(Measurement(value: tour.lengthMeters, unit: UnitLength.meters).formatted(.measurement(width: .abbreviated, usage: .road)))")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    TourListView(tours: TourLibrary.load())
        .environment(LocationService())
        .environment(Narrator())
        .environment(OtterDialogue())
        .environment(DemoController())
}
