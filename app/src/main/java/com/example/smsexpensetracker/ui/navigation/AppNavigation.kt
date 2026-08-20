package com.example.smsexpensetracker.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.ui.details.TransactionDetailScreen
import com.example.smsexpensetracker.ui.home.HomeScreen
import com.example.smsexpensetracker.ui.home.HomeViewModel
import com.example.smsexpensetracker.ui.onboarding.PermissionsOnboardingScreen
import com.example.smsexpensetracker.ui.reports.ReportsScreen
import com.example.smsexpensetracker.ui.reports.ReportsViewModel
import com.example.smsexpensetracker.ui.review.FlaggedReviewScreen
import com.example.smsexpensetracker.ui.review.FlaggedReviewViewModel
import com.example.smsexpensetracker.ui.settings.SettingsScreen
import com.example.smsexpensetracker.ui.settings.SettingsViewModel

@Composable
fun AppNavigation(
    hasSmsPermission: Boolean,
    onRequestPermissions: () -> Unit,
    homeViewModel: HomeViewModel,
    reportsViewModel: ReportsViewModel,
    reviewViewModel: FlaggedReviewViewModel,
    settingsViewModel: SettingsViewModel,
    onSaveTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember {
        mutableStateOf<Screen>(if (hasSmsPermission) Screen.Home else Screen.Onboarding)
    }

    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var previousScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    val navigateTo: (Screen) -> Unit = { screen ->
        previousScreen = currentScreen
        currentScreen = screen
    }

    val openDetail: (Transaction) -> Unit = { tx ->
        selectedTransaction = tx
        previousScreen = currentScreen
        currentScreen = Screen.Detail
    }

    BackHandler(enabled = currentScreen != Screen.Home && currentScreen != Screen.Onboarding) {
        if (currentScreen == Screen.Detail) {
            currentScreen = previousScreen
        } else {
            currentScreen = Screen.Home
        }
    }

    val showBottomBar = currentScreen in Screen.bottomNavItems

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Screen.bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = {
                                screen.icon?.let {
                                    Icon(imageVector = it, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title) }
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                Screen.Onboarding -> {
                    PermissionsOnboardingScreen(
                        onRequestPermissions = {
                            onRequestPermissions()
                            currentScreen = Screen.Home
                        },
                        onSkip = {
                            currentScreen = Screen.Home
                        }
                    )
                }
                Screen.Home -> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onTransactionClick = openDetail,
                        onNavigateToReview = { currentScreen = Screen.Review }
                    )
                }
                Screen.Reports -> {
                    ReportsScreen(
                        viewModel = reportsViewModel,
                        onTransactionClick = openDetail
                    )
                }
                Screen.Review -> {
                    FlaggedReviewScreen(
                        viewModel = reviewViewModel,
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }
                Screen.Settings -> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onTriggerScan = {
                            currentScreen = Screen.Home
                            homeViewModel.startHistoricalScan()
                        }
                    )
                }
                Screen.Detail -> {
                    selectedTransaction?.let { tx ->
                        TransactionDetailScreen(
                            transaction = tx,
                            onSave = { updated ->
                                onSaveTransaction(updated)
                            },
                            onDelete = { deleted ->
                                onDeleteTransaction(deleted)
                            },
                            onNavigateBack = {
                                currentScreen = previousScreen
                            }
                        )
                    }
                }
            }
        }
    }
}
