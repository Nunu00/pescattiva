package com.vincenzo.previsionipesca.engine

import com.vincenzo.previsionipesca.models.Coordinate
import org.shredzone.commons.suncalc.MoonIllumination
import org.shredzone.commons.suncalc.MoonPosition
import org.shredzone.commons.suncalc.MoonTimes
import org.shredzone.commons.suncalc.SunTimes
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

data class AstronomyData(
    val sunrise: Date?,
    val sunset: Date?,
    val moonrise: Date?,
    val moonset: Date?,
    val moonTransit: Date?,
    val moonAntiTransit: Date?,
    val moonAge: Double,
    val moonDistance: Double
)

object AstronomyEngine {
    fun calculateAstronomy(date: Date, coordinate: Coordinate): AstronomyData {
        // Convert Date to ZonedDateTime (using system default zone id)
        val zoneId = ZoneId.systemDefault()
        val zdt = ZonedDateTime.ofInstant(date.toInstant(), zoneId)

        // Sun rise and set
        val sunTimes = SunTimes.compute()
            .on(zdt)
            .at(coordinate.latitude, coordinate.longitude)
            .execute()
        val sunrise = sunTimes.rise?.let { Date.from(it.toInstant()) }
        val sunset = sunTimes.set?.let { Date.from(it.toInstant()) }

        // Moon rise and set
        val moonTimes = MoonTimes.compute()
            .on(zdt)
            .at(coordinate.latitude, coordinate.longitude)
            .execute()
        val moonrise = moonTimes.rise?.let { Date.from(it.toInstant()) }
        val moonset = moonTimes.set?.let { Date.from(it.toInstant()) }

        // Exact Moon Transit and Anti-Transit (Nadir) using altitude search
        val moonTransit = findMoonTransit(zdt, coordinate.latitude, coordinate.longitude)
        val moonAntiTransit = findMoonAntiTransit(zdt, coordinate.latitude, coordinate.longitude)

        // Moon Age: shredzone MoonIllumination getPhase() range [-180, 180] (where -180 is new moon, 0 is full moon)
        val illumination = MoonIllumination.compute()
            .on(zdt)
            .execute()
        val phaseDeg = illumination.phase + 180.0 // range [0, 360]
        val moonAge = (phaseDeg / 360.0) * 29.53059

        // Moon Distance in kilometers
        val moonPos = MoonPosition.compute()
            .on(zdt)
            .at(coordinate.latitude, coordinate.longitude)
            .execute()
        val moonDistance = moonPos.distance

        return AstronomyData(
            sunrise = sunrise,
            sunset = sunset,
            moonrise = moonrise,
            moonset = moonset,
            moonTransit = moonTransit,
            moonAntiTransit = moonAntiTransit,
            moonAge = moonAge,
            moonDistance = moonDistance
        )
    }

    private fun findMoonTransit(zdt: ZonedDateTime, latitude: Double, longitude: Double): Date {
        val baseTimeMs = zdt.toLocalDate().atStartOfDay(zdt.zone).toInstant().toEpochMilli()
        var maxAlt = -Double.MAX_VALUE
        var transitTimeMs = baseTimeMs

        // Coarse search every 30 minutes
        for (i in 0..48) {
            val t = baseTimeMs + i * 30 * 60 * 1000L
            val testZdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(t), zdt.zone)
            val alt = MoonPosition.compute()
                .on(testZdt)
                .at(latitude, longitude)
                .execute()
                .altitude
            if (alt > maxAlt) {
                maxAlt = alt
                transitTimeMs = t
            }
        }

        // Fine search around the candidate ±15 minutes (in 1-minute steps)
        var fineTransitTimeMs = transitTimeMs
        maxAlt = -Double.MAX_VALUE
        for (m in -15..15) {
            val t = transitTimeMs + m * 60 * 1000L
            val testZdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(t), zdt.zone)
            val alt = MoonPosition.compute()
                .on(testZdt)
                .at(latitude, longitude)
                .execute()
                .altitude
            if (alt > maxAlt) {
                maxAlt = alt
                fineTransitTimeMs = t
            }
        }

        return Date(fineTransitTimeMs)
    }

    private fun findMoonAntiTransit(zdt: ZonedDateTime, latitude: Double, longitude: Double): Date {
        val baseTimeMs = zdt.toLocalDate().atStartOfDay(zdt.zone).toInstant().toEpochMilli()
        var minAlt = Double.MAX_VALUE
        var antiTransitTimeMs = baseTimeMs

        // Coarse search every 30 minutes
        for (i in 0..48) {
            val t = baseTimeMs + i * 30 * 60 * 1000L
            val testZdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(t), zdt.zone)
            val alt = MoonPosition.compute()
                .on(testZdt)
                .at(latitude, longitude)
                .execute()
                .altitude
            if (alt < minAlt) {
                minAlt = alt
                antiTransitTimeMs = t
            }
        }

        // Fine search around the candidate ±15 minutes (in 1-minute steps)
        var fineAntiTransitTimeMs = antiTransitTimeMs
        minAlt = Double.MAX_VALUE
        for (m in -15..15) {
            val t = antiTransitTimeMs + m * 60 * 1000L
            val testZdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(t), zdt.zone)
            val alt = MoonPosition.compute()
                .on(testZdt)
                .at(latitude, longitude)
                .execute()
                .altitude
            if (alt < minAlt) {
                minAlt = alt
                fineAntiTransitTimeMs = t
            }
        }

        return Date(fineAntiTransitTimeMs)
    }
}
