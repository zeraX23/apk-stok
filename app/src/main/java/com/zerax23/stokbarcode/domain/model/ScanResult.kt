package com.zerax23.stokbarcode.domain.model

import com.zerax23.stokbarcode.data.local.entity.BarcodeSerial
import com.zerax23.stokbarcode.data.local.entity.Product

sealed class ScanResult {
    data class Available(
        val product: Product,
        val serial: BarcodeSerial,
        val remainingStock: Int
    ) : ScanResult()

    data class AlreadySold(
        val product: Product,
        val serial: BarcodeSerial,
        val soldAt: Long
    ) : ScanResult()

    data class Returned(
        val product: Product,
        val serial: BarcodeSerial
    ) : ScanResult()

    data class NotFound(
        val scannedCode: String
    ) : ScanResult()
}
