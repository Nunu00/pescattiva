import Foundation

public struct Coordinate: Hashable, Codable {
    public var latitude: Double
    public var longitude: Double
    public var name: String?
    
    public init(latitude: Double, longitude: Double, name: String? = nil) {
        self.latitude = latitude
        self.longitude = longitude
        self.name = name
    }
}

public struct Location: Hashable, Identifiable, Codable {
    public var id: UUID = UUID()
    public var name: String
    public var coordinate: Coordinate
    
    public init(name: String, latitude: Double, longitude: Double) {
        self.name = name
        self.coordinate = Coordinate(latitude: latitude, longitude: longitude, name: name)
    }
}

public enum TideType: String, Codable {
    case alta = "Alta"
    case bassa = "Bassa"
}

public struct TideEvent: Identifiable, Codable, Hashable {
    public var id: UUID = UUID()
    public var time: Date
    public var height: Double // in meters
    public var type: TideType
}

public enum SolunarType: String, Codable {
    case maggior = "Maggiore"
    case minor = "Minore"
}

public struct SolunarPeriod: Identifiable, Codable, Hashable {
    public var id: UUID = UUID()
    public var startTime: Date
    public var endTime: Date
    public var type: SolunarType
    public var description: String // e.g. "Transito lunare", "Alba lunare"
    public var isEnhanced: Bool = false // overlaps with sunrise/sunset
}

public enum ActivityLevel: String, Codable, CaseIterable {
    case bassa = "Bassa"
    case media = "Media"
    case alta = "Alta"
    case moltoAlta = "Molto Alta"
    
    public var score: Int {
        switch self {
        case .bassa: return 0
        case .media: return 1
        case .alta: return 2
        case .moltoAlta: return 3
        }
    }
    
    public var description: String {
        switch self {
        case .bassa: return "Attività Bassa"
        case .media: return "Attività Media"
        case .alta: return "Attività Alta 🎣"
        case .moltoAlta: return "Attività Eccezionale! 🔥"
        }
    }
}

public struct HourlyInterval: Identifiable, Codable, Hashable {
    public var id: UUID = UUID()
    public var hour: Int // 0-23
    public var startTime: Date
    public var endTime: Date
    public var activity: ActivityLevel
    public var score: Double
    public var isMajorPeriod: Bool = false
    public var isMinorPeriod: Bool = false
    public var isEnhanced: Bool = false
}

public struct DailyForecast: Identifiable, Codable {
    public var id: UUID = UUID()
    public var date: Date
    public var location: Location
    
    // Astro
    public var sunrise: Date?
    public var sunset: Date?
    public var moonrise: Date?
    public var moonset: Date?
    public var moonTransit: Date?
    public var moonAntiTransit: Date?
    public var moonPhase: String
    public var moonAge: Double
    public var moonIllumination: Double // 0-100%
    
    // Tides
    public var tides: [TideEvent]
    public var maxTideAmplitude: Double
    
    // Solunar
    public var solunarPeriods: [SolunarPeriod]
    
    // Ratings
    public var dailyActivity: ActivityLevel
    public var hourlyIntervals: [HourlyInterval]
}
