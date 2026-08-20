package com.example.smsexpensetracker.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    val indianLocale: Locale by lazy {
        Locale.Builder().setLanguage("en").setRegion("IN").build()
    }

    val inrCurrencyFormat: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(indianLocale)
    }

    val inrCurrencyFormatNoDecimals: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(indianLocale).apply {
            maximumFractionDigits = 0
        }
    }

    fun formatInr(amount: Double, includeDecimals: Boolean = true): String {
        return if (includeDecimals) {
            inrCurrencyFormat.format(amount)
        } else {
            inrCurrencyFormatNoDecimals.format(amount)
        }
    }

    fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatFullDateTime(timestamp: Long): String {
        return SimpleDateFormat("EEEE, dd MMMM yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}
