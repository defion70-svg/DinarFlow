package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.CategoryEntity
import com.example.ui.theme.LocalSemanticColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.viewmodel.PeriodMode
import com.example.ui.viewmodel.PeriodSummary
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import com.example.util.InputCurrency

val CuratedCategoryEmojis = listOf(
    "🛒", "🏠", "☕", "🐾", "🎮", "🚕", "🎯", "💼", "🪙", "💻",
    "🍔", "🍕", "💊", "🎬", "✈️", "👕", "📱", "⚡", "💧", "🔥",
    "🎁", "📚", "🏋️", "💈", "🛠️", "🚗", "🚌", "🎵", "🌿", "🧾"
)

val CuratedAccentColors = listOf(
    0xFF10B981L, 0xFF3B82F6L, 0xFFF59E0BL, 0xFFEC4899L,
    0xFF8B5CF6L, 0xFF06B6D4L, 0xFF14B8A6L, 0xFF6366F1L,
    0xFFEF4444L, 0xFF84CC16L
)

@Composable
fun DinarFlowTopBar(
    periodSummary: PeriodSummary,
    onShiftMonth: (Int) -> Unit,
    onSelectPeriodMode: (PeriodMode) -> Unit,
    onPickCustomRange: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPeriodMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)) {
                    append("Dinar")
                }
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append("Flow")
                }
            },
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (periodSummary.mode == PeriodMode.MONTH) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(40.dp)
                    ) {
                        IconButton(
                            onClick = { onShiftMonth(-1) },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("month_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Предыдущий месяц",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = DateTimeUtils.formatMonthLabel(periodSummary.selectedMonth),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clickable { showPeriodMenu = true }
                                .padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = { onShiftMonth(1) },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("month_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Следующий месяц",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    onClick = {
                        if (periodSummary.mode == PeriodMode.RANGE) onPickCustomRange()
                        else showPeriodMenu = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (periodSummary.mode == PeriodMode.RANGE) {
                                "${DateTimeUtils.formatShortDayMonth(periodSummary.rangeStart)} – ${DateTimeUtils.formatShortDayMonth(periodSummary.rangeEnd)}"
                            } else {
                                "Всё время"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Box {
                IconButton(
                    onClick = { showPeriodMenu = true },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("period_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Выбор периода",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showPeriodMenu,
                    onDismissRequest = { showPeriodMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Месяц") },
                        trailingIcon = {
                            if (periodSummary.mode == PeriodMode.MONTH) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            showPeriodMenu = false
                            onSelectPeriodMode(PeriodMode.MONTH)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Период (С – По)") },
                        trailingIcon = {
                            if (periodSummary.mode == PeriodMode.RANGE) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            showPeriodMenu = false
                            onPickCustomRange()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Всё время") },
                        trailingIcon = {
                            if (periodSummary.mode == PeriodMode.ALL_TIME) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            showPeriodMenu = false
                            onSelectPeriodMode(PeriodMode.ALL_TIME)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CompactPeriodSummaryCard(
    summary: PeriodSummary,
    eurRate: Double,
    onToggleCarryover: (Boolean) -> Unit,
    isCollapsed: Boolean = false,
    onExpandClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val cornerRadius by animateDpAsState(
        targetValue = if (isCollapsed) 14.dp else 22.dp,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "balance_corner_radius"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(
                if (isCollapsed) Modifier.clickable { onExpandClick() } else Modifier
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (isCollapsed) 0.28f else 0.22f)
        )
    ) {
        AnimatedContent(
            targetState = isCollapsed,
            contentAlignment = Alignment.TopStart,
            transitionSpec = {
                (fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(animationSpec = tween(130, easing = FastOutLinearInEasing)))
                    .using(
                        SizeTransform(
                            clip = true,
                            sizeAnimationSpec = { _, _ ->
                                tween(durationMillis = 240, easing = FastOutSlowInEasing)
                            }
                        )
                    )
            },
            label = "balance_collapse_transition"
        ) { collapsed ->
            if (collapsed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Баланс",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = CurrencyFormatter.formatRsd(summary.displayedBalanceRsd),
                            style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle).copy(fontWeight = FontWeight.Bold),
                            color = if (summary.displayedBalanceRsd < -0.004) {
                                semantic.expense
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Развернуть баланс",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Баланс",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.formatRsd(summary.displayedBalanceRsd),
                            style = MaterialTheme.typography.headlineMedium.merge(TabularNumberStyle),
                            color = if (summary.displayedBalanceRsd < -0.004) {
                                semantic.expense
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = CurrencyFormatter.formatEurApprox(summary.displayedBalanceRsd, eurRate),
                            style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                            color = semantic.tipsGold
                        )
                    }

                    // Carryover switch
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "С переносом",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Switch(
                                checked = summary.carryoverEnabled,
                                onCheckedChange = onToggleCarryover,
                                modifier = Modifier.testTag("carryover_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Income / Expense high-contrast two-column strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = semantic.incomeContainer.copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, semantic.income.copy(alpha = 0.42f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = semantic.income,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Доходы",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = semantic.income.copy(alpha = 0.9f)
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = CurrencyFormatter.formatRsd(summary.incomeRsd, showSign = true),
                                style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle).copy(fontWeight = FontWeight.Bold),
                                color = semantic.income,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = CurrencyFormatter.formatEurApprox(summary.incomeRsd, eurRate),
                                style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = semantic.expenseContainer.copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, semantic.expense.copy(alpha = 0.42f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = semantic.expense,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Траты",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = semantic.expense.copy(alpha = 0.9f)
                                    )
                                }
                                if (summary.incomeRsd > 0.0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = semantic.expense.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "${summary.expensePercentOfIncome}%",
                                            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle).copy(fontWeight = FontWeight.Bold),
                                            color = semantic.expense,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (summary.expenseRsd > 0.0) {
                                    "−${CurrencyFormatter.formatRsd(summary.expenseRsd)}"
                                } else {
                                    CurrencyFormatter.formatRsd(0.0)
                                },
                                style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle).copy(fontWeight = FontWeight.Bold),
                                color = semantic.expense,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = CurrencyFormatter.formatEurApprox(summary.expenseRsd, eurRate),
                                style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = summary.carryoverEnabled && summary.mode != PeriodMode.ALL_TIME) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(6.dp))
                        val periodLabel = if (summary.mode == PeriodMode.MONTH) "Текущий месяц" else "Период"
                        Text(
                            text = "Прошл.: ${CurrencyFormatter.formatRsd(summary.priorNetRsd, showSign = true)} · $periodLabel: ${CurrencyFormatter.formatRsd(summary.currentNetRsd, showSign = true)}",
                            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
fun CurrencyAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    currency: InputCurrency,
    onCurrencyChange: (InputCurrency) -> Unit,
    eurRate: Double,
    label: String = "Сумма",
    testTag: String = "amount_input",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { raw ->
                onValueChange(CurrencyFormatter.sanitizeDecimalInput(raw))
            },
            label = { Text(label) },
            placeholder = { Text("0") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
            trailingIcon = {
                Surface(
                    onClick = {
                        onCurrencyChange(
                            if (currency == InputCurrency.RSD) InputCurrency.EUR else InputCurrency.RSD
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (currency == InputCurrency.EUR) {
                        LocalSemanticColors.current.tipsGoldContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .testTag("${testTag}_currency_toggle")
                ) {
                    Text(
                        text = if (currency == InputCurrency.RSD) "RSD" else "EUR €",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (currency == InputCurrency.EUR) {
                            LocalSemanticColors.current.tipsGold
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )

        val parsed = CurrencyFormatter.parseAmount(value)
        if (parsed != null && parsed > 0.0) {
            Spacer(Modifier.height(4.dp))
            if (currency == InputCurrency.EUR) {
                val rsdVal = CurrencyFormatter.toRsd(parsed, eurRate)
                Text(
                    text = "= ${CurrencyFormatter.formatRsd(rsdVal)} (курс 1 € = ${CurrencyFormatter.formatRsd(eurRate, withUnit = false)} RSD)",
                    style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = CurrencyFormatter.formatEurApprox(parsed, eurRate),
                    style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun resolveRsdAndNote(
    rawInput: String,
    currency: InputCurrency,
    eurRate: Double,
    rawNote: String
): Pair<Double?, String> {
    val parsed = CurrencyFormatter.parseAmount(rawInput) ?: return null to rawNote
    if (parsed <= 0.0) return null to rawNote
    return if (currency == InputCurrency.EUR) {
        val rsd = CurrencyFormatter.toRsd(parsed, eurRate)
        val noteWithEur = CurrencyFormatter.appendEurNote(rawNote, parsed)
        rsd to noteWithEur
    } else {
        parsed to rawNote.trim()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChipGrid(
    categories: List<CategoryEntity>,
    selectedCategoryId: String,
    onSelectCategory: (CategoryEntity) -> Unit,
    horizontalScroll: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (horizontalScroll) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                CategorySelectChip(
                    category = cat,
                    selected = cat.id == selectedCategoryId,
                    onClick = { onSelectCategory(cat) }
                )
            }
        }
    } else {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                CategorySelectChip(
                    category = cat,
                    selected = cat.id == selectedCategoryId,
                    onClick = { onSelectCategory(cat) }
                )
            }
        }
    }
}

@Composable
private fun CategorySelectChip(
    category: CategoryEntity,
    selected: Boolean,
    onClick: () -> Unit
) {
    val catColor = Color(category.colorHex)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = "${category.emoji} ${category.name}",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = catColor.copy(alpha = 0.22f),
            selectedLabelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = catColor,
            borderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.testTag("cat_chip_${category.id}")
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun darkCalendarPickerColors(): DatePickerColors {
    val darkBg = Color(0xFF0F172A)
    val onDark = Color(0xFFF1F5F9)
    val onDarkMuted = Color(0xFF94A3B8)
    val accentBlue = Color(0xFF38BDF8)
    val onAccent = Color(0xFF041C2E)
    return DatePickerDefaults.colors(
        containerColor = darkBg,
        titleContentColor = onDarkMuted,
        headlineContentColor = onDark,
        weekdayContentColor = onDarkMuted,
        subheadContentColor = onDark,
        navigationContentColor = onDark,
        yearContentColor = onDark,
        disabledYearContentColor = onDarkMuted.copy(alpha = 0.4f),
        currentYearContentColor = accentBlue,
        selectedYearContentColor = onAccent,
        disabledSelectedYearContentColor = onDarkMuted,
        selectedYearContainerColor = accentBlue,
        disabledSelectedYearContainerColor = darkBg,
        dayContentColor = onDark,
        disabledDayContentColor = onDarkMuted.copy(alpha = 0.4f),
        selectedDayContentColor = onAccent,
        disabledSelectedDayContentColor = onDarkMuted,
        selectedDayContainerColor = accentBlue,
        disabledSelectedDayContainerColor = darkBg,
        todayContentColor = accentBlue,
        todayDateBorderColor = accentBlue,
        dayInSelectionRangeContentColor = onDark,
        dayInSelectionRangeContainerColor = accentBlue.copy(alpha = 0.22f),
        dividerColor = Color(0xFF1E2D4A)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeDatePickerModal(
    initialDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initialMillis = remember(initialDate) { DateTimeUtils.dateToEpochMillis(initialDate) }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    val pickerColors = darkCalendarPickerColors()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = pickerColors,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: initialMillis
                    onConfirm(DateTimeUtils.epochMillisToDate(millis))
                }
            ) {
                Text("Выбрать", color = Color(0xFF38BDF8))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFF94A3B8))
            }
        }
    ) {
        DatePicker(
            state = state,
            colors = pickerColors
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeDateRangePickerModal(
    initialStart: String,
    initialEnd: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = if (initialStart.isNotEmpty()) DateTimeUtils.dateToEpochMillis(initialStart) else null,
        initialSelectedEndDateMillis = if (initialEnd.isNotEmpty()) DateTimeUtils.dateToEpochMillis(initialEnd) else null
    )
    val pickerColors = darkCalendarPickerColors()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = pickerColors,
        confirmButton = {
            TextButton(
                onClick = {
                    val s = state.selectedStartDateMillis?.let { DateTimeUtils.epochMillisToDate(it) }
                        ?: DateTimeUtils.todayStr()
                    val e = state.selectedEndDateMillis?.let { DateTimeUtils.epochMillisToDate(it) } ?: s
                    onConfirm(s, e)
                }
            ) {
                Text("Применить", color = Color(0xFF38BDF8))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFF94A3B8))
            }
        }
    ) {
        val startText = state.selectedStartDateMillis?.let {
            DateTimeUtils.formatShortDayMonth(DateTimeUtils.epochMillisToDate(it))
        } ?: "Начало"
        val endText = state.selectedEndDateMillis?.let {
            DateTimeUtils.formatShortDayMonth(DateTimeUtils.epochMillisToDate(it))
        } ?: "Конец"

        DateRangePicker(
            state = state,
            colors = pickerColors,
            title = {
                Text(
                    text = "Выберите период",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            },
            headline = {
                Text(
                    text = "$startText – $endText",
                    style = MaterialTheme.typography.titleLarge.merge(TabularNumberStyle),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp)
                )
            },
            modifier = Modifier.height(460.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeTimePickerDialog(
    title: String,
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val initHour = parts.getOrNull(0)?.toIntOrNull() ?: 17
    val initMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val state = rememberTimePickerState(
        initialHour = initHour.coerceIn(0, 23),
        initialMinute = initMin.coerceIn(0, 59),
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(16.dp))
                TimePicker(state = state)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onConfirm(DateTimeUtils.formatTime(state.hour, state.minute))
                        }
                    ) {
                        Text("Готово")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmojiPickerFlow(
    selectedEmoji: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CuratedCategoryEmojis.forEach { emoji ->
            val selected = emoji == selectedEmoji
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSwatchPicker(
    selectedColorHex: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CuratedAccentColors.forEach { hex ->
            val selected = hex == selectedColorHex
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(hex))
                    .border(
                        width = if (selected) 3.dp else 0.dp,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
