package com.example.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

object CategoryType {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
}

object SystemCategoryIds {
    const val GOALS = "cat_goals"
    const val SALARY = "cat_salary"
    const val TIPS = "cat_tips"
}

@Immutable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    val colorHex: Long,
    val type: String, // CategoryType.EXPENSE or CategoryType.INCOME
    val monthlyLimitRsd: Double = 0.0,
    val sortOrder: Int = 0,
    val isSystem: Boolean = false
)

@Immutable
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String, // CategoryType.EXPENSE or CategoryType.INCOME
    val amountRsd: Double,
    val categoryId: String,
    val date: String, // "YYYY-MM-DD"
    val note: String = "",
    val linkedShiftId: String? = null,
    val linkedGoalId: String? = null,
    val linkedBillId: String? = null,
    val linkedBillMonth: String? = null, // "YYYY-MM"
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    val date: String, // "YYYY-MM-DD"
    val startTime: String, // "HH:mm"
    val endTime: String, // "HH:mm"
    val durationHours: Double,
    val hourlyRate: Double,
    val basePay: Double,
    val tipsRsd: Double = 0.0,
    val isPlanned: Boolean = false,
    val usesDefaultRate: Boolean = true,
    val note: String = ""
)

@Immutable
@Entity(tableName = "shift_presets")
data class ShiftPresetEntity(
    @PrimaryKey val id: String,
    val startTime: String,
    val endTime: String,
    val sortOrder: Int = 0
)

@Immutable
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmountRsd: Double,
    val savedAmountRsd: Double = 0.0,
    val emoji: String = "🎯",
    val colorHex: Long = 0xFF10B981,
    val sortOrder: Int = 0
)

@Immutable
@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amountRsd: Double,
    val categoryId: String,
    val dueDay: Int = 15,
    val emoji: String = "🧾",
    val manualPaidMonths: String = "" // Comma-separated "YYYY-MM"
)

@Immutable
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val eurRate: Double = 117.0,
    val hourlyRate: Double = 450.0,
    val payday1Date: Int = 25,
    val payday2Date: Int = 10,
    val carryoverEnabled: Boolean = false,
    val includePlannedInFree: Boolean = false,
    val themeMode: String = "DARK",
    val syncUrl: String = ""
)
