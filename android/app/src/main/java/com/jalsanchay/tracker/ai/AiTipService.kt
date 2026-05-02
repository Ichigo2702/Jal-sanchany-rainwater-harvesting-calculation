package com.jalsanchay.tracker.ai

import com.jalsanchay.tracker.BuildConfig
import com.jalsanchay.tracker.model.MonthlyData
import com.jalsanchay.tracker.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AiTipService {
    private val client = OkHttpClient()
    private val mediaType = "application/json".toMediaType()

    fun offlinePlaceholder(settings: UserSettings): String {
        return "AI service pending backend/API setup. Based on your ${settings.roofArea.toInt()} ${settings.unit} roof and ${settings.tankCapacity.toInt()} L tank, clean filters before rain and inspect the first-flush diverter."
    }

    suspend fun getSeasonAnalysis(
        setup: UserSettings,
        monthlyData: List<MonthlyData>,
        allSaved: Double,
        impact: Double
    ): String {
        val best = monthlyData.maxByOrNull { it.totalWaterSaved }
        val prompt = """
            Water conservation advisor. Analyse rainwater data.
            Setup: ${setup.roofArea.toInt()} ${setup.unit} ${runoffLabel(setup.runoffCoeff)} roof, ${setup.tankCapacity.toInt()}L tank.
            Monthly: ${monthlyJson(monthlyData)}. Total: ${"%.1f".format(allSaved)}L (${"%.1f".format(impact)} days). Best: ${best?.monthKey.orEmpty()} ${best?.totalWaterSaved ?: 0.0}L.
            Return: 1) Two-sentence summary. 2) Three bullet observations.
            3) One milestone projection. Concise and specific.
        """.trimIndent()
        return postPrompt(prompt)
    }

    suspend fun getTips(
        setup: UserSettings,
        entryCount: Int,
        allSaved: Double,
        bestDay: Double,
        streak: Int,
        locName: String,
        season: String
    ): String {
        val prompt = """
            Rainwater expert. 4 personalised tips for:
            ${setup.roofArea.toInt()} ${setup.unit} ${runoffLabel(setup.runoffCoeff)} roof (coeff ${setup.runoffCoeff}), ${setup.tankCapacity.toInt()}L tank.
            Location: $locName. Season: $season. Saved: ${"%.1f".format(allSaved)}L, $entryCount entries.
            Best day: ${"%.1f".format(bestDay)}L. Streak: $streak days.
            Number 1-4. Max 2 sentences each. Specific numbers. No generics.
        """.trimIndent()
        return postPrompt(prompt)
    }

    suspend fun askGlossary(question: String, setup: UserSettings): String {
        val prompt = """
            Answer in plain English, 3-4 sentences. User has ${setup.roofArea.toInt()} ${setup.unit}
            ${runoffLabel(setup.runoffCoeff)} roof, ${setup.tankCapacity.toInt()}L tank in India. Question: $question
        """.trimIndent()
        return postPrompt(prompt)
    }

    private suspend fun postPrompt(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.ANTHROPIC_API_KEY.isBlank()) return@withContext "Unable to generate response."
            val body = JSONObject()
                .put("model", "claude-sonnet-4-20250514")
                .put("max_tokens", 800)
                .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
                .toString()
                .toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("content-type", "application/json")
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "Unable to generate response."
                val json = JSONObject(response.body?.string().orEmpty())
                json.getJSONArray("content").optJSONObject(0)?.optString("text") ?: "Unable to generate response."
            }
        } catch (_: Exception) {
            "Unable to generate response."
        }
    }

    private fun monthlyJson(monthlyData: List<MonthlyData>): String {
        return JSONArray(monthlyData.map {
            JSONObject()
                .put("month", it.monthKey)
                .put("rainfallMm", it.totalRainfallMm)
                .put("litres", it.totalWaterSaved)
                .put("days", it.impactDays)
        }).toString()
    }

    private fun runoffLabel(runoff: Double): String {
        return when (runoff) {
            0.85 -> "Concrete"
            0.75 -> "Tiled"
            0.90 -> "Metal Sheet"
            0.40 -> "Green Roof"
            else -> "Selected"
        }
    }
}
