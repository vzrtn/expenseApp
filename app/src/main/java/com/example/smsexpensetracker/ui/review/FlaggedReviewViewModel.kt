package com.example.smsexpensetracker.ui.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsexpensetracker.data.local.AppDatabase
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.repository.SettingsRepository
import com.example.smsexpensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FlaggedReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository
    private val settingsRepository: SettingsRepository

    init {
        val db = AppDatabase.getDatabase(application)
        transactionRepository = TransactionRepository(
            db.transactionDao(),
            db.categoryMappingDao(),
            db.excludedSenderDao()
        )
        settingsRepository = SettingsRepository(
            db.categoryMappingDao(),
            db.excludedSenderDao()
        )
    }

    val flaggedTransactions: StateFlow<List<Transaction>> =
        transactionRepository.getFlaggedTransactions().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun confirmTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(
                transaction.copy(isManualReviewRequired = false, confidence = 1.0f)
            )
        }
    }

    fun updateAndConfirm(
        transaction: Transaction,
        newMerchant: String,
        newCategory: String,
        learnKeyword: Boolean = true
    ) {
        viewModelScope.launch {
            val merchantClean = newMerchant.trim().ifEmpty { transaction.merchantOrPayee }
            transactionRepository.updateTransaction(
                transaction.copy(
                    merchantOrPayee = merchantClean,
                    category = newCategory,
                    isManualReviewRequired = false,
                    confidence = 1.0f
                )
            )

            if (learnKeyword && merchantClean.isNotBlank() && merchantClean != "Unknown Payee") {
                settingsRepository.addCategoryMapping(merchantClean, newCategory)
            }
        }
    }

    fun excludeTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction.copy(isExcluded = true))
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }
}
