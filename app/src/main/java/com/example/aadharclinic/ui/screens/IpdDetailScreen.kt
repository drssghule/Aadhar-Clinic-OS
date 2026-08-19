package com.example.aadharclinic.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aadharclinic.data.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpdDetailScreen(
    admission: IpdAdmission,
    patient: Patient?,
    dailyNotes: List<IpdDailyNote>,
    medicinesAdministered: List<IpdMedicineAdministered>,
    inventoryList: List<InventoryItem>,
    currency: String,
    onBackClick: () -> Unit,
    onAddDailyNote: (IpdDailyNote) -> Unit,
    onAdministerMedicine: (
        medicineName: String,
        inventoryItemId: Long?,
        dose: String,
        route: String,
        quantity: Int,
        administeredBy: String,
        unitCost: Double
    ) -> Unit,
    onDischargePatient: (
        dischargeCondition: String,
        dischargeSummary: String,
        dischargeAdvice: String,
        finalDiagnosis: String,
        totalAmount: Double,
        paidAmount: Double,
        paymentMode: String,
        paymentStatus: String,
        context: Context
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAdministerMedDialog by remember { mutableStateOf(false) }
    var showDischargeDialog by remember { mutableStateOf(false) }

    val daysAdmitted = remember(admission.admissionDate, admission.dischargeDate) {
        val endTime = admission.dischargeDate ?: System.currentTimeMillis()
        val diffMs = endTime - admission.admissionDate
        (diffMs / 86400000L).toInt().coerceAtLeast(1)
    }

    val bedChargesTotal = daysAdmitted * admission.roomChargePerDay
    val medicineChargesTotal = remember(medicinesAdministered) {
        medicinesAdministered.sumOf { it.totalCost }
    }
    val currentCalculatedBill = bedChargesTotal + medicineChargesTotal

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(admission.patientName, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Bed: ${admission.bedRoomNumber} • ${admission.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (admission.status == "Admitted") {
                        FilledTonalButton(
                            onClick = { showDischargeDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFE8F5E9)),
                            modifier = Modifier.testTag("discharge_action_button")
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF1B5E20), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Discharge", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // IPD Summary Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Admitting Diagnosis", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(admission.admittingDiagnosis, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Badge(
                                containerColor = if (admission.status == "Admitted") Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                                contentColor = if (admission.status == "Admitted") Color(0xFFE65100) else Color(0xFF1B5E20)
                            ) {
                                Text(
                                    text = admission.status,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Admitted: ${dateFormat.format(Date(admission.admissionDate))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Duration: $daysAdmitted Day(s)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Bill breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Bed Charges ($daysAdmitted d)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("$currency$bedChargesTotal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Medicines (${medicinesAdministered.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("$currency$medicineChargesTotal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Running Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "$currency$currentCalculatedBill",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Tab Selector: Daily Notes vs Administered Medicines
            item {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Daily Notes (${dailyNotes.size})", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Administered Drugs (${medicinesAdministered.size})", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Tab 0: Daily Notes
            if (selectedTab == 0) {
                if (admission.status == "Admitted") {
                    item {
                        Button(
                            onClick = { showAddNoteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.PostAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Doctor Round / Daily Note")
                        }
                    }
                }

                if (dailyNotes.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No daily progress notes recorded yet.", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                } else {
                    items(dailyNotes, key = { it.id }) { note ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = dateFormat.format(Date(note.timestamp)),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "By ${note.doctorOrNurse}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                val vitals = listOfNotNull(
                                    if (note.bp.isNotBlank()) "BP: ${note.bp}" else null,
                                    if (note.pulse.isNotBlank()) "Pulse: ${note.pulse}" else null,
                                    if (note.temp.isNotBlank()) "Temp: ${note.temp}°F" else null,
                                    if (note.spo2.isNotBlank()) "SpO2: ${note.spo2}%" else null
                                )
                                if (vitals.isNotEmpty()) {
                                    Text(
                                        text = vitals.joinToString(" • "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                Text(text = note.clinicalNotes, style = MaterialTheme.typography.bodyMedium)

                                if (note.treatmentGiven.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Treatment: ${note.treatmentGiven}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tab 1: Administered Medicines
            if (selectedTab == 1) {
                if (admission.status == "Admitted") {
                    item {
                        Button(
                            onClick = { showAdministerMedDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Medication, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Administer Medicine (Deducts Stock)")
                        }
                    }
                }

                if (medicinesAdministered.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No medicines administered yet in IPD.", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                } else {
                    items(medicinesAdministered, key = { it.id }) { med ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = med.medicineName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${med.dose} (${med.route}) • Qty: ${med.quantity} • By ${med.administeredBy}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = dateFormat.format(Date(med.administeredAt)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Text(
                                    text = "$currency${med.totalCost}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Daily Progress Note
    if (showAddNoteDialog) {
        var noteText by remember { mutableStateOf("") }
        var doctorName by remember { mutableStateOf("Doctor") }
        var bpVal by remember { mutableStateOf("120/80") }
        var pulseVal by remember { mutableStateOf("78") }
        var tempVal by remember { mutableStateOf("98.6") }
        var spo2Val by remember { mutableStateOf("98") }
        var treatmentVal by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Add Daily Doctor Round Note", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(value = bpVal, onValueChange = { bpVal = it }, label = { Text("BP") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = pulseVal, onValueChange = { pulseVal = it }, label = { Text("Pulse") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = tempVal, onValueChange = { tempVal = it }, label = { Text("Temp") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("Clinical Examination / Progress Notes *") },
                            placeholder = { Text("e.g. Afebrile, chest clear, abdomen soft, oral intake improving") },
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = treatmentVal,
                            onValueChange = { treatmentVal = it },
                            label = { Text("Treatment / Orders for Today") },
                            placeholder = { Text("e.g. Continue IV Fluids, Inj Pantop 40mg IV OD") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteText.isBlank()) return@Button
                        onAddDailyNote(
                            IpdDailyNote(
                                ipdAdmissionId = admission.id,
                                timestamp = System.currentTimeMillis(),
                                doctorOrNurse = doctorName.trim(),
                                clinicalNotes = noteText.trim(),
                                bp = bpVal.trim(),
                                pulse = pulseVal.trim(),
                                temp = tempVal.trim(),
                                spo2 = spo2Val.trim(),
                                treatmentGiven = treatmentVal.trim()
                            )
                        )
                        showAddNoteDialog = false
                    }
                ) {
                    Text("Save Round Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Administer Medicine
    if (showAdministerMedDialog) {
        var selectedItem by remember { mutableStateOf(inventoryList.firstOrNull()) }
        var dropdownExpanded by remember { mutableStateOf(false) }
        var customMedName by remember { mutableStateOf(selectedItem?.name ?: "") }
        var doseVal by remember { mutableStateOf("1 Amp / Tab") }
        var routeVal by remember { mutableStateOf("IV") }
        var qtyVal by remember { mutableStateOf("1") }
        var administeredBy by remember { mutableStateOf("Staff Nurse") }

        AlertDialog(
            onDismissRequest = { showAdministerMedDialog = false },
            title = { Text("Administer Medicine", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("Pick from In-House Clinic Inventory (Deducts Stock):", style = MaterialTheme.typography.labelSmall)
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = customMedName,
                                onValueChange = { customMedName = it },
                                label = { Text("Medicine Name") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                inventoryList.forEach { inv ->
                                    DropdownMenuItem(
                                        text = { Text("${inv.name} (Stock: ${inv.currentStock}, $currency${inv.sellingPrice})") },
                                        onClick = {
                                            selectedItem = inv
                                            customMedName = inv.name
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(value = doseVal, onValueChange = { doseVal = it }, label = { Text("Dose") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = routeVal, onValueChange = { routeVal = it }, label = { Text("Route (IV/IM/Oral)") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = qtyVal,
                                onValueChange = { qtyVal = it },
                                label = { Text("Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = administeredBy,
                            onValueChange = { administeredBy = it },
                            label = { Text("Administered By (Nurse/Doctor)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = qtyVal.toIntOrNull() ?: 1
                        val unitPrice = selectedItem?.sellingPrice ?: 50.0
                        onAdministerMedicine(
                            customMedName.trim(),
                            selectedItem?.id,
                            doseVal.trim(),
                            routeVal.trim(),
                            qty,
                            administeredBy.trim(),
                            unitPrice
                        )
                        showAdministerMedDialog = false
                    }
                ) {
                    Text("Administer & Deduct Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdministerMedDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Discharge Workflow
    if (showDischargeDialog) {
        var finalDiagnosis by remember { mutableStateOf(admission.admittingDiagnosis) }
        var condition by remember { mutableStateOf("Recovered / Stable") }
        var summary by remember { mutableStateOf("Patient admitted with ${admission.admittingDiagnosis}. Treated conservatively with IV fluids and medications. Responded well and vitals are stable.") }
        var advice by remember { mutableStateOf("Oral medications as prescribed for 5 days. High protein diet, adequate fluid intake. Review in OPD in 5 days.") }
        var paymentMode by remember { mutableStateOf("Cash") }
        var paidAmountVal by remember { mutableStateOf(currentCalculatedBill.toString()) }

        AlertDialog(
            onDismissRequest = { showDischargeDialog = false },
            title = { Text("Discharge from Daycare & Print Summary", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = finalDiagnosis,
                            onValueChange = { finalDiagnosis = it },
                            label = { Text("Final Diagnosis *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = condition,
                            onValueChange = { condition = it },
                            label = { Text("Condition on Daycare Discharge") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = summary,
                            onValueChange = { summary = it },
                            label = { Text("Daycare Course & Treatment Summary") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = advice,
                            onValueChange = { advice = it },
                            label = { Text("Discharge Advice / Home Medications") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Final Daycare Bill: $currency$currentCalculatedBill", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Daycare Bed/Care: $currency$bedChargesTotal | Medicines/IV: $currency$medicineChargesTotal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = paidAmountVal,
                                onValueChange = { paidAmountVal = it },
                                label = { Text("Paid Amount ($currency)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = paymentMode,
                                onValueChange = { paymentMode = it },
                                label = { Text("Mode") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paid = paidAmountVal.toDoubleOrNull() ?: currentCalculatedBill
                        val status = if (paid >= currentCalculatedBill) "Paid" else "Partial"
                        onDischargePatient(
                            condition.trim(),
                            summary.trim(),
                            advice.trim(),
                            finalDiagnosis.trim(),
                            currentCalculatedBill,
                            paid,
                            paymentMode.trim(),
                            status,
                            context
                        )
                        showDischargeDialog = false
                    }
                ) {
                    Text("Discharge & Print Summary (PDF)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDischargeDialog = false }) { Text("Cancel") }
            }
        )
    }
}
