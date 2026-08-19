package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ipd_daily_notes",
    indices = [Index("ipdAdmissionId")]
)
data class IpdDailyNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ipdAdmissionId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val doctorOrNurse: String = "Doctor",
    val bp: String = "",
    val pulse: String = "",
    val temp: String = "",
    val spo2: String = "",
    val clinicalNotes: String = "",
    val treatmentGiven: String = ""
)
