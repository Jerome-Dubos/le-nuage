import SwiftUI

extension Color {
    init(hex: String) {
        let s = hex.hasPrefix("#") ? String(hex.dropFirst()) : hex
        var v: UInt64 = 0
        Scanner(string: s).scanHexInt64(&v)
        self.init(
            red: Double((v >> 16) & 0xFF) / 255,
            green: Double((v >> 8) & 0xFF) / 255,
            blue: Double(v & 0xFF) / 255
        )
    }
}

// Palette + identité visuelle par ton. Le doux n'est pas qu'un nuage rose : c'est une
// DA « carnet de croquis / coloriage au crayon » — papier crème, police manuscrite,
// contours dessinés main, version sombre façon ardoise/craie. Le taquin reste net,
// moderne, bleu froid. Source unique partagée app ↔ widget.
struct PaletteTon {
    let corps: String          // teinte claire (texte en mode sombre)
    let ombre: String          // séparateurs en mode clair
    let encre: String          // texte en mode clair
    let fondHautClair: String
    let fondBasClair: String
    let fondHautSombre: String
    let fondBasSombre: String
    let traitClair: String     // couleur des contours dessinés (mode clair)
    let traitSombre: String    // idem mode sombre
    let croquis: Bool          // applique l'identité croquis (police + contours)

    static let taquin = PaletteTon(
        corps: "#EAF1F9", ombre: "#D8E5F2", encre: "#33414F",
        fondHautClair: "#DCEBFA", fondBasClair: "#F7FBFF",
        fondHautSombre: "#2B3A4E", fondBasSombre: "#1E2935",
        traitClair: "rgba(51,65,79,0)", traitSombre: "rgba(234,241,249,0)",
        croquis: false
    )

    static let doux = PaletteTon(
        corps: "#F4E9D8", ombre: "#E7D3C4", encre: "#5A4640",
        fondHautClair: "#FBF3E6", fondBasClair: "#FEFAF2",   // papier crème
        fondHautSombre: "#34302A", fondBasSombre: "#28241F",  // ardoise chaude
        traitClair: "rgba(90,70,64,.42)", traitSombre: "rgba(244,233,216,.32)",  // crayon / craie
        croquis: true
    )

    static func pour(_ ton: Ton) -> PaletteTon { ton == .doux ? .doux : .taquin }

    // Police : manuscrite pour le doux, arrondie système pour le taquin.
    var policeCSS: String {
        croquis ? "\"Noteworthy\", \"Marker Felt\", \"Bradley Hand\", -apple-system, cursive"
                : "-apple-system, \"SF Pro Rounded\", sans-serif"
    }
    var policeWidget: String? { croquis ? "Noteworthy" : nil }

    // "#EAF1F9" → "234,241,249"
    private static func rgb(_ hex: String) -> String {
        let s = hex.hasPrefix("#") ? String(hex.dropFirst()) : hex
        var v: UInt64 = 0
        Scanner(string: s).scanHexInt64(&v)
        return "\((v >> 16) & 0xFF),\((v >> 8) & 0xFF),\(v & 0xFF)"
    }

    // Variables CSS de la vue app (HTMLBuilder)
    var varsClaires: String {
        "--fond-haut:\(fondHautClair); --fond-bas:\(fondBasClair); --texte:\(encre); --carte:rgba(255,255,255,.55); --bulle:rgba(255,255,255,.5); --separateur:\(ombre); --trait:\(traitClair); --lisere:rgba(255,255,255,.65);"
    }
    var varsSombres: String {
        let c = Self.rgb(corps)
        return "--fond-haut:\(fondHautSombre); --fond-bas:\(fondBasSombre); --texte:\(corps); --carte:rgba(\(c),.06); --bulle:rgba(\(c),.08); --separateur:rgba(\(c),.12); --trait:\(traitSombre); --lisere:rgba(\(c),.10);"
    }
}
