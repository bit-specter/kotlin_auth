package cloud.meis.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "servers",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"])]
)
data class ServerEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "user_id")
    val userId: Int,

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
