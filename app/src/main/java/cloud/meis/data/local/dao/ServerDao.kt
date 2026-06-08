package cloud.meis.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cloud.meis.data.local.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY server_name ASC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers ORDER BY server_name ASC")
    suspend fun getAllServersOnce(): List<ServerEntity>

    @Query("SELECT * FROM servers WHERE id = :id LIMIT 1")
    suspend fun getServerById(id: Int): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity): Long

    @Update
    suspend fun updateServer(server: ServerEntity)

    @Query(
        """
        UPDATE servers
        SET last_status = :isUp,
            last_latency = :latencyMs,
            last_checked = :checkedAt
        WHERE id = :id
        """
    )
    suspend fun updateServerStatus(
        id: Int,
        isUp: Boolean,
        latencyMs: Int,
        checkedAt: Long
    )

    @Delete
    suspend fun deleteServer(server: ServerEntity)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteServerById(id: Int)
}
