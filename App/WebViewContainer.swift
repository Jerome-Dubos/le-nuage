import SwiftUI
import WebKit

// Pont SwiftUI ↔ WKWebView. Charge le cœur web partagé (web/index.html) une fois, puis
// pousse le view-model via window.rendre(<json>) à chaque mise à jour (sans recharger).
// Deux interactions natives : tap sur le nuage → haptique ; secousse → réaction du nuage.
struct WebViewContainer: UIViewRepresentable {
    let vmJSON: String
    var cap: Double = 0

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.userContentController.add(context.coordinator, name: "haptique")
        let wv = WKWebView(frame: .zero, configuration: config)
        wv.isOpaque = false
        wv.backgroundColor = .clear
        wv.scrollView.backgroundColor = .clear
        wv.navigationDelegate = context.coordinator
        context.coordinator.webView = wv
        if let url = Bundle.main.url(forResource: "index", withExtension: "html", subdirectory: "web") {
            wv.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
        }
        return wv
    }

    func updateUIView(_ wv: WKWebView, context: Context) {
        context.coordinator.applique(vmJSON)
        // pousse le cap (boussole) sans recharger la page
        let cap = Int(self.cap.rounded())
        if context.coordinator.dernierCap != cap {
            context.coordinator.dernierCap = cap
            wv.evaluateJavaScript("window.cap && window.cap(\(cap))")
        }
    }

    final class Coordinator: NSObject, WKScriptMessageHandler, WKNavigationDelegate {
        var dernierCap: Int?
        weak var webView: WKWebView?
        private var chargee = false
        private var vmEnAttente: String?
        private var dernierVM: String?
        private let leger = UIImpactFeedbackGenerator(style: .light)

        override init() {
            super.init()
            NotificationCenter.default.addObserver(
                self, selector: #selector(secousse), name: .nuageSecoue, object: nil)
        }

        // Applique un nouveau view-model : direct si la page est chargée, en attente sinon.
        func applique(_ json: String) {
            guard json != dernierVM else { return }
            dernierVM = json
            if chargee { rendre(json) } else { vmEnAttente = json }
        }

        private func rendre(_ json: String) {
            webView?.evaluateJavaScript("window.rendre(\(json))")
        }

        func webView(_ wv: WKWebView, didFinish navigation: WKNavigation!) {
            chargee = true
            if let v = vmEnAttente { vmEnAttente = nil; rendre(v) }
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
