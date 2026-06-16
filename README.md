# Le Nuage ☁️

> Un widget météo iOS mignon mais taquin. Plus il a l'air inoffensif, plus ses vannes font mouche.

<p align="center">
  <img src="png/radieux.png" width="110" alt="Radieux">
  <img src="png/detendu.png" width="110" alt="Espiègle">
  <img src="png/endormi.png" width="110" alt="Perdu">
  <img src="png/ronchon.png" width="110" alt="Ronchon">
  <img src="png/blase.png" width="110" alt="Triste">
  <img src="png/emmitoufle.png" width="110" alt="Emmitouflé">
  <img src="png/flippe.png" width="110" alt="Flippé">
</p>

Un petit nuage tout doux s'installe sur l'écran d'accueil, affiche la météo du jour
et balance une réplique cinglante en français — affectueusement insultant, jamais
méchant. Gratuit, sans pub, sans abonnement, sans compte.

« *Il flotte. Reste couché, le monde t'en demande pas plus.* »

> **Note :** ce dépôt est désormais une **app iOS native** (SwiftUI + WidgetKit).
> L'ancienne version [Scriptable](le-nuage.js) reste présente comme source du portage.

## Fonctionnalités

- **Widget moyen** (écran d'accueil) : expression du nuage selon la météo, température,
  ressenti, min/max, pluie dans l'heure (créneaux de 15 min), la vanne du moment, et un
  bouton ✨ pour en tirer une nouvelle.
- **App plein écran** : semaine complète avec probabilités de pluie, détail heure par
  heure, carte **Détails** (humidité, vent, rafales, couverture, UV, ensoleillement,
  visibilité…) à **couleurs sémantiques** pour lire d'un coup d'œil.
- **Nuage vivant** : il flotte, **respire**, **cligne des yeux**, frétille quand on le
  tape (retour haptique) et réagit quand on **secoue** le téléphone.
- **Boussole** du vent qui **s'oriente selon le cap réel** du téléphone.
- **Multi-lieux** : ta position GPS plus les villes de ton choix (recherche par nom), en
  **pages** que l'on fait défiler.
- **Deux tons** commutables : **Taquin** (piquant, bleu) ou **Doux** (bienveillant,
  croquis au crayon) — change toute l'ambiance, app + widget.
- **Dates spéciales** : anniversaires et fêtes configurables, qui **reviennent chaque
  année** avec une réplique dédiée et des confettis.
- **Effets météo** (pluie, neige, étoiles, orage, brume), tirage des vannes **sans
  répétition sur la journée**, thème clair/sombre (la nuit force le sombre),
  `prefers-reduced-motion` respecté.

Aucune clé API : données d'[Open-Meteo](https://open-meteo.com/) (météo + recherche de villes).

## Installation (sideload gratuit)

Pas d'App Store : l'app s'installe via un **Apple ID gratuit** (signature 7 jours,
rafraîchie sans fil par SideStore). Étapes détaillées dans [`setup-ios.md`](setup-ios.md).
En résumé :

1. `xcodegen generate` puis ouvrir `LeNuage.xcodeproj` dans Xcode.
2. Brancher l'iPhone, sélectionner le scheme **LeNuage**, signer avec ton Apple ID
   (Personal Team), **Run**.
3. Ajouter le widget : appui long sur l'écran d'accueil → `+` → **Le Nuage** → taille moyenne.
4. Pour une installation pérenne : exporter l'`.ipa` et l'installer via
   [SideStore](https://sidestore.io/) (refresh automatique tous les 7 jours).

## Réglages

Tout se configure dans l'app (bouton réglages en haut à droite) :

| Réglage | Effet |
|---|---|
| **Caractère du nuage** | bascule **Taquin / Doux** (vannes, visuels, palette) |
| **Dates spéciales** | liste nom + jour/mois, récurrence annuelle automatique |
| **Lieux** (bouton **+**) | recherche d'une ville à ajouter ; le GPS reste la 1ʳᵉ page |

## Structure du repo

App native SwiftUI (`App/`, `Shared/`, `Widget/`), assets nuage (`svg/`, `svg-doux/`,
`png/`, `png-doux/`), projet **XcodeGen** (`project.yml`). Détail complet et conventions :
**[`context.md`](context.md)**. Historique des décisions de portage : [`brief-portage-ios.md`](brief-portage-ios.md).

## Régénérer les expressions du nuage

Générées par un script Python paramétrique (changer une valeur régénère les 14 SVG d'un
coup). Les yeux sont balisés `<g class="yeux">` pour permettre le clignement animé.

```bash
python3 gen_nuage.py        # ton taquin  → svg/        (ajuster OUT_SVG)
python3 gen_nuage_doux.py   # ton doux     → svg-doux/
```

Palette taquine : corps `#EAF1F9` · ombre `#D8E5F2` · joues `#F4B8BE` · traits `#33414F`.

## Licence

- **Code** (`le-nuage.js`, `gen_nuage.py`) : [MIT](LICENSE) — utilisez, modifiez, partagez.
- **Le Nuage et ses visuels** (`png/`, `svg/`) : [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/deed.fr) —
  réutilisation avec crédit, pas d'usage commercial. Le personnage reste © Jérôme Dubos.

## Notes

- Le rafraîchissement du widget est décidé par iOS (~15–30 min), comme pour
  tout widget. La vue app, elle, charge des données fraîches à chaque ouverture.
- Personnage original. Données météo © Open-Meteo (CC BY 4.0).
