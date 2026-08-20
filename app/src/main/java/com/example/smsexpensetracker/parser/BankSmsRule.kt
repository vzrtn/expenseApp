package com.example.smsexpensetracker.parser

import com.example.smsexpensetracker.data.model.Transaction

sealed class ParseResult {
    data class Success(val transaction: Transaction) : ParseResult()
    data class Ignored(val reason: String) : ParseResult()
    data class LowConfidence(val candidate: Transaction, val reason: String) : ParseResult()
    data class Failed(val reason: String) : ParseResult()
}

interface BankSmsRule {
    val ruleId: String
    val bankName: String
    val priority: Int

    /**
     * Checks if this rule can potentially process the SMS based on sender ID or body cues.
     */
    fun canHandle(sender: String, body: String): Boolean

    /**
     * Attempts to parse the SMS into a structured transaction.
     */
    fun parse(sender: String, body: String, timestamp: Long): ParseResult
}
