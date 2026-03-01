package com.autoguard.vpn.data.api

import com.autoguard.vpn.data.model.ServerListResponse
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Server API Interface
 * Used to fetch VPN node configuration information from a remote server
 */
interface ServerApiService {

    /**
     * Get the server list from a specified URL
     * @param url The URL of the server list JSON file
     * @return Server list response object
     */
    @GET
    suspend fun getServerList(@Url url: String): ServerListResponse

    /**
     * Default server list endpoint
     * @return Server list response object
     */
    @GET("servers.json")
    suspend fun getDefaultServerList(): ServerListResponse
}
