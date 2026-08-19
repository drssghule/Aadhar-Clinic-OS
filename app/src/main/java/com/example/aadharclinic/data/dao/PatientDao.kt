package com.example.aadharclinic.data.dao

import androidx.room.*
import com.example.aadharclinic.data.model.Patient
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY createdAt DESC")
    fun getAllPatients(): Flow<List<Patient>>

    @Query("SELECT * FROM patients WHERE id = :id LIMIT 1")
    fun getPatientById(id: Long): Flow<Patient?>

    @Query("SELECT * FROM patients WHERE id = :id LIMIT 1")
    suspend fun getPatientByIdOnce(id: Long): Patient?

    @Query("""
        SELECT * FROM patients 
        WHERE name LIKE '%' || :query || '%' 
           OR mobile LIKE '%' || :query || '%' 
           OR patientCode LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchPatients(query: String): Flow<List<Patient>>

    @Query("SELECT COUNT(*) FROM patients")
    fun getPatientCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM patients")
    suspend fun getPatientCountOnce(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatients(patients: List<Patient>)

    @Update
    suspend fun updatePatient(patient: Patient)

    @Delete
    suspend fun deletePatient(patient: Patient)

    // Sync helpers
    @Query("SELECT * FROM patients WHERE syncStatus = :status")
    suspend fun getPatientsBySyncStatus(status: String = "PENDING"): List<Patient>

    @Query("SELECT * FROM patients WHERE recordId = :recordId LIMIT 1")
    suspend fun getPatientByRecordId(recordId: String): Patient?

    @Query("SELECT * FROM patients WHERE patientCode = :patientCode LIMIT 1")
    suspend fun getPatientByCode(patientCode: String): Patient?

    @Query("UPDATE patients SET syncStatus = :status, lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updatePatientSyncStatus(id: Long, status: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM patients WHERE syncStatus = 'PENDING'")
    suspend fun getPendingPatientCount(): Int

    @Query("SELECT COUNT(*) FROM patients WHERE syncStatus = 'SYNCED'")
    suspend fun getSyncedPatientCount(): Int

    @Query("SELECT * FROM patients")
    suspend fun getAllPatientsOnce(): List<Patient>
}
