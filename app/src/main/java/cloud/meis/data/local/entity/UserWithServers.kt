package cloud.meis.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class UserWithServers(
    @Embedded
    val user: UserEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "user_id"
    )
    val servers: List<ServerEntity>
)