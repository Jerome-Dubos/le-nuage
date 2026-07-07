import UIKit
import Messages

// Navigateur de stickers piloté par code (l'approche .stickerpack ne compilait pas).
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
