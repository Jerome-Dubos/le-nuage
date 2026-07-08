package fr.duboswebservices.monptitnuage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Port de HTMLBuilder.viewModel : produit le view-model JSON consommé par web/nuage.js
// (window.rendre). Toute la logique métier reste native, le rendu est partagé.
class ViewModelBuilder(private val ctx: Context) {

    private val repliques = Repliques(ctx)

    fun json(meteo: Meteo, ton: Ton, lieu: String, estPosition: Boolean): String {
        val c = meteo.current
        val nuit = c.is_day == 0
        val info = Wmo.info(c.weather_code, c.is_day)
        val jours = listOf("dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam.")

        val effet = when (Wmo.groupeReplique(c.weather_code)) {
            "clear" -> if (nuit) "etoiles" else "soleil"
            "drizzle", "rain" -> "pluie"
            "snow" -> "neige"
            "storm" -> "orage"
            "fog" -> "brume"
            else -> ""
        }

        val (occVanne, deco) = occasionDuJour(ton)
        val vanne = occVanne ?: repliques.choisit(meteo, ton)

        val tempInt = Math.round(c.temperature_2m).toInt()
        val etat = Wmo.humeur(c.weather_code, tempInt)
        var nuageSVG = svgInline(etat, nuit, ton)
        val tenuesActives = ctx.getSharedPreferences("nuage", Context.MODE_PRIVATE)
            .getBoolean("tenues-meteo", true)
        Tenue.pour(c.weather_code, tempInt, Math.round(c.wind_speed_10m).toInt(), c.is_day == 1, tenuesActives)?.let {
            nuageSVG = injecteTenue(nuageSVG, it)
        }
        val peutCligner = peutCligner(etat, nuit)
        val vannes = repliques.pool(meteo, ton).map { repliques.remplaceTemp(it, meteo) }

        val vent = Math.round(c.wind_speed_10m).toInt()
        val chips = JSONArray()
            .put(chip("vent", "$vent km/h"))
            .put(chip("lever", heureCourte(meteo.daily.sunrise.firstOrNull())))
            .put(chip("coucher", heureCourte(meteo.daily.sunset.firstOrNull())))

        // prochaines 12 heures
        val maintenant = Date()
        val depart = meteo.hourly.time.indexOfFirst {
            (parseLocal(it)?.time ?: 0L) > maintenant.time - 3600_000L
        }
        val heures = JSONArray()
        if (depart >= 0) {
            val fin = minOf(depart + 12, meteo.hourly.time.size)
            for (i in depart until fin) {
                val d = parseLocal(meteo.hourly.time[i]) ?: Date()
                val hr = cal(d).get(Calendar.HOUR_OF_DAY)
                val label = if (i == depart) "Maint." else "$hr h"
                val isDayH = if (hr >= 21 || hr <= 6) 0 else 1
                val ico = Wmo.info(meteo.hourly.weather_code[i], isDayH).icone
                val proba = meteo.hourly.precipitation_probability.getOrNull(i) ?: 0
                heures.put(JSONObject()
                    .put("label", label).put("ico", ico)
                    .put("temp", Math.round(meteo.hourly.temperature_2m[i]))
                    .put("proba", proba))
            }
        }

        // 7 jours
        val joursArr = JSONArray()
        for (i in meteo.daily.time.indices) {
            val d = parseJour(meteo.daily.time[i]) ?: Date()
            val nom = if (i == 0) "Aujourd'hui" else {
                val ca = cal(d)
                val wd = ca.get(Calendar.DAY_OF_WEEK) - 1
                "${jours[wd]} ${ca.get(Calendar.DAY_OF_MONTH)}"
            }
            val ico = Wmo.info(meteo.daily.weather_code[i], 1).icone
            val proba = meteo.daily.precipitation_probability_max.getOrNull(i)
            joursArr.put(JSONObject()
                .put("nom", nom).put("ico", ico)
                .put("min", Math.round(meteo.daily.temperature_2m_min[i]))
                .put("max", Math.round(meteo.daily.temperature_2m_max[i]))
                .put("proba", proba ?: JSONObject.NULL))
        }

        // carte Détails
        val stats = JSONArray()
        c.relative_humidity_2m?.let { h ->
            val (q, col) = humiditeInfo(h); stats.put(stat("goutte", "$h %", "Humidité", q, col))
        }
        c.wind_direction_10m?.let { dir ->
            stats.put(stat("boussole", cardinal(dir), "Vent", directionNom(dir), C_GRIS, dir))
        }
        c.wind_gusts_10m?.let { g ->
            val (q, col) = rafalesInfo(g); stats.put(stat("vent", "${Math.round(g)} km/h", "Rafales", q, col))
        }
        c.cloud_cover?.let { cc ->
            val (q, col) = couvertureInfo(cc); stats.put(stat("nuage", "$cc %", "Couverture", q, col))
        }
        meteo.daily.uv_index_max?.firstOrNull()?.let { uv ->
            val (q, col) = uvInfo(uv); stats.put(stat("soleil", "${Math.round(uv)}", "Indice UV", q, col))
        }
        meteo.daily.sunshine_duration?.firstOrNull()?.let { sec ->
            stats.put(stat("soleil", "${Math.round(sec / 3600)} h", "Soleil", "", ""))
        }
        meteo.daily.daylight_duration?.firstOrNull()?.let { sec ->
            stats.put(stat("duree", dureeJour(sec), "Durée du jour", "", ""))
        }
        if (depart >= 0) {
            meteo.hourly.visibility?.getOrNull(depart)?.let { v ->
                val km = Math.round(v / 1000).toInt()
                val (q, col) = visibiliteInfo(km); stats.put(stat("oeil", "$km km", "Visibilité", q, col))
            }
        }

        val palette = Palette.pour(ton)
        val tempStyle: Any = when {
            palette.croquis -> JSONObject.NULL
            c.temperature_2m >= 32 -> "#E0703A"
            c.temperature_2m <= -2 -> "#4F8AC9"
            else -> JSONObject.NULL
        }

        return JSONObject().apply {
            put("ton", if (ton == Ton.DOUX) "doux" else "taquin")
            put("croquis", palette.croquis)
            put("police", palette.policeCSS)
            put("varsClaires", palette.varsClaires)
            put("varsSombres", palette.varsSombres)
            put("nuit", nuit)
            put("lieu", lieu)
            put("estPosition", estPosition)
            put("temp", tempInt)
            put("tempStyle", tempStyle)
            put("label", info.label)
            put("ressenti", Math.round(c.apparent_temperature))
            put("nuageSVG", nuageSVG)
            put("peutCligner", peutCligner)
            put("effet", effet)
            put("deco", deco)
            put("vanne", vanne)
            put("vannes", JSONArray(vannes))
            put("chips", chips)
            put("agenda", if (estPosition) Agenda.json(ctx, ton) else JSONObject.NULL)
            put("stats", stats)
            put("heures", heures)
            put("jours", joursArr)
        }.toString()
    }

