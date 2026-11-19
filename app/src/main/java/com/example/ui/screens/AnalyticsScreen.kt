package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryType
import com.example.ui.components.CurrencyAmountField
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.resolveRsdAndNote
import com.example.ui.theme.LocalSemanticColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.viewmodel.CategoryBreakdownItem
import com.example.ui.viewmodel.DinarFlowUiState
import com.example.util.CurrencyFormatter
import com.example.util.InputCurrency

@Composable
fun AnalyticsScreen(
    uiState: DinarFlowUiState,
    onUpdateCategoryLimit: (categoryId: String, limitRsd: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val analytics = uiState.analytics
    val eurRate = uiState.settings.eurRate
    var mode by rememberSaveable { mutableStateOf(CategoryType.EXPENSE) }
    var editingLimitCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    val rows = if (mode == CategoryType.EXPENSE) analytics.expenseRows else analytics.incomeRows

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "analytics_toggle", contentType = "analytics_toggle") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode == CategoryType.EXPENSE,
                    onClick = { mode = CategoryType.EXPENSE },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Расходы") }
                SegmentedButton(
                    selected = mode == CategoryType.INCOME,
                    onClick = { mode = CategoryType.INCOME },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Доходы") }
            }
        }

        // Overall Monthly Budgets Status (shown if at least 1 expense category has a limit)
        if (analytics.hasAnyLimits && mode == CategoryType.EXPENSE) {
            item(key = "budgets_overview", contentType = "budgets_overview") {
                val limitColor = when {
                    analytics.limitedUsagePercent >= 100 -> semantic.expense
                    analytics.limitedUsagePercent >= 80 -> semantic.warning
                    else -> semantic.income
                }
                val frac = (analytics.limitedUsagePercent / 100f).coerceIn(0f, 1f)

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Лимиты бюджета", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${analytics.limitedUsagePercent}%",
                                style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle),
                                color = limitColor
                            )
                        }

                        LinearProgressIndicator(
                            progress = { frac },
                            color = limitColor,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "В лимитах: ${CurrencyFormatter.formatRsd(analytics.totalLimitedSpentRsd)}",
                                style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle)
                            )
                            Text(
                                text = "Лимит: ${CurrencyFormatter.formatRsd(analytics.totalLimitRsd)}",
                                style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val rem = analytics.limitedRemainingRsd
                            Text(
                                text = if (rem >= 0.0) {
                                    "Остаток: ${CurrencyFormatter.formatRsd(rem)}"
                                } else {
                                    "Перерасход: ${CurrencyFormatter.formatRsd(-rem)}"
                                },
                                style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                                color = if (rem >= 0.0) semantic.income else semantic.expense
                            )
                            Text(
                                text = "Риск 80%+: ${analytics.warningCount} · Превышено: ${analytics.exceededCount}",
                                style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Key Period Metrics Grid
        item(key = "period_metrics", contentType = "period_metrics") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Показатели", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatTile(
                            label = "Расход / день",
                            value = CurrencyFormatter.formatRsd(analytics.avgDailyExpenseRsd),
                            sub = CurrencyFormatter.formatEurApprox(analytics.avgDailyExpenseRsd, eurRate),
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Сбережения",
                            value = "${analytics.savingsRatePercent}%",
                            sub = "от доходов",
                            valueColor = if (analytics.savingsRatePercent >= 0) semantic.income else semantic.expense,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatTile(
                            label = "Операций",
                            value = analytics.transactionCount.toString(),
                            sub = "за период",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Свободно",
                            value = CurrencyFormatter.formatRsd(uiState.billsSummary.freeBalanceRsd),
                            sub = "после счетов",
                            valueColor = if (uiState.billsSummary.freeBalanceRsd >= 0.0) semantic.income else semantic.expense,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Category Breakdown
        item(key = "breakdown_header", contentType = "breakdown_header") {
            Text(
                text = if (mode == CategoryType.EXPENSE) "Расходы по категориям" else "Доходы по категориям",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (rows.isEmpty()) {
            item(key = "analytics_empty", contentType = "analytics_empty") {
                EmptyStateCard(
                    icon = Icons.Default.PieChart,
                    title = "Нет данных",
                    subtitle = "Операции за период появятся здесь."
                )
            }
        } else {
            items(
                items = rows,
                key = { it.category.id },
                contentType = { "breakdown_card" }
            ) { row ->
                CategoryBreakdownCard(
                    item = row,
                    eurRate = eurRate,
                    showLimitConfig = mode == CategoryType.EXPENSE,
                    onEditLimit = { editingLimitCategory = row.category }
                )
            }
        }
    }

    editingLimitCategory?.let { cat ->
        CategoryLimitEditSheet(
            category = cat,
            eurRate = eurRate,
            onDismiss = { editingLimitCategory = null },
            onSave = { newLimit ->
                onUpdateCategoryLimit(cat.id, newLimit)
                editingLimitCategory = null
            }
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    item: CategoryBreakdownItem,
    eurRate: Double,
    showLimitConfig: Boolean,
    onEditLimit: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val catColor = Color(item.category.colorHex)

    Surface(
        onClick = { if (showLimitConfig) onEditLimit() },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(catColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = item.category.emoji, fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = item.category.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.sharePercent}% от суммы · ${item.transactionCount} оп.",
                            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatRsd(item.amountRsd),
                        style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle)
                    )
                    Text(
                        text = CurrencyFormatter.formatEurApprox(item.amountRsd, eurRate),
                        style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Share bar
            LinearProgressIndicator(
                progress = { item.shareFraction },
                color = catColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            if (showLimitConfig) {
                if (item.limitRsd > 0.0) {
                    val limitColor = when {
                        item.limitPercent >= 100 -> semantic.expense
                        item.limitPercent >= 80 -> semantic.warning
                        else -> semantic.income
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Лимит: ${CurrencyFormatter.formatRsd(item.limitRsd)} (${item.limitPercent}%)",
                            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                            color = limitColor
                        )
                        Text(
                            text = "Изменить",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { item.limitFraction },
                        color = limitColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = " Задать лимит",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryLimitEditSheet(
    category: CategoryEntity,
    eurRate: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var limitInput by remember {
        mutableStateOf(
            if (category.monthlyLimitRsd > 0.0) {
                if (category.monthlyLimitRsd % 1.0 == 0.0) category.monthlyLimitRsd.toInt().toString()
                else category.monthlyLimitRsd.toString()
            } else ""
        )
    }
    var currency by remember { mutableStateOf(InputCurrency.RSD) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("${category.emoji} Лимит: ${category.name}", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Месячный лимит (0 — без лимита).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CurrencyAmountField(
                value = limitInput,
                onValueChange = { limitInput = it },
                currency = currency,
                onCurrencyChange = { currency = it },
                eurRate = eurRate,
                label = "Месячный лимит"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onSave(0.0) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Без лимита")
                }
                Button(
                    onClick = {
                        val rsd = if (limitInput.isBlank()) 0.0 else {
                            resolveRsdAndNote(limitInput, currency, eurRate, "").first ?: 0.0
                        }
                        onSave(rsd)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}
