package com.example.smsexpensetracker.parser.rules

import com.example.smsexpensetracker.data.model.PaymentMode
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.TransactionType
import com.example.smsexpensetracker.parser.BankSmsRule
import com.example.smsexpensetracker.parser.CategoryClassifier
import com.example.smsexpensetracker.parser.ParseResult
import com.example.smsexpensetracker.parser.ParserUtils

class YesBankRule : BankSmsRule {
    override val ruleId = "YES_BANK_RULE"
    override val bankName = "YES Bank"
    override val priority = 100

    private val debitPatterns = listOf(
        // "INR 450.00 debited from YES Bank A/c ending 1234 on 20-Aug-26 towards UBER. Avl Bal: INR 12,000"
        Regex(
            """(?i)(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*debited\s*from\s*YES\s*Bank\s*A\/c\s*(?:ending\s*)?(?<account>\d{4}|XX\d{4})\s*on\s*(?<date>[^\s]+)\s*(?:towards|to)\s*(?<merchant>[^.]+?)(?:\.\s*Avl|$)""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val creditPatterns = listOf(
        // "INR 1,000.00 credited to YES Bank A/c ending 1234 on 20-Aug-26 by transfer from John"
        Regex(
            """(?i)(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*credited\s*to\s*YES\s*Bank\s*A\/c\s*(?:ending\s*)?(?<account>\d{4}|XX\d{4})\s*on\s*(?<date>[^\s]+)\s*(?:by|from)\s*(?<merchant>[^.]+?)(?:\.|\s*Bal|$)""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    override fun canHandle(sender: String, body: String): Boolean {
        val s = sender.uppercase()
        val b = body.uppercase()
        return s.contains("YESBNK") || s.contains("YESBK") || b.contains("YES BANK")
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParseResult {
        for (pattern in debitPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amount = ParserUtils.parseAmount(ParserUtils.safeGroup(match, "amount")) ?: continue
                val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "YES Bank Payee")
                val category = CategoryClassifier.classify(merchant, body)
                val transaction = Transaction(
                    amount = amount,
                    transactionType = TransactionType.DEBIT,
                    bank = bankName,
                    paymentMode = PaymentMode.UPI,
                    merchantOrPayee = merchant,
                    accountLast4 = account,
                    referenceId = null,
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

        for (pattern in creditPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amount = ParserUtils.parseAmount(ParserUtils.safeGroup(match, "amount")) ?: continue
                val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "YES Bank Payer")
                val category = CategoryClassifier.classify(merchant, body)
                val transaction = Transaction(
                    amount = amount,
                    transactionType = TransactionType.CREDIT,
                    bank = bankName,
                    paymentMode = PaymentMode.UPI,
                    merchantOrPayee = merchant,
                    accountLast4 = account,
                    referenceId = null,
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

        return ParseResult.Failed("Could not parse YES Bank SMS with defined templates")
    }
}
