package cloud.meis.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "server_name")
    val serverName: String,

    @ColumnInfo(name = "host_address")
    val hostAddress: String,

    @ColumnInfo(name = "protocol")
    val protocol: String,

    @ColumnInfo(name = "last_status")
    val lastStatus: Boolean = false,

    @ColumnInfo(name = "last_latency")
    val lastLatency: Int = 0,

    @ColumnInfo(name = "last_checked")
    val lastChecked: Long = 0L
)
