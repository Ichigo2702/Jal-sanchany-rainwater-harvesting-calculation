package com.jalsanchay.tracker.util

import com.jalsanchay.tracker.data.RainfallEntry
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

object Calculations {

    fun calculateWaterCollected(
        roofArea: Double,
        unit: String,
        rainfallMm: Double,
        runoffCoeff: Double,
        tankCapacity: Double
    ): Double {
        val ft2 = if (unit == "sqm") roofArea * 10.764 else roofArea
        val raw = ft2 * rainfallMm * 0.0929 * runoffCoeff
        return minOf(raw, tankCapacity)
    }

    fun calculateImpactScore(totalLitres: Double): Double =
        totalLitres / 135.0

    fun calculateTankPercentage(
        todayLitres: Double,
        tankCapacity: Double
    ): Float =
        (todayLitres / tankCapacity).coerceIn(0.0, 1.0).toFloat()

    fun calculateStreak(entries: List<RainfallEntry>): Int {
        if (entries.isEmpty()) return 0
        val today = LocalDate.now()
        val dateSet = entries.map { it.date }.toSet()
        var streak = 0
        var current = today
        while (dateSet.contains(current.toString())) {
            streak++
            current = current.minusDays(1)
        }
        return streak
    }

    fun getBestDayLitres(entries: List<RainfallEntry>): Double =
        entries.maxOfOrNull { it.litresCollected } ?: 0.0

    fun getAvgMonthlyLitres(entries: List<RainfallEntry>): Double {
        if (entries.isEmpty()) return 0.0
        val months = entries.map { it.date.substring(0, 7) }.toSet()
        if (months.size < 2) return 0.0
        return entries.sumOf { it.litresCollected } / months.size
    }

    fun getDryDaysCount(entries: List<RainfallEntry>): Int =
        entries.count { it.rainfallMm == 0.0 }

    data class MonthlyTotal(
        val monthKey: String,
        val displayName: String,
        val totalMm: Double,
        val totalLitres: Double,
        val impactDays: Double
    ) {
        val totalRainfallMm: Double get() = totalMm
        val totalWaterSaved: Double get() = totalLitres
    }

    fun getMonthlyTotals(entries: List<RainfallEntry>): List<MonthlyTotal> {
        val grouped = entries.groupBy { it.date.substring(0, 7) }
        return grouped.entries
            .sortedBy { it.key }
            .map { (monthKey, monthEntries) ->
                val totalMm = monthEntries.sumOf { it.rainfallMm }
                val totalLitres = monthEntries.sumOf { it.litresCollected }
                MonthlyTotal(
                    monthKey = monthKey,
                    displayName = formatMonthKey(monthKey),
                    totalMm = totalMm,
                    totalLitres = totalLitres,
                    impactDays = calculateImpactScore(totalLitres)
                )
            }
    }

    fun formatMonthKey(monthKey: String): String {
        val (year, month) = monthKey.split("-").map { it.toInt() }
        val monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return "$monthName $year"
    }

    fun getNextMilestone(totalLitres: Double): Double? {
        val milestones = listOf(500.0, 1000.0, 5000.0, 10000.0, 25000.0, 50000.0)
        return milestones.firstOrNull { it > totalLitres }
    }

    fun getCurrentMonthProgress(
        entries: List<RainfallEntry>,
        currentMonthKey: String
    ): Float {
        val avg = getAvgMonthlyLitres(entries)
        if (avg == 0.0) return 0f
        val currentTotal = entries
            .filter { it.date.startsWith(currentMonthKey) }
            .sumOf { it.litresCollected }
        return (currentTotal / avg).toFloat().coerceAtLeast(0f)
    }
}

fun calculateImpactDays(totalLitres: Double): Double =
    Calculations.calculateImpactScore(totalLitres)
