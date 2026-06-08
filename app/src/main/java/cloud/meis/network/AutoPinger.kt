package cloud.meis.network

import android.util.Log
import cloud.meis.data.model.PingResult
import kotlinx.coroutines.withTimeoutOrNull

class AutoPinger(
    private val networkPinger: NetworkPinger = NetworkPinger(),
    private val icmpPinger: IcmpPinger = IcmpPinger()
) {
    suspend fun ping(target: String): PingResult {
        val cleanTarget = target.trim()
        val host = extractHost(cleanTarget)
        Log.d(TAG, "auto start target=$cleanTarget host=$host")

        val httpResult = withTimeoutOrNull(HTTP_TIMEOUT_MS) {
            networkPinger.ping(cleanTarget)
        } ?: timeoutResult("http-timeout:$cleanTarget")

        Log.d(
            TAG,
            "auto http target=$cleanTarget up=${httpResult.isUp} latency=${httpResult.latencyMs} source=${httpResult.source}"
        )

        if (httpResult.isUp) {
            return httpResult
        }

        val icmpResult = withTimeoutOrNull(ICMP_TIMEOUT_MS) {
            icmpPinger.ping(host)
        } ?: timeoutResult("icmp-timeout:$host")

        Log.d(
            TAG,
            "auto icmp target=$cleanTarget up=${icmpResult.isUp} latency=${icmpResult.latencyMs} source=${icmpResult.source}"
        )

        return if (icmpResult.isUp) {
            icmpResult
        } else {
            httpResult
        }
    }

    private fun extractHost(value: String): String {
        return value
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .substringBefore(":")
    }

    private fun timeoutResult(source: String): PingResult {
        return PingResult(
            isUp = false,
            latencyMs = HTTP_TIMEOUT_MS.toInt(),
            checkedAt = System.currentTimeMillis(),
            source = source
        )
    }

    companion object {
        private const val HTTP_TIMEOUT_MS = 8_500L
        private const val ICMP_TIMEOUT_MS = 3_500L
        private const val TAG = "PingMonDebug"
    }
}
