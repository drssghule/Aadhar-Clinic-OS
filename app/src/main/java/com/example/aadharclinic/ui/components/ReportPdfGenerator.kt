package com.example.aadharclinic.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.aadharclinic.data.model.ClinicProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class DoctorBreakdownItem(
    val doctorName: String,
    val patientCount: Int,
    val revenue: Double
)

data class ReportRowItem(
    val dateStr: String,
    val patientName: String,
    val ageSex: String,
    val diagnosis: String,
    val treatmentServices: String,
    val doctorName: String = "",
    val billingAmount: Double
)

object ReportPdfGenerator {

    /**
     * Generates an ISO A4-size PDF for Daily or Monthly Clinic Reports.
     * Report includes:
     * - Clinic name + Doctor name + Qualifications
     * - Report date / range
     * - Doctor-wise + Clinic-wide Breakdown Summary
     * - Patient details (Name, Age/Sex, Diagnosis, Treatment, Services/Billing amount, Doctor)
     * - Total patients and Total revenue summary
     * - NO medicine billing
     * Saves to device storage under:
     * "Clinic OS/Daily Report/<date>.pdf" or "Clinic OS/Monthly Report/<month-year>.pdf"
     */
    fun generateAndSaveReportPdf(
        context: Context,
        profile: ClinicProfile,
        reportTitle: String, // "Daily Report" or "Monthly Report"
        dateLabel: String,   // "18-08-2026" or "August-2026"
        items: List<ReportRowItem>,
        totalPatients: Int,
        totalRevenue: Double,
        doctorBreakdown: List<DoctorBreakdownItem> = emptyList(),
        filterDoctorLabel: String = "Entire Clinic"
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // ISO A4 (595 x 842 points)
            val pageHeight = 842

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val currency = profile.currency.ifBlank { "₹" }

            val itemsPerPage = 12
            val totalPages = if (items.isEmpty()) 1 else ((items.size - 1) / itemsPerPage) + 1

            for (pageIndex in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                var yPos = 35f

                // 1. Top Header Banner (Dark Teal)
                paint.color = Color.rgb(0, 106, 96)
                paint.style = Paint.Style.FILL
                canvas.drawRect(25f, 25f, 570f, 105f, paint)

                // Clinic Name
                textPaint.color = Color.WHITE
                textPaint.textSize = 18f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(profile.clinicName, 40f, 52f, textPaint)

                // Doctor Name & Credentials
                textPaint.textSize = 11f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("${profile.doctorName} • ${profile.qualification}", 40f, 70f, textPaint)
                canvas.drawText("Reg No: ${profile.regNumber} | Contact: ${profile.contactNumber}", 40f, 88f, textPaint)

                yPos = 125f

                // 2. Report Sub-header & Metadata
                textPaint.color = Color.rgb(20, 20, 20)
                textPaint.textSize = 13f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("$reportTitle - $dateLabel ($filterDoctorLabel)", 25f, yPos, textPaint)

                textPaint.textSize = 9f
                textPaint.color = Color.rgb(90, 90, 90)
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val generatedTimestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                canvas.drawText("Generated: $generatedTimestamp | Page ${pageIndex + 1} of $totalPages", 320f, yPos, textPaint)

                yPos += 12f
                paint.color = Color.rgb(210, 220, 220)
                paint.strokeWidth = 1.2f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(25f, yPos, 570f, yPos, paint)

                // 3. Summary KPI Box & Doctor Breakdown (On first page)
                if (pageIndex == 0) {
                    yPos += 10f
                    val boxHeight = if (doctorBreakdown.isNotEmpty()) 65f else 38f
                    paint.style = Paint.Style.FILL
                    paint.color = Color.rgb(240, 248, 246)
                    canvas.drawRoundRect(25f, yPos, 570f, yPos + boxHeight, 6f, 6f, paint)

                    textPaint.textSize = 10.5f
                    textPaint.color = Color.rgb(0, 106, 96)
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("TOTAL CLINIC PATIENTS: $totalPatients", 38f, yPos + 20f, textPaint)
                    canvas.drawText("TOTAL CLINIC REVENUE: $currency${String.format("%,.0f", totalRevenue)}", 300f, yPos + 20f, textPaint)

                    if (doctorBreakdown.isNotEmpty()) {
                        textPaint.textSize = 8.5f
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        textPaint.color = Color.rgb(50, 50, 50)
                        val breakdownStr = doctorBreakdown.joinToString("   |   ") {
                            "${it.doctorName}: ${it.patientCount} pts, $currency${it.revenue.toInt()}"
                        }
                        canvas.drawText("Doctor Breakdown: $breakdownStr", 38f, yPos + 45f, textPaint)
                    }

                    yPos += (boxHeight + 12f)
                } else {
                    yPos += 15f
                }

                // 4. Table Header
                val tableTop = yPos
                paint.style = Paint.Style.FILL
                paint.color = Color.rgb(228, 238, 236)
                canvas.drawRect(25f, tableTop, 570f, tableTop + 22f, paint)

                textPaint.color = Color.rgb(30, 40, 40)
                textPaint.textSize = 8.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

                canvas.drawText("#", 28f, tableTop + 15f, textPaint)
                canvas.drawText("Time / Date", 46f, tableTop + 15f, textPaint)
                canvas.drawText("Patient Name", 110f, tableTop + 15f, textPaint)
                canvas.drawText("Age/Sex", 205f, tableTop + 15f, textPaint)
                canvas.drawText("Doctor", 255f, tableTop + 15f, textPaint)
                canvas.drawText("Diagnosis", 340f, tableTop + 15f, textPaint)
                canvas.drawText("Services", 430f, tableTop + 15f, textPaint)
                canvas.drawText("Amt ($currency)", 515f, tableTop + 15f, textPaint)

                yPos = tableTop + 22f

                // 5. Table Rows
                val startIdx = pageIndex * itemsPerPage
                val endIdx = minOf(startIdx + itemsPerPage, items.size)

                for (i in startIdx until endIdx) {
                    val item = items[i]
                    val rowHeight = 32f

                    if (i % 2 == 1) {
                        paint.color = Color.rgb(248, 250, 250)
                        canvas.drawRect(25f, yPos, 570f, yPos + rowHeight, paint)
                    }

                    paint.color = Color.rgb(235, 235, 235)
                    paint.strokeWidth = 0.5f
                    paint.style = Paint.Style.STROKE
                    canvas.drawLine(25f, yPos + rowHeight, 570f, yPos + rowHeight, paint)

                    textPaint.color = Color.rgb(30, 30, 30)
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                    // Index
                    canvas.drawText("${i + 1}", 28f, yPos + 18f, textPaint)

                    // Time/Date
                    val shortDate = if (item.dateStr.length > 10) item.dateStr.take(10) else item.dateStr
                    canvas.drawText(shortDate, 46f, yPos + 18f, textPaint)

                    // Patient Name
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val pName = if (item.patientName.length > 15) item.patientName.take(14) + ".." else item.patientName
                    canvas.drawText(pName, 110f, yPos + 18f, textPaint)

                    // Age / Sex
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(item.ageSex, 205f, yPos + 18f, textPaint)

                    // Doctor
                    val doc = if (item.doctorName.length > 14) item.doctorName.take(12) + ".." else item.doctorName.ifBlank { "Clinic" }
                    canvas.drawText(doc, 255f, yPos + 18f, textPaint)

                    // Diagnosis
                    val diag = if (item.diagnosis.length > 15) item.diagnosis.take(13) + ".." else item.diagnosis.ifBlank { "-" }
                    canvas.drawText(diag, 340f, yPos + 18f, textPaint)

                    // Treatment / Services
                    val treat = if (item.treatmentServices.length > 15) item.treatmentServices.take(13) + ".." else item.treatmentServices.ifBlank { "Consultation" }
                    canvas.drawText(treat, 430f, yPos + 18f, textPaint)

                    // Amount
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textPaint.color = Color.rgb(0, 106, 96)
                    canvas.drawText(String.format("%,.0f", item.billingAmount), 518f, yPos + 18f, textPaint)

                    yPos += rowHeight
                }

                if (items.isEmpty()) {
                    textPaint.color = Color.GRAY
                    textPaint.textSize = 10f
                    canvas.drawText("No consultations or billings recorded for this period.", 160f, yPos + 30f, textPaint)
                    yPos += 50f
                }

                // 6. Summary Footer on Last Page
                if (pageIndex == totalPages - 1) {
                    yPos = maxOf(yPos + 15f, 740f)

                    paint.style = Paint.Style.FILL
                    paint.color = Color.rgb(230, 244, 241)
                    canvas.drawRoundRect(25f, yPos, 570f, yPos + 35f, 6f, 6f, paint)

                    textPaint.color = Color.rgb(0, 106, 96)
                    textPaint.textSize = 10.5f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("TOTAL: $totalPatients Visits", 40f, yPos + 22f, textPaint)
                    canvas.drawText("TOTAL REVENUE: $currency${String.format("%,.0f", totalRevenue)}", 330f, yPos + 22f, textPaint)

                    textPaint.color = Color.rgb(80, 80, 80)
                    textPaint.textSize = 9f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("Authorized Signatory: ________________________", 300f, yPos + 65f, textPaint)
                }

                pdfDocument.finishPage(page)
            }

            val subFolder = if (reportTitle.contains("Daily", ignoreCase = true)) "Daily Report" else "Monthly Report"
            val sanitizedDate = dateLabel.replace("/", "-").replace(" ", "-")
            val fileName = "$sanitizedDate.pdf"

            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val primaryDir = File(publicDocs, "Clinic OS/$subFolder")
            if (!primaryDir.exists()) {
                primaryDir.mkdirs()
            }

            val appDocs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val fallbackDir = File(appDocs, "Clinic OS/$subFolder")
            if (!fallbackDir.exists()) {
                fallbackDir.mkdirs()
            }

            val targetFile = try {
                if (primaryDir.canWrite() || primaryDir.exists()) {
                    File(primaryDir, fileName)
                } else {
                    File(fallbackDir, fileName)
                }
            } catch (e: Exception) {
                File(fallbackDir, fileName)
            }

            val outStream = FileOutputStream(targetFile)
            pdfDocument.writeTo(outStream)
            outStream.flush()
            outStream.close()
            pdfDocument.close()

            try {
                if (targetFile.parentFile?.absolutePath != fallbackDir.absolutePath) {
                    val mirrorFile = File(fallbackDir, fileName)
                    targetFile.copyTo(mirrorFile, overwrite = true)
                }
            } catch (_: Exception) {}

            return targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun openOrShareReportPdf(context: Context, pdfFile: File, title: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Open or Share $title")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(chooser)
        } catch (e: Exception) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share $title").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (ex: Exception) {
                Toast.makeText(context, "Saved to: ${pdfFile.absolutePath}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
