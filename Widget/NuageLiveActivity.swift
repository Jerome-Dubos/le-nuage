import ActivityKit
import WidgetKit
import SwiftUI

// Live Activity du Nuage : île compacte (nuage + °), île étendue (nuage, état, °, vanne)
// et bannière écran verrouillé. Palette selon le ton actif. Réutilise les PNG du nuage.
struct NuageLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: NuageActivityAttributes.self) { context in
            BannereVerrou(s: context.state)
                .activityBackgroundTint(fond(context.state))
                .activitySystemActionForegroundColor(.primary)
        } dynamicIsland: { context in
            let s = context.state
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Image(s.asset).resizable().scaledToFit()
                        .frame(width: 46, height: 36).padding(.leading, 4)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    VStack(alignment: .trailing, spacing: 0) {
                        Text("\(s.temp)°").font(.system(size: 30, weight: .bold, design: .rounded))
                        Text("↓\(s.min)° ↑\(s.max)°").font(.system(size: 11)).foregroundStyle(.secondary)
                    }.padding(.trailing, 4)
                }
                DynamicIslandExpandedRegion(.center) {
                    Text(s.label).font(.system(size: 13, weight: .medium)).foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text("« \(s.vanne) »")
                        .font(.system(size: 13, design: .rounded)).italic()
                        .multilineTextAlignment(.center).lineLimit(2)
                        .frame(maxWidth: .infinity)
                }
            } compactLeading: {
                Image(s.asset).resizable().scaledToFit().frame(width: 26, height: 21)
            } compactTrailing: {
                Text("\(s.temp)°").font(.system(size: 15, weight: .semibold, design: .rounded))
            } minimal: {
                Text("\(s.temp)°").font(.system(size: 13, weight: .semibold, design: .rounded))
            }
            .keylineTint(Color(hex: PaletteTon.pour(Ton(rawValue: s.tonRaw) ?? .taquin).fondHautClair))
        }
    }

    private func fond(_ s: NuageActivityAttributes.ContentState) -> Color {
        let pal = PaletteTon.pour(Ton(rawValue: s.tonRaw) ?? .taquin)
        return Color(hex: s.nuit ? pal.fondBasSombre : pal.fondHautClair)
    }
}

// Bannière de l'écran verrouillé (fond teinté par activityBackgroundTint).
private struct BannereVerrou: View {
    let s: NuageActivityAttributes.ContentState

    var body: some View {
        let ton = Ton(rawValue: s.tonRaw) ?? .taquin
        let pal = PaletteTon.pour(ton)
        let ink = s.nuit ? Color(hex: pal.corps) : Color(hex: pal.encre)
        HStack(spacing: 12) {
            Image(s.asset).resizable().scaledToFit().frame(width: 58, height: 46)
            VStack(alignment: .leading, spacing: 1) {
                Text(s.label).font(.system(size: 12, weight: .medium, design: police(ton)))
                    .foregroundStyle(ink.opacity(0.65)).lineLimit(1)
                Text("\(s.temp)°").font(.system(size: 26, weight: .bold, design: police(ton)))
                    .foregroundStyle(ink)
                Text("« \(s.vanne) »").font(.system(size: 12, design: police(ton))).italic()
                    .foregroundStyle(ink.opacity(0.85)).lineLimit(2)
            }
            Spacer(minLength: 4)
            Text("↓\(s.min)°\n↑\(s.max)°").font(.system(size: 12, weight: .medium))
                .foregroundStyle(ink.opacity(0.7)).multilineTextAlignment(.trailing)
        }
        .padding(EdgeInsets(top: 12, leading: 16, bottom: 12, trailing: 16))
    }

    private func police(_ ton: Ton) -> Font.Design { ton == .doux ? .default : .rounded }
}
