# context.md — Le Nuage (iOS natif)

Référence technique de l'état **actuel** du projet (source de vérité unique du code en
place). Pour l'historique des décisions de portage, voir [`brief-portage-ios.md`](brief-portage-ios.md).
Pour le sideload, voir [`setup-ios.md`](setup-ios.md).

> App météo « cute mais taquine » : un nuage expressif qui commente la météo. App
> SwiftUI plein écran + widget WidgetKit. Gratuit, sans pub, sans compte, sans clé API
> (Open-Meteo seul). Sideload via Apple ID gratuit + SideStore.

---

## 1. Stack & identité

- **Swift + SwiftUI + WidgetKit**, deux targets (app `LeNuage` + extension
  `LeNuageWidgetExtension`).
- Projet généré par **XcodeGen** (`project.yml` versionné, `.xcodeproj` régénérable et
  non commité). Régénérer après ajout de fichier : `xcodegen generate`.
- **Bundle id** `com.jeromedubos.lenuage` · **App Group** `group.com.jeromedubos.lenuage`
  · **Team** `7AZ4QCLZAA` (Personal Team, signature gratuite) · **cible iOS 18**.
- Météo : **Open-Meteo** (`api.open-meteo.com`), géocodage **Open-Meteo geocoding**, pas
  de clé. Reverse-geocoding du GPS via **CLGeocoder** (nom de ville).

---

## 2. Arborescence réelle

```
LeNuage/
  App/
    LeNuageApp.swift        // @main
    AppView.swift           // racine : TabView paginé (GPS + lieux) + barre flottante
    PageMeteoVue.swift       // une page météo = un lieu (charge sa météo, rend le HTML)
    WebViewContainer.swift  // pont SwiftUI ↔ WKWebView : HTML, haptique, secousse, cap
    HTMLBuilder.swift       // génère tout le HTML/CSS/JS de la vue app (le plus gros)
    ReglagesView.swift      // réglages : ton taquin/doux + dates spéciales (jour+mois)
    LieuxView.swift         // recherche de ville (geocoding) + gestion des lieux
  Shared/                   // partagé app ↔ widget
    AppGroupe.swift         // suite App Group + Reglages (ton, anniversaires, lieux) + modèles
    Couleurs.swift          // Color(hex:) + PaletteTon (palettes taquin/doux, vars CSS)
    WeatherModels.swift     // structs Codable Open-Meteo + Coordonnees + Horodatage
    WeatherService.swift    // appel météo + recherche de lieu (geocoding)
    WMOCode.swift           // mappings code WMO → état nuage / groupe réplique / libellé+icône
    Repliques.swift         // banques de vannes + tirage sans répétition (App Group)
    LocationProvider.swift  // CoreLocation : position, cap (boussole), nom de ville
    PluieDansLHeure.swift   // logique minutely_15 (pluie dans l'heure)
    NouvelleVanneIntent.swift // AppIntent : bouton ✨ du widget → recharge timeline
  Widget/
    LeNuageWidget.swift     // widget moyen + TimelineProvider + vue (PNG, pas d'anim)
  svg/  svg-doux/           // 14 SVG du nuage vivant (7 expressions × jour/nuit) par ton
  png/  png-doux/           // PNG équivalents (utilisés par le widget)
  Nuages.xcassets           // PNG nuage pour le widget
  Assets.xcassets           // AppIcon (1024², nuage radieux sur dégradé bleu) — source icon-src.svg
  vannes-taquines.json  messages-doux.json   // banques de vannes par ton
  gen_nuage.py  gen_nuage_doux.py            // générateurs SVG paramétriques
  project.yml               // XcodeGen
```

Les dossiers `App` et `Shared` sont des sources XcodeGen : tout nouveau `.swift` y est
pris automatiquement à la régénération. `svg/` et `svg-doux/` sont des **folder
references** (sous-répertoires conservés dans le bundle, lus par `Bundle.main.url(...,
subdirectory:)`).

---

## 3. App ↔ widget : modèles partagés (`AppGroupe.swift`)

