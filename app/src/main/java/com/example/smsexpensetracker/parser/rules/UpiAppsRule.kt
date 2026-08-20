package com.example.smsexpensetracker.parser.rules

import com.example.smsexpensetracker.data.model.PaymentMode
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.TransactionType
import com.example.smsexpensetracker.parser.BankSmsRule
import com.example.smsexpensetracker.parser.CategoryClassifier
import com.example.smsexpensetracker.parser.ParseResult
import com.example.smsexpensetracker.parser.ParserUtils

class UpiAppsRule : BankSmsRule {
    override val ruleId = "UPI_APPS_RULE"
    override val bankName = "UPI App"
    override val priority = 90

    private val debitPatterns = listOf(
        // PhonePe: "Paid Rs.199.00 to Swiggy on PhonePe. UPI Ref: 123456789012."
        Regex(
            """(?i)Paid\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*to\s*(?<merchant>[^.]+?)\s*on\s*PhonePe\.(?:\s*UPI\s*Ref:\s*(?<ref>[A-Za-z0-9]+))?""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // PhonePe 2: "You have sent Rs.250 to John Doe on PhonePe. Txn ID: T123456"
        Regex(
            """(?i)You\s*have\s*sent\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*to\s*(?<merchant>[^.]+?)\s*on\s*PhonePe\.(?:\s*(?:Txn\s*ID|UPI\s*Ref):\s*(?<ref>[A-Za-z0-9]+))?""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Google Pay: "Paid Rs.500.00 to Starbucks using Google Pay UPI ID merchant@okhdfcbank."
        Regex(
            """(?i)Paid\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*to\s*(?<merchant>[^.]+?)\s*using\s*Google\s*Pay(?:\s*UPI\s*ID\s*(?<ref>[^\s.]+))?""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Paytm: "Paid Rs.350 on Paytm to Dominos Pizza. Txn ID: 987654321"
        Regex(
            """(?i)Paid\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*on\s*Paytm\s*to\s*(?<merchant>[^.]+?)\.(?:\s*(?:Txn\s*ID|UPI\s*Ref):\s*(?<ref>[A-Za-z0-9]+))?""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Amazon Pay: "Transferred Rs.199.00 to John Doe via Amazon Pay. UPI Ref: 123456"
        Regex(
            """(?i)(?:Transferred|Paid)\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*to\s*(?<merchant>[^.]+?)\s*via\s*Amazon\s*Pay\.(?:\s*UPI\s*Ref:\s*(?<ref>[A-Za-z0-9]+))?""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // CRED: "Your payment of Rs 1,500.00 on CRED to HDFC Card XX1234 is successful. Ref: CRD123"
        Regex(
            """(?i)Your\s*payment\s*of\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*on\s*CRED\s*to\s*(?<merchant>[^.]+?)\s*is\s*successful\.(?:\s*Ref:\s*(?<ref>[A-Za-z0-9]+))?""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // BHIM: "Sent Rs.400.00 to John Doe via BHIM UPI Ref No 123456789012"
        Regex(
            """(?i)Sent\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*to\s*(?<merchant>[^.]+?)\s*via\s*BHIM\s*(?:UPI\s*Ref\s*(?:No\.?)?\s*(?<ref>[A-Za-z0-9]+))?""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val creditPatterns = listOf(
        // PhonePe Credit: "You have received Rs.500.00 from John Doe on PhonePe. UPI Ref: 987654"
        Regex(
            """(?i)You\s*have\s*received\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*from\s*(?<merchant>[^.]+?)\s*on\s*PhonePe\.(?:\s*UPI\s*Ref:\s*(?<ref>[A-Za-z0-9]+))?""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Paytm Credit: "Received Rs.1,000 on Paytm from John. Txn ID: 123456"
        Regex(
            """(?i)Received\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*on\s*Paytm\s*from\s*(?<merchant>[^.]+?)\.(?:\s*(?:Txn\s*ID|UPI\s*Ref):\s*(?<ref>[A-Za-z0-9]+))?""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    override fun canHandle(sender: String, body: String): Boolean {
        val s = sender.uppercase()
        val b = body.uppercase()
        return s.contains("PAYTM") || s.contains("PHONEPE") || s.contains("GPAY") ||
                s.contains("AMAZON") || s.contains("CRED") || s.contains("BHIM") ||
                b.contains("PHONEPE") || b.contains("GOOGLE PAY") || b.contains("PAYTM") ||
                b.contains("AMAZON PAY") || b.contains("CRED") || b.contains("BHIM")
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParseResult {
        val detectedApp = when {
            body.contains("PhonePe", ignoreCase = true) || sender.contains("PHONEPE", ignoreCase = true) -> "PhonePe"
            body.contains("Google Pay", ignoreCase = true) || body.contains("GPay", ignoreCase = true) || sender.contains("GPAY", ignoreCase = true) -> "Google Pay"
            body.contains("Paytm", ignoreCase = true) || sender.contains("PAYTM", ignoreCase = true) -> "Paytm"
            body.contains("Amazon Pay", ignoreCase = true) || sender.contains("AMZPAY", ignoreCase = true) -> "Amazon Pay"
            body.contains("CRED", ignoreCase = true) || sender.contains("CRED", ignoreCase = true) -> "CRED"
            body.contains("BHIM", ignoreCase = true) || sender.contains("BHIM", ignoreCase = true) -> "BHIM"
            else -> "UPI App"
        }

        // Debits
        for (pattern in debitPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amount = ParserUtils.parseAmount(ParserUtils.safeGroup(match, "amount")) ?: continue
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "Payee")
                val ref = ParserUtils.safeGroup(match, "ref")
                val category = CategoryClassifier.classify(merchant, body)
                val transaction = Transaction(
                    amount = amount,
                    transactionType = TransactionType.DEBIT,
                    bank = detectedApp,
                    paymentMode = PaymentMode.UPI,
                    paymentApp = detectedApp,
                    merchantOrPayee = merchant,
                    accountLast4 = null,
                    referenceId = ref,
                    timestamp = timestamp,
                    rawSmsBody = body,
                    confidence = 0.95f,
                    category = category,
                    sender = sender,
                    isManualReviewRequired = false
                )
                return ParseResult.Success(transaction)
            }
        }

        // Credits
        for (pattern in creditPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amount = ParserUtils.parseAmount(ParserUtils.safeGroup(match, "amount")) ?: continue
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "Payer")
                val ref = ParserUtils.safeGroup(match, "ref")
                val category = CategoryClassifier.classify(merchant, body)
                val transaction = Transaction(
                    amount = amount,
                    transactionType = TransactionType.CREDIT,
                    bank = detectedApp,
                    paymentMode = PaymentMode.UPI,
                    paymentApp = detectedApp,
                    merchantOrPayee = merchant,
                    accountLast4 = null,
                    referenceId = ref,
                    timestamp = timestamp,
                    rawSmsBody = body,
                    confidence = 0.95f,
                    category = category,
                    sender = sender,
                    isManualReviewRequired = false
                )
                return ParseResult.Success(transaction)
            }
        }

        return ParseResult.Failed("Could not parse UPI App SMS with defined templates")
    }
}
