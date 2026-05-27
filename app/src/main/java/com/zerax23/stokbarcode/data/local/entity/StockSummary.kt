package com.zerax23.stokbarcode.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_summary")
data class StockSummary(
    @PrimaryKey
    val productId: Int,
    val totalGenerated: Int = 0,
    val totalAvailable: Int = 0,
    val totalSold: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
