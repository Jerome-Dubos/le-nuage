import SwiftUI

@main
struct LeNuageApp: App {
    var body: some Scene {
        WindowGroup {
            AppView()
        }
        // iOS réveille l'app pour vérifier la pluie à venir (alerte pluie opt-in).
        .backgroundTask(.appRefresh(AlertePluie.idTache)) {
            await AlertePluie.executer()
        }
    }
}
