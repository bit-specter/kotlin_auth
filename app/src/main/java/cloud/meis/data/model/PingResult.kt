package cloud.meis.data.model

data class PingResult(
    val isUp: Boolean,
    val latencyMs: Int,
    val checkedAt: Long = System.currentTimeMillis(),
    val source: String = "unknown"
)
