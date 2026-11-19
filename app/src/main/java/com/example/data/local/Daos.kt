package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type ASC, sortOrder ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY type ASC, sortOrder ASC")
    suspend fun getAllOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CategoryEntity)

    @Update
    suspend fun updateAll(items: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE linkedShiftId = :shiftId LIMIT 1")
    suspend fun getByLinkedShiftId(shiftId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE linkedGoalId = :goalId AND categoryId = 'cat_goals' AND type = 'EXPENSE' ORDER BY date DESC, createdAt DESC")
    suspend fun getGoalTransactionsLifo(goalId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE linkedGoalId = :goalId AND categoryId = 'cat_goals' AND type = 'EXPENSE' AND date = :date LIMIT 1")
    suspend fun getGoalTransactionForDate(goalId: String, date: String): TransactionEntity?

    @Query("SELECT COALESCE(SUM(amountRsd), 0.0) FROM transactions WHERE linkedGoalId = :goalId AND categoryId = 'cat_goals' AND type = 'EXPENSE'")
    suspend fun getGoalSavedSum(goalId: String): Double

    @Query("SELECT * FROM transactions WHERE linkedBillId = :billId AND linkedBillMonth = :monthStr LIMIT 1")
    suspend fun getBillTransactionForMonth(billId: String, monthStr: String): TransactionEntity?

    @Query("SELECT COALESCE(SUM(amountRsd), 0.0) FROM transactions WHERE type = 'EXPENSE' AND categoryId = :categoryId AND substr(date, 1, 7) = :monthStr")
    suspend fun getCategoryMonthlyExpense(categoryId: String, monthStr: String): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions WHERE linkedShiftId = :shiftId")
    suspend fun deleteByLinkedShiftId(shiftId: String)

    @Query("DELETE FROM transactions WHERE linkedGoalId = :goalId")
    suspend fun deleteByLinkedGoalId(goalId: String)

    @Query("UPDATE transactions SET linkedGoalId = NULL WHERE linkedGoalId = :goalId")
    suspend fun unlinkGoal(goalId: String)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY date DESC, startTime DESC")
    fun observeAll(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts ORDER BY date DESC, startTime DESC")
    suspend fun getAllOnce(): List<ShiftEntity>

    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE date = :date AND isPlanned = 1")
    suspend fun getPlannedForDate(date: String): List<ShiftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShiftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShiftEntity>)

    @Query("DELETE FROM shifts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM shifts WHERE date = :date AND isPlanned = 1 AND id != :excludeId")
    suspend fun deletePlannedOnDateExcept(date: String, excludeId: String)

    @Query("DELETE FROM shifts")
    suspend fun clearAll()
}

@Dao
interface ShiftPresetDao {
    @Query("SELECT * FROM shift_presets ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<ShiftPresetEntity>>

    @Query("SELECT * FROM shift_presets ORDER BY sortOrder ASC")
    suspend fun getAllOnce(): List<ShiftPresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShiftPresetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShiftPresetEntity>)

    @Query("DELETE FROM shift_presets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM shift_presets")
    suspend fun clearAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllOnce(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GoalEntity>)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM goals")
    suspend fun clearAll()
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY dueDay ASC, name ASC")
    fun observeAll(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills ORDER BY dueDay ASC, name ASC")
    suspend fun getAllOnce(): List<BillEntity>

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BillEntity>)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM bills")
    suspend fun clearAll()
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettingsEntity)

    @Query("DELETE FROM app_settings")
    suspend fun clearAll()
}
