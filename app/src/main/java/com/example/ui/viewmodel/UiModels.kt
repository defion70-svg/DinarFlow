package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.example.data.local.AppSettingsEntity
import com.example.data.local.BillEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.GoalEntity
import com.example.data.local.ShiftEntity
import com.example.data.local.ShiftPresetEntity
import com.example.data.local.TransactionEntity
import com.example.util.DateTimeUtils

enum class MainTab(val label: String) {
    TRANSACTIONS("Операции"),
    SHIFTS("Смены"),
    GOALS("Цели"),
    ANALYTICS("Сводка"),
    SETTINGS("Настройки")
}

enum class PeriodMode(val label: String) {
    MONTH("Месяц"),
    RANGE("Период"),
    ALL_TIME("Всё время")
}

@Immutable
data class PeriodSummary(
    val mode: PeriodMode = PeriodMode.MONTH,
    val selectedMonth: String = DateTimeUtils.currentMonthStr(),
    val rangeStart: String = "${DateTimeUtils.currentMonthStr()}-01",
    val rangeEnd: String = DateTimeUtils.todayStr(),
    val carryoverEnabled: Boolean = false,
    val priorNetRsd: Double = 0.0,
    val currentNetRsd: Double = 0.0,
    val displayedBalanceRsd: Double = 0.0,
    val incomeRsd: Double = 0.0,
    val expenseRsd: Double = 0.0,
    val expensePercentOfIncome: Int = 0
)

@Immutable
data class TransactionDayGroup(
    val date: String,
    val humanDate: String,
    val items: List<TransactionEntity>
)

@Immutable
data class BillUiItem(
    val bill: BillEntity,
    val category: CategoryEntity?,
    val isPaid: Boolean,
    val paidByTransaction: Boolean,
    val effectiveDueDate: String
)

@Immutable
data class BillsSummary(
    val items: List<BillUiItem> = emptyList(),
    val paidCount: Int = 0,
    val totalCount: Int = 0,
    val unpaidSumRsd: Double = 0.0,
    val plannedShiftForecastRsd: Double = 0.0,
    val freeBalanceRsd: Double = 0.0,
    val includePlannedInFree: Boolean = false
)

@Immutable
data class WeekdayTipStat(
    val weekdayIndex: Int, // 0=Mon..6=Sun
    val shortName: String,
    val avgTipsRsd: Double,
    val shiftCount: Int,
    val isBest: Boolean
)

@Immutable
data class PaydayPeriodStat(
    val title: String,
    val payoutSubLabel: String,
    val dateRangeLabel: String,
    val paydayDate: String,
    val workedHours: Double,
    val workedBasePayRsd: Double,
    val workedTipsRsd: Double,
    val workedCount: Int,
    val plannedHours: Double,
    val plannedBasePayRsd: Double,
    val forecastBasePayRsd: Double,
    val forecastTipsRsd: Double,
    val forecastTotalPayRsd: Double,
    val plannedCount: Int,
    val isSecondHalf: Boolean = false
)

@Immutable
data class ShiftMonthSummary(
    val workedCount: Int = 0,
    val totalHours: Double = 0.0,
    val plannedCount: Int = 0,
    val plannedHours: Double = 0.0,
    val basePayRsd: Double = 0.0,
    val tipsRsd: Double = 0.0,
    val totalEarningsRsd: Double = 0.0,
    val monthWithPlanRsd: Double = 0.0,
    val effectiveHourlyRate: Double = 0.0,
    val tipsPerHourRsd: Double = 0.0,
    val avgPayPer8hShift: Double = 0.0,
    val bestShift: ShiftEntity? = null,
    val bestDayDate: String? = null,
    val historicalAvgTipsPerHour: Double = 0.0,
    val weekdayAvgTips: List<WeekdayTipStat> = emptyList(),
    val payday1: PaydayPeriodStat? = null,
    val payday2: PaydayPeriodStat? = null
)

@Immutable
data class GoalUiItem(
    val goal: GoalEntity,
    val remainingRsd: Double,
    val progressFraction: Float,
    val percent: Int,
    val shiftsBaseExact: Double,
    val shiftsBaseCeil: Int,
    val shiftsWithTipsExact: Double,
    val shiftsWithTipsCeil: Int
)

@Immutable
data class CategoryBreakdownItem(
    val category: CategoryEntity,
    val amountRsd: Double,
    val sharePercent: Int,
    val shareFraction: Float,
    val limitRsd: Double,
    val limitPercent: Int,
    val limitFraction: Float,
    val transactionCount: Int
)

@Immutable
data class AnalyticsUiState(
    val hasAnyLimits: Boolean = false,
    val totalLimitRsd: Double = 0.0,
    val totalLimitedSpentRsd: Double = 0.0,
    val limitedRemainingRsd: Double = 0.0,
    val limitedUsagePercent: Int = 0,
    val warningCount: Int = 0,
    val exceededCount: Int = 0,
    val avgDailyExpenseRsd: Double = 0.0,
    val savingsRatePercent: Int = 0,
    val transactionCount: Int = 0,
    val expenseRows: List<CategoryBreakdownItem> = emptyList(),
    val incomeRows: List<CategoryBreakdownItem> = emptyList()
)

@Immutable
data class DinarFlowUiState(
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val categories: List<CategoryEntity> = emptyList(),
    val categoriesById: Map<String, CategoryEntity> = emptyMap(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val periodSummary: PeriodSummary = PeriodSummary(),
    val periodTransactions: List<TransactionEntity> = emptyList(),
    val groupedTransactions: List<TransactionDayGroup> = emptyList(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val billsSummary: BillsSummary = BillsSummary(),
    val monthShifts: List<ShiftEntity> = emptyList(),
    val workedMonthShifts: List<ShiftEntity> = emptyList(),
    val shiftsByDate: Map<String, List<ShiftEntity>> = emptyMap(),
    val shiftPresets: List<ShiftPresetEntity> = emptyList(),
    val shiftMonthSummary: ShiftMonthSummary = ShiftMonthSummary(),
    val goals: List<GoalUiItem> = emptyList(),
    val totalGoalsSavedRsd: Double = 0.0,
    val analytics: AnalyticsUiState = AnalyticsUiState(),
    val searchQuery: String = "",
    val filterCategoryId: String? = null,
    val isSyncing: Boolean = false
)
