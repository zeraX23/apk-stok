package com.zerax23.stokbarcode.presentation.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerax23.stokbarcode.data.repository.ReportRepository
import com.zerax23.stokbarcode.domain.model.DailyReport
import com.zerax23.stokbarcode.util.CurrencyFormatter
import com.zerax23.stokbarcode.util.ImageSaver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class ReportUiState(
    val selectedDate: LocalDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val dailyReport: DailyReport? = null,
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val imageSaver: ImageSaver
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        loadReport(_uiState.value.selectedDate)
    }

    fun onDateChange(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            isLoading = true
        )
        loadReport(date)
    }

    private fun loadReport(date: LocalDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            reportRepository.getDailyReport(date).collect { report ->
                _uiState.value = _uiState.value.copy(
                    dailyReport = report,
                    isLoading = false
                )
            }
        }
    }

    fun getShareText(): String {
        val report = _uiState.value.dailyReport ?: return "Tidak ada data"
        val date = _uiState.value.selectedDate

        val sb = StringBuilder()
        sb.appendLine("📊 LAPORAN HARIAN — $date")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("Total Terjual : ${report.totalItemsSold} item")
        sb.appendLine(
            "Total Omzet   : ${
                CurrencyFormatter.formatRupiah(report.totalRevenue)
            }"
        )
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

        report.perProductSummary.forEach { summary ->
            sb.appendLine(
                "📦 ${summary.productName.padEnd(20)} : " +
                "${summary.itemsSold} pcs — " +
                CurrencyFormatter.formatRupiah(summary.revenue)
            )
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("Dibuat oleh StokBarcode App")
        return sb.toString()
    }

    fun shareReport(context: Context) {
        val text = getShareText()
        imageSaver.shareText(context, text)
    }

    fun previousDay() {
        val current = _uiState.value.selectedDate
        val previous = LocalDate(
            current.year,
            current.monthNumber,
            current.dayOfMonth - 1
        )
        onDateChange(previous)
    }

    fun nextDay() {
        val current = _uiState.value.selectedDate
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Tidak bisa pilih tanggal masa depan
        if (current >= today) return

        val next = LocalDate(
            current.year,
            current.monthNumber,
            current.dayOfMonth + 1
        )
        onDateChange(next)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
