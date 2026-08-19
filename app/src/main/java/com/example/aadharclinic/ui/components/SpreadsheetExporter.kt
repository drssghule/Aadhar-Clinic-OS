package com.example.aadharclinic.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.aadharclinic.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

object SpreadsheetExporter {

    private fun escapeCsv(value: Any?): String {
        if (value == null) return "\"\""
        val str = value.toString().replace("\"", "\"\"").replace("\n", " ").replace("\r", " ")
        return "\"$str\""
    }

    /**
     * Exports Patient Master Data with Diagnosis, Vitals, Hospital Treatment, Services/Procedures & Billing
     * into a standard CSV spreadsheet compatible with Microsoft Excel, Google Sheets, LibreOffice Calc.
     */
    fun exportPatientsMasterSpreadsheet(
        context: Context,
        profile: ClinicProfile?,
        patients: List<Patient>,
        consultations: List<Consultation>,
        prescriptionItems: List<PrescriptionItem>,
        admissions: List<IpdAdmission> = emptyList(),
        invoices: List<BillInvoice> = emptyList()
    ) {
        if (patients.isEmpty()) {
            Toast.makeText(context, "No patient records to export", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
        val dateOnlyFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val clinicName = profile?.clinicName ?: "Aadhar Clinic"

        try {
            val cacheDir = File(context.cacheDir, "spreadsheets")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "Patient_Master_Register_$timestampStr.csv")
            val fos = FileOutputStream(file)
            val osw = OutputStreamWriter(fos, StandardCharsets.UTF_8)

            // UTF-8 BOM so Excel opens Hindi/Marathi and Special Symbols without encoding issues
            osw.write("\uFEFF")

            // Title & Clinic Metadata Header
            osw.write("${escapeCsv(clinicName)} - PATIENT MASTER REGISTER & CLINICAL SPREADSHEET\n")
            osw.write("Doctor: ${escapeCsv(profile?.doctorName ?: "Dr. Sanket Ghule, BAMS EMS")},Generated On: ${escapeCsv(dateFormat.format(Date()))},Total Patients: ${escapeCsv(patients.size)}\n\n")

            // Columns definition
            val headers = listOf(
                "S.No",
                "Patient ID",
                "Registration Date",
                "Patient Full Name",
                "Age",
                "Sex",
                "Mobile Phone",
                "Address / City",
                "Blood Group",
                "Allergies",
                "Medical History",
                "Latest Consultation Date",
                "Latest Diagnosis",
                "Chief Complaints",
                "Blood Pressure (BP)",
                "Pulse (bpm)",
                "Temperature (°F)",
                "SpO2 (%)",
                "Weight (kg)",
                "Blood Sugar (RBS)",
                "Hospital Dispensary Medicines Prescribed & Cost",
                "Services & Procedures Done (OPD/Stitches/Dressing/ECG)",
                "Doctor Notes / Advice",
                "Total Consultations Count",
                "Daycare / IPD Admissions Count",
                "Total Billed Amount (₹)",
                "Total Paid (₹)",
                "Balance Due (₹)",
                "Latest Payment Mode",
                "Latest Payment Status"
            )
            osw.write(headers.joinToString(",") { escapeCsv(it) } + "\n")

            var grandTotalBilled = 0.0
            var grandTotalPaid = 0.0

            patients.forEachIndexed { index, patient ->
                val patientConsultations = consultations.filter { it.patientId == patient.id }.sortedByDescending { it.date }
                val latestCons = patientConsultations.firstOrNull()
                val patientAdmissions = admissions.filter { it.patientId == patient.id }
                val patientInvoices = invoices.filter { it.patientId == patient.id }

                val patientPrescriptions = if (latestCons != null) {
                    prescriptionItems.filter { it.consultationId == latestCons.id }
                } else emptyList()

                val medicinesSummary = if (patientPrescriptions.isNotEmpty()) {
                    patientPrescriptions.joinToString("; ") {
                        val cost = if (it.totalPrice > 0) " (₹${it.totalPrice.toInt()})" else ""
                        "${it.medicineName} [${it.dose} ${it.frequency} x ${it.duration}] Qty:${it.quantity}$cost"
                    }
                } else "None"

                val servicesSummary = if (latestCons != null && latestCons.servicesSummary.isNotBlank()) {
                    latestCons.servicesSummary
                } else if (latestCons != null && latestCons.serviceCharge > 0) {
                    "Services: ₹${latestCons.serviceCharge}"
                } else "Standard OPD"

                val totalPatientBilled = if (patientInvoices.isNotEmpty()) {
                    patientInvoices.sumOf { it.totalAmount }
                } else {
                    patientConsultations.sumOf { it.totalAmount } + patientAdmissions.sumOf { it.totalAmount }
                }

                val totalPatientPaid = if (patientInvoices.isNotEmpty()) {
                    patientInvoices.sumOf { it.paidAmount }
                } else {
                    patientConsultations.sumOf { it.paidAmount } + patientAdmissions.sumOf { it.paidAmount }
                }

                grandTotalBilled += totalPatientBilled
                grandTotalPaid += totalPatientPaid

                val latestPayMode = latestCons?.paymentMode ?: patientInvoices.firstOrNull()?.paymentMode ?: "Cash"
                val latestPayStatus = latestCons?.paymentStatus ?: if (totalPatientBilled - totalPatientPaid <= 0) "Paid" else "Pending"

                val row = listOf(
                    (index + 1).toString(),
                    patient.patientCode,
                    dateOnlyFormat.format(Date(patient.createdAt)),
                    patient.name,
                    patient.age.toString(),
                    patient.sex,
                    patient.mobile,
                    patient.address,
                    patient.bloodGroup,
                    patient.allergies,
                    patient.medicalHistory,
                    if (latestCons != null) dateFormat.format(Date(latestCons.date)) else "No visits yet",
                    latestCons?.diagnosis ?: "N/A",
                    latestCons?.chiefComplaints ?: "N/A",
                    latestCons?.bp ?: "-",
                    latestCons?.pulse ?: "-",
                    latestCons?.temperature ?: "-",
                    latestCons?.spo2 ?: "-",
                    latestCons?.weight ?: "-",
                    latestCons?.rbs ?: "-",
                    medicinesSummary,
                    servicesSummary,
                    latestCons?.doctorNotes ?: "",
                    patientConsultations.size.toString(),
                    patientAdmissions.size.toString(),
                    "%.2f".format(Locale.US, totalPatientBilled),
                    "%.2f".format(Locale.US, totalPatientPaid),
                    "%.2f".format(Locale.US, (totalPatientBilled - totalPatientPaid).coerceAtLeast(0.0)),
                    latestPayMode,
                    latestPayStatus
                )

                osw.write(row.joinToString(",") { escapeCsv(it) } + "\n")
            }

            // Summary Totals Row
            osw.write("\n")
            val summaryRow = listOf(
                "TOTALS",
                "${patients.size} Patients",
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "${consultations.size} Consultations",
                "${admissions.size} Admissions",
                "₹%.2f".format(Locale.US, grandTotalBilled),
                "₹%.2f".format(Locale.US, grandTotalPaid),
                "₹%.2f".format(Locale.US, (grandTotalBilled - grandTotalPaid).coerceAtLeast(0.0)),
                "", ""
            )
            osw.write(summaryRow.joinToString(",") { escapeCsv(it) } + "\n")

            osw.flush()
            osw.close()
            fos.close()

            shareSpreadsheetFile(context, file, "Patient Master Register Spreadsheet (${patients.size} Records)")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Exports Daily / Period OPD Consultations & Treatments into Spreadsheet
     */
    fun exportConsultationsSpreadsheet(
        context: Context,
        profile: ClinicProfile?,
        consultations: List<Consultation>,
        prescriptionItems: List<PrescriptionItem>
    ) {
        if (consultations.isEmpty()) {
            Toast.makeText(context, "No consultation records to export", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        try {
            val cacheDir = File(context.cacheDir, "spreadsheets")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "OPD_Consultations_Register_$timestampStr.csv")
            val fos = FileOutputStream(file)
            val osw = OutputStreamWriter(fos, StandardCharsets.UTF_8)

            osw.write("\uFEFF")
            osw.write("${escapeCsv(profile?.clinicName ?: "Aadhar Clinic")} - OPD CONSULTATION & TREATMENT SPREADSHEET\n")
            osw.write("Doctor: ${escapeCsv(profile?.doctorName ?: "Dr. Sanket Ghule, BAMS EMS")},Export Date: ${escapeCsv(dateFormat.format(Date()))}\n\n")

            val headers = listOf(
                "S.No",
                "Receipt No",
                "Visit Date & Time",
                "Patient Name",
                "Chief Complaints",
                "Diagnosis",
                "BP",
                "Pulse",
                "Temp",
                "SpO2",
                "RBS",
                "Weight",
                "Services / Procedures Done",
                "Hospital Medicines Dispensed & Cost",
                "Consultation Fee (₹)",
                "Service Charges (₹)",
                "Hospital Medicine Cost (₹)",
                "Other Charges (₹)",
                "Discount (₹)",
                "Total Bill (₹)",
                "Paid Amount (₹)",
                "Payment Mode",
                "Payment Status",
                "Doctor Notes / Advice",
                "Next Follow-up"
            )
            osw.write(headers.joinToString(",") { escapeCsv(it) } + "\n")

            val dateOnlyFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

            consultations.forEachIndexed { index, cons ->
                val items = prescriptionItems.filter { it.consultationId == cons.id }
                val medsStr = if (items.isNotEmpty()) {
                    items.joinToString("; ") {
                        "${it.medicineName} (${it.dose}, ${it.frequency}, ${it.duration}, Qty:${it.quantity}) [₹${it.totalPrice.toInt()}]"
                    }
                } else "None"

                val followUpStr = if (cons.nextFollowUpDate != null && cons.nextFollowUpDate > 0) {
                    dateOnlyFormat.format(Date(cons.nextFollowUpDate))
                } else "None"

                val row = listOf(
                    (index + 1).toString(),
                    cons.receiptNumber,
                    dateFormat.format(Date(cons.date)),
                    cons.patientName,
                    cons.chiefComplaints,
                    cons.diagnosis,
                    cons.bp,
                    cons.pulse,
                    cons.temperature,
                    cons.spo2,
                    cons.rbs,
                    cons.weight,
                    cons.servicesSummary.ifBlank { "General OPD" },
                    medsStr,
                    "%.2f".format(Locale.US, cons.consultationFee),
                    "%.2f".format(Locale.US, cons.serviceCharge),
                    "%.2f".format(Locale.US, cons.medicineCharge),
                    "%.2f".format(Locale.US, cons.otherCharge),
                    "%.2f".format(Locale.US, cons.discount),
                    "%.2f".format(Locale.US, cons.totalAmount),
                    "%.2f".format(Locale.US, cons.paidAmount),
                    cons.paymentMode,
                    cons.paymentStatus,
                    cons.doctorNotes,
                    followUpStr
                )
                osw.write(row.joinToString(",") { escapeCsv(it) } + "\n")
            }

            osw.flush()
            osw.close()
            fos.close()

            shareSpreadsheetFile(context, file, "OPD Consultations Spreadsheet (${consultations.size} Records)")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Exports Hospital Dispensary Inventory Stock into Spreadsheet
     */
    fun exportDispensaryInventorySpreadsheet(
        context: Context,
        profile: ClinicProfile?,
        inventoryList: List<InventoryItem>
    ) {
        if (inventoryList.isEmpty()) {
            Toast.makeText(context, "No inventory items to export", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        try {
            val cacheDir = File(context.cacheDir, "spreadsheets")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "Hospital_Dispensary_Stock_$timestampStr.csv")
            val fos = FileOutputStream(file)
            val osw = OutputStreamWriter(fos, StandardCharsets.UTF_8)

            osw.write("\uFEFF")
            osw.write("${escapeCsv(profile?.clinicName ?: "Aadhar Clinic")} - HOSPITAL DISPENSARY INVENTORY STOCK\n")
            osw.write("Date: ${escapeCsv(dateFormat.format(Date()))},Total Drug Items: ${escapeCsv(inventoryList.size)}\n\n")

            val headers = listOf(
                "S.No",
                "Medicine / Item Name",
                "Generic Composition",
                "Category",
                "Batch Number",
                "Expiry Date",
                "Current Stock",
                "Unit",
                "Min Alert Level",
                "Stock Status",
                "Buying Price (₹)",
                "Selling Price (₹)",
                "Stock Valuation at Buying (₹)",
                "Stock Valuation at Selling (₹)"
            )
            osw.write(headers.joinToString(",") { escapeCsv(it) } + "\n")

            var totalBuyingValuation = 0.0
            var totalSellingValuation = 0.0

            inventoryList.forEachIndexed { index, item ->
                val buyVal = item.currentStock * item.purchasePrice
                val sellVal = item.currentStock * item.sellingPrice
                totalBuyingValuation += buyVal
                totalSellingValuation += sellVal

                val status = if (item.currentStock <= 0) "OUT OF STOCK"
                else if (item.currentStock <= item.minThreshold) "LOW STOCK"
                else "IN STOCK"

                val row = listOf(
                    (index + 1).toString(),
                    item.name,
                    item.genericName,
                    item.category,
                    item.batchNumber,
                    item.expiryDate,
                    item.currentStock.toString(),
                    item.unit,
                    item.minThreshold.toString(),
                    status,
                    "%.2f".format(Locale.US, item.purchasePrice),
                    "%.2f".format(Locale.US, item.sellingPrice),
                    "%.2f".format(Locale.US, buyVal),
                    "%.2f".format(Locale.US, sellVal)
                )
                osw.write(row.joinToString(",") { escapeCsv(it) } + "\n")
            }

            osw.write("\n")
            val totals = listOf(
                "TOTALS", "", "", "", "", "",
                inventoryList.sumOf { it.currentStock }.toString(),
                "", "", "", "", "",
                "₹%.2f".format(Locale.US, totalBuyingValuation),
                "₹%.2f".format(Locale.US, totalSellingValuation)
            )
            osw.write(totals.joinToString(",") { escapeCsv(it) } + "\n")

            osw.flush()
            osw.close()
            fos.close()

            shareSpreadsheetFile(context, file, "Hospital Dispensary Stock Spreadsheet")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareSpreadsheetFile(context: Context, file: File, subject: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "Here is the spreadsheet file from Aadhar Clinic.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open / Share Spreadsheet (Google Sheets, Excel, WhatsApp)"))
    }
}
