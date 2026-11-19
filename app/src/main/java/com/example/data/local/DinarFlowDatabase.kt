package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        ShiftEntity::class,
        ShiftPresetEntity::class,
        GoalEntity::class,
        BillEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DinarFlowDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun shiftDao(): ShiftDao
    abstract fun shiftPresetDao(): ShiftPresetDao
    abstract fun goalDao(): GoalDao
    abstract fun billDao(): BillDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: DinarFlowDatabase? = null

        fun getInstance(context: Context): DinarFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DinarFlowDatabase::class.java,
                    "dinarflow_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
