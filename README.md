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

## 🧪 Tester Le Nuage (sans être développeur)

> On t'a partagé ce lien pour essayer le nuage ? Bienvenue. ☁️ Il n'est pas sur l'App
> Store : Apple oblige à l'**installer soi-même** (« sideload »). C'est **gratuit**, ça
> prend ~15 min la première fois, et ça marche avec **ton propre identifiant Apple**.

**Ce qu'il te faut**
- Un **iPhone** (iOS 17 ou plus récent).
- Un **identifiant Apple**. Ton compte habituel convient très bien : il sert seulement à
  « signer » l'app sur ton téléphone, **rien ne m'est envoyé** et Le Nuage ne peut pas
  accéder à tes photos, contacts ou iCloud (il est isolé comme toute app iOS). Si tu
  préfères, tu peux utiliser un second identifiant dédié — c'est optionnel.
- ~15 min et un peu de patience la première fois.

**Étapes**

1. **Récupère l'app** : télécharge le fichier `LeNuage.ipa` depuis la page
   **[Releases](../../releases/latest)** de ce dépôt (dernière version, tout en haut).
2. **Installe SideStore** sur ton iPhone en suivant le guide officiel :
   👉 **<https://sidestore.io/>** (c'est l'outil qui pose l'app et la garde à jour).
   Pendant l'installation, SideStore te demandera de te connecter avec **ton identifiant
   Apple** : c'est normal, ça sert uniquement à signer l'app sur ton téléphone.
3. **Ajoute Le Nuage** : dans SideStore, onglet **My Apps → ➕ → Fichiers →**
   `LeNuage.ipa`. Quand il demande *« Register App ID for Each Extension »*, **accepte**
   (ça garde le widget et la Dynamic Island).
4. **Active le nuage** : Réglages iPhone → **Confidentialité → Mode développeur → activer**
   (redémarrage demandé), puis ouvre **Le Nuage**. 🎉
5. **Pose le widget** : appui long sur l'écran d'accueil → `+` → **Le Nuage** → taille moyenne.

**Bon à savoir (limites du gratuit, imposées par Apple)**
- 🔄 L'app **expire au bout de 7 jours** : garde SideStore installé, il la **rafraîchit
  tout seul** en tâche de fond quand ton iPhone a du réseau. Rien à refaire à la main.
- 📦 Un identifiant Apple gratuit permet **3 apps** sideloadées à la fois (le widget et la
  Dynamic Island font partie du Nuage, pas de souci de ce côté).
- 🔒 Aucune donnée ne quitte ton téléphone : pas de compte, pas de pub, pas de traçage.

*Ça coince ? [Écris-moi](mailto:contact@duboswebservices.fr), je te débloque.* Un serveur
d'authentification (« Anisette ») peut être nécessaire pour SideStore — demande-moi, je
peux t'en prêter un.

## 🛠 Installer depuis les sources (développeurs)

Signature via un **Apple ID gratuit** (Personal Team, 7 jours). Étapes détaillées dans
[`setup-ios.md`](setup-ios.md). En résumé :

1. `xcodegen generate` puis ouvrir `LeNuage.xcodeproj` dans Xcode.
2. Brancher l'iPhone, sélectionner le scheme **LeNuage**, signer avec ton Apple ID, **Run**.
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

## 🌤️ Et demain ?

Le nuage ne compte pas rester sur un seul écran. Doucement, il aimerait s'installer un
peu partout dans ta vie :

- ⌚ **au poignet**, sur l'Apple Watch, sa vanne à portée de regard ;
- 🔲 **en plein d'autres formats** — petits widgets, écran verrouillé, mode veille ;
- 🗣️ **quand tu l'appelles**, d'un « dis, le nuage, il dit quoi ? » ;
- 🇬🇧 **dans d'autres langues**, pour taquiner plus de monde ;
- 🤖 **sur Android**, en version toute douce — « mon p'tit nuage ».

Rien n'est promis, tout est rêvé. *Il flotte, mais il a des projets.*

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
