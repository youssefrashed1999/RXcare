package com.example.rxcare.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rxcare.ui.theme.BrandPrimary
import com.example.rxcare.ui.theme.CardBorder
import com.example.rxcare.ui.theme.CardShape
import com.example.rxcare.ui.theme.CardSurface
import com.example.rxcare.ui.theme.DividerHairline
import com.example.rxcare.ui.theme.GreenText
import com.example.rxcare.ui.theme.TextHint
import com.example.rxcare.ui.theme.TextPrimary
import com.example.rxcare.ui.theme.TextSecondary

@Composable
fun RequestCard(
    chatId: String,
    status: String,
    title: String,
    subtitle: String,
    timestamp: String,
    onAction: () -> Unit,
    onClaimAction: () -> Unit = {},
    currentUserRole: com.example.rxcare.domain.model.UserRole = com.example.rxcare.domain.model.UserRole.CLIENT,
    modifier: Modifier = Modifier
) {
    val isClaimed = status.equals("CLAIMED", ignoreCase = true)
    val cardBorder = if (isClaimed) {
        BorderStroke(1.5.dp, GreenText)
    } else {
        BorderStroke(1.dp, CardBorder)
    }
    
    val actionText = when (status.uppercase()) {
        "PENDING" -> when (currentUserRole) {
            com.example.rxcare.domain.model.UserRole.PHARMACIST -> "Claim →"
            com.example.rxcare.domain.model.UserRole.CLIENT -> "View →"
            else -> "View →"
        }
        "CLAIMED" -> "Open chat →"
        "DONE" -> "View history →"
        else -> "View →"
    }
    
    val actionColor = when (status.uppercase()) {
        "DONE" -> TextHint
        else -> BrandPrimary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(CardSurface)
            .border(cardBorder, CardShape)
            .clickable { onAction() }
            .padding(12.dp)
    ) {
        // Top row: title and status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            StatusBadge(status = status)
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Middle: subtitle only
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextHint,
            fontSize = 11.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Divider
        Divider(
            color = DividerHairline,
            thickness = 1.dp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Bottom row: timestamp and action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = com.example.rxcare.ui.theme.SpaceMono),
                color = TextHint,
                fontSize = 10.sp
            )
            
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodyMedium,
                color = actionColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier.clickable {
                    // Handle claim action for pharmacists on pending requests
                    if (status.uppercase() == "PENDING" && currentUserRole == com.example.rxcare.domain.model.UserRole.PHARMACIST) {
                        onClaimAction()
                    }
                }
            )
        }
    }
}
