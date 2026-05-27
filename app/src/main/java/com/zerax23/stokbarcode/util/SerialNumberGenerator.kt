package com.zerax23.stokbarcode.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SerialNumberGenerator @Inject constructor() {

    /**
     * Generate serial number format:
     * {CAT_CODE}-{SKU}-{YYYYMMDD}-{XXXX}
     * Contoh: ELC-TV001-20260527-0001
     */
    fun generateSerial(
        category: String,
        sku: String,
        counter: Int
    ): String {
        val catCode = category
            .take(3)
            .uppercase()
            .padEnd(3, 'X')

        val skuCode = sku
            .uppercase()
            .replace(" ", "")
            .take(10)

        val dateCode = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            .format(Date())

        val counterCode = counter
            .toString()
            .padStart(4, '0')

        return "$catCode-$skuCode-$dateCode-$counterCode"
    }

    /**
     * Generate batch serial numbers
     * Counter tidak reset, terus berlanjut dari lastCounter
     */
    fun generateBatch(
        category: String,
        sku: String,
        quantity: Int,
        lastCounter: Int
    ): List<String> {
        return (1..quantity).map { i ->
            generateSerial(
                category = category,
                sku = sku,
                counter = lastCounter + i
            )
        }
    }

    /**
     * Extract counter dari serial number yang sudah ada
     * Format: CAT-SKU-DATE-COUNTER
     */
    fun extractCounter(serialNumber: String): Int {
        return try {
            serialNumber.split("-").last().toInt()
        } catch (e: Exception) {
            0
        }
    }
}
