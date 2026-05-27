package com.zerax23.stokbarcode.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerax23.stokbarcode.data.local.entity.BarcodeSerial
import com.zerax23.stokbarcode.data.repository.BarcodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StatusFilter(val label: String, val value: String) {
    ALL("Semua", "ALL"),
    AVAILABLE("Tersedia", "AVAILABLE"),
    SOLD("Terjual", "SOLD"),
    RETURNED("Diretur", "RETURNED")
}

data class HistoryUiState(
    val serials: List<BarcodeSerial> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val isLoading: Boolean = false,
    val message: String? = null,
    val showReturnDialog: Boolean = false,
    val selectedSerial: BarcodeSerial? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val barcodeRepository: BarcodeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    private val _message = MutableStateFlow<String?>(null)
    private val _showReturnDialog = MutableStateFlow(false)
    private val _selectedSerial = MutableStateFlow<BarcodeSerial?>(null)

    // Gabungkan semua state
    val uiState: StateFlow<HistoryUiState> = combine(
        _searchQuery,
        _statusFilter,
        _message,
        _showReturnDialog,
        _selectedSerial
    ) { query, filter, message, showDialog, selectedSerial ->
        HistoryUiState(
            searchQuery = query,
            statusFilter = filter,
            message = message,
            showReturnDialog = showDialog,
            selectedSerial = selectedSerial
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    // Serials berdasarkan filter dan search
    val serials: StateFlow<List<BarcodeSerial>> = combine(
        _searchQuery,
        _statusFilter
    ) { query, filter ->
        Pair(query, filter)
    }.flatMapLatest { (query, filter) ->
        when {
            query.isNotBlank() ->
                barcodeRepository.searchSerials(query)
            filter == StatusFilter.ALL ->
                barcodeRepository.getAllSerials()
            else ->
                barcodeRepository.getSerialsByStatus(filter.value)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    fun showReturnDialog(serial: BarcodeSerial) {
        _selectedSerial.value = serial
        _showReturnDialog.value = true
    }

    fun dismissReturnDialog() {
        _showReturnDialog.value = false
        _selectedSerial.value = null
    }

    fun processReturn() {
        val serial = _selectedSerial.value ?: return

        viewModelScope.launch {
            val success = barcodeRepository.processReturn(serial.id)
            _showReturnDialog.value = false
            _selectedSerial.value = null
            _message.value = if (success)
                "✅ Retur berhasil diproses"
            else
                "❌ Gagal memproses retur"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
