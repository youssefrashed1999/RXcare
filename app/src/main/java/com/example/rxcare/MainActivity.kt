package com.example.rxcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rxcare.di.KoinApplication
import com.example.rxcare.presentation.navigation.RxCareNavigation
import com.example.rxcare.presentation.viewmodel.AuthViewModel
import com.example.rxcare.ui.theme.RxCareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize dependency injection
        KoinApplication.initialize(this)
        
        setContent {
            RxCareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RxCareNavigation()
                }
            }
        }
    }
}