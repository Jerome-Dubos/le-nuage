package fr.duboswebservices.monptitnuage

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView

// Dates spéciales : anniversaires/fêtes que le nuage célèbre chaque année (nom + jour/mois).
// Pendant natif de la section « Dates spéciales » de ReglagesView iOS.
class DatesActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("nuage", Context.MODE_PRIVATE) }
    private var estDoux = false; private var nuit = false
    private var fond = 0; private var carte = 0; private var texte = 0; private var secondaire = 0; private var trait = 0; private var accent = 0

    private val mois = listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc.")
    private var liste = mutableListOf<Anniversaire>()
    private lateinit var conteneur: LinearLayout

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
        liste = Anniversaires.charge(this).toMutableList()

        val racine = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(fond) }
        racine.addView(barreTitre())
        val contenu = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(24)) }
        conteneur = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        contenu.addView(conteneur)
        contenu.addView(boutonAjouter())
        contenu.addView(TextView(this).apply {
            text = "Chaque date revient tous les ans : le nuage la fête avec une réplique dédiée et des confettis."
            setTextColor(secondaire); textSize = 13f; typeface = police(false); setPadding(dp(6), dp(16), dp(6), 0)
        })
        racine.addView(ScrollView(this).apply { addView(contenu) })
        setContentView(racine)
        rendre()
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
            text = "←"; textSize = 26f; setTextColor(texte); setPadding(dp(12), dp(4), dp(12), dp(4))
            isClickable = true; setOnClickListener { finish() }
        })
        barre.addView(TextView(this).apply {
            text = "Dates spéciales"; textSize = if (estDoux) 26f else 24f; setTextColor(texte); typeface = police(true)
        })
        return barre
    }

    private fun fondCarte() = GradientDrawable().apply {
        setColor(carte)
        if (estDoux) { val g = dp(20).toFloat(); val p = dp(8).toFloat(); cornerRadii = floatArrayOf(g, g, p, p, g, g, p, p); setStroke(dp(2), trait) }
        else { cornerRadius = dp(16).toFloat(); setStroke(dp(1), trait) }
    }

    private fun sauve() = Anniversaires.sauve(this, liste)

    private fun rendre() {
        conteneur.removeAllViews()
        if (liste.isEmpty()) {
            conteneur.addView(TextView(this).apply {
                text = "Aucune date. Ajoute un anniversaire pour que le nuage le fête."
                setTextColor(secondaire); textSize = 14f; typeface = police(false); setPadding(dp(6), dp(6), dp(6), dp(4))
            })
            return
        }
        liste.indices.forEach { i -> conteneur.addView(ligne(i)) }
    }

    private fun ligne(i: Int): View {
        val a = liste[i]
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = fondCarte(); setPadding(dp(12), dp(6), dp(6), dp(6))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(8); layoutParams = lp
        }
        row.addView(EditText(this).apply {
            setText(a.nom); hint = "Nom (ex. Maman)"
            setHintTextColor(secondaire); setTextColor(texte); textSize = 15f; typeface = police(false)
            background = null; maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { liste[i] = liste[i].copy(nom = s?.toString() ?: ""); sauve() }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, af: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            })
        })
        val moisAct = a.date.substringBefore("-").toIntOrNull() ?: 1
        val jourAct = a.date.substringAfter("-").toIntOrNull() ?: 1
        row.addView(spinner((1..31).map { it.toString() }, jourAct - 1) { j ->
            liste[i] = liste[i].copy(date = "%02d-%02d".format(moisCourant(i), j + 1)); sauve()
        })
        row.addView(spinner(mois, moisAct - 1) { m ->
            liste[i] = liste[i].copy(date = "%02d-%02d".format(m + 1, jourCourant(i))); sauve()
        })
        row.addView(TextView(this).apply {
            text = "✕"; setTextColor(secondaire); textSize = 17f; setPadding(dp(10), dp(6), dp(8), dp(6))
            isClickable = true; setOnClickListener { liste.removeAt(i); sauve(); rendre() }
        })
        return row
    }

    private fun moisCourant(i: Int) = liste[i].date.substringBefore("-").toIntOrNull() ?: 1
    private fun jourCourant(i: Int) = liste[i].date.substringAfter("-").toIntOrNull() ?: 1

    private fun spinner(items: List<String>, sel: Int, onSel: (Int) -> Unit): Spinner {
        return Spinner(this).apply {
            adapter = ArrayAdapter(this@DatesActivity, android.R.layout.simple_spinner_dropdown_item, items).also {
                // teinte le texte des items
            }
            setSelection(sel.coerceIn(0, items.size - 1))
            var premier = true
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    (v as? TextView)?.setTextColor(texte)
                    if (premier) { premier = false; return }
                    onSel(pos)
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun boutonAjouter(): View {
        return TextView(this).apply {
            text = "＋  Ajouter une date"
            setTextColor(accent); textSize = 16f; typeface = police(true)
            setPadding(dp(6), dp(18), dp(6), dp(6))
            isClickable = true
            setOnClickListener {
                val ca = java.util.Calendar.getInstance()
                liste.add(Anniversaire("", "%02d-%02d".format(ca.get(java.util.Calendar.MONTH) + 1, ca.get(java.util.Calendar.DAY_OF_MONTH))))
                sauve(); rendre()
            }
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
