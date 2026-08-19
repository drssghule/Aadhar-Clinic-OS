package com.example.aadharclinic.data.db

import androidx.room.TypeConverter
import com.example.aadharclinic.data.model.MedicineSourceType

class Converters {
    @TypeConverter
    fun fromMedicineSourceType(value: MedicineSourceType): String {
        return value.name
    }

    @TypeConverter
    fun toMedicineSourceType(value: String): MedicineSourceType {
        return try {
            MedicineSourceType.valueOf(value)
        } catch (e: Exception) {
            MedicineSourceType.CHEMIST
        }
    }
}
