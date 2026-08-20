package com.example.smsexpensetracker.theme

import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF2563EB)
val PrimaryBlueDark = Color(0xFF1D4ED8)
val AccentTeal = Color(0xFF0D9488)

val DebitRed = Color(0xFFDC2626)
val DebitRedContainer = Color(0xFFFEE2E2)
val CreditGreen = Color(0xFF16A34A)
val CreditGreenContainer = Color(0xFFDCFCE7)

// Category Colors
val ColorFood = Color(0xFFF97316)       // Orange
val ColorGroceries = Color(0xFF10B981)  // Emerald
val ColorShopping = Color(0xFF8B5CF6)   // Purple
val ColorBills = Color(0xFF06B6D4)      // Cyan
val ColorTransport = Color(0xFF3B82F6)  // Blue
val ColorEntertainment = Color(0xFFEC4899) // Pink
val ColorTransfers = Color(0xFF6366F1)  // Indigo
val ColorHealth = Color(0xFFEF4444)     // Red
val ColorInvestments = Color(0xFF14B8A6) // Teal
val ColorOthers = Color(0xFF64748B)     // Slate

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Food & Dining" -> ColorFood
        "Groceries" -> ColorGroceries
        "Shopping" -> ColorShopping
        "Bills & Utilities" -> ColorBills
        "Transport" -> ColorTransport
        "Entertainment" -> ColorEntertainment
        "Transfers" -> ColorTransfers
        "Health & Medical" -> ColorHealth
        "Investments" -> ColorInvestments
        else -> ColorOthers
    }
}
