package com.example.rxcare.presentation.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rxcare.R
import com.example.rxcare.presentation.viewmodel.AuthViewModel
import com.example.rxcare.ui.theme.RxCareTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.koin.androidx.compose.koinViewModel

@Composable
fun SplashScreen(
    onNavigateToSignIn: () -> Unit,
    onNavigateToHome: () -> Unit,
    authViewModel: AuthViewModel = koinViewModel()
) {
    LaunchedEffect(Unit) {
        delay(2000) // Show splash for 2 seconds
        
        // Check authentication state directly from repository
        val isLoggedIn = authViewModel.isLoggedIn().first()
        if (isLoggedIn) {
            onNavigateToHome()
        } else {
            onNavigateToSignIn()
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFFFBFE)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo or icon
            Image(
                painter = painterResource(id = R.drawable.rx_care),
                contentDescription = "RxCare Logo",
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
