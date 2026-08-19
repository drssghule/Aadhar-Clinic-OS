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
import com.example.aadharclinic.data.model.InventoryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    inventoryList: List<InventoryItem>,
    lowStockList: List<InventoryItem>,
    currency: String,
    onSaveItem: (InventoryItem) -> Unit,
    onAdjustStock: (itemId: Long, quantityDelta: Int, type: String, notes: String) -> Unit,
    onDeleteItem: (InventoryItem) -> Unit,
    onExportSpreadsheet: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All Stock, 1: Low Stock
    var showAddDialog by remember { mutableStateOf(false) }
    var itemForStockAction by remember { mutableStateOf<InventoryItem?>(null) }
    var isAddingStockAction by remember { mutableStateOf(true) } // true: + add, false: - reduce

    val filteredList = remember(inventoryList, lowStockList, searchQuery, selectedTab) {
        val base = if (selectedTab == 1) lowStockList else inventoryList
        if (searchQuery.isBlank()) base
        else base.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.genericName.contains(searchQuery, ignoreCase = true) ||
                    it.batchNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top Header & "+ Add Stock" Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CLINIC MEDICINE STOCK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${inventoryList.size} Medicines in Stock",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("add_stock_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Stock", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search medicine name (e.g. Paracetamol, NS, Inj)...") },
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

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("All Medicines (${inventoryList.size})") }
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Low Stock (${lowStockList.size})")
                        if (lowStockList.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFBA1A1A), CircleShape)
                            )
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No medicine matches '$searchQuery'" else "No stock items found.",
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    StockItemCard(
                        item = item,
                        onAddQuantity = {
                            itemForStockAction = item
                            isAddingStockAction = true
                        },
                        onReduceQuantity = {
                            itemForStockAction = item
                            isAddingStockAction = false
                        }
                    )
                }
            }
        }
    }

    // Minimal Add Stock Dialog
    if (showAddDialog) {
        MinimalAddStockDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newItem ->
                onSaveItem(newItem)
                showAddDialog = false
            }
        )
    }

    // Minimal Adjust Stock Dialog (+ Add / - Reduce)
    if (itemForStockAction != null) {
        MinimalStockAdjustDialog(
            item = itemForStockAction!!,
            isAdd = isAddingStockAction,
            onDismiss = { itemForStockAction = null },
            onConfirm = { delta, reason ->
                val adjustedDelta = if (isAddingStockAction) delta else -delta
                val type = if (isAddingStockAction) "STOCK_ADD" else "STOCK_USE"
                onAdjustStock(itemForStockAction!!.id, adjustedDelta, type, reason)
                itemForStockAction = null
            }
        )
    }
}

/**
 * Minimal Stock Card:
 * Medicine name — Remaining quantity
 * Example: Paracetamol — 120
 */
@Composable
fun StockItemCard(
    item: InventoryItem,
    onAddQuantity: () -> Unit,
    onReduceQuantity: () -> Unit
) {
    val isLowStock = item.currentStock <= item.minThreshold

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLowStock) Color(0xFFFFF4F4) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Medicine Name and Quantity
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isLowStock) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " — ${item.currentStock}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isLowStock) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLowStock) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = "LOW STOCK",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBA1A1A)
                            )
                        }
                    }

                    if (item.expiryDate.isNotBlank()) {
                        Text(
                            text = "Exp: ${item.expiryDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (item.batchNumber.isNotBlank()) {
                        Text(
                            text = "Batch: ${item.batchNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Quick Actions: + Add / - Use
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalIconButton(
                    onClick = onReduceQuantity,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Reduce Stock", modifier = Modifier.size(18.dp))
                }

                FilledTonalIconButton(
                    onClick = onAddQuantity,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Stock", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/**
 * Minimal Add Stock Dialog:
 * - Medicine name
 * - Quantity added
 * - Optional: Expiry date, Batch number
 * - Save
 */
@Composable
fun MinimalAddStockDialog(
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AddBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Medicine Stock", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; hasError = false },
                    label = { Text("Medicine Name *") },
                    placeholder = { Text("e.g. Paracetamol 650, NS 500ml") },
                    singleLine = true,
                    isError = hasError && name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it.filter { ch -> ch.isDigit() }; hasError = false },
                    label = { Text("Quantity Added *") },
                    placeholder = { Text("e.g. 100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = hasError && quantityStr.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = expiry,
                        onValueChange = { expiry = it },
                        label = { Text("Expiry (Optional)") },
                        placeholder = { Text("MM/YYYY") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = batch,
                        onValueChange = { batch = it },
                        label = { Text("Batch (Optional)") },
                        placeholder = { Text("B-101") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || quantityStr.isBlank()) {
                        hasError = true
                        return@Button
                    }
                    val qty = quantityStr.toIntOrNull() ?: 0
                    val newItem = InventoryItem(
                        name = name.trim(),
                        genericName = name.trim(),
                        category = "General",
                        batchNumber = batch.trim(),
                        expiryDate = expiry.trim(),
                        currentStock = qty,
                        minThreshold = 20,
                        purchasePrice = 0.0,
                        sellingPrice = 0.0,
                        unit = "Units",
                        isHospitalStock = true
                    )
                    onSave(newItem)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Stock", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Minimal Stock Adjust Dialog:
 * Quick + Add or - Reduce quantity
 */
@Composable
fun MinimalStockAdjustDialog(
    item: InventoryItem,
    isAdd: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, notes: String) -> Unit
) {
    var qtyStr by remember { mutableStateOf("10") }
    var notes by remember { mutableStateOf(if (isAdd) "Stock Restocked" else "Used in Clinic") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isAdd) "Add Stock: ${item.name}" else "Reduce Stock: ${item.name}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Current remaining quantity: ${item.currentStock}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (isAdd) "Quantity to Add" else "Quantity Used / Reduced") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick preset buttons (1, 5, 10, 20, 50)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(1, 5, 10, 20, 50).forEach { preset ->
                        FilledTonalButton(
                            onClick = { qtyStr = preset.toString() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("$preset", fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reason / Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyStr.toIntOrNull() ?: 0
                    if (qty > 0) {
                        onConfirm(qty, notes.trim())
                    }
                },
                colors = if (!isAdd) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isAdd) "Add to Stock" else "Deduct Stock", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
