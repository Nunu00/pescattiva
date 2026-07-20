package com.vincenzo.previsionipesca.engine

import com.vincenzo.previsionipesca.models.FetchedWeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

object WeatherService {

    private suspend fun getUrlContent(urlString: String): String = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        
        val code = conn.responseCode
        if (code != 200) {
            throw Exception("HTTP Error $code")
        }
        
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line)
        }
        reader.close()
        conn.disconnect()
        sb.toString()
    }

    suspend fun fetch7DayWeather(latitude: Double, longitude: Double): Map<String, FetchedWeatherData> = withContext(Dispatchers.IO) {
        val forecastUrl = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&hourly=cloud_cover,wind_direction_10m,temperature_2m,wind_speed_10m&wind_speed_unit=ms&forecast_days=7"
        val marineUrl = "https://marine-api.open-meteo.com/v1/marine?latitude=$latitude&longitude=$longitude&hourly=sea_surface_temperature,wave_height&forecast_days=7"
        
        val forecastResponse = getUrlContent(forecastUrl)
        val forecastJson = JSONObject(forecastResponse)
        val hourlyForecast = forecastJson.optJSONObject("hourly")
        
        val clouds = optDoubleArray(hourlyForecast, "cloud_cover")
        val winds = optDoubleArray(hourlyForecast, "wind_direction_10m")
        val temps = optDoubleArray(hourlyForecast, "temperature_2m")
        val windSpeeds = optDoubleArray(hourlyForecast, "wind_speed_10m")
        
        // Parse time strings
        val timeStrings = mutableListOf<String>()
        val timeJsonArray = hourlyForecast?.optJSONArray("time")
        if (timeJsonArray != null) {
            for (i in 0 until timeJsonArray.length()) {
                timeStrings.add(timeJsonArray.optString(i, ""))
            }
        }

        var ssts = DoubleArray(0)
        var waves = DoubleArray(0)
        
        try {
            val marineResponse = getUrlContent(marineUrl)
            val marineJson = JSONObject(marineResponse)
            val hourlyMarine = marineJson.optJSONObject("hourly")
            ssts = optDoubleArray(hourlyMarine, "sea_surface_temperature")
            waves = optDoubleArray(hourlyMarine, "wave_height")
        } catch (e: Exception) {
            e.printStackTrace()
            // marine API can fail if coordinates are inland
        }
        
        val result = mutableMapOf<String, FetchedWeatherData>()
        
        for (day in 0 until 7) {
            val startIndex = day * 24
            val endIndex = (day + 1) * 24
            
            if (timeStrings.size <= startIndex) continue
            val dateStr = timeStrings[startIndex].take(10) // "yyyy-MM-dd"
            
            // Slice hourly values
            val cloudsSlice = sliceArray(clouds, startIndex, endIndex)
            val windsSlice = sliceArray(winds, startIndex, endIndex)
            val tempsSlice = sliceArray(temps, startIndex, endIndex)
            val sstSlice = sliceArray(ssts, startIndex, endIndex)
            val waveSlice = sliceArray(waves, startIndex, endIndex)
            val windSpeedSlice = sliceArray(windSpeeds, startIndex, endIndex)
            
            val avgCloud = if (cloudsSlice.isEmpty()) 20.0 else cloudsSlice.average()
            
            // Wind direction change over last 3 hours of mid-day
            var windChange = 10.0
            if (windsSlice.size > 12) {
                val change = abs(windsSlice[12] - windsSlice[9])
                windChange = if (change > 180.0) 360.0 - change else change
            }
            
            val tempDelta = if (tempsSlice.size >= 24) tempsSlice[23] - tempsSlice[0] else 0.0
            val avgSst = if (sstSlice.isEmpty()) 20.0 else sstSlice.average()
            val maxWave = if (waveSlice.isEmpty()) 0.2 else waveSlice.maxOrNull() ?: 0.2
            val avgWind = if (windSpeedSlice.isEmpty()) 4.0 else windSpeedSlice.average()
            
            result[dateStr] = FetchedWeatherData(
                waterTemp = avgSst,
                cloudCover = avgCloud,
                windDirectionChange = windChange,
                swellHeight = maxWave,
                surfaceTempDelta24h = tempDelta,
                windSpeedMps = avgWind
            )
        }
        
        result
    }

    private fun optDoubleArray(obj: JSONObject?, key: String): DoubleArray {
        val arr = obj?.optJSONArray(key) ?: return DoubleArray(0)
        val res = DoubleArray(arr.length())
        for (i in 0 until arr.length()) {
            res[i] = arr.optDouble(i, 0.0)
        }
        return res
    }

    private fun sliceArray(arr: DoubleArray, start: Int, end: Int): DoubleArray {
        if (arr.size <= start) return DoubleArray(0)
        val actualEnd = minOf(end, arr.size)
        return arr.copyOfRange(start, actualEnd)
    }
}
