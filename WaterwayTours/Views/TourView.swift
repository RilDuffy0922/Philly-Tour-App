import CoreLocation
import MapKit
import SwiftUI

struct TourView: View {
    let tour: Tour
    /// True while the guided demo is running: progress isn't saved and the demo drives the screen.
    let isDemo: Bool

    @State private var session: TourSession
    @State private var camera: MapCameraPosition
    @State private var confirmingReset = false
    @State private var lastFix: CLLocation?
    @State private var visibleRegion: MKCoordinateRegion?
    @Environment(LocationService.self) private var location
    @Environment(Narrator.self) private var narrator
    @Environment(DemoController.self) private var demo

    init(tour: Tour, isDemo: Bool = false) {
        self.tour = tour
        self.isDemo = isDemo
        _session = State(initialValue: TourSession(tour: tour, persistent: !isDemo))
        _camera = State(initialValue: .rect(tour.mapRect))
    }

    var body: some View {
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
        .onMapCameraChange(frequency: .onEnd) { context in
            visibleRegion = context.region
        }
        .demoTarget(.map)
        .mapControls {
            MapUserLocationButton()
            MapCompass()
            MapScaleView()
        }
        .overlay(alignment: .top) { banner }
        .overlay(alignment: .topLeading) { zoomControls }
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 10) {
                if !session.isComplete, let next = session.nextStop {
                    OtterGuide(stop: next, isTracking: location.isTracking,
                               isPaused: session.presentedStop != nil || demo.isActive)
                        .padding(.horizontal)
                        // During the demo the walkthrough otter talks instead; this one only shows for its own step.
                        .opacity(demo.isActive && demo.current?.target != .otter ? 0 : 1)
                }
                TourStatusPanel(session: session) { openDirections(to: $0) }
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
            trackTravel(to: newLocation)
            if let newLocation, session.needsOrdering { session.orderStops(from: newLocation) }
            guard location.isTracking, let newLocation, let stop = session.stopArrived(at: newLocation) else { return }
            arrive(at: stop)
        }
        .onChange(of: location.isTracking) { lastFix = nil }
        .onAppear {
            if isDemo { demo.actionHandler = { await performDemo($0) } }
        }
        .onDisappear {
            demo.actionHandler = nil
            location.stop()
            location.clearManual()
            narrator.stop()
        }
    }

    // MARK: - Toolbar and banner

    private var toolbarMenu: some View {
        Menu {
            Section("Location") {
                Menu("Place me near a stop", systemImage: "mappin.and.ellipse") {
                    ForEach(session.stops) { stop in
                        Button(stop.name) { placeNear(stop) }
                    }
                }
                if location.isManual {
                    Button("Use my real location", systemImage: "location") {
                        lastFix = nil
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
                .demoTarget(.menuButton)
        }
    }

    @ViewBuilder
    private var banner: some View {
        if isDemo {
            BannerView(text: "Demo mode", buttonTitle: nil, action: {})
        } else if location.isManual {
            BannerView(text: "Using a location you set", buttonTitle: "Use real") {
                lastFix = nil
                location.clearManual()
            }
        }
    }

    private var zoomControls: some View {
        VStack(spacing: 0) {
            Button { zoom(by: 0.5) } label: {
                Image(systemName: "plus").frame(width: 44, height: 44)
            }
            .accessibilityLabel("Zoom in")
            Divider().frame(width: 28)
            Button { zoom(by: 2) } label: {
                Image(systemName: "minus").frame(width: 44, height: 44)
            }
            .accessibilityLabel("Zoom out")
        }
        .font(.title3.weight(.semibold))
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
        .shadow(radius: 2)
        .padding(.leading, 12)
        .padding(.top, 60)
    }

    // MARK: - Actions

    /// Zooms the map in (factor < 1) or out (factor > 1) around what's currently in view.
    private func zoom(by factor: Double) {
        guard let region = visibleRegion else { return }
        let span = MKCoordinateSpan(
            latitudeDelta: min(max(region.span.latitudeDelta * factor, 0.0005), 60),
            longitudeDelta: min(max(region.span.longitudeDelta * factor, 0.0005), 60))
        withAnimation { camera = .region(MKCoordinateRegion(center: region.center, span: span)) }
    }

    private func arrive(at stop: Stop) {
        session.markVisited(stop)
        session.presentedStop = stop
        // Show the stop itself, not the rider's position.
        withAnimation {
            camera = .region(MKCoordinateRegion(center: stop.coordinate, latitudinalMeters: 500, longitudinalMeters: 500))
        }
        if !isDemo { narrator.speak(stop.narrationScript) }
    }

    /// Adds the distance moved since the last good fix, ignoring GPS jumps and teleports.
    private func trackTravel(to newLocation: CLLocation?) {
        guard location.isTracking, let newLocation, newLocation.horizontalAccuracy >= 0, newLocation.horizontalAccuracy <= 50 else { return }
        if let lastFix {
            let moved = newLocation.distance(from: lastFix)
            if moved >= 3 && moved <= 300 { session.addTravel(moved) }
        }
        lastFix = newLocation
    }

    /// Puts the rider about 200 m south of `stop`, so the tour can be walked from there.
    private func placeNear(_ stop: Stop) {
        lastFix = nil
        let coordinate = CLLocationCoordinate2D(latitude: stop.latitude - 0.0018, longitude: stop.longitude)
        location.setManual(coordinate)
        camera = .region(MKCoordinateRegion(center: stop.coordinate, latitudinalMeters: 1200, longitudinalMeters: 1200))
    }

    /// Opens Apple Maps with directions from wherever the rider is to `stop`.
    private func openDirections(to stop: Stop) {
        let item = MKMapItem(placemark: MKPlacemark(coordinate: stop.coordinate))
        item.name = stop.name
        let mode = tour.mode == .bike ? MKLaunchOptionsDirectionsModeCycling : MKLaunchOptionsDirectionsModeWalking
        item.openInMaps(launchOptions: [MKLaunchOptionsDirectionsModeKey: mode])
    }

    // MARK: - Guided demo

    private func performDemo(_ action: DemoAction) async {
        switch action {
        case .walkToFirstStop:
            await walkToFirstStop()
        case .closeSheet:
            session.presentedStop = nil
            try? await Task.sleep(for: .seconds(0.6))
        }
    }

    /// Walks a pretend rider from a little way off to the first stop, so the arrival plays out for real.
    private func walkToFirstStop() async {
        let first = tour.stops[0]
        var here = CLLocationCoordinate2D(latitude: first.latitude - 0.003, longitude: first.longitude - 0.002)
        camera = .rect(tour.mapRect)
        lastFix = nil
        location.setManual(here)
        try? await Task.sleep(for: .seconds(1.5))

        guard let target = session.nextStop else { return }
        let start = here
        let steps = 40
        for step in 1...steps {
            if Task.isCancelled || session.presentedStop != nil { break }
            let t = Double(step) / Double(steps)
            here = CLLocationCoordinate2D(
                latitude: start.latitude + (target.latitude - start.latitude) * t,
                longitude: start.longitude + (target.longitude - start.longitude) * t)
            location.setManual(here)
            try? await Task.sleep(for: .seconds(0.2))
        }
        try? await Task.sleep(for: .seconds(1))
    }
}

private struct BannerView: View {
    let text: String
    let buttonTitle: String?
    let action: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Text(text).font(.subheadline.weight(.medium))
            if let buttonTitle {
                Button(buttonTitle, action: action)
                    .font(.subheadline.weight(.semibold))
                    .buttonStyle(.bordered)
                    .controlSize(.small)
            }
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
    let onDirections: (Stop) -> Void
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
                Text("Trivia \(session.correctAnswers)/\(session.answers.count)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .accessibilityLabel("Trivia: \(session.correctAnswers) correct of \(session.answers.count) answered")
            }
            .demoTarget(.stopsProgress)

            ProgressView(value: Double(session.visited.count + session.skipped.count), total: Double(session.stops.count))

            estimateRow

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
                    .demoTarget(.skip)
                }
            }

            if !session.isComplete, let next = session.nextStop {
                HStack(spacing: 10) {
                    Button {
                        onDirections(next)
                    } label: {
                        Label("Directions", systemImage: "arrow.triangle.turn.up.right.diamond.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.large)
                    .accessibilityLabel("Directions to \(next.name)")
                    .demoTarget(.directions)

                    if location.isDenied && !location.isManual {
                        Text("Location is off. Turn it on in Settings so stops start automatically.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    } else {
                        Button {
                            location.isTracking ? location.stop() : location.start()
                        } label: {
                            Label(location.isTracking ? "Pause tour" : "Start tour",
                                  systemImage: location.isTracking ? "pause.fill" : "location.fill")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .controlSize(.large)
                        .demoTarget(.startTour)
                    }
                }
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20))
        .padding(.horizontal)
    }

    /// Time left and finish time (like an arrival estimate), and miles traveled so far.
    private var estimateRow: some View {
        HStack(alignment: .firstTextBaseline) {
            Group {
                if let estimate = session.estimate(from: location.location) {
                    TimelineView(.periodic(from: .now, by: 30)) { context in
                        let finish = context.date.addingTimeInterval(estimate.seconds)
                        Label("\(Duration.seconds(estimate.seconds).formatted(.units(allowed: [.hours, .minutes], width: .abbreviated, maximumUnitCount: 2))) left · done \(finish.formatted(date: .omitted, time: .shortened))",
                              systemImage: "clock")
                    }
                } else {
                    Label("All done", systemImage: "checkmark.circle")
                }
            }
            .font(.subheadline)
            .demoTarget(.eta)

            Spacer()

            Label("\(Measurement(value: session.traveledMeters, unit: UnitLength.meters).formatted(.measurement(width: .abbreviated, usage: .road))) traveled",
                  systemImage: "figure.walk.motion")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .demoTarget(.traveled)
        }
    }
}
