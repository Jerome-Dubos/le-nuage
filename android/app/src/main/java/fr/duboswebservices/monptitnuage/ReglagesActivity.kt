package fr.duboswebservices.monptitnuage

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

// Réglages Android (pendant natif de ReglagesView iOS) : caractère du nuage (deux cartes
// personnages, comme iOS) et tenues météo. Persistés dans les préférences « nuage ».
class ReglagesActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("nuage", Context.MODE_PRIVATE) }

    private var nuit = false
    private var fond = 0; private var carte = 0; private var texte = 0
    private var secondaire = 0; private var separateur = 0

    private val accentTaquin = "#5B86C4"
    private val accentDoux = "#E0A0B4"
    private val accentBleu = Color.parseColor("#4C86C6")

    private var tonChoisi = "taquin"
    private var carteTaquin: LinearLayout? = null
    private var carteDoux: LinearLayout? = null

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
        setPadding(dp(6), dp(20), dp(6), dp(8))
    }

    // Sélecteur de ton : deux cartes personnages côte à côte (comme iOS).
    private fun carteCaractere(): View {
        tonChoisi = prefs.getString("ton", "taquin") ?: "taquin"
        val rang = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        carteTaquin = carteTon("radieux_taquin.png", "Taquin", "Tendre, mais un peu piquant") { choisir("taquin") }
        carteDoux = carteTon("radieux_doux.png", "Doux", "Tout doux et bienveillant") { choisir("doux") }
        rang.addView(carteTaquin, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            .apply { marginEnd = dp(6) })
        rang.addView(carteDoux, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            .apply { marginStart = dp(6) })
        rafraichitCartes()
        return rang
    }

    private fun choisir(ton: String) {
        tonChoisi = ton
        prefs.edit().putString("ton", ton).apply()
        rafraichitCartes()
    }

    private fun rafraichitCartes() {
        carteTaquin?.background = fondCarteTon(tonChoisi == "taquin", accentTaquin)
        carteDoux?.background = fondCarteTon(tonChoisi == "doux", accentDoux)
    }

    private fun fondCarteTon(selectionne: Boolean, accentHex: String): GradientDrawable {
        val a = Color.parseColor(accentHex)
        return GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(if (selectionne) teinte(a, if (nuit) 0.22f else 0.12f) else carte)
            setStroke(dp(if (selectionne) 2 else 1), if (selectionne) a else separateur)
        }
    }

    private fun carteTon(image: String, titre: String, desc: String, onClick: () -> Unit): LinearLayout {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), dp(16), dp(12), dp(14))
            isClickable = true; isFocusable = true
            setOnClickListener { onClick() }
        }
        val bmp = try { assets.open("personas/$image").use { BitmapFactory.decodeStream(it) } } catch (e: Exception) { null }
        col.addView(ImageView(this).apply {
            bmp?.let { setImageBitmap(it) }
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(66))
        })
        col.addView(TextView(this).apply {
            text = titre; setTextColor(texte); textSize = 16f
            setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(8), 0, 0)
        })
        col.addView(TextView(this).apply {
            text = desc; setTextColor(secondaire); textSize = 12f
            gravity = Gravity.CENTER; setPadding(0, dp(3), 0, 0)
        })
        return col
    }

    // Carte « tenues météo » : libellé + interrupteur + aide.
    private fun carteTenues(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { cornerRadius = dp(18).toFloat(); setColor(carte) }
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
            fun teinteThumb(on: Boolean) { thumbTintList = ColorStateList.valueOf(if (on) accentBleu else Color.parseColor("#9AA7B4")) }
            teinteThumb(isChecked)
            trackTintList = ColorStateList.valueOf(Color.parseColor("#804C86C6"))
            setOnCheckedChangeListener { _, on ->
                prefs.edit().putBoolean("tenues-meteo", on).apply(); teinteThumb(on)
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

    // Mélange un accent avec le fond de carte à une opacité donnée (état sélectionné).
    private fun teinte(accent: Int, alpha: Float): Int {
        val r = (Color.red(accent) * alpha + Color.red(carte) * (1 - alpha)).toInt()
        val g = (Color.green(accent) * alpha + Color.green(carte) * (1 - alpha)).toInt()
        val b = (Color.blue(accent) * alpha + Color.blue(carte) * (1 - alpha)).toInt()
        return Color.rgb(r, g, b)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
