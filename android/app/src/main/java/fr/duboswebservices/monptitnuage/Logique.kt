package fr.duboswebservices.monptitnuage

// Port Kotlin de la logique métier partagée (Swift : WMO, PaletteTon, Tenue, Ton).
// Ces règles restent natives des deux côtés ; seul le RENDU (web/) est partagé.

enum class Ton(val fichierVannes: String) {
    TAQUIN("vannes-taquines"),
    DOUX("messages-doux");

    companion object {
        fun depuis(brut: String?) = if (brut == "doux") DOUX else TAQUIN
    }
}

// ---- WMO : code météo → état du nuage / groupe de vannes / libellé + icône ----
object Wmo {
    fun etatNuage(code: Int): String = when (code) {
        0 -> "radieux"
        in 1..3 -> "detendu"
        45, 48 -> "endormi"
        in 51..57 -> "ronchon"
        in 61..67, in 80..82 -> "blase"
        in 71..77, 85, 86 -> "emmitoufle"
        in 95..Int.MAX_VALUE -> "flippe"
        else -> "detendu"
    }

    // Humeur : météo enrichie de canicule (≥33°) / grelotte (≤0°) par temps calme.
    fun humeur(code: Int, temp: Int): String {
        val calme = code <= 3 || code == 45 || code == 48
        if (calme) {
            if (temp >= 33) return "canicule"
            if (temp <= 0) return "grelotte"
        }
        return etatNuage(code)
    }

    fun groupeReplique(code: Int): String = when (code) {
        0 -> "clear"
        in 1..3 -> "clouds"
        45, 48 -> "fog"
        in 51..57 -> "drizzle"
        in 61..67, in 80..82 -> "rain"
        in 71..77, 85, 86 -> "snow"
        in 95..Int.MAX_VALUE -> "storm"
        else -> "clouds"
    }

    data class Info(val label: String, val icone: String)

    fun info(code: Int, isDay: Int): Info {
        val nuit = isDay == 0
        return when (code) {
            0 -> Info("Ciel clair", if (nuit) "lune" else "soleil")
            1 -> Info("Plutôt dégagé", if (nuit) "lune" else "soleil")
            2 -> Info("Partiellement nuageux", if (nuit) "lune-nuage" else "soleil-nuage")
            3 -> Info("Couvert", "nuage")
            45, 48 -> Info("Brouillard", "brouillard")
            in 51..57 -> Info("Bruine", "bruine")
            in 61..67 -> Info("Pluie", "pluie")
            in 71..77 -> Info("Neige", "neige")
            in 80..82 -> Info("Averses", "pluie")
            85, 86 -> Info("Averses de neige", "neige")
            in 95..Int.MAX_VALUE -> Info("Orage", "orage")
            else -> Info("Variable", "nuage")
        }
    }
}

// ---- Tenue météo (accessoire selon la météo en direct) ----
enum class Tenue(val fichier: String) {
    LUNETTES("lunettes"), BONNET("bonnet"), PARAPLUIE("parapluie"), ECHARPE("echarpe");

    companion object {
        private val PLUIE = setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)
        private val NEIGE = setOf(71, 73, 75, 77, 85, 86)

        // active = réglage « tenues » (activé par défaut). Prochainement dans les Réglages.
        fun pour(code: Int, temp: Int, vent: Int, jour: Boolean, active: Boolean = true): Tenue? {
            if (!active) return null
            if (code in PLUIE) return PARAPLUIE
            if (code in NEIGE || temp <= 4) return BONNET
            if (vent >= 35) return ECHARPE
            if (temp >= 26 && jour && code in listOf(0, 1, 2)) return LUNETTES
            return null
        }
    }
}

// ---- Palette + identité visuelle par ton (port de PaletteTon) ----
class Palette private constructor(
    private val corps: String, private val ombre: String, private val encre: String,
    private val fondHautClair: String, private val fondBasClair: String,
    private val fondHautSombre: String, private val fondBasSombre: String,
    private val traitClair: String, private val traitSombre: String,
    val croquis: Boolean
) {
    val policeCSS: String
        get() = if (croquis)
            "\"Noteworthy\", \"Marker Felt\", \"Bradley Hand\", -apple-system, cursive"
        else
            "-apple-system, \"SF Pro Rounded\", sans-serif"

    val varsClaires: String
        get() = "--fond-haut:$fondHautClair; --fond-bas:$fondBasClair; --texte:$encre; " +
            "--carte:rgba(255,255,255,.55); --bulle:rgba(255,255,255,.5); --separateur:$ombre; " +
            "--trait:$traitClair; --lisere:rgba(255,255,255,.65);"

    val varsSombres: String
        get() {
            val c = rgb(corps)
            return "--fond-haut:$fondHautSombre; --fond-bas:$fondBasSombre; --texte:$corps; " +
                "--carte:rgba($c,.06); --bulle:rgba($c,.08); --separateur:rgba($c,.12); " +
                "--trait:$traitSombre; --lisere:rgba($c,.10);"
        }

    companion object {
        val TAQUIN = Palette(
            "#EAF1F9", "#D8E5F2", "#33414F",
            "#DCEBFA", "#F7FBFF", "#2B3A4E", "#1E2935",
            "rgba(51,65,79,0)", "rgba(234,241,249,0)", false
        )
        val DOUX = Palette(
            "#F4E9D8", "#E7D3C4", "#5A4640",
            "#FBF3E6", "#FEFAF2", "#34302A", "#28241F",
            "rgba(90,70,64,.42)", "rgba(244,233,216,.32)", true
        )

        fun pour(ton: Ton) = if (ton == Ton.DOUX) DOUX else TAQUIN

        // "#EAF1F9" → "234,241,249"
        private fun rgb(hex: String): String {
            val s = hex.removePrefix("#")
            val v = s.toLong(16)
            return "${(v shr 16) and 0xFF},${(v shr 8) and 0xFF},${v and 0xFF}"
        }
    }
}
