package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CategoryType
import com.example.data.local.DinarFlowDatabase
import com.example.data.local.SystemCategoryIds
import com.example.data.repository.FinanceRepository
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinanceRepositoryTest {

    private lateinit var db: DinarFlowDatabase
    private lateinit var repo: FinanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DinarFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = FinanceRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `first launch has empty data and seeded categories`() = runBlocking {
        repo.ensureSeeded()
        assertTrue(repo.transactionsFlow.first().isEmpty())
        assertTrue(repo.shiftsFlow.first().isEmpty())
        assertTrue(repo.goalsFlow.first().isEmpty())
        assertTrue(repo.billsFlow.first().isEmpty())
        assertEquals(10, repo.categoriesFlow.first().size)
        assertEquals(4, repo.shiftPresetsFlow.first().size)
    }

    @Test
    fun `overnight shift duration and tips sync work accurately`() = runBlocking {
        repo.ensureSeeded()
        assertEquals(8.0, DateTimeUtils.calculateShiftHours("17:00", "01:00"), 0.01)
        assertEquals(14.5, DateTimeUtils.calculateShiftHours("11:30", "02:00"), 0.01)

        repo.saveShift(
            id = null,
            date = "2026-11-15",
            startTime = "17:00",
            endTime = "01:00",
            hourlyRate = 500.0,
            tipsRsd = 2500.0,
            isPlanned = false,
            usesDefaultRate = true,
            note = ""
        )

        val txs = repo.transactionsFlow.first()
        assertEquals(1, txs.size)
        assertEquals(SystemCategoryIds.TIPS, txs.first().categoryId)
        assertEquals(2500.0, txs.first().amountRsd, 0.01)

        // Deleting linked tips transaction resets shift.tipsRsd to 0
        repo.deleteTransaction(txs.first().id)
        val shifts = repo.shiftsFlow.first()
        assertEquals(0.0, shifts.first().tipsRsd, 0.01)
    }

    @Test
    fun `goal same-day merge, multi-day separation, and LIFO refund`() = runBlocking {
        repo.ensureSeeded()
        repo.createGoal("Ноутбук", 100_000.0, 0.0, "💻", 0xFF10B981)
        val goalId = repo.goalsFlow.first().first().id

        // Two deposits on Day 1 should merge into 1 transaction
        repo.depositToGoal(goalId, 2000.0, "2026-10-01", "")
        repo.depositToGoal(goalId, 3000.0, "2026-10-01", "")
        // One deposit on Day 2 should stay separate
        repo.depositToGoal(goalId, 4000.0, "2026-11-05", "")

        val txsBefore = repo.transactionsFlow.first()
        assertEquals(2, txsBefore.size)
        assertEquals(9000.0, repo.goalsFlow.first().first().savedAmountRsd, 0.01)

        // LIFO refund of 5500 removes Nov 5 (4000) completely and deducts 1500 from Oct 1 (leaving 3500)
        repo.withdrawFromGoal(goalId, 5500.0)
        val txsAfter = repo.transactionsFlow.first()
        assertEquals(1, txsAfter.size)
        assertEquals("2026-10-01", txsAfter.first().date)
        assertEquals(3500.0, txsAfter.first().amountRsd, 0.01)
        assertEquals(3500.0, repo.goalsFlow.first().first().savedAmountRsd, 0.01)
    }

    @Test
    fun `json backup export and import roundtrip preserves state`() = runBlocking {
        repo.ensureSeeded()
        repo.addTransaction(CategoryType.INCOME, 50_000.0, SystemCategoryIds.SALARY, "2026-11-10", "ЗП")
        val json = repo.exportJson()
        repo.resetAllData()
        assertTrue(repo.transactionsFlow.first().isEmpty())

        repo.importJson(json)
        val restored = repo.transactionsFlow.first()
        assertEquals(1, restored.size)
        assertEquals(50_000.0, restored.first().amountRsd, 0.01)
    }
}
