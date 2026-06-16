# setup-ios.md — Mise en place de l'environnement iOS (« Le Nuage » natif)

Guide pas-à-pas pour passer d'un Mac vierge à une chaîne complète capable de
compiler, tester et installer l'app + le widget « Le Nuage » sur un iPhone, en
**100 % gratuit** (Apple ID gratuit + sideload).

- **Machine cible** : MacBook Air M4 (Apple Silicon).
- **Appareil cible** : le dernier iPhone (iOS 26).
- **Résultat attendu en fin de guide** : un projet qui build sur simulateur, qui
  s'installe sur l'iPhone réel, et qui se ré-signe tout seul sans fil tous les
  7 jours (via SideStore) — donc sans maintenance manuelle récurrente.

> Convention : tout ce qui est `entre back-ticks sur une ligne` est une commande à
> taper dans le **Terminal** (Applications → Utilitaires → Terminal). Les chemins de
> type *Réglages → Général → …* sont des menus à suivre sur le Mac ou l'iPhone.

---

## 0. Avant de commencer — état des lieux

Coche cette liste avant la moindre installation.

| Élément | Détail | Comment vérifier |
|---|---|---|
| Mac à jour | macOS **Sequoia 15.6 minimum** (ou Tahoe 26) — exigé par Xcode 26 | Menu  → « À propos de ce Mac » |
| Espace disque | Prévoir **~30 Go libres** (Xcode pèse lourd, +15 Go) | Réglages → Général → Stockage |
| Apple ID | Un identifiant Apple (gratuit). Celui de l'iPhone convient. | tu le connais |
| iPhone | Le dernier modèle, **avec un code de verrouillage activé** (obligatoire pour le sideload) | Réglages → Face ID et code |
| Câble | Un câble **USB-C ↔ USB-C** (iPhone récent + Mac M4) | physique |
| Wi-Fi | Mac et iPhone sur le **même réseau Wi-Fi** (utile pour SideStore) | — |
| Compte Claude | **Claude Pro minimum** : Claude Code ne fonctionne pas avec le plan gratuit | claude.ai → ton abonnement |

**Mettre macOS à jour si besoin** : Réglages → Général → Mise à jour de logiciels →
installer la dernière version. Sans macOS 15.6+, Xcode 26 refusera de s'installer.

---

## 1. Xcode + outils en ligne de commande

Xcode fournit le SDK iOS, les simulateurs, le compilateur et `xcodebuild`. **Il est
obligatoire** : VSCode seul ne peut pas compiler de l'iOS, il ne fait que piloter
les outils d'Xcode.

1. Ouvre le **Mac App Store**, cherche **Xcode**, clique sur *Obtenir / Installer*.
   C'est un très gros téléchargement (compte 30 min à plusieurs heures selon ta
   connexion). Laisse tourner.
2. Lance Xcode **une première fois**. Il propose d'installer des composants
   additionnels → accepte (mot de passe Mac demandé).
3. Installe les *Command Line Tools* (au cas où) :
   ```
   xcode-select --install
   ```
   Si elles sont déjà là, le message « command line tools are already installed »
   apparaît, c'est normal.
4. Vérifie que tout pointe au bon endroit :
   ```
   xcode-select -p
   xcodebuild -version
   ```
   La 1re commande doit renvoyer `/Applications/Xcode.app/Contents/Developer`.
   La 2e doit afficher **Xcode 26.x**. Si ce n'est pas le cas :
   ```
   sudo xcode-select -s /Applications/Xcode.app
   sudo xcodebuild -license accept
   ```

> **Note SDK** : l'obligation « build avec Xcode 26 » concerne les soumissions à
> l'App Store. Toi tu sideloades, donc ce n'est pas une contrainte légale pour toi —
> mais installer le dernier Xcode te donne le SDK iOS 26 et t'évite toute surprise.

