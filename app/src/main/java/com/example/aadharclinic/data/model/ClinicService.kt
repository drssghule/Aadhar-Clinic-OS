package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clinic_services")
data class ClinicService(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceName: String,
    val defaultPrice: Double,
    val category: String = "Procedure", // Consultation, Procedure, Nursing, Diagnostics, Daycare, Other
    val description: String = "",
    val isSystemDefault: Boolean = false
)

data class ServiceEntryItem(
    val serviceId: Long? = null,
    val serviceName: String,
    val unitPrice: Double,
    val quantity: Int = 1,
    val totalPrice: Double = unitPrice * quantity
)

object PredefinedClinicServices {
    val STANDARD_SERVICES = listOf(
        ClinicService(1L, "Consultation", 300.0, "Consultation", isSystemDefault = true),
        ClinicService(2L, "Injection", 100.0, "Nursing", isSystemDefault = true),
        ClinicService(3L, "IV", 300.0, "Nursing", isSystemDefault = true),
        ClinicService(4L, "IV + Injection", 350.0, "Nursing", isSystemDefault = true),
        ClinicService(5L, "Dressing", 150.0, "Procedure", isSystemDefault = true),
        ClinicService(6L, "Suturing", 400.0, "Procedure", isSystemDefault = true),
        ClinicService(7L, "Nebulization", 100.0, "Procedure", isSystemDefault = true),
        ClinicService(8L, "Procedure", 500.0, "Procedure", isSystemDefault = true),
        ClinicService(9L, "Other", 200.0, "Other", isSystemDefault = true)
    )
}

