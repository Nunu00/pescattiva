package com.vincenzo.previsionipesca.engine

import com.vincenzo.previsionipesca.models.*
import java.util.*
import kotlin.math.*

object TideEngine {
    val stations = listOf(
        Location("Sibari", 39.73, 16.48, 1.5),
        Location("Trebisacce", 39.87, 16.53, 1.5),
        Location("Corigliano", 39.60, 16.52, 1.5),
        Location("Rossano", 39.57, 16.63, 1.5),
        Location("Le Castella", 38.91, 17.02, 1.5),
        Location("Soverato", 38.68, 16.55, 1.5),
        Location("Roccella Ionica", 38.32, 16.41, 1.5),
        Location("Cetraro (Tirreno)", 39.52, 15.94, 1.3),
        Location("Amantea (Tirreno)", 39.13, 16.07, 1.3),
        Location("Tropea (Tirreno)", 38.68, 15.90, 1.3),
        Location("Scilla (Tirreno)", 38.25, 15.72, 1.2)
    )

    private val speeds = mapOf(
        "M2" to 28.9841042,
        "S2" to 30.0000000,
        "N2" to 28.4397295,
        "K1" to 15.0410686,
        "O1" to 13.9430356
    )

    private val constituents = mapOf(
        "Trebisacce" to mapOf(
            "M2" to Pair(0.08, 290.0), "S2" to Pair(0.03, 310.0), "N2" to Pair(0.02, 270.0), "K1" to Pair(0.04, 60.0), "O1" to Pair(0.03, 40.0)
        ),
        "Sibari" to mapOf(
            "M2" to Pair(0.08, 290.0), "S2" to Pair(0.03, 310.0), "N2" to Pair(0.02, 270.0), "K1" to Pair(0.04, 60.0), "O1" to Pair(0.03, 40.0)
        ),
        "Corigliano" to mapOf(
            "M2" to Pair(0.08, 290.0), "S2" to Pair(0.03, 310.0), "N2" to Pair(0.02, 270.0), "K1" to Pair(0.04, 60.0), "O1" to Pair(0.03, 40.0)
        ),
        "Rossano" to mapOf(
            "M2" to Pair(0.08, 290.0), "S2" to Pair(0.03, 310.0), "N2" to Pair(0.02, 270.0), "K1" to Pair(0.04, 60.0), "O1" to Pair(0.03, 40.0)
        ),
        "Le Castella" to mapOf(
            "M2" to Pair(0.09, 295.0), "S2" to Pair(0.04, 315.0), "N2" to Pair(0.02, 275.0), "K1" to Pair(0.04, 65.0), "O1" to Pair(0.03, 45.0)
        ),
        "Soverato" to mapOf(
            "M2" to Pair(0.10, 305.0), "S2" to Pair(0.04, 320.0), "N2" to Pair(0.02, 280.0), "K1" to Pair(0.05, 70.0), "O1" to Pair(0.04, 50.0)
        ),
        "Roccella Ionica" to mapOf(
            "M2" to Pair(0.10, 305.0), "S2" to Pair(0.04, 320.0), "N2" to Pair(0.02, 280.0), "K1" to Pair(0.05, 70.0), "O1" to Pair(0.04, 50.0)
        ),
        "Cetraro (Tirreno)" to mapOf(
            "M2" to Pair(0.11, 280.0), "S2" to Pair(0.04, 295.0), "N2" to Pair(0.02, 260.0), "K1" to Pair(0.05, 50.0), "O1" to Pair(0.03, 35.0)
        ),
        "Amantea (Tirreno)" to mapOf(
            "M2" to Pair(0.11, 280.0), "S2" to Pair(0.04, 295.0), "N2" to Pair(0.02, 260.0), "K1" to Pair(0.05, 50.0), "O1" to Pair(0.03, 35.0)
        ),
        "Tropea (Tirreno)" to mapOf(
            "M2" to Pair(0.11, 280.0), "S2" to Pair(0.04, 295.0), "N2" to Pair(0.02, 260.0), "K1" to Pair(0.05, 50.0), "O1" to Pair(0.03, 35.0)
        ),
        "Scilla (Tirreno)" to mapOf(
            "M2" to Pair(0.22, 110.0), "S2" to Pair(0.08, 125.0), "N2" to Pair(0.04, 90.0), "K1" to Pair(0.07, 280.0), "O1" to Pair(0.05, 260.0)
        )
    )

