import WidgetKit
import SwiftUI

// MARK: - Données d'une entrée

struct NuageEntry: TimelineEntry {
    let date: Date
    let ton: Ton
    let contenu: Contenu?  // nil → widget d'erreur

    struct Contenu {
        let asset: String          // ex. "taquin/radieux_nuit"
        let label: String
        let temp: Int
        let min: Int
        let max: Int
        let ressenti: Int
        let pluie: PluieDansLHeure.Resultat
        let vanne: String
        let nuit: Bool
    }
}

// MARK: - TimelineProvider

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> NuageEntry {
        NuageEntry(date: Date(), ton: .taquin, contenu: NuageEntry.exemple)
    }

    func getSnapshot(in context: Context, completion: @escaping (NuageEntry) -> Void) {
        completion(NuageEntry(date: Date(), ton: Reglages.ton, contenu: NuageEntry.exemple))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<NuageEntry>) -> Void) {
        // Le widget LIT les coordonnées et le ton dans l'App Group (jamais de GPS en extension).
        let coords = LocationProvider.lectureAppGroup() ?? LocationProvider.repli
        let ton = Reglages.ton
        Task {
            do {
                let meteo = try await WeatherService.charge(coords: coords)
                let entry = NuageEntry(date: Date(), ton: ton, contenu: NuageEntry.construit(meteo, ton: ton))
                // Refresh ~30 min, dans le budget iOS (cf. brief §6).
                completion(Timeline(entries: [entry], policy: .after(Date().addingTimeInterval(30 * 60))))
            } catch {
                let entry = NuageEntry(date: Date(), ton: ton, contenu: nil)
                completion(Timeline(entries: [entry], policy: .after(Date().addingTimeInterval(10 * 60))))
            }
        }
    }
}

extension NuageEntry {
    static func construit(_ m: Meteo, ton: Ton) -> Contenu {
        let c = m.current
        let nuit = c.is_day == 0
        let etat = WMO.etatNuage(c.weather_code)
        return Contenu(
            asset: "\(ton.rawValue)/\(etat)\(nuit ? "_nuit" : "")",
            label: WMO.info(c.weather_code, isDay: c.is_day).label,
            temp: Int(c.temperature_2m.rounded()),
            min: Int(m.daily.temperature_2m_min[0].rounded()),
            max: Int(m.daily.temperature_2m_max[0].rounded()),
            ressenti: Int(c.apparent_temperature.rounded()),
            pluie: PluieDansLHeure.calcule(m),
            vanne: Repliques.choisit(pour: m, ton: ton),
            nuit: nuit
        )
    }

    // Aperçu/placeholder hors-ligne (galerie de widgets, chargement).
    static let exemple = Contenu(
        asset: "taquin/radieux",
        label: "Ciel clair",
        temp: 21, min: 14, max: 24, ressenti: 20,
        pluie: .sec,
        vanne: "Pas l'ombre d'un nuage. Sauf moi. Je suis l'exception.",
        nuit: false
    )
}

// MARK: - Vue

struct LeNuageWidgetEntryView: View {
    @Environment(\.colorScheme) private var scheme
    var entry: NuageEntry

    var body: some View {
        if let c = entry.contenu {
            contenu(c)
        } else {
            erreur()
        }
    }

    // La nuit force le sombre ; sinon on suit iOS.
    private func sombre(_ nuit: Bool) -> Bool { nuit || scheme == .dark }

    // Police manuscrite en doux, arrondie système en taquin.
    private func police(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        if let nom = PaletteTon.pour(entry.ton).policeWidget {
            let gras = weight == .bold || weight == .semibold || weight == .heavy
            return .custom(gras ? "\(nom)-Bold" : "\(nom)-Light", size: size)
        }
        return .system(size: size, weight: weight, design: .rounded)
    }

