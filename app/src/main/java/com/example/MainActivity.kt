package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CompactPeriodSummaryCard
import com.example.ui.components.DinarFlowTopBar
import com.example.ui.components.NativeDateRangePickerModal
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShiftsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.DinarFlowTheme
import com.example.ui.viewmodel.DinarFlowViewModel
import com.example.ui.viewmodel.MainTab
import com.example.util.DateTimeUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: DinarFlowViewModel = viewModel()
            DinarFlowApp(viewModel = viewModel)
        }
    }
}

private data class TabItemMeta(
    val tab: MainTab,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val NavigationTabs = listOf(
    TabItemMeta(MainTab.TRANSACTIONS, Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    TabItemMeta(MainTab.SHIFTS, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    TabItemMeta(MainTab.GOALS, Icons.Filled.Savings, Icons.Outlined.Savings),
    TabItemMeta(MainTab.ANALYTICS, Icons.Filled.PieChart, Icons.Outlined.PieChart),
    TabItemMeta(MainTab.SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun DinarFlowApp(viewModel: DinarFlowViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableStateOf(MainTab.TRANSACTIONS) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var isBalanceCollapsed by rememberSaveable { mutableStateOf(false) }

    val density = LocalDensity.current
    val collapseTriggerPx = with(density) { 6.dp.toPx() }
    val expandTriggerPx = with(density) { 8.dp.toPx() }
    val collapseAbsorbHeightPx = with(density) { 130.dp.toPx() }

    val scrollConnection = remember(collapseTriggerPx, expandTriggerPx, collapseAbsorbHeightPx) {
        object : NestedScrollConnection {
            private var downScrollAcc = 0f
            private var upScrollAcc = 0f
            private var absorbRemainingPx = 0f

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val dy = available.y
                if (dy < -0.5f) {
                    upScrollAcc = 0f
                    if (!isBalanceCollapsed && source == NestedScrollSource.UserInput) {
                        downScrollAcc += -dy
                        if (downScrollAcc >= collapseTriggerPx) {
                            isBalanceCollapsed = true
                            absorbRemainingPx = collapseAbsorbHeightPx
                            downScrollAcc = 0f
                        }
                        return Offset(0f, dy)
                    }
                    if (isBalanceCollapsed && absorbRemainingPx > 0f) {
                        val consumeY = maxOf(dy, -absorbRemainingPx)
                        absorbRemainingPx = (absorbRemainingPx + consumeY).coerceAtLeast(0f)
                        return Offset(0f, consumeY)
                    }
                } else if (dy > 0.5f) {
                    downScrollAcc = 0f
                    absorbRemainingPx = 0f
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val unconsumedUp = available.y
                if (unconsumedUp > 0.5f && isBalanceCollapsed) {
                    upScrollAcc += unconsumedUp
                    if (upScrollAcc >= expandTriggerPx) {
                        isBalanceCollapsed = false
                        upScrollAcc = 0f
                        absorbRemainingPx = 0f
                    }
                } else if (unconsumedUp < -0.5f) {
                    upScrollAcc = 0f
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(selectedTab) {
        isBalanceCollapsed = false
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = selectedTab != MainTab.TRANSACTIONS) {
        selectedTab = MainTab.TRANSACTIONS
    }

    DinarFlowTheme(themeMode = uiState.settings.themeMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 700.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail) {
                        Column {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                tonalElevation = 0.dp
                            ) {
                                val navColors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                NavigationTabs.forEach { meta ->
                                    val selected = selectedTab == meta.tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { selectedTab = meta.tab },
                                        colors = navColors,
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) meta.selectedIcon else meta.unselectedIcon,
                                                contentDescription = meta.tab.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = meta.tab.label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        modifier = Modifier.testTag("nav_tab_${meta.tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        val railColors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            NavigationTabs.forEach { meta ->
                                val selected = selectedTab == meta.tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { selectedTab = meta.tab },
                                    colors = railColors,
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) meta.selectedIcon else meta.unselectedIcon,
                                            contentDescription = meta.tab.label
                                        )
                                    },
                                    label = { Text(meta.tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag("rail_tab_${meta.tab.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 760.dp)
                                .nestedScroll(scrollConnection)
                        ) {
                            DinarFlowTopBar(
                                periodSummary = uiState.periodSummary,
                                onShiftMonth = viewModel::shiftMonth,
                                onSelectPeriodMode = viewModel::setPeriodMode,
                                onPickCustomRange = { showDateRangePicker = true }
                            )

                            if (selectedTab != MainTab.SETTINGS) {
                                CompactPeriodSummaryCard(
                                    summary = uiState.periodSummary,
                                    eurRate = uiState.settings.eurRate,
                                    onToggleCarryover = viewModel::toggleCarryover,
                                    isCollapsed = isBalanceCollapsed,
                                    onExpandClick = { isBalanceCollapsed = false }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                when (selectedTab) {
                                    MainTab.TRANSACTIONS -> TransactionsScreen(
                                        uiState = uiState,
                                        onAddTransaction = viewModel::addTransaction,
                                        onUpdateTransaction = viewModel::updateTransaction,
                                        onDeleteTransaction = viewModel::deleteTransaction,
                                        onSearchQueryChange = viewModel::setSearchQuery,
                                        onFilterCategoryChange = viewModel::setFilterCategory,
                                        onSaveBill = viewModel::saveBill,
                                        onPayBill = viewModel::payBill,
                                        onToggleBillManualPaid = viewModel::toggleBillManualPaid,
                                        onDeleteBill = viewModel::deleteBill,
                                        onToggleIncludePlannedInFree = viewModel::toggleIncludePlannedInFree
                                    )

                                    MainTab.SHIFTS -> ShiftsScreen(
                                        uiState = uiState,
                                        onSaveShift = viewModel::saveShift,
                                        onDeleteShift = viewModel::deleteShift,
                                        onCreditSalary = viewModel::creditSalary
                                    )

                                    MainTab.GOALS -> GoalsScreen(
                                        uiState = uiState,
                                        onCreateGoal = viewModel::createGoal,
                                        onUpdateGoal = viewModel::updateGoal,
                                        onDepositToGoal = viewModel::depositToGoal,
                                        onWithdrawFromGoal = viewModel::withdrawFromGoal,
                                        onDeleteGoal = viewModel::deleteGoal
                                    )

                                    MainTab.ANALYTICS -> AnalyticsScreen(
                                        uiState = uiState,
                                        onUpdateCategoryLimit = viewModel::updateCategoryLimit
                                    )

                                    MainTab.SETTINGS -> SettingsScreen(
                                        uiState = uiState,
                                        onUpdateEurRate = viewModel::updateEurRate,
                                        onUpdatePaydays = viewModel::updatePaydays,
                                        onUpdateBaseHourlyRate = viewModel::updateBaseHourlyRate,
                                        onUpdateThemeMode = viewModel::updateThemeMode,
                                        onAddShiftPreset = viewModel::addShiftPreset,
                                        onDeleteShiftPreset = viewModel::deleteShiftPreset,
                                        onSaveCategory = viewModel::saveCategory,
                                        onReorderCategories = viewModel::reorderCategories,
                                        onDeleteCategory = viewModel::deleteCategory,
                                        onGenerateJsonBackup = viewModel::generateJsonBackup,
                                        onGenerateCsvExport = viewModel::generateCsvExport,
                                        onImportJsonBackup = viewModel::importJsonBackup,
                                        onUploadCloudBackup = viewModel::uploadBackupToCloud,
                                        onDownloadCloudBackup = viewModel::downloadBackupFromCloud,
                                        onUpdateSyncUrl = viewModel::updateSyncUrl,
                                        onResetAllData = viewModel::resetAllData,
                                        onShowMessage = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    )
                                }

                                // Soft top edge fade so scrolling cards fade smoothly instead of clipping sharply under the balance card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .align(Alignment.TopCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.background,
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDateRangePicker) {
            NativeDateRangePickerModal(
                initialStart = uiState.periodSummary.rangeStart.ifEmpty {
                    "${uiState.periodSummary.selectedMonth.ifEmpty { DateTimeUtils.currentMonthStr() }}-01"
                },
                initialEnd = uiState.periodSummary.rangeEnd.ifEmpty { DateTimeUtils.todayStr() },
                onDismiss = { showDateRangePicker = false },
                onConfirm = { start, end ->
                    viewModel.setCustomRange(start, end)
                    showDateRangePicker = false
                }
            )
        }
    }
}
