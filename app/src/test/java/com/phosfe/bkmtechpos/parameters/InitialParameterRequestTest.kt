package com.phosfe.bkmtechpos.parameters

import com.phosfe.bkmtechpos.protocol.BkmTlv
import com.phosfe.bkmtechpos.terminal.RotatingStan
import com.phosfe.bkmtechpos.terminal.TerminalClock
import com.phosfe.bkmtechpos.terminal.TerminalProfile
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Clock

class InitialParameterRequestTest {
    @Test fun rsaOnlyInstallationDoesNotAdvertiseTr34Tag() {
        val request = ParameterRequestFactory(
            TerminalProfile("SERIAL000001", "PHS", "POS", "0100", "3500", 12),
            RotatingStan(), TerminalClock(Clock.systemUTC())
        ).initial(ParameterRequestProfile(
            ParameterLoadReason.INITIAL_INSTALLATION, emptyList(), TerminalDeviceClass.POS,
            fiscalIdentity = FiscalIdentity("12345678901", null)
        ))
        assertFalse(BkmTlv.decode(request.fields.getValue(63)).any { it.id == 0x34 })
    }
}
