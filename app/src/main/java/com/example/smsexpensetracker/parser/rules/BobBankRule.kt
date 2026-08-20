package com.example.smsexpensetracker.parser.rules

import com.example.smsexpensetracker.data.model.PaymentMode
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.TransactionType
import com.example.smsexpensetracker.parser.BankSmsRule
import com.example.smsexpensetracker.parser.CategoryClassifier
import com.example.smsexpensetracker.parser.ParseResult
import com.example.smsexpensetracker.parser.ParserUtils

class BobBankRule : BankSmsRule {
    override val ruleId = "BOB_BANK_RULE"
    override val bankName = "Bank of Baroda"
    override val priority = 100

    private val debitPatterns = listOf(
        // "Your A/C 1234 has been debited by INR 350.00 on 20-Aug-26 towards ZOMATO. Total Bal: INR 8500"
        Regex(
            """(?i)Your\s*A\/C\s*(?<account>\d{4}|XX\d{4})\s*has\s*been\s*debited\s*by\s*(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*on\s*(?<date>[^\s]+)\s*(?:towards|to)\s*(?<merchant>[^.]+?)(?:\.\s*Total\s*Bal|$)""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val creditPatterns = listOf(
        // "Your A/C 1234 has been credited with INR 2,000.00 on 20-Aug-26 by transfer from John"
        Regex(
            """(?i)Your\s*A\/C\s*(?<account>\d{4}|XX\d{4})\s*has\s*been\s*credited\s*(?:with|by)\s*(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*on\s*(?<date>[^\s]+)\s*(?:by|from)\s*(?<merchant>[^.]+?)(?:\.|\s*Bal|$)""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    override fun canHandle(sender: String, body: String): Boolean {
        val s = sender.uppercase()
        val b = body.uppercase()
        return s.contains("BOB") || s.contains("BARODA") || b.contains("BANK OF BARODA") || b.contains("BOB WORLD")
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParseResult {
        for (pattern in debitPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amount = ParserUtils.parseAmount(ParserUtils.safeGroup(match, "amount")) ?: continue
                val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "BOB Payee")
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
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "BOB Payer")
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

        return ParseResult.Failed("Could not parse BOB SMS with defined templates")
    }
}
