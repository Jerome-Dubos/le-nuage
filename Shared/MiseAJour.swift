import Foundation

// Recherche de mise à jour maison : interroge l'API GitHub Releases, compare la version
// publiée à la version installée, et prépare un lien d'installation SideStore en un tap.
// Aucun serveur, aucun traçage — juste un GET public sur GitHub.
enum MiseAJour {
    static let depot = "Jerome-Dubos/le-nuage"

    struct Info {
        let version: String   // ex. "1.3"
        let installUrl: URL   // sidestore://install?url=… (installation directe)
        let ipaUrl: URL       // lien direct du .ipa (secours navigateur)
    }

    static var versionActuelle: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0"
    }

    static func verifie() async -> Info? {
        guard let url = URL(string: "https://api.github.com/repos/\(depot)/releases/latest")
        else { return nil }
        var req = URLRequest(url: url)
        req.setValue("application/vnd.github+json", forHTTPHeaderField: "Accept")

        guard let (data, _) = try? await URLSession.shared.data(for: req),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let tag = json["tag_name"] as? String,
              let assets = json["assets"] as? [[String: Any]],
              let ipa = assets.first(where: { ($0["name"] as? String)?.hasSuffix(".ipa") == true }),
              let ipaStr = ipa["browser_download_url"] as? String,
              let ipaUrl = URL(string: ipaStr)
        else { return nil }

        let distante = tag.hasPrefix("v") ? String(tag.dropFirst()) : tag
        guard estPlusRecente(distante, que: versionActuelle) else { return nil }

        let install = URL(string: "sidestore://install?url=\(ipaStr)") ?? ipaUrl
        return Info(version: distante, installUrl: install, ipaUrl: ipaUrl)
    }

    // Compare deux numéros de version "x.y.z" composant par composant.
    static func estPlusRecente(_ a: String, que b: String) -> Bool {
        let pa = a.split(separator: ".").map { Int($0) ?? 0 }
        let pb = b.split(separator: ".").map { Int($0) ?? 0 }
        for i in 0..<max(pa.count, pb.count) {
            let x = i < pa.count ? pa[i] : 0
            let y = i < pb.count ? pb[i] : 0
            if x != y { return x > y }
        }
        return false
    }
}
