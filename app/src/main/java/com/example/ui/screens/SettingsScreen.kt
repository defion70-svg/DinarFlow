package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryType
import com.example.ui.components.ColorSwatchPicker
import com.example.ui.components.CuratedAccentColors
import com.example.ui.components.CurrencyAmountField
import com.example.ui.components.EmojiPickerFlow
import com.example.ui.components.NativeTimePickerDialog
import com.example.ui.components.resolveRsdAndNote
import com.example.ui.theme.LocalSemanticColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.viewmodel.DinarFlowUiState
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import com.example.util.InputCurrency
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: DinarFlowUiState,
    onUpdateEurRate: (Double) -> Unit,
    onUpdatePaydays: (Int, Int) -> Unit,
    onUpdateBaseHourlyRate: (Double, Boolean) -> Unit,
    onUpdateThemeMode: (String) -> Unit,
    onAddShiftPreset: (String, String) -> Unit,
    onDeleteShiftPreset: (String) -> Unit,
    onSaveCategory: (id: String?, name: String, emoji: String, colorHex: Long, type: String, monthlyLimitRsd: Double) -> Unit,
    onReorderCategories: (List<CategoryEntity>) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onGenerateJsonBackup: suspend () -> String,
    onGenerateCsvExport: suspend () -> String,
    onImportJsonBackup: (String) -> Unit,
    onUploadCloudBackup: (String) -> Unit,
    onDownloadCloudBackup: (String) -> Unit,
    onUpdateSyncUrl: (String) -> Unit,
    onResetAllData: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val semantic = LocalSemanticColors.current
    val settings = uiState.settings

    // Work & Currency local fields synced with settings
    var hourlyInput by remember(settings.hourlyRate) {
        mutableStateOf(
            if (settings.hourlyRate % 1.0 == 0.0) settings.hourlyRate.toInt().toString()
            else settings.hourlyRate.toString()
        )
    }
    var eurInput by remember(settings.eurRate) {
        mutableStateOf(
            if (settings.eurRate % 1.0 == 0.0) settings.eurRate.toInt().toString()
            else settings.eurRate.toString()
        )
    }
    var payday1Input by remember(settings.payday1Date) { mutableStateOf(settings.payday1Date.toString()) }
    var payday2Input by remember(settings.payday2Date) { mutableStateOf(settings.payday2Date.toString()) }
    var syncUrlInput by remember(settings.syncUrl) { mutableStateOf(settings.syncUrl) }

    var pendingRateToConfirm by remember { mutableStateOf<Double?>(null) }
    var showAddPresetSheet by remember { mutableStateOf(false) }

    // Category management state
    var catTab by rememberSaveable { mutableStateOf(CategoryType.EXPENSE) }
    val managedCategories = if (catTab == CategoryType.EXPENSE) {
        uiState.expenseCategories
    } else {
        uiState.incomeCategories
    }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCategorySheet by remember { mutableStateOf(false) }

    // Reset confirmation sheet
    var showResetSheet by remember { mutableStateOf(false) }
    var showJsonTextSheet by remember { mutableStateOf(false) }

    // SAF Launchers (Zero-permission file export/import per Google Play policy)
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = onGenerateJsonBackup()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Не удалось открыть файл")
                }.onSuccess {
                    onShowMessage("JSON резервная копия сохранена")
                }.onFailure {
                    onShowMessage("Ошибка экспорта JSON: ${it.message}")
                }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error("Не удалось прочитать файл")
                }.onSuccess { rawJson ->
                    onImportJsonBackup(rawJson)
                }.onFailure {
                    onShowMessage("Ошибка чтения файла: ${it.message}")
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val csv = onGenerateCsvExport()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: error("Не удалось открыть файл")
                }.onSuccess {
                    onShowMessage("CSV файл операций сохранён")
                }.onFailure {
                    onShowMessage("Ошибка экспорта CSV: ${it.message}")
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Work & Currency Parameters
        item(key = "work_currency_section") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Работа и валюта", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = hourlyInput,
                            onValueChange = { hourlyInput = CurrencyFormatter.sanitizeDecimalInput(it) },
                            label = { Text("Базовая ставка (RSD/ч)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_hourly_rate")
                        )
                        Button(
                            onClick = {
                                val parsed = CurrencyFormatter.parseAmount(hourlyInput)
                                if (parsed != null && parsed >= 0.0) {
                                    pendingRateToConfirm = parsed
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Text("Ок")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = eurInput,
                            onValueChange = { eurInput = CurrencyFormatter.sanitizeDecimalInput(it) },
                            label = { Text("Курс 1 EUR (в RSD)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_eur_rate")
                        )
                        Button(
                            onClick = {
                                val parsed = CurrencyFormatter.parseAmount(eurInput)
                                if (parsed != null && parsed > 0.0) {
                                    onUpdateEurRate(parsed)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Text("Ок")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = payday1Input,
                            onValueChange = { payday1Input = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Выплата №1 (1–15)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = payday2Input,
                            onValueChange = { payday2Input = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Выплата №2 (16–31)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val p1 = payday1Input.toIntOrNull()?.coerceIn(1, 31) ?: 25
                                val p2 = payday2Input.toIntOrNull()?.coerceIn(1, 31) ?: 10
                                onUpdatePaydays(p1, p2)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Text("Ок")
                        }
                    }

                    // Theme mode
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Тема оформления", style = MaterialTheme.typography.labelLarge)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf("SYSTEM" to "Системная", "DARK" to "Тёмная", "LIGHT" to "Светлая")
                                .forEachIndexed { idx, (key, title) ->
                                    SegmentedButton(
                                        selected = settings.themeMode == key,
                                        onClick = { onUpdateThemeMode(key) },
                                        shape = SegmentedButtonDefaults.itemShape(idx, 3)
                                    ) {
                                        Text(title, maxLines = 1)
                                    }
                                }
                        }
                    }
                }
            }
        }

        // 2. Shift Presets Editor
        item(key = "presets_section") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        Text("Пресеты смен", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showAddPresetSheet = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Добавить")
                        }
                    }

                    if (uiState.shiftPresets.isEmpty()) {
                        Text(
                            text = "Нет пресетов",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.shiftPresets.forEach { preset ->
                            val hours = DateTimeUtils.calculateShiftHours(preset.startTime, preset.endTime)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "${preset.startTime} – ${preset.endTime}",
                                            style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle)
                                        )
                                        Text(
                                            text = "(${DateTimeUtils.formatHours(hours)})",
                                            style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteShiftPreset(preset.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Удалить пресет",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Category Management Header
        item(key = "categories_header") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Категории", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Перетаскивайте за ≡ или используйте ↑/↓ — порядок сразу обновляется при вводе операций.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                editingCategory = null
                                showCategorySheet = true
                            },
                            modifier = Modifier.testTag("add_category_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Новая категория", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = catTab == CategoryType.EXPENSE,
                            onClick = { catTab = CategoryType.EXPENSE },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("Расходы") }
                        SegmentedButton(
                            selected = catTab == CategoryType.INCOME,
                            onClick = { catTab = CategoryType.INCOME },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("Доходы") }
                    }

                    ReorderableCategoryList(
                        categories = managedCategories,
                        onReorder = { newList ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onReorderCategories(newList)
                        },
                        onEdit = { cat ->
                            editingCategory = cat
                            showCategorySheet = true
                        }
                    )
                }
            }
        }

        // 4. Backup & Cloud Sync & Reset
        item(key = "backup_sync_section") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Резервное копирование и экспорт", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                exportJsonLauncher.launch("dinarflow_backup_${DateTimeUtils.todayStr()}.json")
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Экспорт JSON")
                        }
                        OutlinedButton(
                            onClick = {
                                importJsonLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Импорт файла")
                        }
                    }

                    OutlinedButton(
                        onClick = { showJsonTextSheet = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("json_text_backup_button")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Вставить / Скопировать JSON текстом")
                    }

                    OutlinedButton(
                        onClick = {
                            exportCsvLauncher.launch("dinarflow_operations_${DateTimeUtils.todayStr()}.csv")
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Экспорт операций в CSV")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Облачная синхронизация (URL)", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = syncUrlInput,
                        onValueChange = {
                            syncUrlInput = it
                            onUpdateSyncUrl(it)
                        },
                        label = { Text("URL / эндпоинт резервной копии") },
                        placeholder = { Text("https://example.com/dinarflow.json") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onDownloadCloudBackup(syncUrlInput) },
                            enabled = !uiState.isSyncing && syncUrlInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Загрузить")
                        }
                        Button(
                            onClick = { onUploadCloudBackup(syncUrlInput) },
                            enabled = !uiState.isSyncing && syncUrlInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Сохранить")
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    OutlinedButton(
                        onClick = { showResetSheet = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.expense),
                        border = BorderStroke(1.dp, semantic.expense.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_data_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Полный сброс всех данных")
                    }
                }
            }
        }
    }

    // Confirm Hourly Rate update for future planned shifts
    pendingRateToConfirm?.let { newRate ->
        HourlyRateConfirmSheet(
            newRate = newRate,
            onDismiss = { pendingRateToConfirm = null },
            onConfirm = { updatePlanned ->
                onUpdateBaseHourlyRate(newRate, updatePlanned)
                pendingRateToConfirm = null
            }
        )
    }

    if (showAddPresetSheet) {
        AddShiftPresetSheet(
            onDismiss = { showAddPresetSheet = false },
            onSave = { start, end ->
                onAddShiftPreset(start, end)
                showAddPresetSheet = false
            }
        )
    }

    if (showCategorySheet) {
        CategoryEditorSheet(
            category = editingCategory,
            defaultType = catTab,
            eurRate = settings.eurRate,
            onDismiss = {
                showCategorySheet = false
                editingCategory = null
            },
            onSave = { name, emoji, colorHex, limitRsd ->
                onSaveCategory(
                    editingCategory?.id,
                    name,
                    emoji,
                    colorHex,
                    editingCategory?.type ?: catTab,
                    limitRsd
                )
                showCategorySheet = false
                editingCategory = null
            },
            onDelete = editingCategory?.takeIf { !it.isSystem }?.let { cat ->
                {
                    onDeleteCategory(cat.id)
                    showCategorySheet = false
                    editingCategory = null
                }
            }
        )
    }

    if (showResetSheet) {
        ResetDataConfirmSheet(
            onDismiss = { showResetSheet = false },
            onConfirmReset = {
                onResetAllData()
                showResetSheet = false
            }
        )
    }

    if (showJsonTextSheet) {
        JsonTextBackupSheet(
            onGenerateJsonBackup = onGenerateJsonBackup,
            onDismiss = { showJsonTextSheet = false },
            onImportJson = { rawJson ->
                onImportJsonBackup(rawJson)
                showJsonTextSheet = false
            },
            onShowMessage = onShowMessage
        )
    }
}

