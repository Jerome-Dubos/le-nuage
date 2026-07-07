import Foundation

// Tenue météo du nuage : un accessoire choisi selon la météo EN DIRECT (réglable).
// Le visuel est un SVG (viewBox 512×400) superposé au nuage : `accessoires/<nom>` dans
// l'asset catalog (widget) et `svg-accessoires/<nom>.svg` dans le bundle (app).
enum Tenue: String, CaseIterable {
    case lunettes   // chaud & ensoleillé
    case bonnet     // froid ou neige
    case parapluie  // pluie

    var asset: String { "accessoires/\(rawValue)" }

    // Choix selon le code météo (WMO), la température et le jour/nuit.
    // Priorité : pluie > neige/froid > chaud ensoleillé. nil = tenue neutre ou désactivé.
    static func pour(code: Int, temp: Int, jour: Bool) -> Tenue? {
        guard Reglages.tenues else { return nil }
        let pluie: Set<Int> = [51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99]
        let neige: Set<Int> = [71, 73, 75, 77, 85, 86]
        if pluie.contains(code) { return .parapluie }
        if neige.contains(code) || temp <= 4 { return .bonnet }
        if temp >= 26, jour, [0, 1, 2].contains(code) { return .lunettes }
        return nil
    }
}
