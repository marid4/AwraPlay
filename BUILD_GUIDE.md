# Panduan Build APK AwraPlay (GitHub Actions, Debug & Release)

## 1. Build Otomatis di GitHub Actions (Tanpa PC / Android Studio)
Proyek ini sudah dilengkapi workflow `.github/workflows/build-apk.yml` dan `.gitignore`:
1. Buat repository baru di GitHub, lalu unggah (push) seluruh isi folder `AwraPlay` (pastikan folder `.github/workflows/` ikut terunggah).
2. Buka tab **Actions** di repository GitHub Anda → pilih workflow **Build AwraPlay APK** → klik **Run workflow** (atau otomatis berjalan saat `push`).
3. Setelah selesai, unduh file APK dari bagian **Artifacts**:
   - `AwraPlay-Debug-APK` (`app-debug.apk` — langsung siap instal di Android)
   - `AwraPlay-Release-APK` (`app-release-unsigned.apk`, atau `app-release-signed.apk` jika Secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, dan `KEY_PASSWORD` sudah diatur di **Settings → Secrets and variables → Actions**).

---

## 2. Build APK Debug (Android Studio / AndroidIDE)
1. Ekstrak `AwraPlay-Android.zip` ke folder proyek Anda.
2. Buka folder `AwraPlay` di **Android Studio** (atau **AndroidIDE** — file `local.properties` sudah disiapkan agar SDK terdeteksi otomatis).
3. Tunggu proses **Gradle Sync** selesai.
4. Untuk build **Debug APK**:
   - Di Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
   - Atau via terminal: `./gradlew assembleDebug` (atau `gradle assembleDebug`).
5. Hasil APK Debug: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 3. Build APK Release (Signed APK untuk Distribusi)
1. Buat keystore baru (jika belum punya):
   ```bash
   keytool -genkey -v -keystore awraplay-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias awraplay
   ```
2. Di Android Studio: **Build → Generate Signed Bundle / APK...** → pilih **APK** → masukkan `awraplay-release.jks` → pilih variant **release** → **Create**.
3. Hasil APK Release (sudah teroptimasi R8/ProGuard): `app/build/outputs/apk/release/app-release.apk`.

---

## 4. Cara Mengaktifkan Model AI Vocal Separation (ONNX)
- Aplikasi sudah langsung dapat memisahkan vokal menggunakan mesin **Spectral STFT** bawaan secara offline.
- Untuk kualitas pemisahan AI studio penuh (**MDX-Net**), buka **Settings → AI Vocal Separation → Unduh `UVR-MDX-NET-Voc_FT.onnx`** (atau impor file `.onnx` lokal dari penyimpanan perangkat).
