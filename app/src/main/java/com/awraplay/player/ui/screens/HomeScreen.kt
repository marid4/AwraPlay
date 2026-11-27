package com.awraplay.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.awraplay.player.data.db.PlaylistEntity
import com.awraplay.player.data.model.FolderGroup
import com.awraplay.player.data.model.MediaItem
import com.awraplay.player.data.model.MediaType
import com.awraplay.player.ui.components.PlaylistPickerDialog
import com.awraplay.player.ui.components.SleepTimerDialog
import com.awraplay.player.ui.components.formatDuration
import com.awraplay.player.ui.theme.AwraAccentPink
import com.awraplay.player.ui.theme.AwraPrimaryPurple
import com.awraplay.player.ui.theme.AwraSecondaryBlue
import com.awraplay.player.ui.viewmodel.AwraPlayViewModel

private enum class HomeTab(val label: String) {
    MUSIC("My Music"),
    VIDEOS("My Videos"),
    RECENT("Recently Played"),
    PLAYLISTS("Playlist"),
    FAVORITES("Favorites"),
    FOLDERS("Folder")
}

@Composable
fun HomeScreen(
    vm: AwraPlayViewModel,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenVideoPlayer: () -> Unit,
    onOpenKaraoke: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val audioList by vm.audioList.collectAsState()
    val videoList by vm.videoList.collectAsState()
    val isScanning by vm.isScanning.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val history by vm.historyEntities.collectAsState()
    val allMediaMap by vm.allMediaMap.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val folders by vm.folders.collectAsState()
    val sleepSec by vm.sleepTimerRemainingSec.collectAsState()

    var selectedTab by remember { mutableStateOf(HomeTab.MUSIC) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var selectedFolder by remember { mutableStateOf<FolderGroup?>(null) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var trackForPlaylist by remember { mutableStateOf<MediaItem?>(null) }
    var newPlaylistName by remember { mutableStateOf("") }

    val filteredAudio = remember(audioList, searchQuery) {
        if (searchQuery.isBlank()) audioList
        else audioList.filter {
            it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true)
        }
    }
    val filteredVideos = remember(videoList, searchQuery) {
        if (searchQuery.isBlank()) videoList
        else videoList.filter {
            it.title.contains(searchQuery, true) || it.folderName.contains(searchQuery, true)
        }
    }
    val recentMedia = remember(history, allMediaMap) {
        history.mapNotNull { allMediaMap[it.mediaId] }
    }
    val favoriteMedia = remember(favoriteIds, allMediaMap) {
        favoriteIds.mapNotNull { allMediaMap[it] }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Logo AwraPlay + Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(AwraPrimaryPurple, AwraSecondaryBlue, AwraAccentPink))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.GraphicEq, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AwraPlay",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Smart Audio & Video Karaoke",
                        style = MaterialTheme.typography.labelMedium,
                        color = AwraSecondaryBlue
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showSleepDialog = true }) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = "Sleep Timer",
                        tint = if (sleepSec != null) AwraAccentPink else MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = { vm.scanAllMedia() }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Scan Media", tint = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        // Search Bar + Karaoke Mode Quick Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.setSearchQuery(it) },
                placeholder = { Text("Cari musik, artis, atau video...") },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = AwraSecondaryBlue) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AwraPrimaryPurple,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(listOf(AwraAccentPink, AwraPrimaryPurple))
                    )
                    .clickable {
                        if (vm.currentMedia.value == null && audioList.isNotEmpty()) {
                            vm.playMedia(audioList.first(), audioList)
                        }
                        vm.enterKaraokeMode()
                        onOpenKaraoke()
                    }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Mic, "Karaoke Mode", tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Karaoke", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (!hasPermission) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Izin Akses Media Diperlukan",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Berikan izin agar AwraPlay dapat memindai musik dan video lokal di perangkat Anda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = AwraPrimaryPurple)
                    ) {
                        Text("Izinkan Akses Media")
                    }
                }
            }
        }

        // Recently Played Horizontal Carousel
        if (recentMedia.isNotEmpty() && searchQuery.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recently Played", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${recentMedia.size} item",
                    style = MaterialTheme.typography.labelMedium,
                    color = AwraSecondaryBlue
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recentMedia.take(10), key = { "rec_${it.id}" }) { item ->
                    Surface(
                        modifier = Modifier
                            .width(136.dp)
                            .clickable {
                                vm.playMedia(item, recentMedia)
                                if (item.mediaType == MediaType.VIDEO) onOpenVideoPlayer() else onOpenNowPlaying()
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(86.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(listOf(AwraPrimaryPurple, AwraSecondaryBlue))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (item.mediaType == MediaType.VIDEO) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                                    null, tint = Color.White
                                )
                                AsyncImage(
                                    model = item.albumArtUri,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                item.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                item.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Category Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeTab.entries.forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = {
                        selectedTab = tab
                        selectedPlaylist = null
                        selectedFolder = null
                    },
                    label = { Text(tab.label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AwraPrimaryPurple,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AwraPrimaryPurple)
            }
        }

        // Tab Content
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (selectedTab) {
                HomeTab.MUSIC -> {
                    items(filteredAudio, key = { it.id }) { item ->
                        MediaRowItem(
                            item = item,
                            isFavorite = favoriteIds.contains(item.id),
                            onPlay = {
                                vm.playMedia(item, filteredAudio)
                                onOpenNowPlaying()
                            },
                            onToggleFav = { vm.toggleFavorite(item.id) },
                            onAddToPlaylist = { trackForPlaylist = item }
                        )
                    }
                }
                HomeTab.VIDEOS -> {
                    items(filteredVideos, key = { it.id }) { item ->
                        MediaRowItem(
                            item = item,
                            isFavorite = favoriteIds.contains(item.id),
                            onPlay = {
                                vm.playMedia(item, filteredVideos)
                                onOpenVideoPlayer()
                            },
                            onToggleFav = { vm.toggleFavorite(item.id) },
                            onAddToPlaylist = { trackForPlaylist = item }
                        )
                    }
                }
                HomeTab.RECENT -> {
                    items(recentMedia, key = { "hist_${it.id}" }) { item ->
                        MediaRowItem(
                            item = item,
                            isFavorite = favoriteIds.contains(item.id),
                            onPlay = {
                                vm.playMedia(item, recentMedia)
                                if (item.mediaType == MediaType.VIDEO) onOpenVideoPlayer() else onOpenNowPlaying()
                            },
                            onToggleFav = { vm.toggleFavorite(item.id) },
                            onAddToPlaylist = { trackForPlaylist = item }
                        )
                    }
                }
                HomeTab.FAVORITES -> {
                    items(favoriteMedia, key = { "fav_${it.id}" }) { item ->
                        MediaRowItem(
                            item = item,
                            isFavorite = true,
                            onPlay = {
                                vm.playMedia(item, favoriteMedia)
                                if (item.mediaType == MediaType.VIDEO) onOpenVideoPlayer() else onOpenNowPlaying()
                            },
                            onToggleFav = { vm.toggleFavorite(item.id) },
                            onAddToPlaylist = { trackForPlaylist = item }
                        )
                    }
                }
                HomeTab.PLAYLISTS -> {
                    if (selectedPlaylist == null) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newPlaylistName,
                                    onValueChange = { newPlaylistName = it },
                                    placeholder = { Text("Buat playlist baru...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        vm.createPlaylist(newPlaylistName)
                                        newPlaylistName = ""
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(AwraPrimaryPurple)
                                ) {
                                    Icon(Icons.Rounded.Add, "Buat", tint = Color.White)
                                }
                            }
                        }
                        items(playlists, key = { "pl_${it.id}" }) { pl ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlaylist = pl },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.PlaylistPlay, null, tint = AwraSecondaryBlue)
                                        Spacer(Modifier.width(12.dp))
                                        Text(pl.name, style = MaterialTheme.typography.titleMedium)
                                    }
                                    IconButton(onClick = { vm.deletePlaylist(pl.id) }) {
                                        Icon(Icons.Rounded.Delete, "Hapus", tint = AwraAccentPink)
                                    }
                                }
                            }
                        }
                    } else {
                        val pl = selectedPlaylist!!
                        item {
                            Text(
                                text = "← Kembali ke Daftar Playlist (${pl.name})",
                                color = AwraSecondaryBlue,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { selectedPlaylist = null }
                                    .padding(vertical = 6.dp)
                            )
                        }
                        val trackIds by vm.observePlaylistTrackIds(pl.id).collectAsState(initial = emptyList())
                        val tracks = trackIds.mapNotNull { allMediaMap[it] }
                        items(tracks, key = { "pl_t_${it.id}" }) { item ->
                            MediaRowItem(
                                item = item,
                                isFavorite = favoriteIds.contains(item.id),
                                onPlay = {
                                    vm.playMedia(item, tracks)
                                    if (item.mediaType == MediaType.VIDEO) onOpenVideoPlayer() else onOpenNowPlaying()
                                },
                                onToggleFav = { vm.toggleFavorite(item.id) },
                                onAddToPlaylist = { vm.removeTrackFromPlaylist(pl.id, item.id) }
                            )
                        }
                    }
                }
                HomeTab.FOLDERS -> {
                    if (selectedFolder == null) {
                        items(folders, key = { "fld_${it.path}" }) { fg ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFolder = fg },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Folder, null, tint = AwraPrimaryPurple)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fg.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${fg.itemCount} file • ${fg.path}",
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val fg = selectedFolder!!
                        item {
                            Text(
                                text = "← Kembali ke Folder (${fg.name})",
                                color = AwraSecondaryBlue,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { selectedFolder = null }
                                    .padding(vertical = 6.dp)
                            )
                        }
                        items(fg.items, key = { "fi_${it.id}" }) { item ->
                            MediaRowItem(
                                item = item,
                                isFavorite = favoriteIds.contains(item.id),
                                onPlay = {
                                    vm.playMedia(item, fg.items)
                                    if (item.mediaType == MediaType.VIDEO) onOpenVideoPlayer() else onOpenNowPlaying()
                                },
                                onToggleFav = { vm.toggleFavorite(item.id) },
                                onAddToPlaylist = { trackForPlaylist = item }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            remainingSec = sleepSec,
            onSelectMinutes = { vm.startSleepTimer(it) },
            onDismiss = { showSleepDialog = false }
        )
    }

    trackForPlaylist?.let { target ->
        PlaylistPickerDialog(
            playlists = playlists,
            onCreatePlaylist = { vm.createPlaylist(it) },
            onSelectPlaylist = { pid -> vm.addTrackToPlaylist(pid, target.id) },
            onDismiss = { trackForPlaylist = null }
        )
    }
}

@Composable
private fun MediaRowItem(
    item: MediaItem,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFav: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            if (item.mediaType == MediaType.VIDEO)
                                listOf(AwraAccentPink, AwraPrimaryPurple)
                            else listOf(AwraPrimaryPurple, AwraSecondaryBlue)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item.mediaType == MediaType.VIDEO) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                    null, tint = Color.White
                )
                AsyncImage(
                    model = item.albumArtUri,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.artist} • ${formatDuration(item.durationMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onToggleFav) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) AwraAccentPink else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onAddToPlaylist) {
                Icon(
                    imageVector = Icons.Rounded.PlaylistAdd,
                    contentDescription = "Add to Playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