    fun findNearestStation(coordinate: Coordinate): Location {
        var nearest = stations[0]
        var minDistance = Double.MAX_VALUE
        for (station in stations) {
            val latDiff = station.coordinate.latitude - coordinate.latitude
            val lonDiff = station.coordinate.longitude - coordinate.longitude
            val dist = sqrt(latDiff * latDiff + lonDiff * lonDiff)
            if (dist < minDistance) {
                minDistance = dist
                nearest = station
            }
        }
        return nearest
    }

    fun referenceSpringAmplitude(coordinate: Coordinate): Double {
        val station = findNearestStation(coordinate)
        val stationConsts = constituents[station.name] ?: constituents["Sibari"]!!
        val m2Amp = stationConsts["M2"]?.first ?: 0.08
        val s2Amp = stationConsts["S2"]?.first ?: 0.03
        return m2Amp + s2Amp
    }

    fun calculateTideCoefficient(date: Date, coordinate: Coordinate): Double {
        val station = findNearestStation(coordinate)
        val lagMs = (station.tideLagDays * 24.0 * 3600.0 * 1000.0).toLong()
        val laggedDate = Date(date.time - lagMs)
        val ast = AstronomyEngine.calculateAstronomy(laggedDate, coordinate)

        val baseCoeff = 70.0 + 30.0 * cos(4.0 * Math.PI * (ast.moonAge / 29.53059))
        val normDist = (406700.0 - ast.moonDistance) / (406700.0 - 356400.0)
        val distAdj = (normDist - 0.5) * 30.0

        return max(20.0, min(120.0, baseCoeff + distAdj))
    }

    fun calculateHeight(date: Date, coordinate: Coordinate): Double {
        val station = findNearestStation(coordinate)
        val stationConsts = constituents[station.name] ?: constituents["Sibari"]!!

        // J2000 reference: Jan 1, 2000, 12:00 UTC
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2000, Calendar.JANUARY, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val j2000Ms = calendar.timeInMillis
        val hoursSinceJ2000 = (date.time - j2000Ms) / 3600000.0

        var height = 0.0
        for ((name, speed) in speeds) {
            val const = stationConsts[name]
            if (const != null) {
                val speedRad = (speed * Math.PI) / 180.0
                val phaseRad = (const.second * Math.PI) / 180.0
                height += const.first * cos(speedRad * hoursSinceJ2000 - phaseRad)
            }
        }
        return height
    }

    fun calculateDailyTides(date: Date, coordinate: Coordinate): List<TideEvent> {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.time

        // Sample tide heights every 15 minutes for 24 hours (96 samples)
        val samples = mutableListOf<Pair<Date, Double>>()
        for (i in 0..96) {
            val sampleCal = Calendar.getInstance().apply {
                time = startOfDay
                add(Calendar.MINUTE, i * 15)
            }
            val sampleDate = sampleCal.time
            val height = calculateHeight(sampleDate, coordinate)
            samples.add(Pair(sampleDate, height))
        }

        val events = mutableListOf<TideEvent>()

        // Peak detection (first derivative sign change)
        for (i in 1 until samples.size - 1) {
            val prev = samples[i - 1].second
            val curr = samples[i].second
            val next = samples[i + 1].second

            if (curr > prev && curr > next) {
                // High Tide
                events.add(TideEvent(time = samples[i].first, height = curr, type = TideType.ALTA))
            } else if (curr < prev && curr < next) {
                // Low Tide
                events.add(TideEvent(time = samples[i].first, height = curr, type = TideType.BASSA))
            }
        }

        // De-duplicate events occurring in the same 60-minute window
        val filtered = mutableListOf<TideEvent>()
        for (event in events) {
            var tooClose = false
            for (fEvent in filtered) {
                if (abs(event.time.time - fEvent.time.time) < 3600000) {
                    tooClose = true
                    break
                }
            }
            if (!tooClose) {
                filtered.add(event)
            }
        }

        return filtered.sortedBy { it.time }
    }
}
