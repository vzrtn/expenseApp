package com.example.smsexpensetracker.parser

import java.util.regex.Matcher
import java.util.regex.Pattern

object ParserUtils {

    fun parseAmount(amountStr: String?): Double? {
        if (amountStr.isNullOrBlank()) return null
        return try {
            val cleaned = amountStr.replace(",", "").replace(" ", "").trim()
            cleaned.toDoubleOrNull()
        } catch (_: Exception) {
            null
        }
    }

    fun cleanAccountLast4(accountStr: String?): String? {
        if (accountStr.isNullOrBlank()) return null
        // Extract 4 digits from strings like "XX1234", "**1234", "1234", "ending 1234"
        val digits = accountStr.filter { it.isDigit() }
        return if (digits.length >= 4) {
            digits.takeLast(4)
        } else if (digits.isNotEmpty()) {
            digits
        } else {
            null
        }
    }

    fun cleanMerchantName(merchant: String?): String {
        if (merchant.isNullOrBlank()) return "Unknown Payee"
        var cleaned = merchant.trim()

        // Remove trailing periods, commas, or semicolons
        cleaned = cleaned.trimEnd('.', ',', ';', ':', '-', '/')

        // Remove common suffixes like "Avl Bal", "Avail Bal", "Bal:", "Ref:", "UPI Ref", etc.
        val balIndex = cleaned.indexOf("Avail", ignoreCase = true)
        if (balIndex > 0) {
            cleaned = cleaned.substring(0, balIndex).trim()
        }
        val balIndex2 = cleaned.indexOf("Avl Bal", ignoreCase = true)
        if (balIndex2 > 0) {
            cleaned = cleaned.substring(0, balIndex2).trim()
        }
        val balIndex3 = cleaned.indexOf("Total Bal", ignoreCase = true)
        if (balIndex3 > 0) {
            cleaned = cleaned.substring(0, balIndex3).trim()
        }

        // Clean prefixes like "to ", "at ", "towards "
        if (cleaned.startsWith("to ", ignoreCase = true)) {
            cleaned = cleaned.substring(3).trim()
        } else if (cleaned.startsWith("at ", ignoreCase = true)) {
            cleaned = cleaned.substring(3).trim()
        } else if (cleaned.startsWith("towards ", ignoreCase = true)) {
            cleaned = cleaned.substring(8).trim()
        }

        return cleaned.ifBlank { "Unknown Payee" }
    }

    fun isTransactionalSender(sender: String): Boolean {
        val s = sender.trim().uppercase()
        // Standard Indian Shortcode: 2 letters, hyphen, 6 characters (e.g. AD-HDFCBK, VM-ICICIB, JD-SBIINB, AX-PAYTM)
        val shortcodeRegex = Regex("^[A-Z]{2}-[A-Z0-9]{6}$")
        if (shortcodeRegex.matches(s)) return true

        // Known bank prefixes or keywords
        val bankKeywords = listOf(
            "HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "PNB", "BOB", "IDFC", "YESBNK",
            "YESBK", "PAYTM", "GPAY", "PHONEPE", "AMAZON", "CANARA", "UNIONB", "INDUS",
            "FEDBNK", "RBL", "SCBANK", "HSBC", "CITI", "CRED", "BHIM", "AIRTEL", "JIO"
        )
        return bankKeywords.any { s.contains(it) }
    }

    fun safeGroup(match: MatchResult, groupName: String): String? {
        return try {
            match.groups[groupName]?.value
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

