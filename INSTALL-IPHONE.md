# Installer « Le Nuage » sur ton iPhone (et le garder à vie)

Guide pas à pas, à suivre **une seule fois**. Objectif : installer l'app via **SideStore**
pour qu'elle se **re-signe toute seule tous les 7 jours**, sans rebrancher le Mac.

> Pour le détail technique / le dépannage avancé, voir [`setup-ios.md`](setup-ios.md) §10.
> Ici c'est la version « checklist » centrée sur ce qu'il te reste à faire.

---

## Ce que tu as déjà

- ✅ L'app fonctionne (tu l'as lancée via Xcode).
- ✅ Le fichier à installer est prêt :
  ```
  /Users/jeromedubos/Documents/Projets/Le Nuage/LeNuage.ipa
  ```
  (1,4 Mo, widget inclus. Volontairement **non signé** : SideStore le signe à l'install.)

## Ce qu'il reste à faire (≈ 20–30 min, une fois)

1. [ ] Préparer l'iPhone et le Mac (prérequis)
2. [ ] Installer SideStore sur l'iPhone (via iloader)
3. [ ] Installer « Le Nuage » dans SideStore (le `.ipa`)
4. [ ] Ajouter le widget + faire le ménage
5. [ ] (à connaître) Le refresh des 7 jours

---

## 0. Prérequis

**Sur l'iPhone**
- [ ] Un **code de verrouillage** activé (obligatoire pour le sideload) :
  Réglages → Face ID/Touch ID et code.
- [ ] **Même réseau Wi-Fi** que le Mac.
- [ ] Installer **LocalDevVPN** depuis l'App Store (gratuit). Ouvre-le → **Connect** →
  autorise la configuration VPN (+ code).
  ⚠️ Ce VPN doit être **activé** à chaque install/refresh dans SideStore. Il ne fait
  que ça, en local — aucune donnée ne sort.

**Sur le Mac**
- [ ] Télécharger **iloader** (build macOS) depuis `iloader.app` ou les *releases* GitHub
  `nab138/iloader`. Installe-le comme une app normale (glisser dans Applications).
- [ ] Connaître ton **Apple ID** et son mot de passe (le même partout — la casse compte).

> 💡 Apple ID gratuit = limites normales : **3 apps** sideloadées max en même temps, et
> les identifiants d'app **expirent au bout de 7 jours** (c'est tout l'intérêt de
> SideStore : il les renouvelle automatiquement).

---

## 1. Installer SideStore (via iloader)

1. [ ] Branche l'iPhone au Mac en **USB**. Sur l'iPhone, tape **Se fier** si demandé
   (+ code).
2. [ ] Ouvre **iloader** sur le Mac.
3. [ ] **Connecte-toi avec ton Apple ID** dans iloader.
4. [ ] Sélectionne **ton iPhone** dans la liste.
5. [ ] Clique **Install SideStore (Stable)**. Laisse-le faire.
6. [ ] Sur l'iPhone, fais confiance au certificat développeur :
   Réglages → **Général → VPN et gestion de l'appareil** → sous *App du développeur*,
   choisis **ton compte** → **Se fier** (redémarre si proposé).
7. [ ] Ouvre **LocalDevVPN** → **Connect** (s'il s'est déconnecté).
8. [ ] Ouvre **SideStore** → connecte-toi avec **le même Apple ID** qu'à l'étape 3.
9. [ ] Onglet **My Apps** → tape le compteur **« 7 DAYS »** à droite de SideStore pour
   le rafraîchir une 1re fois. Si on te propose de créer/révoquer un certificat →
   **Yes / Refresh Now**.

✅ SideStore est posé. Tu n'as plus besoin du câble pour la suite.

---

## 2. Installer « Le Nuage »

1. [ ] **AirDrop** le fichier `LeNuage.ipa` du **Mac vers l'iPhone**.
   - Sur le Mac : clic droit sur `LeNuage.ipa` → **Partager → AirDrop** → ton iPhone.
   - ⚠️ **Évite iCloud Drive** comme transfert : il renomme parfois l'extension et casse
     le fichier. AirDrop (ou un câble) est le plus sûr.
