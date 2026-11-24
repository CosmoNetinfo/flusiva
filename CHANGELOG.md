# Changelog

## 0.2.0 — 2026-11-24

* Nuovi servizi: TikTok, Vimeo, Dailymotion e SoundCloud, oltre a YouTube; solo contenuti pubblici singoli. TikTok e Vimeo possono richiedere accesso o limitare le richieste.
* Link brevi con redirect verificati, formati comuni e gestione dei contenuti solo audio.
* Nuova pagina Impostazioni con cartella di download, richiesta della destinazione, qualità e formato predefiniti, apertura cartella, riduzione movimento e ripristino.
* Tema chiaro, scuro e di sistema, con preferenze salvate localmente in JSON tramite scrittura atomica.
* Salvataggio automatico con nomi univoci, senza sovrascrivere file esistenti.
* 19 test deterministici e test Electron esteso a navigazione e preferenze; profilo isolato per i test.
* Verifiche online e limitazioni per piattaforma documentate in `docs/PLATFORMS.md`.

## 0.1.1 — 2026-11-24

* Corretto il test CI su Linux: configura il sandbox helper con proprietario e permessi corretti.
* I test CI verificano interfaccia, bridge e binari senza richiedere screenshot a un compositor virtuale. Gli screenshot di documentazione restano acquisiti localmente dall’app reale.
* Conservati i controlli della sandbox; nessun uso di `--no-sandbox`.

## 0.1.0 — 2026-11-24

Prima versione desktop di Flusiva.

* Download da un singolo link YouTube, inclusi link brevi, Shorts e YouTube Music.
* MP4 alla migliore risoluzione disponibile o risoluzione massima scelta, con video e audio uniti localmente.
* MP3 a 128, 192, 256 o 320 kbps.
* Anteprima del video, selezione della destinazione, avanzamento, annullamento e apertura della cartella.
* Elaborazione locale con yt-dlp, FFmpeg, FFprobe e runtime Node inclusi.
* Renderer isolato, processi senza shell e pulizia dei temporanei.
* Identità blu/azzurro/bianco e logo puntinato, con studio delle alternative.
* Build GitHub Actions per Windows x64, Linux x64 e macOS Intel/Apple Silicon.

Limiti: niente playlist, live, login, aggiornamento automatico, altre piattaforme o certificati di firma. Qualità limitata alla sorgente; la conversione MP3 non ripristina dettagli persi.
