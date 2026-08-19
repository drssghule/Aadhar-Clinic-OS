package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: String = java.util.UUID.randomUUID().toString(),
    val clinicId: String = "AADHAR_CLINIC_PUNE",
    val title: String,
    val category: String = "Miscellaneous", // "Electricity Bill", "Hospital Medicine & IV Stock", "Staff Salary", "Clinic Rent", "Miscellaneous", "Biomedical Waste", "Equipment & Maintenance"
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val paymentMode: String = "Cash", // Cash, UPI, Bank Transfer, Cheque
    val referenceNumber: String = "", // e.g. Bill / Voucher No
    val notes: String = "",
    val inventoryItemId: Long? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val lastSyncedAt: Long = 0L,
    val version: Int = 1,
    val localCreatedAt: Long = System.currentTimeMillis()
)
