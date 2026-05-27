package com.zerax23.stokbarcode.presentation.addproduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerax23.stokbarcode.data.local.entity.Product
import com.zerax23.stokbarcode.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditUiState(
    val id: Int = 0,
    val name: String = "",
    val category: String = "Lainnya",
    val sku: String = "",
    val price: String = "",
    val colorHex: String = "#1B5E20",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    val categories = listOf(
        "Elektronik",
        "Makanan",
        "Minuman",
        "Pakaian",
        "Perabot",
        "Kesehatan",
        "Kosmetik",
        "Lainnya"
    )

    val colorOptions = listOf(
        "#1B5E20", // Hijau tua
        "#1565C0", // Biru
        "#B71C1C", // Merah
        "#E65100", // Oranye
        "#4A148C", // Ungu
        "#006064", // Teal
        "#F57F17", // Kuning
        "#37474F"  // Abu
    )

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onCategoryChange(value: String) {
        _uiState.value = _uiState.value.copy(category = value)
    }

    fun onSkuChange(value: String) {
        _uiState.value = _uiState.value.copy(sku = value)
    }

    fun onPriceChange(value: String) {
        _uiState.value = _uiState.value.copy(price = value)
    }

    fun onColorChange(value: String) {
        _uiState.value = _uiState.value.copy(colorHex = value)
    }

    fun autoGenerateSku() {
        val name = _uiState.value.name
        val category = _uiState.value.category
        if (name.isBlank()) return

        val catCode = category.take(3).uppercase()
        val nameCode = name.replace(" ", "")
            .take(5)
            .uppercase()
        val timestamp = System.currentTimeMillis()
            .toString()
            .takeLast(4)

        _uiState.value = _uiState.value.copy(
            sku = "$catCode-$nameCode$timestamp"
        )
    }

    fun loadProduct(productId: Int) {
        viewModelScope.launch {
            productRepository.getProductById(productId)
                .collect { product ->
                    product?.let {
                        _uiState.value = AddEditUiState(
                            id = it.id,
                            name = it.name,
                            category = it.category,
                            sku = it.sku,
                            price = it.price?.toString() ?: "",
                            colorHex = it.colorHex
                        )
                    }
                }
        }
    }

    fun saveProduct() {
        val state = _uiState.value

        // Validasi
        if (state.name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Nama produk tidak boleh kosong")
            return
        }
        if (state.sku.isBlank()) {
            _uiState.value = state.copy(errorMessage = "SKU tidak boleh kosong")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true)
            try {
                val product = Product(
                    id = state.id,
                    name = state.name.trim(),
                    category = state.category,
                    sku = state.sku.trim().uppercase(),
                    price = state.price.toDoubleOrNull(),
                    colorHex = state.colorHex
                )

                if (state.id == 0) {
                    productRepository.insertProduct(product)
                } else {
                    productRepository.updateProduct(product)
                }

                _uiState.value = state.copy(
                    isLoading = false,
                    isSaved = true
                )
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isLoading = false,
                    errorMessage = "SKU sudah digunakan, gunakan SKU lain"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
