import AppIntents
import WidgetKit

// Bouton interactif du widget (iOS 17+) : taper sert une nouvelle vanne sans ouvrir
// l'app. WidgetKit recharge la timeline après l'intent → getTimeline repioche une
// vanne (tirage sans répétition) et rafraîchit la météo au passage.
struct NouvelleVanneIntent: AppIntent {
    static var title: LocalizedStringResource = "Nouvelle vanne"
    static var description = IntentDescription("Faire dire une nouvelle réplique au nuage.")

    func perform() async throws -> some IntentResult {
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}
