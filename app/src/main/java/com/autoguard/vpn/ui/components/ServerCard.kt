package com.autoguard.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autoguard.vpn.R
import com.autoguard.vpn.data.model.VpnServer
import com.autoguard.vpn.ui.theme.CardShape
import com.autoguard.vpn.ui.theme.ConnectedGreen
import com.autoguard.vpn.ui.theme.LatencyText
import com.autoguard.vpn.ui.theme.ServerNameText

/**
 * Server card component
 * Displays information for a single VPN server
 *
 * @param server Server data
 * @param isSelected Whether selected
 * @param onClick Click event
 * @param modifier Modifier
 */
@Composable
fun ServerCard(
    server: VpnServer,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Flag and server info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Flag or Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val flag = server.getFlagEmoji()
                    if (flag.isNotEmpty()) {
                        Text(
                            text = flag,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Server name and details
                Column {
                    Text(
                        text = server.city,
                        style = ServerNameText.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = server.endpoint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Throughput and sessions
                    if (server.throughput != null || server.vpnSessions != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (server.throughput != null) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = stringResource(R.string.server_throughput),
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${String.format("%.1f", server.throughput)} ${stringResource(R.string.server_throughput_unit)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            
                            if (server.vpnSessions != null) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = stringResource(R.string.server_sessions),
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${server.vpnSessions}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Right: Latency info and selection indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Latency display
                LatencyIndicator(pingLatency = server.pingLatency)

                Spacer(modifier = Modifier.width(12.dp))

                // Selection Indicator
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ConnectedGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = stringResource(R.string.status_connected),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.action_connect),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * Latency Indicator component
 */
@Composable
fun LatencyIndicator(
    pingLatency: Long,
    modifier: Modifier = Modifier
) {
    val latencyColor = when {
        pingLatency < 50 && pingLatency >= 0 -> ConnectedGreen // Excellent - Green
        pingLatency < 100 && pingLatency >= 0 -> MaterialTheme.colorScheme.primary // Good - Blue
        pingLatency < 200 && pingLatency >= 0 -> MaterialTheme.colorScheme.tertiary // Fair - Orange
        pingLatency >= 200 -> MaterialTheme.colorScheme.error // Poor - Red
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) // Unknown - Gray
    }

    val latencyText = when {
        pingLatency < 0 -> "-- ms"
        pingLatency < 1000 -> stringResource(R.string.latency_ms, pingLatency.toInt())
        else -> "${pingLatency / 1000}s"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Public,
            contentDescription = stringResource(R.string.server_latency),
            modifier = Modifier.size(16.dp),
            tint = latencyColor
        )
        Text(
            text = latencyText,
            style = LatencyText,
            color = latencyColor
        )
    }
}

/**
 * Country Header component
 */
@Composable
fun CountryHeader(
    country: String,
    serverCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val emoji = countryCodeToEmoji(country)
            if (emoji.isNotEmpty()) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = getCountryName(country),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.server_count, serverCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Country code to emoji
 */
private fun countryCodeToEmoji(code: String): String {
    val upperCode = code.uppercase()
    if (upperCode.length != 2 || !upperCode.all { it in 'A'..'Z' }) return ""
    val firstChar = Character.codePointAt(upperCode, 0) - 0x41 + 0x1F1E6
    val secondChar = Character.codePointAt(upperCode, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
}

/**
 * Get country name
 */
@Composable
private fun getCountryName(code: String): String {
    return when (code.uppercase()) {
        "US" -> stringResource(R.string.country_us)
        "GB" -> stringResource(R.string.country_gb)
        "DE" -> stringResource(R.string.country_de)
        "JP" -> stringResource(R.string.country_jp)
        "SG" -> stringResource(R.string.country_sg)
        "AU" -> stringResource(R.string.country_au)
        "CA" -> stringResource(R.string.country_ca)
        "FR" -> stringResource(R.string.country_fr)
        "NL" -> stringResource(R.string.country_nl)
        "HK" -> stringResource(R.string.country_hk)
        "CN" -> stringResource(R.string.country_cn)
        "KR" -> stringResource(R.string.country_kr)
        "TW" -> stringResource(R.string.country_tw)
        "IN" -> stringResource(R.string.country_in)
        "BR" -> stringResource(R.string.country_br)
        "🌐" -> stringResource(R.string.country_all)
        else -> code
    }
}
