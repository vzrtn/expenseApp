package com.example.smsexpensetracker.data.model

enum class TransactionType {
    DEBIT,
    CREDIT
}

enum class PaymentMode(val displayName: String) {
    UPI("UPI"),
    DEBIT_CARD("Debit Card"),
    CREDIT_CARD("Credit Card"),
    NET_BANKING("NetBanking"),
    NEFT_IMPS("NEFT/IMPS"),
    ATM_WITHDRAWAL("ATM Withdrawal"),
    WALLET("Wallet"),
    OTHER("Other");

    companion object {
        fun fromString(str: String?): PaymentMode {
            if (str == null) return OTHER
            val s = str.uppercase()
            return when {
                s.contains("UPI") || s.contains("VPA") || s.contains("@") -> UPI
                s.contains("CREDIT CARD") || s.contains("CC ") || s.contains("SBI CARD") -> CREDIT_CARD
                s.contains("DEBIT CARD") || s.contains("DC ") -> DEBIT_CARD
                s.contains("ATM") || s.contains("CASH WDL") || s.contains("WITHDRAWAL") -> ATM_WITHDRAWAL
                s.contains("NEFT") || s.contains("IMPS") || s.contains("RTGS") -> NEFT_IMPS
                s.contains("NETBANKING") || s.contains("NET BANKING") || s.contains("INB") -> NET_BANKING
                s.contains("WALLET") || s.contains("PAYTM WALLET") -> WALLET
                s.contains("CARD") -> DEBIT_CARD
                else -> OTHER
            }
        }
    }
}
