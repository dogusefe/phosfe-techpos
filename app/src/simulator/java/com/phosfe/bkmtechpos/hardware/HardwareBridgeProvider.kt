package com.phosfe.bkmtechpos.hardware

import android.content.Context
import com.phosfe.bkmtechpos.domain.CardChannel
import com.phosfe.bkmtechpos.domain.MinorAmount

object HardwareBridgeProvider {
    fun create(context: Context): TerminalHardware = SimulatorTerminal()
}

private class SimulatorTerminal : TerminalHardware {
    override val vendorName = "Simulator"
    override val capabilities = Capability.entries.toSet()
    override suspend fun initialize() = Result.success(Unit)
    override suspend fun awaitCard(accepted: Set<CardChannel>) = Result.success(
        CapturedCard(accepted.firstOrNull() ?: CardChannel.CONTACTLESS, "5400 00** **** 0001")
    )
    override suspend fun capturePin(amount: MinorAmount, pan: String) = Result.success(null)
    override suspend fun print(receipt: String) = Result.success(Unit)
    override fun cancelPendingOperation() = Unit
}

