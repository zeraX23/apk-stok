package com.zerax23.stokbarcode.presentation.generate

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerax23.stokbarcode.data.local.entity.BarcodeSerial
import com.zerax23.stokbarcode.data.local.entity.Product
import com.zerax23.stokbarcode.data.repository.BarcodeRepository
import com.zerax23.stokbarcode.data.repository.ProductRepository
import com.zerax23.stokbarcode.util.BarcodeGenerator
import com.zerax23.stokbarcode.util.CurrencyFormatter
import com.zerax23.stokbarcode.util.ImageSaver
import com.google.zxing.BarcodeFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GenerateUiState(
    val selectedProduct: Product? = null,
    val quantity: Int = 1,
    val barcodeFormat: String = "CODE_128",
    val generatedSerials: List<BarcodeSerial> = emptyList(),
    val generatedBitmaps: List<Bitmap> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class GenerateBarcodeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val barcodeRepository: BarcodeRepository,
    private val barcodeGenerator: BarcodeGenerator,
    private val imageSaver: ImageSaver
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerateUiState())
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()

    val products: StateFlow<List<Product>> = productRepository
        .getAllProductsSortedByName()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectProduct(product: Product) {
        _uiState.value = _uiState.value.copy(
            selectedProduct = product,
            generatedSerials = emptyList(),
            generatedBitmaps = emptyList()
        )
    }

    fun setQuantity(qty: Int) {
        _uiState.value = _uiState.value.copy(
            quantity = qty.coerceIn(1, 100)
        )
    }

    fun setBarcodeFormat(format: String) {
        _uiState.value = _uiState.value.copy(barcodeFormat = format)
    }

    fun generateBatch() {
        val product = _uiState.value.selectedProduct ?: return
        val quantity = _uiState.value.quantity
        val format = _uiState.value.barcodeFormat

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val serials = barcodeRepository.generateBatchSerialsForProduct(
                    productId = product.id,
                    productName = product.name,
                    category = product.category,
                    sku = product.sku,
                    quantity = quantity,
                    format = format
                )

                // Generate bitmap untuk setiap serial
                val bitmaps = serials.map { serial ->
                    val zxingFormat = if (format == "QR_CODE")
                        BarcodeFormat.QR_CODE
                    else
                        BarcodeFormat.CODE_128

                    barcodeGenerator.generateLabelBitmap(
                        productName = product.name,
                        serialNumber = serial.serialNumber,
                        price = product.price?.let {
                            CurrencyFormatter.formatRupiah(it)
                        },
                        barcodeContent = serial.serialNumber,
                        format = zxingFormat
                    ) ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    generatedSerials = serials,
                    generatedBitmaps = bitmaps,
                    message = "${serials.size} barcode berhasil dibuat"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Gagal generate barcode: ${e.message}"
                )
            }
        }
    }

    fun saveSingleAsPng(context: Context, index: Int) {
        val bitmaps = _uiState.value.generatedBitmaps
        val serials = _uiState.value.generatedSerials
        if (index >= bitmaps.size) return

        viewModelScope.launch {
            val uri = imageSaver.saveBitmapToGallery(
                context = context,
                bitmap = bitmaps[index],
                filename = "barcode_${serials[index].serialNumber}"
            )
            _uiState.value = _uiState.value.copy(
                message = if (uri != null)
                    "Barcode disimpan ke galeri"
                else
                    "Gagal menyimpan barcode"
            )
        }
    }

    fun saveAllAsPng(context: Context) {
        val bitmaps = _uiState.value.generatedBitmaps
        val serials = _uiState.value.generatedSerials

        viewModelScope.launch {
            var successCount = 0
            bitmaps.forEachIndexed { index, bitmap ->
                val uri = imageSaver.saveBitmapToGallery(
                    context = context,
                    bitmap = bitmap,
                    filename = "barcode_${serials[index].serialNumber}"
                )
                if (uri != null) successCount++
            }
            _uiState.value = _uiState.value.copy(
                message = "$successCount barcode disimpan ke galeri"
            )
        }
    }

    fun shareBarcode(context: Context, index: Int) {
        val bitmaps = _uiState.value.generatedBitmaps
        val serials = _uiState.value.generatedSerials
        if (index >= bitmaps.size) return

        viewModelScope.launch {
            val uri = imageSaver.saveBitmapToGallery(
                context = context,
                bitmap = bitmaps[index],
                filename = "barcode_${serials[index].serialNumber}"
            )
            uri?.let { imageSaver.shareImage(context, it) }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
