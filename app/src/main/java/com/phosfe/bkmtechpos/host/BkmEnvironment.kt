package com.phosfe.bkmtechpos.host

import com.phosfe.bkmtechpos.BuildConfig

data class HostEndpoint(val host: String, val port: Int) {
    init {
        require(host.isNotBlank())
        require(port in 1..65_535)
    }
}

data class BkmEnvironment(
    val name: String,
    val vendorId: String,
    val producerCode: String,
    val deviceType: String,
    val serialHeader: String,
    val primary: HostEndpoint,
    val secondary: HostEndpoint,
    val rsaExponent: String,
    val rsaModulus: String,
    val immk: String,
    val configured: Boolean
) {
    fun requireProvisioned(): BkmEnvironment = apply {
        check(configured) {
            "BKM configuration is incomplete; provide Phosfe-assigned values in bkm-environment.properties"
        }
    }

    companion object {
        fun fromBuild(): BkmEnvironment = BkmEnvironment(
            name = BuildConfig.BKM_ENVIRONMENT,
            vendorId = BuildConfig.VENDOR_ID,
            producerCode = BuildConfig.PRODUCER_CODE,
            deviceType = BuildConfig.DEVICE_TYPE,
            serialHeader = BuildConfig.SERIAL_HEADER,
            primary = HostEndpoint(BuildConfig.HOST_PRIMARY, BuildConfig.PORT_PRIMARY),
            secondary = HostEndpoint(BuildConfig.HOST_SECONDARY, BuildConfig.PORT_SECONDARY),
            rsaExponent = BuildConfig.RSA_EXPONENT,
            rsaModulus = BuildConfig.HOST_RSA_MODULUS,
            immk = BuildConfig.IMMK,
            configured = BuildConfig.BKM_CONFIGURATION_COMPLETE
        )
    }
}
