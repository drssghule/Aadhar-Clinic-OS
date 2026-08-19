package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ipd_admissions")
data class IpdAdmission(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: String = java.util.UUID.randomUUID().toString(),
    val clinicId: String = "AADHAR_CLINIC_PUNE",
    val doctorId: Long = 1,
    val doctorName: String = "Dr. Sanket Ghule",
    val patientId: Long,
    val patientName: String,
    val admissionDate: Long = System.currentTimeMillis(),
    val dischargeDate: Long? = null,
    val status: String = "Admitted", // Admitted, Discharged
    val bedRoomNumber: String = "Bed 1",
    val admittingDiagnosis: String = "",
    val finalDiagnosis: String = "",
    val depositAdvance: Double = 0.0,
    val roomChargePerDay: Double = 1000.0,
    val nursingChargePerDay: Double = 300.0,
    val doctorRoundsFee: Double = 500.0,
    val procedureCharges: Double = 0.0,
    val otherCharges: Double = 0.0,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val paymentStatus: String = "Pending", // Paid, Pending, Partial
    val paymentMode: String = "Cash", // Cash, UPI, Card
    val dischargeSummary: String = "",
    val dischargeCondition: String = "Stable", // Stable, Improved, Cured, Referred
    val dischargeAdvice: String = "",
    val receiptNumber: String = "",
    val syncStatus: String = SyncStatus.PENDING,
    val lastSyncedAt: Long = 0L,
    val version: Int = 1,
    val localCreatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
