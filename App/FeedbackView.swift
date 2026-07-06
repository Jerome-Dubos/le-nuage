import SwiftUI
import MessageUI
import UIKit

// Formulaire de retour : l'utilisateur choisit un type, écrit son message, et l'app
// compose un e-mail joliment pré-rempli vers l'auteur. Repli mailto si aucune boîte
// mail n'est configurée. Aucun serveur, aucun traçage.
struct FeedbackView: View {
    let ton: Ton
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @State private var type: TypeRetour = .idee
    @State private var message = ""
    @State private var mailOuvert = false

    enum TypeRetour: String, CaseIterable, Identifiable {
        case idee = "Une idée"
        case bug = "Un souci"
        case autre = "Autre"
        var id: String { rawValue }
    }

    private let destinataire = "contact@duboswebservices.fr"

    private var messagePret: Bool {
        !message.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private var sujet: String { "[Le Nuage] \(type.rawValue)" }

    private var corps: String {
        message + "\n\n———\nEnvoyé depuis Le Nuage v\(MiseAJour.versionActuelle)"
            + " · iOS \(UIDevice.current.systemVersion)"
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    VStack(spacing: 8) {
                        Image(ton == .doux ? "doux/detendu" : "taquin/detendu")
                            .resizable().scaledToFit().frame(height: 64)
                        Text("Dis-moi tout")
                            .font(.headline)
                        Text("Une idée pour améliorer Le Nuage, un bug croisé ? Écris-le ici, ça arrive directement dans ma boîte mail.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
                    .listRowBackground(Color.clear)
                }

                Section("Ton retour") {
                    Picker("Type", selection: $type) {
                        ForEach(TypeRetour.allCases) { Text($0.rawValue).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    TextField("Écris ton message…", text: $message, axis: .vertical)
                        .lineLimit(4...10)
                }

                Section {
                    Button {
                        envoyer()
                    } label: {
                        Label("Envoyer mon retour", systemImage: "paperplane.fill")
                            .frame(maxWidth: .infinity)
                            .fontWeight(.semibold)
                    }
                    .disabled(!messagePret)
                } footer: {
                    Text("Le mail s'ouvre pré-rempli : tu gardes la main sur l'envoi. Rien n'est transmis sans ton accord.")
                }
            }
            .navigationTitle("Donner mon avis")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Fermer") { dismiss() }
                }
            }
            .sheet(isPresented: $mailOuvert) {
                MailComposer(destinataire: destinataire, sujet: sujet, corps: corps) {
                    mailOuvert = false
                    dismiss()
                }
                .ignoresSafeArea()
            }
        }
    }

    private func envoyer() {
        if MFMailComposeViewController.canSendMail() {
            mailOuvert = true
        } else {
            // Repli : ouvre l'app mail par défaut via un lien mailto pré-rempli.
            let enc = { (s: String) in
                s.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? s
            }
            if let url = URL(string: "mailto:\(destinataire)?subject=\(enc(sujet))&body=\(enc(corps))") {
                openURL(url)
                dismiss()
            }
        }
    }
}
