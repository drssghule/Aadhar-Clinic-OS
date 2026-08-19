package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,
    DOCTOR,
    STAFF,
    RECEPTION
}

@Entity(tableName = "clinic_users")
data class ClinicUser(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: String = java.util.UUID.randomUUID().toString(),
    val clinicId: String = "AADHAR_CLINIC_PUNE",
    val loginId: String, // Unique user ID (e.g. dr_sanket, dr_joshi, staff_pooja)
    val passwordHash: String, // Password
    val name: String, // e.g. Dr. Sanket Ghule
    val role: UserRole = UserRole.DOCTOR,
    val qualification: String = "BAMS EMS",
    val regNumber: String = "",
    val contactNumber: String = "",
    val email: String = "",
    val isActive: Boolean = true,
    val syncStatus: String = SyncStatus.PENDING,
    val lastSyncedAt: Long = 0L,
    val version: Int = 1,
    val localCreatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
