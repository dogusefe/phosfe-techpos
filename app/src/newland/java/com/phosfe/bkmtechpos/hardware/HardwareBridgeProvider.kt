package com.phosfe.bkmtechpos.hardware

import android.content.Context
import com.phosfe.bkmtechpos.domain.CardChannel
import com.phosfe.bkmtechpos.domain.MinorAmount

object HardwareBridgeProvider {
    fun create(context: Context): TerminalHardware = NewlandTerminalAdapter(context.applicationContext)
}

private class NewlandTerminalAdapter(private val context: Context) : TerminalHardware {
    override val vendorName = "Newland"
    override val capabilities = setOf(Capability.CHIP, Capability.CONTACTLESS, Capability.MAGNETIC, Capability.PIN, Capability.PRINTER, Capability.SECURE_KEYSTORE)
    override suspend fun initialize() = Result.failure<Unit>(UnsupportedOperationException("Newland SDK bağlayıcısı henüz sağlanmadı"))
    override suspend fun awaitCard(accepted: Set<CardChannel>) = Result.failure<CapturedCard>(UnsupportedOperationException("Newland kart okuyucu bağlayıcısı henüz sağlanmadı"))
    override suspend fun capturePin(amount: MinorAmount, pan: String) = Result.failure<PinCapture?>(UnsupportedOperationException("Newland PIN bağlayıcısı henüz sağlanmadı"))
    override suspend fun print(receipt: String) = Result.failure<Unit>(UnsupportedOperationException("Newland yazıcı bağlayıcısı henüz sağlanmadı"))
    override fun cancelPendingOperation() = Unit
}

