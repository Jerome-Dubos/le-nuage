import SwiftUI
import WidgetKit

// Vue racine : un système de pages (swipe horizontal). Première page = position GPS,
// pages suivantes = lieux enregistrés. Chaque page charge sa propre météo et rend le
// HTML partagé. Deux boutons flottants : ajouter un lieu (gauche), réglages (droite).
struct AppView: View {
    @StateObject private var loc = LocationProvider()
    @State private var ton = Reglages.ton
    @State private var live = Reglages.liveActivite
    @State private var lieux = Reglages.lieux
    @State private var page = 0
    @State private var reglagesOuverts = false
    @State private var lieuxOuverts = false

    var body: some View {
        ZStack(alignment: .top) {
            TabView(selection: $page) {
                PageMeteoVue(coords: loc.coords,
                             nom: loc.nomLieu ?? "Ma position",
                             estPosition: true, ton: ton, cap: loc.cap, live: live)
                    .tag(0)

                ForEach(Array(lieux.enumerated()), id: \.element.id) { i, l in
                    PageMeteoVue(coords: Coordonnees(latitude: l.lat, longitude: l.lon),
                                 nom: l.nom, estPosition: false, ton: ton, cap: loc.cap)
                        .tag(i + 1)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: lieux.isEmpty ? .never : .always))
            .ignoresSafeArea()

            barreFlottante
        }
        .onAppear { loc.demande() }
        .sheet(isPresented: $reglagesOuverts) {
            ReglagesView(ton: $ton, live: $live).presentationDetents([.large])
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
