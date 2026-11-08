package com.example.dataone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.feature.telegram.data.TelegramRepositoryImpl
import com.example.feature.telegram.ui.TelegramScreen
import com.example.feature.telegram.ui.TelegramViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = TelegramRepositoryImpl(applicationContext)
        val telegramViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TelegramViewModel(repository) as T
                }
            }
        )[TelegramViewModel::class.java]

        setContent {
            TelegramScreen(
                viewModel = telegramViewModel,
                onNavigateBack = { }
            )
        }
    }
}
