package com.example.smsexpensetracker.service

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.smsexpensetracker.data.local.AppDatabase
import com.example.smsexpensetracker.data.repository.TransactionRepository
import com.example.smsexpensetracker.parser.ParseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoricalSmsScanWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_SCANNED = "scanned"
        const val KEY_TOTAL = "total"
        const val KEY_FOUND = "found"
        const val KEY_PERCENT = "percent"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val contentResolver = applicationContext.contentResolver
        val uri: Uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        var scannedCount = 0
        var foundCount = 0

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = TransactionRepository(
            db.transactionDao(),
            db.categoryMappingDao(),
            db.excludedSenderDao()
        )

        try {
            val cursor = contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val totalMessages = it.count
                val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    if (isStopped) {
                        return@withContext Result.failure()
                    }

                    val sender = it.getString(addressIdx) ?: ""
                    val body = it.getString(bodyIdx) ?: ""
                    val date = it.getLong(dateIdx)

                    scannedCount++

                    if (sender.isNotBlank() && body.isNotBlank()) {
                        val result = repository.processAndInsertSms(sender, body, date)
                        if (result is ParseResult.Success || result is ParseResult.LowConfidence) {
                            foundCount++
                        }
                    }

                    // Update progress periodically every 25 messages or at the end
                    if (scannedCount % 25 == 0 || scannedCount == totalMessages) {
                        val percent = if (totalMessages > 0) (scannedCount * 100) / totalMessages else 100
                        setProgress(
                            workDataOf(
                                KEY_SCANNED to scannedCount,
                                KEY_TOTAL to totalMessages,
                                KEY_FOUND to foundCount,
                                KEY_PERCENT to percent
                            )
                        )
                    }
                }
            }

            Result.success(
                workDataOf(
                    KEY_SCANNED to scannedCount,
                    KEY_FOUND to foundCount
                )
            )
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to (e.message ?: "SMS inbox access error")))
        }
    }
}
