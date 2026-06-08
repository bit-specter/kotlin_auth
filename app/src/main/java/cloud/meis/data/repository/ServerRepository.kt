package cloud.meis.data.repository

import cloud.meis.data.local.dao.ServerDao
import cloud.meis.data.local.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

class ServerRepository(
    private val serverDao: ServerDao
) {
    fun getAllServers(): Flow<List<ServerEntity>> {
        return serverDao.getAllServers()
    }

    suspend fun getAllServersOnce(): List<ServerEntity> {
        return serverDao.getAllServersOnce()
    }

    suspend fun getServerById(id: Int): ServerEntity? {
        return serverDao.getServerById(id)
    }

    suspend fun addServer(server: ServerEntity): Long {
        return serverDao.insertServer(server)
    }

    suspend fun updateServer(server: ServerEntity) {
        serverDao.updateServer(server)
    }

    suspend fun updateServerStatus(
        id: Int,
        isUp: Boolean,
        latencyMs: Int,
        checkedAt: Long
    ) {
        serverDao.updateServerStatus(
            id = id,
            isUp = isUp,
            latencyMs = latencyMs,
            checkedAt = checkedAt
        )
    }

    suspend fun deleteServer(server: ServerEntity) {
        serverDao.deleteServer(server)
    }

    suspend fun deleteServerById(id: Int) {
        serverDao.deleteServerById(id)
    }
}
