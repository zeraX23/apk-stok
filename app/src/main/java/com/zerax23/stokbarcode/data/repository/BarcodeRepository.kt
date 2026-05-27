package com.zerax23.stokbarcode.data.repository

import com.zerax23.stokbarcode.data.local.dao.BarcodeSerialDao
import com.zerax23.stokbarcode.data.local.dao.ProductDao
import com.zerax23.stokbarcode.data.local.dao.StockSummaryDao
import com.zerax23.stokbarcode.data.local.entity.BarcodeSerial
import com.zerax23.stokbarcode.data.local.entity.StockSummary
import com.zerax23.stokbarcode.domain.model.ScanResult
import com.zerax23.stokbarcode.util.SerialNumberGenerator
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BarcodeRepository @Inject constructor(
    private val barcodeSerialDao: BarcodeSerialDao,
    private val productDao: ProductDao,
    private val stockSummaryDao: StockSummaryDao,
    private val serialNumberGenerator: SerialNumberGenerator
) {
    fun getAllSerials(): Flow<List<BarcodeSerial>> =
        barcodeSerialDao.getAllSerials()

    fun getAllSerialsByProduct(productId: Int): Flow<List<BarcodeSerial>> =
        barcodeSerialDao.getAllSerialsByProduct(productId)

    fun getSerialsByStatus(status: String): Flow<List<BarcodeSerial>> =
        barcodeSerialDao.getSerialsByStatus(status)

    fun searchSerials(query: String): Flow<List<BarcodeSerial>> =
        barcodeSerialDao.searchSerials(query)

    fun getSoldSerialsByDate(
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<BarcodeSerial>> =
        barcodeSerialDao.getSoldSerialsByDate(startOfDay, endOfDay)

    suspend fun generateBatchSerials(
        productId: Int,
        quantity: Int,
        format: String
    ): List<BarcodeSerial> {
        val product = productDao.getProductBySku(
            productDao.getAllProductsSortedByName().let { "" }
        )
        val lastCounter = barcodeSerialDao.getLastCounterByProduct(productId)
        val prod = barcodeSerialDao.getSerialById(0)

        // Ambil product langsung dari DB
        var currentProduct = productDao.getProductById(productId)

        val serials = mutableListOf<BarcodeSerial>()
        var counter = lastCounter

        repeat(quantity) {
            counter++
            // Generate serial dengan retry jika duplikat
            var serialNumber: String
            var attempts = 0
            do {
                serialNumber = serialNumberGenerator.generateSerial(
                    category = "",
                    sku = "",
                    counter = counter + attempts
                )
                attempts++
            } while (
                barcodeSerialDao.getSerialByNumber(serialNumber) != null
                && attempts < 10
            )

            serials.add(
                BarcodeSerial(
                    productId = productId,
                    serialNumber = serialNumber,
                    barcodeFormat = format,
                    status = "AVAILABLE"
                )
            )
        }

        barcodeSerialDao.insertSerials(serials)
        updateStockSummary(productId)
        return serials
    }

    suspend fun generateBatchSerialsForProduct(
        productId: Int,
        productName: String,
        category: String,
        sku: String,
        quantity: Int,
        format: String
    ): List<BarcodeSerial> {
        val lastCounter = barcodeSerialDao.getLastCounterByProduct(productId)
        val serials = mutableListOf<BarcodeSerial>()
        var counter = lastCounter

        repeat(quantity) {
            counter++
            var serialNumber: String
            var attempts = 0
            do {
                serialNumber = serialNumberGenerator.generateSerial(
                    category = category,
                    sku = sku,
                    counter = counter + attempts
                )
                attempts++
            } while (
                barcodeSerialDao.getSerialByNumber(serialNumber) != null
                && attempts < 10
            )

            serials.add(
                BarcodeSerial(
                    productId = productId,
                    serialNumber = serialNumber,
                    barcodeFormat = format,
                    status = "AVAILABLE"
                )
            )
        }

        barcodeSerialDao.insertSerials(serials)
        updateStockSummary(productId)
        return serials
    }

    suspend fun markAsSold(
        serialNumber: String,
        buyerName: String? = null,
        soldPrice: Double? = null
    ): Boolean {
        val serial = barcodeSerialDao.getSerialByNumber(serialNumber)
            ?: return false

        if (serial.status != "AVAILABLE") return false

        val updated = serial.copy(
            status = "SOLD",
            soldAt = System.currentTimeMillis(),
            soldPrice = soldPrice,
            buyerName = buyerName,
            transactionId = "TRX-${System.currentTimeMillis()}"
        )
        barcodeSerialDao.updateSerial(updated)
        updateStockSummary(serial.productId)
        return true
    }

    suspend fun processReturn(serialId: Int): Boolean {
        val serial = barcodeSerialDao.getSerialById(serialId)
            ?: return false

        if (serial.status != "SOLD") return false

        val updated = serial.copy(
            status = "RETURNED",
            soldAt = null,
            soldPrice = null,
            buyerName = null,
            transactionId = null
        )
        barcodeSerialDao.updateSerial(updated)
        updateStockSummary(serial.productId)
        return true
    }

    suspend fun scanAndLookup(
        serialNumber: String,
        getProductById: suspend (Int) -> com.zerax23.stokbarcode.data.local.entity.Product?
    ): ScanResult {
        val serial = barcodeSerialDao.getSerialByNumber(serialNumber)
            ?: return ScanResult.NotFound(serialNumber)

        val product = getProductById(serial.productId)
            ?: return ScanResult.NotFound(serialNumber)

        return when (serial.status) {
            "AVAILABLE" -> {
                val remaining = barcodeSerialDao.countByProductAndStatus(
                    serial.productId, "AVAILABLE"
                )
                ScanResult.Available(product, serial, remaining)
            }
            "SOLD" -> ScanResult.AlreadySold(
                product, serial, serial.soldAt ?: 0L
            )
            "RETURNED" -> ScanResult.Returned(product, serial)
            else -> ScanResult.NotFound(serialNumber)
        }
    }

    private suspend fun updateStockSummary(productId: Int) {
        val available = barcodeSerialDao.countByProductAndStatus(
            productId, "AVAILABLE"
        )
        val sold = barcodeSerialDao.countByProductAndStatus(
            productId, "SOLD"
        )
        val existing = stockSummaryDao.getSummaryByProductOnce(productId)
        val total = existing?.totalGenerated ?: (available + sold)

        stockSummaryDao.upsertSummary(
            StockSummary(
                productId = productId,
                totalGenerated = total,
                totalAvailable = available,
                totalSold = sold,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }
}
