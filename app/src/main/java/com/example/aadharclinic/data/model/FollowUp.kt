package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "follow_ups")
data class FollowUp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val patientName: String,
    val patientMobile: String = "",
    val consultationId: Long? = null,
    val scheduledDate: Long, // Epoch ms (start of target date)
    val reason: String = "Routine Review",
    val isCompleted: Boolean = false,
    val completedDate: Long? = null,
    val notes: String = ""
)
