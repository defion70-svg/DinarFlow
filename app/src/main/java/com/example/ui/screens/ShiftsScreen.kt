package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ShiftEntity
import com.example.data.local.ShiftPresetEntity
import com.example.ui.components.CurrencyAmountField
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.NativeDatePickerModal
import com.example.ui.components.NativeTimePickerDialog
import com.example.ui.components.resolveRsdAndNote
import com.example.ui.theme.LocalSemanticColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.viewmodel.DinarFlowUiState
import com.example.ui.viewmodel.PaydayPeriodStat
import com.example.ui.viewmodel.WeekdayTipStat
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import com.example.util.InputCurrency
import kotlin.math.roundToLong

@Composable
fun ShiftsScreen(
    uiState: DinarFlowUiState,
    onSaveShift: (
        id: String?,
        date: String,
        startTime: String,
        endTime: String,
        hourlyRate: Double,
        tipsRsd: Double,
        isPlanned: Boolean,
        usesDefaultRate: Boolean,
        note: String
    ) -> Unit,
    onDeleteShift: (shiftId: String, deleteLinkedTips: Boolean) -> Unit,
    onCreditSalary: (amountRsd: Double, date: String, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedMonth = uiState.periodSummary.selectedMonth.ifEmpty { DateTimeUtils.currentMonthStr() }
    val summary = uiState.shiftMonthSummary
    val workedShifts = uiState.workedMonthShifts
    val shiftsByDate = uiState.shiftsByDate

    var activeDateForSheet by remember { mutableStateOf<String?>(null) }
    var activeShiftForSheet by remember { mutableStateOf<ShiftEntity?>(null) }
    var salaryPeriodForSheet by remember { mutableStateOf<PaydayPeriodStat?>(null) }
    var shiftPendingDelete by remember { mutableStateOf<ShiftEntity?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Interactive Monthly Calendar
        item(key = "shift_calendar", contentType = "calendar") {
            ShiftCalendarCard(
                monthStr = selectedMonth,
                shiftsByDate = shiftsByDate,
                bestDayDate = summary.bestDayDate,
                onDayClick = { dateStr ->
                    val existing = shiftsByDate[dateStr]
                    activeShiftForSheet = existing?.firstOrNull { !it.isPlanned } ?: existing?.firstOrNull()
                    activeDateForSheet = dateStr
                }
            )
        }

        // 2. Unified Summary, Potential Income & Payouts Card
        item(key = "shift_summary", contentType = "summary") {
            ShiftSummaryCard(
                summary = summary,
                eurRate = uiState.settings.eurRate,
                onCreditSalaryClick = { stat -> salaryPeriodForSheet = stat }
            )
        }

        // 3. Worked Shifts Chronological List
        item(key = "worked_list_header", contentType = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Отработано",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${workedShifts.size}",
                    style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (workedShifts.isEmpty()) {
            item(key = "shifts_empty", contentType = "empty") {
                EmptyStateCard(
                    icon = Icons.Default.WorkHistory,
                    title = "Смен нет",
                    subtitle = "Нажмите на день в календаре.",
                    actionLabel = "Добавить на сегодня",
                    onAction = {
                        val today = DateTimeUtils.todayStr()
                        val target = if (DateTimeUtils.monthOf(today) == selectedMonth) today else "$selectedMonth-01"
                        val existing = shiftsByDate[target]
                        activeShiftForSheet = existing?.firstOrNull { !it.isPlanned } ?: existing?.firstOrNull()
                        activeDateForSheet = target
                    }
                )
            }
        } else {
            items(
                items = workedShifts,
                key = { it.id },
                contentType = { "worked_shift_row" }
            ) { shift ->
                WorkedShiftRow(
                    shift = shift,
                    eurRate = uiState.settings.eurRate,
                    isBest = summary.bestShift?.id == shift.id,
                    onClick = {
                        activeShiftForSheet = shift
                        activeDateForSheet = shift.date
                    },
                    onDeleteClick = {
                        if (!shift.isPlanned && shift.tipsRsd > 0.0) {
                            shiftPendingDelete = shift
                        } else {
                            onDeleteShift(shift.id, false)
                        }
                    }
                )
            }
        }
    }

    // Contextual Day Bottom Sheet
    activeDateForSheet?.let { dateStr ->
        ShiftDayBottomSheet(
            date = dateStr,
            existingShift = activeShiftForSheet,
            defaultHourlyRate = uiState.settings.hourlyRate,
            presets = uiState.shiftPresets,
            eurRate = uiState.settings.eurRate,
            onDismiss = {
                activeDateForSheet = null
                activeShiftForSheet = null
            },
            onSave = { id, targetDate, start, end, rate, tips, isPlanned, usesDefault, note ->
                onSaveShift(id, targetDate, start, end, rate, tips, isPlanned, usesDefault, note)
                activeDateForSheet = null
                activeShiftForSheet = null
            },
            onRequestDelete = { shift ->
                activeDateForSheet = null
                activeShiftForSheet = null
                if (!shift.isPlanned && shift.tipsRsd > 0.0) {
                    shiftPendingDelete = shift
                } else {
                    onDeleteShift(shift.id, false)
                }
            }
        )
    }

    // Delete confirmation sheet when worked shift has linked tips
    shiftPendingDelete?.let { shift ->
        DeleteShiftTipsConfirmSheet(
            shift = shift,
            onDismiss = { shiftPendingDelete = null },
            onConfirm = { deleteTips ->
                onDeleteShift(shift.id, deleteTips)
                shiftPendingDelete = null
            }
        )
    }

    // Salary credit confirmation sheet
    salaryPeriodForSheet?.let { stat ->
        CreditSalarySheet(
            stat = stat,
            eurRate = uiState.settings.eurRate,
            onDismiss = { salaryPeriodForSheet = null },
            onConfirm = { amount, date, note ->
                onCreditSalary(amount, date, note)
                salaryPeriodForSheet = null
            }
        )
    }
}

private val PlannedDashPathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

@Composable
private fun ShiftCalendarCard(
    monthStr: String,
    shiftsByDate: Map<String, List<ShiftEntity>>,
    bestDayDate: String?,
    onDayClick: (String) -> Unit
) {
    val semantic = LocalSemanticColors.current
    val today = remember { DateTimeUtils.todayStr() }
    val daysCount = DateTimeUtils.daysInMonth(monthStr)
    val leadingBlanks = DateTimeUtils.firstDayOfWeekMon0(monthStr)
    val totalCells = leadingBlanks + daysCount
    val weeks = (totalCells + 6) / 7

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateTimeUtils.formatMonthLabel(monthStr),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendDot(color = semantic.income, label = "Смена")
                    LegendDot(color = semantic.plannedSky, label = "План")
                    LegendDot(color = semantic.tipsGold, label = "Лучший")
                }
            }

            Spacer(Modifier.height(10.dp))

            // Weekday Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                DateTimeUtils.weekdayNamesShort.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            for (week in 0 until weeks) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0..6) {
                        val cellIndex = week * 7 + col
                        val dayNum = cellIndex - leadingBlanks + 1
                        if (dayNum < 1 || dayNum > daysCount) {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(0.9f))
                        } else {
                            val dayStr = if (dayNum < 10) "0$dayNum" else dayNum.toString()
                            val dateStr = "$monthStr-$dayStr"
                            val dayShifts = shiftsByDate[dateStr].orEmpty()
                            val worked = dayShifts.firstOrNull { !it.isPlanned }
                            val planned = dayShifts.firstOrNull { it.isPlanned }
                            val isToday = dateStr == today
                            val isBest = dateStr == bestDayDate

                            CalendarDayCell(
                                day = dayNum,
                                workedShift = worked,
                                plannedShift = planned,
                                isToday = isToday,
                                isBestDay = isBest,
                                onClick = { onDayClick(dateStr) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    workedShift: ShiftEntity?,
    plannedShift: ShiftEntity?,
    isToday: Boolean,
    isBestDay: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = when {
        workedShift != null -> semantic.incomeContainer.copy(alpha = 0.72f)
        plannedShift != null -> semantic.plannedSkyContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)
    }

    val plannedColor = semantic.plannedSky
    val goldColor = semantic.tipsGold
    val hasDashedPlan = plannedShift != null && workedShift == null

    Box(
        modifier = modifier
            .aspectRatio(0.86f)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (hasDashedPlan || isToday || isBestDay) {
                    Modifier.drawBehind {
                        val cr = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        when {
                            hasDashedPlan -> drawRoundRect(
                                color = plannedColor,
                                cornerRadius = cr,
                                style = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PlannedDashPathEffect
                                )
                            )
                            isToday -> drawRoundRect(
                                color = primaryColor,
                                cornerRadius = cr,
                                style = Stroke(width = 1.8.dp.toPx())
                            )
                            isBestDay -> drawRoundRect(
                                color = goldColor,
                                cornerRadius = cr,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                } else Modifier
            )
            .clickable(onClick = onClick)
            .testTag("calendar_day_$day"),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle).copy(
                        fontWeight = if (isToday || workedShift != null) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (isBestDay) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Лучший день",
                        tint = semantic.tipsGold,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            val activeShift = workedShift ?: plannedShift
            if (activeShift != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = DateTimeUtils.formatHours(activeShift.durationHours).replace(" ", ""),
                        style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle).copy(fontSize = 10.sp),
                        color = if (workedShift != null) semantic.income else semantic.plannedSky,
                        maxLines = 1
                    )
                    if (workedShift != null && workedShift.tipsRsd > 0.0) {
                        Text(
                            text = "+${CurrencyFormatter.formatCompactRsd(workedShift.tipsRsd)}",
                            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle).copy(fontSize = 10.sp),
                            color = semantic.tipsGold,
                            maxLines = 1
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ShiftSummaryCard(
    summary: com.example.ui.viewmodel.ShiftMonthSummary,
    eurRate: Double,
    onCreditSalaryClick: (PaydayPeriodStat) -> Unit
) {
    val semantic = LocalSemanticColors.current
    val avg8hRounded = summary.avgPayPer8hShift.roundToLong().toDouble()
    val avg8hEur = if (eurRate > 0.0) avg8hRounded / eurRate else 0.0
    val avg8hEurFormatted = String.format(java.util.Locale.US, "%.1f", avg8hEur).replace('.', ',')
    val monthWithPlanRounded = summary.monthWithPlanRsd.roundToLong().toDouble()

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + plan pill + 8h estimate
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Сводка и выплаты",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (summary.plannedCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = semantic.plannedSkyContainer.copy(alpha = 0.55f)
                        ) {
                            Text(
                                text = "План: ${summary.plannedCount} см. · ${DateTimeUtils.formatHours(summary.plannedHours)}",
                                style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                                color = semantic.plannedSky,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "8 ч ≈ ${CurrencyFormatter.formatRsd(avg8hRounded)} ($avg8hEurFormatted €)",
                    style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle).copy(fontWeight = FontWeight.SemiBold),
                    color = semantic.income
                )
            }

            // 2x2 Metric Grid: СМЕНЫ (ФАКТ), ЗАРАБОТАНО, МЕСЯЦ С ПЛАНОМ, СТАВКА С ЧАЕМ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: СМЕНЫ (ФАКТ)
                SummaryMetricTile(
                    label = "СМЕНЫ (ФАКТ)",
                    value = "${summary.workedCount} · ${DateTimeUtils.formatHours(summary.totalHours)}",
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    subContent = {
                        Text(
                            text = if (summary.plannedCount > 0) {
                                "План: +${summary.plannedCount} см. (${DateTimeUtils.formatHours(summary.plannedHours)})"
                            } else {
                                "Без плановых смен"
                            },
                            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                            color = if (summary.plannedCount > 0) semantic.plannedSky else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                // Card 2: ЗАРАБОТАНО (base and tips on separate clean lines so tips never truncate)
                SummaryMetricTile(
                    label = "ЗАРАБОТАНО",
                    value = CurrencyFormatter.formatRsd(summary.totalEarningsRsd),
                    valueColor = semantic.income,
                    subContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "База: ${CurrencyFormatter.formatRsd(summary.basePayRsd)}",
                                style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                                color = semantic.plannedSky,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Чай: +${CurrencyFormatter.formatRsd(summary.tipsRsd)}",
                                style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle).copy(fontWeight = FontWeight.SemiBold),
                                color = semantic.tipsGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 3: МЕСЯЦ С ПЛАНОМ (Potential Monthly Income)
                SummaryMetricTile(
                    label = "МЕСЯЦ С ПЛАНОМ",
                    value = CurrencyFormatter.formatRsd(monthWithPlanRounded),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    containerColor = semantic.plannedSkyContainer.copy(alpha = 0.28f),
                    borderColor = semantic.plannedSky.copy(alpha = 0.35f),
                    subContent = {
                        Text(
                            text = CurrencyFormatter.formatEurApprox(monthWithPlanRounded, eurRate),
                            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                            color = semantic.plannedSky,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                // Card 4: СТАВКА С ЧАЕМ
                SummaryMetricTile(
                    label = "СТАВКА С ЧАЕМ",
                    value = "${CurrencyFormatter.formatRsd(summary.effectiveHourlyRate.roundToLong().toDouble(), withUnit = false)} RSD/ч",
                    valueColor = semantic.tipsGold,
                    subContent = {
                        Text(
                            text = "Чай: +${CurrencyFormatter.formatRsd(summary.tipsPerHourRsd.roundToLong().toDouble(), withUnit = false)} RSD/ч",
                            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // Two Payday Period Cards inside the unified card
            summary.payday1?.let { p1 ->
                PaydayPeriodCard(
                    stat = p1,
                    onCreditClick = { onCreditSalaryClick(p1) }
                )
            }
            summary.payday2?.let { p2 ->
                PaydayPeriodCard(
                    stat = p2,
                    onCreditClick = { onCreditSalaryClick(p2) }
                )
            }

            // Weekday Average Tips Strip inside the unified card
            WeekdayTipsBarCard(
                stats = summary.weekdayAvgTips
            )
        }
    }
}

@Composable
private fun SummaryMetricTile(
    label: String,
    value: String,
    valueColor: Color,
    subContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle).copy(fontWeight = FontWeight.Bold),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subContent()
        }
    }
}

@Composable
private fun WeekdayTipsBarCard(
    stats: List<WeekdayTipStat>
) {
    val semantic = LocalSemanticColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Чай по дням",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stats.forEach { day ->
                    val isHighlight = day.isBest && day.avgTipsRsd > 0.0
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isHighlight) {
                            semantic.tipsGoldContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isHighlight) semantic.tipsGold.copy(alpha = 0.65f)
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = day.shortName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = if (day.avgTipsRsd > 0.0) {
                                    "+${day.avgTipsRsd.roundToLong()}"
                                } else {
                                    "—"
                                },
                                style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle).copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (day.avgTipsRsd > 0.0) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (day.avgTipsRsd > 0.0) semantic.tipsGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = if (day.shiftCount > 0) "${day.shiftCount} см." else " ",
                                style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle).copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaydayPeriodCard(
    stat: PaydayPeriodStat,
    onCreditClick: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val accentColor = if (stat.isSecondHalf) semantic.income else semantic.plannedSky
    val accentContainer = if (stat.isSecondHalf) {
        semantic.incomeContainer.copy(alpha = 0.4f)
    } else {
        semantic.plannedSkyContainer.copy(alpha = 0.35f)
    }

    val forecastBaseRounded = stat.forecastBasePayRsd.roundToLong().toDouble()
    val forecastTipsRounded = stat.forecastTipsRsd.roundToLong().toDouble()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top header: single clean row with period/payout on left, worked shifts/hours on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stat.title,
                        style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle).copy(fontWeight = FontWeight.Bold),
                        color = accentColor,
                        maxLines = 1
                    )
                    Text(
                        text = stat.payoutSubLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${stat.workedCount} см. · ${DateTimeUtils.formatHours(stat.workedHours)}",
                    style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Middle row: Base pay + Tips stacked cleanly on left, Action button on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = CurrencyFormatter.formatRsd(stat.workedBasePayRsd),
                        style = MaterialTheme.typography.titleLarge.merge(TabularNumberStyle).copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "Чай: +${CurrencyFormatter.formatRsd(stat.workedTipsRsd)}",
                        style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle).copy(fontWeight = FontWeight.SemiBold),
                        color = semantic.tipsGold,
                        maxLines = 1
                    )
                }

                Surface(
                    onClick = onCreditClick,
                    shape = RoundedCornerShape(14.dp),
                    color = accentContainer,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "+ Зачислить ЗП",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // Full-width bottom forecast line (never squeezed next to the button)
            val forecastBaseText = CurrencyFormatter.formatRsd(forecastBaseRounded).replace(' ', '\u00A0')
            val forecastTipsText = CurrencyFormatter.formatRsd(forecastTipsRounded).replace(' ', '\u00A0')
            val planHoursText = if (stat.plannedHours > 0.0) {
                " · план +${DateTimeUtils.formatHours(stat.plannedHours).replace(' ', '\u00A0')}"
            } else ""
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append("Прогноз: ")
                    }
                    withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) {
                        append(forecastBaseText)
                    }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append(" (+чай ≈ $forecastTipsText$planHoursText)")
                    }
                },
                style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun WorkedShiftRow(
    shift: ShiftEntity,
    eurRate: Double,
    isBest: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val total = shift.basePay + shift.tipsRsd
    val dayStr = remember(shift.date) {
        val d = DateTimeUtils.parseDay(shift.date)
        val m = DateTimeUtils.parseMonth(shift.date)
        "${if (d < 10) "0$d" else d}.${if (m < 10) "0$m" else m}"
    }
    val rateShort = CurrencyFormatter.formatRsd(shift.hourlyRate, withUnit = false)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            if (isBest) semantic.tipsGold.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Date + Hours Badge ("18.11" / "8 ч")
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = semantic.tipsGoldContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, semantic.tipsGold.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayStr,
                        style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle).copy(fontWeight = FontWeight.Bold),
                        color = semantic.tipsGold,
                        maxLines = 1
                    )
                    Text(
                        text = DateTimeUtils.formatHours(shift.durationHours),
                        style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle).copy(fontSize = 10.sp),
                        color = semantic.tipsGold.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Row 1: Time + hourly rate + star on left | Total earned + Delete on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "${shift.startTime}–${shift.endTime}",
                            style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle).copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "· $rateShort/ч",
                            style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        if (isBest) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Лучшая смена",
                                tint = semantic.tipsGold,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = CurrencyFormatter.formatRsd(total),
                            style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle).copy(fontWeight = FontWeight.Bold),
                            color = semantic.income,
                            maxLines = 1
                        )
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить смену",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Row 2: Full-width Base + Tips breakdown (never squished or truncated)
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = semantic.plannedSky, fontWeight = FontWeight.Medium)) {
                            append("База ${CurrencyFormatter.formatRsd(shift.basePay)}")
                        }
                        if (shift.tipsRsd > 0.0) {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                append(" · ")
                            }
                            withStyle(SpanStyle(color = semantic.tipsGold, fontWeight = FontWeight.SemiBold)) {
                                append("Чай +${CurrencyFormatter.formatRsd(shift.tipsRsd)}")
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftDayBottomSheet(
    date: String,
    existingShift: ShiftEntity?,
    defaultHourlyRate: Double,
    presets: List<ShiftPresetEntity>,
    eurRate: Double,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        date: String,
        startTime: String,
        endTime: String,
        hourlyRate: Double,
        tipsRsd: Double,
        isPlanned: Boolean,
        usesDefaultRate: Boolean,
        note: String
    ) -> Unit,
    onRequestDelete: (ShiftEntity) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val semantic = LocalSemanticColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var targetDate by remember { mutableStateOf(date) }
    var isPlanned by remember { mutableStateOf(existingShift?.isPlanned ?: (date > DateTimeUtils.todayStr())) }
    var startTime by remember { mutableStateOf(existingShift?.startTime ?: presets.firstOrNull()?.startTime ?: "17:00") }
    var endTime by remember { mutableStateOf(existingShift?.endTime ?: presets.firstOrNull()?.endTime ?: "01:00") }
    var rateInput by remember {
        val r = existingShift?.hourlyRate ?: defaultHourlyRate
        mutableStateOf(if (r % 1.0 == 0.0) r.toInt().toString() else r.toString())
    }
    var tipsInput by remember {
        val t = existingShift?.tipsRsd ?: 0.0
        mutableStateOf(if (t > 0.0) (if (t % 1.0 == 0.0) t.toInt().toString() else t.toString()) else "")
    }
    var tipsCurrency by remember { mutableStateOf(InputCurrency.RSD) }
    var note by remember { mutableStateOf(existingShift?.note ?: "") }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val computedHours = DateTimeUtils.calculateShiftHours(startTime, endTime)
    val parsedRate = CurrencyFormatter.parseAmount(rateInput) ?: defaultHourlyRate
    val basePay = CurrencyFormatter.roundRsd(computedHours * parsedRate)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateTimeUtils.formatDateHuman(targetDate),
                    style = MaterialTheme.typography.headlineSmall
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(targetDate, style = MaterialTheme.typography.labelMedium)
                }
            }

            if (existingShift != null && existingShift.isPlanned) {
                Surface(
                    onClick = { isPlanned = false },
                    shape = RoundedCornerShape(14.dp),
                    color = semantic.incomeContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, semantic.income.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = semantic.income)
                        Text(
                            text = "Отметить как отработанную",
                            style = MaterialTheme.typography.titleSmall,
                            color = semantic.income
                        )
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = isPlanned,
                    onClick = { isPlanned = true },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Запланировать") }
                SegmentedButton(
                    selected = !isPlanned,
                    onClick = { isPlanned = false },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Отработанная") }
            }

            // Presets
            if (presets.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { p ->
                        val selected = startTime == p.startTime && endTime == p.endTime
                        FilterChip(
                            selected = selected,
                            onClick = {
                                startTime = p.startTime
                                endTime = p.endTime
                            },
                            label = {
                                Text("${p.startTime}–${p.endTime}", style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle))
                            }
                        )
                    }
                }
            }

            // Custom time buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartTimePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Начало: $startTime")
                }
                OutlinedButton(
                    onClick = { showEndTimePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Конец: $endTime")
                }
            }

            Text(
                text = "Длительность: ${DateTimeUtils.formatHours(computedHours)} · База: ${CurrencyFormatter.formatRsd(basePay)}",
                style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = rateInput,
                onValueChange = { rateInput = CurrencyFormatter.sanitizeDecimalInput(it) },
                label = { Text("Ставка (RSD / час)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (!isPlanned) {
                CurrencyAmountField(
                    value = tipsInput,
                    onValueChange = { tipsInput = it },
                    currency = tipsCurrency,
                    onCurrencyChange = { tipsCurrency = it },
                    eurRate = eurRate,
                    label = "Чаевые"
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Заметка") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (existingShift != null) {
                    OutlinedButton(
                        onClick = { onRequestDelete(existingShift) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.expense),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Удалить")
                    }
                }

                Button(
                    onClick = {
                        val tipsRsd = if (!isPlanned && tipsInput.isNotBlank()) {
                            resolveRsdAndNote(tipsInput, tipsCurrency, eurRate, "").first ?: 0.0
                        } else 0.0
                        val usesDefault = kotlin.math.abs(parsedRate - defaultHourlyRate) < 0.01
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(
                            existingShift?.id,
                            targetDate,
                            startTime,
                            endTime,
                            parsedRate,
                            tipsRsd,
                            isPlanned,
                            usesDefault,
                            note
                        )
                    },
                    enabled = computedHours > 0.0,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сохранить")
                }
            }
        }
    }

    if (showStartTimePicker) {
        NativeTimePickerDialog(
            title = "Начало смены",
            initialTime = startTime,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { picked ->
                startTime = picked
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        NativeTimePickerDialog(
            title = "Окончание смены",
            initialTime = endTime,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { picked ->
                endTime = picked
                showEndTimePicker = false
            }
        )
    }

    if (showDatePicker) {
        NativeDatePickerModal(
            initialDate = targetDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                targetDate = picked
                showDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteShiftTipsConfirmSheet(
    shift: ShiftEntity,
    onDismiss: () -> Unit,
    onConfirm: (deleteLinkedTips: Boolean) -> Unit
) {
    val semantic = LocalSemanticColors.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Удалить смену?", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Удалить также запись чаевых (${CurrencyFormatter.formatRsd(shift.tipsRsd)})?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { onConfirm(true) },
                colors = ButtonDefaults.buttonColors(containerColor = semantic.expense),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Удалить с чаевыми")
            }
            OutlinedButton(
                onClick = { onConfirm(false) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Оставить чаевые")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreditSalarySheet(
    stat: PaydayPeriodStat,
    eurRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (amountRsd: Double, date: String, note: String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var amountInput by remember {
        val base = stat.workedBasePayRsd
        mutableStateOf(
            if (base > 0.0) (if (base % 1.0 == 0.0) base.toInt().toString() else base.toString()) else ""
        )
    }
    var currency by remember { mutableStateOf(InputCurrency.RSD) }
    var date by remember { mutableStateOf(stat.paydayDate) }
    var note by remember { mutableStateOf("ЗП (${stat.dateRangeLabel})") }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Зачислить зарплату", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stat.dateRangeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CurrencyAmountField(
                value = amountInput,
                onValueChange = { amountInput = it },
                currency = currency,
                onCurrencyChange = { currency = it },
                eurRate = eurRate,
                label = "Сумма"
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Дата: $date")
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Заметка") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            val (rsdPreview, _) = resolveRsdAndNote(amountInput, currency, eurRate, note)
            Button(
                onClick = {
                    val (rsd, finalNote) = resolveRsdAndNote(amountInput, currency, eurRate, note)
                    if (rsd != null && rsd > 0.0) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(rsd, date, finalNote)
                    }
                },
                enabled = rsdPreview != null && rsdPreview > 0.0,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Зачислить")
            }
        }
    }

    if (showDatePicker) {
        NativeDatePickerModal(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                date = picked
                showDatePicker = false
            }
        )
    }
}
