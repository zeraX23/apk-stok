package com.zerax23.stokbarcode.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerax23.stokbarcode.data.local.entity.Product
import com.zerax23.stokbarcode.data.local.entity.StockSummary
import com.zerax23.stokbarcode.data.repository.BarcodeRepository
import com.zerax23.stokbarcode.data.repository.ProductRepository
import com.zerax23.stokbarcode.domain.model.TodaySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val barcodeRepository: BarcodeRepository
) : ViewModel() {

    // Daftar semua produk
    val products: StateFlow<List<Product>> = productRepository
        .getAllProducts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Summary stok per produk
    private val _stockSummaries = MutableStateFlow<Map<Int, StockSummary>>(emptyMap())
    val stockSummaries: StateFlow<Map<Int, StockSummary>> = _stockSummaries.asStateFlow()

    // Summary hari ini
    private val _todaySummary = MutableStateFlow(
        TodaySummary(
            date = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date,
            totalSold = 0,
            totalRevenue = 0.0
        )
    )
    val todaySummary: StateFlow<TodaySummary> = _todaySummary.asStateFlow()

    // Pesan error/sukses
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadTodaySummary()
    }

    private fun loadTodaySummary() {
        viewModelScope.launch {
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date

            // Hitung start dan end of day
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis

            val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1

            barcodeRepository.getSoldSerialsByDate(startOfDay, endOfDay)
                .collect { soldSerials ->
                    _todaySummary.value = TodaySummary(
                        date = today,
                        totalSold = soldSerials.size,
                        totalRevenue = soldSerials.sumOf { it.soldPrice ?: 0.0 }
                    )
                }
        }
    }

    fun loadStockSummary(productId: Int) {
        viewModelScope.launch {
            barcodeRepository.getAllSerialsByProduct(productId)
                .collect { serials ->
                    val available = serials.count { it.status == "AVAILABLE" }
                    val sold = serials.count { it.status == "SOLD" }
                    val summary = StockSummary(
                        productId = productId,
                        totalGenerated = serials.size,
                        totalAvailable = available,
                        totalSold = sold
                    )
                    _stockSummaries.value = _stockSummaries.value.toMutableMap().apply {
                        put(productId, summary)
                    }
                }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            try {
                productRepository.deleteProduct(product)
                _message.value = "Produk ${product.name} dihapus"
            } catch (e: Exception) {
                _message.value = "Gagal menghapus produk"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
