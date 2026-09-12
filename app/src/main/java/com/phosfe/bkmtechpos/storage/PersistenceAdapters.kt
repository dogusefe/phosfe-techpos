package com.phosfe.bkmtechpos.storage

import com.phosfe.bkmtechpos.parameters.ParameterActivationStore
import com.phosfe.bkmtechpos.parameters.ParameterTableBlock
import com.phosfe.bkmtechpos.protocol.IsoMessage
import com.phosfe.bkmtechpos.data.local.dao.DeliveryDebtDao
import com.phosfe.bkmtechpos.data.local.dao.ParameterRecordDao
import com.phosfe.bkmtechpos.data.local.dao.ParameterProjectionDao
import com.phosfe.bkmtechpos.data.local.entity.DeliveryDebtEntity
import com.phosfe.bkmtechpos.data.local.entity.ParameterRecordEntity
import com.phosfe.bkmtechpos.parameters.ParameterProjectionMapper
import com.phosfe.bkmtechpos.parameters.tables.ParameterTableDecoder
import com.phosfe.bkmtechpos.transaction.JournalCipher
import com.phosfe.bkmtechpos.transaction.PendingReversal
import com.phosfe.bkmtechpos.transaction.ReversalJournal
import java.time.Clock
import java.util.UUID

class RoomParameterActivationStore(
    private val dao: ParameterRecordDao,
    private val projectionDao: ParameterProjectionDao,
    private val clock: Clock = Clock.systemUTC()
) : ParameterActivationStore {
    override fun activateAtomically(tables: List<ParameterTableBlock>) {
        require(tables.map { it.type }.distinct().size == tables.size)
        val now = clock.millis()
        val deleted = tables.filter(ParameterTableBlock::deletesExistingTable).map(ParameterTableBlock::type)
        val records = tables.filterNot(ParameterTableBlock::deletesExistingTable).map {
            ParameterRecordEntity(it.type, it.action, it.reference, it.version, it.data.copyOf(), now)
        }
        deleted.forEach(projectionDao::delete)
        records.forEach { record ->
            val block = tables.first { it.type == record.type }
            projectionDao.upsert(ParameterProjectionMapper.map(block, ParameterTableDecoder.decode(block), now))
        }
        dao.activate(records, deleted)
    }
}

class RoomReversalJournal(
    private val dao: DeliveryDebtDao,
    private val cipher: JournalCipher,
    private val clock: Clock = Clock.systemUTC()
) : ReversalJournal {
    override fun pending(): PendingReversal? = dao.firstActive(DeliveryKind.REVERSAL)?.let {
        PendingReversal(it.id, cipher.open(it.requestEnvelope).let(IsoPayloadCodec::decode))
    }

    override fun save(entry: PendingReversal) {
        val now = clock.millis()
        dao.insertExclusive(
            DeliveryDebtEntity(
                id = entry.id,
                paymentId = null,
                kind = DeliveryKind.REVERSAL,
                priority = 0,
                requestEnvelope = cipher.seal(IsoPayloadCodec.encode(entry.request)),
                correlationStan = entry.request.text(11),
                availableAtEpochMs = now,
                createdAtEpochMs = now
            )
        )
    }

    override fun clear(expectedId: String) {
        val current = dao.firstActive(DeliveryKind.REVERSAL) ?: return
        require(current.id == expectedId) { "Refusing to clear a different reversal" }
        check(dao.markDelivered(expectedId, clock.millis(), "00", null) == 1)
    }
}

data class PendingDelivery(val id: String, val kind: String, val request: IsoMessage, val attempts: Int)

class DurableDeliveryQueue(
    private val dao: DeliveryDebtDao,
    private val cipher: JournalCipher,
    private val clock: Clock = Clock.systemUTC()
) {
    fun enqueue(kind: String, request: IsoMessage, paymentId: String? = null): String {
        require(kind in setOf(DeliveryKind.OFFLINE_ADVICE, DeliveryKind.TC_ADVICE, DeliveryKind.BATCH_UPLOAD))
        val now = clock.millis()
        val id = UUID.randomUUID().toString()
        dao.insert(
            DeliveryDebtEntity(
                id = id,
                paymentId = paymentId,
                kind = kind,
                priority = priority(kind),
                requestEnvelope = cipher.seal(IsoPayloadCodec.encode(request)),
                correlationStan = request.text(11),
                availableAtEpochMs = now,
                createdAtEpochMs = now
            )
        )
        return id
    }

    fun next(): PendingDelivery? {
        val entity = dao.nextReady(clock.millis()) ?: return null
        check(dao.markAttemptStarted(entity.id, clock.millis()) == 1)
        return PendingDelivery(
            entity.id,
            entity.kind,
            IsoPayloadCodec.decode(cipher.open(entity.requestEnvelope)),
            entity.attempts + 1
        )
    }

    fun acknowledge(id: String, responseCode: String, hostReference: String? = null) {
        check(dao.markDelivered(id, clock.millis(), responseCode, hostReference) == 1)
    }

    fun retry(id: String, delayMillis: Long, responseCode: String? = null) {
        require(delayMillis >= 0)
        check(dao.reschedule(id, clock.millis() + delayMillis, responseCode) == 1)
    }

    fun pendingCount(): Int = dao.activeCount()

    private fun priority(kind: String): Int = when (kind) {
        DeliveryKind.OFFLINE_ADVICE -> 10
        DeliveryKind.TC_ADVICE -> 20
        DeliveryKind.BATCH_UPLOAD -> 30
        else -> error("Unsupported delivery kind $kind")
    }
}
