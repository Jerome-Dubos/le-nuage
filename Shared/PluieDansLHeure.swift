import Foundation

// Pluie dans l'heure depuis minutely_15 (brief §4). Transposition 1:1 du JS :
// on regarde les 4 créneaux de 15 min à venir, et on renvoie le délai au premier
// créneau pluvieux.
enum PluieDansLHeure {
    enum Resultat: Equatable {
        case pluie(dans: Int)  // minutes avant la pluie
        case sec
        case indispo
    }

    static func calcule(_ meteo: Meteo, maintenant: Date = Date()) -> Resultat {
        guard let m = meteo.minutely_15 else { return .indispo }
        let now = maintenant.timeIntervalSince1970
        let temps = m.time.map { Horodatage.local($0)?.timeIntervalSince1970 }

        // premier créneau de 15 min pas encore terminé
        guard let debut = temps.firstIndex(where: { ($0 ?? 0) + 15 * 60 > now }) else { return .indispo }
        let fin = min(debut + 4, m.precipitation.count)

        for i in debut..<fin {
            if m.precipitation[i] > 0, let t = temps[i] {
                let dans = max(0, Int(((t - now) / 60).rounded()))
                return .pluie(dans: dans)
            }
        }
        return .sec
    }
}
