package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppSettingsEntity
import com.example.data.local.BillEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryType
import com.example.data.local.DinarFlowDatabase
import com.example.data.local.GoalEntity
import com.example.data.local.ShiftEntity
import com.example.data.local.ShiftPresetEntity
import com.example.data.local.SystemCategoryIds
import com.example.data.local.TransactionEntity
import com.example.util.BackupPayload
import com.example.util.BackupSerializer
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

data class BudgetAlert(
    val categoryName: String,
    val emoji: String,
    val spentRsd: Double,
    val limitRsd: Double,
    val percent: Int,
    val isExceeded: Boolean
)

class FinanceRepository(private val db: DinarFlowDatabase) {

    private val categoryDao = db.categoryDao()
    private val transactionDao = db.transactionDao()
    private val shiftDao = db.shiftDao()
    private val shiftPresetDao = db.shiftPresetDao()
    private val goalDao = db.goalDao()
    private val billDao = db.billDao()
    private val settingsDao = db.appSettingsDao()

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val categoriesFlow: Flow<List<CategoryEntity>> = categoryDao.observeAll()
    val transactionsFlow: Flow<List<TransactionEntity>> = transactionDao.observeAll()
    val shiftsFlow: Flow<List<ShiftEntity>> = shiftDao.observeAll()
    val shiftPresetsFlow: Flow<List<ShiftPresetEntity>> = shiftPresetDao.observeAll()
    val goalsFlow: Flow<List<GoalEntity>> = goalDao.observeAll()
    val billsFlow: Flow<List<BillEntity>> = billDao.observeAll()
    val settingsFlow: Flow<AppSettingsEntity> = settingsDao.observe().map { it ?: AppSettingsEntity() }

    suspend fun ensureSeeded() {
        db.withTransaction {
            if (settingsDao.getOnce() == null) {
                settingsDao.upsert(AppSettingsEntity())
            }
            if (categoryDao.getAllOnce().isEmpty()) {
                categoryDao.insertAll(defaultCategories())
            }
            if (shiftPresetDao.getAllOnce().isEmpty()) {
                shiftPresetDao.insertAll(defaultShiftPresets())
            }
        }
    }

