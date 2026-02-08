package com.autoguard.vpn.data.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * VPN Gate API Interface
 * Used to fetch server list from vpngate.net
 *
 * API Documentation: https://www.vpngate.net/api/iphone/
 * Response format: CSV text
 */
interface VpnGateApiService {

    /**
     * Fetch server list CSV data from VPN Gate
     * @param url API endpoint URL, default: https://www.vpngate.net/api/iphone/
     * @return Server data in CSV format
     */
    @GET
    suspend fun getServerList(@Url url: String = DEFAULT_API_URL): ResponseBody

    /**
     * Get default VPN Gate server list
     * @return Server data in CSV format
     */
    @GET("api/iphone/")
    suspend fun getDefaultServerList(): ResponseBody

    companion object {
        // VPN Gate official API endpoint (Using HTTPS to comply with modern Android network security policies)
        const val DEFAULT_API_URL = "https://www.vpngate.net/api/iphone/"

        // HTTPS backup endpoint
        const val HTTPS_API_URL = "https://www.vpngate.net/api/iphone/"

        // HTTP backup endpoint (If HTTPS is unavailable, must be allowed in networkSecurityConfig)
        const val HTTP_API_URL = "http://www.vpngate.net/api/iphone/"

        // Timeout configurations
        const val CONNECT_TIMEOUT = 30L
        const val READ_TIMEOUT = 30L
        const val WRITE_TIMEOUT = 30L
    }
}
