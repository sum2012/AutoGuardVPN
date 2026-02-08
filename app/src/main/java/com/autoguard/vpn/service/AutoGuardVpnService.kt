package com.autoguard.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.autoguard.vpn.R
import com.autoguard.vpn.data.model.VpnConnectionState
import com.autoguard.vpn.data.model.VpnServer
import com.autoguard.vpn.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.setBooleanPrefValue
import kittoku.osc.preference.accessor.setIntPrefValue
import kittoku.osc.preference.accessor.setSetPrefValue
import kittoku.osc.preference.accessor.setStringPrefValue
import kittoku.osc.service.ACTION_VPN_CONNECT
import kittoku.osc.service.ACTION_VPN_DISCONNECT
import kittoku.osc.service.SstpVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AutoGuard VPN Service
 * Connects to VPN Gate servers using SSTP protocol
 */
@AndroidEntryPoint
class AutoGuardVpnService : VpnService() {

    companion object {
        private const val TAG = "AutoGuardVpnService"
        const val ACTION_CONNECT = "com.autoguard.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.autoguard.vpn.ACTION_DISCONNECT"
        const val EXTRA_SERVER = "extra_server"

        private const val NOTIFICATION_CHANNEL_ID = "autoguard_vpn_channel"
        private const val NOTIFICATION_ID = 1001

        private val _connectionState = MutableStateFlow(VpnConnectionState.DISCONNECTED)
        val connectionState: StateFlow<VpnConnectionState> = _connectionState.asStateFlow()

        private val _connectedServer = MutableStateFlow<VpnServer?>(null)
        val connectedServer: StateFlow<VpnServer?> = _connectedServer.asStateFlow()

        @Volatile
        private var instance: AutoGuardVpnService? = null

        fun getInstance(): AutoGuardVpnService? = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val server = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_SERVER, VpnServer::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_SERVER)
                }
                if (server != null) {
                    connect(server)
                }
            }
            ACTION_DISCONNECT -> {
                disconnect()
            }
        }
        return START_STICKY
    }

    private fun connect(server: VpnServer) {
        if (_connectionState.value == VpnConnectionState.CONNECTED) return

        _connectionState.value = VpnConnectionState.CONNECTING
        _connectedServer.value = server

        // Start foreground notification
        val notification = createNotification(server)
        startForeground(NOTIFICATION_ID, notification)

        Log.d(TAG, "Connecting to ${server.endpoint}:${server.port} via SSTP")

        // Configure SSTP preferences
        configureSstpPreferences(server)

        // Start SSTP VPN Service
        val sstpIntent = Intent(this, SstpVpnService::class.java).apply {
            action = ACTION_VPN_CONNECT
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(sstpIntent)
        } else {
            startService(sstpIntent)
        }

        _connectionState.value = VpnConnectionState.CONNECTED
    }

    private fun createNotification(server: VpnServer): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AutoGuard VPN is running")
            .setContentText("Connected to: ${server.endpoint}")
            .setSmallIcon(R.drawable.ic_baseline_vpn_lock_24)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun configureSstpPreferences(server: VpnServer) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        // Server settings
        setStringPrefValue(server.endpoint, OscPrefKey.HOME_HOSTNAME, prefs)
        setStringPrefValue(server.username ?: "vpn", OscPrefKey.HOME_USERNAME, prefs)
        setStringPrefValue(server.password ?: "vpn", OscPrefKey.HOME_PASSWORD, prefs)
        
        // SSL settings
        setIntPrefValue(server.port, OscPrefKey.SSL_PORT, prefs)
        setBooleanPrefValue(false, OscPrefKey.SSL_DO_VERIFY, prefs) // VPN Gate uses self-signed certs
        
        // PPP settings
        setBooleanPrefValue(true, OscPrefKey.PPP_IPv4_ENABLED, prefs)
        setBooleanPrefValue(false, OscPrefKey.PPP_IPv6_ENABLED, prefs)
        setSetPrefValue(setOf("PAP", "MSCHAPv2"), OscPrefKey.PPP_AUTH_PROTOCOLS, prefs)
        
        // Route settings  
        setBooleanPrefValue(true, OscPrefKey.ROUTE_DO_ADD_DEFAULT_ROUTE, prefs)
        
        // DNS settings
        setBooleanPrefValue(true, OscPrefKey.DNS_DO_REQUEST_ADDRESS, prefs)
        
        Log.d(TAG, "SSTP preferences configured for ${server.endpoint}")
    }

    private fun disconnect() {
        _connectionState.value = VpnConnectionState.DISCONNECTING
        
        // Stop SSTP VPN Service
        val sstpIntent = Intent(this, SstpVpnService::class.java).apply {
            action = ACTION_VPN_DISCONNECT
        }
        startService(sstpIntent)

        _connectedServer.value = null
        _connectionState.value = VpnConnectionState.DISCONNECTED
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "AutoGuard VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN Connection Status"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onRevoke() {
        super.onRevoke()
        disconnect()
    }
}
