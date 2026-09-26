import CoreLocation
import MapKit
import SwiftUI

struct TourView: View {
    let tour: Tour

    @State private var session: TourSession
    @State private var camera: MapCameraPosition
    @State private var confirmingReset = false
    @State private var pickingLocation = false
    @State private var demoTask: Task<Void, Never>?
    @Environment(LocationService.self) private var location
    @Environment(Narrator.self) private var narrator

    init(tour: Tour) {
        self.tour = tour
        _session = State(initialValue: TourSession(tour: tour))
        _camera = State(initialValue: .rect(tour.mapRect))
    }

    private var isDemoRunning: Bool { demoTask != nil }

    var body: some View {
        MapReader { proxy in
            Map(position: $camera) {
                UserAnnotation()

                if let manual = location.manualLocation {
                    Annotation("You", coordinate: manual.coordinate) {
                        Image(systemName: "figure.walk.circle.fill")
                            .font(.system(size: 30))
                            .foregroundStyle(.white, .orange)
                            .shadow(radius: 2)
                    }
                }

                MapPolyline(coordinates: session.stops.map(\.coordinate))
                    .stroke(.tint.opacity(0.7), style: StrokeStyle(lineWidth: 4, lineCap: .round, dash: [8, 6]))

                ForEach(session.stops) { stop in
                    MapCircle(center: stop.coordinate, radius: stop.radius)
                        .foregroundStyle(.teal.opacity(0.12))
                }

                ForEach(session.stops) { stop in
                    Annotation(stop.name, coordinate: stop.coordinate) {
                        StopPin(number: session.number(of: stop),
                                visited: session.visited.contains(stop.id),
                                skipped: session.skipped.contains(stop.id))
                            .onTapGesture { session.presentedStop = stop }
                    }
                }
            }
            .onTapGesture { point in
                guard pickingLocation, let coordinate = proxy.convert(point, from: .local) else { return }
                pickingLocation = false
                location.setManual(coordinate)
            }
        }
        .mapControls {
            MapUserLocationButton()
            MapCompass()
            MapScaleView()
        }
        .overlay(alignment: .top) { banner }
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 10) {
                if !session.isComplete, let next = session.nextStop {
                    OtterGuide(stop: next, isTracking: location.isTracking, isPaused: session.presentedStop != nil)
                        .padding(.horizontal)
                }
                TourStatusPanel(session: session)
            }
        }
        .navigationTitle(tour.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbarMenu }
        .confirmationDialog("Reset progress for this tour?", isPresented: $confirmingReset, titleVisibility: .visible) {
            Button("Reset", role: .destructive) { session.reset() }
        }
        .sheet(item: $session.presentedStop) { stop in
            StopSheet(stop: stop, session: session) { arrive(at: stop) }
        }
        .onChange(of: location.location) { _, newLocation in
            if let newLocation, session.needsOrdering { session.orderStops(from: newLocation) }
            guard location.isTracking, let newLocation, let stop = session.stopArrived(at: newLocation) else { return }
            arrive(at: stop)
        }
        .onDisappear {
            stopDemo()
            location.stop()
            location.clearManual()
            narrator.stop()
        }
    }

    // MARK: - Toolbar and banner

    private var toolbarMenu: some View {
        Menu {
            Section("Try it out") {
                if isDemoRunning {
                    Button("Stop demo", systemImage: "stop.circle", action: stopDemo)
                } else {
                    Button("Run a demo", systemImage: "play.circle", action: startDemo)
                }
            }
            Section("Location") {
                Button("Tap the map to set my location", systemImage: "hand.tap") {
                    stopDemo()
                    pickingLocation = true
                }
                Menu("Place me near a stop", systemImage: "mappin.and.ellipse") {
                    ForEach(session.stops) { stop in
                        Button(stop.name) { placeNear(stop) }
                    }
                }
                if location.isManual {
                    Button("Use my real location", systemImage: "location") {
                        stopDemo()
                        location.clearManual()
                    }
                }
            }
            Section {
                Button("Reset progress", systemImage: "arrow.counterclockwise", role: .destructive) {
                    confirmingReset = true
                }
            }
        } label: {
            Image(systemName: "ellipsis.circle")
        }
    }

    @ViewBuilder
    private var banner: some View {
        if pickingLocation {
            BannerView(text: "Tap the map to set where you are", buttonTitle: "Cancel") { pickingLocation = false }
        } else if isDemoRunning {
            BannerView(text: "Demo running", buttonTitle: "Stop", action: stopDemo)
        } else if location.isManual {
            BannerView(text: "Using a location you set", buttonTitle: "Use real") { location.clearManual() }
        }
    }

    // MARK: - Actions

    private func arrive(at stop: Stop) {
        session.markVisited(stop)
        session.presentedStop = stop
        narrator.speak(stop.narrationScript)
    }

    /// Puts the rider about 200 m south of `stop`, so the tour can be walked from there.
    private func placeNear(_ stop: Stop) {
        stopDemo()
        let coordinate = CLLocationCoordinate2D(latitude: stop.latitude - 0.0018, longitude: stop.longitude)
        location.setManual(coordinate)
        camera = .region(MKCoordinateRegion(center: stop.coordinate, latitudinalMeters: 1200, longitudinalMeters: 1200))
    }

    // MARK: - Demo

    private func startDemo() {
        stopDemo()
        narrator.stop()
        session.reset()
        demoTask = Task { await runDemo() }
    }

    private func stopDemo() {
        guard demoTask != nil else { return }
        demoTask?.cancel()
        demoTask = nil
        narrator.stop()
    }

    /// Walks a pretend rider from stop to stop so the whole tour plays out without leaving the couch.
    private func runDemo() async {
        let first = tour.stops[0]
        var here = CLLocationCoordinate2D(latitude: first.latitude - 0.003, longitude: first.longitude - 0.002)
        camera = .rect(tour.mapRect)
        location.setManual(here)
        await pause(2.5)

        while !Task.isCancelled, let next = session.nextStop {
            let start = here
            let steps = 40
            for step in 1...steps {
                if Task.isCancelled || session.presentedStop != nil { break }
                let t = Double(step) / Double(steps)
                here = CLLocationCoordinate2D(
                    latitude: start.latitude + (next.latitude - start.latitude) * t,
                    longitude: start.longitude + (next.longitude - start.longitude) * t)
                location.setManual(here)
                await pause(0.2)
            }
            guard !Task.isCancelled else { break }

            // Arrival opens the stop sheet and starts the narration; let it finish, then move on.
            await pause(1)
            var waited = 0.0
            while narrator.currentText != nil, waited < 60, !Task.isCancelled {
                await pause(0.5)
                waited += 0.5
            }
            await pause(2)
            session.presentedStop = nil
            await pause(1)
        }

        guard !Task.isCancelled else { return }
        demoTask = nil
    }

    private func pause(_ seconds: Double) async {
        try? await Task.sleep(for: .seconds(seconds))
    }
}

