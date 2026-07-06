import SwiftUI
import MessageUI

// Enveloppe SwiftUI du compositeur d'e-mail natif. Ouvre Mail pré-rempli (destinataire,
// sujet, corps) : l'envoi reste entre les mains de l'utilisateur, aucun serveur tiers.
struct MailComposer: UIViewControllerRepresentable {
    let destinataire: String
    let sujet: String
    let corps: String
    var termine: () -> Void

    func makeUIViewController(context: Context) -> MFMailComposeViewController {
        let vc = MFMailComposeViewController()
        vc.mailComposeDelegate = context.coordinator
        vc.setToRecipients([destinataire])
        vc.setSubject(sujet)
        vc.setMessageBody(corps, isHTML: false)
        return vc
    }

    func updateUIViewController(_ vc: MFMailComposeViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(termine: termine) }

    final class Coordinator: NSObject, MFMailComposeViewControllerDelegate {
        let termine: () -> Void
        init(termine: @escaping () -> Void) { self.termine = termine }
        func mailComposeController(_ controller: MFMailComposeViewController,
                                   didFinishWith result: MFMailComposeResult,
                                   error: Error?) {
            termine()
        }
    }
}
