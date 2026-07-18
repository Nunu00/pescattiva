import Foundation

public class RulesEngine {
    
    public static func evaluateForecast(
        date: Date,
        location: Location,
        sunrise: Date?,
        sunset: Date?,
        moonrise: Date?,
        moonset: Date?,
        moonTransit: Date?,
        moonAntiTransit: Date?,
        moonAge: Double,
        tides: [TideEvent]
    ) -> DailyForecast {
        
        var calendar = Calendar.current
        calendar.timeZone = TimeZone.current
        let startOfDay = calendar.startOfDay(for: date)
        
        // 1. Calculate Tide Amplitude
        var maxAmplitude = 0.0
        if tides.count >= 2 {
            for i in 0..<(tides.count - 1) {
                let diff = abs(tides[i].height - tides[i+1].height)
                if diff > maxAmplitude {
                    maxAmplitude = diff
                }
            }
        }
        
        // 2. Generate Solunar Periods
        var periods: [SolunarPeriod] = []
        
        // Major Periods: ±1 hour around lunar transit and anti-transit
        if let transit = moonTransit {
            periods.append(SolunarPeriod(
                startTime: transit.addingTimeInterval(-3600),
                endTime: transit.addingTimeInterval(3600),
                type: .maggior,
                description: "Transito Lunare (Luna in meridiano)"
            ))
        }
        if let antiTransit = moonAntiTransit {
            periods.append(SolunarPeriod(
                startTime: antiTransit.addingTimeInterval(-3600),
                endTime: antiTransit.addingTimeInterval(3600),
                type: .maggior,
                description: "Transito Opposto (Luna in nadir)"
            ))
        }
        
        // Minor Periods: ±30 minutes around moonrise and moonset
        if let rise = moonrise {
            periods.append(SolunarPeriod(
                startTime: rise.addingTimeInterval(-1800),
                endTime: rise.addingTimeInterval(1800),
                type: .minor,
                description: "Alba Lunare (Sorgere della Luna)"
            ))
        }
        if let set = moonset {
            periods.append(SolunarPeriod(
                startTime: set.addingTimeInterval(-1800),
                endTime: set.addingTimeInterval(1800),
                type: .minor,
                description: "Tramonto Lunare (Tramonto della Luna)"
            ))
        }
        
        // 3. Mark enhanced peaks (overlapping with sunrise/sunset ±30 minutes)
        for i in 0..<periods.count {
            var isEnhanced = false
            for sunTime in [sunrise, sunset].compactMap({ $0 }) {
                // Check overlap
                let startOverlap = max(periods[i].startTime, sunTime.addingTimeInterval(-1800))
                let endOverlap = min(periods[i].endTime, sunTime.addingTimeInterval(1800))
                if startOverlap <= endOverlap {
                    isEnhanced = true
                    break
                }
            }
            periods[i].isEnhanced = isEnhanced
        }
        
        // 4. Base level derived from tide amplitude
        // Mediterranean amplitude is usually small (0.05m to 0.3m)
        let baseScore: Double
        if maxAmplitude < 0.08 {
            baseScore = 0.5 // low tide movement
        } else if maxAmplitude < 0.15 {
            baseScore = 1.0 // normal movement
        } else if maxAmplitude < 0.22 {
            baseScore = 1.5 // strong movement
        } else {
            baseScore = 2.0 // very strong movement ( Messina / spring tide )
        }
        
        // 5. Build Hourly Intervals & Calculate Scores
        var intervals: [HourlyInterval] = []
        for hour in 0..<24 {
            guard let intervalStart = calendar.date(byAdding: .hour, value: hour, to: startOfDay),
                  let intervalEnd = calendar.date(byAdding: .hour, value: hour + 1, to: startOfDay) else { continue }
            
            var hourScore = baseScore
            var isMajor = false
            var isMinor = false
            var isEnhanced = false
            
            // Check matching periods
            for period in periods {
                // If the hour overlaps with the period
                let overlapStart = max(intervalStart, period.startTime)
                let overlapEnd = min(intervalEnd, period.endTime)
                
                if overlapStart < overlapEnd {
                    let overlapDuration = overlapEnd.timeIntervalSince(overlapStart) / 3600.0
                    let bonus: Double = (period.type == .maggior) ? 1.5 : 1.0
                    hourScore += bonus * overlapDuration
                    
                    if period.type == .maggior { isMajor = true }
                    if period.type == .minor { isMinor = true }
                    if period.isEnhanced {
                        isEnhanced = true
                        hourScore += 0.5 // additional bonus for sunrise/sunset overlap
                    }
                }
            }
            
            // Tide rate of change bonus
            let hStart = TideEngine.calculateHeight(at: intervalStart, coordinate: location.coordinate)
            let hEnd = TideEngine.calculateHeight(at: intervalEnd, coordinate: location.coordinate)
            let tideMovement = abs(hEnd - hStart)
            hourScore += tideMovement * 2.0 // add bonus for active tide movement
            
            // Map score to Activity level
            let level: ActivityLevel
            if hourScore < 1.2 {
                level = .bassa
            } else if hourScore < 2.0 {
                level = .media
            } else if hourScore < 3.0 {
                level = .alta
            } else {
                level = .moltoAlta
            }
            
            intervals.append(HourlyInterval(
                hour: hour,
                startTime: intervalStart,
                endTime: intervalEnd,
                activity: level,
                score: hourScore,
                isMajorPeriod: isMajor,
                isMinorPeriod: isMinor,
                isEnhanced: isEnhanced
            ))
        }
        
        // 6. Calculate Daily Activity Rating
        // Combine tide base and moon phase bonus (New Moon and Full Moon are best)
        var dailyScore = baseScore
        
        // Moon phase bonus: New Moon (age 0-2 or 28-30) and Full Moon (age 13-16) get +1.0
        let roundedAge = round(moonAge)
        if roundedAge <= 2 || roundedAge >= 28 || (roundedAge >= 13 && roundedAge <= 16) {
            dailyScore += 1.0 // New/Full moon peak activity
        } else if (roundedAge >= 6 && roundedAge <= 8) || (roundedAge >= 21 && roundedAge <= 23) {
            dailyScore += 0.0 // Half moons (Quarters)
        } else {
            dailyScore += 0.5 // intermediate phases
        }
        
        // Add contribution from the active periods
        let activeScoreSum = intervals.filter { $0.isMajorPeriod || $0.isMinorPeriod }.map { $0.score }.reduce(0.0, +)
        let activeCount = Double(intervals.filter { $0.isMajorPeriod || $0.isMinorPeriod }.count)
        if activeCount > 0 {
            dailyScore += (activeScoreSum / activeCount) * 0.5
        }
        
        let dailyLevel: ActivityLevel
        if dailyScore < 1.8 {
            dailyLevel = .bassa
        } else if dailyScore < 2.5 {
            dailyLevel = .media
        } else if dailyScore < 3.3 {
            dailyLevel = .alta
        } else {
            dailyLevel = .moltoAlta
        }
        
        // 7. Get Moon illumination for display
        let moonIllumination = AstronomyEngine.calculateAstronomy(date: date, coordinate: location.coordinate).moonIllumination
        let moonPhase = AstronomyEngine.calculateAstronomy(date: date, coordinate: location.coordinate).moonPhase
        
        return DailyForecast(
            date: date,
            location: location,
            sunrise: sunrise,
            sunset: sunset,
            moonrise: moonrise,
            moonset: moonset,
            moonTransit: moonTransit,
            moonAntiTransit: moonAntiTransit,
            moonPhase: moonPhase,
            moonAge: moonAge,
            moonIllumination: moonIllumination,
            tides: tides,
            maxTideAmplitude: maxAmplitude,
            solunarPeriods: periods,
            dailyActivity: dailyLevel,
            hourlyIntervals: intervals
        )
    }
}
