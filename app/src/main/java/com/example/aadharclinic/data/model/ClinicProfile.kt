package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clinic_profile")
data class ClinicProfile(
    @PrimaryKey val id: Int = 1,
    val hospitalId: String = "aadhar123",
    val clinicName: String = "Aadhar Multi-Speciality Clinic",
    val doctorName: String = "Dr. Sanket Ghule",
    val qualification: String = "BAMS EMS",
    val regNumber: String = "MCIM/EMS-74892",
    val address: String = "102, Shanti Complex, Station Road, Pune - 411001",
    val contactNumber: String = "+91 98230 12345",
    val email: String = "dr.s.s.ghule@gmail.com",
    val defaultConsultationFee: Double = 300.0,
    val prescriptionHeader: String = "Complete Family Healthcare & In-Patient Facility",
    val prescriptionFooter: String = "Timings: Mon - Sat (9:00 AM - 1:30 PM & 5:30 PM - 9:00 PM) | Sunday Emergency Only",
    val currency: String = "₹",
    val passwordHash: String = "clinic123" // Default secure clinic login password
)
