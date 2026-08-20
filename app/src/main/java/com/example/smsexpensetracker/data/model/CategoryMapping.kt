package com.example.smsexpensetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_mappings")
data class CategoryMapping(
    @PrimaryKey
    val keyword: String,
    val category: String
)
