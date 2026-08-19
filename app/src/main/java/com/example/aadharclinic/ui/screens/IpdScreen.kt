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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aadharclinic.data.model.IpdAdmission
import com.example.aadharclinic.data.model.Patient
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpdScreen(
    admissions: List<IpdAdmission>,
    patients: List<Patient>,
    currency: String,
    onAdmissionClick: (IpdAdmission) -> Unit,
    onAddAdmissionClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Active, 1: Discharged
    val activeList = remember(admissions) { admissions.filter { it.status == "Admitted" } }
    val dischargedList = remember(admissions) { admissions.filter { it.status != "Admitted" } }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top Header & Add Admission Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DAYCARE DEPARTMENT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${activeList.size} Daycare Beds Occupied",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Button(
                onClick = onAddAdmissionClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_admission_button")
            ) {
                Icon(Icons.Filled.Hotel, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Admit to Daycare", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Selector (Active vs Discharged)
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Active Daycare (${activeList.size})", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Discharged (${dischargedList.size})", fontWeight = FontWeight.SemiBold) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val currentDisplayList = if (selectedTab == 0) activeList else dischargedList

        if (currentDisplayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Hotel,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (selectedTab == 0) "No active patients in Daycare" else "No discharged daycare records",
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
                items(currentDisplayList, key = { it.id }) { admission ->
                    IpdAdmissionCard(
                        admission = admission,
                        currency = currency,
                        dateFormat = dateFormat,
                        onClick = { onAdmissionClick(admission) }
                    )
                }
            }
        }
    }
}

@Composable
fun IpdAdmissionCard(
    admission: IpdAdmission,
    currency: String,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("ipd_card_${admission.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Hotel, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = admission.patientName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bed: ${admission.bedRoomNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Badge(
                    containerColor = if (admission.status == "Admitted") Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                    contentColor = if (admission.status == "Admitted") Color(0xFFE65100) else Color(0xFF1B5E20)
                ) {
                    Text(
                        text = admission.status,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Diagnosis: ${admission.admittingDiagnosis}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Admitted on: ${dateFormat.format(Date(admission.admissionDate))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.8.dp)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Rate: $currency${admission.roomChargePerDay}/day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Text(
                    text = "Total Bill: $currency${admission.totalAmount}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIpdAdmissionDialog(
    patients: List<Patient>,
    onDismiss: () -> Unit,
    onSave: (IpdAdmission) -> Unit
) {
    var selectedPatient by remember { mutableStateOf(patients.firstOrNull()) }
    var patientExpanded by remember { mutableStateOf(false) }
    var bedNumber by remember { mutableStateOf("Daycare Bed-1 (Observation)") }
    var diagnosis by remember { mutableStateOf("") }
    var consultant by remember { mutableStateOf("Dr. S. S. Ghule") }
    var dailyBedCharge by remember { mutableStateOf("800") }
    var advanceDeposit by remember { mutableStateOf("1500") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Daycare Admission", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Select Patient *", style = MaterialTheme.typography.labelSmall)
                    ExposedDropdownMenuBox(
                        expanded = patientExpanded,
                        onExpandedChange = { patientExpanded = !patientExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPatient?.let { "${it.name} (${it.patientCode})" } ?: "Select Patient",
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
                                    text = { Text("${p.name} (${p.patientCode} • ${p.age}y)") },
                                    onClick = {
                                        selectedPatient = p
                                        patientExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = bedNumber,
                        onValueChange = { bedNumber = it },
                        label = { Text("Daycare Bed / Room *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = diagnosis,
                        onValueChange = { diagnosis = it },
                        label = { Text("Daycare Diagnosis / Reason *") },
                        placeholder = { Text("e.g. Acute Gastroenteritis, IV Therapy, Observation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dailyBedCharge,
                            onValueChange = { dailyBedCharge = it },
                            label = { Text("Daycare Charge (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = advanceDeposit,
                            onValueChange = { advanceDeposit = it },
                            label = { Text("Advance Paid (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = consultant,
                        onValueChange = { consultant = it },
                        label = { Text("Attending Doctor") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedPatient == null || bedNumber.isBlank()) return@Button
                    val deposit = advanceDeposit.toDoubleOrNull() ?: 0.0
                    val daily = dailyBedCharge.toDoubleOrNull() ?: 800.0
                    val admission = IpdAdmission(
                        patientId = selectedPatient!!.id,
                        patientName = selectedPatient!!.name,
                        admissionDate = System.currentTimeMillis(),
                        bedRoomNumber = bedNumber.trim(),
                        admittingDiagnosis = diagnosis.trim().ifBlank { "Daycare Observation & IV Therapy" },
                        depositAdvance = deposit,
                        roomChargePerDay = daily,
                        totalAmount = deposit.coerceAtLeast(daily),
                        paidAmount = deposit,
                        status = "Admitted"
                    )
                    onSave(admission)
                }
            ) {
                Text("Confirm Daycare Admission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
