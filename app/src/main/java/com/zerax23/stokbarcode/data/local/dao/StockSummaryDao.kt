package com.zerax23.stokbarcode.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zerax23.stokbarcode.data.local.entity.StockSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface StockSummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: StockSummary)

    @Query("SELECT * FROM stock_summary WHERE productId = :productId")
    fun getSummaryByProduct(productId: Int): Flow<StockSummary?>

    @Query("SELECT * FROM stock_summary")
    fun getAllSummaries(): Flow<List<StockSummary>>

    @Query("SELECT * FROM stock_summary WHERE productId = :productId")
    suspend fun getSummaryByProductOnce(productId: Int): StockSummary?
}
