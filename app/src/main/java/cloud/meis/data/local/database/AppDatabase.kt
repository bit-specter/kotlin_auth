package cloud.meis.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cloud.meis.data.local.dao.ServerDao
import cloud.meis.data.local.dao.UserDao
import cloud.meis.data.local.entity.ServerEntity
import cloud.meis.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, ServerEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun userDao(): UserDao

    companion object {
        private const val DATABASE_NAME = "pingmon.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                    ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
