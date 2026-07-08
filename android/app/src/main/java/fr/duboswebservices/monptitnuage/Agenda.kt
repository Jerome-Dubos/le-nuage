package fr.duboswebservices.monptitnuage

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// « Petit monde autour du nuage » : agenda commenté + dates spéciales (port de Agenda.swift
// et des anniversaires iOS). Lecture seule, local, opt-in.

data class Evenement(val heure: String, val titre: String)

object Agenda {
    // Évènements d'aujourd'hui encore à venir (max 3). [] si refusé ou rien de prévu.
    fun duJour(ctx: Context): List<Evenement> {
        if (ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED)
            return emptyList()

        val debut = System.currentTimeMillis()
        val fin = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
        }.timeInMillis

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uri, debut); ContentUris.appendId(uri, fin)
        val proj = arrayOf(CalendarContract.Instances.BEGIN, CalendarContract.Instances.TITLE, CalendarContract.Instances.ALL_DAY)

        val fmt = SimpleDateFormat("HH:mm", Locale.FRANCE)
        val liste = mutableListOf<Pair<Long, Evenement>>()
        try {
            ctx.contentResolver.query(uri.build(), proj, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext()) {
                    if (c.getInt(2) == 1) continue // journée entière
                    val t = c.getLong(0)
                    val titre = c.getString(1)?.ifBlank { "Évènement" } ?: "Évènement"
                    liste.add(t to Evenement(fmt.format(java.util.Date(t)), titre))
                }
            }
        } catch (e: Exception) { return emptyList() }

        return liste.sortedBy { it.first }.take(3).map { it.second }
    }

    // Petit mot du nuage sur la journée, selon le ton.
    fun commentaire(events: List<Evenement>, ton: Ton): String {
        val p = events.firstOrNull()
            ?: return if (ton == Ton.TAQUIN)
                "Rien de prévu aujourd'hui. Le vide intersidéral. Ton domaine, quoi."
            else "Journée libre — profite-en pour souffler, tu le mérites."
        val taquin = listOf(
            "${p.titre} à ${p.heure} ? Courage, je te fais de l'ombre.",
            "À ${p.heure}, ${p.titre}. J'espère pour toi que c'est plus fun que la météo.",
            "${p.titre}… encore ? Respire un coup, j'arrive avec un ciel (presque) dégagé."
        )
        val doux = listOf(
            "${p.titre} à ${p.heure} — tu vas gérer, comme toujours. 💙",
            "À ${p.heure}, ${p.titre}. Je pense à toi, vas-y en douceur.",
            "Pense à ${p.titre} à ${p.heure}. Prends bien soin de toi d'ici là."
        )
        return (if (ton == Ton.TAQUIN) taquin else doux).random()
    }

    // JSON view-model de la carte agenda (ou null si désactivé / pas de permission).
    fun json(ctx: Context, ton: Ton): Any {
        val actif = ctx.getSharedPreferences("nuage", Context.MODE_PRIVATE).getBoolean("calendrier", false)
        if (!actif || ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED)
            return JSONObject.NULL
        val evs = duJour(ctx)
        val lignes = JSONArray()
        evs.forEach { lignes.put(JSONObject().put("heure", it.heure).put("titre", it.titre)) }
        return JSONObject().put("lignes", lignes).put("note", commentaire(evs, ton))
    }
}

// Signature de l'état qui influe sur le rendu (déclenche un re-rendu au retour de Réglages).
object Etat {
    fun signature(ctx: Context): String {
        val p = ctx.getSharedPreferences("nuage", Context.MODE_PRIVATE)
        return listOf(
            p.getString("ton", "taquin"),
            p.getBoolean("tenues-meteo", true),
            p.getBoolean("calendrier", false),
            p.getString("anniversaires", "")
        ).joinToString("|")
    }
}

// ---- Dates spéciales (anniversaires) ----
data class Anniversaire(val nom: String, val date: String) // date = "MM-dd"

object Anniversaires {
    fun charge(ctx: Context): List<Anniversaire> {
        val s = prefs(ctx).getString("anniversaires", null) ?: return emptyList()
        return try {
            val arr = JSONArray(s)
            List(arr.length()) { val o = arr.getJSONObject(it); Anniversaire(o.getString("nom"), o.getString("date")) }
        } catch (e: Exception) { emptyList() }
    }

    fun sauve(ctx: Context, liste: List<Anniversaire>) {
        val arr = JSONArray()
        liste.forEach { arr.put(JSONObject().put("nom", it.nom).put("date", it.date)) }
        prefs(ctx).edit().putString("anniversaires", arr.toString()).apply()
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("nuage", Context.MODE_PRIVATE)
}
