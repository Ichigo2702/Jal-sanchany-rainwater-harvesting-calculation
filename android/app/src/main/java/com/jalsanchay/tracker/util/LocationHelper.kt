package com.jalsanchay.tracker.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.coroutines.resume

class LocationHelper(private val context: Context, private val httpClient: OkHttpClient = OkHttpClient()) {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentCityName(): String? {
        return try {
            if (!hasLocationPermission()) return null

            // Try getCurrentLocation first (forces a fresh GPS fix)
            val location = try {
                suspendCancellableCoroutine<android.location.Location?> { continuation ->
                    val cts = CancellationTokenSource()
                    LocationServices.getFusedLocationProviderClient(context)
                        .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                        .addOnSuccessListener { continuation.resume(it) }
                        .addOnFailureListener { continuation.resume(null) }
                    continuation.invokeOnCancellation { cts.cancel() }
                }
            } catch (_: Exception) { null }
            // Fallback to lastLocation if getCurrentLocation returns null
                ?: suspendCancellableCoroutine<android.location.Location?> { continuation ->
                    LocationServices.getFusedLocationProviderClient(context)
                        .lastLocation
                        .addOnSuccessListener { loc -> continuation.resume(loc) }
                        .addOnFailureListener { continuation.resume(null) }
                }

            location ?: return null

            val request = Request.Builder()
                .url("https://nominatim.openstreetmap.org/reverse?lat=${location.latitude}&lon=${location.longitude}&format=json&zoom=10")
                .header("User-Agent", "JalSanchayTracker/1.0")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val address = JSONObject(response.body?.string().orEmpty()).optJSONObject("address") ?: return null
                address.optString("city")
                    .ifBlank { address.optString("town") }
                    .ifBlank { address.optString("suburb") }
                    .ifBlank { null }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}
