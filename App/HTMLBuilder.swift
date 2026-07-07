import UIKit

// Port de buildHTML (le-nuage.js). Génère le HTML de la vue app — réutilisé tel
// quel, destiné à devenir la vue partagée iOS/Android (brief §7). Le nuage est
// injecté en base64 depuis l'asset catalog ; le pool de vannes est embarqué pour
// que taper le nuage en serve une nouvelle sans aller-retour réseau.
enum HTMLBuilder {
    static func build(_ data: Meteo, ton: Ton, lieu: String = "", estPosition: Bool = false,
                      agenda: [Agenda.Evenement]? = nil) -> String {
        let c = data.current
        let nuit = c.is_day == 0
        let info = WMO.info(c.weather_code, isDay: c.is_day)
        let replique = Repliques.choisit(pour: data, ton: ton)
        let jours = ["dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam."]

        // ambiance animée selon la météo (vague 2)
        let effet: String
        switch WMO.groupeReplique(c.weather_code) {
        case "clear": effet = nuit ? "etoiles" : "soleil"
        case "drizzle", "rain": effet = "pluie"
        case "snow": effet = "neige"
        case "storm": effet = "orage"
        case "fog": effet = "brume"
        default: effet = ""
        }

        // easter eggs : occasions spéciales (vague 4) — vanne thématique + décor festif
        let (occVanne, deco) = occasionDuJour(ton: ton)
        let repliqueAffichee = occVanne ?? replique

        let etat = WMO.humeur(code: c.weather_code, temp: Int(c.temperature_2m.rounded()))
        var nuageSVG = svgInline(etat: etat, nuit: nuit, ton: ton)
        if let tenue = Tenue.pour(code: c.weather_code,
                                  temp: Int(c.temperature_2m.rounded()),
                                  vent: Int(c.wind_speed_10m.rounded()),
                                  jour: c.is_day == 1) {
            nuageSVG = injecteTenue(nuageSVG, tenue: tenue)
        }
        // ne cligne que si les yeux affichés sont ouverts (sinon œil déjà fermé : clin / mi-clos)
        let peutCligner = peutCligner(etat: etat, nuit: nuit)
        let vannes = Repliques.pool(pour: data, ton: ton).map { Repliques.remplaceTemp($0, meteo: data) }
        let vannesJSON = (try? JSONEncoder().encode(vannes)).flatMap { String(data: $0, encoding: .utf8) } ?? "[]"

        let vent = Int(c.wind_speed_10m.rounded())
        let lever = heureCourte(data.daily.sunrise.first)
        let coucher = heureCourte(data.daily.sunset.first)

        // prochaines 12 heures, proba de pluie affichée à partir de 30 %
        let maintenant = Date()
        let depart = data.hourly.time.firstIndex {
            (Horodatage.local($0) ?? .distantPast) > maintenant.addingTimeInterval(-3600)
        }
        var heuresHTML = ""
        if let depart {
            let fin = min(depart + 12, data.hourly.time.count)
            for i in depart..<fin {
                let d = Horodatage.local(data.hourly.time[i]) ?? Date()
                let hr = Calendar.current.component(.hour, from: d)
                let h = i == depart ? "Maint." : "\(hr) h"
                let isDayH = (hr >= 21 || hr <= 6) ? 0 : 1
                let ico = WMO.info(data.hourly.weather_code[i], isDay: isDayH).icone
                let proba = data.hourly.precipitation_probability.indices.contains(i)
                    ? (data.hourly.precipitation_probability[i] ?? 0) : 0
                let probaHTML = proba >= 30
                    ? "<div class=\"h-pluie\">\(proba) %</div>"
                    : "<div class=\"h-pluie\">&nbsp;</div>"
                heuresHTML += "<div class=\"heure\"><div class=\"h-label\">\(h)</div><div class=\"h-ico\">\(svgIcone(ico, 26))</div><div class=\"h-temp\">\(Int(data.hourly.temperature_2m[i].rounded()))°</div>\(probaHTML)</div>"
            }
        }

        // 7 jours, proba de pluie max du jour à partir de 20 %
        var joursHTML = ""
        for i in data.daily.time.indices {
            let d = jourLocal(data.daily.time[i]) ?? Date()
            let nom: String
            if i == 0 {
                nom = "Aujourd'hui"
            } else {
                let wd = Calendar.current.component(.weekday, from: d) - 1  // 0 = dimanche
                let jour = Calendar.current.component(.day, from: d)
                nom = "\(jours[wd]) \(jour)"
            }
            let ico = WMO.info(data.daily.weather_code[i], isDay: 1).icone
            let probaOpt = data.daily.precipitation_probability_max.indices.contains(i)
                ? data.daily.precipitation_probability_max[i] : nil
            let probaHTML: String
            if let p = probaOpt, p >= 20 {
                probaHTML = "<span class=\"j-pluie\">\(svgIcone("goutte", 12)) \(p) %</span>"
            } else {
                probaHTML = "<span class=\"j-pluie\"></span>"
            }
            joursHTML += "<div class=\"jour\"><span class=\"j-nom\">\(nom)</span>\(probaHTML)<span class=\"j-ico\">\(svgIcone(ico, 22))</span><span class=\"j-temps\"><span class=\"j-min\">\(Int(data.daily.temperature_2m_min[i].rounded()))°</span> / <span class=\"j-max\">\(Int(data.daily.temperature_2m_max[i].rounded()))°</span></span></div>"
        }

        // carte « Détails » : valeur + interprétation colorée. Chaque stat si dispo.
        var stats: [(ico: String, val: String, cat: String, qual: String, couleur: String)] = []
        if let h = c.relative_humidity_2m {
            let (q, col) = humiditeInfo(h)
            stats.append((svgIcone("goutte", 22), "\(h) %", "Humidité", q, col))
        }
        if let dir = c.wind_direction_10m {
            stats.append((boussole(dir), cardinal(dir), "Vent", directionNom(dir), cGris))
        }
        if let g = c.wind_gusts_10m {
            let (q, col) = rafalesInfo(g)
            stats.append((svgIcone("vent", 22), "\(Int(g.rounded())) km/h", "Rafales", q, col))
        }
        if let cc = c.cloud_cover {
            let (q, col) = couvertureInfo(cc)
            stats.append((svgIcone("nuage", 22), "\(cc) %", "Couverture", q, col))
        }
        if let uv = data.daily.uv_index_max?.first.flatMap({ $0 }) {
            let (q, col) = uvInfo(uv)
            stats.append((svgIcone("soleil", 22), "\(Int(uv.rounded()))", "Indice UV", q, col))
        }
        if let sec = data.daily.sunshine_duration?.first.flatMap({ $0 }) {
            stats.append((svgIcone("soleil", 22), "\(Int((sec / 3600).rounded())) h", "Soleil", "", ""))
        }
        if let sec = data.daily.daylight_duration?.first.flatMap({ $0 }) {
            stats.append((svgIcone("duree", 22), dureeJour(sec), "Durée du jour", "", ""))
        }
        if let depart, let vis = data.hourly.visibility, vis.indices.contains(depart),
           let v = vis[depart] {
            let km = Int((v / 1000).rounded())
            let (q, col) = visibiliteInfo(km)
            stats.append((svgIcone("oeil", 22), "\(km) km", "Visibilité", q, col))
        }
        var statsHTML = ""
        for s in stats {
            let qual = s.qual.isEmpty ? "" : " · <span style=\"color:\(s.couleur); font-weight:600\">\(s.qual)</span>"
            statsHTML += "<div class=\"stat\"><span class=\"s-ico\">\(s.ico)</span><span class=\"s-val\">\(s.val)</span><span class=\"s-lbl\">\(s.cat)\(qual)</span></div>"
        }
        let legende = """
        <div class="legende" id="legende" hidden>
          <p><b>Indice UV</b> — 0–2 faible · 3–5 modéré · 6–7 élevé · 8–10 très élevé · 11+ extrême.</p>
          <p><b>Couverture</b> — part du ciel masquée par les nuages.</p>
          <p><b>Vent</b> — direction d'où il souffle ; la flèche indique où il va.</p>
          <p><b>Visibilité</b> — au-delà de 20 km : excellente ; sous 1 km : brouillard.</p>
        </div>
        """
        let detailsCard = stats.isEmpty ? "" : """
        <div class="card"><h2>Détails <button class="aide" id="aide" type="button" aria-label="Légende">?</button></h2><div class="stats">\(statsHTML)</div>\(legende)</div>
        """

        // Carte agenda : n'apparaît que si le réglage est actif (agenda non-nil, page GPS).
        let agendaCard = agenda.map { evs -> String in
            let lignes = evs.map {
                "<div class=\"prog-l\"><span class=\"prog-h\">\($0.heure)</span><span class=\"prog-t\">\(echappe($0.titre))</span></div>"
            }.joined()
            let note = Agenda.commentaire(evs, ton: ton)
            return "<div class=\"card\"><h2>Ton programme</h2>\(lignes)<div class=\"prog-note\">« \(note) »</div></div>"
        } ?? ""

        // thème : palette selon le ton ; nuit → sombre forcé ; sinon prefers-color-scheme
        let palette = PaletteTon.pour(ton)
        // température teintée aux extrêmes (chaud/froid), hors mode croquis
        let tempStyle: String
        if palette.croquis { tempStyle = "" }
        else if c.temperature_2m >= 32 { tempStyle = " style=\"color:#E0703A\"" }
        else if c.temperature_2m <= -2 { tempStyle = " style=\"color:#4F8AC9\"" }
        else { tempStyle = "" }
        let varsClaires = palette.varsClaires
        let varsSombres = palette.varsSombres
        let themeForce = nuit ? ":root { \(varsSombres) }" : ""

        // identité « croquis » (doux) : contours dessinés main + séparateurs au crayon.
        // Les border-radius irréguliers donnent l'effet trait à la main.
        let croquisCSS = palette.croquis ? """
          .card, .vanne, .chip, .stat { border: 1.7px solid var(--trait); }
          .card { border-radius: 235px 18px 225px 18px / 18px 220px 18px 235px; box-shadow: none; }
          .card:nth-of-type(even) { border-radius: 18px 225px 18px 235px / 220px 18px 235px 18px; }
          .vanne { border-radius: 225px 14px 225px 14px / 14px 225px 14px 225px; }
          .chip { border-radius: 130px 10px 130px 10px / 10px 130px 10px 130px; }
          .stat { padding: 8px 4px; border-radius: 120px 14px 120px 14px / 14px 120px 14px 120px; }
          .jour { border-bottom-style: dashed; }
          .temp { letter-spacing: 0; font-weight: 700; }
          h2 { letter-spacing: .04em; }
        """ : ""

        // polish « premium » du taquin : ombres soignées, fins lisérés, typo resserrée
        let premiumCSS = palette.croquis ? "" : """
          .temp { font-size: 70px; font-weight: 700; letter-spacing: -2.5px; }
          .label { font-size: 18px; letter-spacing: .2px; }
          .card {
            border: 0.5px solid var(--lisere);
            box-shadow: 0 12px 32px rgba(20,40,70,.10), 0 1px 2px rgba(20,40,70,.05);
            margin-top: 18px;
          }
          .vanne {
            border: 0.5px solid var(--lisere);
            box-shadow: 0 6px 20px rgba(20,40,70,.08);
            padding: 14px 18px;
          }
          .chip {
            border: 0.5px solid var(--lisere);
            box-shadow: 0 2px 8px rgba(20,40,70,.05);
            padding: 7px 13px;
          }
          .infos { gap: 9px; margin-top: 16px; }
          .s-val { font-size: 17px; letter-spacing: -.3px; }
        """

        return """
        <!DOCTYPE html>
        <html lang="fr"><head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
        <style>
          :root { \(varsClaires) }
          @media (prefers-color-scheme: dark) { :root { \(varsSombres) } }
          \(themeForce)
          * { box-sizing: border-box; margin: 0; }
          /* fond : couleur unie sur <html> (peinte par WKWebView au rebond)
             + calque fixe surdimensionné pour le dégradé (background-attachment: fixed cassé sur iOS) */
          html { background-color: var(--fond-bas); min-height: 100%; }
          body {
            font-family: \(palette.policeCSS);
            background: transparent;
            color: var(--texte);
            min-height: 100vh;
            padding: calc(34px + env(safe-area-inset-top)) 18px calc(28px + env(safe-area-inset-bottom));
          }
          .fond {
            position: fixed; left: 0; right: 0; top: -100vh; height: 300vh;
            z-index: -2; pointer-events: none;
            background: linear-gradient(var(--fond-haut) 33%, var(--fond-bas) 67%);
          }
          /* ambiance météo animée (vague 2) — derrière le contenu, sans interaction */
          .fx { position: fixed; inset: 0; z-index: -1; overflow: hidden; pointer-events: none; }
          .goutte-fx {
            position: absolute; top: -12%; width: 2px; height: 18px; border-radius: 2px;
            background: linear-gradient(transparent, rgba(120,160,210,.55));
            animation: chute-pluie linear infinite;
          }
          @keyframes chute-pluie { to { transform: translateY(118vh); } }
          .flocon {
            position: absolute; top: -6%; border-radius: 50%;
            background: rgba(255,255,255,.9); animation: chute-neige linear infinite;
          }
          @keyframes chute-neige {
            0% { transform: translate(0, -6vh); opacity: 0; }
            10% { opacity: .9; }
            100% { transform: translate(var(--derive, 10px), 112vh); opacity: .9; }
          }
          .etoile {
            position: absolute; border-radius: 50%; background: #fff;
            animation: scintille ease-in-out infinite alternate;
          }
          @keyframes scintille { from { opacity: .15; } to { opacity: .95; } }
          .halo {
            position: absolute; top: 4%; left: 50%; width: 320px; height: 320px;
            transform: translateX(-50%); border-radius: 50%;
            background: radial-gradient(circle, rgba(255,214,120,.30), transparent 62%);
            animation: pulse-soleil 5s ease-in-out infinite alternate;
          }
          @keyframes pulse-soleil { from { opacity: .55; transform: translateX(-50%) scale(.94); } to { opacity: 1; transform: translateX(-50%) scale(1.06); } }
          .brume-fx {
            position: absolute; left: -20%; width: 140%; height: 90px; border-radius: 50%;
            background: rgba(200,210,220,.18); filter: blur(14px);
            animation: derive-brume linear infinite;
          }
          @keyframes derive-brume { from { transform: translateX(-8%); } to { transform: translateX(8%); } }
          .eclair {
            position: fixed; inset: 0; background: rgba(255,255,255,.0); z-index: -1; pointer-events: none;
          }
          .eclair.flash { animation: flash-eclair .9s ease-out; }
          @keyframes flash-eclair {
            0%, 100% { background: rgba(255,255,255,0); }
            4% { background: rgba(255,255,255,.55); }
            8% { background: rgba(255,255,255,.05); }
            12% { background: rgba(255,255,255,.4); }
            20% { background: rgba(255,255,255,0); }
          }
          .confetti {
            position: absolute; top: -5%; width: 8px; height: 12px; opacity: .9;
            animation: chute-confetti linear infinite;
          }
          @keyframes chute-confetti {
            0% { transform: translateY(0) rotate(0); }
            100% { transform: translateY(114vh) rotate(720deg); }
          }
          .coeur {
            position: absolute; bottom: -8%; animation: monte-coeur linear infinite;
          }
          @keyframes monte-coeur {
            0% { transform: translateY(0) scale(.6); opacity: 0; }
            15% { opacity: .95; }
            100% { transform: translateY(-116vh) scale(1); opacity: 0; }
          }
          @media (prefers-reduced-motion: reduce) {
            .fx, .eclair { display: none; }
          }
          svg { display: inline-block; vertical-align: middle; }
          .header { text-align: center; }
          .lieu {
            display: flex; align-items: center; justify-content: center; gap: 5px;
            font-size: 15px; font-weight: 600; opacity: .72; margin-bottom: 8px;
          }
          .lieu svg { opacity: .85; }
          .nuage-scene { animation: flotte 4.5s ease-in-out infinite alternate; }
          .nuage {
            width: 190px; max-width: 60vw; margin: 0 auto; cursor: pointer;
            -webkit-tap-highlight-color: transparent; user-select: none;
          }
          .nuage svg { width: 100%; height: auto; display: block; }
          /* clignement des yeux (nuage SVG vivant) */
          .yeux { transform-box: view-box; transform-origin: 256px 232px; }
          .yeux.cligne { animation: clignement .2s ease; }
          @keyframes clignement { 0%, 100% { transform: scaleY(1); } 50% { transform: scaleY(.06); } }
          @media (prefers-reduced-motion: reduce) { .yeux.cligne { animation: none; } }
          .nuage.fretille { animation: fretille .55s ease; }
          .nuage.secoue { animation: secoue .6s ease; }
          @keyframes secoue {
            0%, 100% { transform: translateX(0) rotate(0); }
            15% { transform: translateX(-10px) rotate(-8deg); }
            35% { transform: translateX(9px) rotate(7deg); }
            55% { transform: translateX(-7px) rotate(-5deg); }
            75% { transform: translateX(5px) rotate(3deg); }
          }
          @keyframes flotte { from { transform: translateY(0) scale(1); } to { transform: translateY(-9px) scale(1.025); } }
          @keyframes fretille {
            25% { transform: rotate(-6deg) scale(1.04); }
            60% { transform: rotate(5deg); }
            100% { transform: rotate(0); }
          }
          @media (prefers-reduced-motion: reduce) {
            .nuage-scene, .nuage.fretille { animation: none; }
          }
          .temp { font-size: 64px; font-weight: 800; letter-spacing: -2px; }
          .label { font-size: 17px; font-weight: 600; opacity: .85; }
          .ressenti { font-size: 14px; opacity: .65; margin-top: 2px; }
          .infos { display: flex; justify-content: center; gap: 8px; margin-top: 14px; flex-wrap: wrap; }
          .chip {
            display: inline-flex; align-items: center; gap: 6px;
            background: var(--bulle); border-radius: 14px; padding: 6px 12px;
            font-size: 13px; font-weight: 600;
          }
          .vanne {
            margin: 16px auto 0; max-width: 320px;
            font-style: italic; font-size: 15px; line-height: 1.4; text-align: center;
            background: var(--bulle);
            border-radius: 20px; padding: 12px 16px;
          }
          .card {
            background: var(--carte);
            border-radius: 24px; padding: 16px; margin-top: 16px;
            box-shadow: 0 6px 18px rgba(51,65,79,.07);
          }
          .card h2 { font-size: 13px; text-transform: uppercase; letter-spacing: .08em; opacity: .55; margin-bottom: 12px; }
          .prog-l { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; padding: 5px 0; border-bottom: 1px solid currentColor; border-color: rgba(128,128,128,.14); }
          .prog-l:last-of-type { border-bottom: none; }
          .prog-h { font-weight: 600; font-variant-numeric: tabular-nums; }
          .prog-t { text-align: right; opacity: .85; }
          .prog-note { margin-top: 12px; font-style: italic; opacity: .9; }
          .heures { display: flex; gap: 6px; overflow-x: auto; -webkit-overflow-scrolling: touch; padding-bottom: 4px; }
          .heure { min-width: 54px; text-align: center; }
          .h-label { font-size: 12px; opacity: .6; }
          .h-ico { margin: 4px 0; opacity: .9; }
          .h-temp { font-size: 15px; font-weight: 700; }
          .h-pluie { font-size: 11px; opacity: .65; margin-top: 2px; }
          .jour { display: flex; align-items: center; padding: 9px 2px; border-bottom: 1px solid var(--separateur); }
          .jour:last-child { border-bottom: none; }
          .j-nom { flex: 1; font-weight: 600; font-size: 15px; }
          .j-pluie {
            display: inline-flex; align-items: center; justify-content: flex-end; gap: 3px;
            width: 74px; font-size: 12px; opacity: .6;
          }
          .j-ico { width: 44px; display: flex; justify-content: center; opacity: .9; }
          .j-temps { width: 84px; text-align: right; font-size: 15px; font-variant-numeric: tabular-nums; }
          .j-min { opacity: .55; }
          .j-max { font-weight: 700; }
          .stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px 8px; }
          .stat { display: flex; flex-direction: column; align-items: center; gap: 3px; text-align: center; }
          .s-ico { opacity: .8; }
          .s-val { font-size: 16px; font-weight: 700; }
          .s-lbl { font-size: 11px; opacity: .55; line-height: 1.25; }
          .card h2 { display: flex; align-items: center; }
          .aide {
            margin-left: auto; width: 20px; height: 20px; border-radius: 50%;
            border: none; background: var(--bulle); color: var(--texte);
            font: 600 12px -apple-system, sans-serif; opacity: .7; cursor: pointer;
            -webkit-tap-highlight-color: transparent;
          }
          .legende { margin-top: 14px; font-size: 12px; line-height: 1.5; opacity: .72; }
          .legende p { margin-top: 7px; }
          .legende[hidden] { display: none; }
          .aiguille { transform-origin: 12px 12px; animation: vent-sway 3.5s ease-in-out infinite; }
          @keyframes vent-sway { 0%, 100% { transform: rotate(-4deg); } 50% { transform: rotate(4deg); } }
          @media (prefers-reduced-motion: reduce) { .aiguille { animation: none; } }
          \(premiumCSS)
          \(croquisCSS)
        </style></head>
        <body>
          <div class="fond"></div>
          <div class="fx" id="fx"></div>
          <div class="eclair" id="eclair"></div>
          <div class="header">
            \(lieu.isEmpty ? "" : "<div class=\"lieu\">\(svgIcone(estPosition ? "position" : "epingle", 15)) \(lieu)</div>")
            <div class="nuage-scene"><div id="nuage" class="nuage" role="img" aria-label="\(info.label)">\(nuageSVG)</div></div>
            <div class="temp"\(tempStyle)>\(Int(c.temperature_2m.rounded()))°</div>
            <div class="label">\(info.label)</div>
            <div class="ressenti">Ressenti \(Int(c.apparent_temperature.rounded()))°</div>
            <div class="infos">
              <span class="chip">\(svgIcone("vent", 16)) \(vent) km/h</span>
              <span class="chip">\(svgIcone("lever", 16)) \(lever)</span>
              <span class="chip">\(svgIcone("coucher", 16)) \(coucher)</span>
            </div>
          </div>
          <div class="vanne" id="vanne">« \(repliqueAffichee) »</div>
          \(agendaCard)
          \(detailsCard)
          <div class="card"><h2>Prochaines heures</h2><div class="heures">\(heuresHTML)</div></div>
          <div class="card"><h2>La semaine</h2>\(joursHTML)</div>
          <script>
            // taper le nuage : il frétille et sert une nouvelle vanne
            const VANNES = \(vannesJSON);
            const elNuage = document.getElementById("nuage");
            const elVanne = document.getElementById("vanne");
            function nouvelleVanne() {
              let v;
              do { v = VANNES[Math.floor(Math.random() * VANNES.length)]; }
              while (VANNES.length > 1 && "« " + v + " »" === elVanne.textContent);
              elVanne.textContent = "« " + v + " »";
              elNuage.classList.remove("fretille");
              void elNuage.offsetWidth; // relance l'animation
              elNuage.classList.add("fretille");
            }
            elNuage.addEventListener("click", () => {
              nouvelleVanne();
              window.webkit?.messageHandlers?.haptique?.postMessage("tap");
            });
            // secousse de l'appareil (déclenchée depuis Swift) : réaction renforcée
            window.secousse = function() {
              nouvelleVanne();
              elNuage.classList.remove("secoue");
              void elNuage.offsetWidth;
              elNuage.classList.add("secoue");
            };
            // boussole : oriente l'anneau (N + aiguille) selon le cap réel du téléphone.
            // Appelée depuis Swift à chaque mise à jour du magnétomètre.
            const capRot = document.querySelector(".cap-rot");
            window.cap = function(deg) {
              if (capRot) capRot.style.transform = "rotate(" + (-deg) + "deg)";
            };

            // nuage vivant : clignement périodique des yeux (désactivé si œil déjà fermé)
            const PEUT_CLIGNER = \(peutCligner);
            const yeux = PEUT_CLIGNER ? document.querySelector("#nuage .yeux") : null;
            function clignote() {
              if (yeux) {
                yeux.classList.remove("cligne"); yeux.getBoundingClientRect(); yeux.classList.add("cligne");
                if (Math.random() < 0.22) setTimeout(() => {
                  yeux.classList.remove("cligne"); yeux.getBoundingClientRect(); yeux.classList.add("cligne");
                }, 300);
              }
              setTimeout(clignote, 2500 + Math.random() * 4500);
            }
            if (yeux && !matchMedia("(prefers-reduced-motion: reduce)").matches) {
              setTimeout(clignote, 1500 + Math.random() * 2500);
            }

            // nuage vivant : petit frétille automatique de temps en temps
            function fretilleAuto() {
              elNuage.classList.remove("fretille");
              void elNuage.offsetWidth;
              elNuage.classList.add("fretille");
              setTimeout(fretilleAuto, 9000 + Math.random() * 12000);
            }
            if (!matchMedia("(prefers-reduced-motion: reduce)").matches) {
              setTimeout(fretilleAuto, 8000 + Math.random() * 8000);
            }

            // bouton « ? » : déplie la légende d'interprétation
            const elAide = document.getElementById("aide");
            const elLegende = document.getElementById("legende");
            if (elAide && elLegende) {
              elAide.addEventListener("click", () => { elLegende.hidden = !elLegende.hidden; });
            }

            // ambiance météo animée (vague 2)
            const EFFET = "\(effet)";
            const fx = document.getElementById("fx");
            const reduit = matchMedia("(prefers-reduced-motion: reduce)").matches;
            const rnd = (min, max) => min + Math.random() * (max - min);
            function el(cls, css) {
              const d = document.createElement("div");
              d.className = cls;
              Object.assign(d.style, css);
              fx.appendChild(d);
              return d;
            }
            if (fx && !reduit) {
              if (EFFET === "pluie" || EFFET === "orage") {
                for (let i = 0; i < 60; i++) el("goutte-fx", {
                  left: rnd(0, 100) + "vw",
                  animationDuration: rnd(0.5, 1.1) + "s",
                  animationDelay: rnd(0, 2) + "s",
                  opacity: rnd(0.4, 0.9)
                });
              }
              if (EFFET === "neige") {
                for (let i = 0; i < 45; i++) {
                  const t = rnd(3, 7);
                  el("flocon", {
                    left: rnd(0, 100) + "vw",
                    width: t + "px", height: t + "px",
                    "--derive": rnd(-40, 40) + "px",
                    animationDuration: rnd(6, 12) + "s",
                    animationDelay: rnd(0, 8) + "s"
                  });
                }
              }
              if (EFFET === "etoiles") {
                for (let i = 0; i < 50; i++) {
                  const t = rnd(1, 3);
                  el("etoile", {
                    left: rnd(0, 100) + "vw", top: rnd(0, 55) + "vh",
                    width: t + "px", height: t + "px",
                    animationDuration: rnd(1.5, 3.5) + "s",
                    animationDelay: rnd(0, 3) + "s"
                  });
                }
              }
              if (EFFET === "soleil") { el("halo", {}); }
              if (EFFET === "brume") {
                for (let i = 0; i < 4; i++) el("brume-fx", {
                  top: rnd(10, 80) + "vh",
                  animationDuration: rnd(7, 13) + "s",
                  animationDelay: rnd(0, 4) + "s",
                  animationDirection: i % 2 ? "alternate-reverse" : "alternate"
                });
              }
              if (EFFET === "orage") {
                const ec = document.getElementById("eclair");
                const flash = () => {
                  ec.classList.remove("flash"); void ec.offsetWidth; ec.classList.add("flash");
                  setTimeout(flash, rnd(4000, 9000));
                };
                setTimeout(flash, rnd(1500, 4000));
              }
            }

            // décor festif des occasions spéciales (vague 4)
            const DECO = "\(deco)";
            if (fx && !reduit && DECO) {
              if (DECO === "neige") {
                for (let i = 0; i < 45; i++) {
                  const t = rnd(3, 7);
                  el("flocon", { left: rnd(0, 100) + "vw", width: t + "px", height: t + "px",
                    "--derive": rnd(-40, 40) + "px", animationDuration: rnd(6, 12) + "s",
                    animationDelay: rnd(0, 8) + "s" });
                }
              }
              if (DECO === "confettis") {
                const cols = ["#FF6B6B", "#FFD93D", "#6BCB77", "#4D96FF", "#C780FA"];
                for (let i = 0; i < 44; i++) el("confetti", {
                  left: rnd(0, 100) + "vw", background: cols[i % cols.length],
                  borderRadius: Math.random() < .5 ? "2px" : "50%",
                  animationDuration: rnd(2.5, 5) + "s", animationDelay: rnd(0, 4) + "s"
                });
              }
              if (DECO === "coeurs") {
                for (let i = 0; i < 18; i++) {
                  const h = el("coeur", { left: rnd(0, 100) + "vw",
                    fontSize: rnd(14, 26) + "px",
                    animationDuration: rnd(5, 9) + "s", animationDelay: rnd(0, 6) + "s" });
                  h.textContent = "💗";
                }
              }
            }
          </script>
        </body></html>
        """
    }

