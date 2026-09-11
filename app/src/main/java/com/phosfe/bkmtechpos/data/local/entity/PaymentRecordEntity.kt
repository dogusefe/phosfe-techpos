package com.phosfe.bkmtechpos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_record",
    indices = [
        Index(value = ["batchNumber", "sequenceNumber"], unique = true),
        Index("batchNumber"),
        Index("stan"),
        Index("referenceNumber"),
        Index("createdAtEpochMs")
    ]
)
data class PaymentRecordEntity(
    @PrimaryKey val id: String,
    val batchNumber: Int,
    val sequenceNumber: Int,
    val operation: String,
    val amountMinor: Long,
    val currencyNumeric: String,
    val stan: String,
    val referenceNumber: String?,
    val authorizationCode: String?,
    val responseCode: String?,
    val state: String,
    val maskedPan: String?,
    val requestEnvelope: ByteArray,
    val responseEnvelope: ByteArray?,
    val panEnvelope: ByteArray?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val batchUploaded: Boolean = false
)
