package com.autoguard.vpn.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoguard.vpn.data.model.VpnConnectionState
import com.autoguard.vpn.data.model.VpnGateServer
import com.autoguard.vpn.data.model.VpnServer
import com.autoguard.vpn.data.repository.ServerRepository
import com.autoguard.vpn.data.repository.VpnGateStats
import com.autoguard.vpn.service.AutoGuardVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

// DataStore Extension
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "vpn_settings")

/**
 * Main Interface ViewModel
 * Manages VPN connection status, server list, and user interaction
 * Supports fetching server data from VPN Gate
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val serverRepository: ServerRepository
) : AndroidViewModel(application) {

    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
    }

    private val context = application.applicationContext

    // VPN Connection State
    val connectionState: StateFlow<VpnConnectionState> = AutoGuardVpnService.connectionState

    // Currently Connected Server
    val connectedServer: StateFlow<VpnServer?> = AutoGuardVpnService.connectedServer

    // Server List
    val serverList: StateFlow<List<VpnServer>> = serverRepository.serverList

    // VPN Gate Raw Server Data
    val vpnGateServers: StateFlow<List<VpnGateServer>> = serverRepository.vpnGateServers

    // VPN Gate Statistics
    val vpnGateStats: StateFlow<VpnGateStats> = MutableStateFlow(
        VpnGateStats(0, 0, 0, 0, "")
    ).also { flow ->
        viewModelScope.launch {
            serverRepository.vpnGateServers.collect { servers ->
                flow.value = serverRepository.getVpnGateStats()
            }
        }
    }

    // Loading State
    val isLoading: StateFlow<Boolean> = serverRepository.isLoading

    // Error Message
    val error: StateFlow<String?> = serverRepository.error

    // Current Data Source Type
    val dataSourceType: StateFlow<ServerRepository.DataSourceType> = serverRepository.dataSourceType

    // Whether to show server selector
    private val _showServerSelector = MutableStateFlow(false)
    val showServerSelector: StateFlow<Boolean> = _showServerSelector.asStateFlow()

    // Currently selected server
    private val _selectedServer = MutableStateFlow<VpnServer?>(null)
    val selectedServer: StateFlow<VpnServer?> = _selectedServer.asStateFlow()

    // User country preference
    private val _preferredCountry = MutableStateFlow<String?>(null)
    val preferredCountry: StateFlow<String?> = _preferredCountry.asStateFlow()

    // Whether Kill Switch is enabled
    private val _killSwitchEnabled = MutableStateFlow(false)
    val killSwitchEnabled: StateFlow<Boolean> = _killSwitchEnabled.asStateFlow()

    // Language State
    val language = context.settingsDataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: ""
    }

    init {
        // Automatically fetch server list and test connectivity on startup
        fetchServers()
        
        // Apply saved language
        viewModelScope.launch {
            context.settingsDataStore.data.collect { preferences ->
                val lang = preferences[LANGUAGE_KEY]
                if (!lang.isNullOrEmpty()) {
                    applyLanguage(lang)
                }
            }
        }
    }

    /**
     * Set App Language
     */
    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            context.settingsDataStore.edit { preferences ->
                preferences[LANGUAGE_KEY] = languageCode
            }
            applyLanguage(languageCode)
        }
    }

    private fun applyLanguage(languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    /**
     * Fetch server list
     * Prioritizes fetching from VPN Gate, then automatically tests connectivity
     */
    fun fetchServers() {
        viewModelScope.launch {
            serverRepository.fetchServerList().onSuccess {
                testAllServers()
            }
        }
    }

    /**
     * Fetch server list from VPN Gate and test connectivity
     */
    fun fetchFromVpnGate() {
        viewModelScope.launch {
            serverRepository.fetchFromVpnGate().onSuccess {
                testAllServers()
            }
        }
    }

    /**
     * Test connectivity for all servers
     */
    fun testAllServers() {
        viewModelScope.launch {
            serverRepository.testAllServers()
        }
    }

    /**
     * Switch data source
     */
    fun switchDataSource(type: ServerRepository.DataSourceType) {
        viewModelScope.launch {
            serverRepository.switchDataSource(type).onSuccess {
                testAllServers()
            }
        }
    }

    /**
     * Select a server
     */
    fun selectServer(server: VpnServer) {
        _selectedServer.value = server
        _showServerSelector.value = false
    }

    /**
     * Show server selector
     */
    fun showServerSelector() {
        _showServerSelector.value = true
    }

    /**
     * Hide server selector
     */
    fun hideServerSelector() {
        _showServerSelector.value = false
    }

    /**
     * Toggle VPN connection state
     */
    fun toggleConnection() {
        when (connectionState.value) {
            VpnConnectionState.CONNECTED -> {
                disconnect()
            }
            VpnConnectionState.DISCONNECTED -> {
                val server = _selectedServer.value ?: serverList.value.firstOrNull()
                if (server != null) {
                    connect(server)
                } else {
                    // Show error if no server available
                }
            }
            else -> {
                // Connecting or disconnecting, do nothing
            }
        }
    }

    /**
     * Connect to VPN
     */
    private fun connect(server: VpnServer) {
        val context = getApplication<Application>()
        val intent = Intent(context, AutoGuardVpnService::class.java).apply {
            action = AutoGuardVpnService.ACTION_CONNECT
            putExtra(AutoGuardVpnService.EXTRA_SERVER, server)
        }
        context.startForegroundService(intent)
    }

    /**
     * Disconnect from VPN
     */
    private fun disconnect() {
        val context = getApplication<Application>()
        val intent = Intent(context, AutoGuardVpnService::class.java).apply {
            action = AutoGuardVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }

    /**
     * Set preferred country
     */
    fun setPreferredCountry(countryCode: String?) {
        _preferredCountry.value = countryCode
    }

    /**
     * Toggle Kill Switch
     */
    fun toggleKillSwitch() {
        _killSwitchEnabled.value = !_killSwitchEnabled.value
    }

    /**
     * Set Kill Switch status
     */
    fun setKillSwitchEnabled(enabled: Boolean) {
        _killSwitchEnabled.value = enabled
    }

    /**
     * Get best server (lowest latency)
     */
    fun getBestServer(): VpnServer? {
        return serverList.value.filter { it.pingLatency > 0 }.minByOrNull { it.pingLatency }
    }

    /**
     * Get high-quality servers (score >= 10000)
     */
    fun getHighQualityServers(): List<VpnServer> {
        return serverRepository.getHighQualityServers()
    }

    /**
     * Filter servers by country
     */
    fun getServersByCountry(country: String): List<VpnServer> {
        return serverRepository.getServersByCountry(country)
    }

    /**
     * Get list of all countries
     */
    fun getCountries(): List<String> {
        return serverRepository.getCountries()
    }

    /**
     * Get details for a VPN Gate server
     */
    fun getVpnGateServerDetails(server: VpnServer): VpnGateServer? {
        val ip = server.endpoint.split(":").firstOrNull() ?: return null
        return serverRepository.getVpnGateServerByIp(ip)
    }

    /**
     * Get connection info text
     */
    fun getConnectionInfo(server: VpnServer): String {
        val vpnGateServer = getVpnGateServerDetails(server)
        return if (vpnGateServer != null) {
            """
            |Server: ${vpnGateServer.country}
            |IP: ${vpnGateServer.ipAddress}
            |ISP: ${vpnGateServer.operatorName}
            |Score: ${vpnGateServer.getQualityDescription()}
            |Throughput: ${vpnGateServer.throughput.format(2)} Mbps
            |Sessions: ${vpnGateServer.vpnSessions}
            |Logging Policy: ${vpnGateServer.loggingPolicy}
            |Type: ${vpnGateServer.getConnectionType()}
            |Latency: ${if (server.pingLatency > 0) "${server.pingLatency}ms" else "Measuring..."}
            """.trimMargin()
        } else {
            "Connection info unavailable"
        }
    }
    
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    /**
     * Clear error message
     */
    fun clearError() {
        // Error is managed by the repository
    }
}
