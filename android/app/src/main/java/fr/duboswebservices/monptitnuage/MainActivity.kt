package fr.duboswebservices.monptitnuage

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient

// Jalon 1 du port Android : charge le cœur web partagé (assets, copié du web/ du repo) et
// lui pousse un view-model d'exemple via window.rendre — exactement comme l'iOS. Prochaine
// étape : produire ce JSON depuis de vraies données météo (port Kotlin de HTMLBuilder).
class MainActivity : Activity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val web = WebView(this)
        web.setBackgroundColor(Color.TRANSPARENT)
        web.settings.javaScriptEnabled = true
        web.settings.allowFileAccess = true

        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                view.evaluateJavascript("window.rendre($VM_EXEMPLE)", null)
            }
        }

        web.loadUrl("file:///android_asset/index.html")
        setContentView(web)
    }
}

// View-model d'exemple (placeholder nuage) — sera remplacé par un vrai producteur Kotlin.
private const val VM_EXEMPLE = """
{
  "ton": "taquin", "croquis": false,
  "police": "-apple-system, sans-serif",
  "varsClaires": "--fond-haut:#DCEBFA; --fond-bas:#F7FBFF; --texte:#33414F; --carte:rgba(255,255,255,.55); --bulle:rgba(255,255,255,.5); --separateur:#D8E5F2; --trait:rgba(51,65,79,0); --lisere:rgba(255,255,255,.65);",
  "varsSombres": "--fond-haut:#2B3A4E; --fond-bas:#1E2935; --texte:#EAF1F9; --carte:rgba(234,241,249,.06); --bulle:rgba(234,241,249,.08); --separateur:rgba(234,241,249,.12); --trait:rgba(234,241,249,0); --lisere:rgba(234,241,249,.10);",
  "nuit": false,
  "lieu": "Strasbourg", "estPosition": true,
  "temp": 16, "tempStyle": null, "label": "Partiellement nuageux", "ressenti": 16,
  "nuageSVG": "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 512 400\"><defs><clipPath id=\"corps\"><circle cx=\"152\" cy=\"232\" r=\"82\"/><circle cx=\"218\" cy=\"160\" r=\"92\"/><circle cx=\"316\" cy=\"178\" r=\"82\"/><circle cx=\"360\" cy=\"232\" r=\"82\"/><rect x=\"122\" y=\"196\" width=\"268\" height=\"118\" rx=\"59\"/></clipPath></defs><ellipse cx=\"256\" cy=\"372\" rx=\"132\" ry=\"16\" fill=\"#D8E5F2\" opacity=\"1\"/><g transform=\"translate(256 240) scale(1.055) translate(-256 -240)\"><circle cx=\"152\" cy=\"232\" r=\"82\" fill=\"#D8E5F2\"/><circle cx=\"218\" cy=\"160\" r=\"92\" fill=\"#D8E5F2\"/><circle cx=\"316\" cy=\"178\" r=\"82\" fill=\"#D8E5F2\"/><circle cx=\"360\" cy=\"232\" r=\"82\" fill=\"#D8E5F2\"/><rect x=\"122\" y=\"196\" width=\"268\" height=\"118\" rx=\"59\" fill=\"#D8E5F2\"/></g><circle cx=\"152\" cy=\"232\" r=\"82\" fill=\"#EAF1F9\"/><circle cx=\"218\" cy=\"160\" r=\"92\" fill=\"#EAF1F9\"/><circle cx=\"316\" cy=\"178\" r=\"82\" fill=\"#EAF1F9\"/><circle cx=\"360\" cy=\"232\" r=\"82\" fill=\"#EAF1F9\"/><rect x=\"122\" y=\"196\" width=\"268\" height=\"118\" rx=\"59\" fill=\"#EAF1F9\"/><ellipse cx=\"256\" cy=\"334\" rx=\"212\" ry=\"66\" fill=\"#D8E5F2\" opacity=\"0.5\" clip-path=\"url(#corps)\"/><ellipse cx=\"164\" cy=\"266\" rx=\"21\" ry=\"12\" fill=\"#F4B8BE\"/><ellipse cx=\"348\" cy=\"266\" rx=\"21\" ry=\"12\" fill=\"#F4B8BE\"/><g class=\"yeux\"><circle cx=\"204\" cy=\"232\" r=\"12\" fill=\"#33414F\"/><circle cx=\"200\" cy=\"228\" r=\"4.1\" fill=\"#FFFFFF\"/><circle cx=\"208\" cy=\"236\" r=\"1.9\" fill=\"#FFFFFF\" opacity=\"0.85\"/><path d=\"M 294 230 Q 308 241 322 230\" stroke=\"#33414F\" stroke-width=\"9\" stroke-linecap=\"round\" stroke-linejoin=\"round\" fill=\"none\"/></g><path d=\"M 232 270 Q 258 288 284 264\" stroke=\"#33414F\" stroke-width=\"9\" stroke-linecap=\"round\" stroke-linejoin=\"round\" fill=\"none\"/></svg>",
  "peutCligner": true,
  "effet": "soleil", "deco": "",
  "vanne": "Salut Android. On se sent un peu à l'étroit ici, mais ça va le faire.",
  "vannes": ["Salut Android. On se sent un peu à l'étroit ici, mais ça va le faire.", "Le même nuage que sur iPhone, promis, juré."],
  "chips": [{"ico": "vent", "texte": "21 km/h"}, {"ico": "lever", "texte": "05:54"}, {"ico": "coucher", "texte": "20:34"}],
  "agenda": null,
  "stats": [
    {"ico": "goutte", "val": "79 %", "cat": "Humidité", "qual": "Humide", "couleur": "#4C86C6", "deg": null},
    {"ico": "boussole", "val": "O", "cat": "Vent", "qual": "Ouest", "couleur": "#8B95A1", "deg": 270},
    {"ico": "vent", "val": "29 km/h", "cat": "Rafales", "qual": "Modérées", "couleur": "#C99A1E", "deg": null},
    {"ico": "nuage", "val": "30 %", "cat": "Couverture", "qual": "Partiel", "couleur": "#3FA46A", "deg": null},
    {"ico": "soleil", "val": "8", "cat": "Indice UV", "qual": "Très élevé", "couleur": "#D2544B", "deg": null},
    {"ico": "duree", "val": "14h39", "cat": "Durée du jour", "qual": "", "couleur": "", "deg": null}
  ],
  "heures": [
    {"label": "Maint.", "ico": "soleil-nuage", "temp": 16, "proba": 0},
    {"label": "15 h", "ico": "soleil", "temp": 17, "proba": 0},
    {"label": "16 h", "ico": "pluie", "temp": 15, "proba": 60}
  ],
  "jours": [
    {"nom": "Aujourd'hui", "ico": "soleil-nuage", "min": 12, "max": 18, "proba": null},
    {"nom": "lun. 8", "ico": "pluie", "min": 11, "max": 16, "proba": 70},
    {"nom": "mar. 9", "ico": "soleil", "min": 13, "max": 20, "proba": null}
  ]
}
"""
