package com.autoguard.vpn.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autoguard.vpn.R
import com.autoguard.vpn.data.model.VpnConnectionState
import com.autoguard.vpn.ui.components.ConnectButton
import com.autoguard.vpn.ui.components.ServerCard
import com.autoguard.vpn.ui.theme.ConnectedGreen
import com.autoguard.vpn.ui.theme.ConnectingYellow
import com.autoguard.vpn.ui.theme.DisconnectedGray
import com.autoguard.vpn.ui.theme.ErrorRed
import com.autoguard.vpn.ui.theme.GradientEnd
import com.autoguard.vpn.ui.theme.GradientStart
import com.autoguard.vpn.ui.viewmodel.MainViewModel

/**
 * Main Screen
 * Shows VPN connection status and main operation interface
 *
 * @param viewModel Main ViewModel
 * @param onNavigateToSettings Callback for settings
 * @param onNavigateToServerList Callback for server list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToServerList: () -> Unit = {}
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val connectedServer by viewModel.connectedServer.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    // Refresh Button
                    IconButton(
                        onClick = { viewModel.fetchServers() },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.action_refresh)
                            )
                        }
                    }

                    // Settings Button
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.action_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Status Display
            ConnectionStatusSection(
                connectionState = connectionState,
                connectedServer = connectedServer
            )

            // Middle: Connection Button
            ConnectButton(
                connectionState = connectionState,
                onClick = { viewModel.toggleConnection() },
                modifier = Modifier.padding(vertical = 48.dp)
            )

            // Bottom: Server Selection
            ServerSelectionSection(
                selectedServer = selectedServer ?: connectedServer,
                connectionState = connectionState,
                onClick = onNavigateToServerList
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Error Tip
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(
                            color = ErrorRed.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: "",
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Connection Status Section
 */
@Composable
private fun ConnectionStatusSection(
    connectionState: VpnConnectionState,
    connectedServer: com.autoguard.vpn.data.model.VpnServer?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Text
        val statusText = when (connectionState) {
            VpnConnectionState.CONNECTED -> stringResource(R.string.status_connected)
            VpnConnectionState.CONNECTING -> stringResource(R.string.status_connecting)
            VpnConnectionState.DISCONNECTING -> stringResource(R.string.status_disconnecting)
            VpnConnectionState.ERROR -> stringResource(R.string.home_status_failed)
            VpnConnectionState.DISCONNECTED -> stringResource(R.string.home_status_not_connected)
        }

        val statusColor = when (connectionState) {
            VpnConnectionState.CONNECTED -> ConnectedGreen
            VpnConnectionState.CONNECTING -> ConnectingYellow
            VpnConnectionState.DISCONNECTING -> ConnectingYellow
            VpnConnectionState.ERROR -> ErrorRed
            VpnConnectionState.DISCONNECTED -> DisconnectedGray
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = statusColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Server Info
        if (connectedServer != null && connectionState == VpnConnectionState.CONNECTED) {
            Text(
                text = connectedServer.getDisplayName(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (connectionState == VpnConnectionState.CONNECTING) {
            Text(
                text = stringResource(R.string.home_info_connecting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = stringResource(R.string.home_info_tap_to_connect),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Server Selection Section
 */
@Composable
private fun ServerSelectionSection(
    selectedServer: com.autoguard.vpn.data.model.VpnServer?,
    connectionState: VpnConnectionState,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        ServerCard(
            server = selectedServer ?: com.autoguard.vpn.data.model.VpnServer(
                id = "default",
                name = stringResource(R.string.server_select_title),
                country = "🌐",
                city = stringResource(R.string.server_select_title),
                endpoint = "",
                publicKey = "",
                allowedIps = "0.0.0.0/0",
                port = 51820
            ),
            isSelected = false,
            onClick = onClick
        )
    }
}
