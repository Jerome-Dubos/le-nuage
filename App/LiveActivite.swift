import ActivityKit
import Foundation

// Pilotage de la Live Activity depuis l'app. On démarre/actualise avec la météo de la
// position GPS (page 0). Respecte le réglage utilisateur ; termine si désactivé.
enum LiveActivite {
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
        // périme au bout d'1 h (iOS grise l'activité) → sera rafraîchie à la prochaine ouverture
        let content = ActivityContent(state: state, staleDate: Date().addingTimeInterval(3600))

        if let act = Activity<NuageActivityAttributes>.activities.first {
            Task { await act.update(content) }
        } else {
            _ = try? Activity.request(attributes: NuageActivityAttributes(), content: content)
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
