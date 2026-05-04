package com.jalsanchay.tracker.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.jalsanchay.tracker.data.RainfallEntry
import com.jalsanchay.tracker.data.UserSetup
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

sealed class ImportResult {
    data class Success(
        val settings: UserSetup,
        val entries: List<RainfallEntry>
    ) : ImportResult()

    data class Error(val message: String) : ImportResult()
}

fun exportToJson(
    context: Context,
    settings: UserSetup,
    entries: List<RainfallEntry>
): Uri {
    val root = JSONObject()
        .put("version", 1)
        .put("exportedAt", System.currentTimeMillis())
        .put(
            "settings",
            JSONObject()
                .put("id", settings.id)
                .put("roofArea", settings.roofArea)
                .put("unit", settings.unit)
                .put("tankCapacity", settings.tankCapacity)
                .put("runoffCoefficient", settings.runoffCoefficient)
                .put("runoffLabel", settings.runoffLabel)
                .put("locationName", settings.locationName)
                .put("setupDone", settings.setupDone)
                .put("darkMode", settings.darkMode)
                .put("amoledMode", settings.amoledMode)
        )
        .put(
            "entries",
            JSONArray(entries.map {
                JSONObject()
                    .put("date", it.date)
                    .put("rainfallMm", it.rainfallMm)
                    .put("litresCollected", it.litresCollected)
                    .put("createdAt", it.createdAt)
            })
        )
    val dir = File(context.getExternalFilesDir("backups"), "")
    dir.mkdirs()
    val file = File(dir, "jalsanchay-backup-${LocalDate.now()}.json")
    file.writeText(root.toString(2))
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun importFromJson(
    context: Context,
    uri: Uri
): ImportResult {
    return try {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return ImportResult.Error("Unable to read backup file")
        val root = JSONObject(text)
        if (!root.has("version")) return ImportResult.Error("Invalid backup file")
        val settingsJson = root.getJSONObject("settings")
        val settings = UserSetup(
            id = 1,
            roofArea = settingsJson.getDouble("roofArea"),
            unit = settingsJson.getString("unit"),
            tankCapacity = settingsJson.getDouble("tankCapacity"),
            runoffCoefficient = settingsJson.getDouble("runoffCoefficient"),
            runoffLabel = settingsJson.optString("runoffLabel", "Concrete"),
            locationName = settingsJson.optString("locationName", ""),
            setupDone = settingsJson.optBoolean("setupDone", true),
            darkMode = settingsJson.optBoolean("darkMode", false),
            amoledMode = settingsJson.optBoolean("amoledMode", false)
        )
        val entriesJson = root.getJSONArray("entries")
        val entries = (0 until entriesJson.length()).map {
            val entry = entriesJson.getJSONObject(it)
            RainfallEntry(
                date = entry.getString("date"),
                rainfallMm = entry.getDouble("rainfallMm"),
                litresCollected = entry.getDouble("litresCollected"),
                createdAt = entry.optLong("createdAt", System.currentTimeMillis())
            )
        }
        ImportResult.Success(settings, entries)
    } catch (e: Exception) {
        ImportResult.Error("Import error: ${e.message}")
    }
}
