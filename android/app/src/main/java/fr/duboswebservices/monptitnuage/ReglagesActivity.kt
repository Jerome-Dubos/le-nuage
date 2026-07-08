package fr.duboswebservices.monptitnuage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
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

// Réglages Android (pendant natif de ReglagesView iOS). L'écran adopte l'AMBIANCE COMPLÈTE
// du ton choisi : taquin = bleu net & moderne ; doux = crème/ardoise, police manuscrite,
// cartes aux contours « croquis ». Le changement de ton re-thème l'écran en direct.
class ReglagesActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("nuage", Context.MODE_PRIVATE) }

    private var nuit = false
    private var estDoux = false
    private var fond = 0; private var carte = 0; private var texte = 0
    private var secondaire = 0; private var separateur = 0; private var trait = 0
    private var accent = 0

    private val accentTaquin = Color.parseColor("#5B86C4")
    private val accentDoux = Color.parseColor("#E0A0B4")

    private var carteTaquin: LinearLayout? = null
    private var carteDoux: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        estDoux = prefs.getString("ton", "taquin") == "doux"
        nuit = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        accent = if (estDoux) accentDoux else accentTaquin

        if (estDoux) {
            fond = if (nuit) Color.parseColor("#28241F") else Color.parseColor("#FBF3E6")
            carte = if (nuit) Color.parseColor("#34302A") else Color.parseColor("#FFFDF8")
            texte = if (nuit) Color.parseColor("#F4E9D8") else Color.parseColor("#5A4640")
            secondaire = if (nuit) Color.parseColor("#B5A99A") else Color.parseColor("#9A857C")
            trait = if (nuit) Color.parseColor("#52F4E9D8") else Color.parseColor("#6B5A4640")
            separateur = trait
        } else {
            fond = if (nuit) Color.parseColor("#1E2935") else Color.parseColor("#F2F7FC")
            carte = if (nuit) Color.parseColor("#28374C") else Color.parseColor("#FFFFFF")
            texte = if (nuit) Color.parseColor("#EAF1F9") else Color.parseColor("#33414F")
            secondaire = if (nuit) Color.parseColor("#9FB0C0") else Color.parseColor("#7A8794")
            separateur = if (nuit) Color.parseColor("#33FFFFFF") else Color.parseColor("#14000000")
            trait = separateur
        }

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
        contenu.addView(carteToggle("Tenues météo", "Bonnet, parapluie, lunettes, écharpe — selon la météo en direct.", "tenues-meteo", true, null))

        contenu.addView(entete("Ton programme"))
        contenu.addView(carteToggle("Agenda commenté", "Le nuage lit ton agenda du jour et le commente. Lecture seule, rien n'est envoyé.", "calendrier", false) { on ->
            if (on && checkSelfPermission(android.Manifest.permission.READ_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(android.Manifest.permission.READ_CALENDAR), 2)
        })

        contenu.addView(entete("Notifications"))
        contenu.addView(carteReveil())
        contenu.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(10)) })
        contenu.addView(carteToggle("Alerte pluie", "Un avertissement quand la pluie approche dans l'heure.", "alerte-pluie", false) { on ->
            if (on) demandeNotif()
            Notifs.programmePluie(this)
        })

        contenu.addView(entete("Dates spéciales"))
        contenu.addView(carteAction("Gérer les dates spéciales  🎂", "Anniversaires et fêtes, célébrés chaque année.") {
            startActivity(Intent(this, DatesActivity::class.java))
        })

        contenu.addView(entete("À propos"))
        contenu.addView(carteAction("Quoi de neuf ?  ✨", "Les nouveautés des dernières mises à jour.") {
            startActivity(Intent(this, NouveautesActivity::class.java).putExtra("tout", true))
        })

        contenu.addView(entete("Contact & soutien"))
        contenu.addView(carteAction("Proposer une amélioration", "Une idée, un bug ? Écris-moi.") {
            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:contact@duboswebservices.fr"))
            i.putExtra(Intent.EXTRA_SUBJECT, "Mon p'tit nuage — retour")
            runCatching { startActivity(i) }
        })
        contenu.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(10)) })
        contenu.addView(carteAction("Soutenir Le Nuage  ☕", "Offrir un café sur Ko-fi.") {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/duboswebservices"))) }
        })

        contenu.addView(TextView(this).apply {
            text = "D'autres réglages arriveront avec les prochaines fonctionnalités."
            setTextColor(secondaire); textSize = 12f; typeface = police(false)
            setPadding(dp(6), dp(18), dp(6), 0)
        })

        racine.addView(ScrollView(this).apply { addView(contenu) })
        setContentView(racine)
    }

    // Police : manuscrite (cursive) pour le doux, arrondie système pour le taquin.
    private fun police(gras: Boolean): Typeface {
        val style = if (gras) Typeface.BOLD else Typeface.NORMAL
        return if (estDoux) Typeface.create("cursive", style) else Typeface.create(Typeface.DEFAULT, style)
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
            text = "Réglages"; textSize = if (estDoux) 26f else 24f; setTextColor(texte)
            typeface = police(true)
        })
        return barre
    }

    private fun entete(t: String) = TextView(this).apply {
        text = t.uppercase(); setTextColor(secondaire); textSize = 12f
        letterSpacing = 0.08f; typeface = police(false)
        setPadding(dp(6), dp(20), dp(6), dp(8))
    }

    // Contours de carte : arrondis irréguliers + trait dessiné pour le doux (croquis).
    private fun fondCarte(): GradientDrawable = GradientDrawable().apply {
        setColor(carte)
        if (estDoux) {
            val g = dp(24).toFloat(); val p = dp(8).toFloat()
            cornerRadii = floatArrayOf(g, g, p, p, g, g, p, p)
            setStroke(dp(2), trait)
        } else {
            cornerRadius = dp(18).toFloat()
            setStroke(dp(1), separateur)
        }
    }

    // Sélecteur de ton : deux cartes personnages côte à côte.
    private fun carteCaractere(): View {
        val rang = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        carteTaquin = carteTon("radieux_taquin.png", "Taquin", "Tendre, mais un peu piquant", accentTaquin, !estDoux) { choisir("taquin") }
        carteDoux = carteTon("radieux_doux.png", "Doux", "Tout doux et bienveillant", accentDoux, estDoux) { choisir("doux") }
        rang.addView(carteTaquin, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            .apply { marginEnd = dp(6) })
        rang.addView(carteDoux, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            .apply { marginStart = dp(6) })
        return rang
    }

    // Changer de ton re-thème tout l'écran (ambiance complète) → on recrée l'activité.
    private fun choisir(ton: String) {
        if (prefs.getString("ton", "taquin") == ton) return
        prefs.edit().putString("ton", ton).apply()
        recreate()
    }

    private fun carteTon(image: String, titre: String, desc: String, acc: Int, selectionne: Boolean, onClick: () -> Unit): LinearLayout {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), dp(16), dp(12), dp(14))
            isClickable = true; isFocusable = true
            background = GradientDrawable().apply {
                setColor(if (selectionne) teinte(acc, if (nuit) 0.22f else 0.12f) else carte)
                if (estDoux) {
                    val g = dp(24).toFloat(); val p = dp(8).toFloat()
                    cornerRadii = floatArrayOf(g, g, p, p, g, g, p, p)
                    setStroke(dp(2), if (selectionne) acc else trait)
                } else {
                    cornerRadius = dp(18).toFloat()
                    setStroke(dp(if (selectionne) 2 else 1), if (selectionne) acc else separateur)
                }
            }
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
            typeface = police(true); setPadding(0, dp(8), 0, 0)
        })
        col.addView(TextView(this).apply {
            text = desc; setTextColor(secondaire); textSize = 12f
            gravity = Gravity.CENTER; typeface = police(false); setPadding(0, dp(3), 0, 0)
        })
        return col
    }

    // Carte à interrupteur générique (titre + aide + toggle sur une clé de préférence).
    private fun carteToggle(titre: String, sous: String, cle: String, defaut: Boolean, onChange: ((Boolean) -> Unit)?): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = fondCarte()
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val ligne = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        ligne.addView(TextView(this).apply {
            text = titre
            setTextColor(texte); textSize = 16f; typeface = police(false)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        ligne.addView(Switch(this).apply {
            isChecked = prefs.getBoolean(cle, defaut)
            fun teinteThumb(on: Boolean) { thumbTintList = ColorStateList.valueOf(if (on) accent else Color.parseColor("#9AA7B4")) }
            teinteThumb(isChecked)
            trackTintList = ColorStateList.valueOf(teinte(accent, 0.5f))
            setOnCheckedChangeListener { _, on ->
                prefs.edit().putBoolean(cle, on).apply(); teinteThumb(on); onChange?.invoke(on)
            }
        })
        col.addView(ligne)
        col.addView(TextView(this).apply {
            text = sous
            setTextColor(secondaire); textSize = 13f; typeface = police(false)
            setPadding(0, dp(6), 0, 0)
        })
        return col
    }

    // Carte « réveil » : interrupteur + heure (ouvre un sélecteur d'heure).
    private fun carteReveil(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = fondCarte()
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val ligne = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        ligne.addView(TextView(this).apply {
            text = "Réveil du nuage"; setTextColor(texte); textSize = 16f; typeface = police(false)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        ligne.addView(Switch(this).apply {
            isChecked = prefs.getBoolean("reveil", false)
            fun teinteThumb(on: Boolean) { thumbTintList = android.content.res.ColorStateList.valueOf(if (on) accent else Color.parseColor("#9AA7B4")) }
            teinteThumb(isChecked)
            trackTintList = android.content.res.ColorStateList.valueOf(teinte(accent, 0.5f))
            setOnCheckedChangeListener { _, on ->
                prefs.edit().putBoolean("reveil", on).apply(); teinteThumb(on)
                if (on) demandeNotif()
                Notifs.programmeReveil(this@ReglagesActivity)
            }
        })
        col.addView(ligne)

        val heureVal = TextView(this).apply {
            setTextColor(accent); textSize = 16f; typeface = police(true)
            text = heureTexte()
        }
        val ligneHeure = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(12), 0, 0)
            isClickable = true
            addView(TextView(this@ReglagesActivity).apply {
                text = "Heure"; setTextColor(secondaire); textSize = 14f; typeface = police(false)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(heureVal)
            setOnClickListener {
                val min = prefs.getInt("reveil-heure", 8 * 60)
                android.app.TimePickerDialog(this@ReglagesActivity, { _, h, m ->
                    prefs.edit().putInt("reveil-heure", h * 60 + m).apply()
                    heureVal.text = heureTexte()
                    Notifs.programmeReveil(this@ReglagesActivity)
                }, min / 60, min % 60, true).show()
            }
        }
        col.addView(ligneHeure)
        col.addView(TextView(this).apply {
            text = "Un petit mot du nuage chaque matin à l'heure choisie."
            setTextColor(secondaire); textSize = 13f; typeface = police(false); setPadding(0, dp(8), 0, 0)
        })
        return col
    }

    private fun heureTexte(): String {
        val m = prefs.getInt("reveil-heure", 8 * 60)
        return "%02d:%02d".format(m / 60, m % 60)
    }

    private fun demandeNotif() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 3)
    }

    // Carte cliquable (action) : titre + sous-texte.
    private fun carteAction(titre: String, sous: String, onClick: () -> Unit): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = fondCarte()
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isClickable = true; isFocusable = true
            setOnClickListener { onClick() }
        }
        col.addView(TextView(this).apply {
            text = titre; setTextColor(accent); textSize = 16f; typeface = police(true)
        })
        col.addView(TextView(this).apply {
            text = sous; setTextColor(secondaire); textSize = 13f; typeface = police(false)
            setPadding(0, dp(4), 0, 0)
        })
        return col
    }

    // Mélange un accent avec le fond de carte à une opacité donnée.
    private fun teinte(a: Int, alpha: Float): Int {
        val r = (Color.red(a) * alpha + Color.red(carte) * (1 - alpha)).toInt()
        val g = (Color.green(a) * alpha + Color.green(carte) * (1 - alpha)).toInt()
        val b = (Color.blue(a) * alpha + Color.blue(carte) * (1 - alpha)).toInt()
        return Color.rgb(r, g, b)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
