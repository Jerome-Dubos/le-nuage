// ============================================================
// Le Nuage — widget météo mignon mais taquin
// Scriptable (iOS) · Open-Meteo · gratuit, sans clé API
//
// Un seul script, deux modes :
//   - sur l'écran d'accueil (widget moyen) → vue compacte
//   - tapé / ouvert dans Scriptable → "vraie app" en WebView HTML
//
// Assets : dépose les PNG dans iCloud Drive/Scriptable/nuage/
// (radieux.png, detendu.png, endormi.png, ronchon.png, blase.png,
//  emmitoufle.png, flippe.png + variantes *_nuit.png).
// Tant qu'ils n'existent pas, le nuage est dessiné en DrawContext.
// ============================================================

// ---------- Config ----------
const REPLI = { latitude: 48.6056, longitude: 7.7497 } // Schiltigheim
const DOSSIER_ASSETS = "nuage" // sous Documents de Scriptable (iCloud)
// URL de base des PNG hébergés (repli si iCloud indisponible/saturé).
const ASSETS_URL = "https://raw.githubusercontent.com/Jerome-Dubos/le-nuage/main/png"
const SEUIL_CANICULE = 32      // °C — réplique chaleur prioritaire
const SEUIL_GEL = -2           // °C — réplique froid prioritaire

const PALETTE = {
  corps: "#EAF1F9",
  ombre: "#D8E5F2",
  joues: "#F4B8BE",
  encre: "#33414F",
}

// Apparence : "auto" suit le réglage iOS clair/sombre ; "sombre"/"clair" forcent.
// La nuit météo (is_day = 0) force toujours le sombre.
const THEME = "auto"

function teinte(claire, sombre) {
  if (THEME === "sombre") return new Color(sombre)
  if (THEME === "clair") return new Color(claire)
  return Color.dynamic(new Color(claire), new Color(sombre))
}

