import UIKit

// Produit le VIEW-MODEL (JSON) consommé par le cœur web partagé (web/nuage.js →
// window.rendre). Toute la logique métier (WMO, vannes, tenues, palettes, occasions,
// interprétation des stats) reste native ; le JS partagé construit le DOM à partir de
// ce JSON. Android produira le même JSON en Kotlin et chargera le même web/.
enum HTMLBuilder {

    // MARK: - View-model (contrat partagé avec web/nuage.js)

    struct VueModele: Encodable {
        let ton: String
        let croquis: Bool
        let police: String
        let varsClaires: String
        let varsSombres: String
        let nuit: Bool
        let lieu: String
        let estPosition: Bool
        let temp: Int
        let tempStyle: String?
        let label: String
        let ressenti: Int
        let nuageSVG: String
        let peutCligner: Bool
        let effet: String
        let deco: String
        let vanne: String
        let vannes: [String]
        let chips: [Chip]
        let agenda: AgendaVM?
        let stats: [Stat]
        let heures: [Heure]
        let jours: [Jour]

        struct Chip: Encodable { let ico: String; let texte: String }
        struct AgendaVM: Encodable {
            let lignes: [Ligne]; let note: String
            struct Ligne: Encodable { let heure: String; let titre: String }
        }
        struct Stat: Encodable {
            let ico: String; let val: String; let cat: String
            let qual: String; let couleur: String; let deg: Int?
        }
        struct Heure: Encodable { let label: String; let ico: String; let temp: Int; let proba: Int }
        struct Jour: Encodable { let nom: String; let ico: String; let min: Int; let max: Int; let proba: Int? }
    }

    // Point d'entrée : renvoie le view-model sérialisé en JSON (une ligne, prêt pour
    // window.rendre(<json>)). Remplace l'ancien build() qui crachait tout le HTML.
    static func viewModelJSON(_ data: Meteo, ton: Ton, lieu: String = "", estPosition: Bool = false,
                              agenda: [Agenda.Evenement]? = nil) -> String {
        let vm = viewModel(data, ton: ton, lieu: lieu, estPosition: estPosition, agenda: agenda)
        guard let d = try? JSONEncoder().encode(vm), let s = String(data: d, encoding: .utf8) else { return "{}" }
        return s
    }

