package fr.duboswebservices.monptitnuage

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Locale

// Widget écran d'accueil (pendant Android du widget iOS) : le nuage + la météo, suivant le ton.
//
// Anti-« écran de chargement » + anti-clignotement : certains launchers (Nothing OS) ne
// conservent pas notre dernier rendu quand l'app est tuée et redéclenchent onUpdate en rafale,
// ré-affichant l'état initial (nuage seul). La parade : on garde en cache (SharedPreferences)
// les dernières valeurs affichées et on PEINT le widget instantanément depuis ce cache à
// chaque onUpdate (synchrone, sans réseau). Le widget montre donc toujours de vraies données,
// même app tuée, et comme le contenu est identique d'un onUpdate à l'autre → pas de flash.
// La météo réseau, elle, n'est rafraîchie qu'au besoin (cache périmé) via WorkManager.
class NuageWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        WidgetRender.peintDepuisCache(ctx, mgr, ids)
        val prefs = ctx.getSharedPreferences("nuage", Context.MODE_PRIVATE)
        if (System.currentTimeMillis() - prefs.getLong("widget-maj", 0L) > FRAICHEUR_MS)
            WorkManager.getInstance(ctx).enqueue(OneTimeWorkRequestBuilder<WidgetWorker>().build())
    }

    companion object {
        // Au-delà de cette ancienneté, on va rechercher la météo à jour (sinon on garde le cache).
        const val FRAICHEUR_MS = 20 * 60_000L

        // Ouverture de l'app / changement de ton : on repeint tout de suite depuis le cache
        // (visuel instantané) puis on relance une vraie mesure météo.
        fun rafraichit(ctx: Context, force: Boolean = false) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, NuageWidget::class.java))
            if (ids.isEmpty()) return
            WidgetRender.peintDepuisCache(ctx, mgr, ids)
            WorkManager.getInstance(ctx).enqueue(OneTimeWorkRequestBuilder<WidgetWorker>().build())
        }
    }
}

// Construction du RemoteViews. Partagé entre la peinture instantanée (cache) et le worker.
object WidgetRender {
    fun peintDepuisCache(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val prefs = ctx.getSharedPreferences("nuage", Context.MODE_PRIVATE)
        val tonStr = prefs.getString("ton", "taquin") ?: "taquin"
        applique(
            ctx, mgr, ids,
            doux = tonStr == "doux",
            resName = prefs.getString("widget-nuage", "nuage_${tonStr}_radieux") ?: "nuage_${tonStr}_radieux",
            temp = prefs.getString("widget-temp", "") ?: "",
            label = prefs.getString("widget-label", "") ?: "",
            vanne = prefs.getString("widget-vanne", "") ?: ""
        )
    }

    fun applique(ctx: Context, mgr: AppWidgetManager, ids: IntArray,
                 doux: Boolean, resName: String, temp: String, label: String, vanne: String) {
        val resId = ctx.resources.getIdentifier(resName, "drawable", ctx.packageName)
        val couleurTexte = if (doux) Color.parseColor("#5A4640") else Color.parseColor("#33414F")
        val couleurVanne = if (doux) Color.parseColor("#7A6459") else Color.parseColor("#4A5A6A")
        val ouvre = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_nuage)
            rv.setInt(R.id.widget_root, "setBackgroundResource", if (doux) R.drawable.widget_bg_doux else R.drawable.widget_bg)
            if (resId != 0) rv.setImageViewResource(R.id.widget_nuage, resId)
            rv.setTextViewText(R.id.widget_temp, temp)
            rv.setTextViewText(R.id.widget_label, label)
            rv.setTextViewText(R.id.widget_vanne, vanne)
            rv.setTextColor(R.id.widget_temp, couleurTexte)
            rv.setTextColor(R.id.widget_label, couleurTexte)
            rv.setTextColor(R.id.widget_vanne, couleurVanne)
            rv.setOnClickPendingIntent(R.id.widget_root, ouvre)
            mgr.updateAppWidget(id, rv)
        }
    }
}

class WidgetWorker(private val ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, NuageWidget::class.java))
        if (ids.isEmpty()) return Result.success()

        val coords = derniereePosition() ?: Coordonnees(48.8566, 2.3522)
        // Si la météo échoue (réseau) : le widget garde son cache, on retente une seule fois.
        val meteo = try { WeatherService.charge(coords) } catch (e: Exception) {
            return if (runAttemptCount < 1) Result.retry() else Result.success()
        }

        val prefs = ctx.getSharedPreferences("nuage", Context.MODE_PRIVATE)
        val tonStr = prefs.getString("ton", "taquin") ?: "taquin"
        val ton = Ton.depuis(tonStr)
        val c = meteo.current
        val nuit = c.is_day == 0
        val temp = "${Math.round(c.temperature_2m).toInt()}°"
        val info = Wmo.info(c.weather_code, c.is_day)
        val etat = Wmo.etatNuage(c.weather_code)
        val vanne = Repliques(ctx).pool(meteo, ton).randomOrNull()?.let { Repliques(ctx).remplaceTemp(it, meteo) } ?: ""
        val resName = "nuage_${tonStr}_${etat}${if (nuit) "_nuit" else ""}"

        // Mémorise pour la peinture instantanée hors-ligne, puis peint.
        prefs.edit()
            .putString("widget-nuage", resName)
            .putString("widget-temp", temp)
            .putString("widget-label", info.label)
            .putString("widget-vanne", "« $vanne »")
            .putLong("widget-maj", System.currentTimeMillis())
            .apply()

        WidgetRender.applique(ctx, mgr, ids, ton == Ton.DOUX, resName, temp, info.label, "« $vanne »")
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun derniereePosition(): Coordonnees? {
        if (ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        return loc?.let { Coordonnees(it.latitude, it.longitude) }
    }
}
