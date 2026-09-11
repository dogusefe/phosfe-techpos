package com.phosfe.bkmtechpos.storage

object LedgerState {
    const val OPEN = "OPEN"
    const val APPROVED = "APPROVED"
    const val DECLINED = "DECLINED"
    const val VOIDED = "VOIDED"
    const val SETTLED = "SETTLED"
}

object DeliveryKind {
    const val REVERSAL = "REVERSAL"
    const val OFFLINE_ADVICE = "OFFLINE_ADVICE"
    const val TC_ADVICE = "TC_ADVICE"
    const val BATCH_UPLOAD = "BATCH_UPLOAD"
}

object DeliveryState {
    const val PENDING = "PENDING"
    const val IN_FLIGHT = "IN_FLIGHT"
    const val DELIVERED = "DELIVERED"
}