    // MARK: - Icônes SVG (port de svgIcone)

    static func svgIcone(_ nom: String, _ taille: Int = 24) -> String {
        let nuage = "<path d=\"M7 18h10a4 4 0 0 0 .75-7.93A6 6 0 0 0 6.2 9.4 3.9 3.9 0 0 0 7 18Z\"/>"
        let nuageHaut = "<g transform=\"translate(0 -2.6)\">" + nuage + "</g>"
        let icones: [String: String] = [
            "soleil": "<circle cx=\"12\" cy=\"12\" r=\"4.2\"/><path d=\"M12 2.8v2.2M12 19v2.2M2.8 12H5M19 12h2.2M5.2 5.2l1.6 1.6M17.2 17.2l1.6 1.6M18.8 5.2l-1.6 1.6M6.8 17.2l-1.6 1.6\"/>",
            "lune": "<path d=\"M20.4 13.2A8.5 8.5 0 1 1 10.8 3.6a6.8 6.8 0 0 0 9.6 9.6Z\"/>",
            "soleil-nuage": "<circle cx=\"7.6\" cy=\"6.8\" r=\"2.5\"/><path d=\"M7.6 2.6v1.3M3.4 6.8h1.3M4.6 3.8l.9.9M10.6 3.8l-.9.9\"/><path d=\"M10 19.5h7a3.4 3.4 0 0 0 .64-6.74A5 5 0 0 0 9.3 11.7 3.3 3.3 0 0 0 10 19.5Z\"/>",
            "lune-nuage": "<path d=\"M12.3 7.4A4.3 4.3 0 1 1 7.3 2.4a3.4 3.4 0 0 0 5 5Z\"/><path d=\"M10 19.5h7a3.4 3.4 0 0 0 .64-6.74A5 5 0 0 0 9.3 11.7 3.3 3.3 0 0 0 10 19.5Z\"/>",
            "nuage": nuage,
            "brouillard": nuageHaut + "<path d=\"M6 19h12M8 21.6h8\"/>",
            "bruine": nuageHaut + "<path d=\"M9.5 18.6v2M13 19.6v2M16.5 18.6v2\"/>",
            "pluie": nuageHaut + "<path d=\"M9.8 18.6l-1.1 2.8M13.4 18.6l-1.1 2.8M17 18.6l-1.1 2.8\"/>",
            "neige": nuageHaut + "<g fill=\"currentColor\" stroke=\"none\"><circle cx=\"9.5\" cy=\"19.6\" r=\"1\"/><circle cx=\"13.2\" cy=\"21\" r=\"1\"/><circle cx=\"16.6\" cy=\"19.3\" r=\"1\"/></g>",
            "orage": nuageHaut + "<path d=\"M13.2 16.8l-2.6 3.7h2l-1.1 3.3 3.7-4.6h-2.1l1.5-2.4Z\" fill=\"currentColor\" stroke=\"none\"/>",
            "goutte": "<path d=\"M12 4.8C9.2 9 7.4 11.5 7.4 14a4.6 4.6 0 0 0 9.2 0C16.6 11.5 14.8 9 12 4.8Z\"/>",
            "vent": "<path d=\"M3.5 8.8h9.7a2.5 2.5 0 1 0-2.5-2.5M3.5 13.6h13.6a2.7 2.7 0 1 1-2.7 2.7\"/>",
            "lever": "<path d=\"M3.5 19h17M8 19a4 4 0 0 1 8 0M5.2 13l1 .9M18.8 13l-1 .9M12 4.4v3.4M9.4 6.4 12 4l2.6 2.4\"/>",
            "coucher": "<path d=\"M3.5 19h17M8 19a4 4 0 0 1 8 0M5.2 13l1 .9M18.8 13l-1 .9M12 7.8V4.4M9.4 5.8 12 8.2l2.6-2.4\"/>",
            "jauge": "<path d=\"M5 17a8 8 0 1 1 14 0\"/><path d=\"M12 17l4-5\"/><circle cx=\"12\" cy=\"17\" r=\"1.3\" fill=\"currentColor\" stroke=\"none\"/>",
            "oeil": "<path d=\"M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z\"/><circle cx=\"12\" cy=\"12\" r=\"3\"/>",
            "duree": "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M12 8v4l2.6 1.6\"/>",
            "fleche": "<path d=\"M12 4.5v15M12 4.5l-5 5M12 4.5l5 5\"/>",
            "epingle": "<path d=\"M12 21.2s6.5-5.9 6.5-10.7a6.5 6.5 0 1 0-13 0c0 4.8 6.5 10.7 6.5 10.7Z\"/><circle cx=\"12\" cy=\"10.2\" r=\"2.4\"/>",
            "position": "<path d=\"M20.5 3.5 3 10.8l7.4 2 2 7.4Z\"/>",
        ]
        let corps = icones[nom] ?? nuage
        return "<svg viewBox=\"0 0 24 24\" width=\"\(taille)\" height=\"\(taille)\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\">\(corps)</svg>"
    }

