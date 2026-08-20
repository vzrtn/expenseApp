package com.example.smsexpensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Onboarding : Screen("onboarding", "Onboarding")
    object Home : Screen("home", "Dashboard", Icons.Default.Home)
    object Reports : Screen("reports", "Reports", Icons.Default.Assessment)
    object Review : Screen("review", "Review", Icons.Default.RateReview)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Detail : Screen("detail", "Transaction Detail")

    companion object {
        val bottomNavItems = listOf(Home, Reports, Review, Settings)
    }
}
