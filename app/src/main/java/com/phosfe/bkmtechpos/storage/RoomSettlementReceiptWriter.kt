package com.phosfe.bkmtechpos.storage

import com.phosfe.bkmtechpos.data.local.dao.SettlementReceiptDao
import com.phosfe.bkmtechpos.data.local.entity.SettlementReceiptEntity
import com.phosfe.bkmtechpos.settlement.BatchSettlement
import com.phosfe.bkmtechpos.settlement.SettlementReceiptWriter
import com.phosfe.bkmtechpos.settlement.SettlementTotalsCodec
import java.time.Clock
import java.util.UUID

class RoomSettlementReceiptWriter(private val dao: SettlementReceiptDao, private val clock: Clock = Clock.systemUTC()) : SettlementReceiptWriter {
    override fun record(totals: BatchSettlement, hostReference: String, uploadedCount: Int) {
        dao.insert(SettlementReceiptEntity(UUID.randomUUID().toString(), totals.batchNumber, true, hostReference, "00", SettlementTotalsCodec.encode(totals), clock.millis()))
    }
}
