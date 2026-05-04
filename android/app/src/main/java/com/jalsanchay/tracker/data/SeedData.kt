package com.jalsanchay.tracker.data

import com.jalsanchay.tracker.util.Calculations

object SeedData {

    val DEFAULT_SETUP = UserSetup(
        id = 1,
        roofArea = 1000.0,
        unit = "sqft",
        tankCapacity = 3000.0,
        runoffCoefficient = 0.85,
        runoffLabel = "Concrete",
        locationName = "",
        setupDone = true
    )

    private fun litres(rainfallMm: Double): Double {
        return Calculations.calculateWaterCollected(
            roofArea = DEFAULT_SETUP.roofArea,
            unit = DEFAULT_SETUP.unit,
            rainfallMm = rainfallMm,
            runoffCoeff = DEFAULT_SETUP.runoffCoefficient,
            tankCapacity = DEFAULT_SETUP.tankCapacity
        )
    }

    // Realistic data spanning May 2025 → May 2026
    // Covers all Indian seasons: Pre-Monsoon, SW Monsoon, NE Monsoon, Dry
    val ENTRIES = listOf(
        // ── Pre-Monsoon 2025 (May) ──
        RainfallEntry(date = "2025-05-03", rainfallMm = 6.0, litresCollected = litres(6.0)),
        RainfallEntry(date = "2025-05-08", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2025-05-12", rainfallMm = 12.0, litresCollected = litres(12.0)),
        RainfallEntry(date = "2025-05-18", rainfallMm = 14.0, litresCollected = litres(14.0)),
        RainfallEntry(date = "2025-05-22", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2025-05-28", rainfallMm = 9.0, litresCollected = litres(9.0)),

        // ── SW Monsoon 2025 (June) ──
        RainfallEntry(date = "2025-06-02", rainfallMm = 18.0, litresCollected = litres(18.0)),
        RainfallEntry(date = "2025-06-05", rainfallMm = 28.0, litresCollected = litres(28.0)),
        RainfallEntry(date = "2025-06-09", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2025-06-12", rainfallMm = 35.0, litresCollected = litres(35.0)),
        RainfallEntry(date = "2025-06-16", rainfallMm = 22.0, litresCollected = litres(22.0)),
        RainfallEntry(date = "2025-06-20", rainfallMm = 42.0, litresCollected = litres(42.0)),
        RainfallEntry(date = "2025-06-25", rainfallMm = 15.0, litresCollected = litres(15.0)),
        RainfallEntry(date = "2025-06-28", rainfallMm = 41.0, litresCollected = litres(41.0)),

        // ── SW Monsoon 2025 (July — peak) ──
        RainfallEntry(date = "2025-07-01", rainfallMm = 48.0, litresCollected = litres(48.0)),
        RainfallEntry(date = "2025-07-03", rainfallMm = 52.0, litresCollected = litres(52.0)),
        RainfallEntry(date = "2025-07-06", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2025-07-09", rainfallMm = 38.0, litresCollected = litres(38.0)),
        RainfallEntry(date = "2025-07-12", rainfallMm = 65.0, litresCollected = litres(65.0)),
        RainfallEntry(date = "2025-07-15", rainfallMm = 60.0, litresCollected = litres(60.0)),
        RainfallEntry(date = "2025-07-18", rainfallMm = 33.0, litresCollected = litres(33.0)),
        RainfallEntry(date = "2025-07-22", rainfallMm = 45.0, litresCollected = litres(45.0)),
        RainfallEntry(date = "2025-07-25", rainfallMm = 28.0, litresCollected = litres(28.0)),
        RainfallEntry(date = "2025-07-29", rainfallMm = 55.0, litresCollected = litres(55.0)),

        // ── SW Monsoon 2025 (August) ──
        RainfallEntry(date = "2025-08-02", rainfallMm = 48.0, litresCollected = litres(48.0)),
        RainfallEntry(date = "2025-08-05", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2025-08-08", rainfallMm = 36.0, litresCollected = litres(36.0)),
        RainfallEntry(date = "2025-08-11", rainfallMm = 55.0, litresCollected = litres(55.0)),
        RainfallEntry(date = "2025-08-15", rainfallMm = 42.0, litresCollected = litres(42.0)),
        RainfallEntry(date = "2025-08-19", rainfallMm = 32.0, litresCollected = litres(32.0)),
        RainfallEntry(date = "2025-08-23", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2025-08-27", rainfallMm = 40.0, litresCollected = litres(40.0)),
        RainfallEntry(date = "2025-08-30", rainfallMm = 25.0, litresCollected = litres(25.0)),

        // ── SW Monsoon 2025 (September — retreating) ──
        RainfallEntry(date = "2025-09-02", rainfallMm = 20.0, litresCollected = litres(20.0)),
        RainfallEntry(date = "2025-09-04", rainfallMm = 30.0, litresCollected = litres(30.0)),
        RainfallEntry(date = "2025-09-08", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2025-09-12", rainfallMm = 15.0, litresCollected = litres(15.0)),
        RainfallEntry(date = "2025-09-15", rainfallMm = 18.0, litresCollected = litres(18.0)),
        RainfallEntry(date = "2025-09-20", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2025-09-24", rainfallMm = 22.0, litresCollected = litres(22.0)),
        RainfallEntry(date = "2025-09-28", rainfallMm = 10.0, litresCollected = litres(10.0)),

        // ── NE Monsoon 2025 (October) ──
        RainfallEntry(date = "2025-10-03", rainfallMm = 12.0, litresCollected = litres(12.0)),
        RainfallEntry(date = "2025-10-08", rainfallMm = 25.0, litresCollected = litres(25.0)),
        RainfallEntry(date = "2025-10-14", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2025-10-20", rainfallMm = 15.0, litresCollected = litres(15.0)),
        RainfallEntry(date = "2025-10-26", rainfallMm = 30.0, litresCollected = litres(30.0)),
        RainfallEntry(date = "2025-10-30", rainfallMm = 8.0, litresCollected = litres(8.0)),

        // ── NE Monsoon 2025 (November) ──
        RainfallEntry(date = "2025-11-05", rainfallMm = 20.0, litresCollected = litres(20.0)),
        RainfallEntry(date = "2025-11-10", rainfallMm = 35.0, litresCollected = litres(35.0)),
        RainfallEntry(date = "2025-11-15", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2025-11-18", rainfallMm = 12.0, litresCollected = litres(12.0)),
        RainfallEntry(date = "2025-11-24", rainfallMm = 28.0, litresCollected = litres(28.0)),
        RainfallEntry(date = "2025-11-28", rainfallMm = 15.0, litresCollected = litres(15.0)),

        // ── NE Monsoon 2025 (December) ──
        RainfallEntry(date = "2025-12-04", rainfallMm = 18.0, litresCollected = litres(18.0)),
        RainfallEntry(date = "2025-12-10", rainfallMm = 6.0, litresCollected = litres(6.0)),
        RainfallEntry(date = "2025-12-15", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2025-12-22", rainfallMm = 10.0, litresCollected = litres(10.0)),
        RainfallEntry(date = "2025-12-28", rainfallMm = 4.0, litresCollected = litres(4.0)),

        // ── Dry Season 2026 (January) ──
        RainfallEntry(date = "2026-01-05", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2026-01-14", rainfallMm = 4.0, litresCollected = litres(4.0)),
        RainfallEntry(date = "2026-01-22", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2026-01-28", rainfallMm = 2.0, litresCollected = litres(2.0)),

        // ── Dry Season 2026 (February) ──
        RainfallEntry(date = "2026-02-06", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2026-02-14", rainfallMm = 5.0, litresCollected = litres(5.0)),
        RainfallEntry(date = "2026-02-20", rainfallMm = 10.0, litresCollected = litres(10.0)),
        RainfallEntry(date = "2026-02-28", rainfallMm = 0.0, litresCollected = 0.0),

        // ── Pre-Monsoon 2026 (March) ──
        RainfallEntry(date = "2026-03-05", rainfallMm = 8.0, litresCollected = litres(8.0)),
        RainfallEntry(date = "2026-03-12", rainfallMm = 16.0, litresCollected = litres(16.0)),
        RainfallEntry(date = "2026-03-18", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2026-03-25", rainfallMm = 12.0, litresCollected = litres(12.0)),

        // ── Pre-Monsoon 2026 (April) ──
        RainfallEntry(date = "2026-04-02", rainfallMm = 10.0, litresCollected = litres(10.0)),
        RainfallEntry(date = "2026-04-08", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2026-04-12", rainfallMm = 18.0, litresCollected = litres(18.0)),
        RainfallEntry(date = "2026-04-18", rainfallMm = 22.0, litresCollected = litres(22.0)),
        RainfallEntry(date = "2026-04-24", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2026-04-28", rainfallMm = 15.0, litresCollected = litres(15.0)),
        RainfallEntry(date = "2026-04-30", rainfallMm = 25.0, litresCollected = litres(25.0)),

        // ── Pre-Monsoon 2026 (May — current month) ──
        RainfallEntry(date = "2026-05-01", rainfallMm = 8.0, litresCollected = litres(8.0)),
        RainfallEntry(date = "2026-05-03", rainfallMm = 14.0, litresCollected = litres(14.0)),
        RainfallEntry(date = "2026-05-04", rainfallMm = 0.0, litresCollected = 0.0)
    )
}
