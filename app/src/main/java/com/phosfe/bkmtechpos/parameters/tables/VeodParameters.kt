package com.phosfe.bkmtechpos.parameters.tables

data class VeodParameters(
    val maximumTransactions: Int,
    val automaticEodEnabled: Boolean,
    val automaticEodTime: String,
    val retryLimit: Int,
    val retryPeriodMinutes: Int,
    val minimumWaitAfterManualMinutes: Int
)
