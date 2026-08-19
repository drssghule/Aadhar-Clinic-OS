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
import com.example.aadharclinic.data.model.BillInvoice
import com.example.aadharclinic.data.model.ClinicService
import com.example.aadharclinic.data.model.Patient
import com.example.aadharclinic.data.model.PredefinedClinicServices
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    invoices: List<BillInvoice>,
    patients: List<Patient>,
    servicesList: List<ClinicService> = emptyList(),
    currency: String = "₹",
    onSaveInvoice: (BillInvoice) -> Unit,
    onDeleteInvoice: (BillInvoice) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateInvoiceDialog by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    val filteredInvoices = remember(invoices, searchQuery) {
        if (searchQuery.isBlank()) invoices
        else invoices.filter {
            it.patientName.contains(searchQuery, ignoreCase = true) ||
                    it.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    it.notes.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalCollected = remember(invoices) { invoices.sumOf { it.paidAmount } }
    val totalPending = remember(invoices) { invoices.sumOf { it.balanceDue } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top Summary & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CLINIC BILLING (SERVICES ONLY)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$currency ${String.format("%,.0f", totalCollected)} Total Collected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Button(
                onClick = { showCreateInvoiceDialog = true },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("create_invoice_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Bill", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by patient name or bill #...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredInvoices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching bills found." else "No bills generated yet.",
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredInvoices, key = { it.id }) { invoice ->
                    InvoiceItemCard(
                        invoice = invoice,
                        currency = currency,
                        dateFormat = dateFormat
                    )
                }
            }
        }
    }

    if (showCreateInvoiceDialog) {
        CreateServiceBillDialog(
            patients = patients,
            servicesList = servicesList,
            currency = currency,
            onDismiss = { showCreateInvoiceDialog = false },
            onSave = { inv ->
                onSaveInvoice(inv)
                showCreateInvoiceDialog = false
            }
        )
    }
}

@Composable
fun InvoiceItemCard(
    invoice: BillInvoice,
    currency: String,
    dateFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = invoice.patientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${invoice.invoiceNumber} • ${dateFormat.format(Date(invoice.date))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (invoice.paymentStatus.lowercase()) {
                        "paid" -> Color(0xFFE8F5E9)
                        "partial" -> Color(0xFFFFF3E0)
                        else -> Color(0xFFFFEBEE)
                    }
                ) {
                    Text(
                        text = "${invoice.paymentStatus.uppercase()} (${invoice.paymentMode})",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (invoice.paymentStatus.lowercase()) {
                            "paid" -> Color(0xFF2E7D32)
                            "partial" -> Color(0xFFE65100)
                            else -> Color(0xFFC62828)
                        }
                    )
                }
            }

            if (invoice.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = invoice.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Billed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Text(
                    text = "$currency ${String.format("%,.0f", invoice.totalAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServiceBillDialog(
    patients: List<Patient>,
    servicesList: List<ClinicService>,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (BillInvoice) -> Unit
) {
    var selectedPatient by remember { mutableStateOf(patients.firstOrNull()) }
    var patientDropdownExpanded by remember { mutableStateOf(false) }

    // Resolve service prices: use saved price from settings or fallback to predefined
    val resolvedServices = remember(servicesList) {
        PredefinedClinicServices.STANDARD_SERVICES.map { def ->
            val match = servicesList.find { it.serviceName.equals(def.serviceName, ignoreCase = true) }
            val price = match?.defaultPrice ?: def.defaultPrice
            def.copy(defaultPrice = price)
        }
    }

    // Checkbox selections map: serviceName -> isChecked (default Consultation checked)
    val checkedServices = remember {
        mutableStateMapOf<String, Boolean>().apply {
            resolvedServices.forEach { svc ->
                this[svc.serviceName] = (svc.serviceName.equals("Consultation", ignoreCase = true))
            }
        }
    }

    // Auto-calculated total from checked service boxes
    val autoCalculatedTotal = remember(checkedServices.values.toList()) {
        resolvedServices.filter { checkedServices[it.serviceName] == true }.sumOf { it.defaultPrice }
    }

    var customAmountStr by remember(autoCalculatedTotal) {
        mutableStateOf(autoCalculatedTotal.toInt().toString())
    }

    var paymentMode by remember { mutableStateOf("Cash") }
    var paymentStatus by remember { mutableStateOf("Paid") }
    var customNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Service Bill", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Select Patient
                item {
                    Text("1. SELECT PATIENT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = patientDropdownExpanded,
                        onExpandedChange = { patientDropdownExpanded = !patientDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPatient?.let { "${it.name} (${it.patientCode})" } ?: "Select Patient",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = patientDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = patientDropdownExpanded,
                            onDismissRequest = { patientDropdownExpanded = false }
                        ) {
                            patients.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (${p.patientCode} • ${p.mobile})") },
                                    onClick = {
                                        selectedPatient = p
                                        patientDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. Predefined Service Checkboxes (9 standard services)
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("2. SELECT SERVICES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        resolvedServices.forEach { svc ->
                            val isChecked = checkedServices[svc.serviceName] ?: false
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        checkedServices[svc.serviceName] = !isChecked
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checkedServices[svc.serviceName] = it }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(svc.serviceName, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal)
                                }
                                Text(
                                    text = "$currency ${svc.defaultPrice.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                // 3. Final Amount & Payment Mode
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("3. FINAL AMOUNT & PAYMENT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = customAmountStr,
                        onValueChange = { customAmountStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Total Bill Amount ($currency) - Editable") },
                        prefix = { Text("$currency ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Cash", "UPI", "Card").forEach { mode ->
                            FilterChip(
                                selected = paymentMode == mode,
                                onClick = { paymentMode = mode },
                                label = { Text(mode) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Paid", "Pending").forEach { status ->
                            FilterChip(
                                selected = paymentStatus == status,
                                onClick = { paymentStatus = status },
                                label = { Text(status) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedPatient == null) return@Button
                    val finalAmount = customAmountStr.toDoubleOrNull() ?: autoCalculatedTotal
                    val timestamp = System.currentTimeMillis()
                    val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))
                    val randomSuffix = (100..999).random()

                    val activeServiceNames = resolvedServices
                        .filter { checkedServices[it.serviceName] == true }
                        .joinToString(", ") { "${it.serviceName} ($currency${it.defaultPrice.toInt()})" }

                    val invoice = BillInvoice(
                        invoiceNumber = "INV-$dateStr-$randomSuffix",
                        patientId = selectedPatient!!.id,
                        patientName = selectedPatient!!.name,
                        category = "OPD Services",
                        date = timestamp,
                        consultationFee = if (checkedServices["Consultation"] == true) (resolvedServices.find { it.serviceName == "Consultation" }?.defaultPrice ?: 300.0) else 0.0,
                        procedureCharges = finalAmount,
                        medicineCharges = 0.0, // Strict rule: No medicine billing
                        subtotal = finalAmount,
                        totalAmount = finalAmount,
                        paidAmount = if (paymentStatus == "Paid") finalAmount else 0.0,
                        balanceDue = if (paymentStatus == "Paid") 0.0 else finalAmount,
                        paymentStatus = paymentStatus,
                        paymentMode = paymentMode,
                        notes = if (activeServiceNames.isNotBlank()) activeServiceNames else "Clinic Services Bill"
                    )
                    onSave(invoice)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Bill", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
