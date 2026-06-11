#!/usr/bin/env python3
# Générateur des expressions du Nuage — SVG paramétriques, palette §8 du context.md
# v2 : yeux plus expressifs, détendu espiègle (clin d'œil), ronchon vraiment fâché,
#      brouillard "perdu" (fichier endormi.png conservé), blasé triste avec larmes.
import os

CORPS = "#FBE9EF"
OMBRE = "#F2D4DF"
JOUES = "#F2A0B5"
JOUES_FONCE = "#E183A0"
ENCRE = "#4A3B47"
LARME = "#BFD9F2"

OR = "#F2C879"
OUT_SVG = "/home/claude/nuage-doux/svg"
os.makedirs(OUT_SVG, exist_ok=True)

# --- corps : union de bosses + base arrondie (viewBox 512x400) ---
def formes_corps(fill=None):
    f = f' fill="{fill}"' if fill else ""
    return (
        f'<circle cx="152" cy="232" r="82"{f}/>'
        f'<circle cx="218" cy="160" r="92"{f}/>'
        f'<circle cx="316" cy="178" r="82"{f}/>'
        f'<circle cx="360" cy="232" r="82"{f}/>'
        f'<rect x="122" y="196" width="268" height="118" rx="59"{f}/>'
    )

def base_nuage(nuit=False):
    return (
        f'<ellipse cx="256" cy="372" rx="132" ry="16" fill="{OMBRE}" opacity="{0.55 if nuit else 1}"/>'
        f'<g transform="translate(256 240) scale(1.055) translate(-256 -240)">{formes_corps(OMBRE)}</g>'
        f'{formes_corps(CORPS)}'
        f'<ellipse cx="256" cy="334" rx="212" ry="66" fill="{OMBRE}" opacity="0.5" clip-path="url(#corps)"/>'
        f'<ellipse cx="164" cy="266" rx="21" ry="12" fill="{JOUES}"/>'
        f'<ellipse cx="348" cy="266" rx="21" ry="12" fill="{JOUES}"/>'
    )

def trait(d, w=9, couleur=ENCRE):
    return (f'<path d="{d}" stroke="{couleur}" stroke-width="{w}" '
            f'stroke-linecap="round" stroke-linejoin="round" fill="none"/>')

# --- yeux (centres : gauche 204, droite 308, y 232) ---
def oeil_vivant(x, r=12):
    # pupille + double reflet = regard plus vivant
    return (f'<circle cx="{x}" cy="232" r="{r}" fill="{ENCRE}"/>'
            f'<circle cx="{x - r*0.35:.0f}" cy="{232 - r*0.35:.0f}" r="{r*0.34:.1f}" fill="#FFFFFF"/>'
            f'<circle cx="{x + r*0.32:.0f}" cy="{232 + r*0.36:.0f}" r="{r*0.16:.1f}" fill="#FFFFFF" opacity="0.85"/>')

def yeux_ouverts(r=12):
    return oeil_vivant(204, r) + oeil_vivant(308, r)

def yeux_clin():  # espiègle : un œil ouvert, un clin d'œil
    return oeil_vivant(204, 12) + trait("M 294 230 Q 308 241 322 230")

def yeux_miclos():
    return trait("M 190 232 L 218 232", 10) + trait("M 294 232 L 322 232", 10)

def yeux_tristes():  # coins extérieurs tombants
    return trait("M 190 238 L 218 228", 9) + trait("M 294 228 L 322 238", 9)

def yeux_perdus():  # pupilles qui partent chacune de leur côté
    out = ""
    for x, dx, dy in ((204, -4, 3), (308, 4, -3)):
        out += (f'<circle cx="{x}" cy="232" r="13" fill="#FFFFFF" stroke="{ENCRE}" stroke-width="5"/>'
                f'<circle cx="{x+dx}" cy="{232+dy}" r="5" fill="{ENCRE}"/>')
    return out

def yeux_grands():
    out = ""
    for x in (204, 308):
        out += (f'<circle cx="{x}" cy="232" r="15" fill="#FFFFFF" stroke="{ENCRE}" stroke-width="5"/>'
                f'<circle cx="{x}" cy="234" r="5.5" fill="{ENCRE}"/>')
    return out

def yeux_ronchon():
    # pupilles écrasées par des paupières en colère + sourcils plongeants
    out = oeil_vivant(204, 10) + oeil_vivant(308, 10)
    out += trait("M 186 218 L 224 231", 16, CORPS) + trait("M 288 231 L 326 218", 16, CORPS)  # paupières
    out += trait("M 182 206 L 222 221", 10) + trait("M 290 221 L 330 206", 10)               # sourcils
    return out

# --- bouches (centre 256, ~272) ---
GRAND_SOURIRE = (f'<path d="M 224 262 A 32 32 0 0 0 288 262 Z" fill="{ENCRE}"/>'
                 f'<path d="M 240 270 A 16 11 0 0 0 272 270 Z" fill="{JOUES}"/>')  # langue
SOURIRE_COIN  = trait("M 232 270 Q 258 288 284 264")
PLAT          = trait("M 234 274 L 278 274")
MOUE_FACHEE   = trait("M 226 287 Q 256 263 286 287", 10)
PETIT_O       = f'<circle cx="256" cy="276" r="9" fill="{ENCRE}"/>'
GRAND_O       = f'<ellipse cx="256" cy="278" rx="12" ry="14" fill="{ENCRE}"/>'
TREMBLANT     = trait("M 230 276 Q 243 268 256 276 Q 269 284 282 276", 8)
PERDU_BOUCHE  = trait("M 240 276 Q 248 270 256 276 Q 264 282 272 276", 8)
TRISTE_BOUCHE = trait("M 240 282 Q 256 271 272 282", 9)

