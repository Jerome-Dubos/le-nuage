import Foundation

// Banque de vannes + tirage sans répétition sur la journée (brief §10).
// Le texte vit dans les JSON embarqués (un par ton), plus dans le code.
enum Repliques {
    static let seuilCanicule = 32.0  // °C — pool chaleur prioritaire
    static let seuilGel = -2.0       // °C — pool froid prioritaire

    static func banque(ton: Ton) -> [String: [String]] {
        guard let url = Bundle.main.url(forResource: ton.fichierVannes, withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let dict = try? JSONDecoder().decode([String: [String]].self, from: data)
        else { return [:] }
        return dict
    }

    // Pool actif : seuils chaleur/froid prioritaires, sinon groupe météo ;
    // les vannes de nuit s'ajoutent quand is_day == 0.
    static func pool(pour meteo: Meteo, ton: Ton) -> [String] {
        let banque = banque(ton: ton)
        let c = meteo.current
        var pool: [String]
        if c.temperature_2m >= seuilCanicule { pool = banque["heat"] ?? [] }
        else if c.temperature_2m <= seuilGel { pool = banque["cold"] ?? [] }
        else { pool = banque[WMO.groupeReplique(c.weather_code)] ?? [] }
        if c.is_day == 0 { pool += banque["night"] ?? [] }
        return pool
    }

    private struct Historique: Codable {
        var jour: String
        var servies: [String]
    }

    // Historique gardé dans l'App Group (remplace le Keychain du JS), remis à zéro
    // chaque jour ou quand le pool courant est épuisé.
    static func choisit(pour meteo: Meteo, ton: Ton) -> String {
        let pool = pool(pour: meteo, ton: ton)
        guard !pool.isEmpty else { return "" }

        let cle = "vannes-du-jour"
        let defaults = AppGroupe.defaults
        let aujourdHui = jourCourant()

        var hist = Historique(jour: aujourdHui, servies: [])
        if let data = defaults.data(forKey: cle),
           let lu = try? JSONDecoder().decode(Historique.self, from: data),
           lu.jour == aujourdHui {
            hist = lu
        }

        var dispo = pool.filter { !hist.servies.contains($0) }
        if dispo.isEmpty {
            hist.servies.removeAll { pool.contains($0) }
            dispo = pool
        }

        let r = dispo.randomElement()!
        hist.servies.append(r)
        if hist.servies.count > 150 { hist.servies = Array(hist.servies.suffix(150)) }
        if let data = try? JSONEncoder().encode(hist) { defaults.set(data, forKey: cle) }

        return remplaceTemp(r, meteo: meteo)
    }

    static func remplaceTemp(_ r: String, meteo: Meteo) -> String {
        r.replacingOccurrences(of: "{TEMP}", with: "\(Int(meteo.current.temperature_2m.rounded())) °C")
    }

    private static func jourCourant() -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: Date())
    }
}
