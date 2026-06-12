package com.hexaghost.flixora.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hexaghost.flixora.data.local.dao.WatchlistDao
import com.hexaghost.flixora.data.local.entity.WatchlistEntity

@Database(
    entities = [WatchlistEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FlixoraDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        const val DATABASE_NAME = "flixora_db"
    }
}