SOURCILS_FLIPPE = (trait("M 184 198 Q 204 186 224 196", 8) +
                   trait("M 288 196 Q 308 186 328 198", 8))
SOURCIL_PERDU   = trait("M 290 200 Q 308 191 326 200", 7)

# --- accessoires ---
def zzz(couleur):
    return (trait("M 372 86 L 404 86 L 372 118 L 404 118", 9, couleur) +
            trait("M 418 54 L 442 54 L 418 78 L 442 78", 7, couleur))

def interrogation(couleur):
    return (trait("M 374 98 Q 374 70 396 70 Q 418 70 418 92 Q 418 110 396 114 L 396 126", 10, couleur) +
            f'<circle cx="396" cy="146" r="6.5" fill="{couleur}"/>')

GRR = trait("M 382 138 L 398 154", 6) + trait("M 398 138 L 382 154", 6)  # petite croix d'agacement

PARAPLUIE = (
    '<g transform="rotate(-16 380 140)">'
    + trait("M 380 138 L 380 206 Q 380 222 364 218", 8)
    + f'<path d="M 310 134 Q 380 42 450 134 Q 427 120 404 134 Q 380 120 357 134 Q 333 120 310 134 Z" '
      f'fill="{JOUES}" stroke="{ENCRE}" stroke-width="7" stroke-linejoin="round"/>'
    + trait("M 380 60 L 380 78", 8)
    + f'<circle cx="380" cy="54" r="6" fill="{ENCRE}"/>'
    '</g>'
)

def larme(x, miroir=1):
    return (f'<path d="M {x} 246 Q {x + 11*miroir} 264 {x} 276 Q {x - 11*miroir} 264 {x} 246 Z" '
            f'fill="{LARME}" stroke="{OMBRE}" stroke-width="2.5"/>')

LARMES = larme(184, -1) + larme(328, 1)

ECHARPE = (
    f'<rect x="174" y="296" width="164" height="34" rx="17" fill="{JOUES}"/>'
    f'<rect x="296" y="318" width="34" height="60" rx="16" fill="{JOUES}"/>'
    + trait("M 302 336 L 324 336 M 302 350 L 324 350", 5, JOUES_FONCE)
    + f'<path d="M 188 313 L 324 313" stroke="{JOUES_FONCE}" stroke-width="5" '
      f'stroke-linecap="round" opacity="0.7"/>'
)

GOUTTE = (f'<path d="M 130 124 Q 148 154 130 168 Q 112 154 130 124 Z" fill="{LARME}" '
          f'stroke="{OMBRE}" stroke-width="3"/>')

def etoile(x, y, s, couleur=None):
    couleur = couleur or CORPS
    return (f'<path d="M {x} {y-s} Q {x+s*0.22} {y-s*0.22} {x+s} {y} '
            f'Q {x+s*0.22} {y+s*0.22} {x} {y+s} Q {x-s*0.22} {y+s*0.22} {x-s} {y} '
            f'Q {x-s*0.22} {y-s*0.22} {x} {y-s} Z" fill="{couleur}" opacity="0.95"/>')

ETOILES = etoile(98, 92, 9, OR) + etoile(436, 120, 7, OR)  # 2 étoiles sobres

# --- états : (yeux jour, bouche, sourcils, accessoires fixes, interjection nuit-adaptative) ---
ETATS = {
    "radieux":    (yeux_ouverts(14), GRAND_SOURIRE, "",              "",        None),
    "detendu":    (yeux_clin(),      SOURIRE_COIN,  "",              "",        None),
    "endormi":    (yeux_perdus(),    PERDU_BOUCHE,  SOURCIL_PERDU,   "",        interrogation),  # "perdu"
    "ronchon":    (yeux_ronchon(),   MOUE_FACHEE,   "",              "",        None),
    "blase":      (yeux_tristes(),   TRISTE_BOUCHE, "",              PARAPLUIE + LARMES, None),
    "emmitoufle": (yeux_ouverts(12), PETIT_O,       "",              ECHARPE,   None),
    "flippe":     (yeux_grands(),    GRAND_O,       SOURCILS_FLIPPE, GOUTTE,    None),
}

# états qui gardent leurs yeux la nuit (l'émotion prime sur la somnolence)
GARDENT_LEURS_YEUX = ("endormi", "flippe", "blase", "ronchon")

def svg_nuage(nom, nuit=False):
    yeux, bouche, sourcils, acc, interj = ETATS[nom]
    if nuit:
        if nom not in GARDENT_LEURS_YEUX:
            yeux = yeux_miclos()
        acc += ETOILES
    if interj:
        acc = interj(CORPS if nuit else ENCRE) + acc
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 400">'
        f'<defs><clipPath id="corps">{formes_corps()}</clipPath></defs>'
        f'{base_nuage(nuit)}{sourcils}{yeux}{bouche}{acc}'
        '</svg>'
    )

for nom in ETATS:
    for nuit in (False, True):
        fichier = f"{nom}_nuit.svg" if nuit else f"{nom}.svg"
        with open(os.path.join(OUT_SVG, fichier), "w") as f:
            f.write(svg_nuage(nom, nuit))

print(f"{len(ETATS) * 2} SVG doux épurés générés")
