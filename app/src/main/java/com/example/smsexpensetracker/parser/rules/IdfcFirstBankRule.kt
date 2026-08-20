package com.example.smsexpensetracker.parser.rules

import com.example.smsexpensetracker.data.model.PaymentMode
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.TransactionType
import com.example.smsexpensetracker.parser.BankSmsRule
import com.example.smsexpensetracker.parser.CategoryClassifier
import com.example.smsexpensetracker.parser.ParseResult
import com.example.smsexpensetracker.parser.ParserUtils

class IdfcFirstBankRule : BankSmsRule {
    override val ruleId = "IDFC_FIRST_BANK_RULE"
    override val bankName = "IDFC FIRST Bank"
    override val priority = 100

    private val debitPatterns = listOf(
        // "Paid INR 1,250.00 from IDFC FIRST Bank A/C ending 1234 to APOLLO PHARMACY on 20-Aug-26. UPI Ref: 123456789"
        Regex(
            """(?i)Paid\s*(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*from\s*IDFC\s*FIRST\s*Bank\s*(?:A\/C|A\/c|Card)\s*(?:ending\s*)?(?<account>\d{4}|XX\d{4})\s*to\s*(?<merchant>.+?)\s*on\s*(?<date>[^\s.]+?)\.(?:\s*UPI\s*Ref:\s*(?<ref>[A-Za-z0-9]+))?""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val creditPatterns = listOf(
        // "Received INR 2,500.00 in IDFC FIRST Bank A/C ending 1234 on 20-Aug-26 from John Doe. UPI Ref: 987654321"
        Regex(
            """(?i)Received\s*(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*in\s*IDFC\s*FIRST\s*Bank\s*(?:A\/C|A\/c)\s*(?:ending\s*)?(?<account>\d{4}|XX\d{4})\s*on\s*(?<date>[^\s]+)\s*from\s*(?<merchant>[^.]+?)\.(?:\s*UPI\s*Ref:\s*(?<ref>[A-Za-z0-9]+))?""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    override fun canHandle(sender: String, body: String): Boolean {
        val s = sender.uppercase()
        val b = body.uppercase()
        return s.contains("IDFC") || b.contains("IDFC FIRST") || b.contains("IDFC BANK")
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParseResult {
        for (pattern in debitPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amount = ParserUtils.parseAmount(ParserUtils.safeGroup(match, "amount")) ?: continue
                val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "IDFC Payee")
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
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "IDFC Payer")
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

        return ParseResult.Failed("Could not parse IDFC FIRST SMS with defined templates")
    }
}
