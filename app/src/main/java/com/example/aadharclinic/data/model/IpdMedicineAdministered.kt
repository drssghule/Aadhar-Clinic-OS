package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ipd_medicines_administered",
    indices = [Index("ipdAdmissionId")]
)
data class IpdMedicineAdministered(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ipdAdmissionId: Long,
    val inventoryItemId: Long? = null,
    val medicineName: String,
    val dose: String = "1 Dose",
    val route: String = "IV", // IV, IM, Oral, SC
    val quantity: Int = 1,
    val administeredAt: Long = System.currentTimeMillis(),
    val administeredBy: String = "Staff Nurse",
    val unitCost: Double = 0.0,
    val totalCost: Double = 0.0
)
