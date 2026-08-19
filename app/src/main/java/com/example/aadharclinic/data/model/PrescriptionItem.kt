package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MedicineSourceType {
    CLINIC_STOCK, // Dispensed from in-house clinic stock -> reduces inventory
    CHEMIST       // Prescribed to buy from outside chemist -> DOES NOT reduce inventory
}

@Entity(
    tableName = "prescription_items",
    indices = [Index("consultationId")]
)
data class PrescriptionItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val consultationId: Long,
    val medicineName: String,
    val strength: String = "", // e.g. 500mg, 10ml, 1g
    val dose: String = "1 Tab", // e.g. 1 Tab, 2 Puffs, 5ml
    val route: String = "Oral", // Oral, Topical, IV, IM, Inhalation, Sublingual
    val frequency: String = "1-0-1", // 1-0-1, 1-1-1, 0-0-1, 1-0-0, SOS, Stat, QDS
    val duration: String = "5 Days", // 3 Days, 5 Days, 1 Week, 1 Month
    val quantity: Int = 10,
    val instructions: String = "After Food", // Before Food, After Food, At Bedtime, With Milk
    val sourceType: MedicineSourceType = MedicineSourceType.CLINIC_STOCK,
    val inventoryItemId: Long? = null,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0
)
