package com.zerax23.stokbarcode.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zerax23.stokbarcode.data.local.entity.BarcodeSerial
import kotlinx.coroutines.flow.Flow

@Dao
interface BarcodeSerialDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSerial(serial: BarcodeSerial): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSerials(serials: List<BarcodeSerial>)

    @Update
    suspend fun updateSerial(serial: BarcodeSerial)

    @Query("SELECT * FROM barcode_serials WHERE serialNumber = :serialNumber LIMIT 1")
    suspend fun getSerialByNumber(serialNumber: String): BarcodeSerial?

    @Query("SELECT * FROM barcode_serials WHERE productId = :productId ORDER BY generatedAt DESC")
    fun getAllSerialsByProduct(productId: Int): Flow<List<BarcodeSerial>>

    @Query("SELECT * FROM barcode_serials WHERE status = :status ORDER BY generatedAt DESC")
    fun getSerialsByStatus(status: String): Flow<List<BarcodeSerial>>

    @Query("SELECT * FROM barcode_serials ORDER BY generatedAt DESC")
    fun getAllSerials(): Flow<List<BarcodeSerial>>

    @Query("""
        SELECT * FROM barcode_serials 
        WHERE serialNumber LIKE '%' || :query || '%'
        ORDER BY generatedAt DESC
    """)
    fun searchSerials(query: String): Flow<List<BarcodeSerial>>

    @Query("""
        SELECT * FROM barcode_serials
        WHERE status = 'SOLD'
        AND soldAt BETWEEN :startOfDay AND :endOfDay
        ORDER BY soldAt DESC
    """)
    fun getSoldSerialsByDate(startOfDay: Long, endOfDay: Long): Flow<List<BarcodeSerial>>

    @Query("""
        SELECT COUNT(*) FROM barcode_serials
        WHERE productId = :productId
    """)
    suspend fun getLastCounterByProduct(productId: Int): Int

    @Query("""
        SELECT COUNT(*) FROM barcode_serials
        WHERE productId = :productId AND status = :status
    """)
    suspend fun countByProductAndStatus(productId: Int, status: String): Int

    @Query("SELECT * FROM barcode_serials WHERE id = :id LIMIT 1")
    suspend fun getSerialById(id: Int): BarcodeSerial?
}
