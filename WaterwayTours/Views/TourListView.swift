import SwiftUI

struct TourListView: View {
    let tours: [Tour]

    @AppStorage("hasSeenTutorial") private var hasSeenTutorial = false
    @State private var showingTutorial = false
    @State private var showingSettings = false

    /// Cities in the order they first appear in tours.json.
    private var toursByCity: [(city: String, tours: [Tour])] {
        var cities: [String] = []
        for tour in tours where !cities.contains(tour.city) { cities.append(tour.city) }
        return cities.map { city in (city: city, tours: tours.filter { $0.city == city }) }
    }

    var body: some View {
        NavigationStack {
            List {
                ForEach(toursByCity, id: \.city) { group in
                    Section(group.city) {
                        ForEach(group.tours) { tour in
                            NavigationLink(value: tour) {
                                TourRow(tour: tour)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Waterway Tours")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("How it works", systemImage: "questionmark.circle") { showingTutorial = true }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Settings", systemImage: "gearshape") { showingSettings = true }
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
                TourView(tour: tour)
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
}
