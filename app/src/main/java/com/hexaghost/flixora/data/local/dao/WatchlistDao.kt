package com.hexaghost.flixora.data.local.dao

import androidx.room.*
import com.hexaghost.flixora.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAllWatchlist(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistEntity)

    @Delete
    suspend fun removeFromWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE id = :mediaId")
    suspend fun removeFromWatchlistById(mediaId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :mediaId)")
    fun isInWatchlist(mediaId: Int): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM watchlist")
    fun getWatchlistCount(): Flow<Int>
}
