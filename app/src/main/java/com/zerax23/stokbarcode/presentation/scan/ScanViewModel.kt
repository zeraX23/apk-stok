package com.zerax23.stokbarcode.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerax23.stokbarcode.data.repository.BarcodeRepository
import com.zerax23.stokbarcode.data.repository.ProductRepository
import com.zerax23.stokbarcode.domain.model.ScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanUiState(
    val scanResult: ScanResult? = null,
    val isProcessing: Boolean = false,
    val isScanning: Boolean = true,
    val buyerName: String = "",
    val soldPrice: String = "",
    val message: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val barcodeRepository: BarcodeRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    // Mencegah scan ganda dalam waktu singkat
    private var lastScannedCode = ""
    private var lastScanTime = 0L

    fun onBarcodeDetected(rawValue: String) {
        val now = System.currentTimeMillis()

        // Debounce: abaikan scan yang sama dalam 2 detik
        if (rawValue == lastScannedCode && now - lastScanTime < 2000) return
        if (_uiState.value.isProcessing) return
        if (!_uiState.value.isScanning) return

        lastScannedCode = rawValue
        lastScanTime = now

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                isScanning = false
            )

            val result = barcodeRepository.scanAndLookup(
                serialNumber = rawValue,
                getProductById = { productId ->
                    var product: com.zerax23.stokbarcode.data.local.entity.Product? = null
                    productRepository.getProductById(productId).collect {
                        product = it
                        return@collect
                    }
                    product
                }
            )

            // Set default harga jual jika produk ditemukan
            val defaultPrice = when (result) {
                is ScanResult.Available ->
                    result.product.price?.toString() ?: ""
                else -> ""
            }

            _uiState.value = _uiState.value.copy(
                scanResult = result,
                isProcessing = false,
                soldPrice = defaultPrice
            )
        }
    }

    fun onBuyerNameChange(value: String) {
        _uiState.value = _uiState.value.copy(buyerName = value)
    }

    fun onSoldPriceChange(value: String) {
        _uiState.value = _uiState.value.copy(soldPrice = value)
    }

    fun confirmSold() {
        val result = _uiState.value.scanResult
        if (result !is ScanResult.Available) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)

            val success = barcodeRepository.markAsSold(
                serialNumber = result.serial.serialNumber,
                buyerName = _uiState.value.buyerName
                    .trim()
                    .ifBlank { null },
                soldPrice = _uiState.value.soldPrice
                    .toDoubleOrNull()
                    ?: result.product.price
            )

            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                isSuccess = success,
                message = if (success)
                    "✅ Berhasil ditandai terjual!"
                else
                    "❌ Gagal memperbarui status"
            )
        }
    }

    fun processReturn(serialId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)

            val success = barcodeRepository.processReturn(serialId)

            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                message = if (success)
                    "✅ Retur berhasil diproses"
                else
                    "❌ Gagal memproses retur"
            )
        }
    }

    fun resetScan() {
        lastScannedCode = ""
        lastScanTime = 0L
        _uiState.value = ScanUiState()
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(
            message = null,
            isSuccess = false
        )
    }
}
