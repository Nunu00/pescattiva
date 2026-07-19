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
        tides: [TideEvent],
        weather: WeatherCondition = .sereno,
        waterTemp: WaterTemp = .ideale,
        wind: WindCondition = .calmo
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
        
        // 4. Build Hourly Intervals & Calculate Scores
        var intervals: [HourlyInterval] = []
        for hour in 0..<24 {
            guard let intervalStart = calendar.date(byAdding: .hour, value: hour, to: startOfDay),
                  let intervalEnd = calendar.date(byAdding: .hour, value: hour + 1, to: startOfDay) else { continue }
            
            var hourScore = 0.5 // baseline activity
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
            hourScore += tideMovement * 10.0 // add bonus for active tide movement (multiplied by 10 for Mediterranean ranges)
            
            // Map score to Activity level
            let level: ActivityLevel
            if hourScore < 1.0 {
                level = .bassa
            } else if hourScore < 1.8 {
                level = .media
            } else if hourScore < 2.8 {
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
        
        // 5. Calculate Daily Activity Rating using our calibrated solunar and tide rules
        let coeff = TideEngine.calculateTideCoefficient(at: date, coordinate: location.coordinate)
        let enhancedCount = periods.filter { $0.isEnhanced }.count
        
        let ast = AstronomyEngine.calculateAstronomy(date: date, coordinate: location.coordinate)
        let moonDistance = ast.moonDistance
        
        // --- Principled Physics-Based Multiplicative Model ---
        // 1. Moon Phase Factor (0.0 to 1.0)
        // Cosine squared model: peaks at New Moon (age=0/29.53) and Full Moon (age=14.77), troughs at Quarters
        let baseP = 0.10
        let angle = 2.0 * Double.pi * moonAge / 29.53059
        let fPhase = baseP + (1.0 - baseP) * pow(cos(angle), 2.0)
        
        // 2. Moon Distance Factor (using inverse-cube relative gravitational tidal force)
        // Normalised relative to mean distance 384,400 km
        let fDist = pow(384400.0 / moonDistance, 3.0)
        
        // 3. Tide range/coefficient Factor (0.0 to 1.0)
        let baseC = 0.30
        let fCoeff = baseC + (1.0 - baseC) * (Double(coeff) - 20.0) / 100.0
        
        // 4. Overlaps/Solunar peak alignment Factor (1.0 to 1.6)
        let wO = 0.60
        let fOverlap = 1.0 + wO * Double(enhancedCount)
        
        // Combine multiplicatively and apply environmental modulations
        var score = fPhase * fDist * fCoeff * fOverlap
        score *= weather.multiplier
        score *= waterTemp.multiplier
        score *= wind.multiplier
        
        // Map continuous score to daily activity level using optimized thresholds:
        // T1 = 0.166, T2 = 0.416, T3 = 1.288
        let dailyLevel: ActivityLevel
        if score < 0.166 {
            dailyLevel = .bassa
        } else if score < 0.416 {
            dailyLevel = .media
        } else if score < 1.288 {
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
