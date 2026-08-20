package com.example.smsexpensetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.smsexpensetracker.data.model.CategoryMapping
import com.example.smsexpensetracker.data.model.ExcludedSender
import com.example.smsexpensetracker.data.model.Transaction

@Database(
    entities = [
        Transaction::class,
        CategoryMapping::class,
        ExcludedSender::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryMappingDao(): CategoryMappingDao
    abstract fun excludedSenderDao(): ExcludedSenderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_expense_tracker.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
