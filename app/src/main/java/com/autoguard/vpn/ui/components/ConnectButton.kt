package com.autoguard.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.autoguard.vpn.R
import com.autoguard.vpn.data.model.VpnConnectionState
import com.autoguard.vpn.ui.theme.ConnectedGreen
import com.autoguard.vpn.ui.theme.ConnectingYellow
import com.autoguard.vpn.ui.theme.DisconnectedGray
import com.autoguard.vpn.ui.theme.ErrorRed
import com.autoguard.vpn.ui.theme.GradientConnectedEnd
import com.autoguard.vpn.ui.theme.GradientConnectedStart
import com.autoguard.vpn.ui.theme.GradientEnd
import com.autoguard.vpn.ui.theme.GradientStart

/**
 * VPN connection button component
 * Displays connection status and handles user clicks
 *
 * @param connectionState Current connection state
 * @param onClick Click event handler
 * @param modifier Modifier
 */
@Composable
fun ConnectButton(
    connectionState: VpnConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // Pulse animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Pulse alpha
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Get color based on state
    val buttonColor by animateColorAsState(
        targetValue = when (connectionState) {
            VpnConnectionState.CONNECTED -> ConnectedGreen
            VpnConnectionState.CONNECTING -> ConnectingYellow
            VpnConnectionState.DISCONNECTING -> ConnectingYellow
            VpnConnectionState.ERROR -> ErrorRed
            VpnConnectionState.DISCONNECTED -> DisconnectedGray
        },
        animationSpec = tween(300),
        label = "button_color"
    )

    // Gradient colors
    val gradientColors = when (connectionState) {
        VpnConnectionState.CONNECTED -> listOf(GradientConnectedStart, GradientConnectedEnd)
        VpnConnectionState.CONNECTING -> listOf(ConnectingYellow, ConnectingYellow.copy(alpha = 0.8f))
        VpnConnectionState.DISCONNECTING -> listOf(ConnectingYellow, ConnectingYellow.copy(alpha = 0.8f))
        VpnConnectionState.ERROR -> listOf(ErrorRed, ErrorRed.copy(alpha = 0.8f))
        VpnConnectionState.DISCONNECTED -> listOf(GradientStart, GradientEnd)
    }

    val isConnecting = connectionState == VpnConnectionState.CONNECTING ||
            connectionState == VpnConnectionState.DISCONNECTING

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Pulse halo (only during connecting)
        if (isConnecting && connectionState == VpnConnectionState.CONNECTING) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        color = buttonColor.copy(alpha = pulseAlpha)
                    )
            )
        }

        // Border effect
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = gradientColors + listOf(Color.Transparent)
                    )
                )
                .border(
                    width = 4.dp,
                    brush = Brush.linearGradient(
                        colors = gradientColors
                    ),
                    shape = CircleShape
                )
                .clickable(enabled = !isConnecting) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // Inner circle background
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Icon
                Icon(
                    imageVector = when (connectionState) {
                        VpnConnectionState.CONNECTED -> Icons.Default.Power
                        else -> Icons.Default.PowerOff
                    },
                    contentDescription = when (connectionState) {
                        VpnConnectionState.CONNECTED -> stringResource(R.string.action_disconnect)
                        VpnConnectionState.CONNECTING -> stringResource(R.string.status_connecting)
                        VpnConnectionState.DISCONNECTING -> stringResource(R.string.status_disconnecting)
                        VpnConnectionState.ERROR -> stringResource(R.string.status_error)
                        VpnConnectionState.DISCONNECTED -> stringResource(R.string.action_connect)
                    },
                    modifier = Modifier.size(64.dp),
                    tint = buttonColor
                )
            }
        }
    }
}

/**
 * Simple connect button (no animation)
 */
@Composable
fun SimpleConnectButton(
    connectionState: VpnConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColor by animateColorAsState(
        targetValue = when (connectionState) {
            VpnConnectionState.CONNECTED -> ConnectedGreen
            VpnConnectionState.CONNECTING -> ConnectingYellow
            VpnConnectionState.DISCONNECTING -> ConnectingYellow
            VpnConnectionState.ERROR -> ErrorRed
            VpnConnectionState.DISCONNECTED -> GradientStart
        },
        animationSpec = tween(300),
        label = "simple_button_color"
    )

    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(buttonColor, buttonColor.copy(alpha = 0.8f))
                )
            )
            .border(
                width = 2.dp,
                color = buttonColor.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (connectionState) {
                VpnConnectionState.CONNECTED -> Icons.Default.Power
                else -> Icons.Default.PowerOff
            },
            contentDescription = stringResource(R.string.action_connect),
            modifier = Modifier.size(32.dp),
            tint = Color.White
        )
    }
}
