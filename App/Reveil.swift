import Foundation
import UserNotifications

// Réveil du nuage : une notification locale quotidienne à l'heure choisie, avec une
// petite phrase selon le ton. Aucun serveur — tout est planifié en local. Opt-in.
enum Reveil {
    static let identifiant = "reveil-nuage"

    // À appeler quand le réglage change et au lancement de l'app (rafraîchit la phrase).
    static func applique() {
        annule()
        guard Reglages.reveil else { return }
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { ok, _ in
            if ok { planifie() }
        }
    }

    private static func planifie() {
        let contenu = UNMutableNotificationContent()
        contenu.title = "Le Nuage ☁️"
        contenu.body = phrase(ton: Reglages.ton)
        contenu.sound = .default

        var quand = DateComponents()
        quand.hour = Reglages.reveilHeure / 60
        quand.minute = Reglages.reveilHeure % 60
        let trigger = UNCalendarNotificationTrigger(dateMatching: quand, repeats: true)
        let requete = UNNotificationRequest(identifier: identifiant, content: contenu, trigger: trigger)
        UNUserNotificationCenter.current().add(requete)
    }

    static func annule() {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [identifiant])
    }

    // Ponts avec le DatePicker des Réglages (heure stockée en minutes depuis minuit).
    static var heureDate: Date {
        var c = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        c.hour = Reglages.reveilHeure / 60
        c.minute = Reglages.reveilHeure % 60
        return Calendar.current.date(from: c) ?? Date()
    }

    static func minutes(de date: Date) -> Int {
        let c = Calendar.current.dateComponents([.hour, .minute], from: date)
        return (c.hour ?? 8) * 60 + (c.minute ?? 0)
    }

    private static func phrase(ton: Ton) -> String {
        let taquin = [
            "Debout. Le ciel est déjà réveillé, lui.",
            "Nouvelle journée. Essaie de ne pas la gâcher.",
            "Il fait un temps… viens voir, j'ai un avis dessus.",
            "Réveil. J'ai une vanne toute fraîche sur ta journée.",
        ]
        let doux = [
            "Bonjour toi. Passe une journée toute douce.",
            "Un nouveau jour tout neuf. Prends soin de toi.",
            "Coucou ! Viens voir le temps qu'il fait aujourd'hui.",
            "Bien réveillé ? Je te souhaite une belle journée.",
        ]
        return (ton == .doux ? doux : taquin).randomElement() ?? "Bonjour ☁️"
    }
}
