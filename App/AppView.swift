import SwiftUI
import WidgetKit

// Vue racine : un système de pages (swipe horizontal). Première page = position GPS,
// pages suivantes = lieux enregistrés. Chaque page charge sa propre météo et rend le
// HTML partagé. Deux boutons flottants : ajouter un lieu (gauche), réglages (droite).
struct AppView: View {
    @StateObject private var loc = LocationProvider()
    @Environment(\.openURL) private var openURL
    @State private var ton = Reglages.ton
    @State private var live = Reglages.liveActivite
    @State private var cal = Reglages.calendrier
    @State private var lieux = Reglages.lieux
    @State private var page = 0
    @State private var reglagesOuverts = false
    @State private var lieuxOuverts = false
    @State private var maj: MiseAJour.Info?
    @State private var quoiDeNeuf: JournalMaj?

    var body: some View {
        ZStack(alignment: .top) {
            TabView(selection: $page) {
                PageMeteoVue(coords: loc.coords,
                             nom: loc.nomLieu ?? "Ma position",
                             estPosition: true, ton: ton, cap: loc.cap, live: live, cal: cal)
                    .tag(0)

                ForEach(Array(lieux.enumerated()), id: \.element.id) { i, l in
                    PageMeteoVue(coords: Coordonnees(latitude: l.lat, longitude: l.lon),
                                 nom: l.nom, estPosition: false, ton: ton, cap: loc.cap)
                        .tag(i + 1)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: lieux.isEmpty ? .never : .always))
            .ignoresSafeArea()

            VStack(spacing: 8) {
                barreFlottante
                if let maj { banniereMaj(maj) }
            }
        }
        .onAppear {
            loc.demande(); Reveil.applique(); AlertePluie.programme()
            let n = Nouveautes.aMontrer()
            if !n.isEmpty { quoiDeNeuf = JournalMaj(entrees: n) }
        }
        .sheet(item: $quoiDeNeuf) { j in
            NouveautesView(entrees: j.entrees).presentationDetents([.medium, .large])
        }
        .task { maj = await MiseAJour.verifie() }
        .sheet(isPresented: $reglagesOuverts) {
            ReglagesView(ton: $ton, live: $live, cal: $cal).presentationDetents([.large])
        }
        .sheet(isPresented: $lieuxOuverts) {
            LieuxView(lieux: $lieux)
        }
        .onChange(of: ton) { _, nouveau in
            Reglages.ton = nouveau
            WidgetCenter.shared.reloadAllTimelines()
        }
        .onChange(of: lieux) { _, nouveaux in
            // si la page courante n'existe plus (lieu supprimé), revenir au GPS
            if page > nouveaux.count { page = max(0, nouveaux.count) }
        }
    }

    private var barreFlottante: some View {
        HStack {
            bouton("plus") { lieuxOuverts = true }
            Spacer()
            bouton("slider.horizontal.3") { reglagesOuverts = true }
        }
        .padding(.horizontal, 18)
        .padding(.top, 6)
    }

    // Bandeau discret : n'apparaît que si une version plus récente existe. Un tap lance
    // l'installation via SideStore. Se referme de lui-même une fois l'app à jour.
    private func banniereMaj(_ info: MiseAJour.Info) -> some View {
        Button {
            openURL(info.installUrl)
        } label: {
            HStack(spacing: 9) {
                Image(systemName: "arrow.down.circle.fill")
                    .font(.system(size: 17, weight: .semibold))
                Text("Mise à jour dispo — v\(info.version)")
                    .font(.subheadline.weight(.semibold))
                Spacer(minLength: 0)
                Text("Installer")
                    .font(.footnote.weight(.bold))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.primary.opacity(0.12), in: Capsule())
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(.ultraThinMaterial, in: Capsule())
        }
        .tint(.primary)
        .padding(.horizontal, 18)
        .transition(.move(edge: .top).combined(with: .opacity))
    }

    private func bouton(_ icone: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: icone)
                .font(.system(size: 16, weight: .semibold))
                .padding(10)
                .background(.ultraThinMaterial, in: Circle())
        }
        .tint(.primary)
    }
}

#Preview {
    AppView()
}
