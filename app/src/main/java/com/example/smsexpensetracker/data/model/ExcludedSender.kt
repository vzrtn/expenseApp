package com.example.smsexpensetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "excluded_senders")
data class ExcludedSender(
    @PrimaryKey
    val senderPattern: String,
    val description: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis()
)
