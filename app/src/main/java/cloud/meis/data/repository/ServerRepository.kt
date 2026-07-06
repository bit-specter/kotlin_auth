package cloud.meis.data.repository

import cloud.meis.data.local.dao.ServerDao
import cloud.meis.data.local.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

class ServerRepository(
    private val serverDao: ServerDao
) {
    fun getAllServers(userId: Int): Flow<List<ServerEntity>> {
        return serverDao.getAllServers(userId)
    }

    suspend fun getAllServersOnce(userId: Int): List<ServerEntity> {
        return serverDao.getAllServersOnce(userId)
    }

    suspend fun getServerById(userId: Int, id: Int): ServerEntity? {
        return serverDao.getServerById(userId, id)
    }

    suspend fun addServer(server: ServerEntity): Long {
        return serverDao.insertServer(server)
    }

    suspend fun updateServer(server: ServerEntity) {
        serverDao.updateServer(server)
    }

    suspend fun updateServerStatus(
        userId: Int,
        id: Int,
        isUp: Boolean,
        latencyMs: Int,
        checkedAt: Long
    ) {
        serverDao.updateServerStatus(
            userId = userId,
            id = id,
            isUp = isUp,
            latencyMs = latencyMs,
            checkedAt = checkedAt
        )
    }

    suspend fun deleteServer(server: ServerEntity) {
        serverDao.deleteServer(server)
    }

    suspend fun deleteServerById(userId: Int, id: Int) {
        serverDao.deleteServerById(userId, id)
    }
}
