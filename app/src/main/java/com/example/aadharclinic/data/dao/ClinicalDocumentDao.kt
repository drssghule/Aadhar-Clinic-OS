package com.example.aadharclinic.data.dao

import androidx.room.*
import com.example.aadharclinic.data.model.ClinicalDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface ClinicalDocumentDao {
    @Query("SELECT * FROM clinical_documents ORDER BY date DESC, id DESC")
    fun getAllDocuments(): Flow<List<ClinicalDocument>>

    @Query("SELECT * FROM clinical_documents WHERE documentType = :type ORDER BY date DESC, id DESC")
    fun getDocumentsByType(type: String): Flow<List<ClinicalDocument>>

    @Query("SELECT * FROM clinical_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): ClinicalDocument?

    @Query("SELECT * FROM clinical_documents WHERE patientId = :patientId ORDER BY date DESC")
    fun getDocumentsForPatient(patientId: Long): Flow<List<ClinicalDocument>>

    @Query("SELECT * FROM clinical_documents WHERE patientId = :patientId")
    suspend fun getDocumentsForPatientOnce(patientId: Long): List<ClinicalDocument>

    @Query("SELECT COUNT(*) FROM clinical_documents")
    suspend fun getDocumentCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: ClinicalDocument): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<ClinicalDocument>)

    @Update
    suspend fun updateDocument(document: ClinicalDocument)

    @Delete
    suspend fun deleteDocument(document: ClinicalDocument)

    // Sync helpers
    @Query("SELECT * FROM clinical_documents WHERE syncStatus = :status")
    suspend fun getDocumentsBySyncStatus(status: String = "PENDING"): List<ClinicalDocument>

    @Query("SELECT * FROM clinical_documents WHERE recordId = :recordId LIMIT 1")
    suspend fun getDocumentByRecordId(recordId: String): ClinicalDocument?

    @Query("UPDATE clinical_documents SET syncStatus = :status, lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateDocumentSyncStatus(id: Long, status: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM clinical_documents WHERE syncStatus = 'PENDING'")
    suspend fun getPendingDocumentCount(): Int

    @Query("SELECT COUNT(*) FROM clinical_documents WHERE syncStatus = 'SYNCED'")
    suspend fun getSyncedDocumentCount(): Int

    @Query("SELECT * FROM clinical_documents")
    suspend fun getAllDocumentsOnce(): List<ClinicalDocument>
}
