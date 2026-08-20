package com.example.smsexpensetracker.parser.rules

import com.example.smsexpensetracker.data.model.PaymentMode
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.TransactionType
import com.example.smsexpensetracker.parser.BankSmsRule
import com.example.smsexpensetracker.parser.CategoryClassifier
import com.example.smsexpensetracker.parser.ParseResult
import com.example.smsexpensetracker.parser.ParserUtils

class IciciBankRule : BankSmsRule {
    override val ruleId = "ICICI_BANK_RULE"
    override val bankName = "ICICI Bank"
    override val priority = 100

    private val debitPatterns = listOf(
        // Pattern 1: "Acct XX1234 debited with INR 750.00 on 20-Aug-26. Info: SWIGGY. UPI:123456789. Avl Bal: INR 10,000"
        Regex(
            """(?i)Acct\s*(?<account>XX\d{4}|\d{4})\s*debited\s*with\s*(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*on\s*(?<date>[^\s.]+?)\.\s*Info:\s*(?<merchant>[^.]+?)\.\s*(?:UPI:(?<ref>\d+))?""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Pattern 2: "Dear Customer, INR 1,299.00 spent on ICICI Bank Card XX5678 on 20-Aug-26 at FLIPKART. Avail Limit: INR 50,000."
        Regex(
            """(?i)Dear\s*Customer,\s*(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*spent\s*on\s*ICICI\s*Bank\s*(?:Credit\s*Card|Debit\s*Card|Card)\s*(?<account>XX\d{4}|\d{4})\s*on\s*(?<date>[^\s]+)\s*at\s*(?<merchant>[^.]+?)\.""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Pattern 3: "INR 500.00 debited from ICICI Bank Account XX9012 on 20-Aug-26 for transfer to John Doe."
        Regex(
            """(?i)(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*debited\s*from\s*ICICI\s*Bank\s*(?:Account|A\/c)\s*(?<account>XX\d{4}|\d{4})\s*on\s*(?<date>[^\s]+)\s*(?:for\s*transfer\s*to|to|towards)\s*(?<merchant>[^.]+?)\.""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val creditPatterns = listOf(
        // Pattern 1: "Acct XX1234 credited with INR 5,000.00 on 20-Aug-26. Info: NEFT-SALARY. Avl Bal: INR 55,000"
        Regex(
            """(?i)Acct\s*(?<account>XX\d{4}|\d{4})\s*credited\s*with\s*(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*on\s*(?<date>[^\s.]+?)\.\s*Info:\s*(?<merchant>[^.]+?)\.""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Pattern 2: "Dear Customer, your ICICI Bank Account XX1234 has been credited with INR 1,200.00 on 20-Aug-26 by UPI/987654321/Merchant"
        Regex(
            """(?i)credited\s*with\s*(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*on\s*(?<date>[^\s]+)\s*(?:by|from)\s*(?<merchant>[^.]+?)(?:\.|\s*Avl|$)""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    override fun canHandle(sender: String, body: String): Boolean {
        val s = sender.uppercase()
        val b = body.uppercase()
        return s.contains("ICICI") || b.contains("ICICI BANK") || b.contains("ICICIB")
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParseResult {
        // Debits
        for (pattern in debitPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amount = ParserUtils.parseAmount(ParserUtils.safeGroup(match, "amount")) ?: continue
                val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "ICICI Merchant")
                val ref = ParserUtils.safeGroup(match, "ref")
                val mode = when {
                    body.contains("Credit Card", ignoreCase = true) -> PaymentMode.CREDIT_CARD
                    body.contains("Card", ignoreCase = true) -> PaymentMode.DEBIT_CARD
                    body.contains("UPI", ignoreCase = true) || merchant.contains("@") -> PaymentMode.UPI
                    body.contains("ATM", ignoreCase = true) -> PaymentMode.ATM_WITHDRAWAL
                    body.contains("NEFT", ignoreCase = true) || body.contains("IMPS", ignoreCase = true) -> PaymentMode.NEFT_IMPS
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
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "ICICI Payer")
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

        return ParseResult.Failed("Could not parse ICICI SMS with defined templates")
    }
}
