package com.example.aadharclinic.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aadharclinic.data.model.*
import com.example.aadharclinic.ui.components.PrescriptionAction
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility to format clinical durations into compact badge strings (e.g. "7 days" -> "7D", "5 Days" -> "5D").
 */
fun formatDurationCompact(duration: String): String {
    val trimmed = duration.trim()
    if (trimmed.isBlank()) return "5D"
    
    // Already in compact format like 7D, 3D, 5D, 14D, 1M, 2W
    if (trimmed.matches(Regex("^[0-9]+[DdWwMmYy]$", RegexOption.IGNORE_CASE))) {
        return trimmed.uppercase()
    }
    
    // Parse "7 days" / "7 Days" / "7 day" / "7"
    val daysMatch = Regex("^([0-9]+)\\s*(?:days?|divas?|d)$", RegexOption.IGNORE_CASE).find(trimmed)
    if (daysMatch != null) {
        return "${daysMatch.groupValues[1]}D"
    }
    
    // Parse "1 week" / "2 weeks"
    val weekMatch = Regex("^([0-9]+)\\s*(?:weeks?|w)$", RegexOption.IGNORE_CASE).find(trimmed)
    if (weekMatch != null) {
        val numWeeks = weekMatch.groupValues[1].toIntOrNull() ?: 1
        return "${numWeeks * 7}D"
    }
    
    // Parse "1 month"
    val monthMatch = Regex("^([0-9]+)\\s*(?:months?|m)$", RegexOption.IGNORE_CASE).find(trimmed)
    if (monthMatch != null) {
        return "${monthMatch.groupValues[1]}M"
    }
    
    // Just a raw number
    val rawNum = trimmed.toIntOrNull()
    if (rawNum != null) {
        return "${rawNum}D"
    }
    
    return trimmed
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationScreen(
    initialPatientId: Long? = null,
    patients: List<Patient>,
    inventoryList: List<InventoryItem>,
    presets: List<QuickPreset> = emptyList(),
    servicesList: List<ClinicService> = emptyList(),
    profile: ClinicProfile?,
    currentDoctorName: String? = null,
    onBackClick: () -> Unit,
    onSavePreset: (QuickPreset) -> Unit = {},
    onDeletePreset: (QuickPreset) -> Unit = {},
    onUpdateMedicineDefault: (itemId: Long, frequency: String, duration: String, dose: String, instructions: String) -> Unit = { _, _, _, _, _ -> },
    onSaveConsultation: (
        consultation: Consultation,
        items: List<PrescriptionItem>,
        patientMobile: String,
        generatePdf: Boolean,
        action: PrescriptionAction,
        context: Context
    ) -> Unit
) {
    val context = LocalContext.current
    val currency = profile?.currency ?: "₹"
    val consultationDate = remember { System.currentTimeMillis() }

    // Selected Patient State
    var selectedPatient by remember {
        mutableStateOf(patients.find { it.id == initialPatientId } ?: patients.firstOrNull())
    }
    var patientExpanded by remember { mutableStateOf(false) }

    // Vitals & Clinical notes
    var chiefComplaints by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var bp by remember { mutableStateOf("120/80") }
    var pulse by remember { mutableStateOf("76") }
    var temp by remember { mutableStateOf("98.6") }
    var spo2 by remember { mutableStateOf("99") }
    var weight by remember { mutableStateOf("") }
    var rbs by remember { mutableStateOf("") }
    var doctorNotes by remember { mutableStateOf("") }

    // Follow-up Date (Automatically 7 days after consultation, editable)
    val defaultFollowUp = remember(consultationDate) {
        consultationDate + 7L * 24 * 60 * 60 * 1000L
    }
    var followUpDateMillis by remember { mutableStateOf(defaultFollowUp) }
    var selectedFollowUpDays by remember { mutableIntStateOf(7) }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // 1. Prescribed Medicines List (Default minimal treatment rows)
    var prescriptionItems by remember {
        mutableStateOf<List<PrescriptionItem>>(
            listOf(
                PrescriptionItem(
                    consultationId = 0,
                    medicineName = "Pantoprazole 40 mg",
                    strength = "40 mg",
                    dose = "1 Tab",
                    route = "Oral",
                    frequency = "BD",
                    duration = "7 days",
                    quantity = 14,
                    instructions = "Before Food (AC)",
                    sourceType = MedicineSourceType.CLINIC_STOCK
                ),
                PrescriptionItem(
                    consultationId = 0,
                    medicineName = "Paracetamol 650 mg",
                    strength = "650 mg",
                    dose = "1 Tab",
                    route = "Oral",
                    frequency = "TID",
                    duration = "3 days",
                    quantity = 9,
                    instructions = "After Food (PC)",
                    sourceType = MedicineSourceType.CLINIC_STOCK
                )
            )
        )
    }

    // Presets list with fallback
    val displayPresets = remember(presets) {
        if (presets.isNotEmpty()) presets else listOf(
            QuickPreset(presetName = "Antacid", medicineName = "Pantoprazole 40 mg", frequency = "BD", defaultDuration = "7 days", instructions = "Before Food"),
            QuickPreset(presetName = "Fever/Pain", medicineName = "Paracetamol 650 mg", frequency = "TID", defaultDuration = "3 days", instructions = "After Food"),
            QuickPreset(presetName = "Antibiotic", medicineName = "Amoxyclav 625 mg", frequency = "BD", defaultDuration = "5 days", instructions = "After Food"),
            QuickPreset(presetName = "Antiallergic", medicineName = "Cetirizine 10 mg", frequency = "OD", defaultDuration = "5 days", instructions = "At Bedtime"),
            QuickPreset(presetName = "Cough Syrup", medicineName = "Ascoril D Syrup", dose = "5 ml", frequency = "TID", defaultDuration = "5 days", instructions = "After Food"),
            QuickPreset(presetName = "Muscle Relax", medicineName = "Aceclofenac + Paracetamol", frequency = "BD", defaultDuration = "3 days", instructions = "After Food"),
            QuickPreset(presetName = "Multivitamin", medicineName = "Becosules / B-Complex", frequency = "OD", defaultDuration = "15 days", instructions = "After Breakfast"),
            QuickPreset(presetName = "Probiotic", medicineName = "Bacillus Clausii", frequency = "BD", defaultDuration = "5 days", instructions = "After Food")
        )
    }

    // Modal state for editing a specific medicine row for this patient
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var showPresetManagerDialog by remember { mutableStateOf(false) }
    var presetToEditInDialog by remember { mutableStateOf<QuickPreset?>(null) }

    // Medicine Search state
    var searchMedicineQuery by remember { mutableStateOf("") }
    var searchDropdownExpanded by remember { mutableStateOf(false) }

    // 2. Predefined Service Checkboxes for Billing (Consultation, Injection, IV, etc.)
    val resolvedServices = remember(servicesList) {
        PredefinedClinicServices.STANDARD_SERVICES.map { def ->
            val match = servicesList.find { it.serviceName.equals(def.serviceName, ignoreCase = true) }
            val price = match?.defaultPrice ?: def.defaultPrice
            def.copy(defaultPrice = price)
        }
    }

    val checkedServices = remember {
        mutableStateMapOf<String, Boolean>().apply {
            resolvedServices.forEach { svc ->
                this[svc.serviceName] = (svc.serviceName.equals("Consultation", ignoreCase = true))
            }
        }
    }

    val autoCalculatedTotal = remember(checkedServices.values.toList(), resolvedServices) {
        resolvedServices.filter { checkedServices[it.serviceName] == true }.sumOf { it.defaultPrice }
    }

    var customAmountStr by remember(autoCalculatedTotal) {
        mutableStateOf(autoCalculatedTotal.toInt().toString())
    }

    var paymentMode by remember { mutableStateOf("Cash") }
    var paymentStatus by remember { mutableStateOf("Paid") }

    val activeServicesSummary = remember(checkedServices.values.toList(), resolvedServices) {
        resolvedServices
            .filter { checkedServices[it.serviceName] == true }
            .joinToString(", ") { "${it.serviceName} ($currency${it.defaultPrice.toInt()})" }
    }

    fun buildConsultationObject(): Consultation? {
        if (selectedPatient == null) return null
        val finalBill = customAmountStr.toDoubleOrNull() ?: autoCalculatedTotal
        val actualPaid = if (paymentStatus == "Paid") finalBill else 0.0

        return Consultation(
            patientId = selectedPatient!!.id,
            patientName = selectedPatient!!.name,
            doctorName = currentDoctorName ?: profile?.doctorName ?: "Dr. Sanket Ghule",
            date = consultationDate,
            chiefComplaints = chiefComplaints.trim(),
            bp = bp.trim(),
            pulse = pulse.trim(),
            temperature = temp.trim(),
            spo2 = spo2.trim(),
            weight = weight.trim(),
            rbs = rbs.trim(),
            diagnosis = diagnosis.trim().ifBlank { "General Clinical Checkup" },
            doctorNotes = doctorNotes.trim(),
            nextFollowUpDate = followUpDateMillis,
            followUpInstructions = "Follow-up on ${dateFormat.format(Date(followUpDateMillis))}",
            consultationFee = if (checkedServices["Consultation"] == true) (resolvedServices.find { it.serviceName == "Consultation" }?.defaultPrice ?: 300.0) else 0.0,
            serviceCharge = finalBill,
            servicesSummary = activeServicesSummary,
            medicineCharge = 0.0, // Strict rule: No medicine billing
            totalAmount = finalBill,
            paidAmount = actualPaid,
            paymentStatus = paymentStatus,
            paymentMode = paymentMode
        )
    }

    // Function to instantly add preset to treatment without opening any duration form
    fun addPresetDirectly(preset: QuickPreset) {
        val newItem = PrescriptionItem(
            consultationId = 0,
            medicineName = preset.medicineName,
            strength = preset.strength,
            dose = preset.dose,
            route = preset.route,
            frequency = preset.frequency,
            duration = preset.defaultDuration, // Configured default duration
            quantity = preset.quantity,
            instructions = preset.instructions,
            sourceType = MedicineSourceType.CLINIC_STOCK
        )
        prescriptionItems = prescriptionItems + newItem
    }

    // Function to add medicine from search using its configured personal default duration & frequency
    fun addMedicineFromInventory(item: InventoryItem) {
        val fullName = if (item.strength.isNotBlank() && !item.name.contains(item.strength, ignoreCase = true)) {
            "${item.name} ${item.strength}"
        } else {
            item.name
        }
        val newItem = PrescriptionItem(
            consultationId = 0,
            medicineName = fullName,
            strength = item.strength,
            dose = item.defaultDose.ifBlank { "1 Tab" },
            route = item.defaultRoute.ifBlank { "Oral" },
            frequency = item.defaultFrequency.ifBlank { "BD" },
            duration = item.defaultDuration.ifBlank { "5 days" },
            quantity = 10,
            instructions = item.defaultInstructions.ifBlank { "After Food" },
            sourceType = MedicineSourceType.CLINIC_STOCK,
            inventoryItemId = item.id
        )
        prescriptionItems = prescriptionItems + newItem
        searchMedicineQuery = ""
        searchDropdownExpanded = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("OPD Consultation & Prescription", fontWeight = FontWeight.Bold)
                        Text(
                            text = selectedPatient?.let { "${it.name} (${it.patientCode} • ${it.age}y/${it.sex})" } ?: "Select Patient",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Patient Selector Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "1. PATIENT SELECTION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ExposedDropdownMenuBox(
                            expanded = patientExpanded,
                            onExpandedChange = { patientExpanded = !patientExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedPatient?.let { "${it.name} (${it.patientCode} • ${it.age}y/${it.sex})" } ?: "Select Patient",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = patientExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = patientExpanded,
                                onDismissRequest = { patientExpanded = false }
                            ) {
                                patients.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("${p.name} (${p.patientCode} • ${p.age}y/${p.sex})") },
                                        onClick = {
                                            selectedPatient = p
                                            patientExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Vitals Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "2. PATIENT VITALS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = bp,
                                onValueChange = { bp = it },
                                label = { Text("BP (mmHg)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pulse,
                                onValueChange = { pulse = it },
                                label = { Text("Pulse (bpm)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = temp,
                                onValueChange = { temp = it },
                                label = { Text("Temp (°F)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = spo2,
                                onValueChange = { spo2 = it },
                                label = { Text("SpO2 (%)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = weight,
                                onValueChange = { weight = it },
                                label = { Text("Weight (kg)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = rbs,
                                onValueChange = { rbs = it },
                                label = { Text("RBS (mg/dL)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. Compact Complaints & Diagnosis Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "3. CHIEF COMPLAINTS & DIAGNOSIS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = chiefComplaints,
                            onValueChange = { chiefComplaints = it },
                            label = { Text("Chief Complaints (Compact)") },
                            placeholder = { Text("e.g. Fever x 3 days, bodyache, throat pain") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = diagnosis,
                            onValueChange = { diagnosis = it },
                            label = { Text("Diagnosis *") },
                            placeholder = { Text("e.g. Acute Viral Fever, Gastritis, URTI") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = doctorNotes,
                            onValueChange = { doctorNotes = it },
                            label = { Text("Doctor Advice & Precautions") },
                            placeholder = { Text("e.g. Plenty of fluids, warm salt water gargling") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 4. OPD Treatment & Quick Presets Section (Minimal Treatment Rows)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Section Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "4. TREATMENT / ℞ PRESCRIPTION",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Tap preset to add instantly • Tap row to adjust duration for this patient",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp
                                )
                            }

                            TextButton(
                                onClick = {
                                    presetToEditInDialog = null
                                    showPresetManagerDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Presets", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // A. Quick Preset Chips Bar (Instant 1-Tap Addition with Saved Default Durations)
                        Text(
                            text = "Quick Presets (1-Tap Add):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            displayPresets.forEach { preset ->
                                val compactDur = formatDurationCompact(preset.defaultDuration)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .clickable { addPresetDirectly(preset) }
                                        .testTag("preset_chip_${preset.presetName.lowercase().replace(" ", "_")}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = preset.presetName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(1.dp)
                                        ) {
                                            Text(
                                                text = "${preset.frequency} • $compactDur",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // B. Medicine Search Autocomplete (Supports Drug Defaults)
                        ExposedDropdownMenuBox(
                            expanded = searchDropdownExpanded,
                            onExpandedChange = { searchDropdownExpanded = !searchDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = searchMedicineQuery,
                                onValueChange = {
                                    searchMedicineQuery = it
                                    searchDropdownExpanded = it.isNotBlank()
                                },
                                label = { Text("Search & Add Drug (Uses Drug Default Duration)") },
                                placeholder = { Text("e.g. Paracetamol, Pantocid, Azithromycin...") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = {
                                    if (searchMedicineQuery.isNotBlank()) {
                                        IconButton(onClick = {
                                            searchMedicineQuery = ""
                                            searchDropdownExpanded = false
                                        }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            val matchingInventory = inventoryList.filter {
                                it.name.contains(searchMedicineQuery, ignoreCase = true) ||
                                        it.genericName.contains(searchMedicineQuery, ignoreCase = true)
                            }

                            if (searchDropdownExpanded && searchMedicineQuery.isNotBlank()) {
                                ExposedDropdownMenu(
                                    expanded = searchDropdownExpanded,
                                    onDismissRequest = { searchDropdownExpanded = false }
                                ) {
                                    matchingInventory.take(8).forEach { inv ->
                                        val durCompact = formatDurationCompact(inv.defaultDuration)
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(inv.name, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        text = "Default: ${inv.defaultFrequency} • $durCompact (${inv.defaultDuration}) • ${inv.defaultInstructions}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                addMedicineFromInventory(inv)
                                            }
                                        )
                                    }

                                    // Option to add as custom drug
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Add '${searchMedicineQuery.trim()}' as new drug", fontWeight = FontWeight.SemiBold)
                                            }
                                        },
                                        onClick = {
                                            prescriptionItems = prescriptionItems + PrescriptionItem(
                                                consultationId = 0,
                                                medicineName = searchMedicineQuery.trim(),
                                                strength = "",
                                                dose = "1 Tab",
                                                frequency = "BD",
                                                duration = "5 days",
                                                quantity = 10,
                                                instructions = "After Food",
                                                sourceType = MedicineSourceType.CLINIC_STOCK
                                            )
                                            searchMedicineQuery = ""
                                            searchDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // C. Minimal Treatment Rows Header & List
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Prescribed Treatment (${prescriptionItems.size} Drugs):",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            TextButton(
                                onClick = {
                                    prescriptionItems = prescriptionItems + PrescriptionItem(
                                        consultationId = 0,
                                        medicineName = "New Medicine",
                                        strength = "",
                                        dose = "1 Tab",
                                        frequency = "BD",
                                        duration = "5 days",
                                        quantity = 10,
                                        instructions = "After Food",
                                        sourceType = MedicineSourceType.CLINIC_STOCK
                                    )
                                    // Open edit modal for the newly added item
                                    editingItemIndex = prescriptionItems.size
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Row")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (prescriptionItems.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No medications prescribed. Tap a Quick Preset above or search a drug to add.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            // Display Minimal Compact Rows
                            prescriptionItems.forEachIndexed { index, item ->
                                MinimalTreatmentRow(
                                    item = item,
                                    index = index,
                                    onClick = { editingItemIndex = index },
                                    onDelete = {
                                        val list = prescriptionItems.toMutableList()
                                        list.removeAt(index)
                                        prescriptionItems = list
                                    }
                                )
                                if (index < prescriptionItems.size - 1) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 5. Follow-up Section (Auto-calculated 7 days, editable)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "5. FOLLOW-UP DATE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = dateFormat.format(Date(followUpDateMillis)),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Days Selectors (3 days, 5 days, 7 days, 10 days, 14 days)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(3, 5, 7, 10, 14, 30).forEach { days ->
                                FilterChip(
                                    selected = selectedFollowUpDays == days,
                                    onClick = {
                                        selectedFollowUpDays = days
                                        followUpDateMillis = consultationDate + days * 24 * 60 * 60 * 1000L
                                    },
                                    label = { Text("${days}D", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                )
                            }
                        }
                    }
                }
            }

            // 6. Predefined Services & Fast Billing Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "6. PREDEFINED SERVICES & BILLING",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Fast Service Checkboxes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Render dynamic grid of predefined service checkboxes
                        resolvedServices.chunked(2).forEach { rowServices ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowServices.forEach { service ->
                                    val isChecked = checkedServices[service.serviceName] ?: false
                                    FilterChip(
                                        selected = isChecked,
                                        onClick = {
                                            checkedServices[service.serviceName] = !isChecked
                                        },
                                        label = {
                                            Text(
                                                text = "${service.serviceName} ($currency${service.defaultPrice.toInt()})",
                                                fontSize = 12.sp,
                                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = if (isChecked) {
                                            {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowServices.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Final Calculated & Editable Bill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Final Billing Amount:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Auto: $currency$autoCalculatedTotal (Editable)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            OutlinedTextField(
                                value = customAmountStr,
                                onValueChange = { customAmountStr = it },
                                label = { Text("Amount ($currency)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.width(130.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Payment Mode & Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = paymentMode,
                                onValueChange = { paymentMode = it },
                                label = { Text("Payment Mode") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = paymentStatus,
                                onValueChange = { paymentStatus = it },
                                label = { Text("Status") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 7. Prominent Print & Save Action Buttons
                        Button(
                            onClick = {
                                val cons = buildConsultationObject() ?: return@Button
                                onSaveConsultation(
                                    cons,
                                    prescriptionItems,
                                    selectedPatient?.mobile ?: "",
                                    true,
                                    PrescriptionAction.PRINT_AND_SAVE,
                                    context
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("generate_prescription_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Print Prescription (A4 PDF)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    val cons = buildConsultationObject() ?: return@FilledTonalButton
                                    onSaveConsultation(
                                        cons,
                                        prescriptionItems,
                                        selectedPatient?.mobile ?: "",
                                        true,
                                        PrescriptionAction.SAVE_TO_DEVICE,
                                        context
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val cons = buildConsultationObject() ?: return@FilledTonalButton
                                    onSaveConsultation(
                                        cons,
                                        prescriptionItems,
                                        selectedPatient?.mobile ?: "",
                                        true,
                                        PrescriptionAction.SHARE,
                                        context
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val cons = buildConsultationObject() ?: return@OutlinedButton
                                    onSaveConsultation(
                                        cons,
                                        prescriptionItems,
                                        selectedPatient?.mobile ?: "",
                                        false,
                                        PrescriptionAction.SAVE_TO_DEVICE,
                                        context
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("save_only_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Only", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet / Dialog to edit a specific medicine row for this patient (e.g. change 7D to 3D for just this patient)
    if (editingItemIndex != null && editingItemIndex!! in prescriptionItems.indices) {
        val currentItem = prescriptionItems[editingItemIndex!!]
        RxMedicineEditDialog(
            item = currentItem,
            inventoryList = inventoryList,
            onDismiss = { editingItemIndex = null },
            onSaveForPatient = { updatedItem, updateSavedDefault ->
                val list = prescriptionItems.toMutableList()
                list[editingItemIndex!!] = updatedItem
                prescriptionItems = list

                // If doctor also checked "Save as default duration & frequency for this medicine"
                if (updateSavedDefault && currentItem.inventoryItemId != null) {
                    onUpdateMedicineDefault(
                        currentItem.inventoryItemId,
                        updatedItem.frequency,
                        updatedItem.duration,
                        updatedItem.dose,
                        updatedItem.instructions
                    )
                }
                editingItemIndex = null
            }
        )
    }

    // Quick Preset Manager / Edit Dialog
    if (showPresetManagerDialog) {
        QuickPresetManagerDialog(
            presets = displayPresets,
            initialPresetToEdit = presetToEditInDialog,
            onDismiss = {
                showPresetManagerDialog = false
                presetToEditInDialog = null
            },
            onSavePreset = { preset ->
                onSavePreset(preset)
                showPresetManagerDialog = false
                presetToEditInDialog = null
            },
            onDeletePreset = { preset ->
                onDeletePreset(preset)
            }
        )
    }
}

/**
 * Minimal Treatment Row as specified by user:
 * Pantoprazole 40 mg    BD    7D    ✕
 * Paracetamol 500 mg    TID   3D    ✕
 *
 * Keep this as a single compact row.
 * Does not open a form for duration unless doctor taps the medicine row to edit it.
 */
@Composable
fun MinimalTreatmentRow(
    item: PrescriptionItem,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val compactDur = formatDurationCompact(item.duration)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("treatment_row_$index")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Drug Name & Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Index badge
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.medicineName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.instructions.isNotBlank() || item.dose.isNotBlank()) {
                        Text(
                            text = "${item.dose} • ${item.instructions}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Frequency Pill (e.g. BD, TID, 1-0-1)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = item.frequency.ifBlank { "BD" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Duration Pill (e.g. 7D, 3D, 5D)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = compactDur,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Close / Delete Button with 48dp minimum touch target
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("remove_drug_${index}")
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove Drug",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Compact Edit Dialog for adjusting a prescription item's duration/frequency for this patient.
 */
@Composable
fun RxMedicineEditDialog(
    item: PrescriptionItem,
    inventoryList: List<InventoryItem>,
    onDismiss: () -> Unit,
    onSaveForPatient: (PrescriptionItem, Boolean) -> Unit
) {
    var medName by remember { mutableStateOf(item.medicineName) }
    var dose by remember { mutableStateOf(item.dose) }
    var frequency by remember { mutableStateOf(item.frequency) }
    var duration by remember { mutableStateOf(item.duration) }
    var instructions by remember { mutableStateOf(item.instructions) }
    var saveAsDefault by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Edit Prescription Item", fontWeight = FontWeight.Bold)
                Text(
                    "Change duration/frequency for this patient",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = medName,
                    onValueChange = { medName = it },
                    label = { Text("Drug Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dose,
                        onValueChange = { dose = it },
                        label = { Text("Dose") },
                        placeholder = { Text("1 Tab") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("Frequency") },
                        placeholder = { Text("BD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Frequency Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("OD", "BD", "TID", "1-0-1", "1-1-1", "SOS", "HS").forEach { freq ->
                        FilterChip(
                            selected = frequency.equals(freq, ignoreCase = true),
                            onClick = { frequency = freq },
                            label = { Text(freq, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration (e.g. 3 days, 5 days, 7D)") },
                    placeholder = { Text("e.g. 3 days") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Duration Shortcuts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("1D", "3D", "5D", "7D", "10D", "14D", "1M").forEach { dur ->
                        FilterChip(
                            selected = formatDurationCompact(duration).equals(dur, ignoreCase = true),
                            onClick = { duration = dur },
                            label = { Text(dur, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions") },
                    placeholder = { Text("After Food") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Instruction Shortcuts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Before Food", "After Food", "At Bedtime", "जेवणापूर्वी", "जेवणानंतर").forEach { inst ->
                        FilterChip(
                            selected = instructions.equals(inst, ignoreCase = true),
                            onClick = { instructions = inst },
                            label = { Text(inst, fontSize = 10.sp, fontWeight = FontWeight.Medium) }
                        )
                    }
                }

                // Checkbox to optionally save as default for this medicine
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { saveAsDefault = !saveAsDefault }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = saveAsDefault,
                        onCheckedChange = { saveAsDefault = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save as default duration & frequency for future prescriptions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = item.copy(
                        medicineName = medName.trim(),
                        dose = dose.trim().ifBlank { "1 Tab" },
                        frequency = frequency.trim().ifBlank { "BD" },
                        duration = duration.trim().ifBlank { "5 days" },
                        instructions = instructions.trim()
                    )
                    onSaveForPatient(updated, saveAsDefault)
                }
            ) {
                Text("Update for This Patient")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog to configure and manage Quick Presets with default duration once.
 */
@Composable
fun QuickPresetManagerDialog(
    presets: List<QuickPreset>,
    initialPresetToEdit: QuickPreset? = null,
    onDismiss: () -> Unit,
    onSavePreset: (QuickPreset) -> Unit,
    onDeletePreset: (QuickPreset) -> Unit
) {
    var isEditing by remember { mutableStateOf(initialPresetToEdit != null) }
    var currentPreset by remember {
        mutableStateOf(
            initialPresetToEdit ?: QuickPreset(
                presetName = "",
                medicineName = "",
                strength = "40 mg",
                dose = "1 Tab",
                frequency = "BD",
                route = "Oral",
                defaultDuration = "7 days",
                instructions = "Before Food"
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Edit Quick Preset" else "Quick Presets & Default Durations",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                if (!isEditing) {
                    IconButton(
                        onClick = {
                            currentPreset = QuickPreset(
                                presetName = "",
                                medicineName = "",
                                strength = "40 mg",
                                dose = "1 Tab",
                                frequency = "BD",
                                route = "Oral",
                                defaultDuration = "7 days",
                                instructions = "Before Food"
                            )
                            isEditing = true
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add New Preset")
                    }
                }
            }
        },
        text = {
            if (isEditing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = currentPreset.presetName,
                        onValueChange = { currentPreset = currentPreset.copy(presetName = it) },
                        label = { Text("Preset Name * (e.g. Antacid)") },
                        placeholder = { Text("e.g. Antacid, Fever, Antibiotic") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = currentPreset.medicineName,
                        onValueChange = { currentPreset = currentPreset.copy(medicineName = it) },
                        label = { Text("Medicine Name *") },
                        placeholder = { Text("e.g. Pantoprazole 40 mg") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = currentPreset.strength,
                            onValueChange = { currentPreset = currentPreset.copy(strength = it) },
                            label = { Text("Strength") },
                            placeholder = { Text("40 mg") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = currentPreset.dose,
                            onValueChange = { currentPreset = currentPreset.copy(dose = it) },
                            label = { Text("Dose") },
                            placeholder = { Text("1 Tab") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = currentPreset.frequency,
                            onValueChange = { currentPreset = currentPreset.copy(frequency = it) },
                            label = { Text("Frequency *") },
                            placeholder = { Text("BD") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = currentPreset.defaultDuration,
                            onValueChange = { currentPreset = currentPreset.copy(defaultDuration = it) },
                            label = { Text("Default Duration *") },
                            placeholder = { Text("7 days") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Quick Duration suggestions for preset configuration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("3 days", "5 days", "7 days", "10 days", "14 days", "1 month").forEach { dur ->
                            FilterChip(
                                selected = currentPreset.defaultDuration.equals(dur, ignoreCase = true),
                                onClick = { currentPreset = currentPreset.copy(defaultDuration = dur) },
                                label = { Text(dur, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = currentPreset.instructions,
                        onValueChange = { currentPreset = currentPreset.copy(instructions = it) },
                        label = { Text("Instructions") },
                        placeholder = { Text("Before Food") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // List of configured presets
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    Text(
                        text = "Configured Doctor Presets (Tap to edit default duration):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(presets) { _, preset ->
                            val durCompact = formatDurationCompact(preset.defaultDuration)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentPreset = preset
                                        isEditing = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = preset.presetName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    text = "${preset.frequency} • $durCompact",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${preset.medicineName} • ${preset.instructions}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            currentPreset = preset
                                            isEditing = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                Button(
                    onClick = {
                        if (currentPreset.presetName.isNotBlank() && currentPreset.medicineName.isNotBlank()) {
                            onSavePreset(currentPreset)
                            isEditing = false
                        }
                    }
                ) {
                    Text("Save Preset")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            if (isEditing) {
                TextButton(onClick = { isEditing = false }) {
                    Text("Back to Presets")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
