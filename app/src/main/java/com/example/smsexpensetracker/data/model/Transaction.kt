package com.example.smsexpensetracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["category"]),
        Index(value = ["paymentMode"]),
        Index(value = ["bank"]),
        Index(value = ["referenceId"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val transactionType: TransactionType,
    val bank: String,
    val paymentMode: PaymentMode,
    val paymentApp: String? = null,
    val merchantOrPayee: String,
    val accountLast4: String? = null,
    val referenceId: String? = null,
    val timestamp: Long,
    val rawSmsBody: String,
    val confidence: Float,
    val category: String,
    val sender: String,
    val isManualReviewRequired: Boolean = false,
    val isExcluded: Boolean = false,
    val notes: String? = null
)
