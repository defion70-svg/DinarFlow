package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.GoalEntity
import com.example.ui.components.ColorSwatchPicker
import com.example.ui.components.CuratedAccentColors
import com.example.ui.components.CurrencyAmountField
import com.example.ui.components.EmojiPickerFlow
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.NativeDatePickerModal
import com.example.ui.components.resolveRsdAndNote
import com.example.ui.theme.LocalSemanticColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.viewmodel.DinarFlowUiState
import com.example.ui.viewmodel.GoalUiItem
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import com.example.util.InputCurrency
import java.util.Locale

@Composable
fun GoalsScreen(
    uiState: DinarFlowUiState,
    onCreateGoal: (name: String, targetRsd: Double, initialSavedRsd: Double, emoji: String, colorHex: Long) -> Unit,
    onUpdateGoal: (goalId: String, name: String, targetRsd: Double, newSavedRsd: Double, emoji: String, colorHex: Long) -> Unit,
    onDepositToGoal: (goalId: String, amountRsd: Double, date: String, note: String) -> Unit,
    onWithdrawFromGoal: (goalId: String, amountRsd: Double) -> Unit,
    onDeleteGoal: (goalId: String, refundTransactions: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val eurRate = uiState.settings.eurRate

    var showCreateEditSheet by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var depositSheetGoal by remember { mutableStateOf<GoalUiItem?>(null) }
    var withdrawSheetGoal by remember { mutableStateOf<GoalUiItem?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Piggy Bank Summary
        item(key = "goals_header") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Накоплено в целях",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.formatRsd(uiState.totalGoalsSavedRsd),
                            style = MaterialTheme.typography.headlineMedium.merge(TabularNumberStyle),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = CurrencyFormatter.formatEurApprox(uiState.totalGoalsSavedRsd, eurRate),
                            style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                            color = LocalSemanticColors.current.tipsGold
                        )
                    }

                    Button(
                        onClick = {
                            editingGoal = null
                            showCreateEditSheet = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("add_goal_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Цель")
                    }
                }
            }
        }

        if (uiState.goals.isEmpty()) {
            item(key = "goals_empty") {
                EmptyStateCard(
                    icon = Icons.Default.Savings,
                    title = "Финансовых целей пока нет",
                    subtitle = "Создайте цель и отслеживайте прогресс в динарах, евро и оставшихся 8-часовых сменах.",
                    actionLabel = "+ Добавить цель",
                    onAction = {
                        editingGoal = null
                        showCreateEditSheet = true
                    }
                )
            }
        } else {
            items(items = uiState.goals, key = { it.goal.id }) { item ->
                GoalCard(
                    item = item,
                    eurRate = eurRate,
                    hourlyRate = uiState.settings.hourlyRate,
                    hasShiftTipsHistory = uiState.shiftMonthSummary.historicalAvgTipsPerHour > 0.0,
                    onQuickDeposit = { amountRsd, note ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDepositToGoal(item.goal.id, amountRsd, DateTimeUtils.todayStr(), note)
                    },
                    onOpenCustomDeposit = { depositSheetGoal = item },
                    onOpenWithdraw = { withdrawSheetGoal = item },
                    onEdit = {
                        editingGoal = item.goal
                        showCreateEditSheet = true
                    }
                )
            }
        }
    }

    if (showCreateEditSheet) {
        GoalCreateEditSheet(
            goal = editingGoal,
            eurRate = eurRate,
            onDismiss = {
                showCreateEditSheet = false
                editingGoal = null
            },
            onSave = { name, targetRsd, savedRsd, emoji, colorHex ->
                val existing = editingGoal
                if (existing == null) {
                    onCreateGoal(name, targetRsd, savedRsd, emoji, colorHex)
                } else {
                    onUpdateGoal(existing.id, name, targetRsd, savedRsd, emoji, colorHex)
                }
                showCreateEditSheet = false
                editingGoal = null
            },
            onDelete = editingGoal?.let { g ->
                { refund ->
                    onDeleteGoal(g.id, refund)
                    showCreateEditSheet = false
                    editingGoal = null
                }
            }
        )
    }

    depositSheetGoal?.let { item ->
        GoalDepositSheet(
            goal = item.goal,
            eurRate = eurRate,
            onDismiss = { depositSheetGoal = null },
            onConfirm = { amountRsd, date, note ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDepositToGoal(item.goal.id, amountRsd, date, note)
                depositSheetGoal = null
            }
        )
    }

    withdrawSheetGoal?.let { item ->
        GoalWithdrawSheet(
            goal = item.goal,
            eurRate = eurRate,
            onDismiss = { withdrawSheetGoal = null },
            onConfirm = { amountRsd ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onWithdrawFromGoal(item.goal.id, amountRsd)
                withdrawSheetGoal = null
            }
        )
    }
}

