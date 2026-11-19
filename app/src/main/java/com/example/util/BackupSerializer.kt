package com.example.util

import com.example.data.local.AppSettingsEntity
import com.example.data.local.BillEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryType
import com.example.data.local.GoalEntity
import com.example.data.local.ShiftEntity
import com.example.data.local.ShiftPresetEntity
import com.example.data.local.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val settings: AppSettingsEntity,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val shifts: List<ShiftEntity>,
    val shiftPresets: List<ShiftPresetEntity>,
    val goals: List<GoalEntity>,
    val bills: List<BillEntity>
)

object BackupSerializer {

    fun toJson(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("app", "DinarFlow")
        root.put("schemaVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val s = payload.settings
        root.put("settings", JSONObject().apply {
            put("eurRate", s.eurRate)
            put("hourlyRate", s.hourlyRate)
            put("payday1Date", s.payday1Date)
            put("payday2Date", s.payday2Date)
            put("carryoverEnabled", s.carryoverEnabled)
            put("includePlannedInFree", s.includePlannedInFree)
            put("themeMode", s.themeMode)
            put("syncUrl", s.syncUrl)
        })

        root.put("categories", JSONArray().apply {
            payload.categories.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("emoji", c.emoji)
                    put("colorHex", c.colorHex)
                    put("type", c.type)
                    put("monthlyLimitRsd", c.monthlyLimitRsd)
                    put("sortOrder", c.sortOrder)
                    put("isSystem", c.isSystem)
                })
            }
        })

        root.put("transactions", JSONArray().apply {
            payload.transactions.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("type", t.type)
                    put("amountRsd", t.amountRsd)
                    put("categoryId", t.categoryId)
                    put("date", t.date)
                    put("note", t.note)
                    put("linkedShiftId", t.linkedShiftId ?: JSONObject.NULL)
                    put("linkedGoalId", t.linkedGoalId ?: JSONObject.NULL)
                    put("linkedBillId", t.linkedBillId ?: JSONObject.NULL)
                    put("linkedBillMonth", t.linkedBillMonth ?: JSONObject.NULL)
                    put("createdAt", t.createdAt)
                })
            }
        })

        root.put("shifts", JSONArray().apply {
            payload.shifts.forEach { sh ->
                put(JSONObject().apply {
                    put("id", sh.id)
                    put("date", sh.date)
                    put("startTime", sh.startTime)
                    put("endTime", sh.endTime)
                    put("durationHours", sh.durationHours)
                    put("hourlyRate", sh.hourlyRate)
                    put("basePay", sh.basePay)
                    put("tipsRsd", sh.tipsRsd)
                    put("isPlanned", sh.isPlanned)
                    put("usesDefaultRate", sh.usesDefaultRate)
                    put("note", sh.note)
                })
            }
        })

        root.put("shiftPresets", JSONArray().apply {
            payload.shiftPresets.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("startTime", p.startTime)
                    put("endTime", p.endTime)
                    put("sortOrder", p.sortOrder)
                })
            }
        })

        root.put("goals", JSONArray().apply {
            payload.goals.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id)
                    put("name", g.name)
                    put("targetAmountRsd", g.targetAmountRsd)
                    put("savedAmountRsd", g.savedAmountRsd)
                    put("emoji", g.emoji)
                    put("colorHex", g.colorHex)
                    put("sortOrder", g.sortOrder)
                })
            }
        })

        root.put("bills", JSONArray().apply {
            payload.bills.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("name", b.name)
                    put("amountRsd", b.amountRsd)
                    put("categoryId", b.categoryId)
                    put("dueDay", b.dueDay)
                    put("emoji", b.emoji)
                    put("manualPaidMonths", b.manualPaidMonths)
                })
            }
        })

        return root.toString(2)
    }

    fun fromJson(rawJson: String): BackupPayload {
        val root = JSONObject(rawJson)
        val settingsObj = root.optJSONObject("settings")
        val settings = if (settingsObj != null) {
            AppSettingsEntity(
                id = 1,
                eurRate = settingsObj.optDouble("eurRate", 117.0).takeIf { it > 0 } ?: 117.0,
                hourlyRate = settingsObj.optDouble("hourlyRate", 450.0).takeIf { it >= 0 } ?: 450.0,
                payday1Date = settingsObj.optInt("payday1Date", 25).coerceIn(1, 31),
                payday2Date = settingsObj.optInt("payday2Date", 10).coerceIn(1, 31),
                carryoverEnabled = settingsObj.optBoolean("carryoverEnabled", false),
                includePlannedInFree = settingsObj.optBoolean("includePlannedInFree", false),
                themeMode = settingsObj.optString("themeMode", "SYSTEM"),
                syncUrl = settingsObj.optString("syncUrl", "")
            )
        } else {
            AppSettingsEntity()
        }

        val categories = mutableListOf<CategoryEntity>()
        val catArr = root.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until catArr.length()) {
            val o = catArr.getJSONObject(i)
            categories.add(
                CategoryEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    emoji = o.optString("emoji", "📦"),
                    colorHex = o.optLong("colorHex", 0xFF10B981),
                    type = o.optString("type", CategoryType.EXPENSE),
                    monthlyLimitRsd = o.optDouble("monthlyLimitRsd", 0.0),
                    sortOrder = o.optInt("sortOrder", i),
                    isSystem = o.optBoolean("isSystem", false)
                )
            )
        }

        val transactions = mutableListOf<TransactionEntity>()
        val txArr = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txArr.length()) {
            val o = txArr.getJSONObject(i)
            transactions.add(
                TransactionEntity(
                    id = o.getString("id"),
                    type = o.getString("type"),
                    amountRsd = o.getDouble("amountRsd"),
                    categoryId = o.getString("categoryId"),
                    date = o.getString("date"),
                    note = o.optString("note", ""),
                    linkedShiftId = o.optStringOrNull("linkedShiftId"),
                    linkedGoalId = o.optStringOrNull("linkedGoalId"),
                    linkedBillId = o.optStringOrNull("linkedBillId"),
                    linkedBillMonth = o.optStringOrNull("linkedBillMonth"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val shifts = mutableListOf<ShiftEntity>()
        val shArr = root.optJSONArray("shifts") ?: JSONArray()
        for (i in 0 until shArr.length()) {
            val o = shArr.getJSONObject(i)
            shifts.add(
                ShiftEntity(
                    id = o.getString("id"),
                    date = o.getString("date"),
                    startTime = o.getString("startTime"),
                    endTime = o.getString("endTime"),
                    durationHours = o.getDouble("durationHours"),
                    hourlyRate = o.getDouble("hourlyRate"),
                    basePay = o.getDouble("basePay"),
                    tipsRsd = o.optDouble("tipsRsd", 0.0),
                    isPlanned = o.optBoolean("isPlanned", false),
                    usesDefaultRate = o.optBoolean("usesDefaultRate", true),
                    note = o.optString("note", "")
                )
            )
        }

        val presets = mutableListOf<ShiftPresetEntity>()
        val prArr = root.optJSONArray("shiftPresets") ?: JSONArray()
        for (i in 0 until prArr.length()) {
            val o = prArr.getJSONObject(i)
            presets.add(
                ShiftPresetEntity(
                    id = o.getString("id"),
                    startTime = o.getString("startTime"),
                    endTime = o.getString("endTime"),
                    sortOrder = o.optInt("sortOrder", i)
                )
            )
        }

        val goals = mutableListOf<GoalEntity>()
        val gArr = root.optJSONArray("goals") ?: JSONArray()
        for (i in 0 until gArr.length()) {
            val o = gArr.getJSONObject(i)
            goals.add(
                GoalEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    targetAmountRsd = o.getDouble("targetAmountRsd"),
                    savedAmountRsd = o.optDouble("savedAmountRsd", 0.0),
                    emoji = o.optString("emoji", "🎯"),
                    colorHex = o.optLong("colorHex", 0xFF10B981),
                    sortOrder = o.optInt("sortOrder", i)
                )
            )
        }

        val bills = mutableListOf<BillEntity>()
        val bArr = root.optJSONArray("bills") ?: JSONArray()
        for (i in 0 until bArr.length()) {
            val o = bArr.getJSONObject(i)
            bills.add(
                BillEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    amountRsd = o.getDouble("amountRsd"),
                    categoryId = o.getString("categoryId"),
                    dueDay = o.optInt("dueDay", 15).coerceIn(1, 31),
                    emoji = o.optString("emoji", "🧾"),
                    manualPaidMonths = o.optString("manualPaidMonths", "")
                )
            )
        }

        return BackupPayload(
            settings = settings,
            categories = categories,
            transactions = transactions,
            shifts = shifts,
            shiftPresets = presets,
            goals = goals,
            bills = bills
        )
    }

    fun toCsv(
        transactions: List<TransactionEntity>,
        categoriesById: Map<String, CategoryEntity>,
        eurRate: Double
    ): String {
        val sb = StringBuilder()
        sb.append('\uFEFF') // UTF-8 BOM for Excel
        sb.append("Дата;Тип;Категория;Сумма RSD;Сумма EUR;Заметка\n")
        for (tx in transactions) {
            val typeLabel = if (tx.type == CategoryType.INCOME) "Доход" else "Расход"
            val catName = categoriesById[tx.categoryId]?.let { "${it.emoji} ${it.name}" } ?: tx.categoryId
            val eur = CurrencyFormatter.toEur(tx.amountRsd, eurRate)
            val safeNote = tx.note.replace(";", ",").replace("\n", " ")
            sb.append("${tx.date};$typeLabel;$catName;${tx.amountRsd};$eur;$safeNote\n")
        }
        return sb.toString()
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key, "")
        return value.ifEmpty { null }
    }
}
