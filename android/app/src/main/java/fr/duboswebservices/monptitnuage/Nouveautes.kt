package fr.duboswebservices.monptitnuage

import android.content.Context

// « Quoi de neuf » embarqué (offline, instantané au 1er lancement après une MAJ, sans
// serveur — pendant Android de Nouveautes.swift). Le journal est écrit à la main à chaque
// release : une entrée par version, la plus récente en tête. Symboles = emoji (pas de
// SF Symbols côté Android).
object Nouveautes {
    private const val CLE = "derniere-version-vue"

    data class Point(val symbole: String, val texte: String)
    data class Entree(val version: String, val titre: String, val points: List<Point>)

    // Journal, de la plus récente à la plus ancienne.
    val journal: List<Entree> = listOf(
        Entree("0.14", "Quoi de neuf ?", listOf(
            Point("✨", "Retrouve les nouveautés de chaque mise à jour, ici même."),
        )),
        Entree("0.13", "Le nuage sur ton écran d'accueil", listOf(
            Point("🌤", "Un widget avec le nuage, la température et une petite vanne, qui suit ton ton."),
        )),
        Entree("0.11", "Ton petit monde", listOf(
            Point("🎂", "Dates spéciales : anniversaires et fêtes, célébrés chaque année."),
            Point("📅", "Le nuage lit ton agenda du jour et le commente à sa façon."),
        )),
        Entree("0.9", "Il te réveille, il te prévient", listOf(
            Point("⏰", "Réveil du nuage : un petit mot chaque matin, si tu veux."),
            Point("🌧", "Alerte pluie : une notif quand l'averse arrive dans l'heure."),
        )),
        Entree("0.5", "Plusieurs lieux", listOf(
            Point("📍", "Ajoute des villes et fais-les défiler d'un glissé."),
        )),
    )

    // Versions à montrer au lancement : celles plus récentes que la dernière vue.
    // Premier lancement (aucune version mémorisée) : on note la version courante sans rien
    // afficher (on ne déroule pas tout l'historique à une nouvelle install).
    fun aMontrer(ctx: Context): List<String> {
        val prefs = ctx.getSharedPreferences("nuage", Context.MODE_PRIVATE)
        val actuelle = MiseAJour.versionActuelle(ctx)
        val vue = prefs.getString(CLE, null)
        prefs.edit().putString(CLE, actuelle).apply()

        if (vue == null || vue == actuelle) return emptyList()
        return journal.filter { MiseAJour.estPlusRecente(it.version, vue) }.map { it.version }
    }
}
