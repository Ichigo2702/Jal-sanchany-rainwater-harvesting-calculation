package com.jalsanchay.tracker.util

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.jalsanchay.tracker.model.MonthlyReport
import java.io.File
import java.io.FileOutputStream

class PdfExporter(private val context: Context) {
    fun exportMonthlyReport(reports: List<MonthlyReport>): File {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply { textSize = 12f }

        canvas.drawText("Jal-Sanchay Tracker - Monthly Report", 40f, 48f, titlePaint)
        canvas.drawText("Month", 40f, 88f, titlePaint)
        canvas.drawText("Rainfall", 160f, 88f, titlePaint)
        canvas.drawText("Water Saved", 280f, 88f, titlePaint)
        canvas.drawText("Impact", 430f, 88f, titlePaint)

        reports.take(24).forEachIndexed { index, report ->
            val y = 118f + index * 26f
            canvas.drawText(report.monthKey, 40f, y, textPaint)
            canvas.drawText("${"%.1f".format(report.totalRainfallMm)} mm", 160f, y, textPaint)
            canvas.drawText("${"%.1f".format(report.totalWaterSaved)} L", 280f, y, textPaint)
            canvas.drawText("${"%.1f".format(report.impactDays)} days", 430f, y, textPaint)
        }

        document.finishPage(page)
        val output = File(context.getExternalFilesDir(null), "jal-sanchay-monthly-report.pdf")
        FileOutputStream(output).use { document.writeTo(it) }
        document.close()
        return output
    }
}
