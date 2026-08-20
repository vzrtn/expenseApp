package com.example.smsexpensetracker.parser.rules

import com.example.smsexpensetracker.data.model.PaymentMode
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.TransactionType
import com.example.smsexpensetracker.parser.BankSmsRule
import com.example.smsexpensetracker.parser.CategoryClassifier
import com.example.smsexpensetracker.parser.ParseResult
import com.example.smsexpensetracker.parser.ParserUtils

class AxisBankRule : BankSmsRule {
    override val ruleId = "AXIS_BANK_RULE"
    override val bankName = "Axis Bank"
    override val priority = 100

    private val debitPatterns = listOf(
        // Pattern 1: "INR 850.00 debited from A/c no. XX1234 on 20-Aug-26 to ZOMATO. UPI Ref 123456789. Avl Bal: INR 15,000"
        Regex(
            """(?i)(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*debited\s*from\s*(?:A\/c\s*(?:no\.?)?\s*|Axis\s*Bank\s*A\/c\s*)?(?<account>XX\d{4}|\d{4})\s*on\s*(?<date>[^\s]+)\s*to\s*(?<merchant>[^.]+?)\.\s*(?:UPI\s*Ref\s*(?<ref>\d+))?""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Pattern 2: "Spent INR 1,499.00 on Axis Bank Credit Card XX4321 at MYNTRA on 20-08-2026. Avail Lmt: INR 80,000"
        Regex(
            """(?i)Spent\s*(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*on\s*Axis\s*Bank\s*(?:Credit\s*Card|Debit\s*Card|Card)\s*(?<account>XX\d{4}|\d{4})\s*at\s*(?<merchant>[^.]+?)\s*on\s*(?<date>[^\s.]+?)\.""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val creditPatterns = listOf(
        // Pattern 1: "INR 2,000.00 credited to A/c XX1234 on 20-Aug-26 by VPA sender@upi. UPI Ref 987654"
        Regex(
            """(?i)(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*credited\s*to\s*(?:A\/c\s*)*(?<account>XX\d{4}|\d{4})\s*on\s*(?<date>[^\s]+)\s*by\s*(?<merchant>[^.]+?)(?:\.\s*UPI\s*Ref\s*(?<ref>\d+))?(?:\.|\s*Avl|$)""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    override fun canHandle(sender: String, body: String): Boolean {
        val s = sender.uppercase()
        val b = body.uppercase()
        return s.contains("AXIS") || b.contains("AXIS BANK") || b.contains("AXISBK")
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParseResult {
        // Debits
        for (pattern in debitPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amount = ParserUtils.parseAmount(ParserUtils.safeGroup(match, "amount")) ?: continue
                val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "Axis Payee")
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
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "Axis Payer")
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

        return ParseResult.Failed("Could not parse Axis Bank SMS with defined templates")
    }
}
