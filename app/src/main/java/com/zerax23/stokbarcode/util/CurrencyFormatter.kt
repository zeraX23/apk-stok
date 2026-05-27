package com.zerax23.stokbarcode.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {

    private val rupiahFormat = NumberFormat.getCurrencyInstance(
        Locale("id", "ID")
    )

    fun formatRupiah(amount: Double): String {
        return rupiahFormat.format(amount)
            .replace("Rp", "Rp ")
            .replace(",00", "")
    }

    fun formatRupiah(amount: Long): String {
        return formatRupiah(amount.toDouble())
    }

    fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            .format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))
            .format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .format(Date(timestamp))
    }
}