5. **Télécharger la plateforme iOS** (Xcode 26 ne l'embarque plus, ~8,5 Go) :
   ```
   xcodebuild -downloadPlatform iOS
   ```
   Sans elle, aucune destination simulateur n'existe (« iOS 26.x is not installed »).
   Si l'installation se plaint d'un *Duplicate* de runtime, liste les images
   (`xcrun simctl runtime list`) et supprime celles marquées *Unusable*
   (`xcrun simctl runtime delete <UUID>`), en gardant celle marquée *Ready*.

---

## 2. Connecter l'Apple ID dans Xcode (signature gratuite)

C'est ce qui permet de signer l'app avec ton compte gratuit.

1. Xcode → menu **Xcode → Settings…** (ou ⌘ + ,).
2. Onglet **Accounts** → bouton **+** en bas à gauche → **Apple ID** → connecte-toi.
3. Une **« Personal Team »** apparaît sous ton compte. C'est ton équipe de signature
   gratuite.

**Ce que « gratuit » implique (contrainte Apple, pas Xcode)** :
- Une app signée gratuitement **expire au bout de 7 jours** → il faut la ré-signer.
  (Avec un compte payant à 99 €/an, ce serait 1 an. On reste gratuit → SideStore
  automatisera ce refresh, voir §10.)
- **3 apps installées simultanément** maximum par appareil avec un Apple ID gratuit.
  « Le Nuage » = **1 seule app** (le widget est une extension embarquée dans le
  bundle de l'app, il ne consomme pas de slot séparé). Tu es donc très large.

---

## 3. Homebrew (gestionnaire de paquets)

Sert à installer XcodeGen (et, si tu veux, Claude Code et Node).

1. Installe Homebrew :
   ```
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```
2. À la fin, Homebrew te demande d'ajouter son chemin au shell. Sur Apple Silicon :
   ```
   echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
   eval "$(/opt/homebrew/bin/brew shellenv)"
   ```
3. Vérifie :
   ```
   brew --version
   ```

---

## 4. XcodeGen

XcodeGen génère le fichier projet Xcode à partir d'un `project.yml` versionné dans
le repo. Avantage : le projet est **reproductible** et lisible (pas de `.xcodeproj`
binaire impossible à relire dans 6 mois).

```
brew install xcodegen
xcodegen --version
```

Plus tard, dans le dossier du projet, la commande sera simplement `xcodegen generate`
pour (re)fabriquer le `.xcodeproj`.

---

## 5. Claude Code (+ VSCode)

### Claude Code

Méthode **recommandée** par Anthropic : l'installeur natif (aucun Node requis,
se met à jour tout seul) :
```
curl -fsSL https://claude.ai/install.sh | bash
```
Ouvre **une nouvelle fenêtre de Terminal** ensuite (pour recharger le PATH), puis :
```
claude --version
```
Au premier `claude` lancé dans un dossier de projet, une connexion par navigateur
s'ouvre (OAuth) → connecte-toi avec ton compte **Claude Pro**.

> Alternatives si tu préfères : `brew install --cask claude-code` (Homebrew), ou
> `npm install -g @anthropic-ai/claude-code` (nécessite Node 18+, voir §6). L'installeur
> natif reste le plus simple et le seul à s'auto-mettre à jour.

### VSCode

1. Télécharge **Visual Studio Code** depuis code.visualstudio.com, glisse-le dans
   Applications.
2. (Optionnel mais conseillé) installe l'extension **Swift** (éditeur : `swiftlang`)
   pour la coloration et l'autocomplétion Swift.
3. Tu lanceras Claude Code depuis le **terminal intégré** de VSCode (Terminal →
   Nouveau terminal), dans le dossier du projet. C'est là que se fera ~95 % du dev.

---

## 6. (Optionnel) Node.js

**Non requis** pour ce projet iOS : XcodeGen vient de Homebrew et Claude Code de son
installeur natif. N'installe Node que si tu en as besoin par ailleurs (tes projets
React/Vite, ou la méthode npm de Claude Code) :
```
brew install node
node -v && npm -v
```

