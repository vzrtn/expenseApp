package com.example.smsexpensetracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.smsexpensetracker.MainActivity
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.TransactionType
import com.example.smsexpensetracker.util.FormatUtils

object NotificationHelper {

    private const val CHANNEL_ID = "sms_expense_tracker_channel"
    private const val CHANNEL_NAME = "Expense Alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for detected transactions from SMS"
                }
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(channel)
            } catch (_: Exception) {
                // Ignore channel creation errors on restricted ROMs
            }
        }
    }

    fun showTransactionNotification(context: Context, transaction: Transaction) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            transaction.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = FormatUtils.formatInr(transaction.amount)

        val title = if (transaction.transactionType == TransactionType.DEBIT) {
            "Spent $formattedAmount"
        } else {
            "Received $formattedAmount"
        }

        val content = if (transaction.transactionType == TransactionType.DEBIT) {
            "Paid to ${transaction.merchantOrPayee} (${transaction.category}) via ${transaction.bank}"
        } else {
            "From ${transaction.merchantOrPayee} to ${transaction.bank}"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(transaction.id.toInt(), notification)
        } catch (_: SecurityException) {
            // Notification permission not granted
        }
    }
}
