# AwraPlay — Smart Audio & Video Player with Karaoke Mode

**Package Name:** `com.awraplay.player`  
**Tagline:** *Your Music, Your Voice, Your Karaoke.*

## Fitur Utama
- **Audio Player (Media3 ExoPlayer 1.5.1):** Memutar MP3, WAV, FLAC, AAC, M4A, OGG; pemindaian otomatis `MediaStore`; album art, metadata, Repeat, Shuffle, Playlist Manager (Room), Favorites, Recently Played, Folder Browser, Sleep Timer, Mini Player, Background Playback & Notification/Bluetooth/Headphone Controls.
- **Video Player:** Memutar MP4, MKV, AVI, MOV; Fullscreen mode; Gesture brightness (kiri), volume (kanan), horizontal seek, double-tap ±10s; Playback Speed `0.5x–2.0x`; Subtitle `.srt` (sidecar/SAF); Picture-in-Picture (PiP).
- **Smart Vocal ON/OFF & Vocal Volume Slider (0%–100%):**
  - Pemisahan vokal & instrumental menggunakan **ONNX Runtime Mobile** (model **UVR MDX-Net** seperti `UVR-MDX-NET-Voc_FT.onnx` / `Kim_Vocal_2.onnx`) dengan fallback **Spectral STFT Center-Side Masking** (bukan sekadar equalizer) yang menyimpan stem `vocals.wav` & `accompaniment.wav` di cache lokal.
  - **Vocal Volume Slider (0%–100%):** `0%` = Karaoke/Instrumental, `25%` = Vokal sangat pelan, `50%` = Seimbang, `75%` = Vokal dominan, `100%` = Vokal asli.
  - **Sinkronisasi Video:** Saat mode vokal diubah pada video, video tetap berjalan mulus tanpa restart sementara `StemSyncAudioEngine` menjaga sinkronisasi audio-video.
- **Audio Effects:** Equalizer 10-Band (`31Hz–16kHz`), Bass Boost, Treble Control, Reverb, Surround (Virtualizer), Preset (`Flat`, `Pop`, `Rock`, `Jazz`, `Classical`, `EDM`, `Acoustic`, `Karaoke`), Master Volume & Balance L/R.
- **Desain Dark Premium:** `#101014` Background, `#8855FF` Neon Purple, `#20BFFF` Electric Blue, `#FF4DA6` Neon Pink, dengan opsi Light Theme di Settings.
