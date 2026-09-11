package com.phosfe.bkmtechpos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.phosfe.bkmtechpos.data.local.entity.BatchCycleEntity
import com.phosfe.bkmtechpos.storage.LedgerState

@Dao
abstract class BatchCycleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(batch: BatchCycleEntity): Long

    @Query("SELECT * FROM batch_cycle WHERE state = 'OPEN' ORDER BY number DESC LIMIT 1")
    abstract fun openBatch(): BatchCycleEntity?

    @Query("SELECT * FROM batch_cycle WHERE number = :number LIMIT 1")
    abstract fun byNumber(number: Int): BatchCycleEntity?

    @Query("UPDATE batch_cycle SET nextSequenceNumber = nextSequenceNumber + 1 WHERE number = :number AND state = 'OPEN'")
    abstract fun incrementSequence(number: Int): Int

    @Query("UPDATE batch_cycle SET state = 'SETTLED', closedAtEpochMs = :at, hostReference = :hostReference WHERE number = :number AND state = 'OPEN'")
    abstract fun close(number: Int, hostReference: String, at: Long): Int

    @Transaction
    open fun ensureOpen(number: Int, openedAt: Long) {
        require(number in 1..999_999)
        val current = openBatch()
        check(current == null || current.number == number) {
            "Batch ${current?.number} must be settled before opening $number"
        }
        insert(BatchCycleEntity(number = number, openedAtEpochMs = openedAt))
        check(byNumber(number)?.state == LedgerState.OPEN) { "Batch $number is not open" }
    }

    @Transaction
    open fun allocateSequence(number: Int): Int {
        val batch = openBatch() ?: error("No open batch")
        require(batch.number == number) { "Batch $number is not open" }
        check(incrementSequence(number) == 1) { "Could not advance batch sequence" }
        return batch.nextSequenceNumber
    }
}
