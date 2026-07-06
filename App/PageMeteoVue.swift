import SwiftUI

// Une page météo (un lieu). Charge sa propre météo et rend le HTML partagé.
// Réutilisée pour la position GPS comme pour chaque lieu enregistré (système de pages).
struct PageMeteoVue: View {
    let coords: Coordonnees
    let nom: String
    let estPosition: Bool
    let ton: Ton
    let cap: Double
    var live: Bool = false  // état du réglage Dynamic Island (relance charge au changement)

    @State private var html: String?
    @State private var erreur = false

    var body: some View {
        contenu
            // recharge si le lieu, la position, le ton ou le réglage Live Activity changent
            .task(id: "\(coords.latitude),\(coords.longitude)|\(nom)|\(ton.rawValue)|\(live)") {
                await charge()
            }
    }

    @ViewBuilder
    private var contenu: some View {
        if let html {
            WebViewContainer(html: html, cap: cap).ignoresSafeArea()
        } else if erreur {
            VStack(spacing: 14) {
                Text("☁️").font(.system(size: 56))
                Text("Impossible de joindre la météo.\nVérifie ta connexion et réessaie.")
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
                Button("Réessayer") { erreur = false; Task { await charge() } }
                    .buttonStyle(.borderedProminent)
            }
            .padding()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private func charge() async {
        do {
            let m = try await WeatherService.charge(coords: coords)
            html = HTMLBuilder.build(m, ton: ton, lieu: nom, estPosition: estPosition)
            erreur = false
            // la Live Activity suit la position GPS (page 0)
            if estPosition { LiveActivite.majDepuis(m, ton: ton) }
        } catch {
            if html == nil { erreur = true }
        }
    }
}