    static func viewModel(_ data: Meteo, ton: Ton, lieu: String, estPosition: Bool,
                          agenda: [Agenda.Evenement]?) -> VueModele {
        let c = data.current
        let nuit = c.is_day == 0
        let info = WMO.info(c.weather_code, isDay: c.is_day)
        let jours = ["dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam."]

        // ambiance animée selon la météo
        let effet: String
        switch WMO.groupeReplique(c.weather_code) {
        case "clear": effet = nuit ? "etoiles" : "soleil"
        case "drizzle", "rain": effet = "pluie"
        case "snow": effet = "neige"
        case "storm": effet = "orage"
        case "fog": effet = "brume"
        default: effet = ""
        }

        // occasions spéciales (vanne thématique + décor festif)
        let (occVanne, deco) = occasionDuJour(ton: ton)
        let vanne = occVanne ?? Repliques.choisit(pour: data, ton: ton)

        // nuage : humeur (météo + canicule/grelotte) + tenue météo superposée
        let etat = WMO.humeur(code: c.weather_code, temp: Int(c.temperature_2m.rounded()))
        var nuageSVG = svgInline(etat: etat, nuit: nuit, ton: ton)
        if let tenue = Tenue.pour(code: c.weather_code,
                                  temp: Int(c.temperature_2m.rounded()),
                                  vent: Int(c.wind_speed_10m.rounded()),
                                  jour: c.is_day == 1) {
            nuageSVG = injecteTenue(nuageSVG, tenue: tenue)
        }
        let peutCligner = peutCligner(etat: etat, nuit: nuit)
        let vannes = Repliques.pool(pour: data, ton: ton).map { Repliques.remplaceTemp($0, meteo: data) }

        let vent = Int(c.wind_speed_10m.rounded())
        let chips = [
            VueModele.Chip(ico: "vent", texte: "\(vent) km/h"),
            VueModele.Chip(ico: "lever", texte: heureCourte(data.daily.sunrise.first)),
            VueModele.Chip(ico: "coucher", texte: heureCourte(data.daily.sunset.first)),
        ]

        // prochaines 12 heures
        let maintenant = Date()
        let depart = data.hourly.time.firstIndex {
            (Horodatage.local($0) ?? .distantPast) > maintenant.addingTimeInterval(-3600)
        }
        var heures: [VueModele.Heure] = []
        if let depart {
            let fin = min(depart + 12, data.hourly.time.count)
            for i in depart..<fin {
                let d = Horodatage.local(data.hourly.time[i]) ?? Date()
                let hr = Calendar.current.component(.hour, from: d)
                let h = i == depart ? "Maint." : "\(hr) h"
                let isDayH = (hr >= 21 || hr <= 6) ? 0 : 1
                let ico = WMO.info(data.hourly.weather_code[i], isDay: isDayH).icone
                let proba = data.hourly.precipitation_probability.indices.contains(i)
                    ? (data.hourly.precipitation_probability[i] ?? 0) : 0
                heures.append(.init(label: h, ico: ico,
                                    temp: Int(data.hourly.temperature_2m[i].rounded()), proba: proba))
            }
        }

        // 7 jours
        var joursVM: [VueModele.Jour] = []
        for i in data.daily.time.indices {
            let d = jourLocal(data.daily.time[i]) ?? Date()
            let nom: String
            if i == 0 {
                nom = "Aujourd'hui"
            } else {
                let wd = Calendar.current.component(.weekday, from: d) - 1
                let jour = Calendar.current.component(.day, from: d)
                nom = "\(jours[wd]) \(jour)"
            }
            let ico = WMO.info(data.daily.weather_code[i], isDay: 1).icone
            let proba = data.daily.precipitation_probability_max.indices.contains(i)
                ? data.daily.precipitation_probability_max[i] : nil
            joursVM.append(.init(nom: nom, ico: ico,
                                 min: Int(data.daily.temperature_2m_min[i].rounded()),
                                 max: Int(data.daily.temperature_2m_max[i].rounded()), proba: proba))
        }

        // carte « Détails » : valeur + interprétation colorée
        var stats: [VueModele.Stat] = []
        if let h = c.relative_humidity_2m {
            let (q, col) = humiditeInfo(h)
            stats.append(.init(ico: "goutte", val: "\(h) %", cat: "Humidité", qual: q, couleur: col, deg: nil))
        }
        if let dir = c.wind_direction_10m {
            stats.append(.init(ico: "boussole", val: cardinal(dir), cat: "Vent",
                               qual: directionNom(dir), couleur: cGris, deg: dir))
        }
        if let g = c.wind_gusts_10m {
            let (q, col) = rafalesInfo(g)
            stats.append(.init(ico: "vent", val: "\(Int(g.rounded())) km/h", cat: "Rafales", qual: q, couleur: col, deg: nil))
        }
        if let cc = c.cloud_cover {
            let (q, col) = couvertureInfo(cc)
            stats.append(.init(ico: "nuage", val: "\(cc) %", cat: "Couverture", qual: q, couleur: col, deg: nil))
        }
        if let uv = data.daily.uv_index_max?.first.flatMap({ $0 }) {
            let (q, col) = uvInfo(uv)
            stats.append(.init(ico: "soleil", val: "\(Int(uv.rounded()))", cat: "Indice UV", qual: q, couleur: col, deg: nil))
        }
        if let sec = data.daily.sunshine_duration?.first.flatMap({ $0 }) {
            stats.append(.init(ico: "soleil", val: "\(Int((sec / 3600).rounded())) h", cat: "Soleil", qual: "", couleur: "", deg: nil))
        }
        if let sec = data.daily.daylight_duration?.first.flatMap({ $0 }) {
            stats.append(.init(ico: "duree", val: dureeJour(sec), cat: "Durée du jour", qual: "", couleur: "", deg: nil))
        }
        if let depart, let vis = data.hourly.visibility, vis.indices.contains(depart), let v = vis[depart] {
            let km = Int((v / 1000).rounded())
            let (q, col) = visibiliteInfo(km)
            stats.append(.init(ico: "oeil", val: "\(km) km", cat: "Visibilité", qual: q, couleur: col, deg: nil))
        }

        // carte agenda (page GPS, réglage actif)
        let agendaVM = agenda.map { evs in
            VueModele.AgendaVM(lignes: evs.map { .init(heure: $0.heure, titre: $0.titre) },
                               note: Agenda.commentaire(evs, ton: ton))
        }

        // thème selon le ton ; température teintée aux extrêmes (hors mode croquis)
        let palette = PaletteTon.pour(ton)
        let tempStyle: String?
        if palette.croquis { tempStyle = nil }
        else if c.temperature_2m >= 32 { tempStyle = "#E0703A" }
        else if c.temperature_2m <= -2 { tempStyle = "#4F8AC9" }
        else { tempStyle = nil }

        return VueModele(
            ton: ton.rawValue, croquis: palette.croquis, police: palette.policeCSS,
            varsClaires: palette.varsClaires, varsSombres: palette.varsSombres, nuit: nuit,
            lieu: lieu, estPosition: estPosition,
            temp: Int(c.temperature_2m.rounded()), tempStyle: tempStyle,
            label: info.label, ressenti: Int(c.apparent_temperature.rounded()),
            nuageSVG: nuageSVG, peutCligner: peutCligner,
            effet: effet, deco: deco, vanne: vanne, vannes: vannes,
            chips: chips, agenda: agendaVM, stats: stats, heures: heures, jours: joursVM
        )
    }