private struct BannerView: View {
    let text: String
    let buttonTitle: String
    let action: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Text(text).font(.subheadline.weight(.medium))
            Button(buttonTitle, action: action)
                .font(.subheadline.weight(.semibold))
                .buttonStyle(.bordered)
                .controlSize(.small)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(.regularMaterial, in: Capsule())
        .shadow(radius: 2)
        .padding(.top, 8)
    }
}

private struct StopPin: View {
    let number: Int
    let visited: Bool
    let skipped: Bool

    var body: some View {
        ZStack {
            Circle()
                .fill(visited ? Color.green : skipped ? Color.gray : Color.accentColor)
                .frame(width: 32, height: 32)
                .shadow(radius: 2)
            if visited {
                Image(systemName: "checkmark")
                    .font(.system(size: 14, weight: .bold))
            } else if skipped {
                Image(systemName: "forward.fill")
                    .font(.system(size: 12, weight: .bold))
            } else {
                Text("\(number)")
                    .font(.system(size: 15, weight: .bold, design: .rounded))
            }
        }
        .foregroundStyle(.white)
        .overlay(Circle().stroke(.white, lineWidth: 2))
    }
}

private struct TourStatusPanel: View {
    let session: TourSession
    @Environment(LocationService.self) private var location

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("\(session.visited.count) of \(session.stops.count) stops")
                    .font(.headline)
                if !session.skipped.isEmpty {
                    Text("· \(session.skipped.count) skipped")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Label("\(session.correctAnswers)/\(session.answers.count)", systemImage: "questionmark.bubble")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .accessibilityLabel("Trivia: \(session.correctAnswers) correct of \(session.answers.count) answered")
            }
            ProgressView(value: Double(session.visited.count + session.skipped.count), total: Double(session.stops.count))

            if session.isComplete {
                Text("Tour complete! You got \(session.correctAnswers) of \(session.answers.count) trivia questions right.")
                    .font(.subheadline)
            } else if let next = session.nextStop {
                HStack {
                    Button {
                        session.presentedStop = next
                    } label: {
                        HStack {
                            Text("Next: **\(next.name)**")
                                .lineLimit(1)
                            Spacer()
                            if let here = location.location {
                                Text(Measurement(value: here.distance(from: next.location), unit: UnitLength.meters)
                                    .formatted(.measurement(width: .abbreviated, usage: .road)))
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .font(.subheadline)
                    }
                    .buttonStyle(.plain)

                    Button("Skip", systemImage: "forward.fill") {
                        session.skip(next)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                    .accessibilityLabel("Skip \(next.name)")
                }
            }

            if location.isDenied && !location.isManual {
                Text("Location is off, so stops won't start automatically. Tap a stop on the map to play it, set a location from the ••• menu, or enable location in Settings.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            } else if !session.isComplete {
                Button {
                    location.isTracking ? location.stop() : location.start()
                } label: {
                    Label(location.isTracking ? "Pause tour" : "Start tour",
                          systemImage: location.isTracking ? "pause.fill" : "location.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20))
        .padding(.horizontal)
    }
}