@Composable
private fun GoalCard(
    item: GoalUiItem,
    eurRate: Double,
    hourlyRate: Double,
    hasShiftTipsHistory: Boolean,
    onQuickDeposit: (amountRsd: Double, note: String) -> Unit,
    onOpenCustomDeposit: () -> Unit,
    onOpenWithdraw: () -> Unit,
    onEdit: () -> Unit
) {
    val goal = item.goal
    val accent = Color(goal.colorHex)
    val semantic = LocalSemanticColors.current

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = goal.emoji, fontSize = 21.sp)
                    }
                    Column {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.percent}% выполнено",
                            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                            color = accent
                        )
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать цель",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { item.progressFraction },
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )

            Column {
                Text(
                    text = "${CurrencyFormatter.formatRsd(goal.savedAmountRsd)} / ${CurrencyFormatter.formatRsd(goal.targetAmountRsd)}",
                    style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle)
                )
                Text(
                    text = if (item.remainingRsd > 0.0) {
                        "Осталось ${CurrencyFormatter.formatRsd(item.remainingRsd)} (${CurrencyFormatter.formatEurApprox(item.remainingRsd, eurRate)})"
                    } else {
                        "Цель достигнута!"
                    },
                    style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                    color = if (item.remainingRsd > 0.0) MaterialTheme.colorScheme.onSurfaceVariant else semantic.income
                )
            }

            // 8-hour shifts calculator
            if (item.remainingRsd > 0.0 && hourlyRate > 0.0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Сколько 8-ч смен осталось:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val exactBase = String.format(Locale.US, "%.1f", item.shiftsBaseExact).replace('.', ',')
                        Text(
                            text = "• По базовой ставке: ${item.shiftsBaseCeil} смен ($exactBase)",
                            style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle)
                        )
                        if (hasShiftTipsHistory) {
                            val exactTips = String.format(Locale.US, "%.1f", item.shiftsWithTipsExact).replace('.', ',')
                            Text(
                                text = "• Со средними чаевыми: ${item.shiftsWithTipsCeil} смен ($exactTips)",
                                style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                                color = semantic.tipsGold
                            )
                        }
                    }
                }
            }

            // Quick deposit chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickDepositPill(
                    label = "+2 000",
                    onClick = { onQuickDeposit(2000.0, "") },
                    modifier = Modifier.weight(1f)
                )
                QuickDepositPill(
                    label = "+5 000",
                    onClick = { onQuickDeposit(5000.0, "") },
                    modifier = Modifier.weight(1f)
                )
                QuickDepositPill(
                    label = "+50 €",
                    onClick = {
                        val rsd = CurrencyFormatter.toRsd(50.0, eurRate)
                        onQuickDeposit(rsd, "(50 €)")
                    },
                    isGold = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Custom deposit / LIFO withdraw
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenCustomDeposit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+ Внести")
                }
                OutlinedButton(
                    onClick = onOpenWithdraw,
                    enabled = goal.savedAmountRsd > 0.0,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("− Вернуть")
                }
            }
        }
    }
}

