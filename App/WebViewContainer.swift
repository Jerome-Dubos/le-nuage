import SwiftUI
import WebKit

// Pont SwiftUI ↔ WKWebView. Charge le HTML généré, fond transparent (rebond iOS),
// + deux interactions natives (vague 3) :
//   - tap sur le nuage → retour haptique (JS poste un message « haptique »)
//   - secousse de l'appareil → le nuage réagit (nouvelle vanne + frétille + haptique)
struct WebViewContainer: UIViewRepresentable {
    let html: String
    var cap: Double = 0

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.userContentController.add(context.coordinator, name: "haptique")
        let wv = WKWebView(frame: .zero, configuration: config)
        wv.isOpaque = false
        wv.backgroundColor = .clear
        wv.scrollView.backgroundColor = .clear
        context.coordinator.webView = wv
        return wv
    }

    func updateUIView(_ wv: WKWebView, context: Context) {
        if context.coordinator.dernierHTML != html {
            context.coordinator.dernierHTML = html
            wv.loadHTMLString(html, baseURL: nil)
        }
        // pousse le cap (boussole) sans recharger la page
        let cap = Int(self.cap.rounded())
        if context.coordinator.dernierCap != cap {
            context.coordinator.dernierCap = cap
            wv.evaluateJavaScript("window.cap && window.cap(\(cap))")
        }
    }

    final class Coordinator: NSObject, WKScriptMessageHandler {
        var dernierHTML: String?
        var dernierCap: Int?
        weak var webView: WKWebView?
        private let leger = UIImpactFeedbackGenerator(style: .light)

        override init() {
            super.init()
            NotificationCenter.default.addObserver(
                self, selector: #selector(secousse), name: .nuageSecoue, object: nil)
        }

        func userContentController(_ uc: WKUserContentController, didReceive message: WKScriptMessage) {
            if message.name == "haptique" { leger.impactOccurred() }
        }

        @objc func secousse() {
            UINotificationFeedbackGenerator().notificationOccurred(.success)
            webView?.evaluateJavaScript("window.secousse && window.secousse()")
        }
    }
}

extension Notification.Name {
    static let nuageSecoue = Notification.Name("nuageSecoue")
}

// Détection globale de la secousse (motionShake) — relayée à la WebView via notification.
extension UIWindow {
    open override func motionEnded(_ motion: UIEvent.EventSubtype, with event: UIEvent?) {
        if motion == .motionShake {
            NotificationCenter.default.post(name: .nuageSecoue, object: nil)
        }
    }
}
