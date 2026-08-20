package com.example.smsexpensetracker.parser.rules

import com.example.smsexpensetracker.data.model.PaymentMode
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.TransactionType
import com.example.smsexpensetracker.parser.BankSmsRule
import com.example.smsexpensetracker.parser.CategoryClassifier
import com.example.smsexpensetracker.parser.ParseResult
import com.example.smsexpensetracker.parser.ParserUtils

class HdfcBankRule : BankSmsRule {
    override val ruleId = "HDFC_BANK_RULE"
    override val bankName = "HDFC Bank"
    override val priority = 100

    private val debitPatterns = listOf(
        // Pattern 1: "Rs.500.00 debited from A/c XX1234 on 20-Aug-26 to VPA merchant@ybl UPI Ref No 123456"
        Regex(
            """(?i)(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*debited\s*from\s*(?:HDFC\s*Bank\s*)?(?:A\/c\s*|acct\s*|account\s*)*(?<account>XX\d{4}|\d{4}|\*{2}\d{4})?\s*(?:on\s*(?<date>[^\s,]+))?\s*(?:to|at|towards|info:)\s*(?<merchant>[^.]+?)(?:\s*(?:UPI\s*(?:ref|Ref|txn|Txn)\s*(?:no\.?)?\s*|Ref\s*(?:no\.?)?\s*|UTR\s*:?\s*)(?<ref>[A-Za-z0-9]+))?(?:\.|\s*Available|\s*Avail|\s*Bal|$)""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Pattern 2: "UPDATE: INR 1,200.00 debited from HDFC Bank A/c 5678 on 20-Aug-26 to SWIGGY. UPI:987654"
        Regex(
            """(?i)UPDATE:\s*(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*debited\s*from\s*HDFC\s*Bank\s*A\/c\s*(?<account>XX\d{4}|\d{4})?\s*on\s*(?<date>[^\s]+)\s*to\s*(?<merchant>[^.]+?)\.\s*(?:UPI:(?<ref>\d+))?""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Pattern 3: "Spent INR 350.00 on HDFC Bank Card XX9012 at AMAZON on 20-Aug-26"
        Regex(
            """(?i)(?:Spent|Paid)\s*(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*on\s*HDFC\s*Bank\s*(?:Credit\s*Card|Debit\s*Card|Card)\s*(?<account>XX\d{4}|\d{4})\s*at\s*(?<merchant>[^.]+?)(?:\s*on\s*(?<date>[^\s.]+))?\.""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Pattern 4: "INR 2,000.00 withdrawn from ATM using HDFC Bank Card ending 1234"
        Regex(
            """(?i)(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*withdrawn\s*(?:from\s*ATM)?\s*(?:using\s*HDFC\s*Bank\s*Card\s*(?:ending\s*)?(?<account>XX\d{4}|\d{4}))?""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val creditPatterns = listOf(
        // Pattern 1: "Rs 1,500.00 credited to HDFC Bank A/c XX1234 on 20-Aug-26 by John Doe Ref 12345"
        Regex(
            """(?i)(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*credited\s*to\s*(?:HDFC\s*Bank\s*)?(?:A\/c\s*|account\s*)*(?<account>XX\d{4}|\d{4})?\s*(?:on\s*(?<date>[^\s,]+))?\s*(?:by|from|via)\s*(?<merchant>[^.]+?)(?:\s*(?:UPI\s*ref|Ref|UTR|txn)\s*(?<ref>[A-Za-z0-9]+))?(?:\.|\s*Available|\s*Bal|$)""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    override fun canHandle(sender: String, body: String): Boolean {
        val s = sender.uppercase()
        val b = body.uppercase()
        return s.contains("HDFC") || b.contains("HDFC BANK") || b.contains("HDFCBK")
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParseResult {
        // Check Debits
        for (pattern in debitPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amountStr = ParserUtils.safeGroup(match, "amount")
                val amount = ParserUtils.parseAmount(amountStr) ?: continue
                val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "HDFC Merchant")
                val ref = ParserUtils.safeGroup(match, "ref")
                val mode = when {
                    body.contains("ATM", ignoreCase = true) -> PaymentMode.ATM_WITHDRAWAL
                    body.contains("Credit Card", ignoreCase = true) -> PaymentMode.CREDIT_CARD
                    body.contains("Card", ignoreCase = true) -> PaymentMode.DEBIT_CARD
                    body.contains("UPI", ignoreCase = true) || merchant.contains("@") -> PaymentMode.UPI
                    body.contains("NEFT", ignoreCase = true) || body.contains("IMPS", ignoreCase = true) -> PaymentMode.NEFT_IMPS
                    else -> PaymentMode.UPI
                }
                val category = CategoryClassifier.classify(merchant, body)
                val transaction = Transaction(
                    amount = amount,
                    transactionType = TransactionType.DEBIT,
                    bank = bankName,
                    paymentMode = mode,
                    paymentApp = if (body.contains("Paytm", ignoreCase = true)) "Paytm" else if (body.contains("GPay", ignoreCase = true)) "Google Pay" else null,
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

        // Check Credits
        for (pattern in creditPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amountStr = ParserUtils.safeGroup(match, "amount")
                val amount = ParserUtils.parseAmount(amountStr) ?: continue
                val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "HDFC Payer")
                val ref = ParserUtils.safeGroup(match, "ref")
                val mode = if (body.contains("UPI", ignoreCase = true) || merchant.contains("@")) PaymentMode.UPI else PaymentMode.NEFT_IMPS
                val category = CategoryClassifier.classify(merchant, body)
                val transaction = Transaction(
                    amount = amount,
                    transactionType = TransactionType.CREDIT,
                    bank = bankName,
                    paymentMode = mode,
                    paymentApp = null,
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

        return ParseResult.Failed("Could not parse HDFC SMS with defined templates")
    }
}
