package com.example.smsexpensetracker.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.smsexpensetracker.data.local.AppDatabase
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.repository.TransactionRepository
import com.example.smsexpensetracker.service.HistoricalSmsScanWorker
import com.example.smsexpensetracker.ui.components.DonutSlice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val totalSpentToday: Double = 0.0,
    val totalCreditsToday: Double = 0.0,
    val debitCount: Int = 0,
    val creditCount: Int = 0,
    val yesterdaySpend: Double = 0.0,
    val todayTransactions: List<Transaction> = emptyList(),
    val categoryBreakdown: List<DonutSlice> = emptyList(),
    val flaggedCount: Int = 0,
    val isScanning: Boolean = false,
    val scanScannedCount: Int = 0,
    val scanTotalCount: Int = 0,
    val scanFoundCount: Int = 0,
    val scanPercent: Int = 0,
    val showScanDialog: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    private val workManager: WorkManager by lazy { WorkManager.getInstance(application) }

    private val _scanState = MutableStateFlow(
        ScanProgress(isScanning = false, scanned = 0, total = 0, found = 0, percent = 0, showDialog = false)
    )

    private data class ScanProgress(
        val isScanning: Boolean,
        val scanned: Int,
        val total: Int,
        val found: Int,
        val percent: Int,
        val showDialog: Boolean
    )

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TransactionRepository(
            db.transactionDao(),
            db.categoryMappingDao(),
            db.excludedSenderDao()
        )
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }

    private fun getYesterdayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }

    private val todayRange = getTodayRange()
    private val yesterdayRange = getYesterdayRange()

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getTransactionsBetween(todayRange.first, todayRange.second),
        repository.getDebitsBetween(yesterdayRange.first, yesterdayRange.second),
        repository.getFlaggedTransactions(),
        _scanState
    ) { todayTxns, yesterdayDebits, flaggedTxns, scan ->
        val debits = todayTxns.filter { it.transactionType == com.example.smsexpensetracker.data.model.TransactionType.DEBIT }
        val credits = todayTxns.filter { it.transactionType == com.example.smsexpensetracker.data.model.TransactionType.CREDIT }

        val totalSpent = debits.sumOf { it.amount }
        val totalCredits = credits.sumOf { it.amount }
        val yesterdaySpent = yesterdayDebits.sumOf { it.amount }

        // Category Breakdown
        val categories = debits.groupBy { it.category }
            .map { (cat, list) -> DonutSlice(label = cat, value = list.sumOf { it.amount }) }
            .sortedByDescending { it.value }

        HomeUiState(
            totalSpentToday = totalSpent,
            totalCreditsToday = totalCredits,
            debitCount = debits.size,
            creditCount = credits.size,
            yesterdaySpend = yesterdaySpent,
            todayTransactions = todayTxns,
            categoryBreakdown = categories,
            flaggedCount = flaggedTxns.size,
            isScanning = scan.isScanning,
            scanScannedCount = scan.scanned,
            scanTotalCount = scan.total,
            scanFoundCount = scan.found,
            scanPercent = scan.percent,
            showScanDialog = scan.showDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun startHistoricalScan() {
        val request = OneTimeWorkRequestBuilder<HistoricalSmsScanWorker>().build()
        workManager.enqueueUniqueWork(
            "historical_sms_scan",
            ExistingWorkPolicy.REPLACE,
            request
        )

        _scanState.value = _scanState.value.copy(isScanning = true, showDialog = true)

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(request.id).collect { workInfo ->
                if (workInfo != null) {
                    val progress = workInfo.progress
                    val scanned = progress.getInt(HistoricalSmsScanWorker.KEY_SCANNED, 0)
                    val total = progress.getInt(HistoricalSmsScanWorker.KEY_TOTAL, 0)
                    val found = progress.getInt(HistoricalSmsScanWorker.KEY_FOUND, 0)
                    val percent = progress.getInt(HistoricalSmsScanWorker.KEY_PERCENT, 0)

                    val isRunning = workInfo.state == WorkInfo.State.RUNNING || workInfo.state == WorkInfo.State.ENQUEUED
                    _scanState.value = _scanState.value.copy(
                        isScanning = isRunning,
                        scanned = scanned,
                        total = total,
                        found = found,
                        percent = percent
                    )
                }
            }
        }
    }

    fun dismissScanDialog() {
        _scanState.value = _scanState.value.copy(showDialog = false)
    }

    fun cancelScan() {
        workManager.cancelUniqueWork("historical_sms_scan")
        _scanState.value = _scanState.value.copy(isScanning = false, showDialog = false)
    }
}
