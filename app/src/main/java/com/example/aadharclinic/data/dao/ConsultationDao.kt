package com.example.aadharclinic.data.dao

import androidx.room.*
import com.example.aadharclinic.data.model.Consultation
import com.example.aadharclinic.data.model.PrescriptionItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsultationDao {
    @Query("SELECT * FROM consultations ORDER BY date DESC")
    fun getAllConsultations(): Flow<List<Consultation>>

    @Query("SELECT * FROM consultations WHERE id = :id LIMIT 1")
    fun getConsultationById(id: Long): Flow<Consultation?>

    @Query("SELECT * FROM consultations WHERE id = :id LIMIT 1")
    suspend fun getConsultationByIdOnce(id: Long): Consultation?

    @Query("SELECT * FROM consultations WHERE patientId = :patientId ORDER BY date DESC")
    fun getConsultationsForPatient(patientId: Long): Flow<List<Consultation>>

    @Query("SELECT * FROM consultations WHERE patientId = :patientId")
    suspend fun getConsultationsForPatientOnce(patientId: Long): List<Consultation>

    @Query("SELECT * FROM consultations WHERE date >= :startOfDay AND date < :endOfDay ORDER BY date DESC")
    fun getTodayConsultations(startOfDay: Long, endOfDay: Long): Flow<List<Consultation>>

    @Query("SELECT * FROM consultations WHERE date >= :sinceDate ORDER BY date ASC")
    fun getConsultationsSince(sinceDate: Long): Flow<List<Consultation>>

    @Query("SELECT COUNT(*) FROM consultations WHERE date >= :startOfDay AND date < :endOfDay")
    fun getTodayConsultationCount(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsultation(consultation: Consultation): Long

    @Update
    suspend fun updateConsultation(consultation: Consultation)

    @Delete
    suspend fun deleteConsultation(consultation: Consultation)

    // Prescription Items
    @Query("SELECT * FROM prescription_items WHERE consultationId = :consultationId")
    fun getPrescriptionItems(consultationId: Long): Flow<List<PrescriptionItem>>

    @Query("SELECT * FROM prescription_items WHERE consultationId = :consultationId")
    suspend fun getPrescriptionItemsOnce(consultationId: Long): List<PrescriptionItem>

    @Query("SELECT * FROM prescription_items")
    fun getAllPrescriptionItems(): Flow<List<PrescriptionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescriptionItems(items: List<PrescriptionItem>)

    @Query("DELETE FROM prescription_items WHERE consultationId = :consultationId")
    suspend fun deletePrescriptionItemsForConsultation(consultationId: Long)

    // Sync helpers
    @Query("SELECT * FROM consultations WHERE syncStatus = :status")
    suspend fun getConsultationsBySyncStatus(status: String = "PENDING"): List<Consultation>

    @Query("SELECT * FROM consultations WHERE recordId = :recordId LIMIT 1")
    suspend fun getConsultationByRecordId(recordId: String): Consultation?

    @Query("UPDATE consultations SET syncStatus = :status, lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateConsultationSyncStatus(id: Long, status: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM consultations WHERE syncStatus = 'PENDING'")
    suspend fun getPendingConsultationCount(): Int

    @Query("SELECT COUNT(*) FROM consultations WHERE syncStatus = 'SYNCED'")
    suspend fun getSyncedConsultationCount(): Int

    @Query("SELECT * FROM consultations")
    suspend fun getAllConsultationsOnce(): List<Consultation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsultations(consultations: List<Consultation>)
}
