package cloud.meis.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cloud.meis.data.local.dao.ServerDao
import cloud.meis.data.local.entity.ServerEntity

@Database(
    entities = [ServerEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao

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
                ).build().also { INSTANCE = it }
            }
        }
    }
}
