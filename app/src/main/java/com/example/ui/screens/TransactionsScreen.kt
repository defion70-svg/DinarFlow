package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BillEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryType
import com.example.data.local.TransactionEntity
import com.example.ui.components.CategoryChipGrid
import com.example.ui.components.CurrencyAmountField
import com.example.ui.components.EmojiPickerFlow
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.NativeDatePickerModal
import com.example.ui.components.resolveRsdAndNote
import com.example.ui.theme.LocalSemanticColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.viewmodel.BillUiItem
import com.example.ui.viewmodel.DinarFlowUiState
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import com.example.util.InputCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: DinarFlowUiState,
    onAddTransaction: (type: String, amountRsd: Double, categoryId: String, date: String, note: String) -> Unit,
    onUpdateTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterCategoryChange: (String?) -> Unit,
    onSaveBill: (id: String?, name: String, amountRsd: Double, categoryId: String, dueDay: Int, emoji: String) -> Unit,
    onPayBill: (String) -> Unit,
    onToggleBillManualPaid: (String) -> Unit,
    onDeleteBill: (String) -> Unit,
    onToggleIncludePlannedInFree: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val semantic = LocalSemanticColors.current
    val selectedMonth = uiState.periodSummary.selectedMonth.ifEmpty { DateTimeUtils.currentMonthStr() }

    // Quick-add form state
    var quickFormExpanded by rememberSaveable { mutableStateOf(true) }
    var txType by rememberSaveable { mutableStateOf(CategoryType.EXPENSE) }
    var amountInput by rememberSaveable { mutableStateOf("") }
    var inputCurrency by rememberSaveable { mutableStateOf(InputCurrency.RSD) }
    var noteInput by rememberSaveable { mutableStateOf("") }
    var selectedDate by rememberSaveable { mutableStateOf(DateTimeUtils.defaultDateForMonth(selectedMonth)) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Keep default date synced to selected month when month changes
    LaunchedEffect(selectedMonth) {
        if (DateTimeUtils.monthOf(selectedDate) != selectedMonth) {
            selectedDate = DateTimeUtils.defaultDateForMonth(selectedMonth)
        }
    }

    val activeCategories = if (txType == CategoryType.EXPENSE) {
        uiState.expenseCategories
    } else {
        uiState.incomeCategories
    }
    var selectedCategoryId by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(txType, activeCategories) {
        if (activeCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = activeCategories.firstOrNull()?.id.orEmpty()
        }
    }

    // Bills section state
    var billsExpanded by rememberSaveable { mutableStateOf(false) }
    var editingBill by remember { mutableStateOf<BillEntity?>(null) }
    var showBillSheet by remember { mutableStateOf(false) }

    // Transaction edit sheet state
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val categoriesById = uiState.categoriesById
    val groupedTransactions = uiState.groupedTransactions

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Quick Add Form Card
        item(key = "quick_add_card") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { quickFormExpanded = !quickFormExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Быстрая запись",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Icon(
                            imageVector = if (quickFormExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (quickFormExpanded) "Свернуть" else "Развернуть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = quickFormExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = txType == CategoryType.EXPENSE,
                                    onClick = { txType = CategoryType.EXPENSE },
                                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = semantic.expenseContainer,
                                        activeContentColor = semantic.expense
                                    ),
                                    modifier = Modifier.testTag("tx_type_expense")
                                ) {
                                    Text("Расход")
                                }
                                SegmentedButton(
                                    selected = txType == CategoryType.INCOME,
                                    onClick = { txType = CategoryType.INCOME },
                                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = semantic.incomeContainer,
                                        activeContentColor = semantic.income
                                    ),
                                    modifier = Modifier.testTag("tx_type_income")
                                ) {
                                    Text("Доход")
                                }
                            }

                            CurrencyAmountField(
                                value = amountInput,
                                onValueChange = { amountInput = it },
                                currency = inputCurrency,
                                onCurrencyChange = { inputCurrency = it },
                                eurRate = uiState.settings.eurRate,
                                testTag = "quick_tx_amount"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = noteInput,
                                    onValueChange = { noteInput = it },
                                    label = { Text("Заметка") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quick_tx_note")
                                )
                                Surface(
                                    onClick = { showDatePicker = true },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .testTag("quick_tx_date")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = "Дата",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = DateTimeUtils.formatDateHuman(selectedDate),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }

                            if (activeCategories.isNotEmpty()) {
                                CategoryChipGrid(
                                    categories = activeCategories,
                                    selectedCategoryId = selectedCategoryId,
                                    onSelectCategory = { selectedCategoryId = it.id },
                                    horizontalScroll = true
                                )
                            }

                            val parsedCheck = CurrencyFormatter.parseAmount(amountInput)
                            val isIncomeTx = txType == CategoryType.INCOME
                            Button(
                                onClick = {
                                    val (rsd, finalNote) = resolveRsdAndNote(
                                        rawInput = amountInput,
                                        currency = inputCurrency,
                                        eurRate = uiState.settings.eurRate,
                                        rawNote = noteInput
                                    )
                                    if (rsd != null && rsd > 0.0 && selectedCategoryId.isNotEmpty()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onAddTransaction(
                                            txType,
                                            rsd,
                                            selectedCategoryId,
                                            selectedDate,
                                            finalNote
                                        )
                                        amountInput = ""
                                        noteInput = ""
                                    }
                                },
                                enabled = parsedCheck != null && parsedCheck > 0.0 && selectedCategoryId.isNotEmpty(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isIncomeTx) semantic.income else semantic.expense,
                                    contentColor = if (isIncomeTx) Color(0xFF042214) else Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("quick_tx_submit")
                            ) {
                                Text(
                                    text = if (txType == CategoryType.EXPENSE) "Добавить расход" else "Добавить доход",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Bills & Mandatory Payments Expandable Section
        item(key = "bills_section") {
            val billsSummary = uiState.billsSummary
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { billsExpanded = !billsExpanded }
                            .testTag("bills_header"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Счета и обязательные платежи",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = "${billsSummary.paidCount}/${billsSummary.totalCount}",
                                        style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Свободно: ${CurrencyFormatter.formatRsd(billsSummary.freeBalanceRsd)}",
                                style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                                color = if (billsSummary.freeBalanceRsd < 0.0) semantic.expense else semantic.income
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    editingBill = null
                                    showBillSheet = true
                                },
                                modifier = Modifier.testTag("add_bill_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Добавить счёт",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (billsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(visible = billsExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onToggleIncludePlannedInFree(!billsSummary.includePlannedInFree)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = billsSummary.includePlannedInFree,
                                    onCheckedChange = onToggleIncludePlannedInFree
                                )
                                Text(
                                    text = "Учитывать прогноз по плановым сменам (+${CurrencyFormatter.formatRsd(billsSummary.plannedShiftForecastRsd)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (billsSummary.items.isEmpty()) {
                                Text(
                                    text = "Нет добавленных счетов. Нажмите +, чтобы добавить аренду, Инфостан или подписки.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            } else {
                                billsSummary.items.forEach { item ->
                                    BillRow(
                                        item = item,
                                        eurRate = uiState.settings.eurRate,
                                        onPay = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onPayBill(item.bill.id)
                                        },
                                        onToggleManual = { onToggleBillManualPaid(item.bill.id) },
                                        onEdit = {
                                            editingBill = item.bill
                                            showBillSheet = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Search & Filter Strip
        item(key = "search_filter") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Поиск по заметке, категории или сумме") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.filterCategoryId == null,
                        onClick = { onFilterCategoryChange(null) },
                        label = { Text("Все") }
                    )
                    uiState.categories.forEach { cat ->
                        FilterChip(
                            selected = uiState.filterCategoryId == cat.id,
                            onClick = { onFilterCategoryChange(cat.id) },
                            label = { Text("${cat.emoji} ${cat.name}") }
                        )
                    }
                }
            }
        }

        // 4. Transaction Feed
        if (groupedTransactions.isEmpty()) {
            item(key = "tx_empty", contentType = "empty") {
                EmptyStateCard(
                    icon = Icons.Default.ReceiptLong,
                    title = "Операций за период нет",
                    subtitle = "Добавьте первый расход или доход через форму выше — данные сохранятся офлайн."
                )
            }
        } else {
            groupedTransactions.forEach { group ->
                item(key = "header_${group.date}", contentType = "day_header") {
                    Text(
                        text = group.humanDate,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                items(
                    items = group.items,
                    key = { it.id },
                    contentType = { "tx_row" }
                ) { tx ->
                    val cat = categoriesById[tx.categoryId]
                    SwipeableTransactionRow(
                        transaction = tx,
                        category = cat,
                        eurRate = uiState.settings.eurRate,
                        onClick = { editingTx = tx },
                        onSwipeDelete = { onDeleteTransaction(tx.id) }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        NativeDatePickerModal(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                selectedDate = picked
                showDatePicker = false
            }
        )
    }

    if (showBillSheet) {
        BillEditorSheet(
            bill = editingBill,
            expenseCategories = uiState.expenseCategories,
            eurRate = uiState.settings.eurRate,
            onDismiss = { showBillSheet = false },
            onSave = { name, amount, catId, dueDay, emoji ->
                onSaveBill(editingBill?.id, name, amount, catId, dueDay, emoji)
                showBillSheet = false
            },
            onDelete = editingBill?.let { b ->
                {
                    onDeleteBill(b.id)
                    showBillSheet = false
                }
            }
        )
    }

    editingTx?.let { tx ->
        TransactionEditSheet(
            transaction = tx,
            expenseCategories = uiState.expenseCategories,
            incomeCategories = uiState.incomeCategories,
            eurRate = uiState.settings.eurRate,
            onDismiss = { editingTx = null },
            onSave = { updated ->
                onUpdateTransaction(updated)
                editingTx = null
            },
            onDelete = {
                onDeleteTransaction(tx.id)
                editingTx = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BillRow(
    item: BillUiItem,
    eurRate: Double,
    onPay: () -> Unit,
    onToggleManual: () -> Unit,
    onEdit: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val bill = item.bill

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            if (item.isPaid) semantic.income.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onToggleManual
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onToggleManual,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (item.isPaid) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (item.isPaid) "Оплачено" else "Не оплачено",
                    tint = if (item.isPaid) semantic.income else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${bill.emoji} ${bill.name}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (item.isPaid) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${CurrencyFormatter.formatRsd(bill.amountRsd)} (${CurrencyFormatter.formatEurApprox(bill.amountRsd, eurRate)}) · до ${bill.dueDay}-го",
                    style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!item.isPaid) {
                Surface(
                    onClick = onPay,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(
                        text = "Оплатить",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            } else {
                Text(
                    text = if (item.paidByTransaction) "Проведён" else "Отмечен",
                    style = MaterialTheme.typography.labelSmall,
                    color = semantic.income
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTransactionRow(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    eurRate: Double,
    onClick: () -> Unit,
    onSwipeDelete: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwipeDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(semantic.expenseContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = semantic.expense
                )
            }
        }
    ) {
        val catColor = Color(category?.colorHex ?: 0xFF38BDF8)
        val isIncome = transaction.type == CategoryType.INCOME
        val typeColor = if (isIncome) semantic.income else semantic.expense
        val typeContainer = if (isIncome) semantic.incomeContainer else semantic.expenseContainer

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, typeColor.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tx_row_${transaction.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(typeColor)
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = category?.emoji ?: "💳", fontSize = 18.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category?.name ?: "Категория",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = transaction.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = if (isIncome) {
                                CurrencyFormatter.formatRsd(transaction.amountRsd, showSign = true)
                            } else {
                                "−${CurrencyFormatter.formatRsd(transaction.amountRsd)}"
                            },
                            style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle).copy(fontWeight = FontWeight.Bold),
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.formatEurApprox(transaction.amountRsd, eurRate),
                        style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionEditSheet(
    transaction: TransactionEntity,
    expenseCategories: List<CategoryEntity>,
    incomeCategories: List<CategoryEntity>,
    eurRate: Double,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
    onDelete: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var txType by remember { mutableStateOf(transaction.type) }
    var amountInput by remember {
        mutableStateOf(
            if (transaction.amountRsd % 1.0 == 0.0) transaction.amountRsd.toInt().toString()
            else transaction.amountRsd.toString()
        )
    }
    var currency by remember { mutableStateOf(InputCurrency.RSD) }
    var note by remember { mutableStateOf(transaction.note) }
    var date by remember { mutableStateOf(transaction.date) }
    var catId by remember { mutableStateOf(transaction.categoryId) }
    var showDatePicker by remember { mutableStateOf(false) }

    val cats = if (txType == CategoryType.EXPENSE) expenseCategories else incomeCategories
    LaunchedEffect(txType) {
        if (cats.none { it.id == catId }) {
            catId = cats.firstOrNull()?.id.orEmpty()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Редактировать операцию", style = MaterialTheme.typography.headlineSmall)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = txType == CategoryType.EXPENSE,
                    onClick = { txType = CategoryType.EXPENSE },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Расход") }
                SegmentedButton(
                    selected = txType == CategoryType.INCOME,
                    onClick = { txType = CategoryType.INCOME },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Доход") }
            }

            CurrencyAmountField(
                value = amountInput,
                onValueChange = { amountInput = it },
                currency = currency,
                onCurrencyChange = { currency = it },
                eurRate = eurRate
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Заметка") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Дата: ${DateTimeUtils.formatDateHuman(date)} ($date)")
            }

            CategoryChipGrid(
                categories = cats,
                selectedCategoryId = catId,
                onSelectCategory = { catId = it.id },
                horizontalScroll = false
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.expense),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Удалить")
                }

                val parsedCheck = CurrencyFormatter.parseAmount(amountInput)
                Button(
                    onClick = {
                        val (rsd, finalNote) = resolveRsdAndNote(amountInput, currency, eurRate, note)
                        if (rsd != null && rsd > 0.0 && catId.isNotEmpty()) {
                            onSave(
                                transaction.copy(
                                    type = txType,
                                    amountRsd = rsd,
                                    categoryId = catId,
                                    date = date,
                                    note = finalNote
                                )
                            )
                        }
                    },
                    enabled = parsedCheck != null && parsedCheck > 0.0 && catId.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сохранить")
                }
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
private fun BillEditorSheet(
    bill: BillEntity?,
    expenseCategories: List<CategoryEntity>,
    eurRate: Double,
    onDismiss: () -> Unit,
    onSave: (name: String, amountRsd: Double, categoryId: String, dueDay: Int, emoji: String) -> Unit,
    onDelete: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(bill?.name ?: "") }
    var amountInput by remember {
        mutableStateOf(
            bill?.let {
                if (it.amountRsd % 1.0 == 0.0) it.amountRsd.toInt().toString() else it.amountRsd.toString()
            } ?: ""
        )
    }
    var currency by remember { mutableStateOf(InputCurrency.RSD) }
    var dueDayInput by remember { mutableStateOf((bill?.dueDay ?: 15).toString()) }
    var catId by remember {
        mutableStateOf(bill?.categoryId ?: expenseCategories.firstOrNull()?.id.orEmpty())
    }
    var emoji by remember { mutableStateOf(bill?.emoji ?: "🧾") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (bill == null) "Новый обязательный счёт" else "Редактировать счёт",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название (например, Инфостан / Аренда)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            CurrencyAmountField(
                value = amountInput,
                onValueChange = { amountInput = it },
                currency = currency,
                onCurrencyChange = { currency = it },
                eurRate = eurRate
            )

            OutlinedTextField(
                value = dueDayInput,
                onValueChange = { dueDayInput = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("День оплаты (1–31)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Категория расхода", style = MaterialTheme.typography.labelLarge)
            CategoryChipGrid(
                categories = expenseCategories,
                selectedCategoryId = catId,
                onSelectCategory = { catId = it.id },
                horizontalScroll = true
            )

            Text("Иконка", style = MaterialTheme.typography.labelLarge)
            EmojiPickerFlow(selectedEmoji = emoji, onSelect = { emoji = it })

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = LocalSemanticColors.current.expense)
                    ) {
                        Text("Удалить")
                    }
                }
                Spacer(Modifier.weight(1f))
                val (rsdPreview, _) = resolveRsdAndNote(amountInput, currency, eurRate, "")
                Button(
                    onClick = {
                        val (rsd, _) = resolveRsdAndNote(amountInput, currency, eurRate, "")
                        val day = dueDayInput.toIntOrNull()?.coerceIn(1, 31) ?: 15
                        if (name.isNotBlank() && rsd != null && rsd > 0.0 && catId.isNotEmpty()) {
                            onSave(name, rsd, catId, day, emoji)
                        }
                    },
                    enabled = name.isNotBlank() && rsdPreview != null && rsdPreview > 0.0 && catId.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}
