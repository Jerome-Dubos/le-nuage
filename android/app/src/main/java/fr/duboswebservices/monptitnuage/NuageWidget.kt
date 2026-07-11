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
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Locale

// Widget écran d'accueil redimensionnable : le nuage + la météo, suivant le ton. La
// disposition s'adapte à la taille — compact (2×2), moyen (large et court), grand (4×3+).
//
// Anti-« écran de chargement » + anti-clignotement (cf. historique) : on garde en cache
// (SharedPreferences) les dernières valeurs affichées et on PEINT le widget instantanément
// depuis ce cache à chaque onUpdate (synchrone, sans réseau). La météo réseau n'est
// rafraîchie qu'au besoin (cache périmé), via WorkManager.
class NuageWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        WidgetRender.peintDepuisCache(ctx, mgr, ids)
        val prefs = ctx.getSharedPreferences("nuage", Context.MODE_PRIVATE)
        if (System.currentTimeMillis() - prefs.getLong("widget-maj", 0L) > FRAICHEUR_MS)
            WorkManager.getInstance(ctx).enqueue(OneTimeWorkRequestBuilder<WidgetWorker>().build())
    }

    // Redimensionnement (utile sous Android 12 où le size-mapping natif n'existe pas) :
    // on repeint la case avec la disposition adaptée à sa nouvelle taille.
    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle) {
        WidgetRender.peintDepuisCache(ctx, mgr, intArrayOf(id))
    }

    companion object {
        const val FRAICHEUR_MS = 20 * 60_000L

        fun rafraichit(ctx: Context, force: Boolean = false) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, NuageWidget::class.java))
            if (ids.isEmpty()) return
            WidgetRender.peintDepuisCache(ctx, mgr, ids)
            WorkManager.getInstance(ctx).enqueue(OneTimeWorkRequestBuilder<WidgetWorker>().build())
        }
    }
}

// Construit et applique le RemoteViews. Partagé entre peinture instantanée (cache) et worker.
object WidgetRender {
    // Valeurs affichées par le widget (issues du cache ou du dernier relevé).
    data class Donnees(
        val doux: Boolean, val resName: String, val temp: String,
        val label: String, val vanne: String, val minmax: String, val minmaxRessenti: String
    )

    fun peintDepuisCache(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val p = ctx.getSharedPreferences("nuage", Context.MODE_PRIVATE)
        val tonStr = p.getString("ton", "taquin") ?: "taquin"
        val min = p.getString("widget-min", "") ?: ""
        val max = p.getString("widget-max", "") ?: ""
        val ress = p.getString("widget-ressenti", "") ?: ""
        applique(ctx, mgr, ids, Donnees(
            doux = tonStr == "doux",
            resName = p.getString("widget-nuage", "nuage_${tonStr}_radieux") ?: "nuage_${tonStr}_radieux",
            temp = p.getString("widget-temp", "") ?: "",
            label = p.getString("widget-label", "") ?: "",
            vanne = p.getString("widget-vanne", "") ?: "",
            minmax = if (min.isNotEmpty()) "↓ $min°   ↑ $max°" else "",
            minmaxRessenti = if (min.isNotEmpty()) "↓ $min°   ↑ $max°   ·   Ressenti $ress°" else ""
        ))
    }

    fun applique(ctx: Context, mgr: AppWidgetManager, ids: IntArray, d: Donnees) {
        for (id in ids) mgr.updateAppWidget(id, rvPourTaille(ctx, mgr, id, d))
    }