// ---------- Banque de répliques ----------
// {TEMP} est remplacé par la température courante arrondie.
const REPLIQUES = {
  clear: [
    "Lève-toi, il fait beau, t'as plus d'excuse pour pourrir au lit.",
    "Soleil de ouf. Sors prendre l'air, tu sens le renfermé.",
    "Grand bleu. Même moi j'ai posé ma journée.",
    "Zéro nuage à part moi. T'as vu ce charisme.",
    "Soleil plein pot. Mets de la crème, futur homard.",
    "Il fait beau. Profite, ça compensera le reste de ta journée.",
    "Ciel dégagé. Ton linge, lui, attend toujours d'être étendu.",
    "Pas un nuage. Enfin si, moi. De rien.",
    "Beau fixe. Va toucher de l'herbe, ça urge.",
    "Soleil radieux. Toi par contre, on repassera.",
    "Temps magnifique. Le gâche pas devant un écran. Oui, celui-là.",
    "Vitamine D gratuite dehors. Plus rentable que tes cryptos.",
    "Le soleil s'est levé pour toi. Fais pareil, par politesse.",
    "Beau temps. Les terrasses t'appellent. Ton canapé survivra.",
    "Ciel bleu azur. Tes cernes, eux, restent gris.",
    "Pas l'ombre d'un nuage. Sauf moi. Je suis l'exception.",
  ],
  clouds: [
    "Quelques nuages. Les collègues passent dire bonjour.",
    "Ciel voilé. Comme tes ambitions du jour.",
    "Mi-soleil mi-nuages. Bref, aussi décidé que toi.",
    "Nuageux. Aujourd'hui c'est moi qui régale.",
    "Couvert. Le ciel a mis sa couette, fais pareil.",
    "Gris clair. Ni beau ni moche, comme ta coupe actuelle.",
    "Le soleil joue à cache-cache. Lui au moins il joue.",
    "Plafond gris. Parfait pour ta productivité légendaire.",
    "Les nuages squattent. On est chez nous, désolé.",
    "Temps mitigé. Prends une veste, pas tes grands airs.",
    "Ciel chargé. Comme ta semaine, courage.",
    "Variable. Le ciel se décide pas, ça me rappelle quelqu'un.",
    "Un peu de gris, un peu de bleu. La vie, quoi.",
    "Nuages décoratifs partout. Aucun ne m'arrive à la cheville.",
  ],
  fog: [
    "On voit que dalle dehors. Comme ta motivation ce matin.",
    "Brouillard. Le ciel a la flemme, je compatis.",
    "Purée de pois. Conduis doucement, tête en l'air.",
    "Tout est flou dehors. Non, c'est pas tes yeux. Quoique.",
    "Brouillard épais. Idéal pour disparaître de tes responsabilités.",
    "Visibilité zéro. Ton avenir aussi, mais ça on gère plus tard.",
    "La brume a tout mangé. Même ta rue. Surtout ta rue.",
    "Brouillard à couper au couteau. Allume tes feux, pas tes stories.",
    "Le paysage est en mode avion. Patiente.",
    "On y voit rien. Parfait pour rater des gens que tu connais.",
    "Brume mystique dehors. Toi en pyjama, ça casse le mythe.",
    "Même moi je me suis perdu là-dedans. Et j'habite le ciel.",
  ],
  drizzle: [
    "Bruine. Même pas une vraie pluie, juste de quoi t'agacer.",
    "Crachin. Ton brushing est déjà mort, lâche l'affaire.",
    "Il pleuviote. Le ciel postillonne, charmant.",
    "Bruine fine. Trop pour tes lunettes, pas assez pour une excuse.",
    "Le ciel bave un peu. Essuie-toi et avance.",
    "Crachin breton en exil. Capuche, et on en parle plus.",
    "Micro-pluie. Le parapluie ferait pitié, assume mouillé.",
    "Il tombe trois gouttes. Oui, pile sur toi. C'est personnel.",
    "Bruine tenace. Comme tes mauvaises habitudes.",
    "Le ciel hésite à pleuvoir. Toi à sortir. Match nul.",
    "Humidité ambiante : 100 % agaçante.",
    "Ni pluie ni beau temps. Le ciel fait semblant de bosser.",
  ],
  rain: [
    "Il flotte. Reste couché, le monde t'en demande pas plus.",
    "Pluie battante. Parfait pour annuler tes plans, comme d'hab.",
    "Prends le parapluie. Pas celui que t'as perdu, l'autre.",
    "Il pleut des cordes. Tes chaussettes vont morfler.",
    "Averses. Cours entre les gouttes, champion.",
    "Il drache. Même les canards ont posé un RTT.",
    "Pluie non-stop. Bonne excuse pour ce que tu comptais pas faire.",
    "Ça tombe dru. Tes plantes sont arrosées, occupe-toi de toi.",
    "Déluge en cours. L'arche est pas comprise, débrouille-toi.",
    "Il pleut. Moi je pleure, nuance. Sois gentil aujourd'hui.",
    "Grosse pluie. Le pyjama est une tenue de jour validée.",
    "Rideau d'eau dehors. Le canapé a gagné, accepte ta défaite.",
    "Pluie violente. Tes chaussures blanches, c'est non.",
    "Ça rince. Au moins ta voiture sera propre, pour une fois.",
    "Temps de chien. Reste au sec, boule de poils.",
    "Il pleut sur tes rêves de barbecue. Sur les miens aussi.",
  ],
  snow: [
    "Il neige. Mets pas tes claquettes, abruti adorable.",
    "Flocons partout. Essaie de pas glisser devant tout le monde.",
    "Il neige. T'as cinq minutes pour redevenir un gosse, fonce.",
    "Neige fraîche. Gratte ta voiture en silence, on t'entend râler d'ici.",
    "Tout est blanc. Touche pas la neige jaune, on en a déjà parlé.",
    "Il neige sérieux. Les transports vont faire des transports.",
    "Poudreuse en ville. Bonhomme de neige obligatoire, c'est la loi.",
    "Flocons XXL. Tire la langue, personne te juge. Enfin si, moi.",
    "Neige qui tient. Tes semelles lisses, elles, tiendront pas.",
    "Ça tombe blanc. Photo, chocolat chaud, et arrête de te plaindre.",
    "Il neige. Ta voiture est devenue un igloo. Bon courage.",
    "Manteau blanc dehors. Manteau tout court sur toi, merci.",
  ],
  storm: [
    "Ça va péter dehors. Range ton barbecue, génie.",
    "Orage en vue. Débranche ta box, pas tes neurones.",
    "Tonnerre au programme. Le ciel est de ton humeur, on dirait.",
    "Ça gronde. Reste pas planté sous un arbre, hein.",
    "Orage violent. Même moi j'ai peur, et j'en fais partie.",
    "Éclairs gratuits dans le ciel. Meilleur spectacle que ta série.",
    "Le ciel pète un câble. Toi, reste zen, pour une fois.",
    "Ça tonne sévère. Le chien du voisin est sous le lit. Rejoins-le.",
    "Alerte orage. Ferme les fenêtres, pas les yeux, on conduit.",
    "Zeus est de mauvais poil. Le nargue pas en slip dehors.",
    "Boum boum là-haut. C'est pas tes voisins, pour une fois.",
    "Orage musclé. Recharge ton téléphone tant qu'il y a du jus.",
  ],
  heat: [
    "{TEMP}. Bois de l'eau, pas que de la bière. Bon ok, les deux.",
    "{TEMP}. Le bitume fond, toi aussi. Hydrate-toi.",
    "Canicule. Ferme les volets, vampire de compétition.",
    "{TEMP}. Le four est jaloux.",
    "Chaleur de plomb. Le ventilo est ton seul ami. Moi aussi, mais je souffle pas.",
    "{TEMP} à l'ombre. Et y'a pas d'ombre.",
    "Cagnard absolu. Évite le sport, héros. La sieste est un sport.",
    "Une chaleur à cuire des œufs sur le capot. Teste pas.",
    "{TEMP}. Même moi je transpire, et je suis fait de vapeur.",
    "Fournaise dehors. L'aspirateur attendra demain. Ou jamais.",
    "Canicule confirmée. Pense aux anciens, à ton chat, et à boire.",
    "{TEMP}. La clim du supermarché devient une destination de vacances.",
  ],
  cold: [
    "Ça caille sévère. Mets un pull ou arrête de chialer après.",
    "{TEMP}. Tes doigts vont te détester. Gants. Maintenant.",
    "Gel au sol. Marche comme un pingouin, dignité optionnelle.",
    "{TEMP}. Le frigo est devenu un sauna en comparaison.",
    "Froid polaire. La couette a légalement le droit de te garder.",
    "Ça pique dehors. Bonnet. Oui, même si ça te décoiffe.",
    "{TEMP}. Sortir sans écharpe, c'est de la provocation.",
    "Gel généralisé. Dégivre le pare-brise, pas à l'eau chaude, malin.",
    "Froid de gueux. Le radiateur est ton nouveau meilleur pote.",
    "On gèle. Même les pigeons ont renoncé, c'est dire.",
    "{TEMP}. Tes oreilles vont tomber. Façon de parler. Quoique.",
    "Température de congélo. Double chaussettes autorisées, zéro jugement.",
  ],
  night: [
    "Il fait nuit. Pose ce téléphone et dors, bon sang.",
    "Tout le monde dort. Toi, tu regardes la météo. Va te coucher.",
    "La nuit porte conseil. Le premier : lâche cet écran.",
    "Belle nuit. Compte les étoiles, pas tes soucis.",
    "Il est tard. La météo de demain sera encore là demain. Toi, dors.",
    "Tu vérifies la pluie à cette heure-ci ? Champion de la procrastination nocturne.",
    "La lune bosse, toi tu devrais débrancher.",
    "Nuit noire. Ferme les yeux, ça marche pareil.",
    "Encore debout ? Ton réveil va se venger, tu le sais.",
    "Le ciel dort presque. Fais-en autant, tête de mule adorable.",
    "Demain commence dans ton lit. Indice : t'y es pas.",
    "Les étoiles brillent. Toi tu scrolles. Une seule des deux activités est poétique.",
  ],
}

