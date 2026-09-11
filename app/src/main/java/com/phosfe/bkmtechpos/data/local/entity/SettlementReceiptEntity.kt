package com.phosfe.bkmtechpos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "settlement_receipt", indices = [Index("batchNumber")])
data class SettlementReceiptEntity(
    @PrimaryKey val id: String,
    val batchNumber: Int,
    val approved: Boolean,
    val hostReference: String?,
    val responseCode: String,
    val totalsEnvelope: ByteArray,
    val completedAtEpochMs: Long
)
