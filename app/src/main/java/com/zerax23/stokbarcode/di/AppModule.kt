package com.zerax23.stokbarcode.di

import android.content.Context
import androidx.room.Room
import com.zerax23.stokbarcode.data.local.AppDatabase
import com.zerax23.stokbarcode.data.local.dao.BarcodeSerialDao
import com.zerax23.stokbarcode.data.local.dao.ProductDao
import com.zerax23.stokbarcode.data.local.dao.StockSummaryDao
import com.zerax23.stokbarcode.data.repository.BarcodeRepository
import com.zerax23.stokbarcode.data.repository.ProductRepository
import com.zerax23.stokbarcode.data.repository.ReportRepository
import com.zerax23.stokbarcode.util.BarcodeGenerator
import com.zerax23.stokbarcode.util.ImageSaver
import com.zerax23.stokbarcode.util.SerialNumberGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "stokbarcode.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideProductDao(db: AppDatabase): ProductDao =
        db.productDao()

    @Provides
    @Singleton
    fun provideBarcodeSerialDao(db: AppDatabase): BarcodeSerialDao =
        db.barcodeSerialDao()

    @Provides
    @Singleton
    fun provideStockSummaryDao(db: AppDatabase): StockSummaryDao =
        db.stockSummaryDao()

    @Provides
    @Singleton
    fun provideSerialNumberGenerator(): SerialNumberGenerator =
        SerialNumberGenerator()

    @Provides
    @Singleton
    fun provideBarcodeGenerator(): BarcodeGenerator =
        BarcodeGenerator()

    @Provides
    @Singleton
    fun provideImageSaver(): ImageSaver =
        ImageSaver()

    @Provides
    @Singleton
    fun provideProductRepository(
        productDao: ProductDao
    ): ProductRepository = ProductRepository(productDao)

    @Provides
    @Singleton
    fun provideBarcodeRepository(
        barcodeSerialDao: BarcodeSerialDao,
        productDao: ProductDao,
        stockSummaryDao: StockSummaryDao,
        serialNumberGenerator: SerialNumberGenerator
    ): BarcodeRepository = BarcodeRepository(
        barcodeSerialDao,
        productDao,
        stockSummaryDao,
        serialNumberGenerator
    )

    @Provides
    @Singleton
    fun provideReportRepository(
        barcodeSerialDao: BarcodeSerialDao,
        productDao: ProductDao,
        stockSummaryDao: StockSummaryDao
    ): ReportRepository = ReportRepository(
        barcodeSerialDao,
        productDao,
        stockSummaryDao
    )
}
