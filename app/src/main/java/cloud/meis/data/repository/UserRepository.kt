package cloud.meis.data.repository

import cloud.meis.data.local.dao.UserDao
import cloud.meis.data.local.entity.UserEntity
import cloud.meis.data.local.entity.UserWithServers

class UserRepository(
    private val userDao: UserDao
) {
    suspend fun registerUser(fullName: String, email: String, password: String): Long {
        return userDao.insertUser(
            UserEntity(
                fullName = fullName.trim(),
                email = email.trim().lowercase(),
                password = password.trim()
            )
        )
    }

    suspend fun login(email: String, password: String): UserEntity? {
        return userDao.getUserByCredentials(
            email = email.trim().lowercase(),
            password = password.trim()
        )
    }

    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email.trim().lowercase())
    }

    suspend fun getUserWithServers(userId: Int): UserWithServers? {
        return userDao.getUserWithServers(userId)
    }
}