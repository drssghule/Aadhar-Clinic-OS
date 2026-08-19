package com.example.aadharclinic.data.dao

import androidx.room.*
import com.example.aadharclinic.data.model.ClinicService
import kotlinx.coroutines.flow.Flow

@Dao
interface ClinicServiceDao {
    @Query("SELECT * FROM clinic_services ORDER BY category ASC, serviceName ASC")
    fun getAllServices(): Flow<List<ClinicService>>

    @Query("SELECT * FROM clinic_services WHERE id = :id")
    suspend fun getServiceById(id: Long): ClinicService?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ClinicService): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<ClinicService>)

    @Update
    suspend fun updateService(service: ClinicService)

    @Delete
    suspend fun deleteService(service: ClinicService)

    @Query("SELECT COUNT(*) FROM clinic_services")
    suspend fun getServiceCount(): Int
}
