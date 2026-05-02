package com.jalsanchay.tracker

import android.os.Bundle
import android.os.Build
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.jalsanchay.tracker.data.JalSanchayDatabase
import com.jalsanchay.tracker.data.TrackerRepository
import com.jalsanchay.tracker.ui.JalSanchayApp
import com.jalsanchay.tracker.viewmodel.TrackerViewModel
import com.jalsanchay.tracker.viewmodel.TrackerViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 20)
        }
        val database = JalSanchayDatabase.getDatabase(applicationContext)
        val repository = TrackerRepository(database.rainfallDao(), database.settingsDao())

        setContent {
            val viewModel: TrackerViewModel = viewModel(factory = TrackerViewModelFactory(repository, applicationContext))
            JalSanchayApp(
                viewModel = viewModel,
                navController = rememberNavController()
            )
        }
    }
}