// ---------- Codes WMO → état du nuage / groupe de répliques / libellé ----------
function etatNuage(code) {
  if (code === 0) return "radieux"
  if (code <= 3) return "detendu"
  if (code === 45 || code === 48) return "endormi"
  if (code >= 51 && code <= 57) return "ronchon"
  if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) return "blase"
  if ((code >= 71 && code <= 77) || code === 85 || code === 86) return "emmitoufle"
  if (code >= 95) return "flippe"
  return "detendu"
}

function groupeReplique(code) {
  if (code === 0) return "clear"
  if (code <= 3) return "clouds"
  if (code === 45 || code === 48) return "fog"
  if (code >= 51 && code <= 57) return "drizzle"
  if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) return "rain"
  if ((code >= 71 && code <= 77) || code === 85 || code === 86) return "snow"
  if (code >= 95) return "storm"
  return "clouds"
}

// Libellé + icône SVG pour la vue app (le widget, lui, utilise les images du nuage)
function infoWMO(code, isDay) {
  const nuit = isDay === 0
  if (code === 0) return { label: "Ciel clair", icone: nuit ? "lune" : "soleil" }
  if (code === 1) return { label: "Plutôt dégagé", icone: nuit ? "lune" : "soleil" }
  if (code === 2) return { label: "Partiellement nuageux", icone: nuit ? "lune-nuage" : "soleil-nuage" }
  if (code === 3) return { label: "Couvert", icone: "nuage" }
  if (code === 45 || code === 48) return { label: "Brouillard", icone: "brouillard" }
  if (code >= 51 && code <= 57) return { label: "Bruine", icone: "bruine" }
  if (code >= 61 && code <= 67) return { label: "Pluie", icone: "pluie" }
  if (code >= 71 && code <= 77) return { label: "Neige", icone: "neige" }
  if (code >= 80 && code <= 82) return { label: "Averses", icone: "pluie" }
  if (code === 85 || code === 86) return { label: "Averses de neige", icone: "neige" }
  if (code >= 95) return { label: "Orage", icone: "orage" }
  return { label: "Variable", icone: "nuage" }
}

