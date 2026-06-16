# Le Nuage (iOS natif) — brief de portage

Document de référence pour la phase de code (sous Claude Code). Portage de l'app
météo Scriptable existante **« Le Nuage »** (taquin) vers une **app iOS native
SwiftUI + widget WidgetKit**. Usage personnel, sideload gratuit. Source de vérité
unique — à tenir à jour pendant le dev.

> Pendant côté **Android** : « Mon p'tit nuage » (version douce, pour la chérie).
> Objectif global : deux vraies apps (iOS taquin / Android doux), **gratuites à vie**,
> propres et maintenables via IA, sans maintenance active.

---

## 1. Objectif

Reproduire fidèlement « Le Nuage » en natif :
- un **widget moyen** sur l'écran d'accueil (nuage expressif + météo + vanne),
- une **vue app** plein écran (semaine, détail horaire, ressenti) ouverte au tap.

Gratuit, sans pub, sans compte, sans clé API. **Open-Meteo uniquement** (pas
WeatherKit, qui exige le programme Apple Developer payant).

---

## 2. Stack & décisions techniques

- **Swift + SwiftUI**, **WidgetKit** pour le widget. iOS récent.
- Projet généré via **XcodeGen** (`project.yml` versionné) → reproductible. **Deux
  targets** : l'app + l'extension widget.
- Météo : **Open-Meteo**, même endpoint et mêmes paramètres que l'iOS Scriptable.
- **Vue app = `WKWebView`** qui charge le HTML existant (`buildHTML`), ré-utilisé tel
  quel. *Décision clé* : ce HTML devient la **vue app partagée iOS + Android**, donc
  un seul fichier à maintenir pour les deux plateformes. Seules les deux coques widget
  (SwiftUI / Kotlin) diffèrent.
- **Localisation** : `CLLocationManager` (autorisation *When In Use*) dans l'app ;
  coordonnées partagées au widget via **App Group** (voir §8).
- Assets : les **14 PNG** embarqués dans l'**asset catalog** (plus de cascade
  iCloud / téléchargement / DrawContext — tout ça disparaît en natif).
- Persistance des vannes du jour : **UserDefaults** (remplace le Keychain iOS).
- Unités : **°C** et **km/h** (comme le script actuel).
- **Bundle id** : `com.jeromedubos.lenuage` *(acté)*.
- **App Group** : `group.com.jeromedubos.lenuage` *(acté)*.
- **Deployment target** : **iOS 18** *(acté)* — marge confortable, WidgetKit moderne ;
  l'iPhone cible (iOS 26) est bien au-dessus, viser iOS 26 n'apporterait aucune API utile.
- **Ton commutable taquin / doux** *(acté, en périmètre)* : le ton n'est **jamais codé
  en dur**. Deux banques de vannes (taquine + douce) et deux jeux de PNG (`png/` +
  `png-doux/`, déjà présents dans le repo). Le choix vit dans l'App Group, lu par l'app
  **et** le widget. Voir §10 et le jalon 6.

---

## 3. Architecture cible

À adapter selon le dev, mais l'esprit reste minimal (simplicité > sur-ingénierie) :

```
LeNuage/
  App/
    LeNuageApp.swift          // point d'entrée SwiftUI
    AppView.swift             // Activity unique : une WKWebView plein écran
    WebViewContainer.swift    // pont SwiftUI ↔ WKWebView + injection du HTML
    HTMLBuilder.swift         // génère le HTML (port de buildHTML)
  Shared/                     // partagé app ↔ widget
    WeatherService.swift      // appel Open-Meteo + décodage Codable
    WeatherModels.swift       // structs Codable (current, hourly, daily, minutely_15)
    WMOCode.swift             // mappings : état nuage / groupe réplique / libellé+icône
    Repliques.swift           // banque de vannes + tirage sans répétition (UserDefaults)
    LocationProvider.swift    // CoreLocation + repli Schiltigheim + App Group
    PluieDansLHeure.swift     // logique minutely_15
  Widget/
    LeNuageWidget.swift       // widget moyen + TimelineProvider
    WidgetView.swift          // layout SwiftUI du widget (parité visuelle)
  Assets.xcassets/            // 14 PNG du nuage + icône d'app
  project.yml                 // XcodeGen (2 targets + App Group)
  README.md
```

---

## 4. Données (Open-Meteo) — inventaire du script réel

Un **seul** appel `fetch` couvre tout. Paramètres exacts du script actuel à
reproduire à l'identique :

