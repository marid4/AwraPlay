package com.awraplay.player.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.awraplay.player.data.model.FolderGroup
import com.awraplay.player.data.model.LyricLine
import com.awraplay.player.data.model.MediaItem
import com.awraplay.player.data.model.MediaType
import com.awraplay.player.data.model.SubtitleCue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaScannerRepository(private val context: Context) {

    suspend fun scanAudio(): List<MediaItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.IS_MUSIC
        )
        val selection = "${MediaStore.Audio.Media.DURATION} > 1000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val path = cursor.getString(dataCol) ?: ""
                val parent = File(path).parentFile
                val albumId = cursor.getLong(albumIdCol)
                val artUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
                result.add(
                    MediaItem(
                        id = id,
                        title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: File(path).nameWithoutExtension,
                        artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" && it.isNotBlank() } ?: "Unknown Artist",
                        album = cursor.getString(albumCol) ?: "Unknown Album",
                        durationMs = cursor.getLong(durCol),
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                        path = path,
                        folderName = parent?.name ?: "Music",
                        folderPath = parent?.absolutePath ?: "",
                        mimeType = cursor.getString(mimeCol) ?: "audio/*",
                        sizeBytes = cursor.getLong(sizeCol),
                        dateAddedSec = cursor.getLong(dateCol),
                        mediaType = MediaType.AUDIO,
                        albumArtUri = artUri
                    )
                )
            }
        }
        result
    }

    suspend fun scanVideos(): List<MediaItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val path = cursor.getString(dataCol) ?: ""
                val parent = File(path).parentFile
                val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                result.add(
                    MediaItem(
                        id = -id, // Negative ID namespace prevents collisions with Audio IDs
                        title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: File(path).nameWithoutExtension,
                        artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" && it.isNotBlank() } ?: "Video",
                        album = parent?.name ?: "Videos",
                        durationMs = cursor.getLong(durCol),
                        uri = contentUri,
                        path = path,
                        folderName = parent?.name ?: "Videos",
                        folderPath = parent?.absolutePath ?: "",
                        mimeType = cursor.getString(mimeCol) ?: "video/*",
                        sizeBytes = cursor.getLong(sizeCol),
                        dateAddedSec = cursor.getLong(dateCol),
                        mediaType = MediaType.VIDEO,
                        albumArtUri = contentUri,
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol)
                    )
                )
            }
        }
        result
    }

    fun groupByFolder(items: List<MediaItem>): List<FolderGroup> {
        return items.groupBy { it.folderPath }
            .map { (path, group) ->
                FolderGroup(
                    name = group.firstOrNull()?.folderName ?: "Folder",
                    path = path,
                    itemCount = group.size,
                    items = group
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun loadSidecarLyrics(mediaPath: String): List<LyricLine> = withContext(Dispatchers.IO) {
        if (mediaPath.isBlank()) return@withContext emptyList()
        val mediaFile = File(mediaPath)
        val lrcFile = File(mediaFile.parentFile, "${mediaFile.nameWithoutExtension}.lrc")
        if (!lrcFile.exists()) return@withContext emptyList()
        parseLrc(lrcFile.readText())
    }

    fun parseLrc(content: String): List<LyricLine> {
        val regex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?\]""")
        val lines = mutableListOf<LyricLine>()
        content.lineSequence().forEach { raw ->
            val matches = regex.findAll(raw).toList()
            if (matches.isNotEmpty()) {
                val text = raw.replace(regex, "").trim()
                if (text.isNotEmpty()) {
                    matches.forEach { m ->
                        val min = m.groupValues[1].toLongOrNull() ?: 0L
                        val sec = m.groupValues[2].toLongOrNull() ?: 0L
                        val fracStr = m.groupValues[3]
                        val ms = when (fracStr.length) {
                            1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                            2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                            3 -> fracStr.toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        lines.add(LyricLine(min * 60_000L + sec * 1_000L + ms, text))
                    }
                }
            }
        }
        return lines.sortedBy { it.timestampMs }
    }

    suspend fun loadSidecarSubtitles(videoPath: String): List<SubtitleCue> = withContext(Dispatchers.IO) {
        if (videoPath.isBlank()) return@withContext emptyList()
        val videoFile = File(videoPath)
        val srtFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}.srt")
        if (!srtFile.exists()) return@withContext emptyList()
        parseSrt(srtFile.readText())
    }

    fun parseSrt(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = content.replace("\r\n", "\n").split(Regex("\n\\s*\n"))
        val timeRegex = Regex("""(\d{2}):(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})[,.](\d{3})""")
        for (block in blocks) {
            val lines = block.trim().lines()
            val timeLineIndex = lines.indexOfFirst { timeRegex.containsMatchIn(it) }
            if (timeLineIndex >= 0) {
                val m = timeRegex.find(lines[timeLineIndex]) ?: continue
                val startMs = toMs(m.groupValues[1], m.groupValues[2], m.groupValues[3], m.groupValues[4])
                val endMs = toMs(m.groupValues[5], m.groupValues[6], m.groupValues[7], m.groupValues[8])
                val text = lines.drop(timeLineIndex + 1).joinToString("\n").trim()
                if (text.isNotEmpty()) {
                    cues.add(SubtitleCue(startMs, endMs, text))
                }
            }
        }
        return cues
    }

    private fun toMs(h: String, m: String, s: String, ms: String): Long {
        return (h.toLongOrNull() ?: 0L) * 3_600_000L +
            (m.toLongOrNull() ?: 0L) * 60_000L +
            (s.toLongOrNull() ?: 0L) * 1_000L +
            (ms.toLongOrNull() ?: 0L)
    }
}