---

## 7. Préparer l'iPhone — Mode développeur

1. Branche l'iPhone au Mac avec le câble USB-C. Sur l'iPhone, **« Se fier à cet
   ordinateur ? » → Se fier**, puis saisis ton code.
2. Active le **Mode développeur** :
   Réglages → **Confidentialité et sécurité** → tout en bas → **Mode développeur** →
   active-le → l'iPhone **redémarre** → confirme après le redémarrage.
3. Dans Xcode, vérifie que l'iPhone est vu :
   menu **Window → Devices and Simulators** → ton iPhone doit apparaître à gauche
   (la 1re fois, laisse-le « préparer » l'appareil quelques minutes).

---

## 8. Premier build sur **simulateur** (validation de la chaîne)

Objectif : prouver que la chaîne compile, **avant** de toucher à l'iPhone.

Une fois le projet créé (phase de dev avec Claude Code), depuis le dossier du projet :
```
xcodegen generate
open LeNuage.xcodeproj
```
Dans Xcode : choisis un simulateur (ex. *iPhone 17* en haut), bouton **▶ Run**.
En ligne de commande, l'équivalent ressemble à :
```
xcodebuild -scheme LeNuage -destination 'platform=iOS Simulator,name=iPhone 17' build
```
Le widget se teste depuis le simulateur (appui long sur l'écran d'accueil simulé →
ajout du widget).

---

## 9. Premier install sur l'iPhone **réel** (via Xcode, USB)

C'est la voie la plus rapide pour valider l'app sur le vrai matériel. Elle vaut
**7 jours**, après quoi il faut re-brancher pour re-signer (d'où SideStore en §10).

1. En haut d'Xcode, choisis **ton iPhone** comme cible (à la place du simulateur).
2. Sélectionne la cible de l'app → onglet **Signing & Capabilities** :
   - coche **Automatically manage signing**,
   - **Team** = ta *Personal Team*,
   - **Bundle Identifier** = quelque chose d'unique, ex. `com.jeromedubos.lenuage`.
3. Bouton **▶ Run**. Au premier lancement, l'iPhone refuse l'app non vérifiée :
   Réglages → **Général → VPN et gestion de l'appareil** → sous *App du développeur*,
   sélectionne ton compte → **Se fier**.
4. Relance depuis Xcode : l'app s'ouvre sur l'iPhone. 🎉

---

## 10. SideStore — refresh sans fil (faible maintenance)

C'est la pièce qui rend le projet « sans entretien » : SideStore re-signe l'app
**en arrière-plan via le Wi-Fi**, sans rebrancher au Mac chaque semaine. Tu n'as
besoin de l'ordinateur **qu'une seule fois**, pour l'installation initiale.

Le flux 2026 utilise deux briques : **LocalDevVPN** (sur l'iPhone) et **iloader**
(sur le Mac). C'est la procédure officielle (docs.sidestore.io) — elle a remplacé
les anciens AltServer/JitterbugPair.

### 10.1 Prérequis

**Sur l'iPhone** :
1. Installe **LocalDevVPN** depuis l'App Store (gratuit).
2. Ouvre-le, **Connect** → « Autoriser les configurations VPN » → autorise + code.
   ⚠️ Ce VPN doit être **activé** à chaque fois que tu installes / mets à jour /
   rafraîchis une app dans SideStore. Il ne sert qu'à ça, en local.

**Sur le Mac** :
3. Télécharge **iloader** (build macOS universel) depuis la page officielle
   `iloader.app` (ou les *releases* GitHub `nab138/iloader`), et installe-le comme
   une app classique.

### 10.2 Installer SideStore

1. Branche l'iPhone au Mac en USB (et *Se fier* si demandé).
2. Ouvre **iloader** sur le Mac.
3. **Connecte-toi avec ton Apple ID** (attention, la casse compte).
4. Sélectionne ton iPhone.
5. Clique **Install SideStore (Stable)**.
6. Sur l'iPhone : Réglages → **Général → VPN et gestion de l'appareil** → fais
   confiance à l'app du développeur (ton compte) → **Se fier** → redémarrage si
   proposé.
7. Ouvre **LocalDevVPN** → **Connect**.
8. Ouvre **SideStore** → connecte-toi avec **le même Apple ID** qu'à l'étape 3.
9. Onglet **My Apps** → tape sur le compteur **« 7 DAYS »** à droite de SideStore
   pour le rafraîchir une première fois. Si on te propose de créer / révoquer un
   certificat → **Yes / Refresh Now**.

SideStore est maintenant posé et auto-géré.

### 10.3 Installer « Le Nuage » via SideStore

1. Côté dev, on produira un fichier **`.ipa`** (export d'Xcode ou via Claude Code).
2. Transfère-le sur l'iPhone (AirDrop, ou app Fichiers — **évite iCloud Drive** qui
   peut modifier l'extension).
3. Dans **SideStore** → **+** → choisis le `.ipa` → SideStore le re-signe et l'installe.
4. SideStore le rafraîchira ensuite **automatiquement** tant que le VPN local est
   actif au moment du refresh.

### 10.4 Entretien

- **Refresh manuel** si une app passe à expiration : ouvre LocalDevVPN (Connect),
  puis SideStore → My Apps → tape le compteur de jours. Ne **désinstalle pas** l'app,
  contente-toi de la rafraîchir.
- **Fichier de pairing expiré** (Apple le fait parfois aléatoirement, ou après une
  mise à jour iOS) : symptôme = erreurs de refresh. Solution : rebranche l'iPhone,
  relance **iloader** et réinstalle/répare le pairing. C'est rare.

---

## 11. Récapitulatif — checklist finale

| Outil | Commande de vérif | Attendu |
|---|---|---|
| macOS | menu  → À propos | ≥ 15.6 (Sequoia) ou 26 |
| Xcode | `xcodebuild -version` | Xcode 26.x |
| CLT | `xcode-select -p` | …/Xcode.app/Contents/Developer |
| Apple ID | Xcode → Settings → Accounts | Personal Team visible |
| Homebrew | `brew --version` | un numéro |
| XcodeGen | `xcodegen --version` | un numéro |
| Claude Code | `claude --version` | un numéro |
| iPhone | Window → Devices and Simulators | iPhone listé |
| Mode dev | Réglages iPhone → Confidentialité | Mode développeur activé |
| SideStore | app sur l'iPhone | « My Apps » fonctionne |

---

## 12. Dépannage courant

- **`xcodebuild` n'affiche pas Xcode 26** → `sudo xcode-select -s /Applications/Xcode.app`.
- **`claude: command not found`** → ouvre une **nouvelle** fenêtre de Terminal (le
  PATH n'était pas encore rechargé), ou relance l'installeur natif.
- **`brew` introuvable après install** → tu as oublié l'étape `eval "$(/opt/homebrew/bin/brew shellenv)"` (§3).
- **L'app ne s'ouvre pas sur l'iPhone (« développeur non vérifié »)** → Réglages →
  Général → VPN et gestion de l'appareil → Se fier à ton compte.
- **SideStore : « no valid server found » / erreur AFC** → le **VPN LocalDevVPN
  n'est pas connecté**, ou le pairing a expiré (cf. §10.4).
- **Refresh impossible** → vérifie que tu utilises **le même Apple ID** partout
  (iloader + SideStore), et que le Wi-Fi est actif (pas en données mobiles).

---

*Ce document décrit l'installation de l'environnement. Le plan de portage du code
lui-même est dans `brief-portage-ios.md`. À tenir à jour si la chaîne d'outils évolue.*
