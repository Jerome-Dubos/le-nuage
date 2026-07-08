package fr.duboswebservices.monptitnuage

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

// Notifications locales du Nuage (port de Reveil.swift + AlertePluie.swift). WorkManager :
// réveil quotidien à l'heure choisie, alerte pluie périodique. Tout opt-in, sans serveur.
object Notifs {
    private const val CANAL = "nuage"
    private const val TAG_REVEIL = "reveil"
    private const val TAG_PLUIE = "pluie"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("nuage", Context.MODE_PRIVATE)

    private fun canal(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CANAL) == null) {
            nm.createNotificationChannel(NotificationChannel(CANAL, "Le Nuage", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    @SuppressLint("MissingPermission")
    private fun montre(ctx: Context, id: Int, corps: String) {
        if (ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        canal(ctx)
        val ouvre = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notif = Notification.Builder(ctx, CANAL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Le Nuage ☁️")
            .setContentText(corps)
            .setStyle(Notification.BigTextStyle().bigText(corps))
            .setContentIntent(ouvre)
            .setAutoCancel(true)
            .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, notif)
    }

    // ---- Réveil quotidien ----
    fun programmeReveil(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        if (!prefs(ctx).getBoolean("reveil", false)) { wm.cancelUniqueWork(TAG_REVEIL); return }
        val minutes = prefs(ctx).getInt("reveil-heure", 8 * 60)
        val req = OneTimeWorkRequestBuilder<ReveilWorker>()
            .setInitialDelay(delaiVers(minutes), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniqueWork(TAG_REVEIL, ExistingWorkPolicy.REPLACE, req)
    }

    fun montreReveil(ctx: Context) {
        val ton = Ton.depuis(prefs(ctx).getString("ton", null))
        montre(ctx, 1, phraseReveil(ton))
    }

    private fun delaiVers(minutes: Int): Long {
        val now = Calendar.getInstance()
        val cible = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60); set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (cible.timeInMillis <= now.timeInMillis) cible.add(Calendar.DAY_OF_YEAR, 1)
        return cible.timeInMillis - now.timeInMillis
    }

    private fun phraseReveil(ton: Ton): String {
        val taquin = listOf(
            "Debout. Le ciel est déjà réveillé, lui.",
            "Nouvelle journée. Essaie de ne pas la gâcher.",
            "Il fait un temps… viens voir, j'ai un avis dessus.",
            "Réveil. J'ai une vanne toute fraîche sur ta journée."
        )
        val doux = listOf(
            "Bonjour toi. Passe une journée toute douce.",
            "Un nouveau jour tout neuf. Prends soin de toi.",
            "Coucou ! Viens voir le temps qu'il fait aujourd'hui.",
            "Bien réveillé ? Je te souhaite une belle journée."
        )
        return (if (ton == Ton.DOUX) doux else taquin).random()
    }

    // ---- Alerte pluie (périodique) ----
    fun programmePluie(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        if (!prefs(ctx).getBoolean("alerte-pluie", false)) { wm.cancelUniqueWork(TAG_PLUIE); return }
        val req = PeriodicWorkRequestBuilder<PluieWorker>(1, TimeUnit.HOURS).build()
        wm.enqueueUniquePeriodicWork(TAG_PLUIE, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun verifiePluie(ctx: Context) {
        val p = prefs(ctx)
        if (!p.getBoolean("alerte-pluie", false)) return
        val coords = derniereePosition(ctx) ?: return
        val dans = pluieDansLHeure(coords) ?: return
        val maintenant = System.currentTimeMillis()
        if (maintenant - p.getLong("derniere-alerte-pluie", 0) < 2 * 3600_000L) return // anti-spam 2 h
        p.edit().putLong("derniere-alerte-pluie", maintenant).apply()
        val ton = Ton.depuis(p.getString("ton", null))
        val corps = if (dans <= 5) {
            if (ton == Ton.DOUX) "Il va pleuvoir dans quelques minutes ☔ Prends un parapluie."
            else "Pluie imminente ☔ Prends un parapluie, ou assume d'être trempé."
        } else {
            if (ton == Ton.DOUX) "Pluie dans environ $dans min ☔ Pense à te couvrir."
            else "Pluie dans ~$dans min ☔ Range le linge, sors le parapluie."
        }
        montre(ctx, 2, corps)
    }

    @SuppressLint("MissingPermission")
    private fun derniereePosition(ctx: Context): Coordonnees? {
        if (ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        return loc?.let { Coordonnees(it.latitude, it.longitude) }
    }

    // minutely_15 : minutes avant la pluie (≤ 60), ou null si sec / indispo.
    private fun pluieDansLHeure(coords: Coordonnees): Int? {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${"%.4f".format(Locale.US, coords.latitude)}" +
            "&longitude=${"%.4f".format(Locale.US, coords.longitude)}&minutely_15=precipitation&forecast_days=1&timezone=auto"
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 12000; conn.readTimeout = 12000
            val txt = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val m = JSONObject(txt).getJSONObject("minutely_15")
            val temps = m.getJSONArray("time")
            val precip = m.getJSONArray("precipitation")
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            val now = System.currentTimeMillis()
            val debut = (0 until temps.length()).firstOrNull {
                (fmt.parse(temps.getString(it))?.time ?: 0L) + 15 * 60_000L > now
            } ?: return null
            for (i in debut until minOf(debut + 4, precip.length())) {
                if (!precip.isNull(i) && precip.getDouble(i) > 0) {
                    val t = fmt.parse(temps.getString(i))?.time ?: continue
                    return maxOf(0, ((t - now) / 60_000L).toInt())
                }
            }
            null
        } catch (e: Exception) { null }
    }
}

// Workers WorkManager.
class ReveilWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        Notifs.montreReveil(applicationContext)
        Notifs.programmeReveil(applicationContext) // reprogramme le lendemain
        return Result.success()
    }
}

class PluieWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        Notifs.verifiePluie(applicationContext)
        return Result.success()
    }
}
