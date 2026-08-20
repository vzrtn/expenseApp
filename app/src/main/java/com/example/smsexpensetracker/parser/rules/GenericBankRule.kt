package com.example.smsexpensetracker.parser.rules

import com.example.smsexpensetracker.data.model.PaymentMode
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.TransactionType
import com.example.smsexpensetracker.parser.BankSmsRule
import com.example.smsexpensetracker.parser.CategoryClassifier
import com.example.smsexpensetracker.parser.ParseResult
import com.example.smsexpensetracker.parser.ParserUtils

class GenericBankRule : BankSmsRule {
    override val ruleId = "GENERIC_BANK_RULE"
    override val bankName = "Bank Account"
    override val priority = 10 // Lowest priority fallback

    private val debitPatterns = listOf(
        // Pattern 1: "Rs.500.00 debited from A/c XX1234 on 20-Aug-26 to VPA merchant@ybl"
        Regex(
            """(?i)(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*(?:is\s*)?debited\s*(?:from|by)?\s*(?:your\s*)?(?:A\/c|Account|Card|ending\s*no\.?)?\s*(?<account>XX\d{4}|\*{2,4}\d{4}|\d{4})?\s*(?:on\s*(?<date>[^\s,]+))?\s*(?:to|at|towards|for)\s*(?<merchant>[^.]+?)(?:\s*(?:UPI\s*ref|Ref|txn|UTR)\s*(?:no\.?)?\s*(?<ref>[A-Za-z0-9]+))?(?:\.|\s*Bal|$)""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Pattern 2: "Spent Rs 799.00 on card XX1234 at SWIGGY on 20-Aug-26"
        Regex(
            """(?i)(?:Spent|Paid|Transferred)\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*(?:on|from|using)?\s*(?:card|A\/c|Account)?\s*(?<account>XX\d{4}|\*{2,4}\d{4}|\d{4})?\s*(?:at|to|for)\s*(?<merchant>[^.]+?)(?:\s*on\s*(?<date>[^\s.]+))?\.""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Pattern 3: "Sent Rs.199 to John Doe via UPI Ref No 123456789012"
        Regex(
            """(?i)Sent\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*to\s*(?<merchant>[^.]+?)\s*(?:via\s*UPI\s*(?:Ref\s*(?:No\.?)?\s*)?(?<ref>[A-Za-z0-9]+))?""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Pattern 4: "INR 1,200.00 withdrawn from ATM using Debit Card ending 1234"
        Regex(
            """(?i)(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*withdrawn\s*(?:from\s*ATM)?\s*(?:using\s*(?:Debit\s*Card|Card)\s*(?:ending\s*)?(?<account>\d{4}|XX\d{4}))?""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    private val creditPatterns = listOf(
        // Pattern 1: "You have received Rs 1,200 in your A/c XX5678"
        Regex(
            """(?i)(?:You\s*have\s*)?received\s*(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*in\s*(?:your\s*)?(?:A\/c|Account)\s*(?<account>XX\d{4}|\d{4})?\s*(?:from\s*(?<merchant>[^.]+?))?(?:\.|\s*Bal|$)""",
            RegexOption.DOT_MATCHES_ALL
        ),
        // Pattern 2: "Rs.2,500.00 credited to A/c XX1234 on 20-Aug-26 by transfer from John. Ref 12345"
        Regex(
            """(?i)(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*(?:is\s*)?credited\s*to\s*(?:your\s*)?(?:A\/c|Account)\s*(?<account>XX\d{4}|\d{4})?\s*(?:on\s*(?<date>[^\s]+))?\s*(?:by|from)\s*(?<merchant>[^.]+?)(?:\s*Ref\s*(?<ref>[A-Za-z0-9]+))?(?:\.|\s*Bal|$)""",
            RegexOption.DOT_MATCHES_ALL
        )
    )

    override fun canHandle(sender: String, body: String): Boolean {
        return true // Fallback handles anything that reaches it
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParseResult {
        val detectedBank = extractBankName(sender, body)

        // Debits
        for (pattern in debitPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amount = ParserUtils.parseAmount(ParserUtils.safeGroup(match, "amount")) ?: continue
                val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "Payee")
                val ref = ParserUtils.safeGroup(match, "ref")
                val mode = PaymentMode.fromString(body)
                val category = CategoryClassifier.classify(merchant, body)
                val confidence = if (account != null || ref != null) 0.85f else 0.70f

                val transaction = Transaction(
                    amount = amount,
                    transactionType = TransactionType.DEBIT,
                    bank = detectedBank,
                    paymentMode = mode,
                    paymentApp = null,
                    merchantOrPayee = merchant,
                    accountLast4 = account,
                    referenceId = ref,
                    timestamp = timestamp,
                    rawSmsBody = body,
                    confidence = confidence,
                    category = category,
                    sender = sender,
                    isManualReviewRequired = confidence < 0.75f
                )

                return if (confidence >= 0.75f) {
                    ParseResult.Success(transaction)
                } else {
                    ParseResult.LowConfidence(transaction, "Generic fallback parser used with partial match")
                }
            }
        }

        // Credits
        for (pattern in creditPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val amount = ParserUtils.parseAmount(ParserUtils.safeGroup(match, "amount")) ?: continue
                val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
                val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "Payer")
                val ref = ParserUtils.safeGroup(match, "ref")
                val mode = PaymentMode.fromString(body)
                val category = CategoryClassifier.classify(merchant, body)
                val confidence = if (account != null || ref != null) 0.85f else 0.70f

                val transaction = Transaction(
                    amount = amount,
                    transactionType = TransactionType.CREDIT,
                    bank = detectedBank,
                    paymentMode = mode,
                    paymentApp = null,
                    merchantOrPayee = merchant,
                    accountLast4 = account,
                    referenceId = ref,
                    timestamp = timestamp,
                    rawSmsBody = body,
                    confidence = confidence,
                    category = category,
                    sender = sender,
                    isManualReviewRequired = confidence < 0.75f
                )

                return if (confidence >= 0.75f) {
                    ParseResult.Success(transaction)
                } else {
                    ParseResult.LowConfidence(transaction, "Generic fallback parser used with partial match")
                }
            }
        }

        return ParseResult.Failed("Generic parser could not identify amount and transaction structure")
    }

    private fun extractBankName(sender: String, body: String): String {
        val s = sender.uppercase()
        val b = body.uppercase()
        return when {
            s.contains("CANARA") || s.contains("CANBNK") || b.contains("CANARA") -> "Canara Bank"
            s.contains("UNION") || s.contains("UNIONB") || b.contains("UNION BANK") -> "Union Bank"
            s.contains("INDUS") || s.contains("INDUSB") || b.contains("INDUSIND") -> "IndusInd Bank"
            s.contains("FED") || s.contains("FEDBNK") || b.contains("FEDERAL") -> "Federal Bank"
            s.contains("RBL") || b.contains("RBL BANK") -> "RBL Bank"
            s.contains("BANDHAN") || b.contains("BANDHAN") -> "Bandhan Bank"
            s.contains("SCB") || b.contains("STANDARD CHARTERED") -> "Standard Chartered"
            s.contains("HSBC") || b.contains("HSBC") -> "HSBC"
            s.contains("CITI") || b.contains("CITIBANK") -> "Citi Bank"
            s.contains("UCO") || b.contains("UCO BANK") -> "UCO Bank"
            s.contains("CENTRAL") || b.contains("CENTRAL BANK") -> "Central Bank of India"
            s.contains("IOB") || b.contains("INDIAN OVERSEAS") -> "Indian Overseas Bank"
            else -> "Bank Account"
        }
    }
}
