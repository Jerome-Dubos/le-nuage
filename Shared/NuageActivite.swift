import ActivityKit
import Foundation

// Données de la Live Activity (Dynamic Island + écran verrouillé). Partagé app ↔ widget :
// l'app publie/actualise l'état, l'extension widget en dessine l'UI.
struct NuageActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        var temp: Int
        var label: String
        var asset: String   // ex. "taquin/radieux_nuit"
        var vanne: String
        var min: Int
        var max: Int
        var nuit: Bool
        var tonRaw: String  // pour adapter la palette
    }
}
