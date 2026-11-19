package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CategoryType
import com.example.data.local.DinarFlowDatabase
import com.example.data.local.SystemCategoryIds
import com.example.data.repository.FinanceRepository
import com.example.util.BackupSerializer
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DinarFlow", appName)
    }

    @Test
    fun `import web app json format parses all sections accurately`() {
        val webJson = """
            {
              "categories": [
                {
                  "id": "cat_groceries",
                  "name": "Продукты",
                  "type": "expense",
                  "icon": "🛒",
                  "color": "#10b981",
                  "monthlyBudget": 25000
                },
                {
                  "id": "cat_tips",
                  "name": "Чаевые",
                  "type": "income",
                  "icon": "🪙",
                  "color": "#f59e0b",
                  "monthlyBudget": 0
                }
              ],
              "transactions": [
                {
                  "id": "tx_tip_1795044005455",
                  "type": "income",
                  "amount": 900,
                  "categoryId": "cat_tips",
                  "note": "Чай 18.11 (16:30–00:30)",
                  "date": "2026-11-18"
                },
                {
                  "id": "tx_bill_bill_1793618408794_1793618411072",
                  "type": "expense",
                  "amount": 52650,
                  "categoryId": "cat_groceries",
                  "note": "Аренда",
                  "date": "2026-11-02"
                }
              ],
              "shifts": [
                {
                  "id": "sh_1795044005455",
                  "date": "2026-11-18",
                  "startTime": "16:30",
                  "endTime": "00:30",
                  "hours": 8,
                  "rate": 450,
                  "basePay": 3600,
                  "tips": 900,
                  "total": 4500,
                  "tipTxId": "tx_tip_1795044005455"
                }
              ],
              "plannedShifts": [
                {
                  "id": "pl_1793618329924",
                  "date": "2026-11-19",
                  "startTime": "17:00",
                  "endTime": "01:00",
                  "hours": 8,
                  "rate": 450
                }
              ],
              "savingsGoals": [],
              "recurringBills": [
                {
                  "id": "bill_1793618408794",
                  "name": "Аренда",
                  "amount": 52650,
                  "categoryId": "cat_groceries",
                  "dayOfMonth": 5,
                  "icon": "🏠",
                  "paidMonths": ["2026-11"]
                }
              ],
              "shiftPresets": [
                { "id": "pr_1", "start": "17:00", "end": "01:00" }
              ],
              "shiftSettings": {
                "hourlyRate": 450,
                "payday1Date": 25,
                "payday2Date": 10,
                "eurRate": 117
              },
              "carryoverEnabled": true,
              "cloudEndpoint": ""
            }
        """.trimIndent()

        val payload = BackupSerializer.fromJson(webJson)

        assertTrue(payload.settings.carryoverEnabled)
        assertEquals(450.0, payload.settings.hourlyRate, 0.01)
        assertEquals(117.0, payload.settings.eurRate, 0.01)
        assertEquals(25, payload.settings.payday1Date)
        assertEquals(10, payload.settings.payday2Date)

        assertEquals(2, payload.categories.size)
        assertEquals("🛒", payload.categories[0].emoji)
        assertEquals(0xFF10B981L, payload.categories[0].colorHex)
        assertEquals(25000.0, payload.categories[0].monthlyLimitRsd, 0.01)
        assertEquals(CategoryType.EXPENSE, payload.categories[0].type)
        assertEquals(CategoryType.INCOME, payload.categories[1].type)

        assertEquals(2, payload.shifts.size)
        val workedShift = payload.shifts[0]
        assertEquals(false, workedShift.isPlanned)
        assertEquals(8.0, workedShift.durationHours, 0.01)
        assertEquals(900.0, workedShift.tipsRsd, 0.01)

        val plannedShift = payload.shifts[1]
        assertTrue(plannedShift.isPlanned)
        assertEquals(3600.0, plannedShift.basePay, 0.01)

        assertEquals(2, payload.transactions.size)
        assertEquals("sh_1795044005455", payload.transactions[0].linkedShiftId)
        assertEquals("bill_1793618408794", payload.transactions[1].linkedBillId)
        assertEquals("2026-11", payload.transactions[1].linkedBillMonth)

        assertEquals(1, payload.bills.size)
        assertEquals(5, payload.bills[0].dueDay)
        assertEquals("🏠", payload.bills[0].emoji)
        assertEquals("2026-11", payload.bills[0].manualPaidMonths)

        assertEquals(1, payload.shiftPresets.size)
        assertEquals("17:00", payload.shiftPresets[0].startTime)
        assertEquals("01:00", payload.shiftPresets[0].endTime)
    }

    @Test
    fun `currency formatter handles amounts above 21 million RSD without Int overflow`() {
        assertEquals(25_000_000.0, CurrencyFormatter.round2(25_000_000.0), 0.001)
        assertEquals("25 000 000 RSD", CurrencyFormatter.formatRsd(25_000_000.0))
        assertEquals("25,0M", CurrencyFormatter.formatCompactRsd(25_000_000.0))
    }

    @Test
    fun `elapsed days in period handles DST transitions accurately`() {
        val originalTz = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Belgrade"))
            assertEquals(31, DateTimeUtils.elapsedDaysInPeriod("2026-03-01", "2026-03-31"))
            assertEquals(31, DateTimeUtils.elapsedDaysInPeriod("2026-10-01", "2026-10-31"))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun `repository preserves imported goal savedAmount on deposit edit and delete and clears bill link on type change`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, DinarFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = FinanceRepository(db)
            val jsonWithGoalAndBill = """
                {
                  "categories": [
                    {"id": "cat_housing", "name": "Квартира", "type": "expense", "icon": "🏠", "color": "#6366f1", "monthlyBudget": 0},
                    {"id": "cat_goals", "name": "Цели", "type": "expense", "icon": "🎯", "color": "#14b8a6", "monthlyBudget": 0}
                  ],
                  "transactions": [],
                  "shifts": [],
                  "plannedShifts": [],
                  "savingsGoals": [
                    {"id": "goal_mac", "name": "MacBook", "targetAmount": 100000, "savedAmount": 15000, "icon": "💻", "color": "#38bdf8"}
                  ],
                  "recurringBills": [
                    {"id": "bill_rent", "name": "Аренда", "amount": 50000, "categoryId": "cat_housing", "dayOfMonth": 5, "icon": "🏠", "paidMonths": []}
                  ],
                  "shiftPresets": [],
                  "shiftSettings": {"hourlyRate": 450, "payday1Date": 25, "payday2Date": 10, "eurRate": 117}
                }
            """.trimIndent()

            repo.importJson(jsonWithGoalAndBill)

            // Deposit 2000 into imported goal
            repo.depositToGoal("goal_mac", 2000.0, "2026-11-18", "Доплата")
            var goal = repo.goalsFlow.first().first { it.id == "goal_mac" }
            assertEquals(17000.0, goal.savedAmountRsd, 0.01)

            // Update that deposit transaction to 3500
            val goalTx = repo.transactionsFlow.first().first { it.linkedGoalId == "goal_mac" }
            repo.updateTransaction(goalTx.copy(amountRsd = 3500.0))
            goal = repo.goalsFlow.first().first { it.id == "goal_mac" }
            assertEquals(18500.0, goal.savedAmountRsd, 0.01)

            // Delete that deposit transaction -> imported 15000 remains intact!
            repo.deleteTransaction(goalTx.id)
            goal = repo.goalsFlow.first().first { it.id == "goal_mac" }
            assertEquals(15000.0, goal.savedAmountRsd, 0.01)

            // Pay bill then change its transaction type to INCOME -> clears linkedBillId & linkedBillMonth
            repo.payBill("bill_rent", "2026-11")
            val billTx = repo.transactionsFlow.first().first { it.linkedBillId == "bill_rent" }
            repo.updateTransaction(billTx.copy(type = CategoryType.INCOME, categoryId = SystemCategoryIds.SALARY))
            val updatedBillTx = repo.transactionsFlow.first().first { it.id == billTx.id }
            assertNull(updatedBillTx.linkedBillId)
            assertNull(updatedBillTx.linkedBillMonth)
        } finally {
            db.close()
        }
    }
}

