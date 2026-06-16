import SwiftUI
import WidgetKit

// Gestion des lieux : recherche d'une ville (geocoding Open-Meteo) pour l'ajouter,
// et liste des lieux enregistrés (suppression / réordonnancement). La position GPS
// reste implicite (première page), elle n'apparaît pas ici.
struct LieuxView: View {
    @Binding var lieux: [Lieu]
    @Environment(\.dismiss) private var dismiss

    @State private var requete = ""
    @State private var resultats: [LieuTrouve] = []
    @State private var recherche = false
    @State private var tacheRecherche: Task<Void, Never>?

    var body: some View {
        NavigationStack {
            List {
                if !resultats.isEmpty {
                    Section("Résultats") {
                        ForEach(resultats) { r in
                            Button { ajoute(r) } label: {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(r.name).foregroundStyle(.primary)
                                    if !r.sousTitre.isEmpty {
                                        Text(r.sousTitre).font(.caption).foregroundStyle(.secondary)
                                    }
                                }
                            }
                        }
                    }
                }

                Section {
                    if lieux.isEmpty {
                        Text("Aucun lieu pour l'instant. Cherche une ville ci-dessus pour l'ajouter.")
                            .font(.callout).foregroundStyle(.secondary)
                    } else {
                        ForEach(lieux) { l in
                            HStack {
                                Image(systemName: "mappin.circle.fill").foregroundStyle(.secondary)
                                Text(l.nom)
                            }
                        }
                        .onDelete { lieux.remove(atOffsets: $0); enregistre() }
                        .onMove { lieux.move(fromOffsets: $0, toOffset: $1); enregistre() }
                    }
                } header: {
                    Text("Mes lieux")
                } footer: {
                    Text("Ta position actuelle reste toujours la première page.")
                }
            }
            .searchable(text: $requete, prompt: "Rechercher une ville…")
            .onChange(of: requete) { _, _ in lanceRecherche() }
            .overlay(alignment: .top) {
                if recherche { ProgressView().padding(.top, 8) }
            }
            .navigationTitle("Lieux")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    if !lieux.isEmpty { EditButton() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("OK") { dismiss() }.fontWeight(.semibold)
                }
            }
        }
    }

    private func lanceRecherche() {
        tacheRecherche?.cancel()
        let q = requete
        guard q.trimmingCharacters(in: .whitespaces).count >= 2 else { resultats = []; return }
        tacheRecherche = Task {
            try? await Task.sleep(nanoseconds: 300_000_000)  // anti-rebond
            if Task.isCancelled { return }
            recherche = true
            let r = await WeatherService.recherche(q)
            if Task.isCancelled { return }
            resultats = r
            recherche = false
        }
    }

    private func ajoute(_ r: LieuTrouve) {
        let lieu = Lieu(nom: r.name, lat: r.latitude, lon: r.longitude)
        if !lieux.contains(where: { abs($0.lat - lieu.lat) < 0.01 && abs($0.lon - lieu.lon) < 0.01 }) {
            lieux.append(lieu)
            enregistre()
        }
        requete = ""
        resultats = []
    }

    private func enregistre() {
        Reglages.lieux = lieux
        WidgetCenter.shared.reloadAllTimelines()
    }
}
