package com.example.smsexpensetracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smsexpensetracker.data.model.ExcludedSender
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcludedSenderDao {

    @Query("SELECT * FROM excluded_senders ORDER BY addedTimestamp DESC")
    fun getAllExcludedSenders(): Flow<List<ExcludedSender>>

    @Query("SELECT * FROM excluded_senders")
    suspend fun getAllExcludedSendersSync(): List<ExcludedSender>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sender: ExcludedSender): Long

    @Delete
    suspend fun delete(sender: ExcludedSender): Int

    @Query("DELETE FROM excluded_senders WHERE senderPattern = :pattern")
    suspend fun deleteByPattern(pattern: String): Int

    @Query("DELETE FROM excluded_senders")
    suspend fun deleteAll(): Int
}
