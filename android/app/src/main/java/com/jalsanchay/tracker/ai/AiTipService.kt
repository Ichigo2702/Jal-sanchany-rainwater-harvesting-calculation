package com.jalsanchay.tracker.ai

import com.jalsanchay.tracker.BuildConfig
import com.jalsanchay.tracker.model.MonthlyData
import com.jalsanchay.tracker.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiTipService {
    private val client = OkHttpClient.Builder()
        .callTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val mediaType = "application/json".toMediaType()

    companion object {
        private val cache = mutableMapOf<String, Pair<String, Long>>()
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
        private const val TIMEOUT_MS = 30_000L

        fun invalidateEntryRelatedCache() {
            cache.remove("tips")
            cache.remove("seasonAnalysis")
        }
    }

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
        return postPrompt("seasonAnalysis", prompt)
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
        return postPrompt("tips", prompt)
    }

    suspend fun askGlossary(question: String, setup: UserSettings): String {
        val prompt = """
            Answer in plain English, 3-4 sentences. User has ${setup.roofArea.toInt()} ${setup.unit}
            ${runoffLabel(setup.runoffCoeff)} roof, ${setup.tankCapacity.toInt()}L tank in India. Question: $question
        """.trimIndent()
        return postPrompt("glossary_${question.take(30)}", prompt)
    }

    private fun getCached(key: String): String? {
        val cached = cache[key] ?: return null
        return if (System.currentTimeMillis() - cached.second < CACHE_TTL_MS) cached.first else null
    }

    private fun setCached(key: String, value: String) {
        cache[key] = value to System.currentTimeMillis()
    }

    private suspend fun postPrompt(cacheKey: String, prompt: String): String = withContext(Dispatchers.IO) {
        try {
            getCached(cacheKey)?.let { return@withContext it }
            val result = sendPromptToGemini(prompt)
            if (result != "Unable to generate response.") {
                setCached(cacheKey, result)
            }
            result
        } catch (_: SocketTimeoutException) {
            "Request timed out. Please try again."
        } catch (_: IOException) {
            "Unable to connect. Check your connection."
        } catch (_: Exception) {
            "Unable to generate response."
        }
    }

    suspend fun sendPromptToGemini(userInput: String): String = withContext(Dispatchers.IO) {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            android.util.Log.e("AiTipService", "GEMINI_API_KEY is missing")
            return@withContext "Unable to generate response."
        }

        val bodyString = JSONObject()
            .put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", userInput)
                ))
            ))
            .toString()

        val maxRetries = 2
        var lastError = "Unable to generate response."

        for (attempt in 0..maxRetries) {
            if (attempt > 0) {
                val delayMs = (2000L * (1 shl (attempt - 1)))  // 2s, 4s
                android.util.Log.d("AiTipService", "Rate limited, retrying in ${delayMs}ms (attempt ${attempt + 1}/${maxRetries + 1})")
                Thread.sleep(delayMs)
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=${BuildConfig.GEMINI_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(bodyString.toRequestBody(mediaType))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()

                    android.util.Log.d("AiTipService", "Gemini Response Code: ${response.code}")
                    android.util.Log.d("AiTipService", "Gemini Response Body: $responseBody")

                    if (response.code == 429) {
                        lastError = "Rate limit reached. Please wait a minute and try again."
                        return@use  // continue to next retry attempt
                    }

                    if (!response.isSuccessful) {
                        lastError = when (response.code) {
                            400 -> "Invalid request. Please try a different question."
                            403 -> "API key not authorized. Check your Gemini API key."
                            404 -> "AI model not found. App update may be needed."
                            500, 503 -> "Google AI service is temporarily down. Try again later."
                            else -> "Service error (${response.code}). Please try again."
                        }
                        android.util.Log.e("AiTipService", "Gemini error ${response.code}: $responseBody")
                        return@withContext lastError
                    }

                    val json = JSONObject(responseBody)
                    val text = json.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")

                    if (text.isNullOrBlank()) {
                        android.util.Log.w("AiTipService", "Gemini returned empty text")
                        return@withContext "Unable to generate response."
                    } else {
                        return@withContext text.trim()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AiTipService", "Gemini API call failed", e)
                lastError = "Unable to generate response."
                return@withContext lastError
            }
        }

        lastError  // all retries exhausted (only for 429)
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
