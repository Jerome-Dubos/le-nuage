package fr.duboswebservices.monptitnuage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Un lieu enregistré (en plus de la position GPS).
data class Lieu(val nom: String, val lat: Double, val lon: Double)

// Résultat de recherche géocodée (ville + région + pays pour lever les ambiguïtés).
data class LieuTrouve(val nom: String, val lat: Double, val lon: Double, val sousTitre: String)

object Lieux {
    fun charge(ctx: Context): List<Lieu> {
        val s = prefs(ctx).getString("lieux", null) ?: return emptyList()
        return try {
            val arr = JSONArray(s)
            List(arr.length()) {
                val o = arr.getJSONObject(it)
                Lieu(o.getString("nom"), o.getDouble("lat"), o.getDouble("lon"))
            }
        } catch (e: Exception) { emptyList() }
    }

    fun sauve(ctx: Context, liste: List<Lieu>) {
        val arr = JSONArray()
        liste.forEach { arr.put(JSONObject().put("nom", it.nom).put("lat", it.lat).put("lon", it.lon)) }
        prefs(ctx).edit().putString("lieux", arr.toString()).apply()
    }

    // Recherche géocodée (Open-Meteo, sans clé). Bloquant → appeler hors UI.
    fun recherche(nom: String): List<LieuTrouve> {
        val q = nom.trim()
        if (q.length < 2) return emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(q, "UTF-8")}&count=10&language=fr&format=json"
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 12000; conn.readTimeout = 12000
            val txt = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val results = JSONObject(txt).optJSONArray("results") ?: return emptyList()
            List(results.length()) {
                val o = results.getJSONObject(it)
                val sous = listOfNotNull(o.optString("admin1").ifBlank { null }, o.optString("country").ifBlank { null })
                    .joinToString(", ")
                LieuTrouve(o.getString("name"), o.getDouble("latitude"), o.getDouble("longitude"), sous)
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("nuage", Context.MODE_PRIVATE)
}
