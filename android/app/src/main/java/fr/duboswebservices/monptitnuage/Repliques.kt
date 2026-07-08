package fr.duboswebservices.monptitnuage

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

// Banque de vannes + tirage sans répétition sur la journée (port de Repliques.swift).
// Le texte vit dans les JSON des assets (un par ton). Historique dans les préférences.
class Repliques(private val ctx: Context) {

    private val seuilCanicule = 32.0
    private val seuilGel = -2.0

    private fun banque(ton: Ton): Map<String, List<String>> {
        return try {
            val txt = ctx.assets.open("${ton.fichierVannes}.json").bufferedReader().use { it.readText() }
            val obj = JSONObject(txt)
            buildMap {
                obj.keys().forEach { k ->
                    val arr = obj.getJSONArray(k)
                    put(k, List(arr.length()) { arr.getString(it) })
                }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // Pool actif : seuils chaleur/froid prioritaires, sinon groupe météo ; + vannes de nuit.
    fun pool(meteo: Meteo, ton: Ton): List<String> {
        val b = banque(ton)
        val c = meteo.current
        var pool = when {
            c.temperature_2m >= seuilCanicule -> b["heat"] ?: emptyList()
            c.temperature_2m <= seuilGel -> b["cold"] ?: emptyList()
            else -> b[Wmo.groupeReplique(c.weather_code)] ?: emptyList()
        }
        if (c.is_day == 0) pool = pool + (b["night"] ?: emptyList())
        return pool
    }

    fun choisit(meteo: Meteo, ton: Ton): String {
        val pool = pool(meteo, ton)
        if (pool.isEmpty()) return ""

        val prefs = ctx.getSharedPreferences("nuage", Context.MODE_PRIVATE)
        val aujourdHui = jourCourant()
        val jourStocke = prefs.getString("vannes-jour", null)
        val servies = if (jourStocke == aujourdHui)
            prefs.getStringSet("vannes-servies", emptySet())!!.toMutableSet()
        else mutableSetOf()

        var dispo = pool.filter { it !in servies }
        if (dispo.isEmpty()) {
            servies.removeAll(pool.toSet())
            dispo = pool
        }
        val r = dispo.random()
        servies.add(r)
        prefs.edit()
            .putString("vannes-jour", aujourdHui)
            .putStringSet("vannes-servies", servies)
            .apply()

        return remplaceTemp(r, meteo)
    }

    fun remplaceTemp(r: String, meteo: Meteo): String =
        r.replace("{TEMP}", "${Math.round(meteo.current.temperature_2m)} °C")

    private fun jourCourant(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
}
