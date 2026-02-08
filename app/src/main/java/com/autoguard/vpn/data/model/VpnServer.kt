package com.autoguard.vpn.data.model

import android.os.Parcelable
import android.util.Base64
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * VPN服务器数据模型
 * 增加协议、配置以及凭据支持
 */
@Parcelize
data class VpnServer(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("country")
    val country: String,

    @SerializedName("city")
    val city: String,

    @SerializedName("endpoint")
    val endpoint: String,

    @SerializedName("port")
    val port: Int = 443,

    @SerializedName("protocol")
    val protocol: String = "OpenVPN",

    @SerializedName("config")
    val config: String? = null,

    @SerializedName("username")
    val username: String? = "vpn",

    @SerializedName("password")
    val password: String? = "vpn",

    @SerializedName("psk")
    val psk: String? = "vpn",

    @SerializedName("publicKey")
    val publicKey: String? = null,

    @SerializedName("allowedIps")
    val allowedIps: String = "0.0.0.0/0",

    @SerializedName("pingLatency")
    val pingLatency: Long = -1L,

    @SerializedName("vpnSessions")
    val vpnSessions: Int? = null,

    @SerializedName("throughput")
    val throughput: Double? = null
) : Parcelable {
    fun getDisplayName(): String {
        return if (city.isNotEmpty()) "$country - $city" else name
    }

    fun getFlagEmoji(): String {
        return countryCodeToEmoji(country)
    }

    private fun countryCodeToEmoji(code: String): String {
        val upperCode = code.uppercase()
        if (upperCode.length != 2 || !upperCode.all { it in 'A'..'Z' }) return ""
        val firstChar = Character.codePointAt(upperCode, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(upperCode, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }
}

data class VpnGateServer(
    val hostname: String,
    val ipAddress: String,
    val score: Long,
    val ping: Long,
    val speed: Long,
    val countryLong: String,
    val countryShort: String,
    val vpnSessions: Int,
    val uptime: Long,
    val totalUsers: Long,
    val totalTraffic: Long,
    val logType: String,
    val operator: String,
    val message: String,
    val configDataBase64: String
) {
    val country: String get() = countryLong
    val throughput: Double get() = speed / 1024.0 / 1024.0 // Mbps
    val operatorName: String get() = operator
    val loggingPolicy: String get() = logType

    fun getConnectionType(): String {
        return if (configDataBase64.isNotEmpty()) "OpenVPN" else "L2TP/IPsec"
    }

    fun supportsOpenVpn(): Boolean = configDataBase64.isNotEmpty()

    fun getQualityDescription(): String {
        return when {
            score >= 100000 -> "极佳"
            score >= 50000 -> "良好"
            score >= 10000 -> "普通"
            else -> "一般"
        }
    }

    fun toVpnServer(): VpnServer {
        val configStr = if (configDataBase64.isNotEmpty()) {
            try {
                String(Base64.decode(configDataBase64, Base64.DEFAULT))
            } catch (_: Exception) { null }
        } else null
        
        val port = parsePortFromConfig(configStr) ?: 443
        val protocol = getConnectionType()
        
        // Use a more unique ID to avoid LazyColumn key collisions
        // Combine IP, port, and hostname to ensure uniqueness for same IP multiple configurations
        val uniqueId = "${ipAddress}_${port}_${protocol}_${hostname}"
        
        return VpnServer(
            id = uniqueId,
            name = hostname,
            country = countryShort,
            city = countryLong,
            endpoint = ipAddress,
            port = port,
            protocol = protocol,
            config = configStr,
            username = "vpn",
            password = "vpn",
            psk = "vpn",
            pingLatency = ping,
            vpnSessions = vpnSessions,
            throughput = throughput
        )
    }

    private fun parsePortFromConfig(config: String?): Int? {
        if (config == null) return null
        return try {
            val match = Regex("""remote\s+\S+\s+(\d+)""").find(config)
            match?.groupValues?.get(1)?.toIntOrNull()
        } catch (_: Exception) {
            null
        }
    }
}

object VpnGateCsvParser {
    fun parse(csvData: String): List<VpnGateServer> {
        val allLines = csvData.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val startIndex = allLines.indexOfFirst { it.startsWith("#HostName") }
        if (startIndex == -1 || startIndex + 1 >= allLines.size) return emptyList()
        
        val dataLines = allLines.subList(startIndex + 1, allLines.size)
            .takeWhile { it != "*" }

        return dataLines.mapNotNull { line ->
            parseLine(line)
        }
    }

    private fun parseLine(line: String): VpnGateServer? {
        try {
            val parts = splitCsvLine(line)
            if (parts.size < 15) return null

            return VpnGateServer(
                hostname = parts[0],
                ipAddress = parts[1],
                score = parts[2].toLongOrNull() ?: 0L,
                ping = parts[3].toLongOrNull() ?: 0L,
                speed = parts[4].toLongOrNull() ?: 0L,
                countryLong = parts[5],
                countryShort = parts[6],
                vpnSessions = parts[7].toIntOrNull() ?: 0,
                uptime = parts[8].toLongOrNull() ?: 0L,
                totalUsers = parts[9].toLongOrNull() ?: 0L,
                totalTraffic = parts[10].toLongOrNull() ?: 0L,
                logType = parts[11],
                operator = parts[12],
                message = parts[13],
                configDataBase64 = parts[14]
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '\"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    fun filterHighQuality(servers: List<VpnGateServer>): List<VpnGateServer> {
        return servers.filter { it.score >= 1000 }
    }

    fun sortByScore(servers: List<VpnGateServer>): List<VpnGateServer> {
        return servers.sortedByDescending { it.score }
    }

    fun groupByCountry(servers: List<VpnGateServer>): Map<String, List<VpnGateServer>> {
        return servers.groupBy { it.countryShort }
    }
}

data class ServerListResponse(
    @SerializedName("servers")
    val servers: List<VpnServer>,
    @SerializedName("version")
    val version: String,
    @SerializedName("lastUpdated")
    val lastUpdated: String
)

enum class VpnConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}
