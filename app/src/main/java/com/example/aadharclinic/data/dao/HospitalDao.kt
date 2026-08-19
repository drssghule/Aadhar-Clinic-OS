package com.example.aadharclinic.data.dao

import androidx.room.*
import com.example.aadharclinic.data.model.Hospital
import kotlinx.coroutines.flow.Flow

@Dao
interface HospitalDao {
    @Query("SELECT * FROM hospitals ORDER BY name ASC")
    fun getAllHospitals(): Flow<List<Hospital>>

    @Query("SELECT * FROM hospitals WHERE name LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchHospitals(query: String): Flow<List<Hospital>>

    @Query("SELECT * FROM hospitals WHERE id = :id LIMIT 1")
    suspend fun getHospitalById(id: Long): Hospital?

    @Query("SELECT COUNT(*) FROM hospitals")
    suspend fun getHospitalCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHospital(hospital: Hospital): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHospitals(hospitals: List<Hospital>)

    @Update
    suspend fun updateHospital(hospital: Hospital)

    @Delete
    suspend fun deleteHospital(hospital: Hospital)

    // Sync helpers
    @Query("SELECT * FROM hospitals WHERE syncStatus = :status")
    suspend fun getHospitalsBySyncStatus(status: String = "PENDING"): List<Hospital>

    @Query("SELECT * FROM hospitals WHERE recordId = :recordId LIMIT 1")
    suspend fun getHospitalByRecordId(recordId: String): Hospital?

    @Query("UPDATE hospitals SET syncStatus = :status, lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateHospitalSyncStatus(id: Long, status: String, timestamp: Long)

    @Query("SELECT * FROM hospitals")
    suspend fun getAllHospitalsOnce(): List<Hospital>
}
