package com.vincenzo.previsionipesca.models

import java.util.Date
import java.util.UUID
import kotlin.math.exp
import kotlin.math.pow

data class Coordinate(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null
)

data class Location(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val coordinate: Coordinate,
    val tideLagDays: Double
) {
    constructor(name: String, latitude: Double, longitude: Double, tideLagDays: Double = 1.5) : this(
        UUID.randomUUID(),
        name,
        Coordinate(latitude, longitude, name),
        tideLagDays
    )
}

enum class TideType(val displayName: String) {
    ALTA("Alta"),
    BASSA("Bassa")
}

data class TideEvent(
    val id: UUID = UUID.randomUUID(),
    val time: Date,
    val height: Double, // in meters
    val type: TideType
)

enum class SolunarType(val displayName: String) {
    MAGGIOR("Maggiore"),
    MINOR("Minore")
}

data class SolunarPeriod(
    val id: UUID = UUID.randomUUID(),
    val startTime: Date,
    val endTime: Date,
    val type: SolunarType,
    val description: String,
    var isEnhanced: Boolean = false
)

enum class ActivityLevel(val displayName: String, val colorHex: String, val description: String, val score: Int) {
    BASSA("Bassa", "#FF5252", "Attività Bassa", 0),
    MODERATA("Moderata", "#FFB74D", "Attività Moderata", 1),
    BUONA("Buona", "#FFF176", "Attività Buona 🎣", 2),
    ALTA("Alta", "#4DB6AC", "Attività Alta! 🎣", 3),
    MOLTO_ALTA("Molto Alta", "#81C784", "Attività Eccezionale! 🔥", 4);
    
    // Alias to easily support eccezionale color mapping
    val colorString get() = colorHex
}

data class HourlyInterval(
    val id: UUID = UUID.randomUUID(),
    val hour: Int, // 0-23
    val startTime: Date,
    val endTime: Date,
    val activity: ActivityLevel,
    val score: Double,
    val isMajorPeriod: Boolean = false,
    val isMinorPeriod: Boolean = false,
    val isEnhanced: Boolean = false
)

data class ActivityWindow(
    val id: UUID = UUID.randomUUID(),
    val start: Date,
    val end: Date,
    val peak: Date,
    val peakScore: Double,
    val label: ActivityLevel,
    val efficacyPercent: Int,
    val reasons: List<String>
)

data class FetchedWeatherData(
    val waterTemp: Double,
    val cloudCover: Double,
    val windDirectionChange: Double,
    val swellHeight: Double,
    val surfaceTempDelta24h: Double,
    val windSpeedMps: Double
)

data class WeatherFactor(
    val cloudCoverPercent: Double,
    val windDirectionChange: Double,
    val swellHeight: Double,
    val surfaceTempDelta24h: Double,
    val windSpeedMps: Double
) {
    fun windFactor(): Double {
        val optimal = 7.2
        val sigma = 4.0
        val gaussian = exp(-(windSpeedMps - optimal).pow(2) / (2.0 * sigma.pow(2)))
        return 0.7 + 0.5 * gaussian
    }

    fun multiplier(): Double {
        var mult = 1.0

        // Cloud cover (bonus for overcast)
        val cloudMult = 0.9 + 0.3 * (cloudCoverPercent / 100.0)
        mult *= cloudMult

        // Wind change (penalty for abrupt wind changes)
        if (windDirectionChange > 45.0) {
            mult *= 0.8
        }

        // Swell height (penalty for rough waves)
        if (swellHeight > 1.2) {
            mult *= 0.7
        } else if (swellHeight > 0.8) {
            mult *= 0.9
        }

        // Rapid drop in water temperature (penalty)
        if (surfaceTempDelta24h < -1.5) {
            mult *= 0.75
        }

        // Apply wind speed factor
        mult *= windFactor()

        return mult
    }
}

data class DailyForecast(
    val id: UUID = UUID.randomUUID(),
    val date: Date,
    val location: Location,
    
    // Astro
    val sunrise: Date?,
    val sunset: Date?,
    val moonrise: Date?,
    val moonset: Date?,
    val moonTransit: Date?,
    val moonAntiTransit: Date?,
    val moonPhase: String,
    val moonAge: Double,
    val moonIllumination: Double,
    
    // Tides
    val tides: List<TideEvent>,
    val maxTideAmplitude: Double,
    val tideCoefficient: Double,
    
    // Solunar
    val solunarPeriods: List<SolunarPeriod>,
    
    // Ratings
    val dailyActivity: ActivityLevel,
    val hourlyIntervals: List<HourlyInterval>,
    val bestWindows: List<ActivityWindow>,
    
    // Detailed factor breakdown
    val rawScore: Double,
    val moonPhaseFactor: Double,
    val moonDistanceFactor: Double,
    val tideCoeffFactor: Double,
    val solunarOverlapFactor: Double,
    val weatherFactorVal: Double,
    val waterTempFactor: Double
)
