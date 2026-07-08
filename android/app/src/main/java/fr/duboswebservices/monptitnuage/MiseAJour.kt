package fr.duboswebservices.monptitnuage

import android.content.Context
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

// Recherche de mise à jour (pendant Android de MiseAJour.swift) : interroge l'API GitHub
// Releases, repère la dernière release taguée android-x.y, compare à la version installée.
// Sur Android la distribution est un simple .apk → on ouvre son lien de téléchargement.
object MiseAJour {
    private const val DEPOT = "Jerome-Dubos/le-nuage"

    data class Info(val version: String, val apkUrl: String)

    fun versionActuelle(ctx: Context): String =
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0"

    // Bloquant → appeler hors UI. null si à jour ou en cas d'échec réseau.
    fun verifie(ctx: Context): Info? {
        return try {
            val conn = URL("https://api.github.com/repos/$DEPOT/releases?per_page=40").openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.connectTimeout = 12000; conn.readTimeout = 12000
            val txt = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val arr = JSONArray(txt)

            var meilleure: Info? = null
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i)
                val tag = r.optString("tag_name")
                if (!tag.startsWith("android-")) continue
                val version = tag.removePrefix("android-")
                val assets = r.optJSONArray("assets") ?: continue
                var apk: String? = null
                for (j in 0 until assets.length()) {
                    val a = assets.getJSONObject(j)
                    if (a.optString("name").endsWith(".apk")) { apk = a.optString("browser_download_url"); break }
                }
                if (apk == null) continue
                if (meilleure == null || estPlusRecente(version, meilleure!!.version)) meilleure = Info(version, apk)
            }

            meilleure?.takeIf { estPlusRecente(it.version, versionActuelle(ctx)) }
        } catch (e: Exception) { null }
    }

    // Compare deux versions "x.y(.z)" composant par composant.
    fun estPlusRecente(a: String, b: String): Boolean {
        val pa = a.split(".").map { it.toIntOrNull() ?: 0 }
        val pb = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }; val y = pb.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
