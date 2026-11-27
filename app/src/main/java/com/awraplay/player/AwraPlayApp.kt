package com.awraplay.player

import android.app.Application
import com.awraplay.player.ai.ModelManager
import com.awraplay.player.ai.OnnxVocalSeparator
import com.awraplay.player.audio.AudioEffectsController
import com.awraplay.player.data.db.AwraDatabase
import com.awraplay.player.data.prefs.SettingsDataStore
import com.awraplay.player.data.repository.MediaScannerRepository

class AwraPlayApp : Application() {

    val database by lazy { AwraDatabase.getInstance(this) }
    val dao by lazy { database.awraDao() }
    val settingsDataStore by lazy { SettingsDataStore(this) }
    val mediaScanner by lazy { MediaScannerRepository(this) }
    val modelManager by lazy { ModelManager(this) }
    val vocalSeparator by lazy { OnnxVocalSeparator(this, dao, modelManager) }
    val effectsController by lazy { AudioEffectsController() }
}
