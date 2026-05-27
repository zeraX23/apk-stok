package com.zerax23.stokbarcode.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zerax23.stokbarcode.data.local.dao.BarcodeSerialDao
import com.zerax23.stokbarcode.data.local.dao.ProductDao
import com.zerax23.stokbarcode.data.local.dao.StockSummaryDao
import com.zerax23.stokbarcode.data.local.entity.BarcodeSerial
import com.zerax23.stokbarcode.data.local.entity.Product
import com.zerax23.stokbarcode.data.local.entity.StockSummary

@Database(
    entities = [
        Product::class,
        BarcodeSerial::class,
        StockSummary::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun barcodeSerialDao(): BarcodeSerialDao
    abstract fun stockSummaryDao(): StockSummaryDao
}
