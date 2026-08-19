package com.example.aadharclinic.data.dao

import androidx.room.*
import com.example.aadharclinic.data.model.BillInvoice
import kotlinx.coroutines.flow.Flow

@Dao
interface BillingDao {
    @Query("SELECT * FROM bill_invoices ORDER BY date DESC")
    fun getAllInvoices(): Flow<List<BillInvoice>>

    @Query("SELECT * FROM bill_invoices WHERE id = :id LIMIT 1")
    fun getInvoiceById(id: Long): Flow<BillInvoice?>

    @Query("SELECT * FROM bill_invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceByIdOnce(id: Long): BillInvoice?

    @Query("SELECT * FROM bill_invoices WHERE patientId = :patientId ORDER BY date DESC")
    fun getInvoicesForPatient(patientId: Long): Flow<List<BillInvoice>>

    @Query("SELECT * FROM bill_invoices WHERE patientId = :patientId")
    suspend fun getInvoicesForPatientOnce(patientId: Long): List<BillInvoice>

    @Query("SELECT * FROM bill_invoices WHERE date >= :startOfDay AND date < :endOfDay ORDER BY date DESC")
    fun getTodayInvoices(startOfDay: Long, endOfDay: Long): Flow<List<BillInvoice>>

    @Query("SELECT SUM(paidAmount) FROM bill_invoices WHERE date >= :startOfDay AND date < :endOfDay")
    fun getTodayRevenue(startOfDay: Long, endOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(paidAmount) FROM bill_invoices WHERE date >= :startOfMonth")
    fun getMonthlyRevenue(startOfMonth: Long): Flow<Double?>

    @Query("SELECT * FROM bill_invoices WHERE date >= :sinceDate ORDER BY date ASC")
    fun getInvoicesSince(sinceDate: Long): Flow<List<BillInvoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: BillInvoice): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<BillInvoice>)

    @Update
    suspend fun updateInvoice(invoice: BillInvoice)

    @Delete
    suspend fun deleteInvoice(invoice: BillInvoice)

    // Sync helpers
    @Query("SELECT * FROM bill_invoices WHERE syncStatus = :status")
    suspend fun getInvoicesBySyncStatus(status: String = "PENDING"): List<BillInvoice>

    @Query("SELECT * FROM bill_invoices WHERE recordId = :recordId LIMIT 1")
    suspend fun getInvoiceByRecordId(recordId: String): BillInvoice?

    @Query("UPDATE bill_invoices SET syncStatus = :status, lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateInvoiceSyncStatus(id: Long, status: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM bill_invoices WHERE syncStatus = 'PENDING'")
    suspend fun getPendingInvoiceCount(): Int

    @Query("SELECT COUNT(*) FROM bill_invoices WHERE syncStatus = 'SYNCED'")
    suspend fun getSyncedInvoiceCount(): Int

    @Query("SELECT * FROM bill_invoices")
    suspend fun getAllInvoicesOnce(): List<BillInvoice>
}