    // ---- nuage SVG (lu dans les assets, tenue superposée) ----
    private fun svgInline(etat: String, nuit: Boolean, ton: Ton): String {
        val dossier = if (ton == Ton.DOUX) "svg-doux" else "svg"
        val nom = "$etat${if (nuit) "_nuit" else ""}.svg"
        return try {
            ctx.assets.open("$dossier/$nom").bufferedReader().use { it.readText() }
        } catch (e: Exception) { "" }
    }

    private fun injecteTenue(svg: String, tenue: Tenue): String {
        val contenu = try {
            ctx.assets.open("svg-accessoires/${tenue.fichier}.svg").bufferedReader().use { it.readText() }
        } catch (e: Exception) { return svg }
        val ouvre = contenu.indexOf('>')
        val clot = contenu.lastIndexOf("</svg>")
        val fermeture = svg.lastIndexOf("</svg>")
        if (ouvre < 0 || clot < 0 || fermeture < 0) return svg
        val interne = contenu.substring(ouvre + 1, clot)
        return svg.substring(0, fermeture) + "<g class=\"tenue\">$interne</g></svg>" + svg.substring(fermeture + 6)
    }

    // ---- occasions spéciales (dates fixes ; anniversaires configurables à venir) ----
    private fun occasionDuJour(ton: Ton): Pair<String?, String> {
        val ca = Calendar.getInstance()
        val m = ca.get(Calendar.MONTH) + 1
        val j = ca.get(Calendar.DAY_OF_MONTH)
        val taquin = ton == Ton.TAQUIN

        // Dates spéciales configurables (prioritaires) : anniversaires + confettis.
        val mmdd = "%02d-%02d".format(m, j)
        Anniversaires.charge(ctx).firstOrNull { it.date == mmdd }?.let { occ ->
            val qui = occ.nom.trim()
            val suffixe = if (qui.isEmpty()) "" else " $qui"
            return if (taquin) "Joyeux anniversaire$suffixe ! Un an de plus, ça se fête (et ça se charrie). 🎂" to "confettis"
            else "Joyeux anniversaire$suffixe 🎂 Une journée aussi douce que toi." to "confettis"
        }

        return when {
            m == 12 && (j == 24 || j == 25) -> Pair(
                if (taquin) "Joyeux Noël. Le Père Noël t'a vu, va falloir t'expliquer."
                else "Joyeux Noël ❄️ Plein de douceur sur toi aujourd'hui.", "neige")
            (m == 12 && j == 31) || (m == 1 && j == 1) -> Pair(
                if (taquin) "Bonne année ! Tes résolutions tiendront jusqu'au 3, sois honnête."
                else "Bonne année ✨ Que du beau et du doux pour toi.", "confettis")
            m == 2 && j == 14 -> Pair(
                if (taquin) "Saint-Valentin. Même un nuage a trouvé l'amour avant toi. Courage."
                else "Joyeuse Saint-Valentin 💗 Tu es adorable, ne l'oublie jamais.", "coeurs")
            m == 4 && j == 1 -> Pair(
                if (taquin) "Il va faire 45° et neiger en même temps. … Poisson d'avril."
                else "Petit poisson d'avril tout doux 🐟 Passe une jolie journée.", "")
            m == 10 && j == 31 -> Pair(
                if (taquin) "Halloween. Le seul truc effrayant ici, c'est ta sonnerie de réveil."
                else "Joyeux Halloween 🎃 Reste au chaud, petit fantôme tout mignon.", "")
            else -> Pair(null, "")
        }
    }

