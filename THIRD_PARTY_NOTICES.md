# Strumenti distribuiti con Flusiva

Flusiva utilizza Electron, yt-dlp, FFmpeg e FFprobe. I binari sono distribuiti come programmi separati; le rispettive licenze e gli avvisi sono inclusi in `resources/tools/licenses`.

* Electron: MIT, https://github.com/electron/electron. Le licenze Chromium sono incluse nella distribuzione Electron.
* yt-dlp 2026.11.23: https://github.com/yt-dlp/yt-dlp/tree/2026.11.23. Le build autonome includono dipendenze con licenze proprie, elencate in THIRD_PARTY_LICENSES.txt.
* FFmpeg 6.1.1: GPL-3.0-or-later per le build statiche distribuite da https://github.com/eugeneware/ffmpeg-static/releases/tag/b6.1.1. Il codice e le ricette di compilazione sono disponibili in https://github.com/eugeneware/ffmpeg-static e https://ffmpeg.org/releases/ffmpeg-6.1.1.tar.xz.
* FFprobe: distribuito dal pacchetto @ffprobe-installer/ffprobe 2.1.2, con versione specifica per piattaforma e informazioni di origine nel relativo pacchetto https://github.com/SavageCore/node-ffprobe-installer. Licenze e informazioni di build incluse nella cartella degli strumenti.

L'utente può sostituire i programmi esterni nella cartella `resources/tools` con versioni compatibili. Le licenze upstream restano applicabili ai rispettivi componenti.
