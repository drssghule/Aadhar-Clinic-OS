package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: String = java.util.UUID.randomUUID().toString(),
    val clinicId: String = "AADHAR_CLINIC_PUNE",
    val createdById: Long = 1,
    val createdByName: String = "Dr. Sanket Ghule",
    val updatedByName: String = "Dr. Sanket Ghule",
    val patientCode: String,
    val name: String,
    val age: Int,
    val dob: String = "",
    val sex: String = "Male",
    val mobile: String = "",
    val address: String = "",
    val bloodGroup: String = "",
    val allergies: String = "None",
    val medicalHistory: String = "",
    val syncStatus: String = SyncStatus.PENDING,
    val lastSyncedAt: Long = 0L,
    val version: Int = 1,
    val localCreatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
