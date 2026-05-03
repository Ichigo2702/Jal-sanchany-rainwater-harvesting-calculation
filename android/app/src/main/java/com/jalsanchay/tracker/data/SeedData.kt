package com.jalsanchay.tracker.data

object SeedData {

    val DEFAULT_SETUP = UserSetup(
        id = 1,
        roofArea = 800.0,
        unit = "sqft",
        tankCapacity = 3000.0,
        runoffCoefficient = 0.85,
        runoffLabel = "Concrete",
        locationName = ""
    )

    val ENTRIES = listOf(
        RainfallEntry(date = "2025-05-10", rainfallMm = 8.0, litresCollected = 504.7),
        RainfallEntry(date = "2025-05-18", rainfallMm = 14.0, litresCollected = 882.5),
        RainfallEntry(date = "2025-06-05", rainfallMm = 28.0, litresCollected = 1765.0),
        RainfallEntry(date = "2025-06-12", rainfallMm = 35.0, litresCollected = 2206.3),
        RainfallEntry(date = "2025-06-20", rainfallMm = 22.0, litresCollected = 1386.9),
        RainfallEntry(date = "2025-06-28", rainfallMm = 41.0, litresCollected = 2584.7),
        RainfallEntry(date = "2025-07-03", rainfallMm = 52.0, litresCollected = 3000.0),
        RainfallEntry(date = "2025-07-09", rainfallMm = 38.0, litresCollected = 2395.7),
        RainfallEntry(date = "2025-07-15", rainfallMm = 60.0, litresCollected = 3000.0),
        RainfallEntry(date = "2025-07-22", rainfallMm = 45.0, litresCollected = 2836.8),
        RainfallEntry(date = "2025-08-02", rainfallMm = 48.0, litresCollected = 3000.0),
        RainfallEntry(date = "2025-08-11", rainfallMm = 55.0, litresCollected = 3000.0),
        RainfallEntry(date = "2025-08-19", rainfallMm = 32.0, litresCollected = 2017.2),
        RainfallEntry(date = "2025-08-27", rainfallMm = 40.0, litresCollected = 2521.6),
        RainfallEntry(date = "2025-09-04", rainfallMm = 30.0, litresCollected = 1891.2),
        RainfallEntry(date = "2025-09-15", rainfallMm = 18.0, litresCollected = 1134.7),
        RainfallEntry(date = "2025-09-24", rainfallMm = 22.0, litresCollected = 1386.9),
        RainfallEntry(date = "2025-10-08", rainfallMm = 25.0, litresCollected = 1576.0),
        RainfallEntry(date = "2025-10-20", rainfallMm = 15.0, litresCollected = 945.6),
        RainfallEntry(date = "2025-11-05", rainfallMm = 20.0, litresCollected = 1260.8),
        RainfallEntry(date = "2025-11-18", rainfallMm = 12.0, litresCollected = 756.5),
        RainfallEntry(date = "2025-12-10", rainfallMm = 6.0, litresCollected = 378.2),
        RainfallEntry(date = "2026-01-14", rainfallMm = 4.0, litresCollected = 252.2),
        RainfallEntry(date = "2026-02-20", rainfallMm = 10.0, litresCollected = 630.4),
        RainfallEntry(date = "2026-03-08", rainfallMm = 16.0, litresCollected = 1008.6),
        RainfallEntry(date = "2026-04-12", rainfallMm = 18.0, litresCollected = 1134.7),
        RainfallEntry(date = "2026-04-28", rainfallMm = 12.0, litresCollected = 756.5),
        RainfallEntry(date = "2026-04-29", rainfallMm = 0.0, litresCollected = 0.0),
        RainfallEntry(date = "2026-04-30", rainfallMm = 25.0, litresCollected = 1576.0)
    )
}
