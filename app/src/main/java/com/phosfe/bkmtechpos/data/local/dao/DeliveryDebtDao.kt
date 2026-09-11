package com.phosfe.bkmtechpos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.phosfe.bkmtechpos.data.local.entity.DeliveryDebtEntity

@Dao
abstract class DeliveryDebtDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract fun insert(entry: DeliveryDebtEntity)

    @Query("SELECT COUNT(*) FROM delivery_debt WHERE kind = :kind AND state IN ('PENDING', 'IN_FLIGHT')")
    abstract fun activeCount(kind: String): Int

    @Query("SELECT * FROM delivery_debt WHERE kind = :kind AND state IN ('PENDING', 'IN_FLIGHT') ORDER BY priority ASC, createdAtEpochMs ASC LIMIT 1")
    abstract fun firstActive(kind: String): DeliveryDebtEntity?

    @Query("SELECT * FROM delivery_debt WHERE state IN ('PENDING', 'IN_FLIGHT') AND availableAtEpochMs <= :now ORDER BY priority ASC, createdAtEpochMs ASC LIMIT 1")
    abstract fun nextReady(now: Long): DeliveryDebtEntity?

    @Query("SELECT COUNT(*) FROM delivery_debt WHERE state IN ('PENDING', 'IN_FLIGHT')")
    abstract fun activeCount(): Int

    @Query("UPDATE delivery_debt SET state = 'IN_FLIGHT', attempts = attempts + 1, requestSent = 1, lastAttemptAtEpochMs = :at WHERE id = :id AND state IN ('PENDING', 'IN_FLIGHT')")
    abstract fun markAttemptStarted(id: String, at: Long): Int

    @Query("UPDATE delivery_debt SET state = 'PENDING', availableAtEpochMs = :retryAt, responseCode = :responseCode WHERE id = :id AND state = 'IN_FLIGHT'")
    abstract fun reschedule(id: String, retryAt: Long, responseCode: String?): Int

    @Query("UPDATE delivery_debt SET state = 'DELIVERED', deliveredAtEpochMs = :at, responseCode = :responseCode, hostReference = :hostReference WHERE id = :id AND state IN ('PENDING', 'IN_FLIGHT')")
    abstract fun markDelivered(id: String, at: Long, responseCode: String?, hostReference: String?): Int

    @Transaction
    open fun insertExclusive(entry: DeliveryDebtEntity) {
        check(activeCount(entry.kind) == 0) { "An active ${entry.kind} debt already exists" }
        insert(entry)
    }
}
