import Foundation
import BackgroundTasks
import UserNotifications

// Alerte pluie : iOS réveille l'app en arrière-plan (quand il le décide), on vérifie la
// pluie dans l'heure pour la position, et on notifie si besoin. Opt-in, aucun serveur.
enum AlertePluie {
    static let idTache = "com.jeromedubos.lenuage.pluie"

    // Planifie le prochain réveil (au moins ~30 min plus tard ; iOS ajuste).
    static func programme() {
        guard Reglages.alertePluie else { return }
        let requete = BGAppRefreshTaskRequest(identifier: idTache)
        requete.earliestBeginDate = Date(timeIntervalSinceNow: 30 * 60)
        try? BGTaskScheduler.shared.submit(requete)
    }

    static func annule() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: idTache)
    }

    // À l'activation du réglage : demande l'autorisation de notifier puis planifie.
    static func active() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { ok, _ in
            if ok { programme() }
        }
    }

    // Exécuté par iOS lors du réveil : on reprogramme, puis on vérifie la pluie.
    static func executer() async {
        programme()
        guard Reglages.alertePluie,
              let coords = LocationProvider.lectureAppGroup(),
              let meteo = try? await WeatherService.charge(coords: coords) else { return }

        guard case .pluie(let dans) = PluieDansLHeure.calcule(meteo) else { return }

        // anti-spam : au plus une alerte toutes les 2 h
        let maintenant = Date().timeIntervalSince1970
        guard maintenant - Reglages.derniereAlertePluie > 2 * 3600 else { return }
        Reglages.derniereAlertePluie = maintenant

        notifie(dans: dans, ton: Reglages.ton)
    }

    private static func notifie(dans: Int, ton: Ton) {
        let contenu = UNMutableNotificationContent()
        contenu.title = "Le Nuage ☔"
        let quand = dans <= 1 ? "maintenant" : "dans ~\(dans) min"
        contenu.body = ton == .taquin
            ? "Pluie \(quand). Rentre le linge. Et toi avec, tant qu'à faire."
            : "Pluie \(quand) — pense à prendre un parapluie. 💙"
        contenu.sound = .default
        let requete = UNNotificationRequest(identifier: "pluie-\(Int(Date().timeIntervalSince1970))",
                                            content: contenu, trigger: nil)
        UNUserNotificationCenter.current().add(requete)
    }
}
