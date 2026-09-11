package com.phosfe.bkmtechpos.hardware

import android.content.Context
import com.phosfe.bkmtechpos.domain.CardChannel
import com.phosfe.bkmtechpos.domain.MinorAmount

data class CapturedCard(
    val channel: CardChannel,
    val maskedPan: String,
    val emvData: ByteArray = byteArrayOf()
)

data class PinCapture(val encryptedPinBlock: ByteArray, val keySerialNumber: ByteArray)

interface TerminalHardware {
    val vendorName: String
    val capabilities: Set<Capability>

    suspend fun initialize(): Result<Unit>
    suspend fun awaitCard(accepted: Set<CardChannel>): Result<CapturedCard>
    suspend fun capturePin(amount: MinorAmount, pan: String): Result<PinCapture?>
    suspend fun print(receipt: String): Result<Unit>
    fun cancelPendingOperation()
}

enum class Capability { CHIP, CONTACTLESS, MAGNETIC, PIN, PRINTER, SECURE_KEYSTORE }

/** Implemented once per vendor product flavor. */
object HardwareBridge {
    fun create(context: Context): TerminalHardware = HardwareBridgeProvider.create(context)
}

