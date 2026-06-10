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

## Fonctionnalités

- **Widget moyen** (écran d'accueil) : expression du nuage selon la météo, température,
  ressenti, min/max, pluie dans l'heure (créneaux de 15 min), et la vanne du moment.
- **Vue app** (au tap sur le widget) : semaine complète avec probabilités de pluie,
  détail heure par heure, vent, lever/coucher du soleil. Le nuage y flotte en CSS,
  et le taper le fait frétiller en servant une nouvelle vanne.
- **130 vannes** réparties par condition météo (soleil, pluie, neige, orage, canicule,
  gel, nuit…), avec un tirage **sans répétition sur la journée**.
- **7 expressions** (+ variantes nuit avec étoiles) : radieux, espiègle, perdu dans le
  brouillard, ronchon, triste sous son parapluie, emmitouflé, flippé par l'orage.
- **Thème adaptatif** : suit le mode clair/sombre d'iOS, la nuit force le sombre.
- **Localisation** : GPS automatique, repli sur Schiltigheim si indisponible.
- Icônes météo SVG dessinées main, animations respectant `prefers-reduced-motion`.

## Installation

1. Installer [Scriptable](https://apps.apple.com/app/scriptable/id1405459188) (gratuit).
2. Créer un nouveau script, y coller le contenu de [`le-nuage.js`](le-nuage.js).
3. Lancer une fois avec ▶ : autoriser la localisation ; la vue app s'ouvre et les
   images du nuage se téléchargent en cache local (une seule fois).
4. Sur l'écran d'accueil : appui long → `+` → Scriptable → taille **moyenne**.
5. Appui long sur le widget → *Modifier le widget* → Script : votre script,
   *When Interacting* : **Run Script** (c'est ce qui ouvre la vue app au tap).

Aucune clé API nécessaire : les données viennent d'[Open-Meteo](https://open-meteo.com/).

## Configuration

En tête de `le-nuage.js` :

| Constante | Rôle | Défaut |
|---|---|---|
| `THEME` | `"auto"` suit iOS, `"sombre"` / `"clair"` forcent | `"auto"` |
| `ASSETS_URL` | Base des PNG hébergés (repli si iCloud indisponible) | ce repo |
| `REPLI` | Coordonnées de secours sans GPS | Schiltigheim |
| `SEUIL_CANICULE` / `SEUIL_GEL` | Déclenchement des vannes chaleur/froid | 32 °C / −2 °C |

Les images sont cherchées en cascade : dossier iCloud `Scriptable/nuage/` →
cache local de l'appareil → téléchargement depuis `ASSETS_URL` → nuage dessiné
en code (`DrawContext`) en dernier recours. Le widget fonctionne donc même
sans iCloud, et hors ligne une fois le cache rempli.

## Structure du repo

```
le-nuage/
├── le-nuage.js     # le script Scriptable complet (widget + vue app)
├── gen_nuage.py    # générateur paramétrique des expressions (SVG → PNG)
├── png/            # les 14 expressions consommées par le script (ne pas renommer)
└── svg/            # sources vectorielles
```

## Régénérer les assets

Les expressions sont générées par un script Python paramétrique : changer une
valeur (taille du sourire, position des joues…) régénère les 14 fichiers d'un coup.

```bash
pip install cairosvg
python3 gen_nuage.py        # produit les SVG
# puis conversion en PNG 600 px à fond transparent via cairosvg
```

Palette : corps `#EAF1F9` · ombre `#D8E5F2` · joues `#F4B8BE` · traits `#33414F`.

## Licence

- **Code** (`le-nuage.js`, `gen_nuage.py`) : [MIT](LICENSE) — utilisez, modifiez, partagez.
- **Le Nuage et ses visuels** (`png/`, `svg/`) : [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/deed.fr) —
  réutilisation avec crédit, pas d'usage commercial. Le personnage reste © Jérôme Dubos.

## Notes

- Le rafraîchissement du widget est décidé par iOS (~15–30 min), comme pour
  tout widget. La vue app, elle, charge des données fraîches à chaque ouverture.
- Personnage original. Données météo © Open-Meteo (CC BY 4.0).
