package com.example.smsexpensetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.smsexpensetracker.data.local.AppDatabase
import com.example.smsexpensetracker.data.repository.TransactionRepository
import com.example.smsexpensetracker.service.NotificationHelper
import com.example.smsexpensetracker.theme.SMSExpenseTrackerTheme
import com.example.smsexpensetracker.ui.components.RationaleDialog
import com.example.smsexpensetracker.ui.home.HomeViewModel
import com.example.smsexpensetracker.ui.navigation.AppNavigation
import com.example.smsexpensetracker.ui.reports.ReportsViewModel
import com.example.smsexpensetracker.ui.review.FlaggedReviewViewModel
import com.example.smsexpensetracker.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val reportsViewModel: ReportsViewModel by viewModels()
    private val reviewViewModel: FlaggedReviewViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannel(this)

        val db = AppDatabase.getDatabase(this)
        val repository = TransactionRepository(
            db.transactionDao(),
            db.categoryMappingDao(),
            db.excludedSenderDao()
        )

        setContent {
            SMSExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var hasSmsPermission by remember {
                        mutableStateOf(checkSmsPermission())
                    }
                    var showRationale by remember { mutableStateOf(false) }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val readSmsGranted = permissions[Manifest.permission.READ_SMS] ?: false
                        val receiveSmsGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
                        if (readSmsGranted || receiveSmsGranted) {
                            hasSmsPermission = true
                            homeViewModel.startHistoricalScan()
                        }
                    }

                    val requestAllPermissions = {
                        val permissionsToRequest = mutableListOf(
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }

                    AppNavigation(
                        hasSmsPermission = hasSmsPermission,
                        onRequestPermissions = {
                            showRationale = true
                        },
                        homeViewModel = homeViewModel,
                        reportsViewModel = reportsViewModel,
                        reviewViewModel = reviewViewModel,
                        settingsViewModel = settingsViewModel,
                        onSaveTransaction = { tx ->
                            lifecycleScope.launch {
                                repository.updateTransaction(tx)
                            }
                        },
                        onDeleteTransaction = { tx ->
                            lifecycleScope.launch {
                                repository.deleteTransaction(tx)
                            }
                        }
                    )

                    if (showRationale) {
                        RationaleDialog(
                            onConfirm = {
                                showRationale = false
                                requestAllPermissions()
                            },
                            onDismiss = {
                                showRationale = false
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
