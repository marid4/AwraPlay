package com.awraplay.player.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awraplay.player.ui.components.AudioEffectsSheet
import com.awraplay.player.ui.theme.AwraAccentPink
import com.awraplay.player.ui.theme.AwraPrimaryPurple
import com.awraplay.player.ui.theme.AwraSecondaryBlue
import com.awraplay.player.ui.viewmodel.AwraPlayViewModel

@Composable
fun SettingsScreen(
    vm: AwraPlayViewModel,
    onBack: () -> Unit
) {
    val settings by vm.settings.collectAsState()
    val stemCache by vm.stemCacheList.collectAsState()
    val downloadProg by vm.modelDownloadProgress.collectAsState()
    val modelStatus by vm.modelStatusMessage.collectAsState()

    var showEqSheet by remember { mutableStateOf(false) }

    val installedModels = remember(modelStatus) { vm.modelManager.getInstalledModels() }
    val totalCacheMb = remember(stemCache) {
        stemCache.sumOf { it.sizeBytes } / (1024f * 1024f)
    }

    val onnxPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { vm.importOnnxModel(it, "imported_vocal_model.onnx") }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "Kembali")
            }
            Text("Pengaturan AwraPlay", style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(Modifier.height(14.dp))

        // 1. Audio Settings
        SettingsSectionCard(title = "Audio Settings", icon = Icons.Rounded.Equalizer) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Equalizer 10-Band & Efek", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Preset: ${settings.effects.selectedPreset.displayName} • Bass ${settings.effects.bassBoostStrength / 10}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showEqSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AwraPrimaryPurple)
                ) {
                    Text("Buka EQ")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 2. Video Settings
        SettingsSectionCard(title = "Video Settings", icon = Icons.Rounded.AutoAwesome) {
            SettingsToggleRow(
                title = "Dukungan Picture-in-Picture",
                subtitle = "Izinkan mode PiP pada pemutar video",
                checked = settings.defaultVideoPip,
                onCheckedChange = { vm.setDefaultVideoPip(it) }
            )
        }

        Spacer(Modifier.height(12.dp))

        // 3. AI Vocal Separation Settings
        SettingsSectionCard(title = "AI Vocal Separation (ONNX / MDX-Net)", icon = Icons.Rounded.AutoAwesome) {
            SettingsToggleRow(
                title = "Gunakan Model AI ONNX",
                subtitle = "Jika belum terpasang, gunakan pemisah Spectral STFT",
                checked = settings.preferAiOnnx,
                onCheckedChange = { vm.setPreferAiOnnx(it) }
            )
            Spacer(Modifier.height(8.dp))
            SettingsToggleRow(
                title = "Pisahkan Otomatis di Mode Karaoke",
                subtitle = "Memulai pemisahan vokal otomatis saat volume vokal < 100%",
                checked = settings.autoSeparateInKaraoke,
                onCheckedChange = { vm.setAutoSeparateInKaraoke(it) }
            )

            Spacer(Modifier.height(10.dp))
            Text(
                text = if (installedModels.isNotEmpty())
                    "Model Terpasang: ${installedModels.joinToString { it.name }}"
                else "Belum ada model .onnx terpasang (Menggunakan mode Spectral STFT)",
                style = MaterialTheme.typography.labelMedium,
                color = if (installedModels.isNotEmpty()) AwraSecondaryBlue else AwraAccentPink
            )

            downloadProg?.let { p ->
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
            }
            modelStatus?.let { msg ->
                Spacer(Modifier.height(4.dp))
                Text(msg, fontSize = 12.sp, color = AwraSecondaryBlue)
            }

            Spacer(Modifier.height(10.dp))
            vm.modelManager.availableModels.forEach { model ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(model.displayName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(model.approxSizeMb, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { vm.downloadOnnxModel(model) },
                        enabled = downloadProg == null,
                        colors = ButtonDefaults.buttonColors(containerColor = AwraPrimaryPurple)
                    ) {
                        Icon(Icons.Rounded.Download, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Unduh", fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onnxPicker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.FolderOpen, null)
                Spacer(Modifier.width(6.dp))
                Text("Impor File .onnx dari Penyimpanan Lokal")
            }
        }

        Spacer(Modifier.height(12.dp))

        // 4. Storage Management
        SettingsSectionCard(title = "Storage Management", icon = Icons.Rounded.DeleteSweep) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cache Pemisahan Vokal", fontWeight = FontWeight.SemiBold)
                    Text(
                        "${stemCache.size} file tersimpan (${String.format("%.1f", totalCacheMb)} MB)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { vm.clearStemCache() },
                    colors = ButtonDefaults.buttonColors(containerColor = AwraAccentPink)
                ) {
                    Text("Bersihkan")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 5. Theme Settings
        SettingsSectionCard(title = "Theme Settings", icon = Icons.Rounded.Palette) {
            SettingsToggleRow(
                title = "Dark Premium Theme",
                subtitle = "Hitam gelap #101014 dengan aksen ungu & biru neon",
                checked = settings.isDarkTheme,
                onCheckedChange = { vm.setDarkTheme(it) }
            )
        }

        Spacer(Modifier.height(12.dp))

        // 6. About AwraPlay
        SettingsSectionCard(title = "About AwraPlay", icon = Icons.Rounded.Info) {
            Text("AwraPlay v1.0.0", fontWeight = FontWeight.Bold)
            Text("Package: com.awraplay.player", style = MaterialTheme.typography.labelMedium, color = AwraSecondaryBlue)
            Spacer(Modifier.height(4.dp))
            Text(
                "Your Music, Your Voice, Your Karaoke.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(80.dp))
    }

    if (showEqSheet) {
        AudioEffectsSheet(
            effectsState = settings.effects,
            bandLabels = vm.effectsController.bandFrequenciesLabel,
            onUpdateEffects = { vm.updateEffects(it) },
            onSelectPreset = { vm.selectEqPreset(it) },
            onDismiss = { showEqSheet = false }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = AwraSecondaryBlue)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AwraPrimaryPurple
            )
        )
    }
}