Tout passe par `AppGroupe.defaults` (suite App Group, repli `.standard` si indisponible —
ce qui est le cas sur **simulateur**, où le partage app↔widget ne marche donc pas ; OK sur
device). `enum Reglages` expose :

- **`ton: Ton`** (`taquin` / `doux`) — sélectionne banque de vannes, jeu de SVG/PNG,
  palette. Jamais codé en dur.
- **`anniversaires: [Anniversaire]`** — `Anniversaire { id, nom, date "MM-dd" }`.
  Liste générique gérable dans les Réglages. Stockée en JSON. **Récurrence annuelle**
  (comparaison jour/mois, sans année). Migre automatiquement l'ancien format
  (clés `anniv-moi` / `anniv-cherie`).
- **`lieux: [Lieu]`** — `Lieu { id, nom, lat, lon }`. Lieux ajoutés en plus du GPS.
  Stockés en JSON.
- Coordonnées GPS actives (`lat` / `lon`) écrites par `LocationProvider`, lues par le widget.

---

## 4. Données Open-Meteo (`WeatherService` + `WeatherModels`)

Un seul appel `forecast`. Paramètres actuels :

- `current` = temperature_2m, apparent_temperature, weather_code, is_day, precipitation,
  wind_speed_10m, **relative_humidity_2m, wind_gusts_10m, wind_direction_10m, cloud_cover**
- `minutely_15` = precipitation
- `daily` = temperature_2m_max/min, weather_code, precipitation_probability_max,
  sunrise, sunset, **uv_index_max, daylight_duration, sunshine_duration**
- `hourly` = temperature_2m, weather_code, precipitation_probability, **visibility**
- `forecast_days=7`, `timezone=auto`

**Recherche de lieu** : `WeatherService.recherche(_:)` → `geocoding-api.open-meteo.com/
v1/search` (anti-rebond 300 ms côté UI), renvoie `[LieuTrouve]` (nom + région + pays).

---

## 5. Mapping WMO (`WMOCode.swift`)

- **code → état nuage** : 0 `radieux` · 1–3 `detendu` · 45/48 `endormi` · 51–57 `ronchon`
  · 61–67/80–82 `blase` · 71–77/85–86 `emmitoufle` · ≥95 `flippe` · défaut `detendu`.
- **code → groupe réplique** : clear / clouds / fog / drizzle / rain / snow / storm.
- **code → libellé FR + icône**, variantes jour/nuit.
- Modificateurs prioritaires : `≥32 °C` pool **heat**, `≤-2 °C` pool **cold**,
  `is_day==0` variante **nuit** + pool **night**.

---

## 6. Vue app (`HTMLBuilder` → `WKWebView`)

HTML/CSS/JS généré en Swift (destiné à rester la **vue partagée iOS/Android**). Sections :
entête **lieu** (nom + icône position/épingle), **nuage** hero, température (+ ressenti,
teinte canicule/gel en taquin), chips vent/lever/coucher, **vanne**, carte **Détails**
(8 stats à couleurs sémantiques + boussole + légende `?`), **prochaines heures** (12 h),
**la semaine** (7 j). Signature : `build(_:ton:lieu:estPosition:)`.

