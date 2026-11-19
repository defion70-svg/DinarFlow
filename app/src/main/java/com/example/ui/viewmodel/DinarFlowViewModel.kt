package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppSettingsEntity
import com.example.data.local.BillEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryType
import com.example.data.local.DinarFlowDatabase
import com.example.data.local.GoalEntity
import com.example.data.local.ShiftEntity
import com.example.data.local.ShiftPresetEntity
import com.example.data.local.TransactionEntity
import com.example.data.repository.BudgetAlert
import com.example.data.repository.FinanceRepository
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

private data class PeriodSelection(
    val mode: PeriodMode = PeriodMode.MONTH,
    val selectedMonth: String = DateTimeUtils.currentMonthStr(),
    val rangeStart: String = "${DateTimeUtils.currentMonthStr()}-01",
    val rangeEnd: String = DateTimeUtils.todayStr()
)

private data class FeedFilter(
    val searchQuery: String = "",
    val filterCategoryId: String? = null
)

class DinarFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DinarFlowDatabase.getInstance(application)
    private val repository = FinanceRepository(db)

    private val periodState = MutableStateFlow(PeriodSelection())
    private val feedFilterState = MutableStateFlow(FeedFilter())
    private val isSyncingState = MutableStateFlow(false)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    private val baseDataFlow = combine(
        repository.settingsFlow,
        repository.categoriesFlow,
        repository.transactionsFlow,
        repository.shiftsFlow,
        repository.shiftPresetsFlow
    ) { settings, categories, transactions, shifts, presets ->
        BaseDataBundle(settings, categories, transactions, shifts, presets)
    }

    private data class ComputedPeriodBundle(
        val uiStateBase: DinarFlowUiState,
        val periodTransactions: List<TransactionEntity>
    )

    private val periodBundleFlow = combine(
        baseDataFlow,
        repository.goalsFlow,
        repository.billsFlow,
        periodState
    ) { base, goals, bills, period ->
        buildPeriodBundle(base, goals, bills, period)
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<DinarFlowUiState> = combine(
        periodBundleFlow,
        feedFilterState,
        isSyncingState
    ) { bundle, feedFilter, isSyncing ->
        val queryLower = feedFilter.searchQuery.trim().lowercase()
        val categoriesById = bundle.uiStateBase.categoriesById
        val filteredFeed = if (feedFilter.filterCategoryId == null && queryLower.isEmpty()) {
            bundle.periodTransactions
        } else {
            bundle.periodTransactions.filter { tx ->
                val catMatch = feedFilter.filterCategoryId == null || tx.categoryId == feedFilter.filterCategoryId
                val queryMatch = if (queryLower.isEmpty()) {
                    true
                } else {
                    val catName = categoriesById[tx.categoryId]?.name?.lowercase() ?: ""
                    tx.note.lowercase().contains(queryLower) ||
                        catName.contains(queryLower) ||
                        tx.amountRsd.toString().contains(queryLower)
                }
                catMatch && queryMatch
            }
        }
        val groupedTransactions = if (filteredFeed === bundle.periodTransactions) {
            bundle.uiStateBase.groupedTransactions
        } else {
            groupTransactionsByDate(filteredFeed)
        }
        bundle.uiStateBase.copy(
            periodTransactions = filteredFeed,
            groupedTransactions = groupedTransactions,
            searchQuery = feedFilter.searchQuery,
            filterCategoryId = feedFilter.filterCategoryId,
            isSyncing = isSyncing
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DinarFlowUiState()
    )

    private data class BaseDataBundle(
        val settings: AppSettingsEntity,
        val categories: List<CategoryEntity>,
        val transactions: List<TransactionEntity>,
        val shifts: List<ShiftEntity>,
        val presets: List<ShiftPresetEntity>
    )

    private fun groupTransactionsByDate(items: List<TransactionEntity>): List<TransactionDayGroup> {
        return items
            .groupBy { it.date }
            .entries
            .sortedByDescending { it.key }
            .map { (dateStr, list) ->
                TransactionDayGroup(
                    date = dateStr,
                    humanDate = DateTimeUtils.formatDateHuman(dateStr),
                    items = list
                )
            }
    }

    private fun buildPeriodBundle(
        base: BaseDataBundle,
        goals: List<GoalEntity>,
        bills: List<BillEntity>,
        filter: PeriodSelection
    ): ComputedPeriodBundle {
        val settings = base.settings
        val categories = base.categories
        val categoriesById = categories.associateBy { it.id }
        val expenseCats = categories.filter { it.type == CategoryType.EXPENSE }.sortedBy { it.sortOrder }
        val incomeCats = categories.filter { it.type == CategoryType.INCOME }.sortedBy { it.sortOrder }

        val periodStart: String?
        val periodEnd: String?
        when (filter.mode) {
            PeriodMode.MONTH -> {
                val maxDay = DateTimeUtils.daysInMonth(filter.selectedMonth)
                periodStart = "${filter.selectedMonth}-01"
                periodEnd = "${filter.selectedMonth}-${String.format(java.util.Locale.US, "%02d", maxDay)}"
            }
            PeriodMode.RANGE -> {
                val s = if (filter.rangeStart <= filter.rangeEnd) filter.rangeStart else filter.rangeEnd
                val e = if (filter.rangeStart <= filter.rangeEnd) filter.rangeEnd else filter.rangeStart
                periodStart = s
                periodEnd = e
            }
            PeriodMode.ALL_TIME -> {
                periodStart = null
                periodEnd = null
            }
        }

        val periodTx = base.transactions.filter { tx ->
            (periodStart == null || tx.date >= periodStart) &&
                (periodEnd == null || tx.date <= periodEnd)
        }

        val priorTx = if (periodStart != null) {
            base.transactions.filter { it.date < periodStart }
        } else {
            emptyList()
        }

        val priorIncome = priorTx.filter { it.type == CategoryType.INCOME }.sumOf { it.amountRsd }
        val priorExpense = priorTx.filter { it.type == CategoryType.EXPENSE }.sumOf { it.amountRsd }
        val priorNet = CurrencyFormatter.roundRsd(priorIncome - priorExpense)

        val periodIncome = CurrencyFormatter.roundRsd(
            periodTx.filter { it.type == CategoryType.INCOME }.sumOf { it.amountRsd }
        )
        val periodExpense = CurrencyFormatter.roundRsd(
            periodTx.filter { it.type == CategoryType.EXPENSE }.sumOf { it.amountRsd }
        )
        val currentNet = CurrencyFormatter.roundRsd(periodIncome - periodExpense)
        val displayedBalance = if (settings.carryoverEnabled && filter.mode != PeriodMode.ALL_TIME) {
            CurrencyFormatter.roundRsd(priorNet + currentNet)
        } else {
            currentNet
        }
        val expensePct = if (periodIncome > 0.0) {
            ((periodExpense / periodIncome) * 100.0).roundToInt()
        } else {
            0
        }

        val periodSummary = PeriodSummary(
            mode = filter.mode,
            selectedMonth = filter.selectedMonth,
            rangeStart = periodStart ?: "",
            rangeEnd = periodEnd ?: "",
            carryoverEnabled = settings.carryoverEnabled,
            priorNetRsd = priorNet,
            currentNetRsd = currentNet,
            displayedBalanceRsd = displayedBalance,
            incomeRsd = periodIncome,
            expenseRsd = periodExpense,
            expensePercentOfIncome = expensePct
        )

        // Shifts calculations
        val allWorkedShifts = base.shifts.filter { !it.isPlanned }
        val totalAllWorkedHours = allWorkedShifts.sumOf { it.durationHours }
        val totalAllWorkedTips = allWorkedShifts.sumOf { it.tipsRsd }
        val historicalAvgTipsPerHour = if (totalAllWorkedHours > 0.0) {
            totalAllWorkedTips / totalAllWorkedHours
        } else {
            0.0
        }

        val monthShifts = base.shifts.filter { DateTimeUtils.monthOf(it.date) == filter.selectedMonth }
        val monthWorked = monthShifts.filter { !it.isPlanned }
        val monthPlanned = monthShifts.filter { it.isPlanned }

        val workedHours = CurrencyFormatter.roundRsd(monthWorked.sumOf { it.durationHours })
        val workedBase = CurrencyFormatter.roundRsd(monthWorked.sumOf { it.basePay })
        val workedTips = CurrencyFormatter.roundRsd(monthWorked.sumOf { it.tipsRsd })
        val totalEarnings = CurrencyFormatter.roundRsd(workedBase + workedTips)
        val tipsPerHourRaw = when {
            workedHours > 0.0 -> workedTips / workedHours
            else -> historicalAvgTipsPerHour
        }
        val tipsPerHourRsd = CurrencyFormatter.roundRsd(tipsPerHourRaw)
        val effHourly = if (workedHours > 0.0) {
            CurrencyFormatter.roundRsd(totalEarnings / workedHours)
        } else {
            CurrencyFormatter.roundRsd(settings.hourlyRate + tipsPerHourRaw)
        }
        val avg8h = CurrencyFormatter.roundRsd(effHourly * 8.0)

        val plannedCount = monthPlanned.size
        val plannedHours = CurrencyFormatter.roundRsd(monthPlanned.sumOf { it.durationHours })
        val plannedBasePay = CurrencyFormatter.roundRsd(monthPlanned.sumOf { it.basePay })
        val plannedTipsForecast = CurrencyFormatter.roundRsd(plannedHours * tipsPerHourRaw)
        val monthWithPlanRsd = CurrencyFormatter.roundRsd(totalEarnings + plannedBasePay + plannedTipsForecast)

        val bestShift = monthWorked.maxByOrNull { it.basePay + it.tipsRsd }
        val dayTotals = monthWorked.groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.basePay + it.tipsRsd } }
        val bestDayDate = dayTotals.maxByOrNull { it.value }?.key

        // Weekday average tips (use current month worked shifts when available, fallback to all worked shifts)
        val weekdaySource = if (monthWorked.isNotEmpty()) monthWorked else allWorkedShifts
        val weekdayStatsRaw = (0..6).map { wd ->
            val matching = weekdaySource.filter { DateTimeUtils.dayOfWeekMon0(it.date) == wd }
            val avg = if (matching.isNotEmpty()) matching.sumOf { it.tipsRsd } / matching.size else 0.0
            Triple(wd, CurrencyFormatter.roundRsd(avg), matching.size)
        }
        val maxWeekdayAvg = weekdayStatsRaw.maxOfOrNull { it.second } ?: 0.0
        val weekdayStats = weekdayStatsRaw.map { (wd, avg, cnt) ->
            WeekdayTipStat(
                weekdayIndex = wd,
                shortName = DateTimeUtils.weekdayNamesShort[wd],
                avgTipsRsd = avg,
                shiftCount = cnt,
                isBest = maxWeekdayAvg > 0.0 && avg >= maxWeekdayAvg - 0.01
            )
        }

        // Two Payday Periods for selectedMonth
        val lastDayOfMonth = DateTimeUtils.daysInMonth(filter.selectedMonth)
        val p1Worked = monthWorked.filter { DateTimeUtils.parseDay(it.date) <= 15 }
        val p1Planned = monthPlanned.filter { DateTimeUtils.parseDay(it.date) <= 15 }
        val p1Date = DateTimeUtils.clampedDateInMonth(filter.selectedMonth, settings.payday1Date)
        val p1WorkedBase = CurrencyFormatter.roundRsd(p1Worked.sumOf { it.basePay })
        val p1WorkedTips = CurrencyFormatter.roundRsd(p1Worked.sumOf { it.tipsRsd })
        val p1PlannedHours = CurrencyFormatter.roundRsd(p1Planned.sumOf { it.durationHours })
        val p1PlannedBase = CurrencyFormatter.roundRsd(p1Planned.sumOf { it.basePay })
        val p1ForecastBase = CurrencyFormatter.roundRsd(p1WorkedBase + p1PlannedBase)
        val p1ForecastTips = CurrencyFormatter.roundRsd(p1WorkedTips + p1PlannedHours * tipsPerHourRaw)
        val p1ForecastTotal = CurrencyFormatter.roundRsd(p1ForecastBase + p1ForecastTips)
        val payday1 = PaydayPeriodStat(
            title = "1–15",
            payoutSubLabel = "выплата ${DateTimeUtils.parseDay(p1Date)}-го",
            dateRangeLabel = "1–15 числа",
            paydayDate = p1Date,
            workedHours = CurrencyFormatter.roundRsd(p1Worked.sumOf { it.durationHours }),
            workedBasePayRsd = p1WorkedBase,
            workedTipsRsd = p1WorkedTips,
            workedCount = p1Worked.size,
            plannedHours = p1PlannedHours,
            plannedBasePayRsd = p1PlannedBase,
            forecastBasePayRsd = p1ForecastBase,
            forecastTipsRsd = p1ForecastTips,
            forecastTotalPayRsd = p1ForecastTotal,
            plannedCount = p1Planned.size,
            isSecondHalf = false
        )

        val p2Worked = monthWorked.filter { DateTimeUtils.parseDay(it.date) >= 16 }
        val p2Planned = monthPlanned.filter { DateTimeUtils.parseDay(it.date) >= 16 }
        val nextMonthStr = DateTimeUtils.shiftMonth(filter.selectedMonth, 1)
        val p2Date = DateTimeUtils.clampedDateInMonth(nextMonthStr, settings.payday2Date)
        val nextMonthShort = arrayOf(
            "янв.", "фев.", "мар.", "апр.", "мая", "июн.",
            "июл.", "авг.", "сен.", "окт.", "ноя.", "дек."
        )[(DateTimeUtils.parseMonth(nextMonthStr) - 1).coerceIn(0, 11)]
        val p2WorkedBase = CurrencyFormatter.roundRsd(p2Worked.sumOf { it.basePay })
        val p2WorkedTips = CurrencyFormatter.roundRsd(p2Worked.sumOf { it.tipsRsd })
        val p2PlannedHours = CurrencyFormatter.roundRsd(p2Planned.sumOf { it.durationHours })
        val p2PlannedBase = CurrencyFormatter.roundRsd(p2Planned.sumOf { it.basePay })
        val p2ForecastBase = CurrencyFormatter.roundRsd(p2WorkedBase + p2PlannedBase)
        val p2ForecastTips = CurrencyFormatter.roundRsd(p2WorkedTips + p2PlannedHours * tipsPerHourRaw)
        val p2ForecastTotal = CurrencyFormatter.roundRsd(p2ForecastBase + p2ForecastTips)
        val payday2 = PaydayPeriodStat(
            title = "16–$lastDayOfMonth",
            payoutSubLabel = "выплата ${DateTimeUtils.parseDay(p2Date)}-го ($nextMonthShort)",
            dateRangeLabel = "16–$lastDayOfMonth числа",
            paydayDate = p2Date,
            workedHours = CurrencyFormatter.roundRsd(p2Worked.sumOf { it.durationHours }),
            workedBasePayRsd = p2WorkedBase,
            workedTipsRsd = p2WorkedTips,
            workedCount = p2Worked.size,
            plannedHours = p2PlannedHours,
            plannedBasePayRsd = p2PlannedBase,
            forecastBasePayRsd = p2ForecastBase,
            forecastTipsRsd = p2ForecastTips,
            forecastTotalPayRsd = p2ForecastTotal,
            plannedCount = p2Planned.size,
            isSecondHalf = true
        )

        val shiftSummary = ShiftMonthSummary(
            workedCount = monthWorked.size,
            totalHours = workedHours,
            plannedCount = plannedCount,
            plannedHours = plannedHours,
            basePayRsd = workedBase,
            tipsRsd = workedTips,
            totalEarningsRsd = totalEarnings,
            monthWithPlanRsd = monthWithPlanRsd,
            effectiveHourlyRate = effHourly,
            tipsPerHourRsd = tipsPerHourRsd,
            avgPayPer8hShift = avg8h,
            bestShift = bestShift,
            bestDayDate = bestDayDate,
            historicalAvgTipsPerHour = CurrencyFormatter.roundRsd(historicalAvgTipsPerHour),
            weekdayAvgTips = weekdayStats,
            payday1 = payday1,
            payday2 = payday2
        )

        // Bills summary for selectedMonth
        val billItems = bills.map { bill ->
            val linkedTx = base.transactions.firstOrNull {
                it.linkedBillId == bill.id && it.linkedBillMonth == filter.selectedMonth
            }
            val manualPaid = bill.manualPaidMonths.split(",").contains(filter.selectedMonth)
            BillUiItem(
                bill = bill,
                category = categoriesById[bill.categoryId],
                isPaid = linkedTx != null || manualPaid,
                paidByTransaction = linkedTx != null,
                effectiveDueDate = DateTimeUtils.clampedDateInMonth(filter.selectedMonth, bill.dueDay)
            )
        }
        val unpaidBillsSum = CurrencyFormatter.roundRsd(
            billItems.filter { !it.isPaid }.sumOf { it.bill.amountRsd }
        )
        val plannedMonthForecast = CurrencyFormatter.roundRsd(
            monthPlanned.sumOf { it.basePay + it.durationHours * tipsPerHourRaw }
        )
        val freeBalance = CurrencyFormatter.roundRsd(
            displayedBalance - unpaidBillsSum +
                if (settings.includePlannedInFree) plannedMonthForecast else 0.0
        )
        val billsSummary = BillsSummary(
            items = billItems,
            paidCount = billItems.count { it.isPaid },
            totalCount = billItems.size,
            unpaidSumRsd = unpaidBillsSum,
            plannedShiftForecastRsd = plannedMonthForecast,
            freeBalanceRsd = freeBalance,
            includePlannedInFree = settings.includePlannedInFree
        )

        // Goals UI items
        val base8hPay = settings.hourlyRate * 8.0
        val avg8hWithTips = base8hPay + historicalAvgTipsPerHour * 8.0
        val goalItems = goals.map { goal ->
            val rem = (goal.targetAmountRsd - goal.savedAmountRsd).coerceAtLeast(0.0)
            val frac = if (goal.targetAmountRsd > 0.0) {
                (goal.savedAmountRsd / goal.targetAmountRsd).toFloat().coerceIn(0f, 1f)
            } else 0f
            val baseExact = if (base8hPay > 0.0) rem / base8hPay else 0.0
            val tipsExact = if (avg8hWithTips > 0.0) rem / avg8hWithTips else 0.0
            GoalUiItem(
                goal = goal,
                remainingRsd = CurrencyFormatter.roundRsd(rem),
                progressFraction = frac,
                percent = (frac * 100f).roundToInt(),
                shiftsBaseExact = (baseExact * 10.0).roundToInt() / 10.0,
                shiftsBaseCeil = if (rem <= 0.0) 0 else ceil(baseExact).toInt(),
                shiftsWithTipsExact = (tipsExact * 10.0).roundToInt() / 10.0,
                shiftsWithTipsCeil = if (rem <= 0.0) 0 else ceil(tipsExact).toInt()
            )
        }
        val totalGoalsSaved = CurrencyFormatter.roundRsd(goals.sumOf { it.savedAmountRsd })

        // Analytics state
        val expenseTx = periodTx.filter { it.type == CategoryType.EXPENSE }
        val incomeTx = periodTx.filter { it.type == CategoryType.INCOME }

        val expenseRows = buildBreakdownRows(
            cats = expenseCats,
            txs = expenseTx,
            totalPeriodAmount = periodExpense,
            includeLimits = true
        )
        val incomeRows = buildBreakdownRows(
            cats = incomeCats,
            txs = incomeTx,
            totalPeriodAmount = periodIncome,
            includeLimits = false
        )

        val limitedCats = expenseRows.filter { it.limitRsd > 0.0 }
        val totalLimit = CurrencyFormatter.roundRsd(limitedCats.sumOf { it.limitRsd })
        val totalLimitedSpent = CurrencyFormatter.roundRsd(limitedCats.sumOf { it.amountRsd })
        val limitedUsagePct = if (totalLimit > 0.0) ((totalLimitedSpent / totalLimit) * 100.0).roundToInt() else 0

        val elapsedDays = when (filter.mode) {
            PeriodMode.MONTH, PeriodMode.RANGE -> {
                if (periodStart != null && periodEnd != null) {
                    DateTimeUtils.elapsedDaysInPeriod(periodStart, periodEnd)
                } else 1
            }
            PeriodMode.ALL_TIME -> {
                val minD = base.transactions.minOfOrNull { it.date }
                val maxD = base.transactions.maxOfOrNull { it.date }
                if (minD != null && maxD != null) DateTimeUtils.elapsedDaysInPeriod(minD, maxD) else 1
            }
        }
        val avgDailyExpense = CurrencyFormatter.roundRsd(periodExpense / elapsedDays.coerceAtLeast(1))
        val savingsRate = if (periodIncome > 0.0) {
            (((periodIncome - periodExpense) / periodIncome) * 100.0).roundToInt()
        } else 0

        val analyticsState = AnalyticsUiState(
            hasAnyLimits = limitedCats.isNotEmpty(),
            totalLimitRsd = totalLimit,
            totalLimitedSpentRsd = totalLimitedSpent,
            limitedRemainingRsd = CurrencyFormatter.roundRsd(totalLimit - totalLimitedSpent),
            limitedUsagePercent = limitedUsagePct,
            warningCount = limitedCats.count { it.limitPercent in 80..99 },
            exceededCount = limitedCats.count { it.limitPercent >= 100 },
            avgDailyExpenseRsd = avgDailyExpense,
            savingsRatePercent = savingsRate,
            transactionCount = periodTx.size,
            expenseRows = expenseRows,
            incomeRows = incomeRows
        )

        val groupedTransactions = groupTransactionsByDate(periodTx)

        val workedMonthShifts = monthWorked.sortedByDescending { it.date }
        val shiftsByDate = monthShifts.groupBy { it.date }

        val baseState = DinarFlowUiState(
            settings = settings,
            categories = categories,
            categoriesById = categoriesById,
            expenseCategories = expenseCats,
            incomeCategories = incomeCats,
            periodSummary = periodSummary,
            periodTransactions = periodTx,
            groupedTransactions = groupedTransactions,
            allTransactions = base.transactions,
            billsSummary = billsSummary,
            monthShifts = monthShifts,
            workedMonthShifts = workedMonthShifts,
            shiftsByDate = shiftsByDate,
            shiftPresets = base.presets,
            shiftMonthSummary = shiftSummary,
            goals = goalItems,
            totalGoalsSavedRsd = totalGoalsSaved,
            analytics = analyticsState
        )
        return ComputedPeriodBundle(uiStateBase = baseState, periodTransactions = periodTx)
    }

    private fun buildBreakdownRows(
        cats: List<CategoryEntity>,
        txs: List<TransactionEntity>,
        totalPeriodAmount: Double,
        includeLimits: Boolean
    ): List<CategoryBreakdownItem> {
        val grouped = txs.groupBy { it.categoryId }
        val rows = mutableListOf<CategoryBreakdownItem>()
        for (cat in cats) {
            val list = grouped[cat.id].orEmpty()
            val sum = CurrencyFormatter.roundRsd(list.sumOf { it.amountRsd })
            val hasLimit = includeLimits && cat.monthlyLimitRsd > 0.0
            if (sum <= 0.0 && !hasLimit) continue
            val shareFrac = if (totalPeriodAmount > 0.0) (sum / totalPeriodAmount).toFloat().coerceIn(0f, 1f) else 0f
            val limitFrac = if (hasLimit) (sum / cat.monthlyLimitRsd).toFloat().coerceAtLeast(0f) else 0f
            rows.add(
                CategoryBreakdownItem(
                    category = cat,
                    amountRsd = sum,
                    sharePercent = (shareFrac * 100f).roundToInt(),
                    shareFraction = shareFrac,
                    limitRsd = if (hasLimit) cat.monthlyLimitRsd else 0.0,
                    limitPercent = (limitFrac * 100f).roundToInt(),
                    limitFraction = limitFrac.coerceIn(0f, 1f),
                    transactionCount = list.size
                )
            )
        }
        return rows.sortedByDescending { it.amountRsd }
    }

    // ================= FILTER & PERIOD ACTIONS =================

    fun shiftMonth(delta: Int) {
        val next = DateTimeUtils.shiftMonth(periodState.value.selectedMonth, delta)
        periodState.value = periodState.value.copy(
            mode = PeriodMode.MONTH,
            selectedMonth = next
        )
    }

    fun selectMonth(monthStr: String) {
        periodState.value = periodState.value.copy(
            mode = PeriodMode.MONTH,
            selectedMonth = monthStr
        )
    }

    fun setPeriodMode(mode: PeriodMode) {
        periodState.value = periodState.value.copy(mode = mode)
    }

    fun setCustomRange(startDate: String, endDate: String) {
        periodState.value = periodState.value.copy(
            mode = PeriodMode.RANGE,
            rangeStart = startDate,
            rangeEnd = endDate
        )
    }

    fun setSearchQuery(query: String) {
        feedFilterState.value = feedFilterState.value.copy(searchQuery = query)
    }

    fun setFilterCategory(categoryId: String?) {
        val current = feedFilterState.value.filterCategoryId
        feedFilterState.value = feedFilterState.value.copy(
            filterCategoryId = if (current == categoryId) null else categoryId
        )
    }

    fun toggleCarryover(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(carryoverEnabled = enabled) }
        }
    }

    fun toggleIncludePlannedInFree(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(includePlannedInFree = enabled) }
        }
    }

    // ================= TRANSACTIONS =================

    fun addTransaction(
        type: String,
        amountRsd: Double,
        categoryId: String,
        date: String,
        note: String
    ) {
        viewModelScope.launch {
            val alert = repository.addTransaction(type, amountRsd, categoryId, date, note)
            emitBudgetAlertIfNeeded(alert)
        }
    }

    fun updateTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            val alert = repository.updateTransaction(tx)
            emitBudgetAlertIfNeeded(alert)
        }
    }

    fun deleteTransaction(txId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(txId)
            _events.tryEmit("Операция удалена")
        }
    }

    private fun emitBudgetAlertIfNeeded(alert: BudgetAlert?) {
        if (alert == null) return
        val msg = if (alert.isExceeded) {
            "${alert.emoji} «${alert.categoryName}»: лимит превышен (${alert.percent}%)"
        } else {
            "${alert.emoji} «${alert.categoryName}»: ${alert.percent}% лимита"
        }
        _events.tryEmit(msg)
    }

    // ================= SHIFTS =================

    fun saveShift(
        id: String?,
        date: String,
        startTime: String,
        endTime: String,
        hourlyRate: Double,
        tipsRsd: Double,
        isPlanned: Boolean,
        usesDefaultRate: Boolean,
        note: String
    ) {
        viewModelScope.launch {
            repository.saveShift(id, date, startTime, endTime, hourlyRate, tipsRsd, isPlanned, usesDefaultRate, note)
            _events.tryEmit("Сохранено")
        }
    }

    fun deleteShift(shiftId: String, deleteLinkedTips: Boolean) {
        viewModelScope.launch {
            repository.deleteShift(shiftId, deleteLinkedTips)
            _events.tryEmit("Смена удалена")
        }
    }

    fun creditSalary(amountRsd: Double, date: String, note: String) {
        viewModelScope.launch {
            repository.creditSalary(amountRsd, date, note)
            _events.tryEmit("Зачислено: ${CurrencyFormatter.formatRsd(amountRsd)}")
        }
    }

    fun addShiftPreset(startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.addShiftPreset(startTime, endTime)
        }
    }

    fun deleteShiftPreset(id: String) {
        viewModelScope.launch {
            repository.deleteShiftPreset(id)
        }
    }

    // ================= GOALS =================

    fun createGoal(
        name: String,
        targetAmountRsd: Double,
        initialSavedRsd: Double,
        emoji: String,
        colorHex: Long
    ) {
        viewModelScope.launch {
            val alert = repository.createGoal(name, targetAmountRsd, initialSavedRsd, emoji, colorHex)
            emitBudgetAlertIfNeeded(alert)
        }
    }

    fun updateGoal(
        goalId: String,
        name: String,
        targetAmountRsd: Double,
        newSavedAmountRsd: Double,
        emoji: String,
        colorHex: Long
    ) {
        viewModelScope.launch {
            val alert = repository.updateGoal(goalId, name, targetAmountRsd, newSavedAmountRsd, emoji, colorHex)
            emitBudgetAlertIfNeeded(alert)
        }
    }

    fun depositToGoal(goalId: String, amountRsd: Double, date: String = DateTimeUtils.todayStr(), note: String = "") {
        viewModelScope.launch {
            val alert = repository.depositToGoal(goalId, amountRsd, date, note)
            _events.tryEmit("Цель пополнена на ${CurrencyFormatter.formatRsd(amountRsd)}")
            emitBudgetAlertIfNeeded(alert)
        }
    }

    fun withdrawFromGoal(goalId: String, amountRsd: Double) {
        viewModelScope.launch {
            repository.withdrawFromGoal(goalId, amountRsd)
            _events.tryEmit("Возвращено на баланс: ${CurrencyFormatter.formatRsd(amountRsd)}")
        }
    }

    fun deleteGoal(goalId: String, refundTransactions: Boolean) {
        viewModelScope.launch {
            repository.deleteGoal(goalId, refundTransactions)
            _events.tryEmit("Цель удалена")
        }
    }

    // ================= BILLS =================

    fun saveBill(
        id: String?,
        name: String,
        amountRsd: Double,
        categoryId: String,
        dueDay: Int,
        emoji: String
    ) {
        viewModelScope.launch {
            repository.saveBill(id, name, amountRsd, categoryId, dueDay, emoji)
        }
    }

    fun payBill(billId: String) {
        viewModelScope.launch {
            val month = periodState.value.selectedMonth
            val alert = repository.payBill(billId, month)
            _events.tryEmit("Счёт оплачен")
            emitBudgetAlertIfNeeded(alert)
        }
    }

    fun toggleBillManualPaid(billId: String) {
        viewModelScope.launch {
            val month = periodState.value.selectedMonth
            repository.toggleBillManualPaid(billId, month)
        }
    }

    fun deleteBill(billId: String) {
        viewModelScope.launch {
            repository.deleteBill(billId)
        }
    }

    // ================= CATEGORIES & SETTINGS =================

    fun saveCategory(
        id: String?,
        name: String,
        emoji: String,
        colorHex: Long,
        type: String,
        monthlyLimitRsd: Double
    ) {
        viewModelScope.launch {
            repository.saveCategory(id, name, emoji, colorHex, type, monthlyLimitRsd)
        }
    }

    fun updateCategoryLimit(categoryId: String, limitRsd: Double) {
        viewModelScope.launch {
            repository.updateCategoryLimit(categoryId, limitRsd)
        }
    }

    fun reorderCategories(orderedList: List<CategoryEntity>) {
        viewModelScope.launch {
            repository.reorderCategories(orderedList)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
        }
    }

    fun updateEurRate(newRate: Double) {
        if (newRate <= 0.0) return
        viewModelScope.launch {
            repository.updateSettings { it.copy(eurRate = CurrencyFormatter.roundRsd(newRate)) }
            _events.tryEmit("Курс: 1 € = ${CurrencyFormatter.formatRsd(newRate)}")
        }
    }

    fun updatePaydays(payday1: Int, payday2: Int) {
        viewModelScope.launch {
            repository.updateSettings {
                it.copy(
                    payday1Date = payday1.coerceIn(1, 31),
                    payday2Date = payday2.coerceIn(1, 31)
                )
            }
            _events.tryEmit("Даты выплат сохранены")
        }
    }

    fun updateBaseHourlyRate(newRate: Double, updateFuturePlanned: Boolean) {
        viewModelScope.launch {
            repository.updateBaseHourlyRate(newRate, updateFuturePlanned)
            _events.tryEmit("Ставка обновлена")
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(themeMode = mode) }
        }
    }

    fun updateSyncUrl(url: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(syncUrl = url.trim()) }
        }
    }

    // ================= BACKUP / SYNC / RESET =================

    suspend fun generateJsonBackup(): String = repository.exportJson()

    suspend fun generateCsvExport(): String = repository.exportCsv()

    fun importJsonBackup(rawJson: String) {
        viewModelScope.launch {
            runCatching {
                repository.importJson(rawJson)
            }.onSuccess {
                _events.tryEmit("Данные импортированы")
            }.onFailure { err ->
                _events.tryEmit("Ошибка JSON: ${err.message ?: "неверный формат"}")
            }
        }
    }

    fun uploadBackupToCloud(url: String) {
        viewModelScope.launch {
            isSyncingState.value = true
            repository.updateSettings { it.copy(syncUrl = url.trim()) }
            val result = repository.uploadBackupToUrl(url)
            isSyncingState.value = false
            _events.tryEmit(result.getOrElse { it.message ?: "Ошибка отправки" })
        }
    }

    fun downloadBackupFromCloud(url: String) {
        viewModelScope.launch {
            isSyncingState.value = true
            repository.updateSettings { it.copy(syncUrl = url.trim()) }
            val result = repository.downloadBackupFromUrl(url)
            isSyncingState.value = false
            _events.tryEmit(result.getOrElse { it.message ?: "Ошибка загрузки" })
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            _events.tryEmit("Данные сброшены")
        }
    }
}
