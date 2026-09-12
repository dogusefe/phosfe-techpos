package com.phosfe.bkmtechpos.data.local.entity

import androidx.room.Entity

@Entity(tableName = "parameter_projection", primaryKeys = ["type"])
data class ParameterProjectionEntity(
    val type: Int,
    val reference: String,
    val version: Long,
    val kind: String,
    val summary: String,
    val activatedAtEpochMs: Long
)