**Nuage vivant** (app seulement — SVG inline depuis `svg/` ou `svg-doux/`) :
- **clignement** des yeux (`<g class="yeux">` mis à l'échelle Y) — désactivé quand l'œil
  est déjà fermé (`peutCligner(etat:nuit:)` : pas de clin `detendu`, ni yeux tristes
  `blase`, ni mi-clos de nuit).
- **respiration** (`flotte`), **frétille** au tap (+ haptique) et automatique,
  **secousse** sur shake de l'appareil (`window.secousse`).

**Boussole** (stat « Vent ») : l'aiguille pointe la direction géographique du vent ;
l'anneau (N + aiguille) **s'oriente selon le cap réel du téléphone** via `window.cap(deg)`,
appelé par `WebViewContainer` à chaque mise à jour du magnétomètre. Sans cap (simulateur),
léger balancement `vent-sway`.

**Effets météo** (`.fx`) : pluie, neige, étoiles, halo soleil, brume, éclairs d'orage.
**Décor d'occasion** : confettis / cœurs / neige selon la date spéciale.
`prefers-reduced-motion` coupe toutes les animations.

---

## 7. Système de pages multi-lieux (`AppView` + `PageMeteoVue`)

`TabView` paginé (`.tabViewStyle(.page)`), points de pagination affichés dès qu'il y a un
lieu. **Page 0 = position GPS** (toujours présente, nom via reverse-geocoding, « Ma
position » en repli). Pages suivantes = `Reglages.lieux`. Chaque `PageMeteoVue` recharge
sa météo quand coords / nom / ton changent. Barre flottante : **+** (gauche → `LieuxView`),
**réglages** (droite → `ReglagesView`). Le **cap** (`loc.cap`) est passé à toutes les pages.

> Le **widget** reste sur la position GPS (lue dans l'App Group). Pas de multi-lieux widget.

---

## 8. Design / tokens (`Couleurs.swift`)

`PaletteTon` par ton, injectée en variables CSS (`varsClaires` / `varsSombres`) ; la nuit
force le thème sombre.
- **Taquin** (premium, ombres soignées, lisérés) : clair `#EAF1F9` / `#33414F`, sombre
  `#DCEBFA` / `#2B3A4E`. Police système arrondie.
- **Doux** (croquis / crayon, bordures dessinées main, séparateurs pointillés) : papier
  crème `#FBF3E6` / ardoise chaude `#34302A`, police manuscrite (Noteworthy).

Un changement de ton **transforme toute l'ambiance** (app + widget), pas qu'un élément.

**Couleurs sémantiques** des Détails (lecture d'un coup d'œil) : vert `#3FA46A` (bon),
jaune `#C99A1E`, orange `#E07B39`, rouge `#D2544B`, bleu `#4C86C6` / `#3A6CA8`
(froid/humide), violet `#9B59B6`, gris `#8B95A1`.

Nuage généré par `gen_nuage*.py` (paramétrique, `<g class="yeux">` pour l'animation).
Régénérer : éditer le script puis pointer `OUT_SVG` vers `svg/` (ou `svg-doux/`).

---

## 9. Widget (`LeNuageWidget.swift`)

Widget moyen, parité visuelle. Lit coords + ton dans l'App Group, appelle Open-Meteo,
refresh ~30 min. Nuage en **PNG** (une extension ne peut pas animer le SVG). Vanne
centrée avec marges ; bordure pointillée croquis en doux. Bouton `Button(intent:
NouvelleVanneIntent())` (icône ✨) → nouvelle vanne. Widget d'erreur si réseau KO.

---

## 10. Interactions natives (`WebViewContainer`)

- tap nuage → message JS `haptique` → `UIImpactFeedbackGenerator(.light)`.
- shake appareil → `UIWindow.motionEnded` poste `.nuageSecoue` → JS `window.secousse()`
  (+ haptique succès).
- cap magnétomètre → `evaluateJavaScript("window.cap(deg)")` (sans recharger la page).

---

## 11. Build & validation

- Régénérer : `xcodegen generate`. Build : scheme **LeNuage** (jamais le scheme widget
  seul). `xcodebuild -scheme LeNuage -destination 'generic/platform=iOS Simulator' build`.
- Validation simulateur : `xcrun simctl` (install / launch / screenshot). Avant lancement :
  `simctl privacy <dev> grant location <bundle>` + `simctl location <dev> set <lat,lon>`.
- **Non testable en simulateur** : App Group réel, **cap magnétomètre**, haptique. À
  vérifier sur device.
- Distribution : Apple ID gratuit + **SideStore** (refresh 7 j sans fil), zéro compte payant.

---

## 12. Conventions

- JavaScript/SwiftUI sans TypeScript ; **CSS par composant** côté HTML, variables CSS pour
  le design system. Modifier le **minimum** de fichiers.
- Commentaires : uniquement ce qui aide une reprise à 6–12 mois.
- Aucune donnée personnelle codée en dur (app destinée à être distribuée) → tout
  configurable dans les Réglages.
