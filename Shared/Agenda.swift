import Foundation
import EventKit

// Agenda commenté : le nuage lit les évènements du jour (EventKit) et les commente à sa
// sauce. Lecture seule, en local, opt-in. Aucun évènement n'est stocké ni envoyé.
enum Agenda {
    struct Evenement { let heure: String; let titre: String }

    private static let store = EKEventStore()

    // Évènements d'aujourd'hui encore à venir (max 3). [] si refusé ou rien de prévu.
    static func duJour() async -> [Evenement] {
        guard Reglages.calendrier else { return [] }
        let ok = (try? await store.requestFullAccessToEvents()) ?? false
        guard ok else { return [] }

        let cal = Calendar.current
        let fin = cal.date(bySettingHour: 23, minute: 59, second: 59, of: Date()) ?? Date()
        let pred = store.predicateForEvents(withStart: Date(), end: fin, calendars: nil)

        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "fr_FR")
        fmt.dateFormat = "HH:mm"

        return store.events(matching: pred)
            .filter { !$0.isAllDay }
            .sorted { $0.startDate < $1.startDate }
            .prefix(3)
            .map { Evenement(heure: fmt.string(from: $0.startDate),
                             titre: ($0.title?.isEmpty == false ? $0.title! : "Évènement")) }
    }

    // Petit mot du nuage sur la journée, selon le ton.
    static func commentaire(_ events: [Evenement], ton: Ton) -> String {
        guard let p = events.first else {
            return ton == .taquin
                ? "Rien de prévu aujourd'hui. Le vide intersidéral. Ton domaine, quoi."
                : "Journée libre — profite-en pour souffler, tu le mérites."
        }
        let taquin = [
            "\(p.titre) à \(p.heure) ? Courage, je te fais de l'ombre.",
            "À \(p.heure), \(p.titre). J'espère pour toi que c'est plus fun que la météo.",
            "\(p.titre)… encore ? Respire un coup, j'arrive avec un ciel (presque) dégagé.",
        ]
        let doux = [
            "\(p.titre) à \(p.heure) — tu vas gérer, comme toujours. 💙",
            "À \(p.heure), \(p.titre). Je pense à toi, vas-y en douceur.",
            "Pense à \(p.titre) à \(p.heure). Prends bien soin de toi d'ici là.",
        ]
        return (ton == .taquin ? taquin : doux).randomElement() ?? ""
    }
}
