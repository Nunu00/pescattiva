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

        // Moon rise, set, and meridian transit
        val moonTimes = MoonTimes.compute()
            .on(zdt)
            .at(coordinate.latitude, coordinate.longitude)
            .execute()
        val moonrise = moonTimes.rise?.let { Date.from(it.toInstant()) }
        val moonset = moonTimes.set?.let { Date.from(it.toInstant()) }
        val moonTransit = moonTimes.transit?.let { Date.from(it.toInstant()) }

        // Anti-transit (Nadir transit): calculated as the transit at the antipodal longitude (+180 degrees)
        val antipodalLongitude = ((coordinate.longitude + 180.0) + 180.0) % 360.0 - 180.0
        val antipodalMoonTimes = MoonTimes.compute()
            .on(zdt)
            .at(coordinate.latitude, antipodalLongitude)
            .execute()
        val moonAntiTransit = antipodalMoonTimes.transit?.let { Date.from(it.toInstant()) }

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
}