    // MARK: - Nuage SVG (assemblé nativement, injecté dans le view-model)

    // Nuage en SVG inline (depuis le bundle) → animable (clignement) côté web.
    private static func svgInline(etat: String, nuit: Bool, ton: Ton) -> String {
        let dossier = ton == .doux ? "svg-doux" : "svg"
        let nom = "\(etat)\(nuit ? "_nuit" : "")"
        guard let url = Bundle.main.url(forResource: nom, withExtension: "svg", subdirectory: dossier),
              let svg = try? String(contentsOf: url, encoding: .utf8) else { return "" }
        return svg
    }

    // Superpose l'accessoire météo au nuage (même viewBox 512×400) : on insère son
    // contenu juste avant la fermeture du SVG.
    private static func injecteTenue(_ svg: String, tenue: Tenue) -> String {
        guard let url = Bundle.main.url(forResource: tenue.rawValue, withExtension: "svg", subdirectory: "svg-accessoires"),
              let contenu = try? String(contentsOf: url, encoding: .utf8),
              let ouvre = contenu.range(of: ">"),
              let clot = contenu.range(of: "</svg>", options: .backwards),
              let fermeture = svg.range(of: "</svg>", options: .backwards) else { return svg }
        let interne = String(contenu[ouvre.upperBound..<clot.lowerBound])
        return svg.replacingCharacters(in: fermeture, with: "<g class=\"tenue\">\(interne)</g></svg>")
    }

    // MARK: - Logique métier (occasions, stats, boussole, helpers)

    // Occasions spéciales : (vanne thématique selon le ton, décor festif). ("", "") sinon.
    private static func occasionDuJour(ton: Ton) -> (String?, String) {
        let comp = Calendar.current.dateComponents([.month, .day], from: Date())
        let taquin = ton == .taquin

        let mmdd = String(format: "%02d-%02d", comp.month ?? 0, comp.day ?? 0)
        if let occ = Reglages.anniversaires.first(where: { $0.date == mmdd }) {
            let qui = occ.nom.trimmingCharacters(in: .whitespaces)
            let suffixe = qui.isEmpty ? "" : " \(qui)"
            return (taquin ? "Joyeux anniversaire\(suffixe) ! Un an de plus, ça se fête (et ça se charrie). 🎂"
                           : "Joyeux anniversaire\(suffixe) 🎂 Une journée aussi douce que toi.", "confettis")
        }

        switch (comp.month, comp.day) {
        case (12, 24), (12, 25):
            return (taquin ? "Joyeux Noël. Le Père Noël t'a vu, va falloir t'expliquer."
                           : "Joyeux Noël ❄️ Plein de douceur sur toi aujourd'hui.", "neige")
        case (12, 31), (1, 1):
            return (taquin ? "Bonne année ! Tes résolutions tiendront jusqu'au 3, sois honnête."
                           : "Bonne année ✨ Que du beau et du doux pour toi.", "confettis")
        case (2, 14):
            return (taquin ? "Saint-Valentin. Même un nuage a trouvé l'amour avant toi. Courage."
                           : "Joyeuse Saint-Valentin 💗 Tu es adorable, ne l'oublie jamais.", "coeurs")
        case (4, 1):
            return (taquin ? "Il va faire 45° et neiger en même temps. … Poisson d'avril."
                           : "Petit poisson d'avril tout doux 🐟 Passe une jolie journée.", "")
        case (10, 31):
            return (taquin ? "Halloween. Le seul truc effrayant ici, c'est ta sonnerie de réveil."
                           : "Joyeux Halloween 🎃 Reste au chaud, petit fantôme tout mignon.", "")
        default:
            return (nil, "")
        }
    }

