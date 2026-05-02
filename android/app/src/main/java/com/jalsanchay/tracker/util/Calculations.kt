package com.jalsanchay.tracker.util

import com.jalsanchay.tracker.model.MonthlyReport
import com.jalsanchay.tracker.model.RainfallEntry
import kotlin.math.min

fun calculateWaterCollected(
    roofArea: Double,
    unit: String,
    rainfallMm: Double,
    runoffCoeff: Double,
    tankCapacityLitres: Double
): Double {
    val roofAreaFt2 = if (unit == "sqm") roofArea * 10.764 else roofArea
    val raw = roofAreaFt2 * rainfallMm * 0.0929 * runoffCoeff
    return min(raw, tankCapacityLitres)
}

fun calculateImpactDays(litres: Double): Double = litres / 135.0

fun buildMonthlyReports(entries: List<RainfallEntry>): List<MonthlyReport> {
    return entries
        .groupBy { it.date.take(7) }
        .map { (month, rows) ->
            val rainfall = rows.sumOf { it.rainfallMm }
            val water = rows.sumOf { it.litresCollected }
            MonthlyReport(month, rainfall, water, calculateImpactDays(water))
        }
        .sortedByDescending { it.monthKey }
        .take(12)
}

fun recalculateEntries(
    entries: List<RainfallEntry>,
    roofArea: Double,
    unit: String,
    runoffCoeff: Double,
    tankCapacity: Double
): List<RainfallEntry> {
    return entries.map {
        it.copy(litresCollected = calculateWaterCollected(roofArea, unit, it.rainfallMm, runoffCoeff, tankCapacity))
    }
}
