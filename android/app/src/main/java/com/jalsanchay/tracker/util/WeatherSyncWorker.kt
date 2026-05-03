package com.jalsanchay.tracker.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jalsanchay.tracker.data.JalSanchayDatabase
import com.jalsanchay.tracker.data.TrackerRepository
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class WeatherSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = JalSanchayDatabase.getInstance(applicationContext)
        val repo = TrackerRepository(db)
        val settings = repo.setup.firstOrNull() ?: return Result.success()
        if (settings.locationName.isBlank()) return Result.success()
        val existing = db.weatherDao().getWeatherCache()
        if (existing != null) {
            val age = System.currentTimeMillis() - existing.fetchedAt
            if (age < 2 * 60 * 60 * 1000L) return Result.success()
        }
        val client = OkHttpClient.Builder()
            .callTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        return when (val result = fetchWeatherForLocation(settings.locationName, client)) {
            is WeatherResult.Success -> {
                repo.saveWeatherCache(result.resolvedName, result.forecastJson)
                Result.success()
            }
            is WeatherResult.Error -> Result.retry()
        }
    }
}
