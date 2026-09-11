package com.phosfe.bkmtechpos.hardware

import android.content.Context
import com.phosfe.bkmtechpos.domain.CardChannel
import com.phosfe.bkmtechpos.domain.MinorAmount

object HardwareBridgeProvider {
    fun create(context: Context): TerminalHardware = DatecsTerminalAdapter(context.applicationContext)
}

private class DatecsTerminalAdapter(private val context: Context) : TerminalHardware {
    override val vendorName = "Datecs"
    override val capabilities = Capability.entries.toSet()
    override suspend fun initialize() = Result.failure<Unit>(UnsupportedOperationException("Datecs SDK bağlayıcısı henüz sağlanmadı"))
    override suspend fun awaitCard(accepted: Set<CardChannel>) = Result.failure<CapturedCard>(UnsupportedOperationException("Datecs kart okuyucu bağlayıcısı henüz sağlanmadı"))
    override suspend fun capturePin(amount: MinorAmount, pan: String) = Result.failure<PinCapture?>(UnsupportedOperationException("Datecs PIN bağlayıcısı henüz sağlanmadı"))
    override suspend fun print(receipt: String) = Result.failure<Unit>(UnsupportedOperationException("Datecs yazıcı bağlayıcısı henüz sağlanmadı"))
    override fun cancelPendingOperation() = Unit
}

