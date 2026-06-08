package cloud.meis.network

import android.util.Log
import cloud.meis.data.model.PingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

class NetworkPinger(
    private val client: OkHttpClient = defaultClient
) {
    suspend fun ping(url: String): PingResult = withContext(Dispatchers.IO) {
        var lastResult: PingResult? = null
        val addressCache = mutableMapOf<String, List<InetAddress>?>()

        candidateUrls(url).forEach { candidateUrl ->
            Log.d(TAG, "tcp start target=$candidateUrl")
            val tcpResult = tcpProbe(candidateUrl, addressCache)
            Log.d(
                TAG,
                "tcp result target=$candidateUrl up=${tcpResult.isUp} latency=${tcpResult.latencyMs} source=${tcpResult.source}"
            )

            if (!tcpResult.isUp) {
                lastResult = tcpResult
                return@forEach
            }

            Log.d(TAG, "http start target=$candidateUrl")
            val httpResult = withTimeoutOrNull(HTTP_STATUS_TIMEOUT_MS) {
                executeHttpStatus(candidateUrl)
            }

            if (httpResult != null) {
                Log.d(
                    TAG,
                    "http result target=$candidateUrl up=${httpResult.isUp} latency=${httpResult.latencyMs} source=${httpResult.source}"
                )
                return@withContext httpResult
            }

            Log.d(TAG, "http no-result target=$candidateUrl fallback=${tcpResult.source}")
            return@withContext tcpResult.copy(source = "${tcpResult.source}:http-no-result")
        }

        lastResult ?: PingResult(isUp = false, latencyMs = 0, source = "no-candidate")
    }

    private suspend fun executeHttpStatus(url: String): PingResult? {
        val startTime = System.nanoTime()
        return suspendCancellableCoroutine { continuation ->
            val request = Request.Builder()
                .url(url)
                .build()
            val call = client.newCall(request)

            continuation.invokeOnCancellation {
                call.cancel()
            }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG, "http failed target=$url error=${e.javaClass.simpleName}:${e.message}")
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val latencyMs = elapsedMillis(startTime)
                    val result = response.use {
                        PingResult(
                            isUp = it.code in 100..499,
                            latencyMs = latencyMs,
                            source = "http:${it.code}:$url"
                        )
                    }
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }
            })
        }
    }

    private fun candidateUrls(input: String): List<String> {
        val value = input.trim()
        return if (
            value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
        ) {
            listOf(value)
        } else if (isIpv4(value)) {
            listOf("http://$value", "https://$value")
        } else {
            listOf("https://$value", "http://$value")
        }
    }

    private fun isIpv4(value: String): Boolean {
        return Regex("^(\\d{1,3}\\.){3}\\d{1,3}$").matches(value.substringBefore(":"))
    }

    private fun tcpProbe(
        url: String,
        addressCache: MutableMap<String, List<InetAddress>?>
    ): PingResult {
        val startTime = System.nanoTime()
        val endpoint = endpointFor(url)
            ?: return downResult(startTime, "tcp-invalid-url")

        val addresses = cachedAddresses(endpoint.host, addressCache)
            ?: return downResult(startTime, "dns-timeout:${endpoint.host}")

        if (addresses.isEmpty()) {
            return downResult(startTime, "dns-empty:${endpoint.host}")
        }

        var lastResult = downResult(startTime, "tcp-unreachable:${endpoint.host}:${endpoint.port}")
        addresses.take(MAX_ADDRESSES_PER_ENDPOINT).forEach { address ->
            val result = connectAddress(endpoint, address, startTime)
            if (result.isUp) {
                return result
            }
            lastResult = result
        }

        return lastResult
    }

    private fun cachedAddresses(
        host: String,
        addressCache: MutableMap<String, List<InetAddress>?>
    ): List<InetAddress>? {
        if (addressCache.containsKey(host)) {
            return addressCache[host]
        }

        val addresses = resolveAddresses(host)
        addressCache[host] = addresses
        return addresses
    }

    private fun resolveAddresses(host: String): List<InetAddress>? {
        if (isIpv4(host)) {
            return listOf(InetAddress.getByName(host))
        }

        val publicAddresses = resolvePublicIpv4(host)
        if (publicAddresses.isNotEmpty()) {
            Log.d(
                TAG,
                "dns result host=$host provider=public addresses=${publicAddresses.joinToString(",") { it.hostAddress.orEmpty() }}"
            )
            return publicAddresses
        }

        Log.d(TAG, "dns system start host=$host")
        val future = probeExecutor.submit<List<InetAddress>> {
            InetAddress.getAllByName(host)
                .toList()
                .sortedWith(
                    compareBy<InetAddress> { it !is Inet4Address }
                        .thenBy { it.hostAddress }
                )
        }

        return try {
            val addresses = future.get(DNS_TOTAL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            Log.d(
                TAG,
                "dns result host=$host provider=system addresses=${addresses.joinToString(",") { it.hostAddress.orEmpty() }}"
            )
            addresses
        } catch (throwable: TimeoutException) {
            future.cancel(true)
            Log.d(TAG, "dns system failed host=$host error=TimeoutException:${throwable.message}")
            null
        } catch (throwable: ExecutionException) {
            val cause = throwable.cause ?: throwable
            Log.d(TAG, "dns system failed host=$host error=${cause.javaClass.simpleName}:${cause.message}")
            null
        } catch (throwable: Throwable) {
            Log.d(TAG, "dns system failed host=$host error=${throwable.javaClass.simpleName}:${throwable.message}")
            null
        }
    }

    private fun resolvePublicIpv4(host: String): List<InetAddress> {
        PUBLIC_DNS_SERVERS.forEach { server ->
            Log.d(TAG, "dns public start host=$host server=$server")
            val addresses = runCatching {
                queryDnsARecord(host, server)
            }.getOrElse {
                Log.d(
                    TAG,
                    "dns public failed host=$host server=$server error=${it.javaClass.simpleName}:${it.message}"
                )
                emptyList()
            }

            if (addresses.isNotEmpty()) {
                return addresses
            }
        }

        return emptyList()
    }

    private fun queryDnsARecord(host: String, server: String): List<InetAddress> {
        val cleanHost = host.trim().trimEnd('.')
        val queryId = (System.nanoTime().toInt() and 0xffff)
        val query = buildDnsQuery(cleanHost, queryId)

        DatagramSocket().use { socket ->
            socket.soTimeout = DNS_UDP_TIMEOUT_MS.toInt()
            val request = DatagramPacket(
                query,
                query.size,
                InetAddress.getByName(server),
                DNS_PORT
            )
            socket.send(request)

            val buffer = ByteArray(DNS_PACKET_SIZE)
            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)

            return parseDnsAResponse(buffer.copyOf(response.length), queryId)
        }
    }

    private fun buildDnsQuery(host: String, queryId: Int): ByteArray {
        val output = ByteArrayOutputStream()
        writeShort(output, queryId)
        writeShort(output, DNS_RECURSION_DESIRED)
        writeShort(output, 1)
        writeShort(output, 0)
        writeShort(output, 0)
        writeShort(output, 0)

        host.split(".")
            .filter { it.isNotBlank() }
            .forEach { label ->
                val bytes = label.toByteArray(StandardCharsets.US_ASCII)
                output.write(bytes.size)
                output.write(bytes)
            }

        output.write(0)
        writeShort(output, DNS_TYPE_A)
        writeShort(output, DNS_CLASS_IN)
        return output.toByteArray()
    }

    private fun parseDnsAResponse(packet: ByteArray, expectedQueryId: Int): List<InetAddress> {
        if (packet.size < DNS_HEADER_SIZE || readUnsignedShort(packet, 0) != expectedQueryId) {
            return emptyList()
        }

        val questionCount = readUnsignedShort(packet, 4)
        val answerCount = readUnsignedShort(packet, 6)
        var offset = DNS_HEADER_SIZE

        repeat(questionCount) {
            offset = skipDnsName(packet, offset) + DNS_QUESTION_TRAILER_SIZE
            if (offset > packet.size) {
                return emptyList()
            }
        }

        val addresses = mutableListOf<InetAddress>()
        repeat(answerCount) {
            offset = skipDnsName(packet, offset)
            if (offset + DNS_ANSWER_HEADER_SIZE > packet.size) {
                return@repeat
            }

            val type = readUnsignedShort(packet, offset)
            val dnsClass = readUnsignedShort(packet, offset + 2)
            val dataLength = readUnsignedShort(packet, offset + 8)
            offset += DNS_ANSWER_HEADER_SIZE

            if (offset + dataLength > packet.size) {
                return@repeat
            }

            if (type == DNS_TYPE_A && dnsClass == DNS_CLASS_IN && dataLength == IPV4_BYTE_COUNT) {
                addresses += InetAddress.getByAddress(packet.copyOfRange(offset, offset + dataLength))
            }

            offset += dataLength
        }

        return addresses
    }

    private fun skipDnsName(packet: ByteArray, startOffset: Int): Int {
        var offset = startOffset
        while (offset < packet.size) {
            val length = packet[offset].toInt() and 0xff
            if (length == 0) {
                return offset + 1
            }
            if ((length and DNS_POINTER_MASK) == DNS_POINTER_MASK) {
                return offset + 2
            }
            offset += length + 1
        }
        return packet.size
    }

    private fun writeShort(output: ByteArrayOutputStream, value: Int) {
        output.write((value ushr 8) and 0xff)
        output.write(value and 0xff)
    }

    private fun readUnsignedShort(packet: ByteArray, offset: Int): Int {
        if (offset + 1 >= packet.size) {
            return 0
        }
        return ((packet[offset].toInt() and 0xff) shl 8) or
            (packet[offset + 1].toInt() and 0xff)
    }

    private fun connectAddress(
        endpoint: Endpoint,
        address: InetAddress,
        startTime: Long
    ): PingResult {
        val socketRef = AtomicReference<Socket?>()
        val addressValue = address.hostAddress.orEmpty()
        Log.d(TAG, "tcp connect host=${endpoint.host} address=$addressValue port=${endpoint.port}")

        val future = probeExecutor.submit<Boolean> {
            Socket().use { socket ->
                socketRef.set(socket)
                socket.connect(
                    InetSocketAddress(address, endpoint.port),
                    TCP_CONNECT_TIMEOUT_MS.toInt()
                )
            }
            true
        }

        return try {
            future.get(TCP_TOTAL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            PingResult(
                isUp = true,
                latencyMs = elapsedMillis(startTime),
                source = "tcp:${endpoint.host}:${endpoint.port}:$addressValue"
            )
        } catch (throwable: TimeoutException) {
            socketRef.get()?.close()
            future.cancel(true)
            Log.d(
                TAG,
                "tcp failed host=${endpoint.host} address=$addressValue error=TimeoutException:${throwable.message}"
            )
            downResult(startTime, "tcp-timeout:${endpoint.host}:${endpoint.port}:$addressValue")
        } catch (throwable: ExecutionException) {
            val cause = throwable.cause ?: throwable
            Log.d(
                TAG,
                "tcp failed host=${endpoint.host} address=$addressValue error=${cause.javaClass.simpleName}:${cause.message}"
            )
            downResult(
                startTime,
                "tcp-error:${cause.javaClass.simpleName}:${endpoint.host}:${endpoint.port}:$addressValue"
            )
        } catch (throwable: Throwable) {
            Log.d(
                TAG,
                "tcp failed host=${endpoint.host} address=$addressValue error=${throwable.javaClass.simpleName}:${throwable.message}"
            )
            downResult(
                startTime,
                "tcp-error:${throwable.javaClass.simpleName}:${endpoint.host}:${endpoint.port}:$addressValue"
            )
        }
    }

    private fun endpointFor(url: String): Endpoint? {
        return runCatching {
            val uri = URI(url)
            val host = uri.host ?: return@runCatching null
            val port = when {
                uri.port > 0 -> uri.port
                uri.scheme.equals("https", ignoreCase = true) -> 443
                else -> 80
            }
            Endpoint(host = host, port = port)
        }.getOrNull()
    }

    private fun downResult(startTime: Long, source: String): PingResult {
        return PingResult(
            isUp = false,
            latencyMs = elapsedMillis(startTime),
            source = source
        )
    }

    private fun elapsedMillis(startTime: Long): Int {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime).toInt()
    }

    companion object {
        private val probeExecutor = Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "PingMonProbe").apply {
                isDaemon = true
            }
        }

        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        private const val DNS_TOTAL_TIMEOUT_MS = 2_000L
        private const val DNS_UDP_TIMEOUT_MS = 1_200L
        private const val TCP_CONNECT_TIMEOUT_MS = 2_000L
        private const val TCP_TOTAL_TIMEOUT_MS = 2_300L
        private const val HTTP_STATUS_TIMEOUT_MS = 1_500L
        private const val MAX_ADDRESSES_PER_ENDPOINT = 3
        private const val DNS_PORT = 53
        private const val DNS_PACKET_SIZE = 512
        private const val DNS_HEADER_SIZE = 12
        private const val DNS_QUESTION_TRAILER_SIZE = 4
        private const val DNS_ANSWER_HEADER_SIZE = 10
        private const val DNS_RECURSION_DESIRED = 0x0100
        private const val DNS_TYPE_A = 1
        private const val DNS_CLASS_IN = 1
        private const val DNS_POINTER_MASK = 0xc0
        private const val IPV4_BYTE_COUNT = 4
        private val PUBLIC_DNS_SERVERS = listOf("1.1.1.1", "8.8.8.8")
        private const val TAG = "PingMonDebug"
    }

    private data class Endpoint(
        val host: String,
        val port: Int
    )
}