    private fun peutCligner(etat: String, nuit: Boolean): Boolean =
        if (nuit) etat in listOf("endormi", "ronchon", "flippe")
        else etat in listOf("radieux", "emmitoufle", "endormi", "ronchon", "flippe")

    private fun dureeJour(sec: Double): String {
        val total = Math.round(sec).toInt()
        return "${total / 3600}h${"%02d".format((total % 3600) / 60)}"
    }

    private fun heureCourte(s: String?): String =
        if (s != null && s.length >= 16) s.substring(11, 16) else ""

    // ---- boussole ----
    private val cardinaux = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    private val cardinauxNoms = listOf("Nord", "Nord-Est", "Est", "Sud-Est", "Sud", "Sud-Ouest", "Ouest", "Nord-Ouest")
    private fun idxCardinal(deg: Int) = (Math.round(deg / 45.0).toInt()) % 8
    private fun cardinal(deg: Int) = cardinaux[idxCardinal(deg)]
    private fun directionNom(deg: Int) = cardinauxNoms[idxCardinal(deg)]

    // ---- couleurs sémantiques + interprétations ----
    private val C_VERT = "#3FA46A"; private val C_JAUNE = "#C99A1E"; private val C_ORANGE = "#E07B39"
    private val C_ROUGE = "#D2544B"; private val C_BLEU = "#4C86C6"; private val C_BLEUF = "#3A6CA8"
    private val C_VIOLET = "#9B59B6"; private val C_GRIS = "#8B95A1"

    private fun humiditeInfo(h: Int) = when {
        h < 40 -> "Air sec" to C_ORANGE; h < 65 -> "Confort" to C_VERT
        h < 80 -> "Humide" to C_BLEU; else -> "Très humide" to C_BLEUF
    }
    private fun rafalesInfo(k: Double) = when {
        k < 20 -> "Légères" to C_VERT; k < 40 -> "Modérées" to C_JAUNE
        k < 62 -> "Fortes" to C_ORANGE; else -> "Violentes" to C_ROUGE
    }
    private fun couvertureInfo(p: Int) = when {
        p < 13 -> "Dégagé" to C_BLEU; p < 50 -> "Partiel" to C_VERT
        p < 88 -> "Nuageux" to C_GRIS; else -> "Couvert" to C_GRIS
    }
    private fun uvInfo(uv: Double) = when {
        uv < 3 -> "Faible" to C_VERT; uv < 6 -> "Modéré" to C_JAUNE
        uv < 8 -> "Élevé" to C_ORANGE; uv < 11 -> "Très élevé" to C_ROUGE
        else -> "Extrême" to C_VIOLET
    }
    private fun visibiliteInfo(km: Int) = when {
        km >= 20 -> "Excellente" to C_VERT; km >= 10 -> "Bonne" to C_VERT
        km >= 4 -> "Moyenne" to C_JAUNE; km >= 1 -> "Faible" to C_ORANGE
        else -> "Brouillard" to C_ROUGE
    }

    // ---- helpers JSON + dates ----
    private fun chip(ico: String, texte: String) = JSONObject().put("ico", ico).put("texte", texte)
    private fun stat(ico: String, v: String, cat: String, qual: String, couleur: String, deg: Int? = null) =
        JSONObject().put("ico", ico).put("val", v).put("cat", cat)
            .put("qual", qual).put("couleur", couleur).put("deg", deg ?: JSONObject.NULL)

    private fun parseLocal(s: String): Date? =
        try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).parse(s) } catch (e: Exception) { null }
    private fun parseJour(s: String): Date? =
        try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s) } catch (e: Exception) { null }
    private fun cal(d: Date) = Calendar.getInstance().apply { time = d }
}
