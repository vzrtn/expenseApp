package com.example.smsexpensetracker.parser

import com.example.smsexpensetracker.data.model.Transaction

object SmsParsingEngine {

    /**
     * Main entry point to parse an incoming or historical SMS.
     *
     * @param sender Originating address / shortcode (e.g. "AD-HDFCBK")
     * @param body SMS text body
     * @param timestamp Receive timestamp (epoch millis)
     * @param excludedSenders Set or list of sender patterns excluded by the user
     * @param userCustomCategoryMappings Map of custom keyword -> category
     * @return [ParseResult] indicating Success, Ignored, LowConfidence, or Failed.
     */
    fun parseSms(
        sender: String,
        body: String,
        timestamp: Long = System.currentTimeMillis(),
        excludedSenders: Set<String> = emptySet(),
        userCustomCategoryMappings: Map<String, String> = emptyMap()
    ): ParseResult {
        // 1. Check if sender is in user's privacy deny list
        val senderClean = sender.trim()
        if (excludedSenders.any { pattern ->
                senderClean.equals(pattern, ignoreCase = true) ||
                senderClean.contains(pattern, ignoreCase = true)
            }) {
            return ParseResult.Ignored("Sender $sender is excluded by user privacy settings")
        }

        // 2. Fast pre-filter: check if sender or body looks transactional
        if (!ParserUtils.isTransactionalSender(senderClean)) {
            // Check if body at least has financial keywords
            val lowerBody = body.lowercase()
            val hasFinancialKeywords = lowerBody.contains("debited") || lowerBody.contains("credited") ||
                    lowerBody.contains("spent") || lowerBody.contains("withdrawn") ||
                    (lowerBody.contains("inr") || lowerBody.contains("rs."))
            if (!hasFinancialKeywords) {
                return ParseResult.Ignored("Non-transactional sender: $sender")
            }
        }

        // 3. Discard OTP, promotional, or non-transactional notices
        val discardReason = OtpPromoDetector.shouldDiscard(body)
        if (discardReason != null) {
            return ParseResult.Ignored(discardReason)
        }

        // 4. Iterate over registered rules in priority order
        val rules = RuleRegistry.getRules()
        var bestLowConfidence: ParseResult.LowConfidence? = null

        for (rule in rules) {
            if (rule.canHandle(senderClean, body)) {
                val result = rule.parse(senderClean, body, timestamp)
                when (result) {
                    is ParseResult.Success -> {
                        // Apply custom user category mappings if applicable
                        val updatedCategory = CategoryClassifier.classify(
                            result.transaction.merchantOrPayee,
                            body,
                            userCustomCategoryMappings
                        )
                        return ParseResult.Success(result.transaction.copy(category = updatedCategory))
                    }
                    is ParseResult.LowConfidence -> {
                        if (bestLowConfidence == null) {
                            val updatedCategory = CategoryClassifier.classify(
                                result.candidate.merchantOrPayee,
                                body,
                                userCustomCategoryMappings
                            )
                            bestLowConfidence = ParseResult.LowConfidence(
                                result.candidate.copy(category = updatedCategory),
                                result.reason
                            )
                        }
                    }
                    is ParseResult.Ignored -> return result
                    is ParseResult.Failed -> continue
                }
            }
        }

        // If a low-confidence parse was found, return it for manual review
        if (bestLowConfidence != null) {
            return bestLowConfidence
        }

        return ParseResult.Failed("No bank rule matched the SMS structure")
    }
}
