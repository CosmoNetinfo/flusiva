# Strumenti distribuiti con Flusiva

Flusiva utilizza Electron, yt-dlp, FFmpeg e FFprobe. I binari sono distribuiti come programmi separati; le rispettive licenze e gli avvisi sono inclusi in `resources/tools/licenses`.

* Electron: MIT, https://github.com/electron/electron. Le licenze Chromium sono incluse nella distribuzione Electron.
* electron-updater 6.8.11: MIT, https://github.com/electron-userland/electron-builder. Il codice e la licenza sono inclusi nelle dipendenze dell’app.
* yt-dlp 2026.11.23: https://github.com/yt-dlp/yt-dlp/tree/2026.11.23. Le build autonome includono dipendenze con licenze proprie, elencate in THIRD_PARTY_LICENSES.txt.
* FFmpeg: GPL-3.0-or-later per le build statiche distribuite da https://github.com/eugeneware/ffmpeg-static/releases/tag/b6.1.1. Le versioni effettive variano con la piattaforma (ad esempio Windows 6.1.1 e Linux 7.0.2) e sono registrate in `tools/versions.json`. Il codice e le ricette di compilazione sono disponibili in https://github.com/eugeneware/ffmpeg-static; i relativi file README inclusi negli strumenti indicano origine e configurazione del binario. Archivi sorgenti: https://ffmpeg.org/releases/.
* FFprobe: distribuito dal pacchetto @ffprobe-installer/ffprobe 2.1.2, con versione specifica per piattaforma e informazioni di origine nel relativo pacchetto https://github.com/SavageCore/node-ffprobe-installer. Licenze e informazioni di build incluse nella cartella degli strumenti.

L'utente può sostituire i programmi esterni nella cartella `resources/tools` con versioni compatibili. Le licenze upstream restano applicabili ai rispettivi componenti.

## Android

La build Android usa youtubedl-android 0.18.1 (GPL-3.0), inclusi runtime Python, QuickJS e FFmpeg: sorgenti, script di compilazione e avvisi upstream in https://github.com/yausername/youtubedl-android/tree/0.18.1, BUILD_PYTHON.md e BUILD_FFMPEG.md. Sono inclusi il motore zipapp ufficiale yt-dlp 2026.11.23 (Unlicense, componenti con licenze proprie), https://github.com/yt-dlp/yt-dlp/tree/2026.11.23, e il runtime libc++ del NDK 27.0.12077973 (Apache-2.0 con eccezioni LLVM), https://github.com/llvm/llvm-project/blob/main/libcxx/LICENSE.TXT. L’APK contiene LICENSE.txt, NOTICE.txt e NDK-NOTICE.txt negli asset. Le dipendenze AndroidX conservano la licenza Apache-2.0; Kotlin conserva Apache-2.0.
