package com.example.aadharclinic.data.dao

import androidx.room.*
import com.example.aadharclinic.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date >= :startDate AND date < :endDate ORDER BY date DESC")
    fun getExpensesBetween(startDate: Long, endDate: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE date >= :startDate AND date < :endDate")
    fun getTotalExpenseBetween(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalExpense(): Flow<Double?>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    fun getExpensesByCategory(category: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // Sync helpers
    @Query("SELECT * FROM expenses WHERE syncStatus = :status")
    suspend fun getExpensesBySyncStatus(status: String = "PENDING"): List<Expense>

    @Query("SELECT * FROM expenses WHERE recordId = :recordId LIMIT 1")
    suspend fun getExpenseByRecordId(recordId: String): Expense?

    @Query("UPDATE expenses SET syncStatus = :status, lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateExpenseSyncStatus(id: Long, status: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM expenses WHERE syncStatus = 'PENDING'")
    suspend fun getPendingExpenseCount(): Int

    @Query("SELECT COUNT(*) FROM expenses WHERE syncStatus = 'SYNCED'")
    suspend fun getSyncedExpenseCount(): Int

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesOnce(): List<Expense>
}
