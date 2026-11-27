package com.awraplay.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.awraplay.player.data.model.MediaType
import com.awraplay.player.ui.components.MiniPlayerBar
import com.awraplay.player.ui.screens.HomeScreen
import com.awraplay.player.ui.screens.KaraokeScreen
import com.awraplay.player.ui.screens.NowPlayingScreen
import com.awraplay.player.ui.screens.SettingsScreen
import com.awraplay.player.ui.screens.SplashScreen
import com.awraplay.player.ui.screens.VideoPlayerScreen
import com.awraplay.player.ui.theme.AwraPlayTheme
import com.awraplay.player.ui.viewmodel.AwraPlayViewModel

class MainActivity : ComponentActivity() {

    private fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun hasMediaPermission(): Boolean {
        val mediaPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return mediaPerms.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val vm: AwraPlayViewModel = viewModel()
            val settings by vm.settings.collectAsState()
            val currentMedia by vm.currentMedia.collectAsState()
            val isPlaying by vm.isPlaying.collectAsState()
            val positionMs by vm.positionMs.collectAsState()
            val durationMs by vm.durationMs.collectAsState()
            val vocalVolume by vm.vocalVolume.collectAsState()

            var hasPermission by remember { mutableStateOf(hasMediaPermission()) }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                hasPermission = hasMediaPermission()
                if (hasPermission) vm.scanAllMedia()
            }

            LaunchedEffect(hasPermission) {
                if (hasPermission) {
                    vm.scanAllMedia()
                } else {
                    permissionLauncher.launch(requiredPermissions())
                }
            }

            AwraPlayTheme(darkTheme = settings.isDarkTheme) {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val route = backStack?.destination?.route

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = "splash"
                        ) {
                            composable("splash") {
                                SplashScreen(
                                    onFinished = {
                                        navController.navigate("home") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("home") {
                                HomeScreen(
                                    vm = vm,
                                    hasPermission = hasPermission,
                                    onRequestPermission = { permissionLauncher.launch(requiredPermissions()) },
                                    onOpenNowPlaying = { navController.navigate("now_playing") },
                                    onOpenVideoPlayer = { navController.navigate("video_player") },
                                    onOpenKaraoke = { navController.navigate("karaoke") },
                                    onOpenSettings = { navController.navigate("settings") }
                                )
                            }
                            composable("now_playing") {
                                NowPlayingScreen(
                                    vm = vm,
                                    onBack = { navController.popBackStack() },
                                    onOpenKaraoke = { navController.navigate("karaoke") }
                                )
                            }
                            composable("video_player") {
                                VideoPlayerScreen(
                                    vm = vm,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("karaoke") {
                                KaraokeScreen(
                                    vm = vm,
                                    onBack = { navController.popBackStack() },
                                    onOpenVideoPlayer = { navController.navigate("video_player") }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    vm = vm,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        // Persistent Mini Player on Home & Settings
                        val media = currentMedia
                        if (media != null && (route == "home" || route == "settings")) {
                            MiniPlayerBar(
                                media = media,
                                isPlaying = isPlaying,
                                positionMs = positionMs,
                                durationMs = durationMs,
                                vocalVolume = vocalVolume,
                                onTogglePlayPause = { vm.togglePlayPause() },
                                onNext = { vm.skipNext() },
                                onClick = {
                                    if (media.mediaType == MediaType.VIDEO) {
                                        navController.navigate("video_player")
                                    } else {
                                        navController.navigate("now_playing")
                                    }
                                },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
    }
}
