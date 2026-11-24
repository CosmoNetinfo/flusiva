# Changelog

## 0.4.1 — 2026-11-24

* Aggiornamenti automatici con electron-updater: controllo all’avvio e ogni 6 ore, download verificato SHA-512 e avanzamento.
* Windows e AppImage: installazione al riavvio richiesto o alla chiusura normale quando non ci sono lavori pendenti. Debian: installazione esplicita con autorizzazione del sistema.
* macOS: percorso automatico per build firmate Developer ID; build attuali non firmate con controllo versione e installazione manuale.
* Preferenza per disattivare l’automatismo, pulsanti di download/riavvio e blocco installazione durante download/coda.
* Feed ufficiali per i tre sistemi, manifest Mac con entrambe le architetture, pubblicazione atomica tramite bozza Release e checksum SHA-256.
* Configurazione GitHub Secrets predisposta per firma/notarizzazione Apple. Migrazione da 0.3: installare 0.4 manualmente una volta.
* 41 test deterministici, inclusi errori e feed incompleti; controllo updater incluso nei pacchetti della CI.


## 0.3.0 — 2026-11-24

* Coda seriale: inserimento multiplo, deduplicazione, pausa dopo il file attuale, annullamento, tentativi individuali, apertura dei file e pulizia degli elementi terminati.
* Byte scaricati, dimensione, velocità e tempo rimanente reali per la traccia corrente; dimensioni stimate e dati assenti espliciti.
* Modalità qualità massima, attiva inizialmente, anche nella coda: migliore sorgente video/audio e MP3 a 320 kbps. Disattivabile per usare limiti manuali.
* Selezione video senza preferire contenitori MP4 inferiori alla migliore sorgente disponibile; remux senza aumento artificiale della qualità.
* Controllo manuale delle Release GitHub e controllo facoltativo all’avvio. Versioni stabili verificate, gestione offline e limite richieste. Installazione manuale.
* Licenza del progetto GPL-3.0-only, sezione Uso responsabile e descrizione About della repository.
* 33 test deterministici; test Electron su coda, preferenze e controlli. Spotify rimandato per scelta del proprietario.

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