@Composable
private fun QuickDepositPill(
    label: String,
    onClick: () -> Unit,
    isGold: Boolean = false,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isGold) semantic.tipsGoldContainer.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (isGold) semantic.tipsGold.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                color = if (isGold) semantic.tipsGold else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalCreateEditSheet(
    goal: GoalEntity?,
    eurRate: Double,
    onDismiss: () -> Unit,
    onSave: (name: String, targetRsd: Double, savedRsd: Double, emoji: String, colorHex: Long) -> Unit,
    onDelete: ((refundTransactions: Boolean) -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var targetInput by remember {
        mutableStateOf(
            goal?.let {
                if (it.targetAmountRsd % 1.0 == 0.0) it.targetAmountRsd.toInt().toString()
                else it.targetAmountRsd.toString()
            } ?: ""
        )
    }
    var targetCurrency by remember { mutableStateOf(InputCurrency.RSD) }

    var savedInput by remember {
        mutableStateOf(
            goal?.let {
                if (it.savedAmountRsd > 0.0) {
                    if (it.savedAmountRsd % 1.0 == 0.0) it.savedAmountRsd.toInt().toString()
                    else it.savedAmountRsd.toString()
                } else ""
            } ?: ""
        )
    }
    var savedCurrency by remember { mutableStateOf(InputCurrency.RSD) }

    var emoji by remember { mutableStateOf(goal?.emoji ?: "🎯") }
    var colorHex by remember { mutableLongStateOf(goal?.colorHex ?: CuratedAccentColors.first()) }
    var refundOnDelete by remember { mutableStateOf(true) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (goal == null) "Новая финансовая цель" else "Редактировать цель",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название цели") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            CurrencyAmountField(
                value = targetInput,
                onValueChange = { targetInput = it },
                currency = targetCurrency,
                onCurrencyChange = { targetCurrency = it },
                eurRate = eurRate,
                label = "Целевая сумма"
            )

            CurrencyAmountField(
                value = savedInput,
                onValueChange = { savedInput = it },
                currency = savedCurrency,
                onCurrencyChange = { savedCurrency = it },
                eurRate = eurRate,
                label = "Уже накоплено (списывается как расход)"
            )

            Text("Иконка", style = MaterialTheme.typography.labelLarge)
            EmojiPickerFlow(selectedEmoji = emoji, onSelect = { emoji = it })

            Text("Цвет", style = MaterialTheme.typography.labelLarge)
            ColorSwatchPicker(selectedColorHex = colorHex, onSelect = { colorHex = it })

            if (onDelete != null) {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { refundOnDelete = !refundOnDelete },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = refundOnDelete,
                        onCheckedChange = { refundOnDelete = it }
                    )
                    Text(
                        text = "Вернуть накопленные взносы на баланс при удалении",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onDelete != null) {
                    TextButton(
                        onClick = { onDelete(refundOnDelete) },
                        colors = ButtonDefaults.textButtonColors(contentColor = LocalSemanticColors.current.expense)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Удалить")
                    }
                }
                Spacer(Modifier.weight(1f))
                val targetPreview = resolveRsdAndNote(targetInput, targetCurrency, eurRate, "").first
                Button(
                    onClick = {
                        val targetRsd = resolveRsdAndNote(targetInput, targetCurrency, eurRate, "").first
                        val savedRsd = if (savedInput.isBlank()) 0.0 else {
                            resolveRsdAndNote(savedInput, savedCurrency, eurRate, "").first ?: 0.0
                        }
                        if (name.isNotBlank() && targetRsd != null && targetRsd > 0.0) {
                            onSave(name, targetRsd, savedRsd, emoji, colorHex)
                        }
                    },
                    enabled = name.isNotBlank() && targetPreview != null && targetPreview > 0.0,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDepositSheet(
    goal: GoalEntity,
    eurRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (amountRsd: Double, date: String, note: String) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(InputCurrency.RSD) }
    var date by remember { mutableStateOf(DateTimeUtils.todayStr()) }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("${goal.emoji} Внести в «${goal.name}»", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Создаёт реальный расход в категории «🎯 Цели» на выбранную дату.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CurrencyAmountField(
                value = amountInput,
                onValueChange = { amountInput = it },
                currency = currency,
                onCurrencyChange = { currency = it },
                eurRate = eurRate
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Дата взноса: ${DateTimeUtils.formatDateHuman(date)} ($date)")
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Заметка (необязательно)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            val preview = resolveRsdAndNote(amountInput, currency, eurRate, note).first
            Button(
                onClick = {
                    val (rsd, finalNote) = resolveRsdAndNote(amountInput, currency, eurRate, note)
                    if (rsd != null && rsd > 0.0) {
                        onConfirm(rsd, date, finalNote)
                    }
                },
                enabled = preview != null && preview > 0.0,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Внести")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalWithdrawSheet(
    goal: GoalEntity,
    eurRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (amountRsd: Double) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(InputCurrency.RSD) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("${goal.emoji} Вернуть из цели", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Доступно: ${CurrencyFormatter.formatRsd(goal.savedAmountRsd)}. Списание происходит с самых свежих взносов (LIFO) и возвращает деньги на баланс.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CurrencyAmountField(
                value = amountInput,
                onValueChange = { amountInput = it },
                currency = currency,
                onCurrencyChange = { currency = it },
                eurRate = eurRate
            )

            val preview = resolveRsdAndNote(amountInput, currency, eurRate, "").first
            Button(
                onClick = {
                    val (rsd, _) = resolveRsdAndNote(amountInput, currency, eurRate, "")
                    if (rsd != null && rsd > 0.0) {
                        onConfirm(rsd.coerceAtMost(goal.savedAmountRsd))
                    }
                },
                enabled = preview != null && preview > 0.0,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Вернуть на баланс")
            }
        }
    }
}
