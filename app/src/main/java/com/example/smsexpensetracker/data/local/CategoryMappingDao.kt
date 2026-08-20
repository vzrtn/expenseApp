package com.example.smsexpensetracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smsexpensetracker.data.model.CategoryMapping
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryMappingDao {

    @Query("SELECT * FROM category_mappings ORDER BY keyword ASC")
    fun getAllMappings(): Flow<List<CategoryMapping>>

    @Query("SELECT * FROM category_mappings")
    suspend fun getAllMappingsSync(): List<CategoryMapping>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: CategoryMapping): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mappings: List<CategoryMapping>): List<Long>

    @Delete
    suspend fun delete(mapping: CategoryMapping): Int

    @Query("DELETE FROM category_mappings WHERE keyword = :keyword")
    suspend fun deleteByKeyword(keyword: String): Int

    @Query("DELETE FROM category_mappings")
    suspend fun deleteAll(): Int
}
