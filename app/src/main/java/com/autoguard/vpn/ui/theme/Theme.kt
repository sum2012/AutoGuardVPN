package com.autoguard.vpn.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark theme color scheme
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = Color.White,

    secondary = PrimaryBlueLight,
    onSecondary = Color.White,
    secondaryContainer = PrimaryBlueDark,
    onSecondaryContainer = Color.White,

    tertiary = GradientEnd,
    onTertiary = Color.White,
    tertiaryContainer = GradientEnd,
    onTertiaryContainer = Color.White,

    background = DarkBackground,
    onBackground = TextPrimaryDark,

    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,

    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed,
    onErrorContainer = Color.White,

    outline = DividerColor,
    outlineVariant = DividerColor
)

/**
 * Light theme color scheme
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = Color.White,

    secondary = PrimaryBlueDark,
    onSecondary = Color.White,
    secondaryContainer = PrimaryBlueLight,
    onSecondaryContainer = Color.White,

    tertiary = GradientEnd,
    onTertiary = Color.White,
    tertiaryContainer = GradientEnd,
    onTertiaryContainer = Color.White,

    background = LightBackground,
    onBackground = TextPrimaryLight,

    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,

    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed,
    onErrorContainer = Color.White,

    outline = DividerColor,
    outlineVariant = DividerColor
)

/**
 * AutoGuard VPN Application Theme
 * Supports Dark/Light modes and Dynamic Color (Android 12+)
 */
@Composable
fun AutoGuardVPNTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color (Android 12+)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic color priority
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Dark theme
        darkTheme -> DarkColorScheme
        // Light theme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

/**
 * Colors corresponding to VPN states
 */
object VpnStatusColors {
    @Composable
    fun getColor(state: com.autoguard.vpn.data.model.VpnConnectionState): Color {
        return when (state) {
            com.autoguard.vpn.data.model.VpnConnectionState.CONNECTED -> ConnectedGreen
            com.autoguard.vpn.data.model.VpnConnectionState.CONNECTING -> ConnectingYellow
            com.autoguard.vpn.data.model.VpnConnectionState.DISCONNECTING -> ConnectingYellow
            com.autoguard.vpn.data.model.VpnConnectionState.ERROR -> ErrorRed
            com.autoguard.vpn.data.model.VpnConnectionState.DISCONNECTED -> DisconnectedGray
        }
    }
}
