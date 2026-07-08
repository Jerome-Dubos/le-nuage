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
  "nuageSVG": "<svg viewBox=\"0 0 512 400\" xmlns=\"http://www.w3.org/2000/svg\"><ellipse cx=\"256\" cy=\"230\" rx=\"150\" ry=\"110\" fill=\"#fff\"/><g class=\"yeux\"><circle cx=\"210\" cy=\"220\" r=\"12\" fill=\"#333\"/><circle cx=\"302\" cy=\"220\" r=\"12\" fill=\"#333\"/></g><path d=\"M230 270 q26 20 52 0\" stroke=\"#333\" stroke-width=\"6\" fill=\"none\" stroke-linecap=\"round\"/><circle cx=\"180\" cy=\"255\" r=\"16\" fill=\"#f7b7c2\" opacity=\".7\"/><circle cx=\"332\" cy=\"255\" r=\"16\" fill=\"#f7b7c2\" opacity=\".7\"/></svg>",
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