    suspend fun addTransaction(
        type: String,
        amountRsd: Double,
        categoryId: String,
        date: String,
        note: String,
        linkedGoalId: String? = null
    ): BudgetAlert? {
        val cleanAmount = CurrencyFormatter.roundRsd(amountRsd)
        if (cleanAmount <= 0.0) return null

        return db.withTransaction {
            val resolvedGoalId = resolveGoalLinkIfNeeded(
                type = type,
                categoryId = categoryId,
                note = note,
                explicitGoalId = linkedGoalId
            )

            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = type,
                amountRsd = cleanAmount,
                categoryId = categoryId,
                date = date,
                note = note.trim(),
                linkedGoalId = resolvedGoalId,
                createdAt = System.currentTimeMillis()
            )
            transactionDao.upsert(tx)

            if (resolvedGoalId != null) {
                adjustGoalSaved(resolvedGoalId, cleanAmount)
            }

            checkBudgetAlert(type = type, categoryId = categoryId, monthStr = DateTimeUtils.monthOf(date))
        }
    }

    suspend fun updateTransaction(updated: TransactionEntity): BudgetAlert? {
        val cleanAmount = CurrencyFormatter.roundRsd(updated.amountRsd)
        if (cleanAmount <= 0.0) return null

        return db.withTransaction {
            val old = transactionDao.getById(updated.id) ?: return@withTransaction null

            // Sync linked shift tips if this transaction was linked to a shift
            var nextLinkedShiftId = updated.linkedShiftId
            if (old.linkedShiftId != null) {
                val shift = shiftDao.getById(old.linkedShiftId)
                if (shift != null) {
                    if (updated.type == CategoryType.INCOME && updated.categoryId == SystemCategoryIds.TIPS) {
                        shiftDao.upsert(shift.copy(tipsRsd = cleanAmount))
                    } else {
                        shiftDao.upsert(shift.copy(tipsRsd = 0.0))
                        nextLinkedShiftId = null
                    }
                } else {
                    nextLinkedShiftId = null
                }
            }

            // Clear bill link if user changed type away from EXPENSE
            val nextBillId = if (updated.type == CategoryType.EXPENSE) updated.linkedBillId else null
            val nextBillMonth = if (nextBillId != null) updated.linkedBillMonth else null

            // Resolve goal linkage if category is cat_goals
            val nextGoalId = when {
                updated.type == CategoryType.EXPENSE && updated.categoryId == SystemCategoryIds.GOALS -> {
                    resolveGoalLinkIfNeeded(
                        type = updated.type,
                        categoryId = updated.categoryId,
                        note = updated.note,
                        explicitGoalId = updated.linkedGoalId
                    )
                }
                else -> null
            }

            val savedTx = updated.copy(
                amountRsd = cleanAmount,
                note = updated.note.trim(),
                linkedShiftId = nextLinkedShiftId,
                linkedGoalId = nextGoalId,
                linkedBillId = nextBillId,
                linkedBillMonth = nextBillMonth
            )
            transactionDao.upsert(savedTx)

            if (old.linkedGoalId != null && old.linkedGoalId == nextGoalId) {
                adjustGoalSaved(nextGoalId, cleanAmount - old.amountRsd)
            } else {
                if (old.linkedGoalId != null) adjustGoalSaved(old.linkedGoalId, -old.amountRsd)
                if (nextGoalId != null) adjustGoalSaved(nextGoalId, cleanAmount)
            }

            checkBudgetAlert(
                type = savedTx.type,
                categoryId = savedTx.categoryId,
                monthStr = DateTimeUtils.monthOf(savedTx.date)
            )
        }
    }

    suspend fun deleteTransaction(txId: String) {
        db.withTransaction {
            val existing = transactionDao.getById(txId) ?: return@withTransaction
            transactionDao.deleteById(txId)

            if (existing.linkedShiftId != null) {
                val shift = shiftDao.getById(existing.linkedShiftId)
                if (shift != null) {
                    shiftDao.upsert(shift.copy(tipsRsd = 0.0))
                }
            }
            if (existing.linkedGoalId != null) {
                adjustGoalSaved(existing.linkedGoalId, -existing.amountRsd)
            }
        }
    }

    private suspend fun resolveGoalLinkIfNeeded(
        type: String,
        categoryId: String,
        note: String,
        explicitGoalId: String?
    ): String? {
        if (type != CategoryType.EXPENSE || categoryId != SystemCategoryIds.GOALS) return null
        if (explicitGoalId != null) return explicitGoalId
        val goals = goalDao.getAllOnce()
        if (goals.isEmpty()) return null
        val lowerNote = note.lowercase()
        val matched = goals.firstOrNull { lowerNote.contains(it.name.lowercase()) }
        if (matched != null) return matched.id
        return if (goals.size == 1) goals.first().id else null
    }

    private suspend fun adjustGoalSaved(goalId: String, deltaRsd: Double) {
        val goal = goalDao.getById(goalId) ?: return
        val nextSaved = CurrencyFormatter.roundRsd((goal.savedAmountRsd + deltaRsd).coerceAtLeast(0.0))
        goalDao.upsert(goal.copy(savedAmountRsd = nextSaved))
    }

    private suspend fun checkBudgetAlert(
        type: String,
        categoryId: String,
        monthStr: String
    ): BudgetAlert? {
        if (type != CategoryType.EXPENSE) return null
        val cat = categoryDao.getById(categoryId) ?: return null
        if (cat.monthlyLimitRsd <= 0.0) return null
        val spent = transactionDao.getCategoryMonthlyExpense(categoryId, monthStr)
        val ratio = spent / cat.monthlyLimitRsd
        val percent = (ratio * 100.0).toInt()
        return if (percent >= 80) {
            BudgetAlert(
                categoryName = cat.name,
                emoji = cat.emoji,
                spentRsd = spent,
                limitRsd = cat.monthlyLimitRsd,
                percent = percent,
                isExceeded = percent >= 100
            )
        } else {
            null
        }
    }

    // ================= SHIFTS =================

    suspend fun saveShift(
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
        db.withTransaction {
            val hours = DateTimeUtils.calculateShiftHours(startTime, endTime)
            val basePay = CurrencyFormatter.roundRsd(hours * hourlyRate)
            val cleanTips = if (isPlanned) 0.0 else CurrencyFormatter.roundRsd(tipsRsd.coerceAtLeast(0.0))
            val shiftId = id ?: UUID.randomUUID().toString()

            val entity = ShiftEntity(
                id = shiftId,
                date = date,
                startTime = startTime,
                endTime = endTime,
                durationHours = hours,
                hourlyRate = CurrencyFormatter.roundRsd(hourlyRate),
                basePay = basePay,
                tipsRsd = cleanTips,
                isPlanned = isPlanned,
                usesDefaultRate = usesDefaultRate,
                note = note.trim()
            )
            shiftDao.upsert(entity)

            if (!isPlanned) {
                // Prevent duplicate planned shift on the same date when moving or converting
                shiftDao.deletePlannedOnDateExcept(date = date, excludeId = shiftId)
                syncTipsTransactionForShift(entity)
            } else {
                // Planned shift must not have tips transaction
                transactionDao.deleteByLinkedShiftId(shiftId)
            }
        }
    }

    private suspend fun syncTipsTransactionForShift(shift: ShiftEntity) {
        val existing = transactionDao.getByLinkedShiftId(shift.id)
        if (shift.tipsRsd > 0.0) {
            val tx = TransactionEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                type = CategoryType.INCOME,
                amountRsd = shift.tipsRsd,
                categoryId = SystemCategoryIds.TIPS,
                date = shift.date,
                note = "Чаевые (${shift.startTime}–${shift.endTime})",
                linkedShiftId = shift.id,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            transactionDao.upsert(tx)
        } else if (existing != null) {
            transactionDao.deleteById(existing.id)
        }
    }

    suspend fun deleteShift(shiftId: String, deleteLinkedTips: Boolean) {
        db.withTransaction {
            val linkedTx = transactionDao.getByLinkedShiftId(shiftId)
            if (linkedTx != null) {
                if (deleteLinkedTips) {
                    transactionDao.deleteById(linkedTx.id)
                } else {
                    transactionDao.upsert(linkedTx.copy(linkedShiftId = null))
                }
            }
            shiftDao.deleteById(shiftId)
        }
    }

    suspend fun creditSalary(amountRsd: Double, date: String, note: String) {
        addTransaction(
            type = CategoryType.INCOME,
            amountRsd = amountRsd,
            categoryId = SystemCategoryIds.SALARY,
            date = date,
            note = note
        )
    }

    suspend fun addShiftPreset(startTime: String, endTime: String) {
        db.withTransaction {
            val current = shiftPresetDao.getAllOnce()
            val maxOrder = current.maxOfOrNull { it.sortOrder } ?: -1
            shiftPresetDao.upsert(
                ShiftPresetEntity(
                    id = UUID.randomUUID().toString(),
                    startTime = startTime,
                    endTime = endTime,
                    sortOrder = maxOrder + 1
                )
            )
        }
    }

    suspend fun deleteShiftPreset(id: String) {
        shiftPresetDao.deleteById(id)
    }

    // ================= GOALS =================

    suspend fun createGoal(
        name: String,
        targetAmountRsd: Double,
        initialSavedRsd: Double,
        emoji: String,
        colorHex: Long
    ): BudgetAlert? {
        return db.withTransaction {
            val current = goalDao.getAllOnce()
            val maxOrder = current.maxOfOrNull { it.sortOrder } ?: -1
            val goal = GoalEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                targetAmountRsd = CurrencyFormatter.roundRsd(targetAmountRsd),
                savedAmountRsd = 0.0,
                emoji = emoji,
                colorHex = colorHex,
                sortOrder = maxOrder + 1
            )
            goalDao.upsert(goal)
            if (initialSavedRsd > 0.0) {
                depositToGoalInternal(
                    goal = goal,
                    amountRsd = initialSavedRsd,
                    date = DateTimeUtils.todayStr(),
                    extraNote = "Начальный взнос"
                )
            } else {
                null
            }
        }
    }

    suspend fun updateGoal(
        goalId: String,
        name: String,
        targetAmountRsd: Double,
        newSavedAmountRsd: Double,
        emoji: String,
        colorHex: Long
    ): BudgetAlert? {
        return db.withTransaction {
            val existing = goalDao.getById(goalId) ?: return@withTransaction null
            val updated = existing.copy(
                name = name.trim(),
                targetAmountRsd = CurrencyFormatter.roundRsd(targetAmountRsd),
                emoji = emoji,
                colorHex = colorHex
            )
            goalDao.upsert(updated)

            val delta = CurrencyFormatter.roundRsd(newSavedAmountRsd - existing.savedAmountRsd)
            var alert: BudgetAlert? = null
            if (delta > 0.009) {
                alert = depositToGoalInternal(
                    goal = updated,
                    amountRsd = delta,
                    date = DateTimeUtils.todayStr(),
                    extraNote = "Корректировка"
                )
            } else if (delta < -0.009) {
                withdrawFromGoalInternal(goalId = goalId, amountRsd = -delta)
            }
            alert
        }
    }

    suspend fun depositToGoal(
        goalId: String,
        amountRsd: Double,
        date: String = DateTimeUtils.todayStr(),
        extraNote: String = ""
    ): BudgetAlert? {
        val clean = CurrencyFormatter.roundRsd(amountRsd)
        if (clean <= 0.0) return null
        return db.withTransaction {
            val goal = goalDao.getById(goalId) ?: return@withTransaction null
            depositToGoalInternal(goal, clean, date, extraNote)
        }
    }

    private suspend fun depositToGoalInternal(
        goal: GoalEntity,
        amountRsd: Double,
        date: String,
        extraNote: String
    ): BudgetAlert? {
        val clean = CurrencyFormatter.roundRsd(amountRsd)
        if (clean <= 0.0) return null

        // Merge same-day contributions for this goal into one transaction for that date
        val existingToday = transactionDao.getGoalTransactionForDate(goal.id, date)
        if (existingToday != null) {
            val updatedNote = if (extraNote.isNotBlank() && !existingToday.note.contains(extraNote)) {
                "${existingToday.note} · $extraNote"
            } else {
                existingToday.note
            }
            transactionDao.upsert(
                existingToday.copy(
                    amountRsd = CurrencyFormatter.roundRsd(existingToday.amountRsd + clean),
                    note = updatedNote
                )
            )
        } else {
            val noteText = buildString {
                append("Цель: ${goal.name}")
                if (extraNote.isNotBlank()) append(" · $extraNote")
            }
            transactionDao.upsert(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = CategoryType.EXPENSE,
                    amountRsd = clean,
                    categoryId = SystemCategoryIds.GOALS,
                    date = date,
                    note = noteText,
                    linkedGoalId = goal.id,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        adjustGoalSaved(goal.id, clean)
        return checkBudgetAlert(
            type = CategoryType.EXPENSE,
            categoryId = SystemCategoryIds.GOALS,
            monthStr = DateTimeUtils.monthOf(date)
        )
    }

    suspend fun withdrawFromGoal(goalId: String, amountRsd: Double) {
        val clean = CurrencyFormatter.roundRsd(amountRsd)
        if (clean <= 0.0) return
        db.withTransaction {
            withdrawFromGoalInternal(goalId, clean)
        }
    }

    private suspend fun withdrawFromGoalInternal(goalId: String, amountRsd: Double) {
        val lifoList = transactionDao.getGoalTransactionsLifo(goalId)
        var remaining = CurrencyFormatter.roundRsd(amountRsd)
        for (tx in lifoList) {
            if (remaining <= 0.004) break
            if (tx.amountRsd <= remaining + 0.004) {
                remaining = CurrencyFormatter.roundRsd(remaining - tx.amountRsd)
                transactionDao.deleteById(tx.id)
            } else {
                val newAmount = CurrencyFormatter.roundRsd(tx.amountRsd - remaining)
                transactionDao.upsert(tx.copy(amountRsd = newAmount))
                remaining = 0.0
            }
        }
        adjustGoalSaved(goalId, -amountRsd)
    }

    suspend fun deleteGoal(goalId: String, refundTransactions: Boolean) {
        db.withTransaction {
            if (refundTransactions) {
                transactionDao.deleteByLinkedGoalId(goalId)
            } else {
                transactionDao.unlinkGoal(goalId)
            }
            goalDao.deleteById(goalId)
        }
    }

    // ================= BILLS =================

    suspend fun saveBill(
        id: String?,
        name: String,
        amountRsd: Double,
        categoryId: String,
        dueDay: Int,
        emoji: String
    ) {
        db.withTransaction {
            val existing = if (id != null) billDao.getById(id) else null
            billDao.upsert(
                BillEntity(
                    id = id ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    amountRsd = CurrencyFormatter.roundRsd(amountRsd),
                    categoryId = categoryId,
                    dueDay = dueDay.coerceIn(1, 31),
                    emoji = emoji,
                    manualPaidMonths = existing?.manualPaidMonths ?: ""
                )
            )
        }
    }

    suspend fun payBill(billId: String, monthStr: String): BudgetAlert? {
        return db.withTransaction {
            val bill = billDao.getById(billId) ?: return@withTransaction null
            val existingTx = transactionDao.getBillTransactionForMonth(billId, monthStr)
            if (existingTx != null) return@withTransaction null

            val today = DateTimeUtils.todayStr()
            val payDate = if (DateTimeUtils.monthOf(today) == monthStr) {
                today
            } else {
                DateTimeUtils.clampedDateInMonth(monthStr, bill.dueDay)
            }

            transactionDao.upsert(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = CategoryType.EXPENSE,
                    amountRsd = bill.amountRsd,
                    categoryId = bill.categoryId,
                    date = payDate,
                    note = "Счёт: ${bill.name}",
                    linkedBillId = bill.id,
                    linkedBillMonth = monthStr,
                    createdAt = System.currentTimeMillis()
                )
            )
            checkBudgetAlert(CategoryType.EXPENSE, bill.categoryId, monthStr)
        }
    }

    suspend fun toggleBillManualPaid(billId: String, monthStr: String) {
        db.withTransaction {
            val bill = billDao.getById(billId) ?: return@withTransaction
            val linkedTx = transactionDao.getBillTransactionForMonth(billId, monthStr)
            val manualSet = bill.manualPaidMonths.split(",").filter { it.isNotBlank() }.toMutableSet()

            if (linkedTx != null) {
                // Remove linked transaction when unmarking payment
                transactionDao.deleteById(linkedTx.id)
                manualSet.remove(monthStr)
            } else if (manualSet.contains(monthStr)) {
                manualSet.remove(monthStr)
            } else {
                manualSet.add(monthStr)
            }
            billDao.upsert(bill.copy(manualPaidMonths = manualSet.sorted().joinToString(",")))
        }
    }

    suspend fun deleteBill(billId: String) {
        billDao.deleteById(billId)
    }

    // ================= CATEGORIES =================

    suspend fun saveCategory(
        id: String?,
        name: String,
        emoji: String,
        colorHex: Long,
        type: String,
        monthlyLimitRsd: Double
    ) {
        db.withTransaction {
            val existing = if (id != null) categoryDao.getById(id) else null
            val allOfType = categoryDao.getAllOnce().filter { it.type == type }
            val nextOrder = existing?.sortOrder ?: ((allOfType.maxOfOrNull { it.sortOrder } ?: -1) + 1)

            categoryDao.upsert(
                CategoryEntity(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    emoji = emoji,
                    colorHex = colorHex,
                    type = existing?.type ?: type,
                    monthlyLimitRsd = if (type == CategoryType.EXPENSE) {
                        CurrencyFormatter.roundRsd(monthlyLimitRsd.coerceAtLeast(0.0))
                    } else 0.0,
                    sortOrder = nextOrder,
                    isSystem = existing?.isSystem ?: false
                )
            )
        }
    }

    suspend fun updateCategoryLimit(categoryId: String, monthlyLimitRsd: Double) {
        db.withTransaction {
            val cat = categoryDao.getById(categoryId) ?: return@withTransaction
            categoryDao.upsert(
                cat.copy(monthlyLimitRsd = CurrencyFormatter.roundRsd(monthlyLimitRsd.coerceAtLeast(0.0)))
            )
        }
    }

    suspend fun reorderCategories(orderedList: List<CategoryEntity>) {
        db.withTransaction {
            val updated = orderedList.mapIndexed { idx, cat -> cat.copy(sortOrder = idx) }
            categoryDao.updateAll(updated)
        }
    }

    suspend fun deleteCategory(categoryId: String) {
        db.withTransaction {
            val cat = categoryDao.getById(categoryId) ?: return@withTransaction
            if (cat.isSystem) return@withTransaction
            categoryDao.deleteById(categoryId)
        }
    }

    // ================= SETTINGS =================

    suspend fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        db.withTransaction {
            val current = settingsDao.getOnce() ?: AppSettingsEntity()
            settingsDao.upsert(transform(current))
        }
    }

    suspend fun updateBaseHourlyRate(newRate: Double, updateFuturePlanned: Boolean) {
        val cleanRate = CurrencyFormatter.roundRsd(newRate.coerceAtLeast(0.0))
        db.withTransaction {
            val current = settingsDao.getOnce() ?: AppSettingsEntity()
            settingsDao.upsert(current.copy(hourlyRate = cleanRate))

            if (updateFuturePlanned) {
                val today = DateTimeUtils.todayStr()
                val futurePlanned = shiftDao.getAllOnce().filter {
                    it.isPlanned && it.usesDefaultRate && it.date >= today
                }
                if (futurePlanned.isNotEmpty()) {
                    shiftDao.insertAll(
                        futurePlanned.map { shift ->
                            shift.copy(
                                hourlyRate = cleanRate,
                                basePay = CurrencyFormatter.roundRsd(shift.durationHours * cleanRate)
                            )
                        }
                    )
                }
            }
        }
    }

    // ================= BACKUP / SYNC / RESET =================

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val payload = BackupPayload(
            settings = settingsDao.getOnce() ?: AppSettingsEntity(),
            categories = categoryDao.getAllOnce(),
            transactions = transactionDao.getAllOnce(),
            shifts = shiftDao.getAllOnce(),
            shiftPresets = shiftPresetDao.getAllOnce(),
            goals = goalDao.getAllOnce(),
            bills = billDao.getAllOnce()
        )
        BackupSerializer.toJson(payload)
    }

    suspend fun exportCsv(): String = withContext(Dispatchers.IO) {
        val settings = settingsDao.getOnce() ?: AppSettingsEntity()
        val categories = categoryDao.getAllOnce().associateBy { it.id }
        val transactions = transactionDao.getAllOnce()
        BackupSerializer.toCsv(transactions, categories, settings.eurRate)
    }

    suspend fun importJson(rawJson: String) = withContext(Dispatchers.IO) {
        val payload = BackupSerializer.fromJson(rawJson)
        db.withTransaction {
            transactionDao.clearAll()
            shiftDao.clearAll()
            shiftPresetDao.clearAll()
            goalDao.clearAll()
            billDao.clearAll()
            categoryDao.clearAll()
            settingsDao.clearAll()

            settingsDao.upsert(payload.settings)
            val cats = if (payload.categories.isNotEmpty()) payload.categories else defaultCategories()
            categoryDao.insertAll(ensureSystemCategoriesPresent(cats))
            val presets = if (payload.shiftPresets.isNotEmpty()) payload.shiftPresets else defaultShiftPresets()
            shiftPresetDao.insertAll(presets)
            if (payload.transactions.isNotEmpty()) transactionDao.insertAll(payload.transactions)
            if (payload.shifts.isNotEmpty()) shiftDao.insertAll(payload.shifts)
            if (payload.goals.isNotEmpty()) goalDao.insertAll(payload.goals)
            if (payload.bills.isNotEmpty()) billDao.insertAll(payload.bills)
        }
    }

    suspend fun resetAllData() {
        db.withTransaction {
            transactionDao.clearAll()
            shiftDao.clearAll()
            shiftPresetDao.clearAll()
            goalDao.clearAll()
            billDao.clearAll()
            categoryDao.clearAll()
            settingsDao.clearAll()

            settingsDao.upsert(AppSettingsEntity())
            categoryDao.insertAll(defaultCategories())
            shiftPresetDao.insertAll(defaultShiftPresets())
        }
    }

    suspend fun uploadBackupToUrl(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanUrl = url.trim()
            require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                "Введите корректный URL (http:// или https://)"
            }
            val json = exportJson()
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(cleanUrl)
                .put(body)
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // Fallback to POST if PUT is not accepted by endpoint
                    val postReq = Request.Builder().url(cleanUrl).post(body).build()
                    okHttpClient.newCall(postReq).execute().use { postResp ->
                        if (!postResp.isSuccessful) {
                            error("Ошибка сервера: HTTP ${postResp.code}")
                        }
                    }
                }
            }
            "Резервная копия отправлена"
        }
    }

    suspend fun downloadBackupFromUrl(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanUrl = url.trim()
            require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                "Введите корректный URL (http:// или https://)"
            }
            val request = Request.Builder().url(cleanUrl).get().build()
            val bodyStr = okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Ошибка загрузки: HTTP ${response.code}")
                response.body?.string() ?: error("Пустой ответ от сервера")
            }
            importJson(bodyStr)
            "Данные успешно загружены из облака"
        }
    }

    private fun ensureSystemCategoriesPresent(existing: List<CategoryEntity>): List<CategoryEntity> {
        val byId = existing.associateBy { it.id }.toMutableMap()
        defaultCategories().filter { it.isSystem }.forEach { sysCat ->
            if (!byId.containsKey(sysCat.id)) {
                byId[sysCat.id] = sysCat
            }
        }
        return byId.values.toList()
    }

    private fun defaultCategories(): List<CategoryEntity> = listOf(
        // Expenses
        CategoryEntity("cat_food", "Продукты", "🛒", 0xFF10B981, CategoryType.EXPENSE, 0.0, 0, false),
        CategoryEntity("cat_home", "Дом и Инфостан", "🏠", 0xFF3B82F6, CategoryType.EXPENSE, 0.0, 1, false),
        CategoryEntity("cat_cafe", "Кафе и рестораны", "☕", 0xFFF59E0B, CategoryType.EXPENSE, 0.0, 2, false),
        CategoryEntity("cat_pets", "Питомцы", "🐾", 0xFFEC4899, CategoryType.EXPENSE, 0.0, 3, false),
        CategoryEntity("cat_games", "Игры и подписки", "🎮", 0xFF8B5CF6, CategoryType.EXPENSE, 0.0, 4, false),
        CategoryEntity("cat_transport", "Транспорт", "🚕", 0xFF06B6D4, CategoryType.EXPENSE, 0.0, 5, false),
        CategoryEntity(SystemCategoryIds.GOALS, "Цели", "🎯", 0xFF14B8A6, CategoryType.EXPENSE, 0.0, 6, true),
        // Income
        CategoryEntity(SystemCategoryIds.SALARY, "Зарплата", "💼", 0xFF10B981, CategoryType.INCOME, 0.0, 0, true),
        CategoryEntity(SystemCategoryIds.TIPS, "Чаевые со смен", "🪙", 0xFFF59E0B, CategoryType.INCOME, 0.0, 1, true),
        CategoryEntity("cat_side", "Подработка / Бонус", "💻", 0xFF6366F1, CategoryType.INCOME, 0.0, 2, false)
    )

    private fun defaultShiftPresets(): List<ShiftPresetEntity> = listOf(
        ShiftPresetEntity("preset_1", "17:00", "01:00", 0),
        ShiftPresetEntity("preset_2", "17:00", "02:00", 1),
        ShiftPresetEntity("preset_3", "11:30", "02:00", 2),
        ShiftPresetEntity("preset_4", "11:30", "17:00", 3)
    )
}
