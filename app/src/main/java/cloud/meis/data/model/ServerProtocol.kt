package cloud.meis.data.model

object ServerProtocol {
    const val AUTO = "AUTO"
    const val HTTP = "HTTP"
    const val ICMP = "ICMP"

    fun normalize(value: String): String {
        return when (value.trim().uppercase()) {
            AUTO -> AUTO
            ICMP -> ICMP
            else -> HTTP
        }
    }
}
