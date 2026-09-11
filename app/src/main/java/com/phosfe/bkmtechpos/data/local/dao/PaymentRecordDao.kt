package com.phosfe.bkmtechpos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phosfe.bkmtechpos.data.local.entity.PaymentRecordEntity

@Dao
interface PaymentRecordDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(record: PaymentRecordEntity)

    @Query("SELECT * FROM payment_record WHERE batchNumber = :batchNumber ORDER BY sequenceNumber ASC")
    fun inBatch(batchNumber: Int): List<PaymentRecordEntity>

    @Query("SELECT * FROM payment_record WHERE batchNumber = :batchNumber AND state = 'APPROVED' AND batchUploaded = 0 ORDER BY sequenceNumber ASC")
    fun approvedForUpload(batchNumber: Int): List<PaymentRecordEntity>

    @Query("SELECT * FROM payment_record WHERE referenceNumber = :reference ORDER BY createdAtEpochMs DESC LIMIT 1")
    fun byReference(reference: String): PaymentRecordEntity?

    @Query("UPDATE payment_record SET state = 'VOIDED', updatedAtEpochMs = :at WHERE id = :id")
    fun markVoided(id: String, at: Long): Int

    @Query("UPDATE payment_record SET batchUploaded = 1, updatedAtEpochMs = :at WHERE id = :id")
    fun markBatchUploaded(id: String, at: Long): Int

    @Query("UPDATE payment_record SET state = 'SETTLED', updatedAtEpochMs = :at WHERE batchNumber = :batchNumber AND state IN ('APPROVED', 'VOIDED')")
    fun markBatchSettled(batchNumber: Int, at: Long): Int
}
