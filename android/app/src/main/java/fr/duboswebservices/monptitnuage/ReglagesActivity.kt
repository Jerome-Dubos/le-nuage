package fr.duboswebservices.monptitnuage

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

// Réglages Android (natif, pendant de ReglagesView côté iOS) : caractère du nuage
// (taquin/doux) et tenues météo. Persistés dans les préférences « nuage », lus par
// ViewModelBuilder/Repliques. Construit en code, sans dépendance ni layout XML.
class ReglagesActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("nuage", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val nuit = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val fond = if (nuit) Color.parseColor("#1E2935") else Color.parseColor("#F7FBFF")
        val texte = if (nuit) Color.parseColor("#EAF1F9") else Color.parseColor("#33414F")
        val secondaire = if (nuit) Color.parseColor("#9FB0C0") else Color.parseColor("#7A8794")

        val racine = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(fond)
            val p = dp(24)
            setPadding(p, dp(40), p, p)
        }

        racine.addView(titre("Réglages", texte))
        racine.addView(sousTitre("Caractère du nuage", secondaire, hautMarge = dp(24)))

        val tonActuel = prefs.getString("ton", "taquin")
        val groupe = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val rbTaquin = radio("Taquin — il te charrie gentiment", texte, checked = tonActuel != "doux")
        val rbDoux = radio("Doux — tendre et rassurant (style croquis)", texte, checked = tonActuel == "doux")
        groupe.addView(rbTaquin)
        groupe.addView(rbDoux)
        groupe.setOnCheckedChangeListener { _, id ->
            prefs.edit().putString("ton", if (id == rbDoux.id) "doux" else "taquin").apply()
        }
        racine.addView(groupe)

        racine.addView(sousTitre("Apparence", secondaire, hautMarge = dp(28)))
        val ligne = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        ligne.addView(TextView(this).apply {
            text = "Tenues météo (bonnet, parapluie…)"
            setTextColor(texte)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        ligne.addView(Switch(this).apply {
            isChecked = prefs.getBoolean("tenues-meteo", true)
            val accent = ColorStateList.valueOf(Color.parseColor("#4C86C6"))
            thumbTintList = accent
            trackTintList = ColorStateList.valueOf(Color.parseColor("#804C86C6"))
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean("tenues-meteo", on).apply() }
        })
        racine.addView(ligne)

        racine.addView(TextView(this).apply {
            text = "Le nuage s'habille selon la météo en direct."
            setTextColor(secondaire)
            textSize = 13f
            setPadding(0, dp(6), 0, 0)
        })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(fond)
            addView(racine)
        })
    }

    private fun titre(t: String, couleur: Int) = TextView(this).apply {
        text = t; setTextColor(couleur); textSize = 28f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun sousTitre(t: String, couleur: Int, hautMarge: Int) = TextView(this).apply {
        text = t.uppercase(); setTextColor(couleur); textSize = 12f
        letterSpacing = 0.08f
        setPadding(0, hautMarge, 0, dp(10))
    }

    private fun radio(t: String, couleur: Int, checked: Boolean) = RadioButton(this).apply {
        text = t; setTextColor(couleur); textSize = 16f; isChecked = checked
        buttonTintList = ColorStateList.valueOf(Color.parseColor("#4C86C6"))
        setPadding(dp(8), dp(10), 0, dp(10))
        id = android.view.View.generateViewId()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
