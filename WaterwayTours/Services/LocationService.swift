import CoreLocation
import Observation

@MainActor @Observable
final class LocationService: NSObject {
    private(set) var deviceLocation: CLLocation?
    /// A location the rider picked by hand (or the demo is driving), used instead of GPS while set.
    private(set) var manualLocation: CLLocation?
    private(set) var authorization: CLAuthorizationStatus
    private(set) var isTracking = false

    @ObservationIgnored private let manager = CLLocationManager()

    var location: CLLocation? { manualLocation ?? deviceLocation }
    var isManual: Bool { manualLocation != nil }

    var isDenied: Bool { authorization == .denied || authorization == .restricted }

    override init() {
        authorization = manager.authorizationStatus
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.distanceFilter = 5
        manager.activityType = .otherNavigation
    }

    /// Pretends the rider is at `coordinate` and starts the tour from there.
    func setManual(_ coordinate: CLLocationCoordinate2D) {
        manualLocation = CLLocation(coordinate: coordinate, altitude: 0, horizontalAccuracy: 5, verticalAccuracy: -1, timestamp: .now)
        isTracking = true
    }

    func clearManual() {
        manualLocation = nil
    }

    func start() {
        if isManual {
            isTracking = true
            return
        }
        if authorization == .notDetermined {
            manager.requestWhenInUseAuthorization()
        }
        manager.startUpdatingLocation()
        isTracking = true
    }

    func stop() {
        manager.stopUpdatingLocation()
        isTracking = false
    }
}

extension LocationService: CLLocationManagerDelegate {
    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let latest = locations.last else { return }
        Task { @MainActor in self.deviceLocation = latest }
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        Task { @MainActor in self.authorization = status }
    }
}