// ---------- Icônes SVG (trait arrondi, currentColor → suivent le thème) ----------
function svgIcone(nom, taille = 24) {
  const NUAGE = '<path d="M7 18h10a4 4 0 0 0 .75-7.93A6 6 0 0 0 6.2 9.4 3.9 3.9 0 0 0 7 18Z"/>'
  const NUAGE_HAUT = '<g transform="translate(0 -2.6)">' + NUAGE + "</g>"
  const ICONES = {
    soleil: '<circle cx="12" cy="12" r="4.2"/><path d="M12 2.8v2.2M12 19v2.2M2.8 12H5M19 12h2.2M5.2 5.2l1.6 1.6M17.2 17.2l1.6 1.6M18.8 5.2l-1.6 1.6M6.8 17.2l-1.6 1.6"/>',
    lune: '<path d="M20.4 13.2A8.5 8.5 0 1 1 10.8 3.6a6.8 6.8 0 0 0 9.6 9.6Z"/>',
    "soleil-nuage": '<circle cx="7.6" cy="6.8" r="2.5"/><path d="M7.6 2.6v1.3M3.4 6.8h1.3M4.6 3.8l.9.9M10.6 3.8l-.9.9"/><path d="M10 19.5h7a3.4 3.4 0 0 0 .64-6.74A5 5 0 0 0 9.3 11.7 3.3 3.3 0 0 0 10 19.5Z"/>',
    "lune-nuage": '<path d="M12.3 7.4A4.3 4.3 0 1 1 7.3 2.4a3.4 3.4 0 0 0 5 5Z"/><path d="M10 19.5h7a3.4 3.4 0 0 0 .64-6.74A5 5 0 0 0 9.3 11.7 3.3 3.3 0 0 0 10 19.5Z"/>',
    nuage: NUAGE,
    brouillard: NUAGE_HAUT + '<path d="M6 19h12M8 21.6h8"/>',
    bruine: NUAGE_HAUT + '<path d="M9.5 18.6v2M13 19.6v2M16.5 18.6v2"/>',
    pluie: NUAGE_HAUT + '<path d="M9.8 18.6l-1.1 2.8M13.4 18.6l-1.1 2.8M17 18.6l-1.1 2.8"/>',
    neige: NUAGE_HAUT + '<g fill="currentColor" stroke="none"><circle cx="9.5" cy="19.6" r="1"/><circle cx="13.2" cy="21" r="1"/><circle cx="16.6" cy="19.3" r="1"/></g>',
    orage: NUAGE_HAUT + '<path d="M13.2 16.8l-2.6 3.7h2l-1.1 3.3 3.7-4.6h-2.1l1.5-2.4Z" fill="currentColor" stroke="none"/>',
    goutte: '<path d="M12 4.8C9.2 9 7.4 11.5 7.4 14a4.6 4.6 0 0 0 9.2 0C16.6 11.5 14.8 9 12 4.8Z"/>',
    vent: '<path d="M3.5 8.8h9.7a2.5 2.5 0 1 0-2.5-2.5M3.5 13.6h13.6a2.7 2.7 0 1 1-2.7 2.7"/>',
    lever: '<path d="M4 18h16M8 18a4 4 0 0 1 8 0M12 11.6V7.2M10 9l2-2 2 2"/>',
    coucher: '<path d="M4 18h16M8 18a4 4 0 0 1 8 0M12 7.2v4.4M10 9.8l2 2 2-2"/>',
  }
  return `<svg viewBox="0 0 24 24" width="${taille}" height="${taille}" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${ICONES[nom] || NUAGE}</svg>`
}

// ---------- Choix de la réplique ----------
function remplaceTemp(r, data) {
  return r.replace("{TEMP}", `${Math.round(data.current.temperature_2m)} °C`)
}

// Pool actif selon météo/température, avec les vannes de nuit en bonus la nuit
function poolRepliques(data) {
  const c = data.current
  let pool
  if (c.temperature_2m >= SEUIL_CANICULE) pool = REPLIQUES.heat
  else if (c.temperature_2m <= SEUIL_GEL) pool = REPLIQUES.cold
  else pool = REPLIQUES[groupeReplique(c.weather_code)]
  if (c.is_day === 0) pool = pool.concat(REPLIQUES.night)
  return pool
}

// Tirage sans répétition sur la journée : l'historique des vannes servies est
// gardé dans le trousseau et repart à zéro chaque jour (ou si le pool est épuisé).
function choisitReplique(data) {
  const pool = poolRepliques(data)
  const CLE = "le-nuage:vannes-du-jour"
  const aujourdHui = new Date().toDateString()
  let hist = { jour: aujourdHui, servies: [] }
  try {
    if (Keychain.contains(CLE)) {
      const lu = JSON.parse(Keychain.get(CLE))
      if (lu && lu.jour === aujourdHui && Array.isArray(lu.servies)) hist = lu
    }
  } catch (e) { /* historique illisible → on repart à neuf */ }

  let dispo = pool.filter(r => !hist.servies.includes(r))
  if (dispo.length === 0) {
    // tout le pool a été servi aujourd'hui → on relance un cycle pour ce pool
    hist.servies = hist.servies.filter(r => !pool.includes(r))
    dispo = pool
  }
  const r = dispo[Math.floor(Math.random() * dispo.length)]
  hist.servies.push(r)
  if (hist.servies.length > 150) hist.servies = hist.servies.slice(-150)
  Keychain.set(CLE, JSON.stringify(hist))
  return remplaceTemp(r, data)
}

// ---------- Pluie dans l'heure (minutely_15) ----------
// Retourne { pluie: true, dans: <minutes> } / { pluie: false } / null si indispo
function pluieDansLHeure(data) {
  const m = data.minutely_15
  if (!m || !m.time || !m.precipitation) return null
  const now = Date.now()
  // premier créneau de 15 min pas encore terminé
  const debut = m.time.findIndex(t => new Date(t).getTime() + 15 * 60000 > now)
  if (debut < 0) return null
  const fin = Math.min(debut + 4, m.precipitation.length)
  for (let i = debut; i < fin; i++) {
    if (m.precipitation[i] > 0) {
      const dans = Math.max(0, Math.round((new Date(m.time[i]).getTime() - now) / 60000))
      return { pluie: true, dans }
    }
  }
  return { pluie: false }
}