@Composable
private fun ReorderableCategoryList(
    categories: List<CategoryEntity>,
    onReorder: (List<CategoryEntity>) -> Unit,
    onEdit: (CategoryEntity) -> Unit
) {
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 64.dp.toPx() }
    var localList by remember(categories) { mutableStateOf(categories) }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(categories) {
        if (draggedIndex == -1) {
            localList = categories
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        localList.forEachIndexed { index, cat ->
            val isDragging = index == draggedIndex
            val catColor = Color(cat.colorHex)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(
                    1.dp,
                    if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 10f else 0f)
                    .offset { IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0) }
                    .clickable { onEdit(cat) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Drag Handle
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Перетащить категорию",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .pointerInput(localList.size) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        draggedIndex = index
                                        dragOffsetY = 0f
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        if (draggedIndex < 0) return@detectVerticalDragGestures
                                        dragOffsetY += dragAmount
                                        val steps = (dragOffsetY / itemHeightPx).toInt()
                                        if (steps != 0) {
                                            val targetIdx = (draggedIndex + steps).coerceIn(0, localList.lastIndex)
                                            if (targetIdx != draggedIndex) {
                                                val mutable = localList.toMutableList()
                                                val moved = mutable.removeAt(draggedIndex)
                                                mutable.add(targetIdx, moved)
                                                localList = mutable
                                                dragOffsetY -= (targetIdx - draggedIndex) * itemHeightPx
                                                draggedIndex = targetIdx
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (draggedIndex >= 0) {
                                            onReorder(localList)
                                        }
                                        draggedIndex = -1
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggedIndex = -1
                                        dragOffsetY = 0f
                                        localList = categories
                                    }
                                )
                            }
                    )

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(catColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = cat.emoji, fontSize = 16.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (cat.type == CategoryType.EXPENSE && cat.monthlyLimitRsd > 0.0) {
                            Text(
                                text = "Лимит: ${CurrencyFormatter.formatRsd(cat.monthlyLimitRsd)}",
                                style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (cat.isSystem) {
                            Text(
                                text = "Системная",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Accessibility Up/Down buttons
                    IconButton(
                        onClick = {
                            if (index > 0) {
                                val mutable = localList.toMutableList()
                                val item = mutable.removeAt(index)
                                mutable.add(index - 1, item)
                                onReorder(mutable)
                            }
                        },
                        enabled = index > 0,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("cat_up_${cat.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Переместить вверх",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (index < localList.lastIndex) {
                                val mutable = localList.toMutableList()
                                val item = mutable.removeAt(index)
                                mutable.add(index + 1, item)
                                onReorder(mutable)
                            }
                        },
                        enabled = index < localList.lastIndex,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("cat_down_${cat.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Переместить вниз",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourlyRateConfirmSheet(
    newRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (updateFuturePlanned: Boolean) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Обновить ставку на ${CurrencyFormatter.formatRsd(newRate)}/ч?",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Отработанные смены сохранят свою зафиксированную ставку. Обновить также будущие запланированные смены со ставкой по умолчанию?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { onConfirm(true) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Обновить и будущие плановые смены")
            }
            OutlinedButton(
                onClick = { onConfirm(false) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Применить только к новым сменам")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddShiftPresetSheet(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var startTime by remember { mutableStateOf("17:00") }
    var endTime by remember { mutableStateOf("01:00") }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val hours = DateTimeUtils.calculateShiftHours(startTime, endTime)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Новый пресет смены", style = MaterialTheme.typography.headlineSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartPicker = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("Начало: $startTime") }
                OutlinedButton(
                    onClick = { showEndPicker = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("Конец: $endTime") }
            }
            Text(
                text = "Длительность: ${DateTimeUtils.formatHours(hours)}",
                style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = { onSave(startTime, endTime) },
                enabled = hours > 0.0,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Добавить пресет")
            }
        }
    }

    if (showStartPicker) {
        NativeTimePickerDialog(
            title = "Начало смены",
            initialTime = startTime,
            onDismiss = { showStartPicker = false },
            onConfirm = {
                startTime = it
                showStartPicker = false
            }
        )
    }
    if (showEndPicker) {
        NativeTimePickerDialog(
            title = "Окончание смены",
            initialTime = endTime,
            onDismiss = { showEndPicker = false },
            onConfirm = {
                endTime = it
                showEndPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditorSheet(
    category: CategoryEntity?,
    defaultType: String,
    eurRate: Double,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, colorHex: Long, limitRsd: Double) -> Unit,
    onDelete: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(category?.name ?: "") }
    var emoji by remember { mutableStateOf(category?.emoji ?: "🛍️") }
    var colorHex by remember { mutableLongStateOf(category?.colorHex ?: CuratedAccentColors.first()) }
    var limitInput by remember {
        mutableStateOf(
            category?.let {
                if (it.monthlyLimitRsd > 0.0) {
                    if (it.monthlyLimitRsd % 1.0 == 0.0) it.monthlyLimitRsd.toInt().toString()
                    else it.monthlyLimitRsd.toString()
                } else ""
            } ?: ""
        )
    }
    var limitCurrency by remember { mutableStateOf(InputCurrency.RSD) }
    val effectiveType = category?.type ?: defaultType

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (category == null) "Новая категория" else "Редактировать категорию",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (effectiveType == CategoryType.EXPENSE) {
                CurrencyAmountField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    currency = limitCurrency,
                    onCurrencyChange = { limitCurrency = it },
                    eurRate = eurRate,
                    label = "Месячный лимит (0 = без лимита)"
                )
            }

            Text("Эмодзи", style = MaterialTheme.typography.labelLarge)
            EmojiPickerFlow(selectedEmoji = emoji, onSelect = { emoji = it })

            Text("Цвет", style = MaterialTheme.typography.labelLarge)
            ColorSwatchPicker(selectedColorHex = colorHex, onSelect = { colorHex = it })

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
                Button(
                    onClick = {
                        val limit = if (effectiveType == CategoryType.EXPENSE && limitInput.isNotBlank()) {
                            resolveRsdAndNote(limitInput, limitCurrency, eurRate, "").first ?: 0.0
                        } else 0.0
                        if (name.isNotBlank()) {
                            onSave(name, emoji, colorHex, limit)
                        }
                    },
                    enabled = name.isNotBlank(),
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
private fun ResetDataConfirmSheet(
    onDismiss: () -> Unit,
    onConfirmReset: () -> Unit
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
            Text("Сбросить все данные?", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Все операции, смены, счета, цели и пользовательские категории будут безвозвратно удалены. Баланс станет равен 0 RSD.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onConfirmReset,
                colors = ButtonDefaults.buttonColors(containerColor = semantic.expense),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_reset_button")
            ) {
                Text("Да, очистить всё")
            }
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Отмена")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JsonTextBackupSheet(
    onGenerateJsonBackup: suspend () -> String,
    onDismiss: () -> Unit,
    onImportJson: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var jsonText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Импорт / Экспорт JSON текстом",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Вставьте текст резервной копии JSON для восстановления данных или скопируйте текущую базу в буфер обмена.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clip = clipboardManager.getText()?.text
                        if (!clip.isNullOrBlank()) {
                            jsonText = clip
                            onShowMessage("Текст вставлен из буфера обмена")
                        } else {
                            onShowMessage("Буфер обмена пуст")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("paste_clipboard_json_button")
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Из буфера", maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            runCatching {
                                onGenerateJsonBackup()
                            }.onSuccess { generated ->
                                jsonText = generated
                                clipboardManager.setText(AnnotatedString(generated))
                                onShowMessage("Текущий JSON скопирован в буфер")
                            }.onFailure {
                                onShowMessage("Ошибка генерации JSON: ${it.message}")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_current_json_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Скопировать базу", maxLines = 1)
                }
            }

            OutlinedTextField(
                value = jsonText,
                onValueChange = { jsonText = it },
                label = { Text("JSON резервная копия") },
                placeholder = { Text("{\n  \"version\": 1,\n  \"transactions\": [...]\n}") },
                minLines = 6,
                maxLines = 12,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("json_text_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Отмена")
                }
                Button(
                    onClick = {
                        if (jsonText.isNotBlank()) {
                            onImportJson(jsonText.trim())
                        }
                    },
                    enabled = jsonText.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("confirm_import_json_text_button")
                ) {
                    Text("Импортировать")
                }
            }
        }
    }
}

