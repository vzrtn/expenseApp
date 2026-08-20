package com.example.smsexpensetracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.smsexpensetracker.data.local.AppDatabase
import com.example.smsexpensetracker.data.repository.TransactionRepository
import com.example.smsexpensetracker.parser.ParseResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val sender = messages[0].displayOriginatingAddress ?: return
            val timestamp = messages[0].timestampMillis

            // Combine multi-part SMS messages
            val bodyBuilder = StringBuilder()
            for (msg in messages) {
                bodyBuilder.append(msg.displayMessageBody)
            }
            val body = bodyBuilder.toString()

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val repository = TransactionRepository(
                        db.transactionDao(),
                        db.categoryMappingDao(),
                        db.excludedSenderDao()
                    )

                    val result = repository.processAndInsertSms(sender, body, timestamp)
                    when (result) {
                        is ParseResult.Success -> {
                            NotificationHelper.showTransactionNotification(context, result.transaction)
                        }
                        is ParseResult.LowConfidence -> {
                            NotificationHelper.showTransactionNotification(context, result.candidate)
                        }
                        else -> {
                            // Ignored or failed
                        }
                    }
                } catch (_: Exception) {
                    // Safe error handling for background receiver
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
