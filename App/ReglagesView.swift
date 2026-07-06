import SwiftUI

// Réglages : choix du caractère du nuage. Sélecteur à deux cartes montrant les deux
// personnages (taquin / doux) plutôt qu'un segmented control basique — plus premium
// et plus parlant. Le ton est persisté dans l'App Group (app + widget suivent).
struct ReglagesView: View {
    @Binding var ton: Ton
    @Binding var live: Bool
    @Environment(\.dismiss) private var dismiss
    @State private var anniversaires = Reglages.anniversaires

    var body: some View {
        NavigationStack {
            Form {
                Section("Caractère du nuage") {
                    HStack(spacing: 14) {
                        carte(.taquin, image: "taquin/radieux", titre: "Taquin",
                              desc: "Tendre, mais un peu piquant", accent: Color(hex: "#5B86C4"))
                        carte(.doux, image: "doux/radieux", titre: "Doux",
                              desc: "Tout doux et bienveillant", accent: Color(hex: "#E0A0B4"))
                    }
                    .listRowInsets(EdgeInsets(top: 14, leading: 14, bottom: 14, trailing: 14))
                    .listRowBackground(Color.clear)
                }

                Section {
                    Toggle("Afficher dans la Dynamic Island", isOn: $live)
                } header: {
                    Text("Dynamic Island")
                } footer: {
                    Text("Un aperçu météo vivant dans l'îlot dynamique et sur l'écran verrouillé, pour ta position actuelle.")
                }

                Section {
                    ForEach($anniversaires) { $a in
                        HStack(spacing: 8) {
                            TextField("Nom (ex. Maman, Léa…)", text: $a.nom)
                                .font(.body)
                            Spacer(minLength: 4)
                            Picker("Jour", selection: jourBinding($a)) {
                                ForEach(1...Self.joursDansMois(moisBinding($a).wrappedValue), id: \.self) {
                                    Text("\($0)").tag($0)
                                }
                            }
                            .labelsHidden()
                            Picker("Mois", selection: moisBinding($a)) {
                                ForEach(1...12, id: \.self) { Text(Self.moisNoms[$0 - 1]).tag($0) }
                            }
                            .labelsHidden()
                        }
                        .pickerStyle(.menu)
                        .padding(.vertical, 2)
                    }
                    .onDelete { anniversaires.remove(atOffsets: $0) }
                    Button {
                        anniversaires.append(Anniversaire(nom: "", date: Reglages.mmdd(Date())))
                    } label: {
                        Label("Ajouter une date", systemImage: "plus.circle.fill")
                    }
                } header: {
                    Text("Dates spéciales")
                } footer: {
                    Text("Chaque date revient automatiquement tous les ans : le nuage la fête avec une réplique dédiée et des confettis.")
                }
            }
            .navigationTitle("Réglages")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    if !anniversaires.isEmpty { EditButton() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("OK") { dismiss() }.fontWeight(.semibold)
                }
            }
            .onChange(of: anniversaires) { _, v in Reglages.anniversaires = v }
            .onChange(of: live) { _, v in Reglages.liveActivite = v }
        }
    }

    // Dates stockées "MM-dd" sans année → récurrence annuelle. Pickers jour + mois.
    static let moisNoms = ["janvier", "février", "mars", "avril", "mai", "juin",
                           "juillet", "août", "septembre", "octobre", "novembre", "décembre"]
    static func joursDansMois(_ mois: Int) -> Int {
        [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][max(1, min(12, mois)) - 1]
    }

    private func moisBinding(_ a: Binding<Anniversaire>) -> Binding<Int> {
        Binding(
            get: { Int(a.wrappedValue.date.prefix(2)) ?? 1 },
            set: { m in
                let j = min(Int(a.wrappedValue.date.suffix(2)) ?? 1, Self.joursDansMois(m))
                a.wrappedValue.date = String(format: "%02d-%02d", m, j)
            })
    }

    private func jourBinding(_ a: Binding<Anniversaire>) -> Binding<Int> {
        Binding(
            get: { Int(a.wrappedValue.date.suffix(2)) ?? 1 },
            set: { j in
                let m = Int(a.wrappedValue.date.prefix(2)) ?? 1
                a.wrappedValue.date = String(format: "%02d-%02d", m, j)
            })
    }

    private func carte(_ valeur: Ton, image: String, titre: String, desc: String, accent: Color) -> some View {
        let actif = ton == valeur
        return Button {
            withAnimation(.snappy(duration: 0.25)) { ton = valeur }
        } label: {
            VStack(spacing: 10) {
                Image(image)
                    .resizable()
                    .scaledToFit()
                    .frame(height: 76)
                Text(titre)
                    .font(.headline)
                Text(desc)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .lineLimit(2, reservesSpace: true)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .padding(.vertical, 18)
            .padding(.horizontal, 10)
            .background(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .fill(actif ? accent.opacity(0.14) : Color(.secondarySystemBackground))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .strokeBorder(actif ? accent : Color.clear, lineWidth: 2)
            )
            .overlay(alignment: .topTrailing) {
                if actif {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(accent)
                        .font(.title3)
                        .padding(10)
                }
            }
        }
        .buttonStyle(.plain)
    }
}
