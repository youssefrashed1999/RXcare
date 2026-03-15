package com.example.rxcare.presentation.ui.components

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rxcare.presentation.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun WebSocketLifecycleManager(
    homeViewModel: HomeViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {

                    // App returned to foreground, silently ensure WebSocket is connected
                    scope.launch {
                        try {
                            val token = homeViewModel.getAuthToken()
                            val role = homeViewModel.getUserRole()
                            
                            if (token.isNotEmpty() && role.isNotEmpty()) {
                                homeViewModel.reconnectWebSockets(role, token)
                            }
                        } catch (e: Exception) {
                            // Silent failure - don't notify UI
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // App going to background - keep WebSocket connected
                    // Don't disconnect here as we want to maintain connection
                }
                else -> { /* Other lifecycle events */ }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
