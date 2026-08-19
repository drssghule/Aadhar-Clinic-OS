package com.example.aadharclinic.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.aadharclinic.data.model.ClinicProfile
import com.example.aadharclinic.data.model.ClinicalDocument
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ClinicalDocumentPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN_LEFT = 50f
    private const val MARGIN_RIGHT = 545f
    private const val CONTENT_WIDTH = (MARGIN_RIGHT - MARGIN_LEFT).toInt()

    /**
     * Generates a clean, professional A4 hospital letterhead PDF for Referral Letters or Medical Sick Certificates.
     * Guaranteed to use proper formatted paragraphs without forms, tables, or checklists.
     */
    fun generateDocumentPdf(
        context: Context,
        profile: ClinicProfile,
        doc: ClinicalDocument
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 106, 96) // Teal primary brand
            textSize = 20f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val subTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(70, 75, 75)
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val bodyTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(25, 25, 25)
            textSize = 12f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        val boldBodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 15, 15)
            textSize = 12f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }

        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val dateString = dateFormat.format(Date(doc.date))

        val clinicName = if (doc.clinicName.isNotBlank()) doc.clinicName else profile.clinicName
        val clinicAddress = if (doc.clinicAddress.isNotBlank()) doc.clinicAddress else profile.address
        val clinicPhone = if (doc.clinicContact.isNotBlank()) doc.clinicContact else profile.contactNumber
        val doctorName = if (doc.doctorName.isNotBlank()) doc.doctorName else profile.doctorName
        val qualification = if (doc.doctorQualification.isNotBlank()) doc.doctorQualification else profile.qualification
        val regNumber = if (doc.doctorRegNumber.isNotBlank()) doc.doctorRegNumber else profile.regNumber

        // 1. HOSPITAL / CLINIC LOGO & HEADER
        // Draw Medical Cross / Caduceus Icon emblem
        val emblemRadius = 18f
        val emblemCenterX = MARGIN_LEFT + 22f
        val emblemCenterY = 52f

        paint.color = Color.rgb(0, 106, 96)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(emblemCenterX, emblemCenterY, emblemRadius, paint)

        // White Cross inside emblem
        paint.color = Color.WHITE
        paint.strokeWidth = 4.5f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(emblemCenterX, emblemCenterY - 10f, emblemCenterX, emblemCenterY + 10f, paint)
        canvas.drawLine(emblemCenterX - 10f, emblemCenterY, emblemCenterX + 10f, emblemCenterY, paint)

        // Clinic Name
        val textStartX = MARGIN_LEFT + 50f
        canvas.drawText(clinicName.uppercase(), textStartX, 48f, titlePaint)

        // Address & Phone
        val addressLine = "$clinicAddress | Ph: $clinicPhone"
        canvas.drawText(addressLine, textStartX, 64f, subTitlePaint)

        // Doctor credentials sub-header
        val docLine = "$doctorName • $qualification | Reg No: $regNumber"
        canvas.drawText(docLine, textStartX, 78f, subTitlePaint)

        // Letterhead Divider
        paint.color = Color.rgb(0, 106, 96)
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(MARGIN_LEFT, 96f, MARGIN_RIGHT, 96f, paint)

        paint.color = Color.rgb(180, 200, 198)
        paint.strokeWidth = 0.75f
        canvas.drawLine(MARGIN_LEFT, 99f, MARGIN_RIGHT, 99f, paint)

        // 2. DOCUMENT BODY RENDERING (EXACT FORMATS IN PARAGRAPHS)
        var yPos = 130f

        if (doc.documentType == "REFERRAL_LETTER") {
            // REFERRAL LETTER — EXACT SPECIFIED FORMAT
            // "To,"
            canvas.drawText("To,", MARGIN_LEFT, yPos, boldBodyPaint)
            yPos += 18f

            // Selected Hospital Name
            canvas.drawText(doc.hospitalName, MARGIN_LEFT, yPos, boldBodyPaint)
            yPos += 16f

            // Hospital Address (wrapped if long)
            val hospitalAddressLayout = StaticLayout.Builder.obtain(
                doc.hospitalAddress,
                0,
                doc.hospitalAddress.length,
                bodyTextPaint,
                CONTENT_WIDTH
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1.15f)
                .build()

            canvas.save()
            canvas.translate(MARGIN_LEFT, yPos)
            hospitalAddressLayout.draw(canvas)
            canvas.restore()
            yPos += hospitalAddressLayout.height + 22f

            // Date: [Auto-generated] (Aligned right or left)
            canvas.drawText("Date: $dateString", MARGIN_LEFT, yPos, bodyTextPaint)
            yPos += 26f

            // Subject: Referral for Further Evaluation and Management
            val subjectText = "Subject: Referral for Further Evaluation and Management"
            canvas.drawText(subjectText, MARGIN_LEFT, yPos, boldBodyPaint)
            // Underline subject
            val subjectWidth = boldBodyPaint.measureText(subjectText)
            paint.color = Color.rgb(15, 15, 15)
            paint.strokeWidth = 0.8f
            canvas.drawLine(MARGIN_LEFT, yPos + 2f, MARGIN_LEFT + subjectWidth, yPos + 2f, paint)
            yPos += 30f

            // "Dear Sir/Madam,"
            canvas.drawText("Dear Sir/Madam,", MARGIN_LEFT, yPos, boldBodyPaint)
            yPos += 24f

            // Paragraph 1
            val complaints = if (doc.chiefComplaints.isNotBlank()) doc.chiefComplaints else "clinical symptoms"
            val treatment = if (doc.treatmentGiven.isNotBlank()) doc.treatmentGiven else "supportive symptomatic therapy"
            val p1Text = "This is to refer ${doc.patientName}, aged ${doc.age} years, ${doc.sex}, who presented with $complaints. The patient was examined and treated at our clinic with $treatment. In view of the patient's clinical condition, further evaluation and appropriate management at your hospital is advised."

            val p1Layout = StaticLayout.Builder.obtain(
                p1Text,
                0,
                p1Text.length,
                bodyTextPaint,
                CONTENT_WIDTH
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(3f, 1.25f)
                .build()

            canvas.save()
            canvas.translate(MARGIN_LEFT, yPos)
            p1Layout.draw(canvas)
            canvas.restore()
            yPos += p1Layout.height + 20f

            // Paragraph 2
            val p2Text = "Kindly evaluate the patient and provide further management as deemed appropriate."
            val p2Layout = StaticLayout.Builder.obtain(
                p2Text,
                0,
                p2Text.length,
                bodyTextPaint,
                CONTENT_WIDTH
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(3f, 1.25f)
                .build()

            canvas.save()
            canvas.translate(MARGIN_LEFT, yPos)
            p2Layout.draw(canvas)
            canvas.restore()
            yPos += p2Layout.height + 26f

            // "Thank you."
            canvas.drawText("Thank you.", MARGIN_LEFT, yPos, bodyTextPaint)

        } else {
            // MEDICAL SICK CERTIFICATE — EXACT SPECIFIED FORMAT
            // Date: [Auto-generated]
            canvas.drawText("Date: $dateString", MARGIN_LEFT, yPos, bodyTextPaint)
            yPos += 36f

            // "To Whom It May Concern"
            canvas.drawText("To Whom It May Concern", MARGIN_LEFT, yPos, boldBodyPaint)
            yPos += 36f

            // Center Heading: "MEDICAL CERTIFICATE"
            val headingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(0, 60, 55)
                textSize = 15f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                letterSpacing = 0.08f
            }
            val headingText = "MEDICAL CERTIFICATE"
            val headingWidth = headingPaint.measureText(headingText)
            val headingX = (PAGE_WIDTH - headingWidth) / 2f
            canvas.drawText(headingText, headingX, yPos, headingPaint)

            // Underline heading
            paint.color = Color.rgb(0, 106, 96)
            paint.strokeWidth = 1.2f
            canvas.drawLine(headingX - 8f, yPos + 4f, headingX + headingWidth + 8f, yPos + 4f, paint)
            yPos += 42f

            // Paragraph 1
            val residence = if (doc.residentOf.isNotBlank()) doc.residentOf else "Pune"
            val days = if (doc.daysCount > 0) doc.daysCount else 1
            val daysStr = if (days == 1) "1 day" else "$days days"

            val p1Text = "This is to certify that ${doc.patientName}, aged ${doc.age} years, ${doc.sex}, resident of $residence, was examined at $clinicName and is advised medical rest for $daysStr on medical grounds."

            val p1Layout = StaticLayout.Builder.obtain(
                p1Text,
                0,
                p1Text.length,
                bodyTextPaint,
                CONTENT_WIDTH
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(4f, 1.3f)
                .build()

            canvas.save()
            canvas.translate(MARGIN_LEFT, yPos)
            p1Layout.draw(canvas)
            canvas.restore()
            yPos += p1Layout.height + 24f

            // Paragraph 2
            val p2Text = "This certificate is issued upon request for the purpose for which it may be required."
            val p2Layout = StaticLayout.Builder.obtain(
                p2Text,
                0,
                p2Text.length,
                bodyTextPaint,
                CONTENT_WIDTH
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(3f, 1.25f)
                .build()

            canvas.save()
            canvas.translate(MARGIN_LEFT, yPos)
            p2Layout.draw(canvas)
            canvas.restore()
        }

        // 3. SIGNATURE & STAMP FOOTER (Positioned gracefully at bottom)
        val sigY = 660f

        // Blank signature line
        paint.color = Color.rgb(60, 60, 60)
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(MARGIN_LEFT, sigY, MARGIN_LEFT + 220f, sigY, paint)

        canvas.drawText("Signature: __________________________", MARGIN_LEFT, sigY - 8f, bodyTextPaint)

        val sigDocPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 20, 20)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val sigDetailPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(60, 60, 60)
            textSize = 10f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }

        canvas.drawText(doctorName, MARGIN_LEFT, sigY + 20f, sigDocPaint)
        canvas.drawText(qualification, MARGIN_LEFT, sigY + 34f, sigDetailPaint)
        canvas.drawText("Reg. No: $regNumber", MARGIN_LEFT, sigY + 48f, sigDetailPaint)
        canvas.drawText(clinicName, MARGIN_LEFT, sigY + 62f, sigDetailPaint)

        // Stamp Box on Right
        val stampBoxLeft = 380f
        val stampBoxTop = sigY - 20f
        val stampBoxRight = MARGIN_RIGHT
        val stampBoxBottom = sigY + 75f

        paint.color = Color.rgb(220, 230, 228)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        canvas.drawRoundRect(stampBoxLeft, stampBoxTop, stampBoxRight, stampBoxBottom, 6f, 6f, paint)
        paint.pathEffect = null

        val stampNotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(140, 150, 150)
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText("[ Official Clinic / Doctor Stamp ]", stampBoxLeft + 18f, stampBoxTop + 50f, stampNotePaint)

        // Security / Verification Footer
        val footY = 800f
        paint.color = Color.rgb(220, 225, 225)
        paint.strokeWidth = 0.5f
        canvas.drawLine(MARGIN_LEFT, footY - 10f, MARGIN_RIGHT, footY - 10f, paint)

        val docTypeLabel = if (doc.documentType == "REFERRAL_LETTER") "OFFICIAL REFERRAL LETTER" else "MEDICAL CERTIFICATE"
        val footerText = "$docTypeLabel • $clinicName • Generated on $dateString"
        canvas.drawText(footerText, MARGIN_LEFT, footY + 4f, subTitlePaint)

        pdfDocument.finishPage(page)

        // Save PDF to documents cache
        val dir = File(context.cacheDir, "clinical_documents")
        if (!dir.exists()) dir.mkdirs()

        val prefix = if (doc.documentType == "REFERRAL_LETTER") "Referral" else "SickCertificate"
        val cleanPatientName = doc.patientName.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${prefix}_${cleanPatientName}_$timestamp.pdf"
        val file = File(dir, fileName)

        val fos = FileOutputStream(file)
        pdfDocument.writeTo(fos)
        fos.flush()
        fos.close()
        pdfDocument.close()

        return file
    }

    /**
     * Triggers Android Print Spooler directly with ISO A4 Letter formatting.
     */
    fun printDocument(
        context: Context,
        pdfFile: File,
        jobName: String
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

                    val buf = ByteArray(1024)
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
     * Shares the PDF file via Android Share sheet.
     */
    fun shareDocument(
        context: Context,
        pdfFile: File,
        subject: String
    ) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share $subject").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Saves a permanent copy to device public Downloads directory.
     */
    fun savePdfToStorage(
        context: Context,
        pdfFile: File,
        docName: String
    ): File? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, pdfFile.name)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "${android.os.Environment.DIRECTORY_DOWNLOADS}/AadharClinic/Documents")
                }
                val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        FileInputStream(pdfFile).use { input ->
                            input.copyTo(output)
                        }
                    }
                }
            } else {
                val publicDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "AadharClinic/Documents")
                if (!publicDir.exists()) publicDir.mkdirs()
                val targetFile = File(publicDir, pdfFile.name)
                pdfFile.copyTo(targetFile, overwrite = true)
                android.media.MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf("application/pdf"), null)
            }

            val destDir = context.getExternalFilesDir(null) ?: context.filesDir
            val finalDir = File(destDir, "SavedDocuments")
            if (!finalDir.exists()) finalDir.mkdirs()

            val targetFile = File(finalDir, pdfFile.name)
            pdfFile.copyTo(targetFile, overwrite = true)

            Toast.makeText(context, "Saved to Downloads: ${pdfFile.name}", Toast.LENGTH_LONG).show()
            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Saved to local cache: ${pdfFile.name}", Toast.LENGTH_SHORT).show()
            pdfFile
        }
    }
}
