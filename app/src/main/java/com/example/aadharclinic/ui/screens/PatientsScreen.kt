package com.example.aadharclinic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aadharclinic.data.model.Patient
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsScreen(
    patients: List<Patient>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onPatientClick: (Patient) -> Unit,
    onAddPatientClick: () -> Unit,
    onDeletePatient: (Patient) -> Unit = {},
    onExportSpreadsheet: () -> Unit = {}
) {
    var patientToDelete by remember { mutableStateOf<Patient?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar with clear button
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("patient_search_input"),
            placeholder = { Text("Search by name, mobile, or ID (e.g. AC-1001)") },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PATIENTS REGISTER (${patients.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline,
                letterSpacing = 1.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onExportSpreadsheet,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("export_spreadsheet_button")
                ) {
                    Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export CSV", style = MaterialTheme.typography.labelMedium)
                }

                FilledTonalButton(
                    onClick = onAddPatientClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_patient_top_button")
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Patient", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (patients.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.PersonSearch,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No patients matching \"$searchQuery\"" else "No patients registered yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(patients, key = { it.id }) { patient ->
                    PatientListItemCard(
                        patient = patient,
                        onClick = { onPatientClick(patient) },
                        onDeleteClick = { patientToDelete = patient }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog from Patients list
    if (patientToDelete != null) {
        val p = patientToDelete!!
        AlertDialog(
            onDismissRequest = { patientToDelete = null },
            icon = {
                Icon(
                    Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Delete Patient Record?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to permanently delete patient ${p.name} (${p.patientCode})?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ Notice: This will remove all associated OPD visit histories, prescriptions, daycare/IPD admissions, and billing invoices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = patientToDelete
                        patientToDelete = null
                        if (target != null) {
                            onDeletePatient(target)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_patient_btn")
                ) {
                    Text("Delete Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { patientToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PatientListItemCard(
    patient: Patient,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("patient_card_${patient.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Patient Code / Sex Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (patient.sex.lowercase() == "female") Color(0xFFFCE4EC) else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (patient.name.isNotBlank()) patient.name.take(1).uppercase() else "P",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (patient.sex.lowercase() == "female") Color(0xFFC2185B) else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = patient.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = patient.patientCode,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${patient.age} Yrs • ${patient.sex} • Ph: ${patient.mobile.ifBlank { "N/A" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                if (patient.allergies.isNotBlank() && patient.allergies.lowercase() != "none") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "Allergy",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Allergy: ${patient.allergies}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (onDeleteClick != null) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_patient_btn_${patient.id}")
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete Patient",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientEditDialog(
    initialPatient: Patient?,
    onDismiss: () -> Unit,
    onSave: (Patient) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialPatient?.name ?: "") }
    var age by remember { mutableStateOf(if (initialPatient != null && initialPatient.age > 0) initialPatient.age.toString() else "") }
    var sex by remember { mutableStateOf(initialPatient?.sex ?: "Male") }
    var mobile by remember { mutableStateOf(initialPatient?.mobile ?: "") }
    var address by remember { mutableStateOf(initialPatient?.address ?: "") }
    var bloodGroup by remember { mutableStateOf(initialPatient?.bloodGroup ?: "") }
    var allergies by remember { mutableStateOf(initialPatient?.allergies ?: "") }
    var medicalHistory by remember { mutableStateOf(initialPatient?.medicalHistory ?: "") }

    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmInEdit by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialPatient == null) "Register New Patient" else "Edit Patient Profile",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("Age (Yrs) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        // Sex Selector Chips
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("Sex", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Male", "Female", "Other").forEach { s ->
                                    FilterChip(
                                        selected = sex == s,
                                        onClick = { sex = s },
                                        label = { Text(s, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bloodGroup,
                            onValueChange = { bloodGroup = it },
                            label = { Text("Blood Group") },
                            placeholder = { Text("e.g. B+, O+") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("City / Area") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = allergies,
                        onValueChange = { allergies = it },
                        label = { Text("Known Drug Allergies") },
                        placeholder = { Text("e.g. Penicillin, Sulfa, None") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = medicalHistory,
                        onValueChange = { medicalHistory = it },
                        label = { Text("Past Medical History") },
                        placeholder = { Text("e.g. DM, HTN, Asthma, Surgeries") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (error != null) {
                    item {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Patient name is required."
                        return@Button
                    }
                    val parsedAge = age.toIntOrNull() ?: 0
                    if (parsedAge <= 0) {
                        error = "Please enter a valid age."
                        return@Button
                    }
                    val patientToSave = (initialPatient ?: Patient(
                        patientCode = "",
                        name = name.trim(),
                        age = parsedAge
                    )).copy(
                        name = name.trim(),
                        age = parsedAge,
                        sex = sex,
                        mobile = mobile.trim(),
                        address = address.trim(),
                        bloodGroup = bloodGroup.trim().uppercase(),
                        allergies = if (allergies.isBlank()) "None" else allergies.trim(),
                        medicalHistory = medicalHistory.trim()
                    )
                    onSave(patientToSave)
                }
            ) {
                Text(if (initialPatient == null) "Save Patient" else "Save Changes")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (initialPatient != null && onDelete != null) {
                    TextButton(
                        onClick = { showDeleteConfirmInEdit = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("dialog_delete_patient_button")
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (showDeleteConfirmInEdit) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmInEdit = false },
            icon = {
                Icon(
                    Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Delete Patient Record?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete ${initialPatient?.name ?: "this patient"}? This will permanently delete the patient and all related visits, prescriptions, admissions, and bills.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmInEdit = false
                        onDelete?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmInEdit = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
