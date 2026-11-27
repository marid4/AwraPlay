package com.awraplay.player.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        StemCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AwraDatabase : RoomDatabase() {
    abstract fun awraDao(): AwraDao

    companion object {
        @Volatile
        private var INSTANCE: AwraDatabase? = null

        fun getInstance(context: Context): AwraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AwraDatabase::class.java,
                    "awraplay.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
