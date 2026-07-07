import Foundation

// Conteneur partagé app ↔ widget. Coordonnées, ton actif et historique des vannes
// y transitent (cf. brief §8). Repli sur .standard si l'App Group n'est pas dispo.
enum AppGroupe {
    static let identifiant = "group.com.jeromedubos.lenuage"

    static var defaults: UserDefaults {
        UserDefaults(suiteName: identifiant) ?? .standard
    }
}

// Ton commutable taquin / doux (le toggle réglages arrive au jalon 6). Chaque ton
// pointe vers sa banque de vannes embarquée et, plus tard, son jeu de PNG.
enum Ton: String {
    case taquin
    case doux

    var fichierVannes: String {
        switch self {
        case .taquin: return "vannes-taquines"
        case .doux: return "messages-doux"
        }
    }
}

// Une date spéciale nommée (anniversaire, fête…) que le nuage célèbre.
struct Anniversaire: Codable, Identifiable, Equatable {
    var id = UUID()
    var nom: String
    var date: String  // "MM-dd"
}

// Un lieu enregistré (en plus de la position GPS, toujours en première page).
struct Lieu: Codable, Identifiable, Equatable {
    var id = UUID()
    var nom: String
    var lat: Double
    var lon: Double
}

enum Reglages {
    static let cleTon = "ton"

    static var ton: Ton {
        get { Ton(rawValue: AppGroupe.defaults.string(forKey: cleTon) ?? "") ?? .taquin }
        set { AppGroupe.defaults.set(newValue.rawValue, forKey: cleTon) }
    }

    // Dates spéciales configurables (easter eggs) : liste générique nom + date "MM-dd",
    // gérable par l'utilisateur → distribuable. Migre les anciennes clés moi/chérie.
    static var anniversaires: [Anniversaire] {
        get {
            let d = AppGroupe.defaults
            if let data = d.data(forKey: "anniversaires"),
               let liste = try? JSONDecoder().decode([Anniversaire].self, from: data) {
                return liste
            }
            // migration unique depuis l'ancien format (deux clés en dur)
            var liste: [Anniversaire] = []
            if let m = d.string(forKey: "anniv-moi") { liste.append(Anniversaire(nom: "Mon anniversaire", date: m)) }
            if let c = d.string(forKey: "anniv-cherie") { liste.append(Anniversaire(nom: "Ma chérie", date: c)) }
            return liste
        }
        set {
            AppGroupe.defaults.set(try? JSONEncoder().encode(newValue), forKey: "anniversaires")
        }
    }

    // Live Activity (Dynamic Island) activée ou non. Opt-in.
    static var liveActivite: Bool {
        get { AppGroupe.defaults.bool(forKey: "live-activite") }
        set { AppGroupe.defaults.set(newValue, forKey: "live-activite") }
    }

    // Tenues météo du nuage (bonnet, lunettes, parapluie, écharpe). Activé par défaut.
    static var tenues: Bool {
        get { AppGroupe.defaults.object(forKey: "tenues-meteo") as? Bool ?? true }
        set { AppGroupe.defaults.set(newValue, forKey: "tenues-meteo") }
    }

    // Réveil du nuage : notif quotidienne. Désactivé par défaut.
    static var reveil: Bool {
        get { AppGroupe.defaults.bool(forKey: "reveil") }
        set { AppGroupe.defaults.set(newValue, forKey: "reveil") }
    }
    // Heure du réveil, en minutes depuis minuit (défaut 8 h 00).
    static var reveilHeure: Int {
        get { AppGroupe.defaults.object(forKey: "reveil-heure") as? Int ?? 8 * 60 }
        set { AppGroupe.defaults.set(newValue, forKey: "reveil-heure") }
    }

    // Lieux enregistrés (en plus de la position GPS). Partagés app ↔ widget.
    static var lieux: [Lieu] {
        get {
            guard let data = AppGroupe.defaults.data(forKey: "lieux"),
                  let liste = try? JSONDecoder().decode([Lieu].self, from: data) else { return [] }
            return liste
        }
        set { AppGroupe.defaults.set(try? JSONEncoder().encode(newValue), forKey: "lieux") }
    }

    // Conversions "MM-dd" ↔ Date (année courante, pour les DatePicker)
    static func dateAnniv(_ mmdd: String) -> Date {
        let p = mmdd.split(separator: "-").compactMap { Int($0) }
        var comp = Calendar.current.dateComponents([.year], from: Date())
        comp.month = p.first ?? 1
        comp.day = p.count > 1 ? p[1] : 1
        return Calendar.current.date(from: comp) ?? Date()
    }
    static func mmdd(_ date: Date) -> String {
        let c = Calendar.current.dateComponents([.month, .day], from: date)
        return String(format: "%02d-%02d", c.month ?? 1, c.day ?? 1)
    }
}
