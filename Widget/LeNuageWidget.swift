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
        let symbole: String        // symbole SF météo (écran verrouillé / accessoires)
        let tenue: String?         // accessoire météo superposé (nil = neutre)
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
            nuit: nuit,
            symbole: sfSymbole(c.weather_code, isDay: c.is_day),
            tenue: Tenue.pour(code: c.weather_code,
                              temp: Int(c.temperature_2m.rounded()),
                              vent: Int(c.wind_speed_10m.rounded()),
                              jour: c.is_day == 1)?.asset
        )
    }

    // Symbole SF météo, lisible en monochrome sur l'écran verrouillé (le PNG du nuage
    // y deviendrait une silhouette illisible).
    static func sfSymbole(_ code: Int, isDay: Int) -> String {
        let jour = isDay == 1
        switch code {
        case 0:                          return jour ? "sun.max.fill" : "moon.stars.fill"
        case 1, 2:                       return jour ? "cloud.sun.fill" : "cloud.moon.fill"
        case 3:                          return "cloud.fill"
        case 45, 48:                     return "cloud.fog.fill"
        case 51, 53, 55, 56, 57:         return "cloud.drizzle.fill"
        case 61, 63, 65, 66, 67, 80, 81, 82: return "cloud.rain.fill"
        case 71, 73, 75, 77, 85, 86:     return "cloud.snow.fill"
        case 95, 96, 99:                 return "cloud.bolt.rain.fill"
        default:                         return "cloud.fill"
        }
    }

    // Aperçu/placeholder hors-ligne (galerie de widgets, chargement).
    static let exemple = Contenu(
        asset: "taquin/radieux",
        label: "Ciel clair",
        temp: 21, min: 14, max: 24, ressenti: 20,
        pluie: .sec,
        vanne: "Pas l'ombre d'un nuage. Sauf moi. Je suis l'exception.",
        nuit: false,
        symbole: "sun.max.fill",
        tenue: nil
    )
}

// MARK: - Vue

struct LeNuageWidgetEntryView: View {
    @Environment(\.colorScheme) private var scheme
    @Environment(\.widgetFamily) private var family
    var entry: NuageEntry

