import UIKit
import Messages

// Extension iMessage pilotée par code (l'approche .stickerpack ne compilait pas via actool).
//
// Point clé : avec NSExtensionPointIdentifier = com.apple.message-payload-provider, iOS
// veut une classe principale MSMessagesAppViewController. Un MSStickerBrowserViewController
// posé directement en classe principale ne s'instancie pas → l'extension disparaît du
// tiroir. On l'héberge donc comme contrôleur ENFANT dans un MSMessagesAppViewController.
final class MessagesViewController: MSMessagesAppViewController {
    override func viewDidLoad() {
        super.viewDidLoad()

        let browser = StickerBrowser()
        addChild(browser)
        browser.view.frame = view.bounds
        browser.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(browser.view)
        browser.didMove(toParent: self)
    }
}

// Charge les PNG des expressions du nuage depuis le bundle de l'extension.
final class StickerBrowser: MSStickerBrowserViewController {
    private var stickers: [MSSticker] = []

    private let noms = ["radieux", "detendu", "endormi", "emmitoufle", "blase", "ronchon", "flippe"]

    override func viewDidLoad() {
        super.viewDidLoad()
        for nom in noms {
            guard let url = Bundle.main.url(forResource: nom, withExtension: "png",
                                            subdirectory: "png") ?? Bundle.main.url(forResource: nom, withExtension: "png"),
                  let sticker = try? MSSticker(contentsOfFileURL: url, localizedDescription: nom)
            else { continue }
            stickers.append(sticker)
        }
        stickerBrowserView.backgroundColor = .clear
    }

    override func numberOfStickers(in stickerBrowserView: MSStickerBrowserView) -> Int {
        stickers.count
    }

    override func stickerBrowserView(_ stickerBrowserView: MSStickerBrowserView,
                                     stickerAt index: Int) -> MSSticker {
        stickers[index]
    }
}
