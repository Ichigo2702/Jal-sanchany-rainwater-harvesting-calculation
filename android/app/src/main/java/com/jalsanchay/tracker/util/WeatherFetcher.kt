package com.jalsanchay.tracker.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class ForecastData(
    val days: List<RainDay>
)

data class RainDay(
    val date: String,
    val precipitationSum: Double
)

sealed class WeatherResult {
    data class Success(
        val resolvedName: String,
        val forecastJson: String,
        val forecast: ForecastData
    ) : WeatherResult()

    data class Error(val message: String) : WeatherResult()
}

suspend fun fetchWeatherForLocation(
    locationName: String,
    httpClient: OkHttpClient
): WeatherResult = withContext(Dispatchers.IO) {
    try {
        // Step 1: Geocode the location name to coordinates
        val geoRequest = Request.Builder()
            .url("https://geocoding-api.open-meteo.com/v1/search?name=${locationName.trim()}&count=1")
            .build()
        val geoResponse = httpClient.newCall(geoRequest).execute()
        if (!geoResponse.isSuccessful) return@withContext WeatherResult.Error("Could not search for location (${geoResponse.code})")
        val geoJson = JSONObject(geoResponse.body?.string().orEmpty())
        val first = geoJson.optJSONArray("results")?.optJSONObject(0)
            ?: return@withContext WeatherResult.Error("'$locationName' not found. Try a different city name.")
        val latitude = first.getDouble("latitude")
        val longitude = first.getDouble("longitude")
        val resolved = first.optString("name", locationName)

        // Step 2: Fetch 7-day forecast from Open-Meteo
        val forecastRequest = Request.Builder()
            .url("https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&daily=precipitation_sum&forecast_days=7&timezone=auto")
            .build()
        val forecastResponse = httpClient.newCall(forecastRequest).execute()
        if (!forecastResponse.isSuccessful) return@withContext WeatherResult.Error("Forecast unavailable (${forecastResponse.code})")
        val forecastJson = forecastResponse.body?.string().orEmpty()
        val daily = JSONObject(forecastJson).getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val rain = daily.getJSONArray("precipitation_sum")
        val days = (0 until dates.length()).map {
            RainDay(dates.getString(it), rain.optDouble(it, 0.0))
        }
        WeatherResult.Success(resolved, forecastJson, ForecastData(days))
    } catch (e: java.net.UnknownHostException) {
        WeatherResult.Error("No internet connection")
    } catch (e: java.net.SocketTimeoutException) {
        WeatherResult.Error("Connection timed out. Try again.")
    } catch (_: Exception) {
        WeatherResult.Error("Forecast unavailable")
    }
}
