package com.example.rxcare.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import com.example.rxcare.ui.theme.AmberBackground
import com.example.rxcare.ui.theme.AmberText
import com.example.rxcare.ui.theme.BlueBackground
import com.example.rxcare.ui.theme.BlueText
import com.example.rxcare.ui.theme.ChipShape
import com.example.rxcare.ui.theme.GreenBackground
import com.example.rxcare.ui.theme.GreenText

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status.uppercase()) {
        "PENDING" -> AmberBackground to AmberText
        "CLAIMED" -> GreenBackground to GreenText
        "DONE" -> BlueBackground to BlueText
        else -> AmberBackground to AmberText
    }

    Text(
        text = status.uppercase(),
        modifier = modifier
            .clip(ChipShape)
            .background(backgroundColor)
            .padding(vertical = 3.dp, horizontal = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        color = textColor
    )
}
