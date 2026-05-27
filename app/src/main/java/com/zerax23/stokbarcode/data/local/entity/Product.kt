package com.zerax23.stokbarcode.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["sku"], unique = true)]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val category: String,
    val sku: String,
    val price: Double? = null,
    val colorHex: String = "#1B5E20",
    val createdAt: Long = System.currentTimeMillis()
)
