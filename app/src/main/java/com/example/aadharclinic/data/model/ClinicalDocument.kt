package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clinical_documents")
data class ClinicalDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: String = java.util.UUID.randomUUID().toString(),
    val clinicId: String = "AADHAR_CLINIC_PUNE",
    val documentType: String, // "REFERRAL_LETTER" or "SICK_CERTIFICATE"
    val patientId: Long? = null,
    val patientName: String,
    val age: Int,
    val sex: String,
    val date: Long = System.currentTimeMillis(),

    // Doctor & Clinic details at time of generation
    val doctorName: String = "",
    val doctorQualification: String = "",
    val doctorRegNumber: String = "",
    val clinicName: String = "",
    val clinicAddress: String = "",
    val clinicContact: String = "",

    // Referral Letter fields
    val hospitalId: Long? = null,
    val hospitalName: String = "",
    val hospitalAddress: String = "",
    val chiefComplaints: String = "",
    val treatmentGiven: String = "",

    // Sick Certificate fields
    val residentOf: String = "",
    val daysCount: Int = 0,

    val pdfFilePath: String = "",
    val syncStatus: String = SyncStatus.PENDING,
    val lastSyncedAt: Long = 0L,
    val version: Int = 1,
    val localCreatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
