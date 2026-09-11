package com.phosfe.bkmtechpos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phosfe.bkmtechpos.data.local.entity.SettlementReceiptEntity

@Dao
interface SettlementReceiptDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(receipt: SettlementReceiptEntity)

    @Query("SELECT * FROM settlement_receipt WHERE batchNumber = :batchNumber ORDER BY completedAtEpochMs DESC LIMIT 1")
    fun latestForBatch(batchNumber: Int): SettlementReceiptEntity?
}
