package fr.duboswebservices.monptitnuage

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.util.Locale
import kotlin.math.sqrt

// Écran principal : un ViewPager de pages météo (position GPS + lieux enregistrés), avec
// deux boutons flottants (ajouter un lieu / réglages) et des points d'indicateur — comme iOS.
class MainActivity : Activity() {

    private lateinit var pager: ViewPager2
    private lateinit var dots: LinearLayout
    private lateinit var banniere: TextView
    private var adapter: PagerAdapter? = null

    private var gpsCoords: Coordonnees? = null
    private var gpsNom = "Ma position"
    private var lieux: List<Lieu> = emptyList()
    private var dernierTonSig = ""

    private val repli = Coordonnees(48.8566, 2.3522)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pager = ViewPager2(this)
        dots = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }

        val root = FrameLayout(this)
        root.addView(pager, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        root.addView(boutonFlottant("＋", Gravity.TOP or Gravity.START) {
            startActivity(Intent(this, LieuxActivity::class.java))
        })
        root.addView(boutonFlottant("⚙", Gravity.TOP or Gravity.END) {
            startActivity(Intent(this, ReglagesActivity::class.java))
        })
        val d = resources.displayMetrics.density
        root.addView(dots, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = (26 * d).toInt()
        })
        banniere = TextView(this).apply {
            visibility = android.view.View.GONE
            setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER
            setPadding((16 * d).toInt(), (11 * d).toInt(), (16 * d).toInt(), (11 * d).toInt())
            background = GradientDrawable().apply { cornerRadius = 24 * d; setColor(Color.parseColor("#4C86C6")) }
            elevation = 8 * d
        }
        root.addView(banniere, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = (60 * d).toInt(); leftMargin = (16 * d).toInt(); rightMargin = (16 * d).toInt()
        })

        // Remonte points + bandeau au-dessus de la barre de navigation système.
        root.setOnApplyWindowInsetsListener { _, insets ->
            val bas = if (Build.VERSION.SDK_INT >= 30)
                insets.getInsets(WindowInsets.Type.systemBars()).bottom
            else @Suppress("DEPRECATION") insets.systemWindowInsetBottom
            (dots.layoutParams as FrameLayout.LayoutParams).bottomMargin = bas + (14 * d).toInt()
            (banniere.layoutParams as FrameLayout.LayoutParams).bottomMargin = bas + (46 * d).toInt()
            dots.requestLayout(); banniere.requestLayout()
            insets
        }
        setContentView(root)

        // (Re)planifie les notifications opt-in (réveil quotidien, alerte pluie).
        Notifs.programmeReveil(this)
        Notifs.programmePluie(this)

        // Recherche de mise à jour (bandeau discret si une version plus récente existe).
        Thread {
            val maj = MiseAJour.verifie(this) ?: return@Thread
            runOnUiThread {
                banniere.text = "Mise à jour dispo — v${maj.version}  ·  Installer"
                banniere.visibility = android.view.View.VISIBLE
                banniere.setOnClickListener {
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(maj.apkUrl))) }
                }
            }
        }.start()

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { majDots(adapter?.itemCount ?: 0, position) }
        })

        demarre()
    }

    private fun demarre() {
        val ok = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (ok) chargePosition()
        else requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        chargePosition()
    }

    private fun chargePosition() {
        Thread {
            val coords = positionActuelle() ?: repli
            val nom = nomLieu(coords)
            runOnUiThread { gpsCoords = coords; gpsNom = nom; construitPager() }
        }.start()
    }

    private fun construitPager() {
        lieux = Lieux.charge(this)
        val pages = mutableListOf<PageInfo>()
        pages.add(PageInfo(gpsCoords ?: repli, gpsNom, true))
        lieux.forEach { pages.add(PageInfo(Coordonnees(it.lat, it.lon), it.nom, false)) }

        val garde = pager.currentItem
        adapter = PagerAdapter(pages)
        pager.adapter = adapter
        pager.offscreenPageLimit = maxOf(1, pages.size)
        if (garde in pages.indices) pager.setCurrentItem(garde, false)
        dernierTonSig = tonSig()
        majDots(pages.size, pager.currentItem)
    }

    override fun onResume() {
        super.onResume()
        val nv = Lieux.charge(this)
        if (gpsCoords != null && nv != lieux) construitPager()
        else if (gpsCoords != null && tonSig() != dernierTonSig) {
            adapter?.rafraichitTon(); dernierTonSig = tonSig()
        }
        val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sm.registerListener(secousseListener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        (getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.unregisterListener(secousseListener)
    }

    private fun tonSig(): String = Etat.signature(this)

    // ---- localisation ----
    @SuppressLint("MissingPermission")
    private fun positionActuelle(): Coordonnees? {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
        return loc?.let { Coordonnees(it.latitude, it.longitude) }
    }

    @Suppress("DEPRECATION")
    private fun nomLieu(coords: Coordonnees): String = try {
        Geocoder(this, Locale.getDefault()).getFromLocation(coords.latitude, coords.longitude, 1)
            ?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea } ?: "Ma position"
    } catch (e: Exception) { "Ma position" }

    // ---- secousse : le nuage de la page courante réagit ----
    private var derniereSecousse = 0L
    private val secousseListener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            val g = sqrt((e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]).toDouble()) / SensorManager.GRAVITY_EARTH
            val t = System.currentTimeMillis()
            if (g > 2.3 && t - derniereSecousse > 1000) {
                derniereSecousse = t
                adapter?.page(pager.currentItem)?.secousse()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    // ---- UI : boutons flottants + points ----
    private fun boutonFlottant(glyphe: String, grav: Int, onClick: () -> Unit): TextView {
        val d = resources.displayMetrics.density
        return TextView(this).apply {
            text = glyphe; textSize = 19f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#33414F"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E8FFFFFF"))
                setStroke((1 * d).toInt(), Color.parseColor("#22000000"))
            }
            elevation = 6 * d
            val t = (44 * d).toInt()
            layoutParams = FrameLayout.LayoutParams(t, t).apply {
                gravity = grav
                topMargin = (52 * d).toInt()
                val m = (16 * d).toInt(); leftMargin = m; rightMargin = m
            }
            setOnClickListener { onClick() }
        }
    }

    private fun majDots(n: Int, courant: Int) {
        dots.removeAllViews()
        if (n <= 1) return
        val d = resources.displayMetrics.density
        for (i in 0 until n) {
            dots.addView(TextView(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (i == courant) Color.parseColor("#DDFFFFFF") else Color.parseColor("#55FFFFFF"))
                }
                val s = (7 * d).toInt()
                layoutParams = LinearLayout.LayoutParams(s, s).apply { marginStart = (4 * d).toInt(); marginEnd = (4 * d).toInt() }
            })
        }
    }

    // ---- ViewPager ----
    data class PageInfo(val coords: Coordonnees, val nom: String, val estPosition: Boolean)

    inner class PagerAdapter(private val pages: List<PageInfo>) : RecyclerView.Adapter<PagerAdapter.VH>() {
        private val vues = HashMap<Int, PageMeteoView>()
        inner class VH(val page: PageMeteoView) : RecyclerView.ViewHolder(page)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = PageMeteoView(this@MainActivity)
            v.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            vues[position] = holder.page
            val p = pages[position]
            holder.page.configure(p.coords, p.nom, p.estPosition)
        }

        override fun getItemCount() = pages.size
        fun rafraichitTon() = vues.values.forEach { it.rafraichitTon() }
        fun page(pos: Int) = vues[pos]
    }
}
