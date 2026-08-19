package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bill_invoices")
data class BillInvoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: String = java.util.UUID.randomUUID().toString(),
    val clinicId: String = "AADHAR_CLINIC_PUNE",
    val doctorId: Long = 1,
    val doctorName: String = "Dr. Sanket Ghule",
    val collectedByName: String = "Dr. Sanket Ghule",
    val invoiceNumber: String,
    val patientId: Long,
    val patientName: String,
    val category: String, // Consultation, IPD, Procedure, Medicine, Other
    val consultationId: Long? = null,
    val ipdAdmissionId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val consultationFee: Double = 0.0,
    val ipdBedCharges: Double = 0.0,
    val procedureCharges: Double = 0.0,
    val medicineCharges: Double = 0.0,
    val otherCharges: Double = 0.0,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val balanceDue: Double = 0.0,
    val paymentStatus: String = "Paid", // Paid, Pending, Partial
    val paymentMode: String = "Cash", // Cash, UPI, Card
    val notes: String = "",
    val syncStatus: String = SyncStatus.PENDING,
    val lastSyncedAt: Long = 0L,
    val version: Int = 1,
    val localCreatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
