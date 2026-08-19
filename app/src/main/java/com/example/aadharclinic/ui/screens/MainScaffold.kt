package com.example.aadharclinic.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aadharclinic.data.model.*
import com.example.aadharclinic.ui.viewmodel.ClinicViewModel

enum class ClinicDestination(val label: String, val icon: ImageVector) {
    DASHBOARD("Home", Icons.Filled.Dashboard),
    PATIENTS("Patients", Icons.Filled.People),
    OPD("OPD Rx", Icons.Filled.Medication),
    LETTERS("Letters", Icons.Filled.Description),
    INVENTORY("Stock", Icons.Filled.Inventory2),
    BILLING("Billing", Icons.Filled.ReceiptLong),
    DAYCARE("Daycare", Icons.Filled.Hotel),
    REPORTS("Reports", Icons.Filled.Analytics),
    SETTINGS("Settings", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: ClinicViewModel
) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(ClinicDestination.DASHBOARD) }

    // Navigation sub-states
    var selectedPatientForDetail by remember { mutableStateOf<Patient?>(null) }
    var selectedAdmissionForDetail by remember { mutableStateOf<IpdAdmission?>(null) }
    var showAddPatientDialog by remember { mutableStateOf(false) }
    var showAddAdmissionDialog by remember { mutableStateOf(false) }
    var opdInitialPatientId by remember { mutableStateOf<Long?>(null) }

    // Collect States
    val profile by viewModel.clinicProfile.collectAsState()
    val patients by viewModel.patients.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val todayConsultationCount by viewModel.todayConsultationCount.collectAsState()
    val todayRevenue by viewModel.todayRevenue.collectAsState()
    val currentIpdCount by viewModel.activeAdmissionCount.collectAsState()
    val revenueHistory by viewModel.last7DaysRevenue.collectAsState()
    val topDiseases by viewModel.topDiseases.collectAsState()
    val allAdmissions by viewModel.allAdmissions.collectAsState()
    val inventoryList by viewModel.allInventory.collectAsState()
    val lowStockList by viewModel.lowStockItems.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()
    val allInvoices by viewModel.allInvoices.collectAsState()
    val allConsultations by viewModel.allConsultations.collectAsState()
    val allServices by viewModel.allServices.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allHospitals by viewModel.allHospitals.collectAsState()
    val allDocuments by viewModel.allDocuments.collectAsState()
    val allPresets by viewModel.allPresets.collectAsState()
    val syncSummary by viewModel.syncSummary.collectAsState()
    val cloudBackupInfo by viewModel.cloudBackupInfo.collectAsState()
    val conflictLogs by viewModel.conflictLogs.collectAsState()
    var showQuickSyncDialog by remember { mutableStateOf(false) }

    val currency = profile?.currency ?: "₹"

    // If logged in user is RECEPTION, render Reception Dashboard exclusively
    if (currentUser?.role == UserRole.RECEPTION) {
        ReceptionDashboardScreen(
            profile = profile,
            currentUser = currentUser,
            patients = patients,
            searchQuery = searchQuery,
            syncSummary = syncSummary,
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            onRegisterPatientClick = { showAddPatientDialog = true },
            onPatientClick = { selectedPatientForDetail = it },
            onLogoutClick = { viewModel.logout() }
        )

        if (showAddPatientDialog || selectedPatientForDetail != null) {
            PatientEditDialog(
                initialPatient = selectedPatientForDetail,
                onDismiss = {
                    showAddPatientDialog = false
                    selectedPatientForDetail = null
                },
                onSave = { updated ->
                    viewModel.savePatient(updated) {
                        showAddPatientDialog = false
                        selectedPatientForDetail = null
                    }
                }
            )
        }
        return
    }

    // If viewing Patient Detail
    if (selectedPatientForDetail != null) {
        val patientId = selectedPatientForDetail!!.id
        val patientConsultations by viewModel.getConsultationsForPatient(patientId).collectAsState(emptyList())
        val patientAdmissions by viewModel.getAdmissionsForPatient(patientId).collectAsState(emptyList())
        val patientInvoices by viewModel.getInvoicesForPatient(patientId).collectAsState(emptyList())

        var showEditDialog by remember { mutableStateOf(false) }

        PatientDetailScreen(
            patient = selectedPatientForDetail!!,
            consultations = patientConsultations,
            admissions = patientAdmissions,
            invoices = patientInvoices,
            currency = currency,
            onBackClick = { selectedPatientForDetail = null },
            onEditPatientClick = { showEditDialog = true },
            onStartConsultationClick = {
                opdInitialPatientId = selectedPatientForDetail!!.id
                selectedPatientForDetail = null
                currentDestination = ClinicDestination.OPD
            },
            onAdmitIpdClick = {
                selectedPatientForDetail = null
                showAddAdmissionDialog = true
                currentDestination = ClinicDestination.DAYCARE
            },
            onPrintPrescription = { cons, action ->
                viewModel.printPrescription(context, cons.id, action)
            },
            onDeletePatient = {
                viewModel.deletePatient(selectedPatientForDetail!!)
                selectedPatientForDetail = null
            }
        )

        if (showEditDialog) {
            PatientEditDialog(
                initialPatient = selectedPatientForDetail,
                onDismiss = { showEditDialog = false },
                onSave = { updated ->
                    viewModel.savePatient(updated) {
                        selectedPatientForDetail = updated
                    }
                    showEditDialog = false
                },
                onDelete = {
                    selectedPatientForDetail?.let { p ->
                        viewModel.deletePatient(p)
                        selectedPatientForDetail = null
                    }
                    showEditDialog = false
                }
            )
        }
        return
    }

    // If viewing IPD Detail
    if (selectedAdmissionForDetail != null) {
        val admission = selectedAdmissionForDetail!!
        val dailyNotes by viewModel.getDailyNotes(admission.id).collectAsState(emptyList())
        val administeredMeds by viewModel.getMedicinesAdministered(admission.id).collectAsState(emptyList())
        val patientObj = patients.find { it.id == admission.patientId }

        IpdDetailScreen(
            admission = admission,
            patient = patientObj,
            dailyNotes = dailyNotes,
            medicinesAdministered = administeredMeds,
            inventoryList = inventoryList,
            currency = currency,
            onBackClick = { selectedAdmissionForDetail = null },
            onAddDailyNote = { note -> viewModel.addDailyNote(note) },
            onAdministerMedicine = { name, invId, dose, route, qty, by, cost ->
                viewModel.administerMedicine(admission.id, name, invId, dose, route, qty, by, cost)
            },
            onDischargePatient = { cond, sum, adv, diag, tot, paid, mode, stat, ctx ->
                viewModel.dischargePatient(admission.id, cond, sum, adv, diag, tot, paid, mode, stat, ctx) {
                    selectedAdmissionForDetail = null
                }
            }
        )
        return
    }

    // Main Scaffold with Navigation Bar
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.LocalHospital,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = profile?.clinicName ?: "Aadhar Clinic OS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentUser?.name ?: "Dr. Sanket Ghule",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = " • ${currentDestination.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Sync Status Indicator Chip in TopBar
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            !syncSummary.isOnline -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            syncSummary.isSyncing -> MaterialTheme.colorScheme.primaryContainer
                            syncSummary.pendingCount > 0 -> Color(0xFFFFF3E0)
                            else -> Color(0xFFE8F5E9)
                        },
                        modifier = Modifier.padding(end = 4.dp),
                        onClick = { showQuickSyncDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            if (syncSummary.isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = when {
                                        !syncSummary.isOnline -> Icons.Filled.CloudOff
                                        syncSummary.pendingCount > 0 -> Icons.Filled.CloudUpload
                                        else -> Icons.Filled.CloudDone
                                    },
                                    contentDescription = "Sync Status",
                                    tint = when {
                                        !syncSummary.isOnline -> MaterialTheme.colorScheme.error
                                        syncSummary.pendingCount > 0 -> Color(0xFFE65100)
                                        else -> Color(0xFF2E7D32)
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    !syncSummary.isOnline -> "Offline"
                                    syncSummary.isSyncing -> "Syncing"
                                    syncSummary.pendingCount > 0 -> "${syncSummary.pendingCount} pend"
                                    else -> "Synced"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    !syncSummary.isOnline -> MaterialTheme.colorScheme.error
                                    syncSummary.isSyncing -> MaterialTheme.colorScheme.primary
                                    syncSummary.pendingCount > 0 -> Color(0xFFE65100)
                                    else -> Color(0xFF2E7D32)
                                }
                            )
                        }
                    }

                    IconButton(onClick = { currentDestination = ClinicDestination.REPORTS }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Reports (A4)", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { currentDestination = ClinicDestination.SETTINGS }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                listOf(
                    ClinicDestination.DASHBOARD to null,
                    ClinicDestination.PATIENTS to patients.size,
                    ClinicDestination.OPD to null,
                    ClinicDestination.LETTERS to null,
                    ClinicDestination.BILLING to null
                ).forEach { (dest, badgeCount) ->
                    NavigationBarItem(
                        selected = currentDestination == dest,
                        onClick = { currentDestination = dest },
                        icon = {
                            if (badgeCount != null && badgeCount > 0) {
                                BadgedBox(badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text("$badgeCount")
                                    }
                                }) {
                                    Icon(dest.icon, contentDescription = dest.label)
                                }
                            } else {
                                Icon(dest.icon, contentDescription = dest.label)
                            }
                        },
                        label = { Text(dest.label, fontSize = 11.sp, fontWeight = if (currentDestination == dest) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_${dest.name.lowercase()}")
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentDestination) {
                ClinicDestination.DASHBOARD -> DashboardScreen(
                    profile = profile,
                    todayPatientsCount = todayConsultationCount,
                    todayRevenueAmount = todayRevenue,
                    currentIpdCount = currentIpdCount,
                    lowStockCount = lowStockCount,
                    revenueHistory = revenueHistory,
                    topDiseasesList = topDiseases,
                    syncSummary = syncSummary,
                    onSyncNow = { viewModel.syncPendingData() },
                    onQuickAddPatient = { showAddPatientDialog = true },
                    onQuickConsultation = {
                        opdInitialPatientId = null
                        currentDestination = ClinicDestination.OPD
                    },
                    onQuickLetters = { currentDestination = ClinicDestination.LETTERS },
                    onQuickIpd = { currentDestination = ClinicDestination.DAYCARE },
                    onQuickReports = { currentDestination = ClinicDestination.REPORTS },
                    onNavigatePatients = { currentDestination = ClinicDestination.PATIENTS },
                    onNavigateStock = { currentDestination = ClinicDestination.INVENTORY },
                    onNavigateBilling = { currentDestination = ClinicDestination.BILLING },
                    onNavigateSettings = { currentDestination = ClinicDestination.SETTINGS }
                )

                ClinicDestination.PATIENTS -> PatientsScreen(
                    patients = patients,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.updateSearchQuery(it) },
                    onPatientClick = { selectedPatientForDetail = it },
                    onAddPatientClick = { showAddPatientDialog = true },
                    onDeletePatient = { viewModel.deletePatient(it) },
                    onExportSpreadsheet = { viewModel.exportPatientsSpreadsheet(context) }
                )

                ClinicDestination.OPD -> ConsultationScreen(
                    initialPatientId = opdInitialPatientId,
                    patients = patients,
                    inventoryList = inventoryList,
                    presets = allPresets,
                    servicesList = allServices,
                    profile = profile,
                    currentDoctorName = currentUser?.name,
                    onBackClick = { currentDestination = ClinicDestination.DASHBOARD },
                    onSavePreset = { viewModel.savePreset(it) },
                    onDeletePreset = { viewModel.deletePreset(it) },
                    onUpdateMedicineDefault = { itemId, freq, dur, dose, inst ->
                        viewModel.updateMedicineDefaultDuration(itemId, freq, dur, dose, inst)
                    },
                    onSaveConsultation = { cons, items, mobile, printPdf, action, ctx ->
                        viewModel.saveConsultation(
                            consultation = cons,
                            prescriptionItems = items,
                            patientMobile = mobile,
                            generatePdfNow = printPdf,
                            action = action,
                            context = ctx
                        ) {
                            currentDestination = ClinicDestination.DASHBOARD
                        }
                    }
                )

                ClinicDestination.LETTERS -> LettersScreen(
                    profile = profile,
                    currentUser = currentUser,
                    patients = patients,
                    hospitals = allHospitals,
                    documentsHistory = allDocuments,
                    onSaveAndProcess = { doc, act, ctx ->
                        viewModel.saveAndProcessDocument(doc, act, ctx)
                    },
                    onReprint = { doc, act, ctx ->
                        viewModel.reprintExistingDocument(doc, act, ctx)
                    },
                    onSaveHospital = { viewModel.saveHospital(it) },
                    onDeleteDocument = { viewModel.deleteClinicalDocument(it) }
                )

                ClinicDestination.INVENTORY -> InventoryScreen(
                    inventoryList = inventoryList,
                    lowStockList = lowStockList,
                    currency = currency,
                    onSaveItem = { viewModel.saveInventoryItem(it) },
                    onAdjustStock = { id, delta, type, notes ->
                        viewModel.adjustStock(id, delta, type, notes)
                    },
                    onDeleteItem = { viewModel.deleteInventoryItem(it) },
                    onExportSpreadsheet = { viewModel.exportInventorySpreadsheet(context) }
                )

                ClinicDestination.BILLING -> BillingScreen(
                    invoices = allInvoices,
                    patients = patients,
                    servicesList = allServices,
                    currency = currency,
                    onSaveInvoice = { viewModel.saveInvoice(it) },
                    onDeleteInvoice = { viewModel.deleteInvoice(it) }
                )

                ClinicDestination.DAYCARE -> IpdScreen(
                    admissions = allAdmissions,
                    patients = patients,
                    currency = currency,
                    onAdmissionClick = { selectedAdmissionForDetail = it },
                    onAddAdmissionClick = { showAddAdmissionDialog = true }
                )

                ClinicDestination.REPORTS -> ReportsScreen(
                    profile = profile,
                    patients = patients,
                    consultations = allConsultations,
                    invoices = allInvoices,
                    users = allUsers,
                    currency = currency
                )

                ClinicDestination.SETTINGS -> SettingsScreen(
                    profile = profile,
                    services = allServices,
                    users = allUsers,
                    hospitals = allHospitals,
                    presets = allPresets,
                    currentUser = currentUser,
                    syncSummary = syncSummary,
                    cloudBackupInfo = cloudBackupInfo,
                    conflictLogs = conflictLogs,
                    onSyncNow = { viewModel.syncPendingData() },
                    onRestoreCloudData = { viewModel.restoreCloudData() },
                    onSaveProfile = { viewModel.updateClinicProfile(it) },
                    onSaveService = { viewModel.saveService(it) },
                    onSaveUser = { viewModel.saveUser(it) },
                    onDeleteUser = { viewModel.deleteUser(it) },
                    onResetPassword = { userId, newPass -> viewModel.resetUserPassword(userId, newPass) },
                    onSaveHospital = { viewModel.saveHospital(it) },
                    onDeleteHospital = { viewModel.deleteHospital(it) },
                    onSavePreset = { viewModel.savePreset(it) },
                    onDeletePreset = { viewModel.deletePreset(it) },
                    onChangePassword = { newPass -> viewModel.changePassword(newPass) {} },
                    onExportJson = { viewModel.exportClinicDataJson() },
                    onLogout = { viewModel.logout() }
                )
            }
        }
    }

    // Dialogs
    if (showQuickSyncDialog) {
        AlertDialog(
            onDismissRequest = { showQuickSyncDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (!syncSummary.isOnline) Icons.Filled.CloudOff else Icons.Filled.CloudSync,
                        contentDescription = null,
                        tint = if (!syncSummary.isOnline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hospital Cloud Sync")
                }
            },
            text = {
                Column {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (syncSummary.isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (syncSummary.isOnline) Color(0xFF2E7D32) else Color(0xFFC62828))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (syncSummary.isOnline) "Connected • Network Active" else "Offline Mode • Hospital Internal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (syncSummary.isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("• Pending Offline Uploads: ${syncSummary.pendingCount}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("• Synced Cloud Records: ${syncSummary.syncedCount}", fontSize = 13.sp)
                    Text("• Clinic ID: ${syncSummary.cloudHospitalId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "All data is saved instantly to device SQLite/Room database. When connected, sync pushes records automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                if (syncSummary.isOnline && syncSummary.pendingCount > 0) {
                    Button(
                        onClick = {
                            viewModel.syncPendingData()
                            showQuickSyncDialog = false
                        }
                    ) {
                        Text("Sync Now")
                    }
                } else {
                    TextButton(onClick = { showQuickSyncDialog = false }) { Text("OK") }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showQuickSyncDialog = false
                    currentDestination = ClinicDestination.SETTINGS
                }) {
                    Text("Sync Settings")
                }
            }
        )
    }

    if (showAddPatientDialog) {
        PatientEditDialog(
            initialPatient = null,
            onDismiss = { showAddPatientDialog = false },
            onSave = { newPatient ->
                viewModel.savePatient(newPatient) { id ->
                    showAddPatientDialog = false
                }
            }
        )
    }

    if (showAddAdmissionDialog) {
        AddIpdAdmissionDialog(
            patients = patients,
            onDismiss = { showAddAdmissionDialog = false },
            onSave = { newAdm ->
                viewModel.saveAdmission(newAdm) {
                    showAddAdmissionDialog = false
                }
            }
        )
    }
}
