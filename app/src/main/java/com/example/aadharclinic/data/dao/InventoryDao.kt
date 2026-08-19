package com.example.aadharclinic.data.dao

import androidx.room.*
import com.example.aadharclinic.data.model.InventoryItem
import com.example.aadharclinic.data.model.StockTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun getAllInventory(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    fun getInventoryItemById(id: Long): Flow<InventoryItem?>

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    suspend fun getInventoryItemByIdOnce(id: Long): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE currentStock <= minThreshold ORDER BY currentStock ASC")
    fun getLowStockItems(): Flow<List<InventoryItem>>

    @Query("SELECT COUNT(*) FROM inventory_items WHERE currentStock <= minThreshold")
    fun getLowStockCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM inventory_items")
    suspend fun getInventoryCountOnce(): Int

    @Query("""
        SELECT * FROM inventory_items 
        WHERE name LIKE '%' || :query || '%' 
           OR genericName LIKE '%' || :query || '%' 
           OR batchNumber LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchInventory(query: String): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem): Long

    @Update
    suspend fun updateInventoryItem(item: InventoryItem)

    @Delete
    suspend fun deleteInventoryItem(item: InventoryItem)

    @Query("UPDATE inventory_items SET currentStock = currentStock - :quantity, lastUpdated = :timestamp WHERE id = :id")
    suspend fun reduceStock(id: Long, quantity: Int, timestamp: Long)

    @Query("UPDATE inventory_items SET currentStock = currentStock + :quantity, lastUpdated = :timestamp WHERE id = :id")
    suspend fun addStock(id: Long, quantity: Int, timestamp: Long)

    // Stock Transactions
    @Query("SELECT * FROM stock_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<StockTransaction>>

    @Query("SELECT * FROM stock_transactions WHERE inventoryItemId = :itemId ORDER BY timestamp DESC")
    fun getTransactionsForItem(itemId: Long): Flow<List<StockTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: StockTransaction): Long

    // Sync helpers
    @Query("SELECT * FROM inventory_items WHERE syncStatus = :status")
    suspend fun getInventoryBySyncStatus(status: String = "PENDING"): List<InventoryItem>

    @Query("SELECT * FROM inventory_items WHERE recordId = :recordId LIMIT 1")
    suspend fun getInventoryByRecordId(recordId: String): InventoryItem?

    @Query("UPDATE inventory_items SET syncStatus = :status, lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateInventorySyncStatus(id: Long, status: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM inventory_items WHERE syncStatus = 'PENDING'")
    suspend fun getPendingInventoryCount(): Int

    @Query("SELECT COUNT(*) FROM inventory_items WHERE syncStatus = 'SYNCED'")
    suspend fun getSyncedInventoryCount(): Int

    @Query("SELECT * FROM inventory_items")
    suspend fun getAllInventoryOnce(): List<InventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItems(items: List<InventoryItem>)
}