- `latitude`, `longitude` (4 décimales)
- `current=temperature_2m,apparent_temperature,weather_code,is_day,precipitation,wind_speed_10m`
- `minutely_15=precipitation` (pluie dans l'heure)
- `daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max,sunrise,sunset`
- `hourly=temperature_2m,weather_code,precipitation_probability`
- `forecast_days=7`
- `timezone=auto`

Endpoint : `https://api.open-meteo.com/v1/forecast`. Pas de clé. Réponse JSON →
structs `Codable`.

---

## 5. Mapping WMO — trois tables parallèles (depuis le code)

**(a) Code → état du nuage** (image affichée) :

| Code WMO | État (fichier PNG) |
|---|---|
| 0 | `radieux` |
| 1–3 | `detendu` |
| 45, 48 | `endormi` |
| 51–57 | `ronchon` |
| 61–67, 80–82 | `blase` |
| 71–77, 85, 86 | `emmitoufle` |
| ≥ 95 | `flippe` |
| (défaut) | `detendu` |

**(b) Code → groupe de répliques** : 0→clear · 1–3→clouds · 45,48→fog · 51–57→drizzle ·
61–67/80–82→rain · 71–77/85–86→snow · ≥95→storm · (défaut)→clouds.

**(c) Code → libellé FR + icône SVG** (vue app) : ciel clair, plutôt dégagé,
partiellement nuageux, couvert, brouillard, bruine, pluie, neige, averses, averses
de neige, orage — avec variantes jour/nuit (`soleil`/`lune`, etc.).

**Modificateurs transverses** (prioritaires sur le groupe) :
- `temperature_2m ≥ 32 °C` → pool **heat** (canicule).
- `temperature_2m ≤ -2 °C` → pool **cold** (gel).
- `is_day === 0` → variante **nuit** de l'image + pool **night** ajouté aux vannes.

---

## 6. Le widget (taille moyenne)

Parité visuelle avec l'actuel. Contenu (rangée haute + bandeau bas) :
- **nuage** à gauche (image selon l'état météo), ~92×70.
- à droite : **libellé** météo, grande **température**, ligne
  **`↓ min° ↑ max° · Ress. ressenti°`**, et **pluie dans l'heure** (« Pluie dans
  ~N min » / « Pas de pluie dans l'heure »).
- **vanne du jour** en bandeau bas (2 lignes max, italique).
- Fond **dégradé** jour/nuit ; la nuit force le sombre.
- Refresh planifié ~**30 min** (`TimelineProvider`, en respectant le budget iOS).
- **Widget d'erreur** si réseau KO (nuage `flippe` + message).

> ⚠️ **Correction du doc** : le ressenti **est** affiché dans le widget (le code le
> met sur la ligne min/max). L'ancien `context.md` disait le contraire — c'est lui
> qui était en retard sur le code. On garde le ressenti dans le widget.

---

## 7. La vue app (WKWebView)

- Réutilise **`buildHTML`** tel quel : header avec **nuage flottant** (tap = frétille
  + nouvelle vanne, pool embarqué), grande temp + **ressenti**, chips **vent / lever /
  coucher**, carte **« prochaines heures »** (12 h, scroll horizontal, proba ≥ 30 %),
  carte **« la semaine »** (7 j, min/max + proba ≥ 20 %).
- Thème clair/sombre, **nuit force le sombre**.
- Conserver le **fix scroll-bounce iOS** déjà présent (calque `.fond` fixe
  surdimensionné ±100vh, `background-attachment: fixed` étant cassé sur WKWebView).
- L'image du nuage est injectée en **base64** dans le HTML (lue depuis l'asset catalog).
- `prefers-reduced-motion` respecté (déjà dans le HTML).

---

## 8. Localisation (App Group, multi-lieux prévu)

- L'**app** possède la localisation : `CLLocationManager`, autorisation *When In Use*,
  clé `NSLocationWhenInUseUsageDescription` dans l'Info.plist.
- L'app écrit les **coordonnées actives** (et plus tard la **liste de lieux**
  enregistrés) dans un conteneur **App Group** partagé.
- Le **widget** ne demande jamais la position (peu fiable en extension) : son
  `TimelineProvider` **lit** les coordonnées dans l'App Group puis appelle Open-Meteo.
- **Repli Schiltigheim** (`48.6056, 7.7497`) si aucune position disponible.
- L'App Group **fonctionne en signature gratuite** ; le seul coût reste la
  re-signature 7 j, automatisée par SideStore (cf. `setup-ios.md` §10).
- *Évolution prévue* : sélection / ajout de lieux dans l'app → la liste vit déjà dans
  le conteneur partagé, donc l'archi est compatible dès le départ.

---

## 9. Assets

- **2 × 14 PNG** (7 expressions + variantes nuit, en version **taquine** `png/` et
  **douce** `png-doux/`) embarqués dans `Assets.xcassets`. Les deux jeux existent déjà
  dans le repo. Le widget/app charge le jeu correspondant au ton actif (App Group).
- Générés par `gen_nuage.py` / `gen_nuage_doux.py` (palette taquine : corps `#EAF1F9`,
  ombre `#D8E5F2`, joues `#F4B8BE`, traits `#33414F`). Repo `Jerome-Dubos/le-nuage`.
- **Icône d'app** : à décliner (variante du nuage `radieux`) en icône iOS.
- Plus aucune logique de chargement réseau / iCloud / dessin de secours : en natif,
  les images sont garanties présentes dans le bundle.

---

## 10. Répliques (banque + tirage sans répétition)