    // MARK: - Helpers

    // Nuage en SVG inline (depuis le bundle) → animable (clignement). Le widget, lui,
    // reste sur les PNG (une extension ne peut pas animer).
    // Échappe le texte libre (titres d'évènements) avant injection dans le HTML.
    private static func echappe(_ s: String) -> String {
        s.replacingOccurrences(of: "&", with: "&amp;")
         .replacingOccurrences(of: "<", with: "&lt;")
         .replacingOccurrences(of: ">", with: "&gt;")
         .replacingOccurrences(of: "\"", with: "&quot;")
    }

    private static func svgInline(etat: String, nuit: Bool, ton: Ton) -> String {
        let dossier = ton == .doux ? "svg-doux" : "svg"
        let nom = "\(etat)\(nuit ? "_nuit" : "")"
        guard let url = Bundle.main.url(forResource: nom, withExtension: "svg", subdirectory: dossier),
              let svg = try? String(contentsOf: url, encoding: .utf8) else { return "" }
        return svg
    }

    // Superpose l'accessoire météo au nuage : on lit le SVG de la tenue (même viewBox
    // 512×400), on extrait son contenu et on l'insère juste avant la fermeture du SVG.
    private static func injecteTenue(_ svg: String, tenue: Tenue) -> String {
        guard let url = Bundle.main.url(forResource: tenue.rawValue, withExtension: "svg", subdirectory: "svg-accessoires"),
              let contenu = try? String(contentsOf: url, encoding: .utf8),
              let ouvre = contenu.range(of: ">"),
              let clot = contenu.range(of: "</svg>", options: .backwards),
              let fermeture = svg.range(of: "</svg>", options: .backwards) else { return svg }
        let interne = String(contenu[ouvre.upperBound..<clot.lowerBound])
        return svg.replacingCharacters(in: fermeture, with: "<g class=\"tenue\">\(interne)</g></svg>")
    }

