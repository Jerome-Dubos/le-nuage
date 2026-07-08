package fr.duboswebservices.monptitnuage

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.VibrationEffect
import android.os.Vibrator
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

// Une page météo (un lieu) : sa propre WebView chargeant le cœur web partagé, sa propre
// météo. Réutilisée pour la position GPS comme pour chaque lieu enregistré (ViewPager).
@SuppressLint("ViewConstructor", "SetJavaScriptEnabled")
class PageMeteoView(private val act: Activity) : FrameLayout(act) {

    private val web = WebView(act)
    private var pageChargee = false
    private var vmEnAttente: String? = null
    private var erreurEnAttente: String? = null

    private var coords: Coordonnees? = null
    private var nom = "Ma position"
    private var estPosition = false
    private var derniereMeteo: Meteo? = null
    private var dernierSig = ""

    init {
        web.setBackgroundColor(Color.TRANSPARENT)
        web.settings.javaScriptEnabled = true
        web.settings.allowFileAccess = true
        web.addJavascriptInterface(Pont(), "NuageAndroid")
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                pageChargee = true
                vmEnAttente?.let { rendre(it); vmEnAttente = null }
                erreurEnAttente?.let { erreurEnAttente = null; afficheErreur(it) }
            }
        }
        web.loadUrl("file:///android_asset/index.html")
        addView(web, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun configure(coords: Coordonnees, nom: String, estPosition: Boolean) {
        // Évite de recharger si la page est déjà configurée sur ce lieu (recyclage ViewPager).
        if (this.coords == coords && this.nom == nom && derniereMeteo != null) return
        this.coords = coords; this.nom = nom; this.estPosition = estPosition
        charge()
    }

    private fun charge() {
        val c = coords ?: return
        Thread {
            try {
                val meteo = WeatherService.charge(c)
                act.runOnUiThread { derniereMeteo = meteo; construitEtRend() }
            } catch (e: Exception) {
                act.runOnUiThread { afficheErreur("${e.javaClass.simpleName}: ${e.message ?: ""}") }
            }
        }.start()
    }

    // Re-rend (ton/tenues changés) sans re-télécharger.
    fun rafraichitTon() {
        if (derniereMeteo != null && signature() != dernierSig) construitEtRend()
    }

    private fun construitEtRend() {
        val meteo = derniereMeteo ?: return
        val ton = Ton.depuis(act.getSharedPreferences("nuage", Context.MODE_PRIVATE).getString("ton", null))
        dernierSig = signature()
        applique(ViewModelBuilder(act).json(meteo, ton, nom, estPosition))
    }

    private fun signature(): String = Etat.signature(act)

    fun secousse() {
        vibre(28)
        web.evaluateJavascript("window.secousse && window.secousse()", null)
    }

    private fun applique(vm: String) {
        if (pageChargee) rendre(vm) else vmEnAttente = vm
    }

    private fun rendre(vm: String) { web.evaluateJavascript("window.rendre($vm)", null) }

    private fun afficheErreur(detail: String) {
        if (!pageChargee) { erreurEnAttente = detail; return }
        val d = detail.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").take(200)
        web.evaluateJavascript(
            "document.body.style.cssText='display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;background:#1E2935;color:#EAF1F9;font-family:-apple-system,sans-serif;text-align:center;padding:30px';" +
            "document.getElementById('app').innerHTML='<div><div style=\"font-size:56px\">☁️</div>" +
            "<p style=\"line-height:1.5;font-size:16px\">Impossible de joindre la météo.<br>Vérifie ta connexion.</p>" +
            "<p style=\"opacity:.4;font-size:12px;margin-top:18px\">$d</p></div>';", null)
    }

    inner class Pont {
        @JavascriptInterface
        fun haptique() = act.runOnUiThread { vibre(16) }
    }

    private fun vibre(ms: Long) {
        val v = act.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (v.hasVibrator()) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
