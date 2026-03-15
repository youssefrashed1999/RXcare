package com.example.rxcare.di

import android.content.Context
import com.example.rxcare.data.local.PreferencesManager
import com.example.rxcare.data.repository.AuthRepository
import com.example.rxcare.data.repository.AuthRepositoryImpl
import com.example.rxcare.data.repository.ChatRepository
import com.example.rxcare.data.repository.ChatRepositoryImpl
import com.example.rxcare.domain.repository.AuthRepository as DomainAuthRepository
import com.example.rxcare.domain.repository.ChatRepository as DomainChatRepository
import com.example.rxcare.presentation.viewmodel.AuthViewModel
import com.example.rxcare.presentation.viewmodel.HomeViewModel
import com.example.rxcare.presentation.viewmodel.ChatViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    
    // Preferences
    single { PreferencesManager(androidContext()) }
    
    // API Repositories
    single<AuthRepository> { AuthRepository(get()) }
    single<ChatRepository> { ChatRepository(get()) }
    
    // Domain Repository Implementations
    single<DomainAuthRepository> { AuthRepositoryImpl(get()) }
    single<DomainChatRepository> { ChatRepositoryImpl(get(), get(), androidContext()) }
    
    // ViewModels
    viewModel { AuthViewModel(get(), get()) }
    viewModel { (currentUserId: String) -> HomeViewModel(get(), currentUserId) }
    viewModel { (currentUserId: String) -> ChatViewModel(get(), currentUserId) }
}

object KoinApplication {
    
    fun initialize(context: Context) {
        startKoin {
            androidContext(context)
            modules(appModule)
        }
    }
}