// ---------- Localisation : GPS, repli Schiltigheim ----------
async function getCoords() {
  try {
    Location.setAccuracyToKilometer() // suffisant pour la météo, plus rapide
    const loc = await Location.current()
    return { latitude: loc.latitude, longitude: loc.longitude }
  } catch (e) {
    return REPLI
  }
}

// ---------- Données Open-Meteo (un seul appel) ----------
async function getMeteo(coords) {
  const params = [
    `latitude=${coords.latitude.toFixed(4)}`,
    `longitude=${coords.longitude.toFixed(4)}`,
    "current=temperature_2m,apparent_temperature,weather_code,is_day,precipitation,wind_speed_10m",
    "minutely_15=precipitation",
    "daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max,sunrise,sunset",
    "hourly=temperature_2m,weather_code,precipitation_probability",
    "forecast_days=7",
    "timezone=auto",
  ].join("&")
  const req = new Request(`https://api.open-meteo.com/v1/forecast?${params}`)
  return await req.loadJSON()
}

// ============================================================
// Nuage provisoire dessiné en code (en attendant les PNG)
// ============================================================
function dessineNuage(etat, nuit) {
  const W = 300, H = 230
  const ctx = new DrawContext()
  ctx.size = new Size(W, H)
  ctx.opaque = false
  ctx.respectScreenScale = true

  const corps = new Color(PALETTE.corps)
  const ombre = new Color(PALETTE.ombre)
  const joues = new Color(PALETTE.joues)
  const encre = new Color(PALETTE.encre)

  // ombre au sol
  ctx.setFillColor(ombre)
  ctx.fillEllipse(new Rect(72, 198, 156, 20))

  // corps : quatre bosses + base arrondie
  ctx.setFillColor(corps)
  ctx.fillEllipse(new Rect(38, 92, 100, 100))
  ctx.fillEllipse(new Rect(162, 92, 100, 100))
  ctx.fillEllipse(new Rect(72, 46, 112, 112))
  ctx.fillEllipse(new Rect(132, 64, 100, 100))
  const base = new Path()
  base.addRoundedRect(new Rect(58, 118, 184, 74), 37, 37)
  ctx.addPath(base)
  ctx.fillPath()

  // joues
  ctx.setFillColor(joues)
  ctx.fillEllipse(new Rect(92, 146, 26, 15))
  ctx.fillEllipse(new Rect(182, 146, 26, 15))

  // --- petits helpers de dessin ---
  const trait = (p) => {
    ctx.setStrokeColor(encre)
    ctx.setLineWidth(5)
    ctx.addPath(p)
    ctx.strokePath()
  }
  const oeilOuvert = (x, y, r = 6) => {
    ctx.setFillColor(encre)
    ctx.fillEllipse(new Rect(x - r, y - r, r * 2, r * 2))
  }
  const oeilMiClos = (x, y) => {
    const p = new Path()
    p.move(new Point(x - 8, y))
    p.addLine(new Point(x + 8, y))
    trait(p)
  }
  const oeilFerme = (x, y) => {
    const p = new Path()
    p.move(new Point(x - 8, y - 2))
    p.addQuadCurve(new Point(x + 8, y - 2), new Point(x, y + 6))
    trait(p)
  }
  const sourcil = (x, y, pente) => {
    const p = new Path()
    p.move(new Point(x - 8, y + (pente > 0 ? 4 : 0)))
    p.addLine(new Point(x + 8, y + (pente > 0 ? 0 : 4)))
    trait(p)
  }
  const sourire = (profondeur = 12, largeur = 13) => {
    const p = new Path()
    p.move(new Point(150 - largeur, 150))
    p.addQuadCurve(new Point(150 + largeur, 150), new Point(150, 150 + profondeur))
    trait(p)
  }
  const moue = () => {
    const p = new Path()
    p.move(new Point(138, 156))
    p.addQuadCurve(new Point(162, 156), new Point(150, 147))
    trait(p)
  }
  const plat = () => {
    const p = new Path()
    p.move(new Point(139, 153))
    p.addLine(new Point(161, 153))
    trait(p)
  }
  const boucheO = (r = 7) => {
    ctx.setFillColor(encre)
    ctx.fillEllipse(new Rect(150 - r, 152 - r + 4, r * 2, r * 2))
  }

  const yG = 128, xG = 118, xD = 182

  // --- expressions par état ---
  if (etat === "radieux") {
    nuit ? (oeilMiClos(xG, yG), oeilMiClos(xD, yG)) : (oeilOuvert(xG, yG, 7), oeilOuvert(xD, yG, 7))
    sourire(16, 15)
  } else if (etat === "detendu") {
    nuit ? (oeilMiClos(xG, yG), oeilMiClos(xD, yG)) : (oeilOuvert(xG, yG), oeilOuvert(xD, yG))
    // sourire en coin
    const p = new Path()
    p.move(new Point(140, 152))
    p.addQuadCurve(new Point(164, 148), new Point(154, 158))
    trait(p)
  } else if (etat === "endormi") {
    oeilFerme(xG, yG); oeilFerme(xD, yG)
    plat()
    ctx.setTextColor(encre)
    ctx.setFont(Font.boldRoundedSystemFont(22))
    ctx.drawText("z", new Point(232, 52))
    ctx.setFont(Font.boldRoundedSystemFont(15))
    ctx.drawText("z", new Point(252, 38))
  } else if (etat === "ronchon") {
    sourcil(xG, yG - 16, -1); sourcil(xD, yG - 16, 1)
    oeilOuvert(xG, yG); oeilOuvert(xD, yG)
    moue()
  } else if (etat === "blase") {
    oeilMiClos(xG, yG); oeilMiClos(xD, yG)
    plat()
  } else if (etat === "emmitoufle") {
    nuit ? (oeilMiClos(xG, yG), oeilMiClos(xD, yG)) : (oeilOuvert(xG, yG), oeilOuvert(xD, yG))
    boucheO(6)
    // écharpe
    ctx.setFillColor(joues)
    const echarpe = new Path()
    echarpe.addRoundedRect(new Rect(102, 166, 96, 16), 8, 8)
    const pan = new Path()
    pan.addRoundedRect(new Rect(162, 178, 16, 28), 7, 7)
    ctx.addPath(echarpe); ctx.fillPath()
    ctx.addPath(pan); ctx.fillPath()
  } else if (etat === "flippe") {
    sourcil(xG, yG - 20, 1); sourcil(xD, yG - 20, -1)
    oeilOuvert(xG, yG, 8); oeilOuvert(xD, yG, 8)
    boucheO(8)
  }

  return ctx.getImage()
}

