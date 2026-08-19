package com.example.aadharclinic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aadharclinic.data.model.ClinicProfile
import com.example.aadharclinic.data.model.SyncSummary
import com.example.aadharclinic.ui.viewmodel.DiseaseCount
import com.example.aadharclinic.ui.viewmodel.RevenueDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    profile: ClinicProfile?,
    todayPatientsCount: Int,
    todayRevenueAmount: Double,
    currentIpdCount: Int,
    lowStockCount: Int,
    revenueHistory: List<RevenueDay>,
    topDiseasesList: List<DiseaseCount>,
    syncSummary: SyncSummary = SyncSummary(),
    onSyncNow: () -> Unit = {},
    onQuickAddPatient: () -> Unit,
    onQuickConsultation: () -> Unit,
    onQuickLetters: () -> Unit = {},
    onQuickIpd: () -> Unit,
    onQuickReports: () -> Unit,
    onNavigatePatients: () -> Unit,
    onNavigateStock: () -> Unit,
    onNavigateBilling: () -> Unit,
    onNavigateSettings: () -> Unit = {}
) {
    var selectedDiseaseForDialog by remember { mutableStateOf<DiseaseCount?>(null) }
    val currency = profile?.currency ?: "₹"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Doctor & Clinic Header Greeting
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile?.clinicName ?: "Aadhar Clinic OS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Hello, ${profile?.doctorName ?: "Doctor"} • Today's Clinic Overview",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Offline-First & Cloud Sync Status Banner
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (!syncSummary.isOnline) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                } else if (syncSummary.pendingCount > 0) {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (!syncSummary.isOnline) Color(0xFFE53935)
                                    else if (syncSummary.isSyncing) Color(0xFF1976D2)
                                    else if (syncSummary.pendingCount > 0) Color(0xFFFB8C00)
                                    else Color(0xFF43A047)
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (!syncSummary.isOnline) "Offline Mode — Data Saved Locally"
                                else if (syncSummary.isSyncing) "Syncing with Hospital Cloud..."
                                else if (syncSummary.pendingCount > 0) "${syncSummary.pendingCount} Records Pending Cloud Sync"
                                else "Cloud Synchronized (${syncSummary.syncedCount} Records)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (!syncSummary.isOnline) "All patient entries, prescriptions & billing work 100% offline."
                                else "Auto-syncs with Firebase cloud when connected.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (syncSummary.isOnline && syncSummary.pendingCount > 0) {
                        FilledTonalButton(
                            onClick = onSyncNow,
                            enabled = !syncSummary.isSyncing,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            if (syncSummary.isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Row: + Patient | Consultation | Stock | Reports
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "QUICK ACTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionButton(
                            icon = Icons.Filled.PersonAdd,
                            label = "+ Patient",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            testTag = "quick_action_add_patient",
                            onClick = onQuickAddPatient,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        QuickActionButton(
                            icon = Icons.Filled.Medication,
                            label = "OPD Rx",
                            containerColor = Color(0xFFE8F5E9),
                            contentColor = Color(0xFF1B5E20),
                            testTag = "quick_action_consultation",
                            onClick = onQuickConsultation,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        QuickActionButton(
                            icon = Icons.Filled.Description,
                            label = "Letters",
                            containerColor = Color(0xFFE0F2F1),
                            contentColor = Color(0xFF00695C),
                            testTag = "quick_action_letters",
                            onClick = onQuickLetters,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        QuickActionButton(
                            icon = Icons.Filled.Inventory2,
                            label = "Stock",
                            containerColor = Color(0xFFFFF3E0),
                            contentColor = Color(0xFFE65100),
                            testTag = "quick_action_stock",
                            onClick = onNavigateStock,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        QuickActionButton(
                            icon = Icons.Filled.Analytics,
                            label = "Reports",
                            containerColor = Color(0xFFEDE7F6),
                            contentColor = Color(0xFF4A148C),
                            testTag = "quick_action_reports",
                            onClick = onQuickReports,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4 Core Summary KPI Cards (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Today's Patients
                    StatCard(
                        title = "Today's Patients",
                        value = "$todayPatientsCount",
                        subtitle = "OPD Consultations",
                        icon = Icons.Filled.PeopleAlt,
                        iconTint = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        onClick = onNavigatePatients,
                        modifier = Modifier.weight(1f),
                        testTag = "stat_today_patients"
                    )

                    // Today's Revenue (Services only)
                    StatCard(
                        title = "Today's Revenue",
                        value = "$currency${String.format("%.0f", todayRevenueAmount)}",
                        subtitle = "Services Collected",
                        icon = Icons.Filled.Payments,
                        iconTint = Color(0xFF2E7D32),
                        containerColor = MaterialTheme.colorScheme.surface,
                        onClick = onQuickReports,
                        modifier = Modifier.weight(1f),
                        testTag = "stat_today_revenue"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Active Daycare
                    StatCard(
                        title = "Active Daycare",
                        value = "$currentIpdCount",
                        subtitle = "Patients in Care",
                        icon = Icons.Filled.SingleBed,
                        iconTint = Color(0xFFE65100),
                        containerColor = MaterialTheme.colorScheme.surface,
                        onClick = onQuickIpd,
                        modifier = Modifier.weight(1f),
                        testTag = "stat_current_daycare"
                    )

                    // Low Stock Alert
                    StatCard(
                        title = "Stock Status",
                        value = if (lowStockCount > 0) "$lowStockCount Low" else "Adequate",
                        subtitle = "Medicines & IVs",
                        icon = Icons.Filled.Inventory,
                        iconTint = if (lowStockCount > 0) Color(0xFFBA1A1A) else Color(0xFF0277BD),
                        containerColor = MaterialTheme.colorScheme.surface,
                        onClick = onNavigateStock,
                        modifier = Modifier.weight(1f),
                        testTag = "stat_stock_status"
                    )
                }
            }
        }

        // 7-Day Revenue Graph
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("revenue_graph_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "7-DAY REVENUE TREND",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline,
                                letterSpacing = 1.sp
                            )
                            val total7Days = revenueHistory.sumOf { it.amount }
                            Text(
                                text = "$currency ${String.format("%,.0f", total7Days)} Total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        AssistChip(
                            onClick = onQuickReports,
                            label = { Text("A4 Reports", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Minimal Bar Visualization for 7 Days
                    val maxAmount = (revenueHistory.maxOfOrNull { it.amount } ?: 1000.0).coerceAtLeast(500.0)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        revenueHistory.forEach { day ->
                            val heightFraction = if (maxAmount > 0) (day.amount / maxAmount).toFloat().coerceIn(0.06f, 1f) else 0.06f
                            val isToday = day == revenueHistory.lastOrNull()

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                if (day.amount > 0) {
                                    Text(
                                        text = "${day.amount.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (isToday) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.primaryContainer
                                        )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = day.dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Diseases
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("top_diseases_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOP PRESENTING COMPLAINTS / DISEASES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Tap to view patients",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (topDiseasesList.isEmpty()) {
                        Text(
                            text = "No diagnostic records yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        topDiseasesList.take(6).forEachIndexed { index, diseaseCount ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedDiseaseForDialog = diseaseCount }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = diseaseCount.disease,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        Text(
                                            text = "${diseaseCount.count} patients",
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Filled.ChevronRight,
                                        contentDescription = "View",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (index < topDiseasesList.take(6).size - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.8.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Disease Patient Names Dialog
    if (selectedDiseaseForDialog != null) {
        val item = selectedDiseaseForDialog!!
        AlertDialog(
            onDismissRequest = { selectedDiseaseForDialog = null },
            icon = { Icon(Icons.Filled.Coronavirus, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = item.disease,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Patients Diagnosed (${item.patientNames.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    item.patientNames.forEachIndexed { i, name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${i + 1}. ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDiseaseForDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        modifier = modifier
            .height(72.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
