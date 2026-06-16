import Foundation

// Appel Open-Meteo, paramètres identiques au script Scriptable (brief §4). Pas de clé.
enum WeatherService {
    static let endpoint = "https://api.open-meteo.com/v1/forecast"

    static func url(pour coords: Coordonnees) -> URL {
        var c = URLComponents(string: endpoint)!
        c.queryItems = [
            URLQueryItem(name: "latitude", value: String(format: "%.4f", coords.latitude)),
            URLQueryItem(name: "longitude", value: String(format: "%.4f", coords.longitude)),
            URLQueryItem(name: "current", value: "temperature_2m,apparent_temperature,weather_code,is_day,precipitation,wind_speed_10m,relative_humidity_2m,wind_gusts_10m,wind_direction_10m,cloud_cover"),
            URLQueryItem(name: "minutely_15", value: "precipitation"),
            URLQueryItem(name: "daily", value: "temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max,sunrise,sunset,uv_index_max,daylight_duration,sunshine_duration"),
            URLQueryItem(name: "hourly", value: "temperature_2m,weather_code,precipitation_probability,visibility"),
            URLQueryItem(name: "forecast_days", value: "7"),
            URLQueryItem(name: "timezone", value: "auto"),
        ]
        return c.url!
    }

    static func charge(coords: Coordonnees) async throws -> Meteo {
        let (data, _) = try await URLSession.shared.data(from: url(pour: coords))
        return try JSONDecoder().decode(Meteo.self, from: data)
    }

    // MARK: - Recherche de lieu (geocoding Open-Meteo, sans clé)

    static func recherche(_ nom: String) async -> [LieuTrouve] {
        let q = nom.trimmingCharacters(in: .whitespaces)
        guard q.count >= 2, var c = URLComponents(string: "https://geocoding-api.open-meteo.com/v1/search") else { return [] }
        c.queryItems = [
            URLQueryItem(name: "name", value: q),
            URLQueryItem(name: "count", value: "10"),
            URLQueryItem(name: "language", value: "fr"),
            URLQueryItem(name: "format", value: "json"),
        ]
        guard let url = c.url,
              let (data, _) = try? await URLSession.shared.data(from: url),
              let rep = try? JSONDecoder().decode(GeoReponse.self, from: data) else { return [] }
        return rep.results ?? []
    }
}

// Résultat de recherche : ville + région + pays pour lever les ambiguïtés.
struct LieuTrouve: Decodable, Identifiable {
    let id: Int
    let name: String
    let latitude: Double
    let longitude: Double
    let country: String?
    let admin1: String?

    var sousTitre: String {
        [admin1, country].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
    }
}

private struct GeoReponse: Decodable { let results: [LieuTrouve]? }
