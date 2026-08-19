package com.example.aadharclinic.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aadharclinic.data.model.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profile: ClinicProfile?,
    services: List<ClinicService> = emptyList(),
    users: List<ClinicUser> = emptyList(),
    hospitals: List<Hospital> = emptyList(),
    presets: List<QuickPreset> = emptyList(),
    currentUser: ClinicUser? = null,
    syncSummary: SyncSummary = SyncSummary(),
    cloudBackupInfo: CloudBackupInfo? = null,
    conflictLogs: List<ConflictAuditEntry> = emptyList(),
    onSyncNow: () -> Unit = {},
    onRestoreCloudData: () -> Unit = {},
    onSaveProfile: (ClinicProfile) -> Unit,
    onSaveService: (ClinicService) -> Unit,
    onSaveUser: (ClinicUser) -> Unit = {},
    onDeleteUser: (ClinicUser) -> Unit = {},
    onResetPassword: (userId: Long, newPass: String) -> Unit = { _, _ -> },
    onSaveHospital: (Hospital) -> Unit = {},
    onDeleteHospital: (Hospital) -> Unit = {},
    onSavePreset: (QuickPreset) -> Unit = {},
    onDeletePreset: (QuickPreset) -> Unit = {},
    onChangePassword: (newPass: String) -> Unit,
    onExportJson: suspend () -> String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showAddHospitalDialog by remember { mutableStateOf(false) }
    var hospitalToEdit by remember { mutableStateOf<Hospital?>(null) }

    var showPresetDialog by remember { mutableStateOf(false) }
    var presetToEdit by remember { mutableStateOf<QuickPreset?>(null) }

    var clinicName by remember(profile) { mutableStateOf(profile?.clinicName ?: "Aadhar Multi-Speciality Clinic") }
    var doctorName by remember(profile) { mutableStateOf(profile?.doctorName ?: "Dr. Sanket Ghule") }
    var regNumber by remember(profile) { mutableStateOf(profile?.regNumber ?: "MMC-2018/06/5432") }
    var qualification by remember(profile) { mutableStateOf(profile?.qualification ?: "BAMS EMS") }
    var clinicAddress by remember(profile) { mutableStateOf(profile?.address ?: "Aadhar Clinic, Pune, Maharashtra") }
    var phone by remember(profile) { mutableStateOf(profile?.contactNumber ?: "+91 98765 43210") }
    var email by remember(profile) { mutableStateOf(profile?.email ?: "info@aadharclinic.com") }
    var currency by remember(profile) { mutableStateOf(profile?.currency ?: "₹") }
    var rxHeader by remember(profile) { mutableStateOf(profile?.prescriptionHeader ?: "AADHAR MULTI-SPECIALITY CLINIC") }
    var rxFooter by remember(profile) { mutableStateOf(profile?.prescriptionFooter ?: "Emergency consultation available 24x7. Not valid for medico-legal purposes.") }

    // Predefined Services Map State
    val servicePriceMap = remember(services) {
        val map = mutableStateMapOf<String, String>()
        PredefinedClinicServices.STANDARD_SERVICES.forEach { defaultSvc ->
            val match = services.find { it.serviceName.equals(defaultSvc.serviceName, ignoreCase = true) }
            val price = match?.defaultPrice ?: defaultSvc.defaultPrice
            map[defaultSvc.serviceName] = price.toInt().toString()
        }
        map
    }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var userToResetPassword by remember { mutableStateOf<ClinicUser?>(null) }
    var showRestoreCloudConfirm by remember { mutableStateOf(false) }
    var showAuditLogDialog by remember { mutableStateOf(false) }

    val appVersion = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.1"
        } catch (e: Exception) {
            "1.1"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "CLINIC SETTINGS & ADMINISTRATION",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
            letterSpacing = 1.sp
        )
        Text(
            text = "Doctor Management, Services & Pricing",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 0. OFFLINE-FIRST & CLOUD SYNC CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "OFFLINE-FIRST & CLOUD SYNCHRONIZATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Local SQLite/Room + Cloud Backup",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (syncSummary.isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (syncSummary.isOnline) Color(0xFF2E7D32) else Color(0xFFC62828))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (syncSummary.isOnline) "ONLINE" else "OFFLINE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (syncSummary.isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Cloud ID & Doctor credentials
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Clinic ID:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(syncSummary.cloudHospitalId, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Doctor Login:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(syncSummary.activeConsultantEmail, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Sync Engine:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    Text("Automatic background upload on network reconnect", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Metrics breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${syncSummary.syncedCount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Synced Records",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (syncSummary.pendingCount > 0) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${syncSummary.pendingCount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (syncSummary.pendingCount > 0) Color(0xFFE65100) else MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "Pending Upload",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val timeStr = if (syncSummary.lastSyncTime > 0) {
                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(syncSummary.lastSyncTime))
                                    } else "Auto"
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Last Sync",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onSyncNow,
                                enabled = syncSummary.isOnline && !syncSummary.isSyncing,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (syncSummary.isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Syncing...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Now", fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = { showRestoreCloudConfirm = true },
                                enabled = syncSummary.isOnline && !syncSummary.isSyncing,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Cloud", fontSize = 12.sp)
                            }
                        }

                        if (conflictLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { showAuditLogDialog = true },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Conflict & Sync Audit Trail (${conflictLogs.size})", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 1. Multi-Doctor & Staff Administration Card (for Admins)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "DOCTOR & USER MANAGEMENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Active Clinic Accounts (${users.size})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            FilledTonalButton(
                                onClick = { showAddUserDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add User / Doctor", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (users.isEmpty()) {
                            Text(
                                text = "No users configured.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            users.forEach { u ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(u.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = when (u.role) {
                                                        UserRole.ADMIN -> MaterialTheme.colorScheme.primaryContainer
                                                        UserRole.STAFF -> MaterialTheme.colorScheme.secondaryContainer
                                                        else -> MaterialTheme.colorScheme.tertiaryContainer
                                                    }
                                                ) {
                                                    Text(
                                                        text = u.role.name,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "ID: ${u.loginId} • ${u.qualification.ifBlank { "Staff" }} • Ph: ${u.contactNumber}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = { userToResetPassword = u },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Filled.Key, contentDescription = "Reset Password", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            }
                                            if (u.role != UserRole.ADMIN) {
                                                IconButton(
                                                    onClick = { onDeleteUser(u) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove User", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 1.5 Saved Referral Hospitals Directory Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SAVED REFERRAL HOSPITALS DIRECTORY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Save once and search in Referral Letters",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    hospitalToEdit = null
                                    showAddHospitalDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (hospitals.isEmpty()) {
                            Text(
                                text = "No saved referral hospitals yet. Tap '+ Add' to register hospitals.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                hospitals.forEach { hosp ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Filled.LocalHospital,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = hosp.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "${hosp.address} ${if (hosp.contactNumber.isNotBlank()) "• Ph: ${hosp.contactNumber}" else ""}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.outline,
                                                        maxLines = 2
                                                    )
                                                }
                                            }

                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        hospitalToEdit = hosp
                                                        showAddHospitalDialog = true
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = { onDeleteHospital(hosp) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Quick Presets & Default Durations Card (OPD Presets Configuration)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "QUICK PRESETS & DEFAULT DURATIONS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Configure preset duration once (e.g. Antacid -> 7D). Future OPD prescriptions auto-use this duration.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    presetToEdit = null
                                    showPresetDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("add_preset_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Preset")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (presets.isEmpty()) {
                            Text(
                                text = "No quick presets configured. Click '+ Add Preset' to create standard prescription presets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                presets.forEach { preset ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            Icons.Filled.Medication,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = preset.presetName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = MaterialTheme.colorScheme.primaryContainer
                                                        ) {
                                                            Text(
                                                                text = "${preset.frequency} • ${preset.defaultDuration}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "${preset.medicineName} (${preset.strength}) • ${preset.dose} • ${preset.instructions}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }

                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        presetToEdit = preset
                                                        showPresetDialog = true
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = { onDeletePreset(preset) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Predefined Service Prices Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PREDEFINED CLINICAL SERVICE CHARGES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Set default rates for single-click OPD billing calculation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PredefinedClinicServices.STANDARD_SERVICES.forEach { svc ->
                            val serviceName = svc.serviceName
                            val currentVal = servicePriceMap[serviceName] ?: svc.defaultPrice.toInt().toString()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = serviceName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = currentVal,
                                    onValueChange = { newVal ->
                                        servicePriceMap[serviceName] = newVal
                                        val priceDbl = newVal.toDoubleOrNull() ?: svc.defaultPrice
                                        val existing = services.find { it.serviceName.equals(serviceName, ignoreCase = true) }
                                        val updated = (existing ?: svc).copy(defaultPrice = priceDbl)
                                        onSaveService(updated)
                                    },
                                    prefix = { Text(currency) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.width(120.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Clinic Information Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CLINIC & DOCTOR PROFILE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = clinicName,
                            onValueChange = { clinicName = it },
                            label = { Text("Clinic Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = doctorName,
                            onValueChange = { doctorName = it },
                            label = { Text("Chief Medical Officer Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = qualification,
                            onValueChange = { qualification = it },
                            label = { Text("Degrees & Qualification") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = regNumber,
                            onValueChange = { regNumber = it },
                            label = { Text("Medical Registration Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = clinicAddress,
                            onValueChange = { clinicAddress = it },
                            label = { Text("Clinic Address (for PDF Header)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Phone") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = rxFooter,
                            onValueChange = { rxFooter = it },
                            label = { Text("Prescription Disclaimer / Footer") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val updated = (profile ?: ClinicProfile()).copy(
                                    clinicName = clinicName.trim(),
                                    doctorName = doctorName.trim(),
                                    regNumber = regNumber.trim(),
                                    qualification = qualification.trim(),
                                    address = clinicAddress.trim(),
                                    contactNumber = phone.trim(),
                                    email = email.trim(),
                                    currency = currency.trim(),
                                    prescriptionHeader = rxHeader.trim(),
                                    prescriptionFooter = rxFooter.trim()
                                )
                                onSaveProfile(updated)
                                saveSuccessMessage = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Profile Changes")
                        }

                        if (saveSuccessMessage) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Settings saved successfully!",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            // 4. Security & Data Actions Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SECURITY & DATA BACKUP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showPasswordDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.LockReset, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Clinic Master Password")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val json = onExportJson()
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, json)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Export Aadhar Clinic Backup")
                                    context.startActivity(shareIntent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Clinic Data (JSON)")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        FilledTonalButton(
                            onClick = { showLogoutConfirm = true },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lock / Logout Session", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 5. About & Developer Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_about_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ABOUT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Developed & Designed by Hero Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Developed & Designed by",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Dr. Sanket Shivajirao Ghule",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Clean, minimal metadata details
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Developer",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "Dr. Sanket Shivajirao Ghule",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "App Developer / Publisher",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "Dr. Sanket Shivajirao Ghule",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Copyright",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "© 2026 Dr. Sanket Shivajirao Ghule",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "App Version",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "v$appVersion",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add User / Doctor Dialog
    if (showAddUserDialog) {
        var uName by remember { mutableStateOf("") }
        var uLoginId by remember { mutableStateOf("") }
        var uPassword by remember { mutableStateOf("clinic123") }
        var uRole by remember { mutableStateOf(UserRole.DOCTOR) }
        var uQual by remember { mutableStateOf("BAMS") }
        var uMobile by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("Add Doctor / Staff Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uName,
                        onValueChange = { uName = it },
                        label = { Text("Full Name (e.g. Dr. A. B. Joshi)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uLoginId,
                        onValueChange = { uLoginId = it.trim().lowercase() },
                        label = { Text("Login ID (e.g. dr_joshi)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uPassword,
                        onValueChange = { uPassword = it },
                        label = { Text("Initial Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uQual,
                        onValueChange = { uQual = it },
                        label = { Text("Qualification / Role Info") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uMobile,
                        onValueChange = { uMobile = it },
                        label = { Text("Mobile Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Select Role:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(UserRole.DOCTOR, UserRole.STAFF, UserRole.RECEPTION, UserRole.ADMIN).forEach { r ->
                            FilterChip(
                                selected = uRole == r,
                                onClick = { uRole = r },
                                label = { Text(r.name) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (uName.isNotBlank() && uLoginId.isNotBlank()) {
                            val activeHospitalId = profile?.hospitalId ?: "aadhar123"
                            val newUser = ClinicUser(
                                clinicId = activeHospitalId,
                                name = uName.trim(),
                                loginId = uLoginId.trim(),
                                passwordHash = uPassword.trim(),
                                role = uRole,
                                qualification = uQual.trim(),
                                contactNumber = uMobile.trim()
                            )
                            onSaveUser(newUser)
                            showAddUserDialog = false
                        }
                    }
                ) {
                    Text("Add Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Reset Password Dialog
    if (userToResetPassword != null) {
        val target = userToResetPassword!!
        var newPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { userToResetPassword = null },
            title = { Text("Reset Password for ${target.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter new password for login ID: ${target.loginId}")
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPass.isNotBlank()) {
                            onResetPassword(target.id, newPass.trim())
                            userToResetPassword = null
                        }
                    }
                ) {
                    Text("Save New Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToResetPassword = null }) { Text("Cancel") }
            }
        )
    }

    // Change Master Password Dialog
    if (showPasswordDialog) {
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var passError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Change Master Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passError != null) {
                        Text(passError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPass.length < 4) {
                            passError = "Password must be at least 4 characters."
                            return@Button
                        }
                        if (newPass != confirmPass) {
                            passError = "Passwords do not match."
                            return@Button
                        }
                        onChangePassword(newPass)
                        showPasswordDialog = false
                    }
                ) {
                    Text("Update Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Logout Dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Lock Session?") },
            text = { Text("You will need to sign in again with your doctor / staff credentials.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Add/Edit Hospital Dialog
    if (showAddHospitalDialog) {
        AddHospitalDialog(
            initialHospital = hospitalToEdit,
            onDismiss = {
                showAddHospitalDialog = false
                hospitalToEdit = null
            },
            onSave = {
                onSaveHospital(it)
                showAddHospitalDialog = false
                hospitalToEdit = null
            }
        )
    }

    // Cloud Backup Restore Dialog
    if (showRestoreCloudConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreCloudConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore from Cloud Backup?")
                }
            },
            text = {
                Column {
                    Text(
                        "This will fetch all clinical records, patients, consultations, invoices, and daycare admissions associated with clinic ID ${syncSummary.cloudHospitalId} from the Firebase cloud.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (cloudBackupInfo != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Available Cloud Snapshot:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("• Patients: ${cloudBackupInfo.patientCount}", fontSize = 12.sp)
                                Text("• Consultations: ${cloudBackupInfo.consultationCount}", fontSize = 12.sp)
                                Text("• Invoices: ${cloudBackupInfo.invoiceCount}", fontSize = 12.sp)
                                Text("• Daycare/IPD: ${cloudBackupInfo.admissionCount}", fontSize = 12.sp)
                                Text("• Inventory: ${cloudBackupInfo.inventoryCount}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreCloudConfirm = false
                        onRestoreCloudData()
                    }
                ) {
                    Text("Proceed Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreCloudConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Conflict & Audit Log Dialog
    if (showAuditLogDialog) {
        AlertDialog(
            onDismissRequest = { showAuditLogDialog = false },
            title = { Text("Conflict Resolution & Sync Audit") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (conflictLogs.isEmpty()) {
                        item {
                            Text("No conflict resolutions recorded. All syncs completed with 100% deterministic local-first resolution.")
                        }
                    } else {
                        items(conflictLogs.size) { index ->
                            val log = conflictLogs[index]
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        "${log.recordType}: ${log.conflictField}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text("Local: ${log.localValue} | Remote: ${log.remoteValue}", fontSize = 11.sp)
                                    Text("Resolved to: ${log.resolvedValue} by ${log.resolvedBy}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAuditLogDialog = false }) { Text("Close") }
            }
        )
    }

    // Quick Preset Add/Edit Dialog
    if (showPresetDialog) {
        var presetName by remember { mutableStateOf(presetToEdit?.presetName ?: "") }
        var medName by remember { mutableStateOf(presetToEdit?.medicineName ?: "") }
        var strength by remember { mutableStateOf(presetToEdit?.strength ?: "40 mg") }
        var dose by remember { mutableStateOf(presetToEdit?.dose ?: "1 Tab") }
        var frequency by remember { mutableStateOf(presetToEdit?.frequency ?: "BD") }
        var route by remember { mutableStateOf(presetToEdit?.route ?: "Oral") }
        var defaultDuration by remember { mutableStateOf(presetToEdit?.defaultDuration ?: "7 days") }
        var instructions by remember { mutableStateOf(presetToEdit?.instructions ?: "Before Food") }

        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = {
                Text(
                    text = if (presetToEdit != null) "Edit Quick Preset" else "Add Quick Preset",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        label = { Text("Preset Name * (e.g. Antacid)") },
                        placeholder = { Text("e.g. Antacid") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = medName,
                        onValueChange = { medName = it },
                        label = { Text("Medicine Name * (e.g. Pantoprazole)") },
                        placeholder = { Text("e.g. Pantoprazole 40 mg") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = strength,
                            onValueChange = { strength = it },
                            label = { Text("Strength") },
                            placeholder = { Text("40 mg") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dose,
                            onValueChange = { dose = it },
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
                            value = frequency,
                            onValueChange = { frequency = it },
                            label = { Text("Frequency *") },
                            placeholder = { Text("BD") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = defaultDuration,
                            onValueChange = { defaultDuration = it },
                            label = { Text("Default Duration *") },
                            placeholder = { Text("7 days") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Quick duration suggestions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("3 days", "5 days", "7 days", "10 days", "14 days", "1 month").forEach { dur ->
                            FilterChip(
                                selected = defaultDuration.equals(dur, ignoreCase = true),
                                onClick = { defaultDuration = dur },
                                label = { Text(dur, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = route,
                            onValueChange = { route = it },
                            label = { Text("Route") },
                            placeholder = { Text("Oral") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = instructions,
                            onValueChange = { instructions = it },
                            label = { Text("Instructions") },
                            placeholder = { Text("Before Food") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetName.isNotBlank() && medName.isNotBlank()) {
                            val presetToSave = (presetToEdit ?: QuickPreset(presetName = presetName, medicineName = medName)).copy(
                                presetName = presetName.trim(),
                                medicineName = medName.trim(),
                                strength = strength.trim(),
                                dose = dose.trim().ifBlank { "1 Tab" },
                                frequency = frequency.trim().ifBlank { "BD" },
                                route = route.trim().ifBlank { "Oral" },
                                defaultDuration = defaultDuration.trim().ifBlank { "7 days" },
                                instructions = instructions.trim()
                            )
                            onSavePreset(presetToSave)
                            showPresetDialog = false
                        }
                    }
                ) {
                    Text("Save Preset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
