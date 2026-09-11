package com.phosfe.bkmtechpos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parameter_table")
data class ParameterRecordEntity(
    @PrimaryKey val type: Int,
    val action: Int,
    val reference: String,
    val version: Long,
    val payload: ByteArray,
    val activatedAtEpochMs: Long
)
