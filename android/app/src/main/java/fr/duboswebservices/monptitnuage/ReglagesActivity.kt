package fr.duboswebservices.monptitnuage

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

// Réglages Android (pendant natif de ReglagesView iOS) : caractère du nuage (taquin/doux)
// et tenues météo. Persistés dans les préférences « nuage ». Stylé pour coller à l'app :
// barre de titre + retour, sections en cartes arrondies, adaptatif clair/sombre.
class ReglagesActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("nuage", Context.MODE_PRIVATE) }

    private var nuit = false
    private var fond = 0; private var carte = 0; private var texte = 0
    private var secondaire = 0; private var separateur = 0
    private val accent = Color.parseColor("#4C86C6")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nuit = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        fond = if (nuit) Color.parseColor("#1E2935") else Color.parseColor("#F2F7FC")
        carte = if (nuit) Color.parseColor("#28374C") else Color.parseColor("#FFFFFF")
        texte = if (nuit) Color.parseColor("#EAF1F9") else Color.parseColor("#33414F")
        secondaire = if (nuit) Color.parseColor("#9FB0C0") else Color.parseColor("#7A8794")
        separateur = if (nuit) Color.parseColor("#33FFFFFF") else Color.parseColor("#14000000")

        val racine = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(fond)
        }
        racine.addView(barreTitre())

        val contenu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
        }
        contenu.addView(entete("Caractère du nuage"))
        contenu.addView(carteCaractere())
        contenu.addView(entete("Apparence"))
        contenu.addView(carteTenues())
        contenu.addView(TextView(this).apply {
            text = "D'autres réglages arriveront avec les prochaines fonctionnalités."
            setTextColor(secondaire); textSize = 12f
            setPadding(dp(6), dp(18), dp(6), 0)
        })

        racine.addView(ScrollView(this).apply { addView(contenu) })
        setContentView(racine)
    }

    // Barre : flèche retour + titre.
    private fun barreTitre(): View {
        val barre = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(38), dp(16), dp(10))
        }
        barre.addView(TextView(this).apply {
            text = "←"; textSize = 26f; setTextColor(texte)
            setPadding(dp(12), dp(4), dp(12), dp(4))
            isClickable = true; isFocusable = true
            setOnClickListener { finish() }
        })
        barre.addView(TextView(this).apply {
            text = "Réglages"; textSize = 24f; setTextColor(texte)
            setTypeface(typeface, Typeface.BOLD)
        })
        return barre
    }

    private fun entete(t: String) = TextView(this).apply {
        text = t.uppercase(); setTextColor(secondaire); textSize = 12f
        letterSpacing = 0.08f
        setPadding(dp(6), dp(18), dp(6), dp(8))
    }

    private fun fondCarte() = GradientDrawable().apply {
        cornerRadius = dp(18).toFloat(); setColor(carte)
    }

    // Carte « caractère du nuage » : deux options radio.
    private fun carteCaractere(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = fondCarte()
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        val tonActuel = prefs.getString("ton", "taquin")
        val groupe = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val rbTaquin = radio("Taquin", "Il te charrie gentiment", tonActuel != "doux")
        val rbDoux = radio("Doux", "Tendre et rassurant, style croquis dessiné main", tonActuel == "doux")
        groupe.addView(rbTaquin); groupe.addView(rbDoux)
        groupe.setOnCheckedChangeListener { _, id ->
            prefs.edit().putString("ton", if (id == rbDoux.id) "doux" else "taquin").apply()
        }
        col.addView(groupe)
        return col
    }

    // Une option radio avec titre + sous-texte.
    private fun radio(titre: String, sous: String, checked: Boolean): RadioButton {
        val html = "$titre\n$sous"
        return RadioButton(this).apply {
            text = html
            setTextColor(texte); textSize = 16f; isChecked = checked
            buttonTintList = ColorStateList.valueOf(accent)
            setPadding(dp(10), dp(12), dp(8), dp(12))
            id = View.generateViewId()
            // colore et réduit la 2e ligne (sous-texte)
            post {
                val sp = android.text.SpannableString(text)
                val i = text.indexOf('\n')
                if (i > 0) {
                    sp.setSpan(android.text.style.ForegroundColorSpan(secondaire), i, text.length, 0)
                    sp.setSpan(android.text.style.RelativeSizeSpan(0.8f), i, text.length, 0)
                    text = sp
                }
            }
        }
    }

    // Carte « tenues météo » : libellé + interrupteur + aide.
    private fun carteTenues(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = fondCarte()
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val ligne = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        ligne.addView(TextView(this).apply {
            text = "Tenues météo"
            setTextColor(texte); textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        ligne.addView(Switch(this).apply {
            isChecked = prefs.getBoolean("tenues-meteo", true)
            thumbTintList = ColorStateList.valueOf(if (isChecked) accent else Color.parseColor("#9AA7B4"))
            trackTintList = ColorStateList.valueOf(Color.parseColor("#804C86C6"))
            setOnCheckedChangeListener { _, on ->
                prefs.edit().putBoolean("tenues-meteo", on).apply()
                thumbTintList = ColorStateList.valueOf(if (on) accent else Color.parseColor("#9AA7B4"))
            }
        })
        col.addView(ligne)
        col.addView(TextView(this).apply {
            text = "Bonnet, parapluie, lunettes, écharpe — selon la météo en direct."
            setTextColor(secondaire); textSize = 13f
            setPadding(0, dp(6), 0, 0)
        })
        return col
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
