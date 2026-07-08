/* Cœur de rendu partagé iOS/Android. Le natif appelle window.rendre(viewModel) avec un
   objet décrivant la vue ; ce script construit tout le DOM et branche les interactions.
   Aucun fetch réseau : le natif fournit déjà le SVG du nuage assemblé et les listes. */

// ---- Bibliothèque d'icônes SVG (port de svgIcone) ------------------------------------
const NUAGE_PATH = '<path d="M7 18h10a4 4 0 0 0 .75-7.93A6 6 0 0 0 6.2 9.4 3.9 3.9 0 0 0 7 18Z"/>';
const NUAGE_HAUT = '<g transform="translate(0 -2.6)">' + NUAGE_PATH + "</g>";
const ICONES = {
  soleil: '<circle cx="12" cy="12" r="4.2"/><path d="M12 2.8v2.2M12 19v2.2M2.8 12H5M19 12h2.2M5.2 5.2l1.6 1.6M17.2 17.2l1.6 1.6M18.8 5.2l-1.6 1.6M6.8 17.2l-1.6 1.6"/>',
  lune: '<path d="M20.4 13.2A8.5 8.5 0 1 1 10.8 3.6a6.8 6.8 0 0 0 9.6 9.6Z"/>',
  "soleil-nuage": '<circle cx="7.6" cy="6.8" r="2.5"/><path d="M7.6 2.6v1.3M3.4 6.8h1.3M4.6 3.8l.9.9M10.6 3.8l-.9.9"/><path d="M10 19.5h7a3.4 3.4 0 0 0 .64-6.74A5 5 0 0 0 9.3 11.7 3.3 3.3 0 0 0 10 19.5Z"/>',
  "lune-nuage": '<path d="M12.3 7.4A4.3 4.3 0 1 1 7.3 2.4a3.4 3.4 0 0 0 5 5Z"/><path d="M10 19.5h7a3.4 3.4 0 0 0 .64-6.74A5 5 0 0 0 9.3 11.7 3.3 3.3 0 0 0 10 19.5Z"/>',
  nuage: NUAGE_PATH,
  brouillard: NUAGE_HAUT + '<path d="M6 19h12M8 21.6h8"/>',
  bruine: NUAGE_HAUT + '<path d="M9.5 18.6v2M13 19.6v2M16.5 18.6v2"/>',
  pluie: NUAGE_HAUT + '<path d="M9.8 18.6l-1.1 2.8M13.4 18.6l-1.1 2.8M17 18.6l-1.1 2.8"/>',
  neige: NUAGE_HAUT + '<g fill="currentColor" stroke="none"><circle cx="9.5" cy="19.6" r="1"/><circle cx="13.2" cy="21" r="1"/><circle cx="16.6" cy="19.3" r="1"/></g>',
  orage: NUAGE_HAUT + '<path d="M13.2 16.8l-2.6 3.7h2l-1.1 3.3 3.7-4.6h-2.1l1.5-2.4Z" fill="currentColor" stroke="none"/>',
  goutte: '<path d="M12 4.8C9.2 9 7.4 11.5 7.4 14a4.6 4.6 0 0 0 9.2 0C16.6 11.5 14.8 9 12 4.8Z"/>',
  vent: '<path d="M3.5 8.8h9.7a2.5 2.5 0 1 0-2.5-2.5M3.5 13.6h13.6a2.7 2.7 0 1 1-2.7 2.7"/>',
  lever: '<path d="M3.5 19h17M8 19a4 4 0 0 1 8 0M5.2 13l1 .9M18.8 13l-1 .9M12 4.4v3.4M9.4 6.4 12 4l2.6 2.4"/>',
  coucher: '<path d="M3.5 19h17M8 19a4 4 0 0 1 8 0M5.2 13l1 .9M18.8 13l-1 .9M12 7.8V4.4M9.4 5.8 12 8.2l2.6-2.4"/>',
  oeil: '<path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z"/><circle cx="12" cy="12" r="3"/>',
  duree: '<circle cx="12" cy="12" r="8"/><path d="M12 8v4l2.6 1.6"/>',
  epingle: '<path d="M12 21.2s6.5-5.9 6.5-10.7a6.5 6.5 0 1 0-13 0c0 4.8 6.5 10.7 6.5 10.7Z"/><circle cx="12" cy="10.2" r="2.4"/>',
  position: '<path d="M20.5 3.5 3 10.8l7.4 2 2 7.4Z"/>',
};
function svgIcone(nom, taille) {
  taille = taille || 24;
  const corps = ICONES[nom] || NUAGE_PATH;
  return '<svg viewBox="0 0 24 24" width="' + taille + '" height="' + taille + '" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' + corps + "</svg>";
}
// Boussole : l'anneau (avec le N) s'oriente selon le cap réel (window.cap), l'aiguille
// pointe la direction du vent. Sans cap, l'anneau reste au nord (léger balancement live).
function boussole(deg) {
  const rot = (deg + 180) % 360;
  return '<svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" stroke-width="1.4">' +
    '<circle cx="12" cy="12" r="9.5" opacity="0.5"/>' +
    '<g class="cap-rot" style="transform-box: view-box; transform-origin: 12px 12px;">' +
    '<circle cx="12" cy="3.4" r="0.9" fill="#D2544B" stroke="none"/>' +
    '<g transform="rotate(' + rot + ' 12 12)"><g class="aiguille">' +
    '<path d="M12 5 L9 13 L12 11.4 L15 13 Z" fill="#D2544B" stroke="none"/>' +
    '<path d="M12 19 L9 13 L12 14.6 L15 13 Z" fill="currentColor" stroke="none" opacity="0.4"/>' +
    "</g></g></g></svg>";
}