- **Deux banques** par condition : `clear, clouds, fog, drizzle, rain, snow, storm,
  heat, cold, night`. Banque **taquine** (texte du script `le-nuage.js`) et banque
  **douce** (`messages-doux.json`, déjà dans le repo). Le ton actif (App Group)
  sélectionne la banque. *Jérôme régénère/complète les banques et les ajoutera au
  projet quand elles seront prêtes — prévoir le chargement depuis une ressource (JSON)
  pour ne pas figer le texte dans le code Swift.*
- `{TEMP}` remplacé par la température courante arrondie.
- **Tirage sans répétition sur la journée** : historique des vannes servies stocké en
  **UserDefaults** (remplace le Keychain), remis à zéro chaque jour ou quand le pool
  est épuisé.
- Pool **night** concaténé la nuit ; pools **heat/cold** prioritaires aux seuils.

---

## 11. Build & validation

- **Claude Code** : `xcodegen generate`, build + run sur **simulateur**
  (`xcodebuild` / `xcrun simctl`) pour valider que ça compile et tourne.
- **Jérôme (Xcode)** : signature **Apple ID gratuit** + install iPhone (USB) pour la
  1re validation sur vrai matériel (vaut 7 j).
- **Sideload pérenne** : export `.ipa` → **SideStore** (refresh sans fil). Détails
  complets dans `setup-ios.md`.
- **Rien** qui suppose un compte payant ou une distribution App Store (pas de
  capability payante, pas de provisioning « distribution »).

---

## 12. Plan de livraison (jalons)

> **État (à jour)** : jalons 1→4 et 6 **faits** ; jalon 5 en cours (reste l'**icône
> d'app**). Au-delà du plan initial, sont aussi livrés : **nuage vivant** (SVG inline
> qui cligne/respire), **interactions** (tap haptique, shake), **boussole orientée au
> cap** du téléphone, **effets météo** + décors d'occasion, **Détails** enrichis à
> couleurs sémantiques, **dates spéciales génériques** (récurrence annuelle), et le
> **multi-lieux** (système de pages GPS + villes recherchées). Détail courant :
> [`context.md`](context.md).

1. **Squelette** ✅ — `project.yml` XcodeGen, 2 targets + App Group, build simulateur à
   vide. Jalon = « ça compile et se lance, app + widget vides ».
2. **Couche données partagée** ✅ — `WeatherService` (Open-Meteo + Codable), `WMOCode`
   (3 mappings), `LocationProvider` (GPS + repli + App Group), `PluieDansLHeure`.
   Transposition 1:1 du JS.
3. **Widget natif moyen** ✅ — parité visuelle, 14 PNG embarqués, vannes sans répétition
   (UserDefaults), refresh ~30 min, widget d'erreur. Jalon = « le widget affiche la
   vraie météo sur l'écran d'accueil ».
4. **Vue app + localisation** ✅ — `WKWebView` + `buildHTML` ré-habillé, App Group qui
   relie app ↔ widget, autorisation de localisation. Jalon = « tap sur le widget →
   vue app complète, le widget suit la position ».
5. **Finitions** ✅ — jour/nuit, budget de refresh iOS, `README`, **icône d'app**
   (nuage radieux sur dégradé bleu). Jalon = livrable installable et auto-rafraîchi.
6. **Réglages + ton commutable** ✅ — écran de réglages dans l'app (toggle **taquin /
   doux**), ton persisté dans l'App Group, app + widget lisent le ton actif pour choisir
   banque de vannes et jeu de PNG. Jalon = « on bascule taquin↔doux dans les réglages,
   l'app et le widget suivent ».

---

## 13. Qualité de code

- Séparation claire **data / widget / ui**, noms explicites.
- Commentaires **utiles à une reprise 6–12 mois** uniquement.
- **Pas de sur-ingénierie** : une seule Activity/vue app, pas de DI lourde.
- Réutiliser au maximum la logique éprouvée du script JS (mappings, seuils, formats).

---

## 14. Décisions verrouillées

- Stack : **SwiftUI + WidgetKit**, projet **XcodeGen** (2 targets).
- Données : **Open-Meteo** seul (pas WeatherKit).
- Vue app : **WKWebView réutilisant le HTML existant** (partagé iOS/Android à terme).
- Widget : **taille moyenne**, parité visuelle, **ressenti inclus**.
- Localisation : **app = CoreLocation**, **widget = lecture App Group**, repli
  Schiltigheim. Architecture **multi-lieux** prête.
- Assets : **14 PNG embarqués** (fin de la cascade de chargement).
- Persistance vannes : **UserDefaults**, tirage sans répétition sur la journée.
- Distribution : **Apple ID gratuit + SideStore** (refresh sans fil), zéro compte payant.
- Identité : **bundle id** `com.jeromedubos.lenuage`, **App Group**
  `group.com.jeromedubos.lenuage`, **deployment target iOS 18**.
- Ton : **commutable taquin / doux** dans les réglages de l'app (jalon 6), jamais codé
  en dur. Deux banques de vannes + deux jeux de PNG, choix partagé via l'App Group.

---

## 15. Questions ouvertes restantes

*Tout le reste a été tranché (cf. §2 et §14). Il ne reste que :*

- **Police** de la vue app : système arrondi (déjà en place) — on garde.