    var body: some View {
        switch family {
        case .accessoryInline:      inline(entry.contenu)
        case .accessoryCircular:    circulaire(entry.contenu)
        case .accessoryRectangular: rectangulaire(entry.contenu)
        default:
            if let c = entry.contenu { contenu(c) } else { erreur() }
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

    // Widgets d'écran d'accueil (small / medium / large) : layout choisi selon la taille,
    // habillage commun (bouton vanne, bordure croquis, dégradé de fond).
    @ViewBuilder
    private func contenu(_ c: NuageEntry.Contenu) -> some View {
        let palette = PaletteTon.pour(entry.ton)
        let dark = sombre(c.nuit)
        let ink = dark ? Color(hex: palette.corps) : Color(hex: palette.encre)

        Group {
            switch family {
            case .systemSmall: petit(c, ink: ink)
            case .systemLarge: grand(c, ink: ink)
            default:           moyen(c, ink: ink)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
        .overlay(alignment: .topTrailing) {
            if family != .systemSmall {
                Button(intent: NouvelleVanneIntent()) {
                    Image(systemName: "sparkles")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(ink.opacity(0.45))
                }
                .buttonStyle(.plain)
                .padding(EdgeInsets(top: 12, leading: 0, bottom: 0, trailing: 14))
            }
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

    // Nuage habillé : image de base + accessoire météo superposé (même cadre → aligné).
    private func nuageHabille(_ c: NuageEntry.Contenu) -> some View {
        ZStack {
            Image(c.asset).resizable().scaledToFit()
            if let t = c.tenue { Image(t).resizable().scaledToFit() }
        }
    }

    // Medium : nuage + météo à gauche, vanne dessous.
    private func moyen(_ c: NuageEntry.Contenu, ink: Color) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            HStack(alignment: .center, spacing: 14) {
                nuageHabille(c)
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
    }

    // Small : nuage centré, température, résumé court (pas de vanne, place limitée).
    private func petit(_ c: NuageEntry.Contenu, ink: Color) -> some View {
        VStack(spacing: 2) {
            nuageHabille(c)
                .frame(height: 58)
            Text("\(c.temp)°")
                .font(police(30, .bold))
                .foregroundStyle(ink)
            Text(c.label)
                .font(police(11, .medium))
                .foregroundStyle(ink.opacity(0.7))
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Text("↓\(c.min)°  ↑\(c.max)°")
                .font(police(10, .medium))
                .foregroundStyle(ink.opacity(0.65))
                .lineLimit(1)
        }
        .padding(12)
    }

    // Large : grande scène, météo détaillée, vanne mise en avant.
    private func grand(_ c: NuageEntry.Contenu, ink: Color) -> some View {
        VStack(spacing: 10) {
            nuageHabille(c)
                .frame(height: 132)
            Text("\(c.temp)°")
                .font(police(54, .bold))
                .foregroundStyle(ink)
            Text(c.label)
                .font(police(15, .medium))
                .foregroundStyle(ink.opacity(0.7))
                .lineLimit(1)
            Text("↓ \(c.min)°   ↑ \(c.max)°   ·   Ressenti \(c.ressenti)°")
                .font(police(13, .medium))
                .foregroundStyle(ink.opacity(0.75))
            pluieVue(c.pluie, ink: ink)
            Spacer(minLength: 4)
            Text("« \(c.vanne) »")
                .font(police(15.5).italic())
                .foregroundStyle(ink.opacity(0.9))
                .multilineTextAlignment(.center)
                .lineLimit(3)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(20)
    }

    // MARK: Écran verrouillé / StandBy (accessoires monochromes)

    // iOS 17+ EXIGE .containerBackground pour tout widget, accessoires compris,
    // sinon l'écran verrouillé affiche « Please adopt containerBackground API ».
    @ViewBuilder
    private func inline(_ c: NuageEntry.Contenu?) -> some View {
        if let c {
            Label("\(c.temp)° · \(c.label)", systemImage: c.symbole)
        } else {
            Label("Le Nuage", systemImage: "cloud.fill")
        }
    }

    @ViewBuilder
    private func circulaire(_ c: NuageEntry.Contenu?) -> some View {
        VStack(spacing: 0) {
            Image(systemName: c?.symbole ?? "cloud.fill")
                .font(.system(size: 14))
            Text("\(c?.temp ?? 0)°")
                .font(.system(size: 16, weight: .bold))
                .minimumScaleFactor(0.7)
        }
        .containerBackground(for: .widget) { AccessoryWidgetBackground() }
    }

    @ViewBuilder
    private func rectangulaire(_ c: NuageEntry.Contenu?) -> some View {
        Group {
            if let c {
                HStack(spacing: 10) {
                    Image(systemName: c.symbole)
                        .font(.largeTitle)
                    VStack(alignment: .leading, spacing: 1) {
                        HStack(alignment: .firstTextBaseline, spacing: 6) {
                            Text("\(c.temp)°")
                                .font(.title3.weight(.semibold))
                            Text("↓\(c.min)° ↑\(c.max)°")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                        Text(c.label)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                            .minimumScaleFactor(0.8)
                    }
                    Spacer(minLength: 0)
                }
            } else {
                Label("Le wifi des nuages est en rade.", systemImage: "cloud.fill")
            }
        }
        .containerBackground(for: .widget) { Color.clear }
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
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge,
                            .accessoryCircular, .accessoryRectangular, .accessoryInline])
        .contentMarginsDisabled()
    }
}

@main
struct LeNuageWidgetBundle: WidgetBundle {
    var body: some Widget {
        LeNuageWidget()
        NuageLiveActivity()
    }
}