    // Occasions spéciales : (vanne thématique selon le ton, décor festif). nil sinon.
    private static func occasionDuJour(ton: Ton) -> (String?, String) {
        let comp = Calendar.current.dateComponents([.month, .day], from: Date())
        let taquin = ton == .taquin

        // dates spéciales (configurables dans les Réglages) — prioritaires
        let mmdd = String(format: "%02d-%02d", comp.month ?? 0, comp.day ?? 0)
        if let occ = Reglages.anniversaires.first(where: { $0.date == mmdd }) {
            let qui = occ.nom.trimmingCharacters(in: .whitespaces)
            let suffixe = qui.isEmpty ? "" : " \(qui)"
            return (taquin ? "Joyeux anniversaire\(suffixe) ! Un an de plus, ça se fête (et ça se charrie). 🎂"
                           : "Joyeux anniversaire\(suffixe) 🎂 Une journée aussi douce que toi.", "confettis")
        }

        switch (comp.month, comp.day) {
        case (12, 24), (12, 25):
            return (taquin ? "Joyeux Noël. Le Père Noël t'a vu, va falloir t'expliquer."
                           : "Joyeux Noël ❄️ Plein de douceur sur toi aujourd'hui.", "neige")
        case (12, 31), (1, 1):
            return (taquin ? "Bonne année ! Tes résolutions tiendront jusqu'au 3, sois honnête."
                           : "Bonne année ✨ Que du beau et du doux pour toi.", "confettis")
        case (2, 14):
            return (taquin ? "Saint-Valentin. Même un nuage a trouvé l'amour avant toi. Courage."
                           : "Joyeuse Saint-Valentin 💗 Tu es adorable, ne l'oublie jamais.", "coeurs")
        case (4, 1):
            return (taquin ? "Il va faire 45° et neiger en même temps. … Poisson d'avril."
                           : "Petit poisson d'avril tout doux 🐟 Passe une jolie journée.", "")
        case (10, 31):
            return (taquin ? "Halloween. Le seul truc effrayant ici, c'est ta sonnerie de réveil."
                           : "Joyeux Halloween 🎃 Reste au chaud, petit fantôme tout mignon.", "")
        default:
            return (nil, "")
        }
    }

