package com.example.smsexpensetracker.parser

import com.example.smsexpensetracker.data.model.Transaction
import kotlin.math.abs

object FuzzyDeduplicator {

    const val DUPLICATE_TIME_WINDOW_MS = 120_000L // 2 minutes (±2 min)

    /**
     * Checks whether candidate matches an existing transaction within the fuzzy window.
     */
    fun isDuplicate(candidate: Transaction, existing: Transaction): Boolean {
        // 1. Same transaction direction (DEBIT or CREDIT)
        if (candidate.transactionType != existing.transactionType) {
            return false
        }

        // 2. Amount must match within tiny float delta (0.01)
        if (abs(candidate.amount - existing.amount) > 0.01) {
            return false
        }

        // 3. Timestamp within ±2 minutes window
        val timeDiff = abs(candidate.timestamp - existing.timestamp)
        if (timeDiff > DUPLICATE_TIME_WINDOW_MS) {
            return false
        }

        // 4. Check reference ID match if present in both
        if (!candidate.referenceId.isNullOrBlank() && !existing.referenceId.isNullOrBlank()) {
            if (candidate.referenceId.equals(existing.referenceId, ignoreCase = true)) {
                return true
            }
        }

        // 5. Check account number match if present in both
        if (!candidate.accountLast4.isNullOrBlank() && !existing.accountLast4.isNullOrBlank()) {
            if (candidate.accountLast4 == existing.accountLast4) {
                return true
            }
        }

        // 6. Fuzzy payee / merchant match if one contains the other or identical
        val payee1 = candidate.merchantOrPayee.trim().lowercase()
        val payee2 = existing.merchantOrPayee.trim().lowercase()
        if (payee1.isNotEmpty() && payee2.isNotEmpty()) {
            if (payee1 == payee2 || payee1.contains(payee2) || payee2.contains(payee1)) {
                return true
            }
        }

        // 7. If time difference is very tight (< 45 sec) and amount matches exactly
        if (timeDiff <= 45_000L) {
            return true
        }

        return false
    }

    /**
     * Checks if candidate is a duplicate against a list of recent transactions.
     */
    fun findDuplicateIn(candidate: Transaction, recentTransactions: List<Transaction>): Transaction? {
        return recentTransactions.firstOrNull { isDuplicate(candidate, it) }
    }
}
