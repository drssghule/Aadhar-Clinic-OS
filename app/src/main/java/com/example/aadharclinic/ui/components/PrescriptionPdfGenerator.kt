package com.example.aadharclinic.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.aadharclinic.data.model.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class PrescriptionAction {
    PRINT,
    SAVE_TO_DEVICE,
    VIEW,
    SHARE,
    PRINT_AND_SAVE
}

object PrescriptionPdfGenerator {

    /**
     * Generates a clean, minimal, professional A4 PDF Prescription conforming to:
     * - HEADER: Clinic Name, Address, Phone Number, Doctor details
     * - Date: ___
     * - Patient Name: ___
     * - Age: ___ | Sex: ___
     * - Chief Complaints: (compact)
     * - Treatment Table: Drug | Dose × Duration | Instructions (e.g. 1-0-1, जेवणापूर्वी / जेवणानंतर)
     * - Follow-up: 7-day auto-calculated, editable
     */
    fun generatePrescriptionPdf(
        context: Context,
        profile: ClinicProfile,
        patient: Patient,
        consultation: Consultation,
        prescriptionItems: List<PrescriptionItem>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (595 x 842 points)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val doctorName = consultation.doctorName.ifBlank { profile.doctorName }
        val dateString = dateFormat.format(Date(consultation.date))
        val timeString = timeFormat.format(Date(consultation.date))

        // 1. HEADER BANNER
        paint.color = Color.rgb(0, 106, 96) // Medical Teal Primary
        paint.style = Paint.Style.FILL
        canvas.drawRect(28f, 25f, 567f, 105f, paint)

        // Clinic Name
        textPaint.color = Color.WHITE
        textPaint.textSize = 19f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(profile.clinicName, 42f, 52f, textPaint)

        // Doctor Name & Reg
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("$doctorName • ${profile.qualification}", 42f, 70f, textPaint)
        canvas.drawText("Address: ${profile.address}", 42f, 85f, textPaint)
        canvas.drawText("Phone: ${profile.contactNumber} | Reg No: ${profile.regNumber}", 42f, 98f, textPaint)

        var yPos = 125f

        // 2. PATIENT DETAILS (Date, Patient Name, Age | Sex)
        paint.color = Color.rgb(242, 246, 245)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(28f, yPos - 12f, 567f, yPos + 48f, 6f, 6f, paint)

        textPaint.color = Color.rgb(20, 20, 20)
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Patient Name: ${patient.name} (${patient.patientCode})", 40f, yPos + 6f, textPaint)
        canvas.drawText("Date: $dateString ($timeString)", 390f, yPos + 6f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 10f
        canvas.drawText("Age: ${patient.age} Yrs   |   Sex: ${patient.sex}   |   Mobile: ${patient.mobile.ifBlank { "N/A" }}", 40f, yPos + 26f, textPaint)
        if (patient.allergies.isNotBlank() && patient.allergies.lowercase() != "none") {
            textPaint.color = Color.rgb(180, 20, 20)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Allergies: ${patient.allergies}", 390f, yPos + 26f, textPaint)
            textPaint.color = Color.rgb(20, 20, 20)
        }

        yPos += 58f

        // 3. VITALS STRIP (Compact)
        val vitals = mutableListOf<String>()
        if (consultation.bp.isNotBlank()) vitals.add("BP: ${consultation.bp} mmHg")
        if (consultation.pulse.isNotBlank()) vitals.add("Pulse: ${consultation.pulse} bpm")
        if (consultation.temperature.isNotBlank()) vitals.add("Temp: ${consultation.temperature} °F")
        if (consultation.spo2.isNotBlank()) vitals.add("SpO2: ${consultation.spo2}%")
        if (consultation.weight.isNotBlank()) vitals.add("Wt: ${consultation.weight} kg")
        if (consultation.rbs.isNotBlank()) vitals.add("RBS: ${consultation.rbs} mg/dL")

        if (vitals.isNotEmpty()) {
            paint.color = Color.rgb(230, 240, 238)
            canvas.drawRoundRect(28f, yPos - 10f, 567f, yPos + 14f, 4f, 4f, paint)
            textPaint.textSize = 9f
            textPaint.color = Color.rgb(0, 106, 96)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(vitals.joinToString("  •  "), 40f, yPos + 6f, textPaint)
            yPos += 26f
        }

        // 4. CHIEF COMPLAINTS & DIAGNOSIS (Compact to save space)
        textPaint.color = Color.rgb(30, 30, 30)
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Chief Complaints:", 40f, yPos, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(consultation.chiefComplaints.ifBlank { "Routine consultation / Checkup" }, 150f, yPos, textPaint)

        yPos += 16f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Diagnosis:", 40f, yPos, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = Color.rgb(0, 80, 72)
        canvas.drawText(consultation.diagnosis.ifBlank { "Clinical Diagnosis" }, 150f, yPos, textPaint)

        yPos += 22f

        // 5. TREATMENT / ℞ SECTION
        textPaint.color = Color.rgb(0, 106, 96)
        textPaint.textSize = 16f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("℞ Treatment", 40f, yPos, textPaint)

        yPos += 8f

        // Treatment Table Header: Drug | Dose × Duration | Instructions
        paint.color = Color.rgb(228, 238, 235)
        paint.style = Paint.Style.FILL
        canvas.drawRect(28f, yPos, 567f, yPos + 22f, paint)

        textPaint.color = Color.rgb(20, 20, 20)
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Drug", 40f, yPos + 15f, textPaint)
        canvas.drawText("Dose × Duration", 270f, yPos + 15f, textPaint)
        canvas.drawText("Instructions", 430f, yPos + 15f, textPaint)

        yPos += 24f

        // Treatment Rows
        if (prescriptionItems.isEmpty()) {
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textPaint.color = Color.rgb(100, 100, 100)
            canvas.drawText("No medications prescribed. General advice provided.", 40f, yPos + 14f, textPaint)
            yPos += 24f
        } else {
            prescriptionItems.forEachIndexed { index, item ->
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textSize = 10f
                textPaint.color = Color.rgb(20, 20, 20)

                // Drug Column
                val drugName = if (item.strength.isNotBlank()) "${item.medicineName} (${item.strength})" else item.medicineName
                canvas.drawText("${index + 1}. $drugName", 40f, yPos + 13f, textPaint)

                // Dose × Duration Column (e.g. 1 Tab × 5 days, 1 Cap × 7 days)
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.textSize = 9.5f
                val doseDuration = "${item.dose.ifBlank { "1 Dose" }} × ${item.duration.ifBlank { "5 Days" }}"
                canvas.drawText(doseDuration, 270f, yPos + 13f, textPaint)

                // Instructions Column (e.g. 1-0-1, जेवणापूर्वी / जेवणानंतर)
                val freqPart = if (item.frequency.isNotBlank()) "${item.frequency}, " else ""
                val instructionText = "$freqPart${item.instructions.ifBlank { "जेवणानंतर" }}"
                canvas.drawText(instructionText, 430f, yPos + 13f, textPaint)

                // Divider line
                paint.color = Color.rgb(235, 235, 235)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.8f
                canvas.drawLine(28f, yPos + 20f, 567f, yPos + 20f, paint)

                yPos += 24f
            }
        }

        // Additional Advice / Doctor Notes if present
        if (consultation.doctorNotes.isNotBlank()) {
            yPos += 8f
            textPaint.color = Color.rgb(40, 40, 40)
            textPaint.textSize = 9.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Advice & Precautions:", 40f, yPos, textPaint)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(consultation.doctorNotes, 40f, yPos + 14f, textPaint)
            yPos += 26f
        }

        // 6. FOLLOW-UP BOX (Auto-calculated 7 days or custom)
        val followUpY = 720f
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(240, 248, 246)
        canvas.drawRoundRect(28f, followUpY, 320f, followUpY + 34f, 6f, 6f, paint)

        textPaint.color = Color.rgb(0, 106, 96)
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val nextDateMillis = consultation.nextFollowUpDate ?: (consultation.date + 7L * 24 * 60 * 60 * 1000L)
        val followUpDateStr = dateFormat.format(Date(nextDateMillis))
        canvas.drawText("Follow-up: $followUpDateStr (After 7 Days)", 38f, followUpY + 16f, textPaint)

        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = Color.rgb(80, 80, 80)
        canvas.drawText("Emergency consultation available 24x7 if symptoms worsen", 38f, followUpY + 28f, textPaint)

        // 7. DOCTOR SIGNATURE AREA (Bottom Right)
        textPaint.color = Color.rgb(30, 30, 30)
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(doctorName, 410f, 735f, textPaint)

        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(profile.qualification, 410f, 749f, textPaint)
        canvas.drawText("Reg No: ${profile.regNumber}", 410f, 762f, textPaint)
        canvas.drawText("Authorized Signatory", 410f, 775f, textPaint)

        // Footer Timings
        paint.color = Color.rgb(130, 130, 130)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(profile.prescriptionFooter, 40f, 815f, paint)

        pdfDocument.finishPage(page)

        // Write PDF file to app internal cache
        val cacheDir = File(context.cacheDir, "prescriptions")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val cleanName = patient.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Prescription_${patient.patientCode}_${cleanName}_$timestamp.pdf"
        val file = File(cacheDir, fileName)

        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return file
    }

    /**
     * Triggers Android Print Spooler directly with standard ISO A4 formatting.
     * Allows doctor to print to Wi-Fi/Bluetooth/USB printers or use system "Save as PDF".
     */
    fun printPdf(
        context: Context,
        pdfFile: File,
        jobName: String = "Prescription_Print"
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Printing not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val pdi = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()

                callback?.onLayoutFinished(pdi, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    val input = FileInputStream(pdfFile)
                    val output = FileOutputStream(destination?.fileDescriptor)

                    val buf = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buf).also { bytesRead = it } > 0) {
                        output.write(buf, 0, bytesRead)
                    }

                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    input.close()
                    output.close()
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, printAdapter, attributes)
    }

    /**
     * Saves a permanent copy of the PDF into the public "Downloads" directory on device.
     */
    fun savePdfToPublicDownloads(
        context: Context,
        pdfFile: File,
        subfolder: String = "Prescriptions"
    ): Uri? {
        return try {
            val fileName = pdfFile.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/AadharClinic/$subfolder")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        FileInputStream(pdfFile).use { input ->
                            input.copyTo(output)
                        }
                    }
                    Toast.makeText(context, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
                    uri
                } else {
                    // Fallback to app external files dir
                    saveFallbackFile(context, pdfFile)
                }
            } else {
                val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AadharClinic/$subfolder")
                if (!publicDir.exists()) publicDir.mkdirs()
                val targetFile = File(publicDir, fileName)
                pdfFile.copyTo(targetFile, overwrite = true)
                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf("application/pdf"), null)
                Toast.makeText(context, "Saved to Downloads/${targetFile.name}", Toast.LENGTH_LONG).show()
                Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            saveFallbackFile(context, pdfFile)
        }
    }

    private fun saveFallbackFile(context: Context, pdfFile: File): Uri? {
        return try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val target = File(docsDir, pdfFile.name)
            pdfFile.copyTo(target, overwrite = true)
            Toast.makeText(context, "Saved to Documents/${target.name}", Toast.LENGTH_LONG).show()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Saved in clinic database: ${pdfFile.name}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * Opens the PDF in default system PDF viewer.
     */
    fun viewPdf(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Open Prescription PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open PDF viewer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shares the PDF via standard Android share sheet (WhatsApp, Email, etc.).
     */
    fun sharePdf(context: Context, pdfFile: File, subject: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Prescription PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * High-level handler for generating, saving to device downloads, and performing requested action.
     */
    fun generateAndHandlePrescription(
        context: Context,
        profile: ClinicProfile,
        patient: Patient,
        consultation: Consultation,
        prescriptionItems: List<PrescriptionItem>,
        action: PrescriptionAction = PrescriptionAction.PRINT_AND_SAVE
    ) {
        try {
            val pdfFile = generatePrescriptionPdf(
                context = context,
                profile = profile,
                patient = patient,
                consultation = consultation,
                prescriptionItems = prescriptionItems
            )

            // Always save a copy to public device downloads
            savePdfToPublicDownloads(context, pdfFile, "Prescriptions")

            when (action) {
                PrescriptionAction.PRINT, PrescriptionAction.PRINT_AND_SAVE -> {
                    printPdf(context, pdfFile, "Rx_${patient.name}")
                }
                PrescriptionAction.SAVE_TO_DEVICE -> {
                    // Already saved to downloads above, notify user
                    Toast.makeText(context, "Prescription saved to device Downloads", Toast.LENGTH_SHORT).show()
                }
                PrescriptionAction.VIEW -> {
                    viewPdf(context, pdfFile)
                }
                PrescriptionAction.SHARE -> {
                    sharePdf(context, pdfFile, "Prescription - ${patient.name} (${profile.clinicName})")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error processing prescription: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Compatibility wrapper for existing callers.
     */
    fun generateAndSharePrescription(
        context: Context,
        profile: ClinicProfile,
        patient: Patient,
        consultation: Consultation,
        prescriptionItems: List<PrescriptionItem>
    ) {
        generateAndHandlePrescription(
            context = context,
            profile = profile,
            patient = patient,
            consultation = consultation,
            prescriptionItems = prescriptionItems,
            action = PrescriptionAction.PRINT_AND_SAVE
        )
    }

    fun generateDischargeSummaryPdf(
        context: Context,
        profile: ClinicProfile,
        patient: Patient,
        admission: IpdAdmission,
        dailyNotes: List<IpdDailyNote>,
        medicinesAdministered: List<IpdMedicineAdministered>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        // Header
        paint.color = Color.rgb(0, 106, 96)
        paint.style = Paint.Style.FILL
        canvas.drawRect(28f, 25f, 567f, 105f, paint)

        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${profile.clinicName} - IPD DISCHARGE SUMMARY", 42f, 52f, paint)

        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${admission.doctorName} • ${profile.qualification}", 42f, 70f, paint)
        canvas.drawText("Bed/Room: ${admission.bedRoomNumber} | Receipt: ${admission.receiptNumber}", 42f, 88f, paint)

        var yPos = 135f
        paint.color = Color.rgb(245, 248, 247)
        canvas.drawRoundRect(28f, yPos - 12f, 567f, yPos + 48f, 6f, 6f, paint)

        paint.color = Color.rgb(30, 30, 30)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Patient: ${patient.name} (${patient.patientCode})", 42f, yPos + 8f, paint)
        canvas.drawText("Admitted: ${dateFormat.format(Date(admission.admissionDate))}", 320f, yPos + 8f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        val disDate = admission.dischargeDate?.let { dateFormat.format(Date(it)) } ?: "Ongoing"
        canvas.drawText("Discharge: $disDate | Status: ${admission.status} | Condition: ${admission.dischargeCondition}", 42f, yPos + 28f, paint)

        yPos += 65f

        // Diagnosis
        paint.color = Color.rgb(30, 30, 30)
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Admitting Diagnosis: ${admission.admittingDiagnosis.ifBlank { "N/A" }}", 42f, yPos, paint)
        yPos += 18f
        canvas.drawText("Final Diagnosis: ${admission.finalDiagnosis.ifBlank { admission.admittingDiagnosis }}", 42f, yPos, paint)

        yPos += 26f

        // Summary
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Clinical Course & Discharge Summary:", 42f, yPos, paint)
        yPos += 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(admission.dischargeSummary.ifBlank { "Patient treated successfully and discharged in stable condition." }, 42f, yPos, paint)

        yPos += 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Discharge Advice / Follow-up:", 42f, yPos, paint)
        yPos += 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(admission.dischargeAdvice.ifBlank { "Complete the oral antibiotic course, follow-up after 5 days." }, 42f, yPos, paint)

        // Signature
        paint.color = Color.rgb(30, 30, 30)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(admission.doctorName, 410f, 740f, paint)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Authorized Medical Officer", 410f, 756f, paint)

        pdfDocument.finishPage(page)

        val cacheDir = File(context.cacheDir, "discharge_summaries")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val fileName = "Discharge_${patient.patientCode}_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"
        val file = File(cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return file
    }

    fun generateAndShareDischargeSummary(
        context: Context,
        profile: ClinicProfile,
        patient: Patient,
        admission: IpdAdmission,
        dailyNotes: List<IpdDailyNote>,
        medicinesAdministered: List<IpdMedicineAdministered>
    ) {
        try {
            val file = generateDischargeSummaryPdf(
                context = context,
                profile = profile,
                patient = patient,
                admission = admission,
                dailyNotes = dailyNotes,
                medicinesAdministered = medicinesAdministered
            )
            savePdfToPublicDownloads(context, file, "Discharges")
            printPdf(context, file, "Discharge_${patient.name}")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
