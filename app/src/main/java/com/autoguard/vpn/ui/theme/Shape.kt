package com.autoguard.vpn.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * AutoGuard VPN Application Shape Definitions
 */
val Shapes = Shapes(
    // Extra Small - Used for small buttons, chips, etc.
    extraSmall = RoundedCornerShape(8.dp),

    // Small - Used for cards, small containers
    small = RoundedCornerShape(12.dp),

    // Medium - Used for buttons, dialogs
    medium = RoundedCornerShape(16.dp),

    // Large - Used for bottom sheets, large cards
    large = RoundedCornerShape(24.dp),

    // Extra Large - Used for full-screen sheets
    extraLarge = RoundedCornerShape(32.dp)
)

// Custom Shapes
val ButtonShape = RoundedCornerShape(16.dp)
val CardShape = RoundedCornerShape(16.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val ChipShape = RoundedCornerShape(8.dp)
val DialogShape = RoundedCornerShape(20.dp)
