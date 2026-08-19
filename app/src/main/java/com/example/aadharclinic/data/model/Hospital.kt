package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hospitals")
data class Hospital(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: String = java.util.UUID.randomUUID().toString(),
    val clinicId: String = "AADHAR_CLINIC_PUNE",
    val name: String,
    val address: String,
    val contactNumber: String = "",
    val email: String = "",
    val specialties: String = "",
    val syncStatus: String = SyncStatus.PENDING,
    val lastSyncedAt: Long = 0L,
    val version: Int = 1,
    val localCreatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
