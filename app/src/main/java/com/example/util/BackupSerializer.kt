package com.example.util

import com.example.data.local.AppSettingsEntity
import com.example.data.local.BillEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryType
import com.example.data.local.GoalEntity
import com.example.data.local.ShiftEntity
import com.example.data.local.ShiftPresetEntity
import com.example.data.local.SystemCategoryIds
import com.example.data.local.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

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
        val cleaned = rawJson.trim().removePrefix("\uFEFF")
        val root = JSONObject(cleaned)

        // Supports both native "settings" and web-app "shiftSettings" + top-level flags
        val settingsObj = root.optJSONObject("settings") ?: root.optJSONObject("shiftSettings")
        val eurRate = settingsObj?.optDoubleOrNull("eurRate")?.takeIf { it > 0.0 }
            ?: root.optDoubleOrNull("eurRate")?.takeIf { it > 0.0 }
            ?: 117.0
        val hourlyRate = settingsObj?.optDoubleOrNull("hourlyRate")?.takeIf { it >= 0.0 }
            ?: root.optDoubleOrNull("hourlyRate")?.takeIf { it >= 0.0 }
            ?: 450.0
        val payday1Date = (settingsObj?.optIntOrNull("payday1Date")
            ?: root.optIntOrNull("payday1Date") ?: 25).coerceIn(1, 31)
        val payday2Date = (settingsObj?.optIntOrNull("payday2Date")
            ?: root.optIntOrNull("payday2Date") ?: 10).coerceIn(1, 31)
        val carryoverEnabled = settingsObj?.optBooleanOrNull("carryoverEnabled")
            ?: root.optBooleanOrNull("carryoverEnabled")
            ?: false
        val includePlannedInFree = settingsObj?.optBooleanOrNull("includePlannedInFree")
            ?: root.optBooleanOrNull("includePlannedInFree")
            ?: false
        val themeMode = settingsObj?.optStringOrNull("themeMode")
            ?: root.optStringOrNull("themeMode")
            ?: "DARK"
        val syncUrl = settingsObj?.optStringOrNull("syncUrl")
            ?: root.optStringOrNull("syncUrl")
            ?: root.optStringOrNull("cloudEndpoint")
            ?: ""

        val settings = AppSettingsEntity(
            id = 1,
            eurRate = eurRate,
            hourlyRate = hourlyRate,
            payday1Date = payday1Date,
            payday2Date = payday2Date,
            carryoverEnabled = carryoverEnabled,
            includePlannedInFree = includePlannedInFree,
            themeMode = themeMode,
            syncUrl = syncUrl
        )

        val categories = mutableListOf<CategoryEntity>()
        val catArr = root.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until catArr.length()) {
            val o = catArr.getJSONObject(i)
            val id = o.getString("id")
            val isSysDefault = id == SystemCategoryIds.GOALS ||
                id == SystemCategoryIds.SALARY ||
                id == SystemCategoryIds.TIPS
            categories.add(
                CategoryEntity(
                    id = id,
                    name = o.getString("name"),
                    emoji = o.optStringOrNull("emoji") ?: o.optStringOrNull("icon") ?: "📦",
                    colorHex = parseColorHex(o),
                    type = normalizeType(o.optString("type", CategoryType.EXPENSE)),
                    monthlyLimitRsd = o.optDoubleOrNull("monthlyLimitRsd")
                        ?: o.optDoubleOrNull("monthlyBudget")
                        ?: 0.0,
                    sortOrder = o.optInt("sortOrder", i),
                    isSystem = if (o.has("isSystem")) o.optBoolean("isSystem", isSysDefault) else isSysDefault
                )
            )
        }

        // Parse worked + planned shifts first so tipTxId links can be restored onto transactions
        val shifts = mutableListOf<ShiftEntity>()
        val tipTxIdToShiftId = mutableMapOf<String, String>()

        val shArr = root.optJSONArray("shifts") ?: JSONArray()
        for (i in 0 until shArr.length()) {
            val o = shArr.getJSONObject(i)
            val id = o.getString("id")
            val startTime = o.getString("startTime")
            val endTime = o.getString("endTime")
            val durationHours = o.optDoubleOrNull("durationHours")
                ?: o.optDoubleOrNull("hours")
                ?: DateTimeUtils.calculateShiftHours(startTime, endTime)
            val shiftRate = o.optDoubleOrNull("hourlyRate")
                ?: o.optDoubleOrNull("rate")
                ?: hourlyRate
            val basePay = o.optDoubleOrNull("basePay")
                ?: CurrencyFormatter.roundRsd(durationHours * shiftRate)
            val tipsRsd = o.optDoubleOrNull("tipsRsd")
                ?: o.optDoubleOrNull("tips")
                ?: 0.0
            val tipTxId = o.optStringOrNull("tipTxId")
            if (tipTxId != null) {
                tipTxIdToShiftId[tipTxId] = id
            }
            shifts.add(
                ShiftEntity(
                    id = id,
                    date = o.getString("date"),
                    startTime = startTime,
                    endTime = endTime,
                    durationHours = durationHours,
                    hourlyRate = shiftRate,
                    basePay = basePay,
                    tipsRsd = tipsRsd,
                    isPlanned = o.optBoolean("isPlanned", false),
                    usesDefaultRate = if (o.has("usesDefaultRate")) {
                        o.optBoolean("usesDefaultRate", true)
                    } else {
                        abs(shiftRate - hourlyRate) < 0.01
                    },
                    note = o.optString("note", "")
                )
            )
        }

        val plannedArr = root.optJSONArray("plannedShifts") ?: JSONArray()
        for (i in 0 until plannedArr.length()) {
            val o = plannedArr.getJSONObject(i)
            val startTime = o.getString("startTime")
            val endTime = o.getString("endTime")
            val durationHours = o.optDoubleOrNull("durationHours")
                ?: o.optDoubleOrNull("hours")
                ?: DateTimeUtils.calculateShiftHours(startTime, endTime)
            val shiftRate = o.optDoubleOrNull("hourlyRate")
                ?: o.optDoubleOrNull("rate")
                ?: hourlyRate
            val basePay = o.optDoubleOrNull("basePay")
                ?: CurrencyFormatter.roundRsd(durationHours * shiftRate)
            shifts.add(
                ShiftEntity(
                    id = o.getString("id"),
                    date = o.getString("date"),
                    startTime = startTime,
                    endTime = endTime,
                    durationHours = durationHours,
                    hourlyRate = shiftRate,
                    basePay = basePay,
                    tipsRsd = o.optDoubleOrNull("tipsRsd") ?: o.optDoubleOrNull("tips") ?: 0.0,
                    isPlanned = true,
                    usesDefaultRate = if (o.has("usesDefaultRate")) {
                        o.optBoolean("usesDefaultRate", true)
                    } else {
                        abs(shiftRate - hourlyRate) < 0.01
                    },
                    note = o.optString("note", "")
                )
            )
        }

        val bills = mutableListOf<BillEntity>()
        val bArr = root.optJSONArray("bills") ?: root.optJSONArray("recurringBills") ?: JSONArray()
        for (i in 0 until bArr.length()) {
            val o = bArr.getJSONObject(i)
            val manualPaidMonths = o.optStringOrNull("manualPaidMonths") ?: run {
                val arr = o.optJSONArray("paidMonths")
                if (arr != null) {
                    (0 until arr.length()).mapNotNull { idx ->
                        arr.optString(idx).takeIf { it.isNotBlank() }
                    }.joinToString(",")
                } else ""
            }
            bills.add(
                BillEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    amountRsd = o.optDoubleOrNull("amountRsd") ?: o.optDoubleOrNull("amount") ?: 0.0,
                    categoryId = o.getString("categoryId"),
                    dueDay = (o.optIntOrNull("dueDay") ?: o.optIntOrNull("dayOfMonth") ?: 15).coerceIn(1, 31),
                    emoji = o.optStringOrNull("emoji") ?: o.optStringOrNull("icon") ?: "🧾",
                    manualPaidMonths = manualPaidMonths
                )
            )
        }

        val transactions = mutableListOf<TransactionEntity>()
        val txArr = root.optJSONArray("transactions") ?: JSONArray()
        val nowMs = System.currentTimeMillis()
        for (i in 0 until txArr.length()) {
            val o = txArr.getJSONObject(i)
            val id = o.getString("id")
            val date = o.getString("date")
            val amountRsd = o.optDoubleOrNull("amountRsd") ?: o.optDoubleOrNull("amount") ?: 0.0
            val matchedBillId = o.optStringOrNull("linkedBillId") ?: bills.firstOrNull { b ->
                id.startsWith("tx_bill_${b.id}_")
            }?.id
            val matchedBillMonth = o.optStringOrNull("linkedBillMonth")
                ?: if (matchedBillId != null) DateTimeUtils.monthOf(date) else null

            transactions.add(
                TransactionEntity(
                    id = id,
                    type = normalizeType(o.optString("type", CategoryType.EXPENSE)),
                    amountRsd = amountRsd,
                    categoryId = o.getString("categoryId"),
                    date = date,
                    note = o.optString("note", ""),
                    linkedShiftId = o.optStringOrNull("linkedShiftId") ?: tipTxIdToShiftId[id],
                    linkedGoalId = o.optStringOrNull("linkedGoalId"),
                    linkedBillId = matchedBillId,
                    linkedBillMonth = matchedBillMonth,
                    createdAt = o.optLong("createdAt", nowMs - i)
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
                    startTime = o.optStringOrNull("startTime") ?: o.getString("start"),
                    endTime = o.optStringOrNull("endTime") ?: o.getString("end"),
                    sortOrder = o.optInt("sortOrder", i)
                )
            )
        }

        val goals = mutableListOf<GoalEntity>()
        val gArr = root.optJSONArray("goals") ?: root.optJSONArray("savingsGoals") ?: JSONArray()
        for (i in 0 until gArr.length()) {
            val o = gArr.getJSONObject(i)
            goals.add(
                GoalEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    targetAmountRsd = o.optDoubleOrNull("targetAmountRsd")
                        ?: o.optDoubleOrNull("targetAmount")
                        ?: o.optDoubleOrNull("target")
                        ?: 0.0,
                    savedAmountRsd = o.optDoubleOrNull("savedAmountRsd")
                        ?: o.optDoubleOrNull("savedAmount")
                        ?: o.optDoubleOrNull("saved")
                        ?: 0.0,
                    emoji = o.optStringOrNull("emoji") ?: o.optStringOrNull("icon") ?: "🎯",
                    colorHex = parseColorHex(o),
                    sortOrder = o.optInt("sortOrder", i)
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

    private fun normalizeType(raw: String): String {
        return if (raw.equals(CategoryType.INCOME, ignoreCase = true)) {
            CategoryType.INCOME
        } else {
            CategoryType.EXPENSE
        }
    }

    private fun parseColorHex(obj: JSONObject): Long {
        if (obj.has("colorHex") && !obj.isNull("colorHex")) {
            val numeric = obj.optLong("colorHex", -1L)
            if (numeric > 0L) return numeric
            val hexStr = obj.optString("colorHex", "")
            parseHexColorString(hexStr)?.let { return it }
        }
        if (obj.has("color") && !obj.isNull("color")) {
            val hexStr = obj.optString("color", "")
            parseHexColorString(hexStr)?.let { return it }
        }
        return 0xFF38BDF8L
    }

    private fun parseHexColorString(raw: String): Long? {
        val cleaned = raw.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
        if (cleaned.isEmpty()) return null
        val parsed = cleaned.toLongOrNull(16) ?: return null
        return when (cleaned.length) {
            6 -> 0xFF000000L or parsed
            8 -> parsed
            else -> null
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key, "")
        return value.ifEmpty { null }
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = optDouble(key, Double.NaN)
        return if (value.isNaN()) null else value
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key)
    }

    private fun JSONObject.optBooleanOrNull(key: String): Boolean? {
        if (!has(key) || isNull(key)) return null
        return optBoolean(key)
    }
}
