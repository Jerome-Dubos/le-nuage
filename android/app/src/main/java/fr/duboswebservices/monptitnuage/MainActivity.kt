package fr.duboswebservices.monptitnuage

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.Locale

// Écran principal : charge le cœur web partagé, récupère la position, appelle Open-Meteo,
// construit le view-model (ViewModelBuilder) et le pousse via window.rendre — comme l'iOS.
class MainActivity : Activity() {

    private lateinit var web: WebView
    private var pageChargee = false
    private var vmEnAttente: String? = null
    private var erreurDetailEnAttente: String? = null

    // Position de repli si la localisation est indisponible (Paris).
    private val repli = Coordonnees(48.8566, 2.3522)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        web = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    pageChargee = true
                    vmEnAttente?.let { rendre(it); vmEnAttente = null }
                    erreurDetailEnAttente?.let { erreurDetailEnAttente = null; afficheErreur(it) }
                }
            }
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(web)

        demarre()
    }

    private fun demarre() {
        val ok = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (ok) chargeMeteo()
        else requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            1
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        chargeMeteo() // accordé → vraie position ; refusé → repli
    }

    private fun chargeMeteo() {
        Thread {
            try {
                val coords = positionActuelle() ?: repli
                val nom = nomLieu(coords)
                val ton = Ton.depuis(getSharedPreferences("nuage", Context.MODE_PRIVATE).getString("ton", null))
                val meteo = WeatherService.charge(coords)
                val vm = ViewModelBuilder(this).json(meteo, ton, nom, true)
                runOnUiThread { applique(vm) }
            } catch (e: Exception) {
                val detail = "${e.javaClass.simpleName}: ${e.message ?: ""}"
                runOnUiThread { afficheErreur(detail) }
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    private fun positionActuelle(): Coordonnees? {
        val ok = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!ok) return null
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val loc = providers.mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
        return loc?.let { Coordonnees(it.latitude, it.longitude) }
    }

    @Suppress("DEPRECATION")
    private fun nomLieu(coords: Coordonnees): String {
        return try {
            val geo = Geocoder(this, Locale.getDefault())
            val res = geo.getFromLocation(coords.latitude, coords.longitude, 1)
            res?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea } ?: "Ma position"
        } catch (e: Exception) { "Ma position" }
    }

    private fun applique(vm: String) {
        if (pageChargee) rendre(vm) else vmEnAttente = vm
    }

    private fun rendre(vm: String) {
        web.evaluateJavascript("window.rendre($vm)", null)
    }

    // Écran d'erreur (météo injoignable) : un nuage + un message + le détail technique
    // (auto-diagnostic : la capture d'écran suffit à identifier la cause).
    private fun afficheErreur(detail: String) {
        if (!pageChargee) { erreurDetailEnAttente = detail; return }
        val d = detail.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").take(200)
        val js = """
          document.body.style.cssText='display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;background:#1E2935;color:#EAF1F9;font-family:-apple-system,sans-serif;text-align:center;padding:30px';
          document.getElementById('app').innerHTML='<div><div style="font-size:56px">☁️</div><p style="line-height:1.5;font-size:16px">Impossible de joindre la météo.<br>Vérifie ta connexion et rouvre l\'app.</p><p style="opacity:.4;font-size:12px;margin-top:18px">$d</p></div>';
        """.trimIndent()
        web.evaluateJavascript(js, null)
    }
}
