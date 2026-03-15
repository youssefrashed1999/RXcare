package com.example.rxcare.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rxcare.domain.model.Message
import com.example.rxcare.ui.theme.*

@Composable
fun SystemEventBubble(
    message: Message,
    eventType: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val (backgroundColor, textColor) = when (eventType) {
            "pharmacist_joined" -> GreenBackground.copy(alpha = 0.5f) to GreenText
            "chat_completed" -> BlueBackground.copy(alpha = 0.5f) to BlueText
            else -> BrandLight.copy(alpha = 0.5f) to BrandPrimary
        }
        
        Text(
            text = when (eventType) {
                "pharmacist_joined" -> "Pharmacist joined the chat"
                "chat_completed" -> "Chat completed"
                else -> message.content
            },
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            color = textColor,
            style = MaterialTheme.typography.caption,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
