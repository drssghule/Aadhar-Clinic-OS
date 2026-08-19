package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "consultations")
data class Consultation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: String = java.util.UUID.randomUUID().toString(),
    val clinicId: String = "AADHAR_CLINIC_PUNE",
    val doctorId: Long = 1,
    val doctorName: String = "Dr. Sanket Ghule",
    val patientId: Long,
    val patientName: String,
    val date: Long = System.currentTimeMillis(),
    val chiefComplaints: String = "",
    val bp: String = "",
    val pulse: String = "",
    val temperature: String = "",
    val spo2: String = "",
    val weight: String = "",
    val height: String = "",
    val rbs: String = "",
    val diagnosis: String = "",
    val doctorNotes: String = "",
    val nextFollowUpDate: Long? = null,
    val followUpInstructions: String = "",
    val consultationFee: Double = 0.0,
    val serviceCharge: Double = 0.0,
    val servicesSummary: String = "",
    val servicesJson: String = "",
    val medicineCharge: Double = 0.0,
    val otherCharge: Double = 0.0,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val paymentStatus: String = "Paid", // Paid, Pending, Partial
    val paymentMode: String = "Cash", // Cash, UPI, Card
    val receiptNumber: String = "",
    val syncStatus: String = SyncStatus.PENDING,
    val lastSyncedAt: Long = 0L,
    val version: Int = 1,
    val localCreatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
