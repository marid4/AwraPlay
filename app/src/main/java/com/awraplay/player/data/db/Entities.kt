package com.awraplay.player.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val mediaId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class HistoryEntity(
    @PrimaryKey val mediaId: Long,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 1,
    val lastPositionMs: Long = 0L
)

@Entity(tableName = "stem_cache")
data class StemCacheEntity(
    @PrimaryKey val mediaId: Long,
    val vocalPath: String,
    val accompanimentPath: String,
    val usedAiModel: Boolean,
    val engineLabel: String,
    val sizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
)