// ---------- Image du nuage ----------
// Cascade : 1) iCloud Drive/Scriptable/nuage  2) cache local de l'appareil
// 3) téléchargement depuis ASSETS_URL (puis mise en cache locale)  4) dessin en code
async function imageNuage(etat, nuit) {
  const candidats = nuit ? [`${etat}_nuit.png`, `${etat}.png`] : [`${etat}.png`]

  // 1) dossier iCloud (si l'espace iCloud le permet un jour, il reprend la main)
  try {
    const fm = FileManager.iCloud()
    const dir = fm.joinPath(fm.documentsDirectory(), DOSSIER_ASSETS)
    for (const nom of candidats) {
      const chemin = fm.joinPath(dir, nom)
      if (fm.fileExists(chemin)) {
        if (fm.isFileStoredIniCloud(chemin)) await fm.downloadFileFromiCloud(chemin)
        return fm.readImage(chemin)
      }
    }
  } catch (e) { /* iCloud indisponible → étape suivante */ }

  // 2) cache local (aucun besoin d'iCloud, persistant)
  try {
    const fm = FileManager.local()
    const dir = fm.joinPath(fm.libraryDirectory(), DOSSIER_ASSETS)
    if (!fm.fileExists(dir)) fm.createDirectory(dir, true)
    for (const nom of candidats) {
      const chemin = fm.joinPath(dir, nom)
      if (fm.fileExists(chemin)) return fm.readImage(chemin)
    }
    // 3) pas en cache → téléchargement à la demande puis stockage local
    if (ASSETS_URL) {
      for (const nom of candidats) {
        try {
          const img = await new Request(`${ASSETS_URL}/${nom}`).loadImage()
          fm.writeImage(fm.joinPath(dir, nom), img)
          return img
        } catch (e) { /* fichier absent en ligne → candidat suivant */ }
      }
    }
  } catch (e) { /* on retombe sur le dessin */ }

  // 4) repli : nuage provisoire dessiné en code
  return dessineNuage(etat, nuit)
}

// ============================================================
// MODE WIDGET (taille moyenne)
// ============================================================
async function buildWidget(data) {
  const c = data.current
  const nuit = c.is_day === 0
  // suit le mode sombre iOS ; la nuit force le sombre quoi qu'il arrive
  const encre = nuit
    ? new Color(PALETTE.corps)
    : teinte(PALETTE.encre, PALETTE.corps)

  const w = new ListWidget()
  w.setPadding(14, 16, 12, 16)
  w.refreshAfterDate = new Date(Date.now() + 30 * 60 * 1000)

  const grad = new LinearGradient()
  grad.colors = nuit
    ? [new Color("#2B3A4E"), new Color("#1E2935")]
    : [teinte("#DCEBFA", "#2B3A4E"), teinte("#F7FBFF", "#1E2935")]
  grad.locations = [0, 1]
  w.backgroundGradient = grad

  // --- rangée haute : nuage à gauche, températures à droite ---
  const haut = w.addStack()
  haut.layoutHorizontally()
  haut.centerAlignContent()

  const img = await imageNuage(etatNuage(c.weather_code), nuit)
  const wi = haut.addImage(img)
  wi.imageSize = new Size(92, 70)

  haut.addSpacer(14)

  const infos = haut.addStack()
  infos.layoutVertically()

  const tLabel = infos.addText(infoWMO(c.weather_code, c.is_day).label)
  tLabel.font = Font.mediumRoundedSystemFont(11)
  tLabel.textColor = encre
  tLabel.textOpacity = 0.6
  tLabel.lineLimit = 1

  const tTemp = infos.addText(`${Math.round(c.temperature_2m)}°`)
  tTemp.font = Font.boldRoundedSystemFont(34)
  tTemp.textColor = encre

  // min/max + ressenti sur une seule ligne : aucun risque d'écrêtage vertical
  const tMinMax = infos.addText(`↓ ${Math.round(data.daily.temperature_2m_min[0])}°  ↑ ${Math.round(data.daily.temperature_2m_max[0])}°  ·  Ress. ${Math.round(c.apparent_temperature)}°`)
  tMinMax.font = Font.mediumRoundedSystemFont(12)
  tMinMax.textColor = encre
  tMinMax.textOpacity = 0.75
  tMinMax.lineLimit = 1
  tMinMax.minimumScaleFactor = 0.8

  const pluie = pluieDansLHeure(data)
  if (pluie) {
    infos.addSpacer(3)
    const txt = pluie.pluie
      ? (pluie.dans <= 1 ? "☔ Pluie imminente" : `☔ Pluie dans ~${pluie.dans} min`)
      : "Pas de pluie dans l'heure"
    const tPluie = infos.addText(txt)
    tPluie.font = Font.mediumRoundedSystemFont(11)
    tPluie.textColor = encre
    tPluie.textOpacity = pluie.pluie ? 1 : 0.6
  }

  haut.addSpacer()

  // --- la vanne du jour ---
  w.addSpacer(8)
  const tQuip = w.addText(`« ${choisitReplique(data)} »`)
  tQuip.font = Font.italicSystemFont(12.5)
  tQuip.textColor = encre
  tQuip.textOpacity = 0.9
  tQuip.lineLimit = 2
  tQuip.minimumScaleFactor = 0.75

  return w
}

