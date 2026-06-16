import Foundation
import CoreLocation

// L'app possède la localisation (autorisation When In Use) et écrit les coordonnées
// actives dans l'App Group. Le widget, lui, ne fait que LIRE ces coordonnées
// (CoreLocation est peu fiable en extension) — cf. brief §8.
final class LocationProvider: NSObject, ObservableObject, CLLocationManagerDelegate {
    static let cleLat = "lat"
    static let cleLon = "lon"

    // Repli Schiltigheim si aucune position n'est encore disponible.
    static let repli = Coordonnees(latitude: 48.6056, longitude: 7.7497)

    private let manager = CLLocationManager()
    private let geocoder = CLGeocoder()
    @Published private(set) var coords: Coordonnees
    // Cap du téléphone (degrés, 0 = nord) pour orienter la boussole en temps réel.
    @Published private(set) var cap: Double = 0
    // Nom de la ville courante (reverse-geocoding), pour l'entête de la page GPS.
    @Published private(set) var nomLieu: String?

    override init() {
        coords = LocationProvider.lectureAppGroup() ?? LocationProvider.repli
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyKilometer
        manager.headingFilter = 2  // ne notifie qu'au-delà de 2° de rotation
    }

    func demande() {
        manager.requestWhenInUseAuthorization()
        manager.requestLocation()
        if CLLocationManager.headingAvailable() { manager.startUpdatingHeading() }
    }

    func locationManager(_ m: CLLocationManager, didUpdateHeading h: CLHeading) {
        let v = h.trueHeading >= 0 ? h.trueHeading : h.magneticHeading
        guard v >= 0 else { return }
        DispatchQueue.main.async { self.cap = v }
    }

    func locationManagerDidChangeAuthorization(_ m: CLLocationManager) {
        switch m.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            m.requestLocation()
        default:
            break
        }
    }

    func locationManager(_ m: CLLocationManager, didUpdateLocations locs: [CLLocation]) {
        guard let loc = locs.last else { return }
        let c = Coordonnees(latitude: loc.coordinate.latitude, longitude: loc.coordinate.longitude)
        LocationProvider.ecritAppGroup(c)
        DispatchQueue.main.async { self.coords = c }
        geocoder.reverseGeocodeLocation(loc) { [weak self] places, _ in
            let nom = places?.first?.locality ?? places?.first?.administrativeArea
            DispatchQueue.main.async { self?.nomLieu = nom }
        }
    }

    func locationManager(_ m: CLLocationManager, didFailWithError error: Error) {
        // On conserve la dernière position connue / le repli.
    }

    // MARK: - Pont App Group

    static func ecritAppGroup(_ c: Coordonnees) {
        let d = AppGroupe.defaults
        d.set(c.latitude, forKey: cleLat)
        d.set(c.longitude, forKey: cleLon)
    }

    static func lectureAppGroup() -> Coordonnees? {
        let d = AppGroupe.defaults
        guard d.object(forKey: cleLat) != nil, d.object(forKey: cleLon) != nil else { return nil }
        return Coordonnees(latitude: d.double(forKey: cleLat), longitude: d.double(forKey: cleLon))
    }
}
