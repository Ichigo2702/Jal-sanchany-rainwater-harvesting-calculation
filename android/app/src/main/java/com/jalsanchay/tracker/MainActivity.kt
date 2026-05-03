package com.jalsanchay.tracker

import android.os.Bundle
import android.os.Build
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.rememberNavController
import com.jalsanchay.tracker.ui.JalSanchayApp
import com.jalsanchay.tracker.util.WeatherSyncWorker
import com.jalsanchay.tracker.viewmodel.TrackerViewModel
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: TrackerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 20)
        }
        viewModel.seedDatabase()
        scheduleWeatherSync()
        val deepLinkScreen = intent.getStringExtra("DEEP_LINK_SCREEN")

        setContent {
            JalSanchayApp(
                viewModel = viewModel,
                navController = rememberNavController(),
                deepLinkScreen = deepLinkScreen
            )
        }
    }

    private fun scheduleWeatherSync() {
        val weatherSync = PeriodicWorkRequestBuilder<WeatherSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weather_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            weatherSync
        )
    }
}
