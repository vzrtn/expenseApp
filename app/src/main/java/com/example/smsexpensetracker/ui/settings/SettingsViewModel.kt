package com.example.smsexpensetracker.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsexpensetracker.data.local.AppDatabase
import com.example.smsexpensetracker.data.model.CategoryMapping
import com.example.smsexpensetracker.data.model.ExcludedSender
import com.example.smsexpensetracker.data.repository.SettingsRepository
import com.example.smsexpensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository: SettingsRepository
    private val transactionRepository: TransactionRepository

    init {
        val db = AppDatabase.getDatabase(application)
        settingsRepository = SettingsRepository(
            db.categoryMappingDao(),
            db.excludedSenderDao()
        )
        transactionRepository = TransactionRepository(
            db.transactionDao(),
            db.categoryMappingDao(),
            db.excludedSenderDao()
        )
    }

    val excludedSenders: StateFlow<List<ExcludedSender>> =
        settingsRepository.getAllExcludedSenders().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categoryMappings: StateFlow<List<CategoryMapping>> =
        settingsRepository.getAllCategoryMappings().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addExcludedSender(pattern: String, description: String? = null) {
        viewModelScope.launch {
            if (pattern.isNotBlank()) {
                settingsRepository.addExcludedSender(pattern.trim(), description?.trim())
            }
        }
    }

    fun deleteExcludedSender(pattern: String) {
        viewModelScope.launch {
            settingsRepository.deleteExcludedSender(pattern)
        }
    }

    fun addCategoryMapping(keyword: String, category: String) {
        viewModelScope.launch {
            if (keyword.isNotBlank() && category.isNotBlank()) {
                settingsRepository.addCategoryMapping(keyword, category)
            }
        }
    }

    fun deleteCategoryMapping(keyword: String) {
        viewModelScope.launch {
            settingsRepository.deleteCategoryMapping(keyword)
        }
    }

    fun exportTransactionsToUri(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getAllTransactions().first()
                val csvContent = settingsRepository.exportToCsv(transactions)

                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(csvContent)
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to export CSV file")
            }
        }
    }

    fun deleteAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            transactionRepository.deleteAllTransactions()
            onComplete()
        }
    }
}
