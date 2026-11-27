package com.awraplay.player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AwraDao {
    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrackToPlaylist(track: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, mediaId: Long)

    @Query("SELECT mediaId FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun observePlaylistTrackIds(playlistId: Long): Flow<List<Long>>

    // Favorites
    @Query("SELECT mediaId FROM favorites ORDER BY addedAt DESC")
    fun observeFavoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun removeFavorite(mediaId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)")
    suspend fun isFavorite(mediaId: Long): Boolean

    // Playback History / Recently Played
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC LIMIT 50")
    fun observeHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(history: HistoryEntity)

    @Query("SELECT * FROM playback_history WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getHistory(mediaId: Long): HistoryEntity?

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    // Stem Cache
    @Query("SELECT * FROM stem_cache ORDER BY createdAt DESC")
    fun observeStemCache(): Flow<List<StemCacheEntity>>

    @Query("SELECT * FROM stem_cache WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getStemCache(mediaId: Long): StemCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStemCache(entry: StemCacheEntity)

    @Query("DELETE FROM stem_cache WHERE mediaId = :mediaId")
    suspend fun deleteStemCache(mediaId: Long)

    @Query("DELETE FROM stem_cache")
    suspend fun clearStemCache()
}
