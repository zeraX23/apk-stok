package com.zerax23.stokbarcode.domain.model

import kotlinx.datetime.LocalDate

data class DailyReport(
    val date: LocalDate,
    val totalItemsSold: Int,
    val totalRevenue: Double,
    val bestSellingProduct: String,
    val perProductSummary: List<ProductSalesSummary>,
    val transactions: List<TransactionItem>,
    val hourlySales: Map<Int, Int>
)

data class ProductSalesSummary(
    val productId: Int,
    val productName: String,
    val category: String,
    val itemsSold: Int,
    val revenue: Double,
    val remainingStock: Int
)

data class TransactionItem(
    val serialId: Int,
    val serialNumber: String,
    val productName: String,
    val soldAt: Long,
    val buyerName: String?,
    val soldPrice: Double?
)

data class TodaySummary(
    val date: LocalDate,
    val totalSold: Int,
    val totalRevenue: Double
)
