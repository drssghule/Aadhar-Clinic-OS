package com.example.aadharclinic.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aadharclinic.data.model.BillInvoice
import com.example.aadharclinic.data.model.ClinicProfile
import com.example.aadharclinic.data.model.ClinicUser
import com.example.aadharclinic.data.model.Consultation
import com.example.aadharclinic.data.model.Patient
import com.example.aadharclinic.ui.components.DoctorBreakdownItem
import com.example.aadharclinic.ui.components.ReportPdfGenerator
import com.example.aadharclinic.ui.components.ReportRowItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    profile: ClinicProfile?,
    patients: List<Patient>,
    consultations: List<Consultation>,
    invoices: List<BillInvoice>,
    users: List<ClinicUser> = emptyList(),
    currency: String = "₹"
) {
    val context = LocalContext.current
    var selectedReportType by remember { mutableIntStateOf(0) } // 0: Daily Report, 1: Monthly Report
    var selectedDoctorFilter by remember { mutableStateOf("ALL") } // "ALL" or Doctor Name

    // Date / Period states
    val cal = remember { Calendar.getInstance() }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) } // 0-11
    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }

    val dayFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val displayDayFormat = SimpleDateFormat("dd MMMM yyyy (EEEE)", Locale.getDefault())
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val rowDateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Distinct Doctor list from consultations and users
    val availableDoctors = remember(consultations, users) {
        val fromCons = consultations.mapNotNull { it.doctorName.takeIf { d -> d.isNotBlank() } }
        val fromUsers = users.map { it.name }
        (listOf("Dr. Sanket Ghule", "Dr. A. B. Joshi") + fromCons + fromUsers).distinct()
    }

    // Daily Report Items Calculation
    val rawDailyConsultations = remember(consultations, selectedDateMillis) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis

        consultations.filter { it.date in startOfDay until endOfDay }
    }

    val dailyDoctorBreakdown = remember(rawDailyConsultations) {
        val grouped = rawDailyConsultations.groupBy { it.doctorName.ifBlank { "Dr. Sanket Ghule" } }
        grouped.map { (doc, list) ->
            DoctorBreakdownItem(
                doctorName = doc,
                patientCount = list.size,
                revenue = list.sumOf { it.totalAmount }
            )
        }
    }

    val filteredDailyConsultations = remember(rawDailyConsultations, selectedDoctorFilter) {
        if (selectedDoctorFilter == "ALL") rawDailyConsultations
        else rawDailyConsultations.filter { it.doctorName.equals(selectedDoctorFilter, ignoreCase = true) }
    }

    val dailyItems = remember(filteredDailyConsultations, patients) {
        filteredDailyConsultations.map { cons ->
            val p = patients.find { it.id == cons.patientId }
            val pName = cons.patientName.ifBlank { p?.name ?: "Unknown" }
            val ageSex = if (p != null) "${p.age}y/${p.sex}" else "-"
            val diag = cons.diagnosis.ifBlank { "OPD Visit" }
            val treat = cons.servicesSummary.ifBlank { "Consultation" }
            val doc = cons.doctorName.ifBlank { "Dr. Sanket Ghule" }
            val amt = cons.totalAmount

            ReportRowItem(
                dateStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(cons.date)),
                patientName = pName,
                ageSex = ageSex,
                diagnosis = diag,
                treatmentServices = treat,
                doctorName = doc,
                billingAmount = amt
            )
        }
    }

    val dailyTotalRevenue = remember(dailyItems) { dailyItems.sumOf { it.billingAmount } }
    val clinicWideDailyTotalRevenue = remember(rawDailyConsultations) { rawDailyConsultations.sumOf { it.totalAmount } }

    // Monthly Report Items Calculation
    val rawMonthlyConsultations = remember(consultations, selectedMonth, selectedYear) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis

        consultations.filter { it.date in startOfMonth until endOfMonth }
    }

    val monthlyDoctorBreakdown = remember(rawMonthlyConsultations) {
        val grouped = rawMonthlyConsultations.groupBy { it.doctorName.ifBlank { "Dr. Sanket Ghule" } }
        grouped.map { (doc, list) ->
            DoctorBreakdownItem(
                doctorName = doc,
                patientCount = list.size,
                revenue = list.sumOf { it.totalAmount }
            )
        }
    }

    val filteredMonthlyConsultations = remember(rawMonthlyConsultations, selectedDoctorFilter) {
        if (selectedDoctorFilter == "ALL") rawMonthlyConsultations
        else rawMonthlyConsultations.filter { it.doctorName.equals(selectedDoctorFilter, ignoreCase = true) }
    }

    val monthlyItems = remember(filteredMonthlyConsultations, patients) {
        filteredMonthlyConsultations.map { cons ->
            val p = patients.find { it.id == cons.patientId }
            val pName = cons.patientName.ifBlank { p?.name ?: "Unknown" }
            val ageSex = if (p != null) "${p.age}y/${p.sex}" else "-"
            val diag = cons.diagnosis.ifBlank { "OPD Visit" }
            val treat = cons.servicesSummary.ifBlank { "Consultation" }
            val doc = cons.doctorName.ifBlank { "Dr. Sanket Ghule" }
            val amt = cons.totalAmount

            ReportRowItem(
                dateStr = rowDateFormat.format(Date(cons.date)),
                patientName = pName,
                ageSex = ageSex,
                diagnosis = diag,
                treatmentServices = treat,
                doctorName = doc,
                billingAmount = amt
            )
        }
    }

    val monthlyTotalRevenue = remember(monthlyItems) { monthlyItems.sumOf { it.billingAmount } }
    val clinicWideMonthlyTotalRevenue = remember(rawMonthlyConsultations) { rawMonthlyConsultations.sumOf { it.totalAmount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "CLINIC & DOCTOR-WISE REPORTS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
            letterSpacing = 1.sp
        )
        Text(
            text = "Multi-Doctor Analytics & ISO A4 PDF Reports",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Daily vs Monthly Tab Segment
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedReportType == 0,
                onClick = { selectedReportType = 0 },
                label = { Text("Daily Report", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = selectedReportType == 1,
                onClick = { selectedReportType = 1 },
                label = { Text("Monthly Report", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Doctor Filter Chips (Clinic-Wide vs Doctor-wise)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filter:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            FilterChip(
                selected = selectedDoctorFilter == "ALL",
                onClick = { selectedDoctorFilter = "ALL" },
                label = { Text("Entire Clinic", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            availableDoctors.forEach { doc ->
                FilterChip(
                    selected = selectedDoctorFilter == doc,
                    onClick = { selectedDoctorFilter = doc },
                    label = { Text(doc.replace("Dr. ", ""), fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedReportType == 0) {
                // ==================== DAILY REPORT SECTION ====================
                item {
                    // Date Header & Navigation
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { selectedDateMillis -= 86400000L }) {
                                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Day")
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = displayDayFormat.format(Date(selectedDateMillis)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Date: ${dayFormat.format(Date(selectedDateMillis))} • ${if (selectedDoctorFilter == "ALL") "Entire Clinic" else selectedDoctorFilter}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            IconButton(onClick = { selectedDateMillis += 86400000L }) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Next Day")
                            }
                        }
                    }
                }

                // Daily Summary Card with Doctor Breakdown
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        if (selectedDoctorFilter == "ALL") "TOTAL CLINIC PATIENTS" else "DOCTOR'S PATIENTS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "${dailyItems.size}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Divider(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .width(1.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        if (selectedDoctorFilter == "ALL") "TOTAL CLINIC REVENUE" else "DOCTOR'S REVENUE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "$currency ${String.format("%,.0f", dailyTotalRevenue)}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            // Doctor Breakdown Sub-card
                            if (dailyDoctorBreakdown.isNotEmpty() && selectedDoctorFilter == "ALL") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "DOCTOR BREAKDOWN:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                dailyDoctorBreakdown.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "• ${item.doctorName}: ${item.patientCount} pts",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "$currency ${String.format("%,.0f", item.revenue)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Generate Daily PDF Button
                item {
                    Button(
                        onClick = {
                            val activeProfile = profile ?: ClinicProfile()
                            val dateLabel = dayFormat.format(Date(selectedDateMillis))
                            val filterDocLabel = if (selectedDoctorFilter == "ALL") "Entire Clinic" else selectedDoctorFilter
                            val generatedFile = ReportPdfGenerator.generateAndSaveReportPdf(
                                context = context,
                                profile = activeProfile,
                                reportTitle = "Daily Clinical & Billing Report",
                                dateLabel = dateLabel,
                                items = dailyItems,
                                totalPatients = dailyItems.size,
                                totalRevenue = dailyTotalRevenue,
                                doctorBreakdown = if (selectedDoctorFilter == "ALL") dailyDoctorBreakdown else emptyList(),
                                filterDoctorLabel = filterDocLabel
                            )

                            if (generatedFile != null) {
                                Toast.makeText(context, "Saved to Clinic OS/Daily Report/$dateLabel.pdf", Toast.LENGTH_LONG).show()
                                ReportPdfGenerator.openOrShareReportPdf(context, generatedFile, "Daily Report - $dateLabel")
                            } else {
                                Toast.makeText(context, "Failed to create PDF report", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_daily_pdf_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Open Daily PDF Report (A4)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                // Daily Patient List Header
                item {
                    Text(
                        text = "PATIENTS REGISTERED ON THIS DATE (${dailyItems.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Daily Table Rows
                if (dailyItems.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No patient consultations recorded for this selection.", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                } else {
                    itemsIndexed(dailyItems) { index, item ->
                        ReportPatientCard(index = index + 1, item = item, currency = currency)
                    }
                }

            } else {
                // ==================== MONTHLY REPORT SECTION ====================
                item {
                    // Month & Year Selector
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (selectedMonth == 0) {
                                    selectedMonth = 11
                                    selectedYear--
                                } else {
                                    selectedMonth--
                                }
                            }) {
                                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Month")
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${monthNames[selectedMonth]} $selectedYear",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Monthly Overview • ${if (selectedDoctorFilter == "ALL") "Entire Clinic" else selectedDoctorFilter}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            IconButton(onClick = {
                                if (selectedMonth == 11) {
                                    selectedMonth = 0
                                    selectedYear++
                                } else {
                                    selectedMonth++
                                }
                            }) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Next Month")
                            }
                        }
                    }
                }

                // Monthly Summary Card with Doctor Breakdown
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        if (selectedDoctorFilter == "ALL") "TOTAL CLINIC PATIENTS" else "DOCTOR'S PATIENTS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "${monthlyItems.size}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                Divider(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .width(1.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f)
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        if (selectedDoctorFilter == "ALL") "TOTAL CLINIC REVENUE" else "DOCTOR'S REVENUE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "$currency ${String.format("%,.0f", monthlyTotalRevenue)}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            // Doctor Breakdown Sub-card for Monthly
                            if (monthlyDoctorBreakdown.isNotEmpty() && selectedDoctorFilter == "ALL") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "DOCTOR BREAKDOWN (MONTH):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                monthlyDoctorBreakdown.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "• ${item.doctorName}: ${item.patientCount} pts",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "$currency ${String.format("%,.0f", item.revenue)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Generate Monthly PDF Button
                item {
                    Button(
                        onClick = {
                            val activeProfile = profile ?: ClinicProfile()
                            val dateLabel = "${monthNames[selectedMonth]}-$selectedYear"
                            val filterDocLabel = if (selectedDoctorFilter == "ALL") "Entire Clinic" else selectedDoctorFilter
                            val generatedFile = ReportPdfGenerator.generateAndSaveReportPdf(
                                context = context,
                                profile = activeProfile,
                                reportTitle = "Monthly Clinical & Billing Report",
                                dateLabel = dateLabel,
                                items = monthlyItems,
                                totalPatients = monthlyItems.size,
                                totalRevenue = monthlyTotalRevenue,
                                doctorBreakdown = if (selectedDoctorFilter == "ALL") monthlyDoctorBreakdown else emptyList(),
                                filterDoctorLabel = filterDocLabel
                            )

                            if (generatedFile != null) {
                                Toast.makeText(context, "Saved to Clinic OS/Monthly Report/$dateLabel.pdf", Toast.LENGTH_LONG).show()
                                ReportPdfGenerator.openOrShareReportPdf(context, generatedFile, "Monthly Report - $dateLabel")
                            } else {
                                Toast.makeText(context, "Failed to create PDF report", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_monthly_pdf_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Open Monthly PDF Report (A4)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                // Monthly Patient List Header
                item {
                    Text(
                        text = "PATIENT VISITS IN ${monthNames[selectedMonth].uppercase()} $selectedYear (${monthlyItems.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Monthly Table Rows
                if (monthlyItems.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No patient consultations recorded for this month.", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                } else {
                    itemsIndexed(monthlyItems) { index, item ->
                        ReportPatientCard(index = index + 1, item = item, currency = currency)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportPatientCard(
    index: Int,
    item: ReportRowItem,
    currency: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$index",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.patientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "$currency ${String.format("%,.0f", item.billingAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Age/Sex: ${item.ageSex} • Time: ${item.dateStr}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                if (item.doctorName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = item.doctorName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Diagnosis: ${item.diagnosis}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Services: ${item.treatmentServices}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
