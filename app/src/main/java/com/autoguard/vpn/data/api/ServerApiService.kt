package com.autoguard.vpn.data.api

import com.autoguard.vpn.data.model.ServerListResponse
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * 服务器API接口
 * 用于从远程服务器获取VPN节点配置信息
 */
interface ServerApiService {

    /**
     * 从指定URL获取服务器列表
     * @param url 服务器列表JSON文件的URL
     * @return 服务器列表响应对象
     */
    @GET
    suspend fun getServerList(@Url url: String): ServerListResponse

    /**
     * 默认服务器列表端点
     * @return 服务器列表响应对象
     */
    @GET("servers.json")
    suspend fun getDefaultServerList(): ServerListResponse
}
