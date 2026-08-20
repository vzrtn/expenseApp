package com.example.smsexpensetracker.data.local

import androidx.room.TypeConverter
import com.example.smsexpensetracker.data.model.PaymentMode
import com.example.smsexpensetracker.data.model.TransactionType

class Converters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return try {
            TransactionType.valueOf(value)
        } catch (_: Exception) {
            TransactionType.DEBIT
        }
    }

    @TypeConverter
    fun fromPaymentMode(value: PaymentMode): String {
        return value.name
    }

    @TypeConverter
    fun toPaymentMode(value: String): PaymentMode {
        return try {
            PaymentMode.valueOf(value)
        } catch (_: Exception) {
            PaymentMode.OTHER
        }
    }
}
