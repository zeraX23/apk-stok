package com.zerax23.stokbarcode.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "barcode_serials",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["serialNumber"], unique = true),
        Index(value = ["productId"])
    ]
)
data class BarcodeSerial(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: Int,
    val serialNumber: String,
    val barcodeFormat: String = "CODE_128",
    val status: String = "AVAILABLE",
    val soldAt: Long? = null,
    val soldPrice: Double? = null,
    val buyerName: String? = null,
    val transactionId: String? = null,
    val generatedAt: Long = System.currentTimeMillis()
)