// Widget de secours si l'API ou le réseau est en rade
function widgetErreur() {
  const w = new ListWidget()
  w.setPadding(14, 16, 12, 16)
  w.backgroundColor = teinte("#D8E5F2", "#2B3A4E")
  const haut = w.addStack()
  haut.layoutHorizontally()
  haut.centerAlignContent()
  const wi = haut.addImage(dessineNuage("flippe", false))
  wi.imageSize = new Size(92, 70)
  haut.addSpacer(14)
  const t = haut.addText("Le wifi des nuages est en rade. Réessaie dans un moment.")
  t.font = Font.italicSystemFont(13)
  t.textColor = teinte(PALETTE.encre, PALETTE.corps)
  w.refreshAfterDate = new Date(Date.now() + 10 * 60 * 1000)
  return w
}

// ============================================================
// MODE APP — WebView HTML (semaine, ressenti, détail horaire)
// Thème : suit THEME/prefers-color-scheme ; la nuit force le sombre.
// ============================================================
async function buildHTML(data) {
  const c = data.current
  const nuit = c.is_day === 0
  const info = infoWMO(c.weather_code, c.is_day)
  const replique = choisitReplique(data)
  const JOURS = ["dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam."]

  // le nuage en vedette, embarqué en base64 (PNG du cache, sinon dessin)
  const imgNuage = await imageNuage(etatNuage(c.weather_code), nuit)
  const nuageB64 = Data.fromPNG(imgNuage).toBase64String()
  // pool de vannes embarqué : taper sur le nuage en sert une nouvelle
  const vannes = poolRepliques(data).map(r => remplaceTemp(r, data))

  // pastilles d'infos du moment
  const vent = Math.round(c.wind_speed_10m || 0)
  const lever = (data.daily.sunrise && data.daily.sunrise[0] || "").slice(11, 16)
  const coucher = (data.daily.sunset && data.daily.sunset[0] || "").slice(11, 16)

  // prochaines 12 heures, proba de pluie affichée à partir de 30 %
  const maintenant = Date.now()
  const depart = data.hourly.time.findIndex(t => new Date(t).getTime() > maintenant - 3600 * 1000)
  let heuresHTML = ""
  if (depart >= 0) {
    for (let i = depart; i < Math.min(depart + 12, data.hourly.time.length); i++) {
      const d = new Date(data.hourly.time[i])
      const h = i === depart ? "Maint." : `${d.getHours()} h`
      const ico = infoWMO(data.hourly.weather_code[i], (d.getHours() >= 21 || d.getHours() <= 6) ? 0 : 1).icone
      const proba = data.hourly.precipitation_probability ? data.hourly.precipitation_probability[i] : 0
      const probaHTML = proba >= 30 ? `<div class="h-pluie">${proba} %</div>` : `<div class="h-pluie">&nbsp;</div>`
      heuresHTML += `<div class="heure"><div class="h-label">${h}</div><div class="h-ico">${svgIcone(ico, 26)}</div><div class="h-temp">${Math.round(data.hourly.temperature_2m[i])}°</div>${probaHTML}</div>`
    }
  }

  // 7 jours, avec la proba de pluie max du jour à partir de 20 %
  let joursHTML = ""
  for (let i = 0; i < data.daily.time.length; i++) {
    const d = new Date(data.daily.time[i])
    const nom = i === 0 ? "Aujourd'hui" : JOURS[d.getDay()] + " " + d.getDate()
    const ico = infoWMO(data.daily.weather_code[i], 1).icone
    const proba = data.daily.precipitation_probability_max ? data.daily.precipitation_probability_max[i] : null
    const probaHTML = proba !== null && proba >= 20
      ? `<span class="j-pluie">${svgIcone("goutte", 12)} ${proba} %</span>`
      : `<span class="j-pluie"></span>`
    joursHTML += `<div class="jour"><span class="j-nom">${nom}</span>${probaHTML}<span class="j-ico">${svgIcone(ico, 22)}</span><span class="j-temps"><span class="j-min">${Math.round(data.daily.temperature_2m_min[i])}°</span> / <span class="j-max">${Math.round(data.daily.temperature_2m_max[i])}°</span></span></div>`
  }

  // thème forcé : nuit → sombre ; sinon selon THEME
  const VARS_SOMBRES = `--fond-haut:#2B3A4E; --fond-bas:#1E2935; --texte:${PALETTE.corps}; --carte:rgba(234,241,249,.06); --bulle:rgba(234,241,249,.08); --separateur:rgba(234,241,249,.12);`
  const VARS_CLAIRES = `--fond-haut:#DCEBFA; --fond-bas:#F7FBFF; --texte:${PALETTE.encre}; --carte:rgba(255,255,255,.7); --bulle:rgba(255,255,255,.65); --separateur:${PALETTE.ombre};`
  let themeForce = ""
  if (nuit || THEME === "sombre") themeForce = `:root { ${VARS_SOMBRES} }`
  else if (THEME === "clair") themeForce = `:root { ${VARS_CLAIRES} }`

  return `<!DOCTYPE html>
<html lang="fr"><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<style>
  :root { ${VARS_CLAIRES} }
  @media (prefers-color-scheme: dark) { :root { ${VARS_SOMBRES} } }
  ${themeForce}
  * { box-sizing: border-box; margin: 0; }
  /* fond : couleur unie sur <html> (c'est elle que WKWebView peint au rebond)
     + calque fixe surdimensionné pour le dégradé (background-attachment: fixed est cassé sur iOS) */
  html { background-color: var(--fond-bas); min-height: 100%; }
  body {
    font-family: -apple-system, "SF Pro Rounded", sans-serif;
    background: transparent;
    color: var(--texte);
    min-height: 100vh;
    padding: calc(34px + env(safe-area-inset-top)) 18px calc(28px + env(safe-area-inset-bottom));
  }
  .fond {
    position: fixed; left: 0; right: 0; top: -100vh; height: 300vh;
    z-index: -1; pointer-events: none;
    background: linear-gradient(var(--fond-haut) 33%, var(--fond-bas) 67%);
  }
  svg { display: inline-block; vertical-align: middle; }
  .header { text-align: center; }
  .nuage-scene { animation: flotte 4.5s ease-in-out infinite alternate; }
  .nuage {
    width: 190px; max-width: 60vw; cursor: pointer;
    -webkit-tap-highlight-color: transparent; user-select: none;
  }
  .nuage.fretille { animation: fretille .55s ease; }
  @keyframes flotte { from { transform: translateY(0) } to { transform: translateY(-9px) } }
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
</style></head>
<body>
  <div class="fond"></div>
  <div class="header">
    <div class="nuage-scene"><img id="nuage" class="nuage" src="data:image/png;base64,${nuageB64}" alt="${info.label}"></div>
    <div class="temp">${Math.round(c.temperature_2m)}°</div>
    <div class="label">${info.label}</div>
    <div class="ressenti">Ressenti ${Math.round(c.apparent_temperature)}°</div>
    <div class="infos">
      <span class="chip">${svgIcone("vent", 16)} ${vent} km/h</span>
      <span class="chip">${svgIcone("lever", 16)} ${lever}</span>
      <span class="chip">${svgIcone("coucher", 16)} ${coucher}</span>
    </div>
  </div>
  <div class="vanne" id="vanne">« ${replique} »</div>
  <div class="card"><h2>Prochaines heures</h2><div class="heures">${heuresHTML}</div></div>
  <div class="card"><h2>La semaine</h2>${joursHTML}</div>
  <script>
    // taper le nuage : il frétille et sert une nouvelle vanne
    const VANNES = ${JSON.stringify(vannes)};
    const elNuage = document.getElementById("nuage");
    const elVanne = document.getElementById("vanne");
    elNuage.addEventListener("click", () => {
      let v;
      do { v = VANNES[Math.floor(Math.random() * VANNES.length)]; }
      while (VANNES.length > 1 && "« " + v + " »" === elVanne.textContent);
      elVanne.textContent = "« " + v + " »";
      elNuage.classList.remove("fretille");
      void elNuage.offsetWidth; // relance l'animation
      elNuage.classList.add("fretille");
    });
  </script>
</body></html>`
}

// ============================================================
// Point d'entrée
// ============================================================
const coords = await getCoords()
let data = null
try { data = await getMeteo(coords) } catch (e) { /* réseau KO → widget d'erreur */ }

if (config.runsInWidget) {
  Script.setWidget(data ? await buildWidget(data) : widgetErreur())
} else {
  if (data) {
    const wv = new WebView()
    await wv.loadHTML(await buildHTML(data))
    await wv.present(true) // plein écran
  } else {
    const a = new Alert()
    a.title = "Le Nuage"
    a.message = "Impossible de joindre la météo. Vérifie ta connexion et réessaie."
    a.addAction("OK")
    await a.present()
  }
}
Script.complete()
