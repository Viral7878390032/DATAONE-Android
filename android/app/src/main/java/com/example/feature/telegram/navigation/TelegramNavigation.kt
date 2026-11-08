package com.example.feature.telegram.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.feature.telegram.ui.TelegramScreen
import com.example.feature.telegram.ui.TelegramViewModel

const val TELEGRAM_ROUTE = "feature_telegram_route"

/**
 * Clean navigation extension function for DATAONE NavHost.
 * Connects the Telegram feature without modifying existing routes for Gmail, Photos, Supabase Auth, etc.
 */
fun NavGraphBuilder.telegramScreen(
    viewModel: TelegramViewModel,
    onNavigateBack: () -> Unit
) {
    composable(route = TELEGRAM_ROUTE) {
        TelegramScreen(
            viewModel = viewModel,
            onNavigateBack = onNavigateBack
        )
    }
}

fun NavController.navigateToTelegram() {
    this.navigate(TELEGRAM_ROUTE)
}
