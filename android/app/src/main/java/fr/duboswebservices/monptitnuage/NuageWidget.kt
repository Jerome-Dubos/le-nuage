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

// Widget écran d'accueil (pendant Android du widget iOS) : le nuage + la météo, suivant
// le ton. Rafraîchi périodiquement (updatePeriodMillis) et à l'ajout, via WorkManager.
class NuageWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        WorkManager.getInstance(ctx).enqueue(OneTimeWorkRequestBuilder<WidgetWorker>().build())
    }

    companion object {
        fun rafraichit(ctx: Context) {
            val ids = AppWidgetManager.getInstance(ctx)
                .getAppWidgetIds(ComponentName(ctx, NuageWidget::class.java))
            if (ids.isNotEmpty()) WorkManager.getInstance(ctx).enqueue(OneTimeWorkRequestBuilder<WidgetWorker>().build())
        }
    }
}

class WidgetWorker(private val ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, NuageWidget::class.java))
        if (ids.isEmpty()) return Result.success()

        val coords = derniereePosition() ?: Coordonnees(48.8566, 2.3522)
        val meteo = try { WeatherService.charge(coords) } catch (e: Exception) { return Result.retry() }

        val tonStr = ctx.getSharedPreferences("nuage", Context.MODE_PRIVATE).getString("ton", "taquin") ?: "taquin"
        val ton = Ton.depuis(tonStr)
        val c = meteo.current
        val nuit = c.is_day == 0
        val temp = Math.round(c.temperature_2m).toInt()
        val info = Wmo.info(c.weather_code, c.is_day)
        val etat = Wmo.etatNuage(c.weather_code)
        val nom = nomLieu(coords)
        val vanne = Repliques(ctx).pool(meteo, ton).randomOrNull()?.let { Repliques(ctx).remplaceTemp(it, meteo) } ?: ""

        val resName = "nuage_${tonStr}_${etat}${if (nuit) "_nuit" else ""}"
        val resId = ctx.resources.getIdentifier(resName, "drawable", ctx.packageName)

        val doux = ton == Ton.DOUX
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
            rv.setTextViewText(R.id.widget_temp, "$temp°")
            rv.setTextViewText(R.id.widget_label, info.label)
            rv.setTextViewText(R.id.widget_vanne, "« $vanne »")
            rv.setTextColor(R.id.widget_temp, couleurTexte)
            rv.setTextColor(R.id.widget_label, couleurTexte)
            rv.setTextColor(R.id.widget_vanne, couleurVanne)
            rv.setOnClickPendingIntent(R.id.widget_root, ouvre)
            mgr.updateAppWidget(id, rv)
        }
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

    @Suppress("DEPRECATION")
    private fun nomLieu(coords: Coordonnees): String = try {
        android.location.Geocoder(ctx, Locale.getDefault())
            .getFromLocation(coords.latitude, coords.longitude, 1)
            ?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea } ?: "Ma position"
    } catch (e: Exception) { "Ma position" }
}
