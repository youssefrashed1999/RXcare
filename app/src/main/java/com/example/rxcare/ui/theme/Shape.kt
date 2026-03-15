package com.example.rxcare.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val Shapes = androidx.compose.material3.Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Custom shape tokens
val CardShape = RoundedCornerShape(14.dp)
val ButtonShape = RoundedCornerShape(12.dp)
val ChipShape = RoundedCornerShape(20.dp)
val InputShape = RoundedCornerShape(10.dp)
val AvatarShape = RoundedCornerShape(50.dp) // Fully rounded for circles
val WarningShape = RoundedCornerShape(8.dp)
