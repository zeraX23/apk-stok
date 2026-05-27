package com.zerax23.stokbarcode.data.repository

import com.zerax23.stokbarcode.data.local.dao.BarcodeSerialDao
import com.zerax23.stokbarcode.data.local.dao.ProductDao
import com.zerax23.stokbarcode.data.local.dao.StockSummaryDao
import com.zerax23.stokbarcode.data.local.entity.BarcodeSerial
import com.zerax23.stokbarcode.domain.model.DailyReport
import com.zerax23.stokbarcode.domain.model.ProductSalesSummary
import com.zerax23.stokbarcode.domain.model.TransactionItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val barcodeSerialDao: BarcodeSerialDao,
    private val productDao: ProductDao,
    private val stockSummaryDao: StockSummaryDao
) {
    fun getDailyReport(date: LocalDate): Flow<DailyReport> {
        val tz = TimeZone.currentSystemDefault()
        val startOfDay = date.atStartOfDayIn(tz).toEpochMilliseconds()
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1

        return barcodeSerialDao.getSoldSerialsByDate(startOfDay, endOfDay)
            .map { soldSerials ->
                buildReport(date, soldSerials)
            }
    }

    private suspend fun buildReport(
        date: LocalDate,
        soldSerials: List<BarcodeSerial>
    ): DailyReport {
        val totalSold = soldSerials.size
        val totalRevenue = soldSerials.sumOf { it.soldPrice ?: 0.0 }

        // Group by product
        val groupedByProduct = soldSerials.groupBy { it.productId }
        val perProductSummary = mutableListOf<ProductSalesSummary>()

        groupedByProduct.forEach { (productId, serials) ->
            val product = productDao.getProductBySku("") // placeholder
            val remaining = stockSummaryDao.getSummaryByProductOnce(productId)
            // We'll use productId to get product name via dao
            perProductSummary.add(
                ProductSalesSummary(
                    productId = productId,
                    productName = "Produk #$productId",
                    category = "",
                    itemsSold = serials.size,
                    revenue = serials.sumOf { it.soldPrice ?: 0.0 },
                    remainingStock = remaining?.totalAvailable ?: 0
                )
            )
        }

        val bestSelling = perProductSummary
            .maxByOrNull { it.itemsSold }?.productName ?: "-"

        // Hourly sales
        val hourlySales = mutableMapOf<Int, Int>()
        soldSerials.forEach { serial ->
            serial.soldAt?.let { soldAt ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = soldAt
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                hourlySales[hour] = (hourlySales[hour] ?: 0) + 1
            }
        }

        // Transactions
        val transactions = soldSerials.map { serial ->
            TransactionItem(
                serialId = serial.id,
                serialNumber = serial.serialNumber,
                productName = "Produk #${serial.productId}",
                soldAt = serial.soldAt ?: 0L,
                buyerName = serial.buyerName,
                soldPrice = serial.soldPrice
            )
        }

        return DailyReport(
            date = date,
            totalItemsSold = totalSold,
            totalRevenue = totalRevenue,
            bestSellingProduct = bestSelling,
            perProductSummary = perProductSummary,
            transactions = transactions,
            hourlySales = hourlySales
        )
    }
}