    // Durée du jour (secondes) → "15h47"
    private static func dureeJour(_ secondes: Double) -> String {
        let total = Int(secondes.rounded())
        return "\(total / 3600)h\(String(format: "%02d", (total % 3600) / 60))"
    }

    // Boussole : degrés → point cardinal abrégé + nom complet
    private static let cardinaux = ["N", "NE", "E", "SE", "S", "SO", "O", "NO"]
    private static let cardinauxNoms = ["Nord", "Nord-Est", "Est", "Sud-Est", "Sud", "Sud-Ouest", "Ouest", "Nord-Ouest"]
    private static func cardinal(_ deg: Int) -> String { cardinaux[idxCardinal(deg)] }
    private static func directionNom(_ deg: Int) -> String { cardinauxNoms[idxCardinal(deg)] }
    private static func idxCardinal(_ deg: Int) -> Int { Int((Double(deg) / 45).rounded()) % 8 }

    // Système de couleurs sémantiques (lecture d'un coup d'œil)
    private static let cVert = "#3FA46A", cJaune = "#C99A1E", cOrange = "#E07B39",
                       cRouge = "#D2544B", cBleu = "#4C86C6", cBleuF = "#3A6CA8",
                       cViolet = "#9B59B6", cGris = "#8B95A1"

    private static func humiditeInfo(_ h: Int) -> (String, String) {
        switch h { case ..<40: return ("Air sec", cOrange); case ..<65: return ("Confort", cVert)
                   case ..<80: return ("Humide", cBleu); default: return ("Très humide", cBleuF) }
    }
    private static func rafalesInfo(_ kmh: Double) -> (String, String) {
        switch kmh { case ..<20: return ("Légères", cVert); case ..<40: return ("Modérées", cJaune)
                     case ..<62: return ("Fortes", cOrange); default: return ("Violentes", cRouge) }
    }
    private static func couvertureInfo(_ pct: Int) -> (String, String) {
        switch pct { case ..<13: return ("Dégagé", cBleu); case ..<50: return ("Partiel", cVert)
                     case ..<88: return ("Nuageux", cGris); default: return ("Couvert", cGris) }
    }
    private static func uvInfo(_ uv: Double) -> (String, String) {
        switch uv { case ..<3: return ("Faible", cVert); case ..<6: return ("Modéré", cJaune)
                    case ..<8: return ("Élevé", cOrange); case ..<11: return ("Très élevé", cRouge)
                    default: return ("Extrême", cViolet) }
    }
    private static func visibiliteInfo(_ km: Int) -> (String, String) {
        switch km { case 20...: return ("Excellente", cVert); case 10..<20: return ("Bonne", cVert)
                    case 4..<10: return ("Moyenne", cJaune); case 1..<4: return ("Faible", cOrange)
                    default: return ("Brouillard", cRouge) }
    }

    // Yeux affichés ouverts (donc « clignables ») selon l'état et le jour/nuit.
    private static func peutCligner(etat: String, nuit: Bool) -> Bool {
        nuit ? ["endormi", "ronchon", "flippe"].contains(etat)
             : ["radieux", "emmitoufle", "endormi", "ronchon", "flippe"].contains(etat)
    }

    // "yyyy-MM-ddTHH:mm" → "HH:mm"
    private static func heureCourte(_ s: String?) -> String {
        guard let s, s.count >= 16 else { return "" }
        return String(s.dropFirst(11).prefix(5))
    }

    private static let jourFormateur: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = .current
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    private static func jourLocal(_ s: String) -> Date? { jourFormateur.date(from: s) }
}