2. [ ] Sur l'iPhone, accepte la réception → « Enregistrer dans Fichiers » (n'importe quel
   dossier **local**, ex. *Sur mon iPhone*).
3. [ ] Ouvre **LocalDevVPN** → **Connect**.
4. [ ] Ouvre **SideStore** → bouton **+** (en haut) → **parcourt Fichiers** → choisis
   `LeNuage.ipa`.
5. [ ] SideStore le **re-signe et l'installe**. L'icône du nuage apparaît sur l'écran
   d'accueil. 🎉

---

## 3. Widget + ménage

1. [ ] **Ajouter le widget** : appui long sur l'écran d'accueil → **+** (en haut à
   gauche) → cherche **Le Nuage** → choisis la taille **moyenne** → *Ajouter*.
2. [ ] **Autoriser la localisation** : ouvre l'app une fois → *Autoriser lorsque l'app
   est active* (sinon la météo reste sur Schiltigheim par défaut).
3. [ ] **Supprimer la version installée par Xcode** (s'il y en a une en doublon) : c'est
   maintenant **celle de SideStore** qui doit vivre sur le téléphone, car c'est elle qui
   gère le refresh automatique.

---

## 4. Le refresh des 7 jours (à connaître)

- **En théorie, rien à faire** : tant que **LocalDevVPN est connecté** quand SideStore
  fait son refresh (il le tente en arrière-plan), l'app reste valide indéfiniment.
- **Bon réflexe** : une fois par semaine, ouvre **LocalDevVPN → Connect**, puis
  **SideStore → My Apps** et tape le compteur de jours si une app approche de 0.
  - ⚠️ **Ne désinstalle jamais** l'app pour la « réinstaller » : tu perdrais tes réglages
    (lieux, dates, ton). Un **refresh** suffit, il ne touche pas aux données.

---

## 5. Si ça coince

| Symptôme | Cause probable | Solution |
|---|---|---|
| SideStore : *« no valid server found »* / erreur AFC | LocalDevVPN pas connecté, ou pairing expiré | Ouvre LocalDevVPN → **Connect**, réessaie. Sinon rebranche au Mac et relance iloader. |
| Refresh impossible | Apple ID différent entre iloader et SideStore, ou données mobiles | Même Apple ID partout, **Wi-Fi actif** (pas en 4G/5G). |
| L'app ne s'ouvre plus après ~7 j | Refresh raté | LocalDevVPN → Connect → SideStore → My Apps → tape le compteur. |
| `.ipa` refusé par SideStore | Fichier altéré (passé par iCloud) | Re-transfère par **AirDrop**, ou redemande-moi un `.ipa` frais. |
| *« App du développeur non fiable »* | Certificat pas approuvé | Réglages → Général → VPN et gestion de l'appareil → **Se fier**. |

---

## Plus tard : régénérer un `.ipa` (nouvelle version)

Quand on modifiera l'app, il faudra un nouveau `.ipa`. Demande-le-moi, ou en une ligne
depuis le dossier du projet :

```bash
xcodebuild -project LeNuage.xcodeproj -scheme LeNuage -configuration Release \
  -sdk iphoneos -derivedDataPath build/ipa CODE_SIGNING_ALLOWED=NO build
APP="build/ipa/Build/Products/Release-iphoneos/LeNuage.app"
rm -rf build/Payload LeNuage.ipa && mkdir -p build/Payload && cp -R "$APP" build/Payload/ \
  && (cd build && zip -qr -X ../LeNuage.ipa Payload) && rm -rf build/Payload
```

Puis dans SideStore : **+** → choisis le nouveau `.ipa` (il met à jour l'app **sans
effacer tes données**, même bundle id).

---

*Bloqué ? Reviens vers moi avec le message d'erreur exact (et à quelle étape), je te
débloque.* 🌥️
