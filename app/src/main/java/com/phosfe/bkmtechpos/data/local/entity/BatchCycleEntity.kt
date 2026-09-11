package com.phosfe.bkmtechpos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.phosfe.bkmtechpos.storage.LedgerState

@Entity(tableName = "batch_cycle", indices = [Index("state")])
data class BatchCycleEntity(
    @PrimaryKey val number: Int,
    val state: String = LedgerState.OPEN,
    val nextSequenceNumber: Int = 1,
    val openedAtEpochMs: Long,
    val closedAtEpochMs: Long? = null,
    val hostReference: String? = null
)
