import Foundation

// « Quoi de neuf » embarqué (offline, instantané au 1er lancement après une MAJ, sans
// serveur — cohérent avec l'ADN du Nuage). Le journal est écrit à la main à chaque
// release : une entrée par version, la plus récente en tête.
struct Nouveaute: Identifiable {
    let id = UUID()
    let version: String
    let titre: String
    let points: [Point]

    struct Point: Identifiable {
        let id = UUID()
        let symbole: String   // SF Symbol
        let texte: String
    }
}

// Emballage Identifiable pour présenter le lot via .sheet(item:).
struct JournalMaj: Identifiable {
    let id = UUID()
    let entrees: [Nouveaute]
}

enum Nouveautes {
    private static let cle = "derniere-version-vue"

    // Journal, de la plus récente à la plus ancienne.
    static let journal: [Nouveaute] = [
        Nouveaute(version: "1.14", titre: "Le nuage débarque dans iMessage", points: [
            .init(symbole: "message.fill", texte: "7 autocollants du nuage à envoyer dans tes conversations."),
        ]),
        Nouveaute(version: "1.12", titre: "Il ressent la météo", points: [
            .init(symbole: "thermometer.sun.fill", texte: "Humeurs canicule et grelotte selon la température."),
            .init(symbole: "cloud.rain.fill", texte: "Alerte pluie : une notif quand l'averse arrive dans l'heure."),
        ]),
        Nouveaute(version: "1.11", titre: "Ton petit monde", points: [
            .init(symbole: "calendar", texte: "Le nuage lit ton agenda du jour et le commente à sa façon."),
        ]),
        Nouveaute(version: "1.8", titre: "Il s'habille et te réveille", points: [
            .init(symbole: "figure.walk", texte: "Tenues météo : bonnet, lunettes, parapluie, écharpe."),
            .init(symbole: "alarm.fill", texte: "Réveil du nuage : un petit mot chaque matin, si tu veux."),
        ]),
    ]

    // Entrées à montrer au lancement : celles plus récentes que la dernière version vue.
    // Premier lancement (aucune version mémorisée) : on note la version courante, sans rien
    // afficher (on ne déroule pas tout l'historique à une nouvelle install).
    static func aMontrer() -> [Nouveaute] {
        let actuelle = MiseAJour.versionActuelle
        let vue = AppGroupe.defaults.string(forKey: cle)
        AppGroupe.defaults.set(actuelle, forKey: cle)

        guard let vue, vue != actuelle else { return [] }
        return journal.filter { MiseAJour.estPlusRecente($0.version, que: vue) }
    }
}
