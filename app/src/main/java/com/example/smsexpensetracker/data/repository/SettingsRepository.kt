package com.example.smsexpensetracker.data.repository

import com.example.smsexpensetracker.data.local.CategoryMappingDao
import com.example.smsexpensetracker.data.local.ExcludedSenderDao
import com.example.smsexpensetracker.data.model.CategoryMapping
import com.example.smsexpensetracker.data.model.ExcludedSender
import com.example.smsexpensetracker.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsRepository(
    private val categoryMappingDao: CategoryMappingDao,
    private val excludedSenderDao: ExcludedSenderDao
) {

    fun getAllCategoryMappings(): Flow<List<CategoryMapping>> =
        categoryMappingDao.getAllMappings()

    suspend fun addCategoryMapping(keyword: String, category: String) = withContext(Dispatchers.IO) {
        categoryMappingDao.insert(CategoryMapping(keyword.trim().lowercase(), category.trim()))
    }

    suspend fun deleteCategoryMapping(keyword: String) = withContext(Dispatchers.IO) {
        categoryMappingDao.deleteByKeyword(keyword)
    }

    fun getAllExcludedSenders(): Flow<List<ExcludedSender>> =
        excludedSenderDao.getAllExcludedSenders()

    suspend fun addExcludedSender(pattern: String, description: String? = null) = withContext(Dispatchers.IO) {
        excludedSenderDao.insert(ExcludedSender(pattern.trim().uppercase(), description))
    }

    suspend fun deleteExcludedSender(pattern: String) = withContext(Dispatchers.IO) {
        excludedSenderDao.deleteByPattern(pattern)
    }

    /**
     * Generates CSV format content locally for all transactions.
     */
    fun exportToCsv(transactions: List<Transaction>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("ID,Date,Amount,Type,Bank,Payment Mode,Payee/Merchant,Category,Account Last 4,Reference ID,Sender\n")

        for (tx in transactions) {
            val dateStr = dateFormat.format(Date(tx.timestamp))
            val payeeClean = tx.merchantOrPayee.replace(",", " ")
            val bankClean = tx.bank.replace(",", " ")
            val categoryClean = tx.category.replace(",", " ")
            sb.append("${tx.id},\"$dateStr\",${tx.amount},${tx.transactionType},\"$bankClean\",\"${tx.paymentMode.displayName}\",\"$payeeClean\",\"$categoryClean\",\"${tx.accountLast4 ?: ""}\",\"${tx.referenceId ?: ""}\",\"${tx.sender}\"\n")
        }

        return sb.toString()
    }
}
