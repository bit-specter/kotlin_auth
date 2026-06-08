package cloud.meis.network

import android.util.Log
import cloud.meis.data.model.PingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.TimeUnit

class IcmpPinger {
    suspend fun ping(host: String): PingResult = withContext(Dispatchers.IO) {
        val address = host.trim()
        val command = listOf("ping", "-c", "1", "-W", "2", address)
        val startTime = System.nanoTime()
        Log.d(TAG, "icmp start target=$address")

        runCatching {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val finished = process.waitFor(3, TimeUnit.SECONDS)
            if (!finished) {
                process.destroy()
                return@runCatching PingResult(
                    isUp = false,
                    latencyMs = elapsedMillis(startTime),
                    source = "icmp-timeout:$address"
                )
            }

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val latencyMs = parseLatency(output) ?: elapsedMillis(startTime)
            val up = process.exitValue() == 0
            Log.d(TAG, "icmp result target=$address up=$up latency=$latencyMs")

            PingResult(
                isUp = up,
                latencyMs = latencyMs,
                source = "icmp:$address"
            )
        }.getOrElse {
            Log.d(TAG, "icmp failed target=$address error=${it.javaClass.simpleName}:${it.message}")
            PingResult(
                isUp = false,
                latencyMs = elapsedMillis(startTime),
                source = "icmp-error:$address"
            )
        }
    }

    private fun parseLatency(output: String): Int? {
        val match = Regex("time[=<]([0-9]+(?:\\.[0-9]+)?)\\s*ms", RegexOption.IGNORE_CASE)
            .find(output)
            ?: return null

        return match.groupValues[1]
            .lowercase(Locale.US)
            .toDoubleOrNull()
            ?.toInt()
    }

    private fun elapsedMillis(startTime: Long): Int {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime).toInt()
    }

    companion object {
        private const val TAG = "PingMonDebug"
    }
}
