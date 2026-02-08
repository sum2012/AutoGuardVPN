package kittoku.osc

import androidx.preference.PreferenceManager
import kittoku.osc.preference.AppString
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.preference.accessor.getBooleanPrefValue
import kittoku.osc.preference.accessor.getIntPrefValue
import kittoku.osc.preference.accessor.getSetPrefValue
import kittoku.osc.preference.accessor.getStringPrefValue
import kittoku.osc.preference.getValidAllowedAppInfos
import kittoku.osc.service.SstpVpnService
import kittoku.osc.terminal.IPTerminal
import kittoku.osc.terminal.SSLTerminal
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID


enum class Where {
    CERT,
    CERT_PATH,
    SSL,
    PROXY,
    SSTP_DATA,
    SSTP_CONTROL,
    SSTP_REQUEST,
    SSTP_HASH,
    PPP,
    PAP,
    CHAP,
    MSCHAPV2,
    EAP,
    LCP,
    LCP_MRU,
    LCP_AUTH,
    IPCP,
    IPCP_IP,
    IPV6CP,
    IPV6CP_IDENTIFIER,
    IP,
    IPv4,
    IPv6,
    ROUTE,
    INCOMING,
    OUTGOING,
}

data class ControlMessage(
    val from: Where,
    val result: Result,
    val supplement: String? = null
)

enum class Result {
    PROCEEDED,

    // common errors
    ERR_TIMEOUT,
    ERR_COUNT_EXHAUSTED,
    ERR_UNKNOWN_TYPE, // the data cannot be parsed
    ERR_UNEXPECTED_MESSAGE, // the data can be parsed, but it's received in the wrong time
    ERR_PARSING_FAILED,
    ERR_VERIFICATION_FAILED,

    // for SSTP
    ERR_NEGATIVE_ACKNOWLEDGED,
    ERR_ABORT_REQUESTED,
    ERR_DISCONNECT_REQUESTED,

    // for PPP
    ERR_TERMINATE_REQUESTED,
    ERR_PROTOCOL_REJECTED,
    ERR_CODE_REJECTED,
    ERR_AUTHENTICATION_FAILED,
    ERR_ADDRESS_REJECTED,
    ERR_OPTION_REJECTED,

    // for IP
    ERR_INVALID_ADDRESS,

    // for INCOMING
    ERR_INVALID_PACKET_SIZE,
}

internal class SharedBridge(val service: SstpVpnService) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(service)
    val builder = service.Builder()
    lateinit var handler: CoroutineExceptionHandler

    val controlMailbox = Channel<ControlMessage>(Channel.BUFFERED)

    var sslTerminal: SSLTerminal? = null
    var ipTerminal: IPTerminal? = null

    val HOME_USERNAME = getStringPrefValue(OscPrefKey.HOME_USERNAME, prefs)
    val HOME_PASSWORD = getStringPrefValue(OscPrefKey.HOME_PASSWORD, prefs)
    val PPP_MRU = getIntPrefValue(OscPrefKey.PPP_MRU, prefs)
    val PPP_MTU = getIntPrefValue(OscPrefKey.PPP_MTU, prefs)
    val PPP_AUTH_PROTOCOLS = getSetPrefValue(OscPrefKey.PPP_AUTH_PROTOCOLS, prefs)
    val PPP_IPv4_ENABLED = getBooleanPrefValue(OscPrefKey.PPP_IPv4_ENABLED, prefs)
    val PPP_IPv6_ENABLED = getBooleanPrefValue(OscPrefKey.PPP_IPv6_ENABLED, prefs)

    var hlak: ByteArray? = null
    val nonce = ByteArray(32)
    val guid = UUID.randomUUID().toString()
    var hashProtocol: Byte = 0

    private val mutex = Mutex()
    private var frameID = -1

    var currentMRU = PPP_MRU
    var currentAuth = ""
    val currentIPv4 = ByteArray(4)
    val currentIPv6 = ByteArray(8)
    val currentProposedDNS = ByteArray(4)

    val allowedApps: List<AppString> = mutableListOf<AppString>().also {
        if (getBooleanPrefValue(OscPrefKey.ROUTE_DO_ENABLE_APP_BASED_RULE, prefs)) {
            getValidAllowedAppInfos(prefs, service.packageManager).forEach { info ->
                it.add(
                    AppString(
                        info.packageName,
                        service.packageManager.getApplicationLabel(info).toString()
                    )
                )
            }
        }
    }

    fun isEnabled(authProtocol: String): Boolean {
        return authProtocol in PPP_AUTH_PROTOCOLS
    }

    fun attachSSLTerminal() {
        sslTerminal = SSLTerminal(this)
    }

    fun attachIPTerminal() {
        ipTerminal = IPTerminal(this)
    }

    suspend fun allocateNewFrameID(): Byte {
        mutex.withLock {
            frameID += 1
            return frameID.toByte()
        }
    }
}
