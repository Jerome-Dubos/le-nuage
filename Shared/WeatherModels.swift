import Foundation

// Réponse Open-Meteo : un seul appel couvre tout (cf. brief §4). Les noms collent
// aux clés JSON de l'API pour un décodage direct.
struct Meteo: Decodable {
    let current: Actuel
    let minutely_15: Minutely15?
    let daily: Quotidien
    let hourly: Horaire

    struct Actuel: Decodable {
        let temperature_2m: Double
        let apparent_temperature: Double
        let weather_code: Int
        let is_day: Int
        let precipitation: Double
        let wind_speed_10m: Double
        // Détails (vue app) — optionnels pour rester tolérant aux réponses partielles.
        let relative_humidity_2m: Int?
        let wind_gusts_10m: Double?
        let wind_direction_10m: Int?
        let cloud_cover: Int?
    }

    struct Minutely15: Decodable {
        let time: [String]
        let precipitation: [Double]
    }

    struct Quotidien: Decodable {
        let time: [String]
        let temperature_2m_max: [Double]
        let temperature_2m_min: [Double]
        let weather_code: [Int]
        let precipitation_probability_max: [Int?]
        let sunrise: [String]
        let sunset: [String]
        let uv_index_max: [Double?]?
        let daylight_duration: [Double?]?
        let sunshine_duration: [Double?]?
    }

    struct Horaire: Decodable {
        let time: [String]
        let temperature_2m: [Double]
        let weather_code: [Int]
        let precipitation_probability: [Int?]
        let visibility: [Double?]?
    }
}

struct Coordonnees: Equatable {
    let latitude: Double
    let longitude: Double
}

// Parse les horodatages "yyyy-MM-ddTHH:mm" d'Open-Meteo (timezone=auto → heure locale,
// sans offset). Interprétés dans le fuseau de l'appareil, comme le faisait le JS.
enum Horodatage {
    static let formateur: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = .current
        f.dateFormat = "yyyy-MM-dd'T'HH:mm"
        return f
    }()

    static func local(_ s: String) -> Date? { formateur.date(from: s) }
}
