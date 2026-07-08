package fr.duboswebservices.monptitnuage

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Réponse Open-Meteo (port de Meteo.swift). Un seul appel couvre tout.
data class Meteo(
    val current: Actuel,
    val daily: Quotidien,
    val hourly: Horaire
) {
    data class Actuel(
        val temperature_2m: Double,
        val apparent_temperature: Double,
        val weather_code: Int,
        val is_day: Int,
        val wind_speed_10m: Double,
        val relative_humidity_2m: Int?,
        val wind_gusts_10m: Double?,
        val wind_direction_10m: Int?,
        val cloud_cover: Int?
    )

    data class Quotidien(
        val time: List<String>,
        val temperature_2m_max: List<Double>,
        val temperature_2m_min: List<Double>,
        val weather_code: List<Int>,
        val precipitation_probability_max: List<Int?>,
        val sunrise: List<String>,
        val sunset: List<String>,
        val uv_index_max: List<Double?>?,
        val daylight_duration: List<Double?>?,
        val sunshine_duration: List<Double?>?
    )

    data class Horaire(
        val time: List<String>,
        val temperature_2m: List<Double>,
        val weather_code: List<Int>,
        val precipitation_probability: List<Int?>,
        val visibility: List<Double?>?
    )
}

data class Coordonnees(val latitude: Double, val longitude: Double)

// Appel Open-Meteo — mêmes paramètres que l'iOS. Pas de clé. Bloquant (appelé hors UI).
object WeatherService {
    private const val ENDPOINT = "https://api.open-meteo.com/v1/forecast"

    fun charge(coords: Coordonnees): Meteo {
        val q = buildString {
            append("latitude=").append("%.4f".format(coords.latitude))
            append("&longitude=").append("%.4f".format(coords.longitude))
            append("&current=").append(enc("temperature_2m,apparent_temperature,weather_code,is_day,precipitation,wind_speed_10m,relative_humidity_2m,wind_gusts_10m,wind_direction_10m,cloud_cover"))
            append("&daily=").append(enc("temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max,sunrise,sunset,uv_index_max,daylight_duration,sunshine_duration"))
            append("&hourly=").append(enc("temperature_2m,weather_code,precipitation_probability,visibility"))
            append("&forecast_days=7&timezone=auto")
        }
        val json = getJson("$ENDPOINT?$q")
        return parse(json)
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun getJson(url: String): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        try {
            val txt = conn.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(txt)
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(j: JSONObject): Meteo {
        val c = j.getJSONObject("current")
        val d = j.getJSONObject("daily")
        val h = j.getJSONObject("hourly")
        return Meteo(
            current = Meteo.Actuel(
                temperature_2m = c.getDouble("temperature_2m"),
                apparent_temperature = c.getDouble("apparent_temperature"),
                weather_code = c.getInt("weather_code"),
                is_day = c.getInt("is_day"),
                wind_speed_10m = c.getDouble("wind_speed_10m"),
                relative_humidity_2m = c.optIntOrNull("relative_humidity_2m"),
                wind_gusts_10m = c.optDoubleOrNull("wind_gusts_10m"),
                wind_direction_10m = c.optIntOrNull("wind_direction_10m"),
                cloud_cover = c.optIntOrNull("cloud_cover")
            ),
            daily = Meteo.Quotidien(
                time = d.getJSONArray("time").strings(),
                temperature_2m_max = d.getJSONArray("temperature_2m_max").doubles(),
                temperature_2m_min = d.getJSONArray("temperature_2m_min").doubles(),
                weather_code = d.getJSONArray("weather_code").ints(),
                precipitation_probability_max = d.getJSONArray("precipitation_probability_max").intsOrNull(),
                sunrise = d.getJSONArray("sunrise").strings(),
                sunset = d.getJSONArray("sunset").strings(),
                uv_index_max = d.optJSONArray("uv_index_max")?.doublesOrNull(),
                daylight_duration = d.optJSONArray("daylight_duration")?.doublesOrNull(),
                sunshine_duration = d.optJSONArray("sunshine_duration")?.doublesOrNull()
            ),
            hourly = Meteo.Horaire(
                time = h.getJSONArray("time").strings(),
                temperature_2m = h.getJSONArray("temperature_2m").doubles(),
                weather_code = h.getJSONArray("weather_code").ints(),
                precipitation_probability = h.getJSONArray("precipitation_probability").intsOrNull(),
                visibility = h.optJSONArray("visibility")?.doublesOrNull()
            )
        )
    }
}

// ---- helpers org.json (null-safe : Open-Meteo renvoie parfois null dans les tableaux) ----
private fun JSONObject.optIntOrNull(k: String) = if (isNull(k)) null else optInt(k)
private fun JSONObject.optDoubleOrNull(k: String) = if (isNull(k)) null else optDouble(k)

private fun JSONArray.strings() = List(length()) { getString(it) }
private fun JSONArray.ints() = List(length()) { getInt(it) }
private fun JSONArray.doubles() = List(length()) { getDouble(it) }
private fun JSONArray.intsOrNull() = List(length()) { if (isNull(it)) null else getInt(it) }
private fun JSONArray.doublesOrNull() = List(length()) { if (isNull(it)) null else getDouble(it) }
