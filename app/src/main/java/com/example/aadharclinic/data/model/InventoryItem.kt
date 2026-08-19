package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: String = java.util.UUID.randomUUID().toString(),
    val clinicId: String = "aadhar123",
    val name: String,
    val genericName: String = "",
    val category: String = "Tablet", // Tablet, Capsule, Syrup, Injection, IV Fluid, Ointment, Surgical
    val strength: String = "", // e.g. "500 mg", "40 mg", "625 mg"
    val defaultDose: String = "1 Tab",
    val defaultFrequency: String = "BD", // e.g. "BD", "TID", "OD", "1-0-1"
    val defaultDuration: String = "5 days", // e.g. "3 days", "5 days", "7 days"
    val defaultRoute: String = "Oral",
    val defaultInstructions: String = "After Food",
    val batchNumber: String = "",
    val expiryDate: String = "", // e.g. "12/2026"
    val currentStock: Int = 0,
    val minThreshold: Int = 20, // Low stock alert threshold
    val purchasePrice: Double = 0.0, // Cost price
    val sellingPrice: Double = 0.0, // Dispense/selling price
    val unit: String = "Tablet", // Tablet, Bottle, Vial, Piece, Strip
    val isHospitalStock: Boolean = false, // Hospital medicine, IV fluid, cannula, or in-house surgical consumable
    val syncStatus: String = SyncStatus.PENDING,
    val lastSyncedAt: Long = 0L,
    val version: Int = 1,
    val localCreatedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)