// ---- petits utilitaires DOM ----------------------------------------------------------
function esc(s) {
  return String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

// ---- Rendu principal : construit le DOM depuis le view-model -------------------------
window.rendre = function (vm) {
  // thème : variables de couleur + police, selon le ton et le mode
  const police = vm.police || "-apple-system, sans-serif";
  const varsClaires = vm.varsClaires + " --police:" + police + ";";
  const varsSombres = vm.varsSombres + " --police:" + police + ";";
  let theme = document.getElementById("theme");
  if (!theme) { theme = document.createElement("style"); theme.id = "theme"; document.head.appendChild(theme); }
  theme.textContent =
    ":root{" + varsClaires + "}" +
    "@media (prefers-color-scheme: dark){:root{" + varsSombres + "}}" +
    (vm.nuit ? ":root{" + varsSombres + "}" : "");

  document.body.className = vm.croquis ? "croquis" : "premium";

  // chips (vent / lever / coucher)
  const chips = (vm.chips || []).map(function (c) {
    return '<span class="chip">' + svgIcone(c.ico, 16) + " " + esc(c.texte) + "</span>";
  }).join("");

  const lieuHTML = vm.lieu
    ? '<div class="lieu">' + svgIcone(vm.estPosition ? "position" : "epingle", 15) + " " + esc(vm.lieu) + "</div>"
    : "";
  const tempStyle = vm.tempStyle ? ' style="color:' + vm.tempStyle + '"' : "";

  // carte agenda (optionnelle)
  let agendaCard = "";
  if (vm.agenda) {
    const lignes = (vm.agenda.lignes || []).map(function (l) {
      return '<div class="prog-l"><span class="prog-h">' + esc(l.heure) + '</span><span class="prog-t">' + esc(l.titre) + "</span></div>";
    }).join("");
    agendaCard = '<div class="card"><h2>Ton programme</h2>' + lignes + '<div class="prog-note">« ' + esc(vm.agenda.note) + ' »</div></div>';
  }

  // carte détails (stats)
  let detailsCard = "";
  if (vm.stats && vm.stats.length) {
    const statsHTML = vm.stats.map(function (s) {
      const ico = s.ico === "boussole" ? boussole(s.deg || 0) : svgIcone(s.ico, 22);
      const qual = s.qual ? ' · <span style="color:' + s.couleur + '; font-weight:600">' + esc(s.qual) + "</span>" : "";
      return '<div class="stat"><span class="s-ico">' + ico + '</span><span class="s-val">' + esc(s.val) + '</span><span class="s-lbl">' + esc(s.cat) + qual + "</span></div>";
    }).join("");
    const legende =
      '<div class="legende" id="legende" hidden>' +
      "<p><b>Indice UV</b> — 0–2 faible · 3–5 modéré · 6–7 élevé · 8–10 très élevé · 11+ extrême.</p>" +
      "<p><b>Couverture</b> — part du ciel masquée par les nuages.</p>" +
      "<p><b>Vent</b> — direction d'où il souffle ; la flèche indique où il va.</p>" +
      "<p><b>Visibilité</b> — au-delà de 20 km : excellente ; sous 1 km : brouillard.</p></div>";
    detailsCard = '<div class="card"><h2>Détails <button class="aide" id="aide" type="button" aria-label="Légende">?</button></h2><div class="stats">' + statsHTML + "</div>" + legende + "</div>";
  }

  // prochaines heures
  const heuresHTML = (vm.heures || []).map(function (h) {
    const proba = (h.proba != null && h.proba >= 30) ? '<div class="h-pluie">' + h.proba + " %</div>" : '<div class="h-pluie">&nbsp;</div>';
    return '<div class="heure"><div class="h-label">' + esc(h.label) + '</div><div class="h-ico">' + svgIcone(h.ico, 26) + '</div><div class="h-temp">' + h.temp + "°</div>" + proba + "</div>";
  }).join("");

  // semaine (7 jours)
  const joursHTML = (vm.jours || []).map(function (j) {
    const proba = (j.proba != null && j.proba >= 20) ? '<span class="j-pluie">' + svgIcone("goutte", 12) + " " + j.proba + " %</span>" : '<span class="j-pluie"></span>';
    return '<div class="jour"><span class="j-nom">' + esc(j.nom) + "</span>" + proba + '<span class="j-ico">' + svgIcone(j.ico, 22) + '</span><span class="j-temps"><span class="j-min">' + j.min + '°</span> / <span class="j-max">' + j.max + "°</span></span></div>";
  }).join("");

  document.getElementById("app").innerHTML =
    '<div class="fond"></div><div class="fx" id="fx"></div><div class="eclair" id="eclair"></div>' +
    '<div class="header">' + lieuHTML +
    '<div class="nuage-scene"><div id="nuage" class="nuage" role="img" aria-label="' + esc(vm.label) + '">' + vm.nuageSVG + "</div></div>" +
    '<div class="temp"' + tempStyle + ">" + vm.temp + "°</div>" +
    '<div class="label">' + esc(vm.label) + "</div>" +
    '<div class="ressenti">Ressenti ' + vm.ressenti + "°</div>" +
    '<div class="infos">' + chips + "</div></div>" +
    '<div class="vanne" id="vanne">« ' + esc(vm.vanne) + ' »</div>' +
    agendaCard + detailsCard +
    '<div class="card"><h2>Prochaines heures</h2><div class="heures">' + heuresHTML + "</div></div>" +
    '<div class="card"><h2>La semaine</h2>' + joursHTML + "</div>";

  brancheInteractions(vm);
};

// ---- Interactions & animations (port du <script> d'origine) --------------------------
function brancheInteractions(vm) {
  const VANNES = vm.vannes || [];
  const elNuage = document.getElementById("nuage");
  const elVanne = document.getElementById("vanne");
  const reduit = matchMedia("(prefers-reduced-motion: reduce)").matches;

  function nouvelleVanne() {
    let v;
    do { v = VANNES[Math.floor(Math.random() * VANNES.length)]; }
    while (VANNES.length > 1 && "« " + v + " »" === elVanne.textContent);
    elVanne.textContent = "« " + v + " »";
    elNuage.classList.remove("fretille");
    void elNuage.offsetWidth;
    elNuage.classList.add("fretille");
  }
  elNuage.addEventListener("click", function () {
    if (VANNES.length) nouvelleVanne();
    (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.haptique) &&
      window.webkit.messageHandlers.haptique.postMessage("tap");
    window.NuageAndroid && window.NuageAndroid.haptique && window.NuageAndroid.haptique();
  });
  // secousse de l'appareil (déclenchée depuis le natif)
  window.secousse = function () {
    if (VANNES.length) nouvelleVanne();
    elNuage.classList.remove("secoue");
    void elNuage.offsetWidth;
    elNuage.classList.add("secoue");
  };
  // boussole : oriente l'anneau selon le cap réel du téléphone
  window.cap = function (deg) {
    const capRot = document.querySelector(".cap-rot");
    if (capRot) capRot.style.transform = "rotate(" + (-deg) + "deg)";
  };

  // clignement périodique des yeux (si l'œil affiché est ouvert)
  const yeux = vm.peutCligner ? document.querySelector("#nuage .yeux") : null;
  function clignote() {
    if (yeux) {
      yeux.classList.remove("cligne"); yeux.getBoundingClientRect(); yeux.classList.add("cligne");
      if (Math.random() < 0.22) setTimeout(function () {
        yeux.classList.remove("cligne"); yeux.getBoundingClientRect(); yeux.classList.add("cligne");
      }, 300);
    }
    setTimeout(clignote, 2500 + Math.random() * 4500);
  }
  if (yeux && !reduit) setTimeout(clignote, 1500 + Math.random() * 2500);

  // frétille automatique de temps en temps
  function fretilleAuto() {
    elNuage.classList.remove("fretille");
    void elNuage.offsetWidth;
    elNuage.classList.add("fretille");
    setTimeout(fretilleAuto, 9000 + Math.random() * 12000);
  }
  if (!reduit) setTimeout(fretilleAuto, 8000 + Math.random() * 8000);

  // bouton « ? » : déplie la légende
  const elAide = document.getElementById("aide");
  const elLegende = document.getElementById("legende");
  if (elAide && elLegende) elAide.addEventListener("click", function () { elLegende.hidden = !elLegende.hidden; });

  // ambiance météo + décor festif
  const fx = document.getElementById("fx");
  const rnd = function (min, max) { return min + Math.random() * (max - min); };
  function el(cls, css) {
    const d = document.createElement("div");
    d.className = cls; Object.assign(d.style, css); fx.appendChild(d); return d;
  }
  if (fx && !reduit) {
    const EFFET = vm.effet || "";
    if (EFFET === "pluie" || EFFET === "orage") {
      for (let i = 0; i < 60; i++) el("goutte-fx", { left: rnd(0, 100) + "vw", animationDuration: rnd(0.5, 1.1) + "s", animationDelay: rnd(0, 2) + "s", opacity: rnd(0.4, 0.9) });
    }
    if (EFFET === "neige") {
      for (let i = 0; i < 45; i++) { const t = rnd(3, 7); el("flocon", { left: rnd(0, 100) + "vw", width: t + "px", height: t + "px", "--derive": rnd(-40, 40) + "px", animationDuration: rnd(6, 12) + "s", animationDelay: rnd(0, 8) + "s" }); }
    }
    if (EFFET === "etoiles") {
      for (let i = 0; i < 50; i++) { const t = rnd(1, 3); el("etoile", { left: rnd(0, 100) + "vw", top: rnd(0, 55) + "vh", width: t + "px", height: t + "px", animationDuration: rnd(1.5, 3.5) + "s", animationDelay: rnd(0, 3) + "s" }); }
    }
    if (EFFET === "soleil") { el("halo", {}); }
    if (EFFET === "brume") {
      for (let i = 0; i < 4; i++) el("brume-fx", { top: rnd(10, 80) + "vh", animationDuration: rnd(7, 13) + "s", animationDelay: rnd(0, 4) + "s", animationDirection: i % 2 ? "alternate-reverse" : "alternate" });
    }
    if (EFFET === "orage") {
      const ec = document.getElementById("eclair");
      const flash = function () { ec.classList.remove("flash"); void ec.offsetWidth; ec.classList.add("flash"); setTimeout(flash, rnd(4000, 9000)); };
      setTimeout(flash, rnd(1500, 4000));
    }

    const DECO = vm.deco || "";
    if (DECO === "neige") {
      for (let i = 0; i < 45; i++) { const t = rnd(3, 7); el("flocon", { left: rnd(0, 100) + "vw", width: t + "px", height: t + "px", "--derive": rnd(-40, 40) + "px", animationDuration: rnd(6, 12) + "s", animationDelay: rnd(0, 8) + "s" }); }
    }
    if (DECO === "confettis") {
      const cols = ["#FF6B6B", "#FFD93D", "#6BCB77", "#4D96FF", "#C780FA"];
      for (let i = 0; i < 44; i++) el("confetti", { left: rnd(0, 100) + "vw", background: cols[i % cols.length], borderRadius: Math.random() < .5 ? "2px" : "50%", animationDuration: rnd(2.5, 5) + "s", animationDelay: rnd(0, 4) + "s" });
    }
    if (DECO === "coeurs") {
      for (let i = 0; i < 18; i++) { const h = el("coeur", { left: rnd(0, 100) + "vw", fontSize: rnd(14, 26) + "px", animationDuration: rnd(5, 9) + "s", animationDelay: rnd(0, 6) + "s" }); h.textContent = "💗"; }
    }
  }
}

// Si le natif a déposé le view-model avant le chargement du script, on rend tout de suite.
if (window.__vm) window.rendre(window.__vm);
