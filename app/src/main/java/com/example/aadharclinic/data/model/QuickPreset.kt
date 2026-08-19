package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quick_presets")
data class QuickPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clinicId: String = "aadhar123",
    val presetName: String, // e.g. "Antacid", "Fever / Pain", "Antibiotic", "Cough & Cold"
    val medicineName: String, // e.g. "Pantoprazole 40 mg", "Paracetamol 650 mg"
    val strength: String = "40 mg",
    val dose: String = "1 Tab",
    val frequency: String = "BD", // e.g. "BD", "TID", "OD", "1-0-1", "1-1-1", "SOS", "HS"
    val route: String = "Oral", // "Oral", "Topical", "IV", "IM", "Inhalation"
    val defaultDuration: String = "7 days", // e.g. "7 days", "5 days", "3 days"
    val instructions: String = "Before Food", // e.g. "Before Food", "After Food", "At Bedtime", "जेवणापूर्वी"
    val quantity: Int = 14,
    val syncStatus: String = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