    private fun rvPourTaille(ctx: Context, mgr: AppWidgetManager, id: Int, d: Donnees): RemoteViews {
        // Android 12+ : on fournit les trois dispositions, le launcher choisit selon la taille.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return RemoteViews(mapOf(
                SizeF(110f, 110f) to petit(ctx, d),
                SizeF(210f, 110f) to moyen(ctx, d),
                SizeF(210f, 210f) to grand(ctx, d)
            ))
        }
        // Avant Android 12 : on lit la taille courante et on choisit la disposition.
        val o = mgr.getAppWidgetOptions(id)
        val minW = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
        val maxH = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 110)
        return when {
            minW >= 200 && maxH >= 200 -> grand(ctx, d)
            minW >= 200 -> moyen(ctx, d)
            else -> petit(ctx, d)
        }
    }

    // --- dispositions ---
    private fun petit(ctx: Context, d: Donnees) = base(ctx, R.layout.widget_nuage, d).apply {
        setTextViewText(R.id.widget_temp, d.temp)
        setTextViewText(R.id.widget_label, d.label)
        setTextViewText(R.id.widget_vanne, d.vanne)
        setTextColor(R.id.widget_temp, encre(d.doux))
        setTextColor(R.id.widget_label, encre(d.doux))
        setTextColor(R.id.widget_vanne, encreVanne(d.doux))
    }

    private fun moyen(ctx: Context, d: Donnees) = base(ctx, R.layout.widget_nuage_moyen, d).apply {
        setTextViewText(R.id.widget_temp, d.temp)
        setTextViewText(R.id.widget_label, d.label)
        setTextViewText(R.id.widget_minmax, d.minmax)
        setTextViewText(R.id.widget_vanne, d.vanne)
        setTextColor(R.id.widget_temp, encre(d.doux))
        setTextColor(R.id.widget_label, encre(d.doux))
        setTextColor(R.id.widget_minmax, encre(d.doux))
        setTextColor(R.id.widget_vanne, encreVanne(d.doux))
    }

    private fun grand(ctx: Context, d: Donnees) = base(ctx, R.layout.widget_nuage_grand, d).apply {
        setTextViewText(R.id.widget_temp, d.temp)
        setTextViewText(R.id.widget_label, d.label)
        setTextViewText(R.id.widget_minmax, d.minmaxRessenti)
        setTextViewText(R.id.widget_vanne, d.vanne)
        setTextColor(R.id.widget_temp, encre(d.doux))
        setTextColor(R.id.widget_label, encre(d.doux))
        setTextColor(R.id.widget_minmax, encre(d.doux))
        setTextColor(R.id.widget_vanne, encreVanne(d.doux))
    }

    // Commun aux trois : fond selon le ton, image du nuage, ouverture de l'app au tap.
    private fun base(ctx: Context, layout: Int, d: Donnees): RemoteViews {
        val rv = RemoteViews(ctx.packageName, layout)
        rv.setInt(R.id.widget_root, "setBackgroundResource",
            if (d.doux) R.drawable.widget_bg_doux else R.drawable.widget_bg)
        val resId = ctx.resources.getIdentifier(d.resName, "drawable", ctx.packageName)
        if (resId != 0) rv.setImageViewResource(R.id.widget_nuage, resId)
        rv.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE))
        return rv
    }

    private fun encre(doux: Boolean) = if (doux) Color.parseColor("#5A4640") else Color.parseColor("#33414F")
    private fun encreVanne(doux: Boolean) = if (doux) Color.parseColor("#7A6459") else Color.parseColor("#4A5A6A")
}

class WidgetWorker(private val ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, NuageWidget::class.java))
        if (ids.isEmpty()) return Result.success()

        val coords = derniereePosition() ?: Coordonnees(48.8566, 2.3522)
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
        val min = meteo.daily.temperature_2m_min.firstOrNull()?.let { Math.round(it).toInt().toString() } ?: ""
        val max = meteo.daily.temperature_2m_max.firstOrNull()?.let { Math.round(it).toInt().toString() } ?: ""
        val ressenti = Math.round(c.apparent_temperature).toInt().toString()

        prefs.edit()
            .putString("widget-nuage", resName)
            .putString("widget-temp", temp)
            .putString("widget-label", info.label)
            .putString("widget-vanne", "« $vanne »")
            .putString("widget-min", min)
            .putString("widget-max", max)
            .putString("widget-ressenti", ressenti)
            .putLong("widget-maj", System.currentTimeMillis())
            .apply()

        WidgetRender.peintDepuisCache(ctx, mgr, ids)
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