    @ViewBuilder
    private func contenu(_ c: NuageEntry.Contenu) -> some View {
        let palette = PaletteTon.pour(entry.ton)
        let dark = sombre(c.nuit)
        let ink = dark ? Color(hex: palette.corps) : Color(hex: palette.encre)

        VStack(alignment: .leading, spacing: 7) {
            HStack(alignment: .center, spacing: 14) {
                Image(c.asset)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 88, height: 68)

                VStack(alignment: .leading, spacing: 0) {
                    Text(c.label)
                        .font(police(11, .medium))
                        .foregroundStyle(ink.opacity(0.6))
                        .lineLimit(1)
                    Text("\(c.temp)°")
                        .font(police(34, .bold))
                        .foregroundStyle(ink)
                    Text("↓ \(c.min)°  ↑ \(c.max)°  ·  Ress. \(c.ressenti)°")
                        .font(police(12, .medium))
                        .foregroundStyle(ink.opacity(0.75))
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                    pluieVue(c.pluie, ink: ink)
                }

                Spacer(minLength: 0)
            }

            Text("« \(c.vanne) »")
                .font(police(12.5).italic())
                .foregroundStyle(ink.opacity(0.9))
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .minimumScaleFactor(0.8)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .center)
        }
        .padding(EdgeInsets(top: 14, leading: 16, bottom: 14, trailing: 16))
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
        .overlay(alignment: .topTrailing) {
            Button(intent: NouvelleVanneIntent()) {
                Image(systemName: "sparkles")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(ink.opacity(0.45))
            }
            .buttonStyle(.plain)
            .padding(EdgeInsets(top: 12, leading: 0, bottom: 0, trailing: 14))
        }
        .overlay {
            if palette.croquis {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(ink.opacity(0.3), style: StrokeStyle(lineWidth: 1.4, dash: [5, 4]))
                    .padding(7)
            }
        }
        .containerBackground(for: .widget) {
            LinearGradient(
                colors: dark ? [Color(hex: palette.fondHautSombre), Color(hex: palette.fondBasSombre)]
                             : [Color(hex: palette.fondHautClair), Color(hex: palette.fondBasClair)],
                startPoint: .top, endPoint: .bottom
            )
        }
    }

    @ViewBuilder
    private func pluieVue(_ pluie: PluieDansLHeure.Resultat, ink: Color) -> some View {
        switch pluie {
        case .pluie(let dans):
            Text(dans <= 1 ? "☔ Pluie imminente" : "☔ Pluie dans ~\(dans) min")
                .font(police(11, .medium))
                .foregroundStyle(ink)
                .padding(.top, 3)
        case .sec:
            Text("Pas de pluie dans l'heure")
                .font(police(11, .medium))
                .foregroundStyle(ink.opacity(0.6))
                .padding(.top, 3)
        case .indispo:
            EmptyView()
        }
    }

    @ViewBuilder
    private func erreur() -> some View {
        let palette = PaletteTon.pour(entry.ton)
        let dark = scheme == .dark
        let ink = dark ? Color(hex: palette.corps) : Color(hex: palette.encre)
        HStack(alignment: .center, spacing: 14) {
            Image("\(entry.ton.rawValue)/flippe")
                .resizable()
                .scaledToFit()
                .frame(width: 88, height: 68)
            Text("Le wifi des nuages est en rade. Réessaie dans un moment.")
                .font(police(13).italic())
                .foregroundStyle(ink)
            Spacer(minLength: 0)
        }
        .padding(EdgeInsets(top: 14, leading: 16, bottom: 14, trailing: 16))
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
        .containerBackground(for: .widget) {
            dark ? Color(hex: palette.fondHautSombre) : Color(hex: palette.ombre)
        }
    }
}

// MARK: - Déclaration du widget

struct LeNuageWidget: Widget {
    let kind = "LeNuageWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            LeNuageWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Le Nuage")
        .description("La météo, en plus taquin.")
        .supportedFamilies([.systemMedium])
        .contentMarginsDisabled()
    }
}

@main
struct LeNuageWidgetBundle: WidgetBundle {
    var body: some Widget {
        LeNuageWidget()
    }
}
