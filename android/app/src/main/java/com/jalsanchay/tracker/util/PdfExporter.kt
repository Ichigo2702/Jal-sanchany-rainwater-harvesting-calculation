package com.jalsanchay.tracker.util

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.jalsanchay.tracker.data.UserSetup
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

    companion object {
        fun generateReport(
            context: Context,
            setup: UserSetup,
            monthly: List<Calculations.MonthlyTotal>,
            totalLitres: Double,
            impactScore: Double
        ): Uri {
            val document = PdfDocument()
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            val canvas = page.canvas
            val titlePaint = Paint().apply {
                textSize = 20f
                isFakeBoldText = true
            }
            val textPaint = Paint().apply { textSize = 12f }

            canvas.drawText("Jal-Sanchay Tracker Report", 40f, 48f, titlePaint)
            canvas.drawText("Setup: ${setup.roofArea.toInt()} ${setup.unit}, ${setup.runoffLabel}, ${setup.tankCapacity.toInt()} L tank", 40f, 76f, textPaint)
            canvas.drawText("Total: ${"%.1f".format(totalLitres)} L (${ "%.1f".format(impactScore)} days)", 40f, 98f, textPaint)
            canvas.drawText("Month", 40f, 138f, titlePaint)
            canvas.drawText("Rainfall", 160f, 138f, titlePaint)
            canvas.drawText("Water Saved", 280f, 138f, titlePaint)
            canvas.drawText("Impact", 430f, 138f, titlePaint)

            monthly.take(24).forEachIndexed { index, report ->
                val y = 168f + index * 26f
                canvas.drawText(report.monthKey, 40f, y, textPaint)
                canvas.drawText("${"%.1f".format(report.totalMm)} mm", 160f, y, textPaint)
                canvas.drawText("${"%.1f".format(report.totalLitres)} L", 280f, y, textPaint)
                canvas.drawText("${"%.1f".format(report.impactDays)} days", 430f, y, textPaint)
            }

            document.finishPage(page)
            val dir = File(context.getExternalFilesDir("reports"), "")
            dir.mkdirs()
            val output = File(dir, "jal-sanchay-report.pdf")
            FileOutputStream(output).use { document.writeTo(it) }
            document.close()
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", output)
        }
    }
}
