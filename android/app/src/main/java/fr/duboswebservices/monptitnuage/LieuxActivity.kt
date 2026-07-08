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
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

// Gestion des lieux : recherche géocodée pour ajouter une ville, liste des lieux
// enregistrés avec suppression. Persistés via Lieux ; MainActivity reconstruit ses pages.
class LieuxActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("nuage", Context.MODE_PRIVATE) }
    private var estDoux = false; private var nuit = false
    private var fond = 0; private var carte = 0; private var texte = 0
    private var secondaire = 0; private var trait = 0; private var accent = 0

    private lateinit var resultats: LinearLayout
    private lateinit var maListe: LinearLayout
    private var lieux = mutableListOf<Lieu>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        estDoux = prefs.getString("ton", "taquin") == "doux"
        nuit = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
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
        lieux = Lieux.charge(this).toMutableList()

        val racine = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(fond) }
        racine.addView(barreTitre())

        val contenu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
        }
        contenu.addView(champRecherche())
        resultats = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        contenu.addView(resultats)
        contenu.addView(entete("Mes lieux"))
        maListe = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        contenu.addView(maListe)
        rafraichitListe()

        racine.addView(ScrollView(this).apply { addView(contenu) })
        setContentView(racine)
    }

    private fun police(gras: Boolean): Typeface {
        val s = if (gras) Typeface.BOLD else Typeface.NORMAL
        return if (estDoux) Typeface.create("cursive", s) else Typeface.create(Typeface.DEFAULT, s)
    }

    private fun barreTitre(): View {
        val barre = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(38), dp(16), dp(10))
        }
        barre.addView(TextView(this).apply {
            text = "←"; textSize = 26f; setTextColor(texte)
            setPadding(dp(12), dp(4), dp(12), dp(4)); isClickable = true
            setOnClickListener { finish() }
        })
        barre.addView(TextView(this).apply {
            text = "Mes lieux"; textSize = if (estDoux) 26f else 24f; setTextColor(texte); typeface = police(true)
        })
        return barre
    }

    private fun entete(t: String) = TextView(this).apply {
        text = t.uppercase(); setTextColor(secondaire); textSize = 12f; letterSpacing = 0.08f
        typeface = police(false); setPadding(dp(6), dp(22), dp(6), dp(8))
    }

    private fun fondCarte() = GradientDrawable().apply {
        setColor(carte)
        if (estDoux) { val g = dp(20).toFloat(); val p = dp(8).toFloat(); cornerRadii = floatArrayOf(g, g, p, p, g, g, p, p); setStroke(dp(2), trait) }
        else { cornerRadius = dp(16).toFloat(); setStroke(dp(1), trait) }
    }

    private fun champRecherche(): View {
        val champ = EditText(this).apply {
            hint = "Rechercher une ville…"
            setHintTextColor(secondaire); setTextColor(texte); textSize = 16f
            typeface = police(false)
            background = fondCarte()
            setPadding(dp(16), dp(14), dp(16), dp(14))
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            maxLines = 1
            setOnEditorActionListener { _, _, _ -> lance(text.toString()); true }
        }
        return champ
    }

    private fun lance(q: String) {
        resultats.removeAllViews()
        resultats.addView(TextView(this).apply { text = "Recherche…"; setTextColor(secondaire); typeface = police(false); setPadding(dp(6), dp(10), dp(6), dp(4)) })
        Thread {
            val res = Lieux.recherche(q)
            runOnUiThread {
                resultats.removeAllViews()
                if (res.isEmpty()) {
                    resultats.addView(TextView(this).apply { text = "Aucun résultat."; setTextColor(secondaire); typeface = police(false); setPadding(dp(6), dp(10), dp(6), dp(4)) })
                } else res.forEach { r -> resultats.addView(ligneResultat(r)) }
            }
        }.start()
    }

    private fun ligneResultat(r: LieuTrouve): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = fondCarte()
            setPadding(dp(16), dp(12), dp(16), dp(12)); isClickable = true
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(8); layoutParams = lp
            setOnClickListener {
                lieux.add(Lieu(r.nom, r.lat, r.lon)); Lieux.sauve(this@LieuxActivity, lieux)
                resultats.removeAllViews(); rafraichitListe()
            }
        }
        col.addView(TextView(this).apply { text = r.nom; setTextColor(texte); textSize = 16f; typeface = police(true) })
        if (r.sousTitre.isNotEmpty()) col.addView(TextView(this).apply {
            text = r.sousTitre; setTextColor(secondaire); textSize = 13f; typeface = police(false); setPadding(0, dp(2), 0, 0)
        })
        return col
    }

    private fun rafraichitListe() {
        maListe.removeAllViews()
        if (lieux.isEmpty()) {
            maListe.addView(TextView(this).apply {
                text = "Aucun lieu enregistré. Cherche une ville ci-dessus pour l'ajouter."
                setTextColor(secondaire); textSize = 14f; typeface = police(false); setPadding(dp(6), dp(6), dp(6), 0)
            })
            return
        }
        lieux.toList().forEach { l ->
            val ligne = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                background = fondCarte(); setPadding(dp(16), dp(12), dp(10), dp(12))
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.topMargin = dp(8); layoutParams = lp
            }
            ligne.addView(TextView(this).apply {
                text = l.nom; setTextColor(texte); textSize = 16f; typeface = police(false)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            ligne.addView(TextView(this).apply {
                text = "✕"; setTextColor(secondaire); textSize = 18f; setPadding(dp(12), dp(4), dp(12), dp(4))
                isClickable = true
                setOnClickListener {
                    lieux.remove(l); Lieux.sauve(this@LieuxActivity, lieux); rafraichitListe()
                }
            })
            maListe.addView(ligne)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
