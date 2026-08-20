package com.example.smsexpensetracker.parser.rules

import com.example.smsexpensetracker.data.model.PaymentMode
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.TransactionType
import com.example.smsexpensetracker.parser.BankSmsRule
import com.example.smsexpensetracker.parser.CategoryClassifier
import com.example.smsexpensetracker.parser.ParseResult
import com.example.smsexpensetracker.parser.ParserUtils

class KotakBankRule : BankSmsRule {
    override val ruleId = "KOTAK_BANK_RULE"
    override val bankName = "Kotak Bank"
    override val priority = 100

    private val debitPatterns = listOf(
        // Pattern 1: "Sent Rs.499.00 from Kotak Bank AC XX1234 to Uber on 20-Aug-26. Ref 1234567890."
        Regex(
            """(?i)Sent\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*from\s*Kotak\s*Bank\s*(?:AC|A\/c|Account)\s*(?<account>XX\d{4}|\d{4})\s*to\s*(?<merchant>[^\s]+)\s*on\s*(?<date>[^\s.]+?)\.\s*Ref\s*(?<ref>[A-Za-z0-9]+)""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Pattern 2: "Rs.1,200.00 spent on Kotak Credit Card XX9999 at STARBUCKS on 20-Aug-26. Bal limit: 45,000"
        Regex(
            """(?i)(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*spent\s*on\s*Kotak\s*(?:Credit\s*Card|Debit\s*Card|Card)\s*(?<account>XX\d{4}|\d{4})\s*at\s*(?<merchant>[^.]+?)\s*on\s*(?<date>[^\s.]+?)\.""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val creditPatterns = listOf(
        // Pattern 1: "Rs.3,000.00 credited to Kotak Bank AC XX1234 on 20-Aug-26 by transfer from John. Ref 12345"
        Regex(
            """(?i)(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*credited\s*to\s*Kotak\s*Bank\s*(?:AC|A\/c)\s*(?<account>XX\d{4}|\d{4})\s*on\s*(?<date>[^\s]+)\s*(?:by\s*transfer\s*from|by|from)\s*(?<merchant>[^.]+?)(?:\.\s*Ref\s*(?<ref>[A-Za-z0-9]+))?(?:\.|\s*Bal|$)""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    override fun canHandle(sender: String, body: String): Boolean {
        val s = sender.uppercase()
        val b = body.uppercase()
        return s.contains("KOTAK") || b.contains("KOTAK BANK") || b.contains("KOTAK")
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParseResult {
        // Debits
        for (pattern in debitPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amount = ParserUtils.parseAmount(ParserUtils.safeGroup(match, "amount")) ?: continue
                val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "Kotak Payee")
                val ref = ParserUtils.safeGroup(match, "ref")
                val mode = when {
                    body.contains("Credit Card", ignoreCase = true) -> PaymentMode.CREDIT_CARD
                    body.contains("Card", ignoreCase = true) -> PaymentMode.DEBIT_CARD
                    body.contains("UPI", ignoreCase = true) || merchant.contains("@") -> PaymentMode.UPI
                    else -> PaymentMode.UPI
                }
                val category = CategoryClassifier.classify(merchant, body)
                val transaction = Transaction(
                    amount = amount,
                    transactionType = TransactionType.DEBIT,
                    bank = bankName,
                    paymentMode = mode,
                    merchantOrPayee = merchant,
                    accountLast4 = account,
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
                val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "Kotak Payer")
                val ref = ParserUtils.safeGroup(match, "ref")
                val mode = if (body.contains("UPI", ignoreCase = true)) PaymentMode.UPI else PaymentMode.NEFT_IMPS
                val category = CategoryClassifier.classify(merchant, body)
                val transaction = Transaction(
                    amount = amount,
                    transactionType = TransactionType.CREDIT,
                    bank = bankName,
                    paymentMode = mode,
                    merchantOrPayee = merchant,
                    accountLast4 = account,
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

        return ParseResult.Failed("Could not parse Kotak Bank SMS with defined templates")
    }
}
