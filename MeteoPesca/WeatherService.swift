import Foundation

public struct FetchedWeatherData {
    public let waterTemp: Double
    public let cloudCover: Double
    public let windDirectionChange: Double
    public let swellHeight: Double
    public let surfaceTempDelta24h: Double
}

public class WeatherService {
    
    public static func fetchWeather(latitude: Double, longitude: Double) async throws -> FetchedWeatherData {
        // 1. Fetch Forecast Data (Atmospheric Conditions)
        let forecastUrlString = "https://api.open-meteo.com/v1/forecast?latitude=\(latitude)&longitude=\(longitude)&current=cloud_cover,wind_direction_10m&hourly=wind_direction_10m,temperature_2m&forecast_days=1"
        guard let forecastUrl = URL(string: forecastUrlString) else {
            throw URLError(.badURL)
        }
        
        let (forecastData, _) = try await URLSession.shared.data(from: forecastUrl)
        let forecastJSON = try JSONSerialization.jsonObject(with: forecastData) as? [String: Any]
        
        // Parse cloud cover
        let current = forecastJSON?["current"] as? [String: Any]
        let cloudCover = current?["cloud_cover"] as? Double ?? 20.0
        
        // Parse hourly values for wind direction change and air temperature
        let hourly = forecastJSON?["hourly"] as? [String: Any]
        let hourlyWind = hourly?["wind_direction_10m"] as? [Double] ?? []
        let hourlyAir = hourly?["temperature_2m"] as? [Double] ?? []
        
        // Wind direction change over last 3 hours (using index 12 vs index 9)
        var windDirectionChange = 10.0
        if hourlyWind.count > 12 {
            let change = abs(hourlyWind[12] - hourlyWind[9])
            windDirectionChange = change > 180.0 ? 360.0 - change : change
        }
        
        // Air temperature change over 24 hours (last index vs first index)
        var surfaceTempDelta24h = 0.0
        if hourlyAir.count >= 24 {
            surfaceTempDelta24h = hourlyAir[hourlyAir.count - 1] - hourlyAir[0]
        }
        
        // 2. Fetch Marine Data (Native Sea Surface Temperature & Swell Height)
        let marineUrlString = "https://marine-api.open-meteo.com/v1/marine?latitude=\(latitude)&longitude=\(longitude)&current=sea_surface_temperature,wave_height&forecast_days=1"
        var waterTemp = 20.0
        var swellHeight = 0.2
        
        if let marineUrl = URL(string: marineUrlString) {
            do {
                let (marineData, _) = try await URLSession.shared.data(from: marineUrl)
                if let marineJSON = try JSONSerialization.jsonObject(with: marineData) as? [String: Any],
                   let currentMarine = marineJSON["current"] as? [String: Any] {
                    if let sst = currentMarine["sea_surface_temperature"] as? Double {
                        waterTemp = sst
                    }
                    if let waveHeight = currentMarine["wave_height"] as? Double {
                        swellHeight = waveHeight
                    }
                }
            } catch {
                // If inland or marine API fails (e.g. coordinates inland), fallback to neutral defaults
                print("Marine API fetch failed or coordinates inland: \(error)")
            }
        }
        
        return FetchedWeatherData(
            waterTemp: waterTemp,
            cloudCover: cloudCover,
            windDirectionChange: windDirectionChange,
            swellHeight: swellHeight,
            surfaceTempDelta24h: surfaceTempDelta24h
        )
    }
}
