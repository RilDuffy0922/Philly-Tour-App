import MapKit
import SwiftUI

struct TourView: View {
    let tour: Tour

    @State private var session: TourSession
    @State private var camera: MapCameraPosition
    @State private var confirmingReset = false
    @Environment(LocationService.self) private var location
    @Environment(Narrator.self) private var narrator

    init(tour: Tour) {
        self.tour = tour
        _session = State(initialValue: TourSession(tour: tour))
        _camera = State(initialValue: .rect(tour.mapRect))
    }

    var body: some View {
        Map(position: $camera) {
            UserAnnotation()

            MapPolyline(coordinates: session.stops.map(\.coordinate))
                .stroke(.tint.opacity(0.7), style: StrokeStyle(lineWidth: 4, lineCap: .round, dash: [8, 6]))

            ForEach(session.stops) { stop in
                MapCircle(center: stop.coordinate, radius: stop.radius)
                    .foregroundStyle(.teal.opacity(0.12))
            }

            ForEach(session.stops) { stop in
                Annotation(stop.name, coordinate: stop.coordinate) {
                    StopPin(number: session.number(of: stop), visited: session.visited.contains(stop.id))
                        .onTapGesture { session.presentedStop = stop }
                }
            }
        }
        .mapControls {
            MapUserLocationButton()
            MapCompass()
            MapScaleView()
        }
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
        .toolbar {
            Menu {
                Button("Reset progress", systemImage: "arrow.counterclockwise", role: .destructive) {
                    confirmingReset = true
                }
            } label: {
                Image(systemName: "ellipsis.circle")
            }
        }
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
            location.stop()
            narrator.stop()
        }
    }

    private func arrive(at stop: Stop) {
        session.markVisited(stop)
        session.presentedStop = stop
        narrator.speak(stop.narrationScript)
    }
}

private struct StopPin: View {
    let number: Int
    let visited: Bool

    var body: some View {
        ZStack {
            Circle()
                .fill(visited ? Color.green : Color.accentColor)
                .frame(width: 32, height: 32)
                .shadow(radius: 2)
            if visited {
                Image(systemName: "checkmark")
                    .font(.system(size: 14, weight: .bold))
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
                Spacer()
                Label("\(session.correctAnswers)/\(session.answers.count)", systemImage: "questionmark.bubble")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .accessibilityLabel("Trivia: \(session.correctAnswers) correct of \(session.answers.count) answered")
            }
            ProgressView(value: Double(session.visited.count), total: Double(session.stops.count))

            if session.isComplete {
                Text("Tour complete! You got \(session.correctAnswers) of \(session.stops.count) trivia questions right.")
                    .font(.subheadline)
            } else if let next = session.nextStop {
                Button {
                    session.presentedStop = next
                } label: {
                    HStack {
                        Text("Next: **\(next.name)**")
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
            }

            if location.isDenied {
                Text("Location is off, so stops won't start automatically. Tap a stop on the map to play it, or enable location in Settings.")
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
