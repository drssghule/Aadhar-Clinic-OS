package com.example.aadharclinic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_transactions")
data class StockTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inventoryItemId: Long,
    val medicineName: String,
    val transactionType: String, // PURCHASE_ADD, OPD_DISPENSE, IPD_ADMINISTERED, MANUAL_ADJUST
    val quantityChange: Int, // +ve or -ve
    val remainingStock: Int,
    val referenceId: Long? = null, // consultationId or ipdId
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
