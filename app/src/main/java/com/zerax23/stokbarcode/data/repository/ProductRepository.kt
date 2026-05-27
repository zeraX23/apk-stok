package com.zerax23.stokbarcode.data.repository

import com.zerax23.stokbarcode.data.local.dao.ProductDao
import com.zerax23.stokbarcode.data.local.entity.Product
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao
) {
    fun getAllProducts(): Flow<List<Product>> =
        productDao.getAllProducts()

    fun getProductById(id: Int): Flow<Product?> =
        productDao.getProductById(id)

    suspend fun getProductBySku(sku: String): Product? =
        productDao.getProductBySku(sku)

    suspend fun insertProduct(product: Product): Long =
        productDao.insertProduct(product)

    suspend fun updateProduct(product: Product) =
        productDao.updateProduct(product)

    suspend fun deleteProduct(product: Product) =
        productDao.deleteProduct(product)

    fun getAllProductsSortedByName(): Flow<List<Product>> =
        productDao.getAllProductsSortedByName()
}
