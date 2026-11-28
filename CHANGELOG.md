# Changelog

## 0.9.0 — 2026-11-28

* Android: verifica dei file MP4/MP3 Meta con FFprobe incluso, indipendente dai decoder del telefono. Corretto il rifiuto di MP4 VP9 validi; durata, traccia richiesta e integrità rimangono controllate, con errori distinti.
* Aggiornamenti Android nelle Impostazioni: controllo, download con avanzamento e annullamento tramite DownloadManager, verifica e avvio dell’installazione di sistema. Download continua chiudendo la schermata e stato conservato dopo riapertura.
* Controllo automatico all’avvio una volta al giorno, disattivabile; download e installazione richiedono il comando dell’utente. Verifica SHA-256, package, versione e stessa firma dell’app installata. FileProvider limitato alla cartella degli aggiornamenti; installazione rinviata durante i download dei contenuti.
* Test per MP4 VP9 sintetico, tracce mancanti, file corrotto, APK reale, hash/versione/package errati, firma estranea e controlli della pagina aggiornamenti.

## 0.8.1 — 2026-11-28

* Prima pubblicazione delle funzioni 0.8; nomi Instagram/Facebook visibili anche mentre gli elementi della coda attendono l’analisi. I download fotografici sono indicati come FOTO.
* La build 0.8.0 è stata interrotta prima della pubblicazione per includere questa correzione.

## 0.8.0 — 2026-11-28

* MP4 da Reel e video pubblici Instagram e Facebook, inclusi link Instagram `share/`, Facebook `share/v/`, `share/r/` e `fb.watch` con redirect controllati.
* Foto dei post Instagram: selezione nei caroselli, file JPEG/PNG/WebP senza ricompressione e distinzione esplicita dalle copertine video tramite un’estensione del motore inclusa nell’app.
* Dati Meta incompleti: durata e codec assenti non bloccano un video valido. Le tracce e il limite di durata sono verificati sul file prodotto.
* Android: preparazione del motore in background, riuso dei metadati per 90 secondi e quattro frammenti paralleli quando disponibili. Nuova analisi dopo scadenza o cambio del contenuto.
* Indicazioni di fonte/formato, elenco di sette piattaforme e controlli foto su Android e desktop. Test aggiunti per link, immagini, stato UI, salvataggio e metadati recenti.

## 0.7.1 — 2026-11-25

* Stati visivi coerenti per icone disabilitate e comandi selezionati.
* Prova delle preferenze con attesa esplicita dell’evento di input, oltre ai controlli di navigazione, fonte e playback della 0.7.

## 0.7.0 — 2026-11-25

* Android: pagina Scarica distinta dalla sezione Musica, con navigazione dedicata a Lettore, Playlist e Audio/equalizzatore. I contenuti non sono più impilati in un’unica schermata.
* Comandi musicali a icone, pulsante centrale di riproduzione, timer e modalità compatte, superfici piatte con feedback al tocco e stati disabilitati leggibili.
* Risposta dei controlli dagli eventi MediaController e avanzamento aggiornato localmente; qualità ed EQ vengono ridisegnati solo quando cambiano.
* Libreria con righe riutilizzate e salvataggi delle playlist fuori dal thread dell’interfaccia.
* Downloader con piattaforma riconosciuta dal link e comando esplicito per formato e fonte, come Scarica MP3 da YouTube.

## 0.6.0 — 2026-11-25

* Android: lettore musicale locale con Media3, controlli multimediali e riproduzione a schermo spento.
* Playlist persistenti: creazione, rinomina, aggiunta/rimozione, riordino e cancellazione dei riferimenti senza cancellare l’audio. Coda, casuale e ripetizione.
* Libreria dei file audio scaricati e importazione tramite selettore Android, senza permessi generali alla memoria.
* Codec, bitrate audio, frequenza, canali e decoder; i dati mancanti non vengono inventati e il bitrate video non viene presentato come qualità audio.
* Equalizzatore per la sessione Flusiva, bande e limiti letti dal dispositivo, preset, guadagni manuali e preferenze persistenti. Stato esplicito se non disponibile.
* Timer di spegnimento e controlli di posizione, precedente/successivo. Test di playback e persistenza su emulatore aggiunti alla CI.
* Le nuove funzioni musicali sono per Android; gli installer desktop restano inclusi nella Release unificata.

## 0.5.0 — 2026-11-24

* App Android nativa Kotlin per Android 10+, APK universale ARM64/ARMv7/x86_64.
* Download e conversione locali con yt-dlp, Python, QuickJS e FFmpeg inclusi. Stesse cinque piattaforme del desktop; Spotify rimandato.
* Ricezione link da Condividi, analisi, scelta MP4/MP3 e qualità massima o personalizzata.
* Salvataggio in Download/Flusiva con MediaStore, apertura e condivisione dei file, senza accesso generale alla memoria.
* Servizio con notifica, avanzamento, annullamento e protezione dai download simultanei. Preferenze locali e controllo aggiornamenti con download manuale dell’APK.
* Firma Android gratuita stabile nelle GitHub Actions; test JVM, Android Lint, test del motore e della UI su emulatore. APK incluso nella Release e nei checksum.
* Prima versione Android: singolo download, tema chiaro; coda e temi desktop non ancora disponibili su telefono.

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
