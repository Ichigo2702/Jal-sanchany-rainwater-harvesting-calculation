package com.jalsanchay.tracker.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jalsanchay.tracker.MainActivity
import com.jalsanchay.tracker.R
import com.jalsanchay.tracker.data.JalSanchayDatabase
import com.jalsanchay.tracker.util.Calculations
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class RainPredictionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val db = JalSanchayDatabase.getInstance(applicationContext)
        val cache = db.weatherDao().getWeatherCache() ?: return Result.success()
        val heavyRain = findHeavyRain(cache.forecastJson) ?: return Result.success()
        val today = LocalDate.now()
        if (heavyRain.date.isAfter(today.plusDays(2))) return Result.success()

        val prefs = applicationContext.getSharedPreferences("rain_prediction", Context.MODE_PRIVATE)
        if (prefs.getString("last_notification_date", "") == today.toString()) return Result.success()

        val setup = db.userSetupDao().getSetup().firstOrNull() ?: return Result.success()
        val todayLitres = db.rainfallDao().getEntryByDate(today.toString())?.litresCollected ?: 0.0
        val projected = Calculations.calculateWaterCollected(
            setup.roofArea,
            setup.unit,
            heavyRain.mm,
            setup.runoffCoefficient,
            setup.tankCapacity
        )
        val tankPct = Calculations.calculateTankPercentage(todayLitres, setup.tankCapacity).times(100).toInt()
        val dayName = heavyRain.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle("Heavy rain expected $dayName")
            .setContentText("${heavyRain.mm.toInt()}mm forecast · You could collect ${projected.toInt()}L")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${heavyRain.mm.toInt()}mm of rain is expected $dayName. Based on your setup, you could collect up to ${projected.toInt()}L. Your tank is currently at $tankPct% capacity."
                )
            )
            .setContentIntent(deepLinkToDashboard())
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(RAIN_PREDICTION_ID, notification)
        prefs.edit().putString("last_notification_date", today.toString()).apply()
        return Result.success()
    }

    private fun findHeavyRain(forecastJson: String): HeavyRain? {
        return try {
            val daily = JSONObject(forecastJson).getJSONObject("daily")
            val dates = daily.getJSONArray("time")
            val rain = daily.getJSONArray("precipitation_sum")
            (0 until dates.length())
                .map { HeavyRain(LocalDate.parse(dates.getString(it)), rain.optDouble(it, 0.0)) }
                .firstOrNull { it.mm > 20.0 }
        } catch (_: Exception) {
            null
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Rain prediction", NotificationManager.IMPORTANCE_DEFAULT)
            applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun deepLinkToDashboard(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("DEEP_LINK_SCREEN", "dashboard")
        }
        return PendingIntent.getActivity(
            applicationContext,
            RAIN_PREDICTION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private data class HeavyRain(val date: LocalDate, val mm: Double)

    companion object {
        const val CHANNEL_ID = "rain_prediction"
        const val RAIN_PREDICTION_ID = 2002
    }
}