    // Durée du jour (secondes) → "15h47"
    private static func dureeJour(_ secondes: Double) -> String {
        let total = Int(secondes.rounded())
        return "\(total / 3600)h\(String(format: "%02d", (total % 3600) / 60))"
    }

    // Boussole : degrés → point cardinal abrégé + nom complet
    private static let cardinaux = ["N", "NE", "E", "SE", "S", "SO", "O", "NO"]
    private static let cardinauxNoms = ["Nord", "Nord-Est", "Est", "Sud-Est", "Sud", "Sud-Ouest", "Ouest", "Nord-Ouest"]
    private static func cardinal(_ deg: Int) -> String { cardinaux[idxCardinal(deg)] }
    private static func directionNom(_ deg: Int) -> String { cardinauxNoms[idxCardinal(deg)] }
    private static func idxCardinal(_ deg: Int) -> Int { Int((Double(deg) / 45).rounded()) % 8 }

    // Système de couleurs sémantiques (lecture d'un coup d'œil)
    private static let cVert = "#3FA46A", cJaune = "#C99A1E", cOrange = "#E07B39",
                       cRouge = "#D2544B", cBleu = "#4C86C6", cBleuF = "#3A6CA8",
                       cViolet = "#9B59B6", cGris = "#8B95A1"

    private static func humiditeInfo(_ h: Int) -> (String, String) {
        switch h { case ..<40: return ("Air sec", cOrange); case ..<65: return ("Confort", cVert)
                   case ..<80: return ("Humide", cBleu); default: return ("Très humide", cBleuF) }
    }
    private static func rafalesInfo(_ kmh: Double) -> (String, String) {
        switch kmh { case ..<20: return ("Légères", cVert); case ..<40: return ("Modérées", cJaune)
                     case ..<62: return ("Fortes", cOrange); default: return ("Violentes", cRouge) }
    }
    private static func couvertureInfo(_ pct: Int) -> (String, String) {
        switch pct { case ..<13: return ("Dégagé", cBleu); case ..<50: return ("Partiel", cVert)
                     case ..<88: return ("Nuageux", cGris); default: return ("Couvert", cGris) }
    }
    private static func uvInfo(_ uv: Double) -> (String, String) {
        switch uv { case ..<3: return ("Faible", cVert); case ..<6: return ("Modéré", cJaune)
                    case ..<8: return ("Élevé", cOrange); case ..<11: return ("Très élevé", cRouge)
                    default: return ("Extrême", cViolet) }
    }
    private static func visibiliteInfo(_ km: Int) -> (String, String) {
        switch km { case 20...: return ("Excellente", cVert); case 10..<20: return ("Bonne", cVert)
                    case 4..<10: return ("Moyenne", cJaune); case 1..<4: return ("Faible", cOrange)
                    default: return ("Brouillard", cRouge) }
    }

