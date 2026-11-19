package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.sin
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
    val collapseTriggerPx = with(density) { 18.dp.toPx() }
    val expandTriggerPx = with(density) { 12.dp.toPx() }

    val scrollConnection = remember(collapseTriggerPx, expandTriggerPx) {
        object : NestedScrollConnection {
            private var downScrollAcc = 0f
            private var upScrollAcc = 0f

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val dy = available.y
                if (dy < -0.5f && source == NestedScrollSource.UserInput) {
                    upScrollAcc = 0f
                    if (!isBalanceCollapsed) {
                        downScrollAcc += -dy
                        if (downScrollAcc >= collapseTriggerPx) {
                            isBalanceCollapsed = true
                            downScrollAcc = 0f
                        }
                    }
                } else if (dy > 0.5f) {
                    downScrollAcc = 0f
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
                        JumpingDropletBottomBar(
                            tabs = NavigationTabs,
                            selectedTab = selectedTab,
                            onSelectTab = { selectedTab = it }
                        )
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
                                CollapsibleBalanceHeader(
                                    summary = uiState.periodSummary,
                                    eurRate = uiState.settings.eurRate,
                                    onToggleCarryover = viewModel::toggleCarryover,
                                    isCollapsedProvider = { isBalanceCollapsed },
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

@Composable
private fun JumpingDropletBottomBar(
    tabs: List<TabItemMeta>,
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    val selectedIndex = tabs.indexOfFirst { it.tab == selectedTab }.coerceAtLeast(0)
    val animatedIndex = remember { Animatable(selectedIndex.toFloat()) }
    val jumpProgress = remember { Animatable(0f) }
    var previousIndex by remember { mutableIntStateOf(selectedIndex) }
    var travelDirection by remember { mutableStateOf(1f) }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != previousIndex) {
            travelDirection = if (selectedIndex > previousIndex) 1f else -1f
            previousIndex = selectedIndex
            launch {
                animatedIndex.animateTo(
                    targetValue = selectedIndex.toFloat(),
                    animationSpec = spring(
                        dampingRatio = 0.62f,
                        stiffness = 380f
                    )
                )
            }
            launch {
                jumpProgress.snapTo(0f)
                jumpProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(54.dp)
            ) {
                // Liquid droplet indicator that arcs upward and stretches while jumping between tabs
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val slotWidth = size.width / tabs.size.coerceAtLeast(1)
                    val cx = (animatedIndex.value + 0.5f) * slotWidth
                    val arc = sin(Math.PI * jumpProgress.value).toFloat().coerceAtLeast(0f)

                    val baseCenterY = 20.dp.toPx()
                    val cy = baseCenterY - 6.dp.toPx() * arc

                    val baseW = 42.dp.toPx()
                    val baseH = 24.dp.toPx()
                    val pillW = baseW + 18.dp.toPx() * arc
                    val pillH = baseH - 4.5.dp.toPx() * arc
                    val corner = CornerRadius(pillH / 2f, pillH / 2f)

                    val topLeft = Offset(cx - pillW / 2f, cy - pillH / 2f)
                    val rectSize = Size(pillW, pillH)

                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.19f + 0.12f * arc),
                        topLeft = topLeft,
                        size = rectSize,
                        cornerRadius = corner
                    )
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.32f + 0.25f * arc),
                        topLeft = topLeft,
                        size = rectSize,
                        cornerRadius = corner,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    if (arc > 0.08f) {
                        val trailRadius = 3.2.dp.toPx() * arc
                        val trailCx = cx - travelDirection * (pillW * 0.54f + 4.dp.toPx())
                        val trailCy = baseCenterY - 2.dp.toPx() * arc
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.42f * arc),
                            radius = trailRadius,
                            center = Offset(trailCx, trailCy)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { meta ->
                        val selected = selectedTab == meta.tab
                        val contentColor by animateColorAsState(
                            targetValue = if (selected) primaryColor else mutedColor,
                            animationSpec = tween(durationMillis = 200),
                            label = "tabColor"
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.1f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "tabScale"
                        )
                        val iconLift by animateDpAsState(
                            targetValue = if (selected) (-1.5).dp else 0.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "tabLift"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(role = Role.Tab) { onSelectTab(meta.tab) }
                                .testTag("nav_tab_${meta.tab.name.lowercase()}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                        translationY = iconLift.toPx()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (selected) meta.selectedIcon else meta.unselectedIcon,
                                    contentDescription = meta.tab.label,
                                    tint = contentColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = meta.tab.label,
                                color = contentColor,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleBalanceHeader(
    summary: com.example.ui.viewmodel.PeriodSummary,
    eurRate: Double,
    onToggleCarryover: (Boolean) -> Unit,
    isCollapsedProvider: () -> Boolean,
    onExpandClick: () -> Unit
) {
    CompactPeriodSummaryCard(
        summary = summary,
        eurRate = eurRate,
        onToggleCarryover = onToggleCarryover,
        isCollapsed = isCollapsedProvider(),
        onExpandClick = onExpandClick
    )
}
