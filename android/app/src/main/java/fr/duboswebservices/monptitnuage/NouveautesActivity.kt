package fr.duboswebservices.monptitnuage

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

// « Quoi de neuf » : s'ouvre au 1er lancement après une mise à jour (via Nouveautes.aMontrer)
// et se rouvre à la demande depuis les Réglages (extra "tout" = tout l'historique). L'écran
// adopte l'ambiance complète du ton, comme les Réglages.
class NouveautesActivity : Activity() {

    private var estDoux = false
    private var nuit = false
    private var fond = 0; private var carte = 0; private var texte = 0
    private var secondaire = 0; private var trait = 0; private var accent = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("nuage", Context.MODE_PRIVATE)
        estDoux = prefs.getString("ton", "taquin") == "doux"
        nuit = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        accent = if (estDoux) Color.parseColor("#E0A0B4") else Color.parseColor("#5B86C4")

        if (estDoux) {
            fond = if (nuit) Color.parseColor("#28241F") else Color.parseColor("#FBF3E6")
            carte = if (nuit) Color.parseColor("#34302A") else Color.parseColor("#FFFDF8")
            texte = if (nuit) Color.parseColor("#F4E9D8") else Color.parseColor("#5A4640")
            secondaire = if (nuit) Color.parseColor("#B5A99A") else Color.parseColor("#9A857C")
            trait = if (nuit) Color.parseColor("#52F4E9D8") else Color.parseColor("#6B5A4640")
        } else {
            fond = if (nuit) Color.parseColor("#1E2935") else Color.parseColor("#F2F7FC")
            carte = if (nuit) Color.parseColor("#28374C") else Color.parseColor("#FFFFFF")
            texte = if (nuit) Color.parseColor("#EAF1F9") else Color.parseColor("#33414F")
            secondaire = if (nuit) Color.parseColor("#9FB0C0") else Color.parseColor("#7A8794")
            trait = if (nuit) Color.parseColor("#33FFFFFF") else Color.parseColor("#14000000")
        }

        // Quelles entrées afficher : tout l'historique (depuis les Réglages) ou seulement
        // les versions passées en extra (au lancement après MAJ).
        val entrees = if (intent.getBooleanExtra("tout", false)) {
            Nouveautes.journal
        } else {
            val versions = intent.getStringArrayListExtra("versions")?.toSet() ?: emptySet()
            Nouveautes.journal.filter { it.version in versions }
        }
        if (entrees.isEmpty()) { finish(); return }

        val racine = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(fond)
        }

        val contenu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(44), dp(24), dp(24))
        }

        // En-tête.
        contenu.addView(TextView(this).apply {
            text = "☁️"; textSize = 52f; gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = pleineLargeur()
        })
        contenu.addView(TextView(this).apply {
            text = "Quoi de neuf ?"; setTextColor(texte); textSize = 30f
            typeface = police(true); gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(10), 0, 0); layoutParams = pleineLargeur()
        })
        contenu.addView(TextView(this).apply {
            text = "Le nuage a un peu changé."; setTextColor(secondaire); textSize = 15f
            typeface = police(false); gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(6), 0, dp(22)); layoutParams = pleineLargeur()
        })

        entrees.forEach { contenu.addView(carteEntree(it)) }

        val scroll = ScrollView(this).apply {
            addView(contenu)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        racine.addView(scroll)

        // Bouton « Continuer » fixé en bas.
        val bouton = TextView(this).apply {
            text = "Continuer"; setTextColor(texte); textSize = 16f
            typeface = police(true); gravity = Gravity.CENTER
            setPadding(0, dp(15), 0, dp(15))
            background = GradientDrawable().apply {
                setColor(carte); setStroke(dp(1), trait)
                cornerRadius = dp(16).toFloat()
            }
            isClickable = true; isFocusable = true
            setOnClickListener { finish() }
        }
        racine.addView(LinearLayout(this).apply {
            setPadding(dp(24), dp(8), dp(24), dp(16))
            addView(bouton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })

        setContentView(racine)
    }

    private fun carteEntree(e: Nouveautes.Entree): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = fondCarte()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(14) }
        }

        // Titre + pastille de version.
        val entete = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        entete.addView(TextView(this).apply {
            text = e.titre; setTextColor(texte); textSize = 17f; typeface = police(true)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        entete.addView(TextView(this).apply {
            text = "v${e.version}"; setTextColor(secondaire); textSize = 12f; typeface = police(true)
        })
        col.addView(entete)

        // Points.
        e.points.forEach { p ->
            val ligne = LinearLayout(this).apply {
                setPadding(0, dp(12), 0, 0)
            }
            ligne.addView(TextView(this).apply {
                text = p.symbole; textSize = 18f; gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(30), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            ligne.addView(TextView(this).apply {
                text = p.texte; setTextColor(texte); textSize = 15f; typeface = police(false)
                setPadding(dp(10), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            col.addView(ligne)
        }
        return col
    }

    // Contours de carte : arrondis irréguliers + trait pour le doux (croquis), sinon nets.
    private fun fondCarte(): GradientDrawable = GradientDrawable().apply {
        setColor(carte)
        if (estDoux) {
            val g = dp(24).toFloat(); val p = dp(8).toFloat()
            cornerRadii = floatArrayOf(g, g, p, p, g, g, p, p)
            setStroke(dp(2), trait)
        } else {
            cornerRadius = dp(18).toFloat()
            setStroke(dp(1), trait)
        }
    }

    private fun police(gras: Boolean): Typeface {
        val style = if (gras) Typeface.BOLD else Typeface.NORMAL
        return if (estDoux) Typeface.create("cursive", style) else Typeface.create(Typeface.DEFAULT, style)
    }

    private fun pleineLargeur() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