    // Boussole : l'anneau (avec le N) s'oriente selon le cap réel du téléphone (window.cap),
    // l'aiguille pointe la direction géographique vers où va le vent. Sans cap (simulateur),
    // l'anneau reste au nord et un léger balancement « live » anime l'aiguille.
    private static func boussole(_ deg: Int) -> String {
        let rot = (deg + 180) % 360
        return """
        <svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" stroke-width="1.4">
          <circle cx="12" cy="12" r="9.5" opacity="0.5"/>
          <g class="cap-rot" style="transform-box: view-box; transform-origin: 12px 12px;">
            <circle cx="12" cy="3.4" r="0.9" fill="#D2544B" stroke="none"/>
            <g transform="rotate(\(rot) 12 12)"><g class="aiguille">
              <path d="M12 5 L9 13 L12 11.4 L15 13 Z" fill="#D2544B" stroke="none"/>
              <path d="M12 19 L9 13 L12 14.6 L15 13 Z" fill="currentColor" stroke="none" opacity="0.4"/>
            </g></g>
          </g>
        </svg>
        """
    }

    // Yeux affichés ouverts (donc « clignables ») selon l'état et le jour/nuit.
    // De jour : detendu (clin) et blase (yeux tristes) ont l'œil fermé → pas de clignement.
    // De nuit : seuls les états qui gardent leurs yeux ronds (endormi/ronchon/flippe) clignent.
    private static func peutCligner(etat: String, nuit: Bool) -> Bool {
        nuit ? ["endormi", "ronchon", "flippe"].contains(etat)
             : ["radieux", "emmitoufle", "endormi", "ronchon", "flippe"].contains(etat)
    }

    // "yyyy-MM-ddTHH:mm" → "HH:mm"
    private static func heureCourte(_ s: String?) -> String {
        guard let s, s.count >= 16 else { return "" }
        return String(s.dropFirst(11).prefix(5))
    }

    private static let jourFormateur: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = .current
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    private static func jourLocal(_ s: String) -> Date? { jourFormateur.date(from: s) }
}
