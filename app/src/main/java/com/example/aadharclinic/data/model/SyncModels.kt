package com.example.aadharclinic.data.model

object SyncStatus {
    const val PENDING = "PENDING"
    const val SYNCED = "SYNCED"
    const val FAILED = "FAILED"
}

data class SyncSummary(
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    val lastSyncTime: Long = 0L,
    val statusMessage: String = "All data synced",
    val cloudHospitalId: String = "AADHAR_CLINIC_PUNE",
    val activeConsultantEmail: String = "dr.s.s.ghule@gmail.com"
)

data class CloudBackupInfo(
    val hospitalId: String,
    val hospitalName: String,
    val totalRecords: Int,
    val patientCount: Int,
    val consultationCount: Int,
    val admissionCount: Int,
    val invoiceCount: Int,
    val inventoryCount: Int,
    val lastSyncTimestamp: Long,
    val cloudVersion: String = "v2.1"
)

data class ConflictAuditEntry(
    val id: Long = 0,
    val recordId: String,
    val recordType: String,
    val conflictField: String,
    val localValue: String,
    val remoteValue: String,
    val resolvedValue: String,
    val resolvedBy: String,
    val timestamp: Long = System.currentTimeMillis()
)
