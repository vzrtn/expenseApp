package com.example.smsexpensetracker.data.repository

import com.example.smsexpensetracker.data.local.CategoryMappingDao
import com.example.smsexpensetracker.data.local.ExcludedSenderDao
import com.example.smsexpensetracker.data.local.TransactionDao
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.parser.FuzzyDeduplicator
import com.example.smsexpensetracker.parser.ParseResult
import com.example.smsexpensetracker.parser.SmsParsingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryMappingDao: CategoryMappingDao,
    private val excludedSenderDao: ExcludedSenderDao
) {

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsBetween(startTime, endTime)

    fun getDebitsBetween(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactionDao.getDebitsBetween(startTime, endTime)

    fun getCreditsBetween(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactionDao.getCreditsBetween(startTime, endTime)

    fun getFlaggedTransactions(): Flow<List<Transaction>> =
        transactionDao.getFlaggedTransactions()

    suspend fun getTransactionById(id: Long): Transaction? = withContext(Dispatchers.IO) {
        transactionDao.getTransactionById(id)
    }

    suspend fun insertTransaction(transaction: Transaction): Long = withContext(Dispatchers.IO) {
        transactionDao.insert(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.update(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.delete(transaction)
    }

    suspend fun deleteTransactionById(id: Long) = withContext(Dispatchers.IO) {
        transactionDao.deleteById(id)
    }

    suspend fun deleteAllTransactions() = withContext(Dispatchers.IO) {
        transactionDao.deleteAll()
    }

    suspend fun getTransactionCount(): Int = withContext(Dispatchers.IO) {
        transactionDao.getTransactionCount()
    }

    /**
     * Parses an incoming or historical SMS, checks excluded senders, deduplicates against recent
     * database transactions within ±2 minutes, and saves if valid.
     */
    suspend fun processAndInsertSms(
        sender: String,
        body: String,
        timestamp: Long
    ): ParseResult = withContext(Dispatchers.IO) {
        val excludedSenders = excludedSenderDao.getAllExcludedSendersSync()
            .map { it.senderPattern }
            .toSet()

        val customMappings = categoryMappingDao.getAllMappingsSync()
            .associate { it.keyword to it.category }

        val parseResult = SmsParsingEngine.parseSms(
            sender = sender,
            body = body,
            timestamp = timestamp,
            excludedSenders = excludedSenders,
            userCustomCategoryMappings = customMappings
        )

        when (parseResult) {
            is ParseResult.Success -> {
                val candidate = parseResult.transaction
                // Check deduplication within ±2 min window
                val minTime = timestamp - FuzzyDeduplicator.DUPLICATE_TIME_WINDOW_MS
                val maxTime = timestamp + FuzzyDeduplicator.DUPLICATE_TIME_WINDOW_MS
                val windowTxns = transactionDao.getTransactionsInWindow(minTime, maxTime)

                val duplicate = FuzzyDeduplicator.findDuplicateIn(candidate, windowTxns)
                if (duplicate != null) {
                    ParseResult.Ignored("Duplicate transaction detected (matches Txn #${duplicate.id})")
                } else {
                    val insertedId = transactionDao.insert(candidate)
                    ParseResult.Success(candidate.copy(id = insertedId))
                }
            }
            is ParseResult.LowConfidence -> {
                val candidate = parseResult.candidate.copy(isManualReviewRequired = true)
                val insertedId = transactionDao.insert(candidate)
                ParseResult.LowConfidence(candidate.copy(id = insertedId), parseResult.reason)
            }
            is ParseResult.Ignored -> parseResult
            is ParseResult.Failed -> parseResult
        }
    }
}
