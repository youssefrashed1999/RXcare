package com.example.rxcare.presentation.ui.home

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.rxcare.data.remote.websocket.WebSocketClient

@Composable
fun ConnectionStatusIndicator(
    status: WebSocketClient.ConnectionStatus,
    modifier: Modifier = Modifier
) {
    val (color, description) = when (status) {
        WebSocketClient.ConnectionStatus.CONNECTED -> 
            Color(0xFF4CAF50) to "Connected"
        WebSocketClient.ConnectionStatus.CONNECTING -> 
            Color(0xFFFF9800) to "Connecting..."
        WebSocketClient.ConnectionStatus.DISCONNECTED -> 
            Color(0xFF9E9E9E) to "Disconnected"
        WebSocketClient.ConnectionStatus.ERROR -> 
            Color(0xFFF44336) to "Error"
    }
    
    IconButton(
        onClick = { },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.Circle,
            contentDescription = description,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
    }
}
