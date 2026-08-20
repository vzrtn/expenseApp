package com.example.smsexpensetracker.parser.rules

import com.example.smsexpensetracker.data.model.PaymentMode
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.TransactionType
import com.example.smsexpensetracker.parser.BankSmsRule
import com.example.smsexpensetracker.parser.CategoryClassifier
import com.example.smsexpensetracker.parser.ParseResult
import com.example.smsexpensetracker.parser.ParserUtils

class PnbBankRule : BankSmsRule {
    override val ruleId = "PNB_BANK_RULE"
    override val bankName = "Punjab National Bank"
    override val priority = 100

    private val debitPatterns = listOf(
        // "A/C ****1234 Debited by Rs.650.00 on 20-Aug-26 via UPI to SWIGGY. Ref:123456. Bal:Rs.10000"
        Regex(
            """(?i)A\/C\s*(?<account>\*{0,4}\d{4}|XX\d{4})\s*Debited\s*by\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*on\s*(?<date>[^\s]+)\s*(?:via\s*UPI\s*to|to|towards)\s*(?<merchant>[^.]+?)\.\s*(?:Ref:\s*(?<ref>[A-Za-z0-9]+))?""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val creditPatterns = listOf(
        // "A/C ****1234 Credited by Rs.1,500.00 on 20-Aug-26 via UPI by John. Ref:987654. Bal:Rs.11500"
        Regex(
            """(?i)A\/C\s*(?<account>\*{0,4}\d{4}|XX\d{4})\s*Credited\s*by\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*on\s*(?<date>[^\s]+)\s*(?:via\s*UPI\s*by|by|from)\s*(?<merchant>[^.]+?)\.\s*(?:Ref:\s*(?<ref>[A-Za-z0-9]+))?""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    override fun canHandle(sender: String, body: String): Boolean {
        val s = sender.uppercase()
        val b = body.uppercase()
        return s.contains("PNB") || s.contains("PUNBNK") || b.contains("PNB") || b.contains("PUNJAB NATIONAL")
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParseResult {
        for (pattern in debitPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amount = ParserUtils.parseAmount(ParserUtils.safeGroup(match, "amount")) ?: continue
                val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "PNB Payee")
                val ref = ParserUtils.safeGroup(match, "ref")
                val category = CategoryClassifier.classify(merchant, body)
                val transaction = Transaction(
                    amount = amount,
                    transactionType = TransactionType.DEBIT,
                    bank = bankName,
                    paymentMode = PaymentMode.UPI,
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

        for (pattern in creditPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amount = ParserUtils.parseAmount(ParserUtils.safeGroup(match, "amount")) ?: continue
                val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "PNB Payer")
                val ref = ParserUtils.safeGroup(match, "ref")
                val category = CategoryClassifier.classify(merchant, body)
                val transaction = Transaction(
                    amount = amount,
                    transactionType = TransactionType.CREDIT,
                    bank = bankName,
                    paymentMode = PaymentMode.UPI,
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

        return ParseResult.Failed("Could not parse PNB SMS with defined templates")
    }
}
