import Foundation

// Trois tables parallèles, transposées 1:1 du script JS (brief §5).
enum WMO {
    // (a) code → état du nuage (nom du PNG affiché)
    static func etatNuage(_ code: Int) -> String {
        switch code {
        case 0: return "radieux"
        case 1...3: return "detendu"
        case 45, 48: return "endormi"
        case 51...57: return "ronchon"
        case 61...67, 80...82: return "blase"
        case 71...77, 85, 86: return "emmitoufle"
        case let c where c >= 95: return "flippe"
        default: return "detendu"
        }
    }

    // (b) code → groupe de répliques
    static func groupeReplique(_ code: Int) -> String {
        switch code {
        case 0: return "clear"
        case 1...3: return "clouds"
        case 45, 48: return "fog"
        case 51...57: return "drizzle"
        case 61...67, 80...82: return "rain"
        case 71...77, 85, 86: return "snow"
        case let c where c >= 95: return "storm"
        default: return "clouds"
        }
    }

    // (c) code → libellé FR + nom d'icône SVG (vue app), avec variantes jour/nuit
    struct Info {
        let label: String
        let icone: String
    }

    static func info(_ code: Int, isDay: Int) -> Info {
        let nuit = isDay == 0
        switch code {
        case 0: return Info(label: "Ciel clair", icone: nuit ? "lune" : "soleil")
        case 1: return Info(label: "Plutôt dégagé", icone: nuit ? "lune" : "soleil")
        case 2: return Info(label: "Partiellement nuageux", icone: nuit ? "lune-nuage" : "soleil-nuage")
        case 3: return Info(label: "Couvert", icone: "nuage")
        case 45, 48: return Info(label: "Brouillard", icone: "brouillard")
        case 51...57: return Info(label: "Bruine", icone: "bruine")
        case 61...67: return Info(label: "Pluie", icone: "pluie")
        case 71...77: return Info(label: "Neige", icone: "neige")
        case 80...82: return Info(label: "Averses", icone: "pluie")
        case 85, 86: return Info(label: "Averses de neige", icone: "neige")
        case let c where c >= 95: return Info(label: "Orage", icone: "orage")
        default: return Info(label: "Variable", icone: "nuage")
        }
    }
}
