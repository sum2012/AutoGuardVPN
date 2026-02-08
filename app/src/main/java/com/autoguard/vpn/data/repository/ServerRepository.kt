package com.autoguard.vpn.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autoguard.vpn.data.api.ServerApiService
import com.autoguard.vpn.data.api.VpnGateApiService
import com.autoguard.vpn.data.model.VpnGateCsvParser
import com.autoguard.vpn.data.model.VpnGateServer
import com.autoguard.vpn.data.model.VpnServer
import com.autoguard.vpn.di.DefaultClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

// DataStore Extension
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vpn_settings")

/**
 * Server Repository Class
 * Responsible for managing server data fetching, caching, and local storage
 */
@Singleton
class ServerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ServerApiService,
    private val vpnGateApiService: VpnGateApiService,
    @DefaultClient private val okHttpClient: OkHttpClient
) {
    private val _serverList = MutableStateFlow<List<VpnServer>>(emptyList())
    val serverList: StateFlow<List<VpnServer>> = _serverList.asStateFlow()

    private val _vpnGateServers = MutableStateFlow<List<VpnGateServer>>(emptyList())
    val vpnGateServers: StateFlow<List<VpnGateServer>> = _vpnGateServers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _dataSourceType = MutableStateFlow(DataSourceType.VPN_GATE)
    val dataSourceType: StateFlow<DataSourceType> = _dataSourceType.asStateFlow()

    companion object {
        private val CUSTOM_API_URL = stringPreferencesKey("custom_api_url")
        private val CACHED_SERVERS = stringPreferencesKey("cached_servers")
        private val CACHED_VPN_GATE_SERVERS = stringPreferencesKey("cached_vpn_gate_servers")
        private const val DEFAULT_API_URL = "https://api.autoguard-vpn.com/servers.json"
        // Connectivity test no longer depends only on Google, as it might be inaccessible before VPN connection
        private const val FALLBACK_TEST_URL = "https://www.bing.com"
    }

    enum class DataSourceType {
        VPN_GATE,
        CUSTOM_API
    }

    val customApiUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_API_URL] ?: DEFAULT_API_URL
    }

    suspend fun fetchServerList(): Result<List<VpnServer>> {
        return fetchFromVpnGate()
    }

    suspend fun fetchFromVpnGate(): Result<List<VpnServer>> {
        return withContext(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _error.value = null
                _dataSourceType.value = DataSourceType.VPN_GATE

                val response = vpnGateApiService.getDefaultServerList()
                val csvData = response.string()

                val vpnGateServerList = VpnGateCsvParser.parse(csvData)
                val filteredServers = VpnGateCsvParser.filterHighQuality(vpnGateServerList)
                val servers = filteredServers.map { it.toVpnServer() }

                _vpnGateServers.value = VpnGateCsvParser.sortByScore(vpnGateServerList)
                _serverList.value = servers

                cacheVpnGateServers(vpnGateServerList)

                _isLoading.value = false
                Result.success(servers)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch server list from VPN Gate"
                val cachedServers = loadCachedVpnGateServers()
                if (cachedServers.isNotEmpty()) {
                    val servers = cachedServers.map { it.toVpnServer() }
                    _vpnGateServers.value = cachedServers
                    _serverList.value = servers
                    _isLoading.value = false
                    Result.success(servers)
                } else {
                    _isLoading.value = false
                    Result.failure(e)
                }
            }
        }
    }

    /**
     * Test server connectivity
     * Improvement: prioritize testing server port response rather than requesting Google directly
     */
    suspend fun testServerConnectivity(server: VpnServer): Long {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val socket = Socket()
                // Attempt to establish TCP connection, timeout 3 seconds
                socket.connect(InetSocketAddress(server.endpoint, server.port), 3000)
                val latency = System.currentTimeMillis() - startTime
                socket.close()
                latency
            } catch (_: Exception) {
                // If TCP probe fails, try HTTP probe as fallback
                try {
                    val request = Request.Builder()
                        .url(FALLBACK_TEST_URL)
                        .build()
                    val startTime = System.currentTimeMillis()
                    okHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) System.currentTimeMillis() - startTime else -1L
                    }
                } catch (_: Exception) {
                    -1L
                }
            }
        }
    }

    suspend fun testAllServers() {
        val currentList = _serverList.value
        if (currentList.isEmpty()) return

        _isLoading.value = true
        withContext(Dispatchers.IO) {
            val updatedList = currentList.map { server ->
                async {
                    val latency = testServerConnectivity(server)
                    server.copy(pingLatency = latency)
                }
            }.awaitAll()
            
            _serverList.value = updatedList
        }
        _isLoading.value = false
    }

    suspend fun fetchFromCustomApi(): Result<List<VpnServer>> {
        return withContext(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _error.value = null
                val apiUrl = customApiUrlFlow.first()
                val response = apiService.getServerList(apiUrl)
                val servers = response.servers
                _serverList.value = servers
                cacheServerList(servers)
                _isLoading.value = false
                Result.success(servers)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch server list from custom API"
                _isLoading.value = false
                Result.failure(e)
            }
        }
    }

    suspend fun switchDataSource(type: DataSourceType): Result<List<VpnServer>> {
        return when (type) {
            DataSourceType.VPN_GATE -> fetchFromVpnGate()
            DataSourceType.CUSTOM_API -> fetchFromCustomApi()
        }
    }

    suspend fun setCustomApiUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_API_URL] = url
        }
    }

    private suspend fun cacheVpnGateServers(servers: List<VpnGateServer>) {
        try {
            val json = GsonHelper.toJson(servers)
            context.dataStore.edit { preferences ->
                preferences[CACHED_VPN_GATE_SERVERS] = json
            }
        } catch (_: Exception) {}
    }

    private suspend fun loadCachedVpnGateServers(): List<VpnGateServer> {
        return try {
            val json = context.dataStore.data.first()[CACHED_VPN_GATE_SERVERS] ?: return emptyList()
            GsonHelper.fromJson<List<VpnGateServer>>(json)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun cacheServerList(servers: List<VpnServer>) {
        try {
            val json = GsonHelper.toJson(servers)
            context.dataStore.edit { preferences ->
                preferences[CACHED_SERVERS] = json
            }
        } catch (_: Exception) {}
    }

    fun getServerById(serverId: String): VpnServer? {
        return _serverList.value.find { it.id == serverId }
    }

    fun getVpnGateServerByIp(ipAddress: String): VpnGateServer? {
        return _vpnGateServers.value.find { it.ipAddress == ipAddress }
    }

    fun getHighQualityServers(): List<VpnServer> {
        return _serverList.value.filter { server ->
            val vpnGateServer = getVpnGateServerByIp(server.endpoint)
            (vpnGateServer?.score ?: 0) >= 10000
        }
    }

    fun getServersByCountry(country: String): List<VpnServer> {
        return _serverList.value.filter { it.country == country }
    }

    fun getCountries(): List<String> {
        return _serverList.value.map { it.country }.distinct().sorted()
    }

    fun getServersGroupedByCountry(): Map<String, List<VpnServer>> {
        return _serverList.value.groupBy { it.country }
    }

    fun getVpnGateServersGroupedByCountry(): Map<String, List<VpnGateServer>> {
        return VpnGateCsvParser.groupByCountry(_vpnGateServers.value)
    }

    fun getVpnGateStats(): VpnGateStats {
        val servers = _vpnGateServers.value
        return VpnGateStats(
            totalServers = servers.size,
            openVpnSupported = servers.count { it.supportsOpenVpn() },
            averagePing = if (servers.isNotEmpty()) servers.map { it.ping }.average().toLong() else 0L,
            countries = servers.map { it.countryShort }.distinct().size,
            topCountry = servers.groupBy { it.countryShort }
                .maxByOrNull { it.value.size }
                ?.key ?: ""
        )
    }
}

data class VpnGateStats(
    val totalServers: Int,
    val openVpnSupported: Int,
    val averagePing: Long,
    val countries: Int,
    val topCountry: String
)

object GsonHelper {
    @PublishedApi
    internal val gson = com.google.gson.Gson()

    fun <T> toJson(obj: T): String {
        return gson.toJson(obj)
    }

    inline fun <reified T> fromJson(json: String): T {
        return try {
            val type = object : com.google.gson.reflect.TypeToken<T>() {}.type
            gson.fromJson(json, type)
        } catch (_: Exception) {
            gson.fromJson(json, T::class.java)
        }
    }
}
