package com.example.smsexpensetracker.parser

object OtpPromoDetector {

    private val otpKeywords = listOf(
        "otp", "one time password", "verification code", "is your verification",
        "secret code", "do not share", "valid for", "security code",
        "authentication code", "login otp", "authorization code"
    )

    private val promoKeywords = listOf(
        "flat % off", "flat 50% off", "cashback of upto", "apply now for",
        "pre-approved loan", "claim your reward", "win up to", "use code",
        "click to apply", "exclusive offer", "discount on your next",
        "limited period offer", "special offer for you", "pre approved personal loan",
        "congratulations! you are eligible", "instant loan", "get 0% interest"
    )

    private val nonTransactionalKeywords = listOf(
        "your e-statement for", "e-statement of your account",
        "kyc is pending", "link your aadhaar", "link aadhaar to",
        "missed call alert", "service request", "cheque book request",
        "minimum balance", "balance enquiry", "your monthly statement"
    )

    /**
     * Checks if the message is an OTP, promotional, or non-transactional SMS.
     * Returns a reason string if it should be discarded, or null if it's potentially transactional.
     */
    fun shouldDiscard(body: String): String? {
        val lower = body.lowercase()

        // 1. Check OTP keywords
        for (kw in otpKeywords) {
            if (lower.contains(kw)) {
                // If it contains "debited" or "spent" along with OTP, ensure it's not a debit alert that mentions OTP
                val hasFinancialDebit = lower.contains("debited") || lower.contains("spent on") || lower.contains("withdrawn")
                val hasAmount = lower.contains("rs.") || lower.contains("rs ") || lower.contains("inr")
                if (!hasFinancialDebit || !hasAmount) {
                    return "OTP message ($kw)"
                }
            }
        }

        // 2. Check Promotional keywords
        for (kw in promoKeywords) {
            if (lower.contains(kw)) {
                return "Promotional message ($kw)"
            }
        }

        // 3. Check non-transactional notices
        for (kw in nonTransactionalKeywords) {
            if (lower.contains(kw)) {
                return "Non-transactional notification ($kw)"
            }
        }

        return null
    }
}
