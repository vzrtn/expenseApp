package com.example.smsexpensetracker.parser.rules

import com.example.smsexpensetracker.data.model.PaymentMode
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.TransactionType
import com.example.smsexpensetracker.parser.BankSmsRule
import com.example.smsexpensetracker.parser.CategoryClassifier
import com.example.smsexpensetracker.parser.ParseResult
import com.example.smsexpensetracker.parser.ParserUtils

class SbiBankRule : BankSmsRule {
    override val ruleId = "SBI_BANK_RULE"
    override val bankName = "SBI"
    override val priority = 100

    private val debitPatterns = listOf(
        // Pattern 1: "Dear SBI User, A/c 1234 debited by Rs450.00 on 20Aug26 trf to SWIGGY Ref 123456789012"
        Regex(
            """(?i)Dear\s*SBI\s*User,\s*A\/c\s*(?<account>\d{4}|XX\d{4})\s*debited\s*by\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*on\s*(?<date>[^\s]+)\s*(?:trf\s*to|transfer\s*to|to)\s*(?<merchant>[^\s]+)\s*(?:Ref|Ref\s*No)?\s*(?<ref>\d+)?""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Pattern 2: "INR 350.00 spent on your SBI Credit Card ending 9012 at AMAZON on 20-Aug-26"
        Regex(
            """(?i)(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*spent\s*on\s*your\s*SBI\s*Credit\s*Card\s*(?:ending\s*)?(?<account>\d{4}|XX\d{4})\s*on\s*(?<date>[^\s]+)\s*at\s*(?<merchant>[^.]+?)\.""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Pattern 3: "Dear Customer, INR 2,500.00 withdrawn from ATM using SBI Debit Card ending 4321 on 20Aug26"
        Regex(
            """(?i)(?:Dear\s*Customer,\s*)?(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*withdrawn\s*from\s*ATM\s*(?:using\s*SBI\s*Debit\s*Card\s*(?:ending\s*)?(?<account>\d{4}|XX\d{4}))?""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Pattern 4: "Your A/c ending 1234 is debited for Rs 199.00 on 20-Aug-26 by UPI ref 1234567890 (Payee: John Doe)"
        Regex(
            """(?i)Your\s*A\/c\s*(?:ending\s*)?(?<account>\d{4}|XX\d{4})\s*is\s*debited\s*for\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*on\s*(?<date>[^\s]+)\s*by\s*UPI\s*ref\s*(?<ref>\d+)\s*(?:\(Payee:\s*(?<merchant>[^)]+)\))?""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val creditPatterns = listOf(
        // Pattern 1: "Dear SBI User, A/c 1234 credited by Rs1200.00 on 20Aug26 by transfer from John Doe Ref 123456"
        Regex(
            """(?i)Dear\s*SBI\s*User,\s*A\/c\s*(?<account>\d{4}|XX\d{4})\s*credited\s*by\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*on\s*(?<date>[^\s]+)\s*(?:by\s*transfer\s*from|by|from)\s*(?<merchant>[^.]+?)(?:\s*Ref\s*(?<ref>\d+))?(?:\.|\s*Bal|$)""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    override fun canHandle(sender: String, body: String): Boolean {
        val s = sender.uppercase()
        val b = body.uppercase()
        return s.contains("SBI") || s.contains("SBISMS") || b.contains("SBI USER") || b.contains("STATE BANK") || b.contains("SBI CARD")
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParseResult {
        // Debits
        for (pattern in debitPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amount = ParserUtils.parseAmount(ParserUtils.safeGroup(match, "amount")) ?: continue
                val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "SBI Payee")
                val ref = ParserUtils.safeGroup(match, "ref")
                val mode = when {
                    body.contains("ATM", ignoreCase = true) -> PaymentMode.ATM_WITHDRAWAL
                    body.contains("Credit Card", ignoreCase = true) || sender.contains("SBICRD", ignoreCase = true) -> PaymentMode.CREDIT_CARD
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
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "SBI Payer")
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

        return ParseResult.Failed("Could not parse SBI SMS with defined templates")
    }
}
