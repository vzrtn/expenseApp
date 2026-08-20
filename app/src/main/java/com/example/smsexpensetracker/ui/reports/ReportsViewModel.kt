package com.example.smsexpensetracker.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsexpensetracker.data.local.AppDatabase
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.TransactionType
import com.example.smsexpensetracker.data.repository.TransactionRepository
import com.example.smsexpensetracker.theme.PrimaryBlue
import com.example.smsexpensetracker.ui.components.BarData
import com.example.smsexpensetracker.ui.components.DonutSlice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReportTimeframe(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

enum class BreakdownTab(val displayName: String) {
    CATEGORY("Category"),
    PAYMENT_MODE("Payment Mode"),
    BANK("Bank")
}

data class ReportsUiState(
    val timeframe: ReportTimeframe = ReportTimeframe.DAILY,
    val breakdownTab: BreakdownTab = BreakdownTab.CATEGORY,
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val periodLabel: String = "Today",
    val totalSpent: Double = 0.0,
    val totalCredits: Double = 0.0,
    val debitCount: Int = 0,
    val creditCount: Int = 0,
    val categorySlices: List<DonutSlice> = emptyList(),
    val modeSlices: List<DonutSlice> = emptyList(),
    val bankSlices: List<DonutSlice> = emptyList(),
    val trendBars: List<BarData> = emptyList(),
    val transactions: List<Transaction> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    private val _timeframe = MutableStateFlow(ReportTimeframe.DAILY)
    private val _breakdownTab = MutableStateFlow(BreakdownTab.CATEGORY)
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TransactionRepository(
            db.transactionDao(),
            db.categoryMappingDao(),
            db.excludedSenderDao()
        )
    }

    private data class FilterParams(
        val timeframe: ReportTimeframe,
        val breakdownTab: BreakdownTab,
        val dateMillis: Long
    )

    val uiState: StateFlow<ReportsUiState> = combine(
        _timeframe,
        _breakdownTab,
        _selectedDate
    ) { tf, tab, date ->
        FilterParams(tf, tab, date)
    }.flatMapLatest { params ->
        val (startTime, endTime, label) = computePeriodRange(params.timeframe, params.dateMillis)

        // Query 7 days range for trend bars if weekly or daily
        val (trendStart, trendEnd) = compute7DaysRange(params.dateMillis)

        combine(
            repository.getTransactionsBetween(startTime, endTime),
            repository.getDebitsBetween(trendStart, trendEnd)
        ) { txns, trendDebits ->
            val debits = txns.filter { it.transactionType == TransactionType.DEBIT }
            val credits = txns.filter { it.transactionType == TransactionType.CREDIT }

            val totalSpent = debits.sumOf { it.amount }
            val totalCredits = credits.sumOf { it.amount }

            // 1. Category Breakdown
            val categories = debits.groupBy { it.category }
                .map { (cat, list) -> DonutSlice(label = cat, value = list.sumOf { it.amount }) }
                .sortedByDescending { it.value }

            // 2. Payment Mode Breakdown
            val modes = debits.groupBy { it.paymentMode.displayName }
                .map { (mode, list) -> DonutSlice(label = mode, value = list.sumOf { it.amount }) }
                .sortedByDescending { it.value }

            // 3. Bank Breakdown
            val banks = debits.groupBy { it.bank }
                .map { (bank, list) -> DonutSlice(label = bank, value = list.sumOf { it.amount }) }
                .sortedByDescending { it.value }

            // 4. Trend Bars (Last 7 Days)
            val trendBars = computeTrendBars(trendDebits, params.dateMillis)

            ReportsUiState(
                timeframe = params.timeframe,
                breakdownTab = params.breakdownTab,
                selectedDateMillis = params.dateMillis,
                periodLabel = label,
                totalSpent = totalSpent,
                totalCredits = totalCredits,
                debitCount = debits.size,
                creditCount = credits.size,
                categorySlices = categories,
                modeSlices = modes,
                bankSlices = banks,
                trendBars = trendBars,
                transactions = txns
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState()
    )

    fun setTimeframe(timeframe: ReportTimeframe) {
        _timeframe.value = timeframe
    }

    fun setBreakdownTab(tab: BreakdownTab) {
        _breakdownTab.value = tab
    }

    fun setDate(dateMillis: Long) {
        _selectedDate.value = dateMillis
    }

    fun previousPeriod() {
        val calendar = Calendar.getInstance().apply { timeInMillis = _selectedDate.value }
        when (_timeframe.value) {
            ReportTimeframe.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, -1)
            ReportTimeframe.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, -1)
            ReportTimeframe.MONTHLY -> calendar.add(Calendar.MONTH, -1)
        }
        _selectedDate.value = calendar.timeInMillis
    }

    fun nextPeriod() {
        val calendar = Calendar.getInstance().apply { timeInMillis = _selectedDate.value }
        when (_timeframe.value) {
            ReportTimeframe.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            ReportTimeframe.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            ReportTimeframe.MONTHLY -> calendar.add(Calendar.MONTH, 1)
        }
        _selectedDate.value = calendar.timeInMillis
    }

    fun goToToday() {
        _selectedDate.value = System.currentTimeMillis()
    }

    private fun computePeriodRange(timeframe: ReportTimeframe, anchorDate: Long): Triple<Long, Long, String> {
        val calendar = Calendar.getInstance().apply { timeInMillis = anchorDate }
        val now = Calendar.getInstance()

        return when (timeframe) {
            ReportTimeframe.DAILY -> {
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

                val isToday = calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

                now.add(Calendar.DAY_OF_YEAR, -1)
                val isYesterday = calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

                val label = when {
                    isToday -> "Today (${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(anchorDate))})"
                    isYesterday -> "Yesterday (${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(anchorDate))})"
                    else -> SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(anchorDate))
                }

                Triple(start, end, label)
            }
            ReportTimeframe.WEEKLY -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis

                calendar.add(Calendar.DAY_OF_WEEK, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis

                val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                val label = "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"

                Triple(start, end, label)
            }
            ReportTimeframe.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis

                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, maxDay)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis

                val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(anchorDate))

                Triple(start, end, label)
            }
        }
    }

    private fun compute7DaysRange(anchorDate: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply { timeInMillis = anchorDate }
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -6)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        return Pair(start, end)
    }

    private fun computeTrendBars(trendDebits: List<Transaction>, anchorDate: Long): List<BarData> {
        val bars = mutableListOf<BarData>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = Calendar.getInstance().apply { timeInMillis = anchorDate }

        // Last 7 days
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = anchorDate
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dayOfYear = dayCal.get(Calendar.DAY_OF_YEAR)
            val year = dayCal.get(Calendar.YEAR)

            val daySpend = trendDebits.filter {
                val txCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                txCal.get(Calendar.YEAR) == year && txCal.get(Calendar.DAY_OF_YEAR) == dayOfYear
            }.sumOf { it.amount }

            val label = dayFormat.format(dayCal.time)
            val isSelected = i == 0
            bars.add(BarData(label = label, amount = daySpend, isHighlighted = isSelected))
        }

        return bars
    }
}
