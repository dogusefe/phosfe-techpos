package com.phosfe.bkmtechpos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.phosfe.bkmtechpos.storage.DeliveryState

@Entity(
    tableName = "delivery_debt",
    indices = [
        Index(value = ["kind", "state", "availableAtEpochMs"]),
        Index("paymentId"),
        Index("correlationStan")
    ]
)
data class DeliveryDebtEntity(
    @PrimaryKey val id: String,
    val paymentId: String?,
    val kind: String,
    val state: String = DeliveryState.PENDING,
    val priority: Int,
    val requestEnvelope: ByteArray,
    val correlationStan: String?,
    val attempts: Int = 0,
    val requestSent: Boolean = false,
    val availableAtEpochMs: Long,
    val createdAtEpochMs: Long,
    val lastAttemptAtEpochMs: Long? = null,
    val deliveredAtEpochMs: Long? = null,
    val responseCode: String? = null,
    val hostReference: String? = null
)
