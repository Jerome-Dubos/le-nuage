import ActivityKit
import Foundation

// Pilotage de la Live Activity depuis l'app. On démarre/actualise avec la météo de la
// position GPS (page 0). Respecte le réglage utilisateur ; termine si désactivé.
//
// Comportement « poli » voulu : le nuage fait une apparition brève dans l'îlot puis
// s'efface, pour ne jamais squatter la place des autres apps. Il réapparaît à chaque
// actualisation météo (ouverture de l'app) — pas de réveil autonome possible en sideload.
enum LiveActivite {
    // Durée pendant laquelle le nuage reste visible avant de s'effacer tout seul.
    static let dureeVisible: TimeInterval = 90

    static func majDepuis(_ m: Meteo, ton: Ton) {
        guard Reglages.liveActivite else { termine(); return }
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }

        let c = m.current
        let nuit = c.is_day == 0
        let etat = WMO.etatNuage(c.weather_code)
        let state = NuageActivityAttributes.ContentState(
            temp: Int(c.temperature_2m.rounded()),
            label: WMO.info(c.weather_code, isDay: c.is_day).label,
            asset: "\(ton.rawValue)/\(etat)\(nuit ? "_nuit" : "")",
            vanne: Repliques.choisit(pour: m, ton: ton),
            min: Int(m.daily.temperature_2m_min[0].rounded()),
            max: Int(m.daily.temperature_2m_max[0].rounded()),
            nuit: nuit,
            tonRaw: ton.rawValue
        )
        // périme (donc s'estompe) à la fin de la fenêtre de visibilité
        let content = ActivityContent(state: state, staleDate: Date().addingTimeInterval(dureeVisible))

        Task {
            // repart d'une activité neuve à chaque passage → réapparition franche
            for a in Activity<NuageActivityAttributes>.activities {
                await a.end(nil, dismissalPolicy: .immediate)
            }
            guard let act = try? Activity.request(attributes: NuageActivityAttributes(), content: content)
            else { return }
            // s'efface de lui-même au bout de dureeVisible, libérant l'îlot pour les autres apps
            try? await Task.sleep(nanoseconds: UInt64(dureeVisible * 1_000_000_000))
            await act.end(nil, dismissalPolicy: .immediate)
        }
    }

    static func termine() {
        Task {
            for a in Activity<NuageActivityAttributes>.activities {
                await a.end(nil, dismissalPolicy: .immediate)
            }
        }
    }
}
