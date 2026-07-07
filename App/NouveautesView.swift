import SwiftUI

// Feuille « Quoi de neuf » : s'ouvre au 1er lancement après une mise à jour (via
// Nouveautes.aMontrer) et se rouvre à la demande depuis les Réglages.
struct NouveautesView: View {
    let entrees: [Nouveaute]
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 30) {
                    VStack(spacing: 10) {
                        Text("☁️").font(.system(size: 60))
                        Text("Quoi de neuf ?").font(.largeTitle.bold())
                        Text("Le nuage a un peu changé.")
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 44)
                    .padding(.bottom, 6)

                    ForEach(entrees) { entree in
                        VStack(alignment: .leading, spacing: 14) {
                            HStack(spacing: 8) {
                                Text(entree.titre).font(.headline)
                                Text("v\(entree.version)")
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(.secondary)
                            }
                            ForEach(entree.points) { point in
                                HStack(alignment: .top, spacing: 12) {
                                    Image(systemName: point.symbole)
                                        .font(.system(size: 18))
                                        .frame(width: 26)
                                        .foregroundStyle(.tint)
                                    Text(point.texte)
                                        .fixedSize(horizontal: false, vertical: true)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .padding(24)
            }

            Button {
                dismiss()
            } label: {
                Text("Continuer")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 15)
                    .background(Color.primary.opacity(0.1), in: RoundedRectangle(cornerRadius: 16))
            }
            .tint(.primary)
            .padding(.horizontal, 24)
            .padding(.bottom, 12)
        }
    }
}
