package com.example.aadharclinic.data.dao

import androidx.room.*
import com.example.aadharclinic.data.model.IpdAdmission
import com.example.aadharclinic.data.model.IpdDailyNote
import com.example.aadharclinic.data.model.IpdMedicineAdministered
import kotlinx.coroutines.flow.Flow

@Dao
interface IpdDao {
    @Query("SELECT * FROM ipd_admissions ORDER BY admissionDate DESC")
    fun getAllAdmissions(): Flow<List<IpdAdmission>>

    @Query("SELECT * FROM ipd_admissions WHERE status = 'Admitted' ORDER BY admissionDate DESC")
    fun getActiveAdmissions(): Flow<List<IpdAdmission>>

    @Query("SELECT COUNT(*) FROM ipd_admissions WHERE status = 'Admitted'")
    fun getActiveAdmissionCount(): Flow<Int>

    @Query("SELECT * FROM ipd_admissions WHERE id = :id LIMIT 1")
    fun getAdmissionById(id: Long): Flow<IpdAdmission?>

    @Query("SELECT * FROM ipd_admissions WHERE id = :id LIMIT 1")
    suspend fun getAdmissionByIdOnce(id: Long): IpdAdmission?

    @Query("SELECT * FROM ipd_admissions WHERE patientId = :patientId ORDER BY admissionDate DESC")
    fun getAdmissionsForPatient(patientId: Long): Flow<List<IpdAdmission>>

    @Query("SELECT * FROM ipd_admissions WHERE patientId = :patientId")
    suspend fun getAdmissionsForPatientOnce(patientId: Long): List<IpdAdmission>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmission(admission: IpdAdmission): Long

    @Update
    suspend fun updateAdmission(admission: IpdAdmission)

    @Delete
    suspend fun deleteAdmission(admission: IpdAdmission)

    // Daily Notes
    @Query("SELECT * FROM ipd_daily_notes WHERE ipdAdmissionId = :admissionId ORDER BY timestamp DESC")
    fun getDailyNotes(admissionId: Long): Flow<List<IpdDailyNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyNote(note: IpdDailyNote): Long

    @Delete
    suspend fun deleteDailyNote(note: IpdDailyNote)

    @Query("DELETE FROM ipd_daily_notes WHERE ipdAdmissionId = :admissionId")
    suspend fun deleteDailyNotesForAdmission(admissionId: Long)

    // Medicines Administered
    @Query("SELECT * FROM ipd_medicines_administered WHERE ipdAdmissionId = :admissionId ORDER BY administeredAt DESC")
    fun getMedicinesAdministered(admissionId: Long): Flow<List<IpdMedicineAdministered>>

    @Query("SELECT * FROM ipd_medicines_administered")
    fun getAllMedicinesAdministered(): Flow<List<IpdMedicineAdministered>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicineAdministered(item: IpdMedicineAdministered): Long

    @Delete
    suspend fun deleteMedicineAdministered(item: IpdMedicineAdministered)

    @Query("DELETE FROM ipd_medicines_administered WHERE ipdAdmissionId = :admissionId")
    suspend fun deleteMedicinesAdministeredForAdmission(admissionId: Long)

    // Sync helpers
    @Query("SELECT * FROM ipd_admissions WHERE syncStatus = :status")
    suspend fun getAdmissionsBySyncStatus(status: String = "PENDING"): List<IpdAdmission>

    @Query("SELECT * FROM ipd_admissions WHERE recordId = :recordId LIMIT 1")
    suspend fun getAdmissionByRecordId(recordId: String): IpdAdmission?

    @Query("UPDATE ipd_admissions SET syncStatus = :status, lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateAdmissionSyncStatus(id: Long, status: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM ipd_admissions WHERE syncStatus = 'PENDING'")
    suspend fun getPendingAdmissionCount(): Int

    @Query("SELECT COUNT(*) FROM ipd_admissions WHERE syncStatus = 'SYNCED'")
    suspend fun getSyncedAdmissionCount(): Int

    @Query("SELECT * FROM ipd_admissions")
    suspend fun getAllAdmissionsOnce(): List<IpdAdmission>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmissions(admissions: List<IpdAdmission>)
}
